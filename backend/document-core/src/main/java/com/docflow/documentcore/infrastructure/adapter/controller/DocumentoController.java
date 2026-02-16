package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.CreateVersionRequest;
import com.docflow.documentcore.application.dto.DocumentoMovidoResponse;
import com.docflow.documentcore.application.dto.DownloadDocumentDto;
import com.docflow.documentcore.application.dto.MoverDocumentoRequest;
import com.docflow.documentcore.application.dto.VersionResponse;
import com.docflow.documentcore.application.service.DocumentService;
import com.docflow.documentcore.application.service.DocumentoMoverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

/**
 * Controlador REST para operaciones sobre documentos.
 * 
 * <p>Expone endpoints para la gestión de documentos en el sistema DocFlow,
 * incluyendo operaciones de movimiento entre carpetas con validación de permisos.
 * 
 * <p>Todos los endpoints requieren autenticación mediante JWT y headers
 * de contexto de usuario y organización (establecidos por el gateway).
 * 
 * @see com.docflow.documentcore.application.service.DocumentoMoverService
 */
@Slf4j
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "Endpoints para gestión de documentos")
public class DocumentoController {
    
    private final DocumentoMoverService documentoMoverService;
    private final DocumentService documentService;
    
    /**
     * Mueve un documento de una carpeta a otra.
     * 
     * <p>Valida que el usuario tenga permiso de ESCRITURA tanto en la carpeta
     * origen como en la carpeta destino. La operación es atómica y genera
     * un evento de auditoría.
     * 
     * <p><b>Reglas de negocio:</b>
     * <ul>
     *   <li>No se puede mover un documento a la carpeta donde ya se encuentra</li>
     *   <li>Requiere permiso ESCRITURA o ADMINISTRACION en carpeta origen</li>
     *   <li>Requiere permiso ESCRITURA o ADMINISTRACION en carpeta destino</li>
     * </ul>
     * 
     * @param id ID del documento a mover
     * @param request Objeto con carpetaDestinoId
     * @param usuarioId ID del usuario (inyectado desde header X-User-Id)
     * @param organizacionId ID de la organización (inyectado desde header X-Organization-Id)
     * @return Response con información del movimiento exitoso
     */
    @PatchMapping("/{id}/mover")
    @Operation(
        summary = "Mover documento a otra carpeta",
        description = """
            Mueve un documento entre carpetas validando permisos en origen y destino.
            
            La operación requiere:
            - Permiso de ESCRITURA en la carpeta origen
            - Permiso de ESCRITURA en la carpeta destino
            - El documento no puede moverse a la misma carpeta donde ya está
            
            La operación es transaccional y emite un evento de auditoría DOCUMENTO_MOVIDO.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Documento movido exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DocumentoMovidoResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Intento de mover a la misma carpeta o datos inválidos",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permiso en carpeta origen o destino",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Documento o carpeta destino no encontrado",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<DocumentoMovidoResponse> moverDocumento(
            @PathVariable
            @Parameter(description = "ID del documento a mover", required = true, example = "100")
            Long id,
            
            @Valid @RequestBody
            @Parameter(description = "Datos de la carpeta destino", required = true)
            MoverDocumentoRequest request,
            
            @RequestHeader(value = "X-User-Id", required = true)
            @Parameter(description = "ID del usuario autenticado (inyectado por gateway)", required = true, example = "1")
            Long usuarioId,
            
            @RequestHeader(value = "X-Organization-Id", required = true)
            @Parameter(description = "ID de la organización del usuario (inyectado por gateway)", required = true, example = "1")
            Long organizacionId
    ) {
        log.info("REST: PATCH /api/documentos/{}/mover - usuarioId={}, orgId={}, carpetaDestinoId={}",
                id, usuarioId, organizacionId, request.getCarpetaDestinoId());
        
        DocumentoMovidoResponse response = documentoMoverService.moverDocumento(
            id,
            request.getCarpetaDestinoId(),
            usuarioId,
            organizacionId
        );
        
        log.info("REST: Document moved successfully - documentoId={}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Elimina (soft delete) un documento del sistema.
     * 
     * <p>Marca el documento con fecha_eliminacion sin eliminar físicamente
     * el registro ni los archivos de versiones. Esto mantiene trazabilidad
     * y auditoría completa.</p>
     * 
     * <p><b>Validaciones:</b></p>
     * <ul>
     *   <li>Documento debe existir y pertenecer a la organización del usuario</li>
     *   <li>Usuario debe tener permiso de ESCRITURA o ADMINISTRACION</li>
     *   <li>Documento no debe estar ya eliminado</li>
     * </ul>
     * 
     * <p><b>Auditoría:</b> Emite evento DOCUMENTO_ELIMINADO para trazabilidad.</p>
     * 
     * <p><b>US-DOC-008:</b> Eliminación de documento desde la UI.</p>
     * 
     * @param id ID del documento a eliminar
     * @return ResponseEntity sin contenido (204) si exitoso
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar documento (soft delete)",
        description = """
            Marca un documento como eliminado sin borrar físicamente los datos.
            
            La operación requiere:
            - Documento debe existir y pertenecer a la organización del usuario (tenant isolation)
            - Usuario debe tener permiso de ESCRITURA o ADMINISTRACION sobre el documento
            - Documento no debe estar ya eliminado
            
            La eliminación es lógica (soft delete) mediante campo fecha_eliminacion.
            Se emite un evento de auditoría DOCUMENTO_ELIMINADO.
            
            Importante para seguridad:
            - HTTP 404 en lugar de 403 para documentos de otras organizaciones
            - HTTP 403 solo cuando el usuario está autenticado pero sin permisos en su propia organización
            - HTTP 409 si el documento ya está eliminado
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Documento eliminado exitosamente"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado - Token JWT ausente o inválido",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permiso de ESCRITURA o ADMINISTRACION sobre el documento",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Documento no encontrado o pertenece a otra organización",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Documento ya está eliminado",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<Void> deleteDocument(
            @PathVariable
            @Parameter(description = "ID del documento a eliminar", required = true, example = "100")
            Long id
    ) {
        log.info("REST: DELETE /api/documentos/{}", id);

        documentService.deleteDocument(id);

        log.info("REST: Document deleted successfully - documentoId={}", id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Descarga la versión actual de un documento.
     * 
     * <p>Endpoint para descargar el archivo de la versión actual de un documento.
     * Utiliza streaming para soportar archivos grandes sin consumir memoria excesiva.
     * 
     * <p><b>Validaciones:</b>
     * <ul>
     *   <li>El documento debe existir y pertenecer a la organización del usuario</li>
     *   <li>El usuario debe tener permiso de LECTURA sobre el documento</li>
     *   <li>El archivo físico debe estar disponible en el almacenamiento</li>
     * </ul>
     * 
     * <p><b>Auditoría:</b> Emite evento DOCUMENTO_DESCARGADO para trazabilidad.
     * 
     * <p><b>US-DOC-002:</b> Descarga de versión actual de documento.
     * 
     * @param id ID del documento a descargar
     * @param response HttpServletResponse para configurar headers
     * @return StreamingResponseBody con el contenido del archivo (200) o error (403, 404, 500)
     */
    @GetMapping("/{id}/download")
    @Operation(
        summary = "Descargar versión actual de un documento",
        description = """
            Descarga el archivo de la versión actual del documento.
            
            La operación requiere:
            - Documento debe existir y pertenecer a la organización del usuario (tenant isolation)
            - Usuario debe tener permiso de LECTURA, ESCRITURA o ADMINISTRACION sobre el documento
            - Archivo físico debe estar disponible en almacenamiento
            
            La descarga se hace mediante streaming para soportar archivos grandes.
            Se emite un evento de auditoría DOCUMENTO_DESCARGADO.
            
            Importante para seguridad:
            - HTTP 404 en lugar de 403 para documentos de otras organizaciones (security by obscurity)
            - HTTP 403 solo cuando el usuario está autenticado pero sin permisos en su propia organización
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Archivo descargado exitosamente",
            content = @Content(
                mediaType = "application/octet-stream"
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado - Token JWT ausente o inválido",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acceso denegado - Sin permiso de LECTURA sobre el documento",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Documento no encontrado o no pertenece a la organización",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error de almacenamiento - Archivo no disponible",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<StreamingResponseBody> downloadDocument(
            @PathVariable
            @Parameter(description = "ID del documento a descargar", required = true, example = "100")
            Long id,
            
            HttpServletResponse response
    ) {
        log.info("REST: GET /api/documentos/{}/download", id);
        
        // Delegar al servicio (que valida permisos y tenant isolation)
        DownloadDocumentDto downloadDto = documentService.downloadDocument(id);
        
        // Construir nombre de archivo completo con extensión
        String fullFilename = downloadDto.getFullFilename();
        
        // Sanitizar nombre de archivo para prevenir inyección de headers HTTP
        String sanitizedFilename = sanitizeFilename(fullFilename);
        
        // Configurar headers de respuesta
        String contentDisposition = String.format("attachment; filename=\"%s\"", sanitizedFilename);
        
        // Crear StreamingResponseBody para transmisión eficiente
        StreamingResponseBody stream = outputStream -> {
            try (InputStream inputStream = downloadDto.stream()) {
                byte[] buffer = new byte[8192]; // Buffer 8KB
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                log.debug("File streamed successfully: {} bytes", downloadDto.sizeBytes());
            } catch (Exception e) {
                log.error("Error streaming file for documentId={}", id, e);
                throw new RuntimeException("Error streaming file", e);
            }
        };
        
        log.info("REST: Document download initiated successfully - documentoId={}, filename={}, sizeBytes={}", 
            id, sanitizedFilename, downloadDto.sizeBytes());
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, downloadDto.mimeType())
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadDto.sizeBytes()))
            .body(stream);
    }
    
    /**
     * Crea una nueva versión de un documento existente.
     * 
     * <p>Este endpoint implementa US-DOC-003 y permite subir una nueva versión de un documento
     * manteniendo un historial completo de cambios. El sistema incrementará automáticamente el
     * número de versión secuencial y actualizará la referencia de versión actual del documento.
     * 
     * <p><b>Reglas de negocio:</b>
     * <ul>
     *   <li>Requiere permiso de ESCRITURA en el documento</li>
     *   <li>El número de versión se incrementa automáticamente</li>
     *   <li>Las versiones son inmutables (no pueden modificarse o eliminarse)</li>
     *   <li>La versión actual se actualiza automáticamente</li>
     *   <li>Tamaño máximo de archivo: 500 MB</li>
     * </ul>
     * 
     * @param id ID del documento al que agregar nueva versión
     * @param file Archivo a cargar como nueva versión
     * @param comentarioCambio Descripción opcional de los cambios (máx 500 caracteres)
     * @return Response con información de la nueva versión creada
     */
    @PostMapping("/{id}/versiones")
    @Operation(
        summary = "Subir nueva versión de documento",
        description = """
            Crea una nueva versión de un documento existente con historial inmutable.
            
            La operación requiere:
            - Permiso de ESCRITURA en el documento
            - Archivo válido (no vacío, máx 500 MB)
            - Documento debe existir y no estar eliminado
            
            El número de versión se incrementa automáticamente y la nueva versión se convierte
            en la versión actual del documento. Las versiones anteriores permanecen accesibles
            en el historial.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Nueva versión creada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VersionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Archivo inválido (vacío, demasiado grande, etc.)",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Sin permiso de ESCRITURA en el documento",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Documento no encontrado o no pertenece a la organización",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflicto en la creación de versión (carga concurrente)",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "413",
            description = "Archivo demasiado grande (>500 MB)",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable 
            @Parameter(description = "ID del documento al que agregar nueva versión", required = true, example = "100")
            Long id,
            
            @RequestParam("file")
            @Parameter(description = "Archivo a cargar como nueva versión", required = true)
            org.springframework.web.multipart.MultipartFile file,
            
            @RequestParam(value = "comentarioCambio", required = false)
            @Parameter(description = "Descripción opcional de los cambios (máx 500 caracteres)", example = "Actualización de contenido Q1 2026")
            String comentarioCambio
    ) {
        log.info("REST: POST /api/documentos/{}/versiones", id);
        
        // Construir request
        CreateVersionRequest request = 
            new CreateVersionRequest(file, comentarioCambio);
        
        // Delegar al servicio (que valida permisos y tenant isolation)
        VersionResponse response = documentService.createVersion(id, request);
        
        log.info("REST: Nueva versión creada exitosamente - documentoId={}, versionId={}, numeroSecuencial={}", 
            id, response.getId(), response.getNumeroSecuencial());
        
        return ResponseEntity.status(201).body(response);
    }
    
    /**
     * Sanitiza el nombre de archivo para prevenir inyección de headers HTTP.
     * 
     * <p>Elimina caracteres peligrosos que podrían inyectar headers adicionales:
     * <ul>
     *   <li>Comillas dobles (") → Se reemplazan por comillas simples (')</li>
     *   <li>Saltos de línea (\r, \n) → Se eliminan</li>
     *   <li>Caracteres de control → Se eliminan</li>
     * </ul>
     * 
     * <p>Esto previene ataques de HTTP Response Splitting.
     * 
     * @param filename nombre de archivo original
     * @return nombre sanitizado seguro para usar en headers HTTP
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "download";
        }
        
        return filename
            // Eliminar saltos de línea y retornos de carro
            .replaceAll("[\r\n]", "")
            // Reemplazar comillas dobles por comillas simples
            .replace("\"", "'")
            // Eliminar caracteres de control ASCII (0x00-0x1F, 0x7F)
            .replaceAll("[\\x00-\\x1F\\x7F]", "")
            .trim();
    }
}
