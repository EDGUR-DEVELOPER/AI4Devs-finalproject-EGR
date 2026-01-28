# Backend Implementation Plan: US-ACL-001 Definir niveles de acceso estándar

## Overview

Implementación del catálogo centralizado de niveles de acceso (ACL) para el sistema de gestión documental DocFlow. Este feature establece la base para el modelo RBAC (Role-Based Access Control) que será usado en todo el sistema. Los niveles de acceso (`LECTURA`, `ESCRITURA`, `ADMINISTRACION`) serán persistidos en la base de datos, cached en backend, y consumidos por frontend y otros servicios.

**Principios Arquitectónicos:**
- Domain-Driven Design (DDD): Entidades y lógica de dominio desacopladas
- Hexagonal Architecture: Puertos (interfaces) y adaptadores (implementaciones)
- Clean Code: Responsabilidad única, testing exhaustivo (TDD)
- SOLID Principles: Especialmente LSP e ISP para interfaces de validación

---

## Architecture Context

### Layers Involved

1. **Domain Layer** (`src/main/java/.../domain/acl/`)
   - Entidad `NivelAcceso` (value object para el catálogo)
   - Enum `CodigoNivelAcceso` (LECTURA, ESCRITURA, ADMINISTRACION)
   - Interface `INivelAccesoRepository` (contrato de persistencia)

2. **Application Layer** (`src/main/java/.../application/services/`)
   - `NivelAccesoService` (orquestación de lógica de negocio)
   - `NivelAccesoValidator` (validación centralizada)

3. **Infrastructure Layer** (`src/main/java/.../infrastructure/`)
   - `NivelAccesoRepositoryImpl` (implementación con Spring Data JPA)
   - `NivelAccesoRepository` (Spring Data JPA interface)
   - `NivelAccesoEntity` (JPA entity para persistencia)
   - `NivelAccesoMapper` (mapeo DTO ↔ Domain)
   - `AclController` (exposición REST)
   - `NivelAccesoDTO` (contrato de respuesta API)

4. **Database Layer**
   - Tabla `nivel_acceso` con estructura JSONB para acciones
   - Seed SQL idempotente para cargar datos iniciales

### Key Files & Structure

```
backend/document-core/
├── src/main/java/com/docflow/
│   ├── domain/acl/
│   │   ├── NivelAcceso.java                   # Entidad de dominio
│   │   ├── CodigoNivelAcceso.java             # Enum de códigos
│   │   └── INivelAccesoRepository.java        # Puerto (interfaz)
│   │
│   ├── application/services/
│   │   ├── NivelAccesoService.java            # Orquestación
│   │   └── NivelAccesoValidator.java          # Validación
│   │
│   └── infrastructure/
│       ├── adapters/persistence/
│       │   ├── entity/NivelAccesoEntity.java  # JPA Entity
│       │   ├── NivelAccesoJpaRepository.java  # Spring Data JPA
│       │   └── NivelAccesoRepositoryImpl.java  # Implementación del puerto
│       ├── api/
│       │   ├── controllers/AclController.java # Endpoints REST
│       │   ├── dto/NivelAccesoDTO.java        # DTO de respuesta
│       │   └── mappers/NivelAccesoMapper.java # MapStruct mapper
│       │
│       └── config/JpaConfig.java              # Configuración JPA
│
├── src/main/resources/db/migration/
│   └── V001__Create_Nivel_Acceso_Table.sql   # Creación de tabla
│
├── src/main/resources/db/seeds/
│   └── S001__Seed_Niveles_Acceso.sql          # Inserción de datos
│
└── src/test/java/com/docflow/
    ├── domain/acl/NivelAccesoTest.java
    ├── application/services/
    │   ├── NivelAccesoServiceTest.java
    │   └── NivelAccesoValidatorTest.java
    └── infrastructure/
        ├── adapters/NivelAccesoRepositoryTest.java
        └── api/controllers/AclControllerTest.java
```

---

## Implementation Steps

### Step 1: Create Database Migration Script

**File**: `backend/document-core/src/main/resources/db/migration/V001__Create_Nivel_Acceso_Table.sql`

**Action**: Crear tabla PostgreSQL para almacenar niveles de acceso con estructura JSONB flexible

**Implementation Steps**:

1. **Create Table Structure**:
   - Crear tabla `nivel_acceso` con campos: id (PK), codigo (UNIQUE), nombre, descripcion, acciones_permitidas (JSONB), orden, activo, fecha_creacion, fecha_actualizacion
   - Usar UUID para id o SERIAL según convención del proyecto
   - Campo `codigo` UNIQUE NOT NULL para invariabilidad
   - Campo `acciones_permitidas` JSONB NOT NULL para flexibilidad futura
   - Timestamps con DEFAULT CURRENT_TIMESTAMP

2. **Create Indexes**:
   - Index en `codigo` para búsquedas rápidas por código
   - Index en `activo` para filtrados

3. **Add Constraints**:
   - CHECK constraint: `activo` es booleano
   - CHECK constraint: `codigo` en [LECTURA, ESCRITURA, ADMINISTRACION] o flexible si aplica

**SQL Template**:
```sql
CREATE TABLE IF NOT EXISTS nivel_acceso (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  codigo VARCHAR(50) NOT NULL UNIQUE,
  nombre VARCHAR(100) NOT NULL,
  descripcion TEXT,
  acciones_permitidas JSONB NOT NULL,
  orden INT,
  activo BOOLEAN NOT NULL DEFAULT true,
  fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_nivel_acceso_codigo ON nivel_acceso(codigo);
CREATE INDEX idx_nivel_acceso_activo ON nivel_acceso(activo);
```

**Dependencies**: PostgreSQL 12+ con soporte JSONB

**Implementation Notes**:
- Usar migraciones Flyway setup del proyecto
- Versionado: V001 (primera migración), V002, etc. si hay cambios posteriores
- No es necesario usar `ON DELETE CASCADE` para esta tabla (es catálogo independiente)

---

### Step 2: Create Database Seed Script

**File**: `backend/document-core/src/main/resources/db/seeds/S001__Seed_Niveles_Acceso.sql`

**Action**: Insertar datos estándar de niveles de acceso de forma idempotente

**Implementation Steps**:

1. **Insert Standard Levels**:
   - LECTURA: ver, listar, descargar
   - ESCRITURA: ver, listar, descargar, subir, modificar, crear_version
   - ADMINISTRACION: todas las acciones

2. **Ensure Idempotency**:
   - Usar `INSERT INTO ... ON CONFLICT DO NOTHING` (PostgreSQL)
   - O `INSERT IGNORE` (MySQL) si aplica
   - Evitar duplicación en ejecuciones múltiples

3. **Define Actions JSON**:
   - Usar array JSON: `["ver", "listar", "descargar"]`
   - Orden consistente alfabético o por importancia

**SQL Template**:
```sql
INSERT INTO nivel_acceso (codigo, nombre, descripcion, acciones_permitidas, orden, activo)
VALUES
  ('LECTURA', 'Lectura / Consulta', 
   'Permite ver, listar y descargar documentos. Sin capacidad de modificación.',
   '["ver", "listar", "descargar"]'::jsonb, 1, true),
  
  ('ESCRITURA', 'Escritura / Modificación',
   'Permite subir nuevas versiones, renombrar y modificar metadatos de documentos.',
   '["ver", "listar", "descargar", "subir", "modificar", "crear_version"]'::jsonb, 2, true),
  
  ('ADMINISTRACION', 'Administración / Control Total',
   'Acceso total: crear, modificar, eliminar carpetas/documentos y gestionar permisos granulares.',
   '["ver", "listar", "descargar", "subir", "modificar", "crear_version", "eliminar", "administrar_permisos", "cambiar_version_actual"]'::jsonb, 3, true)
ON CONFLICT (codigo) DO NOTHING;
```

**Implementation Notes**:
- Si semilla es ejecutada con `@Sql` en tests, usar `classpath:db/seeds/S001__Seed_Niveles_Acceso.sql`
- Datos deben coincidir exactamente con la especificación de US-ACL-001

---

### Step 3: Create Domain Entity and Enum

**File**: `backend/document-core/src/main/java/com/docflow/domain/acl/NivelAcceso.java`

**Action**: Implementar entidad de dominio que representa un nivel de acceso (value object inmutable)

**Function Signature**:
```java
public class NivelAcceso {
  private final UUID id;
  private final String codigo;                    // LECTURA, ESCRITURA, ADMINISTRACION
  private final String nombre;
  private final String descripcion;
  private final List<String> accionePermitidas;   // Array JSON deserializado
  private final Integer orden;
  private final boolean activo;
  // ... getters
}
```

**Implementation Steps**:

1. **Define Class Structure**:
   - Marcar como `final` (inmutable)
   - Todos los campos privados y finales
   - Constructor con todos los parámetros + validaciones

2. **Add Validations** (en constructor):
   - `codigo` NOT NULL y válido (máx 50 chars)
   - `nombre` NOT NULL (máx 100 chars)
   - `accionePermitidas` NOT NULL y no vacío

3. **Implement Getter Methods**:
   - `getId()`, `getCodigo()`, `getNombre()`, etc.
   - Retornar copias de colecciones (`Collections.unmodifiableList()`)

4. **Add Factory Method**:
   - `static NivelAcceso crear(String codigo, String nombre, ...)` para construcción con validaciones

5. **Add Equals & HashCode**:
   - Basarse en `codigo` (identificador único)

**Dependencies**:
```java
import java.util.*;
import lombok.Getter;  // O generar manualmente
```

**Implementation Notes**:
- No usar Lombok `@Data` (genera setters que violan inmutabilidad)
- Usar `@Getter` de Lombok si se permite (solo getters, sin setters)
- Puede usar `@Value` de Lombok (equivalente a `final` + getters)

**File**: `backend/document-core/src/main/java/com/docflow/domain/acl/CodigoNivelAcceso.java`

**Action**: Crear enum para códigos estándar (type-safe alternative a strings)

**Function Signature**:
```java
public enum CodigoNivelAcceso {
  LECTURA("LECTURA"),
  ESCRITURA("ESCRITURA"),
  ADMINISTRACION("ADMINISTRACION");
  
  private final String valor;
}
```

**Implementation Steps**:
1. Definir los 3 valores del enum
2. Agregar campo `valor` (String) para serialización
3. Agregar método `fromString(String codigo)` para deserialización

---

### Step 4: Create Repository Interface (Domain Port)

**File**: `backend/document-core/src/main/java/com/docflow/domain/acl/INivelAccesoRepository.java`

**Action**: Definir contrato (puerto) para acceso a datos de niveles de acceso

**Function Signature**:
```java
public interface INivelAccesoRepository {
  Optional<NivelAcceso> findByCodigo(String codigo);
  List<NivelAcceso> findAll();
  List<NivelAcceso> findAllActivos();
  NivelAcceso save(NivelAcceso nivel);
  boolean existsByCodigo(String codigo);
}
```

**Implementation Steps**:

1. **Define Query Methods**:
   - `findByCodigo(String)`: Búsqueda por código único
   - `findAll()`: Todos los niveles (incluyendo inactivos)
   - `findAllActivos()`: Solo niveles con `activo=true`
   - `existsByCodigo(String)`: Validación de existencia

2. **Define Mutation Methods**:
   - `save(NivelAcceso)`: Persistir o actualizar

3. **Use Return Types**:
   - `Optional<T>` para búsquedas que pueden no encontrar
   - `List<T>` para múltiples resultados (nunca null)
   - `boolean` para existencia

**Dependencies**: 
```java
import java.util.Optional;
import java.util.List;
```

**Implementation Notes**:
- Esta es una interfaz en la capa de dominio (no contiene lógica, solo contrato)
- La implementación estará en infrastructure layer

---

