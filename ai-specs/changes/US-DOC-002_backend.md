# Plan de Implementación Backend: US-DOC-002 Descargar versión actual (API)

## Descripción General

Esta tarea implementa el endpoint `GET /api/documents/{documentId}/download` para permitir que usuarios autenticados con permiso `LECTURA` descarguen la versión actual de un documento. La implementación sigue principios de Diseño Dirigido por Dominio (DDD), respeta la Arquitectura Hexagonal, y mantiene la seguridad a través de validación de permisos basada en ACL con aislamiento adecuado de inquilinos (tenants).

**Principio Clave de Arquitectura**: Esta implementación separa responsabilidades en tres capas:
- **Capa de Dominio**: Lógica de negocio para recuperación de documentos y validación de permisos
- **Capa de Aplicación**: DTOs y orquestación de casos de uso
- **Capa de Infraestructura**: Controladores HTTP, adaptadores de almacenamiento, y persistencia de datos

---

## Contexto de Arquitectura

### Capas Involucradas

1. **Capa de Dominio** (`src/main/java/com/docflow/documentcore/domain/`)
   - Servicio para operaciones de documentos y validación de permisos
   - Tipos de excepciones para violaciones de reglas de negocio
   - Entidades del modelo (Document, DocumentVersion)

2. **Capa de Aplicación** (`src/main/java/com/docflow/documentcore/application/`)
   - DTOs para manejo de solicitudes/respuestas
   - Orquestación de servicios
   - Implementaciones de validadores

3. **Capa de Infraestructura** (`src/main/java/com/docflow/documentcore/infrastructure/`)
   - Controlador REST que expone endpoints HTTP
   - Adaptador de almacenamiento para operaciones del sistema de archivos
   - Repositorio para acceso a base de datos
   - Manejadores de excepciones para respuestas de error consistentes
   - Utilidad de resolución de tipo MIME

4. **Capa de Presentación** (`src/main/java/com/docflow/documentcore/presentation/`)
   - Controladores HTTP y manejadores de respuesta

### Componentes y Archivos Referenciados

- Adaptador de almacenamiento: `infrastructure/adapter/storage/StorageService.java`
- Repositorio de documentos: `infrastructure/adapter/persistence/DocumentRepository.java`
- Controlador: `presentation/controller/DocumentController.java`
- Servicio de dominio: `domain/service/DocumentService.java`
- DTOs: `application/dto/` (nuevo DownloadDocumentDto)
- Excepciones: `domain/exception/` (nuevas DocumentDownloadException, FileStorageException)
- Utilidades: `infrastructure/util/MimeTypeResolver.java`

---

## Pasos de Implementación

### Paso 0: Crear Rama de Característica

**Acción**: Crear y cambiar a una rama de característica para aislar el trabajo de implementación siguiendo el flujo de trabajo de desarrollo.

**Nombre de Rama**: `feature/US-DOC-002-download-document-api`

**Pasos de Implementación**:

1. Asegúrate de estar en la rama `develop` o `main` más reciente
2. Descarga los cambios más recientes: `git pull origin develop`
3. Crea nueva rama: `git checkout -b feature/US-DOC-002-download-document-api`
4. Verifica la creación de rama: `git branch --show-current`

**Notas**: 
- Consulta la sección "Development Workflow" en [backend-standards.md](../specs/backend-standards.md) para convenciones específicas de nombres de rama
- Este debe ser el **PRIMER paso** antes de cualquier cambio de código
- Asegúrate de que no haya cambios sin confirmar en el directorio de trabajo antes de crear la rama

---

### Paso 1: Crear Excepciones Personalizadas

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/DocumentDownloadException.java` (nuevo)

**Acción**: Crear excepciones de dominio para fallos en operaciones de descarga de documentos.

**Firma de Función**: 
```java
public class DocumentDownloadException extends RuntimeException {
    private final String codigo;
    private final Map<String, Object> detalles;
    
    public DocumentDownloadException(String codigo, String mensaje, Map<String, Object> detalles)
}

public class FileStorageException extends RuntimeException {
    private final String codigo;
    private final Map<String, Object> detalles;
    
    public FileStorageException(String codigo, String mensaje, Map<String, Object> detalles)
}
```

**Pasos de Implementación**:

1. Crear clase excepción `DocumentDownloadException`
   - Extiende `RuntimeException`
   - Almacenar campo error `codigo` (p. ej., "ACCESO_DENEGADO", "DOCUMENTO_NO_ENCONTRADO")
   - Almacenar `mensaje` (mensaje amigable para el usuario)
   - Almacenar `detalles` Map para contexto adicional (document_id, version_id, etc.)
   - Proporcionar constructor que acepte codigo, mensaje, y detalles

2. Crear clase excepción `FileStorageException`
   - Estructura similar a DocumentDownloadException
   - Se usa cuando no se encuentra el archivo físico
   - Almacenar error codigo: "ARCHIVO_NO_DISPONIBLE"

3. Crear clase excepción `PermissionDeniedException`
   - Se usa cuando el usuario carece de permiso de LECTURA
   - Almacenar codigo: "ACCESO_DENEGADO"
   - Almacenar nivel de permiso requerido y permiso actual

**Dependencias**:
- `java.util.Map`
- `java.util.HashMap`

**Notas de Implementación**:
- Ambas excepciones deben ser excepciones de tiempo de ejecución (sin verificar) ya que resultan de violaciones de lógica de negocio
- Las excepciones deben ser inmutables una vez creadas
- Almacenar codigo como constante para fácil referencia en manejadores de excepciones

---

### Paso 2: Crear DownloadDocumentDto

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/DownloadDocumentDto.java` (nuevo)

**Acción**: Crear DTO para encapsular datos de respuesta de descarga separados de la lógica de entidad.

**Firma de Función**:
```java
public record DownloadDocumentDto(
    InputStream stream,
    String filename,
    String extension,
    String mimeType,
    Long sizeBytes
) {}
```

**Pasos de Implementación**:

1. Crear clase record con campos para respuesta de streaming
   - `stream`: InputStream para leer datos del archivo
   - `filename`: Nombre de archivo original para header Content-Disposition
   - `extension`: Extensión del archivo (p. ej., "pdf", "docx")
   - `mimeType`: Tipo MIME (p. ej., "application/pdf")
   - `sizeBytes`: Tamaño del archivo para header Content-Length

2. Usar sintaxis record de Java 14+ para inmutabilidad y constructor/getters automáticos

3. Asegurar manejo adecuado de recursos (stream debe cerrarse después de transmisión)

**Dependencias**:
- `java.io.InputStream`

**Notas de Implementación**:
- Record asegura inmutabilidad y reduce código repetitivo
- Stream no debe cerrarse en DTO; controlador responsable de cerrar después de transmisión
- Campo extensión útil para Content-Disposition y resolución de tipo MIME

