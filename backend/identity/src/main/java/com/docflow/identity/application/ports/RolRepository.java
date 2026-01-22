package com.docflow.identity.application.ports;

import com.docflow.identity.domain.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Obtiene los roles disponibles para una organización.
     * Incluye roles globales del sistema (organizacionId = 0) y roles 
     * personalizados de la organización.
     * 
     * @param organizacionId ID de la organización
     * @return Lista de roles activos globales y de la organización especificada
     */
    @Query("SELECT r FROM Rol r WHERE r.activo = true AND (r.organizacionId IS NULL OR r.organizacionId = :organizacionId)")
    List<Rol> findAvailableRolesByOrganizacionId(@Param("organizacionId") Integer organizacionId);
}
