package com.docflow.documentcore.presentation.controller;

import com.docflow.documentcore.application.dto.CreateDocumentoRequest;
import com.docflow.documentcore.application.dto.DocumentoResponse;
import com.docflow.documentcore.application.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador REST para gestión de documentos.
 * 
 * US-DOC-001: Endpoint para carga de documentos.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/folders/{folderId}/documents")
@RequiredArgsConstructor
@Tag(name = "Documentos", description = "Gestión de documentos y archivos")
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * Carga un nuevo documento en una carpeta.
     * 
     * @param folderId ID de la carpeta destino
     * @param file Archivo a cargar
     * @param comentarioCambio Comentario opcional para la versión inicial
     * @param nombrePersonalizado Nombre personalizado opcional
     * @return DocumentoResponse con información del documento creado (201)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Cargar documento a carpeta",
        description = "Crea un nuevo documento en la carpeta especificada con su versión inicial (v1). " +
                     "Requiere permiso de ESCRITURA o ADMINISTRACION en la carpeta."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Documento creado exitosamente",
            content = @Content(schema = @Schema(implementation = DocumentoResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Archivo inválido o datos de entrada incorrectos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Sin permisos para crear documentos en esta carpeta"),
        @ApiResponse(responseCode = "404", description = "Carpeta no encontrada"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<DocumentoResponse> uploadDocument(
        @PathVariable Long folderId,
        @RequestPart("file") @Valid MultipartFile file,
        @RequestPart(required = false) String comentarioCambio,
        @RequestPart(required = false) String nombrePersonalizado
    ) {
        log.info("Solicitud de carga de documento a carpeta {}: archivo={}, tamaño={}", 
            folderId, file.getOriginalFilename(), file.getSize());
        
        // Construir request
        CreateDocumentoRequest request = new CreateDocumentoRequest();
        request.setFile(file);
        request.setComentarioCambio(comentarioCambio);
        request.setNombrePersonalizado(nombrePersonalizado);
        
        // Crear documento
        DocumentoResponse response = documentService.createDocument(folderId, request);
        
        log.info("Documento {} creado exitosamente en carpeta {}", response.getId(), folderId);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
}