---

### Paso 3: Crear Utilidad MimeTypeResolver

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/util/MimeTypeResolver.java` (nuevo)

**Acción**: Crear utilidad para resolver tipos MIME a partir de extensiones de archivo.

**Firma de Función**:
```java
public class MimeTypeResolver {
    public static String getMimeType(String extension)
    public static String getMimeType(String extension, String defaultMimeType)
}
```

**Pasos de Implementación**:

1. Crear método estático `getMimeType(String extension)`
   - Entrada: extensión sin punto (p. ej., "pdf", "docx", "jpg")
   - Retorna: cadena de tipo MIME o por defecto "application/octet-stream"

2. Crear método estático con parámetro de respaldo por defecto
   - Entrada: extensión y tipo MIME por defecto personalizado
   - Retorna: tipo MIME o por defecto proporcionado

3. Construir mapeo de extensiones comunes a tipos MIME:
   - `.pdf` → `application/pdf`
   - `.docx` → `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
   - `.xlsx` → `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
   - `.jpg`, `.jpeg` → `image/jpeg`
   - `.png` → `image/png`
   - `.doc` → `application/msword`
   - `.xls` → `application/vnd.ms-excel`
   - `.txt` → `text/plain`
   - `.csv` → `text/csv`
   - `.zip` → `application/zip`
   - otros → `application/octet-stream` (respaldo)

4. Hacer búsquedas insensibles a mayúsculas (normalizar a minúsculas antes de búsqueda)

**Dependencias**:
- `java.util.Map`
- `java.util.HashMap`
- `java.util.Locale` (para toLowerCase)

**Notas de Implementación**:
- Usar un Map estático final para mapeos de tipo MIME
- Inicializar mapeos usando inicialización de doble llave o inicializador estático
- Entrada de extensión debe tener el punto eliminado antes de búsqueda
- Retornar application/octet-stream para extensiones desconocidas (compatible con RFC 2616)

---

### Paso 4: Crear Interfaz e Implementación del Servicio de Almacenamiento

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/storage/StorageService.java` (interfaz)

**Acción**: Definir contrato para operaciones de almacenamiento (abstracción del sistema de archivos).

**Firma de Función**:
```java
public interface StorageService {
    InputStream download(String storagePath) throws FileStorageException;
    String getStoragePath(UUID documentId, UUID versionId);
}
```

**Pasos de Implementación**:

1. Crear interfaz `StorageService` en paquete de almacenamiento
   - Método: `InputStream download(String storagePath) throws FileStorageException`
     - Entrada: storagePath de tabla version_documento
     - Retorna: InputStream para contenido del archivo
     - Lanza: FileStorageException si archivo no existe
   
   - Método: `String getStoragePath(UUID documentId, UUID versionId)`
     - Entrada: ID de documento e ID de versión
     - Retorna: cadena de ruta de almacenamiento

2. Crear implementación `LocalStorageService` que implementa StorageService
   - Lee archivos del sistema de archivos local (usado en desarrollo)
   - Resuelve ruta desde propiedad de configuración `docflow.storage.basePath`
   - Detalles de implementación:
     - Verificar si archivo existe antes de abrir
     - Lanzar FileStorageException con codigo "ARCHIVO_NO_DISPONIBLE" si no se encuentra
     - Manejar IOException y convertir a FileStorageException
     - Registrar errores apropiadamente

3. Considerar implementación futura de S3/MinIO (interfaz lo soporta)

**Dependencias**:
- Anotación Spring Component
- Anotación Spring ConfigurationProperties o Value
- java.io.File, FileInputStream, FileNotFoundException
- java.util.UUID

**Notas de Implementación**:
- Mantener interfaz genérica para soportar múltiples backends de almacenamiento (local, S3, MinIO)
- Manejo de rutas debe ser seguro (prevenir recorrido de directorios con normalización de ruta)
- Usar try-with-resources para manejo de InputStream en controlador
- LocalStorageService debe validar seguridad de ruta

---

### Paso 5: Extender DocumentRepository

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/DocumentRepository.java`

**Acción**: Añadir método de consulta para recuperar documento con versión actual en uníca operación.

**Firma de Función**:
```java
public interface DocumentRepository {
    Optional<DocumentWithCurrentVersionProjection> findDocumentWithCurrentVersionByIdAndOrgId(
        UUID documentId, 
        UUID organizationId
    );
}

public interface DocumentWithCurrentVersionProjection {
    UUID getDocumentoId();
    UUID getVersionId();
    String getNombreArchivo();
    String getExtension();
    Long getTamaniob();
    String getRutaAlmacenamiento();
    String getEstado();
}
```

**Pasos de Implementación**:

1. Crear interfaz de proyección `DocumentWithCurrentVersionProjection`
   - Campos para datos de documento y versión actual necesarios para descarga
   - Incluir: documento_id, version_id, nombre_archivo, extension, tamanio_bytes, ruta_almacenamiento, estado

2. Añadir método de consulta de repositorio: `findDocumentWithCurrentVersionByIdAndOrgId`
   - Acepta parámetros documentId y organizationId
   - Retorna Optional de proyección
   - Consulta debe:
     - Unir tablas documento y version_documento en version_actual_id
     - Filtrar por documento.documento_id = ? AND documento.organizacion_id = ? AND documento.estado != 'ELIMINADO'
     - Retornar un resultado o Optional vacío

3. Consulta debe usar anotación @Query con JPQL o SQL nativo
   - Ejemplo: `SELECT d.id as documentoId, v.id as versionId, ... FROM documento d JOIN version_documento v ON d.version_actual_id = v.id WHERE d.id = ? AND d.organizacion_id = ? AND d.estado != 'ELIMINADO'`

**Dependencias**:
- Spring Data JPA
- org.springframework.data.jpa.repository.Query
- java.util.Optional
- java.util.UUID

**Notas de Implementación**:
- Proyección mejora rendimiento al seleccionar solo columnas necesarias
- Usar patrón Optional para operaciones seguras contra nulo
- Verificación de estado de documento previene acceso a documentos eliminados suavemente
- Aislamiento de tenant a través de filtro organizacion_id es crítico para seguridad

---

### Paso 6: Crear Método del Servicio de Dominio

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/service/DocumentService.java`

**Acción**: Implementar lógica de negocio para orquestación de descarga de documento.

**Firma de Función**:
```java
public DownloadDocumentDto downloadDocument(
    UUID documentId, 
    UUID userId, 
    UUID organizationId
) throws DocumentDownloadException, FileStorageException
```

**Pasos de Implementación**:

1. Crear método en DocumentService
   - Acepta: documentId, userId (de principal JWT), organizationId (de principal JWT)
   - Retorna: DownloadDocumentDto con stream y metadatos
   - Lanza: DocumentDownloadException para violaciones de lógica de negocio

