# Backend Implementation Plan: US-FOLDER-004 Eliminar carpeta vacía (soft delete)

## Overview

Implementación del endpoint DELETE para eliminación lógica de carpetas vacías en DocFlow. Este feature permite a administradores eliminar carpetas sin contenido activo, manteniendo trazabilidad mediante soft delete. La eliminación respeta las reglas de negocio: solo carpetas vacías, permiso de ADMINISTRACIÓN requerido, y se preserva auditoría.

**Principios Arquitectónicos:**
- Domain-Driven Design (DDD): Lógica de validación en dominio
- Hexagonal Architecture: Separación clara entre capas
- Clean Code: Responsabilidad única, validaciones escalables
- SOLID Principles: Single Responsibility, Open/Closed, Interface Segregation

---

## Architecture Context

### Layers Involved

1. **Domain Layer** (`src/main/java/.../domain/`)
   - Exception: `CarpetaNoVaciaException` (nueva)
   - Service: validaciones de negocio en dominio

2. **Application Layer** (`src/main/java/.../application/`)
   - Service: `CarpetaService.eliminarCarpeta()` (método orquestador)
   - Validator: `CarpetaValidator.validarCarpetaVacia()` (validación centralizada)

3. **Infrastructure Layer** (`src/main/java/.../infrastructure/`)
   - Repository: `ICarpetaRepository.estaVacia()` y `eliminarLogicamente()` (métodos de persistencia)
   - Controller: `CarpetaController.deleteCarpeta()` (endpoint REST)
   - Error Handler: mapeo de excepciones a códigos HTTP

4. **Database Layer**
   - Índices: creación de índices para `EXISTS` queries eficientes
   - Query optimization: evitar cargas completas de datos

### Key Files & Structure

```
backend/document-core/
├── src/main/java/com/docflow/documentcore/
│   ├── domain/
│   │   ├── exception/carpeta/
│   │   │   ├── CarpetaNoVaciaException.java         # Nueva excepción
│   │   │   ├── CarpetaNotFoundException.java        # Existente
│   │   │   └── SinPermisoCarpetaException.java      # Existente
│   │   │
│   │   ├── repository/
│   │   │   └── ICarpetaRepository.java              # Extender con nuevos métodos
│   │   │
│   │   └── service/
│   │       └── CarpetaService.java                  # Existente, agregar lógica
│   │
│   ├── application/
│   │   ├── service/
│   │   │   ├── CarpetaService.java                  # Extender con eliminarCarpeta()
│   │   │   └── DocumentoService.java                # Revisar (para verificar documentos)
│   │   │
│   │   └── validator/
│   │       └── CarpetaValidator.java                # Extender con validarCarpetaVacia()
│   │
│   └── infrastructure/
│       ├── adapter/
│       │   ├── persistence/
│       │   │   ├── repository/CarpetaRepositoryAdapter.java    # Extender
│       │   │   └── entity/CarpetaEntity.java                   # Revisar campos
│       │   │
│       │   ├── controller/
│       │   │   └── CarpetaController.java                      # Extender DELETE
│       │   │
│       │   └── error/
│       │       ├── GlobalExceptionHandler.java                 # Extender mapping
│       │       └── ErrorCatalog.java                           # Agregar CARPETA_NO_VACIA
│       │
│       └── persistence/
│           └── repository/
│               ├── ICarpetaJpaRepository.java                  # Extender queries
│               └── IDocumentoJpaRepository.java                # Revisar queries
│
├── src/main/resources/db/migration/
│   ├── V00X__Create_Indexes_Carpeta_Documento.sql             # Nueva migración
│   └── (existentes)
│
└── src/test/java/com/docflow/documentcore/
    ├── domain/model/CarpetaTest.java                # Revisar tests
    │
    ├── application/service/
    │   └── CarpetaServiceTest.java                  # Extender tests
    │
    └── infrastructure/adapter/
        ├── repository/CarpetaRepositoryTest.java    # Tests de repository
        └── controller/CarpetaControllerTest.java    # Tests de endpoint
```

---

## Implementation Steps

