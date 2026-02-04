# Plan de Implementación Backend: US-FOLDER-003 - Mover Documento a Otra Carpeta (API)

## Resumen

Este ticket implementa un endpoint PATCH para mover un documento entre carpetas dentro de la misma organización. La implementación sigue los principios de Domain-Driven Design (DDD) y Clean Architecture, asegurando operaciones atómicas, validación dual de permisos (carpetas origen y destino), y emisión de eventos de auditoría.

**Requisitos Clave:**
- Validar permiso de ESCRITURA en carpetas origen y destino
- Asegurar transacción atómica (actualización documento + evento auditoría)
- Aislamiento multi-tenant (basado en organización)
- Retornar códigos HTTP apropiados (200, 400, 403, 404)
- Emitir evento de auditoría `DOCUMENTO_MOVIDO`

**Capas de Arquitectura:**
- **Capa de Dominio**: Reglas de negocio, lógica de validación, excepciones
- **Capa de Aplicación**: Orquestación de servicios, mapeo de DTOs, gestión de transacciones
- **Capa de Infraestructura**: Controlador REST, repositorios JPA, emisión de eventos de auditoría

## Contexto de Arquitectura

### Capas Involucradas
1. **Capa de Dominio** (`domain/`):
   - Modelo: `Documento.java` (entidad con campo `carpetaId`)
   - Excepciones: `DocumentoNotFoundException`, `CarpetaNotFoundException`, excepciones de negocio personalizadas
   - Interfaz de Servicio: `IEvaluadorPermisos` (evaluador de permisos existente)

2. **Capa de Aplicación** (`application/`):
   - Servicio: `DocumentoMoverService` (nuevo servicio para operación de movimiento)
   - DTOs: `MoverDocumentoRequest`, `DocumentoMovidoResponse`
   - Mapper: DTOs ↔ Entidades de dominio

3. **Capa de Infraestructura** (`infrastructure/adapter/`):
   - Controlador: `DocumentoController` (agregar endpoint PATCH)
   - Repositorio: Usar `IDocumentoRepository` existente
   - Auditoría: Integración con servicio de auditoría (P5)

### Componentes Clave Referenciados
- `IEvaluadorPermisos`: Servicio existente para validación de permisos
- `ICarpetaRepository`: Validar existencia de carpeta
- `IDocumentoRepository`: Obtener y actualizar documento
- Publicador de Eventos de Auditoría: Emitir evento `DOCUMENTO_MOVIDO`

## Pasos de Implementación

### Paso 1: Crear DTO de Solicitud

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/MoverDocumentoRequest.java`

**Acción**: Crear DTO para solicitud de mover documento con validación

**Pasos de Implementación**:

1. **Create DTO Class**:
   ```java
   package com.docflow.documentcore.application.dto;
   
   import com.fasterxml.jackson.annotation.JsonProperty;
   import jakarta.validation.constraints.NotNull;
   import jakarta.validation.constraints.Positive;
   import io.swagger.v3.oas.annotations.media.Schema;
   
   @Schema(description = "Request to move a document to another folder")
   public class MoverDocumentoRequest {
       
       @JsonProperty("carpeta_destino_id")
       @NotNull(message = "carpeta_destino_id es requerido")
       @Positive(message = "carpeta_destino_id debe ser positivo")
       @Schema(description = "ID of the destination folder", example = "25", required = true)
       private Long carpetaDestinoId;
       
       // Constructors
       public MoverDocumentoRequest() {}
       
       public MoverDocumentoRequest(Long carpetaDestinoId) {
           this.carpetaDestinoId = carpetaDestinoId;
       }
       
       // Getters and Setters
       public Long getCarpetaDestinoId() {
           return carpetaDestinoId;
       }
       
       public void setCarpetaDestinoId(Long carpetaDestinoId) {
           this.carpetaDestinoId = carpetaDestinoId;
       }
   }
   ```

**Dependencias**:
- `jakarta.validation.constraints.*`
- `com.fasterxml.jackson.annotation.JsonProperty`
- `io.swagger.v3.oas.annotations.media.Schema`

**Notas de Implementación**:
- Usar `@NotNull` para validación de campos obligatorios
- Usar `@Positive` para asegurar que el ID es mayor que 0
- Usar snake_case en el nombre de propiedad JSON (`carpeta_destino_id`)
- Agregar anotación de esquema OpenAPI para documentación Swagger

---

### Paso 2: Crear DTO de Respuesta

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/DocumentoMovidoResponse.java`

**Acción**: Crear DTO para respuesta exitosa de movimiento

**Pasos de Implementación**:

1. **Crear DTO de Respuesta**:
   ```java
   package com.docflow.documentcore.application.dto;
   
   import com.fasterxml.jackson.annotation.JsonProperty;
   import io.swagger.v3.oas.annotations.media.Schema;
   
   @Schema(description = "Response after moving a document")
   public class DocumentoMovidoResponse {
       
       @JsonProperty("documento_id")
       @Schema(description = "ID of the moved document", example = "100")
       private Long documentoId;
       
       @JsonProperty("carpeta_origen_id")
       @Schema(description = "ID of the origin folder", example = "10")
       private Long carpetaOrigenId;
       
       @JsonProperty("carpeta_destino_id")
       @Schema(description = "ID of the destination folder", example = "25")
       private Long carpetaDestinoId;
       
       @JsonProperty("mensaje")
       @Schema(description = "Success message", example = "Documento movido exitosamente")
       private String mensaje;
       
       // Constructor
       public DocumentoMovidoResponse(
           Long documentoId, 
           Long carpetaOrigenId, 
           Long carpetaDestinoId, 
           String mensaje
       ) {
           this.documentoId = documentoId;
           this.carpetaOrigenId = carpetaOrigenId;
           this.carpetaDestinoId = carpetaDestinoId;
           this.mensaje = mensaje;
       }
       
       // Getters (Setters optional for immutable response)
       public Long getDocumentoId() { return documentoId; }
       public Long getCarpetaOrigenId() { return carpetaOrigenId; }
       public Long getCarpetaDestinoId() { return carpetaDestinoId; }
       public String getMensaje() { return mensaje; }
   }
   ```

