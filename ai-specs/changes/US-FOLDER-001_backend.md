# Plan de Implementación Backend: US-FOLDER-001 Crear Carpeta (API)

## 1. Descripción General

**Característica:** Implementar un endpoint REST API para crear carpetas dentro de una estructura jerárquica en DocFlow, respetando los límites organizacionales, validación de permisos y garantizando unicidad de nombres por nivel de directorio.

**Principios Arquitectónicos:**
- **Domain-Driven Design (DDD):** La carpeta es una entidad de dominio central con reglas de negocio explícitas
- **Arquitectura Hexagonal:** Separación de capas de dominio, aplicación e infraestructura
- **Multi-tenencia:** Todas las operaciones deben estar limitadas a `organizacion_id` del token JWT
- **API Limpia:** Endpoint RESTful con contratos claros de solicitud/respuesta y códigos de error específicos
- **Enfoque TDD:** Escribir pruebas antes de la implementación para garantizar claridad de requisitos

**Restricciones Clave:**
- La creación de carpeta requiere permiso `ESCRITURA` o `ADMINISTRACION` en la carpeta padre
- Los nombres de carpetas deben ser únicos dentro del mismo directorio padre (por organización)
- Eliminación lógica (soft delete) con timestamp `fecha_eliminacion`
- Las relaciones jerárquicas no deben cruzar límites organizacionales
- Todas las respuestas siguen el patrón de envoltorio con estructuras `data`, `meta` y `error`

---

## 2. Contexto Arquitectónico

### Capas Involucradas

**Capa de Dominio:**
- Entidad `Carpeta` (inmutable, raíz agregada)
- Interfaz `ICarpetaRepository` para abstracción de persistencia
- `CarpetaValidator` para validación de reglas de negocio

**Capa de Aplicación:**
- `CarpetaService` para orquestación y casos de uso
- DTOs (`CreateCarpetaDTO`, `CarpetaResponseDTO`)
- Mappers (MapStruct para conversión entidad ↔ DTO)

**Capa de Infraestructura:**
- `CarpetaEntity` (entidad JPA)
- `CarpetaJpaRepository` (Spring Data JPA)
- `CarpetaRepositoryAdapter` (adaptador hexagonal)
- Migraciones de base de datos (Flyway)
- Manejadores de excepciones

**Capa de Presentación:**
- `CarpetaController` (endpoint REST)

### Componentes Referenciados
- Sistema ACL (US-ACL-001, US-ACL-002): Evaluación de permisos
- Evaluador de Permisos (US-ACL-006, o stub durante desarrollo): Verificar permisos de usuario
- Sistema de Auditoría: Emisión de eventos para creación de carpeta
- Servicio de Organización: Verificar contexto organizacional

---

## 3. Pasos de Implementación

### **Paso 1: Crear Migraciones de Base de Datos**

**Archivo:** `backend/document-core/src/main/resources/db/migration/V003__Create_Carpetas_Table.sql`

**Acción:** Definir el esquema relacional para jerarquía de carpetas con soporte de eliminación lógica.

**Pasos de Implementación:**

1. **Crear Tabla Carpetas:**
   ```sql
   CREATE TABLE carpetas (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     organizacion_id UUID NOT NULL REFERENCES organizaciones(id) ON DELETE CASCADE,
     carpeta_padre_id UUID REFERENCES carpetas(id) ON DELETE SET NULL,
     nombre VARCHAR(255) NOT NULL,
     descripcion TEXT,
     creado_por UUID NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
     fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     fecha_eliminacion TIMESTAMP,
     CONSTRAINT ck_nombre_length CHECK (LENGTH(nombre) > 0 AND LENGTH(nombre) <= 255),
     CONSTRAINT ck_descripcion_length CHECK (descripcion IS NULL OR LENGTH(descripcion) <= 500),
     CONSTRAINT ck_org_padre_org CHECK (
       carpeta_padre_id IS NULL OR 
       organizacion_id = (SELECT organizacion_id FROM carpetas WHERE id = carpeta_padre_id)
     )
   );
   ```

2. **Crear Índices:**
   ```sql
   -- Índice para buscar hijos por padre y organización
   CREATE INDEX idx_carpetas_org_padre ON carpetas(organizacion_id, carpeta_padre_id) 
   WHERE fecha_eliminacion IS NULL;
   
   -- Índice para consultas por organización
   CREATE INDEX idx_carpetas_org ON carpetas(organizacion_id) 
   WHERE fecha_eliminacion IS NULL;
   
   -- Índice único para unicidad de nombre por padre por org (consciente de eliminación lógica)
   CREATE UNIQUE INDEX ux_carpeta_nombre_padre_org 
   ON carpetas(organizacion_id, COALESCE(carpeta_padre_id, '00000000-0000-0000-0000-000000000000'::uuid), nombre)
   WHERE fecha_eliminacion IS NULL;
   
   -- Índice para carpetas creadas por usuario (pista de auditoría)
   CREATE INDEX idx_carpetas_creado_por ON carpetas(creado_por);
   ```

3. **Verificar Migración:**
   - Asegurar no hay errores de sintaxis
   - Confirmar que todas las restricciones están correctamente definidas
   - Verificar relaciones de claves foráneas

**Dependencias:**
- PostgreSQL 12+
- Versión de Flyway especificada en `pom.xml`

**Notas de Implementación:**
- Usar `COALESCE` en índice único para manejar NULL `carpeta_padre_id` (carpetas raíz)
- Implementación de eliminación lógica: `fecha_eliminacion IS NULL` en todas las cláusulas WHERE
- Restricción `ck_org_padre_org` asegura que la carpeta padre está en la misma organización
- `ON DELETE SET NULL` para padre permite mover carpetas arriba en la jerarquía

---

### **Paso 2: Crear Migración de Carpeta Raíz**

**Archivo:** `backend/document-core/src/main/resources/db/migration/V004__Create_Carpeta_Raiz_Por_Organizacion.sql`

