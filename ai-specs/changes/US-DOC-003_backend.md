# Plan de Implementación Backend: US-DOC-003 - Subir Nueva Versión (API) Incrementa Secuencia

## 1. Descripción General

Esta característica permite que usuarios autenticados con permiso de ESCRITURA suban una nueva versión de un documento existente, manteniendo un historial completo de cambios sin sobrescribir versiones anteriores. El sistema incrementará automáticamente el número de versión secuencial y actualizará la referencia de versión actual del documento. Esta implementación sigue principios de Domain-Driven Design (DDD) con una arquitectura hexagonal en capas, separando la lógica de dominio, servicios de aplicación y adaptadores de infraestructura.

**Principios Arquitectónicos Clave:**
- **Domain-Driven Design**: Gestión de versiones encapsulada en entidades de dominio con lógica de negocio
- **Arquitectura Limpia**: Separación clara entre capas de presentación, aplicación e infraestructura
- **Orientado a Eventos**: La creación de versiones dispara eventos de auditoría para trazabilidad
- **Control de Concurrencia**: Aislamiento de transacciones SERIALIZABLE para prevenir condiciones de carrera
- **Responsabilidad Única**: Cada capa tiene una responsabilidad específica y enfocada

---

## 2. Contexto Arquitectónico

### Capas Involucradas

**Capa de Dominio** (`backend/document-core/src/main/java/domain/`)
- Entidad `DocumentoVersion`: Representa las reglas de negocio del versionado
- Interfaz `DocumentoVersionRepository`: Puerto para operaciones de persistencia
- `VersionSequenceService`: Servicio de dominio para lógica de numeración de versiones
- Value Objects: `VersionNumber`, `FileHash`, `FileSize`

**Capa de Aplicación** (`backend/document-core/src/main/java/application/`)
- `DocumentVersionApplicationService`: Orquesta el caso de uso de creación de versiones
- DTOs: `CreateVersionRequest`, `VersionResponse`
- `VersionEventPublisher`: Puerto para publicación de eventos del dominio

**Capa de Infraestructura** (`backend/document-core/src/main/java/infrastructure/`)
- `DocumentoVersionRepositoryAdapter`: Implementación JPA/Spring Data
- `StorageAdapter`: Manejo de carga de archivos a S3/MinIO
- `EventPublisherAdapter`: Publicación de eventos a broker de mensajes
- `FileHashCalculator`: Cálculo de hash SHA256

**Capa de Presentación** (`backend/document-core/src/main/java/presentation/`)
- `DocumentVersionController`: Manejador del endpoint REST
- Mapeo de solicitudes/respuestas y validación

### Componentes y Archivos Referenciados

```
backend/document-core/src/main/java/
├── domain/
│   ├── entity/
│   │   ├── DocumentoVersion.java          [CREAR]
│   │   └── Documento.java                 [MODIFICAR - agregar version_actual_id]
│   ├── service/
│   │   └── VersionSequenceService.java    [CREAR]
│   ├── port/
│   │   ├── DocumentoVersionRepository.java          [CREAR]
│   │   ├── VersionEventPublisher.java    [CREAR]
│   │   └── FileStoragePort.java           [EXISTE/MODIFICAR]
│   └── valueobject/
│       ├── VersionNumber.java             [CREAR]
│       ├── FileHash.java                  [CREAR]
│       └── FileSize.java                  [CREAR]
├── application/
│   ├── service/
│   │   └── DocumentVersionApplicationService.java   [CREAR]
│   └── dto/
│       ├── CreateVersionRequest.java      [CREAR]
│       └── VersionResponse.java           [CREAR]
├── infrastructure/
│   ├── adapters/
│   │   ├── persistence/
│   │   │   └── DocumentoVersionRepositoryAdapter.java [CREAR]
│   │   ├── storage/
│   │   │   └── S3VersionStorageAdapter.java [CREAR]
│   │   ├── event/
│   │   │   └── VersionEventPublisherAdapter.java     [CREAR]
│   │   └── hash/
│   │       └── SHA256HashCalculator.java [CREAR]
│   └── jpa/
│       └── DocumentoVersionJpaEntity.java [CREAR]
└── presentation/
    └── controller/
        └── DocumentVersionController.java [CREAR]

db/migrations/
└── V003__create_documento_version_table.sql [CREAR]
```

---

## 3. Pasos de Implementación

### **Paso 1: Crear Migración de Base de Datos**

**Archivo**: `db/migrations/V003__create_documento_version_table.sql`

**Acción**: Crear tabla `documento_version` con restricciones apropiadas, índices y claves foráneas. Agregar columnas `version_actual_id` y `fecha_modificacion` a la tabla `documento`.

**Pasos de Implementación**:

1. Crear tabla `documento_version`:
   - Clave primaria: `id` (UUID)
   - Restricción única: `(documento_id, numero_secuencial)` para asegurar unicidad secuencial por documento
   - Claves foráneas: `documento_id`, `usuario_creador_id`, `organizacion_id`
   - Índice en `documento_id` para búsquedas eficientes
   - Índice en `organizacion_id` para consultas multi-inquilino

2. Agregar columnas a la tabla `documento`:
   - `version_actual_id` (UUID, nullable, referenciará `documento_version.id`)
   - `fecha_modificacion` (TIMESTAMP, por defecto CURRENT_TIMESTAMP)

3. Agregar restricción de clave foránea:
   - `documento.version_actual_id` → `documento_version.id`

4. Crear índices:
   - `idx_documento_version_documento_id` en `documento_id`
   - `idx_documento_version_organizacion_id` en `organizacion_id`
   - `idx_documento_version_fecha_creacion` en `fecha_creacion` (para consultas de rango temporal)

