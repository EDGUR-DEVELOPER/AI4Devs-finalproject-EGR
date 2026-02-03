# Backend Implementation Plan: US-ACL-003 Revocar permiso de carpeta (eliminar ACL)

## Overview

Implementar la funcionalidad para revocar (eliminar) permisos de acceso a carpetas mediante la eliminación de entradas ACL. Esta implementación se basa en la infraestructura creada en US-ACL-002 y sigue principios de Domain-Driven Design (DDD) con arquitectura hexagonal.

**Principios Clave:**
- **Operación de Hard Delete**: Eliminación física de la entrada ACL de la base de datos
- **Aislamiento por Tenant**: Validación obligatoria de `organizacion_id` en todas las operaciones
- **Autorización Estricta**: Solo usuarios con rol `ADMIN` o permiso `ADMINISTRACION` pueden revocar
- **Auditoría Inmutable**: Registro de todas las revocaciones en el sistema de auditoría
- **Seguridad por Defecto**: No exponer información de recursos inexistentes o fuera de la organización

## Architecture Context

### Layers Involved

**Esta US reutiliza la infraestructura creada en US-ACL-002 y agrega:**

1. **Domain Layer** (`domain/service/*`)
   - Nuevo método en `AclCarpetaService.revocarPermiso()`
   - Validación de existencia de ACL
   - Reglas de negocio para autorización

2. **Application Layer** (`application/validator/*`)
   - Validación de autorización del usuario solicitante
   - Validación de aislamiento de organización

3. **Infrastructure Layer** (`infrastructure/adapter/persistence/*`)
   - Método de eliminación en `AclCarpetaJpaRepository.deleteByUsuarioIdAndCarpetaIdAndOrganizacionId()`
   - Actualización de eventos de auditoría

4. **Presentation Layer (API)** (`api/controller/*`)
   - Nuevo endpoint: `DELETE /carpetas/{carpetaId}/permisos/{usuarioId}`
   - Manejo de códigos HTTP: 204, 403, 404, 409

### Files to be Modified/Created

```
backend/document-core/
├── src/main/java/.../
│   ├── domain/
│   │   └── service/
│   │       └── AclCarpetaService.java              # MODIFICAR: agregar método revocarPermiso()
│   │   └── exception/
│   │       └── AclNotFoundException.java           # CREAR: excepción para ACL no encontrado
│   │
│   ├── application/
│   │   └── validator/
│   │       └── AclCarpetaValidator.java            # MODIFICAR: agregar validación autorización
│   │
│   ├── infrastructure/
│   │   └── adapter/
│   │       ├── persistence/
│   │       │   └── jpa/
│   │       │       └── AclCarpetaJpaRepository.java  # MODIFICAR: agregar método delete
│   │       └── event/
│   │           └── AclCarpetaEventPublisher.java   # MODIFICAR: agregar evento ACL_REVOKED
│   │
│   └── api/
│       └── controller/
│           └── AclCarpetaController.java           # MODIFICAR: agregar endpoint DELETE
│
└── src/test/java/.../
    ├── domain/service/
    │   └── AclCarpetaServiceTest.java              # MODIFICAR: agregar tests de revocación
    ├── application/validator/
    │   └── AclCarpetaValidatorTest.java            # MODIFICAR: agregar tests de autorización
    └── api/controller/
        └── AclCarpetaControllerTest.java           # MODIFICAR: agregar tests del endpoint DELETE
```

## Implementation Steps

### Step 1: Create Domain Exception for ACL Not Found

**File**: `src/main/java/com/docflow/documentcore/domain/exception/AclNotFoundException.java`

**Action**: Crear excepción específica cuando un ACL no existe

**Function Signature**:
```java
public class AclNotFoundException extends RuntimeException {
    public AclNotFoundException(Long carpetaId, Long usuarioId);
    public AclNotFoundException(Long aclId);
}
```

**Implementation Steps**:

1. Crear clase que extienda `RuntimeException`
2. Agregar constructores para diferentes casos:
   - Constructor con `carpetaId` y `usuarioId` para búsquedas específicas
   - Constructor con `aclId` para búsquedas por ID
3. Mensaje descriptivo: `"ACL no encontrado para usuario {usuarioId} en carpeta {carpetaId}"`
4. Incluir código de error para auditoría: `ACL_NOT_FOUND`

**Dependencies**: 
- `java.lang.RuntimeException`

**Implementation Notes**:
- Esta excepción será capturada en el controller para retornar HTTP 404
- El mensaje NO debe revelar información sensible de otros tenants
- Usar `String.format()` para construcción de mensajes

**Example**:
```java
package com.docflow.documentcore.domain.exception;

public class AclNotFoundException extends RuntimeException {
    private static final String MESSAGE_BY_CARPETA_USUARIO = 
        "ACL no encontrado para usuario %d en carpeta %d";
    private static final String MESSAGE_BY_ID = 
        "ACL no encontrado con ID %d";
    
    public AclNotFoundException(Long carpetaId, Long usuarioId) {
        super(String.format(MESSAGE_BY_CARPETA_USUARIO, usuarioId, carpetaId));
    }
    
    public AclNotFoundException(Long aclId) {
        super(String.format(MESSAGE_BY_ID, aclId));
    }
}
```

---

### Step 2: Add Delete Method to JPA Repository

**File**: `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/jpa/AclCarpetaJpaRepository.java`

**Action**: Agregar método de eliminación con validación de tenant

