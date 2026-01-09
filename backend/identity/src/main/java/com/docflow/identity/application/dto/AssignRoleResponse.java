package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta tras asignar un rol a un usuario.
 * Incluye información de confirmación y metadatos de la asignación.
 */
@Schema(description = "Respuesta de asignación de rol a usuario")
public record AssignRoleResponse(
    
    @Schema(
        description = "Mensaje de confirmación de la operación",
        example = "Rol asignado correctamente"
    )
    String mensaje,
    
    @Schema(
        description = "ID del usuario al que se asignó el rol",
        example = "100"
    )
    Long usuarioId,
    
    @Schema(
        description = "ID del rol asignado",
        example = "2"
    )
    Integer rolId,
    
    @Schema(
        description = "Nombre del rol asignado",
        example = "ADMIN"
    )
    String nombreRol,
    
    @Schema(
        description = "Fecha y hora de la asignación",
        example = "2026-01-09T10:30:00Z"
    )
    OffsetDateTime fechaAsignacion,
    
    @Schema(
        description = "Indica si se reactivó una asignación previamente desactivada",
        example = "false"
    )
    boolean reactivado
    
) {}