**Dependencias**:
- `com.fasterxml.jackson.annotation.JsonProperty`
- `io.swagger.v3.oas.annotations.media.Schema`

**Notas de Implementación**:
- Incluir IDs de carpetas origen y destino para claridad
- Usar snake_case para propiedades JSON
- Inicialización basada en constructor (considerar inmutabilidad)
- Agregar mensaje de éxito descriptivo

---

### Paso 3: Crear Excepción de Negocio

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/MismaUbicacionException.java`

**Acción**: Crear excepción específica para intentar mover a la misma carpeta

**Pasos de Implementación**:

1. **Crear Excepción Personalizada**:
   ```java
   package com.docflow.documentcore.domain.exception;
   
   /**
    * Exception thrown when attempting to move a document to the same folder it's already in.
    */
   public class MismaUbicacionException extends DomainException {
       
       public MismaUbicacionException(Long documentoId, Long carpetaId) {
           super(
               String.format(
                   "El documento %d ya se encuentra en la carpeta %d", 
                   documentoId, 
                   carpetaId
               ),
               "MISMA_UBICACION"
           );
       }
   }
   ```

**Dependencias**: Extiende de `DomainException` existente

**Notas de Implementación**:
- Mensaje de error descriptivo en español
- Código de error para consumidores de API
- Específico para regla de negocio: no se puede mover a la misma ubicación

---

### Paso 4: Actualizar Manejador Global de Excepciones

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/exception/GlobalExceptionHandler.java`

**Acción**: Agregar manejador para `MismaUbicacionException` → HTTP 400

**Pasos de Implementación**:

1. **Agregar Método Manejador de Excepción**:
   ```java
   @ExceptionHandler(MismaUbicacionException.class)
   public ProblemDetail handleMismaUbicacion(MismaUbicacionException ex) {
       log.debug("Move to same location attempted: {}", ex.getMessage());
       
       var problem = ProblemDetail.forStatusAndDetail(
           HttpStatus.BAD_REQUEST,
           ex.getMessage()
       );
       
       problem.setTitle("Operación Inválida");
       problem.setType(URI.create("https://docflow.com/errors/misma-ubicacion"));
       problem.setProperty("timestamp", Instant.now());
       problem.setProperty("errorCode", "MISMA_UBICACION");
       
       return problem;
   }
   ```

**Dependencias**:
- `org.springframework.http.ProblemDetail`
- `org.springframework.http.HttpStatus`
- `java.net.URI`
- `java.time.Instant`

**Notas de Implementación**:
- Seguir RFC 7807 (Problem Details for HTTP APIs)
- Usar `HttpStatus.BAD_REQUEST` (400)
- Incluir propiedad de código de error para manejo programático
- Consistente con manejadores existentes en la clase

---

### Paso 5: Crear DTO de Evento de Auditoría

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/DocumentoMovidoEvent.java`

**Acción**: Crear payload de evento de auditoría para operación de mover documento

**Pasos de Implementación**:

1. **Crear DTO de Evento**:
   ```java
   package com.docflow.documentcore.application.dto;
   
   import java.time.OffsetDateTime;
   
   /**
    * Audit event payload for DOCUMENTO_MOVIDO event.
    * Sent to audit log service after successful document move.
    */
   public class DocumentoMovidoEvent {
       
       private String codigoEvento = "DOCUMENTO_MOVIDO";
       private Long documentoId;
       private Long carpetaOrigenId;
       private Long carpetaDestinoId;
       private Long usuarioId;
       private Long organizacionId;
       private OffsetDateTime timestamp;
       
       // Constructor
       public DocumentoMovidoEvent(
           Long documentoId,
           Long carpetaOrigenId,
           Long carpetaDestinoId,
           Long usuarioId,
           Long organizacionId
       ) {
           this.documentoId = documentoId;
           this.carpetaOrigenId = carpetaOrigenId;
           this.carpetaDestinoId = carpetaDestinoId;
           this.usuarioId = usuarioId;
           this.organizacionId = organizacionId;
           this.timestamp = OffsetDateTime.now();
       }
       
       // Getters and Setters
       public String getCodigoEvento() { return codigoEvento; }
       public Long getDocumentoId() { return documentoId; }
       public Long getCarpetaOrigenId() { return carpetaOrigenId; }
       public Long getCarpetaDestinoId() { return carpetaDestinoId; }
       public Long getUsuarioId() { return usuarioId; }
       public Long getOrganizacionId() { return organizacionId; }
       public OffsetDateTime getTimestamp() { return timestamp; }
   }
   ```

**Dependencias**:
- `java.time.OffsetDateTime`

**Notas de Implementación**:
- Código de evento fijo: `DOCUMENTO_MOVIDO`
- Incluir todo el contexto: origen, destino, usuario, organización
- Timestamp establecido automáticamente en construcción
- Este DTO será enviado al servicio de auditoría (punto de integración para P5)

---

### Paso 6: Crear Servicio de Aplicación (Lógica de Negocio Principal)

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/application/service/DocumentoMoverService.java`

**Acción**: Implementar servicio para orquestar operación de movimiento con validación dual de permisos

**Firma de Función**:
```java
@Service
@Transactional
public class DocumentoMoverService {
    public DocumentoMovidoResponse moverDocumento(
        Long documentoId,
        Long carpetaDestinoId,
        Long usuarioId,
        Long organizacionId
    );
}
```

**Pasos de Implementación**:

