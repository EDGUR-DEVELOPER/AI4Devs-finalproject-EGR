package com.docflow.documentcore.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para lectura de usuarios.
 * 
 * <p>Solo incluye los campos necesarios para resolver ACL de carpetas.</p>
 */
@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    public UsuarioEntity() {
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

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }
}
