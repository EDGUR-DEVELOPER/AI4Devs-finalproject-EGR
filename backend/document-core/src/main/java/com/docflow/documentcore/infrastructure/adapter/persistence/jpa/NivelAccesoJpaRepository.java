package com.docflow.documentcore.infrastructure.adapter.persistence.jpa;

import com.docflow.documentcore.infrastructure.adapter.persistence.entity.NivelAccesoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for NivelAccesoEntity
 */
@Repository
public interface NivelAccesoJpaRepository extends JpaRepository<NivelAccesoEntity, UUID> {
    
    /**
     * Find by unique codigo
     */
    Optional<NivelAccesoEntity> findByCodigo(String codigo);
    
    /**
     * Find all active levels ordered by orden field
     */
    @Query("SELECT n FROM NivelAccesoEntity n WHERE n.activo = true ORDER BY n.orden ASC")
    List<NivelAccesoEntity> findAllActiveOrderByOrden();
    
    /**
     * Find all levels ordered by orden field
     */
    @Query("SELECT n FROM NivelAccesoEntity n ORDER BY n.orden ASC")
    List<NivelAccesoEntity> findAllOrderByOrden();
    
    /**
     * Check existence by codigo
     */
    boolean existsByCodigo(String codigo);
}
