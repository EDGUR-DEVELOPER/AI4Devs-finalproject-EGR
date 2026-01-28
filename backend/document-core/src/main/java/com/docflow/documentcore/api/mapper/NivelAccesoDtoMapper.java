package com.docflow.documentcore.api.mapper;

import com.docflow.documentcore.api.dto.NivelAccesoDTO;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct Mapper for NivelAcceso <-> NivelAccesoDTO conversion
 */
@Mapper(componentModel = "spring")
public interface NivelAccesoDtoMapper {
    
    @Mapping(target = "codigo", expression = "java(mapCodigoToString(domain.getCodigo()))")
    NivelAccesoDTO toDto(NivelAcceso domain);
    
    @Mapping(target = "codigo", expression = "java(mapStringToCodigo(dto.getCodigo()))")
    NivelAcceso toDomain(NivelAccesoDTO dto);
    
    default String mapCodigoToString(CodigoNivelAcceso codigo) {
        return codigo != null ? codigo.getCodigo() : null;
    }
    
    default CodigoNivelAcceso mapStringToCodigo(String codigo) {
        return codigo != null ? CodigoNivelAcceso.fromCodigo(codigo) : null;
    }
}
