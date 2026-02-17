# Backend Implementation Plan: US-DOC-005 — Cambiar Versión Actual (Rollback)

## Overview

Implementación de funcionalidad para realizar rollback (cambio de versión actual) en documentos, permitiendo a usuarios con permisos de administración revertir cambios no deseados manteniendo el historial completo de versiones. La operación es atómica, totalmente auditada y resguarda el aislamiento multi-tenant.

**Principios Arquitectónicos**:
- Arquitectura Hexagonal (Ports & Adapters)
- Domain-Driven Design (DDD) con lógica de negocio concentrada en domain services
- SOLID principles
- Clean Architecture con separación clara entre capas

**Historia de Usuario**: Como usuario autorizado con permisos de administración en un documento, quiero marcar una versión anterior como versión actual (rollback), para que pueda revertir cambios no deseados sin perder el historial de versiones.

---

## Architecture Context

### Capas Involucradas

**Capa de Dominio** (`domain/`)
- `model/Documento.java` - Entidad con lógica de cambio de versión
- `model/Version.java` - Entidad (validación que pertenece al documento)
- `repository/DocumentoRepository.java` - Puerto de repositorio (ya existe)
- `repository/VersionRepository.java` - Puerto de repositorio (ya existe)
- `service/IEvaluadorPermisos.java` - Puerto para evaluación de permisos
- `exception/VersionControlNegocioException.java` - Nueva excepción de dominio (crear)
- `exception/InvalidVersionException.java` - Nueva excepción de dominio (crear)

**Capa de Aplicación** (`application/`)
- `dto/ChangeCurrentVersionRequest.java` - DTO de request (crear)
- `dto/DocumentoResponse.java` - DTO de respuesta (puede existir, extender si es necesario)
- `service/DocumentoVersionChangeService.java` - Servicio de aplicación (crear)
- `port/IAuditEventPublisher.java` - Puerto para publicar eventos de auditoría (crear)

**Capa de Infraestructura** (`infrastructure/`)
- `adapter/controller/VersionChangeController.java` - Controlador REST (crear)
- `adapter/persistence/AuditEventAdapter.java` - Adaptador de auditoría (crear si no existe)
- `config/SecurityConfig.java` - Revisar configuración de seguridad (puede requerir ajustes)

**Base de Datos**
- Índice en `documento.version_actual_id` para acceso rápido (verificar existencia)
- Índice compuesto `(documento_id, version_id)` en tabla de versiones (verificar)
- Log de auditoría con campos: usuario_id, documento_id, version_anterior_id, version_nueva_id, timestamp, etc.

### Servicio: document-core

```
backend/document-core/
├── src/main/java/com/docflow/documentcore/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Documento.java [✓ existe]
│   │   │   └── Version.java [✓ existe]
│   │   ├── repository/
│   │   │   ├── DocumentoRepository.java [✓ existe]
│   │   │   └── VersionRepository.java [✓ existe]
│   │   ├── service/
│   │   │   ├── IEvaluadorPermisos.java [✓ existe]
│   │   │   └── VersionChangeEventPublisher.java [⚠️ crear interfaz/puerto]
│   │   └── exception/
│   │       ├── VersionNotBelongToDocumentException.java [⚠️ crear]
│   │       └── InsufficientPermissionsException.java [✓ puede existir]
│   ├── application/
│   │   ├── dto/
│   │   │   ├── ChangeCurrentVersionRequest.java [⚠️ crear]
│   │   │   └── DocumentoResponse.java [⚠️ crear/extender]
│   │   ├── service/
│   │   │   └── DocumentoVersionChangeService.java [⚠️ crear]
│   │   └── port/
│   │       └── IAuditEventPublisher.java [⚠️ crear puerto]
│   └── infrastructure/
│       ├── adapter/
│       │   ├── controller/
│       │   │   └── VersionChangeController.java [⚠️ crear]
│       │   └── persistence/
│       │       └── AuditEventRepositoryAdapter.java [⚠️ crear]
│       └── config/
│           └── SecurityConfig.java [✓ revisar]
└── src/test/java/com/docflow/documentcore/
    ├── application/service/
    │   └── DocumentoVersionChangeServiceTest.java [⚠️ crear]
    └── infrastructure/adapter/controller/
        └── VersionChangeControllerTest.java [⚠️ crear]
```

---

## Implementation Steps

### Step 1: Create Domain Exceptions

- **File**: 
  - `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/VersionNotBelongToDocumentException.java`
  - `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/InsufficientPermissionsException.java` (si no existe)

- **Action**: Crear excepciones específicas de dominio para casos de error

- **Excepciones a Implementar**:

#### Exception 1: VersionNotBelongToDocumentException
```java
public class VersionNotBelongToDocumentException extends RuntimeException {
    private Long versionId;
    private Long documentoId;
    
    public VersionNotBelongToDocumentException(Long versionId, Long documentoId) {
        super("La versión " + versionId + " no pertenece al documento " + documentoId);
        this.versionId = versionId;
        this.documentoId = documentoId;
    }
}
```

