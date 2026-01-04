package com.docflow.identity.application.ports.output;

import com.docflow.identity.domain.model.Organizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la entidad Organizacion.
 */
@Repository
public interface OrganizacionRepository extends JpaRepository<Organizacion, Integer> {
}
