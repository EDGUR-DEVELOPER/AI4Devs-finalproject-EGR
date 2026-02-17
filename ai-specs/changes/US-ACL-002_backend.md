# Backend Implementation Plan: US-ACL-002 Conceder permiso de carpeta a usuario (crear ACL)

## Overview

Implementar un sistema de lista de control de acceso (ACL) a nivel de carpeta que permita la asignación granular de permisos (`LECTURA`, `ESCRITURA`, `ADMINISTRACION`) a usuarios específicos dentro de una organización. La implementación seguirá principios de Domain-Driven Design (DDD) con separación clara entre capas de dominio, aplicación e infraestructura (Hexagonal Architecture / Ports & Adapters).

**Principios Clave:**
- **Aislamiento por Tenant**: Validación obligatoria de `organizacion_id` en todas las operaciones
- **Precedencia de Permisos**: Permiso explícito en documento > permiso de carpeta directo > permiso heredado recursivo
- **Auditoría Inmutable**: Todos los cambios deben ser registrados en eventos auditables
- **Seguridad por Defecto**: No exponer información de recursos inexistentes o fuera de la organización

## Architecture Context

### Layers Involved

1. **Domain Layer** (`domain/model/*` y `domain/repository/*`)
   - `AclCarpeta`: Entidad de dominio inmutable
   - `IAclCarpetaRepository`: Puerto de repositorio (interface)
   - Reglas de negocio encapsuladas en entidades de dominio

2. **Application Layer** (`application/service/*` y `application/validator/*`)
   - `AclCarpetaService`: Orquestación de casos de uso
   - `AclCarpetaValidator`: Reglas de validación de negocio
   - Coordinación entre dominio e infraestructura

3. **Infrastructure Layer** (`infrastructure/adapter/*`)
   - **Persistence**: `AclCarpetaEntity`, `AclCarpetaJpaRepository`, `AclCarpetaRepositoryAdapter`
   - **Mapper**: `AclCarpetaMapper` (MapStruct para domain ↔ entity)
   - **Event**: `AclCarpetaEventPublisher` (auditoría)
   - **Database Migration**: `V002__Create_ACL_Carpetas_Table.sql` (Flyway)

4. **Presentation Layer (API)** (`api/controller/*`, `api/dto/*`, `api/mapper/*`)
   - `AclCarpetaController`: Endpoints REST
   - DTOs: `CreateAclCarpetaDTO`, `UpdateAclCarpetaDTO`, `AclCarpetaResponseDTO`
   - `AclCarpetaDtoMapper`: MapStruct para DTO ↔ domain
   

### Project Structure

```
backend/document-core/
├── src/main/java/com/docflow/.../
│   ├── domain/                                    # CAPA DE DOMINIO
│   │   ├── model/
│   │   │   └── acl/
│   │   │       ├── AclCarpeta.java                # Entidad de dominio inmutable
│   │   │       └── AclCarpetaId.java              # Value Object (opcional)
│   │   └── repository/
│   │       └── IAclCarpetaRepository.java         # Puerto (interface)
│   │
│   ├── application/                               # CAPA DE APLICACIÓN
│   │   ├── service/
│   │   │   └── AclCarpetaService.java             # Orquestación de casos de uso
│   │   └── validator/
│   │       └── AclCarpetaValidator.java           # Validaciones de negocio
│   │
│   ├── infrastructure/                            # CAPA DE INFRAESTRUCTURA
│   │   └── adapter/
│   │       ├── persistence/
│   │       │   ├── entity/
│   │       │   │   └── AclCarpetaEntity.java      # Entidad JPA
│   │       │   ├── jpa/
│   │       │   │   └── AclCarpetaJpaRepository.java  # Spring Data JPA
│   │       │   ├── mapper/
│   │       │   │   └── AclCarpetaMapper.java      # MapStruct: Domain ↔ Entity
│   │       │   └── AclCarpetaRepositoryAdapter.java  # Implementación del puerto
│   │       └── event/
│   │           └── AclCarpetaEventPublisher.java  # Publicación de eventos
│   │
│   └── api/                                       # CAPA DE PRESENTACIÓN
│       ├── controller/
│       │   └── AclCarpetaController.java          # Endpoints REST
│       ├── dto/
│       │   ├── CreateAclCarpetaDTO.java           # DTO de entrada (POST)
│       │   ├── UpdateAclCarpetaDTO.java           # DTO de entrada (PATCH)
│       │   ├── AclCarpetaResponseDTO.java         # DTO de salida
│       │   ├── UsuarioEmbebidoDTO.java            # DTO embebido
│       │   └── NivelAccesoDTO.java                # DTO embebido
│       └── mapper/
│           └── AclCarpetaDtoMapper.java           # MapStruct: DTO ↔ Domain
│
├── src/main/resources/
│   └── db/migration/
│       └── V002__Create_ACL_Carpetas_Table.sql    # Migración Flyway
│
└── src/test/java/.../
    ├── application/service/
    │   └── AclCarpetaServiceTest.java             # Tests del servicio
    ├── application/validator/
    │   └── AclCarpetaValidatorTest.java           # Tests del validador
    ├── infrastructure/adapter/persistence/
    │   └── AclCarpetaRepositoryAdapterTest.java   # Tests del adaptador
    └── api/controller/
        └── AclCarpetaControllerTest.java          # Tests del controller
```

## Implementation Steps

### Step 1: Create Database Migration

**File**: `src/main/resources/db/migration/V002__Create_ACL_Carpetas_Table.sql`

**Action**: Crear tabla de persistencia para ACLs de carpetas

**Implementation Steps**:

1. Crear archivo SQL con migración Flyway
2. Definir tabla `acl_carpetas` con las siguientes columnas:
   - `id BIGSERIAL PRIMARY KEY`
   - `carpeta_id BIGINT NOT NULL REFERENCES carpetas(id)`
   - `usuario_id BIGINT NOT NULL REFERENCES usuarios(id)`
   - `organizacion_id BIGINT NOT NULL REFERENCES organizaciones(id)`
   - `nivel_acceso_id BIGINT NOT NULL REFERENCES nivel_acceso(id)`
   - `recursivo BOOLEAN NOT NULL DEFAULT false`
   - `fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`
   - `fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`

3. Crear índices para optimización:
   - Índice único compuesto: `UNIQUE(carpeta_id, usuario_id)` para evitar duplicados
   - Índice en `usuario_id` para queries por usuario
   - Índice en `organizacion_id` para aislamiento por tenant
   - Índice en `(carpeta_id, recursivo)` para búsquedas jerárquicas

4. Agregar trigger para actualizar `fecha_actualizacion` automáticamente

5. Validación de restricciones referenciales en cascada (ON DELETE CASCADE)

**SQL Structure**:
```sql
CREATE TABLE IF NOT EXISTS acl_carpetas (
  id BIGSERIAL PRIMARY KEY,
  carpeta_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  organizacion_id BIGINT NOT NULL,
  nivel_acceso_id BIGINT NOT NULL,
  recursivo BOOLEAN NOT NULL DEFAULT false,
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT fk_acl_carpeta FOREIGN KEY (carpeta_id) REFERENCES carpetas(id) ON DELETE CASCADE,
  CONSTRAINT fk_acl_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
  CONSTRAINT fk_acl_organizacion FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE CASCADE,
  CONSTRAINT fk_acl_nivel FOREIGN KEY (nivel_acceso_id) REFERENCES nivel_acceso(id),
  CONSTRAINT uq_acl_carpeta_usuario UNIQUE(carpeta_id, usuario_id)
);

CREATE INDEX idx_acl_usuario ON acl_carpetas(usuario_id);
CREATE INDEX idx_acl_organizacion ON acl_carpetas(organizacion_id);
CREATE INDEX idx_acl_carpeta_recursivo ON acl_carpetas(carpeta_id, recursivo);
```

**Dependencies**: 
- Flyway debe estar configurado en `pom.xml`
- Tablas: `carpetas`, `usuarios`, `organizaciones`, `nivel_acceso` deben existir

**Implementation Notes**:
- Usar nomenclatura `V00X__` para versiones Flyway
- Todas las columnas deben tener restricciones NOT NULL donde corresponda
- Los índices son críticos para performance en queries de auditoría y evaluación de permisos

---

### Step 2: Create Value Objects and Domain Model

**File**: `src/main/java/.../domain/model/acl/AclCarpeta.java`

**Action**: Crear entidad de dominio inmutable siguiendo principios DDD

**Function Signature**:
```java
public class AclCarpeta {
    private final Long id;
    private final Long carpetaId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final Long nivelAccesoId;
    private final Boolean recursivo;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaActualizacion;
}
```

**Implementation Steps**:

1. Crear clase `AclCarpeta` como entidad de dominio inmutable
   - Usar `final` en todos los campos
   - Constructor privado con builder
   - Métodos getter sin setters

2. Implementar Value Objects:
   - `AclCarpetaId` (envuelve Long)
   - `NivelAcceso` (enum o VO para LECTURA, ESCRITURA, ADMINISTRACION)

3. Agregar métodos de negocio:
   - `canRead()`: validar permiso de lectura
   - `canWrite()`: validar permiso de escritura
   - `canAdmin()`: validar permiso de administración
   - `isHeritagePermission()`: determinar si es herencia
   - `equalsIgnoreTimestamps()`: comparación sin timestamps

4. Implementar builder pattern para construcción
   ```java
   public static AclCarpetaBuilder builder() { ... }
   ```

5. Implementar `equals()` y `hashCode()` basados en id y claves naturales

6. Agregar métodos de validación privados

**Dependencies**: 
- `java.time.LocalDateTime`
- Anotaciones Lombok (opcional): `@Value`, `@Builder`

**Implementation Notes**:
- La entidad de dominio es totalmente independiente de la persistencia
- No debe tener referencias a JPA o Spring
- Es responsabilidad del servicio validar las reglas de negocio

---

### Step 3: Create Repository Interface (Port)

**File**: `src/main/java/.../domain/repository/IAclCarpetaRepository.java`

**Action**: Definir contrato de acceso a datos (Puerto Hexagonal)

**Function Signature**:
```java
public interface IAclCarpetaRepository {
    AclCarpeta save(AclCarpeta acl);
    Optional<AclCarpeta> findById(Long id);
    Optional<AclCarpeta> findByCarpetaAndUsuario(Long carpetaId, Long usuarioId);
    List<AclCarpeta> findByCarpeta(Long carpetaId);
    List<AclCarpeta> findByUsuarioAndOrganizacion(Long usuarioId, Long organizacionId);
    void delete(Long id);
    boolean exists(Long carpetaId, Long usuarioId);
}
```

**Implementation Steps**:

1. Crear interface que defina operaciones de repositorio
2. Métodos de búsqueda optimizados para casos de uso:
   - `findByCarpetaAndUsuario`: validar duplicados antes de crear
   - `findByCarpeta`: listar ACLs de una carpeta
   - `findByUsuarioAndOrganizacion`: auditoría y validación de tenant
3. Sin anotaciones de persistencia (es un puerto, no una implementación)
4. Retornar tipos de dominio (`AclCarpeta`), no entities

