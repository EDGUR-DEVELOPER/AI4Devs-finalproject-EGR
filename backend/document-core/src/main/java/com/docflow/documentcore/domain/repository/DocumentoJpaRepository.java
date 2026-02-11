package com.docflow.documentcore.domain.repository;

import com.docflow.documentcore.domain.model.Documento;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Obtiene documentos de una carpeta filtrando por permisos del usuario con paginación.
     * 
     * <p>Utiliza consultas sobre permisos directos del documento.</p>
     * 
     * @param carpetaId identificador de la carpeta contenedora
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @param pageable paginación y ordenamiento
     * @return página de documentos accesibles
     */
    @Query(value = """
        SELECT d.*
        FROM documento d
        WHERE d.carpeta_id = :carpetaId
          AND d.organizacion_id = :organizacionId
          AND d.fecha_eliminacion IS NULL
          AND EXISTS (
              SELECT 1 FROM permisos_documento_usuario pdu
              WHERE pdu.documento_id = d.id
                AND pdu.usuario_id = :usuarioId
                AND pdu.nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')
          )
        ORDER BY d.nombre ASC
        """, nativeQuery = true)
    List<Documento> findDocumentosConPermiso(
            @Param("carpetaId") Long carpetaId,
            @Param("usuarioId") Long usuarioId,
            @Param("organizacionId") Long organizacionId,
            Pageable pageable
    );

    /**
     * Cuenta documentos de una carpeta filtrando por permisos del usuario.
     * 
     * @param carpetaId identificador de la carpeta contenedora
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @return número de documentos accesibles
     */
    @Query(value = """
        SELECT COUNT(DISTINCT d.id)
        FROM documento d
        WHERE d.carpeta_id = :carpetaId
          AND d.organizacion_id = :organizacionId
          AND d.fecha_eliminacion IS NULL
          AND EXISTS (
              SELECT 1 FROM permisos_documento_usuario pdu
              WHERE pdu.documento_id = d.id
                AND pdu.usuario_id = :usuarioId
                AND pdu.nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')
          )
        """, nativeQuery = true)
    long countDocumentosConPermiso(
            @Param("carpetaId") Long carpetaId,
            @Param("usuarioId") Long usuarioId,
            @Param("organizacionId") Long organizacionId
    );
}
