package com.docflow.identity.infrastructure.adapters.rest;

import com.docflow.identity.application.dto.LoginRequest;
import com.docflow.identity.application.dto.LoginResponse;
import com.docflow.identity.application.dto.SwitchOrganizationRequest;
import com.docflow.identity.application.services.AuthenticationUseCase;
import com.docflow.identity.application.services.JwtTokenService;
import com.docflow.identity.application.services.OrganizationSwitchUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para endpoints de autenticación.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "Endpoints de autenticación y gestión de sesiones")
public class AuthenticationController {

    private final AuthenticationUseCase authenticationUseCase;
    private final OrganizationSwitchUseCase switchUseCase;
    private final JwtTokenService tokenService;

    /**
     * Endpoint de login multi-organización.
     * Valida credenciales, resuelve organización y emite token JWT.
     */
    @PostMapping("/login")
    @Operation(
        summary = "Autenticar usuario",
        description = "Valida credenciales y emite token JWT en contexto de organización"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Autenticación exitosa",
            content = @Content(
                schema = @Schema(implementation = LoginResponse.class),
                examples = @ExampleObject(
                    name = "Login exitoso",
                    value = """
                        {
                          "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "tipoToken": "Bearer",
                          "expiraEn": 86400,
                          "organizacionId": 1
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Error de validación en los datos de entrada",
            content = @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(
                    name = "Validación fallida",
                    value = """
                        {
                          "type": "https://docflow.com/errors/validation-error",
                          "title": "Error de Validación",
                          "status": 400,
                          "detail": "Error de validación en los datos de entrada",
                          "instance": "/api/v1/auth/login",
                          "codigo": "VALIDATION_ERROR",
                          "errors": {
                            "email": "El email es obligatorio",
                            "password": "La contraseña debe tener al menos 8 caracteres"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas",
            content = @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(
                    name = "Credenciales incorrectas",
                    value = """
                        {
                          "type": "https://docflow.com/errors/credenciales-invalidas",
                          "title": "Credenciales Inválidas",
                          "status": 401,
                          "detail": "Credenciales inválidas",
                          "instance": "/api/v1/auth/login",
                          "codigo": "CREDENCIALES_INVALIDAS"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuario sin organizaciones activas",
            content = @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(
                    name = "Sin organización",
                    value = """
                        {
                          "type": "https://docflow.com/errors/sin-organizacion",
                          "title": "Sin Organización",
                          "status": 403,
                          "detail": "El usuario no tiene organizaciones activas",
                          "instance": "/api/v1/auth/login",
                          "codigo": "SIN_ORGANIZACION"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Configuración de organizaciones inválida",
            content = @Content(
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(
                    name = "Configuración inválida",
                    value = """
                        {
                          "type": "https://docflow.com/errors/organizacion-config-invalida",
                          "title": "Configuración de Organización Inválida",
                          "status": 409,
                          "detail": "Usuario con múltiples organizaciones debe tener una predeterminada",
                          "instance": "/api/v1/auth/login",
                          "codigo": "ORGANIZACION_CONFIG_INVALIDA"
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var response = authenticationUseCase.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para cambiar de organización.
     * Requiere token JWT válido en el header Authorization.
     */
    @PostMapping("/switch")
    @Operation(
        summary = "Cambiar organización",
        description = "Cambia el contexto de organización del usuario emitiendo un nuevo token"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Cambio exitoso",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token inválido o expirado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuario no pertenece a la organización solicitada",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<LoginResponse> switchOrganization(
            @Valid @RequestBody SwitchOrganizationRequest request,
            @RequestHeader("Authorization") String authHeader) {

        // Extract and validate token
        String token = extractToken(authHeader);
        var validation = tokenService.validateToken(token);

        if (!validation.isValid()) {
            return ResponseEntity.status(401).build();
        }

        var response = switchUseCase.switchOrganization(validation.usuarioId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Extrae el token JWT del header Authorization.
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}
