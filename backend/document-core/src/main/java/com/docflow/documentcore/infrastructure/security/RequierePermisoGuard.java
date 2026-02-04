package com.docflow.documentcore.infrastructure.security;

import com.docflow.documentcore.application.service.EvaluadorPermisosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AOP Aspect that intercepts methods annotated with {@link RequierePermiso}
 * and enforces permission checks before method execution.
 * 
 * <h3>How It Works</h3>
 * <ol>
 *   <li>Intercepts method calls with {@code @RequierePermiso}</li>
 *   <li>Extracts user ID and organization ID from HTTP headers</li>
 *   <li>Extracts resource ID from method parameters (specified by {@link RequierePermiso#paramIndex()})</li>
 *   <li>Calls {@link EvaluadorPermisosService#tieneAcceso} to check permission</li>
 *   <li>Throws {@code ResponseStatusException(403)} if permission is denied</li>
 *   <li>Proceeds with method execution if permission is granted</li>
 * </ol>
 * 
 * <h3>Header Requirements</h3>
 * <p>The guard expects the following HTTP headers (set by authentication middleware):</p>
 * <ul>
 *   <li><strong>X-User-Id</strong>: ID of the authenticated user</li>
 *   <li><strong>X-Organization-Id</strong>: ID of the user's organization (tenant)</li>
 * </ul>
 * 
 * <h3>Error Handling</h3>
 * <p>Permission denials are logged at WARN level for security auditing.
 * Successful checks are logged at DEBUG level to avoid log noise in production.</p>
 * 
 * <h3>Example Usage</h3>
 * <pre>{@code
 * @GetMapping("/{documentoId}")
 * @RequierePermiso(
 *     tipoRecurso = TipoRecurso.DOCUMENTO,
 *     nivelRequerido = NivelAcceso.LECTURA,
 *     paramIndex = 0
 * )
 * public ResponseEntity<DocumentoDTO> getDocumento(@PathVariable Long documentoId) {
 *     // This line only executes if permission check passes
 *     return documentoService.getById(documentoId);
 * }
 * }</pre>
 * 
 * @see RequierePermiso
 * @see EvaluadorPermisosService
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequierePermisoGuard {

    private final EvaluadorPermisosService evaluadorPermisos;
    
    /**
     * Advice that wraps methods annotated with @RequierePermiso.
     * 
     * @param joinPoint the join point representing the intercepted method
     * @param requierePermiso the annotation instance with permission requirements
     * @return the result of the proceeded method call
     * @throws Throwable if the method throws an exception or permission is denied
     */
    @Around("@annotation(requierePermiso)")
    public Object verificarPermiso(
            ProceedingJoinPoint joinPoint,
            RequierePermiso requierePermiso
    ) throws Throwable {
        
        String methodName = joinPoint.getSignature().toShortString();
        
        try {
            // Extract context information from HTTP headers
            Long usuarioId = getUserId();
            Long organizacionId = getOrganizacionId();
            
            // Extract resource ID from method parameters
            Long recursoId = extraerRecursoId(joinPoint, requierePermiso.paramIndex());
            
            log.debug("Permission check initiated: method={}, user={}, org={}, resource={}, type={}, required={}",
                    methodName, usuarioId, organizacionId, recursoId,
                    requierePermiso.tipoRecurso(), requierePermiso.nivelRequerido());
            
            // Check permission using evaluator service
            boolean tieneAcceso = evaluadorPermisos.tieneAcceso(
                    usuarioId,
                    recursoId,
                    requierePermiso.tipoRecurso(),
                    requierePermiso.nivelRequerido(),
                    organizacionId
            );
            
            if (!tieneAcceso) {
                log.warn("Permission denied: method={}, user={}, org={}, resource={}, type={}, required={}",
                        methodName, usuarioId, organizacionId, recursoId,
                        requierePermiso.tipoRecurso(), requierePermiso.nivelRequerido());
                
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        requierePermiso.errorMessage()
                );
            }
            
            log.debug("Permission granted: method={}, user={}, resource={}",
                    methodName, usuarioId, recursoId);
            
            // Proceed with the original method execution
            return joinPoint.proceed();
            
        } catch (ResponseStatusException e) {
            // Re-throw permission exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error during permission check for method={}: {}", methodName, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error checking permissions"
            );
        }
    }
    
    /**
     * Extracts the resource ID from the method's parameters.
     * 
     * @param joinPoint the join point with method parameters
     * @param paramIndex the index of the parameter containing the resource ID
     * @return the resource ID as Long
     * @throws IllegalArgumentException if parameter index is invalid or parameter is not a Long
     */
    private Long extraerRecursoId(ProceedingJoinPoint joinPoint, int paramIndex) {
        Object[] args = joinPoint.getArgs();
        
        if (paramIndex < 0 || paramIndex >= args.length) {
            throw new IllegalArgumentException(
                    String.format("Invalid paramIndex %d for method %s with %d parameters",
                            paramIndex, joinPoint.getSignature().getName(), args.length)
            );
        }
        
        Object param = args[paramIndex];
        
        if (!(param instanceof Long)) {
            throw new IllegalArgumentException(
                    String.format("Parameter at index %d must be Long, but was %s",
                            paramIndex, param != null ? param.getClass().getSimpleName() : "null")
            );
        }
        
        return (Long) param;
    }
    
    /**
     * Extracts user ID from HTTP request headers.
     * 
     * @return user ID from X-User-Id header
     * @throws ResponseStatusException if header is missing
     */
    private Long getUserId() {
        HttpServletRequest request = getCurrentRequest();
        String userIdHeader = request.getHeader("X-User-Id");
        
        if (userIdHeader == null || userIdHeader.isBlank()) {
            log.error("Missing X-User-Id header in request");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User authentication required"
            );
        }
        
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            log.error("Invalid X-User-Id header value: {}", userIdHeader);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid user ID format"
            );
        }
    }
    
    /**
     * Extracts organization ID from HTTP request headers.
     * 
     * @return organization ID from X-Organization-Id header
     * @throws ResponseStatusException if header is missing
     */
    private Long getOrganizacionId() {
        HttpServletRequest request = getCurrentRequest();
        String orgIdHeader = request.getHeader("X-Organization-Id");
        
        if (orgIdHeader == null || orgIdHeader.isBlank()) {
            log.error("Missing X-Organization-Id header in request");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Organization context required"
            );
        }
        
        try {
            return Long.parseLong(orgIdHeader);
        } catch (NumberFormatException e) {
            log.error("Invalid X-Organization-Id header value: {}", orgIdHeader);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid organization ID format"
            );
        }
    }
    
    /**
     * Gets the current HTTP servlet request from Spring's request context.
     * 
     * @return current HTTP servlet request
     * @throws IllegalStateException if called outside of an HTTP request context
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes == null) {
            throw new IllegalStateException("No request context available");
        }
        
        return attributes.getRequest();
    }
}
