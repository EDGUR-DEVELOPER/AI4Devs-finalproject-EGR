package com.docflow.documentcore.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para información de versión de documento.
 * 
 * US-DOC-003: Contiene los datos de una versión después de ser creada o consultada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {
    
    /**
     * ID único de la versión.
     */
    private Long id;
    
    /**
     * ID del documento padre.
     */
    private Long documentoId;
    
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
     * Comentario de cambios de esta versión.
     */
    private String comentarioCambio;
    
    /**
     * ID del usuario que creó esta versión.
     */
    private Long creadoPor;
    
    /**
     * Fecha de creación de la versión.
     */
    private OffsetDateTime fechaCreacion;
    
    /**
     * Indica si esta es la versión actual del documento.
     */
    private Boolean esVersionActual;
}
