# Backend Implementation Plan: US-DOC-008 Eliminacion de Documento desde la UI

### 1. Overview
Implementar eliminacion logica (soft delete) de documentos desde el backend del servicio document-core. La solucion sigue DDD y arquitectura por capas: dominio para reglas y eventos, aplicacion para orquestacion y validaciones, presentacion para el endpoint REST, e infraestructura para persistencia y auditoria.

### 2. Architecture Context
- **Domain**:
  - Modelo: `Documento` con `fechaEliminacion`.
  - Excepciones: `ResourceNotFoundException`, `AccessDeniedException`, nueva `DocumentAlreadyDeletedException`.
  - Evento: nuevo `DocumentDeletedEvent` para auditoria.
  - Repositorio: `DocumentoRepository` (consultas JPA).
- **Application**:
  - Servicio: `DocumentService` agrega `deleteDocument`.
  - Seguridad: `SecurityContext` para `usuarioId` y `organizacionId`.
  - Permisos: `IEvaluadorPermisos` con `CodigoNivelAcceso.ESCRITURA` (cubre ADMINISTRACION).
- **Presentation**:
  - Controlador: `DocumentoController` agrega `DELETE /api/documentos/{id}` con OpenAPI.
- **Infrastructure**:
  - Persistencia JPA: `DocumentoRepository` con query de soft delete.
  - Manejador de errores: `GlobalExceptionHandler` para mapear 404/403/409 con `ProblemDetail`.

### 3. Implementation Steps

#### Step 0: Create Feature Branch
- **Action**: Crear y cambiar a rama feature antes de cualquier cambio.
- **Branch Naming**: `feature/US-DOC-008-backend` (obligatorio).
- **Implementation Steps**:
  1. Verificar rama base (`main` o `develop`).
  2. `git pull origin [base-branch]`.
  3. `git checkout -b feature/US-DOC-008-backend`.
  4. `git branch` para validar.
- **Notes**: Seguir `ai-specs/specs/backend-standards.md` (Development Workflow).

#### Step 1: Verificar esquema y migraciones
- **File**: `backend/document-core/src/main/resources/db/migration/V1__create_document_tables.sql` (o migraciones posteriores).
- **Action**: Confirmar que `documento.fecha_eliminacion` existe.
- **Implementation Steps**:
  1. Revisar migracion existente.
  2. Si falta la columna, planificar nueva migracion `Vx__add_documento_fecha_eliminacion.sql`.
- **Notes**: No crear migracion si ya existe la columna.

#### Step 2: Extender repositorio para soft delete y lectura con eliminados
- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/repository/DocumentoRepository.java`.
- **Action**: Agregar metodos para:
  - Buscar documento por id y organizacion incluyendo eliminados.
  - Ejecutar soft delete con `fechaEliminacion`.
- **Function Signature**:
  - `Optional<Documento> findByIdAndOrganizacionIdIncludingEliminados(Long id, Long organizacionId);`
  - `int softDeleteByIdAndOrganizacionId(Long id, Long organizacionId, OffsetDateTime fechaEliminacion);`
- **Implementation Steps**:
  1. Agregar query JPA sin filtro `fechaEliminacion` (incluye eliminados).
  2. Agregar query `@Modifying` para actualizar `fechaEliminacion` con condicion `IS NULL`.
  3. Usar `OffsetDateTime` para consistencia con entidad.
- **Dependencies**: `@Modifying`, `@Query`, `@Param`, `OffsetDateTime`.
- **Implementation Notes**:
  - Retorno `0` indica no actualizado (no existe, otra org o ya eliminado).
  - Mantener filtro de tenant con `organizacionId` en el `WHERE`.

#### Step 3: Nueva excepcion para documento ya eliminado
- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/DocumentAlreadyDeletedException.java` (nuevo).
- **Action**: Crear excepcion de dominio para mapear a 409.
- **Function Signature**:
  - `public class DocumentAlreadyDeletedException extends DomainException`.
- **Implementation Steps**:
  1. Definir mensaje claro y `errorCode` (ej. `DOCUMENT_ALREADY_DELETED`).
  2. Usar constructor con mensaje.
- **Dependencies**: `DomainException`.