**Acción:** Inicializar carpeta raíz para organizaciones existentes y configurar lógica para nuevas organizaciones.

**Pasos de Implementación:**

1. **Insertar Carpeta Raíz para Cada Organización:**
   ```sql
   INSERT INTO carpetas (id, organizacion_id, carpeta_padre_id, nombre, descripcion, creado_por, fecha_creacion, fecha_actualizacion)
   SELECT 
     gen_random_uuid(),
     id,
     NULL,
     'Raíz',
     'Carpeta raíz de la organización',
     id,  -- Usar ID de org como creador de sistema (o podría ser UUID de usuario de sistema)
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP
   FROM organizaciones
   WHERE id NOT IN (
     SELECT DISTINCT organizacion_id FROM carpetas WHERE carpeta_padre_id IS NULL
   )
   ON CONFLICT DO NOTHING;
   ```

2. **Documentar Comportamiento para Nuevas Organizaciones:**
   - Cuando se crea una nueva organización via `US-ADMIN-001`, se debe crear automáticamente una carpeta raíz
   - Esto se puede hacer via:
     - Opción A: Listener JPA `@PostPersist` en `OrganizacionEntity`
     - Opción B: Método de servicio en `OrganizacionService.crear()` que llama `CarpetaService.crearRaiz()`
     - Recomendación: **Opción B** (más explícita y testeable)

**Notas de Implementación:**
- Migración es idempotente (usa `ON CONFLICT DO NOTHING` o verifica existencia primero)
- Carpeta raíz tiene `carpeta_padre_id = NULL`
- Nombre de carpeta raíz es `"Raíz"` (podría personalizarse por estándares del proyecto)

---

### **Paso 3: Crear Modelo de Dominio - Entidad Carpeta**

**Archivo:** `backend/document-core/src/main/java/.../domain/model/carpeta/Carpeta.java`

**Acción:** Definir la entidad de dominio inmutable representando una carpeta con lógica de negocio.

**Firma de Función:**
```java
public final class Carpeta {
  private final UUID id;
  private final UUID organizacionId;
  private final UUID carpetaPadreId;
  private final String nombre;
  private final String descripcion;
  private final UUID creadoPor;
  private final Instant fechaCreacion;
  private final Instant fechaActualizacion;
  private final Instant fechaEliminacion;
  
  // Constructor privado para Builder
  private Carpeta(Builder builder) { ... }
  
  // Patrón Builder
  public static Builder builder() { ... }
  
  // Getters (sin setters - inmutable)
  public UUID getId() { ... }
  public UUID getOrganizacionId() { ... }
  // ... etc
  
  // Métodos de negocio
  public boolean isActiva() { ... }
  public boolean esRaiz() { ... }
  public void validarIntegridad() { ... }
}
```

**Pasos de Implementación:**

1. **Definir Campos:**
   - Todos los campos privados finales
   - Usar UUID para IDs e Instant para timestamps
   - Usar String para nombre/descripción
   - Garantizar inmutabilidad

2. **Implementar Constructor Privado:**
   - Aceptar instancia de Builder
   - Asignar todos los campos desde builder

3. **Implementar Patrón Builder:**
   - Clase Builder con API fluida
   - Sobrescribir build() para llamar constructor privado
   - Validar en build() antes de retornar instancia

4. **Agregar Métodos de Lógica de Negocio:**
   ```java
   public boolean isActiva() {
     return fechaEliminacion == null;
   }
   
   public boolean esRaiz() {
     return carpetaPadreId == null;
   }
   
   public void validarIntegridad() {
     if (nombre == null || nombre.isBlank()) {
       throw new IllegalArgumentException("Nombre no puede estar vacío");
     }
     if (nombre.length() > 255) {
       throw new IllegalArgumentException("Nombre no puede exceder 255 caracteres");
     }
     if (descripcion != null && descripcion.length() > 500) {
       throw new IllegalArgumentException("Descripción no puede exceder 500 caracteres");
     }
   }
   ```

5. **Agregar Equals & HashCode:**
   - Basado en `id` únicamente (entidades se identifican por ID)

**Dependencias:**
- `java.util.UUID`
- `java.time.Instant`
- Lombok (opcional, para @Value o @EqualsAndHashCode)

**Notas de Implementación:**
- Inmutable asegura seguridad en concurrencia y previene mutaciones accidentales
- Reglas de negocio se aplican en métodos de dominio, no en DTOs o servicios
- Patrón Builder facilita crear instancias en pruebas

---

### **Paso 4: Crear Interfaz de Repositorio**

**Archivo:** `backend/document-core/src/main/java/.../domain/repository/ICarpetaRepository.java`

**Acción:** Definir el contrato para operaciones de persistencia de carpeta.

**Firma de Función:**
```java
public interface ICarpetaRepository {
  Carpeta crear(Carpeta carpeta) throws CarpetaYaExisteException;
  Optional<Carpeta> obtenerPorId(UUID id);
  Optional<Carpeta> obtenerPorId(UUID id, UUID organizacionId);
  List<Carpeta> obtenerHijos(UUID carpetaPadreId, UUID organizacionId);
  List<Carpeta> obtenerTodas(UUID organizacionId);
  void actualizar(Carpeta carpeta);
  void eliminarLogicamente(UUID id, UUID organizacionId);
  boolean nombreExisteEnNivel(UUID organizacionId, UUID carpetaPadreId, String nombre);
  Optional<Carpeta> obtenerRaiz(UUID organizacionId);
}
```

**Pasos de Implementación:**

1. **Definir Método Crear:**
   - Aceptar instancia inmutable de Carpeta
   - Lanzar `CarpetaYaExisteException` si hay nombre duplicado en el mismo nivel
   - Retornar Carpeta persistida con ID asignado

2. **Definir Métodos de Consulta:**
   - `obtenerPorId(UUID)` - buscar por ID
   - `obtenerPorId(UUID, UUID)` - buscar por ID dentro de organización (límite de seguridad)
   - `obtenerHijos()` - buscar hijos de una carpeta padre
   - `obtenerTodas()` - listar todas las carpetas en organización
   - `obtenerRaiz()` - obtener carpeta raíz de organización

