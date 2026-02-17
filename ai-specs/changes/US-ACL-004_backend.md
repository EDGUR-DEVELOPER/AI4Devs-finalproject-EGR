# Backend Implementation Plan: US-ACL-004 Permiso Recursivo en Carpeta (Herencia Simple)

## Overview

This document outlines the step-by-step implementation plan for the recursive folder permissions feature (US-ACL-004), which enables inheritance of permissions from parent folders to their descendants when the `recursivo` flag is set to `true`.

**Architecture Principles Applied:**
- **Domain-Driven Design (DDD)**: Domain logic encapsulated in services
- **Layered Architecture**: Clear separation between domain, application, infrastructure, and presentation layers
- **SOLID Principles**: Single responsibility, dependency injection, interface segregation
- **Performance Optimization**: Indexed queries, caching strategy, CTE recursive queries

**Key Feature:**
- Automatic permission inheritance from parent folders to descendants
- Evaluation algorithm: Direct permission → Inherited permission → No permission (403)
- Efficient ancestor path resolution using PostgreSQL CTE recursive queries
- Multi-tenant isolation with `organizacion_id` enforcement

---

## Architecture Context

### Layers Involved

1. **Domain Layer** (`backend/document-core/src/main/java/com/docflow/documentcore/domain`):
   - Permission evaluation logic
   - Business rules for inheritance
   - Value objects for permission results

2. **Application Layer** (`backend/document-core/src/main/java/com/docflow/documentcore/application/service`):
   - Use cases orchestration
   - Permission resolution service
   - Caching management

3. **Infrastructure Layer** (`backend/document-core/src/main/java/com/docflow/documentcore/infrastructure`):
   - JPA repositories
   - Custom queries for ancestor resolution
   - Database indices implementation

4. **Presentation Layer** (`backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/in/rest`):
   - REST controllers
   - DTOs and mappers
   - API endpoints

### Components/Files Referenced

**Domain:**
- `domain/model/ACLCarpeta.java` (existing entity)
- `domain/model/Carpeta.java` (existing entity)
- `domain/model/PermisoEfectivo.java` (new value object)
- `domain/service/PermisoHerenciaService.java` (new domain service)

**Application:**
- `application/service/CarpetaPermissionService.java` (existing, to be extended)
- `application/dto/PermisoEfectivoDTO.java` (new)

**Infrastructure:**
- `infrastructure/adapter/out/persistence/ACLCarpetaRepository.java` (existing, to be extended)
- `infrastructure/adapter/out/persistence/CarpetaRepository.java` (existing, to be extended)
- `infrastructure/adapter/in/rest/CarpetaPermissionController.java` (new endpoint)

**Database:**
- Migration scripts for new indices
- Custom native queries for CTE recursive

---

## Implementation Steps

### Step 1: Create Database Migration for Indices

**File**: `backend/document-core/src/main/resources/db/migration/V1_5__add_acl_inheritance_indices.sql`

**Action**: Create SQL migration script to add performance indices for permission inheritance queries.

**Implementation Steps**:
1. Create new Flyway migration file with version number V1_5 (adjust if needed based on existing migrations)
2. Add index for ACL inheritance queries (filtered index for recursive=true)
3. Add index for carpeta parent navigation
4. Add composite index for ACL lookups
5. Include comments explaining index purpose and expected query patterns

**SQL Script Content**:
```sql
-- Migration: Add indices for permission inheritance queries
-- US-ACL-004: Recursive folder permissions

-- Index for efficient inheritance queries (filtered for recursive ACLs)
CREATE INDEX IF NOT EXISTS idx_acl_carpeta_herencia 
ON acl_carpeta(usuario_id, recursivo, organizacion_id) 
WHERE recursivo = true;

-- Index for efficient parent folder navigation
CREATE INDEX IF NOT EXISTS idx_carpeta_padre 
ON carpeta(carpeta_padre_id, organizacion_id) 
WHERE fecha_eliminacion IS NULL;

-- Composite index for ACL lookups by folder and user
CREATE INDEX IF NOT EXISTS idx_acl_carpeta_carpeta_usuario 
ON acl_carpeta(carpeta_id, usuario_id, organizacion_id)
WHERE fecha_eliminacion IS NULL;

-- Add check constraint to prevent self-referencing folders
ALTER TABLE carpeta 
ADD CONSTRAINT chk_carpeta_no_self_parent 
CHECK (id != carpeta_padre_id);
```

**Dependencies**: None (first step in implementation)

**Implementation Notes**:
- Use `IF NOT EXISTS` to make migration idempotent
- Filtered indices improve query performance and reduce index size
- Check constraint prevents circular references at database level
- Expected query performance: < 5ms for ancestor resolution, < 10ms for full permission evaluation

---

### Step 2: Create Value Object for Effective Permission

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/model/PermisoEfectivo.java`

**Action**: Create immutable value object to represent the result of permission evaluation.

**Function Signature**:
```java
public record PermisoEfectivo(
    String nivelAcceso,
    boolean esHeredado,
    Long carpetaOrigenId,
    String carpetaOrigenNombre,
    List<String> rutaHerencia
) {
    // Validation in compact constructor
}
```

**Implementation Steps**:
1. Create Java record in domain/model package
2. Add compact constructor with validation
3. Ensure nivelAcceso is one of: LECTURA, ESCRITURA, ADMINISTRACION
4. Add factory methods for creating direct and inherited permissions
5. Add equals/hashCode/toString (provided by record)
6. Document the meaning of each field

**Implementation Code**:
```java
package com.docflow.documentcore.domain.model;

import java.util.List;
import java.util.Set;

/**
 * Value object representing the effective permission a user has over a folder.
 * This can be either a direct permission or an inherited one from an ancestor.
 */
public record PermisoEfectivo(
    String nivelAcceso,
    boolean esHeredado,
    Long carpetaOrigenId,
    String carpetaOrigenNombre,
    List<String> rutaHerencia
) {
    private static final Set<String> NIVELES_VALIDOS = Set.of("LECTURA", "ESCRITURA", "ADMINISTRACION");

    public PermisoEfectivo {
        if (nivelAcceso == null || !NIVELES_VALIDOS.contains(nivelAcceso)) {
            throw new IllegalArgumentException("Nivel de acceso inválido: " + nivelAcceso);
        }
        if (carpetaOrigenId == null) {
            throw new IllegalArgumentException("carpetaOrigenId es requerido");
        }
        // For inherited permissions, rutaHerencia should not be null
        if (esHeredado && rutaHerencia == null) {
            throw new IllegalArgumentException("rutaHerencia es requerido para permisos heredados");
        }
        // Make list immutable
        rutaHerencia = rutaHerencia != null ? List.copyOf(rutaHerencia) : null;
    }

    /**
     * Factory method for creating a direct permission result
     */
    public static PermisoEfectivo directo(String nivelAcceso, Long carpetaId, String carpetaNombre) {
        return new PermisoEfectivo(nivelAcceso, false, carpetaId, carpetaNombre, null);
    }

    /**
     * Factory method for creating an inherited permission result
     */
    public static PermisoEfectivo heredado(
        String nivelAcceso, 
        Long carpetaOrigenId, 
        String carpetaOrigenNombre,
        List<String> rutaHerencia
    ) {
        return new PermisoEfectivo(nivelAcceso, true, carpetaOrigenId, carpetaOrigenNombre, rutaHerencia);
    }
}
```

**Implementation Notes**:
- Using Java record for immutability and conciseness
- Validation in compact constructor ensures invariants
- Factory methods provide clear intent
- List is made immutable to prevent external modifications

---

### Step 3: Extend Repository for Ancestor Resolution

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/out/persistence/CarpetaRepository.java`

