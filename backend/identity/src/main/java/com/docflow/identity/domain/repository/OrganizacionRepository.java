package com.docflow.identity.domain.repository;

import com.docflow.identity.domain.model.object.EstadoOrganizacion;
import com.docflow.identity.domain.model.Organizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Organizacion.
 */
@Repository
public interface OrganizacionRepository extends JpaRepository<Organizacion, Integer> {
    
    /**
     * Verifica si existe una organización con el ID y estado dados.
     * Utilizado para validar que organizaciones de roles custom estén activas.
     *
     * @param id ID de la organización
     * @param estado Estado a verificar (ACTIVO, SUSPENDIDO, ARCHIVADO)
     * @return true si existe una organización con ese ID y estado
     */
    boolean existsByIdAndEstado(Integer id, EstadoOrganizacion estado);
}