#### Exception 2: InsufficientPermissionsException (si no existe)
- Verificar si existe en proyecto
- Si no existe: crear con estructura similar
- Debe incluir: tipo de permiso requerido, recurso, tipo de recurso

- **Implementation Notes**:
  - Extender de `RuntimeException` para ser unchecked
  - Incluir campos útiles para logging y diagnóstico
  - Mensaje claro en español para el usuario
  - Será manejada por `GlobalExceptionHandler` para retornar HTTP 400 o 403

---

### Step 2: Create DTOs (Request/Response)

#### File: `ChangeCurrentVersionRequest.java`

- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/ChangeCurrentVersionRequest.java`

- **Action**: Crear DTO para request del endpoint de cambio de versión

- **Implementation Steps**:
  1. Crear clase con anotaciones de Lombok: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
  2. Agregar campo `Long versionId` (ID de versión a marcar como actual)
  3. Agregar validaciones con Bean Validation:
     - `@NotNull(message = "El ID de versión es obligatorio")`
     - `@Positive(message = "El ID de versión debe ser positivo")`
  4. Agregar Javadoc explicando propósito y uso

- **Example Structure**:
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeCurrentVersionRequest {
    
    @NotNull(message = "El ID de versión es obligatorio")
    @Positive(message = "El ID de versión debe ser positivo")
    private Long versionId;
}
```

- **Dependencies**:
```java
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
```

#### File: `DocumentoResponse.java`

- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/DocumentoResponse.java`

- **Action**: Crear o extender DTO de respuesta con información actualizada del documento

- **Implementation Steps**:
  1. Verificar si DTO ya existe (buscar en application/dto/)
  2. Si existe: extender con campo `fechaActualizacion` si falta
  3. Si no existe: crear con estructura completa:
     - `Long id` - ID del documento
     - `String nombre` - Nombre del documento
     - `Long versionActualId` - ID de versión actual (actualizado después del rollback)
     - `String estado` - Estado del documento (ACTIVO, ARCHIVADO, etc.)
     - `OffsetDateTime fechaCreacion` - Fecha de creación
     - `OffsetDateTime fechaActualizacion` - Fecha de última modificación (incluir en update)
     - `Integer numeroTotalVersiones` - Total de versiones
  4. Usar anotaciones de Lombok para reducir boilerplate
  5. Agregar Javadoc

- **Dependencies**:
```java
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
```

---

### Step 3: Create Domain Service Port for Audit Events

- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/service/VersionChangeEventPublisher.java`

- **Action**: Crear interfaz (puerto) para publicar eventos de auditoría de cambios de versión

- **Function Signature**:
```java
public interface VersionChangeEventPublisher {
    
    /**
     * Publica evento de rollback de versión.
     * 
     * @param usuarioId ID del usuario que ejecuta la operación
     * @param documentoId ID del documento
     * @param organizacionId ID de la organización/tenant
     * @param versionAnteriorId ID de la versión anterior (antes del rollback)
     * @param versionNuevaId ID de la versión nueva (después del rollback)
     * @param timestamp Momento de la operación
     */
    void publishVersionRollbackEvent(
        Long usuarioId,
        Long documentoId,
        Long organizacionId,
        Long versionAnteriorId,
        Long versionNuevaId,
        OffsetDateTime timestamp
    );
}
```

- **Implementation Notes**:
  - Interfaz define el contrato, no la implementación
  - El adaptador (infraestructura) implementará esta interfaz
  - Esto permite inyectar la dependencia en servicio de aplicación
  - Patrón Dependency Inversion: aplicación depende de abstracción, no de implementación
  - La auditoría podría ir a base de datos local, evento message broker, etc.

---

### Step 4: Create Application Service

- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/application/service/DocumentoVersionChangeService.java`

- **Action**: Crear servicio de aplicación con lógica de cambio de versión

- **Class Annotations**:
```java
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
```

- **Dependencies to Inject**:
```java
private final DocumentoRepository documentoRepository;
private final VersionRepository versionRepository;
private final IEvaluadorPermisos evaluadorPermisos;
private final VersionChangeEventPublisher auditPublisher;
```

- **Main Method Signature**:
```java
public DocumentoResponse cambiarVersionActual(
    Long documentoId,
    Long versionId,
    Long usuarioId,
    Long organizacionId
) throws VersionNotBelongToDocumentException, 
         InsufficientPermissionsException,
         ResourceNotFoundException