3. **Definir Método de Validación:**
   - `nombreExisteEnNivel()` - verificar nombres duplicados (usado antes de crear)

4. **Definir Método de Eliminación:**
   - `eliminarLogicamente()` - establecer timestamp `fecha_eliminacion`

**Notas de Implementación:**
- Todos los métodos deben filtrar por `organizacionId` (regla de seguridad)
- Las consultas deben usar índices para rendimiento
- Tipo Optional para métodos que podrían no encontrar resultados

---

### **Paso 5: Crear Entidad JPA**

**Archivo:** `backend/document-core/src/main/java/.../infrastructure/adapter/persistence/entity/CarpetaEntity.java`

**Acción:** Definir la entidad JPA para mapeo de base de datos.

**Firma de Función:**
```java
@Entity
@Table(name = "carpetas", indexes = {
  @Index(name = "idx_carpetas_org_padre", columnList = "organizacion_id, carpeta_padre_id"),
  @Index(name = "idx_carpetas_org", columnList = "organizacion_id"),
  @Index(name = "idx_carpetas_creado_por", columnList = "creado_por")
})
public class CarpetaEntity {
  @Id
  private UUID id;
  
  @Column(nullable = false)
  private UUID organizacionId;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "carpeta_padre_id")
  private CarpetaEntity carpetaPadre;
  
  // ... otros campos
}
```

**Pasos de Implementación:**

1. **Definir Campos de Entidad:**
   - `id` con anotación `@Id`
   - `organizacionId` como columna (no relación, por rendimiento)
   - `carpetaPadre` como ManyToOne auto-referencia con fetch LAZY
   - Timestamps, nombre, descripcion, creado_por

2. **Agregar Anotaciones JPA:**
   - `@Entity`, `@Table`, `@Column`
   - `@ManyToOne` para relación de padre
   - `@CreationTimestamp` / `@UpdateTimestamp` (anotaciones de Hibernate) o listeners manuales

3. **Agregar Validación:**
   - `@NotBlank` en nombre
   - `@Size` en descripción
   - `@NotNull` en organizacionId, creado_por

4. **Agregar Convertidor para Eliminación Lógica:**
   - Podría usar anotación `@SQLDelete` de Hibernate:
   ```java
   @SQLDelete(sql = "UPDATE carpetas SET fecha_eliminacion = CURRENT_TIMESTAMP WHERE id = ?")
   @Where(clause = "fecha_eliminacion IS NULL")
   public class CarpetaEntity { ... }
   ```

**Dependencias:**
- API de Persistencia de Jakarta (javax.persistence o jakarta.persistence)
- Anotaciones de Hibernate
- Validación de Bean (javax.validation)

**Notas de Implementación:**
- `carpetaPadre` es LAZY para evitar consultas N+1
- `organizacionId` se almacena explícitamente (desnormalización) para filtrado rápido
- Cláusula `@Where` asegura que carpetas eliminadas lógicamente se excluyen de consultas automáticamente

---

### **Paso 6: Crear Repositorio JPA**

**Archivo:** `backend/document-core/src/main/java/.../infrastructure/adapter/persistence/jpa/CarpetaJpaRepository.java`

**Acción:** Definir repositorio Spring Data JPA con consultas personalizadas.

**Firma de Función:**
```java
public interface CarpetaJpaRepository extends JpaRepository<CarpetaEntity, UUID> {
  List<CarpetaEntity> findByOrganizacionIdAndCarpetaPadreId(UUID organizacionId, UUID carpetaPadreId);
  List<CarpetaEntity> findByOrganizacionId(UUID organizacionId);
  Optional<CarpetaEntity> findByOrganizacionIdAndId(UUID organizacionId, UUID id);
  boolean existsByOrganizacionIdAndCarpetaPadreIdAndNombreAndFechaEliminacionIsNull(
    UUID organizacionId, UUID carpetaPadreId, String nombre
  );
  Optional<CarpetaEntity> findByOrganizacionIdAndCarpetaPadreIdIsNull(UUID organizacionId);
}
```

**Pasos de Implementación:**

1. **Crear Interfaz Spring Data JPA:**
   - Extender `JpaRepository<CarpetaEntity, UUID>`
   - Usar nombres de método que generen SQL correcto

2. **Definir Métodos de Consulta:**
   - `findByOrganizacionIdAndCarpetaPadreId()` - listar hijos
   - `findByOrganizacionId()` - listar todos en org
   - `findByOrganizacionIdAndId()` - obtener por ID en org (filtro de seguridad)
   - `existsByOrganizacionIdAndCarpetaPadreIdAndNombre...()` - verificar duplicado (con filtro de eliminación lógica)
   - `findByOrganizacionIdAndCarpetaPadreIdIsNull()` - buscar carpeta raíz

3. **Probar Métodos de Consulta:**
   - Verificar que se usan índices
   - Confirmar que se aplica cláusula WHERE de eliminación lógica

**Notas de Implementación:**
- Los nombres de método son verbosos pero descriptivos
- Spring Data genera SQL desde los nombres de método automáticamente
- Filtro de eliminación lógica se aplica via anotación JPA `@Where` en entidad

---

### **Step 7: Create Mapper - Entity to Domain**

**File:** `backend/document-core/src/main/java/.../infrastructure/adapter/persistence/mapper/CarpetaMapper.java`

**Action:** Convert between JPA entity and domain model using MapStruct.

**Function Signature:**
```java
@Mapper(componentModel = "spring")
public interface CarpetaMapper {
  Carpeta toDomain(CarpetaEntity entity);
  CarpetaEntity toPersistence(Carpeta domain);
  List<Carpeta> toDomainList(List<CarpetaEntity> entities);
}
```

**Implementation Steps:**

1. **Define MapStruct Mapper Interface:**
   - Use `@Mapper(componentModel = "spring")` for Spring integration
   - Create methods for entity → domain and domain → entity

