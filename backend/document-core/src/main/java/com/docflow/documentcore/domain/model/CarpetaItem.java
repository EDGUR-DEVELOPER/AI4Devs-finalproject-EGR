package com.docflow.documentcore.domain.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Value Object que representa una carpeta dentro del listado de contenido.
 */
public record CarpetaItem(
    Long id,
    String nombre,
    String descripcion,
    Instant fechaCreacion,
    Instant fechaActualizacion,
    int numSubcarpetas,
    int numDocumentos,
    CapacidadesUsuario capacidades
) implements Serializable {

    public CarpetaItem {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id debe ser mayor que 0");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre no puede ser nulo o vacío");
        }
        if (fechaCreacion == null) {
            throw new IllegalArgumentException("fechaCreacion no puede ser nulo");
        }
        if (capacidades == null) {
            throw new IllegalArgumentException("capacidades no puede ser nulo");
        }
    }
}