### Step 1: Create Exception for Non-Empty Folder

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/carpeta/CarpetaNoVaciaException.java`

**Action**: Crear excepción de dominio para cuando carpeta no está vacía

**Implementation Steps**:

1. **Create Exception Class**:
   - Extender `RuntimeException`
   - Adicionar constructor con parámetro `carpetaId`
   - Adicionar constructores para capturar conteos (opcional, para detalles)
   - Campos: `carpetaId`, `subcarpetasActivas`, `documentosActivos`

2. **Implementation Details**:
   ```java
   public class CarpetaNoVaciaException extends RuntimeException {
       private final Long carpetaId;
       private final int subcarpetasActivas;
       private final int documentosActivos;
       
       public CarpetaNoVaciaException(Long carpetaId, int subcarpetasActivas, int documentosActivos) {
           super(String.format("Carpeta %d no está vacía: %d subcarpetas, %d documentos activos",
                   carpetaId, subcarpetasActivas, documentosActivos));
           this.carpetaId = carpetaId;
           this.subcarpetasActivas = subcarpetasActivas;
           this.documentosActivos = documentosActivos;
       }
       
       // Getters para carpetaId, subcarpetasActivas, documentosActivos
   }
   ```

**Dependencies**: 
- `java.lang.RuntimeException`

**Implementation Notes**:
- Mantener consistencia con excepciones existentes del proyecto
- No capturar stack trace completo (performance)

---

### Step 2: Create Exception for Root Folder Deletion

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/carpeta/CarpetaRaizNoEliminableException.java`

**Action**: Crear excepción para intento de eliminar carpeta raíz

**Implementation Steps**:

1. **Create Exception Class**:
   - Extender `RuntimeException`
   - Parámetro: `carpetaId`
   - Mensaje: "No se puede eliminar una carpeta raíz"

**Dependencies**: 
- `java.lang.RuntimeException`

---

### Step 3: Add Validation Method to CarpetaValidator

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/application/validator/CarpetaValidator.java`

**Action**: Extender validador con método para verificar si carpeta está vacía

**Function Signature**:
```java
public void validarCarpetaVacia(Long carpetaId, Long organizacionId) 
    throws CarpetaNoVaciaException
```

**Implementation Steps**:

1. **Call Repository Method**:
   - Invoca `ICarpetaRepository.estaVacia(carpetaId, organizacionId)`
   - Si retorna `false`, obtener conteos específicos

2. **Get Detailed Counts** (para error response):
   - `int subcarpetasActivas = carpetaRepository.contarSubcarpetasActivas(carpetaId, organizacionId)`
   - `int documentosActivos = carpetaRepository.contarDocumentosActivos(carpetaId, organizacionId)`

3. **Throw Exception**:
   - Si no está vacía: `throw new CarpetaNoVaciaException(carpetaId, subcarpetasActivas, documentosActivos)`

**Dependencies**: 
- `ICarpetaRepository`
- `CarpetaNoVaciaException`

**Implementation Notes**:
- Evitar llamadas múltiples al repositorio; optimizar queries
- Usar transacciones read-only para validación

---

### Step 4: Add Validation Method for Root Folder

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/application/validator/CarpetaValidator.java`

**Action**: Agregar validación para detectar intento de eliminar carpeta raíz

**Function Signature**:
```java
public void validarNoEsRaiz(Long carpetaId, Long organizacionId) 
    throws CarpetaRaizNoEliminableException
```

**Implementation Steps**:

1. **Retrieve Carpeta**:
   - `Carpeta carpeta = carpetaRepository.obtenerPorId(carpetaId, organizacionId)`
   - Si no existe, lanzar `CarpetaNotFoundException`

2. **Check if Root**:
   - Si `carpeta.getCarpetaPadreId() == null`: es raíz
   - Lanzar `CarpetaRaizNoEliminableException(carpetaId)`

**Dependencies**: 
- `ICarpetaRepository`
- `CarpetaRaizNoEliminableException`
- `Carpeta`

---

### Step 5: Add Repository Methods for Verification

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/repository/ICarpetaRepository.java`

**Action**: Extender interfaz repository con métodos para verificar si carpeta está vacía

**Function Signatures**:
```java
boolean estaVacia(Long carpetaId, Long organizacionId);

int contarSubcarpetasActivas(Long carpetaId, Long organizacionId);

