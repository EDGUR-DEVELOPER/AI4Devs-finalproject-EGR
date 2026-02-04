package com.docflow.documentcore.domain.exception;

/**
 * Excepción lanzada cuando un usuario no tiene permisos para realizar una operación.
 * 
 * <p><strong>HTTP Status Code:</strong> 403 FORBIDDEN</p>
 */
public class AccessDeniedException extends DomainException {
    
    private static final String ERROR_CODE = "ACCESS_DENIED";
    
    public AccessDeniedException(String mensaje) {
        super(mensaje, ERROR_CODE);
    }
    
    public AccessDeniedException(String mensaje, String errorCode) {
        super(mensaje, errorCode);
    }
}
