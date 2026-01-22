package com.docflow.identity.domain.model;

/**
 * Estados posibles de un usuario en el sistema.
 * Independiente del soft delete (fecha_eliminacion).
 */
public enum EstadoUsuario {
    /**
     * Usuario activo, puede autenticarse y operar normalmente
     */
    ACTIVO,
    
    /**
     * Usuario desactivado administrativamente.
     * Los tokens existentes serán rechazados al validarse contra BD.
     */
    INACTIVO
}