int contarDocumentosActivos(Long carpetaId, Long organizacionId);
```

**Implementation Steps** (en interfaz):

1. **estaVacia()**:
   - Retorna `true` si no hay subcarpetas activas Y no hay documentos activos
   - Utiliza `EXISTS` para eficiencia
   - Lógica: `!existenSubcarpetasActivas && !existenDocumentosActivos`

2. **contarSubcarpetasActivas()**:
   - Cuenta subcarpetas donde `carpeta_padre_id = :carpetaId AND fecha_eliminacion IS NULL`

3. **contarDocumentosActivos()**:
   - Cuenta documentos donde `carpeta_id = :carpetaId AND fecha_eliminacion IS NULL`

**Dependencies**: 
- Long, organizacionId filtering

**Implementation Notes**:
- Métodos de lectura: usar `@Transactional(readOnly = true)`
- Usar `EXISTS` en lugar de COUNT para mejor performance
- Aplicar multi-tenancy (filtrar por `organizacionId`)

---

### Step 6: Implement Repository Methods (JPA Adapter)

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/repository/adapter/CarpetaRepositoryAdapter.java`

**Action**: Implementar métodos de verificación de vacío en adapter JPA

**Implementation Steps**:

1. **Implement estaVacia()**:
   - Query: verificar que no existe subcarpeta activa
   - Query: verificar que no existe documento activo
   - Combinar con AND lógico

2. **Implement contarSubcarpetasActivas()**:
   - Query: `SELECT COUNT(*) FROM carpeta WHERE carpeta_padre_id = ? AND fecha_eliminacion IS NULL AND organizacion_id = ?`
   - Retornar count

3. **Implement contarDocumentosActivos()**:
   - Usar `IDocumentoJpaRepository` o repositorio de documentos
   - Query: `SELECT COUNT(*) FROM documento WHERE carpeta_id = ? AND fecha_eliminacion IS NULL AND organizacion_id = ?`

**Dependencies**: 
- `ICarpetaJpaRepository`
- `IDocumentoJpaRepository` (inyectar si es necesario)

**Implementation Notes**:
- JPQL o native queries según convención del proyecto
- Índices recomendados:
  - `carpeta(carpeta_padre_id, fecha_eliminacion, organizacion_id)`
  - `documento(carpeta_id, fecha_eliminacion, organizacion_id)`

---

### Step 7: Add Soft Delete Method to Repository

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/repository/ICarpetaRepository.java`

**Action**: Asegurar que existe método `eliminarLogicamente()` en interfaz (probablemente ya existe)

**Function Signature**:
```java
void eliminarLogicamente(Long carpetaId, Long organizacionId);
```

**Implementation Steps**:

1. **Update (if not exists)**:
   - Método debe establecer `fecha_eliminacion = CURRENT_TIMESTAMP`
   - Filtrar por `organizacionId` para multi-tenancy
   - Lanzar `CarpetaNotFoundException` si no existe

**Notes**:
- Si ya existe `eliminarLogicamente()`, revisar que está correctamente implementado
- Verificar que retorna `void` (no retorna boolean para mantener consistencia)

---

### Step 8: Add Delete Service Method

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/application/service/CarpetaService.java`

**Action**: Crear método orquestador para eliminar carpeta con validaciones

**Function Signature**:
```java
@Transactional
public void eliminarCarpeta(Long carpetaId, Long usuarioId, Long organizacionId) 
    throws CarpetaNotFoundException, CarpetaNoVaciaException, CarpetaRaizNoEliminableException, SinPermisoCarpetaException
```

**Implementation Steps**:

1. **Log Inicio**:
   - `logger.info("Iniciando eliminación de carpeta {} en organización {} por usuario {}", ...)`

2. **Validar Existencia**:
   - Llamar `validator.validarCarpetaExiste(carpetaId, organizacionId)`
   - Si no existe: lanza `CarpetaNotFoundException`

3. **Validar No Es Raíz**:
   - Llamar `validator.validarNoEsRaiz(carpetaId, organizacionId)`
   - Si es raíz: lanza `CarpetaRaizNoEliminableException`

4. **Validar Permisos ADMINISTRACIÓN**:
   - Llamar `validator.validarPermisos(usuarioId, carpetaId, organizacionId, NivelAcceso.ADMINISTRACION)`
   - Si sin permisos: lanza `SinPermisoCarpetaException`