**Contenido del Script SQL** (pseudo-código):
```sql
-- Crear tabla documento_version
CREATE TABLE IF NOT EXISTS documento_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL,
    numero_secuencial INTEGER NOT NULL,
    url_storage VARCHAR(1024) NOT NULL,
    tamanio_bytes BIGINT NOT NULL,
    hash_contenido VARCHAR(64) NOT NULL,
    usuario_creador_id UUID NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    organizacion_id UUID NOT NULL,
    comentario VARCHAR(500),
    UNIQUE(documento_id, numero_secuencial),
    FOREIGN KEY(documento_id) REFERENCES documento(id) ON DELETE CASCADE,
    FOREIGN KEY(usuario_creador_id) REFERENCES usuario(id),
    FOREIGN KEY(organizacion_id) REFERENCES organizacion(id)
);

-- Crear índices
CREATE INDEX idx_documento_version_documento_id ON documento_version(documento_id);
CREATE INDEX idx_documento_version_organizacion_id ON documento_version(organizacion_id);
CREATE INDEX idx_documento_version_fecha_creacion ON documento_version(fecha_creacion);

-- Alterar tabla documento
ALTER TABLE documento ADD COLUMN IF NOT EXISTS version_actual_id UUID;
ALTER TABLE documento ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE documento ADD CONSTRAINT fk_documento_version_actual 
    FOREIGN KEY(version_actual_id) REFERENCES documento_version(id) ON DELETE SET NULL;
```

**Dependencias**: Flyway (o herramienta de migración ya en uso)

**Notas**: 
- Usar aislamiento SERIALIZABLE para actualizaciones de versiones para prevenir condiciones de carrera
- La eliminación en cascada en documento asegura integridad referencial
- Todos los timestamps usan UTC (CURRENT_TIMESTAMP)

---

### **Paso 2: Crear Value Objects**

**Archivo**: `backend/document-core/src/main/java/domain/valueobject/`

**Acción**: Crear value objects inmutables para semántica de versiones.

#### **Paso 2.1: Value Object VersionNumber**

**Archivo**: `VersionNumber.java`

**Firma de Función**:
```java
public class VersionNumber implements Comparable<VersionNumber> {
    private final Integer value;
    
    private VersionNumber(Integer value) { ... }
    public static VersionNumber of(Integer number) throws InvalidVersionNumber { ... }
    public VersionNumber nextVersion() { ... }
    public boolean isGreaterThan(VersionNumber other) { ... }
}
```

**Pasos de Implementación**:

1. Crear clase inmutable con campo `value` final
2. Implementar constructor privado para encapsulación
3. Agregar método de fábrica `of(Integer)` con validación:
   - Debe ser positivo (>= 1)
   - Si no, lanzar excepción personalizada `InvalidVersionNumber`
4. Agregar `nextVersion()`: retorna nuevo VersionNumber con `value + 1`
5. Implementar `Comparable<VersionNumber>` para ordenamiento
6. Anular `equals()`, `hashCode()`, `toString()`
7. Hacer `Serializable` para caché/mensajería

**Dependencias**: Ninguna (dominio core)

---

#### **Paso 2.2: Value Object FileHash**

**Archivo**: `FileHash.java`

**Firma de Función**:
```java
public class FileHash {
    private final String algorithm;  // ej. "sha256"
    private final String value;
    
    private FileHash(String algorithm, String value) { ... }
    public static FileHash sha256(String hexValue) throws InvalidFileHash { ... }
}
```

**Pasos de Implementación**:

1. Crear clase inmutable con campos `algorithm` y `value`
2. Agregar validación:
   - SHA256: debe ser exactamente 64 caracteres hexadecimales
   - Lanzar `InvalidFileHash` si es inválido
3. Proporcionar factory estático `sha256(String)` para caso común
4. Anular `equals()`, `hashCode()`, `toString()`

**Dependencias**: `commons-codec` para validación hex (ya en pom.xml)

---

#### **Paso 2.3: Value Object FileSize**

**Archivo**: `FileSize.java`

**Firma de Función**:
```java
public class FileSize {
    private static final long MAX_SIZE = 500 * 1024 * 1024;  // 500 MB
    private final Long bytes;
    
    private FileSize(Long bytes) { ... }
    public static FileSize ofBytes(Long bytes) throws FileSizeLimitExceeded { ... }
}
```

**Pasos de Implementación**:

1. Crear clase inmutable con campo `bytes`
2. Definir constante `MAX_SIZE`: 500 MB
3. Agregar método de fábrica `ofBytes(Long)` con validación:
   - Debe ser > 0
   - Debe ser <= 500 MB
   - Lanzar `FileSizeLimitExceeded` si se excede
4. Agregar getter: `getBytes()`
5. Anular `equals()`, `hashCode()`, `toString()`

---

### **Paso 3: Crear Entidad de Dominio - DocumentoVersion**

**Archivo**: `backend/document-core/src/main/java/domain/entity/DocumentoVersion.java`

**Acción**: Crear la entidad de dominio central que representa una versión de documento.

**Firma de Función**:
```java
public class DocumentoVersion {
    private UUID id;
    private UUID documentoId;
    private VersionNumber numeroSecuencial;
    private String urlStorage;
    private FileSize tamanio;
    private FileHash hashContenido;
    private UUID usuarioCreadorId;
    private LocalDateTime fechaCreacion;
    private UUID organizacionId;
    private String comentario;
    
    private DocumentoVersion(...) { }
    public static DocumentoVersion crearNuevaVersion(
        UUID documentoId,
        VersionNumber versNum,
        String urlStorage,
        FileSize tamanio,
        FileHash hash,
        UUID usuarioCreadorId,
        UUID organizacionId,
        String comentario) { ... }
}
```