**Function Signature**:
```java
@Repository
public interface AclCarpetaJpaRepository extends JpaRepository<AclCarpetaEntity, Long> {
    // ... métodos existentes de US-ACL-002 ...
    
    @Modifying
    @Transactional
    int deleteByUsuarioIdAndCarpetaIdAndOrganizacionId(
        Long usuarioId, 
        Long carpetaId, 
        Long organizacionId
    );
    
    Optional<AclCarpetaEntity> findByUsuarioIdAndCarpetaIdAndOrganizacionId(
        Long usuarioId, 
        Long carpetaId, 
        Long organizacionId
    );
}
```

**Implementation Steps**:

1. **Agregar método de búsqueda específica** `findByUsuarioIdAndCarpetaIdAndOrganizacionId()`:
   - Necesario para validar existencia antes de eliminar
   - Incluye validación de tenant en la consulta
   - Retorna `Optional<AclCarpetaEntity>`

2. **Agregar método de eliminación** `deleteByUsuarioIdAndCarpetaIdAndOrganizacionId()`:
   - Usa anotación `@Modifying` para indicar que modifica datos
   - Usa anotación `@Transactional` para asegurar consistencia
   - Retorna `int` con cantidad de registros eliminados (debería ser 0 o 1)
   - Spring Data genera automáticamente el DELETE WHERE basado en los parámetros

3. **Validar la firma del método**:
   - Los tres parámetros (`usuarioId`, `carpetaId`, `organizacionId`) son obligatorios
   - El orden de parámetros debe coincidir con el nombre del método
   - Spring Data traduce esto a: `DELETE FROM acl_carpetas WHERE usuario_id = ? AND carpeta_id = ? AND organizacion_id = ?`

**Dependencies**: 
- `org.springframework.data.jpa.repository.Modifying`
- `org.springframework.transaction.annotation.Transactional`
- `org.springframework.data.jpa.repository.JpaRepository`

**Implementation Notes**:
- El método retorna `int` para saber si se eliminó algo (1) o no existía (0)
- La inclusión de `organizacionId` asegura aislamiento de tenant
- No usar `deleteById()` porque no valida tenant

---

### Step 3: Add Revoke Method to Domain Service

**File**: `src/main/java/com/docflow/documentcore/domain/service/AclCarpetaService.java`

**Action**: Agregar método de negocio para revocar permisos

**Function Signature**:
```java
@Service
public class AclCarpetaService {
    // ... métodos existentes de US-ACL-002 ...
    
    public void revocarPermiso(Long carpetaId, Long usuarioId, Long organizacionId);
}
```

**Implementation Steps**:

1. **Inyectar dependencias necesarias**:
   ```java
   private final IAclCarpetaRepository aclRepository;
   private final AclCarpetaValidator validator;
   private final AclCarpetaEventPublisher eventPublisher;
   ```

2. **Implementar método `revocarPermiso()`**:
   - Validar que carpetaId, usuarioId y organizacionId no sean null
   - Buscar ACL existente usando repositorio
   - Si no existe, lanzar `AclNotFoundException`
   - Validar que el ACL pertenece a la organización correcta (tenant isolation)
   - Ejecutar eliminación en repositorio
   - Publicar evento de auditoría `ACL_REVOKED`

3. **Validaciones de negocio**:
   - No permitir revocar si es el único administrador de la carpeta (opcional, dependiendo de reglas de negocio)
   - Validar que carpeta y usuario existan antes de proceder
   - Validar aislamiento de organización

4. **Manejo de transacciones**:
   - Agregar `@Transactional` al método
   - Asegurar que eliminación y auditoría ocurran en la misma transacción

**Dependencies**: 
- `org.springframework.stereotype.Service`
- `org.springframework.transaction.annotation.Transactional`
- `com.docflow.documentcore.domain.repository.IAclCarpetaRepository`
- `com.docflow.documentcore.application.validator.AclCarpetaValidator`
- `com.docflow.documentcore.infrastructure.adapter.event.AclCarpetaEventPublisher`
- `com.docflow.documentcore.domain.exception.AclNotFoundException`

**Implementation Notes**:
- El método debe ser transaccional para consistencia
- Publicar evento DESPUÉS de eliminación exitosa
- El organizacionId se obtiene del JWT, nunca del cliente

**Example**:
```java
@Service
@Transactional
public class AclCarpetaService {
    
    private final IAclCarpetaRepository aclRepository;
    private final AclCarpetaValidator validator;
    private final AclCarpetaEventPublisher eventPublisher;
    
    public AclCarpetaService(
        IAclCarpetaRepository aclRepository,
        AclCarpetaValidator validator,
        AclCarpetaEventPublisher eventPublisher
    ) {
        this.aclRepository = aclRepository;
        this.validator = validator;
        this.eventPublisher = eventPublisher;
    }
    
    public void revocarPermiso(Long carpetaId, Long usuarioId, Long organizacionId) {
        // 1. Validar parámetros
        if (carpetaId == null || usuarioId == null || organizacionId == null) {
            throw new IllegalArgumentException("Todos los parámetros son obligatorios");
        }
        
        // 2. Buscar ACL existente
        Optional<AclCarpeta> aclOpt = aclRepository.findByCarpetaAndUsuarioAndOrganizacion(
            carpetaId, usuarioId, organizacionId
        );
        
        if (aclOpt.isEmpty()) {
            throw new AclNotFoundException(carpetaId, usuarioId);
        }
        
        AclCarpeta acl = aclOpt.get();
        
        // 3. Validar tenant isolation
        if (!acl.getOrganizacionId().equals(organizacionId)) {
            throw new OrganizacionIsolationException(
                "Carpeta pertenece a otra organización"
            );
        }
        
        // 4. Eliminar ACL
        aclRepository.deleteByCarpetaAndUsuarioAndOrganizacion(
            carpetaId, usuarioId, organizacionId
        );
        
        // 5. Publicar evento de auditoría
        eventPublisher.publishAclRevoked(acl);
    }
}
```

---