2. **Implement Mapping Methods:**
   ```java
   @Mapping(source = "carpetaPadre.id", target = "carpetaPadreId")
   Carpeta toDomain(CarpetaEntity entity);
   ```

3. **Handle Nested Objects:**
   - `carpetaPadre` is optional; map only its ID
   - Use `@Mapping` to customize field mappings

**Dependencies:**
- MapStruct 1.5+ (check `pom.xml`)

**Implementation Notes:**
- Mappers are generated at compile time
- @Mapper generates Spring bean automatically
- Separate mappers for each layer (Persistence, API)

---

### **Step 8: Create Validator - Business Rules**

**File:** `backend/document-core/src/main/java/.../application/validator/CarpetaValidator.java`

**Action:** Validate business rules for folder creation and updates.

**Function Signature:**
```java
@Component
public class CarpetaValidator {
  
  public void validarCreacion(CreateCarpetaDTO dto, UUID organizacionId) 
    throws CarpetaValidationException { ... }
  
  public void validarExistenciaCarpetaPadre(UUID carpetaPadreId, UUID organizacionId) 
    throws CarpetaNotFoundException { ... }
  
  public void validarPermisosEnCarpetaPadre(UUID usuarioId, UUID carpetaPadreId, UUID organizacionId) 
    throws SinPermisoCarpetaException { ... }
  
  public void validarUnicidadNombre(String nombre, UUID carpetaPadreId, UUID organizacionId) 
    throws CarpetaNombreDuplicadoException { ... }
}
```

**Implementation Steps:**

1. **Validate Creation Request:**
   - Check nombre is not empty and ≤255 chars
   - Check descripcion ≤500 chars (if present)
   - Check carpetaPadreId is valid UUID

2. **Validate Parent Folder Exists:**
   - Query repository for parent folder
   - Verify it belongs to the same organization
   - Throw `CarpetaNotFoundException` if not found

3. **Validate User Permissions:**
   - Call permission evaluator (stub or US-ACL-006)
   - Check user has `ESCRITURA` or `ADMINISTRACION` on parent folder
   - Throw `SinPermisoCarpetaException` if insufficient

4. **Validate Name Uniqueness:**
   - Query if nombre already exists at this level (same parent, same org)
   - Throw `CarpetaNombreDuplicadoException` if duplicate

**Implementation Notes:**
- All validation happens before persistence
- Errors are domain exceptions (not generic exceptions)
- Permission check integrates with US-ACL-006 (or stub while developing)

---

### **Step 9: Create Service - Application Logic**

**File:** `backend/document-core/src/main/java/.../application/service/CarpetaService.java`

**Action:** Orchestrate folder creation workflow, validate, persist, and emit events.

**Function Signature:**
```java
@Service
@Transactional
public class CarpetaService {
  
  public Carpeta crear(CreateCarpetaDTO dto, UUID usuarioId, UUID organizacionId) 
    throws CarpetaValidationException, CarpetaNotFoundException, SinPermisoCarpetaException { ... }
  
  public Optional<Carpeta> obtenerPorId(UUID id, UUID organizacionId) { ... }
  
  public List<Carpeta> obtenerHijos(UUID carpetaPadreId, UUID organizacionId) { ... }
  
  private void emitirEvento(CarpetaCreatedEvent event) { ... }
}
```

**Implementation Steps:**

1. **Implement Crear Method:**
   ```java
   @Transactional
   public Carpeta crear(CreateCarpetaDTO dto, UUID usuarioId, UUID organizacionId) {
     // Step 1: Validate input
     validator.validarCreacion(dto, organizacionId);
     
     // Step 2: Validate parent folder exists
     validator.validarExistenciaCarpetaPadre(dto.getCarpetaPadreId(), organizacionId);
     
     // Step 3: Validate permissions
     validator.validarPermisosEnCarpetaPadre(usuarioId, dto.getCarpetaPadreId(), organizacionId);
     
     // Step 4: Validate name uniqueness
     validator.validarUnicidadNombre(dto.getNombre(), dto.getCarpetaPadreId(), organizacionId);
     
     // Step 5: Create domain entity
     Carpeta carpeta = Carpeta.builder()
       .id(UUID.randomUUID())
       .organizacionId(organizacionId)
       .carpetaPadreId(dto.getCarpetaPadreId())
       .nombre(dto.getNombre())
       .descripcion(dto.getDescripcion())
       .creadoPor(usuarioId)
       .fechaCreacion(Instant.now())
       .fechaActualizacion(Instant.now())
       .build();
     
     // Step 6: Validate business rules
     carpeta.validarIntegridad();
     
     // Step 7: Persist
     Carpeta persisted = repository.crear(carpeta);
     
     // Step 8: Emit event
     emitirEvento(new CarpetaCreatedEvent(persisted.getId(), organizacionId, usuarioId));
     
     return persisted;
   }
   ```

2. **Implement Query Methods:**
   - `obtenerPorId()` - filter by organization
   - `obtenerHijos()` - list children

3. **Implement Event Emission:**
   - Emit `CarpetaCreatedEvent` for audit system
   - Include user_id, carpeta_id, organizacion_id, timestamp

**Dependencies:**
- Spring `@Service`, `@Transactional`
- Custom domain exceptions
- Event bus / publisher (Spring ApplicationEventPublisher)

**Implementation Notes:**
- `@Transactional` ensures atomicity (all-or-nothing)
- If event emission fails, transaction rolls back
- organizacion_id comes from token, not user input

---

### **Step 10: Create DTOs and Mappers - API Layer**

**File 1:** `backend/document-core/src/main/java/.../api/dto/CreateCarpetaDTO.java`

```java
@Data
@NoArgsConstructor
public class CreateCarpetaDTO {
  @NotNull(message = "carpeta_padre_id es requerido")
  private UUID carpetaPadreId;
  
  @NotBlank(message = "nombre es requerido")
  @Size(min = 1, max = 255, message = "nombre debe tener entre 1 y 255 caracteres")
  private String nombre;
  
  @Size(max = 500, message = "descripcion no puede exceder 500 caracteres")
  private String descripcion;
}
```