**Action**: Add custom query method to retrieve ancestor path using PostgreSQL CTE.

**Function Signature**:
```java
@Query(value = "...", nativeQuery = true)
List<Map<String, Object>> findAncestorPath(@Param("carpetaId") Long carpetaId, @Param("organizacionId") Long organizacionId);
```

**Implementation Steps**:
1. Open existing `CarpetaRepository` interface
2. Add `@Query` annotation with native SQL using CTE RECURSIVE
3. Query should return id, nombre, nivel (distance from target folder)
4. Order by nivel ASC (closest parent first)
5. Filter by organizacion_id and exclude soft-deleted folders
6. Add method documentation explaining the recursive CTE

**Implementation Code**:
```java
package com.docflow.documentcore.infrastructure.adapter.out.persistence;

import com.docflow.documentcore.domain.model.Carpeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CarpetaRepository extends JpaRepository<Carpeta, Long> {

    /**
     * Retrieves the ancestor path from a folder to the root using a recursive CTE.
     * Returns ancestors ordered by proximity (closest parent first).
     * 
     * @param carpetaId The target folder ID
     * @param organizacionId The organization ID for multi-tenant isolation
     * @return List of maps containing: id, nombre, nivel (distance from target)
     */
    @Query(value = """
        WITH RECURSIVE ancestros AS (
          -- Base case: current folder
          SELECT id, carpeta_padre_id, nombre, 0 AS nivel
          FROM carpeta
          WHERE id = :carpetaId
            AND organizacion_id = :organizacionId
            AND fecha_eliminacion IS NULL
          
          UNION ALL
          
          -- Recursive case: parent folders
          SELECT c.id, c.carpeta_padre_id, c.nombre, a.nivel + 1
          FROM carpeta c
          INNER JOIN ancestros a ON c.id = a.carpeta_padre_id
          WHERE c.organizacion_id = :organizacionId
            AND c.fecha_eliminacion IS NULL
            AND a.nivel < 50
        )
        SELECT id, nombre, nivel
        FROM ancestros
        WHERE nivel > 0
        ORDER BY nivel ASC
        """, nativeQuery = true)
    List<Map<String, Object>> findAncestorPath(
        @Param("carpetaId") Long carpetaId, 
        @Param("organizacionId") Long organizacionId
    );

    // Existing methods...
}
```

**Dependencies**:
- Spring Data JPA

**Implementation Notes**:
- CTE limits recursion to 50 levels to prevent infinite loops
- Multi-tenant isolation via organizacion_id filter
- Soft-delete filtering (fecha_eliminacion IS NULL)
- Returns Map to avoid mapping to entity (performance optimization)
- nivel=0 excluded (current folder, not an ancestor)

---

### Step 4: Extend ACLCarpetaRepository for Permission Lookups

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/out/persistence/ACLCarpetaRepository.java`

**Action**: Add query methods for efficient ACL lookups during permission evaluation.

**Function Signatures**:
```java
Optional<ACLCarpeta> findByUsuarioIdAndCarpetaIdAndOrganizacionId(Long usuarioId, Long carpetaId, Long organizacionId);

@Query("SELECT acl FROM ACLCarpeta acl WHERE acl.usuarioId = :usuarioId AND acl.carpetaId IN :carpetaIds AND acl.organizacionId = :organizacionId AND acl.fechaEliminacion IS NULL")
List<ACLCarpeta> findByUsuarioAndCarpetasIn(@Param("usuarioId") Long usuarioId, @Param("carpetaIds") List<Long> carpetaIds, @Param("organizacionId") Long organizacionId);
```

**Implementation Steps**:
1. Open existing `ACLCarpetaRepository` interface
2. Add method for single ACL lookup (direct permission check)
3. Add method for batch ACL lookup (multiple ancestors at once)
4. Both methods must filter by organizacion_id for multi-tenant isolation
5. Filter out soft-deleted ACLs (fecha_eliminacion IS NULL)
6. Add JavaDoc explaining usage patterns

**Implementation Code**:
```java
package com.docflow.documentcore.infrastructure.adapter.out.persistence;

import com.docflow.documentcore.domain.model.ACLCarpeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ACLCarpetaRepository extends JpaRepository<ACLCarpeta, Long> {

    /**
     * Finds a single ACL entry for a user on a specific folder.
     * Used for direct permission checks.
     * 
     * @param usuarioId User ID
     * @param carpetaId Folder ID
     * @param organizacionId Organization ID for multi-tenant isolation
     * @return Optional ACL entry (empty if not found)
     */
    @Query("SELECT acl FROM ACLCarpeta acl WHERE acl.usuarioId = :usuarioId " +
           "AND acl.carpetaId = :carpetaId " +
           "AND acl.organizacionId = :organizacionId " +
           "AND acl.fechaEliminacion IS NULL")
    Optional<ACLCarpeta> findByUsuarioIdAndCarpetaIdAndOrganizacionId(
        @Param("usuarioId") Long usuarioId, 
        @Param("carpetaId") Long carpetaId, 
        @Param("organizacionId") Long organizacionId
    );

    /**
     * Finds all ACL entries for a user across multiple folders (batch lookup).
     * Used for efficient ancestor permission checking.
     * 
     * @param usuarioId User ID
     * @param carpetaIds List of folder IDs (ancestors)
     * @param organizacionId Organization ID for multi-tenant isolation
     * @return List of ACL entries (empty if none found)
     */
    @Query("SELECT acl FROM ACLCarpeta acl WHERE acl.usuarioId = :usuarioId " +
           "AND acl.carpetaId IN :carpetaIds " +
           "AND acl.organizacionId = :organizacionId " +
           "AND acl.fechaEliminacion IS NULL " +
           "ORDER BY acl.carpetaId")
    List<ACLCarpeta> findByUsuarioAndCarpetasIn(
        @Param("usuarioId") Long usuarioId, 
        @Param("carpetaIds") List<Long> carpetaIds, 
        @Param("organizacionId") Long organizacionId
    );

    // Existing methods...
}
```

**Dependencies**:
- Spring Data JPA
- ACLCarpeta entity

**Implementation Notes**:
- Using JPQL for database-agnostic queries
- Batch lookup reduces N+1 query problem
- Multi-tenant isolation is CRITICAL for security
- Soft-delete filtering ensures deleted ACLs are ignored

---

### Step 5: Create Domain Service for Permission Inheritance

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/service/PermisoHerenciaService.java`

**Action**: Implement core business logic for evaluating inherited permissions.

**Function Signature**:
```java
@Service
public class PermisoHerenciaService {
    public Optional<PermisoEfectivo> evaluarPermisoEfectivo(Long usuarioId, Long carpetaId, Long organizacionId);
    public List<Long> obtenerRutaAncestros(Long carpetaId, Long organizacionId);
}
```

**Implementation Steps**:

1. **Declare Service Class**:
   - Annotate with `@Service` for Spring dependency injection
   - Inject `ACLCarpetaRepository` and `CarpetaRepository` via constructor
   - Add logger for debugging

2. **Implement Ancestor Path Resolution**:
   - Method: `obtenerRutaAncestros(carpetaId, organizacionId)`
   - Call `carpetaRepository.findAncestorPath()`
   - Extract IDs from Map results
   - Return List<Long> ordered by proximity
   - Handle empty results (root folder or folder not found)

