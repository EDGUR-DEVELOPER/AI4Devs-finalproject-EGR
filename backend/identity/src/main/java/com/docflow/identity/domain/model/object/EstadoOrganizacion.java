package com.docflow.identity.domain.model.object;

/**
 * Estado de una organización en el sistema.
 */
public enum EstadoOrganizacion {
    /**
     * Organización activa y operacional.
     */
    ACTIVO,

    /**
     * Organización temporalmente suspendida.
     */
    SUSPENDIDO,

    /**
     * Organización archivada (inactiva permanentemente).
     */
    ARCHIVADO
}