2. Implementar secuencia de lógica:
   - **Paso 2a - Validar que Documento Existe**:
     - Llamar `documentRepository.findDocumentWithCurrentVersionByIdAndOrgId(documentId, organizationId)`
     - Si no encontrado (Optional.empty()), lanzar DocumentDownloadException con codigo "DOCUMENTO_NO_ENCONTRADO"
     - Almacenar proyección de documento para validación adicional

   - **Paso 2b - Validar Permiso de Lectura**:
     - Llamar `permissionService.hasReadPermission(userId, documentId, organizationId)`
     - Implementación debe verificar:
       - Permiso explícito en documento (de tabla documento_permiso)
       - Permiso heredado de carpeta padre (de tabla carpeta_permiso)
       - Aplicar precedencia: explícito > heredado
       - Nivel de permiso debe ser >= LECTURA (LECTURA or ESCRITURA or ADMINISTRACION)
     - Si no tiene permiso de lectura, lanzar DocumentDownloadException con codigo "ACCESO_DENEGADO"
     - Mensaje: "No tiene permisos de lectura sobre este documento"

   - **Paso 2c - Obtener Versión Actual**:
     - Extraer versionId de proyección de documento
     - Validar version_id no es nulo (verificación defensiva)

   - **Paso 2d - Descargar Archivo**:
     - Obtener storagePath de proyección (ruta_almacenamiento)
     - Llamar `storageService.download(storagePath)`
     - Manejar FileStorageException y convertir a error apropiado

   - **Paso 2e - Resolver Tipo MIME**:
     - Extraer extension de proyección
     - Llamar `MimeTypeResolver.getMimeType(extension)`
     - Retorna cadena de tipo MIME

   - **Paso 2f - Crear DTO de Respuesta**:
     - Construir DownloadDocumentDto con:
       - stream: de servicio de almacenamiento
       - filename: de proyección (nombre_archivo)
       - extension: de proyección
       - mimeType: de MimeTypeResolver
       - sizeBytes: de proyección (tamanio_bytes)

   - **Paso 2g - Emitir Evento de Auditoría**:
     - Crear evento de auditoría: DOCUMENTO_DESCARGADO
     - Payload: { documento_id, version_id, usuario_id, timestamp, tamanio_bytes }
     - Publicar evento vía eventPublisher.publishEvent(new DocumentDownloadedEvent(...))

3. Registrar (log) operaciones:
   - INFO: "Iniciando descarga - documento_id={}, usuario_id={}, version_id={}"
   - ERROR: si archivo no se encuentra o servicio de almacenamiento falla
   - WARN: si permiso denegado

**Dependencies**:
- DocumentRepository (injected)
- PermissionService (injected, may be from gateway or local)
- StorageService (injected)
- MimeTypeResolver (shared utility)
- ApplicationEventPublisher (for audit events)
- Logger (SLF4J)
- Custom exceptions

**Implementation Notes**:
- Domain Service should NOT handle HTTP concerns (that's controller's responsibility)
- All business logic validation should happen here
- Permission check is critical for security
- Tenant isolation enforced at database level AND in queries
- Audit event is mandatory per spec (DO_DOCUMENTO_DESCARGADO)
- Stream resources should be managed by caller; service only returns them

---

### Paso 7: Crear Endpoint del Controlador

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/presentation/controller/DocumentController.java`

**Acción**: Exponer endpoint GET HTTP para descarga de documento con manejo apropiado de respuesta.

**Firma de Función**:
```java
@GetMapping("/{documentId}/download")
public ResponseEntity<StreamingResponseBody> downloadDocument(
    @PathVariable UUID documentId,
    @AuthenticationPrincipal UserPrincipal principal,
    HttpServletResponse response
)
```

**Pasos de Implementación**:

1. Crear método endpoint en DocumentController
   - Ruta: `GET /api/documents/{documentId}/download`
   - Parámetro de ruta: documentId (UUID, requerido)
   - Autenticación: @AuthenticationPrincipal inyectando UserPrincipal desde contexto JWT
   - Extraer userId y organizationId de principal

2. Implementar lógica de respuesta:
   - Llamar `documentService.downloadDocument(documentId, userId, organizationId)`
   - Obtener DownloadDocumentDto del servicio

3. Configurar Headers de Respuesta HTTP:
   - `Content-Type`: establecer desde DownloadDocumentDto.mimeType()
   - `Content-Disposition`: 
     - Formato: `attachment; filename="{nombre_archivo_sanitizado}.{extension}"`
     - Sanitizar nombre de archivo para prevenir inyección de header (eliminar comillas, caracteres de control, saltos de línea)
     - Ejemplo: `attachment; filename="report.pdf"`
   - `Content-Length`: establecer desde DownloadDocumentDto.sizeBytes()
   - `Cache-Control`: establecer a "no-cache, no-store, must-revalidate" para documentos sensibles (opcional, según requerimientos)

4. Stream de Respuesta:
   - Usar StreamingResponseBody para soporte de archivos grandes
   - Implementación:
     ```java
     return ResponseEntity
         .ok()
         .header("Content-Type", mimeType)
         .header("Content-Disposition", dispositionHeader)
         .header("Content-Length", String.valueOf(sizeBytes))
         .body(outputStream -> {
             try (InputStream input = downloadDto.stream()) {
                 byte[] buffer = new byte[8192]; // Buffer de 8KB
                 int bytesRead;
                 while ((bytesRead = input.read(buffer)) != -1) {
                     outputStream.write(buffer, 0, bytesRead);
                 }
             }
         });
     ```

5. Manejo de Recursos:
   - Asegurar que InputStream se cierre apropiadamente con try-with-resources
   - StreamingResponseBody maneja cierre de output stream

6. Añadir Anotaciones OpenAPI/Swagger:
   - @Operation(summary = "Descargar versión actual de un documento")
   - @ApiResponse(responseCode = "200", description = "Descarga de archivo exitosa")
   - @ApiResponse(responseCode = "403", description = "Permiso insuficiente")
   - @ApiResponse(responseCode = "404", description = "Documento no encontrado")
   - @ApiResponse(responseCode = "500", description = "Error de almacenamiento")

7. Logging:
   - Registrar inicio de descarga a nivel DEBUG con documentId y userId
   - Registrar finalización exitosa a nivel INFO con nombre de archivo y tamaño
   - Registrar errores a nivel ERROR con detalles de excepción

**Dependencias**:
- Spring Web (RestController, GetMapping, PathVariable, RequestMapping)
- Spring Security (AuthenticationPrincipal)
- Spring Framework (ResponseEntity, StreamingResponseBody)
- Jakarta Servlet (HttpServletResponse)
- DocumentService (inyectado)
- Logger (SLF4J)
- Anotaciones OpenAPI (springdoc-openapi)

**Notas de Implementación**:
- Sanitización de nombre de archivo previene ataques de inyección de header HTTP
- StreamingResponseBody habilita codificación de transferencia en fragmentos para archivos grandes
- Tamaño de buffer de 8-16 KB es óptimo para rendimiento de red
- Nunca exponer ruta completa de archivo al cliente; usar solo nombre de archivo original
- Try-with-resources asegura que InputStream se cierre incluso en excepción
- No usar ResponseEntity.ok().body(resource) para archivos grandes; preferir streaming

---

### Paso 8: Crear Manejador de Excepciones Global

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/exception/DocumentControllerAdvice.java` (o extender GlobalExceptionHandler existente)

