package com.docflow.documentcore.domain.repository;

import java.util.List;
import java.util.Optional;

import com.docflow.documentcore.domain.model.PermisoCarpetaRol;

/**
 * Puerto de repositorio para permisos de roles sobre carpetas.
 * 
 * Los permisos por rol se heredan automáticamente a los usuarios que poseen ese rol.
 */
public interface IPermisoCarpetaRolRepository {

    PermisoCarpetaRol save(PermisoCarpetaRol permiso);

    Optional<PermisoCarpetaRol> findByCarpetaIdAndRolId(Long carpetaId, Long rolId);

    Optional<PermisoCarpetaRol> findByCarpetaIdAndRolIdAndOrganizacionId(
        Long carpetaId,
        Long rolId,
        Long organizacionId
    );

    List<PermisoCarpetaRol> findByCarpetaId(Long carpetaId);

    /**
     * Encuentra permisos de roles sobre una carpeta.
     * Usado para resolver permisos heredados a través de roles.
     *
     * @param carpetaId ID de la carpeta
     * @param rolesIds lista de IDs de roles del usuario
     * @param organizacionId ID de la organización (aislamiento multi-tenant)
     * @return lista de permisos de roles aplicables
     */
    List<PermisoCarpetaRol> findByCarpetaIdAndRolIdInAndOrganizacionId(
        Long carpetaId,
        List<Long> rolesIds,
        Long organizacionId
    );

    /**
     * Encuentra permisos de roles a través de la jerarquía de carpetas (ancestros).
     * Usado para resolver herencia de permisos.
     *
     * @param carpetaId ID de la carpeta objetivo
     * @param rolesIds lista de IDs de roles del usuario
     * @param organizacionId ID de la organización
     * @return lista de permisos heredables (donde recursivo=true)
     */
    List<PermisoCarpetaRol> findHeredableByAncestorsAndRolesAndOrganizacion(
        Long carpetaId,
        List<Long> rolesIds,
        Long organizacionId
    );

    boolean existsByCarpetaIdAndRolId(Long carpetaId, Long rolId);

    /**
     * Revoca permiso de un rol sobre una carpeta.
     *
     * @param carpetaId ID de la carpeta
     * @param rolId ID del rol
     * @param organizacionId ID de la organización
     * @return número de registros eliminados (0 si no existe, 1 si éxito)
     */
    int revokePermission(Long carpetaId, Long rolId, Long organizacionId);
}
