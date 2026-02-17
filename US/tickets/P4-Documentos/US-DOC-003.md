## P4 — Documentos + Versionado Lineal

### [US-DOC-003] Subir nueva versión (API) incrementa secuencia

### 1. Narrativa y Criterios de Aceptación

**Narrativa:**
> Como usuario autenticado con permiso de ESCRITURA sobre un documento o su carpeta padre, quiero subir una nueva versión del documento, para mantener un historial completo de cambios sin sobrescribir versiones anteriores.

**Criterios de Aceptación Detallados:**

```gherkin
Feature: Subir nueva versión (API) incrementa secuencia

  Scenario 1: Nueva versión incrementa secuencia correctamente
    Given un documento existente "documento-uuid-123" con numero_secuencial 2
    And un usuario autenticado con permiso ESCRITURA
    When el usuario ejecuta POST /api/documents/documento-uuid-123/versions
    Then recibe respuesta HTTP 201 Created
    And la nueva versión tiene numero_secuencial 3
    And version_actual_id se actualiza a la nueva versión
    And se emite evento VERSION_CREADA

  Scenario 2: Nueva versión rechazada sin permisos
    Given un documento existente
    And un usuario autenticado sin permiso ESCRITURA
    When intenta POST /api/documents/{id}/versions
    Then recibe respuesta HTTP 403 Forbidden

  Scenario 3: Documento inexistente
    When intenta POST /api/documents/uuid-inexistente/versions
    Then recibe respuesta HTTP 404 Not Found

  Scenario 4: Validación de archivo
    When envía archivo vacío
    Then recibe respuesta HTTP 400 Bad Request
    When envía archivo > 500 MB
    Then recibe respuesta HTTP 413 Payload Too Large

  Scenario 5: Concurrencia en versionado
    Given dos usuarios con permiso ESCRITURA
    When ambos suben versión simultáneamente
    Then una recibe 201 con numero_secuencial 6
    And la otra recibe 409 Conflict

  Scenario 6: Token inválido
    When se envía sin Authorization header
    Then recibe respuesta HTTP 401 Unauthorized
```

---

### 2. Especificación de API REST

#### Endpoint
```
POST /api/documents/{documentId}/versions
```

#### Parámetros
| Tipo | Nombre | Tipo | Obligatorio | Descripción |
|------|--------|------|-------------|-------------|
| Path | documentId | UUID | Sí | ID del documento a versionar |
| Header | Authorization | String | Sí | Bearer token JWT |
| Body (multipart) | file | File | Sí | Archivo binario (max 500 MB) |
| Body (multipart) | comentario | String | No | Comentario (max 500 caracteres) |

#### Response (201 Created)
```json
{
  "status": "success",
  "data": {
    "version_id": "v5-uuid-abc123",
    "documento_id": "doc-uuid-123",
    "numero_secuencial": 5,
    "fecha_creacion": "2026-02-05T14:30:45.123Z",
    "tamanio_bytes": 1048576,
    "hash_contenido": "sha256:abcd1234...",
    "es_version_actual": true
  }
}
```

---

### 3. Modelos de Datos

#### Entity DocumentoVersion
```sql
CREATE TABLE IF NOT EXISTS documento_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL,
    numero_secuencial INTEGER NOT NULL,
    url_storage VARCHAR(1024) NOT NULL,
    tamanio_bytes BIGINT NOT NULL,
    hash_contenido VARCHAR(64) NOT NULL,
    usuario_creador_id UUID NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    organizacion_id UUID NOT NULL,
    
    UNIQUE(documento_id, numero_secuencial),
    FOREIGN KEY(documento_id) REFERENCES documento(id),
    FOREIGN KEY(usuario_creador_id) REFERENCES usuario(id),
    FOREIGN KEY(organizacion_id) REFERENCES organizacion(id)
);

ALTER TABLE documento ADD COLUMN version_actual_id UUID;
ALTER TABLE documento ADD COLUMN fecha_modificacion TIMESTAMP;
```

---

### 4. Lógica de Negocio

```
PROCEDURE CreateVersion(documentId, file, userId, organizacionId)
BEGIN TRANSACTION (ISOLATION LEVEL: SERIALIZABLE)
    
    1. Validar documento existe
    2. Validar permiso ESCRITURA
    3. numero_secuencial = MAX + 1
    4. Upload archivo (SHA256 hash)
    5. Crear documento_version
    6. UPDATE documento.version_actual_id
    7. Emitir evento VERSION_CREADA
    
COMMIT TRANSACTION
END
```

---

### 5. Archivos a Modificar

```
backend/document-core/src/main/java/
├── domain/DocumentoVersion.java              [CREATE]
├── application/DocumentVersionService.java   [CREATE]
├── infrastructure/adapters/*Repository*.java [CREATE]
└── presentation/DocumentVersionController.java [CREATE]

db/migrations/V3__create_documento_version_table.sql [CREATE]
```

---

### 6. Definición de Hecho (DoD)

- [ ] Tests unitarios (coverage >80%)
- [ ] Tests de integración (AC1-AC6)
- [ ] Tests de concurrencia
- [ ] Migración BD ejecutada
- [ ] Endpoint documentado en Swagger
- [ ] Evento de auditoría emitido
- [ ] Code review aprobado
- [ ] QA validado