**Acción**: Manejar excepciones personalizadas y convertir a respuestas de error RFC 7807 ProblemDetail.

**Firma de Función**:
```java
@ExceptionHandler(DocumentDownloadException.class)
public ResponseEntity<ProblemDetail> handleDocumentDownloadException(
    DocumentDownloadException ex,
    HttpServletRequest request
)

@ExceptionHandler(FileStorageException.class)
public ResponseEntity<ProblemDetail> handleFileStorageException(
    FileStorageException ex,
    HttpServletRequest request
)
```

**Pasos de Implementación**:

1. Crear manejador de excepciones para `DocumentDownloadException`
   - Determinar estado HTTP basado en error codigo:
     - `ACCESO_DENEGADO` → 403 Forbidden (permiso denegado)
     - `DOCUMENTO_NO_ENCONTRADO` → 404 Not Found
     - Otros errores de lógica de negocio → 400 Bad Request
   
   - Construir respuesta ProblemDetail:
     ```java
     var problem = ProblemDetail.forStatusAndDetail(
         HttpStatus.FORBIDDEN,
         ex.getMessage()
     );
     problem.setType(URI.create("https://docflow.com/errors/acceso-denegado"));
     problem.setTitle("Acceso Denegado");
     problem.setProperty("codigo", ex.getCodigo());
     problem.setProperty("detalles", ex.getDetalles());
     problem.setInstance(URI.create(request.getRequestURI()));
     ```
   
   - Registrar advertencia con documento_id y usuario_id de detalles para auditoría

2. Crear manejador de excepciones para `FileStorageException`
   - Estado HTTP: 500 Internal Server Error
   - Construir ProblemDetail con codigo "ARCHIVO_NO_DISPONIBLE"
   - Incluir version_id en detalles para debugging
   - Registrar ERROR con traza de pila completa

3. Usar @RestControllerAdvice o @ControllerAdvice con basePackages específicos
   - Aplicar a paquete: `com.docflow.documentcore.presentation`

4. Seguir patrón de respuesta de error existente del proyecto
   - Coincidir formato usado en manejo de excepciones de AuthenticationController
   - Usar URI.create() para campos type e instance

**Dependencias**:
- Spring Web (ExceptionHandler, ControllerAdvice, RestControllerAdvice)
- Spring Framework (ProblemDetail, ResponseEntity, HttpStatus)
- Jakarta Servlet (HttpServletRequest)
- java.net.URI
- Logger (SLF4J)

**Notas de Implementación**:
- ProblemDetail (RFC 7807) es estándar para respuestas de error REST
- HTTP 403 para permiso denegado, NUNCA 404 (prevenir divulgación de información per spec)
- Siempre incluir URI de solicitud en campo instance para debugging
- Almacenar detalles de error extendidos en propiedad "detalles" como Map
- Coincidir formato de error existente del servicio de identity para consistencia
- Registrar a niveles apropiados: WARN para errores esperados, ERROR para inesperados

---

### Paso 9: Añadir Integración del Servicio de Permisos

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/service/PermissionService.java` (o interfaz si se llama a servicio externo)

**Acción**: Integrar con sistema ACL para validar permisos de lectura.

**Firma de Función**:
```java
public boolean hasReadPermission(UUID userId, UUID documentId, UUID organizationId)
public NivelAcceso getEffectivePermission(UUID userId, UUID documentId, UUID organizationId)
```

**Pasos de Implementación**:

1. Crear o extender PermissionService
   - Puede ser implementación local o llamada a servicio gateway/identity
   - Método: `hasReadPermission(userId, documentId, organizationId)`
     - Consultar tabla documento_permiso para permiso explícito
     - Si se encuentra y nivel_acceso >= LECTURA, retornar true
     - Consultar carpeta padre (carpeta contenedora) para permiso heredado
     - Caminar jerarquía de carpetas hasta permiso encontrado o raíz alcanzada
     - Retornar true si hay permiso heredado >= LECTURA
     - Retornar false si no hay permiso en ningún nivel

   - Método: `getEffectivePermission(userId, documentId, organizationId)`
     - Retorna valor enum NivelAcceso (LECTURA, ESCRITURA, ADMINISTRACION)
     - Usado para logging de auditoría y mensajes de error detallados

2. Aplicar reglas de precedencia de permisos:
   - Permiso explícito de documento > Permiso heredado de carpeta
   - Nivel de acceso superior (ADMINISTRACION > ESCRITURA > LECTURA) se antepone

3. Manejar casos especiales:
   - Dueño siempre tiene permiso (si se rastrean en dominio)
   - Usuarios admin pueden omitir verificaciones de permiso (si aplica)
   - Documentos eliminados suavemente deben ser inaccesibles

4. Logging para auditoría:
   - Registrar verificación de permiso a nivel DEBUG
   - Registrar permiso denegado a nivel WARN (para pista de auditoría)

**Dependencias**:
- Enum NivelAcceso (modelo de dominio)
- Repositorios para consultas de documento_permiso y carpeta_permiso
- Logger

**Notas de Implementación**:
- Puede integrarse con tickets ACL existentes (US-ACL-004, etc.)
- Considerar caching de búsquedas de permiso si muchos recorridos ocurren
- Documentar suposiciones sobre jerarquía de carpetas e herencia de permisos

---

### Paso 10: Crear Evento de Auditoría

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/event/DocumentDownloadedEvent.java` (nuevo)

**Acción**: Definir evento de dominio para pista de auditoría de descarga de documento.

**Firma de Función**:
```java
public class DocumentDownloadedEvent extends ApplicationEvent {
    private final UUID documentoId;
    private final UUID versionId;
    private final UUID usuarioId;
    private final UUID organizacionId;
    private final Long tamaniobytes;
    private final LocalDateTime timestamp;
}
```

**Pasos de Implementación**:

1. Crear clase evento que extiende ApplicationEvent
   - Almacenar payload de evento: documentoId, versionId, usuarioId, organizacionId, tamaniobytes, timestamp
   
2. Añadir constructor:
   - Acepta source (DocumentService o similar)
   - Acepta todos campos de datos de evento
   - Llamar super() con source
   - Almacenar todos campos como final private

