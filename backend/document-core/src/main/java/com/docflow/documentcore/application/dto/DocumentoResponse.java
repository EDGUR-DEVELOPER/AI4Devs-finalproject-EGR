package com.docflow.documentcore.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO para respuesta de documento creado/consultado.
 * 
 * US-DOC-001: Información pública del documento sin datos sensibles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoResponse {
    
    private Long id;
    private String nombre;
    private String extension;
    private String tipoContenido;
    private Long tamanioBytes;
    private Long carpetaId;
    private VersionInfoDTO versionActual;
    private Integer numeroVersiones;
    private Boolean bloqueado;
    private String[] etiquetas;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
    
    /**
     * Información básica de la versión actual.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionInfoDTO {
        private Long id;
        private Integer numeroSecuencial;
        private Long tamanioBytes;
        private String hashContenido;
        private OffsetDateTime fechaCreacion;
    }
}
