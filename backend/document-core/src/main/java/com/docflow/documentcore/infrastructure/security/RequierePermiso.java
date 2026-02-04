package com.docflow.documentcore.infrastructure.security;

import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.NivelAcceso;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declarative permission checking on controller methods.
 * 
 * <p>This annotation allows specifying permission requirements directly on methods,
 * eliminating the need for inline permission checking code. The {@link RequierePermisoGuard}
 * AOP aspect intercepts annotated methods and enforces the specified permissions.</p>
 * 
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @GetMapping("/{documentoId}")
 * @RequierePermiso(
 *     tipoRecurso = TipoRecurso.DOCUMENTO,
 *     nivelRequerido = NivelAcceso.LECTURA,
 *     paramIndex = 0
 * )
 * public ResponseEntity<DocumentoDTO> getDocumento(@PathVariable Long documentoId) {
 *     // Method only executes if user has LECTURA permission on documentoId
 * }
 * }</pre>
 * 
 * <h3>Permission Enforcement</h3>
 * <p>The guard will:
 * <ol>
 *   <li>Extract the resource ID from the method parameter at {@link #paramIndex()}</li>
 *   <li>Get the current user and organization from the security context</li>
 *   <li>Call {@link com.docflow.documentcore.domain.service.IEvaluadorPermisos#tieneAcceso}</li>
 *   <li>Throw {@code ResponseStatusException(403)} if permission check fails</li>
 * </ol>
 * </p>
 * 
 * <h3>Benefits</h3>
 * <ul>
 *   <li><strong>Declarative security</strong>: Permission requirements are self-documenting</li>
 *   <li><strong>Consistency</strong>: All endpoints use the same enforcement mechanism</li>
 *   <li><strong>Separation of concerns</strong>: Controllers focus on business logic</li>
 *   <li><strong>Maintainability</strong>: Permission logic is centralized in the guard</li>
 * </ul>
 * 
 * @see RequierePermisoGuard
 * @see com.docflow.documentcore.domain.service.IEvaluadorPermisos
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequierePermiso {
    
    /**
     * Type of resource being accessed (DOCUMENTO or CARPETA).
     * 
     * <p>This determines which evaluation method is called on the
     * permission evaluator service.</p>
     * 
     * @return the resource type
     */
    TipoRecurso tipoRecurso();
    
    /**
     * Minimum required access level for the operation.
     * 
     * <p>Users with a higher access level than required will also be granted access.
     * For example, a user with ESCRITURA can perform operations requiring LECTURA.</p>
     * 
     * <p>Access level hierarchy:
     * <strong>LECTURA < ESCRITURA < ADMINISTRACION</strong></p>
     * 
     * @return the minimum required access level
     */
    NivelAcceso nivelRequerido();
    
    /**
     * Index of the method parameter that contains the resource ID.
     * 
     * <p>The guard will extract the resource ID (Long) from the method's
     * parameter list at this index.</p>
     * 
     * <h3>Examples</h3>
     * <pre>{@code
     * // paramIndex = 0 (default)
     * public void method(Long resourceId, String name) { }
     * 
     * // paramIndex = 1
     * public void method(String name, Long resourceId) { }
     * }</pre>
     * 
     * @return the parameter index (0-based)
     */
    int paramIndex() default 0;
    
    /**
     * Custom error message to return if permission is denied.
     * 
     * <p>This message will be included in the {@code ResponseStatusException}
     * thrown when the user lacks sufficient permissions.</p>
     * 
     * @return the error message
     */
    String errorMessage() default "Insufficient permissions";
}