#### Step 4: Mapear la nueva excepcion en el manejador global
- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/GlobalExceptionHandler.java`.
- **Action**: Agregar handler que devuelva `409 Conflict` en `ProblemDetail`.
- **Function Signature**:
  - `@ExceptionHandler(DocumentAlreadyDeletedException.class)`.
- **Implementation Steps**:
  1. Crear `ProblemDetail` con `HttpStatus.CONFLICT`.
  2. `title`: "Documento Ya Eliminado".
  3. `type`: URL de error coherente con el resto de handlers.
  4. Agregar `timestamp` y `errorCode`.
- **Implementation Notes**:
  - Mantener consistencia con otros handlers que usan RFC 7807.

#### Step 5: Crear evento de auditoria DOCUMENTO_ELIMINADO
- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/event/DocumentDeletedEvent.java` (nuevo).
- **Action**: Evento de dominio similar a `DocumentDownloadedEvent`.
- **Function Signature**:
  - `public class DocumentDeletedEvent extends ApplicationEvent`.
- **Implementation Steps**:
  1. Definir campos: `documentoId`, `usuarioId`, `organizacionId`, `timestamp`.
  2. Validar argumentos no nulos en el constructor.
  3. Implementar `toString` para trazabilidad.
- **Dependencies**: `ApplicationEvent`, `Instant`.
- **Implementation Notes**:
  - No romper descarga si el evento falla (seguir patron usado en `downloadDocument`).

#### Step 6: Implementar deleteDocument en DocumentService
- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/application/service/DocumentService.java`.
- **Action**: Agregar metodo transaccional para soft delete con validaciones.
- **Function Signature**:
  - `public void deleteDocument(Long documentId)`.
- **Implementation Steps**:
  1. Obtener `usuarioId` y `organizacionId` desde `SecurityContext`.
  2. Buscar documento con metodo que incluye eliminados.
     - Si no existe: lanzar `ResourceNotFoundException` (404).
     - Si `fechaEliminacion != null`: lanzar `DocumentAlreadyDeletedException` (409).
  3. Validar permisos con `evaluadorPermisos.tieneAcceso(..., TipoRecurso.DOCUMENTO, CodigoNivelAcceso.ESCRITURA, organizacionId)`.
     - Si false: lanzar `AccessDeniedException` (403).
  4. Ejecutar soft delete usando repositorio (update) o setear `fechaEliminacion` y `save`.
  5. Publicar `DocumentDeletedEvent` con `eventPublisher`.
  6. Loguear resultado en `INFO` y fallos de auditoria en `ERROR` sin romper flujo.
- **Dependencies**: `DocumentDeletedEvent`, `OffsetDateTime`, `SecurityContext`, `IEvaluadorPermisos`, `TipoRecurso`, `CodigoNivelAcceso`.
- **Implementation Notes**:
  - Usar `@Transactional`.
  - No eliminar fisicamente ni versiones.

#### Step 7: Exponer endpoint DELETE /api/documentos/{id}
- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/DocumentoController.java`.
- **Action**: Agregar endpoint REST con OpenAPI y respuesta 204.
- **Function Signature**:
  - `public ResponseEntity<Void> deleteDocument(@PathVariable Long id)`.
- **Implementation Steps**:
  1. Definir `@DeleteMapping("/{id}")`.
  2. Documentar con `@Operation` y `@ApiResponses` (204/401/403/404/409/500) usando `ProblemDetail`.
  3. Llamar `documentService.deleteDocument(id)`.
  4. Retornar `ResponseEntity.noContent().build()`.
- **Implementation Notes**:
  - Mantener consistencia con endpoints existentes en este controlador.
  - Aclarar en descripcion la regla 404 para otra organizacion.

#### Step 8: Pruebas unitarias del servicio
- **File**: `backend/document-core/src/test/java/com/docflow/documentcore/application/service/DocumentServiceTest.java` (crear si no existe, o extender).
- **Action**: Agregar pruebas para delete.
- **Implementation Steps**:
  1. Caso exitoso con permisos validos y documento activo.
  2. Documento no existe -> `ResourceNotFoundException`.
  3. Documento de otra organizacion -> `ResourceNotFoundException`.
  4. Documento ya eliminado -> `DocumentAlreadyDeletedException`.
  5. Sin permisos -> `AccessDeniedException`.
  6. Verificar publicacion de evento `DocumentDeletedEvent`.
- **Dependencies**: JUnit 5, Mockito, AssertJ.
- **Implementation Notes**:
  - Seguir patron `should_DoSomething_When_Condition`.
  - Mock de `SecurityContext` y `IEvaluadorPermisos`.

