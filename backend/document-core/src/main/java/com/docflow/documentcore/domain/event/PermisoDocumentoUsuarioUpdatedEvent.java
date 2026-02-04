package com.docflow.documentcore.domain.event;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando se actualiza un permiso explícito de documento.
 */
public class PermisoDocumentoUsuarioUpdatedEvent {

    private final Long permisoId;
    private final Long documentoId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final String nivelAccesoAnterior;
    private final String nivelAccesoNuevo;
    private final Long actualizadoPor;
    private final Instant timestamp;

    public PermisoDocumentoUsuarioUpdatedEvent(
            Long permisoId,
            Long documentoId,
            Long usuarioId,
            Long organizacionId,
            String nivelAccesoAnterior,
            String nivelAccesoNuevo,
            Long actualizadoPor,
            Instant timestamp
    ) {
        this.permisoId = permisoId;
        this.documentoId = documentoId;
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.nivelAccesoAnterior = nivelAccesoAnterior;
        this.nivelAccesoNuevo = nivelAccesoNuevo;
        this.actualizadoPor = actualizadoPor;
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

    public String getNivelAccesoNuevo() {
        return nivelAccesoNuevo;
    }

    public Long getActualizadoPor() {
        return actualizadoPor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PermisoDocumentoUsuarioUpdatedEvent{" +
                "permisoId=" + permisoId +
                ", documentoId=" + documentoId +
                ", usuarioId=" + usuarioId +
                ", organizacionId=" + organizacionId +
                ", nivelAccesoAnterior='" + nivelAccesoAnterior + '\'' +
                ", nivelAccesoNuevo='" + nivelAccesoNuevo + '\'' +
                ", actualizadoPor=" + actualizadoPor +
                ", timestamp=" + timestamp +
                '}';
    }
}
