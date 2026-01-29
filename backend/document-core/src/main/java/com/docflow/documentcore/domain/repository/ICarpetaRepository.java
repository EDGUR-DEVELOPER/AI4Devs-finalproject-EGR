package com.docflow.documentcore.domain.repository;

import java.util.List;
import java.util.Optional;

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
}
