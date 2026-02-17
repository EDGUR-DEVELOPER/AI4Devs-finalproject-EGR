package com.docflow.documentcore.domain.model.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Identificador compuesto para UsuarioOrganizacionEntity.
 */
public class UsuarioOrganizacionId implements Serializable {

    private Long usuarioId;
    private Long organizacionId;

    public UsuarioOrganizacionId() {
    }

    public UsuarioOrganizacionId(Long usuarioId, Long organizacionId) {
        this.usuarioId = usuarioId;
        this.organizacionId = organizacionId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioOrganizacionId that = (UsuarioOrganizacionId) o;
        return Objects.equals(usuarioId, that.usuarioId) &&
               Objects.equals(organizacionId, that.organizacionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, organizacionId);
    }
}
