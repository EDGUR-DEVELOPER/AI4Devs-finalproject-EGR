# Plan de Implementación Backend: US-DOC-001 — Subir documento (API) crea documento + versión 1

## 1. Descripción General

Este ticket implementa la funcionalidad central de carga de documentos para DocFlow. Crea una entidad `Document` con su primera versión (`DocumentVersion`) y persiste el archivo binario en almacenamiento. La implementación sigue **Domain-Driven Design (DDD)** con Arquitectura Hexagonal (Puertos y Adaptadores), asegurando seguridad mediante validación de permisos, inmutabilidad de pistas de auditoría e integridad transaccional con soporte de reversión.

**Objetivos clave:**
- Implementar endpoint `POST /api/v1/folders/{folderId}/documents`
- Crear modelos de dominio `Document` y `DocumentVersion`
- Persistir archivos binarios mediante `StorageService` (sistema de archivos local por defecto)
- Validar permisos de escritura en carpeta (ESCRITURA o ADMINISTRACION)
- Emitir evento de auditoría `DOCUMENTO_CREADO`
- Asegurar reversión transaccional en fallos de almacenamiento

---

## 2. Contexto de Arquitectura

### Capas Involucradas

**Capa de Dominio** (`src/domain/`)
- Entidad `Document` (Java puro, sin anotaciones JPA)
- Entidad `DocumentVersion` con versionado inmutable
- Interfaz `IDocumentRepository` (contrato de repositorio)
- Servicios de dominio para lógica de negocio (ej. `DocumentDomainService`)
- Objetos de valor para validación (ej. `DocumentMetadata`)

**Capa de Aplicación** (`src/application/`)
- `DocumentService` orquestando operaciones comerciales
- `DocumentValidator` para reglas de validación
- Interfaz `StorageService` (puerto) — abstracción para almacenamiento de archivos
- DTOs para solicitud/respuesta (`CreateDocumentRequest`, `DocumentResponse`)
- Mapeadores (`DocumentMapper`) para conversiones DTO

**Capa de Infraestructura** (`src/infrastructure/`)
- Entidades JPA: `DocumentEntity`, `DocumentVersionEntity`
- Repositorios Spring Data JPA: `DocumentJpaRepository`, `DocumentVersionJpaRepository`
- `DocumentRepositoryImpl` implementando `IDocumentRepository`
- `LocalStorageService` implementando `StorageService`
- Migraciones de base de datos (scripts SQL Flyway/Liquibase)

**Capa de Presentación** (`src/presentation/`)
- `DocumentController` con endpoint `POST /api/v1/folders/{folderId}/documents`
- Manejador de excepciones global para respuestas de error

### Estructura de Proyecto Ejemplo

```
backend/document-core/
├── src/main/java/com/docflow/documentcore/
│   ├── config/
│   │   ├── DocFlowConfig.java
│   │   └── StorageConfig.java
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Document.java
│   │   │   ├── DocumentVersion.java
│   │   │   └── DocumentMetadata.java
│   │   ├── repository/
│   │   │   └── IDocumentRepository.java
│   │   └── service/
│   │       └── DocumentDomainService.java
│   ├── application/
│   │   ├── service/
│   │   │   └── DocumentService.java
│   │   ├── validator/
│   │   │   └── DocumentValidator.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   │   └── CreateDocumentRequest.java
│   │   │   └── response/
│   │   │       └── DocumentResponse.java
│   │   ├── mapper/
│   │   │   └── DocumentMapper.java
│   │   └── storage/
│   │       └── StorageService.java (interfaz/puerto)
│   ├── infrastructure/
│   │   ├── persistence/
│   │   │   ├── entity/
│   │   │   │   ├── DocumentEntity.java
│   │   │   │   └── DocumentVersionEntity.java
│   │   │   ├── repository/
│   │   │   │   ├── DocumentJpaRepository.java
│   │   │   │   ├── DocumentVersionJpaRepository.java
│   │   │   │   └── DocumentRepositoryImpl.java
│   │   ├── storage/
│   │   │   └── LocalStorageService.java
│   │   └── adapter/
│   │       └── PermissionCheckAdapter.java (verificación ACL externa)
│   └── presentation/
│       ├── controller/
│       │   └── DocumentController.java
│       └── exception/
│           └── DocumentExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       ├── V1__create_document_tables.sql
│       └── V2__create_document_version_tables.sql
└── src/test/java/com/docflow/documentcore/
    ├── application/
    │   ├── service/
    │   │   └── DocumentServiceTest.java
    │   └── validator/
    │       └── DocumentValidatorTest.java
    └── infrastructure/
        ├── persistence/
        │   └── DocumentRepositoryImplTest.java
        └── storage/
            └── LocalStorageServiceTest.java
```

---

## 3. Pasos de Implementación

### Paso 1: Crear Migración de Base de Datos — Tabla Documento

**Archivo**: `backend/document-core/src/main/resources/db/migration/V1__create_document_tables.sql`

**Acción**: Crear la tabla `documento` con todos los campos, índices y restricciones requeridos.