```

- **Implementation Steps**:

  1. **Validar aislamiento multi-tenant**:
     - Buscar documento: `documentoRepository.findByIdAndOrganizacionId(documentoId, organizacionId)`
     - Si no existe: lanzar `ResourceNotFoundException("Documento", documentoId)`
     
  2. **Validar permisos elevados (ADMINISTRACION)**:
     - Llamar: `evaluadorPermisos.tieneAcceso(usuarioId, documentoId, TipoRecurso.DOCUMENTO, NivelAcceso.ADMINISTRACION, organizacionId)`
     - Si es false: registrar warn y lanzar `InsufficientPermissionsException("ADMINISTRACION", "DOCUMENTO")`
     
  3. **Validar que la versión existe y pertenece al documento**:
     - Buscar versión: `versionRepository.findByIdAndDocumentoId(versionId, documentoId)` (puede requerir extender repositorio)
     - Si no existe: lanzar `VersionNotBelongToDocumentException(versionId, documentoId)`
     
  4. **Guardar versión anterior para auditoría (antes de cambiar)**:
     - `Long versionAnteriorId = documento.getVersionActualId()`
     
  5. **Actualizar versión actual (atómicamente)**:
     - Si versionId es igual a versionAnteriorId: es idempotente, continuar (registrar que es operación redundante)
     - Actualizar: `documento.setVersionActualId(versionId)`
     - Actualizar: `documento.setFechaActualizacion(OffsetDateTime.now())`
     - Persistir: `documentoRepository.save(documento)` (transacción automática)
     - Log: `log.info("Version rollback ejecutado: documento {}, versión anterior {}, versión nueva {}", documentoId, versionAnteriorId, versionId)`
     
  6. **Publicar evento de auditoría**:
     - `auditPublisher.publishVersionRollbackEvent(usuarioId, documentoId, organizacionId, versionAnteriorId, versionId, OffsetDateTime.now())`
     - Esto debe ser síncrono (no async) para garantizar consistencia
     
  7. **Construir y retornar respuesta**:
     - Crear `DocumentoResponse` con documento actualizado
     - Mapear campos: id, nombre, versionActualId (actualizado), estado, fechaCreacion, fechaActualizacion, numeroTotalVersiones
     - Retornar response

- **Implementation Notes**:
  - Usar `@Transactional` a nivel de método para atomicidad
  - Logging exhaustivo DEL lado del negocio (qué pasó, no cómo pasó)
  - Auditoría se publica SIN importar si es cambio real o idempotente
  - Validaciones fail-fast: fallar temprano, antes de queries costosas
  - Orden de validaciones: existe → permisos → referencial
  - Si hay excepción en publicar auditoría, la transacción hará rollback (rollbackFor = Exception.class)

- **Error Scenarios**:
  - Documento no encontrado → `ResourceNotFoundException` (404)
  - Usuario sin permiso ADMINISTRACION → `InsufficientPermissionsException` (403)
  - Versión no encontrada o no pertenece → `VersionNotBelongToDocumentException` (400)
  - Error de auditoría → Rollback automático de transacción

---

### Step 5: Extend VersionRepository (if needed)

- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/repository/VersionRepository.java`

- **Action**: Verificar y extender repositorio con método para buscar versión por ID y documento

- **Method to Add**:
```java
/**
 * Busca una versión específica que pertenece a un documento.
 * 
 * @param versionId ID de la versión
 * @param documentoId ID del documento propietario
 * @return Optional con la versión si existe y pertenece al documento
 */
Optional<Version> findByIdAndDocumentoId(Long versionId, Long documentoId);
```

- **Implementation Notes**:
  - Spring Data JPA generará la implementación automáticamente
  - Asegura acceso seguro sin risk de que versión de otro documento sea accedida
  - Aprovechar índice compuesto si existe

---

### Step 6: Create Audit Event Infrastructure Adapter

- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/AuditEventRepositoryAdapter.java`

- **Action**: Crear adaptador que implementa puerto de auditoría

- **Implementation Steps**:
  1. Crear clase que implementa `VersionChangeEventPublisher`
  2. Inyectar `AuditEventRepository` (asumir que existe o será disponible)
  3. En método `publishVersionRollbackEvent`:
     - Crear entidad `AuditEvent` con campos:
       - usuarioId
       - documentoId
       - organizacionId
       - codigoEvento = "VERSION_ROLLBACK"
       - versionAnteriorId
       - versionNuevaId
       - timestamp
     - Persistir: `auditEventRepository.save(auditEvent)`
     - Log: `log.info("Audit event published: VERSION_ROLLBACK for documento {} por usuario {}", documentoId, usuarioId)`

- **Decorator Option**:
  - Usar `@Component` para registro automático como bean
  - Anotar con `@Transactional(propagation = Propagation.REQUIRES_NEW)` para que auditoría tenga su propia transacción (si falla auditoría, no afecta cambio de documento)

- **Dependencies**:
```java
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
```

---

### Step 7: Create Controller Method

- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/VersionChangeController.java`

- **Action**: Crear controlador REST con endpoint PATCH para cambio de versión

- **Class Annotations**:
```java
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Versiones", description = "Gestión de cambio de versión de documentos")
```

