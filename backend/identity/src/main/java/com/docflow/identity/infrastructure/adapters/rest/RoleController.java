package com.docflow.identity.infrastructure.adapters.rest;

import com.docflow.identity.application.dto.RoleSummaryDto;
import com.docflow.identity.application.services.JwtTokenService.TokenValidationResult;
import com.docflow.identity.domain.exception.PermisoInsuficienteException;
import com.docflow.identity.application.services.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para consultas de roles por administradores.
 * Expone endpoints protegidos que requieren rol ADMIN.
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Role Administration", description = "Endpoints de consulta de roles (requiere rol ADMIN)")
public class RoleController {

        private final RoleService roleService;

        /**
         * Lista todos los roles disponibles para asignar en la organización del
         * administrador.
         * Incluye roles globales y roles propios de la organización.
         * 
         * @param tokenValidation Información del token JWT del usuario autenticado
         * @return Lista de roles disponibles
         */
        @GetMapping
        @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
        @Operation(summary = "Listar roles disponibles", description = "Retorna la lista de roles que pueden ser asignados en la organización. Incluye roles globales y custom.")
        @SecurityRequirement(name = "bearer-jwt")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente", content = @Content(schema = @Schema(implementation = RoleSummaryDto.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado - Token inválido o expirado"),
                        @ApiResponse(responseCode = "403", description = "Permisos insuficientes - Se requiere rol ADMIN o SUPER_ADMIN")
        })
        public ResponseEntity<List<RoleSummaryDto>> listRoles(
                        @Parameter(description = "Token JWT del usuario autenticado extraído automáticamente", hidden = true)
                        @AuthenticationPrincipal TokenValidationResult tokenValidation) {

                log.info("Solicitud de listado de roles por admin: {} (org: {})",
                                tokenValidation.usuarioId(), tokenValidation.organizacionId());

                // Validación extra de seguridad (aunque PreAuthorize ya debería cubrirlo)
                boolean isAdmin = tokenValidation.roles().stream()
                                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "SUPER_ADMIN".equalsIgnoreCase(r));

                if (!isAdmin) {
                        throw new PermisoInsuficienteException("Se requiere rol ADMIN para listar roles disponibles");
                }

                var roles = roleService.listAvailableRoles(tokenValidation.organizacionId());

                return ResponseEntity.ok(roles);
        }
}