3. Añadir getters para todos campos (sin setters para inmutabilidad)

4. Opcional: Añadir toString() para logging

**Dependencias**:
- org.springframework.context.ApplicationEvent
- java.util.UUID
- java.time.LocalDateTime

**Notas de Implementación**:
- ApplicationEvent habilita listeners asíncrono vía @EventListener
- Evento debe publicarse después de descarga exitosa para evitar auditorías parciales
- Futuro: puede ser consumido por servicio de auditoría para logging persistente

---

### Paso 11: Configurar Propiedades (si es necesario)

**Archivo**: `backend/document-core/src/main/resources/application.yml`

**Acción**: Añadir propiedades de configuración para rutas de almacenamiento y valores por defecto de tipo MIME.

**Propiedades a Añadir**:
```yaml
docflow:
  storage:
    basePath: ${STORAGE_BASE_PATH:./storage}
    maxFileSize: 104857600  # 100MB en bytes
  download:
    bufferSize: 8192  # 8KB
    mimeTypeDefault: application/octet-stream
```

**Pasos de Implementación**:

1. Añadir propiedad `docflow.storage.basePath`
   - Leer desde variable de entorno `STORAGE_BASE_PATH`
   - Por defecto: `./storage` para desarrollo local
   - Usado por LocalStorageService

2. Añadir propiedad `docflow.download.bufferSize`
   - Por defecto 8192 (8KB)
   - Usado en implementación de StreamingResponseBody

3. Estas pueden ser inyectadas vía @Value o @ConfigurationProperties

**Dependencias**:
- Configuración Spring Boot

**Notas de Implementación**:
- Mantener valores por defecto adecuados para desarrollo
- Configuración de producción debe venir de variables de entorno
- Ruta debe ser normalizada para seguridad (sin recorrido de directorios)

---

### Paso 12: Escribir Tests Unitarios

**Archivo**: `backend/document-core/src/test/java/com/docflow/documentcore/domain/service/DocumentServiceTest.java`

**Acción**: Crear tests unitarios exhaustivos para lógica de negocio de descarga de documento.

**Categorías de Tests**:

#### Casos Exitosos
1. **Test Download Success With Valid Permission**:
   - Dado: Documento existe, usuario tiene permiso LECTURA
   - Cuando: downloadDocument() es llamado
   - Entonces: Retorna DownloadDocumentDto con stream y metadatos válidos
   - Verificar: stream no-nulo, nombre de archivo correcto, mimeType resuelto

2. **Test Download With Inherited Permission**:
   - Dado: Documento existe, usuario tiene LECTURA heredado de carpeta padre
   - Cuando: downloadDocument() es llamado
   - Entonces: Retorna DownloadDocumentDto exitosamente
   - Verificar: Permiso heredado aceptado

3. **Test Download With Higher Permission Level**:
   - Dado: Documento existe, usuario tiene permiso ESCRITURA (superior a LECTURA)
   - Cuando: downloadDocument() es llamado
   - Entonces: Retorna DownloadDocumentDto exitosamente

#### Errores de Validación de Permisos
4. **Test Download Denied Without Permission**:
   - Dado: Documento existe, usuario sin permiso
   - Cuando: downloadDocument() es llamado
   - Entonces: Lanza DocumentDownloadException con codigo "ACCESO_DENEGADO"
   - Verificar: Mensaje de excepción incluye requisito de permiso

5. **Test Download Denied With Explicit Denial**:
   - Dado: Documento existe, usuario tiene permiso explícito NINGUNO (si aplica)
   - Cuando: downloadDocument() es llamado
   - Entonces: Lanza DocumentDownloadException con codigo "ACCESO_DENEGADO"

#### Errores de Validación de Documento
6. **Test Document Not Found**:
   - Dado: ID de documento no existe en base de datos
   - Cuando: downloadDocument() es llamado
   - Entonces: Lanza DocumentDownloadException con codigo "DOCUMENTO_NO_ENCONTRADO"
   - Verificar: Sin divulgación de información sensible

7. **Test Deleted Document Not Accessible**:
   - Dado: Documento existe pero estado = 'ELIMINADO'
   - Cuando: downloadDocument() es llamado
   - Entonces: Lanza DocumentDownloadException con codigo "DOCUMENTO_NO_ENCONTRADO"

8. **Test Tenant Isolation**:
   - Dado: Documento existe en organización diferente
   - Cuando: downloadDocument() es llamado con organizationId diferente
   - Entonces: Lanza DocumentDownloadException con codigo "DOCUMENTO_NO_ENCONTRADO"

#### Errores de Almacenamiento de Archivos
9. **Test File Not Found In Storage**:
   - Dado: Documento existe pero archivo ausente de almacenamiento
   - Cuando: downloadDocument() es llamado
   - Entonces: Lanza FileStorageException con codigo "ARCHIVO_NO_DISPONIBLE"
   - Verificar: Incluye version_id en detalles

10. **Test Storage Service IO Error**:
    - Dado: Servicio de almacenamiento lanza IOException
    - Cuando: downloadDocument() es llamado
    - Entonces: Lanza FileStorageException con detalles de error apropiados

#### Eventos de Auditoría
11. **Test Audit Event Emitted on Success**:
    - Dado: Documento descargado exitosamente
    - Cuando: downloadDocument() se completa
    - Entonces: ApplicationEventPublisher.publishEvent() llamado con DocumentDownloadedEvent
    - Verificar: Evento contiene documento_id, usuario_id, version_id correctos

#### Casos Especiales
12. **Test With Null Extension**:
    - Dado: Versión no tiene extensión
    - Cuando: downloadDocument() es llamado
    - Entonces: MimeTypeResolver usa por defecto application/octet-stream

13. **Test With Very Large File Size**:
    - Dado: Tamaño de documento es 100MB
    - Cuando: downloadDocument() es llamado
    - Entonces: Retorna DownloadDocumentDto con tamaño correcto
    - Stream no cargado en memoria

14. **Test Logging Output**:
    - Dado: Operación de descarga en progreso
    - Cuando: downloadDocument() es llamado
    - Entonces: Logs incluyen documento_id, usuario_id, version_id en niveles apropiados

**Estrategia de Mocking**:
- Mock DocumentRepository.findDocumentWithCurrentVersionByIdAndOrgId()
- Mock PermissionService.hasReadPermission()
- Mock StorageService.download() retornando ByteArrayInputStream
- Mock ApplicationEventPublisher
- Usar Mockito para mocking y verificación

**Framework de Testing**:
- JUnit 5 (Jupiter)
- Mockito para mocking
- AssertJ para aserciones