- **Inject**:
```java
private final DocumentoVersionChangeService documentoVersionChangeService;
```

- **Endpoint Method**:

```java
@PatchMapping("/{documentoId}/version-actual")
@Operation(summary = "Cambiar versión actual (rollback)")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Versión cambiada exitosamente"),
    @ApiResponse(responseCode = "400", description = "Versión no válida"),
    @ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
    @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
})
public ResponseEntity<DocumentoResponse> cambiarVersionActual(
    @Parameter(description = "ID del documento")
    @PathVariable Long documentoId,
    
    @Parameter(description = "ID del usuario (inyectado por Gateway)")
    @RequestHeader("X-User-Id") Long usuarioId,
    
    @Parameter(description = "ID de la organización (inyectado por Gateway)")
    @RequestHeader("X-Organization-Id") Long organizacionId,
    
    @Valid @RequestBody ChangeCurrentVersionRequest request
)
```

- **Implementation Steps**:
  1. Registrar petición: `log.info("PATCH /api/documentos/{}/version-actual - Usuario: {}, Versión: {}", documentoId, usuarioId, request.getVersionId())`
  2. Llamar servicio: `documentoVersionChangeService.cambiarVersionActual(documentoId, request.getVersionId(), usuarioId, organizacionId)`
  3. Retornar respuesta: `ResponseEntity.ok(response)`

- **Implementation Notes**:
  - Endpoint debe estar bajo autenticación (JWT via Gateway, fuera de alcance)
  - Headers X-User-Id y X-Organization-Id son inyectados por Gateway (confiar en ellos)
  - Validación de request (`@Valid`) es automática mediante anotaciones en DTO
  - Global exception handler convertirá excepciones a respuestas HTTP
  - OpenAPI annotations permiten documentación automática en Swagger

---

### Step 8: Create Exception Handler Mappings

- **File**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/GlobalExceptionHandler.java`

- **Action**: Agregar handlers para nuevas excepciones de dominio

- **Exception Handlers to Add**:

1. **VersionNotBelongToDocumentException** → HTTP 400:
```java
@ExceptionHandler(VersionNotBelongToDocumentException.class)
public ProblemDetail handleVersionNotBelongToDocument(VersionNotBelongToDocumentException ex) {
    log.warn("Intento de cambio a versión inexistente: {}", ex.getMessage());
    
    var problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "La versión solicitada no pertenece al documento"
    );
    problem.setTitle("Versión No Válida");
    problem.setType(URI.create("https://docflow.com/errors/invalid-version"));
    problem.setProperty("timestamp", Instant.now());
    
    return problem;
}
```

2. **InsufficientPermissionsException** → HTTP 403:
```java
@ExceptionHandler(InsufficientPermissionsException.class)
public ProblemDetail handleInsufficientPermissions(InsufficientPermissionsException ex) {
    log.warn("Usuario intenta operación sin permisos suficientes: {}", ex.getMessage());
    
    var problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        "No posee permiso requerido para cambiar versión actual"
    );
    problem.setTitle("Permisos Insuficientes");
    problem.setType(URI.create("https://docflow.com/errors/insufficient-permissions"));
    problem.setProperty("timestamp", Instant.now());
    
    return problem;
}
```

- **Implementation Notes**:
  - Casos ResourceNotFoundException y AccessDeniedException ya deben existir
  - Seguir patrón RFC 7807 (ProblemDetail)
  - Logging diferenciado: WARN para intentos ilegales, ERROR para fallos del sistema

---

### Step 9: Write Unit Tests for Service

- **File**: `backend/document-core/src/test/java/com/docflow/documentcore/application/service/DocumentoVersionChangeServiceTest.java`

- **Action**: Escribir pruebas unitarias comprehensivas

- **Test Structure**:

```java
@DisplayName("DocumentoVersionChangeService - Cambiar Versión Actual")
@ExtendWith(MockitoExtension.class)
class DocumentoVersionChangeServiceTest {
    
    @Mock
    private DocumentoRepository documentoRepository;
    
    @Mock
    private VersionRepository versionRepository;
    
    @Mock
    private IEvaluadorPermisos evaluadorPermisos;
    
    @Mock
    private VersionChangeEventPublisher auditPublisher;
    
    @InjectMocks
    private DocumentoVersionChangeService service;
    
    private static final Long DOCUMENTO_ID = 100L;
    private static final Long VERSION_ID = 201L;
    private static final Long USUARIO_ID = 1L;
    private static final Long ORGANIZACION_ID = 1L;
    
