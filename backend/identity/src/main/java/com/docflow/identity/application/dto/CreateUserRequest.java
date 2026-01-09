package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Record que representa la solicitud de creación de un nuevo usuario.
 * Incluye validaciones para garantizar la integridad de los datos de entrada.
 * 
 * @param email Email único del usuario (será normalizado a lowercase)
 * @param nombreCompleto Nombre completo del usuario
 * @param password Contraseña inicial (será hasheada con BCrypt antes de persistir)
 */
@Schema(description = "Solicitud de creación de usuario")
public record CreateUserRequest(
    
    @Schema(description = "Email del usuario", example = "nuevo.usuario@docflow.com")
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    String email,
    
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez García")
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String nombreCompleto,
    
    @Schema(description = "Contraseña inicial del usuario", example = "SecurePass123!")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
    )
    String password
) {}