### Step 4: Add Authorization Validation to Validator

**File**: `src/main/java/com/docflow/documentcore/application/validator/AclCarpetaValidator.java`

**Action**: Agregar validación de autorización para revocación

**Function Signature**:
```java
@Component
public class AclCarpetaValidator {
    // ... métodos existentes de US-ACL-002 ...
    
    public void validarAutorizacionRevocacion(
        Long usuarioSolicitante, 
        Long carpetaId, 
        Long organizacionId, 
        List<String> roles
    );
}
```

**Implementation Steps**:

1. **Inyectar dependencias necesarias**:
   ```java
   private final CarpetaRepository carpetaRepository;
   private final IAclCarpetaRepository aclRepository;
   ```

2. **Implementar validación de autorización**:
   - Verificar si usuario tiene rol `ADMIN` en el token → permitir
   - Si no es ADMIN, verificar si tiene permiso `ADMINISTRACION` en la carpeta
   - Si no tiene ninguno, lanzar `UnauthorizedException`

3. **Validar existencia de recursos**:
   - Validar que carpeta existe en la organización
   - Si no existe, lanzar `CarpetaNotFoundException`
   - NO revelar información de carpetas de otros tenants

4. **Validación de tenant isolation**:
   - Asegurar que carpeta pertenece a la organización del token
   - Si no coincide, lanzar `OrganizacionIsolationException` (o retornar 404)

**Dependencies**: 
- `org.springframework.stereotype.Component`
- `com.docflow.documentcore.domain.repository.IAclCarpetaRepository`
- `com.docflow.documentcore.domain.exception.UnauthorizedException`
- `com.docflow.documentcore.domain.exception.CarpetaNotFoundException`

**Implementation Notes**:
- La autorización se basa en dos caminos: rol ADMIN O permiso ADMINISTRACION
- No revelar existencia de recursos fuera del tenant
- Los roles vienen del JWT decodificado en el controller

**Example**:
```java
@Component
public class AclCarpetaValidator {
    
    private final CarpetaRepository carpetaRepository;
    private final IAclCarpetaRepository aclRepository;
    
    public void validarAutorizacionRevocacion(
        Long usuarioSolicitante,
        Long carpetaId,
        Long organizacionId,
        List<String> roles
    ) {
        // 1. Validar que carpeta existe y pertenece al tenant
        Carpeta carpeta = carpetaRepository.findByIdAndOrganizacionId(carpetaId, organizacionId)
            .orElseThrow(() -> new CarpetaNotFoundException(carpetaId));
        
        // 2. Si usuario tiene rol ADMIN, permitir
        if (roles.contains("ADMIN")) {
            return;
        }
        
        // 3. Si no es ADMIN, verificar permiso ADMINISTRACION en la carpeta
        Optional<AclCarpeta> aclAdmin = aclRepository.findByCarpetaAndUsuario(
            carpetaId, usuarioSolicitante
        );
        
        if (aclAdmin.isEmpty() || !aclAdmin.get().canAdmin()) {
            throw new UnauthorizedException(
                "No tienes permiso ADMINISTRACION sobre esta carpeta"
            );
        }
    }
}
```

---

### Step 5: Add Event Publisher for ACL_REVOKED

**File**: `src/main/java/com/docflow/documentcore/infrastructure/adapter/event/AclCarpetaEventPublisher.java`

**Action**: Agregar método para publicar evento de auditoría de revocación

**Function Signature**:
```java
@Component
public class AclCarpetaEventPublisher {
    // ... métodos existentes de US-ACL-002 ...
    
    public void publishAclRevoked(AclCarpeta acl);
}
```

**Implementation Steps**:

1. **Inyectar dependencias**:
   ```java
   private final ApplicationEventPublisher eventPublisher;
   // o conexión directa al servicio de auditoría
   ```

2. **Crear evento `AclRevokedEvent`**:
   - Incluir: `carpetaId`, `usuarioId`, `organizacionId`, `nivelAcceso`, `timestamp`
   - Código de evento: `ACL_REVOKED`
   - Severidad: `INFO`

3. **Publicar evento**:
   - Usar `eventPublisher.publishEvent(event)` de Spring
   - O hacer POST directo al servicio de auditoría si es externo

4. **Manejo de errores**:
   - Si falla la publicación, loggear error pero NO fallar la transacción principal
   - Usar `@Async` para evitar bloquear la operación principal

**Dependencies**: 
- `org.springframework.context.ApplicationEventPublisher`
- `org.springframework.stereotype.Component`

**Implementation Notes**:
- La auditoría es importante pero no debe bloquear la operación principal
- Si hay sistema de auditoría externo, ajustar integración
- Incluir timestamp y usuario que ejecutó la acción

**Example**:
```java
@Component
public class AclCarpetaEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public AclCarpetaEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    public void publishAclRevoked(AclCarpeta acl) {
        AclRevokedEvent event = AclRevokedEvent.builder()
            .codigoEvento("ACL_REVOKED")
            .carpetaId(acl.getCarpetaId())
            .usuarioId(acl.getUsuarioId())
            .organizacionId(acl.getOrganizacionId())
            .nivelAcceso(acl.getNivelAccesoId())
            .timestamp(LocalDateTime.now())
            .build();
        
        eventPublisher.publishEvent(event);
    }
}
```

---

### Step 6: Add DELETE Endpoint to Controller

**File**: `src/main/java/com/docflow/documentcore/api/controller/AclCarpetaController.java`

**Action**: Agregar endpoint para revocar permisos

