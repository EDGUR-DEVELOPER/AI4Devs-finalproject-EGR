package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para la solicitud de login.
 */
@Schema(description = "Solicitud de autenticación de usuario")
public record LoginRequest(
    
    @Schema(description = "Email del usuario", example = "user@docflow.com")
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    String email,
    
    @Schema(description = "Contraseña del usuario", example = "SecurePass123!")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    String password
) {}
