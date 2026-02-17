package com.docflow.documentcore.domain.event;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando se actualiza un permiso explícito de carpeta.
 */
public class PermisoCarpetaUsuarioUpdatedEvent {

    private final Long permisoId;
    private final Long carpetaId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final String nivelAnterior;
    private final String nivelNuevo;
    private final Boolean recursivo;
    private final Long actualizadoPor;
    private final Instant timestamp;

    public PermisoCarpetaUsuarioUpdatedEvent(
            Long permisoId,
            Long carpetaId,
            Long usuarioId,
            Long organizacionId,
            String nivelAnterior,
            String nivelNuevo,
            Boolean recursivo,
            Long actualizadoPor,
            Instant timestamp
    ) {
        this.permisoId = permisoId;
        this.carpetaId = carpetaId;
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.nivelAnterior = nivelAnterior;
        this.nivelNuevo = nivelNuevo;
        this.recursivo = recursivo;
        this.actualizadoPor = actualizadoPor;
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

    public String getNivelAnterior() {
        return nivelAnterior;
    }

    public String getNivelNuevo() {
        return nivelNuevo;
    }

    public Boolean getRecursivo() {
        return recursivo;
    }

    public Long getActualizadoPor() {
        return actualizadoPor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