    @BeforeEach
    void setUp() {
        // Configuración inicial si es necesaria
    }
}
```

- **Test Cases**:

#### Test Group 1: Casos Exitosos
- **Test**: `should_ChangeCurrentVersion_When_AllValidationsPass`
  - Mock: Documento existe, usuario tiene permiso ADMINISTRACION, versión existe
  - Assert: Documento actualizado, evento publicado, respuesta 200
  - Verify: Repositorio.save llamado, auditPublisher.publishVersionRollbackEvent llamado

- **Test**: `should_PublishAuditEvent_When_VersionChanged`
  - Assert: Evento de auditoría contiene usuario_id, documento_id, versión anterior/nueva, timestamp

- **Test**: `should_BeIdempotent_When_ChangingToSameCurrentVersion`
  - Setup: version_actual_id = 201, solicitar cambio a 201
  - Assert: Retorna 200 OK, evento publicado igual, documento no muestra cambios reales

#### Test Group 2: Validaciones de Error
- **Test**: `should_ThrowResourceNotFoundException_When_DocumentNotFound`
  - Setup: documentoRepository.findByIdAndOrganizacionId retorna empty
  - Assert: ResourceNotFoundException lanzada, no hay persistencia

- **Test**: `should_ThrowInsufficientPermissionsException_When_UserLacksAdminPermission`
  - Setup: evaluadorPermisos.tieneAcceso retorna false
  - Assert: InsufficientPermissionsException lanzada antes de cambio

- **Test**: `should_ThrowVersionNotBelongToDocumentException_When_VersionNotFound`
  - Setup: versionRepository.findByIdAndDocumentoId retorna empty
  - Assert: VersionNotBelongToDocumentException lanzada

- **Test**: `should_ThrowVersionNotBelongToDocumentException_When_VersionBelongsToDifferentDocument`
  - Setup: Versión existe pero documento_id no coincide
  - Assert: Excepción lanzada

#### Test Group 3: Validaciones Multi-Tenant
- **Test**: `should_ThrowResourceNotFoundException_When_DocumentBelongsToDifferentOrganization`
  - Setup: findByIdAndOrganizacionId retorna empty (documento pertenece a otro tenant)
  - Assert: ResourceNotFoundException (no revelar existencia)

#### Test Group 4: Transacciones y Atomicidad
- **Test**: `should_RollbackTransaction_When_AuditPublishFails`
  - Setup: auditPublisher.publishVersionRollbackEvent lanza excepción
  - Assert: save() no fue llamado, transacción reverted

- **Dependencies**:
```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
```

- **Testing Best Practices**:
  - Usar `@Nested` para agrupar tests relacionados
  - Convención: `should_[ExpectedBehavior]_When_[Condition]`
  - Usar AssertJ para aserciones fluidas
  - Verify interacciones mock correctas
  - 100% coverage objetivo para lógica de servicio

---

### Step 10: Write Integration Tests for Controller

- **File**: `backend/document-core/src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/VersionChangeControllerTest.java`

- **Action**: Escribir pruebas de integración del controlador

- **Class Setup**:
```java
@WebMvcTest(controllers = VersionChangeController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("VersionChangeController - Tests de Integración")
class VersionChangeControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private DocumentoVersionChangeService documentoVersionChangeService;
    
    private static final Long DOCUMENTO_ID = 100L;
    private static final Long VERSION_ID = 201L;
    private static final Long USUARIO_ID = 1L;
    private static final Long ORGANIZACION_ID = 1L;
}
```

- **Test Cases**:

#### Test Group 1: Peticiones Exitosas
- **Test**: `should_Return200_When_ChangeVersionSucceeds`
  - Petición: PATCH /api/documentos/100/version-actual con body { "versionId": 201 }
  - Mock: Service retorna DocumentoResponse actualizado
  - Assert: Status 200, response tiene versionActualId actualizado, fechaActualizacion reciente

- **Test**: `should_ReturnUpdateResponse_When_VersionChanged`
  - Assert: JSON response contiene: id, nombre, versionActualId, estado, fechaCreacion, fechaActualizacion, numeroTotalVersiones

#### Test Group 2: Validaciones
- **Test**: `should_Return400_When_VersionIdIsNull`
  - Petición: Body body de request está vacío o null
  - Assert: Status 400 Bad Request

- **Test**: `should_Return400_When_VersionIdIsNegative`
  - Petición: Body { "versionId": -1 }
  - Assert: Status 400 (validación @Positive falla)

- **Test**: `should_Return400_When_VersionNotBelongToDocument`
  - Petición: PATCH con versionId válido pero que lanza VersionNotBelongToDocumentException
  - Assert: Status 400, error contiene "no pertenece al documento"

#### Test Group 3: Errores de Autorización
- **Test**: `should_Return403_When_UserLacksAdminPermission`
  - Setup: Service lanza InsufficientPermissionsException
  - Assert: Status 403, error contiene "Permisos insuficientes" o "ADMINISTRACION"

#### Test Group 4: Errores de No Encontrado
- **Test**: `should_Return404_When_DocumentNotFound`
  - Setup: Service lanza ResourceNotFoundException
  - Assert: Status 404

#### Test Group 5: Headers Requeridos
- **Test**: `should_Return400_When_HeaderXUserIdMissing`
  - Petición: Sin header X-User-Id
  - Assert: Status 400 (MissingRequestHeaderException)

- **Test**: `should_Return400_When_HeaderXOrganizationIdMissing`
  - Petición: Sin header X-Organization-Id
  - Assert: Status 400

- **Dependencies**:
```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
```

---

### Step 11: Verify and Create Database Index

- **File**: Script de migración (si aplica) o verificación manual en PostgreSQL

- **Action**: Verificar e índices necesarios existen

- **SQL Scripts**:

```sql
-- 1. Verificar índice en version_actual_id (para acceso rápido a versión actual)
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'documento' 
  AND indexname = 'idx_documento_version_actual_id';