### Step 5: Create Spring Data JPA Repository Adapter

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/adapters/persistence/NivelAccesoJpaRepository.java`

**Action**: Extender Spring Data JPA para queries boilerplate (adaptador técnico)

**Function Signature**:
```java
@Repository
public interface NivelAccesoJpaRepository extends JpaRepository<NivelAccesoEntity, UUID> {
  Optional<NivelAccesoEntity> findByCodigo(String codigo);
  List<NivelAccesoEntity> findByActivoTrue();
}
```

**Implementation Steps**:

1. **Extend JpaRepository**:
   - Parámetros: `<Entity, PrimaryKeyType>` = `<NivelAccesoEntity, UUID>`

2. **Define Custom Query Methods**:
   - `findByCodigo(String codigo)`: Spring Data genera SQL automáticamente
   - `findByActivoTrue()`: Filtro por `activo=true`

3. **Add @Query if Needed**:
   - Para queries complejas, usar `@Query` con JPQL

**Dependencies**:
```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
```

**Implementation Notes**:
- Nombres de método siguen convención Spring Data (findBy + FieldName)
- No implementar lógica aquí; Spring Data genera queries

---

### Step 6: Create JPA Entity

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/adapters/persistence/entity/NivelAccesoEntity.java`

**Action**: Mapear entidad JPA a tabla `nivel_acceso` en PostgreSQL

**Function Signature**:
```java
@Entity
@Table(name = "nivel_acceso")
@Getter
@NoArgsConstructor
public class NivelAccesoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  
  @Column(nullable = false, unique = true, length = 50)
  private String codigo;
  
  @Column(nullable = false, length = 100)
  private String nombre;
  
  @Column(columnDefinition = "TEXT")
  private String descripcion;
  
  @Column(columnDefinition = "JSONB", nullable = false)
  @Convert(converter = JsonbListConverter.class)
  private List<String> accionePermitidas;
  
  @Column
  private Integer orden;
  
  @Column(nullable = false)
  private boolean activo = true;
  
  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime fechaCreacion;
  
  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime fechaActualizacion;
}
```

**Implementation Steps**:

1. **Define Table Mapping**:
   - `@Entity` marca como entidad JPA
   - `@Table(name = "nivel_acceso")` mapeo a tabla específica

2. **Map Columns**:
   - `@Id` y `@GeneratedValue` para clave primaria
   - `@Column` con atributos: nullable, unique, length, columnDefinition
   - Usar `GenerationType.UUID` para PostgreSQL (o IDENTITY si SERIAL)

3. **Handle JSONB Column**:
   - `columnDefinition = "JSONB"` para PostgreSQL
   - Crear converter personalizado: `JsonbListConverter` (ver Step 8)
   - O usar biblioteca: `com.vladmihalceev.hibernate.type.json.JsonType`

4. **Add Timestamps**:
   - `@CreationTimestamp` y `@UpdateTimestamp` de Hibernate
   - `updatable = false` en fecha_creacion

5. **Add Constructors**:
   - Constructor vacío para Hibernate
   - Constructor con todos los campos para construcción

**Dependencies**:
```java
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.NoArgsConstructor;
```

**Implementation Notes**:
- Jakarta Persistence (Jakarta EE 9+) en lugar de javax.persistence (versión antigua)
- Si no hay soporte JSONB nativo, usar `String` y serializar manualmente

---

### Step 7: Create AttributeConverter for JSONB

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/adapters/persistence/converters/JsonbListConverter.java`

**Action**: Convertir lista de strings ↔ JSONB para persistencia transparente

**Function Signature**:
```java
@Converter(autoApply = true)
public class JsonbListConverter implements AttributeConverter<List<String>, String> {
  
  @Override
  public String convertToDatabaseColumn(List<String> list) {
    // List → JSON String
  }
  
  @Override
  public List<String> convertToEntityAttribute(String json) {
    // JSON String → List
  }
}
```

**Implementation Steps**:

1. **Implement convertToDatabaseColumn**:
   - Recibe `List<String>` desde entidad
   - Serializa a JSON: `new ObjectMapper().writeValueAsString(list)`
   - Retorna String

2. **Implement convertToEntityAttribute**:
   - Recibe `String` JSON desde BD
   - Deserializa: `new ObjectMapper().readValue(json, new TypeReference<List<String>>(){})`
   - Retorna `List<String>` (o `Collections.emptyList()` si null)

3. **Handle Null Values**:
   - Si input null, retornar `null` o `Collections.emptyList()`

**Dependencies**:
```java
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
```

**Implementation Notes**:
- `@Converter(autoApply = true)` aplica automáticamente a todos los campos `List<String>`
- Si hay conflictos, remover `autoApply = true` y usar `@Convert(converter = JsonbListConverter.class)` en cada campo
- Usar `ObjectMapper` de Jackson (ya incluida en Spring Boot)

---

### Step 8: Create Repository Implementation (Domain Port Adapter)

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/adapters/persistence/NivelAccesoRepositoryImpl.java`

**Action**: Implementar interfaz `INivelAccesoRepository` usando Spring Data JPA

**Function Signature**:
```java
@Component
public class NivelAccesoRepositoryImpl implements INivelAccesoRepository {
  
  private final NivelAccesoJpaRepository jpaRepository;
  private final NivelAccesoMapper mapper;
  
  public NivelAccesoRepositoryImpl(
    NivelAccesoJpaRepository jpaRepository,
    NivelAccesoMapper mapper
  ) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }
  
  @Override
  public Optional<NivelAcceso> findByCodigo(String codigo) { }
  
  @Override
  public List<NivelAcceso> findAll() { }
  
  @Override
  public List<NivelAcceso> findAllActivos() { }
  
  @Override
  public NivelAcceso save(NivelAcceso nivel) { }
  
  @Override
  public boolean existsByCodigo(String codigo) { }
}
```

**Implementation Steps**:

1. **Inject Dependencies**:
   - `NivelAccesoJpaRepository` (Spring Data)
   - `NivelAccesoMapper` (MapStruct para DTO ↔ Entity)
   - Usar constructor injection

2. **Implement findByCodigo**:
   - Llamar `jpaRepository.findByCodigo(codigo)`
   - Mapear resultado: `.map(mapper::toDomain)`
   - Retornar `Optional`

3. **Implement findAll**:
   - Llamar `jpaRepository.findAll()`
   - Stream y mapear: `.stream().map(mapper::toDomain).collect(Collectors.toList())`
   - Retornar `List` (nunca null)

4. **Implement findAllActivos**:
   - Llamar `jpaRepository.findByActivoTrue()`
   - Mapear lista como en findAll

5. **Implement save**:
   - Convertir domain → entity: `mapper.toEntity(nivel)`
   - Persistir: `jpaRepository.save(entity)`
   - Mapear resultado: `mapper.toDomain(saved)`

