package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para solicitud de asignación de rol a usuario.
 * Utilizado por administradores para asignar roles dentro de su organización.
 */
@Schema(description = "Solicitud de asignación de rol a usuario en la organización")
public record AssignRoleRequest(
    
    @Schema(
        description = "ID del rol a asignar (global o de la organización)",
        example = "2"
    )
    @NotNull(message = "El ID del rol es obligatorio")
    @Positive(message = "El ID del rol debe ser un número positivo")
    Integer rolId
    
) {}
