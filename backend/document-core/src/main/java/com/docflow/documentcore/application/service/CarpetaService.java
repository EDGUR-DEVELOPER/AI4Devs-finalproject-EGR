package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.validator.CarpetaValidator;
import com.docflow.documentcore.domain.event.CarpetaCreatedEvent;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNoVaciaException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaRaizNoEliminableException;
import com.docflow.documentcore.domain.exception.carpeta.SinPermisoCarpetaException;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.CarpetaAncestro;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Servicio de aplicación para operaciones con carpetas.
 * 
 * <p>Orquesta la lógica de negocio, validaciones y emisión de eventos
 * siguiendo el patrón de arquitectura hexagonal.</p>
 *
 * <p><strong>Responsabilidades:</strong>
 * <ul>
 *   <li>Orquestar validaciones de negocio</li>
 *   <li>Coordinar operaciones del repositorio</li>
 *   <li>Emitir eventos de dominio</li>
 *   <li>Garantizar transaccionalidad</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@Service
@Transactional
public class CarpetaService {
    
    private static final Logger logger = LoggerFactory.getLogger(CarpetaService.class);
    
    private final ICarpetaRepository carpetaRepository;
    private final CarpetaValidator validator;
    private final ApplicationEventPublisher eventPublisher;
    private final IEvaluadorPermisos evaluadorPermisos;
    private final PermisoCarpetaUsuarioService permisoCarpetaUsuarioService;
    
    public CarpetaService(
            ICarpetaRepository carpetaRepository,
            CarpetaValidator validator,
            ApplicationEventPublisher eventPublisher,
            IEvaluadorPermisos evaluadorPermisos,
            PermisoCarpetaUsuarioService permisoCarpetaUsuarioService
    ) {
        this.carpetaRepository = carpetaRepository;
        this.validator = validator;
        this.eventPublisher = eventPublisher;
        this.evaluadorPermisos = evaluadorPermisos;
        this.permisoCarpetaUsuarioService = permisoCarpetaUsuarioService;
    }
    
    /**
     * Crea una nueva carpeta con todas las validaciones necesarias.
     * 
     * @param nombre nombre de la carpeta
     * @param descripcion descripción opcional
     * @param carpetaPadreId identificador de la carpeta padre (null para carpeta raíz)
     * @param organizacionId identificador de la organización (desde token JWT)
     * @param usuarioId identificador del usuario creador (desde token JWT)
     * @return carpeta creada
     * @throws CarpetaNotFoundException si la carpeta padre no existe
     * @throws CarpetaNombreDuplicadoException si ya existe una carpeta con ese nombre
     * @throws SinPermisoCarpetaException si el usuario no tiene permisos
     */
    public Carpeta crear(
            String nombre,
            String descripcion,
            Long carpetaPadreId,
            Long organizacionId,
            Long usuarioId
    ) {
        logger.info("Iniciando creación de carpeta '{}' en organización {} por usuario {}",
                nombre, organizacionId, usuarioId);
        
        // 1. Validar que la carpeta padre existe (si se proporciona)
        validator.validarCarpetaPadreExiste(carpetaPadreId, organizacionId);
        
        // 2. Validar permisos del usuario sobre la carpeta padre
        validator.validarPermisos(usuarioId, carpetaPadreId, organizacionId);
        
        // 3. Validar que el nombre es único en este nivel
        validator.validarNombreUnico(nombre, carpetaPadreId, organizacionId);
        
        // 4. Construir el modelo de dominio
        Carpeta carpeta = Carpeta.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .carpetaPadreId(carpetaPadreId)
                .organizacionId(organizacionId)
                .creadoPor(usuarioId)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();
        
        // 5. Persistir
        Carpeta carpetaCreada = carpetaRepository.crear(carpeta);
        
        logger.info("Carpeta creada exitosamente con ID: {}", carpetaCreada.getId());

        NivelAcceso nivelAccesoInicial = NivelAcceso.ADMINISTRACION;
        if (carpetaPadreId != null) {
            PermisoEfectivo permisoPadre = evaluadorPermisos.evaluarPermisoCarpeta(
                usuarioId,
                carpetaPadreId,
                organizacionId
            );
            nivelAccesoInicial = permisoPadre != null
                ? permisoPadre.getNivelAcceso()
                : NivelAcceso.ESCRITURA;
        }

        permisoCarpetaUsuarioService.crearPermisoInicial(
            carpetaCreada.getId(),
            usuarioId,
            nivelAccesoInicial,
            organizacionId
        );
        
        // 6. Emitir evento de dominio para auditoría
        emitirEventoCarpetaCreada(carpetaCreada, usuarioId);
        
        return carpetaCreada;
    }
    
    /**
     * Obtiene una carpeta por su ID.
     * 
     * @param id identificador de la carpeta
     * @param organizacionId identificador de la organización
     * @return carpeta encontrada
     * @throws CarpetaNotFoundException si no se encuentra la carpeta
     */
    @Transactional(readOnly = true)
    public Carpeta obtenerPorId(Long id, Long organizacionId) {
        return carpetaRepository.obtenerPorId(id, organizacionId)
                .orElseThrow(() -> new CarpetaNotFoundException(id));
    }
    
