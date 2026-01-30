package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.application.dto.PermisoCarpetaUsuarioDTO;
import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * Mapper MapStruct para conversión entre PermisoCarpetaUsuario (JPA) y DTO.
 */
@Mapper(componentModel = "spring")
public interface PermisoCarpetaUsuarioMapper {

    @Mappings({
        @Mapping(target = "fechaCreacion", source = "fechaAsignacion"),
        @Mapping(target = "fechaActualizacion", source = "fechaAsignacion"),
        @Mapping(target = "usuario", ignore = true),
        @Mapping(target = "nivelAcceso", ignore = true)
    })
    PermisoCarpetaUsuarioDTO toDto(PermisoCarpetaUsuario permiso);

    List<PermisoCarpetaUsuarioDTO> toDtoList(List<PermisoCarpetaUsuario> permisos);
}
