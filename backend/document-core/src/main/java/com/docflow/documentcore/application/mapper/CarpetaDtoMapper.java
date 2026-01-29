package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.application.dto.CarpetaDTO;
import com.docflow.documentcore.domain.model.Carpeta;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper MapStruct para conversión entre Carpeta (dominio) y CarpetaDTO (API).
 * 
 * <p>Transforma modelos de dominio en DTOs para exposición REST.</p>
 *
 * @author DocFlow Team
 */
@Mapper(componentModel = "spring")
public interface CarpetaDtoMapper {
    
    /**
     * Convierte Carpeta de dominio a DTO de respuesta.
     * 
     * @param carpeta modelo de dominio
     * @return DTO para respuesta API
     */
    @Mapping(target = "esRaiz", expression = "java(carpeta.esRaiz())")
    CarpetaDTO toDto(Carpeta carpeta);
    
    /**
     * Convierte lista de Carpeta a lista de CarpetaDTO.
     * 
     * @param carpetas lista de modelos de dominio
     * @return lista de DTOs
     */
    List<CarpetaDTO> toDtoList(List<Carpeta> carpetas);
}
