package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO con metadata de paginación para respuestas de listados.
 * Incluye información sobre el total de elementos, página actual y límites.
 * 
 * @param total Total de elementos que cumplen los criterios de búsqueda
 * @param pagina Número de página actual (base 1)
 * @param limite Cantidad máxima de elementos por página
 * @param totalPaginas Total de páginas disponibles basado en total/limite
 */
@Schema(description = "Metadata de paginación para respuestas de listados")
public record PaginationMetadataDto(
    @Schema(description = "Total de elementos disponibles", example = "45")
    Integer total,
    
    @Schema(description = "Número de página actual (base 1)", example = "1")
    Integer pagina,
    
    @Schema(description = "Cantidad de elementos por página", example = "20")
    Integer limite,
    
    @Schema(description = "Total de páginas disponibles", example = "3")
    Integer totalPaginas
) {
}