**Notas de Implementación**:
- Apuntar a >80% cobertura de tests según estándares
- Testear rutas de éxito y de error
- Verificar detalles de excepción (campos codigo, detalles)
- Usar fixtures/builders para datos de test complejos
- Testear logging vía @ExtendWith(MockingExtension) o appender de test Logback

---

### Paso 13: Escribir Tests de Integración

**Archivo**: `backend/document-core/src/test/java/com/docflow/documentcore/presentation/controller/DocumentControllerIntegrationTest.java`

**Acción**: Testear comportamiento completo de endpoints HTTP con contexto Spring Test.

**Categorías de Tests**:

#### Ruta Feliz
1. **Test Download Endpoint Returns 200 With File Stream**:
   - Dado: Token JWT válido, documento existe, usuario tiene permiso LECTURA
   - Cuando: GET /api/documents/{documentId}/download es llamado
   - Entonces: Retorna HTTP 200 OK
   - Verificar: 
     - Header Content-Type coincide con tipo MIME
     - Header Content-Disposition presente con nombre de archivo
     - Header Content-Length presente
     - Cuerpo de respuesta es contenido de archivo (puede verificar tamaño)

2. **Test Response Headers Are Correct**:
   - Cuando: Endpoint de descarga es llamado
   - Entonces: Verificar todos headers requeridos:
     - Content-Type: application/pdf (ejemplo)
     - Content-Disposition: attachment; filename="document.pdf"
     - Content-Length: 102400
     - Sin Cache-Control o Cache-Control: no-cache

#### Errores de Permisos (403)
3. **Test Download Returns 403 Without Read Permission**:
   - Dado: Usuario carece de permiso LECTURA
   - Cuando: GET /api/documents/{documentId}/download es llamado
   - Entonces: Retorna HTTP 403 Forbidden
   - Verificar: Respuesta ProblemDetail con codigo "ACCESO_DENEGADO"

4. **Test 403 Response Body Format**:
   - Cuando: Descarga denegada por permiso
   - Entonces: Respuesta incluye:
     - status: 403
     - codigo: "ACCESO_DENEGADO"
     - message: "No tiene permisos de lectura..."
     - detalles con documento_id, permiso_requerido

#### Errores No Encontrado (404)
5. **Test Download Returns 404 For Missing Document**:
   - Dado: ID de documento no existe
   - Cuando: GET /api/documents/{documentId}/download es llamado
   - Entonces: Retorna HTTP 404 Not Found
   - Verificar: ProblemDetail con codigo "DOCUMENTO_NO_ENCONTRADO"

6. **Test 404 Prevents Information Disclosure**:
   - Dado: Documento existe en organización diferente
   - Cuando: GET /api/documents/{documentId}/download es llamado con token de org diferente
   - Entonces: Retorna HTTP 404 (no 403) para ocultar existencia de documento

#### Errores de Servidor (500)
7. **Test Download Returns 500 For Missing File**:
   - Dado: Documento existe pero archivo ausente de almacenamiento
   - Cuando: GET /api/documents/{documentId}/download es llamado
   - Entonces: Retorna HTTP 500 Internal Server Error
   - Verificar: ProblemDetail con codigo "ARCHIVO_NO_DISPONIBLE"

#### Errores de Autenticación (401)
8. **Test Download Returns 401 Without Token**:
   - Dado: Sin token JWT proporcionado
   - Cuando: GET /api/documents/{documentId}/download es llamado sin header Authorization
   - Entonces: Retorna HTTP 401 Unauthorized
   - Framework debe manejar (SpringSecurity)

9. **Test Download Returns 401 With Invalid Token**:
   - Dado: Token JWT malformado o expirado
   - Cuando: GET /api/documents/{documentId}/download es llamado
   - Entonces: Retorna HTTP 401 Unauthorized

#### Manejo de Archivos Grandes
10. **Test Streaming Response For Large File**:
    - Dado: Documento es 100MB
    - Cuando: GET /api/documents/{documentId}/download es llamado
    - Entonces: Respuesta usa codificación de transferencia en fragmentos
    - Cliente recibe todos 100MB sin sobrecarga de memoria

#### Sanitización de Nombre de Archivo
11. **Test Filename Sanitization In Content-Disposition**:
    - Dado: Nombre de archivo contiene caracteres especiales: `report"2024'.pdf`
    - Cuando: Endpoint de descarga es llamado
    - Entonces: Valor de Content-Disposition está sanitizado
    - Verificar: Sin comillas sin escapar o saltos de línea que puedan inyectar headers

#### Múltiples Tipos de Archivo
12. **Test PDF Download**:
    - Cuando: Descarga archivo .pdf
    - Entonces: Content-Type: application/pdf

13. **Test Word Document Download**:
    - Cuando: Descarga archivo .docx
    - Entonces: Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document

14. **Test Unknown Extension**:
    - Cuando: Descarga archivo con extensión desconocida
    - Entonces: Content-Type: application/octet-stream

**Configuración de Test**:
- Usar @SpringBootTest para contexto completo
- Usar @AutoConfigureMockMvc para MockMvc
- Mock servicios externos (StorageService, PermissionService) si es necesario
- Usar TestRestTemplate o MockMvc para solicitudes HTTP
- Usar @TestDatabase o H2 para base de datos de test
- Usar generador de token JWT fixture para solicitudes autenticadas

**Datos de Test**:
- Crear fixtures de test para entidades Document, DocumentVersion
- Pre-popular base de datos de test con documentos y versiones de muestra
- Crear archivos de test en ubicación de almacenamiento temporal

**Verificación**:
- Aserta códigos de estado HTTP
- Verificar headers de respuesta
- Verificar cuerpo/contenido de respuesta (puede usar FilesystemAssertions)
- Verificar estado de base de datos sin cambios (descargas son de solo lectura)
- Verificar eventos de auditoría publicados

**Notas de Implementación**:
- Tests deben ser independientes (sin dependencias de ordenamiento de tests)
- Usar @Transactional para reversión de cambios después de cada test
- Mock servicios externos para aislar testing de document-core
- Testear comportamiento de cliente (descarga de archivo grande, recuperación de interrupción de red)

---

### Paso 14: Actualizar Documentación

**Archivo**: `ai-specs/specs/api-spec.yml`

**Acción**: Documentar endpoint de descarga en especificación OpenAPI.

**Pasos de Implementación**:

1. Añadir endpoint a especificación OpenAPI
   - Ruta: `/documents/{documentId}/download`
   - Método: GET
   - Resumen: "Descargar versión actual de un documento"
   - Descripción: "Permite a usuarios con permiso de LECTURA descargar la versión actual del documento"
   - Etiquetas: ["Documents"]

2. Documentar parámetros:
   - documentId (parámetro de ruta, UUID, requerido)
   - Header Authorization (token JWT, requerido)

3. Documentar respuesta exitosa (200):
   - Content-Type: binario de archivo
   - Headers: Content-Type, Content-Disposition, Content-Length
   - Descripción: "Contenido de archivo de versión actual"

