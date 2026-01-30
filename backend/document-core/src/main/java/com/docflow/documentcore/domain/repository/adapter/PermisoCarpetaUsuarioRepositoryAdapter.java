package com.docflow.documentcore.domain.repository.adapter;

import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.domain.repository.PermisoCarpetaUsuarioJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de repositorio para permisos explícitos de usuarios sobre carpetas.
 */
@Component
public class PermisoCarpetaUsuarioRepositoryAdapter implements IPermisoCarpetaUsuarioRepository {

    private final PermisoCarpetaUsuarioJpaRepository jpaRepository;

    public PermisoCarpetaUsuarioRepositoryAdapter(PermisoCarpetaUsuarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PermisoCarpetaUsuario save(PermisoCarpetaUsuario permiso) {
        return jpaRepository.save(permiso);
    }

    @Override
    public Optional<PermisoCarpetaUsuario> findByCarpetaIdAndUsuarioId(Long carpetaId, Long usuarioId) {
        return jpaRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId);
    }

    @Override
    public List<PermisoCarpetaUsuario> findByCarpetaId(Long carpetaId) {
        return jpaRepository.findByCarpetaId(carpetaId);
    }

    @Override
    public boolean existsByCarpetaIdAndUsuarioId(Long carpetaId, Long usuarioId) {
        return jpaRepository.existsByCarpetaIdAndUsuarioId(carpetaId, usuarioId);
    }
}
