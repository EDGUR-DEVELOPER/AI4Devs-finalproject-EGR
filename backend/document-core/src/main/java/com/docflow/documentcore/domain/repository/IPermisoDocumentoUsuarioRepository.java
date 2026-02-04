package com.docflow.documentcore.domain.repository;

import com.docflow.documentcore.domain.model.permiso.PermisoDocumentoUsuario;

import java.util.List;
import java.util.Optional;

/**
 * Puerto del repositorio para permisos explícitos de documento (Hexagonal Architecture).
 * 
 * Define el contrato para la persistencia sin exponer detalles de infraestructura.
 */
public interface IPermisoDocumentoUsuarioRepository {
    
    /**
     * Guarda o actualiza un permiso de documento.
     */
    PermisoDocumentoUsuario save(PermisoDocumentoUsuario permiso);
    
    /**
     * Busca un permiso por documento y usuario.
     */
    Optional<PermisoDocumentoUsuario> findByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    
    /**
     * Lista todos los permisos de un documento.
     */
    List<PermisoDocumentoUsuario> findByDocumentoId(Long documentoId);
    
    /**
     * Verifica si existe un permiso para documento y usuario.
     */
    boolean existsByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    
    /**
     * Elimina un permiso.
     */
    void delete(PermisoDocumentoUsuario permiso);
    
    /**
     * Elimina un permiso por documento y usuario.
     */
    void deleteByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
}
