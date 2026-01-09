package com.docflow.identity.application.ports;

import com.docflow.identity.domain.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para operaciones CRUD sobre la entidad Rol.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
    
    /**
     * Busca un rol activo por su ID.
     * Utilizado para validar existencia de rol antes de asignación.
     *
     * @param id ID del rol
     * @return Optional con el rol si existe y está activo, Optional.empty() en caso contrario
     */
    Optional<Rol> findByIdAndActivoTrue(Integer id);
}