    /**
     * Lista las carpetas hijas de una carpeta padre.
     * 
     * @param carpetaPadreId identificador de la carpeta padre
     * @param organizacionId identificador de la organización
     * @return lista de carpetas hijas
     */
    @Transactional(readOnly = true)
    public List<Carpeta> obtenerHijos(Long carpetaPadreId, Long organizacionId) {
        return carpetaRepository.obtenerHijos(carpetaPadreId, organizacionId);
    }

    /**
     * Obtiene la ruta de ancestros de una carpeta (del más cercano a la raíz).
     *
     * @param carpetaId identificador de la carpeta
     * @param organizacionId identificador de la organización
     * @return lista de ancestros con nivel de distancia
     */
    @Transactional(readOnly = true)
    public List<CarpetaAncestro> obtenerRutaAncestros(
            Long carpetaId,
            Long organizacionId
    ) {
        return carpetaRepository.obtenerRutaAncestros(carpetaId, organizacionId);
    }
    
    /**
     * Obtiene la carpeta raíz de una organización.
     * 
     * @param organizacionId identificador de la organización
     * @return carpeta raíz
     * @throws CarpetaNotFoundException si no existe carpeta raíz
     */
    @Transactional(readOnly = true)
    public Carpeta obtenerRaiz(Long organizacionId) {
        return carpetaRepository.obtenerRaiz(organizacionId)
                .orElseThrow(() -> new CarpetaNotFoundException(
                        "No se encontró carpeta raíz para la organización: " + organizacionId
                ));
    }

    /**
     * Elimina lógicamente una carpeta vacía (soft delete) con validaciones completas.
     *
     * @param carpetaId identificador de la carpeta
     * @param usuarioId identificador del usuario solicitante
     * @param organizacionId identificador de la organización
     * @throws CarpetaNotFoundException si no se encuentra la carpeta
     * @throws CarpetaRaizNoEliminableException si la carpeta es raíz
     * @throws SinPermisoCarpetaException si el usuario no tiene permisos
     * @throws CarpetaNoVaciaException si la carpeta no está vacía
     */
    public void eliminarCarpeta(Long carpetaId, Long usuarioId, Long organizacionId) {
        logger.info("Iniciando eliminación de carpeta {} en organización {} por usuario {}",
                carpetaId, organizacionId, usuarioId);

        // 1. Validar existencia
        validator.validarCarpetaExiste(carpetaId, organizacionId);

        // 2. Validar que no es carpeta raíz
        validator.validarNoEsRaiz(carpetaId, organizacionId);

        // 3. Validar permisos de administración
        validator.validarPermisos(usuarioId, carpetaId, organizacionId, NivelAcceso.ADMINISTRACION);

        // 4. Validar que la carpeta está vacía
        validator.validarCarpetaVacia(carpetaId, organizacionId);

        // 5. Ejecutar soft delete
        boolean eliminada = carpetaRepository.eliminarLogicamente(carpetaId, organizacionId);

        if (!eliminada) {
            throw new CarpetaNotFoundException(carpetaId);
        }

        logger.info("Carpeta {} eliminada lógicamente por usuario {}", carpetaId, usuarioId);
    }
    
    /**
     * Elimina lógicamente una carpeta.
     * 
     * @param id identificador de la carpeta
     * @param organizacionId identificador de la organización
     * @return true si se eliminó correctamente
     * @throws CarpetaNotFoundException si no se encuentra la carpeta
     */
    public boolean eliminar(Long id, Long organizacionId) {
        logger.info("Iniciando eliminación lógica de carpeta {} en organización {}", 
                id, organizacionId);
        
        boolean eliminada = carpetaRepository.eliminarLogicamente(id, organizacionId);
        
        if (!eliminada) {
            throw new CarpetaNotFoundException(id);
        }
        
        logger.info("Carpeta {} eliminada lógicamente", id);
        return true;
    }
    
    /**
     * Emite el evento de creación de carpeta para auditoría.
     */
    private void emitirEventoCarpetaCreada(Carpeta carpeta, Long usuarioId) {
        try {
            CarpetaCreatedEvent event = new CarpetaCreatedEvent(
                    carpeta.getId(),
                    carpeta.getOrganizacionId(),
                    usuarioId,
                    carpeta.getNombre(),
                    carpeta.getCarpetaPadreId(),
                    Instant.now()
            );
            
            eventPublisher.publishEvent(event);
            logger.debug("Evento CarpetaCreatedEvent emitido para carpeta: {}", carpeta.getId());
        } catch (Exception e) {
            // No fallar la transacción si falla la auditoría
            logger.error("Error al emitir evento de auditoría para carpeta: {}", carpeta.getId(), e);
        }
    }
}