**Function Signature**:
```java
@RestController
@RequestMapping("/carpetas")
public class AclCarpetaController {
    // ... endpoints existentes de US-ACL-002 ...
    
    @DeleteMapping("/{carpetaId}/permisos/{usuarioId}")
    public ResponseEntity<Void> revocarPermiso(
        @PathVariable Long carpetaId,
        @PathVariable Long usuarioId,
        @AuthenticationPrincipal JwtAuthenticationToken token
    );
}
```

**Implementation Steps**:

1. **Inyectar dependencias**:
   ```java
   private final AclCarpetaService aclService;
   private final AclCarpetaValidator validator;
   ```

2. **Extraer claims del JWT**:
   ```java
   Long usuarioSolicitante = token.getClaim("usuario_id");
   Long organizacionId = token.getClaim("organizacion_id");
   List<String> roles = token.getClaim("roles");
   ```

3. **Validar autorización**:
   ```java
   validator.validarAutorizacionRevocacion(
       usuarioSolicitante, carpetaId, organizacionId, roles
   );
   ```

4. **Ejecutar revocación**:
   ```java
   aclService.revocarPermiso(carpetaId, usuarioId, organizacionId);
   ```

5. **Retornar respuesta**:
   - HTTP 204 No Content (sin cuerpo de respuesta)
   - No incluir ningún dato en el body

6. **Manejo de excepciones** (agregar en `@ControllerAdvice`):
   - `AclNotFoundException` → HTTP 404
   - `UnauthorizedException` → HTTP 403
   - `OrganizacionIsolationException` → HTTP 409 (o 404)
   - `IllegalArgumentException` → HTTP 400

**Dependencies**: 
- `org.springframework.web.bind.annotation.*`
- `org.springframework.http.ResponseEntity`
- `org.springframework.security.core.annotation.AuthenticationPrincipal`
- `org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken`

**Implementation Notes**:
- El `organizacion_id` DEBE venir del token, NUNCA del cliente
- Validar roles y permisos ANTES de ejecutar el servicio
- Respuesta 204 no debe incluir cuerpo
- Loggear todas las operaciones para auditoría

**Example**:
```java
@RestController
@RequestMapping("/carpetas")
public class AclCarpetaController {
    
    private final AclCarpetaService aclService;
    private final AclCarpetaValidator validator;
    
    public AclCarpetaController(
        AclCarpetaService aclService,
        AclCarpetaValidator validator
    ) {
        this.aclService = aclService;
        this.validator = validator;
    }
    
    @DeleteMapping("/{carpetaId}/permisos/{usuarioId}")
    public ResponseEntity<Void> revocarPermiso(
        @PathVariable Long carpetaId,
        @PathVariable Long usuarioId,
        @AuthenticationPrincipal JwtAuthenticationToken token
    ) {
        // 1. Extraer claims del token
        Long usuarioSolicitante = token.getClaim("usuario_id");
        Long organizacionId = token.getClaim("organizacion_id");
        List<String> roles = token.getClaim("roles");
        
        // 2. Validar autorización
        validator.validarAutorizacionRevocacion(
            usuarioSolicitante, carpetaId, organizacionId, roles
        );
        
        // 3. Ejecutar revocación
        aclService.revocarPermiso(carpetaId, usuarioId, organizacionId);
        
        // 4. Retornar 204 No Content
        return ResponseEntity.noContent().build();
    }
}
```

---

### Step 7: Add Exception Handler for Controller

**File**: `src/main/java/com/docflow/documentcore/api/exception/GlobalExceptionHandler.java`

**Action**: Agregar manejo de excepciones específicas de ACL

**Function Signature**:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    // ... handlers existentes ...
    
    @ExceptionHandler(AclNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAclNotFound(AclNotFoundException ex);
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex);
    
    @ExceptionHandler(OrganizacionIsolationException.class)
    public ResponseEntity<ErrorResponse> handleOrganizacionIsolation(OrganizacionIsolationException ex);
}
```

**Implementation Steps**:

1. **Crear clase `ErrorResponse`** (si no existe):
   ```java
   public class ErrorResponse {
       private String error;
       private String message;
       private LocalDateTime timestamp;
       private String path;
   }
   ```

2. **Agregar handler para `AclNotFoundException`**:
   - Retornar HTTP 404 Not Found
   - Mensaje: contenido de la excepción
   - Código de error: `ACL_NOT_FOUND`

3. **Agregar handler para `UnauthorizedException`**:
   - Retornar HTTP 403 Forbidden
   - Mensaje: "No tienes permiso para realizar esta acción"
   - Código de error: `FORBIDDEN`

4. **Agregar handler para `OrganizacionIsolationException`**:
   - Retornar HTTP 409 Conflict (o 404 para no revelar existencia)
   - Mensaje: "Recurso no encontrado o no pertenece a tu organización"
   - Código de error: `TENANT_ISOLATION`

5. **Loggear todas las excepciones**:
   - Usar logger para registrar detalles técnicos
   - NO incluir información sensible en los logs de producción

**Dependencies**: 
- `org.springframework.web.bind.annotation.ControllerAdvice`
- `org.springframework.web.bind.annotation.ExceptionHandler`
- `org.springframework.http.ResponseEntity`
- `org.slf4j.Logger`

**Implementation Notes**:
- Los mensajes de error NO deben revelar información de otros tenants
- Usar códigos de error consistentes para facilitar debugging
- Incluir timestamp y path del request en la respuesta

**Example**:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(AclNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAclNotFound(
        AclNotFoundException ex,
        HttpServletRequest request
    ) {
        log.warn("ACL no encontrado: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .error("ACL_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
        UnauthorizedException ex,
        HttpServletRequest request
    ) {
        log.warn("Acceso no autorizado: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .error("FORBIDDEN")
            .message(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    @ExceptionHandler(OrganizacionIsolationException.class)
    public ResponseEntity<ErrorResponse> handleOrganizacionIsolation(
        OrganizacionIsolationException ex,
        HttpServletRequest request
    ) {
        log.warn("Violación de aislamiento de tenant: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .error("NOT_FOUND")  // NO revelar que es problema de tenant
            .message("Recurso no encontrado")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
```

