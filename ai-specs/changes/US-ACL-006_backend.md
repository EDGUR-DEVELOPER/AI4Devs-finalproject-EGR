# Backend Implementation Plan: US-ACL-006 - Permission Precedence Rule (Document > Folder)

## Overview

This user story implements a centralized permission evaluation service that applies the precedence rule: **Document Permission > Folder Permission (direct or inherited)**. This service will resolve permission conflicts automatically, providing a single source of truth for permission evaluation across the entire system.

The implementation follows Domain-Driven Design (DDD) principles with a clear separation between domain logic (permission evaluation algorithm), application services (orchestration), and infrastructure (guards/interceptors). The architecture follows the Hexagonal/Ports and Adapters pattern already established in the document-core service.

## Architecture Context

### Layers Involved

**Domain Layer** (`domain/`)
- New domain service interface: `IEvaluadorPermisos` (port)
- New domain models: `OrigenPermiso`, `TipoRecurso` enums
- Modified domain model: `PermisoEfectivo` (extend to support document permissions)
- Existing repositories: `IPermisoDocumentoUsuarioRepository`, `IPermisoCarpetaUsuarioRepository`

**Application Layer** (`application/`)
- New service: `EvaluadorPermisosService` (implements `IEvaluadorPermisos`)
- Existing services: `PermisoHerenciaService`, `CarpetaService` (dependencies)

**Infrastructure Layer** (`infrastructure/`)
- New security components: `RequierePermisoGuard` (AOP aspect), `RequierePermiso` annotation
- Existing services: `CurrentTenantService`, `CurrentUserService`

**Presentation Layer** (`presentation/`)
- Modified controllers to use `@RequierePermiso` annotation
- New endpoint: `/api/documentos/{documentoId}/mi-permiso` for querying effective permissions

### Components Referenced

- `PermisoHerenciaService`: Existing service for folder inheritance logic (will be used as dependency)
- `CarpetaService`: Service to retrieve folder information
- `PermisoCarpetaUsuario`: Domain entity for folder permissions
- `PermisoDocumentoUsuario`: Domain entity for document permissions
- `NivelAcceso`: Existing enum (LECTURA, ESCRITURA, ADMINISTRACION)
- `PermisoEfectivo`: Existing domain model (needs extension)

## Implementation Steps

### Step 0: Create Feature Branch

**Action**: Create and switch to a new feature branch following the development workflow.

**Branch Naming**: `feature/US-ACL-006-backend`

**Implementation Steps**:
1. Check if branch exists: `git branch --list feature/US-ACL-006-backend`
2. If branch doesn't exist:
   - Ensure on latest main/develop: `git checkout develop && git pull origin develop`
   - Create new branch: `git checkout -b feature/US-ACL-006-backend`
3. If branch exists:
   - Switch to it: `git checkout feature/US-ACL-006-backend`
   - Pull latest changes: `git pull origin feature/US-ACL-006-backend`
4. Verify branch: `git branch --show-current`

**Notes**: This MUST be the FIRST step before any code changes. The branch name follows the project's convention of `feature/[ticket-id]-backend` to separate backend and frontend concerns.

---

### Step 1: Create Domain Enums for Permission Origin and Resource Type

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/model/OrigenPermiso.java`

**Action**: Create enum to represent the origin of an effective permission

**Implementation Steps**:
1. Create package `com.docflow.documentcore.domain.model` (if not exists)
2. Create enum `OrigenPermiso` with three values:
   - `DOCUMENTO`: Permission explicitly set on the document
   - `CARPETA_DIRECTO`: Direct permission on the containing folder
   - `CARPETA_HEREDADO`: Inherited permission from ancestor folder
3. Add JavaDoc explaining each value
4. Use English for enum values (backend standard)

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/model/TipoRecurso.java`

**Action**: Create enum to identify resource type (document or folder)

**Implementation Steps**:
1. Create enum `TipoRecurso` with two values:
   - `DOCUMENTO`: Document resource
   - `CARPETA`: Folder resource
2. Add JavaDoc describing usage

**Dependencies**: None

**Implementation Notes**:
- Follow Java enum naming conventions (PascalCase for enum name, UPPER_CASE for values)
- Keep enums simple without business logic
- These enums will be used in DTOs and service method signatures

---

### Step 2: Extend PermisoEfectivo Domain Model

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/model/PermisoEfectivo.java`

**Action**: Extend existing `PermisoEfectivo` class to support document permissions and origin tracking

**Current Structure**:
```java
public final class PermisoEfectivo {
    private final NivelAcceso nivelAcceso;
    private final boolean esHeredado;
    private final Long carpetaOrigenId;
    private final String carpetaOrigenNombre;
    private final List<String> rutaHerencia;
    
    // Static factory methods: directo(), heredado()
}
```

**Implementation Steps**:
1. **Add new fields**:
   - `OrigenPermiso origen`: Replace `boolean esHeredado` with enum for more precise origin tracking
   - `TipoRecurso tipoRecurso`: Identify if origin is document or folder
   - `Long recursoOrigenId`: Generic field to replace `carpetaOrigenId` (can be document or folder ID)
   - `OffsetDateTime evaluadoEn`: Timestamp of evaluation (useful for caching/debugging)

2. **Update constructor**:
   - Modify private constructor to accept new fields
   - Add null checks for `origen`, `tipoRecurso`, `recursoOrigenId`
   - Make `evaluadoEn` default to `OffsetDateTime.now()` if not provided

3. **Add new static factory methods**:
   - `documento(NivelAcceso, Long documentoId)`: Create permission from document ACL
   - `carpetaDirecto(NivelAcceso, Long carpetaId, String carpetaNombre)`: Create permission from direct folder ACL
   - `carpetaHeredado(NivelAcceso, Long carpetaOrigenId, String carpetaNombre, List<String> ruta)`: Create permission from inherited folder ACL

4. **Mark old factory methods as @Deprecated** (maintain backward compatibility):
   - `directo()`: Delegate to `carpetaDirecto()`
   - `heredado()`: Delegate to `carpetaHeredado()`

5. **Add convenience methods**:
   - `boolean isDesdeDocumento()`: Returns `origen == OrigenPermiso.DOCUMENTO`
   - `boolean isDesdeCarpeta()`: Returns `origen == CARPETA_DIRECTO || origen == CARPETA_HEREDADO`
   - `boolean isHeredado()`: Returns `origen == CARPETA_HEREDADO`

**Dependencies**:
- Import `OrigenPermiso`
- Import `TipoRecurso`
- Import `java.time.OffsetDateTime`

**Implementation Notes**:
- Maintain immutability with `final` fields
- Use defensive copies for collections
- Ensure backward compatibility by keeping old factory methods working
- Add comprehensive JavaDoc explaining the precedence rule

---

### Step 3: Create Domain Service Interface (Port)

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/service/IEvaluadorPermisos.java`

**Action**: Define the contract for centralized permission evaluation

**Function Signatures**:

```java
package com.docflow.documentcore.domain.service;

import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.NivelAcceso;

/**
 * Domain service interface for centralized permission evaluation.
 * 
 * <p>Applies the precedence rule:
 * <strong>Document ACL > Direct Folder ACL > Inherited Folder ACL</strong></p>
 * 
 * <p>This service is the single source of truth for permission resolution
 * and must be used by all guards, middlewares, and authorization logic.</p>
 */
public interface IEvaluadorPermisos {
    
    /**
     * Evaluates effective permission of a user over a document.
     * 
     * <p>Precedence order:
     * <ol>
     *   <li>Document explicit ACL (highest priority)</li>
     *   <li>Containing folder direct ACL</li>
     *   <li>Inherited ACL from ancestor folders</li>
     * </ol>
     * </p>
     * 
     * @param usuarioId User ID
     * @param documentoId Document ID
     * @param organizacionId Organization ID (tenant isolation)
     * @return Effective permission with origin info, or null if no permission
     */
    PermisoEfectivo evaluarPermisoDocumento(
        Long usuarioId, 
        Long documentoId, 
        Long organizacionId
    );
    
    /**
     * Evaluates effective permission of a user over a folder.
     * 
     * <p>Considers direct ACL and inheritance from ancestors.</p>
     * 
     * @param usuarioId User ID
     * @param carpetaId Folder ID
     * @param organizacionId Organization ID
     * @return Effective permission with origin info, or null if no permission
     */
    PermisoEfectivo evaluarPermisoCarpeta(
        Long usuarioId, 
        Long carpetaId, 
        Long organizacionId
    );
    
    /**
     * Checks if a user has at least the required access level on a resource.
     * 
     * <p>This is a convenience method that internally calls
     * {@link #evaluarPermisoDocumento} or {@link #evaluarPermisoCarpeta}
     * and compares the access level.</p>
     * 
     * @param usuarioId User ID
     * @param recursoId Resource ID (document or folder)
     * @param tipoRecurso Resource type
     * @param nivelRequerido Minimum required access level
     * @param organizacionId Organization ID
     * @return true if user has at least the required level, false otherwise
     */
    boolean tieneAcceso(
        Long usuarioId, 
        Long recursoId, 
        TipoRecurso tipoRecurso,
        NivelAcceso nivelRequerido,
        Long organizacionId
    );
}
```

**Implementation Steps**:
1. Create package `com.docflow.documentcore.domain.service` (if not exists)
2. Create interface with three methods as defined above
3. Add comprehensive JavaDoc explaining:
   - Precedence rule
   - Return value semantics (null = no permission)
   - Thread-safety expectations (stateless, safe for concurrent use)
   - Multi-tenancy guarantees (always filter by organizacionId)

**Dependencies**:
- `PermisoEfectivo`, `TipoRecurso`, `NivelAcceso`

**Implementation Notes**:
- This is a **port** in hexagonal architecture (domain interface)
- The implementation will be in the application layer
- Interface belongs to domain to keep dependencies pointing inward
- Document that null return means "no permission" (not an error)

---

### Step 4: Implement Application Service (Adapter)

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/application/service/EvaluadorPermisosService.java`

**Action**: Implement the centralized permission evaluation logic

**Function Signatures**:

```java
@Service
@Transactional(readOnly = true)
public class EvaluadorPermisosService implements IEvaluadorPermisos {
    
    private final IPermisoDocumentoUsuarioRepository permisoDocumentoRepository;
    private final IPermisoCarpetaUsuarioRepository permisoCarpetaRepository;
    private final PermisoHerenciaService permisoHerenciaService;
    private final ICarpetaRepository carpetaRepository;
    
    // Constructor injection
    
    @Override
    public PermisoEfectivo evaluarPermisoDocumento(Long usuarioId, Long documentoId, Long organizacionId) {
        // Implementation
    }
    
    @Override
    public PermisoEfectivo evaluarPermisoCarpeta(Long usuarioId, Long carpetaId, Long organizacionId) {
        // Implementation
    }
    
    @Override
    public boolean tieneAcceso(Long usuarioId, Long recursoId, TipoRecurso tipoRecurso, 
                              NivelAcceso nivelRequerido, Long organizacionId) {
        // Implementation
    }
    
    private boolean cumpleNivelRequerido(NivelAcceso actual, NivelAcceso requerido) {
        // Helper method
    }
    
    private int getNivelJerarquico(NivelAcceso nivel) {
        // Helper method
    }
}
```

**Implementation Steps**:

1. **Service Setup**:
   - Add `@Service` annotation
   - Add `@Transactional(readOnly = true)` for read-only transactions
   - Add `@Slf4j` (Lombok) for logging
   - Inject dependencies via constructor:
     - `IPermisoDocumentoUsuarioRepository`
     - `IPermisoCarpetaUsuarioRepository`
     - `PermisoHerenciaService` (for folder permission resolution)
     - `ICarpetaRepository` (to get document's folder)

2. **Implement `evaluarPermisoDocumento`**:
   ```java
   @Override
   public PermisoEfectivo evaluarPermisoDocumento(Long usuarioId, Long documentoId, Long organizacionId) {
       logger.debug("Evaluating document permission: user={}, doc={}, org={}", usuarioId, documentoId, organizacionId);
       
       // STEP 1: Check for explicit document ACL (HIGHEST PRIORITY)
       Optional<PermisoDocumentoUsuario> aclDocumento = 
           permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId);
       
       if (aclDocumento.isPresent() && aclDocumento.get().getOrganizacionId().equals(organizacionId)) {
           PermisoDocumentoUsuario permiso = aclDocumento.get();
           
           logger.debug("Document permission found: nivel={}, origin=DOCUMENTO", permiso.getNivelAcceso());
           
           return PermisoEfectivo.documento(
               permiso.getNivelAcceso(),
               documentoId
           );
       }
       
       // STEP 2: Fallback to folder permission (direct or inherited)
       logger.debug("No document ACL found, checking folder permissions");
       
       // Get document's containing folder
       Documento documento = documentoRepository.findById(documentoId, organizacionId)
           .orElseThrow(() -> new DocumentoNotFoundException(documentoId));
       
       Long carpetaId = documento.getCarpetaId();
       
       // Delegate to folder evaluation (which handles inheritance)
       return evaluarPermisoCarpeta(usuarioId, carpetaId, organizacionId);
   }
   ```

3. **Implement `evaluarPermisoCarpeta`**:
   ```java
   @Override
   public PermisoEfectivo evaluarPermisoCarpeta(Long usuarioId, Long carpetaId, Long organizacionId) {
       logger.debug("Evaluating folder permission: user={}, folder={}, org={}", usuarioId, carpetaId, organizacionId);
       
       // Delegate to existing PermisoHerenciaService
       // This service already handles direct ACL and inheritance correctly
       Optional<PermisoEfectivo> permisoEfectivo = 
           permisoHerenciaService.evaluarPermisoEfectivo(usuarioId, carpetaId, organizacionId);
       
       return permisoEfectivo.orElse(null);
   }
   ```

4. **Implement `tieneAcceso`**:
   ```java
   @Override
   public boolean tieneAcceso(Long usuarioId, Long recursoId, TipoRecurso tipoRecurso,
                             NivelAcceso nivelRequerido, Long organizacionId) {
       // Evaluate permission based on resource type
       PermisoEfectivo permiso = tipoRecurso == TipoRecurso.DOCUMENTO 
           ? evaluarPermisoDocumento(usuarioId, recursoId, organizacionId)
           : evaluarPermisoCarpeta(usuarioId, recursoId, organizacionId);
       
       if (permiso == null) {
           logger.debug("No permission found for user={} on resource={}", usuarioId, recursoId);
           return false;
       }
       
       // Check if actual level meets required level
       boolean hasAccess = cumpleNivelRequerido(permiso.getNivelAcceso(), nivelRequerido);
       
       logger.debug("Access check: user={}, resource={}, required={}, actual={}, granted={}", 
                   usuarioId, recursoId, nivelRequerido, permiso.getNivelAcceso(), hasAccess);
       
       return hasAccess;
   }
   ```

5. **Implement Helper Methods**:
   ```java
   private boolean cumpleNivelRequerido(NivelAcceso actual, NivelAcceso requerido) {
       int nivelActual = getNivelJerarquico(actual);
       int nivelReq = getNivelJerarquico(requerido);
       return nivelActual >= nivelReq;
   }
   
   private int getNivelJerarquico(NivelAcceso nivel) {
       return switch (nivel) {
           case LECTURA -> 1;
           case ESCRITURA -> 2;
           case ADMINISTRACION -> 3;
       };
   }
   ```

**Dependencies**:
- Spring Framework: `@Service`, `@Transactional`
- Lombok: `@Slf4j`, `@RequiredArgsConstructor`
- Domain repositories and services
- Domain models

**Implementation Notes**:
- **READ-ONLY transactions**: All methods are read-only, optimize with `readOnly = true`
- **Logging strategy**: 
  - DEBUG level for evaluation steps (can be noisy in production)
  - INFO level for permission denials (security audit)
  - WARN level for unexpected states
- **Performance**: Consider caching evaluation results for frequently accessed resources
- **Thread-safety**: Service is stateless, safe for concurrent use
- **Multi-tenancy**: Always validate `organizacionId` matches to prevent cross-tenant access

---

### Step 5: Create Custom Annotation for Permission Checks

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/security/RequierePermiso.java`