#### Step 9: Pruebas de integracion del controlador
- **File**: `backend/document-core/src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/DocumentoControllerIT.java` (crear o extender).
- **Action**: Probar `DELETE /api/documentos/{id}`.
- **Implementation Steps**:
  1. `204 No Content` en eliminacion exitosa.
  2. `401` sin autenticacion/headers requeridos (si aplica al entorno de test).
  3. `403` sin permisos.
  4. `404` documento inexistente u otra organizacion.
  5. `409` documento ya eliminado.
- **Implementation Notes**:
  - Usar `@SpringBootTest` + `MockMvc` siguiendo patrones existentes.
  - Validar que `fecha_eliminacion` fue seteada en BD.

#### Step 10: Update Technical Documentation
- **Action**: Actualizar documentacion tecnica segun cambios.
- **Implementation Steps**:
  1. Revisar cambios realizados.
  2. Actualizar `ai-specs/specs/api-spec.yml` con `DELETE /api/documentos/{id}`.
  3. Verificar si `ai-specs/specs/data-model.md` requiere ajuste (solo si hubo cambios de esquema).
  4. Documentar todo en ESPANOL (regla de documentacion del proyecto).
  5. Reportar archivos actualizados.
- **References**: `ai-specs/specs/documentation-standards.md`.
- **Notes**: Paso obligatorio antes de cierre.

### 4. Implementation Order
1. Step 0: Create Feature Branch
2. Step 1: Verificar esquema y migraciones
3. Step 2: Extender repositorio para soft delete y lectura con eliminados
4. Step 3: Nueva excepcion para documento ya eliminado
5. Step 4: Mapear la nueva excepcion en el manejador global
6. Step 5: Crear evento de auditoria DOCUMENTO_ELIMINADO
7. Step 6: Implementar deleteDocument en DocumentService
8. Step 7: Exponer endpoint DELETE /api/documentos/{id}
9. Step 8: Pruebas unitarias del servicio
10. Step 9: Pruebas de integracion del controlador
11. Step 10: Update Technical Documentation

### 5. Testing Checklist
- [ ] Unit tests `DocumentServiceTest` con cobertura >= 90%.
- [ ] Integration tests `DocumentoControllerIT` para 204/401/403/404/409.
- [ ] Validacion de soft delete: `fecha_eliminacion` seteada.
- [ ] Verificacion de emision de `DocumentDeletedEvent`.

### 6. Error Response Format
- **Formato**: RFC 7807 `ProblemDetail` (segun `GlobalExceptionHandler`).
- **Ejemplo 404**:
```json
{
  "type": "https://docflow.com/errors/resource-not-found",
  "title": "Recurso No Encontrado",
  "status": 404,
  "detail": "Documento no encontrado con id: 123",
  "timestamp": "2026-02-16T12:00:00Z"
}
```
- **Mapeo**:
  - 401: contexto de autenticacion ausente (si aplica).
  - 403: `AccessDeniedException`.
  - 404: `ResourceNotFoundException` (incluye tenant isolation).
  - 409: `DocumentAlreadyDeletedException`.
  - 500: errores inesperados.

### 7. Partial Update Support
No aplica (operacion DELETE).

### 8. Dependencies
- Sin nuevas dependencias externas.
- Reutiliza `SecurityContext`, `IEvaluadorPermisos`, `ApplicationEventPublisher`.

### 9. Notes
- Respetar aislamiento de tenant: 404 si el documento pertenece a otra organizacion.
- Permisos requeridos: ESCRITURA o ADMINISTRACION (usar `CodigoNivelAcceso.ESCRITURA`).
- No eliminar fisicamente registros ni versiones.
- Mantener logs en niveles adecuados y sin datos sensibles.
- Documentacion y comentarios en ESPANOL.

### 10. Next Steps After Implementation
1. Ejecutar suite de pruebas del modulo `document-core`.
2. Validar manualmente via Swagger o Postman.
3. Revisar si el frontend requiere ajustes adicionales (coordinacion).

### 11. Implementation Verification
- [ ] Calidad de codigo (convenciones, inyeccion por constructor).
- [ ] Funcionalidad: soft delete, permisos, tenant isolation, auditoria.
- [ ] Pruebas pasan y cobertura >= 90%.
- [ ] Integracion correcta con `GlobalExceptionHandler`.
- [ ] Documentacion actualizada en ESPANOL.
