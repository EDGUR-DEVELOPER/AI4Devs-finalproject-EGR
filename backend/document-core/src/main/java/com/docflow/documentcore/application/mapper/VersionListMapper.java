package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.application.dto.VersionItemResponse;
import com.docflow.documentcore.domain.model.Version;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper de MapStruct para convertir entidades Version a DTOs VersionItemResponse.
 * 
 * US-DOC-004: Mapper para listado de historial de versiones.
 * Maneja el cálculo del flag esVersionActual y la información del creador.
 */
@Mapper(componentModel = "spring")
@Component
public interface VersionListMapper {
    
    /**
     * Convierte una lista de entidades Version a DTOs VersionItemResponse.
     * 
     * @param versiones Lista de entidades Version a convertir
     * @param versionActualId ID de la versión actual del documento
     * @return Lista de DTOs con información completa para el listado
     */
    default List<VersionItemResponse> toItemResponseList(List<Version> versiones, Long versionActualId) {
        return versiones.stream()
                .map(version -> toItemResponse(version, versionActualId))
                .collect(Collectors.toList());
    }
    
    /**
     * Convierte una entidad Version a DTO VersionItemResponse.
     * 
     * El flag esVersionActual se calcula comparando el ID de la versión con versionActualId.
     * La información del creador se mapea utilizando el método helper mapCreadorInfo.
     * 
     * @param version Entidad Version a convertir
     * @param versionActualId ID de la versión actual del documento para cálculo del flag
     * @return DTO con información completa de la versión
     */
    @Mapping(target = "esVersionActual", expression = "java(version.getId().equals(versionActualId))")
    @Mapping(target = "creadoPor", source = "version", qualifiedByName = "mapCreadorInfo")
    VersionItemResponse toItemResponse(Version version, Long versionActualId);
    
    /**
     * Mapea información del creador de la versión.
     * 
     * MVP: Retorna información básica del creador con solo el ID poblado.
     * Los campos nombreCompleto y email usan placeholders basados en el ID.
     * 
     * TODO: Integrar con servicio de identidad para obtener datos reales del usuario.
     * Opciones de implementación futura:
     * - Opción A: Llamada REST síncrona at identity-service
     * - Opción B: JOIN con tabla de usuarios (requiere replica read de identityDB)
     * - Opción C: Cache distribuido (Redis) con datos de usuarios
     * - Opción D: Enriquecimiento asíncrono en capa de aplicación
     * 
     * @param version Entidad Version con ID del creador
     * @return DTO CreadorInfo con información del usuario
     */
    @Named("mapCreadorInfo")
    default VersionItemResponse.CreadorInfo mapCreadorInfo(Version version) {
        return VersionItemResponse.CreadorInfo.builder()
                .id(version.getCreadoPor())
                .nombreCompleto("Usuario " + version.getCreadoPor()) // TODO: Obtener de identity-service
                .email("user" + version.getCreadoPor() + "@docflow.local") // TODO: Obtener de identity-service
                .build();
    }
}