**File 2:** `backend/document-core/src/main/java/.../api/dto/CarpetaDTO.java`

```java
@Data
@NoArgsConstructor
public class CarpetaDTO {
  private UUID id;
  private UUID organizacionId;
  private UUID carpetaPadreId;
  private String nombre;
  private String descripcion;
  private UUID creadoPor;
  private Instant fechaCreacion;
  private Instant fechaActualizacion;
  private String rutaCompleta;
}
```

**File 3:** `backend/document-core/src/main/java/.../api/mapper/CarpetaDtoMapper.java`

```java
@Mapper(componentModel = "spring")
public interface CarpetaDtoMapper {
  CarpetaDTO toDto(Carpeta carpeta);
  List<CarpetaDTO> toDtoList(List<Carpeta> carpetas);
}
```

**Implementation Steps:**

1. Create DTOs with validation annotations
2. Create MapStruct mapper for Carpeta → CarpetaDTO
3. Ensure null-safety (Optional handling if needed)

---

### **Step 11: Create Exception Classes**

**Files:**
- `backend/document-core/src/main/java/.../infrastructure/adapter/exception/CarpetaNotFoundException.java`
- `backend/document-core/src/main/java/.../infrastructure/adapter/exception/CarpetaNombreDuplicadoException.java`
- `backend/document-core/src/main/java/.../infrastructure/adapter/exception/SinPermisoCarpetaException.java`

**Implementation (Example for each):**

```java
public class CarpetaNotFoundException extends DomainException {
  public CarpetaNotFoundException(UUID id) {
    super("CARPETA_NO_ENCONTRADA", "Carpeta no existe o no pertenece a tu organización", 
      Map.of("carpeta_id", id.toString()));
  }
}

public class CarpetaNombreDuplicadoException extends DomainException {
  public CarpetaNombreDuplicadoException(String nombre, UUID carpetaPadreId) {
    super("NOMBRE_DUPLICADO", "Ya existe una carpeta con este nombre en el mismo directorio",
      Map.of("nombre", nombre, "carpeta_padre_id", carpetaPadreId.toString()));
  }
}

public class SinPermisoCarpetaException extends DomainException {
  public SinPermisoCarpetaException(UUID carpetaPadreId) {
    super("SIN_PERMISO_CARPETA", 
      "Requiere permiso ESCRITURA o ADMINISTRACION en la carpeta padre",
      Map.of("carpeta_padre_id", carpetaPadreId.toString()));
  }
}
```

**Implementation Notes:**
- Extend a base `DomainException` class
- Include error code, message, and details
- Follow RFC 7807 error format

---

### **Step 12: Create Controller - REST Endpoint**

**File:** `backend/document-core/src/main/java/.../api/controller/CarpetaController.java`

**Action:** Expose POST /api/carpetas endpoint for creating folders.

**Function Signature:**
```java
@RestController
@RequestMapping("/api/carpetas")
@Validated
@Tag(name = "Carpetas", description = "API para gestión de carpetas")
public class CarpetaController {
  
  @PostMapping
  @Operation(summary = "Crear una nueva carpeta")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Carpeta creada exitosamente"),
    @ApiResponse(responseCode = "400", description = "Validación fallida"),
    @ApiResponse(responseCode = "403", description = "Sin permisos"),
    @ApiResponse(responseCode = "404", description = "Carpeta padre no existe")
  })
  public ResponseEntity<ApiResponse<CarpetaDTO>> crear(
    @Valid @RequestBody CreateCarpetaDTO dto,
    @AuthenticationPrincipal JwtClaimsPrincipal principal
  ) { ... }
}
```

**Implementation Steps:**

1. **Define Endpoint:**
   - POST /api/carpetas
   - Accept `CreateCarpetaDTO` with `@Valid`
   - Require authentication via `@AuthenticationPrincipal`

2. **Extract Context from Token:**
   ```java
   UUID organizacionId = principal.getOrganizacionId();
   UUID usuarioId = principal.getUsuarioId();
   ```

3. **Call Service:**
   ```java
   Carpeta carpeta = carpetaService.crear(dto, usuarioId, organizacionId);
   ```

4. **Map to DTO:**
   ```java
   CarpetaDTO response = carpetaDtoMapper.toDto(carpeta);
   ```

5. **Return 201:**
   ```java
   return ResponseEntity.status(HttpStatus.CREATED)
     .body(ApiResponse.success(response, "CARPETA_CREADA"));
   ```

6. **Add Exception Handling:**
   - Use `@ExceptionHandler` or global error handler
   - Map exceptions to HTTP status codes

**Dependencies:**
- Spring Web, Validation
- Springdoc OpenAPI annotations
- Custom JWT claims principal

**Implementation Notes:**
- Extract organizacion_id from token (not user input)
- Use ApiResponse envelope for consistency
- Document responses with OpenAPI annotations

---

### **Step 13: Create Global Exception Handler**

**File:** `backend/document-core/src/main/java/.../infrastructure/adapter/exception/GlobalExceptionHandler.java`

**Action:** Centralize error handling for all API exceptions.