**Pasos de Implementación**:

1. Declarar campos privados finales para todos los atributos (inmutable)
2. Crear constructor privado (para uso en fábrica)
3. Agregar método de fábrica estática `crearNuevaVersion()`:
   - Aceptar: documentoId, versionNumber, urlStorage, fileSize, fileHash, usuarioCreadorId, organizacionId, comentario
   - Validar: todos los campos requeridos no nulos
   - Generar nuevo UUID para id
   - Usar timestamp actual para `fechaCreacion`
   - Retornar nueva instancia
4. Agregar getters para todos los campos
5. Anular `equals()` (por id), `hashCode()`, `toString()`
6. Agregar método de negocio `esVersionActual()` (será computado, no almacenado)

**Notas de Implementación**:
- Esta es una raíz agregada en términos DDD
- Todos los value objects utilizados para seguridad de tipos
- Inmutable: cambios de estado solo a través de métodos de fábrica
- Timestamps en UTC

---

### **Paso 4: Actualizar Entidad Documento**

**Archivo**: `backend/document-core/src/main/java/domain/entity/Documento.java`

**Acción**: Modificar entidad existente Documento para agregar gestión de versiones.

**Pasos de Implementación**:

1. Agregar nuevos campos:
   ```java
   private UUID versionActualId;
   private LocalDateTime fechaModificacion;
   ```

2. Agregar método para actualizar versión actual:
   ```java
   public void actualizarVersionActual(UUID nuevoVersionId, LocalDateTime ahora) {
       this.versionActualId = nuevoVersionId;
       this.fechaModificacion = ahora;
   }
   ```

3. Agregar getter para `versionActualId`

4. Asegurar compatibilidad hacia atrás (los campos deben ser nullable para documentos existentes)

**Notas de Dependencias**: 
- Este es un cambio mínimo para soportar seguimiento de versiones
- `versionActualId` apunta a la versión actual

---

### **Paso 5: Crear Servicio de Dominio - VersionSequenceService**

**Archivo**: `backend/document-core/src/main/java/domain/service/VersionSequenceService.java`

**Acción**: Encapsular lógica de secuencia de versiones (servicio de dominio puro, sin dependencias externas).

**Firma de Función**:
```java
public class VersionSequenceService {
    public VersionNumber calcularProximoNumero(Integer ultimoNumero) throws InvalidVersionNumber { ... }
    public VersionNumber asignarPrimeraVersion() { ... }
}
```

**Pasos de Implementación**:

1. Crear servicio de dominio sin estado
2. Agregar método `asignarPrimeraVersion()`:
   - Retorna VersionNumber.of(1)
3. Agregar método `calcularProximoNumero(Integer lastNumber)`:
   - Si lastNumber es null/0, retornar 1
   - Si no, retornar `lastNumber + 1`
   - Lanzar `InvalidVersionNumber` si el cálculo falla
4. Agregar validación: asegurar que el número calculado es válido

**Notas**: 
- Lógica pura de dominio, sin efectos secundarios
- Sin dependencias en repositorios o servicios

---

### **Paso 6: Crear Puertos de Dominio (Interfaces)**

**Archivo**: `backend/document-core/src/main/java/domain/port/`

**Acción**: Definir puertos para interacciones de infraestructura (arquitectura hexagonal).

#### **Paso 6.1: Puerto DocumentoVersionRepository**

**Archivo**: `DocumentoVersionRepository.java`

**Firma de Función**:
```java
public interface DocumentoVersionRepository {
    DocumentoVersion save(DocumentoVersion version);
    DocumentoVersion findById(UUID id);
    Optional<DocumentoVersion> findLatestByDocumentoId(UUID documentoId);
    Integer findMaxSequenceNumberByDocumentoId(UUID documentoId);
    List<DocumentoVersion> findAllByDocumentoId(UUID documentoId, Pageable pageable);
}
```

---

#### **Paso 6.2: Puerto VersionEventPublisher**

**Archivo**: `VersionEventPublisher.java`

**Firma de Función**:
```java
public interface VersionEventPublisher {
    void publishVersionCreated(DocumentoVersion version, UUID usuarioId);
    void publishVersionUploadFailed(UUID documentoId, String motivo);
}
```

---

#### **Paso 6.3: Puerto FileStoragePort (Actualizar si es necesario)**

**Archivo**: `FileStoragePort.java`

**Asegurar que tenga**:
```java
public interface FileStoragePort {
    String uploadFile(InputStream content, String fileName, String contentType, UUID organizacionId) throws StorageException;
    InputStream downloadFile(String storagePath, UUID organizacionId) throws StorageException;
    String calculateHash(InputStream content) throws StorageException;
}
```

---

### **Paso 7: Crear Clases de Excepción**

**Archivo**: `backend/document-core/src/main/java/domain/exception/`

**Acción**: Crear excepciones específicas del dominio.

**Excepciones a crear**:

1. `DocumentNotFound`: Cuando el documento no existe
2. `UnauthorizedVersionUpload`: Cuando el usuario carece de permiso ESCRITURA
3. `InvalidVersionNumber`: Cuando el número de versión es inválido
4. `FileSizeLimitExceeded`: Cuando el archivo excede 500 MB
5. `InvalidFileHash`: Cuando el cálculo de hash falla
6. `ConcurrentVersionUpload`: Cuando se detecta carga concurrente (escenario 409 Conflict)
7. `StorageException`: Cuando la carga a S3/MinIO falla