5. **Validar Carpeta Vacía**:
   - Llamar `validator.validarCarpetaVacia(carpetaId, organizacionId)`
   - Si no vacía: lanza `CarpetaNoVaciaException`

6. **Ejecutar Soft Delete**:
   - Llamar `carpetaRepository.eliminarLogicamente(carpetaId, organizacionId)`

7. **Emit Domain Event** (si aplica auditoría):
   - `eventPublisher.publishEvent(new CarpetaEliminadaEvent(carpetaId, usuarioId, organizacionId, Instant.now()))`

8. **Log Éxito**:
   - `logger.info("Carpeta {} eliminada exitosamente por usuario {}", carpetaId, usuarioId)`

**Dependencies**: 
- `ICarpetaRepository`
- `CarpetaValidator`
- `ApplicationEventPublisher`
- `Logger`

**Implementation Notes**:
- Orden de validaciones: existencia → raíz → permisos → vacío
- Transacción debe ser propagada automáticamente
- Excepciones se propagan al controlador para mapeo HTTP

---

### Step 9: Add Controller Endpoint

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/CarpetaController.java`

**Action**: Exponer endpoint DELETE /api/carpetas/{id}

**Function Signature**:
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteCarpeta(
    @PathVariable Long id,
    @RequestHeader("X-User-Id") Long usuarioId,
    @RequestHeader("X-Organization-Id") Long organizacionId
) throws CarpetaNotFoundException, CarpetaNoVaciaException, CarpetaRaizNoEliminableException, SinPermisoCarpetaException
```

**Implementation Steps**:

1. **Log Request**:
   - `logger.debug("DELETE /api/carpetas/{} - Usuario: {}, Org: {}", id, usuarioId, organizacionId)`

2. **Call Service**:
   - `carpetaService.eliminarCarpeta(id, usuarioId, organizacionId)`

3. **Return Response**:
   - `return ResponseEntity.noContent().build()` (HTTP 204)

4. **Exception Handling** (delegado a global handler):
   - Controller no captura excepciones
   - Las propaga al `GlobalExceptionHandler`

**Swagger Documentation**:
```java
@DeleteMapping("/{id}")
@Operation(
    summary = "Eliminar carpeta vacía",
    description = "Realiza una eliminación lógica de una carpeta vacía. Requiere permisos de ADMINISTRACIÓN y carpeta sin subcarpetas ni documentos activos."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "204",
        description = "Carpeta eliminada exitosamente"
    ),
    @ApiResponse(responseCode = "400", description = "No se puede eliminar carpeta raíz"),
    @ApiResponse(responseCode = "403", description = "Sin permisos ADMINISTRACIÓN"),
    @ApiResponse(responseCode = "404", description = "Carpeta no encontrada o no pertenece a la organización"),
    @ApiResponse(responseCode = "409", description = "Carpeta contiene subcarpetas o documentos activos")
})
@SecurityRequirement(name = "bearer-jwt")
```

**Dependencies**: 
- `CarpetaService`
- `ResponseEntity`
- `RequestHeader`
- `PathVariable`

**Implementation Notes**:
- Path parameter `id` es Long (UUID si el proyecto lo requiere)
- Headers `X-User-Id` y `X-Organization-Id` son requeridos (validar en interceptor)
- Retorno 204 No Content (sin body)

---

