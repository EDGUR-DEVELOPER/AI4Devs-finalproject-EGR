package com.docflow.documentcore.domain.model;

import java.io.Serializable;

/**
 * Value Object que representa un usuario de forma resumida.
 */
public record UsuarioResumen(
    Long id,
    String nombreCompleto
) implements Serializable {

    public UsuarioResumen {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id debe ser mayor que 0");
        }
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new IllegalArgumentException("nombreCompleto no puede ser nulo o vacío");
        }
    }
}