**Patrón de Implementación**:
```java
public class DocumentNotFound extends DomainException {
    public DocumentNotFound(UUID documentoId) {
        super("Document with id " + documentoId + " not found");
    }
}
```

---

### **Paso 8: Crear DTOs (Capa de Aplicación)**

**Archivo**: `backend/document-core/src/main/java/application/dto/`

**Acción**: Crear objetos de transferencia de datos de solicitud/respuesta.

#### **Paso 8.1: CreateVersionRequest**

**Archivo**: `CreateVersionRequest.java`

```java
public class CreateVersionRequest {
    private MultipartFile file;
    private String comentario;  // Opcional, máx 500 caracteres
    
    // Getters, anotaciones de validación (@NotNull, @Size)
}
```

**Validación**:
- `file` no debe ser nulo
- `file.getSize()` no debe exceder 500 MB
- `comentario` debe ser <= 500 caracteres (si se proporciona)

---

#### **Paso 8.2: VersionResponse**

**Archivo**: `VersionResponse.java`

```java
public class VersionResponse {
    private String status;  // "success"
    private VersionData data;
    
    public static class VersionData {
        private UUID versionId;
        private UUID documentoId;
        private Integer numeroSecuencial;
        private LocalDateTime fechaCreacion;
        private Long tamanioBytes;
        private String hashContenido;
        private Boolean esVersionActual;
    }
}
```

---

### **Paso 9: Crear Servicio de Aplicación**

**Archivo**: `backend/document-core/src/main/java/application/service/DocumentVersionApplicationService.java`

**Acción**: Orquestar el caso de uso de creación de versiones.

**Firma de Función**:
```java
@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class DocumentVersionApplicationService {
    public VersionResponse crearNuevaVersion(
        UUID documentoId,
        UUID usuarioId,
        UUID organizacionId,
        MultipartFile archivo,
        String comentario) throws Exception { ... }
}
```

**Pasos de Implementación**:

1. **Inyectar dependencias**:
   - `DocumentoRepository`
   - `DocumentoVersionRepository`
   - `FileStoragePort`
   - `VersionEventPublisher`
   - `VersionSequenceService`
   - `Servicio de Permisos` (o verificador de permisos)

2. **Crear wrapper de transacción** con `@Transactional(isolation = Isolation.SERIALIZABLE)`:
   - Asegura creación atómica de versiones
   - Previene condiciones de carrera en escenarios concurrentes

3. **Implementar lógica**:
   ```
   1. Validar que documento existe
      - si no se encuentra, lanzar DocumentNotFound
   
   2. Verificar permiso
      - verificar que usuarioId tiene permiso ESCRITURA en documentoId
      - si se deniega, lanzar UnauthorizedVersionUpload (403)
   
   3. Validar archivo
      - verificar tamaño de archivo <= 500 MB
      - si se excede, lanzar FileSizeLimitExceeded (413)
      - verificar que archivo no está vacío
      - si está vacío, lanzar InvalidFileInput (400)
   
   4. Calcular hash
      - leer stream de archivo
      - computar hash SHA256
      - almacenar valor de hash
   
   5. Obtener siguiente número de secuencia
      - obtener secuencia máxima para documentoId
      - calcular siguiente a través de VersionSequenceService
   
   6. Cargar archivo a almacenamiento
      - llamar FileStoragePort.uploadFile()
      - obtener URL de almacenamiento
      - si falla, lanzar StorageException
   
   7. Crear entidad DocumentoVersion de dominio
      - usar método de fábrica crearNuevaVersion()
   
   8. Guardar en base de datos
      - llamar DocumentoVersionRepository.save()
      - si viola restricción en (documento_id, numero_secuencial), 
        lanzar ConcurrentVersionUpload (409)
   
   9. Actualizar documento.version_actual_id
      - llamar Documento.actualizarVersionActual()
      - guardar documento actualizado
   
   10. Publicar evento
       - llamar VersionEventPublisher.publishVersionCreated()
   
   11. Retornar VersionResponse con datos de nueva versión
   ```

**Dependencias**:
- Spring Framework (Transaction, Service)
- Puertos de dominio
- Servicios de dominio

**Notas de Implementación**:
- El aislamiento SERIALIZABLE es crítico para concurrencia
- Todas las operaciones dentro de una única transacción
- La carga de archivo a almacenamiento debe ser idémpotente (en caso de reintento)
- La publicación de eventos debe ser transaccional (eventos Spring o broker de mensajes)

---

### **Paso 10: Crear Adaptador de Repositorio**

**Archivo**: `backend/document-core/src/main/java/infrastructure/adapters/persistence/DocumentoVersionRepositoryAdapter.java`

**Acción**: Implementar puerto DocumentoVersionRepository usando JPA.

**Pasos de Implementación**:

1. Crear interfaz del repositorio Spring Data JPA:
   ```java
   @Repository
   public interface DocumentoVersionJpaRepository extends JpaRepository<DocumentoVersionJpaEntity, UUID> {
       List<DocumentoVersionJpaEntity> findByDocumentoIdOrderByNumeroSecuencialDesc(UUID documentoId);
       Optional<DocumentoVersionJpaEntity> findTopByDocumentoIdOrderByNumeroSecuencialDesc(UUID documentoId);
       Integer findMaxNumeroSecuencialByDocumentoId(UUID documentoId);
   }
   ```