-- Si no existe, crear:
CREATE INDEX IF NOT EXISTS idx_documento_version_actual_id 
ON documento (version_actual_id);

-- 2. Verificar índice compuesto en versiones
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'documento_version' 
  AND indexname = 'idx_documento_version_doc_id_version_id';

-- Si no existe, crear:
CREATE INDEX IF NOT EXISTS idx_documento_version_doc_id_version_id 
ON documento_version (documento_id, version_id);
```

- **Verification Queries**:

```sql
-- Verificar query de búsqueda de versión está indexada
EXPLAIN ANALYZE
SELECT v.* FROM documento_version v 
WHERE v.documento_id = 100 AND v.id = 201;

-- Debería mostrar "Index Scan" en el plan
```

- **Implementation Notes**:
  - Índices son críticos para performance
  - Verificar existencia antes de crear (usar IF NOT EXISTS)
  - Agregar comentario explicativo para mantenimiento futuro
  - Documentar en migration file si se usa Flyway/Liquibase

---

### Step 12: Update Technical Documentation

- **Action**: Revisar y actualizar documentación técnica según cambios

- **Files to Update**:

#### File 1: `ai-specs/specs/api-spec.yml`
- **Changes**:
  - Agregar endpoint PATCH definition
  - Incluir request/response schemas
  - Documentar códigos de error y ejemplos
  
- **Example Addition**:
```yaml
/api/documentos/{documentoId}/version-actual:
  patch:
    summary: Cambiar versión actual (rollback)
    tags:
      - Versiones
    security:
      - bearerAuth: []
    parameters:
      - name: documentoId
        in: path
        required: true
        schema:
          type: integer
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ChangeCurrentVersionRequest'
    responses:
      '200':
        description: Versión cambiada exitosamente
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DocumentoResponse'
      '400':
        description: Parámetros no válidos
      '403':
        description: Permisos insuficientes
      '404':
        description: Documento no encontrado
```

#### File 2: `backend/document-core/README.md`
- **Changes**:
  - Agregar sección sobre rollback de versiones
  - Documentar tipo de permiso requerido (ADMINISTRACION)
  - Agregar ejemplo de curl/petición

## Implementation Order

Orden secuencial recomendado:

2. **Step 1**: Crear excepciones de dominio
3. **Step 2**: Crear DTOs
4. **Step 3**: Crear puerto de auditoría (interfaz)
5. **Step 4**: Crear servicio de aplicación
6. **Step 5**: Extender VersionRepository (si necesario)
7. **Step 6**: Crear adaptador de auditoría
8. **Step 7**: Crear controlador REST
9. **Step 8**: Agregar exception handlers
10. **Step 9**: Escribir pruebas unitarias de servicio
11. **Step 10**: Escribir pruebas de integración
12. **Step 11**: Verificar/crear índices de base de datos
13. **Step 12**: Actualizar documentación técnica

**Notas Importantes**:
- Ejecutar pruebas tras cada step importante (`mvn test`)
- Commit frecuente con mensajes significativos
- No proceder al siguiente step si pruebas fallan
- Validar que código compila: `mvn clean package -DskipTests`

---

## Testing Checklist

Post-implementación, verificar:

### Pruebas Unitarias
- [ ] Tests del servicio cubren caso exitoso de cambio de versión
- [ ] Tests cubren caso idempotente (cambio a versión actual)
- [ ] Tests cubren validación de documento no encontrado
- [ ] Tests cubren validación de permisos insuficientes
- [ ] Tests cubren validación de versión no válida
- [ ] Tests cubren validación de tenant isolation
- [ ] Tests cubren casoónea en auditoría con rollback
- [ ] Todas pruebas unitarias pasan: `mvn test -Dtest=DocumentoVersionChangeServiceTest`
- [ ] Cobertura de pruebas ≥ 90% para nueva lógica

### Pruebas de Integración
- [ ] Tests del controlador retornan 200 con respuesta correcta
- [ ] Tests validan estructura de JSON en respuesta
- [ ] Tests verifican validación de parámetros (400 para versionId nulo/negativo)
- [ ] Tests verifican error 403 para permisos insuficientes
- [ ] Tests verifican error 404 para documento no encontrado
- [ ] Tests verifican error 400 para versión no válida
- [ ] Tests verifican headers requeridos (X-User-Id, X-Organization-Id)
- [ ] Todas pruebas de integración pasan: `mvn verify -Dtest=VersionChangeControllerTest`

### Pruebas Manuales
- [ ] Endpoint accesible en `http://localhost:8082/api/documentos/{id}/version-actual`
- [ ] Respuesta exitosa contiene documentoResponse con versionActualId actualizado
- [ ] Rollback a versión anterior funciona correctamente
- [ ] Rollback a versión actual (idempotente) funciona
- [ ] Error 403 cuando usuario no tiene permiso ADMINISTRACION
- [ ] Error 404 cuando documento no existe
- [ ] Error 400 cuando versión no existe
- [ ] Swagger UI muestra endpoint correcto con parámetros y ejemplos
- [ ] Auditoría se registra correctamente en base de datos