**Dependencies**: 
- `java.util.Optional`
- `java.util.List`
- Tipo de dominio `AclCarpeta`

**Implementation Notes**:
- Este es un contrato agnóstico de la implementación
- Múltiples adaptadores podrían implementar esta interface (SQL, NoSQL, etc.)

---

### Step 4: Create Repository Adapter (Persistence Implementation)

**File**: `src/main/java/.../infrastructure/adapter/persistence/jpa/AclCarpetaJpaRepository.java`

**Action**: Crear JPA Repository para acceso a datos

**Function Signature**:
```java
@Repository
public interface AclCarpetaJpaRepository extends JpaRepository<AclCarpetaEntity, Long> {
    Optional<AclCarpetaEntity> findByCarpetaIdAndUsuarioId(Long carpetaId, Long usuarioId);
    List<AclCarpetaEntity> findByCarpetaId(Long carpetaId);
    List<AclCarpetaEntity> findByUsuarioIdAndOrganizacionId(Long usuarioId, Long organizacionId);
    boolean existsByCarpetaIdAndUsuarioId(Long carpetaId, Long usuarioId);
    void deleteById(Long id);
}
```

**Implementation Steps**:

1. Crear interface que extienda `JpaRepository<AclCarpetaEntity, Long>`
2. Definir métodos de query personalizada (Spring Data genera las queries automáticamente)
3. Anotación `@Repository` para que Spring la reconozca
4. Métodos específicos para casos de uso de la aplicación

**Dependencies**: 
- Spring Data JPA
- `org.springframework.stereotype.Repository`
- `org.springframework.data.jpa.repository.JpaRepository`

**Implementation Notes**:
- Spring Data automáticamente genera queries SQL basadas en nombres de método
- Para queries complejas, usar `@Query` con JPQL o SQL nativo

---

### Step 5: Create Entity JPA

**File**: `src/main/java/.../infrastructure/adapter/persistence/entity/AclCarpetaEntity.java`

**Action**: Crear entidad JPA para mapping con base de datos

**Function Signature**:
```java
@Entity
@Table(name = "acl_carpetas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"carpeta_id", "usuario_id"})
})
public class AclCarpetaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long carpetaId;
    
    @Column(nullable = false)
    private Long usuarioId;
    
    @Column(nullable = false)
    private Long organizacionId;
    
    @Column(nullable = false)
    private Long nivelAccesoId;
    
    @Column(nullable = false)
    private Boolean recursivo = false;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
}
```

**Implementation Steps**:

1. Crear clase con anotaciones JPA
2. Mapear columnas con `@Column` especificando restricciones
3. Definir clave primaria con `@Id` y estrategia de generación
4. Agregar restricción única compuesta con `@UniqueConstraint`
5. Usar `LocalDateTime` para campos de auditoría
6. Agregar setter personalizado para `fecha_actualizacion` (pre-persist/pre-update)
7. Implementar `@PreUpdate` para actualizar timestamp automáticamente

**Annotations**:
```java
@Entity
@Table(...)
@Data // Lombok
@NoArgsConstructor // Lombok
@AllArgsConstructor // Lombok
@Builder // Lombok
public class AclCarpetaEntity { ... }
```

**Implementation Notes**:
- Usar Lombok para reducir boilerplate
- La entidad JPA es específica de la base de datos, NO es la entidad de dominio
- Usar anotaciones de auditoría (`@CreationTimestamp`, `@UpdateTimestamp`) si disponibles

---

### Step 6: Create Entity to Domain Mapper

**File**: `src/main/java/.../infrastructure/adapter/persistence/mapper/AclCarpetaMapper.java`

**Action**: Crear mapper MapStruct para convertir Entity ↔ Domain

**Function Signature**:
```java
@Mapper(componentModel = "spring")
public interface AclCarpetaMapper {
    AclCarpeta toDomain(AclCarpetaEntity entity);
    AclCarpetaEntity toEntity(AclCarpeta domain);
    List<AclCarpeta> toDomainList(List<AclCarpetaEntity> entities);
}
```

**Implementation Steps**:

1. Crear interface con `@Mapper` de MapStruct
2. Métodos para convertir Entity → Domain
3. Métodos para convertir Domain → Entity
4. Métodos para colecciones
5. MapStruct genera implementación automáticamente en compilación

**Dependencies**: 
- MapStruct (`org.mapstruct:mapstruct:1.5.x`)
- Plugin Maven en `pom.xml`

**Implementation Notes**:
- MapStruct genera código en tiempo de compilación (sin reflexión)
- Configurar `componentModel = "spring"` para que sea un bean Spring
- Si hay campos con nombres diferentes, usar `@Mapping(source = "...", target = "...")`

---

### Step 7: Create Repository Adapter (Hexagonal Port Implementation)

**File**: `src/main/java/.../infrastructure/adapter/persistence/AclCarpetaRepositoryAdapter.java`

**Action**: Implementar puerto del repositorio usando adaptador Hexagonal

**Function Signature**:
```java
@Component
public class AclCarpetaRepositoryAdapter implements IAclCarpetaRepository {
    private final AclCarpetaJpaRepository jpaRepository;
    private final AclCarpetaMapper mapper;
    
    @Override
    public AclCarpeta save(AclCarpeta acl) { ... }
    
    @Override
    public Optional<AclCarpeta> findById(Long id) { ... }
    
    @Override
    public Optional<AclCarpeta> findByCarpetaAndUsuario(Long carpetaId, Long usuarioId) { ... }
    // ... más métodos
}
```

**Implementation Steps**:

1. Crear clase que implemente `IAclCarpetaRepository` (puerto)
2. Inyectar `AclCarpetaJpaRepository` (tecnología concreta)
3. Inyectar `AclCarpetaMapper` para conversiones
4. Implementar cada método del puerto:
   - Convertir parámetros de dominio a entity si es necesario
   - Llamar métodos JPA
   - Convertir resultados de entity a dominio
   - Manejar `Optional` apropiadamente
5. Agregar logging para debugging

**Example Implementation**:
```java
@Override
public AclCarpeta save(AclCarpeta acl) {
    AclCarpetaEntity entity = mapper.toEntity(acl);
    AclCarpetaEntity saved = jpaRepository.save(entity);
    return mapper.toDomain(saved);
}

@Override
public Optional<AclCarpeta> findByCarpetaAndUsuario(Long carpetaId, Long usuarioId) {
    return jpaRepository
        .findByCarpetaIdAndUsuarioId(carpetaId, usuarioId)
        .map(mapper::toDomain);
}
```

**Dependencies**: 
- Puerto `IAclCarpetaRepository`
- JPA Repository concreto
- Mapper
- Spring `@Component`

**Implementation Notes**:
- Este adaptador es el puente entre tecnología concreta (JPA) y la lógica de negocio
- Mantener lógica de conversión limpia y testeable
- No agregar lógica de negocio aquí, solo adaptación técnica

---

### Step 8: Create Domain Validator

**File**: `src/main/java/.../application/validator/AclCarpetaValidator.java`

**Action**: Centralizar reglas de validación de negocio

**Function Signature**:
```java
@Component
public class AclCarpetaValidator {
    private final IAclCarpetaRepository aclRepository;
    private final ICarpetaRepository carpetaRepository;
    private final IUsuarioRepository usuarioRepository;
    
    public void validateCreateRequest(CreateAclCarpetaDTO dto, Long organizacionId) { ... }
    public void validateNoDuplicate(Long carpetaId, Long usuarioId) { ... }
    public void validateUserBelongsToOrganization(Long usuarioId, Long organizacionId) { ... }
    public void validateCarpetaBelongsToOrganization(Long carpetaId, Long organizacionId) { ... }
    public void validateValidAccessLevel(String nivelAccesoCodigo) { ... }
}
```

**Implementation Steps**:

1. Crear clase como componente Spring
2. Inyectar repositorios necesarios para validación
3. Crear métodos de validación específicos:
   - `validateCreateRequest()`: validar datos de entrada antes de crear
   - `validateNoDuplicate()`: asegurar que no existe ACL duplicado
   - `validateUserBelongsToOrganization()`: validar tenant
   - `validateCarpetaBelongsToOrganization()`: validar tenant
   - `validateValidAccessLevel()`: validar que nivel existe y está activo
4. Lanzar excepciones específicas de negocio cuando validación falla

**Validation Rules**:
```
1. usuarioId no puede ser nulo
2. nivel_acceso_codigo debe ser válido (LECTURA, ESCRITURA, ADMINISTRACION)
3. Usuario debe existir y pertenecer a organizacionId
4. Carpeta debe existir y pertenecer a organizacionId
5. No debe existir ACL previo para (carpetaId, usuarioId)
6. Contexto de organizationId del token debe coincidir
```

**Exception Types**:
- `BusinessRuleException` o `ValidationException` personalizado
- No usar `RuntimeException` genérico

**Dependencies**: 
- Repositorios de carpeta y usuario
- Spring `@Component`

**Implementation Notes**:
- Centralizar validaciones facilita testing y mantenimiento
- Usar validadores de input (hibernate-validator) para DTOs
- Las reglas de negocio van aquí, no en el controlador

---

### Step 9: Create Application Service

**File**: `src/main/java/.../application/service/AclCarpetaService.java`

**Action**: Orquestar casos de uso y aplicar lógica de negocio

**Function Signature**:
```java
@Service
@Transactional
public class AclCarpetaService {
    private final IAclCarpetaRepository aclRepository;
    private final AclCarpetaValidator validator;
    private final AclCarpetaEventPublisher eventPublisher;
    
    public AclCarpeta crearPermiso(CreateAclCarpetaDTO dto, Long organizacionId) { ... }
    public AclCarpeta actualizarPermiso(Long aclId, UpdateAclCarpetaDTO dto, Long organizacionId) { ... }
    public List<AclCarpeta> listarPermisos(Long carpetaId, Long organizacionId) { ... }
    public void revocarPermiso(Long aclId, Long organizacionId) { ... }
}
```

**Implementation Steps**:

1. Crear clase con `@Service` y `@Transactional`
2. Inyectar:
   - `IAclCarpetaRepository` (puerto para persistencia)
   - `AclCarpetaValidator` (validaciones de negocio)
   - `AclCarpetaEventPublisher` (eventos de auditoría)

3. Implementar `crearPermiso()`:
   ```
   - Validar entrada con validator
   - Crear instancia de AclCarpeta de dominio
   - Guardar via repository
   - Publicar evento ACL_CREADO
   - Retornar ACL creado
   ```

4. Implementar `actualizarPermiso()`:
   ```
   - Buscar ACL existente
   - Validar que pertenece a organizacionId
   - Actualizar nivel de acceso y/o recursivo
   - Guardar cambios
   - Publicar evento ACL_ACTUALIZADO con valores anteriores
   - Retornar ACL actualizado
   ```

5. Implementar `listarPermisos()`:
   ```
   - Validar que carpeta pertenece a organizacionId
   - Buscar ACLs de la carpeta
   - Retornar lista
   ```

6. Implementar `revocarPermiso()` (para US-ACL-003):
   ```
   - Buscar ACL
   - Validar que pertenece a organizacionId
   - Eliminar ACL
   - Publicar evento ACL_REVOCADO
   ```