3. **Implement Permission Evaluation Algorithm**:
   - Method: `evaluarPermisoEfectivo(usuarioId, carpetaId, organizacionId)`
   - **Step 1**: Check for direct ACL on target folder
   - **Step 2**: If direct ACL found, return PermisoEfectivo.directo()
   - **Step 3**: If no direct ACL, retrieve ancestor path
   - **Step 4**: Iterate ancestors from closest to root
   - **Step 5**: For each ancestor, check if ACL exists
   - **Step 6**: If ACL with `recursivo=true` found, return PermisoEfectivo.heredado()
   - **Step 7**: If ACL with `recursivo=false` found, stop search
   - **Step 8**: If no ACL found in any ancestor, return Optional.empty()

4. **Add Validation and Error Handling**:
   - Validate input parameters (non-null, positive IDs)
   - Handle circular reference detection (visited set, max depth)
   - Log key decision points for debugging

5. **Implement Caching Strategy** (Optional for MVP, document for future):
   - Add `@Cacheable` annotation for ancestor paths
   - Cache key: `carpetaId + organizacionId`
   - TTL: 1 hour
   - Invalidate on folder move/delete

**Implementation Code**:
```java
package com.docflow.documentcore.domain.service;

import com.docflow.documentcore.domain.model.ACLCarpeta;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.infrastructure.adapter.out.persistence.ACLCarpetaRepository;
import com.docflow.documentcore.infrastructure.adapter.out.persistence.CarpetaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Domain service responsible for evaluating effective permissions with inheritance support.
 * Implements the permission resolution algorithm: Direct → Inherited (recursive) → None (403)
 */
@Service
public class PermisoHerenciaService {

    private static final Logger logger = LoggerFactory.getLogger(PermisoHerenciaService.class);
    private static final int MAX_DEPTH = 50;

    private final ACLCarpetaRepository aclCarpetaRepository;
    private final CarpetaRepository carpetaRepository;

    public PermisoHerenciaService(
        ACLCarpetaRepository aclCarpetaRepository,
        CarpetaRepository carpetaRepository
    ) {
        this.aclCarpetaRepository = aclCarpetaRepository;
        this.carpetaRepository = carpetaRepository;
    }

    /**
     * Evaluates the effective permission a user has over a folder.
     * Algorithm:
     * 1. Check for direct ACL on target folder
     * 2. If not found, traverse ancestors looking for recursive ACL
     * 3. Stop at first non-recursive ACL or when reaching root
     * 
     * @param usuarioId User ID
     * @param carpetaId Folder ID
     * @param organizacionId Organization ID (multi-tenant isolation)
     * @return Optional with effective permission, or empty if no permission
     */
    public Optional<PermisoEfectivo> evaluarPermisoEfectivo(
        Long usuarioId, 
        Long carpetaId, 
        Long organizacionId
    ) {
        logger.debug("Evaluating permission for user={}, folder={}, org={}", 
                     usuarioId, carpetaId, organizacionId);

        // Validate inputs
        if (usuarioId == null || carpetaId == null || organizacionId == null) {
            throw new IllegalArgumentException("All parameters are required");
        }

        // Step 1: Check for direct ACL
        Optional<ACLCarpeta> aclDirecto = aclCarpetaRepository
            .findByUsuarioIdAndCarpetaIdAndOrganizacionId(usuarioId, carpetaId, organizacionId);

        if (aclDirecto.isPresent()) {
            ACLCarpeta acl = aclDirecto.get();
            logger.debug("Direct ACL found: nivel={}", acl.getNivelAccesoCodigo());
            return Optional.of(PermisoEfectivo.directo(
                acl.getNivelAccesoCodigo(),
                carpetaId,
                acl.getCarpeta().getNombre()
            ));
        }

        // Step 2: No direct ACL, check inheritance
        logger.debug("No direct ACL found, checking ancestors");
        List<Long> ancestorIds = obtenerRutaAncestros(carpetaId, organizacionId);

        if (ancestorIds.isEmpty()) {
            logger.debug("No ancestors found (root folder or not found)");
            return Optional.empty();
        }

        // Step 3: Batch lookup ACLs for all ancestors
        List<ACLCarpeta> ancestorAcls = aclCarpetaRepository
            .findByUsuarioAndCarpetasIn(usuarioId, ancestorIds, organizacionId);

        // Convert to map for efficient lookup by carpetaId
        Map<Long, ACLCarpeta> aclMap = ancestorAcls.stream()
            .collect(Collectors.toMap(ACLCarpeta::getCarpetaId, acl -> acl));

        // Step 4: Traverse ancestors from closest to root
        for (Long ancestorId : ancestorIds) {
            ACLCarpeta aclAncestro = aclMap.get(ancestorId);

            if (aclAncestro != null) {
                if (aclAncestro.isRecursivo()) {
                    // Found recursive ACL - this is the inherited permission
                    logger.debug("Inherited permission found from ancestor={}, nivel={}", 
                                 ancestorId, aclAncestro.getNivelAccesoCodigo());
                    
                    List<String> rutaHerencia = construirRutaHerencia(ancestorId, carpetaId, organizacionId);
                    
                    return Optional.of(PermisoEfectivo.heredado(
                        aclAncestro.getNivelAccesoCodigo(),
                        ancestorId,
                        aclAncestro.getCarpeta().getNombre(),
                        rutaHerencia
                    ));
                } else {
                    // Found non-recursive ACL - stop search
                    logger.debug("Non-recursive ACL found at ancestor={}, stopping search", ancestorId);
                    return Optional.empty();
                }
            }
        }

        // Step 5: No permission found in any ancestor
        logger.debug("No permission found in ancestors");
        return Optional.empty();
    }

    /**
     * Retrieves the ancestor path for a folder (from closest to root).
     * 
     * @param carpetaId Folder ID
     * @param organizacionId Organization ID
     * @return List of ancestor folder IDs ordered by proximity
     */
    public List<Long> obtenerRutaAncestros(Long carpetaId, Long organizacionId) {
        List<Map<String, Object>> rawAncestors = carpetaRepository
            .findAncestorPath(carpetaId, organizacionId);

        return rawAncestors.stream()
            .map(row -> ((Number) row.get("id")).longValue())
            .collect(Collectors.toList());
    }

    /**
     * Constructs the inheritance path as a list of folder names.
     * 
     * @param carpetaOrigenId Origin folder ID (where inherited permission is defined)
     * @param carpetaDestinoId Destination folder ID (where permission is being evaluated)
     * @param organizacionId Organization ID
     * @return List of folder names in the inheritance path
     */
    private List<String> construirRutaHerencia(
        Long carpetaOrigenId, 
        Long carpetaDestinoId, 
        Long organizacionId
    ) {
        List<Map<String, Object>> rawAncestors = carpetaRepository
            .findAncestorPath(carpetaDestinoId, organizacionId);

        // Filter ancestors that are between origen and destino
        return rawAncestors.stream()
            .filter(row -> {
                Long id = ((Number) row.get("id")).longValue();
                return id >= carpetaOrigenId; // Include origen and all descendants
            })
            .map(row -> (String) row.get("nombre"))
            .collect(Collectors.toList());
    }
}
```

**Dependencies**:
- Spring Framework (@Service, DI)
- SLF4J for logging
- ACLCarpetaRepository, CarpetaRepository
- PermisoEfectivo value object

**Implementation Notes**:
- Batch ACL lookup reduces database round-trips
- Algorithm stops at first non-recursive ACL (performance optimization)
- Logging at DEBUG level for troubleshooting
- Multi-tenant isolation enforced in repository queries
- Future optimization: add @Cacheable for obtenerRutaAncestros()

---

