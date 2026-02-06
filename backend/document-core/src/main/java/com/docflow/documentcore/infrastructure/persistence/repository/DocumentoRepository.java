package com.docflow.documentcore.infrastructure.persistence.repository;

import com.docflow.documentcore.domain.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para Documento (Spring Data).
 * 
 * US-DOC-001: Proporciona operaciones de persistencia para documentos.
 * Todas las consultas incluyen filtro organizacion_id para multi-tenancy.
 * 
 * @see com.docflow.documentcore.domain.model.Documento
 */
@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    
    /**
     * Busca un documento por ID y organización.
     * Asegura aislamiento de tenant en consultas directas.
     *
     * @param id ID del documento
     * @param organizacionId ID de la organización
     * @return Optional con el documento si existe
     */
    @Query("SELECT d FROM Documento d WHERE d.id = :id AND d.organizacionId = :organizacionId AND d.fechaEliminacion IS NULL")
    Optional<Documento> findByIdAndOrganizacionId(@Param("id") Long id, @Param("organizacionId") Long organizacionId);
    
    /**
     * Busca todos los documentos en una carpeta.
     * Excluye documentos eliminados lógicamente.
     *
     * @param carpetaId ID de la carpeta
     * @param organizacionId ID de la organización
     * @return Lista de documentos en la carpeta
     */
    @Query("SELECT d FROM Documento d WHERE d.carpetaId = :carpetaId AND d.organizacionId = :organizacionId AND d.fechaEliminacion IS NULL ORDER BY d.nombre")
    List<Documento> findByCarpetaIdAndOrganizacionId(@Param("carpetaId") Long carpetaId, @Param("organizacionId") Long organizacionId);
    
    /**
     * Verifica si existe un documento con el mismo nombre en la carpeta.
     * Usado para validar duplicados antes de crear documentos.
     *
     * @param nombre Nombre del documento
     * @param carpetaId ID de la carpeta
     * @param organizacionId ID de la organización
     * @return true si existe un documento con ese nombre
     */
    boolean existsByNombreAndCarpetaIdAndOrganizacionIdAndFechaEliminacionIsNull(
        String nombre, Long carpetaId, Long organizacionId
    );
    
    /**
     * Busca todos los documentos de una organización.
     * Excluye documentos eliminados lógicamente.
     *
     * @param organizacionId ID de la organización
     * @return Lista de todos los documentos de la organización
     */
    @Query("SELECT d FROM Documento d WHERE d.organizacionId = :organizacionId AND d.fechaEliminacion IS NULL ORDER BY d.fechaCreacion DESC")
    List<Documento> findByOrganizacionId(@Param("organizacionId") Long organizacionId);
    
    /**
     * Busca documentos por etiqueta.
     *
     * @param etiqueta Etiqueta a buscar
     * @param organizacionId ID de la organización
     * @return Lista de documentos con la etiqueta especificada
     */
    @Query("SELECT DISTINCT d FROM Documento d JOIN d.etiquetas e WHERE e = :etiqueta AND d.organizacionId = :organizacionId AND d.fechaEliminacion IS NULL")
    List<Documento> findByEtiquetaAndOrganizacionId(@Param("etiqueta") String etiqueta, @Param("organizacionId") Long organizacionId);
    
    /**
     * Busca documentos eliminados lógicamente (papelera).
     *
     * @param organizacionId ID de la organización
     * @return Lista de documentos eliminados
     */
    @Query("SELECT d FROM Documento d WHERE d.organizacionId = :organizacionId AND d.fechaEliminacion IS NOT NULL ORDER BY d.fechaEliminacion DESC")
    List<Documento> findDeletedByOrganizacionId(@Param("organizacionId") Long organizacionId);
}
