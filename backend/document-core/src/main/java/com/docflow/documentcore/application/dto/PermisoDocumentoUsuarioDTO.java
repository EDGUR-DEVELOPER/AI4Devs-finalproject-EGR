package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta para permisos explícitos de documento.
 */
public class PermisoDocumentoUsuarioDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("documento_id")
    private Long documentoId;

    @JsonProperty("usuario_id")
    private Long usuarioId;

    @JsonProperty("usuario")
    private UsuarioResumenDTO usuario;

    @JsonProperty("nivel_acceso")
    private NivelAccesoDTO nivelAcceso;

    @JsonProperty("fecha_expiracion")
    private OffsetDateTime fechaExpiracion;

    @JsonProperty("fecha_asignacion")
    private OffsetDateTime fechaAsignacion;

    public PermisoDocumentoUsuarioDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentoId() {
        return documentoId;
    }

    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
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

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(OffsetDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public OffsetDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(OffsetDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }
}
