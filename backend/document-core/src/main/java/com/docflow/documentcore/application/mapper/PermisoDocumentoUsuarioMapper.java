package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.application.dto.PermisoDocumentoUsuarioDTO;
import com.docflow.documentcore.domain.model.permiso.PermisoDocumentoUsuario;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper para convertir entidades PermisoDocumentoUsuario a DTOs de respuesta.
 * 
 * MapStruct genera la implementación automáticamente en tiempo de compilación.
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PermisoDocumentoUsuarioMapper {
    
    /**
     * Mapea entidad a DTO de respuesta.
     * 
     * La propiedad 'usuario' (UsuarioResumenDTO) se enriquece en el controlador,
     * no en el mapper.
     */
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "nivelAcceso", ignore = true)
    PermisoDocumentoUsuarioDTO toDto(PermisoDocumentoUsuario entity);
    
    /**
     * Mapea lista de entidades a lista de DTOs.
     */
    List<PermisoDocumentoUsuarioDTO> toDtoList(List<PermisoDocumentoUsuario> entities);
}
