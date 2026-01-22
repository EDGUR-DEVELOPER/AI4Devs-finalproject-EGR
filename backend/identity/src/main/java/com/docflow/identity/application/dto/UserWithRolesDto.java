package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO para representar un usuario con sus roles asignados en una organización.
 * Usado en endpoints de listado de usuarios para administradores.
 * 
 * @param id Identificador único del usuario
 * @param email Correo electrónico del usuario
 * @param nombreCompleto Nombre completo del usuario
 * @param estado Estado de membresía del usuario en la organización (ACTIVO, SUSPENDIDO)
 * @param roles Lista de roles asignados al usuario en la organización
 * @param fechaCreacion Fecha de creación del usuario en el sistema
 */
@Schema(description = "Usuario con sus roles asignados en la organización")
public record UserWithRolesDto(
    @Schema(description = "ID único del usuario", example = "101")
    Long id,
    
    @Schema(description = "Email del usuario", example = "admin@acme.com")
    String email,
    
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
    String nombreCompleto,
    
    @Schema(description = "Estado de membresía en la organización", example = "ACTIVO", allowableValues = {"ACTIVO", "SUSPENDIDO"})
    String estado,
    
    @Schema(description = "Lista de roles asignados al usuario")
    List<RoleSummaryDto> roles,
    
    @Schema(description = "Fecha de creación del usuario", example = "2024-01-15T10:30:00Z")
    OffsetDateTime fechaCreacion
) {
}