### Step 10: Add Global Exception Handler Mappings

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/error/GlobalExceptionHandler.java`

**Action**: Adicionar handlers para nuevas excepciones

**Implementation Steps**:

1. **Add Handler for CarpetaNoVaciaException**:
   ```java
   @ExceptionHandler(CarpetaNoVaciaException.class)
   public ResponseEntity<ErrorResponse> handleCarpetaNoVacia(
       CarpetaNoVaciaException ex,
       HttpServletRequest request
   ) {
       return ResponseEntity
           .status(HttpStatus.CONFLICT)
           .body(new ErrorResponse(
               "CARPETA_NO_VACIA",
               "La carpeta debe vaciarse antes de eliminarla",
               Map.of(
                   "carpetaId", ex.getCarpetaId().toString(),
                   "subcarpetasActivas", String.valueOf(ex.getSubcarpetasActivas()),
                   "documentosActivos", String.valueOf(ex.getDocumentosActivos())
               ),
               request.getRequestURI()
           ));
   }
   ```

2. **Add Handler for CarpetaRaizNoEliminableException**:
   ```java
   @ExceptionHandler(CarpetaRaizNoEliminableException.class)
   public ResponseEntity<ErrorResponse> handleCarpetaRaizNoEliminable(
       CarpetaRaizNoEliminableException ex,
       HttpServletRequest request
   ) {
       return ResponseEntity
           .status(HttpStatus.BAD_REQUEST)
           .body(new ErrorResponse(
               "CARPETA_RAIZ_NO_ELIMINABLE",
               "No se puede eliminar una carpeta raíz",
               Map.of("carpetaId", ex.getCarpetaId().toString()),
               request.getRequestURI()
           ));
   }
   ```

3. **Ensure Existing Handlers**:
   - Verificar que `CarpetaNotFoundException` mapea a HTTP 404
   - Verificar que `SinPermisoCarpetaException` mapea a HTTP 403

**Dependencies**: 
- `ExceptionHandler`
- `HttpStatus`
- `ResponseEntity`

**Implementation Notes**:
- ErrorResponse debe ser DTO estándar del proyecto
- Incluir timestamp, path, requestId en respuesta
- No exponer stack traces en producción

---

### Step 11: Add Database Migration for Indices

**File**: `backend/document-core/src/main/resources/db/migration/V00X__Add_Indices_For_Folder_Deletion.sql`

**Action**: Crear índices para queries de verificación eficientes

**Implementation Steps**:

1. **Create Index for Subcarpetas Check**:
   ```sql
   CREATE INDEX IF NOT EXISTS idx_carpeta_padre_eliminacion
   ON carpeta(carpeta_padre_id, fecha_eliminacion, organizacion_id)
   WHERE fecha_eliminacion IS NULL;
   ```

2. **Create Index for Documentos Check**:
   ```sql
   CREATE INDEX IF NOT EXISTS idx_documento_carpeta_eliminacion
   ON documento(carpeta_id, fecha_eliminacion, organizacion_id)
   WHERE fecha_eliminacion IS NULL;
   ```

3. **Idempotent Script**:
   - Usar `IF NOT EXISTS` para evitar errores en re-ejecuciones
   - Incluir comentarios explicativos

**SQL Considerations**:
- Partial indices mejorar performance (solo registros activos)
- Multi-column indices optimizan queries con múltiples condiciones
- Orden de columnas: carpeta_padre_id/carpeta_id → fecha_eliminacion → organizacion_id

---

### Step 12: Write Unit Tests for Validator

**File**: `backend/document-core/src/test/java/com/docflow/documentcore/application/validator/CarpetaValidatorTest.java`

**Action**: Crear tests unitarios para métodos de validación

**Test Cases**:

1. **Test validarCarpetaVacia() - Successful Cases**:
   - `should_Pass_When_CarpetaIsEmpty`: carpeta sin subcarpetas ni documentos
   - Mock: `estaVacia()` retorna true

2. **Test validarCarpetaVacia() - Validation Errors**:
   - `should_Throw_CarpetaNoVacia_When_HasSubcarpetas`: carpeta con subcarpetas
   - `should_Throw_CarpetaNoVacia_When_HasDocumentos`: carpeta con documentos
   - `should_Throw_CarpetaNoVacia_When_HasBoth`: carpeta con ambos
   - Mock: `estaVacia()` retorna false, obtener conteos

3. **Test validarNoEsRaiz() - Successful Cases**:
   - `should_Pass_When_NotRootFolder`: `carpetaPadreId != null`

4. **Test validarNoEsRaiz() - Validation Errors**:
   - `should_Throw_CarpetaRaizNoEliminable_When_IsRootFolder`: `carpetaPadreId == null`
   - `should_Throw_CarpetaNotFound_When_CarpetaDoesNotExist`

**Implementation Notes**:
- Usar Mockito para mock de repositorio
- AssertJ para assertions fluidas
- Naming: `should_[action]_When_[condition]`

---

### Step 13: Write Integration Tests for Repository

**File**: `backend/document-core/src/test/java/com/docflow/documentcore/infrastructure/adapter/repository/CarpetaRepositoryTest.java`

**Action**: Crear tests de integración para métodos del repositorio

**Test Cases**:

1. **Test estaVacia()**:
   - `should_Return_True_When_CarpetaIsEmpty`: carpeta vacía → true
   - `should_Return_False_When_HasSubcarpetas`: con subcarpetas → false
   - `should_Return_False_When_HasDocumentos`: con documentos → false

2. **Test contarSubcarpetasActivas()**:
   - `should_Count_OnlyActiveSub carpetas`: cuenta solo con `fecha_eliminacion IS NULL`
   - `should_Return_Zero_When_NoSubcarpetas`: sin subcarpetas → 0

3. **Test contarDocumentosActivos()**:
   - `should_Count_OnlyActiveDocumentos`: cuenta solo con `fecha_eliminacion IS NULL`
   - `should_Return_Zero_When_NoDocumentos`: sin documentos → 0

4. **Test eliminarLogicamente()**:
   - `should_Set_FechaEliminacion_When_Successful`: `fecha_eliminacion` seteada
   - `should_Throw_CarpetaNotFound_When_DoesNotExist`

**Implementation Notes**:
- Usar `@DataJpaTest` con testcontainers o H2 en memory
- Setup: crear carpetas, subcarpetas, documentos en arrange
- Multi-tenancy: verificar filtrado por `organizacionId`

---

### Step 14: Write Controller Tests

**File**: `backend/document-core/src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/CarpetaControllerTest.java`

**Action**: Crear tests para endpoint DELETE

**Test Cases**:

1. **Test DELETE - Successful Cases**:
   - `should_Return_204_When_EmptyFolderDeleted`: respuesta 204 No Content
   - Mock: `eliminCarpeta()` exitoso

2. **Test DELETE - Validation Errors**:
   - `should_Return_400_When_RootFolderDeletion`: carpeta raíz → 400
   - `should_Return_403_When_NoAdminPermission`: sin permisos → 403
   - `should_Return_404_When_CarpetaNotFound`: no existe → 404
   - `should_Return_409_When_CarpetaNotEmpty`: no vacía → 409

3. **Test DELETE - Missing Headers**:
   - `should_Return_400_When_MissingUserIdHeader`
   - `should_Return_400_When_MissingOrganizationIdHeader`

**Implementation Notes**:
- Usar `@WebMvcTest(CarpetaController.class)`
- Mock: `CarpetaService`
- Assert: status HTTP, body (si aplica)

---

### Step 15: Add End-to-End Integration Test

**File**: `backend/document-core/src/test/java/com/docflow/documentcore/integration/CarpetaDeleteIntegrationTest.java`

**Action**: Test de integración completo para flujo de eliminación

**Test Cases**:

1. **Complete Flow - Successful Deletion**:
   - Setup: crear organización, usuario con permisos, carpeta vacía
   - Action: DELETE /api/carpetas/{id}
   - Verify: HTTP 204, carpeta tiene `fecha_eliminacion`

2. **Complete Flow - Non-Empty Carpeta**:
   - Setup: crear carpeta con subcarpeta
   - Action: DELETE /api/carpetas/{id}
   - Verify: HTTP 409, carpeta sigue activa

3. **Complete Flow - Root Folder**:
   - Setup: obtener ID de carpeta raíz
   - Action: DELETE /api/carpetas/{rootId}
   - Verify: HTTP 400, carpeta sigue activa

4. **Complete Flow - Permission Denied**:
   - Setup: usuario sin permisos ADMINISTRACIÓN
   - Action: DELETE /api/carpetas/{id}
   - Verify: HTTP 403

**Implementation Notes**:
- Usar `@SpringBootTest` con testcontainers para BD real
- Setup con `@BeforeEach` o fixtures
- Verificar estado final de BD

---

### Step 16: Update OpenAPI/Swagger Documentation

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/CarpetaController.java`