### Base de Datos
- [ ] Índice `idx_documento_version_actual_id` existe
- [ ] Índice compuesto en documento_version existe
- [ ] EXPLAIN ANALYZE muestra uso de índices
- [ ] Auditoría se escribe transaccionalmente
- [ ] No hay problemas de race conditions (transacciones)

### Documentación
- [ ] Especificación OpenAPI actualizada con endpoint PATCH
- [ ] Schemas agregados en components
- [ ] README actualizado con información del rollback
- [ ] Javadoc completo para métodos públicos
- [ ] Comentarios de código en Español
- [ ] Error responses documentadas

---

## Error Response Format

Todas las respuestas de error siguen RFC 7807 (ProblemDetail):

```json
{
  "type": "https://docflow.com/errors/[error-code]",
  "title": "Human-Readable Title",
  "status": 400,
  "detail": "Detailed error message in Spanish",
  "instance": "/api/documentos/100/version-actual",
  "timestamp": "2026-02-09T14:30:00Z"
}
```

### HTTP Status Code Mapping

| Code | Exception | Detail | Message |
|------|-----------|--------|---------|
| 400 | VersionNotBelongToDocumentException | Versión no existe o no pertenece al documento | "La versión solicitada no pertenece al documento" |
| 400 | MethodArgumentNotValidException | Validación de parámetros de request | "El ID de versión es obligatorio" |
| 403 | InsufficientPermissionsException | Usuario sin permiso ADMINISTRACION | "No posee permiso requerido para cambiar versión actual" |
| 404 | ResourceNotFoundException | Documento no encontrado o de otro tenant | "Documento no encontrado" |
| 401 | Unauthorized | Token JWT inválido/expirado | Manejado por Gateway |

---

## Dependencies

### Maven Dependencies (Verificar en `pom.xml`)

