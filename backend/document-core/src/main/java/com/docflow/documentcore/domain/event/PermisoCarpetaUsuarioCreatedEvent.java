package com.docflow.documentcore.domain.event;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando se crea un permiso explícito de carpeta.
 */
public class PermisoCarpetaUsuarioCreatedEvent {

    private final Long permisoId;
    private final Long carpetaId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final String nivelAcceso;
    private final Boolean recursivo;
    private final Long otorgadoPor;
    private final Instant timestamp;

    public PermisoCarpetaUsuarioCreatedEvent(
            Long permisoId,
            Long carpetaId,
            Long usuarioId,
            Long organizacionId,
            String nivelAcceso,
            Boolean recursivo,
            Long otorgadoPor,
            Instant timestamp
    ) {
        this.permisoId = permisoId;
        this.carpetaId = carpetaId;
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
        this.nivelAcceso = nivelAcceso;
        this.recursivo = recursivo;
        this.otorgadoPor = otorgadoPor;
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

    public Boolean getRecursivo() {
        return recursivo;
    }

    public Long getOtorgadoPor() {
        return otorgadoPor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
