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

---

## [original] 2. Lista de tickets necesarios

---
### Base de datos
---

* **Título:** Query para validar versión pertenece a documento
* **Objetivo:** Verificar que la versión solicitada existe y pertenece al documento.
* **Tipo:** Tarea
* **Descripción corta:** Crear consulta que dado `version_id`, `documento_id` y `organizacion_id`, valide que la versión existe, pertenece al documento y el documento pertenece al tenant.
* **Entregables:**
    - Método `validateVersionBelongsToDocument(versionId, documentId, orgId)`.
    - Test de integración de BD.

---

* **Título:** Query atómico para actualizar version_actual_id
* **Objetivo:** Actualizar el puntero de versión actual de forma segura.
* **Tipo:** Tarea
* **Descripción corta:** Implementar update atómico que modifique `version_actual_id` en la tabla `Documento`, incluyendo validación de existencia en la misma operación.
* **Entregables:**
    - Método `updateCurrentVersion(documentId, newVersionId, orgId)`.
    - Retorno de filas afectadas para verificación.

---
### Backend
---

* **Título:** Definir permiso requerido para rollback
* **Objetivo:** Establecer qué nivel de permiso permite cambiar la versión actual.
* **Tipo:** Diseño / Tarea
* **Descripción corta:** Documentar y configurar el permiso necesario para rollback. Recomendación: `ADMINISTRACION` sobre el documento/carpeta, o un permiso específico `ROLLBACK` si el modelo lo soporta.
* **Entregables:**
    - Documentación de decisión de diseño.
    - Configuración en sistema ACL si es necesario.
    - Constante `PERMISSION_ROLLBACK` o similar.

---

* **Título:** Implementar validador de permisos para rollback
* **Objetivo:** Verificar que el usuario tiene permiso para cambiar versión actual.
* **Tipo:** Tarea
* **Descripción corta:** Crear método que valide el permiso definido (ej. ADMINISTRACION) sobre el documento/carpeta.
* **Entregables:**
    - Método `hasRollbackPermission(userId, documentId, orgId)`.
    - Integración con sistema ACL.

---

* **Título:** Implementar servicio de cambio de versión actual
* **Objetivo:** Lógica de negocio para ejecutar el rollback.
* **Tipo:** Historia
* **Descripción corta:** Crear `DocumentService.setCurrentVersion()` que: valide documento existente, valide versión pertenece al documento, valide permisos de rollback, actualice `version_actual_id`, emita evento de auditoría.
* **Entregables:**
    - Método `setCurrentVersion(documentId, versionId, userId, orgId)`.
    - Validaciones completas antes de actualizar.
    - DTO de respuesta con nuevo estado del documento.

---

* **Título:** Implementar endpoint `PATCH /api/documents/{documentId}/current-version`
* **Objetivo:** Exponer API REST para operación de rollback.
* **Tipo:** Historia
* **Descripción corta:** Endpoint que recibe `version_id` en body, valida token, invoca servicio de cambio y retorna estado actualizado del documento.
* **Entregables:**
    - Controlador con ruta `PATCH /api/documents/{documentId}/current-version`.
    - Body request: `{ "version_id": "uuid" }`.
    - Respuesta 200 con documento actualizado.
    - Respuesta 400 si versión no válida, 403 sin permisos, 404 no existe.

---

* **Título:** Emitir evento de auditoría para rollback
* **Objetivo:** Registrar obligatoriamente la operación de rollback.
* **Tipo:** Tarea
* **Descripción corta:** Al completar rollback exitoso, emitir evento con `codigo_evento=VERSION_ROLLBACK`, `documento_id`, `version_anterior_id`, `version_nueva_id`, `usuario_id`.
* **Entregables:**
    - Integración con servicio de auditoría.
    - Evento incluye versión anterior y nueva para trazabilidad completa.

---

* **Título:** Pruebas unitarias del servicio de rollback
* **Objetivo:** Asegurar lógica correcta de cambio de versión.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios para `DocumentService.setCurrentVersion()` cubriendo: rollback exitoso, versión no pertenece al documento, documento no existe, sin permisos, versión no existe.
* **Entregables:**
    - Suite de tests unitarios (mínimo 6 casos).
    - Verificación de auditoría emitida.

---

