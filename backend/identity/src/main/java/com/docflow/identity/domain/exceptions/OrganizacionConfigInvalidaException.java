package com.docflow.identity.domain.exceptions;

/**
 * Excepción lanzada cuando la configuración de organizaciones de un usuario es inválida.
 * Casos: múltiples organizaciones sin predeterminada, o múltiples predeterminadas (error de datos).
 * 
 * <p>HTTP Status: 409 Conflict</p>
 */
public class OrganizacionConfigInvalidaException extends RuntimeException {
    
    public OrganizacionConfigInvalidaException(String message) {
        super(message);
    }
}
