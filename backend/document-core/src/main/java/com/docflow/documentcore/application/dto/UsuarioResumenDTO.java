package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO embebido de usuario para respuestas ACL.
 */
public class UsuarioResumenDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("nombre")
    private String nombre;

    public UsuarioResumenDTO() {
    }

    public UsuarioResumenDTO(Long id, String email, String nombre) {
        this.id = id;
        this.email = email;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
