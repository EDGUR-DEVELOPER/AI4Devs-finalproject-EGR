package com.docflow.documentcore.domain.repository.adapter;

import com.docflow.documentcore.application.mapper.CarpetaMapper;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.CarpetaAncestro;
import com.docflow.documentcore.domain.model.entity.CarpetaEntity;
import com.docflow.documentcore.domain.repository.CarpetaJpaRepository;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador del repositorio de Carpetas (implementación de arquitectura hexagonal).
 * 
 * <p>Esta clase implementa el puerto {@link ICarpetaRepository} definido en el dominio,
 * delegando la persistencia real a Spring Data JPA ({@link CarpetaJpaRepository}).</p>
 *
 * <p><strong>Responsabilidades:</strong>
 * <ul>
 *   <li>Traducir entre modelo de dominio (Carpeta) y entidad JPA (CarpetaEntity)</li>
 *   <li>Orquestar operaciones de persistencia</li>
 *   <li>Garantizar aislamiento multi-tenant en todas las operaciones</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@Component
public class CarpetaRepositoryAdapter implements ICarpetaRepository {
    
    private final CarpetaJpaRepository jpaRepository;
    private final CarpetaMapper mapper;
    
    public CarpetaRepositoryAdapter(CarpetaJpaRepository jpaRepository, CarpetaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Carpeta crear(Carpeta carpeta) {
        CarpetaEntity entity = mapper.toEntity(carpeta);
        CarpetaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Carpeta> obtenerPorId(Long id, Long organizacionId) {
        return jpaRepository.findByIdAndOrganizacionId(id, organizacionId)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Carpeta> obtenerHijos(Long carpetaPadreId, Long organizacionId) {
        List<CarpetaEntity> entities = jpaRepository.findByOrganizacionIdAndCarpetaPadreId(
                organizacionId, 
                carpetaPadreId
        );
        return mapper.toDomainList(entities);
    }
    
    @Override
    public Optional<Carpeta> obtenerRaiz(Long organizacionId) {
        List<CarpetaEntity> raices = jpaRepository.findByOrganizacionIdAndCarpetaPadreIdIsNull(organizacionId);
        
        if (raices.isEmpty()) {
            return Optional.empty();
        }
        
        // Si hay múltiples carpetas raíz, retornar la primera y loguear advertencia
        // Esta es una situación anómala que debería ser corregida en los datos
        if (raices.size() > 1) {
            System.err.println("[ADVERTENCIA] Se encontraron " + raices.size() 
                    + " carpetas raíz para la organización " + organizacionId 
                    + ". Se devolviendo la primera. IDs: " 
                    + raices.stream().map(CarpetaEntity::getId).collect(Collectors.toList()));
        }
        
        return Optional.of(mapper.toDomain(raices.get(0)));
    }
    
    @Override
    public boolean nombreExisteEnNivel(String nombre, Long carpetaPadreId, Long organizacionId) {
        return jpaRepository.existsByNombreEnNivel(organizacionId, carpetaPadreId, nombre);
    }
    
    @Override
    public boolean eliminarLogicamente(Long id, Long organizacionId) {
        Optional<CarpetaEntity> optionalEntity = jpaRepository.findByIdAndOrganizacionId(id, organizacionId);
        
        if (optionalEntity.isPresent()) {
            CarpetaEntity entity = optionalEntity.get();
            entity.setFechaEliminacion(Instant.now());
            jpaRepository.save(entity);
            return true;
        }
        
        return false;
    }
    
    @Override
    public Carpeta actualizar(Carpeta carpeta) {
        CarpetaEntity entity = mapper.toEntity(carpeta);
        CarpetaEntity updatedEntity = jpaRepository.save(entity);
        return mapper.toDomain(updatedEntity);
    }

    @Override
    public List<CarpetaAncestro> obtenerRutaAncestros(Long carpetaId, Long organizacionId) {
        return jpaRepository.findRutaAncestros(carpetaId, organizacionId).stream()
                .map(ancestro -> new CarpetaAncestro(
                        ancestro.getId(),
                        ancestro.getNombre(),
                        ancestro.getNivel() != null ? ancestro.getNivel() : 0
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Carpeta> obtenerSubcarpetasConPermiso(
            Long carpetaPadreId,
            Long usuarioId,
            Long organizacionId) {
        List<CarpetaEntity> entities = jpaRepository.findSubcarpetasConPermiso(
                carpetaPadreId,
                usuarioId,
                organizacionId);
        return mapper.toDomainList(entities);
    }

    @Override
    public int contarSubcarpetasConPermiso(
            Long carpetaPadreId,
            Long usuarioId,
            Long organizacionId) {
        return jpaRepository.countSubcarpetasConPermiso(
                carpetaPadreId,
                usuarioId,
                organizacionId);
    }

    @Override
    public boolean estaVacia(Long carpetaId, Long organizacionId) {
        return !jpaRepository.existsSubcarpetasActivas(carpetaId, organizacionId) &&
               !jpaRepository.existsDocumentosActivos(carpetaId, organizacionId);
    }

    @Override
    public int contarSubcarpetasActivas(Long carpetaId, Long organizacionId) {
        return jpaRepository.countSubcarpetasActivas(carpetaId, organizacionId);
    }

    @Override
    public int contarDocumentosActivos(Long carpetaId, Long organizacionId) {
        return jpaRepository.countDocumentosActivos(carpetaId, organizacionId);
    }
}