2. Crear clase adaptadora:
   ```java
   @Component
   public class DocumentoVersionRepositoryAdapter implements DocumentoVersionRepository {
       private final DocumentoVersionJpaRepository jpaRepository;
       private final DocumentoVersionMapper mapper;
       
       @Override
       public DocumentoVersion save(DocumentoVersion version) { ... }
       
       @Override
       public DocumentoVersion findById(UUID id) { ... }
       
       @Override
       public Optional<DocumentoVersion> findLatestByDocumentoId(UUID documentoId) { ... }
       
       @Override
       public Integer findMaxSequenceNumberByDocumentoId(UUID documentoId) { ... }
   }
   ```

3. Crear mapeador para convertir entre entidad de dominio y entidad JPA

---

### **Paso 11: Crear Adaptador de Almacenamiento**

**Archivo**: `backend/document-core/src/main/java/infrastructure/adapters/storage/S3VersionStorageAdapter.java`

**Acción**: Implementar carga/descarga de archivos a S3/MinIO.

**Pasos de Implementación**:

1. Inyectar cliente S3 (de infraestructura existente)

2. Implementar `uploadFile()`:
   - Generar nombre de archivo único: `{organizacionId}/{documentoId}/{versionNumber}/{UUID}_{originalName}`
   - Crear encabezados de metadatos
   - Cargar a bucket S3/MinIO `docflow-documents`
   - Retornar URL de almacenamiento completa
   - Manejar S3Exception y envolver como StorageException

3. Implementar `calculateHash()`:
   - Leer stream y computar SHA256
   - Retornar string hexadecimal

4. Implementar `downloadFile()`:
   - Recuperar de S3/MinIO usando ruta de almacenamiento
   - Verificar que organizationId coincida (límite de seguridad)
   - Retornar InputStream

**Dependencias**:
- AWS SDK o MinIO SDK (ya en uso)
- Apache Commons Codec (para SHA256)

---

### **Paso 12: Crear Adaptador de Publicador de Eventos**

**Archivo**: `backend/document-core/src/main/java/infrastructure/adapters/event/VersionEventPublisherAdapter.java`

**Acción**: Publicar eventos de dominio para auditoría y procesamiento descendente.

**Pasos de Implementación**:

1. Implementar `publishVersionCreated()`:
   - Crear DTO de evento con:
     - `eventType`: "VERSION_CREATED"
     - `versionId`, `documentoId`, `numeroSecuencial`
     - `usuarioId`, `timestamp`
     - `organizacionId`
   - Publicar a broker de mensajes (Kafka, RabbitMQ, o Spring Events)
   - Registrar emisión de evento

2. Implementar `publishVersionUploadFailed()`:
   - Crear evento con detalles de falla
   - Publicar para monitoreo/alertas

**Dependencias**:
- Configuración de broker de mensajes
- Serialización de eventos (Jackson)

---

### **Paso 13: Crear Entidad JPA**

**Archivo**: `backend/document-core/src/main/java/infrastructure/jpa/DocumentoVersionJpaEntity.java`

**Acción**: Crear entidad JPA para mapeo de base de datos.

**Pasos de Implementación**:

1. Agregar anotaciones:
   ```java
   @Entity
   @Table(name = "documento_version", uniqueConstraints = {
       @UniqueConstraint(columnNames = {"documento_id", "numero_secuencial"})
   })
   ```

2. Mapear todos los campos a columnas:
   - `id` → UUID
   - `documento_id` → documentoId
   - `numero_secuencial` → numeroSecuencial
   - `url_storage` → urlStorage
   - `tamanio_bytes` → tamanioBytes
   - `hash_contenido` → hashContenido
   - `usuario_creador_id` → usuarioCreadorId
   - `fecha_creacion` → fechaCreacion (timestamp auto)
   - `organizacion_id` → organizacionId
   - `comentario` → comentario

3. Agregar callbacks del ciclo de vida de JPA si es necesario

---

### **Paso 14: Crear Controlador REST**

**Archivo**: `backend/document-core/src/main/java/presentation/controller/DocumentVersionController.java`

**Acción**: Crear endpoint REST para aceptar solicitudes de carga de versiones.

**Firma de Función**:
```java
@RestController
@RequestMapping("/api/documents/{documentId}/versions")
public class DocumentVersionController {
    
    @PostMapping
    public ResponseEntity<VersionResponse> uploadVersion(
        @PathVariable UUID documentId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "comentario", required = false) String comentario,
        @RequestHeader("Authorization") String authHeader) throws Exception { ... }
}
```

**Pasos de Implementación**:

1. **Extraer información del usuario desde JWT**:
   - Analizar encabezado de Autorización (token Bearer)
   - Extraer userId y organizacionId
   - Establecer en SecurityContext

2. **Validar solicitud**:
   - Verificar que documentId sea UUID válido
   - Usar anotación `@Valid` en CreateVersionRequest
   - Capturar errores de validación → 400 Bad Request

3. **Llamar servicio de aplicación**:
   - `documentVersionApplicationService.crearNuevaVersion(...)`

4. **Manejar excepciones y mapear a respuestas HTTP**:
   - `DocumentNotFound` → 404
   - `UnauthorizedVersionUpload` → 403
   - `FileSizeLimitExceeded` → 413 (Payload Too Large)
   - `ConcurrentVersionUpload` → 409 (Conflict)
   - Otras excepciones → 500

5. **Retornar respuesta**:
   - Éxito: 201 Created con cuerpo VersionResponse
   - Incluir encabezado `Location` con URL de versión

6. **Agregar anotaciones de OpenAPI/Swagger**:
   - `@Operation`, `@ApiResponse`, `@Parameter` para documentación

**Dependencias**:
- Spring Framework
- Spring Security (para extracción de JWT)
- Anotaciones de validación

