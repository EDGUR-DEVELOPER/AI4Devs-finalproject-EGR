package com.docflow.documentcore.domain.event;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando se revoca un permiso explícito de carpeta.
 */
public class PermisoCarpetaUsuarioRevokedEvent {

    private final Long permisoId;
    private final Long carpetaId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final String nivelAcceso;
    private final Long revocadoPor;
    private final Instant timestamp;

    public PermisoCarpetaUsuarioRevokedEvent(
            Long permisoId,
            Long carpetaId,
            Long usuarioId,
            Long organizacionId,
            String nivelAcceso,
            Long revocadoPor,
            Instant timestamp
    ) {
        this.permisoId = permisoId;
        this.carpetaId = carpetaId;
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.nivelAcceso = nivelAcceso;
        this.revocadoPor = revocadoPor;
        this.timestamp = timestamp;
    }

    public Long getPermisoId() {
        return permisoId;
    }

    public Long getCarpetaId() {
        return carpetaId;
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

    public Long getRevocadoPor() {
        return revocadoPor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "PermisoCarpetaUsuarioRevokedEvent{" +
                "permisoId=" + permisoId +
                ", carpetaId=" + carpetaId +
                ", usuarioId=" + usuarioId +
                ", organizacionId=" + organizacionId +
                ", nivelAcceso='" + nivelAcceso + '\'' +
                ", revocadoPor=" + revocadoPor +
                ", timestamp=" + timestamp +
                '}';
    }
}
