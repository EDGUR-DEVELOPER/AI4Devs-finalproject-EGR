package com.docflow.documentcore.domain.service;

import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.NivelAcceso;

/**
 * Domain service interface for centralized permission evaluation.
 * 
 * <p>This service is the single source of truth for permission resolution in the system.
 * It applies the precedence rule:
 * <strong>Document ACL > Direct Folder ACL > Inherited Folder ACL</strong></p>
 * 
 * <p>All guards, middlewares, and authorization logic MUST use this service
 * to ensure consistent permission enforcement across the application.</p>
 * 
 * <h3>Design Pattern: Port (Hexagonal Architecture)</h3>
 * <p>This interface defines a <strong>port</strong> in hexagonal architecture,
 * establishing the contract for permission evaluation without exposing
 * implementation details. The actual implementation resides in the application layer.</p>
 * 
 * <h3>Thread Safety</h3>
 * <p>Implementations must be stateless and thread-safe for concurrent use.</p>
 * 
 * <h3>Multi-Tenancy</h3>
 * <p>All operations MUST respect organization isolation (tenant filtering).</p>
 * 
 * @see PermisoEfectivo
 * @see TipoRecurso
 * @see NivelAcceso
 */
public interface IEvaluadorPermisos {
    
    /**
     * Evaluates the effective permission of a user over a document.
     * 
     * <p>This method applies the following precedence order:</p>
     * <ol>
     *   <li><strong>Document explicit ACL</strong> (highest priority) - if user has a direct
     *       permission set on the document, this is returned regardless of folder permissions</li>
     *   <li><strong>Containing folder direct ACL</strong> - if no document permission exists,
     *       checks for a direct permission on the document's containing folder</li>
     *   <li><strong>Inherited ACL from ancestor folders</strong> - if no direct permissions exist,
     *       traverses the folder hierarchy looking for recursive permissions</li>
     * </ol>
     * 
     * <h3>Return Value Semantics</h3>
     * <ul>
     *   <li><strong>Non-null {@link PermisoEfectivo}</strong>: User has permission with specified level and origin</li>
     *   <li><strong>null</strong>: User has NO permission at any level (not an error, just absence of permission)</li>
     * </ul>
     * 
     * <h3>Example Usage</h3>
     * <pre>{@code
     * PermisoEfectivo permiso = evaluador.evaluarPermisoDocumento(1L, 100L, 10L);
     * if (permiso == null) {
     *     throw new PermissionDeniedException();
     * }
     * if (permiso.getNivelAcceso() == NivelAcceso.LECTURA) {
     *     // User can only read, not modify
     * }
     * }</pre>
     * 
     * @param usuarioId ID of the user whose permission is being evaluated
     * @param documentoId ID of the document to check permission for
     * @param organizacionId ID of the organization (for tenant isolation)
     * @return Effective permission with origin information, or null if no permission exists
     * @throws IllegalArgumentException if any parameter is null
     */
    PermisoEfectivo evaluarPermisoDocumento(
        Long usuarioId, 
        Long documentoId, 
        Long organizacionId
    );
    
    /**
     * Evaluates the effective permission of a user over a folder.
     * 
     * <p>This method considers:</p>
     * <ol>
     *   <li><strong>Direct ACL on the folder</strong> - explicit permission set on this folder</li>
     *   <li><strong>Inherited ACL from ancestor folders</strong> - recursive permissions from parents</li>
     * </ol>
     * 
     * <p><strong>Note:</strong> Folders don't have the same precedence complexity as documents
     * since they don't have a "document-level" permission. The evaluation is simpler:
     * direct permission or inherited permission.</p>
     * 
     * <h3>Return Value Semantics</h3>
     * <ul>
     *   <li><strong>Non-null {@link PermisoEfectivo}</strong>: User has permission</li>
     *   <li><strong>null</strong>: User has NO permission</li>
     * </ul>
     * 
     * @param usuarioId ID of the user whose permission is being evaluated
     * @param carpetaId ID of the folder to check permission for
     * @param organizacionId ID of the organization (for tenant isolation)
     * @return Effective permission with origin information, or null if no permission exists
     * @throws IllegalArgumentException if any parameter is null
     */
    PermisoEfectivo evaluarPermisoCarpeta(
        Long usuarioId, 
        Long carpetaId, 
        Long organizacionId
    );
    
    /**
     * Checks if a user has at least the required access level on a resource.
     * 
     * <p>This is a convenience method that internally calls
     * {@link #evaluarPermisoDocumento(Long, Long, Long)} or
     * {@link #evaluarPermisoCarpeta(Long, Long, Long)} depending on the resource type,
     * and then compares the access level against the required level.</p>
     * 
     * <h3>Access Level Hierarchy</h3>
     * <p>The permission hierarchy is:
     * <strong>LECTURA (1) < ESCRITURA (2) < ADMINISTRACION (3)</strong></p>
     * 
     * <p>A user with ESCRITURA can perform operations requiring LECTURA.
     * A user with ADMINISTRACION can perform any operation.</p>
     * 
     * <h3>Example Usage</h3>
     * <pre>{@code
     * // Check if user can read document
     * if (evaluador.tieneAcceso(userId, docId, TipoRecurso.DOCUMENTO, 
     *                           NivelAcceso.LECTURA, orgId)) {
     *     // Proceed with read operation
     * } else {
     *     throw new ResponseStatusException(HttpStatus.FORBIDDEN);
     * }
     * }</pre>
     * 
     * @param usuarioId ID of the user
     * @param recursoId ID of the resource (document or folder)
     * @param tipoRecurso Type of the resource
     * @param administracion Minimum required access level
     * @param organizacionId ID of the organization
     * @return true if user has at least the required level, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    boolean tieneAcceso(
        Long usuarioId, 
        Long recursoId, 
        TipoRecurso tipoRecurso,
        CodigoNivelAcceso administracion,
        Long organizacionId
    );
}
