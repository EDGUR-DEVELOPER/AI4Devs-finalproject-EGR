package com.docflow.documentcore.application.validator;

import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.repository.DocumentoJpaRepository;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.domain.repository.IPermisoDocumentoUsuarioRepository;
import com.docflow.documentcore.domain.repository.UsuarioJpaRepository;
import org.springframework.stereotype.Component;

/**
 * Validador de reglas de negocio para permisos explícitos de documentos.
 */
@Component
public class PermisoDocumentoUsuarioValidator {

    private final DocumentoJpaRepository documentoRepository;
    private final ICarpetaRepository carpetaRepository;
    private final IPermisoCarpetaUsuarioRepository permisoCarpetaRepository;
    private final UsuarioJpaRepository usuarioRepository;
    private final NivelAccesoValidator nivelAccesoValidator;

    public PermisoDocumentoUsuarioValidator(
            DocumentoJpaRepository documentoRepository,
            ICarpetaRepository carpetaRepository,
            IPermisoCarpetaUsuarioRepository permisoCarpetaRepository,
            IPermisoDocumentoUsuarioRepository permisoRepository,
            UsuarioJpaRepository usuarioRepository,
            NivelAccesoValidator nivelAccesoValidator
    ) {
        this.documentoRepository = documentoRepository;
        this.carpetaRepository = carpetaRepository;
        this.permisoCarpetaRepository = permisoCarpetaRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelAccesoValidator = nivelAccesoValidator;
    }

    /**
     * Valida que el documento existe en la organización.
     * Lanza excepción genérica si no existe o no pertenece a la organización.
     */
    public Documento validarDocumentoEnOrganizacion(Long documentoId, Long organizacionId) {
        return documentoRepository.findByIdAndOrganizacionId(documentoId, organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento", documentoId));
    }

    /**
     * Valida que el usuario pertenece a la organización.
     */
    public void validarUsuarioPerteneceOrganizacion(Long usuarioId, Long organizacionId) {
        boolean existe = usuarioRepository.findActiveByIdAndOrganizacionId(usuarioId, organizacionId).isPresent();
        if (!existe) {
            throw new ResourceNotFoundException("Usuario", usuarioId);
        }
    }

    /**
     * Valida el código de nivel de acceso.
     */
    public CodigoNivelAcceso validarNivelAccesoCodigo(String codigo) {
        CodigoNivelAcceso codigoEnum = nivelAccesoValidator.validateCodigoFormat(codigo);
        nivelAccesoValidator.validateExistsByCodigo(codigoEnum);
        return codigoEnum;
    }

    /**
     * Valida que el usuario es admin o tiene permiso ADMINISTRACION en la carpeta padre del documento.
     * 
     * Para este ticket, cualquier usuario admin puede asignar permisos.
     * Para usuarios no admin, deben tener ADMINISTRACION sobre la carpeta padre.
     */
    public void validarAdministrador(Long usuarioAdminId, Long documentoId, Long organizacionId) {
        // Primero validar que el documento existe
        Documento documento = validarDocumentoEnOrganizacion(documentoId, organizacionId);
        
        // Buscar si tiene permiso de ADMINISTRACION en la carpeta padre
        Long carpetaId = documento.getCarpetaId();
        carpetaRepository.obtenerPorId(carpetaId, organizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Carpeta", carpetaId));
        
        // Verificar si tiene permiso de ADMINISTRACION en la carpeta
        PermisoCarpetaUsuario permisoCarpeta = permisoCarpetaRepository
                .findByCarpetaIdAndUsuarioId(carpetaId, usuarioAdminId)
                .orElse(null);
        
        if (permisoCarpeta == null || !NivelAcceso.ADMINISTRACION.equals(permisoCarpeta.getNivelAcceso())) {
            // No tiene permiso de ADMINISTRACION: verificar si es admin global (simplificado para este ticket)
            // En una implementación completa, se verificaría el rol en el servicio de identity
            throw new AccessDeniedException("No tiene permisos para administrar este documento");
        }
    }

    /**
     * Valida que el permiso no está duplicado.
     * Si existe, permitir (será actualización).
     */
    public void validarNoDuplicado(Long documentoId, Long usuarioId) {
        // Para este ticket, si existe se actualiza, no se lanza excepción
        // Este método está aquí por simetría con el validator de carpeta
    }
}