---

### Step 8: Write Unit Tests for Service

**File**: `src/test/java/com/docflow/documentcore/domain/service/AclCarpetaServiceTest.java`

**Action**: Agregar tests para el método `revocarPermiso()`

**Implementation Steps**:

1. **Test: Revocación exitosa**
   ```java
   @Test
   void shouldRevocarPermisoExistente() {
       // Given
       AclCarpeta acl = crearAclCarpeta(carpetaId=12, usuarioId=5, orgId=1);
       when(aclRepository.findByCarpetaAndUsuarioAndOrganizacion(12L, 5L, 1L))
           .thenReturn(Optional.of(acl));
       
       // When
       service.revocarPermiso(12L, 5L, 1L);
       
       // Then
       verify(aclRepository, times(1))
           .deleteByCarpetaAndUsuarioAndOrganizacion(12L, 5L, 1L);
       verify(eventPublisher, times(1))
           .publishAclRevoked(acl);
   }
   ```

2. **Test: ACL no encontrado**
   ```java
   @Test
   void shouldLanzarExcepcionSiAclNoExiste() {
       // Given
       when(aclRepository.findByCarpetaAndUsuarioAndOrganizacion(12L, 5L, 1L))
           .thenReturn(Optional.empty());
       
       // When & Then
       assertThrows(AclNotFoundException.class, 
           () -> service.revocarPermiso(12L, 5L, 1L));
       
       verify(aclRepository, never()).deleteByCarpetaAndUsuarioAndOrganizacion(any(), any(), any());
       verify(eventPublisher, never()).publishAclRevoked(any());
   }
   ```

3. **Test: Validación de aislamiento de organización**
   ```java
   @Test
   void shouldLanzarExcepcionSiOrganizacionDiferente() {
       // Given
       AclCarpeta acl = crearAclCarpeta(carpetaId=12, usuarioId=5, orgId=2);
       when(aclRepository.findByCarpetaAndUsuarioAndOrganizacion(12L, 5L, 1L))
           .thenReturn(Optional.empty());  // No encuentra porque orgId no coincide
       
       // When & Then
       assertThrows(AclNotFoundException.class,
           () -> service.revocarPermiso(12L, 5L, 1L));
   }
   ```

4. **Test: Parámetros nulos**
   ```java
   @Test
   void shouldLanzarExcepcionSiParametrosNulos() {
       assertThrows(IllegalArgumentException.class,
           () -> service.revocarPermiso(null, 5L, 1L));
       assertThrows(IllegalArgumentException.class,
           () -> service.revocarPermiso(12L, null, 1L));
       assertThrows(IllegalArgumentException.class,
           () -> service.revocarPermiso(12L, 5L, null));
   }
   ```

**Dependencies**: 
- JUnit 5: `@Test`, `@ExtendWith(MockitoExtension.class)`
- Mockito: `@Mock`, `@InjectMocks`, `when()`, `verify()`
- AssertJ: `assertThat()`, `assertThrows()`

**Implementation Notes**:
- Usar `@ExtendWith(MockitoExtension.class)` para inicializar mocks
- Verificar que los métodos del repositorio se llamen con los parámetros correctos
- Verificar que eventos de auditoría se publiquen solo en casos exitosos
- Cobertura objetivo: > 90%

---

### Step 9: Write Unit Tests for Validator

**File**: `src/test/java/com/docflow/documentcore/application/validator/AclCarpetaValidatorTest.java`

**Action**: Agregar tests para `validarAutorizacionRevocacion()`

**Implementation Steps**:

1. **Test: Usuario con rol ADMIN puede revocar**
   ```java
   @Test
   void shouldPermitirRevocacionConRolAdmin() {
       // Given
       List<String> roles = Arrays.asList("ADMIN");
       Carpeta carpeta = crearCarpeta(id=12, orgId=1);
       when(carpetaRepository.findByIdAndOrganizacionId(12L, 1L))
           .thenReturn(Optional.of(carpeta));
       
       // When & Then
       assertDoesNotThrow(() -> 
           validator.validarAutorizacionRevocacion(999L, 12L, 1L, roles)
       );
   }
   ```

2. **Test: Usuario con permiso ADMINISTRACION puede revocar**
   ```java
   @Test
   void shouldPermitirRevocacionConPermisoAdministracion() {
       // Given
       List<String> roles = Arrays.asList("USER");
       Carpeta carpeta = crearCarpeta(id=12, orgId=1);
       AclCarpeta aclAdmin = crearAclCarpeta(nivel=ADMINISTRACION);
       
       when(carpetaRepository.findByIdAndOrganizacionId(12L, 1L))
           .thenReturn(Optional.of(carpeta));
       when(aclRepository.findByCarpetaAndUsuario(12L, 10L))
           .thenReturn(Optional.of(aclAdmin));
       
       // When & Then
       assertDoesNotThrow(() -> 
           validator.validarAutorizacionRevocacion(10L, 12L, 1L, roles)
       );
   }
   ```

3. **Test: Usuario sin permisos no puede revocar**
   ```java
   @Test
   void shouldLanzarExcepcionSiUsuarioSinPermisos() {
       // Given
       List<String> roles = Arrays.asList("USER");
       Carpeta carpeta = crearCarpeta(id=12, orgId=1);
       
       when(carpetaRepository.findByIdAndOrganizacionId(12L, 1L))
           .thenReturn(Optional.of(carpeta));
       when(aclRepository.findByCarpetaAndUsuario(12L, 10L))
           .thenReturn(Optional.empty());  // No tiene ACL
       
       // When & Then
       assertThrows(UnauthorizedException.class,
           () -> validator.validarAutorizacionRevocacion(10L, 12L, 1L, roles)
       );
   }
   ```

