package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.application.dto.VersionResponse;
import com.docflow.documentcore.domain.model.Version;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre entidad Version y VersionResponse DTO.
 * 
 * US-DOC-003: Facilita la transformación de entidades de dominio a DTOs de respuesta.
 */
@Component
public class VersionMapper {
    
    /**
     * Convierte una entidad Version a VersionResponse DTO.
     *
     * @param version Entidad de dominio
     * @param esVersionActual Indica si esta es la versión actual del documento
     * @return DTO de respuesta
     */
    public VersionResponse toResponse(Version version, boolean esVersionActual) {
        if (version == null) {
            return null;
        }
        
        return VersionResponse.builder()
                .id(version.getId())
                .documentoId(version.getDocumentoId())
                .numeroSecuencial(version.getNumeroSecuencial())
                .tamanioBytes(version.getTamanioBytes())
                .hashContenido(version.getHashContenido())
                .comentarioCambio(version.getComentarioCambio())
                .creadoPor(version.getCreadoPor())
                .fechaCreacion(version.getFechaCreacion())
                .esVersionActual(esVersionActual)
                .build();
    }
}
