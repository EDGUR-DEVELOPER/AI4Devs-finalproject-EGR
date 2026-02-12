package com.docflow.documentcore.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.docflow.documentcore.domain.model.PermisoCarpetaRol;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para permisos de roles sobre carpetas.
 * 
 * Implementa el puerto IPermisoCarpetaRolRepository con queries optimizadas
 * y aislamiento multi-tenant.
 */
@Repository
public interface PermisoCarpetaRolJpaRepository extends JpaRepository<PermisoCarpetaRol, Long> {

    Optional<PermisoCarpetaRol> findByCarpetaIdAndRolId(Long carpetaId, Long rolId);

    /**
     * Encuentra un permiso rol-carpeta con validación de aislamiento tenant.
     * 
     * @param carpetaId ID de la carpeta
     * @param rolId ID del rol
     * @param organizacionId ID de la organización (aislamiento multi-tenant)
     * @return Optional con el permiso si se encuentra, vacío en caso contrario
     */
    Optional<PermisoCarpetaRol> findByCarpetaIdAndRolIdAndOrganizacionId(
        Long carpetaId,
        Long rolId,
        Long organizacionId
    );

    List<PermisoCarpetaRol> findByCarpetaId(Long carpetaId);

    /**
     * Encuentra permisos de roles en una carpeta específica.
     * Usado cuando el usuario tiene múltiples roles.
     *
     * @param carpetaId ID de la carpeta
     * @param rolesIds lista de IDs de roles
     * @param organizacionId ID de la organización
     * @return lista de permisos de roles
     */
    @Query("SELECT p FROM PermisoCarpetaRol p " +
           "WHERE p.carpetaId = :carpetaId " +
           "AND p.rolId IN :rolesIds " +
           "AND p.organizacionId = :organizacionId " +
           "ORDER BY p.nivelAcceso DESC")
    List<PermisoCarpetaRol> findByCarpetaIdAndRolIdInAndOrganizacionId(
        @Param("carpetaId") Long carpetaId,
        @Param("rolesIds") List<Long> rolesIds,
        @Param("organizacionId") Long organizacionId
    );

    /**
     * Encuentra permisos de roles heredables en la jerarquía de carpetas.
     * Busca en los ancestros de la carpeta objetivo solo aquellos permisos
     * que tienen recursivo=true.
     *
     * @param carpetaId ID de la carpeta objetivo
     * @param rolesIds lista de IDs de roles del usuario
     * @param organizacionId ID de la organización
     * @return lista de permisos heredables
     */
    @Query(value = 
        "WITH RECURSIVE ancestros AS (" +
        "  SELECT id, carpeta_padre_id, nombre, nivel FROM carpetas " +
        "  WHERE id = :carpetaId AND organizacion_id = :organizacionId " +
        "  UNION ALL " +
        "  SELECT c.id, c.carpeta_padre_id, c.nombre, c.nivel FROM carpetas c " +
        "  INNER JOIN ancestros a ON c.id = a.carpeta_padre_id " +
        "  WHERE c.organizacion_id = :organizacionId" +
        ") " +
        "SELECT p.* FROM permiso_carpeta_rol p " +
        "INNER JOIN ancestros a ON p.carpeta_id = a.id " +
        "WHERE p.rol_id IN :rolesIds " +
        "AND p.organizacion_id = :organizacionId " +
        "AND p.recursivo = true " +
        "ORDER BY a.nivel DESC, p.nivel_acceso DESC",
        nativeQuery = true)
    List<PermisoCarpetaRol> findHeredableByAncestorsAndRolesAndOrganizacion(
        @Param("carpetaId") Long carpetaId,
        @Param("rolesIds") List<Long> rolesIds,
        @Param("organizacionId") Long organizacionId
    );

    boolean existsByCarpetaIdAndRolId(Long carpetaId, Long rolId);

    /**
     * Revoca un permiso de rol sobre una carpeta eliminándolo.
     * Incluye aislamiento multi-tenant en la consulta.
     *
     * @param carpetaId ID de la carpeta
     * @param rolId ID del rol
     * @param organizacionId ID de la organización
     * @return número de registros eliminados (0 si no existe, 1 si éxito)
     */
    @Modifying
    @Query("DELETE FROM PermisoCarpetaRol p " +
           "WHERE p.carpetaId = :carpetaId " +
           "AND p.rolId = :rolId " +
           "AND p.organizacionId = :organizacionId")
    int revokePermission(
        @Param("carpetaId") Long carpetaId,
        @Param("rolId") Long rolId,
        @Param("organizacionId") Long organizacionId
    );
}