6. **Implement existsByCodigo**:
   - Llamar `jpaRepository.existsByCodigo(codigo)`
   - Retornar booleano

**Dependencies**:
```java
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
```

**Implementation Notes**:
- Usar `@Component` (no `@Repository`) para separar interfaz de implementación
- O usar `@Repository` y nombrar `NivelAccesoRepositoryImpl` si se requiere
- Constructor injection evita mutación de estado

---

### Step 9: Create MapStruct Mapper

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/api/mappers/NivelAccesoMapper.java`

**Action**: Mapear entre JPA Entity, Domain Model, y DTO usando MapStruct

**Function Signature**:
```java
@Mapper(componentModel = "spring")
public interface NivelAccesoMapper {
  
  NivelAcceso toDomain(NivelAccesoEntity entity);
  
  NivelAccesoEntity toEntity(NivelAcceso domain);
  
  NivelAccesoDTO toDTO(NivelAcceso domain);
  
  List<NivelAccesoDTO> toDTOList(List<NivelAcceso> domains);
}
```

**Implementation Steps**:

1. **Define Mapper Interface**:
   - Marcar con `@Mapper(componentModel = "spring")` para que Spring inyecte

2. **Create Methods**:
   - `toDomain()`: Entity → Domain (para repositorio)
   - `toEntity()`: Domain → Entity (para persistencia)
   - `toDTO()`: Domain → DTO (para API response)
   - `toDTOList()`: List mapping

3. **Add Custom Mappings if Needed**:
   - Si campos tienen nombres diferentes, usar `@Mapping(source = "...", target = "...")`
   - Ejemplo: `@Mapping(source = "codigo", target = "codigo")`

4. **Handle Nested Objects**:
   - `accionePermitidas` es `List<String>` en ambos lados (sin conversion)

**Dependencies**:
```java
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
```

**Implementation Notes**:
- MapStruct genera clases automáticamente en tiempo de compilación (`NivelAccesoMapperImpl`)
- No requiere reflexión (performance excelente)
- Configurar en `pom.xml`: 
  ```xml
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
      <annotationProcessorPaths>
        <path>
          <groupId>org.mapstruct</groupId>
          <artifactId>mapstruct-processor</artifactId>
        </path>
      </annotationProcessorPaths>
    </configuration>
  </plugin>
  ```

---

### Step 10: Create Service Validator

**File**: `backend/document-core/src/main/java/com/docflow/application/services/NivelAccesoValidator.java`

**Action**: Centralizar validaciones de negocio para niveles de acceso

**Function Signature**:
```java
@Service
public class NivelAccesoValidator {
  
  private final INivelAccesoRepository repository;
  
  public void validarCodigoExiste(String codigo) 
    throws NivelAccesoNotFoundException { }
  
  public void validarCodigoActivo(String codigo) 
    throws NivelAccesoInactivoException { }
  
  public void validarAccionesValidas(List<String> acciones) 
    throws AccionesInvalidasException { }
  
  public NivelAcceso obtenerOValidar(String codigo) { }
}
```

**Implementation Steps**:

1. **Define Validation Methods**:
   - `validarCodigoExiste(String codigo)`: Verifica que código existe en BD
     - Si no existe, lanzar `NivelAccesoNotFoundException`
   - `validarCodigoActivo(String codigo)`: Verifica que nivel está activo
     - Si inactivo, lanzar `NivelAccesoInactivoException`
   - `validarAccionesValidas(List<String> acciones)`: Verifica que acciones son conocidas
     - Si hay acciones inválidas, lanzar `AccionesInvalidasException`

2. **Add Helper Methods**:
   - `obtenerOValidar(String codigo)`: Busca y lanza excepción si no existe
   - Combina `findByCodigo()` + exception throwing

3. **Add Constants**:
   - Lista de acciones válidas: ACCIONES_VALIDAS = ["ver", "listar", "descargar", ...]

4. **Inject Dependencies**:
   - Constructor injection de `INivelAccesoRepository`

**Custom Exceptions** (crear en `domain/acl/exceptions/`):
```java
public class NivelAccesoNotFoundException extends RuntimeException { }
public class NivelAccesoInactivoException extends RuntimeException { }
public class AccionesInvalidasException extends RuntimeException { }
```

**Dependencies**:
```java
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
```

**Implementation Notes**:
- Lanzar excepciones específicas (no genéricas)
- Incluir detalles en mensaje: "Nivel de acceso 'LECTURA' no existe"
- Usar unchecked exceptions (RuntimeException) para que Spring traduzca a 404/400

---

### Step 11: Create Service

**File**: `backend/document-core/src/main/java/com/docflow/application/services/NivelAccesoService.java`

**Action**: Orquestar lógica de obtención de niveles de acceso

**Function Signature**:
```java
@Service
public class NivelAccesoService {
  
  private final INivelAccesoRepository repository;
  private final NivelAccesoValidator validator;
  
  public List<NivelAcceso> obtenerTodos() { }
  
  public NivelAcceso obtenerPorCodigo(String codigo) { }
  
  public List<NivelAcceso> obtenerActivos() { }
  
  public void validarNivelDisponible(String codigo) { }
}
```

**Implementation Steps**:

1. **Implement obtenerTodos**:
   - Llamar `repository.findAll()`
   - Retornar lista
   - Usa caché en futuras iteraciones

2. **Implement obtenerPorCodigo**:
   - Validar codigo existe: `validator.validarCodigoExiste(codigo)`
   - Llamar `repository.findByCodigo(codigo)`.orElseThrow()
   - Retornar el nivel

3. **Implement obtenerActivos**:
   - Llamar `repository.findAllActivos()`
   - Retornar lista

4. **Implement validarNivelDisponible**:
   - Validar que código existe: `validator.validarCodigoExiste(codigo)`
   - Validar que está activo: `validator.validarCodigoActivo(codigo)`

5. **Inject Dependencies**:
   - Constructor injection de `INivelAccesoRepository` y `NivelAccesoValidator`

**Dependencies**:
```java
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
```

**Implementation Notes**:
- Servicio delega validaciones a validator (SRP)
- En futuras historias, agregar caché con `@Cacheable`
- Métodos retornan domain model, no DTOs (DTOs son responsabilidad de controlador)

---

### Step 12: Create Controller

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/api/controllers/AclController.java`