4. Documentar respuestas de error:
   - 401 Unauthorized: Sin token JWT válido
   - 403 Forbidden: Permiso insuficiente LECTURA
   - 404 Not Found: Documento no encontrado o no pertenece a organización
   - 500 Internal Server Error: Archivo no disponible en almacenamiento

5. Añadir ejemplos:
   - Ejemplo exitoso con descarga de PDF de muestra
   - Ejemplos de error para cada código de error

6. Definir componentes/esquemas para respuestas de error (formato ProblemDetail)

**Formato de Documentación**:
- Usar formato OpenAPI 3.1.0
- Seguir estilo de documentación API existente del proyecto
- Incluir esquemas de seguridad (token Bearer)

**Actualizar Documentación Secundaria**:
- Actualizar sección document-core en README.md si es necesario
- Añadir endpoint a resumen de API en documentación principal

**Referencias**:
- Seguir estructura en [api-spec.yml](../specs/api-spec.yml)
- Ejemplos de [documentation-standards.mdc](../specs/documentation-standards.mdc)

---

### Paso 15: Revisión de Código y Testing

**Acción**: Preparar código para revisión y asegurar que todos los tests pasen.

**Pasos de Implementación**:

1. **Ejecutar Todos los Tests**:
   ```bash
   cd backend/document-core
   mvn clean test
   ```
   - Verificar que todos los tests unitarios pasen
   - Verificar >80% cobertura de código para código implementado
   - Verificar sin errores de logging ni advertencias

2. **Análisis Estático**:
   ```bash
   mvn spotbugs:check
   mvn pmd:check
   ```
   - Corregir cualquier problema detectado
   - Asegurar que estándares de calidad de código se cumplan

3. **Construir Proyecto**:
   ```bash
   mvn clean package
   ```
   - Verificar sin errores de compilación
   - Verificar que JAR se construya exitosamente

4. **Testing Manual**:
   - Iniciar aplicación: `mvn spring-boot:run`
   - Testear endpoint con token válido y permisos usando curl o Postman:
     ```bash
     curl -H "Authorization: Bearer {jwt-token}" \
          http://localhost:8082/api/documents/{document-id}/download \
          -o downloaded-file.pdf
     ```
   - Verificar que archivo se descargue correctamente
   - Testear escenarios de error (sin token, sin permiso, documento faltante)
   - Verificar que headers de respuesta sean correctos
   - Verificar integridad y tamaño de archivo

5. **Preparar Commit de Git**:
   ```bash
   git status
   git add -A
   git commit -m "feat: implement document download endpoint (US-DOC-002)

   - Add DocumentController.downloadDocument() endpoint
   - Implement permission validation for LECTURA level
   - Add file streaming support with proper MIME type detection
   - Create exception handlers for error responses
   - Add comprehensive unit and integration tests
   - Update OpenAPI documentation
   - Emit audit events for document downloads
   
   Closes #US-DOC-002"
   ```

---

## Orden de Implementación

1. Paso 0: Crear Rama de Característica
2. Paso 1: Crear Excepciones Personalizadas
3. Paso 2: Crear DownloadDocumentDto
4. Paso 3: Crear Utilidad MimeTypeResolver
5. Paso 4: Crear Interfaz e Implementación del Servicio de Almacenamiento
6. Paso 5: Extender DocumentRepository
7. Paso 6: Crear Método del Servicio de Dominio
8. Paso 7: Crear Endpoint del Controlador
9. Paso 8: Crear Manejador de Excepciones Global
10. Paso 9: Añadir Integración del Servicio de Permisos
11. Paso 10: Crear Evento de Auditoría
12. Paso 11: Configurar Propiedades
13. Paso 12: Escribir Tests Unitarios
14. Paso 13: Escribir Tests de Integración
15. Paso 14: Actualizar Documentación
16. Paso 15: Revisión de Código y Testing

---

## Lista de Verificación de Testing

### Tests Unitarios
- [ ] DocumentService.downloadDocument() con permiso válido
- [ ] DocumentService.downloadDocument() con permiso heredado
- [ ] DocumentService.downloadDocument() sin permiso
- [ ] DocumentService.downloadDocument() con documento faltante
- [ ] DocumentService.downloadDocument() con archivo faltante
- [ ] MimeTypeResolver resuelve extensiones comunes correctamente
- [ ] MimeTypeResolver usa por defecto application/octet-stream
- [ ] DocumentDownloadException almacena codigo y detalles
- [ ] FileStorageException almacena codigo y detalles

### Tests de Integración
- [ ] GET /api/documents/{documentId}/download retorna 200 con archivo
- [ ] Headers de respuesta: Content-Type, Content-Disposition, Content-Length
- [ ] 403 Forbidden cuando usuario carece de permiso LECTURA
- [ ] Respuesta 403 incluye código de error ACCESO_DENEGADO
- [ ] 404 Not Found para documento faltante
- [ ] 404 Not Found para documento en organización diferente (aislamiento de tenant)
- [ ] 500 Internal Server Error cuando archivo falta de almacenamiento
- [ ] Sanitización de nombre de archivo previene inyección de header
- [ ] Streaming funciona para archivos grandes (100MB+)
- [ ] Evento de auditoría publicado en descarga exitosa
- [ ] 401 Unauthorized sin token JWT válido

### Tests No-Funcionales
- [ ] Los logs incluyen documento_id, usuario_id, version_id
- [ ] No hay información sensible en respuestas 404
- [ ] Stream se cierra apropiadamente después de transmisión
- [ ] No hay desbordamiento de memoria con archivos grandes

---

## Formato de Respuesta de Error

### 403 Denegado (Permiso Denegado)
```json
{
  "type": "https://docflow.com/errors/acceso-denegado",
  "title": "Acceso Denegado",
  "status": 403,
  "detail": "No tiene permisos de lectura sobre este documento",
  "instance": "/api/documents/{uuid}/download",
  "codigo": "ACCESO_DENEGADO",
  "detalles": {
    "documento_id": "uuid",
    "permiso_requerido": "LECTURA",
    "permiso_actual": "NINGUNO"
  }
}
```

### 404 No Encontrado (Documento No Encontrado)
```json
{
  "type": "https://docflow.com/errors/documento-no-encontrado",
  "title": "Documento No Encontrado",
  "status": 404,
  "detail": "El documento solicitado no existe o no pertenece a su organización",
  "instance": "/api/documents/{uuid}/download",
  "codigo": "DOCUMENTO_NO_ENCONTRADO",
  "detalles": {
    "documento_id": "uuid"
  }
}
```

