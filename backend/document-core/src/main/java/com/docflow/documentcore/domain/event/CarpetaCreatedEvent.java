package com.docflow.documentcore.domain.event;

import java.time.Instant;

/**
 * Evento de dominio emitido cuando se crea una nueva carpeta.
 * 
 * <p>Este evento es consumido por el sistema de auditoría para registrar
 * todas las operaciones de creación de carpetas.</p>
 *
 * <p>Implementado como Java record para inmutabilidad y sintaxis concisa.</p>
 *
 * @author DocFlow Team
 */
public record CarpetaCreatedEvent(
        Long carpetaId,
        Long organizacionId,
        Long usuarioId,
        String nombre,
        Long carpetaPadreId,
        Instant timestamp
) {
    
    public CarpetaCreatedEvent {
        if (carpetaId == null) {
            throw new IllegalArgumentException("carpetaId no puede ser nulo");
        }
        if (organizacionId == null) {
            throw new IllegalArgumentException("organizacionId no puede ser nulo");
        }
        if (usuarioId == null) {
            throw new IllegalArgumentException("usuarioId no puede ser nulo");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
