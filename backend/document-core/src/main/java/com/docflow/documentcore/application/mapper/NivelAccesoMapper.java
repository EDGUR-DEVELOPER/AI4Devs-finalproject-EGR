package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import com.docflow.documentcore.infrastructure.adapter.entity.NivelAccesoEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct Mapper for NivelAcceso <-> NivelAccesoEntity conversion
 */
@Mapper(componentModel = "spring")
public interface NivelAccesoMapper {
    
    @Mapping(target = "codigo", expression = "java(mapCodigoToString(domain.getCodigo()))")
    NivelAccesoEntity toEntity(NivelAcceso domain);
    
    @Mapping(target = "codigo", expression = "java(mapStringToCodigo(entity.getCodigo()))")
    NivelAcceso toDomain(NivelAccesoEntity entity);
    
    default String mapCodigoToString(CodigoNivelAcceso codigo) {
        return codigo != null ? codigo.getCodigo() : null;
    }
    
    default CodigoNivelAcceso mapStringToCodigo(String codigo) {
        return codigo != null ? CodigoNivelAcceso.fromCodigo(codigo) : null;
    }
}