**Pasos de Implementación**:
1. Crear script de migración versionado `V1__create_document_tables.sql` en el directorio de migración
2. Definir la tabla `documento` con columnas:
   - `id` (BIGSERIAL PRIMARY KEY)
   - `organizacion_id` (BIGINT NOT NULL, FOREIGN KEY a `organizacion`)
   - `carpeta_id` (BIGINT NULLABLE, FOREIGN KEY a `carpeta`)
   - `nombre` (VARCHAR(255) NO NULL)
   - `extension` (VARCHAR(50) NULLABLE)
   - `tipo_contenido` (VARCHAR(100) NO NULL, ej. "application/pdf")
   - `tamanio_bytes` (BIGINT NO NULL)
   - `version_actual_id` (BIGINT NULLABLE, FOREIGN KEY a `documento_version`)
   - `numero_versiones` (INTEGER DEFAULT 1)
   - `bloqueado` (BOOLEAN DEFAULT FALSE)
   - `bloqueado_por` (BIGINT NULLABLE)
   - `bloqueado_en` (TIMESTAMP NULLABLE)
   - `etiquetas` (TEXT[] NULLABLE)
   - `metadatos` (JSONB NULLABLE)
   - `creado_por` (BIGINT NO NULL, FOREIGN KEY a `usuario`)
   - `fecha_creacion` (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - `fecha_actualizacion` (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - `fecha_eliminacion` (TIMESTAMP NULLABLE)

3. Crear índices:
   - Clave primaria: `id`
   - `idx_documento_organizacion_id` en `organizacion_id`
   - `idx_documento_carpeta_id` en `carpeta_id`
   - `idx_documento_creado_por` en `creado_por`
   - `uk_documento_nombre_carpeta` (único en `nombre, carpeta_id, organizacion_id` donde `fecha_eliminacion IS NULL`)
   - `idx_documento_version_actual_id` en `version_actual_id`

4. Agregar restricciones:
   - NOT NULL: `organizacion_id`, `nombre`, `tipo_contenido`, `tamanio_bytes`, `creado_por`
   - CHECK: `tamanio_bytes > 0`

5. Agregar disparador de auditoría (opcional pero recomendado):
   - Disparador para actualizar `fecha_actualizacion` en UPDATE

**Dependencias**: Ninguna (pero requiere que existan las tablas `organizacion`, `carpeta`, `usuario`)

**Notas de Implementación**:
- Usar BIGSERIAL para IDs auto-incrementados (alineado con estándares del proyecto)
- JSONB para soporte nativo JSON de PostgreSQL (mejor que TEXT)
- TEXT[] para soporte de matriz nativa de Postgres para etiquetas
- La restricción única incluye `fecha_eliminacion IS NULL` para soporte de eliminación lógica
- Las claves foráneas se agregarán en migración separada para evitar dependencias circulares

---

### Paso 2: Crear Migración de Base de Datos — Tabla DocumentVersion

**Archivo**: `backend/document-core/src/main/resources/db/migration/V2__create_document_version_tables.sql`

**Acción**: Crear la tabla `documento_version` con soporte de versionado y pista de auditoría.

**Pasos de Implementación**:
1. Crear script de migración versionado `V2__create_document_version_tables.sql`
2. Definir la tabla `documento_version` con columnas:
   - `id` (BIGSERIAL PRIMARY KEY)
   - `documento_id` (BIGINT NO NULL, FOREIGN KEY a `documento`)
   - `numero_secuencial` (INTEGER NO NULL)
   - `tamanio_bytes` (BIGINT NO NULL)
   - `ruta_almacenamiento` (VARCHAR(500) NO NULL)
   - `hash_contenido` (VARCHAR(64) NO NULL, SHA256)
   - `comentario_cambio` (VARCHAR(500) NULLABLE)
   - `creado_por` (BIGINT NO NULL, FOREIGN KEY a `usuario`)
   - `fecha_creacion` (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
   - `descargas` (INTEGER DEFAULT 0)
   - `ultima_descarga_en` (TIMESTAMP NULLABLE)

3. Crear índices:
   - Clave primaria: `id`
   - `idx_documento_version_documento_id` en `documento_id`
   - `uk_documento_version_documento_numero` (único en `documento_id, numero_secuencial`)
   - `idx_documento_version_creado_por` en `creado_por`
   - `idx_documento_version_ruta_almacenamiento` en `ruta_almacenamiento` (para verificaciones de deduplicación)

4. Agregar restricciones:
   - NOT NULL: `documento_id`, `numero_secuencial`, `tamanio_bytes`, `ruta_almacenamiento`, `hash_contenido`, `creado_por`
   - FOREIGN KEY: `documento_id` → `documento(id)` ON DELETE CASCADE
   - FOREIGN KEY: `creado_por` → `usuario(id)`
   - CHECK: `numero_secuencial > 0`, `tamanio_bytes > 0`

5. Agregar restricción de progresión de versión:
   - Asegurar números secuenciales (1, 2, 3...) por documento (mediante lógica de aplicación, no BD)

**Dependencias**: Requiere tabla `documento` del Paso 1

**Notas de Implementación**:
- Las versiones son inmutables (sin actualizaciones, solo inserciones)
- `numero_secuencial` comienza en 1 e incrementa linealmente
- Hash SHA256 almacenado como cadena hexadecimal (64 caracteres)
- `ruta_almacenamiento` es ruta completa en almacenamiento (ej. `/{organizacion_id}/{carpeta_id}/{documento_id}/1/`)
- Eliminación en cascada: si el documento se elimina permanentemente, las versiones también se eliminan
- La restricción única asegura sin números de versión duplicados por documento

---

### Paso 3: Crear Modelo de Dominio — Entidad Document

**Archivo**: `backend/document-core/src/domain/model/Document.java`

**Acción**: Crear entidad de dominio pura para `Document` (sin anotaciones JPA, solo lógica de negocio).

**Firma de Función**:
```java
public class Document {
    private Long id;
    private Long organizationId;
    private Long folderId;
    private String name;
    private String extension;
    private String contentType;
    private Long fileSize;
    private Long currentVersionId;
    private Integer versionCount;
    private Boolean isLocked;
    private Long lockedBy;
    private LocalDateTime lockedAt;
    private List<String> tags;
    private Map<String, Object> metadata;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
```

**Pasos de Implementación**:
1. Crear clase `Document` en `src/domain/model/`
2. Agregar campos privados finales (inmutables después de creación)
3. Crear método de fábrica `Document.create(...)`:
   - Parámetros: `organizationId`, `folderId`, `name`, `extension`, `contentType`, `fileSize`, `createdBy`
   - Valida entradas (nombre no vacío, extensión válida, etc.)
   - Devuelve nueva instancia `Document` con ID auto-generado (establecido por repositorio después)
4. Crear patrón de constructor o constructor con todos los campos
5. Agregar métodos de dominio:
   - `updateCurrentVersion(Long versionId)` — actualiza `currentVersionId` e incrementa `versionCount`
   - `getVersionNumber()` — devuelve `versionCount` actual
   - `isEditable()` — verifica si no está bloqueado
   - `validate()` — asegura invariantes (ej. `currentVersionId` establecido, `versionCount > 0`)
6. Agregar igualdad/hashCode basado en `id`
7. Usar registros (Java 16+) o Lombok `@Value` para inmutabilidad (preferir registros)

**Dependencias**: Ninguna (lógica de dominio pura)

**Notas de Implementación**:
- La entidad de dominio NO DEBE tener anotaciones JPA - las anotaciones van en la entidad JPA
- Usar `LocalDateTime` para timestamps (sin problemas de zona horaria localmente)
- `fileSize` se refiere a versión actual; almacenar por conveniencia solamente
- `metadata` es mapa libre para extensibilidad (almacenado como JSONB en BD)
- No necesitan getters/setters; registros o Lombok @Value lo maneja
- Mantener lógica de validación en métodos de fábrica (create) y método validate() explícito

---

### Paso 4: Crear Modelo de Dominio — Entidad DocumentVersion

**Archivo**: `backend/document-core/src/domain/model/DocumentVersion.java`

**Acción**: Crear entidad de dominio pura para versionado inmutable (sin anotaciones JPA).

**Firma de Función**:
```java
public record DocumentVersion(
    Long id,
    Long documentId,
    Integer versionNumber,
    Long fileSize,
    String storagePath,
    String contentHash,
    String changeComment,
    Long createdBy,
    LocalDateTime createdAt,
    Integer downloadCount,
    LocalDateTime lastDownloadedAt
) {}
```

**Pasos de Implementación**:
1. Crear registro `DocumentVersion` con campos inmutables (usar registros Java 16+)
2. Agregar método de fábrica estática `DocumentVersion.create(...)`:
   - Parámetros: `documentId`, `versionNumber`, `fileSize`, `storagePath`, `contentHash`, `changeComment`, `createdBy`
   - Valida: `versionNumber > 0`, `fileSize > 0`, `contentHash` es SHA256 válido, etc.
   - Devuelve nueva instancia con `downloadCount = 0`, `lastDownloadedAt = null`, `createdAt = ahora()`
3. Agregar métodos de dominio:
   - `recordDownload()` — devuelve nueva instancia con `downloadCount` incrementado e `lastDownloadedAt` actualizado (nueva instancia para inmutabilidad)
   - `incrementVersion(nextVersionNumber)` — valida numeración secuencial

**Dependencias**: Ninguna (lógica de dominio pura)

**Notas de Implementación**:
- Usar registros Java para inmutabilidad (sin setters necesarios)
- Los números de versión deben ser secuenciales (forzado por capa de aplicación)
- El hash de contenido es inmutable (usado para deduplicación e integridad)
- El seguimiento de descargas es informativo (sin impacto comercial en este ticket)
- Usar `record` para construcción simple de datos con equals/hashCode/toString automático

---

### Paso 5: Crear Interfaz de Repositorio de Dominio

**Archivo**: `backend/document-core/src/domain/repository/IDocumentRepository.java`

**Acción**: Definir contrato de repositorio para persistencia de dominio (puerto/interfaz).

**Firma de Función**:
```java
public interface IDocumentRepository {
    Document save(Document document); // Crea o actualiza
    Document findById(Long id);
    Optional<Document> findByIdOptional(Long id);
    void delete(Long id); // Eliminación lógica
    Collection<Document> findByFolderId(Long folderId);
    Collection<Document> findByOrganizationId(Long organizationId);
}
```

**Pasos de Implementación**:
1. Crear interfaz `IDocumentRepository` en `src/domain/repository/`
2. Definir métodos:
   - `Document save(Document document)` — persiste o actualiza; devuelve instancia persistida con ID establecido
   - `Optional<Document> findById(Long id)` — recupera por ID; devuelve Optional
   - `Optional<Document> findByIdAndOrganizationId(Long id, Long orgId)` — recuperación segura con verificación org
   - `Collection<Document> findByFolderId(Long folderId, Long orgId)` — lista todos los documentos en carpeta
   - `void delete(Long id)` — eliminación lógica (establece `deletedAt`)
   - `boolean existsByNameAndFolderId(String name, Long folderId, Long orgId)` — verifica duplicados

3. Agregar métodos por defecto (opcional):
   - `void deleteHard(Long id)` — eliminación real de base de datos (para limpieza/archivo)

**Dependencias**: Requiere entidad de dominio `Document`

**Notas de Implementación**:
- La interfaz vive en capa de dominio (sin conocimiento de Spring/JPA)
- Los métodos usan entidades de dominio, no DTOs o entidades JPA
- Los tipos de retorno deben ser `Optional<T>` para manejar nulabilidad
- Todos los queries deben ser conscientes de la organización (aislamiento de tenant)
- El repositorio es un PUERTO en Arquitectura Hexagonal — la implementación está en infraestructura

---

### Paso 6: Crear Entidad JPA — DocumentEntity

**Archivo**: `backend/document-core/src/infrastructure/persistence/entity/DocumentEntity.java`

**Acción**: Crear entidad JPA de Spring Data para persistencia de base de datos.

**Pasos de Implementación**:
1. Crear clase `DocumentEntity` con anotaciones JPA
2. Agregar campos (igual que modelo de dominio):
   - @Id @GeneratedValue para `id`
   - Anotaciones @Column para restricciones (longitud, nullable, único, etc.)
   - Relacionamientos @ManyToOne: `organizacion`, `carpeta`, `usuario`
3. Agregar anotaciones Lombok:
   - `@Entity`, `@Table(name = "documento")`
   - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
   - `@Builder` para opcional
4. Agregar callbacks de ciclo de vida JPA:
   - `@PrePersist` para establecer `fecha_creacion` y `fecha_actualizacion`
   - `@PreUpdate` para actualizar `fecha_actualizacion`
5. Agregar convertidor para metadatos JSONB:
   - Usar `@Convert(converter = MetadataConverter.class)` o soporte JSON nativo
6. Agregar anotaciones de índice:
   - `@Index(name = "idx_documento_organizacion", columnList = "organizacion_id")`
   - `@Index(name = "idx_documento_carpeta", columnList = "carpeta_id")`

**Dependencias**: JPA starter, Lombok, controlador de base de datos

**Notas de Implementación**:
- Usar `@Column(columnDefinition = "TEXT[]")` para matriz de etiquetas
- Usar `@Column(columnDefinition = "JSONB")` para metadatos (solo PostgreSQL)
- Agregar restricción única: `@UniqueConstraint(name = "uk_documento_nombre_carpeta", columnNames = {"nombre", "carpeta_id", "organizacion_id"})`
- Separar de entidad de dominio para permitir evolución independiente
- Incluir filtro de tenant/listener para multi-tenancy (ver patrones existentes)

---

### Paso 7: Crear Entidad JPA — DocumentVersionEntity

**Archivo**: `backend/document-core/src/infrastructure/persistence/entity/DocumentVersionEntity.java`

**Acción**: Crear entidad JPA de Spring Data para persistencia de versión de documento.

**Pasos de Implementación**:
1. Crear clase `DocumentVersionEntity` con anotaciones JPA
2. Agregar campos:
   - `@Id @GeneratedValue` para `id`
   - Relacionamiento `@ManyToOne`: `documento` (carga EAGER)
   - Anotaciones @Column para todos los campos
3. Agregar anotaciones Lombok:
   - `@Entity`, `@Table(name = "documento_version")`
   - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
4. Agregar callbacks de ciclo de vida JPA:
   - `@PrePersist` para establecer `fecha_creacion`
5. Agregar anotaciones de índice:
   - `@Index(name = "idx_documento_version_documento", columnList = "documento_id")`
   - `@UniqueConstraint(name = "uk_...", columnNames = {"documento_id", "numero_secuencial"})`

**Dependencias**: JPA starter, Lombok

**Notas de Implementación**:
- Las versiones son inmutables en lógica de aplicación (sin setters, solo constructor)
- Usar `@Column(insertable = false, updatable = false)` para timestamps
- El relacionamiento ManyToOne NO debe tener eliminación en cascada de versión a documento (la eliminación se mantiene en DocumentEntity)
- Agregar listener de auditoría si es necesario (pero típicamente inmutable = sin auditoría necesaria)

---

### Paso 8: Crear Interfaces de Repositorio JPA de Spring Data

**Archivo**: `backend/document-core/src/infrastructure/persistence/repository/DocumentJpaRepository.java` y `DocumentVersionJpaRepository.java`

**Acción**: Crear interfaces de repositorio JPA de Spring Data para operaciones de persistencia de bajo nivel.

**Pasos de Implementación**:
1. Crear `DocumentJpaRepository extends JpaRepository<DocumentEntity, Long>`:
   ```java
   @Repository
   public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, Long> {
       @Query("SELECT d FROM DocumentEntity d WHERE d.id = :id AND d.organizacionId = :orgId")
       Optional<DocumentEntity> findByIdAndOrganizacionId(@Param("id") Long id, @Param("orgId") Long orgId);
       
       @Query("SELECT d FROM DocumentEntity d WHERE d.carpetaId = :carpetaId AND d.organizacionId = :orgId AND d.fechaEliminacion IS NULL")
       List<DocumentEntity> findByFolderId(@Param("carpetaId") Long carpetaId, @Param("orgId") Long orgId);
       
       boolean existsByNombreAndCarpetaIdAndOrganizacionIdAndFechaEliminacionIsNull(String nombre, Long carpetaId, Long orgId);
   }
   ```

2. Crear `DocumentVersionJpaRepository extends JpaRepository<DocumentVersionEntity, Long>`:
   ```java
   @Repository
   public interface DocumentVersionJpaRepository extends JpaRepository<DocumentVersionEntity, Long> {
       @Query("SELECT dv FROM DocumentVersionEntity dv WHERE dv.documentoId = :docId AND dv.numeroSecuencial = :versionNum")
       Optional<DocumentVersionEntity> findByDocumentoIdAndVersionNumber(@Param("docId") Long docId, @Param("versionNum") Integer versionNum);
       
       @Query("SELECT dv FROM DocumentVersionEntity dv WHERE dv.documentoId = :docId ORDER BY dv.numeroSecuencial DESC")
       List<DocumentVersionEntity> findByDocumentoIdOrderedByVersionDesc(@Param("docId") Long docId);
   }
   ```

3. Agregar métodos de query:
   - Usar `@Query` para queries JPQL complejos
   - Usar convenciones de nomenclatura de métodos para queries simples
   - Habilitar paginación para métodos de lista si es necesario

**Dependencias**: Spring Data JPA

**Notas de Implementación**:
- Estas son interfaces de Spring Data, NO repositorios de dominio
- Usadas internamente por `DocumentRepositoryImpl`
- Todos los queries deben filtrar por `organizacionId` para aislamiento de tenant
- Usar anotaciones `@Query` para claridad y evitar explosión de longitud de nombre de método
- Los queries deben filtrar por `deletedAt IS NULL` para soporte de eliminación lógica

---

### Paso 9: Crear Implementación de Repositorio de Dominio

**Archivo**: `backend/document-core/src/infrastructure/persistence/repository/DocumentRepositoryImpl.java`

**Acción**: Implementar interfaz `IDocumentRepository` usando repositorios JPA de Spring Data.

**Firma de Función**:
```java
@Repository
public class DocumentRepositoryImpl implements IDocumentRepository {
    private final DocumentJpaRepository jpaRepository;
    private final DocumentVersionJpaRepository versionJpaRepository;
    private final DocumentMapper mapper;
    
    // Métodos de implementación...
}
```

**Pasos de Implementación**:
1. Crear clase `DocumentRepositoryImpl` implementando `IDocumentRepository`
2. Inyectar dependencias:
   - `DocumentJpaRepository` (Spring Data)
   - `DocumentVersionJpaRepository`
   - `DocumentMapper` (para conversión entidad ↔ dominio)
3. Implementar métodos:
   - `save(Document document)`:
     - Mapear dominio `Document` a `DocumentEntity`
     - Llamar `jpaRepository.save()` y `versionJpaRepository.save()` para versión
     - Mapear resultado nuevamente a entidad de dominio
     - Devolver entidad persistida con ID
   - `findById(Long id)`:
     - Llamar `jpaRepository.findByIdAndOrganizacionId()` con contexto de organización actual
     - Mapear a entidad de dominio si se encuentra, devolver `Optional`
   - `findByFolderId(Long folderId, Long orgId)`:
     - Llamar `jpaRepository.findByFolderId()`
     - Mapear todos los resultados a entidades de dominio
     - Devolver como Lista
   - `delete(Long id)`:
     - Buscar entidad y establecer `fechaEliminacion = ahora()`
     - Llamar `jpaRepository.save(entity)` (eliminación lógica)
   - `existsByNameAndFolderId(...)`:
     - Delegar a método de Spring Data

4. Agregar manejo de contexto de tenant:
   - Usar `TenantContextHolder.getCurrentTenantId()` para obtener ID de organización
   - Pasar ID org a todos los queries

**Dependencias**: `DocumentJpaRepository`, `DocumentVersionJpaRepository`, `DocumentMapper`

**Notas de Implementación**:
- Patrón de adaptador de repositorio: traduce entre dominio e infraestructura
- Todas las operaciones de dominio pasan a través de este repositorio
- El contexto de tenant debe inyectarse automáticamente (verificar patrones existentes)
- Usar `@Transactional` si es necesario para operaciones multi-paso
- Siempre incluir ID de organización en filtros (seguridad + multi-tenancy)

---

### Paso 10: Crear Interfaz de Servicio de Almacenamiento (Puerto)

**Archivo**: `backend/document-core/src/application/storage/StorageService.java`

**Acción**: Definir operaciones abstractas de almacenamiento (puerto de Arquitectura Hexagonal).

**Firma de Función**:
```java
public interface StorageService {
    String upload(String organizationId, String folderId, String documentId, 
                  String versionNumber, InputStream content, long contentLength) 
        throws StorageException;
    
    InputStream download(String storagePath) throws StorageException;
    
    void delete(String storagePath) throws StorageException;
    
    boolean exists(String storagePath) throws StorageException;
}
```

**Pasos de Implementación**:
1. Crear interfaz `StorageService` en `src/application/storage/`
2. Definir métodos:
   - `String upload(...)` — persiste archivo a almacenamiento; devuelve ruta de almacenamiento
     - Parámetros: `organizationId`, `folderId`, `documentId`, `versionNumber`, `content` (InputStream), `contentLength` (bytes)
     - Devuelve: ruta de almacenamiento (ej. `/{orgId}/{folderId}/{docId}/1/`)
     - Lanza: `StorageException` por cualquier error I/O o validación
   - `InputStream download(String storagePath)` — recupera archivo como stream
     - Devuelve: InputStream para descarga en stream
     - Lanza: `StorageException` si no se encuentra o error I/O
   - `void delete(String storagePath)` — elimina archivo del almacenamiento
   - `boolean exists(String storagePath)` — verifica si existe archivo

3. Crear excepción personalizada `StorageException extends RuntimeException`

**Dependencias**: Ninguna (solo Java I/O)

**Notas de Implementación**:
- La interfaz representa un PUERTO en Arquitectura Hexagonal
- Las implementaciones pueden ser sistema de archivos local, S3, Azure Blob, MinIO, etc.
- Usar `InputStream` para streaming eficiente en memoria (no arrays de bytes)
- Las excepciones deben ser comprobadas o extender RuntimeException (decidir basado en convención del proyecto)
- El formato de ruta debe ser estandarizado y documentado

---

### Paso 11: Crear FileValidator — Lógica de Validación

**Archivo**: `backend/document-core/src/application/validator/DocumentValidator.java`

**Acción**: Implementar reglas de validación de archivo y documento.

**Firma de Función**:
```java
@Component
public class DocumentValidator {
    public void validateFile(MultipartFile file, DocumentValidationConfig config) throws ValidationException;
    public void validateDocumentName(String name) throws ValidationException;
    public void validateFolderAccess(Long folderId, String accessLevel) throws FolderAccessException;
}
```

**Pasos de Implementación**:
1. Crear clase `DocumentValidator` en `src/application/validator/`
2. Inyectar `DocumentValidationConfig` desde propiedades
3. Agregar métodos de validación:
   - `validateFile(MultipartFile file, config)`:
     - Verificar archivo no vacío: `file.isEmpty() == false`
     - Verificar tamaño de archivo: `file.getSize() <= config.getMaxFileSize()`
     - Verificar tipo MIME: `file.getContentType()` en `config.getAllowedContentTypes()`
     - Verificar extensión de archivo: extraer de nombre, validar contra lista blanca
     - Lanzar `DocumentValidationException` si alguna validación falla
   - `validateDocumentName(String name)`:
     - Verificar no en blanco: `name.isNotBlank() && name.length() >= 1 && name.length() <= 255`
     - Verificar sin caracteres inválidos: regex para excluir separadores de ruta, caracteres especiales
   - `validateFolderPermission(Long folderId, Long userId, String requiredLevel)`:
     - Llamar adaptador de permisos (inyección de dependencia)
     - Lanzar `FolderAccessException` si permiso denegado

4. Agregar utilidades de validación reutilizables:
   - `isValidContentType(String mimeType)`
   - `isValidExtension(String filename)`
   - `getExtensionFromFilename(String filename)`

**Dependencias**: 
- `DocumentValidationConfig` (desde propiedades)
- `PermissionCheckAdapter` (para verificaciones ACL)
- Opcional: interfaz Spring `Validator`

**Notas de Implementación**:
- La configuración debe venir de `application.yml` (maxFileSize, allowedExtensions, etc.)
- Las excepciones deben ser específicas de negocio (ej. `InvalidFileTypeException`, `FileTooLargeException`)
- Soporte para validación de carga de múltiples archivos (método por lotes)
- Considerar seguridad: sanitización de nombre de archivo para prevenir traversal de directorios

---

### Paso 12: Crear DocumentService — Servicio de Aplicación

**Archivo**: `backend/document-core/src/application/service/DocumentService.java`

**Acción**: Implementar lógica de negocio central para creación de documento, orquestando dominio, repositorios, almacenamiento y validación.

**Firma de Función**:
```java
@Service
@Transactional
public class DocumentService {
    public DocumentResponse createDocument(Long folderId, CreateDocumentRequest request, 
                                          String authToken) throws DocumentException;
}
```

**Pasos de Implementación**:
1. Crear clase `DocumentService` en `src/application/service/`
2. Inyectar dependencias:
   - `IDocumentRepository documentRepository`
   - `DocumentValidator validator`
   - `StorageService storageService`
   - `PermissionCheckAdapter permissionAdapter`
   - `AuthenticationService authService` (para extraer organización/usuario del token)
   - `DocumentMapper mapper`
   - Opcional: `AuditEventPublisher auditPublisher`
3. Implementar método `createDocument(...)`:
   - **Paso 3.1**: Extraer usuario y organización del token JWT (vía `authService`)
   - **Paso 3.2**: Validar carpeta existe y usuario tiene permiso ESCRITURA (WRITE) o ADMINISTRACION:
     ```java
     FolderPermissionCheckResult permission = permissionAdapter.checkFolderPermission(
         folderId, userId, "ESCRITURA"
     );
     if (!permission.hasAccess()) throw new FolderAccessDeniedException(...);
     ```
   - **Paso 3.3**: Validar archivo cargado:
     ```java
     validator.validateFile(request.getFile(), config);
     validator.validateDocumentName(request.getFile().getOriginalFilename());
     ```
   - **Paso 3.4**: Crear modelo de dominio para `Document`:
     ```java
     Document domainDocument = Document.create(
         organizationId,
         folderId,
         extractFileName(request.getFile()),
         extractExtension(request.getFile()),
         request.getFile().getContentType(),
         request.getFile().getSize(),
         userId
     );
     ```
   - **Paso 3.5**: Cargar archivo a almacenamiento (punto de integración transaccional):
     ```java
     String storagePath = storageService.upload(
         organizationId.toString(),
         folderId.toString(),
         domainDocument.getId().toString(), // Obtendrá ID de operación guardada
         "1",
         request.getFile().getInputStream(),
         request.getFile().getSize()
     );
     ```
   - **Paso 3.6**: Crear primera versión:
     ```java
     DocumentVersion firstVersion = DocumentVersion.create(
         domainDocument.getId(),
         1,  // versionNumber
         request.getFile().getSize(),
         storagePath,
         computeSHA256(request.getFile()),
         request.getChangeComment()?,
         userId
     );
     ```
   - **Paso 3.7**: Actualizar documento con referencia de versión:
     ```java
     domainDocument.updateCurrentVersion(firstVersion.getId());
     ```
   - **Paso 3.8**: Persistir documento y versión (transaccional):
     ```java
     Document saved = documentRepository.save(domainDocument); // Incluye versión
     ```
   - **Paso 3.9**: Emitir evento de auditoría:
     ```java
     auditPublisher.publishEvent(
         new DocumentCreatedEvent(
             saved.getId(),
             userId,
             organizationId,
             "DOCUMENTO_CREADO"
         )
     );
     ```
   - **Paso 3.10**: Mapear a DTO de respuesta y devolver:
     ```java
     return mapper.toResponse(saved);
     ```

4. Manejo de errores:
   - Envolver método entero en try-catch para manejar fallos de almacenamiento
   - En excepción de almacenamiento: registrar error y lanzar `DocumentCreationException`
   - La reversión de transacción debe ser automática (Spring `@Transactional`)
   - Usar tipos de excepción específicos para diferentes escenarios de fallo

**Dependencias**: 
- `IDocumentRepository`
- `DocumentValidator`
- `StorageService`
- `PermissionCheckAdapter`
- `AuthenticationService`
- `DocumentMapper`
- Opcional: `AuditEventPublisher`

**Notas de Implementación**:
- Marcar método con `@Transactional` para asegurar propiedades ACID
- Si carga de almacenamiento falla, toda transacción se revierte (sin registros BD huérfanos)
- Todas las verificaciones de seguridad (permiso, tenant) ocurren antes de operaciones de almacenamiento
- Usar decorador `@Async` si es necesario para cargas de larga duración (pero probablemente no para MVP)
- Registrar en nivel INFO en éxito, ERROR en fallo (incluir estructura: docId, userId, orgId)
- Nunca registrar contenido de archivo o metadatos sensibles

---

### Paso 13: Crear DTOs — Solicitud

**Archivo**: `backend/document-core/src/application/dto/request/CreateDocumentRequest.java`

**Acción**: Crear DTO de solicitud con anotaciones de validación.

**Pasos de Implementación**:
1. Crear clase `CreateDocumentRequest` en `src/application/dto/request/`
2. Agregar campos:
   ```java
   public class CreateDocumentRequest {
       @NotNull(message = "Se requiere archivo")
       private MultipartFile file;
       
       @Size(max = 500, message = "Máximo 500 caracteres de comentario")
       private String changeComment;
       
       @Transient
       private Long organizationId; // Establecido por servicio desde token
   }
   ```
3. Agregar validación:
   - `@Valid` en campo MultipartFile (si usar validador personalizado)
   - `@Size` en campos de cadena
   - `@NotNull/@NotBlank` para campos requeridos
4. Agregar getters/setters vía Lombok `@Data`
5. Agregar builder si es complejo: `@Builder`

**Dependencias**: API de Validación Jakarta

**Notas de Implementación**:
- `organizationId` NO debe venir de solicitud (seguridad)
- Usar anotaciones de validación para validación declarativa
- La capa de servicio llamará `validator.validateAll()` para reglas de negocio personalizadas
- Mantener DTO simple y enfocado en contrato HTTP

---

### Paso 14: Crear DTOs — Respuesta

**Archivo**: `backend/document-core/src/application/dto/response/DocumentResponse.java`

**Acción**: Crear DTO de respuesta coincidiendo con contrato API.

**Pasos de Implementación**:
1. Crear clase `DocumentResponse` en `src/application/dto/response/`
2. Agregar campos (subconjunto de modelo de dominio apropiado para API):
   ```java
   public class DocumentResponse {
       private Long documentId;
       private String name;
       private String extension;
       private String contentType;
       private Long fileSize;
       private VersionInfo currentVersion;
       private LocalDateTime createdAt;
       private LocalDateTime updatedAt;
       
       public static class VersionInfo {
           private Long id;
           private Integer versionNumber;
       }
   }
   ```
3. Agregar builder: `@Builder`
4. Usar Lombok `@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor`

**Dependencias**: Lombok

**Notas de Implementación**:
- La respuesta NO debe incluir información sensible (rutas de almacenamiento, IDs internos si innecesarios)
- La estructura coincide con respuesta API en especificación OpenAPI
- Incluir número de versión pero no historial completo de versiones (endpoint separado para eso)
- Usar objeto anidado para versión para mantener estructura limpia

---

### Paso 15: Crear Mapeador — Conversión DTO ↔ Entidad

**Archivo**: `backend/document-core/src/application/mapper/DocumentMapper.java`

**Acción**: Crear mapeador MapStruct para conversiones DTO.

**Pasos de Implementación**:
1. Crear interfaz `DocumentMapper` en `src/application/mapper/`
2. Decorar con `@Mapper(componentModel = "spring")`
3. Definir métodos de mapeo:
   ```java
   @Mapper(componentModel = "spring")
   public interface DocumentMapper {
       DocumentResponse toResponse(Document domain);
       
       @Mapping(target = "id", ignore = true)
       @Mapping(target = "createdAt", ignore = true)
       Document toDomain(DocumentEntity entity);
       
       DocumentEntity toEntity(Document domain);
   }
   ```
4. Manejar objetos anidados (ej. VersionInfo)
5. Usar anotaciones `@Mapping` para mapeo de campo personalizado si es necesario

**Dependencias**: MapStruct (auto-genera implementación)

**Notas de Implementación**:
- MapStruct genera implementación en tiempo de compilación
- Usar `@Mapper(componentModel = "spring")` para integración Spring
- Ignorar campos sensibles o generados (timestamps, IDs de entrada)
- Se puede extender con métodos manuales si la auto-generación es insuficiente

---

### Paso 16: Crear Controlador REST

**Archivo**: `backend/document-core/src/presentation/controller/DocumentController.java`

**Acción**: Crear endpoint REST para carga de documento.

**Firma de Función**:
```java
@RestController
@RequestMapping("/api/v1/folders/{folderId}/documents")
public class DocumentController {
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
        @PathVariable Long folderId,
        @Valid @RequestPart("file") MultipartFile file,
        @RequestPart(required = false) String changeComment,
        @RequestHeader("Authorization") String authToken
    ) throws DocumentException {
        // Implementación...
    }
}
```

**Pasos de Implementación**:
1. Crear clase `DocumentController` en `src/presentation/controller/`
2. Decorar clase:
   - `@RestController`
   - `@RequestMapping("/api/v1/folders/{folderId}/documents")`
   - `@Slf4j` (Lombok para logging)
3. Inyectar `DocumentService`
4. Implementar endpoint `uploadDocument()`:
   - Ruta: `POST /api/v1/folders/{folderId}/documents`
   - Consume: `multipart/form-data`
   - Parámetros de solicitud:
     - `@PathVariable Long folderId` — ID de carpeta desde URL
     - `@Valid @RequestPart("file") MultipartFile file` — archivo cargado
     - `@RequestPart(required = false) String changeComment` — descripción de cambio opcional
     - `@RequestHeader("Authorization") String authToken` — token JWT
   - Respuesta: `ResponseEntity<DocumentResponse>` con estado 201 CREATED
   - Implementación:
     ```java
     CreateDocumentRequest request = new CreateDocumentRequest(file, changeComment);
     DocumentResponse response = documentService.createDocument(folderId, request, authToken);
     return ResponseEntity.status(HttpStatus.CREATED).body(response);
     ```

5. Agregar manejo de errores:
   - Dejar excepciones propagarse al manejador de excepciones global
   - Registrar detalles de solicitud (folderID, filesize, userId) sin datos sensibles

6. Agregar documentación:
   - `@Operation(summary = "Cargar documento a carpeta")`
   - `@ApiResponse(responseCode = "201", description = "Documento creado exitosamente")`

**Dependencias**: Spring Web, Validación Jakarta

**Notas de Implementación**:
- Usar `@RequestPart` para multipart/form-data (no `@RequestParam`)
- `@Valid` desencadena validación en DTO
- El análisis del encabezado de autorización se hará mediante capa de servicio o seguridad
- El estado de respuesta DEBE ser 201 CREATED (por convenciones REST)
- Incluir ID de solicitud en logging para trazabilidad

---

### Paso 17: Crear Manejador de Excepciones — GlobalExceptionHandler

**Archivo**: `backend/document-core/src/presentation/exception/DocumentExceptionHandler.java`

**Acción**: Crear manejador de excepciones global para respuestas de error.

**Pasos de Implementación**:
1. Crear `DocumentExceptionHandler` o extender `ResponseEntityExceptionHandler`
2. Decorar con `@ControllerAdvice`
3. Agregar manejadores de excepciones:
   ```java
   @ExceptionHandler(DocumentValidationException.class)
   public ResponseEntity<ErrorResponse> handleValidationException(
       DocumentValidationException ex, 
       HttpServletRequest request
   ) {
       ErrorResponse error = new ErrorResponse(
           "VALIDATION_ERROR",
           ex.getMessage(),
           400,
           request.getRequestURI()
       );
       return ResponseEntity.badRequest().body(error);
   }
   
   @ExceptionHandler(FolderAccessDeniedException.class)
   public ResponseEntity<ErrorResponse> handleAccessDenied(...) {
       // Devolver 403 Forbidden
   }
   
   @ExceptionHandler(FolderNotFoundException.class)
   public ResponseEntity<ErrorResponse> handleNotFound(...) {
       // Devolver 404 Not Found
   }
   
   @ExceptionHandler(StorageException.class)
   public ResponseEntity<ErrorResponse> handleStorageException(...) {
       // Devolver 500 Internal Server Error
   }
   ```

4. Crear DTO de respuesta de error:
   ```java
   public class ErrorResponse {
       private String errorCode;
       private String message;
       private int status;
       private String timestamp;
       private String path;
   }
   ```

**Dependencias**: Spring Web

**Notas de Implementación**:
- Centralizar manejo de errores para consistencia
- Mapear excepciones de dominio a códigos de estado HTTP (ver sección Formato de Respuesta de Error)
- Nunca exponer trazas de pila internas al cliente (solo en logs)
- Siempre incluir timestamp y ruta de solicitud para depuración

---

### Paso 18: Crear Configuración — DocumentValidationConfig

**Archivo**: `backend/document-core/src/config/DocumentValidationConfig.java`

**Acción**: Crear clase de configuración para reglas de validación de archivo.

**Pasos de Implementación**:
1. Crear clase `DocumentValidationConfig` en `src/config/`
2. Decorar con `@Configuration` y `@ConfigurationProperties(prefix = "docflow.document.validation")`
3. Agregar propiedades:
   ```java
   @Configuration
   @ConfigurationProperties(prefix = "docflow.document.validation")
   public class DocumentValidationConfig {
       private Long maxFileSize = 524288000L; // 500MB default
       private List<String> allowedExtensions = Arrays.asList("pdf", "docx", "xlsx", "png", "jpg", "jpeg");
       private List<String> allowedContentTypes = Arrays.asList("application/pdf", "image/png", "image/jpeg");
   }
   ```
4. Agregar getters/setters vía Lombok `@Data`

5. En `application.yml`, agregar:
   ```yaml
   docflow:
     document:
       validation:
         max-file-size: 524288000 # 500MB
         allowed-extensions: pdf,docx,xlsx,png,jpg,jpeg
         allowed-content-types: application/pdf,image/png,image/jpeg
   ```

**Dependencias**: Spring Boot Configuration

**Notas de Implementación**:
- Hacer límites configurables por entorno
- Cargar defaults desde propiedades para permitir override por entorno
- Usar anotación Spring `@Value` como alternativa si más simple

---

### Paso 19: Crear LocalStorageService — Adaptador de Almacenamiento

**Archivo**: `backend/document-core/src/infrastructure/storage/LocalStorageService.java`

**Acción**: Implementar puerto `StorageService` para almacenamiento de sistema de archivos local.

**Pasos de Implementación**:
1. Crear clase `LocalStorageService` implementando `StorageService`
2. Decorar con `@Service` y `@Primary` (para inyección de dependencia)
3. Inyectar `StoragePathConfig` (configuración para directorio raíz de almacenamiento)
4. Implementar métodos:
   - `upload(organizationId, folderId, documentId, versionNumber, content, contentLength)`:
     - Construir ruta: `{storageRoot}/{organizationId}/{folderId}/{documentId}/{versionNumber}/`
     - Crear directorios si no existen: `Files.createDirectories(path)`
     - Escribir archivo: `Files.copy(content, destination, StandardCopyOption.REPLACE_EXISTING)`
     - Devolver ruta de almacenamiento
     - En IOException: lanzar `StorageException`
   - `download(storagePath)`:
     - Verificar archivo existe: `Files.exists(path)`
     - Si no: lanzar `StorageException("Archivo no encontrado")`
     - Devolver `Files.newInputStream(path)` (streaming)
   - `delete(storagePath)`:
     - Mismas verificaciones de seguridad que download
     - Eliminar archivo: `Files.delete(path)`
     - Ignorar errores de archivo no encontrado
   - `exists(storagePath)`:
     - Devolver `Files.exists(path)`

5. Agregar seguridad:
   - Validar construcción de ruta para prevenir traversal de directorios
   - Usar `Path.normalize()` y verificar ruta canónica
   - Todas las rutas deben estar bajo raíz de almacenamiento configurado

6. Agregar logging:
   - INFO en carga/descarga exitosa
   - ERROR en fallos con contexto

**Dependencias**: Java NIO (`java.nio.file.*`)

**Notas de Implementación**:
- El almacenamiento local es por defecto para desarrollo/MVP (no para grado de producción)
- Para producción, implementar adaptador S3 o MinIO usando misma interfaz
- Upload/download basado en streaming para manejar archivos grandes sin overhead de memoria
- Operaciones atómicas: renombrar archivo temporal después de escribir para evitar archivos parciales
- Considerar estrategia de copia de seguridad si usar sistema de archivos local

---

### Paso 20: Crear Entidad de Base de Datos — Actualizar Relacionamientos

**Archivo**: Varios archivos de migración o configuración de entidad existente

**Acción**: Agregar restricciones de Foreign Key y relacionamientos entre Document y otras tablas.

**Pasos de Implementación**:
1. Crear migración `V3__add_document_foreign_keys.sql`:
   ```sql
   ALTER TABLE documento
   ADD CONSTRAINT fk_documento_organizacion
       FOREIGN KEY (organizacion_id) REFERENCES organizacion(id) ON DELETE CASCADE;
   
   ALTER TABLE documento
   ADD CONSTRAINT fk_documento_carpeta
       FOREIGN KEY (carpeta_id) REFERENCES carpeta(id) ON DELETE SET NULL;
   
   ALTER TABLE documento
   ADD CONSTRAINT fk_documento_creado_por
       FOREIGN KEY (creado_por) REFERENCES usuario(id) ON DELETE RESTRICT;
   
   ALTER TABLE documento
   ADD CONSTRAINT fk_documento_version_actual
       FOREIGN KEY (version_actual_id) REFERENCES documento_version(id) ON DELETE SET NULL;
   
   ALTER TABLE documento_version
   ADD CONSTRAINT fk_documento_version_documento
       FOREIGN KEY (documento_id) REFERENCES documento(id) ON DELETE CASCADE;
   
   ALTER TABLE documento_version
   ADD CONSTRAINT fk_documento_version_creado_por
       FOREIGN KEY (creado_por) REFERENCES usuario(id) ON DELETE RESTRICT;
   ```

2. Agregar reglas de cascada:
   - `organizacion` → `documento`: CASCADE DELETE (eliminación org elimina todos docs)
   - `carpeta` → `documento`: SET NULL (eliminación carpeta huérfana docs, no eliminados)
   - `usuario` → `documento`: RESTRICT (no puedes eliminar usuario con docs)
   - `documento` → `documento_version`: CASCADE DELETE (eliminación doc elimina versiones)

**Dependencias**: Tablas de entidad existentes (`organizacion`, `carpeta`, `usuario`)

**Notas de Implementación**:
- Usar SET NULL en carpeta para permitir reorganización de carpetas
- Usar RESTRICT en usuario para forzar integridad de datos
- CASCADE de documento a versiones tiene sentido (ciclo de vida doc incluye todas versiones)

---

### Paso 21: Crear Pruebas Unitarias — DocumentValidator

**Archivo**: `backend/document-core/src/test/java/com/docflow/documentcore/application/validator/DocumentValidatorTest.java`

**Acción**: Escribir pruebas unitarias para lógica de validación de archivo y documento.

**Pasos de Implementación** (Siguiendo enfoque TDD, escribir pruebas PRIMERO):
1. Crear clase de prueba `DocumentValidatorTest` con `@RunWith(MockitoRunner.class)`
2. Configurar:
   - Mock `DocumentValidationConfig`
   - Inicializar `DocumentValidator` bajo prueba
3. Casos de prueba:

   **Casos Exitosos:**
   - `should_validateFile_When_ValidPdfProvided()` — aseverar sin exception lanzada
   - `should_validateFile_When_ValidDocxProvided()` — aseverar sin exception lanzada
   - `should_validateDocumentName_When_ValidNameProvided()` — aseverar pasa

   **Validación Tamaño de Archivo:**
   - `should_throwException_When_FileExceedsMaxSize()` — aseverar lanza `FileTooLargeException` cuando tamaño > max configurado
   - `should_throwException_When_FileIsEmpty()` — aseverar lanza `InvalidFileException` cuando tamaño = 0
   - `should_permitFile_When_SizeEqualToMax()` — caso límite: tamaño == max (debe pasar)

   **Validación Tipo de Archivo:**
   - `should_throwException_When_InvalidContentType()` — no en lista blanca
   - `should_throwException_When_InvalidExtension()` — no en lista blanca
   - `should_throwException_When_ExtensionMismatchesContentType()` — opcional: verificar si pdf dice .doc

   **Validación Nombre de Documento:**
   - `should_throwException_When_NameIsBlank()` — vacío o solo espacios en blanco
   - `should_throwException_When_NameExceedsMaxLength()` — longitud > 255
   - `should_throwException_When_NameContainsPathSeparator()` — rechazar `../`, `/`, `\`

4. Aserciones:
   - Usar AssertJ: `assertThatThrownBy(...).isInstanceOf(...).hasMessage(...)`
   - Verificar mensajes de error son amigables al usuario

**Dependencias**: JUnit 5, Mockito, AssertJ

**Notas de Implementación**:
- Mínimo 6 casos de prueba por método validador
- Probar condiciones límite (vacío, tamaño máximo, longitud máxima)
- Mock config para probar cumplimiento de reglas
- Una aseveración por prueba para claridad

---

### Paso 22: Crear Pruebas Unitarias — DocumentService

**Archivo**: `backend/document-core/src/test/java/com/docflow/documentcore/application/service/DocumentServiceTest.java`

**Acción**: Escribir pruebas unitarias para lógica de negocio de creación de documento.

**Pasos de Implementación**:
1. Crear clase de prueba `DocumentServiceTest` con `@RunWith(MockitoRunner.class)`
2. Configurar:
   - Mock `IDocumentRepository`
   - Mock `DocumentValidator`
   - Mock `StorageService`
   - Mock `PermissionCheckAdapter`
   - Mock `AuthenticationService`
   - Inicializar `DocumentService` bajo prueba
3. Casos de prueba:

   **Casos Exitosos:**
   - `should_createDocument_When_ValidRequestWithWritePermission()`:
     - Dado: archivo válido, carpeta existe, usuario tiene permiso ESCRITURA
     - Cuando: `documentService.createDocument(...)`
     - Entonces: devuelve `DocumentResponse` con `documentId`, `versionNumber = 1`, estado 201

   **Validación Permisos:**
   - `should_throwFolderAccessDeniedException_When_UserLacksWritePermission()`:
     - Dado: usuario sin permiso ESCRITURA
     - Cuando: crear documento
     - Entonces: lanza `FolderAccessDeniedException` con estado 403

   **Validación Archivo:**
   - `should_throwValidationException_When_FileInvalid()`:
     - Dado: archivo excede tamaño máximo
     - Cuando: crear documento
     - Entonces: lanza `DocumentValidationException` antes de carga de almacenamiento

   **Reversión de Almacenamiento:**
   - `should_rollbackTransaction_When_StorageUploadFails()`:
     - Dado: servicio de almacenamiento lanza exception
     - Cuando: crear documento
     - Entonces: lanza `StorageException`, sin documento guardado en BD
     - Verificar: `documentRepository.save()` no llamado (o revertido)

   **Inicialización de Versión:**
   - `should_createDocumentWithVersionNumber1()`:
     - Cuando: crear documento
     - Entonces: `DocumentVersion.versionNumber == 1`, `Document.versionCount == 1`

   **Contexto de Organización:**
   - `should_associateDocumentWithCorrectOrganization()`:
     - Dado: token JWT con `organizationId = 123`
     - Cuando: crear documento
     - Entonces: documento guardado tiene `organizationId = 123` (desde token, no solicitud)

4. Estrategia de mockeo:
   - Mock verificación de permiso exitosa por defecto
   - Mock carga de almacenamiento para devolver ruta
   - Override mocks por caso de prueba para escenarios de fallo

**Dependencias**: JUnit 5, Mockito, AssertJ

**Notas de Implementación**:
- Probar ruta feliz y rutas de fallo
- Mock dependencias externas (almacenamiento, permisos)
- Usar `ArgumentCaptor` para verificar entrada repository.save()
- Mock comportamiento `@Transactional` si es necesario (puede necesitar `@DataJpaTest` prueba integración)

---

### Paso 23: Crear Pruebas de Integración — DocumentController

**Archivo**: `backend/document-core/src/test/java/com/docflow/documentcore/presentation/controller/DocumentControllerIT.java`

**Acción**: Escribir pruebas de integración para endpoint REST.

**Pasos de Implementación**:
1. Crear clase de prueba con `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)`
2. Usar `@LocalServerPort` para obtener puerto aleatorio
3. Usar `TestRestTemplate` para llamadas HTTP
4. Configurar datos de prueba:
   - Crear carpeta de prueba, usuario, organización
   - Autenticar y obtener token JWT
5. Casos de prueba:

   **Caso Éxito:**
   - `should_uploadDocument_Return201_When_ValidFileAndPermission()`:
     - POST a `/api/v1/folders/{folderId}/documents` con archivo multipart
     - Aseverar estado 201, respuesta incluye `documentId`, `versionNumber = 1`
     - Verificar documento persistido en BD

   **Permiso Denegado:**
   - `should_uploadDocument_Return403_When_NoWritePermission()`:
     - Usuario sin permiso ESCRITURA en carpeta
     - Aseverar estado 403

   **Archivo Inválido:**
   - `should_uploadDocument_Return400_When_FileTooLarge()`:
     - Tamaño de archivo excede máximo
     - Aseverar estado 400, mensaje de error menciona límite de tamaño

   **Carpeta No Encontrada:**
   - `should_uploadDocument_Return404_When_FolderDoesNotExist()`:
     - ID de carpeta inválido
     - Aseverar estado 404

   **Autorización Faltante:**
   - `should_uploadDocument_Return401_When_NoToken()`:
     - Solicitud sin encabezado Authorization
     - Aseverar estado 401

6. Usar `MockMultipartFile` para cargas de archivo de prueba

**Dependencias**: Spring Boot Test, MockMvc o TestRestTemplate

**Notas de Implementación**:
- Las pruebas de integración son más lentas (golpean BD/contexto Spring), así que hay menos casos
- Enfocarse en flujo end-to-end y códigos de error aquí
- Las pruebas unitarias cubren detalles de lógica de negocio

---

### Paso 24: Crear PermissionCheckAdapter — Integración de Servicio Externo

**Archivo**: `backend/document-core/src/infrastructure/adapter/PermissionCheckAdapter.java`

**Acción**: Crear adaptador para verificar permisos ACL desde servicio externo (gateway/identity).

**Pasos de Implementación**:
1. Crear interfaz `IPermissionCheckAdapter` en `src/application/port/` o `src/domain/adapter/`
2. Definir contrato:
   ```java
   public interface IPermissionCheckAdapter {
       PermissionCheckResult checkFolderPermission(Long folderId, Long userId, String requiredLevel);
       // requiredLevel: "LECTURA", "ESCRITURA", "ADMINISTRACION"
   }
   ```
3. Crear clase `PermissionCheckAdapter implements IPermissionCheckAdapter` en `infrastructure/adapter/`
4. Inyectar `RestTemplate` o `WebClient` (cliente HTTP)
5. Implementar método:
   - Llamar servicio Gateway: `GET /internal/folders/{folderId}/permissions/user/{userId}`
   - Pasar nivel requerido en parámetro query
   - Analizar respuesta: `{ "hasAccess": true/false, "userLevel": "ESCRITURA", ... }`
   - Devolver objeto `PermissionCheckResult`
   - En 403: devolver `result.withAccess(false)`
   - En 404: lanzar `FolderNotFoundException`
   - En otros errores: lanzar `ExternalServiceException` o reintentar

6. Fallback/timeout:
   - Establecer timeout de conexión (ej. 5 segundos)
   - Considerar patrón circuit breaker si llamar servicio externo frecuentemente

**Dependencias**: Spring Web (RestTemplate), opcional: Resilience4j para circuit breaker

**Notas de Implementación**:
- Almacenar resultado verificación de permiso en contexto de solicitud para evitar llamadas repetidas
- Llamar este adaptador ANTES de operaciones de almacenamiento (fallar rápido por seguridad)
- Servicio externo (gateway) debe validar organizationId coincide
- Considerar cachear resultados de permiso brevemente (ej. 1 minuto) para reducir latencia

---

### Paso 25: Actualizar Documentación Técnica

**Acción**: Revisar y actualizar documentación técnica según cambios realizados.

**Pasos de Implementación**:
1. **Revisar Cambios**: Analizar todos los cambios de código realizados durante implementación:
   - Nuevos modelos de dominio: `Document`, `DocumentVersion`
   - Nuevo servicio: `DocumentService`
   - Nuevo endpoint: `POST /api/v1/folders/{folderId}/documents`
   - Nuevo adaptador de almacenamiento: `LocalStorageService`
   - Migraciones de base de datos: tablas `documento`, `documento_version`

2. **Identificar Archivos de Documentación**: Determinar qué archivos de documentación necesitan updates:
   - **Modelo de Datos**: `ai-specs/specs/data-model.md` — agregar secciones Document, DocumentVersion (si no ya están)
   - **Especificación API**: `ai-specs/specs/api-spec.yml` — agregar definición endpoint, esquemas, códigos de error
   - **Estándares Backend**: `ai-specs/specs/backend-standards.md` — agregar patrones usados (si nuevos)
   - **README del Servicio**: `backend/document-core/README.md` — agregar info endpoint, config de almacenamiento

3. **Actualizar api-spec.yml**:
   - Agregar ruta OpenAPI: `/api/v1/folders/{folderId}/documents`
   - Agregar operación POST con:
     - Sumario: "Cargar documento a carpeta"
     - Descripción con requisitos de seguridad
     - RequestBody con tipo de media multipart/form-data
     - Respuestas: 201 (éxito), 400 (validación), 401 (no autorizado), 403 (permiso denegado), 404 (carpeta no encontrada), 500 (error servidor)
     - Esquema de respuesta: `DocumentResponse` con `documentId`, `name`, objeto `currentVersion`

4. **Actualizar data-model.md**:
   - Agregar sección entidad `Document` (si no ya presente)
   - Agregar sección entidad `DocumentVersion`
   - Incluir campos, relacionamientos, reglas de validación
   - Agregar índices y restricciones

5. **Actualizar README.md** en document-core:
   - Agregar sección de configuración:
     ```
     ## Configuración (application.yml)
     
     docflow:
       document:
         validation:
           max-file-size: 524288000  # 500MB
           allowed-extensions: pdf,docx,xlsx,png,jpg,jpeg
     
     storage:
       type: local
       path: ./documents  # Directorio de almacenamiento local
     ```
   - Agregar documentación endpoint
   - Agregar información de backend de almacenamiento
   - Agregar requisitos de seguridad/permiso

---

## 4. Orden de Implementación

```
1. Paso 0: Crear Rama de Característica
   ↓
2. Paso 1: Crear Migración de Base de Datos — Tabla Documento
   ↓
3. Paso 2: Crear Migración de Base de Datos — Tabla DocumentVersion
   ↓
4. Paso 3: Crear Modelo de Dominio — Entidad Document
   ↓
5. Paso 4: Crear Modelo de Dominio — Entidad DocumentVersion
   ↓
6. Paso 5: Crear Interfaz de Repositorio de Dominio
   ↓
7. Paso 6: Crear Entidad JPA — DocumentEntity
   ↓
8. Paso 7: Crear Entidad JPA — DocumentVersionEntity
   ↓
9. Paso 8: Crear Interfaces de Repositorio JPA de Spring Data
   ↓
10. Paso 9: Crear Implementación de Repositorio de Dominio
    ↓
11. Paso 10: Crear Interfaz de Servicio de Almacenamiento (Puerto)
    ↓
12. Paso 11: Crear FileValidator — Lógica de Validación
    ↓
13. Paso 12: Crear DocumentService — Servicio de Aplicación
    ↓
14. Paso 13: Crear DTOs — Solicitud
    ↓
15. Paso 14: Crear DTOs — Respuesta
    ↓
16. Paso 15: Crear Mapeador — DTO ↔ Entidad
    ↓
17. Paso 16: Crear Controlador REST
    ↓
18. Paso 17: Crear Manejador de Excepciones — GlobalExceptionHandler
    ↓
19. Paso 18: Crear Configuración — DocumentValidationConfig
    ↓
20. Paso 19: Crear LocalStorageService — Adaptador de Almacenamiento
    ↓
21. Paso 20: Crear Entidad de Base de Datos — Actualizar Relacionamientos
    ↓
22. Paso 21: Crear Pruebas Unitarias — DocumentValidator
    ↓
23. Paso 22: Crear Pruebas Unitarias — DocumentService
    ↓
24. Paso 23: Crear Pruebas de Integración — DocumentController
    ↓
25. Paso 24: Crear PermissionCheckAdapter — Integración de Servicio Externo
    ↓
26. Paso 25: Actualizar Documentación Técnica
    ↓
27. Compilar y Verificar
    ↓
```

---

## 5. Lista de Verificación de Pruebas

- [ ] Prueba unitaria: `DocumentValidator.validateFile()` — 6+ casos (tamaño, tipo, extensión, vacío, etc.)
- [ ] Prueba unitaria: `DocumentService.createDocument()` — 6+ casos (ruta feliz, permiso denegado, error validación, fallo storage, init versión, contexto org)
- [ ] Prueba integración: POST endpoint `DocumentController` — 5+ casos (201 creado, 400 error validación, 401 no autorizado, 403 prohibido, 404 no encontrado)
- [ ] Prueba manual: Cargar archivo PDF válido vía `curl` o Postman → verificar respuesta 201, verificar registros documento/versión en BD
- [ ] Prueba manual: Cargar archivo oversized → verificar error 400 con mensaje
- [ ] Prueba manual: Cargar sin token → verificar error 401
- [ ] Prueba manual: Cargar a carpeta sin permiso → verificar error 403
- [ ] Prueba manual: Cargar a carpeta no-existente → verificar error 404
- [ ] Verificar: Archivo persistido en directorio almacenamiento local con estructura de ruta correcta
- [ ] Verificar: Reversión transacción cuando falla almacenamiento → sin documento/versión en BD
- [ ] Verificar: Evento de auditoría emitido (revisar logs para `DOCUMENTO_CREADO`)
- [ ] Cobertura de código: Mínimo 80% para DocumentService, DocumentValidator, DocumentRepositoryImpl

---

## 6. Formato de Respuesta de Error

Todas las respuestas de error siguen esta estructura JSON:

```json
{
  "errorCode": "ERROR_CODE",
  "message": "Mensaje de error legible para humanos",
  "status": 400,
  "timestamp": "2025-02-05T12:34:56Z",
  "path": "/api/v1/folders/123/documents"
}
```

### Mapeo de Código de Estado HTTP

| Código HTTP | Código de Error | Escenario | Mensaje Ejemplo |
|-----------|-----------|----------|-----------------|
| 400 | VALIDATION_ERROR | Archivo muy grande, extensión inválida, archivo vacío | "El tamaño del archivo excede máximo de 500MB" |
| 400 | INVALID_DOCUMENT_NAME | Nombre en blanco, muy largo, caracteres inválidos | "El nombre del documento debe tener 1-255 caracteres" |
| 401 | UNAUTHORIZED | Token JWT faltante o inválido | "Token de autorización inválido o expirado" |
| 403 | PERMISSION_DENIED | Usuario carece de permiso ESCRITURA en carpeta | "Permiso denegado: nivel de acceso insuficiente (LECTURA cuando ESCRITURA requerido)" |
| 404 | FOLDER_NOT_FOUND | Carpeta no existe u pertenece a org diferente | "Carpeta no encontrada o acceso no autorizado" |
| 500 | STORAGE_ERROR | Falló persistencia de archivo a almacenamiento | "Falló guardar documento a almacenamiento" |
| 500 | INTERNAL_ERROR | Error inesperado de servidor | "Ha ocurrido un error inesperado. Por favor, intenta de nuevo más tarde." |

---

## 8. Notas

### Recordatorios Importantes

1. **Seguridad Primero**:
   - SIEMPRE validar permisos de carpeta antes de operaciones de almacenamiento
   - Extraer ID de organización del token JWT, NUNCA de solicitud
   - Sanitizar nombres de archivo para prevenir ataques de traversal de directorios
   - Nunca registrar contenido de archivo o metadatos sensibles

2. **Integridad Transaccional**:
   - Usar `@Transactional` en métodos de servicio para asegurar reversión en fallo
   - Si carga de almacenamiento falla, toda transacción se revierte (sin registros BD huérfanos)
   - Probar escenario de reversión explícitamente

3. **Multi-Tenancy**:
   - Todos los queries deben incluir filtro `organizationId`
   - Usar tenant context holders para obtener organización actual automáticamente
   - Verificar carpeta pertenece a organización antes de otorgar acceso

4. **Reglas de Negocio**:
   - El número de primera versión es SIEMPRE 1 (nunca 0)
   - `versionCount` incrementa con cada nueva versión
   - El documento no puede modificarse si está bloqueado
   - El nombre del documento debe ser único dentro de una carpeta (por org)

5. **Estrategia de Versionado**:
   - Números de versión secuenciales (1, 2, 3, ...)
   - Inmutabilidad de versión: una vez creada, versión no puede modificarse ni eliminarse
   - Cada versión es snapshot (ruta storage separada, registro BD separado)
   - `currentVersionId` siempre apunta a versión activa más reciente

6. **Consideraciones de Performance**:
   - Usar streaming para cargas/descargas de archivos (no arrays de bytes)
   - Lazy-load relacionamientos cuando sea posible (ej. no buscar todas versiones a menos que sea necesario)
   - Índice en `organizationId` y `folderId` para performance de query
   - Considerar paginación para endpoints de lista (ticket futuro)

7. **Auditoría y Cumplimiento**:
   - Emitir evento de auditoría después de creación exitosa de documento
   - Incluir `organizationId` y `userId` en todos los logs de auditoría
   - Almacenar logs de auditoría separadamente (inmutable)
   - Ningún registro de auditoría debe ser eliminado (solo-append)

### Valores de Configuración (Defaults Recomendados)

```yaml
docflow:
  document:
    validation:
      max-file-size: 524288000 # 500 MB en bytes
      allowed-extensions: pdf,docx,xlsx,pptx,txt,png,jpg,jpeg,gif
      allowed-content-types: application/pdf,image/png,image/jpeg,image/gif,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
  storage:
    type: local # local, s3, minio, azure
    local:
      path: ./documents # Relativo a root app u ruta absoluta
```

### Ejemplos de Validación de Extensión

```java
// Permitido: pdf, docx, xlsx, pptx, txt, png, jpg, jpeg, gif
// Bloqueado: exe, js, bat, sh, zip, rar (riesgo de seguridad)
// Bloqueado: vacío o extensión faltante
```

---

## 9. Próximos Pasos Después de Implementación

1. **Revisión de Código**:
   - Crear Pull Request a rama `develop`
   - Asegurar pipeline CI/CD pase (compilación, pruebas, linting)
   - Abordar feedback de revisión de código

2. **Pruebas Manuales**:
   - Probar con varios tipos y tamaños de archivo
   - Verificar manejo de errores y mensajes de usuario
   - Probar con diferentes permisos de usuario

3. **Integración**:
   - Asegurar servicio de permisos sea llamable (endpoint gateway disponible)
   - Sincronizar con equipo frontend en contrato API y códigos de error
   - Documentar setup de entorno (rutas almacenamiento, valores configuración)

4. **Tickets Relacionados** (Paralelo o Secuencial):
   - **US-DOC-002**: Descargar documento (usa DocumentVersion)
   - **US-DOC-003**: Crear nueva versión (actualiza Document existente)
   - **US-ACL-001**: Implementar sistema ACL (verificaciones de permiso)
   - **US-FOLDER-001**: Crear gestión de carpeta (datos de requisito previo)

5. **Mejoras Futuras**:
   - Implementar adaptador S3/MinIO (intercambiar LocalStorageService)
   - Agregar encriptación de archivo en reposo
   - Implementar deduplicación basada en hash de contenido
   - Agregar integración de escaneo de virus
   - Implementar Elasticsearch para búsqueda full-text

---

## 10. Verificación de Implementación — Lista de Verificación Final

### Calidad de Código
- [ ] Todas las clases siguen convenciones de nomenclatura (PascalCase clases, camelCase métodos)
- [ ] Sin valores hardcodeados (constantes en configuración)
- [ ] Sin vulnerabilidades de inyección SQL
- [ ] Sin datos sensibles en logs
- [ ] El código sigue principios DDD (lógica dominio separada infraestructura)
- [ ] Principios SOLID aplicados (SRP, DIP, etc.)

### Funcionalidad
- [ ] Ruta feliz: cargar archivo válido → 201 con documentId y versión 1
- [ ] Validación: archivo oversized → 400
- [ ] Validación: tipo archivo inválido → 400
- [ ] Seguridad: sin permisos de escritura → 403
- [ ] Seguridad: carpeta inválida → 404
- [ ] Seguridad: sin token → 401
- [ ] Almacenamiento: archivo persistido a ruta correcta
- [ ] Base de datos: registros documento y versión creados
- [ ] Transacción: fallo almacenamiento desencadena reversión

### Pruebas
- [ ] Pruebas unitarias para DocumentValidator (6+ casos)
- [ ] Pruebas unitarias para DocumentService (6+ casos)
- [ ] Pruebas integración para Controller (5+ casos)
- [ ] Todas pruebas pasando
- [ ] Cobertura código ≥ 80%

### Integración
- [ ] Manejador excepción mapea excepciones a códigos HTTP correctos
- [ ] Respuestas error coinciden formato especificación API
- [ ] Integración verificación permiso funcionando (llamada servicio externo)
- [ ] Evento auditoría publicado (DOCUMENTO_CREADO)
- [ ] Contexto organización establecido correctamente desde token

### Documentación
- [ ] `api-spec.yml` actualizado con endpoint y esquemas
- [ ] `data-model.md` incluye entidades Document y DocumentVersion
- [ ] `README.md` en document-core actualizado con config y uso
- [ ] Toda documentación en inglés
- [ ] Sin enlaces rotos u referencias desactualizadas

---

**Fin del Plan de Implementación**
