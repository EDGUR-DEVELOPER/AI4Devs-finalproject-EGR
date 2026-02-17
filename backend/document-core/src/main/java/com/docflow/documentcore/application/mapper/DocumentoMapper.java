package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.application.dto.DocumentoResponse;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.Version;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct para conversiones entre entidades de dominio y DTOs.
 * 
 * US-DOC-001: Conversión type-safe entre capas.
 */
@Mapper(componentModel = "spring")
public interface DocumentoMapper {
    
    /**
     * Convierte entidad Documento a DTO de respuesta.
     */
    @Mapping(source = "tamanioBytes", target = "tamanioBytes")
    @Mapping(source = "tipoContenido", target = "tipoContenido")
    DocumentoResponse toResponse(Documento documento);
    
    /**
     * Convierte Version a DTO anidado de versión.
     */
    DocumentoResponse.VersionInfoDTO toVersionInfoDTO(Version version);
}
