package com.docflow.documentcore.domain.exception;

import com.docflow.documentcore.domain.exception.carpeta.CarpetaNombreDuplicadoException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.SinPermisoCarpetaException;

// import com.docflow.documentcore.domain.exceptions.TenantContextMissingException; // TODO: Implementar en US-AUTH-004
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Manejador global de excepciones para el servicio de document-core.
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

    /**
     * Maneja ResourceNotFoundException y retorna HTTP 404.
     * 
     * SEGURIDAD: No revela si el recurso existe en otra organización.
     * Mismo comportamiento para "no existe" vs "existe pero es de otra org".
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.debug("Recurso no encontrado: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, 
            ex.getMessage()
        );
        
        problem.setTitle("Recurso No Encontrado");
        problem.setType(URI.create("https://docflow.com/errors/resource-not-found"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }

    // TODO: US-AUTH-004 - Implementar cuando TenantContextMissingException esté disponible
    /**
     * Maneja TenantContextMissingException y retorna HTTP 401.
     * 
     * Indica que la petición no tiene contexto de organización válido.
     * Puede significar:
     * - Token JWT ausente o inválido
     * - Header X-Organization-Id ausente (problema de configuración del Gateway)
     * - Endpoint protegido accedido sin autenticación
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 401
     */
    /* 
    @ExceptionHandler(TenantContextMissingException.class)
    public ProblemDetail handleTenantContextMissing(TenantContextMissingException ex) {
        log.warn("Contexto tenant ausente: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED, 
            "No se pudo determinar la organización. Autentíquese e intente nuevamente."
        );
        
        problem.setTitle("Contexto de Organización Ausente");
        problem.setType(URI.create("https://docflow.com/errors/tenant-context-missing"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("hint", "Verifique que el token JWT sea válido y contenga el claim 'org_id'");
        
        return problem;
    }
    */

    /**
     * Maneja CarpetaNotFoundException y retorna HTTP 404.
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 404
     */
    @ExceptionHandler(CarpetaNotFoundException.class)
    public ProblemDetail handleCarpetaNotFound(CarpetaNotFoundException ex) {
        log.debug("Carpeta no encontrada: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, 
            ex.getMessage()
        );
        
        problem.setTitle("Carpeta No Encontrada");
        problem.setType(URI.create("https://docflow.com/errors/carpeta-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", ex.getErrorCode());
        
        return problem;
    }
    
    /**
     * Maneja CarpetaNombreDuplicadoException y retorna HTTP 409.
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 409
     */
    @ExceptionHandler(CarpetaNombreDuplicadoException.class)
    public ProblemDetail handleCarpetaNombreDuplicado(CarpetaNombreDuplicadoException ex) {
        log.debug("Nombre de carpeta duplicado: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, 
            ex.getMessage()
        );
        
        problem.setTitle("Nombre de Carpeta Duplicado");
        problem.setType(URI.create("https://docflow.com/errors/carpeta-duplicate-name"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", ex.getErrorCode());
        
        return problem;
    }
    
    /**
     * Maneja SinPermisoCarpetaException y retorna HTTP 403.
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 403
     */
    @ExceptionHandler(SinPermisoCarpetaException.class)
    public ProblemDetail handleSinPermisoCarpeta(SinPermisoCarpetaException ex) {
        log.warn("Acceso denegado a carpeta: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN, 
            ex.getMessage()
        );
        
        problem.setTitle("Permisos Insuficientes");
        problem.setType(URI.create("https://docflow.com/errors/carpeta-permission-denied"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", ex.getErrorCode());
        
        return problem;
    }
    
    /**
     * Maneja excepciones genéricas no capturadas.
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 500
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Error no manejado: ", ex);
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "Ha ocurrido un error interno. Por favor contacte al administrador."
        );
        
        problem.setTitle("Error Interno del Servidor");
        problem.setType(URI.create("https://docflow.com/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
}
