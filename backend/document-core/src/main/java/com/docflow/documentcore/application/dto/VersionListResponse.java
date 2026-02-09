package com.docflow.documentcore.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para lista de versiones de un documento.
 * 
 * US-DOC-004: Incluye lista completa de versiones con metadatos de paginación opcional.
 * Cuando no se solicita paginación, el campo paginacion es null.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionListResponse {
    
    /**
     * Lista de versiones ordenadas ascendentemente por número secuencial.
     * Contiene todas las versiones si no hay paginación, o solo la página solicitada.
     */
    private List<VersionItemResponse> versiones;
    
    /**
     * ID del documento al que pertenecen las versiones.
     */
    private Long documentoId;
    
    /**
     * Total de versiones del documento (independiente de la paginación).
     */
    private Integer totalVersiones;
    
    /**
     * Metadatos de paginación.
     * Es null cuando no se proporcionan parámetros de paginación.
     */
    private PaginacionInfo paginacion;
    
    /**
     * DTO anidado con información de paginación.
     * Solo se incluye cuando la petición incluye parámetros de paginación.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginacionInfo {
        
        /**
         * Número de página actual (base 1: primera página = 1).
         */
        private Integer paginaActual;
        
        /**
         * Tamaño de página (cantidad de elementos por página).
         */
        private Integer tamanio;
        
        /**
         * Total de páginas disponibles.
         */
        private Integer totalPaginas;
        
        /**
         * Total de elementos/versiones en el dataset completo.
         */
        private Integer totalElementos;
        
        /**
         * Indica si esta es la primera página.
         */
        private Boolean primeraPagina;
        
        /**
         * Indica si esta es la última página.
         */
        private Boolean ultimaPagina;
    }
}
