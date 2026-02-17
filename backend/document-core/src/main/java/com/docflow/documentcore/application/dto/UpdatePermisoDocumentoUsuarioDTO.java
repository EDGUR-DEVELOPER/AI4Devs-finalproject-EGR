package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

/**
 * DTO para actualizar un permiso explícito de usuario sobre documento.
 */
public class UpdatePermisoDocumentoUsuarioDTO {

    @NotBlank(message = "nivel_acceso_codigo es requerido")
    @JsonProperty("nivel_acceso_codigo")
    private String nivelAccesoCodigo;

    @JsonProperty("fecha_expiracion")
    private OffsetDateTime fechaExpiracion;

    public UpdatePermisoDocumentoUsuarioDTO() {
    }

    public UpdatePermisoDocumentoUsuarioDTO(String nivelAccesoCodigo, OffsetDateTime fechaExpiracion) {
        this.nivelAccesoCodigo = nivelAccesoCodigo;
        this.fechaExpiracion = fechaExpiracion;
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
