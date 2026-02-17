package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoDocumentoUsuario;
import com.docflow.documentcore.domain.model.PermisoEfectivo;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.repository.IPermisoDocumentoUsuarioRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.Optional;

/**
 * Application service implementing centralized permission evaluation.
 * 
 * <p>This service is the <strong>adapter</strong> that implements the {@link IEvaluadorPermisos}
 * port defined in the domain layer, following hexagonal architecture principles.</p>
 * 
 * <h3>Precedence Rule Implementation</h3>
 * <p>Applies the strict precedence rule:
 * <strong>Document ACL > Direct Folder ACL > Inherited Folder ACL</strong></p>
 * 
 * <h3>Design Decisions</h3>
 * <ul>
 *   <li><strong>Read-only transactions</strong>: All methods use read-only transactions
 *       for optimization since no data is modified</li>
 *   <li><strong>Delegation</strong>: Delegates folder permission evaluation to existing
 *       {@link PermisoHerenciaService} to avoid code duplication</li>
 *   <li><strong>Logging</strong>: DEBUG level for evaluation steps, WARN for permission denials</li>
 *   <li><strong>Stateless</strong>: Service has no state, safe for concurrent use</li>
 * </ul>
 * 
 * <h3>Multi-Tenancy</h3>
 * <p>All operations validate organizacionId to prevent cross-tenant access.</p>
 * 
 * @see IEvaluadorPermisos
 * @see PermisoHerenciaService
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EvaluadorPermisosService implements IEvaluadorPermisos {

    private final IPermisoDocumentoUsuarioRepository permisoDocumentoRepository;
    private final PermisoHerenciaService permisoHerenciaService;
    private final EntityManager entityManager;

    @Override
    public PermisoEfectivo evaluarPermisoDocumento(
            Long usuarioId,
            Long documentoId,
            Long organizacionId
    ) {
        if (usuarioId == null || documentoId == null || organizacionId == null) {
            throw new IllegalArgumentException("usuarioId, documentoId, and organizacionId cannot be null");
        }

        log.debug("Evaluating document permission: user={}, document={}, org={}",
                usuarioId, documentoId, organizacionId);

        // STEP 1: Check for explicit document ACL (HIGHEST PRIORITY)
        Optional<PermisoDocumentoUsuario> aclDocumento =
                permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId);

        if (aclDocumento.isPresent()) {
            PermisoDocumentoUsuario permiso = aclDocumento.get();
            
            // Validate organization isolation
            if (!permiso.getOrganizacionId().equals(organizacionId)) {
                log.warn("Cross-tenant access attempt denied: user={} attempted to access document={} " +
                        "from org={} but document belongs to org={}",
                        usuarioId, documentoId, organizacionId, permiso.getOrganizacionId());
                // Treat as if no permission exists
                return evaluarPermisoCarpetaPorDocumento(usuarioId, documentoId, organizacionId);
            }

            log.debug("Document permission found: nivel={}, origin=DOCUMENTO",
                    permiso.getNivelAcceso());

            return PermisoEfectivo.documento(
                    permiso.getNivelAcceso(),
                    documentoId
            );
        }

        // STEP 2: Fallback to folder permission (direct or inherited)
        log.debug("No document ACL found for user={} on document={}, checking folder permissions",
                usuarioId, documentoId);

        return evaluarPermisoCarpetaPorDocumento(usuarioId, documentoId, organizacionId);
    }

    @Override
    public PermisoEfectivo evaluarPermisoCarpeta(
            Long usuarioId,
            Long carpetaId,
            Long organizacionId
    ) {
        if (usuarioId == null || carpetaId == null || organizacionId == null) {
            throw new IllegalArgumentException("usuarioId, carpetaId, and organizacionId cannot be null");
        }

        log.debug("Evaluating folder permission: user={}, folder={}, org={}",
                usuarioId, carpetaId, organizacionId);

        // Delegate to existing PermisoHerenciaService
        // This service already handles direct ACL and inheritance correctly
        Optional<PermisoEfectivo> permisoEfectivo =
                permisoHerenciaService.evaluarPermisoEfectivo(usuarioId, carpetaId, organizacionId);

        if (permisoEfectivo.isEmpty()) {
            log.debug("No permission found for user={} on folder={}", usuarioId, carpetaId);
        }

        return permisoEfectivo.orElse(null);
    }

    @Override
    public boolean tieneAcceso(
            Long usuarioId,
            Long recursoId,
            TipoRecurso tipoRecurso,
            CodigoNivelAcceso nivelRequerido,
            Long organizacionId
    ) {
        if (usuarioId == null || recursoId == null || tipoRecurso == null ||
                nivelRequerido == null || organizacionId == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }

        // Evaluate permission based on resource type
        PermisoEfectivo permiso = tipoRecurso == TipoRecurso.DOCUMENTO
                ? evaluarPermisoDocumento(usuarioId, recursoId, organizacionId)
                : evaluarPermisoCarpeta(usuarioId, recursoId, organizacionId);

        if (permiso == null) {
            log.debug("Access check failed: no permission found for user={} on resource={} type={}",
                    usuarioId, recursoId, tipoRecurso);
            return false;
        }

        // Check if actual level meets required level
        boolean hasAccess = cumpleNivelRequerido(permiso.getNivelAcceso(), nivelRequerido);

        log.debug("Access check result: user={}, resource={}, type={}, required={}, actual={}, granted={}",
                usuarioId, recursoId, tipoRecurso, nivelRequerido,
                permiso.getNivelAcceso(), hasAccess);

        return hasAccess;
    }

    /**
     * Helper method to get the containing folder ID of a document and evaluate folder permission.
     * 
     * @param usuarioId user ID
     * @param documentoId document ID
     * @param organizacionId organization ID
     * @return folder permission or null if no permission
     */
    private PermisoEfectivo evaluarPermisoCarpetaPorDocumento(
            Long usuarioId,
            Long documentoId,
            Long organizacionId
    ) {
        // Get document's containing folder using JPA query
        // This avoids creating a full repository just for this method
        String jpql = "SELECT d.carpetaId FROM Documento d WHERE d.id = :documentoId AND d.organizacionId = :organizacionId";
        
        try {
            Long carpetaId = entityManager.createQuery(jpql, Long.class)
                    .setParameter("documentoId", documentoId)
                    .setParameter("organizacionId", organizacionId)
                    .getSingleResult();

            if (carpetaId == null) {
                log.warn("Document found but has no carpetaId: documentoId={}", documentoId);
                return null;
            }

            return evaluarPermisoCarpeta(usuarioId, carpetaId, organizacionId);
            
        } catch (jakarta.persistence.NoResultException e) {
            log.warn("Document not found or doesn't belong to organization: documentoId={}, org={}",
                    documentoId, organizacionId);
            return null;
        }
    }

    /**
     * Checks if the actual access level meets the required access level.
     * 
     * <p>Permission hierarchy: LECTURA (1) < ESCRITURA (2) < ADMINISTRACION (3)</p>
     * 
     * @param actual the actual permission level the user has
     * @param requerido the minimum required permission level
     * @return true if actual >= required in the hierarchy
     */
    private boolean cumpleNivelRequerido(NivelAcceso actual, CodigoNivelAcceso requerido) {
        int nivelActual = getNivelJerarquico(actual);
        int nivelReq = getNivelJerarquico(requerido);
        return nivelActual >= nivelReq;
    }

    /**
     * Maps NivelAcceso enum to a hierarchical numeric value.
     * 
     * @param nivel the access level
     * @return numeric value (1 = LECTURA, 2 = ESCRITURA, 3 = ADMINISTRACION)
     */
    private int getNivelJerarquico(NivelAcceso nivel) {
        return switch (nivel) {
            case LECTURA -> 1;
            case ESCRITURA -> 2;
            case ADMINISTRACION -> 3;
        };
    }

    /**
     * Maps CodigoNivelAcceso enum to a hierarchical numeric value.
     * 
     * @param codigo the access level code
     * @return numeric value (1 = LECTURA, 2 = ESCRITURA, 3 = ADMINISTRACION)
     */
    private int getNivelJerarquico(CodigoNivelAcceso codigo) {
        return switch (codigo) {
            case LECTURA -> 1;
            case ESCRITURA -> 2;
            case ADMINISTRACION -> 3;
        };
    }
}
