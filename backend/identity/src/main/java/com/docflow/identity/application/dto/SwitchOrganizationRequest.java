package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para la solicitud de cambio de organización.
 */
@Schema(description = "Solicitud de cambio de organización")
public record SwitchOrganizationRequest(
    
    @Schema(description = "ID de la organización a la que cambiar", example = "2")
    @NotNull(message = "El ID de organización es obligatorio")
    @Positive(message = "El ID de organización debe ser positivo")
    Integer organizacionId
) {}
