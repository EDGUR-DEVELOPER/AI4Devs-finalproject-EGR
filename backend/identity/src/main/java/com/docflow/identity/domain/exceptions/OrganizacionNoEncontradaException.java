package com.docflow.identity.domain.exceptions;

/**
 * Excepción lanzada cuando se intenta acceder a una organización a la que el usuario no pertenece
 * o cuya membresía está inactiva.
 * 
 * <p>HTTP Status: 403 Forbidden</p>
 */
public class OrganizacionNoEncontradaException extends RuntimeException {
    
    public OrganizacionNoEncontradaException(String message) {
        super(message);
    }
}