**Action**: Asegurar documentación completa del endpoint DELETE

**Implementation Steps**:

1. **Add @Operation Annotation**:
   - Summary: "Eliminar carpeta vacía"
   - Description: "Realiza eliminación lógica de carpeta vacía con validación de permisos"

2. **Add @ApiResponses**:
   - 204 No Content: Carpeta eliminada
   - 400 Bad Request: Carpeta raíz / Validación fallida
   - 403 Forbidden: Sin permisos ADMINISTRACIÓN
   - 404 Not Found: Carpeta no existe
   - 409 Conflict: Carpeta no vacía

3. **Add @SecurityRequirement**:
   - `name = "bearer-jwt"`: autenticación requerida

4. **Add @Parameter Annotations**:
   - `id`: descripción, ejemplo
   - Headers: descripción

**Swagger Schema**:
```yaml
DELETE /api/carpetas/{id}:
  parameters:
    - name: id
      in: path
      required: true
      schema:
        type: integer
        format: int64
      description: ID de la carpeta a eliminar
  responses:
    204:
      description: Carpeta eliminada exitosamente
    400:
      description: No se puede eliminar carpeta raíz
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    403:
      description: Sin permisos ADMINISTRACIÓN
    404:
      description: Carpeta no encontrada
    409:
      description: Carpeta contiene contenido activo
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
            properties:
              code:
                example: "CARPETA_NO_VACIA"
              details:
                properties:
                  subcarpetasActivas:
                    type: integer
                  documentosActivos:
                    type: integer
```