**Transaction Scope**:
- `@Transactional` asegura que crear ACL + auditoría sean atómicos

**Dependencies**: 
- Spring `@Service`, `@Transactional`
- Puerto `IAclCarpetaRepository`
- Validator
- Event Publisher

**Implementation Notes**:
- El servicio orquesta, NO implementa lógica de negocio directamente
- Las validaciones las hace el validator
- Los eventos van después de persistencia exitosa

---

### Step 10: Create DTOs and DTO Mappers

**File**: `src/main/java/.../api/dto/CreateAclCarpetaDTO.java`

**Action**: Crear DTOs para entrada/salida de API

**DTOs to Create**:

1. **CreateAclCarpetaDTO** (entrada):
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAclCarpetaDTO {
    @NotNull(message = "usuario_id es requerido")
    private Long usuarioId;
    
    @NotBlank(message = "nivel_acceso_codigo es requerido")
    private String nivelAccesoCodigo; // LECTURA, ESCRITURA, ADMINISTRACION
    
    @NotNull
    private Boolean recursivo = false;
    
    private String comentarioOpcional;
}
```

2. **UpdateAclCarpetaDTO** (entrada para PATCH):
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAclCarpetaDTO {
    @NotBlank
    private String nivelAccesoCodigo;
    
    private Boolean recursivo;
}
```

3. **UsuarioEmbebidoDTO** (para respuestas):
```java
@Data
public class UsuarioEmbebidoDTO {
    private Long id;
    private String email;
    private String nombre;
}
```

4. **NivelAccesoDTO** (para respuestas):
```java
@Data
public class NivelAccesoDTO {
    private Long id;
    private String codigo;
    private String nombre;
}
```

5. **AclCarpetaResponseDTO** (salida):
```java
@Data
public class AclCarpetaResponseDTO {
    private Long id;
    private Long carpetaId;
    private Long usuarioId;
    private UsuarioEmbebidoDTO usuario;
    private NivelAccesoDTO nivelAcceso;
    private Boolean recursivo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
```

**File**: `src/main/java/.../api/mapper/AclCarpetaDtoMapper.java`

**Action**: Mapear entre DTOs y dominio

```java
@Mapper(componentModel = "spring")
public interface AclCarpetaDtoMapper {
    AclCarpeta toDomain(CreateAclCarpetaDTO dto);
    AclCarpetaResponseDTO toResponse(AclCarpeta domain, UsuarioEmbebidoDTO usuario, NivelAccesoDTO nivel);
    List<AclCarpetaResponseDTO> toResponseList(List<AclCarpeta> domainList);
}
```

**Implementation Steps**:

1. Crear DTOs con validaciones mediante anotaciones Hibernate Validator
2. Usar Lombok para reducir boilerplate
3. Crear mapper interface con MapStruct
4. DTOs deben incluir todos los campos necesarios para API
5. Validaciones en DTOs (anotaciones `@NotNull`, `@NotBlank`, etc.)

**Dependencies**: 
- Lombok
- Hibernate Validator (jakarta.validation)
- MapStruct

**Implementation Notes**:
- DTOs separan contrato API de lógica interna
- Las validaciones en DTOs son primarias (input validation)
- No incluir DTOs en la lógica de dominio

---

### Step 11: Create REST Controller

**File**: `src/main/java/.../api/controller/AclCarpetaController.java`

**Action**: Implementar endpoints REST

**Function Signature**:
```java
@RestController
@RequestMapping("/api/carpetas/{carpetaId}/permisos")
@Validated
public class AclCarpetaController {
    private final AclCarpetaService aclService;
    private final AclCarpetaDtoMapper dtoMapper;
    
    @PostMapping
    public ResponseEntity<ApiResponse<AclCarpetaResponseDTO>> crearPermiso(
        @PathVariable Long carpetaId,
        @Valid @RequestBody CreateAclCarpetaDTO dto,
        @AuthenticationPrincipal UserDetails user
    ) { ... }
    
    @PatchMapping("/{usuarioId}")
    public ResponseEntity<ApiResponse<AclCarpetaResponseDTO>> actualizarPermiso(
        @PathVariable Long carpetaId,
        @PathVariable Long usuarioId,
        @Valid @RequestBody UpdateAclCarpetaDTO dto,
        @AuthenticationPrincipal UserDetails user
    ) { ... }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<AclCarpetaResponseDTO>>> listarPermisos(
        @PathVariable Long carpetaId,
        @AuthenticationPrincipal UserDetails user
    ) { ... }
    
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> revocarPermiso(
        @PathVariable Long carpetaId,
        @PathVariable Long usuarioId,
        @AuthenticationPrincipal UserDetails user
    ) { ... }
}
```

**Endpoints Implementados**:

#### **Endpoint 1: POST /api/carpetas/{carpetaId}/permisos**
- **Description**: Crear ACL para un usuario en una carpeta
- **Request**: 
  ```
  {
    "usuario_id": 1,
    "nivel_acceso_codigo": "LECTURA",
    "recursivo": false
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "data": { ... AclCarpetaResponseDTO },
    "meta": { "accion": "PERMISO_CREADO", "timestamp": "..." }
  }
  ```
- **Response 400 Bad Request**: Validación fallida
- **Response 403 Forbidden**: Usuario sin permisos ADMINISTRACION
- **Response 404 Not Found**: Carpeta o usuario no existen
- **Response 409 Conflict**: ACL duplicado

#### **Endpoint 2: PATCH /api/carpetas/{carpetaId}/permisos/{usuarioId}**
- **Description**: Actualizar nivel de acceso o recursividad
- **Request**: 
  ```
  {
    "nivel_acceso_codigo": "ESCRITURA",
    "recursivo": true
  }
  ```
