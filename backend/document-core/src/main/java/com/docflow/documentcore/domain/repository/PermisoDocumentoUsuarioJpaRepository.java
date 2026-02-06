package com.docflow.documentcore.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.docflow.documentcore.domain.model.PermisoDocumentoUsuario;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para gestionar permisos explícitos de documento por usuario.
 */
@Repository
public interface PermisoDocumentoUsuarioJpaRepository extends JpaRepository<PermisoDocumentoUsuario, Long> {
    
    /**
     * Busca un permiso por documento y usuario.
     */
    Optional<PermisoDocumentoUsuario> findByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    
    /**
     * Lista todos los permisos de un documento.
     */
    List<PermisoDocumentoUsuario> findByDocumentoId(Long documentoId);
    
    /**
     * Lista todos los permisos asignados a un usuario.
     */
    List<PermisoDocumentoUsuario> findByUsuarioId(Long usuarioId);
    
    /**
     * Verifica si existe un permiso para el documento y usuario.
     */
    boolean existsByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    
    /**
     * Elimina un permiso por documento y usuario.
     */
    void deleteByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
}
