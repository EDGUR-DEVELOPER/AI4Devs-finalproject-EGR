package com.docflow.documentcore.domain.repository;

import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
