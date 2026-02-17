package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.DocumentoMovidoEvent;
import com.docflow.documentcore.application.dto.DocumentoMovidoResponse;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.MismaUbicacionException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IDocumentoRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación para operación de mover documentos entre carpetas.
 * 
 * <p>Implementa la lógica de negocio para US-FOLDER-003:
 * <ul>
 *   <li>Validación de existencia de documento y carpeta destino</li>
 *   <li>Validación dual de permisos (ESCRITURA en origen y destino)</li>
 *   <li>Regla de negocio: no se puede mover a la misma carpeta</li>
 *   <li>Actualización atómica con emisión de evento de auditoría</li>
 * </ul>
 * 
 * @see com.docflow.documentcore.application.dto.MoverDocumentoRequest
 * @see com.docflow.documentcore.application.dto.DocumentoMovidoResponse
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DocumentoMoverService {
    
    private final IDocumentoRepository documentoRepository;
    private final ICarpetaRepository carpetaRepository;
    private final IEvaluadorPermisos evaluadorPermisos;
    // TODO: Agregar publicador de eventos de auditoría cuando P5 esté implementado
    // private final AuditEventPublisher auditPublisher;
    
    /**
     * Mueve un documento de una carpeta origen a una carpeta destino.
     * 
     * <p>Valida que:
     * <ul>
     *   <li>El documento existe y pertenece a la organización</li>
     *   <li>La carpeta destino existe y pertenece a la organización</li>
     *   <li>El documento no está ya en la carpeta destino</li>
     *   <li>El usuario tiene permiso ESCRITURA en la carpeta origen</li>
     *   <li>El usuario tiene permiso ESCRITURA en la carpeta destino</li>
     * </ul>
     * 
     * <p>La operación es atómica: actualiza el documento y emite evento de auditoría
     * en la misma transacción.
     * 
     * @param documentoId ID del documento a mover
     * @param carpetaDestinoId ID de la carpeta destino
     * @param usuarioId ID del usuario que realiza la operación
     * @param organizacionId ID de la organización (multi-tenant)
     * @return Response con información del movimiento
     * @throws ResourceNotFoundException si el documento no existe
     * @throws CarpetaNotFoundException si la carpeta destino no existe
     * @throws MismaUbicacionException si se intenta mover a la misma carpeta
     * @throws AccessDeniedException si no tiene permisos en origen o destino
     */
    public DocumentoMovidoResponse moverDocumento(
        Long documentoId,
        Long carpetaDestinoId,
        Long usuarioId,
        Long organizacionId
    ) {
        log.info("Moving document: documentoId={}, carpetaDestinoId={}, usuarioId={}, org={}",
                documentoId, carpetaDestinoId, usuarioId, organizacionId);
        
        // Paso 1: Validar que el documento existe y pertenece a la organización
        Documento documento = documentoRepository.obtenerPorId(documentoId, organizacionId)
                .orElseThrow(() -> {
                    log.warn("Document not found: documentoId={}, org={}", documentoId, organizacionId);
                    return new ResourceNotFoundException("Documento", documentoId);
                });
        
        Long carpetaOrigenId = documento.getCarpetaId();
        
        // Paso 2: Validar que la carpeta destino existe
        carpetaRepository.obtenerPorId(carpetaDestinoId, organizacionId)
                .orElseThrow(() -> {
                    log.warn("Destination folder not found: carpetaId={}, org={}", carpetaDestinoId, organizacionId);
                    return new CarpetaNotFoundException(carpetaDestinoId);
                });
        
        // Paso 3: Regla de negocio - no se puede mover a la misma carpeta
        if (carpetaOrigenId.equals(carpetaDestinoId)) {
            log.warn("Attempted to move document to same folder: documentoId={}, carpetaId={}",
                    documentoId, carpetaDestinoId);
            throw new MismaUbicacionException(documentoId, carpetaDestinoId);
        }
        
        // Paso 4: Validar permiso de ESCRITURA en carpeta origen
        PermisoEfectivo permisoOrigen = evaluadorPermisos.evaluarPermisoCarpeta(
                usuarioId, carpetaOrigenId, organizacionId);
        
        if (permisoOrigen == null || 
            !tienePermisoEscritura(permisoOrigen.getNivelAcceso())) {
            log.warn("No WRITE permission on origin folder: userId={}, carpetaOrigenId={}",
                    usuarioId, carpetaOrigenId);
            throw new AccessDeniedException(
                "No tiene permiso de escritura en la carpeta origen",
                "SIN_PERMISO_ORIGEN"
            );
        }
        
        // Paso 5: Validar permiso de ESCRITURA en carpeta destino
        PermisoEfectivo permisoDestino = evaluadorPermisos.evaluarPermisoCarpeta(
                usuarioId, carpetaDestinoId, organizacionId);
        
        if (permisoDestino == null || 
            !tienePermisoEscritura(permisoDestino.getNivelAcceso())) {
            log.warn("No WRITE permission on destination folder: userId={}, carpetaDestinoId={}",
                    usuarioId, carpetaDestinoId);
            throw new AccessDeniedException(
                "No tiene permiso de escritura en la carpeta destino",
                "SIN_PERMISO_DESTINO"
            );
        }
        
        // Paso 6: Actualizar ubicación del documento
        documento.setCarpetaId(carpetaDestinoId);
        documento.setFechaActualizacion(java.time.OffsetDateTime.now());
        
        // La persistencia es automática debido a @Transactional + entidad gestionada por JPA
        log.info("Document moved successfully: documentoId={}, from carpeta {} to carpeta {}",
                documentoId, carpetaOrigenId, carpetaDestinoId);
        
        // Paso 7: Emitir evento de auditoría (dentro de la misma transacción)
        emitirEventoAuditoria(documentoId, carpetaOrigenId, carpetaDestinoId, usuarioId, organizacionId);
        
        // Paso 8: Retornar respuesta
        return new DocumentoMovidoResponse(
            documentoId,
            carpetaOrigenId,
            carpetaDestinoId,
            "Documento movido exitosamente"
        );
    }
    
    /**
     * Verifica si el nivel de acceso permite operaciones de ESCRITURA.
     * Los niveles ESCRITURA y ADMINISTRACION permiten acceso de escritura.
     * 
     * @param nivel Nivel de acceso a verificar
     * @return true si permite escritura, false en caso contrario
     */
    private boolean tienePermisoEscritura(NivelAcceso nivel) {
        return nivel == NivelAcceso.ESCRITURA || 
               nivel == NivelAcceso.ADMINISTRACION;
    }
    
    /**
     * Emite evento de auditoría para operación de mover documento.
     * 
     * <p>TODO: Integrar con servicio de auditoría cuando P5 esté implementado.
     * Por ahora solo registra el evento en logs.
     * 
     * @param documentoId ID del documento movido
     * @param carpetaOrigenId ID de la carpeta origen
     * @param carpetaDestinoId ID de la carpeta destino
     * @param usuarioId ID del usuario que realizó la operación
     * @param organizacionId ID de la organización
     */
    private void emitirEventoAuditoria(
        Long documentoId,
        Long carpetaOrigenId,
        Long carpetaDestinoId,
        Long usuarioId,
        Long organizacionId
    ) {
        new DocumentoMovidoEvent(
            documentoId,
            carpetaOrigenId,
            carpetaDestinoId,
            usuarioId,
            organizacionId
        );
        
        // TODO: Publicar al servicio de auditoría
        // auditPublisher.publish(event);
        
        log.info("Audit event emitted: DOCUMENTO_MOVIDO for documentoId={}", documentoId);
    }
}
