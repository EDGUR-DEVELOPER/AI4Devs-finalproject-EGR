package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.CreatePermisoDocumentoUsuarioDTO;
import com.docflow.documentcore.application.dto.UsuarioResumenDTO;
import com.docflow.documentcore.application.validator.PermisoDocumentoUsuarioValidator;
import com.docflow.documentcore.domain.event.PermisoDocumentoUsuarioCreatedEvent;
import com.docflow.documentcore.domain.event.PermisoDocumentoUsuarioRevokedEvent;
import com.docflow.documentcore.domain.event.PermisoDocumentoUsuarioUpdatedEvent;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoDocumentoUsuario;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.entity.UsuarioEntity;
import com.docflow.documentcore.domain.repository.IPermisoDocumentoUsuarioRepository;
import com.docflow.documentcore.domain.repository.UsuarioJpaRepository;
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
 * Servicio de aplicación para permisos explícitos de usuarios sobre documentos.
 */
@Service
@Transactional
public class PermisoDocumentoUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(PermisoDocumentoUsuarioService.class);

    private final IPermisoDocumentoUsuarioRepository permisoRepository;
    private final PermisoDocumentoUsuarioValidator validator;
    private final UsuarioJpaRepository usuarioRepository;
    private final NivelAccesoService nivelAccesoService;
    private final ApplicationEventPublisher eventPublisher;

    public PermisoDocumentoUsuarioService(
            IPermisoDocumentoUsuarioRepository permisoRepository,
            PermisoDocumentoUsuarioValidator validator,
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

    /**
     * Crea o actualiza un permiso explícito de documento (comportamiento upsert).
     */
    public PermisoDocumentoUsuario crearOActualizarPermiso(
            Long documentoId,
            CreatePermisoDocumentoUsuarioDTO dto,
            Long organizacionId,
            Long usuarioAdminId
    ) {
        log.info("Creando/actualizando permiso de documento para usuario {} en documento {}", dto.getUsuarioId(), documentoId);

        // Validaciones
        validator.validarDocumentoEnOrganizacion(documentoId, organizacionId);
        validator.validarAdministrador(usuarioAdminId, documentoId, organizacionId);
        validator.validarUsuarioPerteneceOrganizacion(dto.getUsuarioId(), organizacionId);
        CodigoNivelAcceso codigoNivel = validator.validarNivelAccesoCodigo(dto.getNivelAccesoCodigo());

        NivelAcceso nivelAcceso = NivelAcceso.valueOf(codigoNivel.name());

        // Verificar si existe (upsert behavior)
        PermisoDocumentoUsuario permiso = permisoRepository
                .findByDocumentoIdAndUsuarioId(documentoId, dto.getUsuarioId())
                .orElse(null);

        boolean esNuevo = (permiso == null);
        NivelAcceso nivelAnterior = null;

        if (esNuevo) {
            // Crear nuevo
            permiso = new PermisoDocumentoUsuario();
            permiso.setDocumentoId(documentoId);
            permiso.setUsuarioId(dto.getUsuarioId());
            permiso.setOrganizacionId(organizacionId);
            permiso.setNivelAcceso(nivelAcceso);
            permiso.setFechaExpiracion(dto.getFechaExpiracion());
            permiso.setFechaAsignacion(OffsetDateTime.now());
        } else {
            // Actualizar existente
            nivelAnterior = permiso.getNivelAcceso();
            permiso.setNivelAcceso(nivelAcceso);
            permiso.setFechaExpiracion(dto.getFechaExpiracion());
        }

        PermisoDocumentoUsuario guardado = permisoRepository.save(permiso);

        // Publicar evento correspondiente
        if (esNuevo) {
            publicarEventoCreado(guardado, codigoNivel, usuarioAdminId);
        } else {
            publicarEventoActualizado(guardado, nivelAnterior, codigoNivel, usuarioAdminId);
        }

        return guardado;
    }

    /**
     * Revoca (elimina) un permiso explícito de documento.
     */
    public void revocarPermiso(
            Long documentoId,
            Long usuarioId,
            Long organizacionId,
            Long usuarioAdminId
    ) {
        log.info("Revocando permiso de documento para usuario {} del documento {}", usuarioId, documentoId);

        // Validaciones
        validator.validarDocumentoEnOrganizacion(documentoId, organizacionId);
        validator.validarAdministrador(usuarioAdminId, documentoId, organizacionId);

        // Buscar permiso para verificar existencia
        PermisoDocumentoUsuario permiso = permisoRepository
                .findByDocumentoIdAndUsuarioId(documentoId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso de documento", documentoId + ":" + usuarioId));

        // Verificar aislamiento multi-tenant
        if (!permiso.getOrganizacionId().equals(organizacionId)) {
            throw new ResourceNotFoundException("Permiso de documento", documentoId + ":" + usuarioId);
        }

        // Eliminar
        permisoRepository.delete(permiso);

        // Publicar evento
        publicarEventoRevocado(permiso, usuarioAdminId);
    }

    /**
     * Lista todos los permisos explícitos de un documento.
     */
    @Transactional(readOnly = true)
    public List<PermisoDocumentoUsuario> listarPermisos(Long documentoId, Long organizacionId) {
        validator.validarDocumentoEnOrganizacion(documentoId, organizacionId);
        return permisoRepository.findByDocumentoId(documentoId);
    }

    /**
     * Obtiene resúmenes de usuarios para enriquecer la respuesta.
     */
    @Transactional(readOnly = true)
    public Map<Long, UsuarioResumenDTO> obtenerUsuariosResumen(List<PermisoDocumentoUsuario> permisos, Long organizacionId) {
        Set<Long> usuarioIds = permisos.stream()
                .map(PermisoDocumentoUsuario::getUsuarioId)
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

    /**
     * Obtiene los niveles de acceso activos para enriquecer la respuesta.
     */
    @Transactional(readOnly = true)
    public Map<CodigoNivelAcceso, com.docflow.documentcore.domain.model.acl.NivelAcceso> obtenerNivelesAccesoActivos() {
        List<com.docflow.documentcore.domain.model.acl.NivelAcceso> niveles = nivelAccesoService.listAllActive();
        return niveles.stream()
                .collect(Collectors.toMap(com.docflow.documentcore.domain.model.acl.NivelAcceso::getCodigo, n -> n));
    }

    // Métodos privados para publicar eventos

    private void publicarEventoCreado(PermisoDocumentoUsuario permiso, CodigoNivelAcceso codigo, Long usuarioAdminId) {
        try {
            PermisoDocumentoUsuarioCreatedEvent event = new PermisoDocumentoUsuarioCreatedEvent(
                    permiso.getId(),
                    permiso.getDocumentoId(),
                    permiso.getUsuarioId(),
                    permiso.getOrganizacionId(),
                    codigo.getCodigo(),
                    usuarioAdminId,
                    Instant.now()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Error al emitir evento de permiso de documento creado", e);
        }
    }

    private void publicarEventoActualizado(
            PermisoDocumentoUsuario permiso,
            NivelAcceso nivelAnterior,
            CodigoNivelAcceso nuevoCodigo,
            Long usuarioAdminId
    ) {
        try {
            PermisoDocumentoUsuarioUpdatedEvent event = new PermisoDocumentoUsuarioUpdatedEvent(
                    permiso.getId(),
                    permiso.getDocumentoId(),
                    permiso.getUsuarioId(),
                    permiso.getOrganizacionId(),
                    nivelAnterior.name(),
                    nuevoCodigo.getCodigo(),
                    usuarioAdminId,
                    Instant.now()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Error al emitir evento de permiso de documento actualizado", e);
        }
    }

    private void publicarEventoRevocado(PermisoDocumentoUsuario permiso, Long usuarioAdminId) {
        try {
            PermisoDocumentoUsuarioRevokedEvent event = new PermisoDocumentoUsuarioRevokedEvent(
                    permiso.getId(),
                    permiso.getDocumentoId(),
                    permiso.getUsuarioId(),
                    permiso.getOrganizacionId(),
                    permiso.getNivelAcceso().name(),
                    usuarioAdminId,
                    Instant.now()
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Error al emitir evento de permiso de documento revocado", e);
        }
    }
}
