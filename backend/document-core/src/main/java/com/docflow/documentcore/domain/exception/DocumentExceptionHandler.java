package com.docflow.documentcore.domain.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;

/**
 * Manejador global de excepciones para el controlador de documentos.
 * 
 * US-DOC-001: Convierte excepciones de dominio/aplicación a respuestas HTTP apropiadas.
 */
@Slf4j
@RestControllerAdvice
public class DocumentExceptionHandler {
    
    /**
     * Maneja excepciones de validación de documentos.
     */
    @ExceptionHandler(DocumentValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        DocumentValidationException ex,
        WebRequest request
    ) {
        log.warn("Error de validación: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            ex.getMessage(),
            400,
            OffsetDateTime.now(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
    
    /**
     * Maneja excepciones de almacenamiento.
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(
        StorageException ex,
        WebRequest request
    ) {
        log.error("Error de almacenamiento: {}", ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            "STORAGE_ERROR",
            "Error al guardar el archivo en almacenamiento",
            500,
            OffsetDateTime.now(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }
    
    /**
     * Maneja excepciones de archivo muy grande.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(
        MaxUploadSizeExceededException ex,
        WebRequest request
    ) {
        log.warn("Archivo demasiado grande: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "FILE_TOO_LARGE",
            "El archivo excede el tamaño máximo permitido",
            400,
            OffsetDateTime.now(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(error);
    }
    
    /**
     * Maneja excepciones de contexto de seguridad.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleSecurityContextException(
        IllegalStateException ex,
        WebRequest request
    ) {
        log.error("Error de contexto de seguridad: {}", ex.getMessage());
        
        ErrorResponse error = new ErrorResponse(
            "SECURITY_CONTEXT_ERROR",
            "Error de autenticación o autorización",
            401,
            OffsetDateTime.now(),
            request.getDescription(false).replace("uri=", "")
        );
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(error);
    }
    
    // NOTA: El handler genérico de Exception.class fue removido para evitar conflictos
    // con GlobalExceptionHandler que usa ProblemDetail (RFC 7807).
    // GlobalExceptionHandler maneja todas las excepciones no específicas.
    
    /**
     * DTO de respuesta de error.
     */
    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private int status;
        private OffsetDateTime timestamp;
        private String path;
    }
}