### 500 Error Interno del Servidor (Archivo No Disponible)
```json
{
  "type": "https://docflow.com/errors/archivo-no-disponible",
  "title": "Archivo No Disponible",
  "status": 500,
  "detail": "El archivo del documento no está disponible en el almacenamiento",
  "instance": "/api/documents/{uuid}/download",
  "codigo": "ARCHIVO_NO_DISPONIBLE",
  "detalles": {
    "documento_id": "uuid",
    "version_id": "uuid"
  }
}
```

### 401 No Autorizado (Token Inválido/Faltante)
```json
{
  "type": "https://docflow.com/errors/unauthorized",
  "title": "No Autorizado",
  "status": 401,
  "detail": "Token JWT inválido o expirado",
  "instance": "/api/documents/{uuid}/download",
  "codigo": "TOKEN_AUSENTE_O_INVALIDO"
}
```

---

## Dependencias

### Librerías Externas (Ya en el Proyecto)
- Spring Boot 3.5.x
- Spring Data JPA
- Spring Security (manejo de JWT)
- Lombok (getters/setters automáticos)
- MapStruct (mapeo de DTO, si se usa)
- Jakarta Servlet API
- SLF4J (logging)

### Nuevas Dependencias (si es necesario)
- Ninguna requerida; toda funcionalidad usa dependencias existentes del proyecto

### Dependencias Internas
- DocumentRepository (Spring Data JPA)
- PermissionService (puede ser servicio gateway o local)
- StorageService (nueva interfaz)
- ApplicationEventPublisher (Spring Framework)
- MimeTypeResolver (nueva utilidad)

---

## Notas

### Recordatorios Importantes
- **Aislamiento de Tenant**: Siempre filtrar por `organizacion_id` en consultas de repositorio
- **Precedencia de Permisos**: Permisos explícitos > Permisos heredados; Nivel superior > Nivel inferior
- **Seguridad**: Retornar 404 para documentos faltantes, nunca 403 para prevenir divulgación de información sobre existencia de documento
- **Sanitización de Nombre de Archivo**: Eliminar saltos de línea, comillas, y caracteres de control de nombres de archivo en Content-Disposition
- **Manejo de Stream**: Siempre cerrar InputStreams con try-with-resources
- **Requisito de Auditoría**: Debe emitir evento DOCUMENTO_DESCARGADO
- **Tamaño de Archivo**: Soportar archivos hasta 100MB con streaming (sin sobrecarga de memoria)

### Reglas de Negocio
- Solo usuarios con permiso mínimo LECTURA pueden descargar
- Permisos ESCRITURA y ADMINISTRACION incluyen LECTURA
- Documentos eliminados suavemente (estado = 'ELIMINADO') no deben ser accesibles
- La descarga no debe modificar estado del documento
- Cada intento de descarga debe ser registrado para pista de auditoría

### Convenciones de Lenguaje y Nombres
- Usar Inglés para código (clases, métodos, variables)
- Usar Español para mensajes de error y texto dirigido al usuario
- Usar camelCase para nombres de variable/método
- Usar PascalCase para nombres de clase
- Usar UPPER_SNAKE_CASE para constantes

### Referencias
- [Backend Standards](../specs/backend-standards.md)
- [API Specification](../specs/api-spec.yml)
- [Data Model](../specs/data-model.md)
- [Error Codes Documentation](../../backend/identity/docs/error-codes.md)
- Tickets relacionados: US-ACL-004 (Resolución de Permisos), US-ACL-006 (Evaluación de Permisos)

---

## Próximos Pasos Después de la Implementación

1. **Revisión de Código**: Enviar PR enfocado en rama `develop`
2. **Testing de Integración**: Probar con tokens JWT reales y permisos
3. **Testing de Rendimiento**: Verificar que streaming funciona con archivos grandes
4. **Despliegue**: Una vez fusionado a develop, desplegar a ambiente de staging
5. **Integración Frontend**: Equipo FE implementa UI de descarga consumiendo este endpoint
6. **Monitoreo**: Configurar logs y métricas para operaciones de descarga
7. **Características Relacionadas**: Implementar carga de documento (US-DOC-001) y gestión de versión si aún no está hecho

---

## Verificación de Implementación

### Lista de Verificación Final

#### Calidad de Código
- [ ] Todo código sigue convenciones de backend-standards.md
- [ ] Sin valores codificados; toda configuración vía propiedades
- [ ] Manejo de errores apropiado con excepciones personalizadas
- [ ] Logging en niveles apropiados (DEBUG, INFO, WARN, ERROR)
- [ ] Sin código comentado
- [ ] Imports organizadas y limpias

#### Funcionalidad
- [ ] Endpoint retorna 200 con stream de archivo en éxito
- [ ] Todos headers HTTP correctos (Content-Type, Content-Disposition, Content-Length)
- [ ] Validación de permisos funcionando (respuesta 403)
- [ ] Documento no encontrado retorna 404
- [ ] Archivo no disponible retorna 500
- [ ] Aislamiento de tenant aplicado
- [ ] Archivos grandes streaming sin problemas de memoria

#### Testing
- [ ] Tests unitarios >80% cobertura en DocumentService
- [ ] Tests de integración cubren todos escenarios éxito/error
- [ ] Todos tests pasando (`mvn clean test`)
- [ ] Sin advertencias ni fallos en tests
- [ ] Testing manual completado con archivos reales

#### Integración
- [ ] Manejadores de excepciones mapean errores a respuestas correctamente
- [ ] Eventos de auditoría publicados y pueden ser consumidos
- [ ] Integración servicio de permisos funcionando
- [ ] Servicio de almacenamiento funcionando con backends local/S3
- [ ] Autenticación JWT integrada apropiadamente

#### Documentación
- [ ] Spec OpenAPI actualizada con endpoint
- [ ] Todas respuestas de error documentadas
- [ ] Comentarios de código en lógica compleja
- [ ] README actualizado si es necesario
- [ ] Ejemplos de comandos curl en documentación

#### Seguridad
- [ ] Sanitización de nombre de archivo previene inyección de header
- [ ] Sin información sensible en respuestas 404
- [ ] Aislamiento de tenant verificado
- [ ] Verificaciones de permisos no pueden ser evitadas
- [ ] Recursos stream se cierran apropiadamente

#### Rendimiento
- [ ] Implementación streaming usa buffering (8-16KB)
- [ ] Sin carga de archivo completo en memoria
- [ ] Headers de respuesta enviados antes de cuerpo
- [ ] Códigos de estado HTTP apropiados para comportamiento de caché

---

**Estado del Plan**: LISTO PARA IMPLEMENTACIÓN

Este plan proporciona guía paso-a-paso para implementar el endpoint de descarga de documento siguiendo principios de Diseño Dirigido por Dominio, mejores prácticas de seguridad, y convenciones establecidas del proyecto. Todos los pasos son discretos y pueden ser implementados secuencialmente. Las dependencias entre pasos están claras y documentadas.