**Function Signature:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
  
  @ExceptionHandler(CarpetaNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleCarpetaNotFound(CarpetaNotFoundException ex) { ... }
  
  @ExceptionHandler(CarpetaNombreDuplicadoException.class)
  public ResponseEntity<ErrorResponse> handleNombreDuplicado(CarpetaNombreDuplicadoException ex) { ... }
  
  @ExceptionHandler(SinPermisoCarpetaException.class)
  public ResponseEntity<ErrorResponse> handleSinPermiso(SinPermisoCarpetaException ex) { ... }
  
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) { ... }
}
```

**Implementation Steps:**

1. **Map Domain Exceptions to HTTP Codes:**
   - `CarpetaNotFoundException` → 404
   - `CarpetaNombreDuplicadoException` → 409
   - `SinPermisoCarpetaException` → 403

2. **Create Error Response Object:**
   ```java
   @Data
   public class ErrorResponse {
     private String codigo;
     private String mensaje;
     private Map<String, Object> detalles;
     private Instant timestamp;
   }
   ```

3. **Handle Validation Errors:**
   - Extract field validation errors
   - Return 400 Bad Request

**Implementation Notes:**
- Use `@RestControllerAdvice` for global error handling
- Consistent error format across all endpoints
- Log errors appropriately (WARN for expected, ERROR for unexpected)

---

### **Step 14: Write Unit Tests - Service Layer**

**File:** `backend/document-core/src/test/java/.../application/service/CarpetaServiceTest.java`

**Action:** Test service logic with mocks (TDD approach).

**Implementation Steps:**

1. **Setup Test Class:**
   ```java
   @ExtendWith(MockitoExtension.class)
   class CarpetaServiceTest {
     @Mock private ICarpetaRepository repository;
     @Mock private CarpetaValidator validator;
     @Mock private ApplicationEventPublisher eventPublisher;
     @Mock private PermissionEvaluator permissionEvaluator;
     
     @InjectMocks private CarpetaService service;
   }
   ```

2. **Test Successful Creation:**
   ```java
   @Test
   void testCrearCarpetaExitosa() {
     // Arrange
     CreateCarpetaDTO dto = new CreateCarpetaDTO();
     UUID organizacionId = UUID.randomUUID();
     UUID usuarioId = UUID.randomUUID();
     
     Carpeta expected = Carpeta.builder()
       .id(UUID.randomUUID())
       .organizacionId(organizacionId)
       .creadoPor(usuarioId)
       .build();
     
     when(repository.crear(any(Carpeta.class))).thenReturn(expected);
     
     // Act
     Carpeta result = service.crear(dto, usuarioId, organizacionId);
     
     // Assert
     assertEquals(expected.getId(), result.getId());
     verify(repository).crear(any());
     verify(eventPublisher).publishEvent(any());
   }
   ```

3. **Test Validation Errors:**
   - Empty name → exception
   - Duplicate name → exception
   - Parent not found → exception
   - No permissions → exception

4. **Test Edge Cases:**
   - NULL description (optional field)
   - Maximum length strings
   - Different organizations (isolation)

5. **Aim for >90% Coverage:**
   - Test all branches in service logic
   - Test all exception paths

**Implementation Notes:**
- Use @Mock for dependencies
- Use @InjectMocks for service under test
- Follow AAA pattern (Arrange, Act, Assert)

---

### **Step 15: Add Audit Event**

**File:** `backend/document-core/src/main/java/.../domain/event/CarpetaCreatedEvent.java`

**Action:** Define event for audit logging.

**Implementation:**
```java
public record CarpetaCreatedEvent(
  UUID carpetaId,
  UUID organizacionId,
  UUID usuarioId,
  Instant timestamp
) implements ApplicationEvent {
  
  public CarpetaCreatedEvent(UUID carpetaId, UUID organizacionId, UUID usuarioId) {
    this(carpetaId, organizacionId, usuarioId, Instant.now());
  }
}
```

**Implementation Notes:**
- Use Java records for immutability
- Publish via `ApplicationEventPublisher.publishEvent()`
- Audit system subscribes with `@EventListener`

---

### **Step 16: Update OpenAPI Documentation**

**File:** `ai-specs/specs/api-spec.yml`

**Action:** Document the new endpoint in OpenAPI/Swagger spec.

**Implementation Steps:**

1. **Add POST /api/carpetas Endpoint:**
   ```yaml
   /api/carpetas:
     post:
       summary: Crear una nueva carpeta
       operationId: crearCarpeta
       tags:
         - Carpetas
       requestBody:
         required: true
         content:
           application/json:
             schema:
               $ref: '#/components/schemas/CreateCarpetaDTO'
       responses:
         '201':
           description: Carpeta creada exitosamente
           content:
             application/json:
               schema:
                 $ref: '#/components/schemas/CarpetaResponse'
         '400':
           description: Validación fallida
         '403':
           description: Sin permisos
         '404':
           description: Carpeta padre no existe
         '409':
           description: Nombre duplicado
   ```

2. **Add Data Models:**
   ```yaml
   components:
     schemas:
       CreateCarpetaDTO:
         type: object
         required:
           - carpeta_padre_id
           - nombre
         properties:
           carpeta_padre_id:
             type: string
             format: uuid
           nombre:
             type: string
             minLength: 1
             maxLength: 255
           descripcion:
             type: string
             maxLength: 500
       
       CarpetaResponse:
         type: object
         properties:
           data:
             $ref: '#/components/schemas/CarpetaDTO'
           meta:
             $ref: '#/components/schemas/Meta'
       
       CarpetaDTO:
         type: object
         properties:
           id:
             type: string
             format: uuid
           organizacion_id:
             type: string
             format: uuid
           carpeta_padre_id:
             type: string
             format: uuid
           nombre:
             type: string
           descripcion:
             type: string
           creado_por:
             type: string
             format: uuid
           fecha_creacion:
             type: string
             format: date-time
           fecha_actualizacion:
             type: string
             format: date-time
   ```

3. **Add Error Schema:**
   ```yaml
   ErrorResponse:
     type: object
     properties:
       error:
         type: object
         properties:
           codigo:
             type: string
           mensaje:
             type: string
           detalles:
             type: object
   ```

---

### **Step 17: Create Technical Documentation**

**File:** `backend/document-core/docs/CARPETAS.md`

**Action:** Document the feature for developers.

**Contents:**

```markdown
# Carpetas (Folders) - Feature Documentation

## Overview
Carpetas provides a hierarchical folder structure within DocFlow, allowing users to organize documents by folders with granular permission control.

## Architecture
- **Domain Model:** `Carpeta` (immutable, aggregate root)
- **Repository:** `ICarpetaRepository` with implementation via `CarpetaRepositoryAdapter`
- **Service:** `CarpetaService` for business logic and orchestration
- **API:** REST endpoint POST /api/carpetas