---

### Step 17: Update Technical Documentation

**File**: `ai-specs/specs/data-model.md` y `ai-specs/specs/api-spec.yml`

**Action**: Actualizar documentación con cambios de modelo y API

**Implementation Steps**:

1. **Update data-model.md**:
   - Sección Folder: documentar comportamiento de soft delete
   - Agregar detalles de índices creados
   - Documentar validaciones de eliminación

2. **Update api-spec.yml**:
   - Agregar endpoint DELETE /api/carpetas/{id}
   - Documentar request/response
   - Incluir error responses detalladas

3. **Update README.md** (si aplica):
   - Sección "Folder Deletion": descripción de feature
   - Ejemplos de uso cURL/HTTP

**References**: 
- Seguir estructura de [documentation-standards.mdc](../../ai-specs/specs/documentation-standards.mdc)
- Mantener contenido en inglés
- Incluir ejemplos de request/response

---

## Implementation Order

* Step 1: Create CarpetaNoVaciaException
* Step 2: Create CarpetaRaizNoEliminableException
* Step 3: Add validarCarpetaVacia() to CarpetaValidator
* Step 4: Add validarNoEsRaiz() to CarpetaValidator
* Step 5: Add repository methods to ICarpetaRepository (interface)
* Step 6: Implement repository methods in CarpetaRepositoryAdapter
* Step 7: Review/add eliminarLogicamente() to repository
* Step 8: Add eliminarCarpeta() to CarpetaService
* Step 9: Add DELETE endpoint to CarpetaController
* Step 10: Add exception handlers to GlobalExceptionHandler
* Step 11: Create DB migration for indices
* Step 12: Write unit tests for CarpetaValidator
* Step 13: Write integration tests for repository
* Step 14: Write controller tests
* Step 15: Write E2E integration tests
* Step 16: Update Swagger documentation
* Step 17: Update technical documentation (data model, API spec, README)

---

## Testing Checklist

### Unit Tests
- [ ] CarpetaValidator: validarCarpetaVacia() - todos los casos
- [ ] CarpetaValidator: validarNoEsRaiz() - todos los casos
- [ ] CarpetaService: eliminarCarpeta() - orquestación correcta

### Integration Tests
- [ ] CarpetaRepository: estaVacia() con datos reales
- [ ] CarpetaRepository: contarSubcarpetasActivas() con datos reales
- [ ] CarpetaRepository: contarDocumentosActivos() con datos reales
- [ ] CarpetaRepository: eliminarLogicamente() - soft delete ejecutado

### Controller Tests (Mock)
- [ ] DELETE /api/carpetas/{id} - 204 exitoso
- [ ] DELETE /api/carpetas/{id} - 400 carpeta raíz
- [ ] DELETE /api/carpetas/{id} - 403 sin permisos
- [ ] DELETE /api/carpetas/{id} - 404 no existe
- [ ] DELETE /api/carpetas/{id} - 409 no vacía

### Manual Testing
- [ ] GET /api/carpetas/{id}/contenido no lista carpeta eliminada
- [ ] Carpeta eliminada no aparece en listados normales
- [ ] Historial de auditoría registra eliminación
- [ ] Permisos verificados correctamente

---

## Error Response Format

