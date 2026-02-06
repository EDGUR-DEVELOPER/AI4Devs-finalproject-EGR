package com.docflow.documentcore.domain.exception;

/**
 * Excepción de validación de documentos.
 * 
 * US-DOC-001: Lanzada cuando un archivo o documento no cumple las reglas de validación.
 */
public class DocumentValidationException extends RuntimeException {
    
    public DocumentValidationException(String message) {
        super(message);
    }
    
    public DocumentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