4. **Test: Carpeta no existe**
   ```java
   @Test
   void shouldLanzarExcepcionSiCarpetaNoExiste() {
       // Given
       List<String> roles = Arrays.asList("USER");
       when(carpetaRepository.findByIdAndOrganizacionId(12L, 1L))
           .thenReturn(Optional.empty());
       
       // When & Then
       assertThrows(CarpetaNotFoundException.class,
           () -> validator.validarAutorizacionRevocacion(10L, 12L, 1L, roles)
       );
   }
   ```

5. **Test: Carpeta de otro tenant**
   ```java
   @Test
   void shouldLanzarExcepcionSiCarpetaDeOtroTenant() {
       // Given
       List<String> roles = Arrays.asList("ADMIN");
       when(carpetaRepository.findByIdAndOrganizacionId(12L, 1L))
           .thenReturn(Optional.empty());  // No encuentra porque orgId no coincide
       
       // When & Then
       assertThrows(CarpetaNotFoundException.class,
           () -> validator.validarAutorizacionRevocacion(10L, 12L, 1L, roles)
       );
   }
   ```

**Dependencies**: 
- JUnit 5, Mockito, AssertJ

**Implementation Notes**:
- Probar ambos caminos de autorización: rol ADMIN y permiso ADMINISTRACION
- Validar que no se revele información de otros tenants
- Verificar mensajes de excepción claros

---

### Step 10: Write Integration Tests for Controller

**File**: `src/test/java/com/docflow/documentcore/api/controller/AclCarpetaControllerTest.java`

**Action**: Agregar tests de integración para el endpoint DELETE

**Implementation Steps**:

1. **Test: Revocación exitosa (HTTP 204)**
   ```java
   @Test
   @WithMockJwtAuth(userId = 1L, organizacionId = 1L, roles = {"ADMIN"})
   void shouldRevocarPermisoYRetornar204() throws Exception {
       // Given
       doNothing().when(aclService).revocarPermiso(12L, 5L, 1L);
       
       // When & Then
       mockMvc.perform(delete("/carpetas/12/permisos/5"))
           .andExpect(status().isNoContent())
           .andExpect(content().string(""));  // Sin cuerpo
   }
   ```

2. **Test: ACL no encontrado (HTTP 404)**
   ```java
   @Test
   @WithMockJwtAuth(userId = 1L, organizacionId = 1L, roles = {"ADMIN"})
   void shouldRetornar404SiAclNoExiste() throws Exception {
       // Given
       doThrow(new AclNotFoundException(12L, 5L))
           .when(aclService).revocarPermiso(12L, 5L, 1L);
       
       // When & Then
       mockMvc.perform(delete("/carpetas/12/permisos/5"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("ACL_NOT_FOUND"))
           .andExpect(jsonPath("$.message").exists());
   }
   ```

3. **Test: Usuario sin permisos (HTTP 403)**
   ```java
   @Test
   @WithMockJwtAuth(userId = 10L, organizacionId = 1L, roles = {"USER"})
   void shouldRetornar403SiUsuarioSinPermisos() throws Exception {
       // Given
       doThrow(new UnauthorizedException("No tienes permiso"))
           .when(validator).validarAutorizacionRevocacion(any(), any(), any(), any());
       
       // When & Then
       mockMvc.perform(delete("/carpetas/12/permisos/5"))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.error").value("FORBIDDEN"));
   }
   ```

4. **Test: Carpeta de otro tenant (HTTP 404)**
   ```java
   @Test
   @WithMockJwtAuth(userId = 1L, organizacionId = 1L, roles = {"ADMIN"})
   void shouldRetornar404SiCarpetaDeOtroTenant() throws Exception {
       // Given
       doThrow(new CarpetaNotFoundException(12L))
           .when(validator).validarAutorizacionRevocacion(any(), any(), any(), any());
       
       // When & Then
       mockMvc.perform(delete("/carpetas/12/permisos/5"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.error").value("NOT_FOUND"));
   }
   ```

5. **Test: Token JWT ausente (HTTP 401)**
   ```java
   @Test
   void shouldRetornar401SinToken() throws Exception {
       mockMvc.perform(delete("/carpetas/12/permisos/5"))
           .andExpect(status().isUnauthorized());
   }
   ```

**Dependencies**: 
- Spring Boot Test: `@SpringBootTest`, `@AutoConfigureMockMvc`
- MockMvc: `@Autowired MockMvc`
- Custom annotation: `@WithMockJwtAuth` (para simular JWT)

**Implementation Notes**:
- Usar `@WebMvcTest(AclCarpetaController.class)` para tests aislados del controller
- O usar `@SpringBootTest` para tests de integración completos
- Verificar respuestas HTTP exactas (204, 403, 404)
- Verificar estructura JSON de errores

---

### Step 11: Update Technical Documentation

**Action**: Actualizar documentación técnica del proyecto según los cambios realizados

**Implementation Steps**:

1. **Review Changes**: Analizar todos los cambios de código realizados:
   - Nuevo endpoint DELETE en AclCarpetaController
   - Nuevos métodos en AclCarpetaService y validator
   - Nueva excepción AclNotFoundException
   - Nuevo método delete en JpaRepository

2. **Identify Documentation Files**: Determinar qué archivos de documentación necesitan actualizaciones:
   - `ai-specs/specs/api-spec.yml` → agregar endpoint DELETE
   - `ai-specs/specs/data-model.md` → NO requiere cambios (usa misma tabla)
   - `backend/document-core/README.md` → actualizar con nueva funcionalidad

