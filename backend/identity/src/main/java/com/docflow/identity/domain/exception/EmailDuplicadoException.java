package com.docflow.identity.domain.exception;

/**
 * Excepción lanzada cuando se intenta registrar un email ya existente en el sistema.
 * Esta excepción se mapea a HTTP 409 Conflict en el GlobalExceptionHandler.
 */
public class EmailDuplicadoException extends RuntimeException {
    
    /**
     * Constructor con mensaje personalizado.
     * 
     * @param mensaje Descripción del error (ej: "Ya existe un usuario con el email: test@docflow.com")
     */
    public EmailDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
