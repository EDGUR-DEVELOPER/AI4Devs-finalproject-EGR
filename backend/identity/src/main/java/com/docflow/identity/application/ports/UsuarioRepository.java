package com.docflow.identity.application.ports;

import com.docflow.identity.domain.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por email que no esté eliminado (soft delete).
     *
     * @param email el email del usuario
     * @return Optional con el usuario si existe y no está eliminado
     */
    Optional<Usuario> findByEmailAndFechaEliminacionIsNull(String email);

    /**
     * Verifica si existe un usuario con el email dado.
     *
     * @param email el email a verificar
     * @return true si existe un usuario con ese email
     */
    boolean existsByEmail(String email);

    /**
     * Busca un usuario por ID que no esté eliminado (soft delete).
     * Utilizado para validar que el usuario objetivo existe y está activo.
     *
     * @param id ID del usuario
     * @return Optional con el usuario si existe y no está eliminado
     */
    Optional<Usuario> findByIdAndFechaEliminacionIsNull(Long id);

    /**
     * Obtiene usuarios de una organización con sus roles asignados mediante
     * constructor expression JPQL.
     * 
     * Query optimizado que:
     * - Usa INNER JOIN con UsuarioOrganizacion filtrando por organizacionId
     * - Usa LEFT JOIN con UsuarioRol y Rol para incluir usuarios sin roles
     * - Filtra usuarios no eliminados (fechaEliminacion IS NULL)
     * - Aplica filtros opcionales de estado (ACTIVOS, INACTIVOS) y búsqueda (email/nombre)
     * - Retorna proyecciones UserWithRolesProjection (1 fila por usuario-rol)
     * - Soporta paginación y ordenamiento
     * 
     * Aprovecha índices existentes:
     * - idx_usuarios_roles_org (organizacion_id) para filtrado eficiente de roles
     * - idx_usuarios_activos (id) para exclusión de usuarios eliminados
     * 
     * Nota: Los resultados deben agruparse en memoria ya que un usuario con N roles
     * genera N filas (desnormalización intencional para eficiencia de query).
     * 
     * @param organizacionId ID de la organización cuyos usuarios se listan
     * @param estado Filtro opcional por estado de membresía (ACTIVOS, INACTIVOS o null para todos)
     * @param busqueda Filtro opcional de búsqueda en email o nombre (case-insensitive, null para sin filtro)
     * @param pageable Configuración de paginación y ordenamiento
     * @return Página de proyecciones con datos de usuarios y roles (sin agrupar)
     */
    @Query("""
            SELECT
                u.id AS usuarioId,
                u.email AS email,
                u.nombreCompleto AS nombreCompleto,
                uo.estado AS estado,
                r.id AS rolId,
                r.codigo AS rolCodigo,
                r.nombre AS rolNombre,
                u.fechaCreacion AS fechaCreacion
            FROM Usuario u
            INNER JOIN UsuarioOrganizacion uo ON uo.usuarioId = u.id
            LEFT JOIN UsuarioRol ur ON ur.usuarioId = u.id
                AND ur.organizacionId = :organizacionId
                AND ur.activo = true
            LEFT JOIN Rol r ON r.id = ur.rolId
            WHERE uo.organizacionId = :organizacionId
              AND u.fechaEliminacion IS NULL
              AND (:estado IS NULL 
                   OR (:estado = 'ACTIVOS' AND uo.estado = 'ACTIVO')
                   OR (:estado = 'INACTIVOS' AND uo.estado = 'INACTIVO'))
              AND (:busqueda IS NULL 
                   OR LOWER(u.email) LIKE LOWER(('%' || CAST(:busqueda AS String) || '%'))
                   OR LOWER(u.nombreCompleto) LIKE LOWER(('%' || CAST(:busqueda AS String) || '%')))
            """)
    Page<UserWithRolesProjection> findUsersWithRolesByOrganizacion(
            @Param("organizacionId") Integer organizacionId,
            @Param("estado") String estado,
            @Param("busqueda") String busqueda,
            Pageable pageable);
}