## Database Schema
- **Table:** `carpetas`
- **Key Fields:** id, organizacion_id, carpeta_padre_id, nombre, descripcion, creado_por, fecha_creacion, fecha_actualizacion, fecha_eliminacion
- **Indexes:** (organizacion_id, carpeta_padre_id), unique partial on (organizacion_id, carpeta_padre_id, nombre)

## API Examples

### Create Folder
```http
POST /api/carpetas
Authorization: Bearer {token}
Content-Type: application/json

{
  "carpeta_padre_id": "770e8400-e29b-41d4-a716-446655440222",
  "nombre": "Proyecto X",
  "descripcion": "Documentación del proyecto"
}
```

**Response (201):**
```json
{
  "data": {
    "id": "660e8400-e29b-41d4-a716-446655440111",
    "organizacion_id": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Proyecto X",
    "creado_por": "880e8400-e29b-41d4-a716-446655440333",
    "fecha_creacion": "2026-01-28T10:30:00Z"
  },
  "meta": {
    "accion": "CARPETA_CREADA"
  }
}
```

## Business Rules
1. **Permission Check:** User must have ESCRITURA or ADMINISTRACION on parent folder
2. **Name Uniqueness:** Name must be unique within the same parent folder per organization
3. **Soft Delete:** Deleted folders are marked with fecha_eliminacion, not removed
4. **Organization Isolation:** All operations scoped to organizacion_id from token

## Integration Points
- **ACL System (US-ACL-006):** Permission evaluation for parent folder
- **Audit System:** Emits CarpetaCreatedEvent for audit logging
- **Organization Service:** Validates organization context

## Testing
- **Unit Tests:** CarpetaServiceTest (>90% coverage)

---
### **Step 18: Update Technical Documentation**

**Action:** Review and update technical documentation per project standards.

**Implementation Steps:**

1. **Review Changes:**
   - Analyze all code changes: new database table, domain entity, service, controller, tests
   - Identify affected documentation areas

2. **Identify Documentation Files:**
   - `ai-specs/specs/data-model.md` — Add Carpeta table description
   - `ai-specs/specs/api-spec.yml` — Add POST /api/carpetas endpoint (done in Step 17)
   - `ai-specs/specs/backend-standards.md` — Reference folder creation patterns if needed
   - `backend/document-core/docs/CARPETAS.md` — Feature documentation (done in Step 18)

3. **Update Data Model Documentation:**
   ```markdown
   ### Carpetas Table
   
   **Purpose:** Hierarchical folder structure for organizing documents within an organization.
   
   | Column | Type | Constraints | Notes |
   |--------|------|-------------|-------|
   | id | UUID | PK | Auto-generated |
   | organizacion_id | UUID | FK, NOT NULL | Organization tenant isolation |
   | carpeta_padre_id | UUID | FK, NULL | Self-reference for hierarchy |
   | nombre | VARCHAR(255) | NOT NULL | Unique per parent per org |
   | descripcion | TEXT | NULL | Optional metadata |
   | creado_por | UUID | FK, NOT NULL | Audit trail |
   | fecha_creacion | TIMESTAMP | DEFAULT NOW() | Auto-set |
   | fecha_actualizacion | TIMESTAMP | DEFAULT NOW() | Auto-set |
   | fecha_eliminacion | TIMESTAMP | NULL | Soft delete marker |
   
   **Indexes:**
   - idx_carpetas_org_padre: (organizacion_id, carpeta_padre_id)
   - ux_carpeta_nombre_padre_org: UNIQUE on (organizacion_id, carpeta_padre_id, nombre) WHERE fecha_eliminacion IS NULL
   ```

4. **Update API Specification:**
   - Already covered in Step 17 (api-spec.yml)
   - Ensure consistency between OpenAPI spec and actual implementation

5. **Update Backend Standards:**
   - Add reference to folder creation pattern if creating new architectural guidelines
   - Document organization isolation best practices applied in this feature

6. **Verify Documentation:**
   - Confirm all changes are accurately reflected
   - Check formatting and consistency with existing docs
   - Ensure spanish language (per documentation standards)

7. **Report Updates:**
   - Document which files were updated
   - Examples: data-model.md (Carpeta table), api-spec.yml (POST endpoint), CARPETAS.md (feature doc)

---

## 4. Orden de Implementación

2. **Paso 1:** Crear Migraciones de Base de Datos (V003, V004)
3. **Paso 2:** Crear Migración de Carpeta Raíz
4. **Paso 3:** Crear Modelo de Dominio - Entidad Carpeta
5. **Paso 4:** Crear Interfaz de Repositorio
6. **Paso 5:** Crear Entidad JPA
7. **Paso 6:** Crear Repositorio JPA
8. **Paso 7:** Crear Mapper (Entidad → Dominio)
9. **Paso 8:** Crear Validador
10. **Paso 9:** Crear Servicio
11. **Paso 10:** Crear DTOs y Mappers (Capa API)
12. **Paso 11:** Crear Clases de Excepción
13. **Paso 12:** Crear Controlador
14. **Paso 13:** Crear Manejador Global de Excepciones
15. **Paso 14:** Escribir Tests Unitarios (Servicio)
17. **Paso 15:** Agregar Evento de Auditoría
18. **Paso 16:** Actualizar Documentación OpenAPI
19. **Paso 17:** Crear Documentación Técnica
21. **Paso 18:** Actualizar Documentación Técnica (OBLIGATORIO)

---

## 5. Lista de Verificación de Pruebas

### Tests Unitarios (Capa de Servicio)
- [ ] Probar crear carpeta con datos válidos → 201
- [ ] Probar crear con nombre vacío → excepción
- [ ] Probar crear con nombre duplicado → excepción
- [ ] Probar crear sin carpeta padre → excepción
- [ ] Probar crear sin permisos → excepción
- [ ] Probar carpeta padre de diferente organización → excepción
- [ ] Probar eliminación lógica permite reusar nombre → éxito
- [ ] Probar emisión de evento después de crear → verificado

