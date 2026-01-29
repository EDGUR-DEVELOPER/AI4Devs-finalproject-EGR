package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.infrastructure.adapter.entity.CarpetaEntity;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper MapStruct para conversión entre CarpetaEntity (JPA) y Carpeta (dominio).
 * 
 * <p>MapStruct genera la implementación automáticamente en tiempo de compilación,
 * asegurando conversiones type-safe y eficientes.</p>
 *
 * <p><strong>Configuración:</strong>
 * <ul>
 *   <li>componentModel = "spring": Genera un @Component Spring</li>
 *   <li>Mapeo automático de campos con mismo nombre</li>
 *   <li>Mapeo explícito cuando los nombres difieren</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@Mapper(componentModel = "spring")
public interface CarpetaMapper {
    
    /**
     * Convierte una CarpetaEntity (infraestructura) a Carpeta (dominio).
     * 
     * @param entity entidad JPA
     * @return modelo de dominio inmutable
     */
    @Mapping(target = "carpetaPadreId", source = "carpetaPadreId")
    Carpeta toDomain(CarpetaEntity entity);
    
    /**
     * Convierte una Carpeta (dominio) a CarpetaEntity (infraestructura).
     * 
     * @param carpeta modelo de dominio
     * @return entidad JPA
     */
    @Mapping(target = "carpetaPadre", ignore = true)  // No mapeamos la relación completa
    CarpetaEntity toEntity(Carpeta carpeta);
    
    /**
     * Convierte una lista de CarpetaEntity a lista de Carpeta.
     * 
     * @param entities lista de entidades JPA
     * @return lista de modelos de dominio
     */
    List<Carpeta> toDomainList(List<CarpetaEntity> entities);
    
    /**
     * Convierte una lista de Carpeta a lista de CarpetaEntity.
     * 
     * @param carpetas lista de modelos de dominio
     * @return lista de entidades JPA
     */
    List<CarpetaEntity> toEntityList(List<Carpeta> carpetas);
}
