## P4 — Documentos + Versionado Lineal

### [US-DOC-001] Subir documento (API) crea documento + version 1

**Narrativa:** Como usuario con permisos, quiero subir un documento a una carpeta, para centralizarlo y compartirlo.

### Alcance funcional
- Crear registro de `Document` con metadatos basicos y asociarlo a la `Folder` del `organizacion_id` del token.
- Crear `DocumentVersion` inicial con `numero_secuencial = 1` y actualizar `version_actual_id`.
- Persistir el binario mediante un `StorageService` (implementacion local por defecto).
- Validar permisos `ESCRITURA` (o `ADMINISTRACION`) sobre la carpeta destino (directo o heredado).
- Emitir evento de auditoria `DOCUMENTO_CREADO` tras commit exitoso.

### No alcance
- UI de carga (ver US-DOC-006).
- Versionado adicional (US-DOC-003).
- Descarga de documentos (US-DOC-002).

### API
**Endpoint:** `POST /api/v1/folders/{folderId}/documents`

**Seguridad:** `Authorization: Bearer <token>`

**Request (multipart/form-data):**
- `file` (required): archivo binario.

**Response 201 (application/json):**
```json
{
    "documento_id": 123,
    "nombre": "contrato.pdf",
    "version_actual": {
        "id": 456,
        "numero_secuencial": 1
    }
}
```

**Errores:**
- `400` archivo invalido (tamano, extension) o payload mal formado.
- `401` token invalido o ausente.
- `403` sin permiso `ESCRITURA` en la carpeta.
- `404` carpeta inexistente o fuera del `organizacion_id` del token.

### Reglas de negocio
- `organizacion_id` siempre se toma del token; nunca del cliente.
- `numero_secuencial` inicial siempre es `1` y unico por `documento_id`.
- `version_actual_id` debe apuntar a la version creada.
- Si falla el storage, se revierte la transaccion (no se crea `Document` ni `DocumentVersion`).

### Validaciones de archivo (configurables por entorno)
- `maxFileSize` (bytes) en configuracion.
- Lista de extensiones permitidas o bloqueadas (por configuracion).
- Rechazar archivos vacios.

### Persistencia (modelo de datos)
- `Document`: `id`, `nombre`, `extension`, `carpeta_id`, `organizacion_id`, `version_actual_id`, `creado_por`, `fecha_creacion`, `fecha_eliminacion`.
- `DocumentVersion`: `id`, `documento_id`, `numero_secuencial`, `ruta_almacenamiento`, `tamanio_bytes`, `hash_contenido`, `creado_por`, `fecha_creacion`.
- Indices: `documento_id + numero_secuencial` unico; `carpeta_id`, `organizacion_id`, `version_actual_id`.

### Storage
- Interfaz `StorageService` con `upload`, `download`, `delete`.
- Implementacion local: `/{organizacion_id}/{carpeta_id}/{documento_id}/{numero_secuencial}/`.
- Guardar `ruta_almacenamiento` resultante en `DocumentVersion`.

### Auditoria
- Emitir `DOCUMENTO_CREADO` con `documento_id`, `usuario_id`, `organizacion_id` despues del commit.

### Archivos y capas a modificar (Document-Core)
- **Domain:** `Document`, `DocumentVersion` y repositorios.
- **Application:** `DocumentService.createDocument(...)` y DTOs request/response.
- **Infrastructure:** entidades JPA, repositorios Spring Data, `LocalStorageService`.
- **Presentation:** controlador REST para `POST /api/v1/folders/{folderId}/documents`.
- **Config:** propiedades de storage y validaciones de archivo en `application.yml`.

### Criterios de aceptacion
- *Scenario 1:* Dado `ESCRITURA` en la carpeta, Cuando subo un archivo valido, Entonces recibo `201` con `documento_id` y `version_actual.numero_secuencial = 1`.
- *Scenario 2:* Dado sin permisos, Cuando subo, Entonces recibo `403`.
- *Scenario 3:* Dado archivo invalido (tamano o extension), Cuando subo, Entonces recibo `400`.
- *Scenario 4:* Dado carpeta fuera de la organizacion, Cuando subo, Entonces recibo `404`.

### Pruebas
- **Unitarias (min 6):**
    - Creacion exitosa con permisos.
    - Rechazo por falta de permisos.
    - Rechazo por archivo invalido.
    - Rollback ante fallo de storage.
    - `numero_secuencial = 1` y `version_actual_id` actualizado.
    - Validacion de organizacion (token vs carpeta).
- **Integracion:**
    - `POST /api/v1/folders/{folderId}/documents` -> `201` y persistencia en BD.
    - `403` sin permiso.
    - `400` por archivo invalido.

### Documentacion
- Actualizar `api-spec.yml` con el endpoint y esquemas de respuesta.
- Documentar variables de entorno de storage y validaciones en README del servicio.

### Requisitos no funcionales
- **Seguridad:** no loguear contenido ni rutas sensibles; validar pertenencia a organizacion.
- **Performance:** usar streaming para el upload y evitar cargar archivos completos en memoria.
- **Observabilidad:** logs estructurados con `documento_id` y `organizacion_id`.

---

## 3. Flujo recomendado de ejecución

```
1. [BD] Crear modelo de Documento
   ↓
2. [BD] Crear modelo de Version_Documento
   ↓
3. [BD] Crear índices y constraints
   ↓
4. [Backend] Implementar servicio de almacenamiento
   ↓
5. [Backend] Implementar validador de permisos
   ↓
6. [Backend] Implementar servicio de creación de documento
   ↓
7. [Backend] Implementar endpoint POST
   ↓
8. [Backend] Implementar validaciones de archivo
   ↓
9. [Backend] Emitir evento de auditoría
   ↓
10. [QA] Pruebas unitarias
    ↓
11. [QA] Pruebas de integración
    ↓
12. [BD] Datos semilla (paralelo o al inicio)
```

### Dependencias entre tickets:
- Modelos de BD son prerequisito para todo el backend
- Servicio de storage es prerequisito para servicio de documentos
- Validador de permisos depende del sistema ACL (P2)
- Endpoint depende del servicio de documentos
- Auditoría depende del sistema de auditoría (P5)
- QA depende de todos los componentes implementados

---

## 4. Recomendación TDD/BDD

### Tickets que deberían tener pruebas primero (TDD):
1. **Servicio de almacenamiento** - Lógica crítica de persistencia
2. **Validador de permisos** - Seguridad, debe ser robusto
3. **Servicio de creación de documento** - Lógica de negocio central
4. **Validaciones de archivo** - Casos de borde importantes

### Tickets que se prestan a escenarios BDD:
```gherkin
Feature: Subir documento a carpeta

  Scenario: Subida exitosa con permisos de escritura
    Given un usuario autenticado con permiso ESCRITURA en carpeta "Proyectos"
    When el usuario sube un archivo "informe.pdf" de 2MB
    Then recibe respuesta 201
    And la respuesta incluye "documento_id" y "version_actual"
    And "version_actual.numero_secuencial" es 1

  Scenario: Subida rechazada sin permisos
    Given un usuario autenticado sin permiso en carpeta "Confidencial"
    When el usuario intenta subir un archivo
    Then recibe respuesta 403
    And el mensaje indica "Sin permisos de escritura"

  Scenario: Subida rechazada por archivo muy grande
    Given un usuario autenticado con permiso ESCRITURA
    And el límite de tamaño configurado es 10MB
    When el usuario intenta subir un archivo de 15MB
    Then recibe respuesta 400
    And el mensaje indica "Archivo excede tamaño máximo"
```

---