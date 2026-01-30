package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.UsuarioResumenDTO;
import com.docflow.documentcore.application.validator.PermisoCarpetaUsuarioValidator;
import com.docflow.documentcore.domain.event.PermisoCarpetaUsuarioCreatedEvent;
import com.docflow.documentcore.domain.event.PermisoCarpetaUsuarioUpdatedEvent;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.model.entity.UsuarioEntity;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.domain.repository.UsuarioJpaRepository;
import com.docflow.documentcore.application.dto.UpdatePermisoCarpetaUsuarioDTO;
import com.docflow.documentcore.application.dto.CreatePermisoCarpetaUsuarioDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para permisos explícitos de usuarios sobre carpetas.
 */
@Service
@Transactional
public class PermisoCarpetaUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(PermisoCarpetaUsuarioService.class);

    private final IPermisoCarpetaUsuarioRepository permisoRepository;
    private final PermisoCarpetaUsuarioValidator validator;
    private final UsuarioJpaRepository usuarioRepository;
    private final NivelAccesoService nivelAccesoService;
    private final ApplicationEventPublisher eventPublisher;

    public PermisoCarpetaUsuarioService(
            IPermisoCarpetaUsuarioRepository permisoRepository,
            PermisoCarpetaUsuarioValidator validator,
            UsuarioJpaRepository usuarioRepository,
            NivelAccesoService nivelAccesoService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.permisoRepository = permisoRepository;
        this.validator = validator;
        this.usuarioRepository = usuarioRepository;
        this.nivelAccesoService = nivelAccesoService;
        this.eventPublisher = eventPublisher;
    }

    public PermisoCarpetaUsuario crearPermiso(
            Long carpetaId,
            CreatePermisoCarpetaUsuarioDTO dto,
            Long organizacionId,
            Long usuarioAdminId
    ) {
        log.info("Creando permiso de carpeta para usuario {} en carpeta {}", dto.getUsuarioId(), carpetaId);

        validator.validarAdministrador(usuarioAdminId, carpetaId, organizacionId);
        validator.validarCarpetaExiste(carpetaId, organizacionId);
        validator.validarUsuarioPerteneceOrganizacion(dto.getUsuarioId(), organizacionId);
        CodigoNivelAcceso codigoNivel = validator.validarNivelAccesoCodigo(dto.getNivelAccesoCodigo());
        validator.validarNoDuplicado(carpetaId, dto.getUsuarioId());

        NivelAcceso nivelAcceso = NivelAcceso.valueOf(codigoNivel.name());

        PermisoCarpetaUsuario permiso = new PermisoCarpetaUsuario();
        permiso.setCarpetaId(carpetaId);
        permiso.setUsuarioId(dto.getUsuarioId());
        permiso.setOrganizacionId(organizacionId);
        permiso.setNivelAcceso(nivelAcceso);
        permiso.setRecursivo(dto.getRecursivo());
        permiso.setFechaAsignacion(OffsetDateTime.now());

        PermisoCarpetaUsuario creado = permisoRepository.save(permiso);

        publicarEventoCreado(creado, codigoNivel, usuarioAdminId);

        return creado;
    }

    public PermisoCarpetaUsuario actualizarPermiso(
            Long carpetaId,
            Long usuarioId,
            UpdatePermisoCarpetaUsuarioDTO dto,
            Long organizacionId,
            Long usuarioAdminId
    ) {
        log.info("Actualizando permiso de carpeta para usuario {} en carpeta {}", usuarioId, carpetaId);

        validator.validarAdministrador(usuarioAdminId, carpetaId, organizacionId);
        validator.validarCarpetaExiste(carpetaId, organizacionId);
        validator.validarUsuarioPerteneceOrganizacion(usuarioId, organizacionId);
        CodigoNivelAcceso nuevoCodigo = validator.validarNivelAccesoCodigo(dto.getNivelAccesoCodigo());

        PermisoCarpetaUsuario permiso = permisoRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso de carpeta", carpetaId + ":" + usuarioId));

        NivelAcceso nivelAnterior = permiso.getNivelAcceso();

        permiso.setNivelAcceso(NivelAcceso.valueOf(nuevoCodigo.name()));
        if (dto.getRecursivo() != null) {
            permiso.setRecursivo(dto.getRecursivo());
        }

        PermisoCarpetaUsuario actualizado = permisoRepository.save(permiso);

        publicarEventoActualizado(actualizado, nivelAnterior, nuevoCodigo, usuarioAdminId);

        return actualizado;
    }

    @Transactional(readOnly = true)
    public List<PermisoCarpetaUsuario> listarPermisos(Long carpetaId, Long organizacionId) {
        validator.validarCarpetaExiste(carpetaId, organizacionId);
        return permisoRepository.findByCarpetaId(carpetaId);
    }

    @Transactional(readOnly = true)
    public Map<Long, UsuarioResumenDTO> obtenerUsuariosResumen(List<PermisoCarpetaUsuario> permisos, Long organizacionId) {
        Set<Long> usuarioIds = permisos.stream()
                .map(PermisoCarpetaUsuario::getUsuarioId)
                .collect(Collectors.toSet());

        if (usuarioIds.isEmpty()) {
            return Map.of();
        }

        List<UsuarioEntity> usuarios = usuarioRepository.findActiveByIdsAndOrganizacionId(
                List.copyOf(usuarioIds),
                organizacionId
        );

        return usuarios.stream()
                .collect(Collectors.toMap(
                        UsuarioEntity::getId,
                        u -> new UsuarioResumenDTO(u.getId(), u.getEmail(), u.getNombreCompleto())
                ));
    }

    @Transactional(readOnly = true)
    public Map<CodigoNivelAcceso, com.docflow.documentcore.domain.model.acl.NivelAcceso> obtenerNivelesAccesoActivos() {
        List<com.docflow.documentcore.domain.model.acl.NivelAcceso> niveles = nivelAccesoService.listAllActive();
        return niveles.stream()
                .collect(Collectors.toMap(com.docflow.documentcore.domain.model.acl.NivelAcceso::getCodigo, n -> n));
    }

    private void publicarEventoCreado(PermisoCarpetaUsuario permiso, CodigoNivelAcceso codigo, Long usuarioAdminId) {
        try {
            PermisoCarpetaUsuarioCreatedEvent event = new PermisoCarpetaUsuarioCreatedEvent(
                    permiso.getId(),
                    permiso.getCarpetaId(),
                    permiso.getUsuarioId(),
                    permiso.getOrganizacionId(),
                    codigo.getCodigo(),
                    permiso.getRecursivo(),
                    usuarioAdminId,
                    Instant.now()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Error al emitir evento de permiso creado", e);
        }
    }

    private void publicarEventoActualizado(
            PermisoCarpetaUsuario permiso,
            NivelAcceso nivelAnterior,
            CodigoNivelAcceso nuevoCodigo,
            Long usuarioAdminId
    ) {
        try {
            PermisoCarpetaUsuarioUpdatedEvent event = new PermisoCarpetaUsuarioUpdatedEvent(
                    permiso.getId(),
                    permiso.getCarpetaId(),
                    permiso.getUsuarioId(),
                    permiso.getOrganizacionId(),
                    nivelAnterior.name(),
                    nuevoCodigo.getCodigo(),
                    permiso.getRecursivo(),
                    usuarioAdminId,
                    Instant.now()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Error al emitir evento de permiso actualizado", e);
        }
    }
}