### Tests de Integración (Controlador)
- [ ] POST /api/carpetas con datos válidos → 201
- [ ] POST sin carpeta_padre_id → 400
- [ ] POST con nombre > 255 caracteres → 400
- [ ] POST de usuario sin ESCRITURA → 403
- [ ] POST con padre no existente → 404
- [ ] POST con nombre duplicado → 409
- [ ] POST desde diferente organización → 404
- [ ] Verificar carpeta creada en BD
- [ ] Verificar evento emitido
- [ ] Verificar carpeta aparece en consultas de lista

### Casos Especiales
- [ ] Crear carpeta bajo raíz
- [ ] Crear carpetas anidadas (3+ niveles de profundidad)
- [ ] Caracteres Unicode en nombre
- [ ] Nombres con caracteres especiales
- [ ] Descripciones muy largas (límite de 500)

---

## 6. Formato de Respuesta de Error

```json
{
  "error": {
    "codigo": "CODIGO_ERROR",
    "mensaje": "Mensaje legible para usuario en español",
    "detalles": {
      "campo": "contexto adicional"
    }
  }
}
```

### Mapeo de Código de Estado HTTP

| Escenario | Código | Código Error |
|----------|--------|-------------|
| Carpeta creada | 201 | N/A |
| Validación fallida | 400 | VALIDACION_FALLIDA |
| No autorizado | 401 | NO_AUTENTICADO |
| Sin permisos | 403 | SIN_PERMISO_CARPETA |
| Padre no encontrado | 404 | CARPETA_NO_ENCONTRADA |
| Nombre duplicado | 409 | NOMBRE_DUPLICADO |
| Error de servidor | 500 | ERROR_INTERNO |

---

## 7. Dependencias

### Librerías Java (verificar versiones en pom.xml)
- Spring Boot 3.x
- Spring Data JPA
- Spring Validation (jakarta.validation)
- MapStruct 1.5+
- Lombok (opcional)
- JUnit 5 + Mockito (testing)

### Base de Datos
- PostgreSQL 12+ (o H2 para tests)
- Flyway para migraciones

### Servicios Externos
- Evaluador de Permisos (US-ACL-006, stub inicialmente)
- Sistema de Eventos de Auditoría (event listener)

---

## 8. Notas

### Recordatorios Importantes

1. **Aislamiento de Organización es Crítico:**
   - Todas las consultas deben filtrar por `organizacion_id` desde token
   - Nunca confiar en entrada del usuario para organización
   - Retornar 404 (no 403) si carpeta pertenece a org diferente (evitar filtración de información)

2. **Conciencia de Eliminación Lógica:**
   - Las consultas deben verificar `fecha_eliminacion IS NULL`
   - Anotación JPA `@Where` maneja esto automáticamente
   - Permite reusar nombres después de eliminación

3. **Integración de Permisos:**
   - Actualmente usar stub si US-ACL-006 no está lista
   - Stub siempre retorna true para rol ADMIN
   - Reemplazar stub cuando evaluador real esté disponible

4. **Filosofía de Testing:**
   - Escribir tests primero (TDD)
   - Probar ruta feliz y casos de error
   - Tests de integración usan BD real (H2 o testcontainers)
   - Apuntar a cobertura >90%

5. **Formato de Respuesta API:**
   - Siempre usar envoltorio: `{ data: {...}, meta: {...} }` o `{ error: {...} }`
   - Incluir código de operación en meta: "CARPETA_CREADA"
   - Incluir timestamp en meta

6. **Convenciones de Nomenclatura:**
   - Clases: PascalCase (CarpetaService)
   - Métodos: camelCase (crearCarpeta)
   - Constantes: MAYUSCULA_CON_GUIONES (MAX_NOMBRE_LENGTH)
   - Columnas de BD: snake_case (carpeta_padre_id)

7. **Estándares de Documentación:**
   - Toda documentación en inglés (per estándares del proyecto)
   - Usar Markdown para docs técnicos
   - Incluir ejemplos de código y escenarios de error
   - Actualizar especificación OpenAPI concurrentemente

---

---

## 10. Implementation Verification

### Final Verification Checklist

✅ **Code Quality**
- [ ] No compilation errors
- [ ] No warning from Maven build
- [ ] Code follows project style guide (ESLint, Checkstyle)
- [ ] No security vulnerabilities (spotbugs)
- [ ] All code formatted consistently

✅ **Functionality**
- [ ] Create endpoint works (201)
- [ ] Permissions validated (403)
- [ ] Name uniqueness enforced (409)
- [ ] Parent folder check (404)
- [ ] Input validation (400)
- [ ] Organization isolation verified (404)

✅ **Testing**
- [ ] All unit tests passing (mvn test)
- [ ] All integration tests passing
- [ ] >90% code coverage
- [ ] Edge cases covered
- [ ] Error scenarios tested

✅ **Integration**
- [ ] Event emission verified (audit trail)
- [ ] Database persistence verified
- [ ] API response format correct
- [ ] OpenAPI spec matches implementation
- [ ] Exception handling works correctly

✅ **Documentation**
- [ ] OpenAPI spec updated (api-spec.yml)
- [ ] Data model documentation updated (data-model.md)
- [ ] Feature documentation created (CARPETAS.md)
- [ ] Backend standards referenced (if applicable)
- [ ] Code comments added for complex logic

---

## Summary

This plan provides a step-by-step, developer-autonomous approach to implementing US-FOLDER-001. Each step is detailed with file paths, function signatures, implementation notes, and validation criteria. The developer can follow this plan sequentially, writing tests first (TDD), and produce a complete, tested, documented feature ready for production.

**Estimated Effort:** 2-3 days with support from AI assistant for code generation  
**Complexity:** Medium (hierarchical data + permissions + soft delete)  
**Dependencies:** US-ACL-001 (completed), stub for US-ACL-006 (permission evaluator)  
**Blocker Risk:** Low (all dependencies can be stubbed/mocked)