- **Response 200 OK**: ACL actualizado
- **Response 404 Not Found**: ACL no existe
- **Response 403 Forbidden**: Usuario sin permisos

#### **Endpoint 3: GET /api/carpetas/{carpetaId}/permisos**
- **Description**: Listar todos los ACLs de una carpeta
- **Response 200 OK**:
  ```json
  {
    "data": [ { ... }, { ... } ],
    "meta": { "total": 2, "carpeta_id": 3 }
  }
  ```

#### **Endpoint 4: DELETE /api/carpetas/{carpetaId}/permisos/{usuarioId}**
- **Description**: Revocar permiso (implementado en US-ACL-003)
- **Response 204 No Content**: Permiso revocado
- **Response 404 Not Found**: ACL no existe

**Implementation Steps**:

1. Crear controlador con `@RestController` y `@RequestMapping`
2. Inyectar servicio y mapper
3. Implementar cada endpoint:
   - Validar entrada con `@Valid`
   - Extraer `organizacionId` del token JWT
   - Llamar servicio correspondiente
   - Mapear respuesta a DTO
   - Retornar `ResponseEntity` con código HTTP apropiado
4. Manejo de excepciones con `@ExceptionHandler` o handler global
5. Agregar anotaciones de documentación (Springdoc OpenAPI)

**Security Validations**:
- Extraer `organizacionId` del JWT en token
- Validar que usuario tiene rol ADMIN o ADMIN_ORG
- No permitir operaciones cross-tenant

**Dependencies**: 
- Spring Web (`@RestController`, `@RequestMapping`, etc.)
- Spring Security (`@AuthenticationPrincipal`)
- DTOs y mappers
- Servicio

**Implementation Notes**:
- El controlador NO contiene lógica de negocio
- Las validaciones de negocio están en `AclCarpetaValidator`
- El servicio orquesta la operación
- Usar API Response wrapper consistente con el proyecto

---

### Step 12: Create Unit Tests

**File**: `src/test/java/.../application/service/AclCarpetaServiceTest.java`

**Action**: Tests unitarios siguiendo TDD (RED → GREEN → REFACTOR)

**Test Classes to Create**:

1. **AclCarpetaServiceTest** (tests del servicio)
2. **AclCarpetaValidatorTest** (tests del validador)
3. **AclCarpetaControllerTest** (tests del controlador)
4. **AclCarpetaRepositoryAdapterTest** (tests del adaptador)

**Implementation Steps - AclCarpetaServiceTest**:

```java
@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class AclCarpetaServiceTest {
    
    @Mock
    private IAclCarpetaRepository aclRepository;
    
    @Mock
    private AclCarpetaValidator validator;
    
    @Mock
    private AclCarpetaEventPublisher eventPublisher;
    
    @InjectMocks
    private AclCarpetaService service;
    
    // Casos exitosos
    @Test
    public void should_CreateAcl_When_ValidRequest() { ... }
    
    @Test
    public void should_CreateRecursiveAcl_When_RecursivoTrue() { ... }
    
    // Errores de validación
    @Test
    public void should_ThrowException_When_NullUsuarioId() { ... }
    
    @Test
    public void should_ThrowException_When_InvalidNivelAcceso() { ... }
    
    // Usuario no existe
    @Test
    public void should_ThrowException_When_UsuarioNotFound() { ... }
    
    // Validación de tenant
    @Test
    public void should_ThrowException_When_UsuarioNotInOrganization() { ... }
    
    // Duplicados
    @Test
    public void should_ThrowException_When_AclAlreadyExists() { ... }
    
    // Actualizar
    @Test
    public void should_UpdateAcl_When_ValidRequest() { ... }
    
    // Listar
    @Test
    public void should_ListAcls_When_CarpetaExists() { ... }
}
```

**Test Scenarios**:

| Scenario | Condition | Expected |
|----------|-----------|----------|
| Create successful | Valid input | ACL created, event published |
| Null usuario_id | Missing field | Exception thrown |
| Invalid nivel | Wrong code | Validation error |
| User not in org | Cross-tenant attempt | 404 returned |
| ACL duplicate | Same user/folder | 409 Conflict |
| Update successful | Valid update | ACL updated with old values in event |
| List empty | No ACLs | Empty list returned |
| Recursive valid | recursivo=true | Inheritance flag set |

**Implementation Notes**:
- Usar `@Mock` para dependencias externas
- Usar `@InjectMocks` para servicio bajo test
- Cada test debe tener un único propósito
- Nombrar tests con patrón: `should_DoThis_When_Condition`
- Usar `given-when-then` (Arrange-Act-Assert)
- No compartir estado entre tests

**Dependencies**: 
- JUnit 5
- Mockito
- AssertJ

---

### Step 13: Create Event Publisher (for Auditing)

**File**: `src/main/java/.../infrastructure/adapter/event/AclCarpetaEventPublisher.java`

**Action**: Publicar eventos para auditoría

**Implementation Steps**:

1. Crear clase para publicar eventos de ACL
2. Eventos a publicar:
   - `ACL_CARPETA_CREADO`: Cuando se crea un nuevo ACL
   - `ACL_CARPETA_ACTUALIZADO`: Cuando se actualiza (nivel, recursivo)
   - `ACL_CARPETA_REVOCADO`: Cuando se elimina (US-ACL-003)

3. Estructura de evento:
   ```json
   {
     "tipo": "ACL_CARPETA_CREADO",
     "usuario_id": 1,
     "carpeta_id": 3,
     "accion": "CREAR",
     "detalles": {
       "nivel_anterior": null,
       "nivel_nuevo": "LECTURA",
       "recursivo": false
     },
     "timestamp": "2026-01-28T10:30:00Z"
   }
   ```

