## P4 — Documentos + Versionado Lineal

### [US-DOC-005] Cambiar versión actual (API) (rollback)

### Narrativa de Usuario

Como **usuario autorizado con permisos de administración** en un documento,  
Quiero **marcar una versión anterior como versión actual (rollback)**,  
Para que **pueda revertir cambios no deseados sin perder el historial de versiones**.

### Criterios de Aceptación (BDD)

#### Escenario 1: Rollback exitoso a versión anterior
```gherkin
Dado un documento existente con múltiples versiones:
  - Versión 1 (original) con contenido "Documento inicial"
  - Versión 2 con contenido "Cambios realizados"
  - Versión 3 (actual) con contenido "Cambios adicionales"
Y un usuario autenticado con permiso ADMINISTRACION sobre el documento
Cuando envía una solicitud PATCH a /api/documents/{documentId}/current-version
  con body: { "version_id": "<id-version-1>" }
Entonces:
  - Recibe respuesta HTTP 200 OK
  - El campo version_actual_id del documento ahora apunta a Versión 1
  - Se registra un evento de auditoría con código VERSION_ROLLBACK
  - El evento incluye: version_anterior_id (Versión 3), version_nueva_id (Versión 1), usuario_id, timestamp
  - Las versiones 1, 2 y 3 siguen existiendo sin cambios
  - El siguiente usuario que descargue el documento obtiene contenido de Versión 1
```

#### Escenario 2: Rollback rechazado sin permisos elevados
```gherkin
Dado un documento existente con múltiples versiones
Y un usuario autenticado con permiso ESCRITURA (pero NO ADMINISTRACION)
Cuando intenta cambiar la versión actual
Entonces:
  - Recibe respuesta HTTP 403 Forbidden
  - El mensaje de error especifica "Permiso insuficiente para cambiar versión actual"
  - No se realiza cambio alguno en la base de datos
  - No se emite evento de auditoría
```

#### Escenario 3: Rollback rechazado con versión inexistente
```gherkin
Dado un documento con versiones existentes
Y un version_id que no existe o no pertenece al documento
Cuando intenta hacer rollback a esa versión
Entonces:
  - Recibe respuesta HTTP 400 Bad Request
  - El mensaje de error especifica "La versión solicitada no pertenece al documento"
  - No se realiza cambio en la base de datos
```

#### Escenario 4: Rollback rechazado con documento inexistente
```gherkin
Dado un documentId que no existe
Y un usuario autenticado con permisos válidos
Cuando intenta hacer rollback
Entonces:
  - Recibe respuesta HTTP 404 Not Found
  - El mensaje de error especifica "Documento no encontrado"
  - No se emite evento de auditoría
```

#### Escenario 5: Aislamiento de tenant validado
```gherkin
Dado un documento en organizacion_id = "org-123"
Y un usuario autenticado en organizacion_id = "org-456"
Cuando intenta hacer rollback al documento
Entonces:
  - Recibe respuesta HTTP 404 Not Found
  - No se revela que el documento existe en otro tenant (seguridad)
```

#### Escenario 6: Prevención de rollback a la versión actual
```gherkin
Dado un documento con version_actual_id = versión X
Y un usuario intenta hacer rollback a versión X (la misma actual)
Entonces:
  - Se acepta la operación (idempotente)
  - Recibe respuesta HTTP 200 OK
  - Se registra auditoría (aunque no hay cambio real)
  - El estado del documento permanece igual
```

### Campos Técnicos Involucrados

| Campo | Tabla | Tipo | Descripción |
|-------|-------|------|-------------|
| `version_actual_id` | `Documento` | UUID | Identificador de la versión marcada como actual |
| `version_id` | `DocumentVersion` | UUID | Identificador único de cada versión |
| `documento_id` | `DocumentVersion` | UUID | Referencia al documento propietario |
| `organizacion_id` | `Documento` | UUID | Tenant owner del documento |
| `usuario_id` | Auditoría | UUID | Usuario que ejecutó la operación |
| `codigo_evento` | `AuditEvent` | String | VERSION_ROLLBACK |
| `version_anterior_id` | `AuditEvent` | UUID | Versión anterior al rollback |
| `version_nueva_id` | `AuditEvent` | UUID | Versión nueva después del rollback |
| `timestamp` | `AuditEvent` | DateTime | Momento de la operación |

### Endpoints y Contratos

#### Request
```http
PATCH /api/documents/{documentId}/current-version
Authorization: Bearer <token>
Content-Type: application/json

{
  "version_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### Response 200 OK
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "nombre": "Mi Documento",
  "version_actual_id": "550e8400-e29b-41d4-a716-446655440000",
  "estado": "ACTIVO",
  "fecha_creacion": "2025-02-01T10:00:00Z",
  "fecha_actualizacion": "2026-02-05T15:30:00Z",
  "numero_total_versiones": 3
}
```

#### Response 400 Bad Request
```json
{
  "error": "INVALID_VERSION",
  "mensaje": "La versión solicitada no pertenece al documento",
  "detalles": {
    "version_id": "550e8400-e29b-41d4-a716-446655440999",
    "documento_id": "550e8400-e29b-41d4-a716-446655440001"
  }
}
```

#### Response 403 Forbidden
```json
{
  "error": "PERMISSION_DENIED",
  "mensaje": "No posee permiso requerido para cambiar versión actual",
  "detalles": {
    "permiso_requerido": "ADMINISTRACION",
    "recurso": "DOCUMENTO"
  }
}
```

### Consideraciones de Seguridad

1. **Validación de Permisos**: El usuario MUST tener permiso `ADMINISTRACION` sobre el documento (no solo `ESCRITURA`)
2. **Aislamiento de Tenant**: Toda operación debe validar que documento y versión pertenecen al organizacion_id del token
3. **Atomicidad**: La actualización de `version_actual_id` MUST ser atómica; debe completarse o no ejecutarse
4. **Auditoría Obligatoria**: La operación MUST registrar auditoría incluso si falla autenticación (fallida intentada) o autorización
5. **Validación Referencial**: La versión MUST ser verificada como existente y propiedad del documento en la misma operación
6. **No hay Soft Delete**: Las versiones nunca se eliminan; solo cambia el puntero

### Consideraciones No-Funcionales

- **Performance**: La query de actualización debe completarse en < 100ms (índice en `version_actual_id`)
- **Idempotencia**: Hacer rollback a la misma versión actual debe ser seguro (200 OK + auditoría registrada)
- **Logging**: Registrar en logs INFO "VERSION_ROLLBACK ejecutado" con documento_id, usuario_id, versiones
- **Rastrabilidad**: La auditoría MUST incluir tanto versión anterior como nueva para trazabilidad completa
