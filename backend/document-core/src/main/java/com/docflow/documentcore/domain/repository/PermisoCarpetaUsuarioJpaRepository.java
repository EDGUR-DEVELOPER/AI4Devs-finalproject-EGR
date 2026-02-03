package com.docflow.documentcore.domain.repository;

import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para permisos explícitos de usuarios sobre carpetas.
 */
@Repository
public interface PermisoCarpetaUsuarioJpaRepository extends JpaRepository<PermisoCarpetaUsuario, Long> {

    Optional<PermisoCarpetaUsuario> findByCarpetaIdAndUsuarioId(Long carpetaId, Long usuarioId);

    List<PermisoCarpetaUsuario> findByCarpetaId(Long carpetaId);

    boolean existsByCarpetaIdAndUsuarioId(Long carpetaId, Long usuarioId);

    /**
     * Finds a permission entry with tenant isolation validation.
     * 
     * @param carpetaId the folder ID
     * @param usuarioId the user ID
     * @param organizacionId the organization ID (for tenant isolation)
     * @return Optional containing the permission if found, empty otherwise
     */
    Optional<PermisoCarpetaUsuario> findByCarpetaIdAndUsuarioIdAndOrganizacionId(
        Long carpetaId, 
        Long usuarioId, 
        Long organizacionId
    );

    /**
     * Revokes a permission entry by deleting it with tenant isolation.
     * Hard delete of the ACL entry from the database.
     *
     * @param carpetaId the folder ID
     * @param usuarioId the user ID
     * @param organizacionId the organization ID (for tenant isolation)
     * @return the number of rows deleted (0 if not found, 1 if successful)
     */
    @Modifying
    int deleteByUsuarioIdAndCarpetaIdAndOrganizacionId(
        Long usuarioId,
        Long carpetaId,
        Long organizacionId
    );
}
