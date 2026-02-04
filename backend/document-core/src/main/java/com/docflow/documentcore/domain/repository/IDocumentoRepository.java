package com.docflow.documentcore.domain.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

import com.docflow.documentcore.domain.model.Documento;

/**
 * Interfaz del repositorio de Documentos (Port de salida).
 * 
 * <p>Define el contrato para operaciones de persistencia de documentos,
 * siguiendo los principios de arquitectura hexagonal donde el dominio
 * define las interfaces y la infraestructura las implementa.</p>
 *
 * <p><strong>Responsabilidades:</strong>
 * <ul>
 *   <li>CRUD de documentos con aislamiento por organizacion_id</li>
 *   <li>Consultas de documentos en carpetas con filtrado de permisos</li>
 *   <li>Soporte de eliminación lógica</li>
 * </ul>
 * </p>
 *
 * <p><strong>Reglas de Multi-Tenancy:</strong>
 * Todos los métodos DEBEN filtrar por organizacionId para garantizar
 * aislamiento entre organizaciones. Nunca retornar documentos de otra organización.
 * </p>
 *
 * @author DocFlow Team
 */
public interface IDocumentoRepository {
    
    /**
     * Busca un documento por su ID, filtrando por organización.
     * 
     * @param id identificador único del documento
     * @param organizacionId identificador de la organización (multi-tenancy)
     * @return Optional con el documento si existe y pertenece a la organización, vacío en caso contrario
     */
    Optional<Documento> obtenerPorId(Long id, Long organizacionId);
    
    /**
     * Obtiene documentos de una carpeta con permisos filtrados para un usuario.
     * 
     * <p>Este método es crítico para la funcionalidad de listado de contenido.
     * Retorna solo documentos sobre los que el usuario tiene acceso de lectura,
     * considerando permisos directos en el documento y herencia del documento.</p>
     * 
     * @param carpetaId identificador de la carpeta contenedora
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @param pageable criterios de paginación y ordenamiento
     * @return página de documentos accesibles para el usuario (solo activos)
     */
    List<Documento> obtenerDocumentosConPermiso(
            Long carpetaId,
            Long usuarioId,
            Long organizacionId,
            Pageable pageable
    );

    /**
     * Cuenta el total de documentos de una carpeta que son accesibles para un usuario.
     * 
     * <p>Utilizado para calcular el totalDocumentos en el listado de contenido.</p>
     * 
     * @param carpetaId identificador de la carpeta contenedora
     * @param usuarioId identificador del usuario
     * @param organizacionId identificador de la organización
     * @return número total de documentos accesibles
     */
    long contarDocumentosConPermiso(
            Long carpetaId,
            Long usuarioId,
            Long organizacionId
    );
}