**Action**: Create custom annotation to declaratively specify required permissions on controller methods

**Implementation Steps**:

1. Create annotation with following properties:
   ```java
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   @Documented
   public @interface RequierePermiso {
       /**
        * Type of resource being accessed (DOCUMENTO or CARPETA)
        */
       TipoRecurso tipoRecurso();
       
       /**
        * Minimum required access level
        */
       NivelAcceso nivelRequerido();
       
       /**
        * Index of the method parameter that contains the resource ID.
        * Default is 0 (first parameter).
        * Example: for method foo(Long docId, String name), use paramIndex = 0
        */
       int paramIndex() default 0;
       
       /**
        * Error message to return if permission is denied.
        * Default: "Insufficient permissions"
        */
       String errorMessage() default "Insufficient permissions";
   }
   ```

2. Add JavaDoc explaining:
   - How to use the annotation
   - Parameter extraction mechanism
   - Error handling behavior

**Dependencies**:
- `TipoRecurso`, `NivelAcceso` enums

**Implementation Notes**:
- `@Retention(RUNTIME)` is required for AOP to detect it at runtime
- `paramIndex` allows flexibility in method signature design
- Default error message can be overridden for specific endpoints

**Example Usage**:
```java
@GetMapping("/{documentoId}")
@RequierePermiso(
    tipoRecurso = TipoRecurso.DOCUMENTO,
    nivelRequerido = NivelAcceso.LECTURA,
    paramIndex = 0
)
public ResponseEntity<DocumentoDTO> getDocumento(@PathVariable Long documentoId) {
    // Method only executes if user has LECTURA on documentoId
}
```

---

### Step 6: Implement Permission Guard (AOP Aspect)

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/security/RequierePermisoGuard.java`

**Action**: Create AOP aspect to intercept methods annotated with `@RequierePermiso` and enforce permission checks

**Function Signatures**:

```java
@Aspect
@Component
@RequiredArgsConstructor
public class RequierePermisoGuard {
    
    private final EvaluadorPermisosService evaluadorPermisos;
    private final CurrentTenantService tenantService;
    private final CurrentUserService userService;
    
    @Around("@annotation(requierePermiso)")
    public Object verificarPermiso(ProceedingJoinPoint joinPoint, RequierePermiso requierePermiso) throws Throwable {
        // Implementation
    }
    
