package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * DTO de respuesta para el endpoint de listado de usuarios.
 * Contiene la lista de usuarios con sus roles y metadata de paginación.
 * 
 * @param usuarios Lista de usuarios con sus roles asignados
 * @param paginacion Metadata de paginación (total, página, límite, totalPáginas)
 */
@Schema(description = "Respuesta del listado de usuarios con paginación")
public record ListUsersResponseDto(
    @Schema(description = "Lista de usuarios con sus roles")
    List<UserWithRolesDto> usuarios,
    
    @Schema(description = "Información de paginación")
    PaginationMetadataDto paginacion
) {
}
