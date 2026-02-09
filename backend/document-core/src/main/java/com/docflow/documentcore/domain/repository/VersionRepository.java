package com.docflow.documentcore.domain.repository;

import com.docflow.documentcore.domain.model.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para Version (Spring Data).
 * 
 * US-DOC-001: Proporciona operaciones de persistencia para versiones de documento.
 * Las versiones son inmutables - solo inserciones y consultas.
 * 
 * @see com.docflow.documentcore.domain.model.Version
 */
@Repository
public interface VersionRepository extends JpaRepository<Version, Long> {
    
    /**
     * Busca una versión específica de un documento.
     *
     * @param documentoId ID del documento padre
     * @param numeroSecuencial Número de versión (1, 2, 3, ...)
     * @return Optional con la versión si existe
     */
    @Query("SELECT v FROM Version v WHERE v.documentoId = :documentoId AND v.numeroSecuencial = :numeroSecuencial")
    Optional<Version> findByDocumentoIdAndNumeroSecuencial(
        @Param("documentoId") Long documentoId, 
        @Param("numeroSecuencial") Integer numeroSecuencial
    );
    
    /**
     * Busca todas las versiones de un documento ordenadas por número secuencial descendente.
     * La primera versión en la lista es la más reciente.
     *
     * @param documentoId ID del documento padre
     * @return Lista de versiones ordenadas por número descendente
     */
    @Query("SELECT v FROM Version v WHERE v.documentoId = :documentoId ORDER BY v.numeroSecuencial DESC")
    List<Version> findByDocumentoIdOrderByNumeroSecuencialDesc(@Param("documentoId") Long documentoId);
    
    /**
     * Busca todas las versiones de un documento ordenadas por número secuencial ascendente.
     * La primera versión en la lista es la más antigua.
     *
     * @param documentoId ID del documento padre
     * @return Lista de versiones ordenadas por número ascendente
     */
    @Query("SELECT v FROM Version v WHERE v.documentoId = :documentoId ORDER BY v.numeroSecuencial ASC")
    List<Version> findByDocumentoIdOrderByNumeroSecuencialAsc(@Param("documentoId") Long documentoId);
        /**
         * Busca todas las versiones de un documento ordenadas por número secuencial ascendente
         * con soporte de paginación.
         * 
         * US-DOC-004: Método con Pageable para listado de versiones con paginación opcional.
         * El ordenamiento está embebido en la consulta para aprovechar el índice de la base de datos.
         * 
         * @param documentoId ID del documento padre
         * @param pageable Configuración de paginación y ordenamiento
         * @return Página de versiones con metadatos de paginación
         */
        @Query("SELECT v FROM Version v WHERE v.documentoId = :documentoId ORDER BY v.numeroSecuencial ASC")
        Page<Version> findByDocumentoIdOrderByNumeroSecuencialAsc(
            @Param("documentoId") Long documentoId, 
            Pageable pageable
        );
    
    
    /**
     * Obtiene la versión más reciente de un documento.
     *
     * @param documentoId ID del documento padre
     * @return Optional con la última versión si existe
     */
    @Query("SELECT v FROM Version v WHERE v.documentoId = :documentoId ORDER BY v.numeroSecuencial DESC LIMIT 1")
    Optional<Version> findLatestByDocumentoId(@Param("documentoId") Long documentoId);
    
    /**
     * Cuenta el número de versiones de un documento.
     *
     * @param documentoId ID del documento padre
     * @return Número total de versiones
     */
    long countByDocumentoId(Long documentoId);
    
    /**
     * Busca versiones por hash de contenido (deduplicación).
     * Permite identificar si el mismo contenido ya existe.
     *
     * @param hashContenido Hash SHA256 del contenido
     * @return Lista de versiones con el mismo contenido
     */
    @Query("SELECT v FROM Version v WHERE v.hashContenido = :hashContenido")
    List<Version> findByHashContenido(@Param("hashContenido") String hashContenido);
    
    /**
     * Busca versiones por ruta de almacenamiento.
     * Útil para verificar integridad del almacenamiento.
     *
     * @param rutaAlmacenamiento Ruta del archivo en almacenamiento
     * @return Optional con la versión si existe
     */
    Optional<Version> findByRutaAlmacenamiento(String rutaAlmacenamiento);
}