    private Long extraerRecursoId(ProceedingJoinPoint joinPoint, int paramIndex) {
        // Helper method
    }
}
```

**Implementation Steps**:

1. **Class Setup**:
   - Add `@Aspect` (Spring AOP)
   - Add `@Component` (Spring bean)
   - Add `@RequiredArgsConstructor` (Lombok constructor injection)
   - Add `@Slf4j` for logging
   - Inject dependencies:
     - `EvaluadorPermisosService`
     - `CurrentTenantService` (to get current tenant ID)
     - `CurrentUserService` (to get current user ID)

2. **Implement `verificarPermiso` Method**:
   ```java
   @Around("@annotation(requierePermiso)")
   public Object verificarPermiso(ProceedingJoinPoint joinPoint, RequierePermiso requierePermiso) throws Throwable {
       
       // Extract context information
       Long usuarioId = userService.getCurrentUserId();
       Long organizacionId = tenantService.getCurrentTenantId();
       
       // Extract resource ID from method parameters
       Long recursoId = extraerRecursoId(joinPoint, requierePermiso.paramIndex());
       
       String methodName = joinPoint.getSignature().toShortString();
       
       logger.debug("Permission check: method={}, user={}, resource={}, type={}, required={}", 
                   methodName, usuarioId, recursoId, requierePermiso.tipoRecurso(), 
                   requierePermiso.nivelRequerido());
       
       // Check permission using evaluator service
       boolean tieneAcceso = evaluadorPermisos.tieneAcceso(
           usuarioId,
           recursoId,
           requierePermiso.tipoRecurso(),
           requierePermiso.nivelRequerido(),
           organizacionId
       );
       
       if (!tieneAcceso) {
           logger.warn("Permission denied: method={}, user={}, resource={}, required={}", 
                      methodName, usuarioId, recursoId, requierePermiso.nivelRequerido());
           
           throw new ResponseStatusException(
               HttpStatus.FORBIDDEN,
               requierePermiso.errorMessage()
           );
       }
       
       logger.debug("Permission granted: method={}, user={}, resource={}", 
                   methodName, usuarioId, recursoId);
       
       // Proceed with the original method execution
       return joinPoint.proceed();
   }
   ```

3. **Implement `extraerRecursoId` Helper**:
   ```java
   private Long extraerRecursoId(ProceedingJoinPoint joinPoint, int paramIndex) {
       Object[] args = joinPoint.getArgs();
       
       if (paramIndex < 0 || paramIndex >= args.length) {
           throw new IllegalArgumentException(
               String.format("Invalid paramIndex %d for method %s with %d parameters", 
                           paramIndex, joinPoint.getSignature().getName(), args.length)
           );
       }
       
       Object param = args[paramIndex];
       
       if (!(param instanceof Long)) {
           throw new IllegalArgumentException(
               String.format("Parameter at index %d must be Long, but was %s", 
                           paramIndex, param.getClass().getSimpleName())
           );
       }
       
       return (Long) param;
   }
   ```

**Dependencies**:
- Spring AOP: `@Aspect`, `@Around`, `ProceedingJoinPoint`
- Spring Framework: `@Component`
- Spring Web: `ResponseStatusException`, `HttpStatus`
- Lombok: `@RequiredArgsConstructor`, `@Slf4j`
- Application services: `EvaluadorPermisosService`, `CurrentTenantService`, `CurrentUserService`

**Implementation Notes**:
- **Exception handling**: Throw `ResponseStatusException` with `403 FORBIDDEN` for permission denials
- **Logging**: 
  - DEBUG for successful checks
  - WARN for denials (security audit)
  - Include user ID and resource ID in all logs
- **Error messages**: Use custom message from annotation, fallback to default
- **Performance**: AOP has minimal overhead, but consider caching for repeated checks
- **Testing**: AOP aspects require integration tests with Spring context

---

### Step 7: Update Existing Controllers to Use Permission Guard

**Files to Modify**:
- `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/DocumentoController.java`
- `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/CarpetaController.java`

**Action**: Replace inline permission checks with `@RequierePermiso` annotation

**Implementation Steps**:

1. **Identify endpoints requiring permission checks**:
   - GET endpoints → `LECTURA`
   - POST/PUT endpoints → `ESCRITURA`
   - DELETE endpoints → `ESCRITURA` or `ADMINISTRACION` (depending on business rules)
   - Permission management endpoints → `ADMINISTRACION`

2. **Add `@RequierePermiso` to Document Endpoints**:
   ```java
   // Example: Get document
   @GetMapping("/{documentoId}")
   @RequierePermiso(
       tipoRecurso = TipoRecurso.DOCUMENTO,
       nivelRequerido = NivelAcceso.LECTURA,
       paramIndex = 0
   )
   public ResponseEntity<DocumentoResponseDTO> obtenerDocumento(@PathVariable Long documentoId) {
       // Remove inline permission checks
       // Business logic only
   }
   
   // Example: Update document
   @PutMapping("/{documentoId}")
   @RequierePermiso(
       tipoRecurso = TipoRecurso.DOCUMENTO,
       nivelRequerido = NivelAcceso.ESCRITURA,
       paramIndex = 0
   )
   public ResponseEntity<DocumentoResponseDTO> actualizarDocumento(
       @PathVariable Long documentoId,
       @RequestBody @Valid ActualizarDocumentoDTO dto
   ) {
       // Business logic only
   }
   
   // Example: Delete document
   @DeleteMapping("/{documentoId}")
   @RequierePermiso(
       tipoRecurso = TipoRecurso.DOCUMENTO,
       nivelRequerido = NivelAcceso.ESCRITURA,
       paramIndex = 0,
       errorMessage = "Cannot delete document: insufficient permissions"
   )
   public ResponseEntity<Void> eliminarDocumento(@PathVariable Long documentoId) {
       // Business logic only
   }
   ```

3. **Add `@RequierePermiso` to Folder Endpoints**:
   ```java
   @GetMapping("/{carpetaId}")
   @RequierePermiso(
       tipoRecurso = TipoRecurso.CARPETA,
       nivelRequerido = NivelAcceso.LECTURA,
       paramIndex = 0
   )
   public ResponseEntity<CarpetaResponseDTO> obtenerCarpeta(@PathVariable Long carpetaId) {
       // Business logic only
   }
   
   @PostMapping("/{carpetaId}/subcarpetas")
   @RequierePermiso(
       tipoRecurso = TipoRecurso.CARPETA,
       nivelRequerido = NivelAcceso.ESCRITURA,
       paramIndex = 0
   )
   public ResponseEntity<CarpetaResponseDTO> crearSubcarpeta(
       @PathVariable Long carpetaId,
       @RequestBody @Valid CrearCarpetaDTO dto
   ) {
       // Business logic only
   }
   ```

4. **Remove Old Permission Check Code**:
   - Delete inline service calls to permission validation
   - Remove try-catch blocks for permission exceptions
   - Simplify controller methods to focus on business logic orchestration

5. **Update Exception Handling**:
   - Remove specific permission exception handlers if they exist
   - Let `ResponseStatusException` from guard be handled by global exception handler
   - Ensure consistent error response format

**Dependencies**:
- `@RequierePermiso` annotation
- `TipoRecurso`, `NivelAcceso` enums

**Implementation Notes**:
- **Separation of concerns**: Controllers should only orchestrate, not check permissions
- **Declarative security**: Permission requirements are self-documenting via annotations
- **Consistency**: All endpoints follow the same permission enforcement mechanism
- **Testing**: Update controller tests to mock authentication context

---

### Step 8: Add Query Endpoint for Effective Permissions

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/PermisoController.java`

**Action**: Create new endpoint for users to query their effective permissions on resources

**Function Signatures**:

```java
@RestController
@RequestMapping("/api/permisos")
@RequiredArgsConstructor
public class PermisoController {
    
    private final EvaluadorPermisosService evaluadorPermisos;
    private final CurrentUserService userService;
    private final CurrentTenantService tenantService;
    
    @GetMapping("/documentos/{documentoId}/mi-permiso")
    public ResponseEntity<PermisoEfectivoDTO> obtenerMiPermisoDocumento(@PathVariable Long documentoId) {
        // Implementation
    }
    
    @GetMapping("/carpetas/{carpetaId}/mi-permiso")
    public ResponseEntity<PermisoEfectivoDTO> obtenerMiPermisoCarpeta(@PathVariable Long carpetaId) {
        // Implementation
    }
}
```

**Implementation Steps**:

1. **Create Controller Class**:
   - Add `@RestController` and `@RequestMapping("/api/permisos")`
   - Inject `EvaluadorPermisosService`, `CurrentUserService`, `CurrentTenantService`

2. **Create PermisoEfectivoDTO** (Response DTO):
   ```java
   // File: application/dto/PermisoEfectivoDTO.java
   @Data
   @Builder
   public class PermisoEfectivoDTO {
       @NotNull
       private String nivelAcceso; // LECTURA, ESCRITURA, ADMINISTRACION
       
       @NotNull
       private String origen; // DOCUMENTO, CARPETA_DIRECTO, CARPETA_HEREDADO
       
       @NotNull
       private Long recursoOrigenId;
       
       @NotNull
       private String tipoRecurso; // DOCUMENTO, CARPETA
       
       private OffsetDateTime evaluadoEn;
   }
   ```

3. **Implement Document Permission Query**:
   ```java
   @GetMapping("/documentos/{documentoId}/mi-permiso")
   public ResponseEntity<PermisoEfectivoDTO> obtenerMiPermisoDocumento(
       @PathVariable Long documentoId
   ) {
       Long usuarioId = userService.getCurrentUserId();
       Long organizacionId = tenantService.getCurrentTenantId();
       
       PermisoEfectivo permiso = evaluadorPermisos.evaluarPermisoDocumento(
           usuarioId, documentoId, organizacionId
       );
       
       if (permiso == null) {
           return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
       }
       
       PermisoEfectivoDTO dto = PermisoEfectivoDTO.builder()
           .nivelAcceso(permiso.getNivelAcceso().name())
           .origen(permiso.getOrigen().name())
           .recursoOrigenId(permiso.getRecursoOrigenId())
           .tipoRecurso(permiso.getTipoRecurso().name())
           .evaluadoEn(permiso.getEvaluadoEn())
           .build();
       
       return ResponseEntity.ok(dto);
   }
   ```

