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

    List<PermisoCarpetaUsuario> findByCarpetaId(Long carpetaId);

    boolean existsByCarpetaIdAndUsuarioId(Long carpetaId, Long usuarioId);
}
