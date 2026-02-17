package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.PermisoEfectivoDTO;
import com.docflow.documentcore.application.mapper.PermisoEfectivoMapper;
import com.docflow.documentcore.application.service.EvaluadorPermisosService;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para consultar permisos efectivos.
 * 
 * <p>Proporciona endpoints para que los usuarios consulten sus propios permisos efectivos
 * sobre recursos (documentos y carpetas). Esto es útil para:</p>
 * <ul>
 *   <li>La UI para mostrar/ocultar botones de acción basados en permisos</li>
 *   <li>Depurar problemas de permisos</li>
 *   <li>Aplicaciones móviles para verificar permisos antes de intentar operaciones</li>
 * </ul>
 * 
 * <h3>Nota de Implementación</h3>
 * <p>Estos endpoints NO usan la anotación {@code @RequierePermiso} porque
 * verifican los permisos internamente. Devolver 403 cuando no existe permiso
 * es el comportamiento esperado.</p>
 * 
 * @see EvaluadorPermisosService
 */
@RestController
@RequestMapping("/api/permisos")
@RequiredArgsConstructor
@Tag(name = "Permisos", description = "API para consultar permisos efectivos")
@SecurityRequirement(name = "bearer-jwt")
public class PermisoController {

    private final EvaluadorPermisosService evaluadorPermisos;
    private final PermisoEfectivoMapper mapper;

    /**
     * Consulta el permiso efectivo del usuario autenticado sobre un documento.
     * 
     * <h3>Códigos de Respuesta</h3>
     * <ul>
     *   <li><strong>200 OK</strong>: El usuario tiene permiso, devuelve detalles del permiso</li>
     *   <li><strong>403 FORBIDDEN</strong>: El usuario no tiene permiso sobre este documento</li>
     * </ul>
     */
    @GetMapping("/documentos/{documentoId}/mi-permiso")
    @Operation(
        summary = "Consultar mi permiso sobre un documento",
        description = "Devuelve el permiso efectivo del usuario autenticado sobre un documento específico, incluyendo el origen del permiso (ACL de documento, ACL de carpeta, o heredado)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Permiso encontrado",
            content = @Content(schema = @Schema(implementation = PermisoEfectivoDTO.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "El usuario no tiene permiso sobre este documento"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Documento no encontrado"
        )
    })
    public ResponseEntity<PermisoEfectivoDTO> obtenerMiPermisoDocumento(
            @PathVariable 
            @Parameter(description = "ID del documento")
            Long documentoId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1")
            @Parameter(description = "ID de usuario del token JWT", hidden = true)
            Long usuarioId,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1")
            @Parameter(description = "ID de organización del token JWT", hidden = true)
            Long organizacionId
    ) {
        PermisoEfectivo permiso = evaluadorPermisos.evaluarPermisoDocumento(
                usuarioId,
                documentoId,
                organizacionId
        );

        if (permiso == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PermisoEfectivoDTO dto = mapper.toDto(permiso);
        return ResponseEntity.ok(dto);
    }

    /**
     * Consulta el permiso efectivo del usuario autenticado sobre una carpeta.
     * 
     * <h3>Códigos de Respuesta</h3>
     * <ul>
     *   <li><strong>200 OK</strong>: El usuario tiene permiso, devuelve detalles del permiso</li>
     *   <li><strong>403 FORBIDDEN</strong>: El usuario no tiene permiso sobre esta carpeta</li>
     * </ul>
     */
    @GetMapping("/carpetas/{carpetaId}/mi-permiso")
    @Operation(
        summary = "Consultar mi permiso sobre una carpeta",
        description = "Devuelve el permiso efectivo del usuario autenticado sobre una carpeta específica, incluyendo el origen del permiso (directo o heredado)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Permiso encontrado",
            content = @Content(schema = @Schema(implementation = PermisoEfectivoDTO.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "El usuario no tiene permiso sobre esta carpeta"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Carpeta no encontrada"
        )
    })
    public ResponseEntity<PermisoEfectivoDTO> obtenerMiPermisoCarpeta(
            @PathVariable
            @Parameter(description = "ID de la carpeta")
            Long carpetaId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1")
            @Parameter(description = "ID de usuario del token JWT", hidden = true)
            Long usuarioId,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1")
            @Parameter(description = "ID de organización del token JWT", hidden = true)
            Long organizacionId
    ) {
        PermisoEfectivo permiso = evaluadorPermisos.evaluarPermisoCarpeta(
                usuarioId,
                carpetaId,
                organizacionId
        );

        if (permiso == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PermisoEfectivoDTO dto = mapper.toDto(permiso);
        return ResponseEntity.ok(dto);
    }
}