**Notas de Implementación**:
- Usar manejo de errores apropiado con mensajes significativos
- Registrar todos los intentos de creación de versión (para auditoría)
- Se debe considerar limitación de velocidad (opcional para el futuro)

---

### **Paso 15: Escribir Tests Unitarios**

**Archivo**: `backend/document-core/src/test/java/`

**Acción**: Crear tests unitarios completos para cada capa.

#### **Estructura de Tests**:

```
test/
├── domain/
│   ├── entity/
│   │   └── DocumentoVersionTest.java
│   ├── service/
│   │   └── VersionSequenceServiceTest.java
│   └── valueobject/
│       ├── VersionNumberTest.java
│       ├── FileSizeTest.java
│       └── FileHashTest.java
├── application/
│   └── service/
│       └── DocumentVersionApplicationServiceTest.java
└── presentation/
    └── controller/
        └── DocumentVersionIntegrationTest.java
```

#### **Casos de Test por Categoría**:

**1. Tests de Value Objects** (VersionNumber, FileSize, FileHash):
- Creación válida
- Creación inválida (lanza excepción)
- Comparación/ordenamiento
- Serialización

**2. Tests de Entidad de Dominio** (DocumentoVersion):
- Método de fábrica crea instancia válida
- Verificación de inmutabilidad
- Corrección de método de negocio

**3. Tests de Servicio de Dominio** (VersionSequenceService):
- Primera versión retorna 1
- Siguiente versión se incrementa correctamente
- Números inválidos lanzan excepción

**4. Tests de Servicio de Aplicación** (DocumentVersionApplicationService):

**Caso Exitoso**:
- Carga de versión válida incrementa secuencia
- URL de almacenamiento poblada correctamente
- Hash calculado y almacenado
- Versión actual actualizada
- Evento publicado

**Errores de Validación**:
- Archivo vacío → 400
- Archivo > 500 MB → 413
- UUID de documento inválido → 400

**Errores de Permiso**:
- Usuario sin permiso ESCRITURA → 403

**No Encontrado**:
- Documento no existe → 404

**Concurrencia**:
- Cargas simultáneas → una exitosa (201), una falla (409)

**Errores de Servidor**:
- Carga a almacenamiento falla → 500
- Transacción BD falla → 500

**Casos Límite**:
- Archivo muy grande cerca del límite de 500 MB
- Nombre de archivo Unicode con caracteres especiales
- Cargas concurrentes con múltiples hilos

**5. Tests de Controlador** (DocumentVersionController):
- Mapeo de solicitud correcto
- Análisis de encabezado de autorización
- Formato de respuesta de error coincide con especificación
- Ruta feliz retorna 201

#### **Objetivo de Cobertura de Tests**: > 80% de cobertura de líneas

#### **Herramientas de Testing**:
- JUnit 5
- Mockito
- Spring Boot Test
- TestcontainersSQL (para tests de integración)

---

### **Paso 16: Escribir Tests de Integración**

**Archivo**: `backend/document-core/src/test/java/integration/`

**Acción**: Crear tests de integración validando escenarios end-to-end.

**Escenarios de Test de Integración**:

1. **Escenario 1: Nueva versión incrementa secuencia**
   - Crear documento
   - Cargar versión 1
   - Cargar versión 2
   - Verificar que secuencia se incrementó correctamente

2. **Escenario 2: Nueva versión rechazada sin permisos**
   - Crear documento con Usuario A
   - Intentar carga desde Usuario B (sin permiso)
   - Verificar respuesta 403

3. **Escenario 3: Documento inexistente**
   - Intentar carga a ID de documento inexistente
   - Verificar respuesta 404

4. **Escenario 4: Validación de archivo**
   - Cargar archivo vacío → 400
   - Cargar archivo > 500 MB → 413

5. **Escenario 5: Concurrencia en versionado**
   - Cargas simultáneas desde 2 usuarios
   - Verificar una exitosa (201), una obtiene 409
   - Verificar que números de secuencia no se superponen

6. **Escenario 6: Token inválido**
   - Solicitud sin encabezado Authorization → 401
   - Solicitud con token inválido → 401

**Enfoque de Test**:
- Usar `@SpringBootTest` con base de datos embebida
- Mockear S3/MinIO con LocalStack o Testcontainers
- Usar `TestRestTemplate` para llamadas HTTP
- Verificar estado de BD después de operaciones
- Usar `@Transactional(propagation = NOT_SUPPORTED)` para evitar rollback automático

---

### **Paso 17: Actualizar Documentación Técnica**

**Acción**: Revisar todos los cambios de código y actualizar archivos de documentación afectados.

**Pasos de Implementación**:

1. **Revisar Cambios Across All Layers**:
   - Documentar cambios de esquema de BD
   - Especificación de endpoint API
   - Actualizaciones de modelo de dominio
   - Decisiones de arquitectura

2. **Identificar Archivos de Documentación a Actualizar**:
   - `ai-specs/specs/data-model.md` → Agregar descripción de entidad DocumentoVersion
   - `ai-specs/specs/api-spec.yml` → Agregar endpoint POST /api/documents/{documentId}/versions
   - `backend/document-core/README.md` → Actualizar con info de gestión de versiones

