package com.docflow.documentcore.domain.repository;

import java.util.List;
import java.util.Optional;

import com.docflow.documentcore.domain.model.CarpetaAncestro;

import com.docflow.documentcore.domain.model.Carpeta;

/**
 * Interfaz del repositorio de Carpetas (Port de salida).
 * 
 * <p>Define el contrato para operaciones de persistencia de carpetas,
 * siguiendo los principios de arquitectura hexagonal donde el dominio
 * define las interfaces y la infraestructura las implementa.</p>
 *
 * <p><strong>Responsabilidades:</strong>
 * <ul>
 *   <li>CRUD de carpetas con aislamiento por organizacion_id</li>
 *   <li>Consultas jerárquicas (padres, hijos)</li>
 *   <li>Validación de unicidad de nombres por nivel</li>
 *   <li>Soporte de eliminación lógica</li>
 * </ul>
 * </p>
 *
 * <p><strong>Reglas de Multi-Tenancy:</strong>
 * Todos los métodos DEBEN filtrar por organizacionId para garantizar
 * aislamiento entre organizaciones. Nunca retornar carpetas de otra organización.
 * </p>
 *
 * @author DocFlow Team
 */
public interface ICarpetaRepository {
    
    /**
     * Crea una nueva carpeta en la base de datos.
     * 
     * @param carpeta modelo de dominio a persistir
     * @return carpeta persistida con ID asignado
     * @throws CarpetaYaExisteException si ya existe una carpeta con el mismo nombre en el nivel
     */
    Carpeta crear(Carpeta carpeta);
    
    /**
     * Busca una carpeta por su ID, filtrando por organización.
     * 
     * @param id identificador único de la carpeta
     * @param organizacionId identificador de la organización (multi-tenancy)
     * @return Optional con la carpeta si existe y pertenece a la organización, vacío en caso contrario
     */
    Optional<Carpeta> obtenerPorId(Long id, Long organizacionId);
    
    /**
     * Lista las carpetas hijas de una carpeta padre.
     * 
     * @param carpetaPadreId identificador de la carpeta padre
     * @param organizacionId identificador de la organización
     * @return lista de carpetas hijas (solo activas, sin soft-deleted)
     */
    List<Carpeta> obtenerHijos(Long carpetaPadreId, Long organizacionId);
    
    /**
     * Obtiene la carpeta raíz de una organización.
     * 
     * @param organizacionId identificador de la organización
     * @return Optional con la carpeta raíz si existe, vacío en caso contrario
     */
    Optional<Carpeta> obtenerRaiz(Long organizacionId);
    
    /**
     * Verifica si ya existe una carpeta con el mismo nombre en el mismo nivel.
     * 
     * <p>Unicidad de nombre aplica dentro de:
     * - La misma organización
     * - El mismo carpeta_padre_id (o ambos NULL para carpetas raíz)
     * - Solo carpetas activas (fecha_eliminacion IS NULL)
     * </p>
     * 
     * @param nombre nombre de la carpeta a verificar
     * @param carpetaPadreId identificador de la carpeta padre (NULL para raíz)
     * @param organizacionId identificador de la organización
     * @return true si ya existe una carpeta con ese nombre en el nivel, false en caso contrario
     */
    boolean nombreExisteEnNivel(String nombre, Long carpetaPadreId, Long organizacionId);
    
    /**
     * Elimina lógicamente una carpeta estableciendo fecha_eliminacion.
     * 
     * <p>No elimina físicamente el registro para mantener auditoría.
     * Las carpetas eliminadas no aparecen en consultas normales.</p>
     * 
     * @param id identificador de la carpeta a eliminar
     * @param organizacionId identificador de la organización
     * @return true si se eliminó correctamente, false si no se encontró
     */
    boolean eliminarLogicamente(Long id, Long organizacionId);
    
    /**
     * Actualiza una carpeta existente.
     * 
     * @param carpeta modelo de dominio con los cambios
     * @return carpeta actualizada
     */
    Carpeta actualizar(Carpeta carpeta);

    /**
     * Obtiene la ruta de ancestros de una carpeta ordenada desde el más cercano.
     *
     * @param carpetaId identificador de la carpeta objetivo
     * @param organizacionId identificador de la organización
     * @return lista de ancestros con nivel de distancia
     */
    List<CarpetaAncestro> obtenerRutaAncestros(
            Long carpetaId,
            Long organizacionId
    );

    /**
     * Obtiene subcarpetas de una carpeta con permisos filtrados para un usuario.
     * 
     * <p>Este método es crítico para la funcionalidad de listado de contenido.
     * Retorna solo subcarpetas sobre las que el usuario tiene acceso de lectura,
     * considerando permisos heredados y precedencia de ACLs.</p>
     * 
     * @param carpetaPadreId identificador de la carpeta padre
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @return lista de subcarpetas accesibles para el usuario (solo activas)
     */
    List<Carpeta> obtenerSubcarpetasConPermiso(
            Long carpetaPadreId,
            Long usuarioId,
            Long organizacionId
    );

    /**
     * Cuenta el total de subcarpetas de una carpeta que son accesibles para un usuario.
     * 
     * <p>Utilizado para calcular el totalSubcarpetas en el listado de contenido.</p>
     * 
     * @param carpetaPadreId identificador de la carpeta padre
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @return número total de subcarpetas accesibles
     */
    int contarSubcarpetasConPermiso(
            Long carpetaPadreId,
            Long usuarioId,
            Long organizacionId
    );

    /**
     * Verifica si una carpeta está vacía (sin subcarpetas ni documentos activos).
     * 
     * <p><strong>Regla de Negocio (US-FOLDER-004):</strong>
     * Una carpeta está vacía si:
     * - No tiene subcarpetas con fecha_eliminacion IS NULL
     * - No tiene documentos con fecha_eliminacion IS NULL
     * </p>
     * 
     * <p>Esta consulta utiliza EXISTS para eficiencia (evita conteos completos).</p>
     * 
     * @param carpetaId identificador de la carpeta a verificar
     * @param organizacionId identificador de la organización
     * @return true si la carpeta está vacía, false si contiene contenido activo
     */
    boolean estaVacia(Long carpetaId, Long organizacionId);

    /**
     * Cuenta las subcarpetas activas directas de una carpeta.
     * 
     * <p>Se utiliza para reportar el número de subcarpetas que impiden
     * la eliminación de una carpeta en respuestas de error.</p>
     * 
     * @param carpetaId identificador de la carpeta padre
     * @param organizacionId identificador de la organización
     * @return número de subcarpetas con fecha_eliminacion IS NULL
     */
    int contarSubcarpetasActivas(Long carpetaId, Long organizacionId);

    /**
     * Cuenta los documentos activos directos de una carpeta.
     * 
     * <p>Se utiliza para reportar el número de documentos que impiden
     * la eliminación de una carpeta en respuestas de error.</p>
     * 
     * @param carpetaId identificador de la carpeta
     * @param organizacionId identificador de la organización
     * @return número de documentos con fecha_eliminacion IS NULL
     */
    int contarDocumentosActivos(Long carpetaId, Long organizacionId);
}

