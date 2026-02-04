package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * DTO para crear un permiso explícito de usuario sobre documento.
 * 
 * Soporta fecha de expiración para accesos temporales.
 */
public class CreatePermisoDocumentoUsuarioDTO {

    @NotNull(message = "usuario_id es requerido")
    @JsonProperty("usuario_id")
    private Long usuarioId;

    @NotBlank(message = "nivel_acceso_codigo es requerido")
    @JsonProperty("nivel_acceso_codigo")
    private String nivelAccesoCodigo;

    @JsonProperty("fecha_expiracion")
    private OffsetDateTime fechaExpiracion;

    public CreatePermisoDocumentoUsuarioDTO() {
    }

    public CreatePermisoDocumentoUsuarioDTO(Long usuarioId, String nivelAccesoCodigo, OffsetDateTime fechaExpiracion) {
        this.usuarioId = usuarioId;
        this.nivelAccesoCodigo = nivelAccesoCodigo;
        this.fechaExpiracion = fechaExpiracion;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNivelAccesoCodigo() {
        return nivelAccesoCodigo;
    }

    public void setNivelAccesoCodigo(String nivelAccesoCodigo) {
        this.nivelAccesoCodigo = nivelAccesoCodigo;
    }

    public OffsetDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(OffsetDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}
