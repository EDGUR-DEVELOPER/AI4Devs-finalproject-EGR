package com.docflow.identity.infrastructure.adapters.rest;

import com.docflow.identity.application.dto.CreateUserRequest;
import com.docflow.identity.application.dto.UserCreatedResponse;
import com.docflow.identity.application.services.AdminUserManagementService;
import com.docflow.identity.domain.exceptions.PermisoInsuficienteException;
import com.docflow.identity.application.services.JwtTokenService.TokenValidationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para operaciones administrativas de gestión de usuarios.
 * 
 * Este controlador expone endpoints que requieren autenticación JWT y rol ADMIN
 * en la organización actual del usuario autenticado.
 * 
 * Todos los endpoints respetan el formato ProblemDetail (RFC 7807) para errores.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "User Administration", description = "Endpoints de administración de usuarios (requiere rol ADMIN)")
public class AdminUserController {
    
    private final AdminUserManagementService adminUserService;
    
    /**
     * Crea un nuevo usuario en la organización del administrador autenticado.
     * 
     * El usuario será creado con:
     * - Estado ACTIVO
     * - Contraseña hasheada con BCrypt (cost factor 12)
     * - Asociación automática a la organización del admin (es_predeterminada=true)
     * - MFA deshabilitado por defecto
     * 
     * Validaciones aplicadas:
     * - Email debe ser válido y único en el sistema
     * - Nombre completo debe tener entre 2 y 100 caracteres
     * - Contraseña debe tener mínimo 8 caracteres con mayúscula, minúscula y número
     * 
     * @param request Datos del usuario a crear (email, nombre, contraseña)
     * @param tokenValidation Información del token JWT del administrador autenticado
     * @return Usuario creado con código HTTP 201
     * @throws PermisoInsuficienteException si el usuario no tiene rol ADMIN
     * @throws EmailDuplicadoException si el email ya existe en el sistema
     */
    @PostMapping
    @Operation(
        summary = "Crear usuario en la organización",
        description = "Crea un nuevo usuario y lo asocia a la organización del administrador. " +
                     "Requiere rol ADMIN. La contraseña se hashea con BCrypt antes de persistir."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Error de validación en los datos de entrada"),
        @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
        @ApiResponse(responseCode = "403", description = "Usuario sin rol ADMIN"),
        @ApiResponse(responseCode = "409", description = "Email ya registrado en el sistema")
    })
    public ResponseEntity<UserCreatedResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal TokenValidationResult tokenValidation) {
        
        log.info("Solicitud de creación de usuario por admin: {} (org: {})", 
            tokenValidation.usuarioId(), tokenValidation.organizacionId());
        
        // 1. Verificar rol ADMIN (guard manual hasta implementar @PreAuthorize)
        var hasAdminRole = tokenValidation.roles().stream()
            .anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));
        
        if (!hasAdminRole) {
            log.warn("Usuario {} sin permisos de administrador intentó crear usuario", 
                tokenValidation.usuarioId());
            throw new PermisoInsuficienteException(
                "Se requiere rol ADMIN para crear usuarios en la organización"
            );
        }
        
        // 2. Delegar creación al servicio con organización del token
        var usuarioCreado = adminUserService.createUser(
            request, 
            tokenValidation.organizacionId()
        );
        
        log.info("Usuario creado exitosamente con ID: {}", usuarioCreado.id());
        
        // 3. Retornar 201 Created
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(usuarioCreado);
    }
}
