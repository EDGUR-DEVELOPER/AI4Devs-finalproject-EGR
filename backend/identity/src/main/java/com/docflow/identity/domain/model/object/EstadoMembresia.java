package com.docflow.identity.domain.model.object;

/**
 * Estado de la membresía de un usuario a una organización.
 */
public enum EstadoMembresia {
    /**
     * Membresía activa y válida.
     */
    ACTIVO,

    /**
     * Membresía temporalmente suspendida.
     */
    SUSPENDIDO,

    /**
     * Membresía desactivada permanentemente por un admin.
     */
    INACTIVO
}
