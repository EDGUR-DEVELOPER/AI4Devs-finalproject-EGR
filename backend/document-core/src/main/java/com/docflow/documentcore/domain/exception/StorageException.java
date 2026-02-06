package com.docflow.documentcore.domain.exception;

/**
 * Excepción de operaciones de almacenamiento.
 * 
 * US-DOC-001: Lanzada cuando ocurre un error al guardar, leer o eliminar archivos.
 */
public class StorageException extends RuntimeException {
    
    public StorageException(String message) {
        super(message);
    }
    
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
