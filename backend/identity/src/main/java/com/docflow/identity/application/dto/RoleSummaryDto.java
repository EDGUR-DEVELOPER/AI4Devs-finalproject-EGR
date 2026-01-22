package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para resumen de rol en respuestas de listado de usuarios.
 * Contiene información básica del rol sin incluir detalles de permisos.
 * 
 * @param id Identificador único del rol
 * @param codigo Código alfanumérico del rol (ej: ADMIN, USER, OPERATOR)
 * @param nombre Nombre descriptivo del rol en lenguaje natural
 */
@Schema(description = "Resumen de rol asignado a un usuario")
public record RoleSummaryDto(
    @Schema(description = "ID único del rol", example = "1")
    Integer id,
    
    @Schema(description = "Código alfanumérico del rol", example = "ADMIN")
    String codigo,
    
    @Schema(description = "Nombre descriptivo del rol", example = "Administrador")
    String nombre
) {
}