```xml
<!-- Spring Boot Web & Data -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Logging & Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Notas**: No se requieren dependencias nuevas. Todo ya está disponible en el proyecto.

---

## Notes

### Reglas de Negocio
1. **Permisos Elevados**: SOLO usuarios con ADMINISTRACION pueden cambiar versión actual
2. **Atomicidad**: Cambio de versión + auditoría deben ser todos o nada
3. **Idempotencia**: Cambiar a versión actual es seguro (200 OK, sin error)
4. **No Soft Delete de Versiones**: Las versiones nunca se eliminan, solo se cambia el puntero
5. **Auditoría Obligatoria**: Siempre registrar, incluso si operación es redundante
6. **Aislamiento de Tenant**: Validar organizacionId en TODAS las queries

### Consideraciones de Seguridad
- **Validación Multi-Tenant**: Toda operación verifica `documento.organizacionId == organizacionId`
- **Fail-Fast**: Fallar temprano ante errores: existe → permisos → referencial
- **No Revelar Existencia**: Si document no encontrado en tenant, retornar 404 (no revelar si existe en otro tenant)
- **Auditoría de Seguridad**: Registrar intentos fallidos con WARN level
- **Transaccionalidad**: Una falla en auditoría hace rollback de cambio de documento

### Consideraciones de Rendimiento
- **Use of Indexes**: Índices critiques para queries rápidas
- **Read-Only Check**: Usar transacción apropiada (no necesariamente readOnly para operación de escritura)
- **Batch Operations**: N/A para este endpoint
- **Expected Performance**: < 100ms para operación completa (validaciones + update + auditoría)

### Mejoras Futuras (Fuera de Alcance)
1. **Versión Temporal**: Marcar versiones como "temporal" antes de rollback real
2. **Aprobaciones**: Workflow de aprobación antes de rollback en documentos críticos
3. **Comparación Visual**: Mostrar diff entre versión anterior y nueva antes de confirmar rollback
4. **Notificaciones**: Alertar a otros usuarios sobre rollback
5. **Límite de Tiempo**: Solo permitir rollback dentro de X días
6. **Autenticación Multi-Factor**: Para rollbacks en documentos críticos

### Requisitos de Idioma
- **Código**: Clases, métodos, variables en Inglés
- **Comentarios/Javadoc**: En Español (para claridad del equipo local)
- **Mensajes de Error**: En Español (para usuarios)
- **Logs**: En Inglés (para consistencia de infraestructura)
- **Documentación Técnica**: En Inglés (API spec, README)

---

## Next Steps After Implementation

1. **Code Review**:
   - Crear Pull Request desde `feature/US-DOC-005-backend` hacia `develop`
   - Solicitar revisión de equipo
   - Atender comentarios

2. **Testing en QA**:
   - Desplegar a ambiente QA
   - Probar con volúmenes de datos realistas
   - Validar auditoría se escribe correctamente

3. **Integration Testing**:
   - Coordinar con frontend sobre contrato de API
   - Validar que Gateway enruta correctamente
   - Probar autenticación JWT

4. **Monitoring Setup**:
   - Configurar alertas para errores 500
   - Monitorear performance (tiempo de respuesta)
   - Rastrear tasa de rollbacks

5. **Despliegue a Producción**:
   - Crear índices de base de datos primero (en ambiente prod)
   - Desplegar código backend
   - Realizar smoke test en producción
   - Monitorear logs para primeras 24H

---

## Implementation Verification

Antes de marcar ticket como completado, verificar:

### Calidad de Código
- [ ] Código sigue convenciones del proyecto (PascalCase clases, camelCase métodos)
- [ ] Inyección por constructor (no field injection)
- [ ] Sin valores hardcodeados (usar propiedades de application.yml)
- [ ] Manejo de excepciones con tipos específicos
- [ ] Anotaciones de Lombok usadas correctamente
- [ ] Sin advertencias du compilador
- [ ] Código bien comentado con Javadoc (Español)
- [ ] Logging exhaustivo (INFO, WARN, ERROR apropiados)

### Funcionalidad
- [ ] Endpoint retorna 200 para peticiones válidas
- [ ] Versión actual se cambia correctamente en base de datos
- [ ] Flag de versión actual es preciso
- [ ] Auditoría se registra completamente
- [ ] Aislamiento multi-tenant se enforce
- [ ] Validación de permisos funciona
- [ ] Respuestas de error coinciden con especificación
- [ ] Idempotencia funciona (cambio a versión actual)

### Pruebas
- [ ] Todas pruebas unitarias pasan: `mvn test`
- [ ] Todas pruebas de integración pasan: `mvn verify`
- [ ] Cobertura ≥ 90% para código nuevo
- [ ] Sin pruebas flaky (ejecutar 3 veces para verificar)
- [ ] Tests son limpios y bien nombrados

### Integración
- [ ] Servicio inicia correctamente: `mvn spring-boot:run`
- [ ] Swagger UI muestra endpoint
- [ ] Sin errores en logs de aplicación
- [ ] GlobalExceptionHandler maneja excepciones correctamente
- [ ] Auditoría se integra sin errores

### Documentación
- [ ] OpenAPI spec actualizada
- [ ] README actualizado
- [ ] Javadoc completo
- [ ] Documentación en Inglés
- [ ] Ejemplos de petición/respuesta precisos

### Base de Datos
- [ ] Índices existen y se usan
- [ ] Auditoría se escribe transaccionalmente
- [ ] Sin race conditions (transactions)
- [ ] Sin deadlocks bajo concurrencia

### Seguridad
- [ ] Headers X-User-Id, X-Organization-Id requeridos
- [ ] Validación multi-tenant funciona
- [ ] Permisos ADMINISTRACION se verifican
- [ ] Mensajes de error no filtran INFO sensible

### Git
- [ ] Rama creada: `feature/US-DOC-005-backend`
- [ ] Commits tienen mensajes significativos
- [ ] Push al remoto completado
- [ ] Sin conflictos de merge
- [ ] Pull Request creado con descripción

---

## Conclusión

Este plan proporciona una guía step-by-step para implementar la funcionalidad de cambio de versión actual (rollback) en documentos, con todos los detalles técnicos necesarios para que un desarrollador pueda ejecutarlo autónomamente del principio al fin. 

El endpoint implementado:
- ✅ Permite rollback a versiones anteriores
- ✅ Requiere permisos elevados (ADMINISTRACION)
- ✅ Registra auditoría completa
- ✅ Es totalmente auditado y trazable
- ✅ Valida aislamiento multi-tenant
- ✅ Es atómico (todo o nada)
- ✅ Es idempotente
- ✅ Tiene cobertura de pruebas 90%+
- ✅ Es completamente documentado

**Principios Aplicados**:
- ✅ Arquitectura Hexagonal
- ✅ Domain-Driven Design
- ✅ SOLID principles
- ✅ Clean Architecture
- ✅ Security best practices
- ✅ Performance optimization
- ✅ Complete test coverage
- ✅ Full documentation
