package com.docflow.documentcore.domain.repository;

import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;

import java.util.List;
import java.util.Optional;

/**
 * Repository Port (Interface) for NivelAcceso
 * Defines persistence operations following hexagonal architecture
 */
public interface INivelAccesoRepository {
    
    /**
     * Find access level by unique ID
     * @param id Long identifier
     * @return Optional containing the access level if found
     */
    Optional<NivelAcceso> findById(Long id);
    
    /**
     * Find access level by unique codigo
     * @param codigo Access level code (LECTURA, ESCRITURA, ADMINISTRACION)
     * @return Optional containing the access level if found
     */
    Optional<NivelAcceso> findByCodigo(CodigoNivelAcceso codigo);
    
    /**
     * Find all active access levels ordered by 'orden' field
     * @return List of active access levels
     */
    List<NivelAcceso> findAllActiveOrderByOrden();
    
    /**
     * Find all access levels (including inactive) ordered by 'orden' field
     * @return List of all access levels
     */
    List<NivelAcceso> findAllOrderByOrden();
    
    /**
     * Check if an access level exists by codigo
     * @param codigo Access level code
     * @return true if exists, false otherwise
     */
    boolean existsByCodigo(CodigoNivelAcceso codigo);
}