3. **Update API Specification** (`ai-specs/specs/api-spec.yml`):
   - Agregar definición del endpoint DELETE `/carpetas/{carpetaId}/permisos/{usuarioId}`
   - Documentar parámetros de ruta: `carpetaId`, `usuarioId`
   - Documentar header de autenticación: `Authorization: Bearer {token}`
   - Documentar respuestas:
     - `204 No Content`: Revocación exitosa
     - `401 Unauthorized`: Token ausente o inválido
     - `403 Forbidden`: Usuario sin permisos
     - `404 Not Found`: ACL o carpeta no existe
     - `409 Conflict`: Carpeta de otro tenant
   - Agregar ejemplos de request y response

4. **Update Service README** (`backend/document-core/README.md`):
   - Agregar sección sobre "Revocación de Permisos ACL"
   - Documentar el flujo de autorización (ADMIN o ADMINISTRACION)
   - Explicar que es hard delete (no soft delete)
   - Mencionar eventos de auditoría generados
   - Incluir ejemplo de uso del endpoint

5. **Verify Documentation**:
   - Confirmar que todos los cambios están reflejados
   - Verificar que la documentación sigue la estructura establecida
   - Verificar que esté en inglés según `documentation-standards.md`

6. **Report Updates**: Documentar qué archivos fueron actualizados:
   - `ai-specs/specs/api-spec.yml`: Agregado endpoint DELETE para revocación de ACL
   - `backend/document-core/README.md`: Documentado flujo de revocación de permisos

**References**: 
- `ai-specs/specs/documentation-standards.md`
- All documentation must be written in English

**Notes**: 
- Este paso es OBLIGATORIO antes de considerar la implementación completa
- No omitir la actualización de documentación

---

## Implementation Order

La implementación debe seguir este orden estricto:

2. **Step 1**: Create Domain Exception (AclNotFoundException)
3. **Step 2**: Add Delete Method to JPA Repository
4. **Step 3**: Add Revoke Method to Domain Service
5. **Step 4**: Add Authorization Validation to Validator
6. **Step 5**: Add Event Publisher for ACL_REVOKED
7. **Step 6**: Add DELETE Endpoint to Controller
8. **Step 7**: Add Exception Handler for Controller
9. **Step 8**: Write Unit Tests for Service
10. **Step 9**: Write Unit Tests for Validator
11. **Step 10**: Write Integration Tests for Controller
12. **Step 11**: Update Technical Documentation

**Nota importante**: La documentación técnica debe actualizarse ANTES de considerar la implementación completa.

---

## Testing Checklist

Después de la implementación, verificar:

- [ ] Revocación exitosa retorna HTTP 204 sin cuerpo
- [ ] Intento de revocar ACL inexistente retorna HTTP 404
- [ ] Usuario sin rol ADMIN ni permiso ADMINISTRACION retorna HTTP 403
- [ ] Usuario con rol ADMIN puede revocar cualquier permiso de su tenant
- [ ] Usuario con permiso ADMINISTRACION puede revocar permisos de esa carpeta
- [ ] Carpeta de otro tenant retorna HTTP 404 (no 409, para no revelar existencia)
- [ ] Evento de auditoría `ACL_REVOKED` se publica correctamente
- [ ] El método de eliminación incluye `organizacion_id` en WHERE clause
- [ ] Después de revocar, usuario afectado no puede acceder a la carpeta (HTTP 403)
- [ ] Tests unitarios del servicio tienen cobertura > 90%
- [ ] Tests unitarios del validator tienen cobertura > 90%
- [ ] Tests de integración del controller cubren todos los códigos HTTP
- [ ] Documentación API actualizada con el nuevo endpoint

---

## Error Response Format

Todos los endpoints deben usar el siguiente formato de error:

```json
{
  "error": "ACL_NOT_FOUND",
  "message": "ACL no encontrado para usuario 5 en carpeta 12",
  "timestamp": "2026-02-03T10:15:30Z",
  "path": "/carpetas/12/permisos/5"
}
```

**Códigos de error específicos para esta US:**
- `ACL_NOT_FOUND` → HTTP 404
- `FORBIDDEN` → HTTP 403
- `UNAUTHORIZED` → HTTP 401
- `NOT_FOUND` → HTTP 404 (para tenant isolation)
- `BAD_REQUEST` → HTTP 400

---

## HTTP Status Code Mapping

| Escenario | Status Code | Body |
|-----------|-------------|------|
| Revocación exitosa | 204 No Content | Vacío |
| ACL no existe | 404 Not Found | `{"error": "ACL_NOT_FOUND", ...}` |
| Usuario sin permisos | 403 Forbidden | `{"error": "FORBIDDEN", ...}` |
| Token inválido/ausente | 401 Unauthorized | `{"error": "UNAUTHORIZED", ...}` |
| Carpeta de otro tenant | 404 Not Found | `{"error": "NOT_FOUND", ...}` |
| Parámetros inválidos | 400 Bad Request | `{"error": "BAD_REQUEST", ...}` |

**Nota importante sobre seguridad:**
- NO usar 409 Conflict para violaciones de tenant isolation
- Usar 404 Not Found para no revelar existencia de recursos en otros tenants
- Los mensajes de error NO deben incluir información sensible

---

## Dependencies

Esta US depende de:

- **US-ACL-002**: Infraestructura de ACL (tabla, entidades, repositorios) debe estar implementada
- **US-ACL-001** (opcional): Sistema de auditoría para eventos `ACL_REVOKED`
- **JWT Authentication**: Gateway debe proveer token JWT con claims: `usuario_id`, `organizacion_id`, `roles`

