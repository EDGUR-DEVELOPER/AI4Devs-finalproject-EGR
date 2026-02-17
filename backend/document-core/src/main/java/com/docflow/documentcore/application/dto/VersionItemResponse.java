package com.docflow.documentcore.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para elemento individual de versión en lista de historial.
 * 
 * US-DOC-004: Contiene información completa de una versión para listado de historial,
 * incluyendo datos del creador y estadísticas de descarga.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionItemResponse {
    
    /**
     * ID único de la versión.
     */
    private Long id;
    
    /**
     * Número secuencial de la versión (1, 2, 3, ...).
     */
    private Integer numeroSecuencial;
    
    /**
     * Tamaño del archivo en bytes.
     */
    private Long tamanioBytes;
    
    /**
     * Hash SHA256 del contenido del archivo.
     */
    private String hashContenido;
    
    /**
     * Comentario de cambios de esta versión (puede ser null).
     */
    private String comentarioCambio;
    
    /**
     * Información del usuario que creó esta versión.
     */
    private CreadorInfo creadoPor;
    
    /**
     * Fecha de creación de la versión.
     */
    private OffsetDateTime fechaCreacion;
    
    /**
     * Contador de descargas de esta versión.
     */
    private Integer descargas;
    
    /**
     * Fecha de la última descarga (puede ser null si nunca se descargó).
     */
    private OffsetDateTime ultimaDescargaEn;
    
    /**
     * Flag que indica si esta es la versión actual del documento.
     * Solo una versión por documento debe tener este valor en true.
     */
    private Boolean esVersionActual;
    
    /**
     * DTO anidado con información del creador de la versión.
     * 
     * Para MVP, contiene información básica del usuario.
     * En futuras iteraciones puede enriquecerse con llamadas al servicio de identidad.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreadorInfo {
        
        /**
         * ID del usuario que creó la versión.
         */
        private Long id;
        
        /**
         * Nombre completo del usuario.
         * Para MVP puede ser un placeholder si no hay integración con identity service.
         */
        private String nombreCompleto;
        
        /**
         * Email del usuario.
         * Para MVP puede ser un placeholder si no hay integración con identity service.
         */
        private String email;
    }
}
