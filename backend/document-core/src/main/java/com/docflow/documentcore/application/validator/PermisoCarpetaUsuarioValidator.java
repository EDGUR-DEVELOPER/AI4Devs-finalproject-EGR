package com.docflow.documentcore.application.validator;

import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.SinPermisoCarpetaException;
import com.docflow.documentcore.domain.exception.permiso.PermisoCarpetaDuplicadoException;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.domain.repository.UsuarioJpaRepository;
import org.springframework.stereotype.Component;

/**
 * Validador de reglas de negocio para permisos explícitos de carpetas.
 */
@Component
public class PermisoCarpetaUsuarioValidator {

    private final ICarpetaRepository carpetaRepository;
    private final IPermisoCarpetaUsuarioRepository permisoRepository;
    private final UsuarioJpaRepository usuarioRepository;
    private final NivelAccesoValidator nivelAccesoValidator;

    public PermisoCarpetaUsuarioValidator(
            ICarpetaRepository carpetaRepository,
            IPermisoCarpetaUsuarioRepository permisoRepository,
            UsuarioJpaRepository usuarioRepository,
            NivelAccesoValidator nivelAccesoValidator
    ) {
        this.carpetaRepository = carpetaRepository;
        this.permisoRepository = permisoRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelAccesoValidator = nivelAccesoValidator;
    }

    public void validarCarpetaExiste(Long carpetaId, Long organizacionId) {
        boolean existe = carpetaRepository.obtenerPorId(carpetaId, organizacionId).isPresent();
        if (!existe) {
            throw new ResourceNotFoundException("Carpeta", carpetaId);
        }
    }

    public void validarUsuarioPerteneceOrganizacion(Long usuarioId, Long organizacionId) {
        boolean existe = usuarioRepository.findActiveByIdAndOrganizacionId(usuarioId, organizacionId).isPresent();
        if (!existe) {
            throw new ResourceNotFoundException("Usuario", usuarioId);
        }
    }

    public CodigoNivelAcceso validarNivelAccesoCodigo(String codigo) {
        CodigoNivelAcceso codigoEnum = nivelAccesoValidator.validateCodigoFormat(codigo);
        nivelAccesoValidator.validateExistsByCodigo(codigoEnum);
        return codigoEnum;
    }

    public void validarNoDuplicado(Long carpetaId, Long usuarioId) {
        if (permisoRepository.existsByCarpetaIdAndUsuarioId(carpetaId, usuarioId)) {
            throw new PermisoCarpetaDuplicadoException(carpetaId, usuarioId);
        }
    }

    public void validarPermisoExiste(Long carpetaId, Long usuarioId) {
        boolean existe = permisoRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId).isPresent();
        if (!existe) {
            throw new ResourceNotFoundException("Permiso de carpeta", carpetaId + ":" + usuarioId);
        }
    }

    public void validarAdministrador(Long usuarioId, Long carpetaId, Long organizacionId) {
        PermisoCarpetaUsuario permiso = permisoRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId)
                .orElseThrow(() -> new SinPermisoCarpetaException(carpetaId));

        if (!permiso.getOrganizacionId().equals(organizacionId)) {
            throw new ResourceNotFoundException("Carpeta", carpetaId);
        }

        if (!NivelAcceso.ADMINISTRACION.equals(permiso.getNivelAcceso())) {
            throw new SinPermisoCarpetaException(carpetaId);
        }
    }
}