4. **Implement Folder Permission Query**:
   ```java
   @GetMapping("/carpetas/{carpetaId}/mi-permiso")
   public ResponseEntity<PermisoEfectivoDTO> obtenerMiPermisoCarpeta(
       @PathVariable Long carpetaId
   ) {
       Long usuarioId = userService.getCurrentUserId();
       Long organizacionId = tenantService.getCurrentTenantId();
       
       PermisoEfectivo permiso = evaluadorPermisos.evaluarPermisoCarpeta(
           usuarioId, carpetaId, organizacionId
       );
       
       if (permiso == null) {
           return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
       }
       
       PermisoEfectivoDTO dto = PermisoEfectivoDTO.builder()
           .nivelAcceso(permiso.getNivelAcceso().name())
           .origen(permiso.getOrigen().name())
           .recursoOrigenId(permiso.getRecursoOrigenId())
           .tipoRecurso(permiso.getTipoRecurso().name())
           .evaluadoEn(permiso.getEvaluadoEn())
           .build();
       
       return ResponseEntity.ok(dto);
   }
   ```

**Dependencies**:
- Spring Web: `@RestController`, `@GetMapping`, `ResponseEntity`
- Application services
- DTOs

**Implementation Notes**:
- **No permission check annotation**: These endpoints check permissions internally
- **403 response**: Return FORBIDDEN if user has no permission (consistent with REST standards)
- **Use cases**: 
  - UI to show/hide action buttons based on permissions
  - Debugging permission issues
  - Mobile apps to pre-check permissions before attempting operations

---

### Step 9: Write Unit Tests for EvaluadorPermisosService

**File**: `backend/document-core/src/test/java/com/docflow/documentcore/application/service/EvaluadorPermisosServiceTest.java`

**Action**: Create comprehensive unit tests using TDD approach (RED → GREEN → REFACTOR)

**Test Categories**:

1. **Successful Cases**
   - Document permission takes precedence over folder permission
   - Folder direct permission used when no document permission exists
   - Inherited permission used when no direct permission exists
   - Permission hierarchy respected (LECTURA < ESCRITURA < ADMINISTRACION)

2. **Permission Denial Cases**
   - No permission at any level returns null
   - Insufficient permission level returns false in `tieneAcceso()`

3. **Edge Cases**
   - Document permission more restrictive than folder (e.g., LECTURA on doc, ESCRITURA on folder)
   - Multiple inheritance paths (deepest ancestor wins)
   - Organization isolation (cross-tenant permission denied)

**Implementation Steps**:

1. **Test Class Setup**:
   ```java
   @ExtendWith(MockitoExtension.class)
   @DisplayName("EvaluadorPermisosService - Unit Tests (TDD)")
   class EvaluadorPermisosServiceTest {
       
       @Mock private IPermisoDocumentoUsuarioRepository permisoDocumentoRepository;
       @Mock private IPermisoCarpetaUsuarioRepository permisoCarpetaRepository;
       @Mock private PermisoHerenciaService permisoHerenciaService;
       @Mock private IDocumentoRepository documentoRepository;
       
       @InjectMocks private EvaluadorPermisosService evaluador;
       
       private Long usuarioId, documentoId, carpetaId, organizacionId;
       
       @BeforeEach
       void setUp() {
           usuarioId = 1L;
           documentoId = 10L;
           carpetaId = 5L;
           organizacionId = 100L;
       }
   }
   ```

2. **Test: Document Permission Takes Precedence**:
   ```java
   @Test
   @DisplayName("Should use document permission when explicit ACL exists")
   void should_UseDocumentPermission_When_ExplicitAclExists() {
       // Given: Document ACL with LECTURA
       PermisoDocumentoUsuario aclDocumento = new PermisoDocumentoUsuario();
       aclDocumento.setDocumentoId(documentoId);
       aclDocumento.setUsuarioId(usuarioId);
       aclDocumento.setNivelAcceso(NivelAcceso.LECTURA);
       aclDocumento.setOrganizacionId(organizacionId);
       
       when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
           .thenReturn(Optional.of(aclDocumento));
       
       // When: Evaluate permission
       PermisoEfectivo resultado = evaluador.evaluarPermisoDocumento(
           usuarioId, documentoId, organizacionId
       );
       
       // Then: Returns document permission
       assertThat(resultado).isNotNull();
       assertThat(resultado.getNivelAcceso()).isEqualTo(NivelAcceso.LECTURA);
       assertThat(resultado.getOrigen()).isEqualTo(OrigenPermiso.DOCUMENTO);
       assertThat(resultado.getRecursoOrigenId()).isEqualTo(documentoId);
       
       verify(permisoDocumentoRepository).findByDocumentoIdAndUsuarioId(documentoId, usuarioId);
       verifyNoInteractions(permisoCarpetaRepository); // Should not check folder
   }
   ```

3. **Test: Folder Permission Used When No Document Permission**:
   ```java
   @Test
   @DisplayName("Should use folder permission when no document ACL exists")
   void should_UseFolderPermission_When_NoDocumentAcl() {
       // Given: No document ACL, folder has ESCRITURA
       when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
           .thenReturn(Optional.empty());
       
       Documento documento = new Documento();
       documento.setCarpetaId(carpetaId);
       documento.setOrganizacionId(organizacionId);
       when(documentoRepository.findById(documentoId, organizacionId))
           .thenReturn(Optional.of(documento));
       
       PermisoEfectivo permisoEsperado = PermisoEfectivo.carpetaDirecto(
           NivelAcceso.ESCRITURA, carpetaId, "Carpeta Test"
       );
       when(permisoHerenciaService.evaluarPermisoEfectivo(usuarioId, carpetaId, organizacionId))
           .thenReturn(Optional.of(permisoEsperado));
       
       // When
       PermisoEfectivo resultado = evaluador.evaluarPermisoDocumento(
           usuarioId, documentoId, organizacionId
       );
       
       // Then
       assertThat(resultado).isNotNull();
       assertThat(resultado.getNivelAcceso()).isEqualTo(NivelAcceso.ESCRITURA);
       assertThat(resultado.getOrigen()).isEqualTo(OrigenPermiso.CARPETA_DIRECTO);
   }
   ```