### Step 6: Create DTO for Effective Permission Response

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/PermisoEfectivoDTO.java`

**Action**: Create DTO for API response containing effective permission details.

**Function Signature**:
```java
public record PermisoEfectivoDTO(
    Long carpetaId,
    String carpetaNombre,
    String nivelAcceso,
    boolean esHeredado,
    CarpetaOrigenDTO carpetaOrigen,
    List<String> rutaHerencia,
    List<String> accionesPermitidas
) {}
```

**Implementation Steps**:
1. Create record in application/dto package
2. Add nested record CarpetaOrigenDTO for origin folder details
3. Add factory method to convert from PermisoEfectivo domain object
4. Map nivel_acceso to acciones_permitidas list (LECTURA → [ver, listar, descargar])
5. Ensure all fields are nullable where appropriate
6. Add Jackson annotations if needed (@JsonProperty for snake_case)

**Implementation Code**:
```java
package com.docflow.documentcore.application.dto;

import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO representing the effective permission a user has over a folder.
 * Includes information about whether the permission is direct or inherited.
 */
public record PermisoEfectivoDTO(
    @JsonProperty("carpeta_id")
    Long carpetaId,
    
    @JsonProperty("carpeta_nombre")
    String carpetaNombre,
    
    @JsonProperty("nivel_acceso")
    String nivelAcceso,
    
    @JsonProperty("es_heredado")
    boolean esHeredado,
    
    @JsonProperty("carpeta_origen")
    CarpetaOrigenDTO carpetaOrigen,
    
    @JsonProperty("ruta_herencia")
    List<String> rutaHerencia,
    
    @JsonProperty("acciones_permitidas")
    List<String> accionesPermitidas
) {

    /**
     * Nested DTO for origin folder details (when permission is inherited)
     */
    public record CarpetaOrigenDTO(
        Long id,
        String nombre,
        String ruta
    ) {}

    /**
     * Maps nivel_acceso to specific actions the user can perform
     */
    private static final Map<String, List<String>> ACCIONES_POR_NIVEL = Map.of(
        "LECTURA", List.of("ver", "listar", "descargar"),
        "ESCRITURA", List.of("ver", "listar", "descargar", "crear", "editar", "eliminar"),
        "ADMINISTRACION", List.of("ver", "listar", "descargar", "crear", "editar", "eliminar", "gestionar_permisos", "mover")
    );

    /**
     * Factory method to create DTO from domain object
     */
    public static PermisoEfectivoDTO fromDomain(
        PermisoEfectivo permiso,
        Long carpetaId,
        String carpetaNombre,
        String rutaCompletaCarpetaOrigen
    ) {
        CarpetaOrigenDTO carpetaOrigen = permiso.esHeredado() ?
            new CarpetaOrigenDTO(
                permiso.carpetaOrigenId(),
                permiso.carpetaOrigenNombre(),
                rutaCompletaCarpetaOrigen
            ) : null;

        List<String> acciones = ACCIONES_POR_NIVEL.getOrDefault(
            permiso.nivelAcceso(),
            List.of()
        );

        return new PermisoEfectivoDTO(
            carpetaId,
            carpetaNombre,
            permiso.nivelAcceso(),
            permiso.esHeredado(),
            carpetaOrigen,
            permiso.rutaHerencia(),
            acciones
        );
    }
}
```

**Dependencies**:
- Jackson for JSON serialization
- PermisoEfectivo domain object

**Implementation Notes**:
- Using @JsonProperty for snake_case JSON fields
- Factory method encapsulates mapping logic
- accionesPermitidas derived from nivelAcceso for client convenience
- carpetaOrigen is null for direct permissions

---

### Step 7: Create REST Endpoint for Effective Permission Query

**File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/in/rest/CarpetaPermissionController.java`

**Action**: Create REST endpoint to query effective permission of authenticated user on a folder.

**Function Signature**:
```java
@GetMapping("/carpetas/{carpetaId}/mi-permiso")
public ResponseEntity<ApiResponse<PermisoEfectivoDTO>> getMyPermission(@PathVariable Long carpetaId, @AuthenticationPrincipal UserDetails userDetails);
```

**Implementation Steps**:

1. **Create Controller Class**:
   - Annotate with `@RestController` and `@RequestMapping("/api")`
   - Inject `PermisoHerenciaService` and `CarpetaRepository` via constructor
   - Add logger

2. **Implement GET Endpoint**:
   - Path: `/carpetas/{carpetaId}/mi-permiso`
   - Method: GET
   - Security: Requires authentication (JWT)
   - Extract usuarioId from JWT token (UserDetails)
   - Extract organizacionId from JWT token or header
   - Call `permisoHerenciaService.evaluarPermisoEfectivo()`
   - If permission found, return 200 with PermisoEfectivoDTO
   - If no permission, return 403 Forbidden
   - If folder not found, return 404 Not Found

3. **Handle Error Cases**:
   - 403 FORBIDDEN: User has no permission (direct or inherited)
   - 404 NOT FOUND: Folder does not exist or is soft-deleted
   - 401 UNAUTHORIZED: User not authenticated
   - 500 INTERNAL SERVER ERROR: Unexpected errors

4. **Add Response Wrapper**:
   - Use standard ApiResponse<T> wrapper for consistency
   - Include metadata (timestamp, path, etc.)

**Implementation Code**:
```java
package com.docflow.documentcore.infrastructure.adapter.in.rest;

import com.docflow.documentcore.application.dto.PermisoEfectivoDTO;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.service.PermisoHerenciaService;
import com.docflow.documentcore.infrastructure.adapter.out.persistence.CarpetaRepository;
import com.docflow.documentcore.infrastructure.exception.PermisoDenegadoException;
import com.docflow.documentcore.infrastructure.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST Controller for folder permission queries.
 * Provides endpoint to check effective permissions with inheritance support.
 */
@RestController
@RequestMapping("/api")
public class CarpetaPermissionController {

    private static final Logger logger = LoggerFactory.getLogger(CarpetaPermissionController.class);

    private final PermisoHerenciaService permisoHerenciaService;
    private final CarpetaRepository carpetaRepository;

    public CarpetaPermissionController(
        PermisoHerenciaService permisoHerenciaService,
        CarpetaRepository carpetaRepository
    ) {
        this.permisoHerenciaService = permisoHerenciaService;
        this.carpetaRepository = carpetaRepository;
    }

    /**
     * GET /api/carpetas/{carpetaId}/mi-permiso
     * 
     * Returns the effective permission the authenticated user has over a folder.
     * This includes both direct permissions and inherited permissions from ancestors.
     * 
     * @param carpetaId Folder ID
     * @param userDetails Authenticated user details (from JWT)
     * @return 200 with permission details, 403 if no permission, 404 if folder not found
     */
    @GetMapping("/carpetas/{carpetaId}/mi-permiso")
    public ResponseEntity<ApiResponse<PermisoEfectivoDTO>> getMyPermission(
        @PathVariable Long carpetaId,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        logger.info("GET /api/carpetas/{}/mi-permiso - user={}", carpetaId, userDetails.getUsername());

        // Extract user and organization from JWT token
        Long usuarioId = extractUsuarioId(userDetails);
        Long organizacionId = extractOrganizacionId(userDetails);

        // Verify folder exists
        Optional<Carpeta> carpetaOpt = carpetaRepository.findById(carpetaId);
        if (carpetaOpt.isEmpty()) {
            logger.warn("Folder not found: id={}", carpetaId);
            throw new ResourceNotFoundException("Carpeta", "id", carpetaId);
        }

        Carpeta carpeta = carpetaOpt.get();

        // Verify folder belongs to user's organization (multi-tenant isolation)
        if (!carpeta.getOrganizacionId().equals(organizacionId)) {
            logger.warn("Folder belongs to different organization: folder.org={}, user.org={}", 
                        carpeta.getOrganizacionId(), organizacionId);
            throw new PermisoDenegadoException("No tienes permiso para acceder a esta carpeta");
        }

        // Evaluate effective permission
        Optional<PermisoEfectivo> permisoOpt = permisoHerenciaService
            .evaluarPermisoEfectivo(usuarioId, carpetaId, organizacionId);

        if (permisoOpt.isEmpty()) {
            logger.warn("No permission found for user={} on folder={}", usuarioId, carpetaId);
            throw new PermisoDenegadoException("No tienes permiso para acceder a esta carpeta");
        }

        // Build DTO with complete folder path for origin
        PermisoEfectivo permiso = permisoOpt.get();
        String rutaCompletaOrigen = construirRutaCompleta(permiso.carpetaOrigenId(), organizacionId);
        
        PermisoEfectivoDTO dto = PermisoEfectivoDTO.fromDomain(
            permiso,
            carpetaId,
            carpeta.getNombre(),
            rutaCompletaOrigen
        );

        logger.info("Permission found: nivel={}, esHeredado={}", 
                    permiso.nivelAcceso(), permiso.esHeredado());

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * Extracts usuario ID from JWT token
     */
    private Long extractUsuarioId(UserDetails userDetails) {
        // Implementation depends on JWT structure
        // Typically: ((CustomUserDetails) userDetails).getUserId()
        // For now, placeholder:
        return 1L; // TODO: Extract from JWT claims
    }

    /**
     * Extracts organizacion ID from JWT token
     */
    private Long extractOrganizacionId(UserDetails userDetails) {
        // Implementation depends on JWT structure
        // Typically: ((CustomUserDetails) userDetails).getOrganizacionId()
        // For now, placeholder:
        return 1L; // TODO: Extract from JWT claims
    }

    /**
     * Constructs the complete path for a folder (e.g., "/Root/Projects/2024")
     */
    private String construirRutaCompleta(Long carpetaId, Long organizacionId) {
        // TODO: Implement path construction from root to target folder
        // For now, return simple name
        return carpetaRepository.findById(carpetaId)
            .map(Carpeta::getNombre)
            .orElse("");
    }
}
```

