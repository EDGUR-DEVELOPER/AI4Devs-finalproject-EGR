package com.docflow.vault.infrastructure.adapters.input.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for REST controllers.
 * <p>
 * Provides centralized exception handling for the Vault Integration Service.
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles SecretNotFoundException and returns a 404 response.
     *
     * @param ex the exception
     * @return ResponseEntity with error details and 404 status
     */
    @ExceptionHandler(SecretNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSecretNotFound(SecretNotFoundException ex) {
        log.warn("Secret not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "Secret not found",
                        "path", ex.getPath()
                ));
    }

    /**
     * Handles generic exceptions and returns a 500 response.
     *
     * @param ex the exception
     * @return ResponseEntity with error details and 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal server error",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                ));
    }
}
