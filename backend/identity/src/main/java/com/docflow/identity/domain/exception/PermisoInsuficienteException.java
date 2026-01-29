package com.docflow.identity.domain.exception;

/**
 * Excepción lanzada cuando un usuario intenta realizar una operación para la cual no tiene permisos.
 * Esta excepción se mapea a HTTP 403 Forbidden en el GlobalExceptionHandler.
 */
public class PermisoInsuficienteException extends RuntimeException {
    
    /**
     * Constructor con mensaje personalizado.
     * 
     * @param mensaje Descripción del error (ej: "Se requiere rol ADMIN para esta operación")
     */
    public PermisoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