4. Publicar mediante ApplicationEventPublisher o broker de mensajes

**Dependencies**: 
- Spring `ApplicationEventPublisher`
- O: Kafka/RabbitMQ si está disponible

**Implementation Notes**:
- Los eventos son fundamentales para auditoría inmutable
- Deben publicarse DESPUÉS de persistencia exitosa
- Si publicación falla, el ACL ya está guardado (eventual consistency)

---

### Step 14: Update Database Migration Catalog

**File**: `backend/document-core/src/main/resources/db/migration/`

**Action**: Documentar migración creada

**Implementation Steps**:

1. Verificar que `V002__Create_ACL_Carpetas_Table.sql` está en el directorio correcto
2. Asegurar que el nombre sigue convención Flyway: `V00X__Description.sql`
3. La migración será ejecutada automáticamente por Flyway al iniciar la aplicación
4. No requiere acción manual adicional

---

### Step 15: Update Technical Documentation

**File**: `ai-specs/specs/data-model.md`

**Action**: Documentar cambios en modelo de datos

**Implementation Steps**:

1. **Review Changes**: Analizar cambios realizados:
   - Nueva tabla: `acl_carpetas`
   - Nuevas entidades de dominio: `AclCarpeta`
   - Nuevos DTOs: `CreateAclCarpetaDTO`, `AclCarpetaResponseDTO`, etc.

2. **Identify Documentation Files** to update:
   - `ai-specs/specs/data-model.md`: Agregar tabla y relaciones
   - `ai-specs/specs/api-spec.yml`: Agregar endpoints OpenAPI
   - `backend/document-core/README.md`: Notas sobre nueva feature

3. **Update data-model.md**:
   ```markdown
   ### ACL de Carpetas (acl_carpetas)
   
   **Purpose**: Control de acceso granular a nivel de carpeta
   
   **Columns**:
   - id (PK): Identificador único
   - carpeta_id (FK): Referencia a carpeta
   - usuario_id (FK): Referencia a usuario
   - organizacion_id (FK): Aislamiento por tenant
   - nivel_acceso_id (FK): Nivel (LECTURA, ESCRITURA, ADMIN)
   - recursivo (BOOLEAN): Si aplica a subcarpetas
   - fecha_creacion, fecha_actualizacion: Auditoría
   
   **Relationships**:
   - Many-to-One: acl_carpetas → carpetas
   - Many-to-One: acl_carpetas → usuarios
   - Many-to-One: acl_carpetas → nivel_acceso
   
   **Indexes**:
   - UNIQUE(carpeta_id, usuario_id)
   - idx_acl_usuario, idx_acl_organizacion, idx_acl_carpeta_recursivo
   ```

4. **Update api-spec.yml**:
   ```yaml
   /api/carpetas/{carpetaId}/permisos:
     post:
       summary: Crear permiso de carpeta
       parameters:
         - name: carpetaId
           in: path
           required: true
           schema:
             type: integer
       requestBody:
         required: true
         content:
           application/json:
             schema:
               $ref: '#/components/schemas/CreateAclCarpetaDTO'
       responses:
         '201':
           description: Permiso creado exitosamente
           content:
             application/json:
               schema:
                 $ref: '#/components/schemas/AclCarpetaResponse'
   ```

5. **Verify Documentation**: 
   - Confirmar que cambios están documentados
   - Verificar consistencia con estructura existente
   - Revisar que ejemplos son correctos

6. **Report Updates**: 
   - Documentar qué archivos fueron actualizados
   - Mencionar en commit message las actualizaciones

**Files Updated**:
- ✅ `ai-specs/specs/data-model.md` - Tabla acl_carpetas
- ✅ `ai-specs/specs/api-spec.yml` - Endpoints ACL
- ✅ `backend/document-core/README.md` - Feature ACL

---

## Implementation Order

2. ✅ **Step 1**: Create Database Migration (`V002__Create_ACL_Carpetas_Table.sql`)
3. ✅ **Step 2**: Create Value Objects and Domain Model (`AclCarpeta.java`)
4. ✅ **Step 3**: Create Repository Interface (`IAclCarpetaRepository.java`)
5. ✅ **Step 4**: Create JPA Repository (`AclCarpetaJpaRepository.java`)
6. ✅ **Step 5**: Create JPA Entity (`AclCarpetaEntity.java`)
7. ✅ **Step 6**: Create Entity to Domain Mapper (`AclCarpetaMapper.java`)
8. ✅ **Step 7**: Create Repository Adapter (`AclCarpetaRepositoryAdapter.java`)
9. ✅ **Step 8**: Create Domain Validator (`AclCarpetaValidator.java`)
10. ✅ **Step 9**: Create Application Service (`AclCarpetaService.java`)
11. ✅ **Step 10**: Create DTOs and DTO Mappers
12. ✅ **Step 11**: Create REST Controller (`AclCarpetaController.java`)
13. ✅ **Step 12**: Create Unit Tests
14. ✅ **Step 13**: Create Event Publisher (`AclCarpetaEventPublisher.java`)
15. ✅ **Step 14**: Database Migration Catalog (automatic)
16. ✅ **Step 15**: Update Technical Documentation

---

## Testing Checklist

### Unit Tests
- [ ] `AclCarpetaServiceTest`: ≥90% coverage
  - [ ] Create successful ACL
  - [ ] Create recursive ACL
  - [ ] Validation errors (null, invalid level)
  - [ ] Organization isolation
  - [ ] Duplicate detection
  - [ ] Update operations
  - [ ] List operations

