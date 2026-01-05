package com.docflow.identity.infrastructure.adapters.input.rest;

import com.docflow.identity.domain.exceptions.InvalidCredentialsException;
import com.docflow.identity.domain.exceptions.OrganizacionConfigInvalidaException;
import com.docflow.identity.domain.exceptions.OrganizacionNoEncontradaException;
import com.docflow.identity.domain.exceptions.SinOrganizacionException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para los controladores REST.
 * Transforma excepciones de dominio en respuestas ProblemDetail (RFC 7807).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String ERROR_URI_BASE = "https://docflow.com/errors/";

    /**
     * Maneja InvalidCredentialsException (401 Unauthorized).
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        log.warn("Intento de login fallido desde IP: {}", request.getRemoteAddr());

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage()
        );
        problem.setType(URI.create(ERROR_URI_BASE + "credenciales-invalidas"));
        problem.setTitle("Credenciales Inválidas");
        problem.setProperty("codigo", "CREDENCIALES_INVALIDAS");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Maneja SinOrganizacionException (403 Forbidden).
     */
    @ExceptionHandler(SinOrganizacionException.class)
    public ResponseEntity<ProblemDetail> handleSinOrganizacion(
            SinOrganizacionException ex,
            HttpServletRequest request) {

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
        problem.setType(URI.create(ERROR_URI_BASE + "sin-organizacion"));
        problem.setTitle("Sin Organización");
        problem.setProperty("codigo", "SIN_ORGANIZACION");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Maneja OrganizacionConfigInvalidaException (409 Conflict).
     */
    @ExceptionHandler(OrganizacionConfigInvalidaException.class)
    public ResponseEntity<ProblemDetail> handleConfigInvalida(
            OrganizacionConfigInvalidaException ex,
            HttpServletRequest request) {

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        problem.setType(URI.create(ERROR_URI_BASE + "organizacion-config-invalida"));
        problem.setTitle("Configuración de Organización Inválida");
        problem.setProperty("codigo", "ORGANIZACION_CONFIG_INVALIDA");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Maneja OrganizacionNoEncontradaException (403 Forbidden).
     */
    @ExceptionHandler(OrganizacionNoEncontradaException.class)
    public ResponseEntity<ProblemDetail> handleOrganizacionNoEncontrada(
            OrganizacionNoEncontradaException ex,
            HttpServletRequest request) {

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
        problem.setType(URI.create(ERROR_URI_BASE + "organizacion-no-encontrada"));
        problem.setTitle("Organización No Encontrada");
        problem.setProperty("codigo", "ORGANIZACION_NO_ENCONTRADA");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Maneja errores de validación de Bean Validation (400 Bad Request).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        var errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
            ));

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Error de validación en los datos de entrada"
        );
        problem.setType(URI.create(ERROR_URI_BASE + "validation-error"));
        problem.setTitle("Error de Validación");
        problem.setProperty("codigo", "VALIDATION_ERROR");
        problem.setProperty("errors", errors);
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Maneja excepciones genéricas no capturadas (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Error inesperado en request {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ha ocurrido un error inesperado. Por favor contacte al soporte."
        );
        problem.setType(URI.create(ERROR_URI_BASE + "error-interno"));
        problem.setTitle("Error Interno del Servidor");
        problem.setProperty("codigo", "ERROR_INTERNO");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
