package com.docflow.documentcore.domain.exception;

import com.docflow.documentcore.domain.exception.carpeta.CarpetaNombreDuplicadoException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNoVaciaException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaRaizNoEliminableException;
import com.docflow.documentcore.domain.exception.carpeta.SinPermisoCarpetaException;
import com.docflow.documentcore.domain.exception.permiso.PermisoCarpetaDuplicadoException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

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
 * IMPORTANTE: Este handler debe ser cargado ANTES que cualquier otro en pruebas
 * unitarias. Los handlers más específicos (carpeta, documento, etc.) se evalúan
 * antes del handler genérico de Exception.
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

    /**
     * Maneja AclNotFoundException y retorna HTTP 404.
     *
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 404
     */
    @ExceptionHandler(AclNotFoundException.class)
    public ProblemDetail handleAclNotFound(AclNotFoundException ex) {
        log.debug("ACL no encontrado: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );

        problem.setTitle("Permiso de Carpeta No Encontrado");
        problem.setType(URI.create("https://docflow.com/errors/acl-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "ACL_NOT_FOUND");

        return problem;
    }

    /**
     * Maneja VersionNotBelongToDocumentException y retorna HTTP 400.
     * 
     * <p>Esta excepción se lanza cuando se intenta realizar una operación
     * (como rollback) con una versión que no pertenece al documento especificado.</p>
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 400
     */
    @ExceptionHandler(VersionNotBelongToDocumentException.class)
    public ProblemDetail handleVersionNotBelongToDocument(VersionNotBelongToDocumentException ex) {
        log.warn("Intento de operación con versión no válida: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "La versión solicitada no pertenece al documento"
        );
        
        problem.setTitle("Versión No Válida");
        problem.setType(URI.create("https://docflow.com/errors/invalid-version"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "VERSION_NOT_BELONG_TO_DOCUMENT");
        problem.setProperty("versionId", ex.getVersionId());
        problem.setProperty("documentoId", ex.getDocumentoId());
        
        return problem;
    }

    /**
     * Maneja InsufficientPermissionsException y retorna HTTP 403.
     * 
     * <p>Esta excepción se lanza cuando un usuario intenta realizar una operación
     * sin el nivel de permiso necesario (ej. intentar rollback sin permiso ADMINISTRACION).</p>
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 403
     */
    @ExceptionHandler(InsufficientPermissionsException.class)
    public ProblemDetail handleInsufficientPermissions(InsufficientPermissionsException ex) {
        log.warn("Usuario intenta operación sin permisos suficientes: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
        
        problem.setTitle("Permisos Insuficientes");
        problem.setType(URI.create("https://docflow.com/errors/insufficient-permissions"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "INSUFFICIENT_PERMISSIONS");
        
        if (ex.getNivelRequerido() != null) {
            problem.setProperty("nivelRequerido", ex.getNivelRequerido());
        }
        if (ex.getTipoRecurso() != null) {
            problem.setProperty("tipoRecurso", ex.getTipoRecurso());
        }
        
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
     * Maneja PermisoCarpetaDuplicadoException y retorna HTTP 409.
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 409
     */
    @ExceptionHandler(PermisoCarpetaDuplicadoException.class)
    public ProblemDetail handlePermisoCarpetaDuplicado(PermisoCarpetaDuplicadoException ex) {
        log.debug("Permiso de carpeta duplicado: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, 
            ex.getMessage()
        );
        
        problem.setTitle("Permiso de Carpeta Duplicado");
        problem.setType(URI.create("https://docflow.com/errors/acl-duplicate"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", ex.getErrorCode());
        
        return problem;
    }

    /**
     * Maneja MismaUbicacionException y retorna HTTP 400.
     * 
     * Indica que se intentó mover un documento a la carpeta donde ya está.
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 400
     */
    @ExceptionHandler(MismaUbicacionException.class)
    public ProblemDetail handleMismaUbicacion(MismaUbicacionException ex) {
        log.debug("Move to same location attempted: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        
        problem.setTitle("Operación Inválida");
        problem.setType(URI.create("https://docflow.com/errors/misma-ubicacion"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "MISMA_UBICACION");
        
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
     * Maneja CarpetaNoVaciaException y retorna HTTP 409.
     *
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 409
     */
    @ExceptionHandler(CarpetaNoVaciaException.class)
    public ProblemDetail handleCarpetaNoVacia(CarpetaNoVaciaException ex) {
        log.debug("Carpeta no vacía: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "La carpeta debe vaciarse antes de eliminarla"
        );

        problem.setTitle("Carpeta No Vacía");
        problem.setType(URI.create("https://docflow.com/errors/carpeta-not-empty"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("carpetaId", ex.getCarpetaId());
        problem.setProperty("subcarpetasActivas", ex.getSubcarpetasActivas());
        problem.setProperty("documentosActivos", ex.getDocumentosActivos());

        return problem;
    }

    /**
     * Maneja CarpetaRaizNoEliminableException y retorna HTTP 400.
     *
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 400
     */
    @ExceptionHandler(CarpetaRaizNoEliminableException.class)
    public ProblemDetail handleCarpetaRaizNoEliminable(CarpetaRaizNoEliminableException ex) {
        log.debug("Intento de eliminar carpeta raíz: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );

        problem.setTitle("Carpeta Raíz No Eliminable");
        problem.setType(URI.create("https://docflow.com/errors/carpeta-root-not-deletable"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("carpetaId", ex.getCarpetaId());

        return problem;
    }

    /**
     * Maneja DocumentValidationException y retorna HTTP 400.
     * Consolidado de DocumentExceptionHandler.
     *
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 400
     */
    @ExceptionHandler(DocumentValidationException.class)
    public ProblemDetail handleDocumentValidationException(DocumentValidationException ex) {
        log.warn("Error de validación de documento: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );

        problem.setTitle("Error de Validación");
        problem.setType(URI.create("https://docflow.com/errors/document-validation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "VALIDATION_ERROR");

        return problem;
    }

    /**
     * Maneja StorageException y retorna HTTP 500.
     * Consolidado de DocumentExceptionHandler.
     *
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 500
     */
    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageException(StorageException ex) {
        log.error("Error de almacenamiento: {}", ex.getMessage(), ex);

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error al guardar el archivo en almacenamiento"
        );

        problem.setTitle("Error de Almacenamiento");
        problem.setType(URI.create("https://docflow.com/errors/storage-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "STORAGE_ERROR");

        return problem;
    }

    /**
     * Maneja MaxUploadSizeExceededException y retorna HTTP 413.
     * Consolidado de DocumentExceptionHandler.
     *
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 413
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.warn("Archivo demasiado grande: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "El archivo excede el tamaño máximo permitido"
        );

        problem.setTitle("Archivo Muy Grande");
        problem.setType(URI.create("https://docflow.com/errors/file-too-large"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "FILE_TOO_LARGE");

        return problem;
    }

    /**
     * Maneja CarpetaRaizNoEncontradaException y retorna HTTP 404.
     * 
     * Indica que no existe carpeta raíz para una organización.
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 404
     */
    @ExceptionHandler(CarpetaRaizNoEncontradaException.class)
    public ProblemDetail handleCarpetaRaizNoEncontrada(CarpetaRaizNoEncontradaException ex) {
        log.debug("Carpeta raíz no encontrada: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        
        problem.setTitle("Carpeta Raíz No Encontrada");
        problem.setType(URI.create("https://docflow.com/errors/carpeta-raiz-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "CARPETA_RAIZ_NO_ENCONTRADA");
        
        return problem;
    }

    /**
     * Maneja AccessDeniedException y retorna HTTP 403.
     * 
     * Indica acceso denegado a un recurso.
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
        
        problem.setTitle("Acceso Denegado");
        problem.setType(URI.create("https://docflow.com/errors/access-denied"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", ex.getErrorCode());
        
        return problem;
    }

    /**
     * Maneja ResponseStatusException y retorna el status HTTP correspondiente.
     * 
     * Esta excepción es comúnmente lanzada por guards y validadores
     * para indicar errores de autenticación (401) o autorización (403).
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con el status y mensaje de la excepción
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        
        log.warn("ResponseStatusException capturada: {} - {}", status, ex.getReason());
        
        var problem = ProblemDetail.forStatusAndDetail(
            status,
            ex.getReason() != null ? ex.getReason() : "Error en la solicitud"
        );
        
        // Definir título y tipo basado en el status code
        switch (status) {
            case UNAUTHORIZED -> {
                problem.setTitle("No Autenticado");
                problem.setType(URI.create("https://docflow.com/errors/unauthorized"));
                problem.setProperty("errorCode", "UNAUTHORIZED");
            }
            case FORBIDDEN -> {
                problem.setTitle("Acceso Prohibido");
                problem.setType(URI.create("https://docflow.com/errors/forbidden"));
                problem.setProperty("errorCode", "FORBIDDEN");
            }
            case NOT_FOUND -> {
                problem.setTitle("Recurso No Encontrado");
                problem.setType(URI.create("https://docflow.com/errors/not-found"));
                problem.setProperty("errorCode", "NOT_FOUND");
            }
            case BAD_REQUEST -> {
                problem.setTitle("Solicitud Inválida");
                problem.setType(URI.create("https://docflow.com/errors/bad-request"));
                problem.setProperty("errorCode", "BAD_REQUEST");
            }
            default -> {
                problem.setTitle("Error en la Solicitud");
                problem.setType(URI.create("https://docflow.com/errors/request-error"));
                problem.setProperty("errorCode", status.name());
            }
        }
        
        problem.setProperty("timestamp", Instant.now());
        
        return problem;
    }
    
    /**
     * Maneja MissingRequestHeaderException y retorna HTTP 400.
     * 
     * Captura cuando un header requerido no está presente en la solicitud.
     * Típicamente ocurre con headers obligatorios como 'X-User-Id'.
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 400
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingRequestHeader(MissingRequestHeaderException ex) {
        String headerName = ex.getHeaderName();
        log.warn("Header requerido ausente: {}", headerName);
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            String.format("Header requerido '%s' no está presente en la solicitud", headerName)
        );
        
        problem.setTitle("Header Requerido Ausente");
        problem.setType(URI.create("https://docflow.com/errors/missing-request-header"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "MISSING_REQUEST_HEADER");
        problem.setProperty("headerName", headerName);
        
        return problem;
    }

    /**
     * Maneja ConstraintViolationException y retorna HTTP 400.
     * 
     * Captura violaciones de validación Bean Validation en parámetros de request
     * (ej: @Min, @Max, @NotNull en parámetros de método del controller).
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 400
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String violationsMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        
        log.warn("Violación de validación: {}", violationsMessage);
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            violationsMessage
        );
        
        problem.setTitle("Error de Validación");
        problem.setType(URI.create("https://docflow.com/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "VALIDATION_ERROR");
        
        return problem;
    }
    
    /**
     * Maneja MethodArgumentNotValidException y retorna HTTP 400.
     * 
     * Captura violaciones de validación Bean Validation en objetos @RequestBody
     * con @Valid (ej: @NotNull, @Positive en campos de DTOs).
     * 
     * @param ex la excepción lanzada
     * @return ProblemDetail con status 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String violationsMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        
        log.warn("Error de validación en request body: {}", violationsMessage);
        
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            violationsMessage
        );
        
        problem.setTitle("Error de Validación");
        problem.setType(URI.create("https://docflow.com/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", "VALIDATION_ERROR");
        
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