4. **Test: Document Permission More Restrictive Than Folder**:
   ```java
   @Test
   @DisplayName("Should prioritize document permission even when folder has higher level")
   void should_PrioritizeDocument_Even_When_FolderHasHigherLevel() {
       // Given: LECTURA on document, ADMINISTRACION on folder
       PermisoDocumentoUsuario aclDocumento = new PermisoDocumentoUsuario();
       aclDocumento.setDocumentoId(documentoId);
       aclDocumento.setUsuarioId(usuarioId);
       aclDocumento.setNivelAcceso(NivelAcceso.LECTURA);
       aclDocumento.setOrganizacionId(organizacionId);
       
       when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
           .thenReturn(Optional.of(aclDocumento));
       
       // When
       PermisoEfectivo resultado = evaluador.evaluarPermisoDocumento(
           usuarioId, documentoId, organizacionId
       );
       
       // Then: Document permission wins (LECTURA), folder never checked
       assertThat(resultado.getNivelAcceso()).isEqualTo(NivelAcceso.LECTURA);
       assertThat(resultado.getOrigen()).isEqualTo(OrigenPermiso.DOCUMENTO);
       verifyNoInteractions(permisoHerenciaService);
   }
   ```

5. **Test: No Permission Returns Null**:
   ```java
   @Test
   @DisplayName("Should return null when no permission exists")
   void should_ReturnNull_When_NoPermission() {
       // Given: No permissions at any level
       when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
           .thenReturn(Optional.empty());
       
       Documento documento = new Documento();
       documento.setCarpetaId(carpetaId);
       documento.setOrganizacionId(organizacionId);
       when(documentoRepository.findById(documentoId, organizacionId))
           .thenReturn(Optional.of(documento));
       
       when(permisoHerenciaService.evaluarPermisoEfectivo(usuarioId, carpetaId, organizacionId))
           .thenReturn(Optional.empty());
       
       // When
       PermisoEfectivo resultado = evaluador.evaluarPermisoDocumento(
           usuarioId, documentoId, organizacionId
       );
       
       // Then
       assertThat(resultado).isNull();
   }
   ```

6. **Test: tieneAcceso Returns False When No Permission**:
   ```java
   @Test
   @DisplayName("tieneAcceso should return false when no permission exists")
   void should_ReturnFalse_When_NoPermission() {
       // Given: No permission
       when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
           .thenReturn(Optional.empty());
       
       Documento documento = new Documento();
       documento.setCarpetaId(carpetaId);
       documento.setOrganizacionId(organizacionId);
       when(documentoRepository.findById(documentoId, organizacionId))
           .thenReturn(Optional.of(documento));
       
       when(permisoHerenciaService.evaluarPermisoEfectivo(usuarioId, carpetaId, organizacionId))
           .thenReturn(Optional.empty());
       
       // When
       boolean resultado = evaluador.tieneAcceso(
           usuarioId, documentoId, TipoRecurso.DOCUMENTO, 
           NivelAcceso.LECTURA, organizacionId
       );
       
       // Then
       assertThat(resultado).isFalse();
   }
   ```

7. **Test: tieneAcceso Returns True When Sufficient Permission**:
   ```java
   @Test
   @DisplayName("tieneAcceso should return true when permission level is sufficient")
   void should_ReturnTrue_When_HasSufficientLevel() {
       // Given: ESCRITURA available, requires LECTURA
       PermisoDocumentoUsuario aclDocumento = new PermisoDocumentoUsuario();
       aclDocumento.setDocumentoId(documentoId);
       aclDocumento.setUsuarioId(usuarioId);
       aclDocumento.setNivelAcceso(NivelAcceso.ESCRITURA);
       aclDocumento.setOrganizacionId(organizacionId);
       
       when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
           .thenReturn(Optional.of(aclDocumento));
       
       // When: Requires LECTURA (lower level)
       boolean resultado = evaluador.tieneAcceso(
           usuarioId, documentoId, TipoRecurso.DOCUMENTO,
           NivelAcceso.LECTURA, organizacionId
       );
       
       // Then: Should return true (ESCRITURA >= LECTURA)
       assertThat(resultado).isTrue();
   }
   ```

8. **Test: Organization Isolation**:
   ```java
   @Test
   @DisplayName("Should deny permission when organization ID does not match")
   void should_DenyPermission_When_OrganizationMismatch() {
       // Given: Permission exists but for different organization
       Long differentOrgId = 999L;
       
       PermisoDocumentoUsuario aclDocumento = new PermisoDocumentoUsuario();
       aclDocumento.setDocumentoId(documentoId);
       aclDocumento.setUsuarioId(usuarioId);
       aclDocumento.setNivelAcceso(NivelAcceso.ADMINISTRACION);
       aclDocumento.setOrganizacionId(differentOrgId); // Different org
       
       when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
           .thenReturn(Optional.of(aclDocumento));
       
       Documento documento = new Documento();
       documento.setCarpetaId(carpetaId);
       documento.setOrganizacionId(organizacionId);
       when(documentoRepository.findById(documentoId, organizacionId))
           .thenReturn(Optional.of(documento));
       
       when(permisoHerenciaService.evaluarPermisoEfectivo(usuarioId, carpetaId, organizacionId))
           .thenReturn(Optional.empty());
       
       // When: User from organizacionId tries to access
       PermisoEfectivo resultado = evaluador.evaluarPermisoDocumento(
           usuarioId, documentoId, organizacionId
       );
       
       // Then: Should be denied (null)
       assertThat(resultado).isNull();
   }
   ```

**Dependencies**:
- JUnit 5: `@ExtendWith`, `@Test`, `@DisplayName`, `@BeforeEach`
- Mockito: `@Mock`, `@InjectMocks`, `when()`, `verify()`
- AssertJ: `assertThat()`

**Implementation Notes**:
- **Test naming**: Follow pattern `should_X_When_Y`
- **Given-When-Then**: Structure tests with clear sections
- **Mock interactions**: Verify correct methods called with correct parameters
- **Edge cases**: Test boundary conditions and unexpected inputs
- **Coverage**: Aim for 100% coverage of permission evaluation logic

---

### Step 10: Write Integration Tests

**File**: `backend/document-core/src/test/java/com/docflow/documentcore/integration/PrecedenciaPermisosIntegrationTest.java`

**Action**: Create end-to-end integration tests with real database and Spring context

**Implementation Steps**:

1. **Test Class Setup**:
   ```java
   @SpringBootTest
   @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
   @Transactional
   @DisplayName("Integration Tests - Permission Precedence")
   class PrecedenciaPermisosIntegrationTest {
       
       @Autowired private EvaluadorPermisosService evaluador;
       @Autowired private IPermisoDocumentoUsuarioRepository permisoDocumentoRepo;
       @Autowired private IPermisoCarpetaUsuarioRepository permisoCarpetaRepo;
       @Autowired private ICarpetaRepository carpetaRepo;
       @Autowired private IDocumentoRepository documentoRepo;
       @Autowired private TestDataBuilder testDataBuilder;
       
       private Long organizacionId;
       private Long usuarioId;
       
       @BeforeEach
       void setUp() {
           organizacionId = 1L;
           usuarioId = 1L;
       }
   }
   ```

