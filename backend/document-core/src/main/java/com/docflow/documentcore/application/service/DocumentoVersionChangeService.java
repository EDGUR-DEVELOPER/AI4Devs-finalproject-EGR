package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.ChangeCurrentVersionRequest;
import com.docflow.documentcore.application.dto.DocumentoResponse;
import com.docflow.documentcore.domain.exception.InsufficientPermissionsException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.exception.VersionNotBelongToDocumentException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import com.docflow.documentcore.domain.service.VersionChangeEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Servicio de aplicación para gestionar cambios de versión actual de documentos (rollback).
 * 
 * <p>Este servicio implementa la funcionalidad de cambiar la versión actual de un documento
 * a una versión anterior, permitiendo efectivamente realizar rollback de cambios no deseados
 * manteniendo el historial completo de versiones.</p>
 * 
 * <h3>Características Principales</h3>
 * <ul>
 *   <li><strong>Atómico:</strong> Toda la operación es transaccional (todo o nada)</li>
 *   <li><strong>Auditado:</strong> Registra evento de auditoría con usuario, documento y versiones</li>
 *   <li><strong>Seguro:</strong> Valida permisos elevados (ADMINISTRACION) antes de proceder</li>
 *   <li><strong>Multi-tenant:</strong> Respeta aislamiento de organización</li>
 *   <li><strong>Idempotente:</strong> Cambiar a la versión actual es operación válida</li>
 * </ul>
 * 
 * <h3>Validaciones Realizadas</h3>
 * <ol>
 *   <li>Documento existe y pertenece a la organización del usuario</li>
 *   <li>Usuario tiene permiso ADMINISTRACION sobre el documento</li>
 *   <li>Versión existe y pertenece al documento especificado</li>
 * </ol>
 * 
 * <h3>Orden de Validación (Fail-Fast)</h3>
 * <p>Las validaciones se ejecutan en orden de costo/importancia:</p>
 * <pre>
 * 1. Existencia del documento (barato, crítico)
 * 2. Permisos del usuario (medio, crítico para seguridad)
 * 3. Validación de versión (barato, específico de operación)
 * </pre>
 * 
 * @see DocumentoResponse
 * @see ChangeCurrentVersionRequest
 * @see VersionChangeEventPublisher
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class DocumentoVersionChangeService {
    
    private final DocumentoRepository documentoRepository;
    private final VersionRepository versionRepository;
    private final IEvaluadorPermisos evaluadorPermisos;
    private final VersionChangeEventPublisher auditPublisher;
    
    /**
     * Cambia la versión actual de un documento a una versión específica (rollback).
     * 
     * <p>Este método realiza una serie de validaciones de seguridad y negocio
     * antes de ejecutar el cambio de versión. Si alguna validación falla, la operación
     * se interrumpe y se lanza la excepción correspondiente.</p>
     * 
     * <h3>Flujo de Ejecución</h3>
     * <pre>
     * 1. Validar aislamiento multi-tenant (documento existe en organización)
     * 2. Validar permisos elevados (usuario tiene ADMINISTRACION)
     * 3. Validar versión (existe y pertenece al documento)
     * 4. Guardar versión anterior para auditoría
     * 5. Actualizar versión actual del documento
     * 6. Publicar evento de auditoría
     * 7. Construir y retornar respuesta
     * </pre>
     * 
     * <h3>Idempotencia</h3>
     * <p>Si la versión solicitada ya es la versión actual, la operación completa
     * exitosamente sin error. El evento de auditoría se registra igualmente para
     * mantener trazabilidad completa.</p>
     * 
     * <h3>Ejemplo de uso:</h3>
     * <pre>
     * DocumentoResponse response = service.cambiarVersionActual(
     *     100L,  // documentoId
     *     201L,  // versionId
     *     1L,    // usuarioId
     *     1L     // organizacionId
     * );
     * </pre>
     * 
     * @param documentoId ID del documento sobre el cual realizar el rollback
     * @param versionId ID de la versión que debe convertirse en versión actual
     * @param usuarioId ID del usuario que ejecuta la operación
     * @param organizacionId ID de la organización/tenant del usuario
     * @return DocumentoResponse con información actualizada del documento
     * 
     * @throws ResourceNotFoundException si el documento no existe o no pertenece a la organización
     * @throws InsufficientPermissionsException si el usuario no tiene permiso ADMINISTRACION
     * @throws VersionNotBelongToDocumentException si la versión no existe o pertenece a otro documento
     */
    public DocumentoResponse cambiarVersionActual(
        Long documentoId,
        Long versionId,
        Long usuarioId,
        Long organizacionId
    ) {
        log.info("Iniciando cambio de versión actual - Documento: {}, Versión: {}, Usuario: {}, Organización: {}", 
                 documentoId, versionId, usuarioId, organizacionId);
        
        // 1. Validar aislamiento multi-tenant: documento existe y pertenece a organización
        Documento documento = documentoRepository.findByIdAndOrganizacionId(documentoId, organizacionId)
            .orElseThrow(() -> {
                log.warn("Documento {} no encontrado para organización {}", documentoId, organizacionId);
                return new ResourceNotFoundException("Documento", documentoId);
            });
        
        // 2. Validar permisos elevados (ADMINISTRACION) sobre el documento
        boolean tienePermiso = evaluadorPermisos.tieneAcceso(
            usuarioId, 
            documentoId, 
            TipoRecurso.DOCUMENTO, 
            CodigoNivelAcceso.ADMINISTRACION,
            organizacionId
        );
        
        if (!tienePermiso) {
            log.warn("Usuario {} intenta cambiar versión actual de documento {} sin permiso ADMINISTRACION", 
                     usuarioId, documentoId);
            throw new InsufficientPermissionsException("ADMINISTRACION", "DOCUMENTO");
        }
        
        // 3. Validar que la versión existe y pertenece al documento
        Version version = versionRepository.findByIdAndDocumentoId(versionId, documentoId)
            .orElseThrow(() -> {
                log.warn("Versión {} no pertenece al documento {} o no existe", versionId, documentoId);
                return new VersionNotBelongToDocumentException(versionId, documentoId);
            });
        
        // 4. Guardar versión anterior para auditoría (antes del cambio)
        Long versionAnteriorId = documento.getVersionActualId();
        
        // 5. Verificar si es operación idempotente (cambio a misma versión actual)
        boolean esIdempotente = versionId.equals(versionAnteriorId);
        if (esIdempotente) {
            log.info("Operación idempotente detectada: versión {} ya es la versión actual del documento {}", 
                     versionId, documentoId);
        }
        
        // 6. Actualizar versión actual (atómicamente)
        documento.setVersionActualId(versionId);
        documento.setFechaActualizacion(OffsetDateTime.now());
        documentoRepository.save(documento);
        
        log.info("Versión rollback ejecutado exitosamente - Documento: {}, Versión anterior: {}, Versión nueva: {}", 
                 documentoId, versionAnteriorId, versionId);
        
        // 7. Publicar evento de auditoría (síncrono, participa en transacción)
        OffsetDateTime timestamp = OffsetDateTime.now();
        auditPublisher.publishVersionRollbackEvent(
            usuarioId, 
            documentoId, 
            organizacionId, 
            versionAnteriorId, 
            versionId, 
            timestamp
        );
        
        log.debug("Evento de auditoría publicado para cambio de versión - Documento: {}", documentoId);
        
        // 8. Construir y retornar respuesta con documento actualizado
        return construirDocumentoResponse(documento, version);
    }
    
    /**
     * Construye el DTO de respuesta con información del documento actualizado.
     * 
     * @param documento Entidad del documento actualizado
     * @param versionActual Entidad de la versión que ahora es actual
     * @return DocumentoResponse con datos completos
     */
    private DocumentoResponse construirDocumentoResponse(Documento documento, Version versionActual) {
        // Construir información de la versión actual
        DocumentoResponse.VersionInfoDTO versionInfo = DocumentoResponse.VersionInfoDTO.builder()
            .id(versionActual.getId())
            .numeroSecuencial(versionActual.getNumeroSecuencial())
            .tamanioBytes(versionActual.getTamanioBytes())
            .hashContenido(versionActual.getHashContenido())
            .fechaCreacion(versionActual.getFechaCreacion())
            .build();
        
        // Construir respuesta completa del documento
        return DocumentoResponse.builder()
            .id(documento.getId())
            .nombre(documento.getNombre())
            .extension(documento.getExtension())
            .tipoContenido(documento.getTipoContenido())
            .tamanioBytes(documento.getTamanioBytes())
            .carpetaId(documento.getCarpetaId())
            .versionActual(versionInfo)
            .numeroVersiones(documento.getNumeroVersiones())
            .bloqueado(documento.getBloqueado())
            .etiquetas(documento.getEtiquetas().toArray(new String[0]))
            .fechaCreacion(documento.getFechaCreacion())
            .fechaActualizacion(documento.getFechaActualizacion())
            .build();
    }
}
