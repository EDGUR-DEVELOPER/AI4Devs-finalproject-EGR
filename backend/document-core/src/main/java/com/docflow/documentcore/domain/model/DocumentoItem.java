package com.docflow.documentcore.domain.model;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Value Object que representa un documento dentro del listado de contenido.
 */
public record DocumentoItem(
    Long id,
    String nombre,
    String extension,
    Long tamanioBytes,
    Long versionActualId,
    OffsetDateTime fechaCreacion,
    OffsetDateTime fechaActualizacion,
    UsuarioResumen creadoPor,
    CapacidadesUsuario capacidades
) implements Serializable {

    public DocumentoItem {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id debe ser mayor que 0");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre no puede ser nulo o vacío");
        }
        if (tamanioBytes == null || tamanioBytes < 0) {
            throw new IllegalArgumentException("tamanioBytes no puede ser negativo");
        }
        if (fechaCreacion == null) {
            throw new IllegalArgumentException("fechaCreacion no puede ser nulo");
        }
        if (creadoPor == null) {
            throw new IllegalArgumentException("creadoPor no puede ser nulo");
        }
        if (capacidades == null) {
            throw new IllegalArgumentException("capacidades no puede ser nulo");
        }
    }
}
