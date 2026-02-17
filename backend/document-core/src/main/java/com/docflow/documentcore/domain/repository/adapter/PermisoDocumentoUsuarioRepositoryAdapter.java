package com.docflow.documentcore.domain.repository.adapter;

import com.docflow.documentcore.domain.model.PermisoDocumentoUsuario;
import com.docflow.documentcore.domain.repository.IPermisoDocumentoUsuarioRepository;
import com.docflow.documentcore.domain.repository.PermisoDocumentoUsuarioJpaRepository;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador que implementa el puerto de repositorio de permisos de documento.
 * 
 * Traduce entre el dominio y la infraestructura JPA (Hexagonal Architecture).
 */
@Repository
@Transactional
public class PermisoDocumentoUsuarioRepositoryAdapter implements IPermisoDocumentoUsuarioRepository {
    
    private final PermisoDocumentoUsuarioJpaRepository jpaRepository;
    
    public PermisoDocumentoUsuarioRepositoryAdapter(PermisoDocumentoUsuarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public PermisoDocumentoUsuario save(PermisoDocumentoUsuario permiso) {
        return jpaRepository.save(permiso);
    }
    
    @Override
    public Optional<PermisoDocumentoUsuario> findByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId) {
        return jpaRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId);
    }
    
    @Override
    public List<PermisoDocumentoUsuario> findByDocumentoId(Long documentoId) {
        return jpaRepository.findByDocumentoId(documentoId);
    }
    
    @Override
    public boolean existsByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId) {
        return jpaRepository.existsByDocumentoIdAndUsuarioId(documentoId, usuarioId);
    }
    
    @Override
    public void delete(PermisoDocumentoUsuario permiso) {
        jpaRepository.delete(permiso);
    }
    
    @Override
    public void deleteByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId) {
        jpaRepository.deleteByDocumentoIdAndUsuarioId(documentoId, usuarioId);
    }
}