1. **Inyectar Dependencias**:
   ```java
   package com.docflow.documentcore.application.service;
   
   import com.docflow.documentcore.application.dto.DocumentoMovidoEvent;
   import com.docflow.documentcore.application.dto.DocumentoMovidoResponse;
   import com.docflow.documentcore.domain.exception.AccessDeniedException;
   import com.docflow.documentcore.domain.exception.MismaUbicacionException;
   import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
   import com.docflow.documentcore.domain.model.Documento;
   import com.docflow.documentcore.domain.model.NivelAcceso;
   import com.docflow.documentcore.domain.model.PermisoEfectivo;
   import com.docflow.documentcore.domain.model.TipoRecurso;
   import com.docflow.documentcore.domain.repository.ICarpetaRepository;
   import com.docflow.documentcore.domain.repository.IDocumentoRepository;
   import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
   import lombok.RequiredArgsConstructor;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.stereotype.Service;
   import org.springframework.transaction.annotation.Transactional;
   
   @Slf4j
   @Service
   @Transactional
   @RequiredArgsConstructor
   public class DocumentoMoverService {
       
       private final IDocumentoRepository documentoRepository;
       private final ICarpetaRepository carpetaRepository;
       private final IEvaluadorPermisos evaluadorPermisos;
       // TODO: Agregar publicador de eventos de auditoría cuando P5 esté implementado
       // private final AuditEventPublisher auditPublisher;
   ```

2. **Implementar Método Principal**:
   ```java
       public DocumentoMovidoResponse moverDocumento(
           Long documentoId,
           Long carpetaDestinoId,
           Long usuarioId,
           Long organizacionId
       ) {
           log.info("Moving document: documentoId={}, carpetaDestinoId={}, usuarioId={}, org={}",
                   documentoId, carpetaDestinoId, usuarioId, organizacionId);
           
           // Paso 1: Validar que el documento existe y pertenece a la organización
           Documento documento = documentoRepository.obtenerPorId(documentoId, organizacionId)
                   .orElseThrow(() -> {
                       log.warn("Document not found: documentoId={}, org={}", documentoId, organizacionId);
                       return new DocumentoNotFoundException(documentoId);
                   });
           
           Long carpetaOrigenId = documento.getCarpetaId();
           
           // Paso 2: Validar que la carpeta destino existe
           carpetaRepository.obtenerPorId(carpetaDestinoId, organizacionId)
                   .orElseThrow(() -> {
                       log.warn("Destination folder not found: carpetaId={}, org={}", carpetaDestinoId, organizacionId);
                       return new CarpetaNotFoundException(carpetaDestinoId);
                   });
           
           // Paso 3: Regla de negocio - no se puede mover a la misma carpeta
           if (carpetaOrigenId.equals(carpetaDestinoId)) {
               log.warn("Attempted to move document to same folder: documentoId={}, carpetaId={}",
                       documentoId, carpetaDestinoId);
               throw new MismaUbicacionException(documentoId, carpetaDestinoId);
           }
           
           // Paso 4: Validar permiso de ESCRITURA en carpeta origen
           PermisoEfectivo permisoOrigen = evaluadorPermisos.evaluarPermisoCarpeta(
                   usuarioId, carpetaOrigenId, organizacionId);
           
           if (permisoOrigen == null || 
               !tienePermisoEscritura(permisoOrigen.getNivelAcceso())) {
               log.warn("No WRITE permission on origin folder: userId={}, carpetaOrigenId={}",
                       usuarioId, carpetaOrigenId);
               throw new AccessDeniedException(
                   "No tiene permiso de escritura en la carpeta origen",
                   "SIN_PERMISO_ORIGEN"
               );
           }
           
           // Paso 5: Validar permiso de ESCRITURA en carpeta destino
           PermisoEfectivo permisoDestino = evaluadorPermisos.evaluarPermisoCarpeta(
                   usuarioId, carpetaDestinoId, organizacionId);
           
           if (permisoDestino == null || 
               !tienePermisoEscritura(permisoDestino.getNivelAcceso())) {
               log.warn("No WRITE permission on destination folder: userId={}, carpetaDestinoId={}",
                       usuarioId, carpetaDestinoId);
               throw new AccessDeniedException(
                   "No tiene permiso de escritura en la carpeta destino",
                   "SIN_PERMISO_DESTINO"
               );
           }
           
           // Paso 6: Actualizar ubicación del documento
           documento.setCarpetaId(carpetaDestinoId);
           documento.setFechaActualizacion(java.time.OffsetDateTime.now());
           
           // La persistencia es automática debido a @Transactional + entidad gestionada por JPA
           log.info("Document moved successfully: documentoId={}, from carpeta {} to carpeta {}",
                   documentoId, carpetaOrigenId, carpetaDestinoId);
           
           // Paso 7: Emitir evento de auditoría (dentro de la misma transacción)
           emitirEventoAuditoria(documentoId, carpetaOrigenId, carpetaDestinoId, usuarioId, organizacionId);
           
           // Paso 8: Retornar respuesta
           return new DocumentoMovidoResponse(
               documentoId,
               carpetaOrigenId,
               carpetaDestinoId,
               "Documento movido exitosamente"
           );
       }
   ```

3. **Agregar Métodos Auxiliares**:
   ```java
       /**
        * Verifica si el nivel de acceso permite operaciones de ESCRITURA.
        * Los niveles ESCRITURA y ADMINISTRACION permiten acceso de escritura.
        */
       private boolean tienePermisoEscritura(NivelAcceso nivel) {
           return nivel == NivelAcceso.ESCRITURA || 
                  nivel == NivelAcceso.ADMINISTRACION;
       }
       
       /**
        * Emite evento de auditoría para operación de mover documento.
        * TODO: Integrar con servicio de auditoría cuando P5 esté implementado.
        */
       private void emitirEventoAuditoria(
           Long documentoId,
           Long carpetaOrigenId,
           Long carpetaDestinoId,
           Long usuarioId,
           Long organizacionId
       ) {
           DocumentoMovidoEvent event = new DocumentoMovidoEvent(
               documentoId,
               carpetaOrigenId,
               carpetaDestinoId,
               usuarioId,
               organizacionId
           );
           
           // TODO: Publicar al servicio de auditoría
           // auditPublisher.publish(event);
           
           log.info("Audit event emitted: DOCUMENTO_MOVIDO for documentoId={}", documentoId);
       }
   }
   ```

