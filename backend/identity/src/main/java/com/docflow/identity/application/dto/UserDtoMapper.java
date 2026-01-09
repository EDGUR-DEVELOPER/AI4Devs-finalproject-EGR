package com.docflow.identity.application.dto;

import com.docflow.identity.domain.model.Rol;
import com.docflow.identity.domain.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Mapper MapStruct para conversión entre entidades de dominio y DTOs de usuarios.
 * Configurado como componente Spring para inyección de dependencias.
 * Política de unmapped targets en IGNORE para evitar advertencias en campos derivados.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserDtoMapper {
    
    /**
     * Mapea un usuario con su lista de roles a UserWithRolesDto.
     * Los roles se pasan como lista ya procesada para evitar dependencias circulares.
     * 
     * @param usuario Entidad de usuario de dominio
     * @param roles Lista de resúmenes de roles ya mapeados
     * @return DTO con información del usuario y sus roles
     */
    @Mapping(target = "id", source = "usuario.id")
    @Mapping(target = "email", source = "usuario.email")
    @Mapping(target = "nombreCompleto", source = "usuario.nombreCompleto")
    @Mapping(target = "fechaCreacion", source = "usuario.fechaCreacion")
    @Mapping(target = "roles", source = "roles")
    UserWithRolesDto toUserWithRolesDto(Usuario usuario, List<RoleSummaryDto> roles);
    
    /**
     * Mapea una entidad Rol a RoleSummaryDto.
     * 
     * @param rol Entidad de rol de dominio
     * @return DTO con resumen del rol
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "codigo", source = "codigo")
    @Mapping(target = "nombre", source = "nombre")
    RoleSummaryDto toRoleSummaryDto(Rol rol);
}
