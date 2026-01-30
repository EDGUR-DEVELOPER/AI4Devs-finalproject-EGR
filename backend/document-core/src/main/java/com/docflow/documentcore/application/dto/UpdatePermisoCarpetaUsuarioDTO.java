package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para actualización parcial de un permiso de carpeta.
 */
public class UpdatePermisoCarpetaUsuarioDTO {

    @NotBlank(message = "nivel_acceso_codigo es requerido")
    @JsonProperty("nivel_acceso_codigo")
    private String nivelAccesoCodigo;

    @JsonProperty("recursivo")
    private Boolean recursivo;

    public UpdatePermisoCarpetaUsuarioDTO() {
    }

    public String getNivelAccesoCodigo() {
        return nivelAccesoCodigo;
    }

    public void setNivelAccesoCodigo(String nivelAccesoCodigo) {
        this.nivelAccesoCodigo = nivelAccesoCodigo;
    }

    public Boolean getRecursivo() {
        return recursivo;
    }

    public void setRecursivo(Boolean recursivo) {
        this.recursivo = recursivo;
    }
}