**Action**: Exponer endpoints REST para consultar niveles de acceso

**Function Signature**:
```java
@RestController
@RequestMapping("/api/acl")
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGINS:http://localhost:3000}")
@Slf4j
public class AclController {
  
  private final NivelAccesoService service;
  private final NivelAccesoMapper mapper;
  
  @GetMapping("/niveles")
  public ResponseEntity<ApiResponse<List<NivelAccesoDTO>>> getNiveles() { }
  
  @GetMapping("/niveles/{codigo}")
  public ResponseEntity<ApiResponse<NivelAccesoDTO>> getNivelByCodigo(
    @PathVariable String codigo
  ) { }
}
```

**Implementation Steps**:

1. **Define Class Annotations**:
   - `@RestController`: Marca como controlador REST
   - `@RequestMapping("/api/acl")`: Base path para todos los endpoints
   - `@CrossOrigin`: Permitir requests desde frontend (configurar desde application.yml)
   - `@Slf4j`: Logging con SLF4J

2. **Implement GET /api/acl/niveles**:
   - Llamar `service.obtenerTodos()`
   - Mapear resultado: `mapper.toDTOList(niveles)`
   - Retornar `ResponseEntity.ok(new ApiResponse(data, ...))`
   - Status: 200 OK
   - Log: `logger.info("GET /api/acl/niveles")`

3. **Implement GET /api/acl/niveles/{codigo}**:
   - Validar parámetro `codigo` con `@Valid` (si aplica)
   - Llamar `service.obtenerPorCodigo(codigo)`
   - Mapear: `mapper.toDTO(nivel)`
   - Retornar `ResponseEntity.ok(new ApiResponse(data, ...))`
   - Status: 200 OK si encontrado, 404 si no (manejado por exception handler)
   - Log: `logger.info("GET /api/acl/niveles/{}", codigo)`

4. **Error Handling**:
   - `NivelAccesoNotFoundException` → 404 Not Found
   - `NivelAccesoInactivoException` → 400 Bad Request o 403 Forbidden
   - Manejado por `@ControllerAdvice` global (crear si no existe)

5. **Inject Dependencies**:
   - Constructor injection de `NivelAccesoService` y `NivelAccesoMapper`

**Response DTO Structure** (ver Step 13):
```json
{
  "data": [
    { "id": "...", "codigo": "LECTURA", "nombre": "...", "accionePermitidas": [...], "activo": true }
  ],
  "meta": { "total": 3, "timestamp": "2026-01-27T10:30:00Z" }
}
```

**Dependencies**:
```java
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.Slf4j;
```

**Implementation Notes**:
- Path `/api/acl` corresponde a "Access Control List" (ACL)
- Controlador es stateless (sin estado)
- Logging en cada endpoint para auditoría
- CORS configurada desde `application.yml`: `CORS_ALLOWED_ORIGINS`

---

### Step 13: Create DTO

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/api/dto/NivelAccesoDTO.java`

**Action**: Definir contrato de respuesta API para nivel de acceso

**Function Signature**:
```java
public record NivelAccesoDTO(
  UUID id,
  String codigo,
  String nombre,
  String descripcion,
  List<String> accionePermitidas,
  Integer orden,
  boolean activo
) { }
```

**Implementation Steps**:

1. **Use Java Record** (Java 21+):
   - Alternativa inmutable a clase POJO
   - Genera constructor, getters, equals, hashCode, toString automáticamente

2. **Define Fields**:
   - `id`: UUID (identificador único)
   - `codigo`: String (invariable: LECTURA, ESCRITURA, ADMINISTRACION)
   - `nombre`: String (ej. "Lectura / Consulta")
   - `descripcion`: String (explicación)
   - `accionePermitidas`: List<String> (acciones permitidas)
   - `orden`: Integer (posición en UI)
   - `activo`: boolean (disponibilidad)

3. **Add Annotations if Needed**:
   - `@JsonProperty` si nombres JSON difieren de field names
   - `@JsonInclude(Include.NON_NULL)` si hay campos opcionales

**Alternative** (si no usar Record):
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NivelAccesoDTO {
  private UUID id;
  private String codigo;
  // ... demás campos
}
```

**Dependencies**:
```java
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import java.util.List;
```

**Implementation Notes**:
- DTO expone solo campos que deben ser visibles en API (no expone timestamps internos)
- Record es más seguro que POJO (inmutable por defecto)
- Serialización automática con Jackson (Spring integrado)

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/api/dto/ApiResponse.java`

**Action**: Envolver respuesta API con metadatos

**Function Signature**:
```java
public class ApiResponse<T> {
  private T data;
  private ApiMeta meta;
}

public class ApiMeta {
  private Integer total;
  private LocalDateTime timestamp;
}
```

**Implementation Steps**:
1. Crear clase genérica `ApiResponse<T>`
2. Campos: `data` (payload), `meta` (metadatos)
3. Clase interna `ApiMeta` con `total`, `timestamp`
4. Constructores y getters

---

### Step 14: Create GlobalExceptionHandler

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/api/exceptions/GlobalExceptionHandler.java`

**Action**: Manejo centralizado de excepciones para traducir a respuestas HTTP consistentes

**Implementation Steps**:

1. **Create @ControllerAdvice Class**:
   ```java
   @ControllerAdvice
   @Slf4j
   public class GlobalExceptionHandler {
   ```

2. **Handle NivelAccesoNotFoundException**:
   - Mapear a `ResponseEntity<ErrorResponse>` con status 404
   - Mensaje: "Nivel de acceso no encontrado: {codigo}"

3. **Handle NivelAccesoInactivoException**:
   - Status 400 Bad Request
   - Mensaje: "Nivel de acceso inactivo: {codigo}"

4. **Handle Generic Exceptions**:
   - Catch-all para excepciones no esperadas
   - Status 500 Internal Server Error
   - Log error con stack trace

**Error Response Structure**:
```json
{
  "error": "NIVEL_NO_ENCONTRADO",
  "message": "Nivel de acceso no encontrado: LECTURA_ESPECIAL",
  "timestamp": "2026-01-27T10:30:00Z"
}
```

