package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.DocumentoMovidoResponse;
import com.docflow.documentcore.application.dto.MoverDocumentoRequest;
import com.docflow.documentcore.application.service.DocumentoMoverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
