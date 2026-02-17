## P2 — Permisos Granulares (ACL por carpeta/documento)

**[US-ACL-005] Conceder permiso explícito a documento**

- **Narrativa:** Como administrador, quiero asignar un permiso directamente a un documento, para manejar excepciones de acceso.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un documento en una carpeta, Cuando asigno `LECTURA` explícita a un usuario, Entonces el usuario puede acceder a ese documento.

---

## [enhanced]

### Historia de Usuario (enriquecida)
**Como** administrador de una organización, **quiero** otorgar un permiso explícito a un usuario sobre un documento específico, **para** habilitar excepciones de acceso sin modificar los permisos de la carpeta contenedora.

### Objetivos
- Permitir crear o actualizar una ACL de documento por usuario (una sola entrada por documento/usuario).
- Garantizar aislamiento multi‑tenant (organización del token).
- Alinear el comportamiento con ACL de carpeta (patrones existentes en `document-core`).

### No‑objetivos
- No definir la precedencia carpeta vs documento (se define en US-ACL-006).
- No exponer datos de otras organizaciones en errores.

### Reglas de Negocio
1. **Solo administradores** (rol admin) o usuarios con permiso `ADMINISTRACION` sobre la **carpeta padre** del documento pueden asignar/revocar permisos del documento.
2. Documento, usuario y permiso deben pertenecer a la **misma organización** del token.
3. **Un único ACL por (documento, usuario)**: si existe, se **actualiza** el nivel de acceso.
4. El permiso explícito del documento **no altera** permisos de carpeta ni herencia.
5. Si el documento no pertenece a la organización, responder **404 genérico**.

### Modelo de Datos (DB)
Tabla `permiso_documento_usuario` (nombre sugerido, consistente con `permiso_carpeta_usuario`).
- `id` (PK)
- `documento_id` (FK, not null)
- `usuario_id` (FK, not null)
- `organizacion_id` (FK, not null)
- `nivel_acceso` (ENUM string, not null)
- `fecha_asignacion` (timestamp, not null)

**Índices:**
- Único: (`documento_id`, `usuario_id`)
- Índice por `documento_id`
- Índice por `usuario_id`

### API (Backend - document-core)
**Base:** `/api`

#### 1) Crear/Actualizar permiso de documento
`POST /documentos/{documentoId}/permisos`

**Request body**
```json
{
    "usuario_id": 123,
    "nivel_acceso_codigo": "LECTURA"
}
```

**Respuestas**
- `201 Created` si se creó una nueva ACL.
- `200 OK` si se actualizó una ACL existente.
- `400 Bad Request` si `nivel_acceso_codigo` es inválido.
- `403 Forbidden` si no tiene rol admin ni permiso `ADMINISTRACION` en carpeta padre.
- `404 Not Found` si el documento o usuario no pertenece a la organización.

**Notas de seguridad:** `organizacion_id` y `usuario_admin_id` se toman del JWT (no del body). Mantener el patrón actual de `X-Organization-Id` como fallback solo si ya existe en el servicio.

#### 2) Revocar permiso de documento
`DELETE /documentos/{documentoId}/permisos/{usuarioId}`

**Respuestas**
- `204 No Content` si se revocó.
- `403 Forbidden` si no autorizado.
- `404 Not Found` si no existe o no pertenece a la organización.

### Validaciones y Errores
- Validar existencia de documento en la organización (`validarDocumentoEnOrganizacion`).
- Validar existencia de usuario en la organización.
- Validar `nivel_acceso_codigo` contra catálogo `NivelAcceso`.
- Errores deben ser **genéricos** para evitar fuga de datos entre organizaciones.

### Eventos / Auditoría
- Emitir eventos de dominio similares a `PermisoCarpetaUsuarioCreated/Updated/Revoked` para auditoría.
- Log con nivel INFO sin datos sensibles (documentoId, usuarioId, organizacionId).

### Archivos a Crear/Modificar
**Backend (document-core):**
- `src/main/java/com/docflow/documentcore/domain/model/permiso/PermisoDocumentoUsuario.java` (nuevo)
- `src/main/java/com/docflow/documentcore/domain/repository/IPermisoDocumentoUsuarioRepository.java` (nuevo)
- `src/main/java/com/docflow/documentcore/application/service/PermisoDocumentoUsuarioService.java` (nuevo)
- `src/main/java/com/docflow/documentcore/application/validator/PermisoDocumentoUsuarioValidator.java` (nuevo)
- `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/jpa/PermisoDocumentoUsuarioJpaRepository.java` (nuevo)
- `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/PermisoDocumentoUsuarioRepositoryAdapter.java` (nuevo)
- `src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/PermisoDocumentoUsuarioController.java` (nuevo)
- `src/main/resources/db/migration/VXXX__create_permiso_documento_usuario.sql` (nuevo)

**Frontend (ACL):**
- `src/features/acl/services/aclDocumentoService.ts` (nuevo)
- `src/features/acl/hooks/useAclDocumento.ts` (nuevo)
- `src/features/acl/types/index.ts` (agregar DTOs y tipos)
- `src/features/acl/components/AdministrarPermisosDocumentoModal.tsx` (nuevo)
- Integración en menú contextual de documento (feature documento)

### Criterios de Aceptación (BDD)
```gherkin
Scenario: Admin asigna permiso LECTURA a documento
    Given un administrador autenticado del organizacion "A"
    And un documento "Contrato.pdf" en carpeta "Docs" del organizacion "A"
    And un usuario "juan@test.com" sin permiso en la carpeta
    When asigno permiso "LECTURA" explícito al usuario sobre el documento
    Then recibo status 201
    And el usuario puede acceder al documento

Scenario: Admin actualiza permiso existente a ESCRITURA
    Given un administrador autenticado
    And un usuario con permiso "LECTURA" sobre el documento
    When asigno permiso "ESCRITURA" explícito al mismo usuario
    Then recibo status 200
    And el permiso del usuario queda en "ESCRITURA"

Scenario: Admin revoca permiso explícito de documento
    Given un administrador autenticado
    And usuario "juan@test.com" con permiso sobre documento "Contrato.pdf"
    When revoco el permiso del documento
    Then recibo status 204
    And el usuario ya no puede acceder al documento
```

### Pruebas Requeridas
**Unitarias:**
- Crear ACL documento (nuevo).
- Actualizar ACL documento (existente).
- Documento inexistente (404).
- Usuario de otra organización (404).
- Rechazo por falta de admin/ADMINISTRACION (403).

**Integración:**
- POST y DELETE con token válido.
- Aislamiento multi‑tenant (org A no ve org B).

### Requisitos No Funcionales
- **Seguridad:** No exponer IDs de otras organizaciones; usar JWT para claims.
- **Rendimiento:** Índices en `documento_id` y `usuario_id`.
- **Observabilidad:** logs INFO y eventos de auditoría.
