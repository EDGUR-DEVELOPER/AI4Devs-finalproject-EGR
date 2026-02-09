package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.VersionListResponse;
import com.docflow.documentcore.application.service.DocumentoVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para gestión de versiones de documentos.
 * 
 * <p>US-DOC-004: Expone endpoint para listar el historial completo de versiones
 * de un documento con soporte de paginación opcional.
 * 
 * <p>Todos los endpoints requieren autenticación mediante JWT y headers
 * de contexto (X-User-Id, X-Organization-Id) establecidos por el Gateway.
 */
@Slf4j
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Versiones", description = "Gestión de versiones de documentos")
public class VersionController {
    
    private final DocumentoVersionService documentoVersionService;
    
    /**
     * Lista todas las versiones de un documento ordenadas ascendentemente.
     * 
     * <p>Retorna el historial completo de versiones de un documento, ordenadas
     * por número secuencial ascendente (1, 2, 3, ...). Soporta paginación opcional
     * para documentos con muchas versiones.
     * 
     * <p><b>Reglas de negocio:</b>
     * <ul>
     *   <li>Requiere permiso de LECTURA sobre el documento</li>
     *   <li>Las versiones están ordenadas ascendentemente por número secuencial</li>
     *   <li>Solo una versión tiene esVersionActual = true</li>
     *   <li>La paginación es opcional (null = retorna todas las versiones)</li>
     *   <li>Tamaño de página por defecto: 20, máximo: 100</li>
     * </ul>
     * 
     * @param documentoId ID del documento a consultar
     * @param usuarioId ID del usuario (inyectado desde header X-User-Id)
     * @param organizacionId ID de organización (inyectado desde header X-Organization-Id)
     * @param pagina Número de página (base 1, opcional)
     * @param tamanio Tamaño de página (entre 1 y 100, default 20)
     * @return Lista de versiones con metadatos de paginación si aplica
     */
    @GetMapping("/{documentoId}/versiones")
    @Operation(
        summary = "Listar versiones de documento",
        description = """
            Obtiene el historial completo de versiones de un documento ordenadas ascendentemente
            por número secuencial.
            
            La respuesta incluye:
            - Lista de versiones con información detallada
            - Flag esVersionActual identificando la versión actual
            - Información del creador de cada versión
            - Estadísticas de descarga
            - Metadatos de paginación (si se solicita paginación)
            
            Paginación:
            - Parámetro opcional 'pagina' (base 1: primera página = 1)
            - Parámetro opcional 'tamanio' (default 20, máximo 100)
            - Si no se especifica paginación, retorna todas las versiones
            
            Requiere permiso de LECTURA sobre el documento.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de versiones obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VersionListResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetros de paginación inválidos (pagina < 1 o tamanio fuera de rango 1-100)",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticación no provisto o inválido",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuario no tiene permiso de LECTURA sobre el documento",
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
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<VersionListResponse> listarVersiones(
            @Parameter(description = "ID del documento", required = true)
            @PathVariable Long documentoId,
            
            @Parameter(
                description = "ID del usuario autenticado (inyectado por Gateway)",
                required = true,
                hidden = true
            )
            @RequestHeader("X-User-Id") Long usuarioId,
            
            @Parameter(
                description = "ID de la organización del usuario (inyectado por Gateway)",
                required = true,
                hidden = true
            )
            @RequestHeader("X-Organization-Id") Long organizacionId,
            
            @Parameter(
                description = "Número de página (base 1, opcional). Si no se especifica, retorna todas las versiones",
                example = "1"
            )
            @RequestParam(required = false)
            @Min(value = 1, message = "La página debe ser mayor o igual a 1")
            Integer pagina,
            
            @Parameter(
                description = "Tamaño de página (opcional, default 20, máximo 100)",
                example = "20"
            )
            @RequestParam(required = false, defaultValue = "20")
            @Min(value = 1, message = "El tamaño de página debe ser mayor o igual a 1")
            @Max(value = 100, message = "El tamaño de página debe ser menor o igual a 100")
            Integer tamanio
    ) {
        log.info("GET /api/documentos/{}/versiones - Usuario: {}, Organización: {}, Pagina: {}, Tamaño: {}",
                documentoId, usuarioId, organizacionId, pagina, tamanio);
        
        VersionListResponse response = documentoVersionService.listarVersiones(
                documentoId,
                usuarioId,
                organizacionId,
                pagina,
                tamanio
        );
        
        log.debug("Retornando {} versiones para documento {}",
                response.getVersiones().size(), documentoId);
        
        return ResponseEntity.ok(response);
    }
}