2. **Test: End-to-End Document Precedence**:
   ```java
   @Test
   @DisplayName("Scenario: User with ESCRITURA on folder but LECTURA on document")
   void scenario_RestrictiveDocumentPermission() {
       // Given: Create folder with ESCRITURA permission
       Carpeta carpeta = testDataBuilder.createCarpeta("Test Folder", null, organizacionId);
       
       PermisoCarpetaUsuario permisoCarpe = new PermisoCarpetaUsuario();
       permisoCarpeta.setCarpetaId(carpeta.getId());
       permisoCarpeta.setUsuarioId(usuarioId);
       permisoCarpeta.setNivelAcceso(NivelAcceso.ESCRITURA);
       permisoCarpeta.setOrganizacionId(organizacionId);
       permisoCarpeta.setRecursivo(false);
       permisoCarpetaRepo.save(permisoCarpeta);
       
       // Create document in folder with LECTURA permission
       Documento documento = testDataBuilder.createDocumento("Test Doc", carpeta.getId(), organizacionId);
       
       PermisoDocumentoUsuario permisoDoc = new PermisoDocumentoUsuario();
       permisoDoc.setDocumentoId(documento.getId());
       permisoDoc.setUsuarioId(usuarioId);
       permisoDoc.setNivelAcceso(NivelAcceso.LECTURA);
       permisoDoc.setOrganizacionId(organizacionId);
       permisoDocumentoRepo.save(permisoDoc);
       
       // When: Evaluate permission
       PermisoEfectivo permiso = evaluador.evaluarPermisoDocumento(
           usuarioId, documento.getId(), organizacionId
       );
       
       // Then: Document permission wins (LECTURA, not ESCRITURA)
       assertThat(permiso).isNotNull();
       assertThat(permiso.getNivelAcceso()).isEqualTo(NivelAcceso.LECTURA);
       assertThat(permiso.getOrigen()).isEqualTo(OrigenPermiso.DOCUMENTO);
       assertThat(permiso.getRecursoOrigenId()).isEqualTo(documento.getId());
       
       // And: User cannot write (insufficient level)
       boolean canWrite = evaluador.tieneAcceso(
           usuarioId, documento.getId(), TipoRecurso.DOCUMENTO,
           NivelAcceso.ESCRITURA, organizacionId
       );
       assertThat(canWrite).isFalse();
   }
   ```

3. **Test: Inherited Permission Scenario**:
   ```java
   @Test
   @DisplayName("Scenario: User with inherited recursive permission from ancestor")
   void scenario_InheritedRecursivePermission() {
       // Given: Root folder with ESCRITURA recursive
       Carpeta raiz = testDataBuilder.createCarpeta("Root", null, organizacionId);
       
       PermisoCarpetaUsuario permisoRaiz = new PermisoCarpetaUsuario();
       permisoRaiz.setCarpetaId(raiz.getId());
       permisoRaiz.setUsuarioId(usuarioId);
       permisoRaiz.setNivelAcceso(NivelAcceso.ESCRITURA);
       permisoRaiz.setRecursivo(true);
       permisoRaiz.setOrganizacionId(organizacionId);
       permisoCarpetaRepo.save(permisoRaiz);
       
       // Subfolder without explicit permission
       Carpeta subfolder = testDataBuilder.createCarpeta("Subfolder", raiz.getId(), organizacionId);
       
       // Document in subfolder without explicit permission
       Documento documento = testDataBuilder.createDocumento("Doc", subfolder.getId(), organizacionId);
       
       // When: Evaluate document permission
       PermisoEfectivo permiso = evaluador.evaluarPermisoDocumento(
           usuarioId, documento.getId(), organizacionId
       );
       
       // Then: Should have inherited ESCRITURA
       assertThat(permiso).isNotNull();
       assertThat(permiso.getNivelAcceso()).isEqualTo(NivelAcceso.ESCRITURA);
       assertThat(permiso.getOrigen()).isEqualTo(OrigenPermiso.CARPETA_HEREDADO);
       assertThat(permiso.getRecursoOrigenId()).isEqualTo(raiz.getId());
   }
   ```

4. **Test: Permission Revocation**:
   ```java
   @Test
   @DisplayName("Scenario: Permission fallback after document ACL is revoked")
   void scenario_PermissionFallbackAfterRevocation() {
       // Given: Folder with ESCRITURA, document with LECTURA
       Carpeta carpeta = testDataBuilder.createCarpeta("Folder", null, organizacionId);
       
       PermisoCarpetaUsuario permisoCarpeta = new PermisoCarpetaUsuario();
       permisoCarpeta.setCarpetaId(carpeta.getId());
       permisoCarpeta.setUsuarioId(usuarioId);
       permisoCarpeta.setNivelAcceso(NivelAcceso.ESCRITURA);
       permisoCarpeta.setOrganizacionId(organizacionId);
       permisoCarpetaRepo.save(permisoCarpeta);
       
       Documento documento = testDataBuilder.createDocumento("Doc", carpeta.getId(), organizacionId);
       
       PermisoDocumentoUsuario permisoDoc = new PermisoDocumentoUsuario();
       permisoDoc.setDocumentoId(documento.getId());
       permisoDoc.setUsuarioId(usuarioId);
       permisoDoc.setNivelAcceso(NivelAcceso.LECTURA);
       permisoDoc.setOrganizacionId(organizacionId);
       permisoDocumentoRepo.save(permisoDoc);
       
       // When: Document ACL is revoked
       permisoDocumentoRepo.deleteByDocumentoIdAndUsuarioId(documento.getId(), usuarioId);
       
       // Then: Should fall back to folder permission (ESCRITURA)
       PermisoEfectivo permiso = evaluador.evaluarPermisoDocumento(
           usuarioId, documento.getId(), organizacionId
       );
       
       assertThat(permiso).isNotNull();
       assertThat(permiso.getNivelAcceso()).isEqualTo(NivelAcceso.ESCRITURA);
       assertThat(permiso.getOrigen()).isEqualTo(OrigenPermiso.CARPETA_DIRECTO);
   }
   ```

**Dependencies**:
- Spring Boot Test: `@SpringBootTest`, `@AutoConfigureTestDatabase`, `@Transactional`
- JUnit 5
- AssertJ
- Test data builders

**Implementation Notes**:
- **Real database**: Use test database (H2 or TestContainers with PostgreSQL)
- **Transaction rollback**: Each test rolls back for isolation
- **Data builders**: Create helper class for building test data
- **Comprehensive scenarios**: Cover all business rules from acceptance criteria

---

### Step 11: Update Technical Documentation

**Action**: Review and update technical documentation according to changes made

**Implementation Steps**:

1. **Review Changes**: Analyze all code changes made during implementation:
   - New domain service: `IEvaluadorPermisos`
   - New application service: `EvaluadorPermisosService`
   - New security components: `RequierePermisoGuard`, `@RequierePermiso`
   - Extended domain model: `PermisoEfectivo`
   - New enums: `OrigenPermiso`, `TipoRecurso`
   - New API endpoint: `/api/permisos/documentos/{id}/mi-permiso`

2. **Identify Documentation Files to Update**:
   - **Data model**: `ai-specs/specs/data-model.md` (PermisoEfectivo extension)
   - **API spec**: `ai-specs/specs/api-spec.yml` (new endpoints)
   - **Backend standards**: `ai-specs/specs/backend-standards.md` (security patterns)

3. **Update `ai-specs/specs/data-model.md`**:
   - Add `OrigenPermiso` enum definition
   - Add `TipoRecurso` enum definition
   - Update `PermisoEfectivo` model with new fields
   - Document precedence algorithm in comments