3. **Actualizar Cada Archivo**:

   **3.1 Modelo de Datos** (`ai-specs/specs/data-model.md`):
   - Agregar diagrama de entidad DocumentoVersion
   - Describir relaciones: Documento ← → DocumentoVersion
   - Explicar lógica de secuenciación de versiones
   - Listar todos los campos y restricciones

   **3.2 Especificación de API** (`ai-specs/specs/api-spec.yml`):
   - Agregar definición de endpoint OpenAPI:
     ```yaml
     /documents/{documentId}/versions:
       post:
         summary: Cargar nueva versión
         parameters:
           - name: documentId
             in: path
             required: true
             schema:
               type: string
               format: uuid
         requestBody:
           required: true
           content:
             multipart/form-data:
               schema:
                 type: object
                 properties:
                   file:
                     type: string
                     format: binary
                   comentario:
                     type: string
         responses:
           '201':
             description: Versión creada exitosamente
           '403':
             description: Prohibido - permisos insuficientes
           '404':
             description: Documento no encontrado
           '409':
             description: Conflicto - carga de versión concurrente
           '413':
             description: Payload Too Large
     ```

   **3.3 Estándares de Backend** (si es necesario):
   - Documentar patrones DDD usados
   - Referenciar nivel de aislamiento de transacciones
   - Notar sobre publicación de eventos

   **3.4 README de Servicio** (`backend/document-core/README.md`):
   - Agregar sección: "Versionado de Documentos"
   - Explicar cómo funciona el versionado
   - Enlazar a especificación de API

4. **Verificar Documentación**:
   - Todos los cambios reflejados con precisión
   - Sigue estructura establecida
   - Idioma inglés apropiado (según estándares de documentación)
   - Formato consistente con documentos existentes

5. **Reportar Actualizaciones**: Documentar qué archivos fueron modificados y qué cambios se hicieron

**Referencias**: 
- `ai-specs/specs/documentation-standards.md` para requerimientos de formato

---

## 4. Orden de Implementación

Los siguientes pasos deben completarse en esta secuencia:

* **Paso 1**: Crear Migración de Base de Datos
* **Paso 2**: Crear Value Objects (VersionNumber, FileSize, FileHash)
* **Paso 3**: Crear Entidad de Dominio - DocumentoVersion
* **Paso 4**: Actualizar Entidad Documento
* **Paso 5**: Crear Servicio de Dominio - VersionSequenceService
* **Paso 6**: Crear Puertos de Dominio (Repository, EventPublisher, FileStorage)
* **Paso 7**: Crear Clases de Excepción
* **Paso 8**: Crear DTOs (CreateVersionRequest, VersionResponse)
* **Paso 9**: Crear Servicio de Aplicación
* **Paso 10**: Crear Adaptador de Repositorio
* **Paso 11**: Crear Adaptador de Almacenamiento
* **Paso 12**: Crear Adaptador de Publicador de Eventos
* **Paso 13**: Crear Entidad JPA
* **Paso 14**: Crear Controlador REST
* **Paso 15**: Escribir Tests Unitarios
* **Paso 16**: Escribir Tests de Integración
* **Paso 17**: Actualizar Documentación Técni* 
---

## 5. Lista de Chequeo de Testing

### Pre-Implementación
- [ ] Rama feature creada exitosamente
- [ ] Archivo de migración de BD creado

### Durante la Implementación
- [ ] Todos los value objects validan correctamente
- [ ] Entidad de dominio es inmutable
- [ ] Servicio de aplicación maneja todos los escenarios
- [ ] Adaptador de repositorio conecta a BD
- [ ] Adaptador de almacenamiento maneja operaciones S3/MinIO
- [ ] Controlador mapea solicitudes correctamente

### Tests Unitarios
- [ ] Tests de value objects pasan (> 95% cobertura)
- [ ] Tests de fábrica de entidad pasan
- [ ] Tests de servicio de dominio pasan
- [ ] Tests de servicio de aplicación pasan (todos los 7 escenarios)
- [ ] Tests de controlador pasan
- [ ] **Cobertura total > 80%**

### Tests de Integración
- [ ] Escenario 1: Secuencia se incrementa (CA1)
- [ ] Escenario 2: Validación de permisos (CA2)
- [ ] Escenario 3: Documento no encontrado (CA3)
- [ ] Escenario 4: Validación de archivo (CA4)
- [ ] Escenario 5: Manejo de concurrencia (CA5)
- [ ] Escenario 6: Validación de token (CA6)

### Post-Integración
- [ ] Todos los tests pasan localmente
- [ ] Tests pasan en pipeline CI/CD
- [ ] Reporte de cobertura de código generado
- [ ] Documentación Swagger renderiza correctamente
- [ ] Testing manual completado

---

## 6. Error Response Format

All error responses follow this JSON structure:

```json
{
  "status": "error",
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "timestamp": "2026-02-05T14:35:00.000Z"
  }
}
```

### HTTP Status Code Mapping

| Scenario | Status | Code | Message |
|----------|--------|------|---------|
| Success | 201 | N/A | Created |
| Bad Request (validation) | 400 | INVALID_REQUEST | File is empty / Invalid format |
| Unauthorized (no token) | 401 | UNAUTHORIZED | Missing or invalid authorization |
| Forbidden (no permission) | 403 | ACCESS_DENIED | Insufficient permissions for this operation |
| Not Found | 404 | DOCUMENT_NOT_FOUND | Document with ID {id} not found |
| Conflict (concurrent) | 409 | CONCURRENT_UPLOAD | Another version upload in progress |
| Payload Too Large | 413 | FILE_TOO_LARGE | File exceeds maximum size of 500 MB |
| Internal Error | 500 | INTERNAL_ERROR | An unexpected error occurred |

---

## 7. Soporte de Actualización Parcial

**No Aplica**: El versionado de documentos no soporta actualizaciones parciales. Cada carga de versión es atómica y crea un nuevo registro de versión completo.

---

### Variables de Entorno

Asegurase de que estas estén configuradas:

