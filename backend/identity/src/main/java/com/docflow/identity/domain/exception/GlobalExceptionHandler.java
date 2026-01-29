package com.docflow.identity.domain.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Manejador global de excepciones para el servicio de identidad.
 * 
 * Implementa el estándar RFC 7807 (Problem Details for HTTP APIs) usando
 * Spring Boot 3's ProblemDetail.
 * 
 * Maneja específicamente excepciones relacionadas con aislamiento multi-tenant:
 * - ResourceNotFoundException → 404 (security by obscurity)
 * - TenantContextMissingException → 401 (no autenticado o sin contexto)
 * 
 * Parte de US-AUTH-004: Aislamiento de datos por organización.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String ERROR_URI_BASE = "urn:docflow:error:";

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
                ex.getMessage());
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
                ex.getMessage());
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
                ex.getMessage());
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
                ex.getMessage());
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
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"));

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Error de validación en los datos de entrada");
        problem.setType(URI.create(ERROR_URI_BASE + "validation-error"));
        problem.setTitle("Error de Validación");
        problem.setProperty("codigo", "VALIDATION_ERROR");
        problem.setProperty("errors", errors);
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Maneja intentos de creación de usuarios con emails duplicados (409 Conflict).
     * 
     * Esta excepción se lanza cuando se intenta registrar un email ya existente en
     * el sistema.
     * Retorna HTTP 409 Conflict con formato ProblemDetail (RFC 7807).
     * 
     * Parte de US-ADMIN-001: Crear usuario (API) dentro de la organización.
     */
    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ProblemDetail> handleEmailDuplicado(
            EmailDuplicadoException ex,
            HttpServletRequest request) {

        log.warn("Email duplicado en creación de usuario: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage());
        problem.setType(URI.create(ERROR_URI_BASE + "email-duplicado"));
        problem.setTitle("Email Duplicado");
        problem.setProperty("codigo", "EMAIL_DUPLICADO");
        problem.setProperty("categoria", "VALIDATION");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Maneja intentos de operaciones administrativas sin permisos suficientes (403
     * Forbidden).
     * 
     * Esta excepción se lanza cuando un usuario intenta realizar una operación que
     * requiere
     * un rol específico (ej. ADMIN) y no lo posee.
     * Retorna HTTP 403 Forbidden con formato ProblemDetail (RFC 7807).
     * 
     * Parte de US-ADMIN-001: Crear usuario (API) dentro de la organización.
     */
    @ExceptionHandler(PermisoInsuficienteException.class)
    public ResponseEntity<ProblemDetail> handlePermisoInsuficiente(
            PermisoInsuficienteException ex,
            HttpServletRequest request) {

        log.warn("Acceso denegado por permisos insuficientes: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage());
        problem.setType(URI.create(ERROR_URI_BASE + "permiso-insuficiente"));
        problem.setTitle("Permisos Insuficientes");
        problem.setProperty("codigo", "PERMISO_INSUFICIENTE");
        problem.setProperty("categoria", "AUTHORIZATION");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Maneja AutoDeactivationNotAllowedException (400 Bad Request).
     * Se lanza cuando un administrador intenta desactivarse a sí mismo.
     */
    @ExceptionHandler(AutoDeactivationNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleAutoDeactivationNotAllowed(
            AutoDeactivationNotAllowedException ex,
            HttpServletRequest request) {

        log.warn("Intento de auto-desactivación bloqueado: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        problem.setType(URI.create(ERROR_URI_BASE + "auto-deactivation-not-allowed"));
        problem.setTitle("Auto-desactivación no permitida");
        problem.setProperty("codigo", "AUTO_DEACTIVACION_NO_PERMITIDA");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.badRequest().body(problem);
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
                "Ha ocurrido un error inesperado. Por favor contacte al soporte.");
        problem.setType(URI.create(ERROR_URI_BASE + "error-interno"));
        problem.setTitle("Error Interno del Servidor");
        problem.setProperty("codigo", "ERROR_INTERNO");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

}
