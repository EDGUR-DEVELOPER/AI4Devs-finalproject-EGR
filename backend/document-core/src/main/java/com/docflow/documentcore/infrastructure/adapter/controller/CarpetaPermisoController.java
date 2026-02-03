package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.PermisoEfectivoDTO;
import com.docflow.documentcore.application.service.PermisoHerenciaService;
import com.docflow.documentcore.domain.exception.carpeta.SinPermisoCarpetaException;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Controlador REST para consultar el permiso efectivo del usuario en una carpeta.
 */
@RestController
@RequestMapping("/api/carpetas")
@Tag(name = "ACL - Permisos efectivos", description = "Consulta de permisos directos o heredados en carpetas")
@SecurityRequirement(name = "bearer-jwt")
public class CarpetaPermisoController {

    private final PermisoHerenciaService permisoHerenciaService;

    public CarpetaPermisoController(PermisoHerenciaService permisoHerenciaService) {
        this.permisoHerenciaService = permisoHerenciaService;
    }

    @GetMapping("/{carpetaId}/mi-permiso")
    @Operation(
            summary = "Obtener permiso efectivo sobre una carpeta",
            description = "Retorna el nivel de acceso efectivo del usuario autenticado, directo o heredado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Permiso efectivo encontrado",
                    content = @Content(schema = @Schema(implementation = PermisoEfectivoDTO.class))
            ),
            @ApiResponse(responseCode = "403", description = "Sin permiso sobre la carpeta"),
            @ApiResponse(responseCode = "404", description = "Carpeta no encontrada")
    })
    public ResponseEntity<PermisoEfectivoDTO> obtenerPermisoEfectivo(
            @PathVariable Long carpetaId,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioId
    ) {
        Optional<PermisoEfectivo> permisoOpt = permisoHerenciaService.evaluarPermisoEfectivo(
                usuarioId,
                carpetaId,
                organizacionId
        );

        if (permisoOpt.isEmpty()) {
            throw new SinPermisoCarpetaException("No tienes permiso para acceder a esta carpeta");
        }

        PermisoEfectivo permiso = permisoOpt.get();
        PermisoEfectivoDTO response = new PermisoEfectivoDTO();
        response.setNivelAcceso(permiso.getNivelAcceso().name());
        response.setEsHeredado(permiso.isEsHeredado());
        response.setCarpetaOrigenId(permiso.getCarpetaOrigenId());
        response.setCarpetaOrigenNombre(permiso.getCarpetaOrigenNombre());
        response.setRutaHerencia(permiso.getRutaHerencia());

        return ResponseEntity.ok(response);
    }
}
