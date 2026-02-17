package com.docflow.documentcore.domain.event;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando un usuario descarga un documento.
 * 
 * <p>Este evento es consumido por el sistema de auditoría para registrar
 * todas las descargas de documentos, permitiendo trazabilidad completa
 * de accesos a información sensible.</p>
 * 
 * <p><strong>US-DOC-002:</strong> Auditoría de descarga de documentos.</p>
 * 
 * <p><strong>Políticas de Registro:</strong></p>
 * <ul>
 *   <li>Registra cada descarga exitosa (no intentos fallidos)</li>
 *   <li>Incluye tamaño del archivo para análisis de uso de ancho de banda</li>
 *   <li>Enlaza descarga con versión específica del documento</li>
 * </ul>
 * 
 * @author DocFlow Team
 */
public class DocumentDownloadedEvent extends ApplicationEvent {
    
    private final Long documentoId;
    private final Long versionId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final Long tamanioBytes;
    private final Instant timestamp;
    
    /**
     * Crea un nuevo evento de descarga de documento.
     * 
     * @param source fuente del evento (típicamente el servicio que dispara el evento)
     * @param documentoId ID del documento descargado
     * @param versionId ID de la versión específica descargada
     * @param usuarioId ID del usuario que realizó la descarga
     * @param organizacionId ID de la organización del usuario
     * @param tamanioBytes tamaño del archivo descargado en bytes
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    public DocumentDownloadedEvent(
        Object source,
        Long documentoId,
        Long versionId,
        Long usuarioId,
        Long organizacionId,
        Long tamanioBytes
    ) {
        super(source);
        
        if (documentoId == null) {
            throw new IllegalArgumentException("documentoId no puede ser nulo");
        }
        if (versionId == null) {
            throw new IllegalArgumentException("versionId no puede ser nulo");
        }
        if (usuarioId == null) {
            throw new IllegalArgumentException("usuarioId no puede ser nulo");
        }
        if (organizacionId == null) {
            throw new IllegalArgumentException("organizacionId no puede ser nulo");
        }
        if (tamanioBytes == null || tamanioBytes < 0) {
            throw new IllegalArgumentException("tamanioBytes debe ser no nulo y no negativo");
        }
        
        this.documentoId = documentoId;
        this.versionId = versionId;
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.tamanioBytes = tamanioBytes;
        this.timestamp = Instant.now();
    }
    
    public Long getDocumentoId() {
        return documentoId;
    }
    
    public Long getVersionId() {
        return versionId;
    }
    
    public Long getUsuarioId() {
        return usuarioId;
    }
    
    public Long getOrganizacionId() {
        return organizacionId;
    }
    
    public Long getTamanioBytes() {
        return tamanioBytes;
    }
    
    public Instant getEventTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format(
            "DocumentDownloadedEvent{documentoId=%d, versionId=%d, usuarioId=%d, organizacionId=%d, tamanioBytes=%d, timestamp=%s}",
            documentoId, versionId, usuarioId, organizacionId, tamanioBytes, timestamp
        );
    }
}