```bash
# S3/MinIO
AWS_S3_BUCKET_NAME=docflow-documents
AWS_S3_ENDPOINT=http://localhost:9000  # Para MinIO
AWS_ACCESS_KEY_ID=docflow-documents
AWS_SECRET_ACCESS_KEY=<password>

# Base de Datos
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/docflow
SPRING_DATASOURCE_USERNAME=docflow
SPRING_DATASOURCE_PASSWORD=docflow_secret

# JWT
JWT_SECRET=<your-secret-key>
```

---

## 8. Notas

### Recordatorios Importantes

1. **Aislamiento SERIALIZABLE**: El nivel de aislamiento de transacción DEBE ser `SERIALIZABLE` para prevenir condiciones de carrera en cargas de versiones concurrentes. Esto es crítico para la corrección.

2. **Objetos de Dominio Inmutables**: Todas las entidades de dominio deben ser inmutables después de la creación. Los cambios de estado solo a través de métodos de fábrica bien definidos.

3. **Publicación de Eventos**: Los eventos de creación de versión DEBEN publicarse dentro de la transacción para asegurar consistencia eventual. Usar `@TransactionalEventListener` de Spring o manejo de transacción explícito.

4. **Idempotencia de Carga de Archivo**: Considerar implementar cargas de archivo idémpotentes a S3 usando validación de ETag, permitiendo reintentos seguros sin crear versiones duplicadas.

5. **Eliminación en Cascada**: La restricción de BD `ON DELETE CASCADE` para documento → documento_version asegura integridad referencial cuando se eliminan documentos.

6. **Consistencia de Timestamps**: Todos los timestamps deben estar en UTC y usar `CURRENT_TIMESTAMP` en el lado de la BD para consistencia.

### Reglas de Negocio

- Cada documento debe tener al menos versión 1 cuando se crea
- Los números de versión son secuenciales y únicos por documento (forzado por restricción única)
- Solo usuarios con permiso ESCRITURA pueden crear versiones
- La versión actual (`version_actual_id`) siempre apunta a la última versión
- Las versiones no pueden ser modificadas o eliminadas (historial inmutable)
- Límite de tamaño de archivo: 500 MB por versión

### Requisitos de Idioma

- Todos los comentarios de código y documentación en español.
- Todos los mensajes de error en español
- Seguir el estilo de código existente en el proyecto

---

## 10. Siguientes Pasos Después de la Implementación

1s. **Enlace de Documentación**:
   - Actualizar README del proyecto para referenciar documentación de versionado
   - Agregar enlace en portal de documentación de API

3. **Monitoreo y Logging**:
   - Configurar alertas para fallos de carga de versión
   - Monitorear uso de almacenamiento para archivos de versión
   - Registrar toda creación de versión para auditoría

4. **Optimización de Desempeño** (futuro):
   - Considerar paginación para consultas de historial de versiones
   - Implementar almacenamiento en caché para versiones frecuentemente accedidas
   - Monitorear desempeño de consultas de BD

---

## 11. Verificación de Implementación

### Lista de Chequeo final de Verificación

#### Calidad de Código
- [ ] Todo el código sigue guías de estilo del proyecto
- [ ] Sin valores hardcodeados (usar configuración)
- [ ] Logging apropiado en niveles adecuados (DEBUG, INFO, WARN, ERROR)
- [ ] Sin vulnerabilidades de seguridad (sin SQL injection, XSS, etc.)
- [ ] Datos sensibles no registrados (contraseñas, tokens)

#### Funcionalidad
- [ ] Todos los 6 criterios de aceptación satisfechos
- [ ] Casos límite manejados graciosamente
- [ ] Mensajes de error claros y accionables
- [ ] Formato de respuesta coincide exactamente con especificación
- [ ] Códigos de estado HTTP correctos

#### Testing
- [ ] Cobertura de test unitario > 80%
- [ ] Todos los escenarios de test de integración pasan
- [ ] Tests se ejecutan exitosamente en CI/CD
- [ ] Sin tests inestables

#### Integración
- [ ] Controlador correctamente cableado al servicio de aplicación
- [ ] Servicio de aplicación usa servicio de dominio y puertos
- [ ] Puertos correctamente implementados por adaptadores
- [ ] Migración BD puede aplicarse y deshacerse

#### Documentación
- [ ] Especificación Swagger/OpenAPI actualizada y precisa
- [ ] Documentación de modelo de datos actualizada
- [ ] Comentarios de código explican lógica compleja
- [ ] README actualizado con info de gestión de versiones
- [ ] Toda documentación en inglés

#### Preparación para Deployment
- [ ] Sin conflictos de migración BD con otras migraciones
- [ ] Variables de entorno documentadas
- [ ] Rama feature puede fusionarse sin conflictos
- [ ] Sin cambios que rompan APIs existentes

---

## Resumen

Este plan de implementación proporciona una guía completa paso a paso para implementar US-DOC-003 siguiendo principios Domain-Driven Design y arquitectura hexagonal. La característica permite versionado de documentos con transacciones atómicas, validación apropiada de permisos y manejo de acceso concurrente. Seguir los pasos en orden, escribir tests a medida que se implementa, y asegurar que toda documentación esté actualizada antes de fusionar.

**Factores Clave de Éxito**:
- ✅ Entidades de dominio inmutables con value objects para seguridad de tipos
- ✅ Aislamiento de transacción SERIALIZABLE para prevención de condiciones de carrera
- ✅ Separación clara de preocupaciones (Domain, Application, Infrastructure, Presentation)
- ✅ Testing comprensivo (unitario + integración)
- ✅ Actualizaciones completas de documentación

