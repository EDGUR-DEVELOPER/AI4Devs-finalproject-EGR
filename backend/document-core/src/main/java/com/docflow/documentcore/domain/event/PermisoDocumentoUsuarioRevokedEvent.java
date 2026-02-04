package com.docflow.documentcore.domain.event;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando se revoca un permiso explícito de documento.
 */
public class PermisoDocumentoUsuarioRevokedEvent {

    private final Long permisoId;
    private final Long documentoId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final String nivelAccesoAnterior;
    private final Long revocadoPor;
    private final Instant timestamp;

    public PermisoDocumentoUsuarioRevokedEvent(
            Long permisoId,
            Long documentoId,
            Long usuarioId,
            Long organizacionId,
            String nivelAccesoAnterior,
            Long revocadoPor,
            Instant timestamp
    ) {
        this.permisoId = permisoId;
        this.documentoId = documentoId;
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.nivelAccesoAnterior = nivelAccesoAnterior;
        this.revocadoPor = revocadoPor;
        this.timestamp = timestamp;
    }

    public Long getPermisoId() {
        return permisoId;
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

    public String getNivelAccesoAnterior() {
        return nivelAccesoAnterior;
    }

    public Long getRevocadoPor() {
        return revocadoPor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PermisoDocumentoUsuarioRevokedEvent{" +
                "permisoId=" + permisoId +
                ", documentoId=" + documentoId +
                ", usuarioId=" + usuarioId +
                ", organizacionId=" + organizacionId +
                ", nivelAccesoAnterior='" + nivelAccesoAnterior + '\'' +
                ", revocadoPor=" + revocadoPor +
                ", timestamp=" + timestamp +
                '}';
    }
}