**Librerías adicionales necesarias:**
- Spring Boot Starter Web (ya incluida)
- Spring Boot Starter Data JPA (ya incluida)
- Spring Boot Starter Security (ya incluida)
- JUnit 5 (ya incluida)
- Mockito (ya incluida)
- AssertJ (ya incluida)

**No se requieren nuevas dependencias en `pom.xml`**.

---

## Notes

**Diferencias con US-ACL-002 (crear ACL):**
- US-ACL-002 crea nuevas entradas ACL (POST)
- US-ACL-003 elimina entradas ACL existentes (DELETE)
- Ambas comparten la misma tabla, entidades y repositorio
- US-ACL-003 NO requiere migración de base de datos

**Decisiones de diseño importantes:**
1. **Hard Delete vs Soft Delete**: Se implementa hard delete (eliminación física). Si en el futuro se requiere soft delete, agregar columna `fecha_eliminacion` y cambiar lógica de eliminación.
   
2. **Autorización**: Dos caminos posibles:
   - Rol `ADMIN` global en la organización
   - Permiso `ADMINISTRACION` específico en la carpeta
   
3. **Tenant Isolation**: El `organizacion_id` se incluye en todas las queries para asegurar aislamiento de datos.

4. **Auditoría**: Todas las revocaciones se registran en el sistema de auditoría con código `ACL_REVOKED`.

5. **Seguridad**: No revelar existencia de recursos fuera del tenant (usar 404 en lugar de 409).

**Reglas de negocio:**
- No se puede revocar el último permiso de ADMINISTRACION de una carpeta (opcional, evaluar con equipo)
- La revocación es inmediata y no reversible (a menos que se vuelva a conceder)
- Solo afecta el permiso directo, no los permisos heredados de carpetas padre

**Consideraciones de rendimiento:**
- Índice compuesto en `(carpeta_id, usuario_id, organizacion_id)` para queries rápidas
- Transacción atómica: eliminación + auditoría
- Timeout configurado: 10 segundos máximo

**Consideraciones de escalabilidad:**
- Si el volumen de revocaciones es muy alto, considerar queue asíncrono para auditoría
- Cache de permisos debe invalidarse tras revocación

---

## Next Steps After Implementation

Después de completar la implementación de US-ACL-003:

1. **Code Review**: Solicitar revisión de código por otro desarrollador
   - Verificar adherencia a principios DDD
   - Revisar cobertura de tests (objetivo: > 90%)
   - Validar manejo de errores y seguridad

2. **Integration Testing**: Ejecutar tests de integración completos
   - Probar endpoint con Postman/curl
   - Verificar logs de auditoría
   - Validar aislamiento de tenant en BD

3. **Performance Testing**: Medir performance de eliminación
   - Objetivo: < 100ms para operación completa
   - Verificar que índices de BD estén optimizados

4. **Security Review**: Revisión de seguridad
   - Verificar que no se revele información de otros tenants
   - Validar que JWT claims se usen correctamente
   - Confirmar que logs no contengan información sensible

5. **Documentation Review**: Verificar que documentación esté completa
   - API spec actualizada
   - README del servicio actualizado
   - Ejemplos de uso incluidos

6. **Merge to Main**: Fusionar feature branch
   - Resolver conflictos si existen
   - Actualizar changelog
   - Tag de versión si aplica

7. **Deploy to Dev Environment**: Desplegar a entorno de desarrollo
   - Ejecutar migraciones de BD (si hubiera nuevas)
   - Verificar funcionamiento en entorno real
   - Monitorear logs para detectar errores

8. **Create Frontend Ticket**: Crear ticket para implementación frontend
   - US-ACL-003 Frontend: Componente para revocar permisos
   - Incluir referencia a esta implementación backend

---

## Implementation Verification

**Checklist final antes de considerar completa la implementación:**

### Code Quality
- [ ] Código sigue principios SOLID
- [ ] No hay código duplicado (DRY)
- [ ] Nombres de variables/métodos son descriptivos
- [ ] Comentarios en inglés donde sea necesario
- [ ] Sin warnings de compilación
- [ ] Código formateado según estándares del proyecto

### Functionality
- [ ] Endpoint DELETE funciona correctamente
- [ ] Validación de autorización implementada
- [ ] Aislamiento de tenant validado
- [ ] Eventos de auditoría se publican
- [ ] Excepciones se manejan apropiadamente

### Testing
- [ ] Tests unitarios del servicio: cobertura > 90%
- [ ] Tests unitarios del validator: cobertura > 90%
- [ ] Tests de integración del controller: todos los casos cubiertos
- [ ] Todos los tests pasan (green)
- [ ] Tests siguen naming convention: `should_DoSomething_When_Condition`

### Integration
- [ ] Integración con sistema de auditoría funciona
- [ ] JWT claims se extraen correctamente
- [ ] Respuestas HTTP correctas (204, 403, 404)
- [ ] Logs apropiados en todos los niveles

### Documentation
- [ ] **API spec actualizada** (`api-spec.yml`)
- [ ] **README del servicio actualizado**
- [ ] Código autoexplicativo y bien comentado
- [ ] Ejemplos de uso incluidos
- [ ] Documentación en inglés

### Security
- [ ] `organizacion_id` solo del token, nunca del cliente
- [ ] No se revela información de otros tenants
- [ ] Validación de autorización en cada request
- [ ] Logs no contienen información sensible

### Performance
- [ ] Operación completa < 100ms
- [ ] Queries optimizadas con índices correctos
- [ ] Sin N+1 queries
- [ ] Transacciones apropiadas

---

**Última actualización**: 3 de febrero de 2026
**Versión del plan**: 1.0
**Ticket relacionado**: US-ACL-003
**Dependencias**: US-ACL-002 (implementación de infraestructura ACL)
