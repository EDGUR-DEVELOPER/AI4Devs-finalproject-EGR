package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para crear un permiso explícito de usuario sobre carpeta.
 */
public class CreatePermisoCarpetaUsuarioDTO {

    @NotNull(message = "usuario_id es requerido")
    @JsonProperty("usuario_id")
    private Long usuarioId;

    @NotBlank(message = "nivel_acceso_codigo es requerido")
    @JsonProperty("nivel_acceso_codigo")
    private String nivelAccesoCodigo;

    @NotNull(message = "recursivo es requerido")
    @JsonProperty("recursivo")
    private Boolean recursivo = false;

    @JsonProperty("comentario_opcional")
    private String comentarioOpcional;

    public CreatePermisoCarpetaUsuarioDTO() {
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

    public Boolean getRecursivo() {
        return recursivo;
    }

    public void setRecursivo(Boolean recursivo) {
        this.recursivo = recursivo;
    }

    public String getComentarioOpcional() {
        return comentarioOpcional;
    }

    public void setComentarioOpcional(String comentarioOpcional) {
        this.comentarioOpcional = comentarioOpcional;
    }
}
