package com.docflow.documentcore.domain.event;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando se crea un permiso explícito de documento.
 */
public class PermisoDocumentoUsuarioCreatedEvent {

    private final Long permisoId;
    private final Long documentoId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final String nivelAcceso;
    private final Long otorgadoPor;
    private final Instant timestamp;

    public PermisoDocumentoUsuarioCreatedEvent(
            Long permisoId,
            Long documentoId,
            Long usuarioId,
            Long organizacionId,
            String nivelAcceso,
            Long otorgadoPor,
            Instant timestamp
    ) {
        this.permisoId = permisoId;
        this.documentoId = documentoId;
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.nivelAcceso = nivelAcceso;
        this.otorgadoPor = otorgadoPor;
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

    public String getNivelAcceso() {
        return nivelAcceso;
    }

    public Long getOtorgadoPor() {
        return otorgadoPor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PermisoDocumentoUsuarioCreatedEvent{" +
                "permisoId=" + permisoId +
                ", documentoId=" + documentoId +
                ", usuarioId=" + usuarioId +
                ", organizacionId=" + organizacionId +
                ", nivelAcceso='" + nivelAcceso + '\'' +
                ", otorgadoPor=" + otorgadoPor +
                ", timestamp=" + timestamp +
                '}';
    }
}
