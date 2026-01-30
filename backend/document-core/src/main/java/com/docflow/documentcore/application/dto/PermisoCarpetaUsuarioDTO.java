package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para permisos explícitos de usuarios sobre carpetas.
 */
public class PermisoCarpetaUsuarioDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("carpeta_id")
    private Long carpetaId;

    @JsonProperty("usuario_id")
    private Long usuarioId;

    @JsonProperty("usuario")
    private UsuarioResumenDTO usuario;

    @JsonProperty("nivel_acceso")
    private NivelAccesoDTO nivelAcceso;

    @JsonProperty("recursivo")
    private Boolean recursivo;

    @JsonProperty("fecha_creacion")
    private OffsetDateTime fechaCreacion;

    @JsonProperty("fecha_actualizacion")
    private OffsetDateTime fechaActualizacion;

    public PermisoCarpetaUsuarioDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCarpetaId() {
        return carpetaId;
    }

    public void setCarpetaId(Long carpetaId) {
        this.carpetaId = carpetaId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public UsuarioResumenDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioResumenDTO usuario) {
        this.usuario = usuario;
    }

    public NivelAccesoDTO getNivelAcceso() {
        return nivelAcceso;
    }

    public void setNivelAcceso(NivelAccesoDTO nivelAcceso) {
        this.nivelAcceso = nivelAcceso;
    }

    public Boolean getRecursivo() {
        return recursivo;
    }

    public void setRecursivo(Boolean recursivo) {
        this.recursivo = recursivo;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(OffsetDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
