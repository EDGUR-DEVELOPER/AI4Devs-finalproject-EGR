package com.docflow.documentcore.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.docflow.documentcore.domain.model.entity.CarpetaEntity;

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

    /**
     * Obtiene la ruta de ancestros de una carpeta usando CTE recursivo.
     * Retorna los ancestros ordenados desde el más cercano al más lejano.
     *
     * @param carpetaId id de la carpeta objetivo
     * @param organizacionId id de la organización
     * @return lista de ancestros con id, nombre y nivel
     */
    @Query(value = """
        WITH RECURSIVE ancestros AS (
            SELECT id, carpeta_padre_id, nombre, 0 AS nivel
            FROM carpetas
            WHERE id = :carpetaId
              AND organizacion_id = :organizacionId
              AND fecha_eliminacion IS NULL

            UNION ALL

            SELECT c.id, c.carpeta_padre_id, c.nombre, a.nivel + 1
            FROM carpetas c
            INNER JOIN ancestros a ON c.id = a.carpeta_padre_id
            WHERE c.organizacion_id = :organizacionId
              AND c.fecha_eliminacion IS NULL
              AND a.nivel < 50
        )
        SELECT id, nombre, nivel
        FROM ancestros
        WHERE nivel > 0
        ORDER BY nivel ASC
        """, nativeQuery = true)
    List<CarpetaAncestroProjection> findRutaAncestros(
            @Param("carpetaId") Long carpetaId,
            @Param("organizacionId") Long organizacionId
    );

    /**
     * Obtiene subcarpetas de una carpeta filtrando por permisos del usuario.
     * 
     * <p>Utiliza CTE recursivo para evaluar herencia de permisos desde carpetas ancestras.</p>
     * 
     * @param carpetaPadreId identificador de la carpeta padre
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @return lista de subcarpetas accesibles (con permiso de lectura)
     */
    @Query(value = """
        WITH RECURSIVE carpetas_accesibles AS (
            -- Subcarpetas directas con permiso directo en carpeta
            SELECT c.id, c.nombre, c.descripcion, c.fecha_creacion, 
                   c.fecha_actualizacion, c.carpeta_padre_id, c.organizacion_id
            FROM carpetas c
            WHERE c.carpeta_padre_id = :carpetaPadreId
              AND c.organizacion_id = :organizacionId
              AND c.fecha_eliminacion IS NULL
              AND EXISTS (
                  SELECT 1 FROM permisos_carpeta_usuario pcu
                  WHERE pcu.carpeta_id = c.id
                    AND pcu.usuario_id = :usuarioId
                    AND pcu.nivel_acceso >= 1
              )
        )
        SELECT * FROM carpetas_accesibles
        ORDER BY nombre ASC
        """, nativeQuery = true)
    List<CarpetaEntity> findSubcarpetasConPermiso(
            @Param("carpetaPadreId") Long carpetaPadreId,
            @Param("usuarioId") Long usuarioId,
            @Param("organizacionId") Long organizacionId
    );

    /**
     * Cuenta subcarpetas de una carpeta filtrando por permisos del usuario.
     * 
     * @param carpetaPadreId identificador de la carpeta padre
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @return número de subcarpetas accesibles
     */
    @Query(value = """
        SELECT COUNT(DISTINCT c.id)
        FROM carpetas c
        WHERE c.carpeta_padre_id = :carpetaPadreId
          AND c.organizacion_id = :organizacionId
          AND c.fecha_eliminacion IS NULL
          AND EXISTS (
              SELECT 1 FROM permisos_carpeta_usuario pcu
              WHERE pcu.carpeta_id = c.id
                AND pcu.usuario_id = :usuarioId
                AND pcu.nivel_acceso >= 1
          )
        """, nativeQuery = true)
    int countSubcarpetasConPermiso(
            @Param("carpetaPadreId") Long carpetaPadreId,
            @Param("usuarioId") Long usuarioId,
            @Param("organizacionId") Long organizacionId
    );
}
