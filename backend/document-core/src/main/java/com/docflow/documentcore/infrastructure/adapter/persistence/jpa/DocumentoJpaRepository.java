package com.docflow.documentcore.infrastructure.adapter.persistence.jpa;

import com.docflow.documentcore.domain.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para gestionar entidades Documento.
 */
@Repository
public interface DocumentoJpaRepository extends JpaRepository<Documento, Long> {
    
    /**
     * Busca un documento por ID y organización para validar pertenencia multi-tenant.
     */
    Optional<Documento> findByIdAndOrganizacionId(Long id, Long organizacionId);
}
