package com.docflow.documentcore.domain.repository;

import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para permisos explícitos de usuarios sobre carpetas.
 */
public interface IPermisoCarpetaUsuarioRepository {

    PermisoCarpetaUsuario save(PermisoCarpetaUsuario permiso);

    Optional<PermisoCarpetaUsuario> findByCarpetaIdAndUsuarioId(Long carpetaId, Long usuarioId);

        Optional<PermisoCarpetaUsuario> findByCarpetaIdAndUsuarioIdAndOrganizacionId(
            Long carpetaId,
            Long usuarioId,
            Long organizacionId
        );

    List<PermisoCarpetaUsuario> findByCarpetaId(Long carpetaId);

        List<PermisoCarpetaUsuario> findByUsuarioIdAndCarpetaIds(
            Long usuarioId,
            List<Long> carpetaIds,
            Long organizacionId
        );

    boolean existsByCarpetaIdAndUsuarioId(Long carpetaId, Long usuarioId);

    /**
     * Revokes permission of a user over a folder by deleting the ACL entry.
     * This method ensures organization isolation by including organizacionId in the query.
     *
     * @param carpetaId the ID of the folder
     * @param usuarioId the ID of the user whose permission is being revoked
     * @param organizacionId the ID of the organization (for tenant isolation)
     * @return the number of records deleted (0 if not found, 1 if successful)
     */
    int revokePermission(Long carpetaId, Long usuarioId, Long organizacionId);
}
