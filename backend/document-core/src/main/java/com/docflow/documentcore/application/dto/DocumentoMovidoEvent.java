package com.docflow.documentcore.application.dto;

import java.time.OffsetDateTime;

/**
 * DTO para payload de evento de auditoría DOCUMENTO_MOVIDO.
 * 
 * <p>Enviado al servicio de log de auditoría después de mover exitosamente un documento.</p>
 *
 * @author DocFlow Team
 */
public class DocumentoMovidoEvent {
    
    private String codigoEvento = "DOCUMENTO_MOVIDO";
    private Long documentoId;
    private Long carpetaOrigenId;
    private Long carpetaDestinoId;
    private Long usuarioId;
    private Long organizacionId;
    private OffsetDateTime timestamp;
    
    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================
    
    public DocumentoMovidoEvent(
        Long documentoId,
        Long carpetaOrigenId,
        Long carpetaDestinoId,
        Long usuarioId,
        Long organizacionId
    ) {
        this.documentoId = documentoId;
        this.carpetaOrigenId = carpetaOrigenId;
        this.carpetaDestinoId = carpetaDestinoId;
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.timestamp = OffsetDateTime.now();
    }
    
    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================
    
    public String getCodigoEvento() {
        return codigoEvento;
    }
    
    public Long getDocumentoId() {
        return documentoId;
    }
    
    public Long getCarpetaOrigenId() {
        return carpetaOrigenId;
    }
    
    public Long getCarpetaDestinoId() {
        return carpetaDestinoId;
    }
    
    public Long getUsuarioId() {
        return usuarioId;
    }
    
    public Long getOrganizacionId() {
        return organizacionId;
    }
    
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
