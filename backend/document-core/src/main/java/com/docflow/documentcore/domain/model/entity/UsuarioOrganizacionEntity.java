package com.docflow.documentcore.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

/**
 * Entidad JPA para la relación usuario-organización.
 */
@Entity
@Table(name = "usuarios_organizaciones")
@IdClass(UsuarioOrganizacionId.class)
public class UsuarioOrganizacionEntity {

    @Id
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Id
    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;

    @Column(name = "estado", nullable = false)
    private String estado;

    public UsuarioOrganizacionEntity() {
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getOrganizacionId() {
        return organizacionId;
    }

    public void setOrganizacionId(Long organizacionId) {
        this.organizacionId = organizacionId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
