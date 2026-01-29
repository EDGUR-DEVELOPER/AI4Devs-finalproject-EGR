package com.docflow.identity.domain.repository;

import com.docflow.identity.domain.model.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones CRUD sobre la entidad UsuarioRol.
 */
@Repository
public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {
    
    /**
     * Obtiene los códigos de roles activos de un usuario en una organización específica.
     * 
     * @param usuarioId ID del usuario
     * @param organizacionId ID de la organización
     * @return Lista de códigos de roles ordenados alfabéticamente (vacía si no tiene roles)
     */
    @Query("""
        SELECT r.codigo 
        FROM UsuarioRol ur 
        JOIN Rol r ON r.id = ur.rolId 
        WHERE ur.usuarioId = :usuarioId 
          AND ur.organizacionId = :organizacionId 
          AND ur.activo = true 
          AND r.activo = true 
        ORDER BY r.codigo
        """)
    List<String> findCodigosRolesByUsuarioIdAndOrganizacionId(
        @Param("usuarioId") Long usuarioId,
        @Param("organizacionId") Integer organizacionId
    );
    
    /**
     * Busca una asignación de rol específica (activa o inactiva).
     * Utilizado para detectar asignaciones existentes y soportar idempotencia/reactivación.
     *
     * @param usuarioId ID del usuario
     * @param rolId ID del rol
     * @param organizacionId ID de la organización
     * @return Optional con la asignación si existe, Optional.empty() en caso contrario
     */
    Optional<UsuarioRol> findByUsuarioIdAndRolIdAndOrganizacionId(
        Long usuarioId,
        Integer rolId,
        Integer organizacionId
    );
}