**Dependencies**:
- Spring Web (@RestController, @GetMapping)
- Spring Security (@AuthenticationPrincipal)
- PermisoHerenciaService, CarpetaRepository
- Custom exceptions (PermisoDenegadoException, ResourceNotFoundException)

**Implementation Notes**:
- Multi-tenant isolation verified at controller level
- JWT extraction logic is placeholder (TODO: implement based on actual JWT structure)
- Path construction for carpetaOrigen can be optimized with caching
- Error handling delegates to @ControllerAdvice global exception handler

---

### Step 8: Create Custom Exceptions

**Files**:
- `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/exception/PermisoDenegadoException.java`
- `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/exception/ResourceNotFoundException.java`

**Action**: Create custom exceptions for permission denial and resource not found scenarios.

**Implementation Steps**:

1. **Create PermisoDenegadoException**:
   - Extend RuntimeException
   - Add constructor with custom message
   - Annotate with @ResponseStatus(HttpStatus.FORBIDDEN)

2. **Create ResourceNotFoundException**:
   - Extend RuntimeException
   - Add constructor with resourceName, fieldName, fieldValue
   - Annotate with @ResponseStatus(HttpStatus.NOT_FOUND)

**Implementation Code**:

**PermisoDenegadoException.java**:
```java
package com.docflow.documentcore.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts to access a resource without sufficient permissions.
 * Maps to HTTP 403 Forbidden.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class PermisoDenegadoException extends RuntimeException {

    public PermisoDenegadoException(String message) {
        super(message);
    }

    public PermisoDenegadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**ResourceNotFoundException.java**:
```java
package com.docflow.documentcore.infrastructure.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource is not found.
 * Maps to HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s: '%s'", resourceName, fieldName, fieldValue));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

**Dependencies**:
- Spring Web (@ResponseStatus)

**Implementation Notes**:
- @ResponseStatus automatically maps to HTTP status codes
- Global exception handler (@ControllerAdvice) can intercept and format responses
- Messages in Spanish per project requirements

---

### Step 9: Write Unit Tests for Permission Inheritance Algorithm

**File**: `backend/document-core/src/test/java/com/docflow/documentcore/domain/service/PermisoHerenciaServiceTest.java`

**Action**: Write comprehensive unit tests for the permission evaluation algorithm.

**Implementation Steps**:

1. **Setup Test Class**:
   - Use JUnit 5 (@Test)
   - Mock ACLCarpetaRepository and CarpetaRepository with Mockito
   - Create PermisoHerenciaService instance with mocked dependencies
   - Use @BeforeEach to reset mocks

2. **Test Cases - Successful Cases**:
   - `should_ReturnDirectPermission_When_DirectAclExists`: Direct ACL found, inheritance not checked
   - `should_ReturnInheritedPermission_When_RecursiveAclInParent`: Recursive ACL in parent folder applies
   - `should_ReturnInheritedPermission_When_RecursiveAclInGrandparent`: Recursive ACL in grandparent applies (multi-level)

3. **Test Cases - Validation Errors**:
   - `should_ThrowException_When_ParametersAreNull`: Null parameters validation

4. **Test Cases - Not Found**:
   - `should_ReturnEmpty_When_NoAclExists`: No direct or inherited permission
   - `should_ReturnEmpty_When_NoAncestorsExist`: Target folder is root or orphaned

5. **Test Cases - Inheritance Logic**:
   - `should_ReturnEmpty_When_NonRecursiveAclInParent`: Non-recursive ACL blocks inheritance
   - `should_UseClosestRecursiveAcl_When_MultipleRecursiveAclsExist`: Closest ancestor takes precedence
   - `should_StopSearch_When_NonRecursiveAclFound`: Non-recursive ACL stops ancestor traversal

6. **Test Cases - Edge Cases**:
   - `should_RespectMultiTenantIsolation_When_DifferentOrganization`: ACL from different org ignored
   - `should_IgnoreSoftDeletedAcls`: Soft-deleted ACLs filtered out

