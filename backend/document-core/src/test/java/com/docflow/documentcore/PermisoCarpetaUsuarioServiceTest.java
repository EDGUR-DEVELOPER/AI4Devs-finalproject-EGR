package com.docflow.documentcore;

import com.docflow.documentcore.application.dto.CreatePermisoCarpetaUsuarioDTO;
import com.docflow.documentcore.application.dto.UpdatePermisoCarpetaUsuarioDTO;
import com.docflow.documentcore.application.service.NivelAccesoService;
import com.docflow.documentcore.application.service.PermisoCarpetaUsuarioService;
import com.docflow.documentcore.application.validator.PermisoCarpetaUsuarioValidator;
import com.docflow.documentcore.domain.event.PermisoCarpetaUsuarioCreatedEvent;
import com.docflow.documentcore.domain.event.PermisoCarpetaUsuarioRevokedEvent;
import com.docflow.documentcore.domain.event.PermisoCarpetaUsuarioUpdatedEvent;
import com.docflow.documentcore.domain.exception.AclNotFoundException;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.domain.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermisoCarpetaUsuarioService - Tests Unitarios")
class PermisoCarpetaUsuarioServiceTest {

    @Mock
    private IPermisoCarpetaUsuarioRepository permisoRepository;

    @Mock
    private PermisoCarpetaUsuarioValidator validator;

    @Mock
    private UsuarioJpaRepository usuarioRepository;

    @Mock
    private NivelAccesoService nivelAccesoService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PermisoCarpetaUsuarioService service;

    private Long carpetaId;
    private Long usuarioId;
    private Long organizacionId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        carpetaId = Math.abs(new Random().nextLong());
        usuarioId = Math.abs(new Random().nextLong());
        organizacionId = Math.abs(new Random().nextLong());
        adminId = Math.abs(new Random().nextLong());
    }

    @Test
    @DisplayName("Debería crear permiso cuando la solicitud es válida")
    void should_CreatePermission_When_RequestIsValid() {
        CreatePermisoCarpetaUsuarioDTO dto = new CreatePermisoCarpetaUsuarioDTO();
        dto.setUsuarioId(usuarioId);
        dto.setNivelAccesoCodigo("LECTURA");
        dto.setRecursivo(false);

        PermisoCarpetaUsuario saved = new PermisoCarpetaUsuario();
        saved.setId(Math.abs(new Random().nextLong()));
        saved.setCarpetaId(carpetaId);
        saved.setUsuarioId(usuarioId);
        saved.setOrganizacionId(organizacionId);
        saved.setNivelAcceso(NivelAcceso.LECTURA);
        saved.setRecursivo(false);
        saved.setFechaAsignacion(OffsetDateTime.now());

        when(permisoRepository.save(any(PermisoCarpetaUsuario.class))).thenReturn(saved);
        when(validator.validarNivelAccesoCodigo("LECTURA")).thenReturn(CodigoNivelAcceso.LECTURA);

        PermisoCarpetaUsuario result = service.crearPermiso(carpetaId, dto, organizacionId, adminId);

        assertThat(result).isNotNull();
        assertThat(result.getCarpetaId()).isEqualTo(carpetaId);
        assertThat(result.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(result.getNivelAcceso()).isEqualTo(NivelAcceso.LECTURA);

        verify(validator).validarAdministrador(adminId, carpetaId, organizacionId);
        verify(validator).validarCarpetaExiste(carpetaId, organizacionId);
        verify(validator).validarUsuarioPerteneceOrganizacion(usuarioId, organizacionId);
        verify(validator).validarNoDuplicado(carpetaId, usuarioId);
        verify(permisoRepository).save(any(PermisoCarpetaUsuario.class));
        verify(eventPublisher).publishEvent(any(PermisoCarpetaUsuarioCreatedEvent.class));
    }

    @Test
    @DisplayName("Debería actualizar permiso cuando la solicitud es válida")
    void should_UpdatePermission_When_RequestIsValid() {
        UpdatePermisoCarpetaUsuarioDTO dto = new UpdatePermisoCarpetaUsuarioDTO();
        dto.setNivelAccesoCodigo("ADMINISTRACION");
        dto.setRecursivo(true);

        PermisoCarpetaUsuario existing = new PermisoCarpetaUsuario();
        existing.setId(Math.abs(new Random().nextLong()));
        existing.setCarpetaId(carpetaId);
        existing.setUsuarioId(usuarioId);
        existing.setOrganizacionId(organizacionId);
        existing.setNivelAcceso(NivelAcceso.LECTURA);
        existing.setRecursivo(false);
        existing.setFechaAsignacion(OffsetDateTime.now());

        when(permisoRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
                .thenReturn(Optional.of(existing));
        when(permisoRepository.save(any(PermisoCarpetaUsuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(validator.validarNivelAccesoCodigo("ADMINISTRACION"))
                .thenReturn(CodigoNivelAcceso.ADMINISTRACION);

        PermisoCarpetaUsuario result = service.actualizarPermiso(carpetaId, usuarioId, dto, organizacionId, adminId);

        assertThat(result.getNivelAcceso()).isEqualTo(NivelAcceso.ADMINISTRACION);
        assertThat(result.getRecursivo()).isTrue();

        verify(validator).validarAdministrador(adminId, carpetaId, organizacionId);
        verify(validator).validarCarpetaExiste(carpetaId, organizacionId);
        verify(validator).validarUsuarioPerteneceOrganizacion(usuarioId, organizacionId);
        verify(permisoRepository).findByCarpetaIdAndUsuarioId(carpetaId, usuarioId);
        verify(permisoRepository).save(any(PermisoCarpetaUsuario.class));
        verify(eventPublisher).publishEvent(any(PermisoCarpetaUsuarioUpdatedEvent.class));
    }

        @Test
        @DisplayName("Debería revocar permiso cuando existe")
        void should_RevokePermission_When_Exists() {
        PermisoCarpetaUsuario existing = new PermisoCarpetaUsuario();
        existing.setId(Math.abs(new Random().nextLong()));
        existing.setCarpetaId(carpetaId);
        existing.setUsuarioId(usuarioId);
        existing.setOrganizacionId(organizacionId);
        existing.setNivelAcceso(NivelAcceso.LECTURA);
        existing.setRecursivo(false);
        existing.setFechaAsignacion(OffsetDateTime.now());

        when(permisoRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
            .thenReturn(Optional.of(existing));
        when(permisoRepository.revokePermission(carpetaId, usuarioId, organizacionId))
            .thenReturn(1);

        service.revocarPermiso(carpetaId, usuarioId, organizacionId, adminId);

        verify(validator).validarAdministrador(adminId, carpetaId, organizacionId);
        verify(validator).validarCarpetaExiste(carpetaId, organizacionId);
        verify(permisoRepository).revokePermission(carpetaId, usuarioId, organizacionId);
        verify(eventPublisher).publishEvent(any(PermisoCarpetaUsuarioRevokedEvent.class));
        }

        @Test
        @DisplayName("Debería lanzar excepción cuando permiso no existe")
        void should_ThrowNotFound_When_PermissionMissing() {
        when(permisoRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revocarPermiso(carpetaId, usuarioId, organizacionId, adminId))
            .isInstanceOf(AclNotFoundException.class);

        verify(permisoRepository, never()).revokePermission(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(PermisoCarpetaUsuarioRevokedEvent.class));
        }
}
