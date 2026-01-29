package com.docflow.documentcore.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.docflow.documentcore.infrastructure.adapter.entity.CarpetaEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad CarpetaEntity.
 * 
 * <p>Proporciona operaciones CRUD automáticas y consultas personalizadas
 * para la gestión de carpetas con aislamiento por organización.</p>
 *
 * <p><strong>Notas importantes:</strong>
 * <ul>
 *   <li>La cláusula @Where en CarpetaEntity automáticamente filtra carpetas eliminadas</li>
 *   <li>Todos los métodos deben incluir filtro por organizacion_id</li>
 *   <li>Los nombres de método generan SQL automáticamente (Spring Data magic)</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@Repository
public interface CarpetaJpaRepository extends JpaRepository<CarpetaEntity, Long> {
    
    /**
     * Lista carpetas hijas de una carpeta padre específica.
     * 
     * @param organizacionId identificador de la organización
     * @param carpetaPadreId identificador de la carpeta padre
     * @return lista de carpetas hijas activas
     */
    List<CarpetaEntity> findByOrganizacionIdAndCarpetaPadreId(
            Long organizacionId, 
            Long carpetaPadreId
    );
    
    /**
     * Busca una carpeta por ID y organización.
     * 
     * @param id identificador de la carpeta
     * @param organizacionId identificador de la organización
     * @return Optional con la carpeta si existe y está activa
     */
    Optional<CarpetaEntity> findByIdAndOrganizacionId(Long id, Long organizacionId);
    
    /**
     * Encuentra la carpeta raíz de una organización (carpeta_padre_id IS NULL).
     * 
     * @param organizacionId identificador de la organización
     * @return Optional con la carpeta raíz si existe
     */
    Optional<CarpetaEntity> findByOrganizacionIdAndCarpetaPadreIdIsNull(Long organizacionId);
    
    /**
     * Verifica si existe una carpeta con el mismo nombre en el mismo nivel.
     * 
     * <p>Se usa para garantizar unicidad de nombres dentro de un directorio padre.</p>
     * 
     * @param organizacionId identificador de la organización
     * @param carpetaPadreId identificador de la carpeta padre (puede ser null para carpetas raíz)
     * @param nombre nombre de la carpeta (case-insensitive)
     * @return true si ya existe una carpeta con ese nombre en el nivel
     */
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM CarpetaEntity c
        WHERE c.organizacionId = :organizacionId
        AND (
            (:carpetaPadreId IS NULL AND c.carpetaPadreId IS NULL)
            OR c.carpetaPadreId = :carpetaPadreId
        )
        AND LOWER(c.nombre) = LOWER(:nombre)
        AND c.fechaEliminacion IS NULL
    """)
    boolean existsByNombreEnNivel(
            @Param("organizacionId") Long organizacionId,
            @Param("carpetaPadreId") Long carpetaPadreId,
            @Param("nombre") String nombre
    );
}