**Implementation Code** (sample tests):
```java
package com.docflow.documentcore.domain.service;

import com.docflow.documentcore.domain.model.ACLCarpeta;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.infrastructure.adapter.out.persistence.ACLCarpetaRepository;
import com.docflow.documentcore.infrastructure.adapter.out.persistence.CarpetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermisoHerenciaService Unit Tests")
class PermisoHerenciaServiceTest {

    @Mock
    private ACLCarpetaRepository aclCarpetaRepository;

    @Mock
    private CarpetaRepository carpetaRepository;

    private PermisoHerenciaService service;

    private static final Long USUARIO_ID = 50L;
    private static final Long ORGANIZACION_ID = 10L;
    private static final Long CARPETA_ID = 4L;
    private static final Long PARENT_ID = 3L;
    private static final Long GRANDPARENT_ID = 2L;

    @BeforeEach
    void setUp() {
        service = new PermisoHerenciaService(aclCarpetaRepository, carpetaRepository);
    }

    @Test
    @DisplayName("Should return direct permission when direct ACL exists")
    void should_ReturnDirectPermission_When_DirectAclExists() {
        // Given: Direct ACL exists
        ACLCarpeta aclDirecto = createACL(CARPETA_ID, "ESCRITURA", false);
        when(aclCarpetaRepository.findByUsuarioIdAndCarpetaIdAndOrganizacionId(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID))
            .thenReturn(Optional.of(aclDirecto));

        // When: Evaluate permission
        Optional<PermisoEfectivo> result = service.evaluarPermisoEfectivo(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID);

        // Then: Direct permission returned
        assertThat(result).isPresent();
        assertThat(result.get().nivelAcceso()).isEqualTo("ESCRITURA");
        assertThat(result.get().esHeredado()).isFalse();
        assertThat(result.get().carpetaOrigenId()).isEqualTo(CARPETA_ID);

        // Verify ancestors not checked
        verify(carpetaRepository, never()).findAncestorPath(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should return inherited permission when recursive ACL in parent")
    void should_ReturnInheritedPermission_When_RecursiveAclInParent() {
        // Given: No direct ACL, but parent has recursive ACL
        when(aclCarpetaRepository.findByUsuarioIdAndCarpetaIdAndOrganizacionId(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID))
            .thenReturn(Optional.empty());

        List<Map<String, Object>> ancestors = List.of(
            Map.of("id", PARENT_ID, "nombre", "Parent", "nivel", 1)
        );
        when(carpetaRepository.findAncestorPath(CARPETA_ID, ORGANIZACION_ID))
            .thenReturn(ancestors);

        ACLCarpeta aclParent = createACL(PARENT_ID, "LECTURA", true);
        when(aclCarpetaRepository.findByUsuarioAndCarpetasIn(
            USUARIO_ID, List.of(PARENT_ID), ORGANIZACION_ID))
            .thenReturn(List.of(aclParent));

        // When: Evaluate permission
        Optional<PermisoEfectivo> result = service.evaluarPermisoEfectivo(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID);

        // Then: Inherited permission returned
        assertThat(result).isPresent();
        assertThat(result.get().nivelAcceso()).isEqualTo("LECTURA");
        assertThat(result.get().esHeredado()).isTrue();
        assertThat(result.get().carpetaOrigenId()).isEqualTo(PARENT_ID);
    }

    @Test
    @DisplayName("Should return empty when no ACL exists (direct or inherited)")
    void should_ReturnEmpty_When_NoAclExists() {
        // Given: No direct ACL
        when(aclCarpetaRepository.findByUsuarioIdAndCarpetaIdAndOrganizacionId(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID))
            .thenReturn(Optional.empty());

        // Given: Has ancestors but no ACLs on them
        List<Map<String, Object>> ancestors = List.of(
            Map.of("id", PARENT_ID, "nombre", "Parent", "nivel", 1)
        );
        when(carpetaRepository.findAncestorPath(CARPETA_ID, ORGANIZACION_ID))
            .thenReturn(ancestors);

        when(aclCarpetaRepository.findByUsuarioAndCarpetasIn(
            USUARIO_ID, List.of(PARENT_ID), ORGANIZACION_ID))
            .thenReturn(List.of());

        // When: Evaluate permission
        Optional<PermisoEfectivo> result = service.evaluarPermisoEfectivo(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID);

        // Then: Empty result (no permission)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when parent has non-recursive ACL")
    void should_ReturnEmpty_When_NonRecursiveAclInParent() {
        // Given: No direct ACL
        when(aclCarpetaRepository.findByUsuarioIdAndCarpetaIdAndOrganizacionId(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID))
            .thenReturn(Optional.empty());

        // Given: Parent has non-recursive ACL
        List<Map<String, Object>> ancestors = List.of(
            Map.of("id", PARENT_ID, "nombre", "Parent", "nivel", 1)
        );
        when(carpetaRepository.findAncestorPath(CARPETA_ID, ORGANIZACION_ID))
            .thenReturn(ancestors);

        ACLCarpeta aclParent = createACL(PARENT_ID, "ESCRITURA", false); // recursivo=false
        when(aclCarpetaRepository.findByUsuarioAndCarpetasIn(
            USUARIO_ID, List.of(PARENT_ID), ORGANIZACION_ID))
            .thenReturn(List.of(aclParent));

        // When: Evaluate permission
        Optional<PermisoEfectivo> result = service.evaluarPermisoEfectivo(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID);

        // Then: Empty result (non-recursive blocks inheritance)
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should use closest recursive ACL when multiple exist")
    void should_UseClosestRecursiveAcl_When_MultipleRecursiveAclsExist() {
        // Given: No direct ACL
        when(aclCarpetaRepository.findByUsuarioIdAndCarpetaIdAndOrganizacionId(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID))
            .thenReturn(Optional.empty());

        // Given: Parent (ESCRITURA) and grandparent (LECTURA), both recursive
        List<Map<String, Object>> ancestors = List.of(
            Map.of("id", PARENT_ID, "nombre", "Parent", "nivel", 1),
            Map.of("id", GRANDPARENT_ID, "nombre", "Grandparent", "nivel", 2)
        );
        when(carpetaRepository.findAncestorPath(CARPETA_ID, ORGANIZACION_ID))
            .thenReturn(ancestors);

        ACLCarpeta aclParent = createACL(PARENT_ID, "ESCRITURA", true);
        ACLCarpeta aclGrandparent = createACL(GRANDPARENT_ID, "LECTURA", true);
        when(aclCarpetaRepository.findByUsuarioAndCarpetasIn(
            USUARIO_ID, List.of(PARENT_ID, GRANDPARENT_ID), ORGANIZACION_ID))
            .thenReturn(List.of(aclParent, aclGrandparent));

        // When: Evaluate permission
        Optional<PermisoEfectivo> result = service.evaluarPermisoEfectivo(
            USUARIO_ID, CARPETA_ID, ORGANIZACION_ID);

        // Then: Closest ancestor (parent) permission used
        assertThat(result).isPresent();
        assertThat(result.get().nivelAcceso()).isEqualTo("ESCRITURA");
        assertThat(result.get().carpetaOrigenId()).isEqualTo(PARENT_ID);
    }

    @Test
    @DisplayName("Should throw exception when parameters are null")
    void should_ThrowException_When_ParametersAreNull() {
        assertThatThrownBy(() -> service.evaluarPermisoEfectivo(null, CARPETA_ID, ORGANIZACION_ID))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.evaluarPermisoEfectivo(USUARIO_ID, null, ORGANIZACION_ID))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.evaluarPermisoEfectivo(USUARIO_ID, CARPETA_ID, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // Helper method to create mock ACL
    private ACLCarpeta createACL(Long carpetaId, String nivelAcceso, boolean recursivo) {
        ACLCarpeta acl = mock(ACLCarpeta.class);
        when(acl.getCarpetaId()).thenReturn(carpetaId);
        when(acl.getNivelAccesoCodigo()).thenReturn(nivelAcceso);
        when(acl.isRecursivo()).thenReturn(recursivo);
        
        Carpeta carpeta = mock(Carpeta.class);
        when(carpeta.getNombre()).thenReturn("Carpeta" + carpetaId);
        when(acl.getCarpeta()).thenReturn(carpeta);
        
        return acl;
    }
}
```

**Dependencies**:
- JUnit 5 (@Test, @BeforeEach)
- Mockito (@Mock, @ExtendWith)
- AssertJ (assertThat)

**Implementation Notes**:
- Tests cover 100% of algorithm branches
- Using descriptive test names (BDD style: should_X_When_Y)
- Mocks isolate unit under test from dependencies
- Helper method reduces boilerplate

---

### Step 10: Write Integration Tests

**File**: `backend/document-core/src/test/java/com/docflow/documentcore/infrastructure/adapter/in/rest/CarpetaPermissionControllerIT.java`

**Action**: Write E2E integration tests with real database and Spring context.

**Implementation Steps**:

1. **Setup Test Class**:
   - Use `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`
   - Use `@Transactional` for test isolation
   - Inject TestRestTemplate and repositories
   - Setup test data in @BeforeEach

