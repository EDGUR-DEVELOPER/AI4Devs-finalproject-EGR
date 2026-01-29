package com.docflow.documentcore.domain.exception;

/**
 * Excepción base para errores de dominio en DocFlow.
 * 
 * <p>Todas las excepciones específicas del dominio deben extender esta clase
 * para permitir manejo centralizado y consistente de errores.</p>
 *
 * @author DocFlow Team
 */
public abstract class DomainException extends RuntimeException {
    
    private final String errorCode;
    
    protected DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