```json
{
  "code": "CARPETA_NO_VACIA",
  "message": "La carpeta debe vaciarse antes de eliminarla",
  "timestamp": "2026-02-04T10:30:00Z",
  "path": "/api/carpetas/123",
  "details": {
    "carpetaId": "123",
    "subcarpetasActivas": 2,
    "documentosActivos": 5
  }
}
```

```json
{
  "code": "CARPETA_RAIZ_NO_ELIMINABLE",
  "message": "No se puede eliminar una carpeta raíz",
  "timestamp": "2026-02-04T10:30:00Z",
  "path": "/api/carpetas/1",
  "details": {
    "carpetaId": "1"
  }
}
```

---

## Dependencies

### External Libraries
- **Spring Framework 6.x**: @Transactional, @Service
- **Spring Data JPA**: Repository pattern
- **Lombok**: @Slf4j, @RequiredArgsConstructor (si aplica)
- **MapStruct**: DTO mapping (si aplica)
- **JUnit 5**: Testing framework
- **Mockito**: Mocking en tests
- **AssertJ**: Fluent assertions

### Project-Internal Dependencies
- `CarpetaService`: orquestador
- `ICarpetaRepository`: acceso a datos
- `CarpetaValidator`: validación centralizada
- `CarpetaController`: exposición REST
- `GlobalExceptionHandler`: mapeo de errores
- `CarpetaEntity`: JPA entity

---

## Notes

### Important Reminders

1. **Security & Multi-Tenancy**:
   - Todas las queries DEBEN filtrar por `organizacionId`
   - Validar permisos ADMINISTRACIÓN antes de permitir eliminación
   - No exponer información de otras organizaciones en errores

2. **Business Rules**:
   - Carpeta raíz (`carpetaPadreId IS NULL`) es inmutable
   - Soft delete: solo marcar `fecha_eliminacion`, no borrar físicamente
   - Carpeta vacía: 0 subcarpetas activas Y 0 documentos activos
   - Usar `EXISTS` en lugar de `COUNT` para mejor performance

3. **Performance**:
   - Crear índices partial para queries activas
   - Usar transacciones read-only donde sea posible
   - Evitar N+1 queries

4. **Transactions & Events**:
   - `@Transactional` en nivel de servicio
   - Emitir eventos de dominio para auditoría
   - Async event handling opcional (si infraestructura lo permite)

5. **Error Handling**:
   - Lanzar excepciones específicas de dominio
   - GlobalExceptionHandler mapea a HTTP codes
   - No exponer stack traces en producción

6. **Testing Standards**:
   - TDD: tests ANTES que implementación (cuando sea posible)
   - Coverage: >= 80% en líneas de código críticas
   - Naming: `should_[action]_When_[condition]`

7. **Documentation Standards**:
   - Javadoc en clases públicas
   - Comentarios en lógica compleja
   - OpenAPI/Swagger annotations en controllers
   - README con ejemplos de uso

### Constraints

- **Java Version**: 21 (LTS)
- **Spring Boot**: 3.5.x
- **PostgreSQL**: Compatible con sintaxis
- **API Contract**: Debe mantener compatibilidad hacia atrás (si aplica)
- **Soft Delete**: NO usar hard delete físico

---

## Implementation Verification

### Code Quality
- [ ] Todos los métodos tienen Javadoc
- [ ] No hay code smells detectados por SonarQube/PMD
- [ ] Naming sigue convenciones del proyecto
- [ ] No hay hardcoded values o magic numbers

### Functionality
- [ ] Endpoint retorna 204 para eliminación exitosa
- [ ] Validaciones funcionan correctamente
- [ ] Permisos se validan antes de eliminar
- [ ] Soft delete marca fecha_eliminacion

### Testing
- [ ] Coverage >= 80% en archivos modificados
- [ ] Todos los tests pasan (mvn test)
- [ ] No hay warnings o errores de compilación
- [ ] Tests de integración usan BD real (H2/testcontainers)

### Integration
- [ ] Nueva excepción manejada en GlobalExceptionHandler
- [ ] Swagger documenta correctamente el endpoint
- [ ] Indices creados y utilizados en queries
- [ ] Multi-tenancy validado con múltiples organizaciones

### Documentation
- [ ] README actualizado con ejemplo de DELETE
- [ ] Error codes documentados en error-codes.md
- [ ] API spec (OpenAPI) actualizado
- [ ] Data model describes soft delete behavior