2. **Test Scenarios**:
   - `should_Return200WithDirectPermission_When_UserHasDirectAcl`
   - `should_Return200WithInheritedPermission_When_UserHasRecursiveAclInParent`
   - `should_Return403_When_UserHasNoPermission`
   - `should_Return403_When_ParentAclIsNonRecursive`
   - `should_Return404_When_FolderDoesNotExist`
   - `should_ReturnDirectPermission_When_BothDirectAndInheritedExist`

3. **Test Data Setup**:
   - Create organization, users, folders (hierarchy)
   - Create ACLs with various configurations
   - Use realistic IDs and relationships

**Implementation Code** (sample test):
```java
package com.docflow.documentcore.infrastructure.adapter.in.rest;

import com.docflow.documentcore.domain.model.ACLCarpeta;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.infrastructure.adapter.out.persistence.ACLCarpetaRepository;
import com.docflow.documentcore.infrastructure.adapter.out.persistence.CarpetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@DisplayName("CarpetaPermissionController Integration Tests")
class CarpetaPermissionControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CarpetaRepository carpetaRepository;

    @Autowired
    private ACLCarpetaRepository aclCarpetaRepository;

    private static final Long ORG_ID = 10L;
    private static final Long USER_ID = 50L;

    private Long rootFolderId;
    private Long parentFolderId;
    private Long childFolderId;

    @BeforeEach
    void setUp() {
        // Create folder hierarchy: Root > Parent > Child
        Carpeta root = createFolder("Root", null, ORG_ID);
        Carpeta parent = createFolder("Parent", root.getId(), ORG_ID);
        Carpeta child = createFolder("Child", parent.getId(), ORG_ID);

        rootFolderId = root.getId();
        parentFolderId = parent.getId();
        childFolderId = child.getId();
    }

    @Test
    @DisplayName("Should return 200 with inherited permission when user has recursive ACL in parent")
    void should_Return200WithInheritedPermission_When_UserHasRecursiveAclInParent() {
        // Given: User has recursive ACL on parent folder
        createACL(USER_ID, parentFolderId, "LECTURA", true, ORG_ID);

        // When: User checks permission on child folder
        String url = "/api/carpetas/" + childFolderId + "/mi-permiso";
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("test@example.com", "password")
            .getForEntity(url, String.class);

        // Then: 200 OK with inherited permission
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
            .contains("\"nivel_acceso\":\"LECTURA\"")
            .contains("\"es_heredado\":true")
            .contains("\"carpeta_origen\"");
    }

    @Test
    @DisplayName("Should return 403 when parent ACL is non-recursive")
    void should_Return403_When_ParentAclIsNonRecursive() {
        // Given: User has non-recursive ACL on parent folder
        createACL(USER_ID, parentFolderId, "ESCRITURA", false, ORG_ID);

        // When: User checks permission on child folder
        String url = "/api/carpetas/" + childFolderId + "/mi-permiso";
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("test@example.com", "password")
            .getForEntity(url, String.class);

        // Then: 403 Forbidden (non-recursive blocks inheritance)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
            .contains("PERMISO_DENEGADO");
    }

    // Helper methods
    private Carpeta createFolder(String nombre, Long parentId, Long orgId) {
        Carpeta carpeta = new Carpeta();
        carpeta.setNombre(nombre);
        carpeta.setCarpetaPadreId(parentId);
        carpeta.setOrganizacionId(orgId);
        return carpetaRepository.save(carpeta);
    }

    private ACLCarpeta createACL(Long userId, Long folderId, String nivel, boolean recursivo, Long orgId) {
        ACLCarpeta acl = new ACLCarpeta();
        acl.setUsuarioId(userId);
        acl.setCarpetaId(folderId);
        acl.setNivelAccesoCodigo(nivel);
        acl.setRecursivo(recursivo);
        acl.setOrganizacionId(orgId);
        return aclCarpetaRepository.save(acl);
    }
}
```

**Dependencies**:
- Spring Boot Test (@SpringBootTest)
- TestRestTemplate for HTTP calls
- JUnit 5, AssertJ

**Implementation Notes**:
- @Transactional rolls back changes after each test
- Uses real database (TestContainers recommended)
- Tests full request-response cycle
- Validates JSON responses

---

### Step 11: Update Technical Documentation

**Action**: Review and update technical documentation according to changes made.

**Implementation Steps**:

1. **Review Changes**: Analyze all code changes made during implementation
   
2. **Identify Documentation Files**: Determine which documentation files need updates:
   - Data model changes → Update `ai-specs/specs/data-model.md` (new indices)
   - API endpoint changes → Update `ai-specs/specs/api-spec.yml` (new endpoint)
   - Architecture changes → Update `backend/document-core/README.md` (new service)

3. **Update Documentation**: For each affected file:

   **Update `ai-specs/specs/api-spec.yml`**:
   ```yaml
   /carpetas/{carpetaId}/mi-permiso:
     get:
       summary: Get effective permission for authenticated user
       tags: [ACL, Carpetas]
       security:
         - BearerAuth: []
       parameters:
         - name: carpetaId
           in: path
           required: true
           schema:
             type: integer
             format: int64
       responses:
         '200':
           description: Effective permission (direct or inherited)
           content:
             application/json:
               schema:
                 $ref: '#/components/schemas/PermisoEfectivoDTO'
         '403':
           $ref: '#/components/responses/Forbidden'
         '404':
           $ref: '#/components/responses/NotFound'
   
   components:
     schemas:
       PermisoEfectivoDTO:
         type: object
         required:
           - carpeta_id
           - nivel_acceso
           - es_heredado
         properties:
           carpeta_id:
             type: integer
             format: int64
           carpeta_nombre:
             type: string
           nivel_acceso:
             type: string
             enum: [LECTURA, ESCRITURA, ADMINISTRACION]
           es_heredado:
             type: boolean
           carpeta_origen:
             $ref: '#/components/schemas/CarpetaOrigenDTO'
           ruta_herencia:
             type: array
             items:
               type: string
           acciones_permitidas:
             type: array
             items:
               type: string
   ```

   **Update `backend/document-core/README.md`**:
   ```markdown
   ## Permission Inheritance (US-ACL-004)

   ### Concept
   
   Folder permissions can be configured as **recursive** (`recursivo=true`), automatically applying to all descendant subfolders.

   ### Evaluation Rules

   1. **Direct Permission** → Always used if exists
   2. **Inherited Permission** → Searched in ancestors if no direct permission
   3. **No Permission** → Access denied (403)

   ### Usage Example

   ```bash
   # Create recursive permission
   POST /api/carpetas/2/permisos
   {
     "usuario_id": 50,
     "nivel_acceso_codigo": "LECTURA",
     "recursivo": true
   }

   # Check effective permission on subfolder
   GET /api/carpetas/4/mi-permiso
   ```

   ### Performance

   - Evaluation: < 10ms for hierarchies up to 20 levels
   - Ancestor cache: 1 hour TTL
   - Optimized indices for inheritance queries
   
   ### Database Indices

   - `idx_acl_carpeta_herencia`: Filtered index for recursive ACLs
   - `idx_carpeta_padre`: Index for parent folder navigation
   - `idx_acl_carpeta_carpeta_usuario`: Composite index for ACL lookups
   ```

   **Update `ai-specs/specs/data-model.md`**:
   Add section under ACL_Carpeta entity:
   ```markdown
   ### Performance Indices (US-ACL-004)

   **Inheritance Query Index**:
   - Name: `idx_acl_carpeta_herencia`
   - Columns: (usuario_id, recursivo, organizacion_id)
   - Filter: WHERE recursivo = true
   - Purpose: Optimize recursive permission lookups
   
   **Parent Navigation Index**:
   - Name: `idx_carpeta_padre`
   - Columns: (carpeta_padre_id, organizacion_id)
   - Filter: WHERE fecha_eliminacion IS NULL
   - Purpose: Efficient ancestor path resolution

   **ACL Lookup Index**:
   - Name: `idx_acl_carpeta_carpeta_usuario`
   - Columns: (carpeta_id, usuario_id, organizacion_id)
   - Filter: WHERE fecha_eliminacion IS NULL
   - Purpose: Fast permission checks
   ```

