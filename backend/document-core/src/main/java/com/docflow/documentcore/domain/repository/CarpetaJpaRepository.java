package com.docflow.documentcore.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.docflow.documentcore.domain.model.CarpetaAncestroProjection;
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
     * Encuentra las carpetas raíz de una organización (carpeta_padre_id IS NULL).
     * 
     * @param organizacionId identificador de la organización
     * @return lista de carpetas con padreId NULL (idealmente debería haber solo 1)
     */
    List<CarpetaEntity> findByOrganizacionIdAndCarpetaPadreIdIsNull(Long organizacionId);
    
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
                   c.fecha_actualizacion, c.carpeta_padre_id, c.organizacion_id,
                   c.creado_por, c.fecha_eliminacion
            FROM carpetas c
            WHERE c.carpeta_padre_id = :carpetaPadreId
              AND c.organizacion_id = :organizacionId
              AND c.fecha_eliminacion IS NULL
              AND EXISTS (
                  SELECT 1 FROM permiso_carpeta_usuario pcu
                  WHERE pcu.carpeta_id = c.id
                    AND pcu.usuario_id = :usuarioId
                    AND pcu.nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')
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
              SELECT 1 FROM permiso_carpeta_usuario pcu
              WHERE pcu.carpeta_id = c.id
                AND pcu.usuario_id = :usuarioId
                AND pcu.nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')
          )
        """, nativeQuery = true)
    int countSubcarpetasConPermiso(
            @Param("carpetaPadreId") Long carpetaPadreId,
            @Param("usuarioId") Long usuarioId,
            @Param("organizacionId") Long organizacionId
    );

    /**
     * Verifica si existen subcarpetas activas (no eliminadas) de una carpeta.
     * 
     * <p>Utiliza EXISTS para eficiencia (evita contar todas las filas).</p>
     * 
     * @param carpetaId identificador de la carpeta padre
     * @param organizacionId identificador de la organización
     * @return true si existe al menos una subcarpeta activa, false en caso contrario
     */
    @Query(value = """
        SELECT CASE WHEN EXISTS (
            SELECT 1 FROM carpetas c
            WHERE c.carpeta_padre_id = :carpetaId
              AND c.organizacion_id = :organizacionId
              AND c.fecha_eliminacion IS NULL
        ) THEN true ELSE false END
        """, nativeQuery = true)
    boolean existsSubcarpetasActivas(
            @Param("carpetaId") Long carpetaId,
            @Param("organizacionId") Long organizacionId
    );

    /**
     * Verifica si existen documentos activos (no eliminados) en una carpeta.
     * 
     * <p>Utiliza EXISTS para eficiencia (evita contar todas las filas).</p>
     * 
     * @param carpetaId identificador de la carpeta
     * @param organizacionId identificador de la organización
     * @return true si existe al menos un documento activo, false en caso contrario
     */
    @Query(value = """
        SELECT CASE WHEN EXISTS (
            SELECT 1 FROM documento d
            WHERE d.carpeta_id = :carpetaId
              AND d.organizacion_id = :organizacionId
              AND d.fecha_eliminacion IS NULL
        ) THEN true ELSE false END
        """, nativeQuery = true)
    boolean existsDocumentosActivos(
            @Param("carpetaId") Long carpetaId,
            @Param("organizacionId") Long organizacionId
    );

    /**
     * Cuenta el número de subcarpetas activas directas de una carpeta.
     * 
     * <p>Se usa para reportar el número de subcarpetas que impiden
     * la eliminación en respuestas de error.</p>
     * 
     * @param carpetaId identificador de la carpeta padre
     * @param organizacionId identificador de la organización
     * @return número de subcarpetas con fecha_eliminacion IS NULL
     */
    @Query(value = """
        SELECT COUNT(c.id)
        FROM carpetas c
        WHERE c.carpeta_padre_id = :carpetaId
          AND c.organizacion_id = :organizacionId
          AND c.fecha_eliminacion IS NULL
        """, nativeQuery = true)
    int countSubcarpetasActivas(
            @Param("carpetaId") Long carpetaId,
            @Param("organizacionId") Long organizacionId
    );

    /**
     * Cuenta el número de documentos activos en una carpeta.
     * 
     * <p>Se usa para reportar el número de documentos que impiden
     * la eliminación en respuestas de error.</p>
     * 
     * @param carpetaId identificador de la carpeta
     * @param organizacionId identificador de la organización
     * @return número de documentos con fecha_eliminacion IS NULL
     */
    @Query(value = """
        SELECT COUNT(d.id)
        FROM documento d
        WHERE d.carpeta_id = :carpetaId
          AND d.organizacion_id = :organizacionId
          AND d.fecha_eliminacion IS NULL
        """, nativeQuery = true)
    int countDocumentosActivos(
            @Param("carpetaId") Long carpetaId,
            @Param("organizacionId") Long organizacionId
    );

    /**
     * Cuenta subcarpetas de una carpeta específica filtrando por permisos del usuario.
     * 
     * <p>Utilizado para mostrar el número de subcarpetas dentro de cada subcarpeta
     * retornada en el listado de contenido.</p>
     * 
     * @param carpetaId identificador de la carpeta (padre de las subcarpetas a contar)
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @return número de subcarpetas accesibles para el usuario
     */
    @Query(value = """
        SELECT COUNT(DISTINCT c.id)
        FROM carpetas c
        WHERE c.carpeta_padre_id = :carpetaId
          AND c.organizacion_id = :organizacionId
          AND c.fecha_eliminacion IS NULL
          AND EXISTS (
              SELECT 1 FROM permiso_carpeta_usuario pcu
              WHERE pcu.carpeta_id = c.id
                AND pcu.usuario_id = :usuarioId
                AND pcu.nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')
          )
        """, nativeQuery = true)
    int countSubcarpetasDeSubcarpetaConPermiso(
            @Param("carpetaId") Long carpetaId,
            @Param("usuarioId") Long usuarioId,
            @Param("organizacionId") Long organizacionId
    );

    /**
     * Cuenta documentos de una carpeta específica filtrando por permisos del usuario.
     * 
     * <p>Utilizado para mostrar el número de documentos dentro de cada subcarpeta
     * retornada en el listado de contenido.</p>
     * 
     * @param carpetaId identificador de la carpeta contenedora
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @return número de documentos accesibles para el usuario
     */
    @Query(value = """
        SELECT COUNT(DISTINCT d.id)
        FROM documento d
        WHERE d.carpeta_id = :carpetaId
          AND d.organizacion_id = :organizacionId
          AND d.fecha_eliminacion IS NULL
          AND EXISTS (
              SELECT 1 FROM permiso_documento_usuario pdu
              WHERE pdu.documento_id = d.id
                AND pdu.usuario_id = :usuarioId
                AND pdu.nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')
          )
        """, nativeQuery = true)
    int countDocumentosDeSubcarpetaConPermiso(
            @Param("carpetaId") Long carpetaId,
            @Param("usuarioId") Long usuarioId,
            @Param("organizacionId") Long organizacionId
    );
}