* **Título:** Pruebas de integración del endpoint de rollback
* **Objetivo:** Verificar flujo completo HTTP y persistencia.
* **Tipo:** QA
* **Descripción corta:** Tests de integración para `PATCH /api/documents/{id}/current-version` validando: 200 con version_actual actualizado, 403 sin permisos, 404 documento/versión inexistente, 400 versión no válida.
* **Entregables:**
    - Tests de integración para escenarios de aceptación.
    - Verificación de `version_actual_id` en BD.
    - Verificación de evento de auditoría creado.

---
### Frontend
---

* **Título:** Sin cambios de UI para US-DOC-005
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La funcionalidad de rollback en UI corresponde a `US-DOC-006` (si se incluye en la UI mínima).
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.

---

## [original] 3. Flujo recomendado de ejecución

```
1. [Diseño] Definir permiso requerido para rollback
   ↓
2. [BD] Query validar versión pertenece a documento
   ↓
3. [BD] Query atómico para actualizar version_actual_id
   ↓
4. [Backend] Implementar validador de permisos para rollback
   ↓
5. [Backend] Implementar servicio de cambio de versión
   ↓
6. [Backend] Implementar endpoint PATCH
   ↓
7. [Backend] Emitir evento de auditoría
   ↓
8. [QA] Pruebas unitarias
   ↓
9. [QA] Pruebas de integración
```

### Dependencias entre tickets:
- Depende de US-DOC-001, US-DOC-003 (modelos y versiones)
- Depende de US-DOC-004 implícitamente (para ver qué versiones existen)
- Decisión de diseño de permisos es prerequisito
- Queries de BD son prerequisito para servicio
- Auditoría depende del sistema de auditoría (P5)
- Auditoría es crítica y no opcional para esta historia

---

## [original] 4. Recomendación TDD/BDD

### Tickets que deberían tener pruebas primero (TDD):
1. **Validación versión pertenece a documento** - Seguridad
2. **Validador de permisos rollback** - Autorización crítica
3. **Servicio de cambio de versión** - Lógica de negocio central
4. **Emisión de auditoría** - Trazabilidad obligatoria

### Tickets que se prestan a escenarios BDD:
```gherkin
Feature: Cambiar versión actual (rollback)

  Scenario: Rollback exitoso a versión anterior
    Given un documento con versiones 1, 2, 3
    And version_actual es la versión 3
    And un usuario autenticado con permiso ADMINISTRACION
    When el usuario selecciona la versión 2 como actual
    Then recibe respuesta 200
    And version_actual del documento ahora es la versión 2
    And se registra evento de auditoría VERSION_ROLLBACK

  Scenario: Rollback preserva todas las versiones
    Given un documento con versiones 1, 2, 3
    When el usuario hace rollback a versión 1
    Then las versiones 1, 2 y 3 siguen existiendo
    And solo cambia el puntero version_actual_id

  Scenario: Rollback rechazado sin permiso elevado
    Given un documento existente
    And un usuario autenticado con solo permiso ESCRITURA (no ADMINISTRACION)
    When el usuario intenta hacer rollback
    Then recibe respuesta 403
    And el mensaje indica "Permiso insuficiente para rollback"

  Scenario: Rollback rechazado con versión de otro documento
    Given documento A con versión "v-abc"
    And documento B con versión "v-xyz"
    When el usuario intenta hacer rollback de documento A a versión "v-xyz"
    Then recibe respuesta 400
    And el mensaje indica "Versión no pertenece al documento"

  Scenario: Auditoría de rollback incluye versiones anterior y nueva
    Given un documento con version_actual = versión 3
    When el usuario hace rollback a versión 1
    Then el evento de auditoría incluye version_anterior_id = versión 3
    And el evento de auditoría incluye version_nueva_id = versión 1

  Scenario: Rollback respeta aislamiento de tenant
    Given un documento en organizacion A
    And un usuario autenticado en organizacion B
    When el usuario intenta hacer rollback
    Then recibe respuesta 404
    And no se revela que el documento existe en otro tenant
```

---

## [original] 5. Archivos generados

| Archivo | Contenido |
|---------|-----------|
| `US-DOC-001.md` | Subir documento (API) |
| `US-DOC-002.md` | Descargar versión actual (API) |
| `US-DOC-003.md` | Subir nueva versión (API) |
| `US-DOC-004.md` | Listar versiones (API) |
| `US-DOC-005.md` | Este archivo - Cambiar versión actual (rollback) |
| `US-DOC-006.md` | UI mínima de carga y ver historial |