- [ ] `AclCarpetaValidatorTest`: All validation rules
  - [ ] User in organization
  - [ ] Folder in organization
  - [ ] Valid access level
  - [ ] No duplicates

### Integration Tests
- [ ] `AclCarpetaControllerTest`: All endpoints
  - [ ] POST create ACL (201, 400, 403, 404, 409)
  - [ ] PATCH update ACL (200, 404, 403)
  - [ ] GET list ACLs (200)
  - [ ] DELETE revoke ACL (204, 404, 403)

---

## Error Response Format

### 400 Bad Request (Validation Error)
```json
{
  "error": {
    "codigo": "VALIDACION_ERROR",
    "mensaje": "Error en validación de entrada",
    "detalles": {
      "usuario_id": ["es requerido"],
      "nivel_acceso_codigo": ["debe ser válido"]
    }
  },
  "timestamp": "2026-01-28T10:30:00Z"
}
```

### 403 Forbidden (Permission Denied)
```json
{
  "error": {
    "codigo": "PERMISO_DENEGADO",
    "mensaje": "No tienes permisos para realizar esta acción",
    "detalles": {
      "requerido": "ADMINISTRACION",
      "actual": "LECTURA"
    }
  },
  "timestamp": "2026-01-28T10:30:00Z"
}
```

### 404 Not Found (Resource Not Found)
```json
{
  "error": {
    "codigo": "NO_ENCONTRADO",
    "mensaje": "Recurso no encontrado",
    "detalles": {
      "tipo": "ACL o recurso relacionado"
    }
  },
  "timestamp": "2026-01-28T10:30:00Z"
}
```

### 409 Conflict (Duplicate ACL)
```json
{
  "error": {
    "codigo": "ACL_DUPLICADO",
    "mensaje": "Ya existe un permiso para este usuario sobre esta carpeta",
    "detalles": {
      "carpeta_id": 3,
      "usuario_id": 1
    }
  },
  "timestamp": "2026-01-28T10:30:00Z"
}
```

---

## Dependencies

### Maven Dependencies Required
```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Hibernate Validator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- MapStruct -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Flyway for migrations -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

### Build Plugins
```xml
<!-- MapStruct annotation processor -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.5.5.Final</version>
            </path>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## Notes

### Important Reminders
2. **Tenant Isolation**: Validar `organizacion_id` en CADA operación - seguridad crítica
3. **Transactions**: Operaciones create + audit deben ser atómicas
4. **Hexagonal Architecture**: Mantener separación entre puertos y adaptadores
5. **Testing First**: Implementar tests ANTES del código (TDD)
6. **No Secrets**: No commitear credenciales, tokens o datos sensibles

### Business Rules
- Permisos explícitos de documento > permisos de carpeta directos > permisos heredados
- Un usuario solo puede tener un ACL por carpeta (no múltiples niveles)
- Actualizar a nivel más restrictivo requiere confirmación en UI
- Cambios de ACL deben ser auditados inmediatamente

### Language Requirements
- Código: **Inglés** (variables, métodos, comentarios)
- Tests: **Inglés** (nombres, assertions)
- Documentación: **Español** en archivos de especificación, **Inglés** en código
- Commits: **Español** con referencia a ticket (ej: `US-ACL-002: Crear endpoint POST /permisos`)

### Code Quality Standards
- Seguir estilo del proyecto (ver `rules-backend.md`)
- Cobertura mínima de tests: **90%**
- Linting: `mvn checkstyle:check` debe pasar
- No usar `@Autowired` - usar constructor injection
- No usar `.orElse(null)` - usar `.orElseThrow()`

---

## Next Steps After Implementation

### After Completing Backend Implementation:

1. **Frontend Implementation**:
   - [ ] Crear ticket US-ACL-002-frontend
   - [ ] Implementar servicios HTTP
   - [ ] Crear componentes React

2. **Documentation**:
   - [ ] Verificar OpenAPI actualizado
   - [ ] Testing manual en ambiente de desarrollo
   - [ ] Crear ejemplos de uso

3. **Deployment**:
   - [ ] Incluir en release plan
   - [ ] Ejecutar scripts de migración en BD
   - [ ] Validar en environment de staging

---

## Implementation Verification

### Final Verification Checklist

**✅ Code Quality**
- [ ] Seguir convenciones del proyecto
- [ ] No hay hardcoded values o secrets
- [ ] Logging apropiado en niveles corretos
- [ ] Sin `System.out.println()` o debug statements
- [ ] Nomes descriptivos y claros

**✅ Functionality**
- [ ] Los 10 criterios de aceptación pasan
- [ ] Crear ACL funciona correctamente
- [ ] Actualizar ACL funciona
- [ ] Listar ACLs funciona
- [ ] Validaciones se ejecutan
- [ ] Errores se manejan correctamente

**✅ Testing**
- [ ] Cobertura >90% en servicio y validator
- [ ] Todos los tests pasan
- [ ] Tests incluyen casos exitosos y fallidos
- [ ] Errores de validación se cubren
- [ ] Edge cases se validan

**✅ Integration**
- [ ] Migration SQL ejecuta sin errores
- [ ] JPA entities mapean correctamente
- [ ] Eventos de auditoría se publican
- [ ] Datos se persisten correctamente

**✅ Documentation**
- [ ] data-model.md actualizado ✅
- [ ] api-spec.yml actualizado ✅
- [ ] README backend actualizado ✅
- [ ] Ejemplos de API documentados ✅

**✅ Security**
- [ ] Validación de tenant en todas operaciones
- [ ] Validación de autorización (ADMIN role)
- [ ] No se exponen datos de recursos no encontrados
- [ ] Contraseñas/tokens no en logs