**Dependencies**:
```java
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.Slf4j;
```

---

### Step 15: Create Unit Tests - Validator

**File**: `backend/document-core/src/test/java/com/docflow/application/services/NivelAccesoValidatorTest.java`

**Action**: Testear validaciones de negocio con TDD (Red → Green → Refactor)

**Test Cases** (usar `@Nested` para agrupar):

#### Successful Cases
```java
@Test
void shouldNotThrowExceptionForValidCodigoLectura() {
  // Arrange
  String codigo = "LECTURA";
  given(repository.findByCodigo(codigo))
    .willReturn(Optional.of(crearNivelAcceso("LECTURA", true)));
  
  // Act & Assert
  assertDoesNotThrow(() -> validator.validarCodigoExiste(codigo));
}

@Test
void shouldReturnNivelWhenCodigoIsValid() {
  // Arrange
  NivelAcceso nivel = crearNivelAcceso("LECTURA", true);
  given(repository.findByCodigo("LECTURA"))
    .willReturn(Optional.of(nivel));
  
  // Act
  NivelAcceso result = validator.obtenerOValidar("LECTURA");
  
  // Assert
  assertThat(result).isNotNull();
  assertThat(result.getCodigo()).isEqualTo("LECTURA");
}
```

#### Validation Errors
```java
@Test
void shouldThrowNivelAccesoNotFoundExceptionForInvalidCodigo() {
  // Arrange
  given(repository.findByCodigo("INVALIDO"))
    .willReturn(Optional.empty());
  
  // Act & Assert
  assertThrows(NivelAccesoNotFoundException.class, 
    () -> validator.validarCodigoExiste("INVALIDO"));
}

@Test
void shouldThrowExceptionWhenCodigoInactive() {
  // Arrange
  NivelAcceso inactivoNivel = crearNivelAcceso("LECTURA", false);
  given(repository.findByCodigo("LECTURA"))
    .willReturn(Optional.of(inactivoNivel));
  
  // Act & Assert
  assertThrows(NivelAccesoInactivoException.class,
    () -> validator.validarCodigoActivo("LECTURA"));
}
```

#### Edge Cases
```java
@Test
void shouldThrowExceptionForNullCodigo() {
  assertThrows(IllegalArgumentException.class,
    () -> validator.validarCodigoExiste(null));
}

@Test
void shouldThrowExceptionForEmptyAcciones() {
  assertThrows(AccionesInvalidasException.class,
    () -> validator.validarAccionesValidas(Collections.emptyList()));
}
```

**Implementation Notes**:
- Usar Mockito: `@Mock` para repository, `@InjectMocks` para validator
- `@ExtendWith(MockitoExtension.class)` para JUnit 5
- Helper method: `crearNivelAcceso(codigo, activo)` para factories
- Secciones: Arrange, Act, Assert (AAA pattern)

---

### Step 16: Create Unit Tests - Repository

**File**: `backend/document-core/src/test/java/com/docflow/infrastructure/adapters/persistence/NivelAccesoRepositoryTest.java`

**Action**: Testear implementación de repositorio (integration test con BD real o embedded)

**Test Cases**:

#### Successful Cases
```java
@DataJpaTest
class NivelAccesoRepositoryTest {
  
  @Autowired
  private NivelAccesoJpaRepository jpaRepository;
  
  @Autowired
  private NivelAccesoRepositoryImpl repository;
  
  @Test
  void shouldFindAllThreeStandardNiveles() {
    // Arrange: BD ya seeded con 3 niveles
    
    // Act
    List<NivelAcceso> result = repository.findAll();
    
    // Assert
    assertThat(result)
      .hasSize(3)
      .extracting(NivelAcceso::getCodigo)
      .containsExactlyInAnyOrder("LECTURA", "ESCRITURA", "ADMINISTRACION");
  }
  
  @Test
  void shouldFindByCodigo() {
    // Act
    Optional<NivelAcceso> result = repository.findByCodigo("LECTURA");
    
    // Assert
    assertThat(result)
      .isPresent()
      .get()
      .extracting(NivelAcceso::getNombre)
      .isEqualTo("Lectura / Consulta");
  }
  
  @Test
  void shouldFindAllActivos() {
    // Act
    List<NivelAcceso> result = repository.findAllActivos();
    
    // Assert
    assertThat(result)
      .allMatch(NivelAcceso::isActivo)
      .hasSize(3);
  }
}
```

#### Validation Errors
```java
@Test
void shouldReturnEmptyOptionalForNonExistentCodigo() {
  // Act
  Optional<NivelAcceso> result = repository.findByCodigo("NO_EXISTE");
  
  // Assert
  assertThat(result).isEmpty();
}

@Test
void shouldReturnFalseForExistenceCheckOfNonExistent() {
  // Act
  boolean exists = repository.existsByCodigo("NO_EXISTE");
  
  // Assert
  assertThat(exists).isFalse();
}
```

#### Edge Cases
```java
@Test
void shouldHandleCodigoWithDifferentCases() {
  // Act: Búsqueda case-sensitive
  Optional<NivelAcceso> result = repository.findByCodigo("lectura");
  
  // Assert
  assertThat(result).isEmpty(); // "lectura" != "LECTURA"
}
```

**Test Setup** (usar `@Sql` para cargar seed):
```java
@DataJpaTest
@Sql("/db/seeds/S001__Seed_Niveles_Acceso.sql")
class NivelAccesoRepositoryTest { }
```

**Implementation Notes**:
- `@DataJpaTest`: Carga solo capas JPA, no controladores
- Usa H2 o TestContainers para BD embebida
- `@Sql` ejecuta seed antes de cada test (idempotencia importante)

---

### Step 17: Create Integration Tests - Controller

**File**: `backend/document-core/src/test/java/com/docflow/infrastructure/api/controllers/AclControllerTest.java`

**Action**: Testear endpoints HTTP GET /api/acl/niveles y GET /api/acl/niveles/{codigo}

**Test Cases**:

#### Successful Cases
```java
@SpringBootTest
@AutoConfigureMockMvc
class AclControllerTest {
  
  @Autowired
  private MockMvc mockMvc;
  
  @Autowired
  private ObjectMapper objectMapper;
  
  @MockBean
  private NivelAccesoService service;
  
  @Test
  void shouldReturnAllNivelesWithStatus200() throws Exception {
    // Arrange
    List<NivelAcceso> niveles = List.of(
      crearNivelAcceso("LECTURA"),
      crearNivelAcceso("ESCRITURA"),
      crearNivelAcceso("ADMINISTRACION")
    );
    given(service.obtenerTodos()).willReturn(niveles);
    
    // Act & Assert
    mockMvc.perform(get("/api/acl/niveles"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data").isArray())
      .andExpect(jsonPath("$.data", hasSize(3)))
      .andExpect(jsonPath("$.data[0].codigo").value("LECTURA"));
  }
  
  @Test
  void shouldReturnNivelByCodigoWithStatus200() throws Exception {
    // Arrange
    NivelAcceso nivel = crearNivelAcceso("LECTURA");
    given(service.obtenerPorCodigo("LECTURA")).willReturn(nivel);
    
    // Act & Assert
    mockMvc.perform(get("/api/acl/niveles/LECTURA"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.codigo").value("LECTURA"))
      .andExpect(jsonPath("$.data.activo").value(true));
  }
}
```

#### Error Cases
```java
@Test
void shouldReturnStatus404WhenCodigoNotFound() throws Exception {
  // Arrange
  given(service.obtenerPorCodigo("NO_EXISTE"))
    .willThrow(new NivelAccesoNotFoundException("NO_EXISTE"));
  
  // Act & Assert
  mockMvc.perform(get("/api/acl/niveles/NO_EXISTE"))
    .andExpect(status().isNotFound());
}

@Test
void shouldReturnStatus400WhenCodigoInactive() throws Exception {
  // Arrange
  given(service.obtenerPorCodigo("LECTURA"))
    .willThrow(new NivelAccesoInactivoException("LECTURA"));
  
  // Act & Assert
  mockMvc.perform(get("/api/acl/niveles/LECTURA"))
    .andExpect(status().isBadRequest());
}
```

**Implementation Notes**:
- `@SpringBootTest`: Carga contexto completo de Spring
- `MockMvc`: Simula requests HTTP sin servidor real
- `@MockBean`: Mockear servicio
- `ObjectMapper`: Serialización/deserialización JSON (Jackson)

---

### Step 18: Create Service Tests

**File**: `backend/document-core/src/test/java/com/docflow/application/services/NivelAccesoServiceTest.java`

**Action**: Testear lógica de servicio

**Test Cases**:

```java
@ExtendWith(MockitoExtension.class)
class NivelAccesoServiceTest {
  
  @Mock
  private INivelAccesoRepository repository;
  
  @Mock
  private NivelAccesoValidator validator;
  
  @InjectMocks
  private NivelAccesoService service;
  
  @Test
  void shouldCallRepositoryFindAllAndReturnList() {
    // Arrange
    List<NivelAcceso> expectedNiveles = List.of(
      crearNivelAcceso("LECTURA"),
      crearNivelAcceso("ESCRITURA")
    );
    given(repository.findAll()).willReturn(expectedNiveles);
    
    // Act
    List<NivelAcceso> result = service.obtenerTodos();
    
    // Assert
    assertThat(result).isEqualTo(expectedNiveles);
    verify(repository).findAll();
  }
  
  @Test
  void shouldValidateAndReturnNivelByCodigo() {
    // Arrange
    String codigo = "LECTURA";
    NivelAcceso nivel = crearNivelAcceso(codigo);
    given(repository.findByCodigo(codigo)).willReturn(Optional.of(nivel));
    
    // Act
    NivelAcceso result = service.obtenerPorCodigo(codigo);
    
    // Assert
    assertThat(result).isEqualTo(nivel);
    verify(validator).validarCodigoExiste(codigo);
  }
}
```

---

### Step 19: Integration Tests with Seed Data

**File**: `backend/document-core/src/test/java/com/docflow/infrastructure/api/integration/AclIntegrationTest.java`

**Action**: Test end-to-end con BD real y datos seeded

**Test Cases**:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Sql("/db/seeds/S001__Seed_Niveles_Acceso.sql")
class AclIntegrationTest {
  
  @Autowired
  private MockMvc mockMvc;
  