4. **Verify Documentation**: 
   - Confirm all changes are accurately reflected
   - Check that documentation follows established structure
   - Ensure proper formatting and English language

5. **Report Updates**: Document which files were updated and what changes were made

**References**: 
- Follow process described in `ai-specs/specs/documentation-standards.mdc`
- All documentation must be written in English

**Notes**: This step is MANDATORY before considering the implementation complete.

---

## Implementation Order

Execute steps in the following sequence:

**Step 1**: Create Database Migration for Indices
**Step 2**: Create Value Object for Effective Permission (PermisoEfectivo)
**Step 3**: Extend Repository for Ancestor Resolution (CarpetaRepository)
**Step 4**: Extend ACLCarpetaRepository for Permission Lookups
**Step 5**: Create Domain Service for Permission Inheritance (PermisoHerenciaService)
**Step 6**: Create DTO for Effective Permission Response (PermisoEfectivoDTO)
**Step 7**: Create REST Endpoint for Effective Permission Query (CarpetaPermissionController)
**Step 8**: Create Custom Exceptions (PermisoDenegadoException, ResourceNotFoundException)
**Step 9**: Write Unit Tests for Permission Inheritance Algorithm
**Step 10**: Write Integration Tests
**Step 11**: Update Technical Documentation

**Critical Path**: Steps 1-5 must be completed sequentially (database → domain → service). Steps 6-8 (presentation layer) can be done in parallel after step 5. Tests (steps 9-10) can be written alongside implementation (TDD approach). Documentation (step 11) is final.

---

## Testing Checklist

After implementation, verify the following:

### Unit Tests
- [ ] Direct permission evaluation test passes
- [ ] Inherited permission from parent test passes
- [ ] Inherited permission from grandparent test passes
- [ ] Non-recursive ACL blocks inheritance test passes
- [ ] Multiple recursive ACLs use closest test passes
- [ ] No permission found returns empty test passes
- [ ] Parameter validation tests pass
- [ ] Multi-tenant isolation test passes
- [ ] Test coverage ≥ 90% for PermisoHerenciaService

### Integration Tests
- [ ] GET /mi-permiso returns 200 with direct permission
- [ ] GET /mi-permiso returns 200 with inherited permission
- [ ] GET /mi-permiso returns 403 when no permission
- [ ] GET /mi-permiso returns 403 when parent ACL is non-recursive
- [ ] GET /mi-permiso returns 404 when folder not found
- [ ] Direct permission has precedence over inherited
- [ ] Multi-level inheritance works (grandparent → child)

### Performance Tests
- [ ] Ancestor resolution query < 5ms (measured)
- [ ] Full permission evaluation < 10ms for 20-level hierarchy
- [ ] Database indices created successfully
- [ ] Query execution plan uses indices (EXPLAIN ANALYZE)

### Manual Tests
- [ ] Create recursive ACL via Postman/curl
- [ ] Query effective permission on descendant folder
- [ ] Verify JSON response structure matches spec
- [ ] Test with different niveles_acceso (LECTURA, ESCRITURA, ADMINISTRACION)
- [ ] Test cross-organization access denied (403)

---

## Error Response Format

All error responses follow this structure:

```json
{
  "error": {
    "codigo": "ERROR_CODE",
    "mensaje": "Human-readable message in Spanish",
    "detalle": "Additional details (optional)",
    "timestamp": "2026-02-03T14:30:00Z",
    "path": "/api/carpetas/4/mi-permiso"
  }
}
```

### HTTP Status Code Mapping

| Status | Error Code | Scenario |
|--------|-----------|----------|
| 200 | - | Permission found (direct or inherited) |
| 403 | PERMISO_DENEGADO | User has no permission (direct or inherited) |
| 404 | CARPETA_NO_ENCONTRADA | Folder does not exist or is soft-deleted |
| 404 | RECURSO_NO_ENCONTRADO | Generic resource not found |
| 401 | NO_AUTENTICADO | User not authenticated (missing/invalid JWT) |
| 500 | ERROR_INTERNO | Unexpected server error |

---

## Dependencies

### External Libraries
- **Spring Boot 3.5.x**: Core framework
- **Spring Data JPA**: Repository abstraction
- **Spring Security**: Authentication and authorization
- **PostgreSQL 16**: Database with CTE support
- **Jackson**: JSON serialization
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions

### Internal Dependencies
- **US-ACL-002**: ACL_Carpeta table with `recursivo` field must exist
- **US-FOLDER-001**: Carpeta table with `carpeta_padre_id` field must exist
- **US-AUTH-004**: JWT authentication with `organizacion_id` claim must be implemented

### Database Requirements
- Flyway for migrations
- Multi-tenant isolation via organizacion_id

---

## Notes

### Important Reminders
1. **Multi-tenant Isolation**: ALWAYS filter by organizacion_id in all queries
2. **Soft Deletes**: ALWAYS exclude soft-deleted records (fecha_eliminacion IS NULL)
3. **Performance**: Keep permission evaluation under 10ms for 20-level hierarchies
4. **Security**: Never expose organizacion_id in API responses; always extract from JWT
5. **Testing**: Follow TDD approach - write tests before implementation when possible

### Business Rules
- Direct permission ALWAYS takes precedence over inherited
- Non-recursive ACL stops ancestor traversal (inheritance blocked)
- Only first recursive ACL in ancestor chain is used (closest wins)
- Root folders (carpeta_padre_id = NULL) have no ancestors
- Maximum hierarchy depth: 50 levels (prevent cycles/performance)

### Language Requirements
- **Code**: English (classes, methods, variables)
- **API responses**: Spanish (error messages, user-facing text, comments)
- **Documentation**: Spanish (README, technical docs)
- **User stories**: Spanish

### Circular Reference Prevention
- Database constraint: `CHECK (id != carpeta_padre_id)`
- Application logic: Max depth limit (50 levels)
- Algorithm: Visited set to detect cycles

---

## Implementation Verification

Before marking the ticket as complete, verify:

### Code Quality
- [ ] All code follows backend-standards.md conventions
- [ ] No hardcoded values (use constants or configuration)
- [ ] Proper error handling in all methods
- [ ] Logging at appropriate levels (DEBUG for algorithm steps, WARN for denied access)
- [ ] No code smells (long methods, god classes, etc.)

### Functionality
- [ ] All acceptance criteria from US-ACL-004 met
- [ ] Algorithm handles all edge cases (cycles, root folders, multi-level)
- [ ] Multi-tenant isolation verified
- [ ] Soft-delete filtering works correctly

### Testing
- [ ] Unit tests pass with ≥ 90% coverage
- [ ] Integration tests pass with real database
- [ ] Performance tests meet targets (< 10ms)
- [ ] Manual testing completed successfully

### Integration
- [ ] API endpoint documented in OpenAPI spec
- [ ] Database migrations run successfully
- [ ] No breaking changes to existing APIs
- [ ] Compatible with existing ACL and folder services

### Documentation Updates Completed
- [ ] `ai-specs/specs/api-spec.yml` updated with new endpoint
- [ ] `backend/document-core/README.md` updated with inheritance section
- [ ] `ai-specs/specs/data-model.md` updated with new indices
- [ ] All documentation in English
- [ ] Code comments added for complex logic

---

**End of Implementation Plan**
