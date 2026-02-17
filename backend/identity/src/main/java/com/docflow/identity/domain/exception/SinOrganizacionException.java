package com.docflow.identity.domain.exception;

/**
 * Excepción lanzada cuando un usuario no tiene organizaciones activas.
 * 
 * <p>HTTP Status: 403 Forbidden</p>
 */
public class SinOrganizacionException extends RuntimeException {
    
    public SinOrganizacionException(String message) {
        super(message);
    }
}