**Dependencias**:
- `IDocumentoRepository`: Obtener documento
- `ICarpetaRepository`: Validar carpeta destino
- `IEvaluadorPermisos`: Verificar permisos
- DTOs: `DocumentoMovidoResponse`, `DocumentoMovidoEvent`
- Excepciones: `DocumentoNotFoundException`, `CarpetaNotFoundException`, `MismaUbicacionException`, `AccessDeniedException`

**Notas de Implementación**:
- `@Transactional` asegura atomicidad (actualización documento + evento auditoría en misma transacción)
- Usar `IEvaluadorPermisos.evaluarPermisoCarpeta()` existente para verificaciones de permisos
- Validar permiso de ESCRITURA en AMBAS carpetas origen y destino
- Mensajes de error claros distinguiendo fallos de permisos origen vs destino
- El documento es una entidad gestionada por JPA - no se necesita save() explícito, los cambios se persisten automáticamente al confirmar la transacción
- Emisión de evento de auditoría es placeholder (implementar cuando servicio de auditoría P5 esté disponible)

---

### Paso 7: Agregar Endpoint en Controlador

**Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/DocumentoController.java`

**Acción**: Agregar endpoint PATCH `/api/documentos/{id}/mover`

**Pasos de Implementación**:

1. **Agregar Método al Controlador Existente** (o crear si no existe):
   ```java
   // En la clase DocumentoController existente
   
   @Autowired
   private DocumentoMoverService documentoMoverService; // Agregar a inyección constructor/campo
   
   @PatchMapping("/{id}/mover")
   @Operation(
       summary = "Mover documento a otra carpeta",
       description = "Mueve un documento entre carpetas validando permisos en origen y destino"
   )
   @ApiResponses(value = {
       @ApiResponse(responseCode = "200", description = "Documento movido exitosamente"),
       @ApiResponse(responseCode = "400", description = "Intento de mover a la misma carpeta"),
       @ApiResponse(responseCode = "403", description = "Sin permiso en carpeta origen o destino"),
       @ApiResponse(responseCode = "404", description = "Documento o carpeta destino no encontrado")
   })
   public ResponseEntity<DocumentoMovidoResponse> moverDocumento(
           @PathVariable
           @Parameter(description = "ID del documento a mover", required = true)
           Long id,
           
           @Valid @RequestBody
           @Parameter(description = "Datos de la carpeta destino", required = true)
           MoverDocumentoRequest request,
           
           @RequestHeader(value = "X-User-Id", required = true)
           @Parameter(description = "ID del usuario", required = true)
           Long usuarioId,
           
           @RequestHeader(value = "X-Organization-Id", required = true)
           @Parameter(description = "ID de la organización", required = true)
           Long organizacionId
   ) {
       DocumentoMovidoResponse response = documentoMoverService.moverDocumento(
           id,
           request.getCarpetaDestinoId(),
           usuarioId,
           organizacionId
       );
       
       return ResponseEntity.ok(response);
   }
   ```

**Dependencias**:
- `org.springframework.web.bind.annotation.*`
- `io.swagger.v3.oas.annotations.*`
- `jakarta.validation.Valid`
- `DocumentoMoverService`
- `MoverDocumentoRequest`, `DocumentoMovidoResponse`

**Notas de Implementación**:
- Usar `@Valid` para activar validación en el DTO de solicitud
- Extraer IDs de usuario y organización de headers (establecidos por gateway/filtro auth)
- Delegar lógica de negocio a capa de servicio
- Retornar 200 OK con cuerpo de respuesta
- Anotaciones OpenAPI para documentación Swagger
- Excepciones manejadas por GlobalExceptionHandler

---

### Paso 8: Escribir Pruebas Unitarias - Capa de Servicio

**Archivo**: `backend/document-core/src/test/java/com/docflow/documentcore/application/service/DocumentoMoverServiceTest.java`

**Acción**: Crear pruebas unitarias completas para `DocumentoMoverService`

**Pasos de Implementación**:

1. **Crear Estructura de Clase de Prueba**:
   ```java
   package com.docflow.documentcore.application.service;
   
   import static org.assertj.core.api.Assertions.*;
   import static org.mockito.ArgumentMatchers.*;
   import static org.mockito.Mockito.*;
   
   import java.time.OffsetDateTime;
   import java.util.Optional;
   
   import org.junit.jupiter.api.BeforeEach;
   import org.junit.jupiter.api.DisplayName;
   import org.junit.jupiter.api.Test;
   import org.junit.jupiter.api.extension.ExtendWith;
   import org.mockito.Mock;
   import org.mockito.junit.jupiter.MockitoExtension;
   
   import com.docflow.documentcore.application.dto.DocumentoMovidoResponse;
   import com.docflow.documentcore.domain.exception.AccessDeniedException;
   import com.docflow.documentcore.domain.exception.MismaUbicacionException;
   import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
   import com.docflow.documentcore.domain.model.Carpeta;
   import com.docflow.documentcore.domain.model.Documento;
   import com.docflow.documentcore.domain.model.NivelAcceso;
   import com.docflow.documentcore.domain.model.PermisoEfectivo;
   import com.docflow.documentcore.domain.repository.ICarpetaRepository;
   import com.docflow.documentcore.domain.repository.IDocumentoRepository;
   import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
   
   @DisplayName("DocumentoMoverService")
   @ExtendWith(MockitoExtension.class)
   class DocumentoMoverServiceTest {
       
       @Mock
       private IDocumentoRepository documentoRepository;
       
       @Mock
       private ICarpetaRepository carpetaRepository;
       
       @Mock
       private IEvaluadorPermisos evaluadorPermisos;
       
       private DocumentoMoverService service;
       
       private static final Long DOCUMENTO_ID = 100L;
       private static final Long CARPETA_ORIGEN_ID = 10L;
       private static final Long CARPETA_DESTINO_ID = 25L;
       private static final Long USUARIO_ID = 1L;
       private static final Long ORGANIZACION_ID = 1L;
       
       @BeforeEach
       void setUp() {
           service = new DocumentoMoverService(
               documentoRepository,
               carpetaRepository,
               evaluadorPermisos
           );
       }
   ```

2. **Prueba: Caso Exitoso**:
   ```java
       @Test
       @DisplayName("should_MoverDocumento_When_AllValidationsPass")
       void shouldMoverDocumentoWhenAllValidationsPass() {
           // Arrange
           Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
           Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
           PermisoEfectivo permisoEscritura = crearPermiso(NivelAcceso.ESCRITURA);
           
           when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(documento));
           when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(carpetaDestino));
           when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                   .thenReturn(permisoEscritura);
           when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID))
                   .thenReturn(permisoEscritura);
           
           // Act
           DocumentoMovidoResponse response = service.moverDocumento(
               DOCUMENTO_ID,
               CARPETA_DESTINO_ID,
               USUARIO_ID,
               ORGANIZACION_ID
           );
           
           // Assert
           assertThat(response).isNotNull();
           assertThat(response.getDocumentoId()).isEqualTo(DOCUMENTO_ID);
           assertThat(response.getCarpetaOrigenId()).isEqualTo(CARPETA_ORIGEN_ID);
           assertThat(response.getCarpetaDestinoId()).isEqualTo(CARPETA_DESTINO_ID);
           assertThat(response.getMensaje()).contains("exitosamente");
           
           // Verificar que el documento fue actualizado
           assertThat(documento.getCarpetaId()).isEqualTo(CARPETA_DESTINO_ID);
           
           verify(documentoRepository).obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID);
           verify(carpetaRepository).obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID);
           verify(evaluadorPermisos, times(2)).evaluarPermisoCarpeta(anyLong(), anyLong(), anyLong());
       }
   ```

3. **Pruebas: Errores de Validación**:
   ```java
       @Test
       @DisplayName("should_ThrowDocumentoNotFoundException_When_DocumentoDoesNotExist")
       void shouldThrowDocumentoNotFoundExceptionWhenDocumentoDoesNotExist() {
           // Arrange
           when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.empty());
           
           // Act & Assert
           assertThatThrownBy(() -> service.moverDocumento(
               DOCUMENTO_ID,
               CARPETA_DESTINO_ID,
               USUARIO_ID,
               ORGANIZACION_ID
           ))
           .isInstanceOf(DocumentoNotFoundException.class);
           
           verify(carpetaRepository, never()).obtenerPorId(anyLong(), anyLong());
           verify(evaluadorPermisos, never()).evaluarPermisoCarpeta(anyLong(), anyLong(), anyLong());
       }
       
       @Test
       @DisplayName("should_ThrowCarpetaNotFoundException_When_CarpetaDestinoDoesNotExist")
       void shouldThrowCarpetaNotFoundExceptionWhenCarpetaDestinoDoesNotExist() {
           // Arrange
           Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
           
           when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(documento));
           when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.empty());
           
           // Act & Assert
           assertThatThrownBy(() -> service.moverDocumento(
               DOCUMENTO_ID,
               CARPETA_DESTINO_ID,
               USUARIO_ID,
               ORGANIZACION_ID
           ))
           .isInstanceOf(CarpetaNotFoundException.class);
           
           verify(evaluadorPermisos, never()).evaluarPermisoCarpeta(anyLong(), anyLong(), anyLong());
       }
       
       @Test
       @DisplayName("should_ThrowMismaUbicacionException_When_MovingToSameFolder")
       void shouldThrowMismaUbicacionExceptionWhenMovingToSameFolder() {
           // Arrange
           Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
           Carpeta carpeta = crearCarpeta(CARPETA_ORIGEN_ID);
           
           when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(documento));
           when(carpetaRepository.obtenerPorId(CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(carpeta));
           
           // Act & Assert
           assertThatThrownBy(() -> service.moverDocumento(
               DOCUMENTO_ID,
               CARPETA_ORIGEN_ID, // Same as origin
               USUARIO_ID,
               ORGANIZACION_ID
           ))
           .isInstanceOf(MismaUbicacionException.class);
           
           verify(evaluadorPermisos, never()).evaluarPermisoCarpeta(anyLong(), anyLong(), anyLong());
       }
   ```

4. **Pruebas: Fallos de Permisos**:
   ```java
       @Test
       @DisplayName("should_ThrowAccessDeniedException_When_NoWritePermissionOnOrigin")
       void shouldThrowAccessDeniedExceptionWhenNoWritePermissionOnOrigin() {
           // Arrange
           Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
           Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
           PermisoEfectivo permisoLecturaOnly = crearPermiso(NivelAcceso.LECTURA);
           
           when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(documento));
           when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(carpetaDestino));
           when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                   .thenReturn(permisoLecturaOnly); // Only READ, not WRITE
           
           // Act & Assert
           assertThatThrownBy(() -> service.moverDocumento(
               DOCUMENTO_ID,
               CARPETA_DESTINO_ID,
               USUARIO_ID,
               ORGANIZACION_ID
           ))
           .isInstanceOf(AccessDeniedException.class)
           .hasMessageContaining("origen");
           
           verify(evaluadorPermisos).evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID);
           verify(evaluadorPermisos, never()).evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID);
       }
       
       @Test
       @DisplayName("should_ThrowAccessDeniedException_When_NoWritePermissionOnDestination")
       void shouldThrowAccessDeniedExceptionWhenNoWritePermissionOnDestination() {
           // Arrange
           Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
           Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
           PermisoEfectivo permisoEscritura = crearPermiso(NivelAcceso.ESCRITURA);
           PermisoEfectivo permisoLecturaOnly = crearPermiso(NivelAcceso.LECTURA);
           
           when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(documento));
           when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(carpetaDestino));
           when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                   .thenReturn(permisoEscritura); // WRITE on origin
           when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID))
                   .thenReturn(permisoLecturaOnly); // Only READ on destination
           
           // Act & Assert
           assertThatThrownBy(() -> service.moverDocumento(
               DOCUMENTO_ID,
               CARPETA_DESTINO_ID,
               USUARIO_ID,
               ORGANIZACION_ID
           ))
           .isInstanceOf(AccessDeniedException.class)
           .hasMessageContaining("destino");
           
           verify(evaluadorPermisos).evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID);
           verify(evaluadorPermisos).evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID);
       }
       
       @Test
       @DisplayName("should_ThrowAccessDeniedException_When_NoPermissionOnOrigin")
       void shouldThrowAccessDeniedExceptionWhenNoPermissionOnOrigin() {
           // Arrange
           Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
           Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
           
           when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(documento));
           when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(carpetaDestino));
           when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                   .thenReturn(null); // No permission at all
           
           // Act & Assert
           assertThatThrownBy(() -> service.moverDocumento(
               DOCUMENTO_ID,
               CARPETA_DESTINO_ID,
               USUARIO_ID,
               ORGANIZACION_ID
           ))
           .isInstanceOf(AccessDeniedException.class);
       }
   ```

5. **Pruebas: Casos Límite**:
   ```java
       @Test
       @DisplayName("should_AllowMove_When_UserHasAdministrationPermission")
       void shouldAllowMoveWhenUserHasAdministrationPermission() {
           // Arrange - El nivel ADMINISTRACION debe permitir operaciones de ESCRITURA
           Documento documento = crearDocumento(DOCUMENTO_ID, CARPETA_ORIGEN_ID);
           Carpeta carpetaDestino = crearCarpeta(CARPETA_DESTINO_ID);
           PermisoEfectivo permisoAdmin = crearPermiso(NivelAcceso.ADMINISTRACION);
           
           when(documentoRepository.obtenerPorId(DOCUMENTO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(documento));
           when(carpetaRepository.obtenerPorId(CARPETA_DESTINO_ID, ORGANIZACION_ID))
                   .thenReturn(Optional.of(carpetaDestino));
           when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_ORIGEN_ID, ORGANIZACION_ID))
                   .thenReturn(permisoAdmin);
           when(evaluadorPermisos.evaluarPermisoCarpeta(USUARIO_ID, CARPETA_DESTINO_ID, ORGANIZACION_ID))
                   .thenReturn(permisoAdmin);
           
           // Act
           DocumentoMovidoResponse response = service.moverDocumento(
               DOCUMENTO_ID,
               CARPETA_DESTINO_ID,
               USUARIO_ID,
               ORGANIZACION_ID
           );
           
           // Assert
           assertThat(response).isNotNull();
           assertThat(documento.getCarpetaId()).isEqualTo(CARPETA_DESTINO_ID);
       }
   ```

6. **Métodos Auxiliares**:
   ```java
       // ========================================================================
       // MÉTODOS AUXILIARES
       // ========================================================================
       
       private Documento crearDocumento(Long id, Long carpetaId) {
           Documento doc = new Documento();
           doc.setId(id);
           doc.setCarpetaId(carpetaId);
           doc.setOrganizacionId(ORGANIZACION_ID);
           doc.setNombre("test-documento.pdf");
           doc.setPropietarioId(USUARIO_ID);
           doc.setFechaCreacion(OffsetDateTime.now());
           doc.setFechaActualizacion(OffsetDateTime.now());
           return doc;
       }
       
       private Carpeta crearCarpeta(Long id) {
           return Carpeta.builder()
                   .id(id)
                   .nombre("Test Carpeta")
                   .organizacionId(ORGANIZACION_ID)
                   .creadoPor(USUARIO_ID)
                   .build();
       }
       
       private PermisoEfectivo crearPermiso(NivelAcceso nivel) {
           return PermisoEfectivo.carpetaDirecto(
               nivel,
               CARPETA_ORIGEN_ID,
               "Test Carpeta"
           );
       }
   }
   ```

**Dependencias**:
- JUnit 5: `@Test`, `@DisplayName`, `@BeforeEach`
- Mockito: `@Mock`, `@ExtendWith(MockitoExtension.class)`
- AssertJ: `assertThat()`, `assertThatThrownBy()`

**Notas de Implementación**:
- Cubrir todos los escenarios: exitosos, errores de validación, fallos de permisos, casos límite
- Usar nombres descriptivos para métodos de prueba siguiendo patrón `should_DoSomething_When_Condition`
- Verificar interacciones con mocks usando `verify()`
- Probar jerarquía de permisos: LECTURA < ESCRITURA < ADMINISTRACION
- Asegurar que las pruebas están aisladas y no dependen entre sí

---

### Paso 9: Escribir Pruebas de Integración

**Archivo**: `backend/document-core/src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/DocumentoMoverIntegrationTest.java`

**Acción**: Crear pruebas de integración para el endpoint PATCH

**Pasos de Implementación**:

1. **Crear Clase de Prueba**:
   ```java
   package com.docflow.documentcore.infrastructure.adapter.controller;
   
   import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
   import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
   
   import org.junit.jupiter.api.DisplayName;
   import org.junit.jupiter.api.Test;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
   import org.springframework.boot.test.context.SpringBootTest;
   import org.springframework.http.MediaType;
   import org.springframework.test.web.servlet.MockMvc;
   import org.springframework.transaction.annotation.Transactional;
   
   /**
    * Pruebas de integración para endpoint PATCH /api/documentos/{id}/mover.
    * 
    * NOTA: Estas pruebas requieren configuración de base de datos con datos de prueba.
    * Para pruebas de integración completas, ejecutar con docker-compose y todos los servicios.
    */
   @DisplayName("DocumentoController - PATCH /api/documentos/{id}/mover")
   @SpringBootTest
   @AutoConfigureMockMvc
   @Transactional // Rollback después de cada prueba
   class DocumentoMoverIntegrationTest {
       
       @Autowired
       private MockMvc mockMvc;
       
       private static final String BASE_URL = "/api/documentos";
       
       @Test
       @DisplayName("should_Return400_When_MissingRequestBody")
       void shouldReturn400WhenMissingRequestBody() throws Exception {
           mockMvc.perform(patch(BASE_URL + "/1/mover")
                   .header("X-User-Id", "1")
                   .header("X-Organization-Id", "1")
                   .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isBadRequest());
       }
       
       @Test
       @DisplayName("should_Return400_When_InvalidCarpetaDestinoId")
       void shouldReturn400WhenInvalidCarpetaDestinoId() throws Exception {
           String requestBody = """
               {
                   "carpeta_destino_id": -1
               }
               """;
           
           mockMvc.perform(patch(BASE_URL + "/1/mover")
                   .header("X-User-Id", "1")
                   .header("X-Organization-Id", "1")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(requestBody))
                   .andExpect(status().isBadRequest());
       }
       
       // Pruebas adicionales requerirían configuración de datos de prueba
       // Para pruebas de integración completas, usar entorno docker-compose
   }
   ```

**Dependencias**:
- Spring Test: `MockMvc`, `@SpringBootTest`, `@AutoConfigureMockMvc`
- `@Transactional` para rollback de datos de prueba

**Notas de Implementación**:
- Pruebas de integración básicas validan estructura de solicitud/respuesta
- Pruebas funcionales completas requieren base de datos con datos de prueba
- Considerar usar `@Sql` para cargar scripts de datos de prueba
- Ejecutar pruebas de integración completas en CI/CD con docker-compose

---

### Paso 10: Actualizar Documentación

**Acción**: Actualizar documentación técnica para reflejar nueva funcionalidad de mover documento

**Pasos de Implementación**:

1. **Actualizar Especificación de API** (`ai-specs/specs/api-spec.yml`):
   - Agregar endpoint `PATCH /api/documentos/{id}/mover`
   - Documentar esquemas de solicitud/respuesta
   - Incluir códigos de respuesta de error (400, 403, 404)

2. **Actualizar Documentación del Modelo de Datos** (`ai-specs/specs/data-model.md`):
   - Documentar evento de auditoría: `DOCUMENTO_MOVIDO`
   - Incluir estructura de payload del evento
   - Nota: No se requieren cambios de esquema (se actualiza campo `carpeta_id` existente)

3. **Actualizar Estándares de Backend** (si aplica):
   - Documentar patrón de validación dual de permisos
   - Referenciar como ejemplo de transacción atómica con auditoría

**Archivos de Documentación a Actualizar**:
- `ai-specs/specs/api-spec.yml`
- `ai-specs/specs/data-model.md`

**Contenido a Agregar**:

For `api-spec.yml`:
```yaml
  /api/documentos/{id}/mover:
    patch:
      summary: Move document to another folder
      description: Moves a document between folders with dual permission validation
      tags:
        - Documentos
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
            format: int64
          description: Document ID
        - name: X-User-Id
          in: header
          required: true
          schema:
            type: integer
            format: int64
        - name: X-Organization-Id
          in: header
          required: true
          schema:
            type: integer
            format: int64
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
                - carpeta_destino_id
              properties:
                carpeta_destino_id:
                  type: integer
                  format: int64
                  description: Destination folder ID
                  example: 25
      responses:
        '200':
          description: Document moved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  documento_id:
                    type: integer
                    format: int64
                  carpeta_origen_id:
                    type: integer
                    format: int64
                  carpeta_destino_id:
                    type: integer
                    format: int64
                  mensaje:
                    type: string
        '400':
          description: Invalid request (same folder, validation error)
        '403':
          description: No write permission on origin or destination folder
        '404':
          description: Document or destination folder not found