  @Test
  void shouldReturnThreeNivelesOnInitialization() throws Exception {
    mockMvc.perform(get("/api/acl/niveles"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.meta.total").value(3));
  }
  
  @Test
  void shouldFindLecturaByCodigoAfterSeed() throws Exception {
    mockMvc.perform(get("/api/acl/niveles/LECTURA"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.acciones_permitidas", hasItems("ver", "listar", "descargar")));
  }
}
```

---

### Step 20: Update Pagination/Caching Configuration

**File**: `backend/document-core/src/main/java/com/docflow/infrastructure/config/CacheConfig.java`

**Action**: Configurar caché para catálogo estático (niveles de acceso)

**Implementation Steps**:

1. **Enable Caching**:
   ```java
   @Configuration
   @EnableCaching
   public class CacheConfig {
     
     @Bean
     public CacheManager cacheManager() {
       return new ConcurrentMapCacheManager("nivelesAcceso");
     }
   }
   ```

2. **Add @Cacheable to Service**:
   ```java
   @Cacheable(value = "nivelesAcceso", key = "'todos'")
   public List<NivelAcceso> obtenerTodos() { }
   ```

**Notes**: En futuras iteraciones, no en esta historia (out of scope)

---

### Step 21: Update Technical Documentation

**File**: `ai-specs/specs/data-model.md` y `ai-specs/specs/api-spec.yml`

**Action**: Revisar y actualizar documentación técnica según cambios implementados

**Implementation Steps**:

1. **Review Changes**:
   - Tabla `nivel_acceso` creada en BD
   - Endpoints `GET /acl/niveles` y `GET /acl/niveles/{codigo}` implementados
   - Estructura de datos: NivelAcceso con campos id, codigo, nombre, descripcion, accionePermitidas, orden, activo

2. **Update data-model.md**:
   - Agregar sección "Nivel de Acceso" si no existe
   - Documentar estructura: campos, tipos, restricciones
   - Documentar relaciones (si aplica)
   - Descripción de acciones permitidas

3. **Update api-spec.yml**:
   - Agregar paths: `/acl/niveles`, `/acl/niveles/{codigo}`
   - Documentar parámetros, respuestas 200/404, ejemplos
   - Usar Swagger format para documentación automática

4. **Verify Documentation**:
   - Confirmación de exactitud
   - Formato consistente con documentos existentes
   - Escritura en inglés (per `documentation-standards.md`)

5. **Report Updates**:
   - Documentar en commit message qué archivos fueron actualizados
   - Ejemplo: "docs: update data-model.md with NivelAcceso entity and api-spec.yml with ACL endpoints"

**References**:
- `ai-specs/specs/documentation-standards.mdc` para proceso
- `ai-specs/specs/data-model.md` para estructura existente
- `ai-specs/specs/api-spec.yml` para formato OpenAPI

---

## Implementation Order

2. **Step 1**: Create Database Migration Script (`V001__Create_Nivel_Acceso_Table.sql`)
3. **Step 2**: Create Database Seed Script (`S001__Seed_Niveles_Acceso.sql`)
4. **Step 3**: Create Domain Entity and Enum (`NivelAcceso.java`, `CodigoNivelAcceso.java`)
5. **Step 4**: Create Repository Interface (`INivelAccesoRepository.java`)
6. **Step 5**: Create Spring Data JPA Repository (`NivelAccesoJpaRepository.java`)
7. **Step 6**: Create JPA Entity (`NivelAccesoEntity.java`)
8. **Step 7**: Create AttributeConverter for JSONB (`JsonbListConverter.java`)
9. **Step 8**: Create Repository Implementation (`NivelAccesoRepositoryImpl.java`)
10. **Step 9**: Create MapStruct Mapper (`NivelAccesoMapper.java`)
11. **Step 10**: Create Service Validator (`NivelAccesoValidator.java`)
12. **Step 11**: Create Service (`NivelAccesoService.java`)
13. **Step 12**: Create Controller (`AclController.java`)
14. **Step 13**: Create DTOs (`NivelAccesoDTO.java`, `ApiResponse.java`)
15. **Step 14**: Create GlobalExceptionHandler
16. **Step 15**: Create Unit Tests - Validator (`NivelAccesoValidatorTest.java`)
17. **Step 16**: Create Unit Tests - Repository (`NivelAccesoRepositoryTest.java`)
18. **Step 17**: Create Integration Tests - Controller (`AclControllerTest.java`)
19. **Step 18**: Create Service Tests (`NivelAccesoServiceTest.java`)
20. **Step 19**: Integration Tests with Seed Data (`AclIntegrationTest.java`)
21. **Step 20**: Update Caching Configuration (opcional, futuro)
22. **Step 21**: Update Technical Documentation (`data-model.md`, `api-spec.yml`)

---

## Testing Checklist

### Unit Tests
- [ ] NivelAccesoValidator: Successful cases (3 niveles válidos)
- [ ] NivelAccesoValidator: Validation errors (código inválido, inactivo)
- [ ] NivelAccesoValidator: Edge cases (null, empty, case sensitivity)
- [ ] NivelAccesoService: obtenerTodos, obtenerPorCodigo, obtenerActivos
- [ ] NivelAccesoRepository: findAll, findByCodigo, findAllActivos

### Integration Tests
- [ ] AclController GET /api/acl/niveles → 200 OK con 3 niveles
- [ ] AclController GET /api/acl/niveles/LECTURA → 200 OK con detalles
- [ ] AclController GET /api/acl/niveles/NO_EXISTE → 404 Not Found
- [ ] AclController GET /api/acl/niveles/LECTURA (si inactivo) → 400 Bad Request
- [ ] GlobalExceptionHandler traduce excepciones a HTTP statuses

### Database Tests
- [ ] Migración SQL crea tabla `nivel_acceso` correctamente
- [ ] Seed SQL inserta 3 niveles sin duplicación (idempotencia)
- [ ] Índices creados en `codigo` y `activo`
- [ ] Constraints validados (UNIQUE en codigo, NOT NULL)

### API Contract
- [ ] Respuesta 200 OK incluye estructura: `{ "data": [...], "meta": {...} }`
- [ ] Cada nivel incluye campos: id, codigo, nombre, descripcion, accionePermitidas, orden, activo
- [ ] accionePermitidas es array de strings: ["ver", "listar", "descargar", ...]
- [ ] Orden de niveles consistente: LECTURA (1), ESCRITURA (2), ADMINISTRACION (3)

### Code Quality
- [ ] Coverage ≥ 90% en clases de negocio (NivelAccesoValidator, NivelAccesoService)
- [ ] Sin warnings de compilación (Java compiler)
- [ ] ESLint/PMD/SpotBugs no reportan issues
- [ ] Código sigue estándares del proyecto (conventions)
- [ ] Comentarios Javadoc en métodos públicos
- [ ] Excepciones personalizadas documentadas

### Documentation
- [ ] Swagger/OpenAPI documentado en controlador
- [ ] data-model.md actualizado con entidad NivelAcceso
- [ ] api-spec.yml actualizado con endpoints
- [ ] Commit messages en español descriptivos

---

## Notes & Recommendations

1. **Dependency Injection**: Usar constructor injection en todos los servicios y controladores (evita estado mutable).

2. **Exception Hierarchy**: Crear `NivelAccesoException` base, extendida por `NivelAccesoNotFoundException`, `NivelAccesoInactivoException`.

3. **Logging**: Incluir logs en niveles INFO (operaciones) y ERROR (fallos) sin exponer datos sensibles.

4. **JSONB Handling**: Si PostgreSQL no está disponible, usar `String` con serialización manual; JSONB es recomendado para queries futuras.

5. **TDD Workflow**:
   - Escribir test RED (falla)
   - Implementar mínimo código GREEN (pasa)
   - REFACTOR (mejorar sin cambiar comportamiento)

6. **Future Enhancements**:
   - Caché en memoria para nivel estático
   - Validación de acciones contra lista blanca
   - Auditoria de cambios en niveles
   - Endpoint para crear/modificar niveles (admin solo)

---

**Estado**: Listo para implementación  
**Fecha**: 27 de enero de 2026  
**Responsable**: Equipo Backend  
**Estimación**: 3-4 sprints (dependiendo de paralelización con frontend)