4. **Update `ai-specs/specs/api-spec.yml`**:
   - Add `/api/permisos/documentos/{documentoId}/mi-permiso` endpoint
   - Add `/api/permisos/carpetas/{carpetaId}/mi-permiso` endpoint
   - Define `PermisoEfectivoDTO` schema
   - Document 403 response for permission denial

5. **Update `ai-specs/specs/backend-standards.md`**:
   - Add section on declarative security with `@RequierePermiso`
   - Document permission evaluation service usage
   - Add AOP security pattern example
   - Update error handling section with permission exceptions

6. **Verify Documentation**:
   - Confirm all changes are accurately reflected
   - Check that documentation follows established structure
   - Ensure proper English grammar and formatting
   - Validate code examples compile and run

7. **Report Updates**: Create summary in PR description:
   ```
   Documentation Updates:
   - data-model.md: Added OrigenPermiso, TipoRecurso enums, updated PermisoEfectivo
   - api-spec.yml: Added permission query endpoints
   - backend-standards.md: Added declarative security pattern with @RequierePermiso
   ```

**References**:
- Follow process described in `ai-specs/specs/documentation-standards.md`
- All documentation must be written in English
- Maintain consistency with existing documentation structure

**Notes**: This step is MANDATORY before considering the implementation complete. Do not skip documentation updates. All technical documentation should be kept up-to-date to ensure the project remains maintainable.

---

## Implementation Order

Follow these steps in sequence:

1. **Step 0**: Create Feature Branch (`feature/US-ACL-006-backend`)
2. **Step 1**: Create Domain Enums (`OrigenPermiso`, `TipoRecurso`)
3. **Step 2**: Extend `PermisoEfectivo` Domain Model
4. **Step 3**: Create Domain Service Interface (`IEvaluadorPermisos`)
5. **Step 4**: Implement Application Service (`EvaluadorPermisosService`)
6. **Step 5**: Create Custom Annotation (`@RequierePermiso`)
7. **Step 6**: Implement Permission Guard (AOP Aspect)
8. **Step 7**: Update Existing Controllers
9. **Step 8**: Add Query Endpoint for Effective Permissions
10. **Step 9**: Write Unit Tests
11. **Step 10**: Write Integration Tests
12. **Step 11**: Update Technical Documentation

## Testing Checklist

After implementation, verify:

- [ ] Unit tests pass with >90% coverage
- [ ] Integration tests pass with real database
- [ ] Document permission takes precedence over folder permission
- [ ] Folder direct permission used when no document permission
- [ ] Inherited permission used when no direct permission
- [ ] Null returned when no permission exists
- [ ] Organization isolation works correctly (cross-tenant denied)
- [ ] Permission hierarchy respected (LECTURA < ESCRITURA < ADMINISTRACION)
- [ ] Controllers protected with `@RequierePermiso` annotation
- [ ] Query endpoints return correct permission information
- [ ] AOP guard throws 403 for unauthorized access
- [ ] Logging captures permission evaluations

## Error Response Format

All permission-related errors follow standard REST error format:

```json
{
  "timestamp": "2026-02-04T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions",
  "path": "/api/documentos/123"
}
```

HTTP Status Code Mapping:
- `200 OK`: Permission granted, operation successful
- `403 FORBIDDEN`: Permission denied
- `404 NOT FOUND`: Resource does not exist
- `500 INTERNAL SERVER ERROR`: Unexpected error during permission evaluation

## Dependencies

### External Libraries
- Spring Framework 6.x (AOP, DI)
- Spring Boot 3.5.x
- Spring Data JPA
- Lombok (code generation)
- SLF4J (logging)

### Testing Libraries
- JUnit 5 (testing framework)
- Mockito (mocking)
- AssertJ (assertions)
- Spring Boot Test (integration testing)

### Internal Dependencies
- `PermisoHerenciaService` (existing folder inheritance logic)
- `CarpetaService` (folder operations)
- `CurrentTenantService` (tenant context)
- `CurrentUserService` (user context)
- Repository interfaces (persistence)

## Notes

### Important Reminders

1. **Precedence Rule is Strict**: Document permission ALWAYS wins, even if more restrictive than folder
2. **Null Means No Permission**: Service returns `null` to indicate absence of permission (not an error state)
3. **Multi-Tenancy is Critical**: Always filter by `organizacionId` to prevent cross-tenant access
4. **Performance Considerations**: 
   - Consider caching evaluation results for frequently accessed resources
   - Monitor query performance with folder inheritance
   - Use indexes on permission tables (document_id, usuario_id, carpeta_id)

### Business Rules

1. **Permission Hierarchy**: `LECTURA` (1) < `ESCRITURA` (2) < `ADMINISTRACION` (3)
2. **Precedence Order**: Document ACL > Folder Direct ACL > Folder Inherited ACL
3. **Inheritance**: Only applies to folders with `recursivo = true`
4. **Fallback Mechanism**: System automatically falls back through precedence levels
5. **No Default Permissions**: If no ACL found at any level, user has NO access

### Language Requirements

- All code, comments, documentation, and commit messages must be in **English**
- JavaDoc must be in English
- Variable names, class names, method names in English
- Log messages in English
- Exception messages in English

## Next Steps After Implementation

1. **Frontend Integration** (separate ticket):
   - Implement permission query service in frontend
   - Add UI indicators for permission origin
   - Show/hide action buttons based on effective permissions
   - Display permission warnings for conflicting ACLs

2. **Performance Optimization**:
   - Implement caching layer for permission evaluations
   - Add database indexes for permission queries
   - Monitor slow queries in permission evaluation

3. **Audit and Compliance**:
   - Log all permission denials for security audit
   - Track permission evaluation metrics
   - Create reports for permission conflicts

4. **User Documentation**:
   - Write user guide explaining precedence rules
   - Create examples of common permission scenarios
   - Document best practices for ACL management

## Implementation Verification

Before marking this ticket as complete, verify:

### Code Quality
- [ ] All code follows backend standards (DDD, hexagonal architecture)
- [ ] Lombok annotations used appropriately (`@RequiredArgsConstructor`, `@Slf4j`)
- [ ] No code duplication
- [ ] Proper error handling
- [ ] Comprehensive logging

### Functionality
- [ ] Precedence rule works correctly in all scenarios
- [ ] Guards enforce permissions on all protected endpoints
- [ ] Query endpoints return accurate permission information
- [ ] Multi-tenancy isolation works

### Testing
- [ ] Unit test coverage >90%
- [ ] All acceptance criteria covered by tests
- [ ] Integration tests pass
- [ ] No flaky tests

### Integration
- [ ] Controllers updated to use `@RequierePermiso`
- [ ] Old permission check code removed
- [ ] No breaking changes to existing APIs
- [ ] Backward compatibility maintained

### Documentation
- [ ] Technical documentation updated
- [ ] API specification updated
- [ ] Code comments comprehensive
- [ ] README updated if needed

---

**End of Implementation Plan**

This plan provides a complete, step-by-step guide for implementing US-ACL-006. Each step is detailed enough for a developer to follow autonomously. The implementation follows DDD principles, hexagonal architecture, and project best practices. All acceptance criteria from the original user story are covered through comprehensive testing.
