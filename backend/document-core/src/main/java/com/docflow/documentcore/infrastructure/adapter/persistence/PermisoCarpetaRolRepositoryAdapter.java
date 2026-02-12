package com.docflow.documentcore.infrastructure.adapter.persistence;

import com.docflow.documentcore.domain.model.PermisoCarpetaRol;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaRolRepository;
import com.docflow.documentcore.domain.repository.PermisoCarpetaRolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistencia para permisos de roles sobre carpetas.
 * 
 * Implementa el puerto IPermisoCarpetaRolRepository del dominio,
 * delegando a PermisoCarpetaRolJpaRepository.
 * 
 * Sigue el patrón hexagonal: expone el puerto de dominio y oculta
 * los detalles de persistencia.
 */
@Repository
@RequiredArgsConstructor
public class PermisoCarpetaRolRepositoryAdapter implements IPermisoCarpetaRolRepository {

    private final PermisoCarpetaRolJpaRepository jpaRepository;

    @Override
    public PermisoCarpetaRol save(PermisoCarpetaRol permiso) {
        return jpaRepository.save(permiso);
    }

    @Override
    public Optional<PermisoCarpetaRol> findByCarpetaIdAndRolId(Long carpetaId, Long rolId) {
        return jpaRepository.findByCarpetaIdAndRolId(carpetaId, rolId);
    }

    @Override
    public Optional<PermisoCarpetaRol> findByCarpetaIdAndRolIdAndOrganizacionId(
            Long carpetaId,
            Long rolId,
            Long organizacionId
    ) {
        return jpaRepository.findByCarpetaIdAndRolIdAndOrganizacionId(carpetaId, rolId, organizacionId);
    }

    @Override
    public List<PermisoCarpetaRol> findByCarpetaId(Long carpetaId) {
        return jpaRepository.findByCarpetaId(carpetaId);
    }

    @Override
    public List<PermisoCarpetaRol> findByCarpetaIdAndRolIdInAndOrganizacionId(
            Long carpetaId,
            List<Long> rolesIds,
            Long organizacionId
    ) {
        return jpaRepository.findByCarpetaIdAndRolIdInAndOrganizacionId(carpetaId, rolesIds, organizacionId);
    }

    @Override
    public List<PermisoCarpetaRol> findHeredableByAncestorsAndRolesAndOrganizacion(
            Long carpetaId,
            List<Long> rolesIds,
            Long organizacionId
    ) {
        return jpaRepository.findHeredableByAncestorsAndRolesAndOrganizacion(carpetaId, rolesIds, organizacionId);
    }

    @Override
    public boolean existsByCarpetaIdAndRolId(Long carpetaId, Long rolId) {
        return jpaRepository.existsByCarpetaIdAndRolId(carpetaId, rolId);
    }

    @Override
    public int revokePermission(Long carpetaId, Long rolId, Long organizacionId) {
        return jpaRepository.revokePermission(carpetaId, rolId, organizacionId);
    }
}
