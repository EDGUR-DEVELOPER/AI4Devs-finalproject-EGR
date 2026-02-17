package com.docflow.documentcore.domain.repository.adapter;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.repository.DocumentoJpaRepository;
import com.docflow.documentcore.domain.repository.IDocumentoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador del repositorio de Documentos (implementación de arquitectura hexagonal).
 * 
 * <p>Esta clase implementa el puerto {@link IDocumentoRepository} definido en el dominio,
 * delegando la persistencia real a Spring Data JPA ({@link DocumentoJpaRepository}).</p>
 *
 * <p><strong>Responsabilidades:</strong>
 * <ul>
 *   <li>Orquestar operaciones de persistencia de documentos</li>
 *   <li>Garantizar aislamiento multi-tenant en todas las operaciones</li>
 *   <li>Filtrado de documentos por permisos de usuario</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@Component
public class DocumentoRepositoryAdapter implements IDocumentoRepository {

    private final DocumentoJpaRepository jpaRepository;

    public DocumentoRepositoryAdapter(DocumentoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Documento> obtenerPorId(Long id, Long organizacionId) {
        return jpaRepository.findByIdAndOrganizacionId(id, organizacionId);
    }

    @Override
    public List<Documento> obtenerDocumentosConPermiso(
            Long carpetaId,
            Long usuarioId,
            Long organizacionId,
            Pageable pageable) {
        return jpaRepository.findDocumentosConPermiso(
                carpetaId,
                usuarioId,
                organizacionId,
                pageable);
    }

    @Override
    public long contarDocumentosConPermiso(
            Long carpetaId,
            Long usuarioId,
            Long organizacionId) {
        return jpaRepository.countDocumentosConPermiso(
                carpetaId,
                usuarioId,
                organizacionId);
    }
}