```

Para `data-model.md`:
```markdown
### Evento de Auditoría: DOCUMENTO_MOVIDO

Emitido cuando un documento es movido exitosamente entre carpetas.

**Código de Evento**: `DOCUMENTO_MOVIDO`

**Payload**:
- `documento_id` (Long): ID del documento movido
- `carpeta_origen_id` (Long): ID de carpeta origen
- `carpeta_destino_id` (Long): ID de carpeta destino
- `usuario_id` (Long): Usuario que realizó el movimiento
- `organizacion_id` (Long): Contexto de organización
- `timestamp` (OffsetDateTime): Timestamp del evento

**Disparador**: Después de operación exitosa de mover documento (dentro de la misma transacción).
```

**Notas**:
- Toda la documentación debe estar en inglés según estándares de documentación
- Seguir estructura y formato existente en cada archivo
- Este paso es OBLIGATORIO antes de considerar la implementación completa

---

## Orden de Implementación

* **Paso 1**: Crear DTO de Solicitud (`MoverDocumentoRequest`)
* **Paso 2**: Crear DTO de Respuesta (`DocumentoMovidoResponse`)
* **Paso 3**: Crear Excepción de Negocio (`MismaUbicacionException`)
* **Paso 4**: Actualizar Manejador Global de Excepciones
* **Paso 5**: Crear DTO de Evento de Auditoría (`DocumentoMovidoEvent`)
* **Paso 6**: Crear Servicio de Aplicación (`DocumentoMoverService`)
* **Paso 7**: Agregar Endpoint en Controlador (PATCH `/api/documentos/{id}/mover`)
* **Paso 8**: Escribir Pruebas Unitarias (Capa de Servicio)
* **Paso 9**: Escribir Pruebas de Integración (Capa de Controlador)
* **Paso 10**: Actualizar Documentación (Especificación API, modelo de datos)

## Lista de Verificación de Pruebas

Después de la implementación, verificar:

- [ ] Pruebas unitarias pasan: `mvn test -Dtest=DocumentoMoverServiceTest`
- [ ] Pruebas de integración pasan: `mvn test -Dtest=DocumentoMoverIntegrationTest`
- [ ] Todas las pruebas pasan: `mvn clean test`
- [ ] Validación de solicitud funciona (400 para datos inválidos)
- [ ] Documento no encontrado retorna 404
- [ ] Carpeta destino no encontrada retorna 404
- [ ] Mover a misma carpeta retorna 400
- [ ] Sin permiso en carpeta origen retorna 403
- [ ] Sin permiso en carpeta destino retorna 403
- [ ] Movimiento exitoso retorna 200 con respuesta correcta
- [ ] `carpeta_id` del documento se actualiza en base de datos
- [ ] Evento de auditoría se emite (verificar logs hasta que P5 esté implementado)
- [ ] Operación es atómica (rollback en error)
- [ ] Documentación Swagger es accesible y correcta

## Formato de Respuesta de Error

Todos los errores siguen RFC 7807 (Problem Details for HTTP APIs):

```json
{
  "type": "https://docflow.com/errors/{error-type}",
  "title": "Error Title",
  "status": 400,
  "detail": "Detailed error message",
  "errorCode": "ERROR_CODE",
  "timestamp": "2026-02-04T10:30:00Z"
}
```

**Mapeo de Códigos de Estado HTTP**:
- **200 OK**: Documento movido exitosamente
- **400 Bad Request**: 
  - Cuerpo de solicitud inválido (error de validación)
  - Intento de mover a la misma carpeta (`MISMA_UBICACION`)
- **403 Forbidden**: 
  - Sin permiso de ESCRITURA en carpeta origen (`SIN_PERMISO_ORIGEN`)
  - Sin permiso de ESCRITURA en carpeta destino (`SIN_PERMISO_DESTINO`)
- **404 Not Found**: 
  - Documento no encontrado en organización
  - Carpeta destino no encontrada en organización

## Dependencias

**Componentes Existentes** (No se necesitan nuevas dependencias):
- Spring Boot 3.x
- Spring Data JPA
- Lombok
- MapStruct (para mapeo futuro de DTOs si es necesario)
- Jakarta Validation
- Springdoc OpenAPI (Swagger)
- JUnit 5, Mockito, AssertJ

**Puntos de Integración**:
- `IEvaluadorPermisos`: Servicio de evaluación de permisos
- `IDocumentoRepository`: Persistencia de documentos
- `ICarpetaRepository`: Validación de carpetas
- Servicio de Auditoría: Emisión de eventos (placeholder para P5)

## Notas

### Reglas de Negocio
1. **Validación Dual de Permisos**: Usuario debe tener permiso de ESCRITURA en AMBAS carpetas origen y destino
2. **Jerarquía de Permisos**: LECTURA (1) < ESCRITURA (2) < ADMINISTRACION (3)
   - Los niveles ESCRITURA y ADMINISTRACION permiten mover documento
3. **Verificación de Misma Ubicación**: No se puede mover documento a la carpeta en la que ya está
4. **Multi-Tenancy**: Todas las operaciones con alcance a organización (filtro implícito)
5. **Atomicidad**: Actualización de documento y evento de auditoría en misma transacción

### Consideraciones de Seguridad
- Nunca revelar existencia de recursos en otras organizaciones (404 para acceso cross-tenant)
- Distinguir fallos de permisos origen vs destino en mensajes de error
- Validar permisos ANTES de actualizar documento
- Registrar eventos de seguridad (denegaciones de permiso)

### Integración de Auditoría
- Emisión de evento de auditoría es actualmente un placeholder
- Cuando P5 (Servicio de Auditoría) esté implementado:
  - Inyectar `AuditEventPublisher` en `DocumentoMoverService`
  - Publicar `DocumentoMovidoEvent` al servicio de auditoría
  - Asegurar que la publicación esté dentro de la misma transacción para atomicidad
- Hasta entonces, registrar detalles del evento para verificación

### Requisitos de Idioma
- **Código**: Inglés (clases, métodos, variables, comentarios)
- **Mensajes cara al usuario** (mensajes de error, respuestas API): Español
- **Documentación**: Español

### Notas de Rendimiento
- Transacción única de base de datos para operación de movimiento
- Dos evaluaciones de permisos (origen + destino)
- Sin operaciones de sistema de archivos (solo actualización de metadatos)
- Considerar agregar índice en `documentos(carpeta_id)` si no existe

## Próximos Pasos Después de la Implementación

1. **Revisión de Código**: Enviar PR con título descriptivo y enlace a US-FOLDER-003
2. **Pruebas Manuales**: 
   - Probar con Postman/curl usando varios escenarios de permisos
   - Verificar entradas de log de auditoría (cuando P5 esté disponible)
3. **Integración con P5**: Cuando servicio de auditoría esté listo:
   - Reemplazar placeholder de evento de auditoría con publicador real
   - Verificar consumo de evento en log de auditoría
4. **Integración UI**: Coordinar con equipo frontend para implementación UI de US-FOLDER-003
5. **Documentación**: Asegurar que README.md incluya nuevo endpoint en servicio document-core

## Verificación de Implementación

Antes de marcar este ticket como completo, verificar:

### Calidad de Código
- [ ] Código sigue estándares del proyecto (`.github/rules-backend.md`)
- [ ] Sin code smells o advertencias
- [ ] Manejo adecuado de errores y logging
- [ ] Se usa inyección por constructor (no inyección por campos)
- [ ] DTOs separados de entidades

### Funcionalidad
- [ ] Todas las reglas de negocio implementadas correctamente
- [ ] Validación dual de permisos funciona (origen + destino)
- [ ] Transacción atómica verificada (rollback en error)
- [ ] Todos los casos de error manejados (400, 403, 404)
- [ ] Evento de auditoría emitido (placeholder verificado)

### Pruebas
- [ ] Pruebas unitarias cubren todos los escenarios (éxito, errores, casos límite)
- [ ] Pruebas de integración validan capa HTTP
- [ ] Todas las pruebas pasan: `mvn clean test`
- [ ] Cobertura de pruebas >= 80% para código nuevo

### Integración
- [ ] Endpoint accesible vía gateway (si aplica)
- [ ] Documentación Swagger UI correcta
- [ ] Compatible con sistema de permisos existente
- [ ] Sin cambios disruptivos para otros servicios

### Documentación
- [ ] Especificación de API actualizada (`api-spec.yml`)
- [ ] Documentación de modelo de datos actualizada (evento de auditoría)
- [ ] README.md incluye nuevo endpoint
- [ ] Comentarios de código claros y útiles

---

**Fin del Plan de Implementación**

Este plan proporciona una guía completa, paso a paso, para implementar US-FOLDER-003. Seguir el orden de implementación, ejecutar pruebas frecuentemente, y asegurar que todos los puntos de verificación pasen antes de enviar para revisión.
