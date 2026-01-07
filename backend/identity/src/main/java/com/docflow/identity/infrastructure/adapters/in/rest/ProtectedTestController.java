package com.docflow.identity.infrastructure.adapters.in.rest;

import com.docflow.identity.application.services.JwtTokenService.TokenValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller de prueba para verificar el funcionamiento del middleware JWT.
 * Este endpoint está protegido y requiere un token JWT válido.
 * 
 * Implementa el smoke test requerido en US-AUTH-003.
 */
@RestController
@RequestMapping("/api/v1/protected")
@Tag(name = "Protected Test", description = "Endpoints protegidos para smoke testing")
@Slf4j
public class ProtectedTestController {

    /**
     * Endpoint de prueba que requiere autenticación JWT.
     * Retorna la información del usuario autenticado extraída del token.
     * 
     * @param tokenValidation datos del usuario extraídos del JWT por el filtro
     * @return información del usuario autenticado
     */
    @GetMapping("/test")
    @Operation(
        summary = "Smoke test de autenticación JWT",
        description = "Endpoint protegido que verifica que el middleware JWT funciona correctamente. " +
                     "Retorna 401 sin token válido y 200 con los datos del usuario si el token es válido.",
        security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Map<String, Object>> testProtectedEndpoint(
            @AuthenticationPrincipal TokenValidationResult tokenValidation
    ) {
        log.info("Acceso autorizado al endpoint de prueba - Usuario: {}, Org: {}", 
            tokenValidation.usuarioId(), 
            tokenValidation.organizacionId());

        return ResponseEntity.ok(Map.of(
            "message", "Acceso concedido - JWT válido",
            "userId", tokenValidation.usuarioId(),
            "organizationId", tokenValidation.organizacionId(),
            "roles", tokenValidation.roles(),
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Endpoint adicional de prueba para verificar autorización por roles.
     * Requiere el rol ADMIN.
     */
    @GetMapping("/admin-only")
    @Operation(
        summary = "Test de autorización por rol",
        description = "Endpoint que requiere el rol ADMIN. Útil para probar futura autorización.",
        security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<Map<String, String>> adminOnlyEndpoint(
            @AuthenticationPrincipal TokenValidationResult tokenValidation
    ) {
        // Verificación manual de rol (en el futuro se usará @PreAuthorize)
        var hasAdminRole = tokenValidation.roles().stream()
                .anyMatch(role -> role.equalsIgnoreCase("ADMIN"));

        if (!hasAdminRole) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Forbidden",
                "message", "Requiere rol ADMIN"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "message", "Acceso administrativo concedido",
            "userId", String.valueOf(tokenValidation.usuarioId())
        ));
    }
}
