package com.docflow.documentcore.domain.event;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando un documento es eliminado logicamente.
 *
 * <p>Este evento es consumido por el sistema de auditoria para registrar
 * eliminaciones de documentos y mantener trazabilidad completa.</p>
 */
public class DocumentDeletedEvent extends ApplicationEvent {

    private final Long documentoId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final Instant timestamp;

    public DocumentDeletedEvent(
        Object source,
        Long documentoId,
        Long usuarioId,
        Long organizacionId
    ) {
        super(source);

        if (documentoId == null) {
            throw new IllegalArgumentException("documentoId no puede ser nulo");
        }
        if (usuarioId == null) {
            throw new IllegalArgumentException("usuarioId no puede ser nulo");
        }
        if (organizacionId == null) {
            throw new IllegalArgumentException("organizacionId no puede ser nulo");
        }

        this.documentoId = documentoId;
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.timestamp = Instant.now();
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getOrganizacionId() {
        return organizacionId;
    }

    public Instant getEventTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format(
            "DocumentDeletedEvent{documentoId=%d, usuarioId=%d, organizacionId=%d, timestamp=%s}",
            documentoId, usuarioId, organizacionId, timestamp
        );
    }
}
