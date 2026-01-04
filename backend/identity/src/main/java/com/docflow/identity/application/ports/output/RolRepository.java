package com.docflow.identity.application.ports.output;

import com.docflow.identity.domain.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para operaciones CRUD sobre la entidad Rol.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
}
