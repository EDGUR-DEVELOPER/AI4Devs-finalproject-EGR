package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.validator.NivelAccesoValidator;
import com.docflow.documentcore.application.validator.PermisoCarpetaUsuarioValidator;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.SinPermisoCarpetaException;
import com.docflow.documentcore.domain.exception.permiso.PermisoCarpetaDuplicadoException;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.domain.repository.UsuarioJpaRepository;
import com.docflow.documentcore.domain.model.entity.UsuarioEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermisoCarpetaUsuarioValidator - Tests Unitarios")
class PermisoCarpetaUsuarioValidatorTest {

    @Mock
    private ICarpetaRepository carpetaRepository;

    @Mock
    private IPermisoCarpetaUsuarioRepository permisoRepository;

    @Mock
    private UsuarioJpaRepository usuarioRepository;

    @Mock
    private NivelAccesoValidator nivelAccesoValidator;

    @InjectMocks
    private PermisoCarpetaUsuarioValidator validator;

    private Long organizacionId;
    private Long carpetaId;
    private Long usuarioId;

    @BeforeEach
    void setUp() {
        organizacionId = Math.abs(new Random().nextLong());
        carpetaId = Math.abs(new Random().nextLong());
        usuarioId = Math.abs(new Random().nextLong());
    }

    @Test
    @DisplayName("Debería validar carpeta existente")
    void should_ValidateFolderExists() {
        Carpeta carpeta = Carpeta.builder()
                .id(carpetaId)
                .organizacionId(organizacionId)
                .nombre("Test")
                .creadoPor(usuarioId)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();

        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId))
                .thenReturn(Optional.of(carpeta));

        validator.validarCarpetaExiste(carpetaId, organizacionId);

        verify(carpetaRepository).obtenerPorId(carpetaId, organizacionId);
    }

    @Test
    @DisplayName("Debería lanzar 404 cuando carpeta no existe")
    void should_ThrowNotFound_When_FolderMissing() {
        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validarCarpetaExiste(carpetaId, organizacionId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Debería validar usuario perteneciente a la organización")
    void should_ValidateUserInOrganization() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(usuarioId);
        usuario.setEmail("user@test.com");
        usuario.setNombreCompleto("User Test");

        when(usuarioRepository.findActiveByIdAndOrganizacionId(usuarioId, organizacionId))
                .thenReturn(Optional.of(usuario));

        validator.validarUsuarioPerteneceOrganizacion(usuarioId, organizacionId);

        verify(usuarioRepository).findActiveByIdAndOrganizacionId(usuarioId, organizacionId);
    }

    @Test
    @DisplayName("Debería lanzar 404 cuando usuario no pertenece a la organización")
    void should_ThrowNotFound_When_UserNotInOrganization() {
        when(usuarioRepository.findActiveByIdAndOrganizacionId(usuarioId, organizacionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validarUsuarioPerteneceOrganizacion(usuarioId, organizacionId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Debería validar nivel de acceso")
    void should_ValidateAccessLevel() {
        when(nivelAccesoValidator.validateCodigoFormat("LECTURA"))
                .thenReturn(CodigoNivelAcceso.LECTURA);

        validator.validarNivelAccesoCodigo("LECTURA");

        verify(nivelAccesoValidator).validateCodigoFormat("LECTURA");
        verify(nivelAccesoValidator).validateExistsByCodigo(CodigoNivelAcceso.LECTURA);
    }

    @Test
    @DisplayName("Debería lanzar conflicto cuando permiso ya existe")
    void should_ThrowConflict_When_PermissionExists() {
        when(permisoRepository.existsByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
                .thenReturn(true);

        assertThatThrownBy(() -> validator.validarNoDuplicado(carpetaId, usuarioId))
                .isInstanceOf(PermisoCarpetaDuplicadoException.class);
    }

    @Test
    @DisplayName("Debería permitir operación si usuario tiene ADMINISTRACION")
    void should_Allow_When_UserHasAdminPermission() {
        PermisoCarpetaUsuario permiso = new PermisoCarpetaUsuario();
        permiso.setId(Math.abs(new Random().nextLong()));
        permiso.setCarpetaId(carpetaId);
        permiso.setUsuarioId(usuarioId);
        permiso.setOrganizacionId(organizacionId);
        permiso.setNivelAcceso(NivelAcceso.ADMINISTRACION);

        when(permisoRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
                .thenReturn(Optional.of(permiso));

        validator.validarAdministrador(usuarioId, carpetaId, organizacionId);

        verify(permisoRepository).findByCarpetaIdAndUsuarioId(carpetaId, usuarioId);
    }

    @Test
    @DisplayName("Debería lanzar 403 si usuario no tiene ADMINISTRACION")
    void should_ThrowForbidden_When_UserLacksAdminPermission() {
        PermisoCarpetaUsuario permiso = new PermisoCarpetaUsuario();
        permiso.setId(Math.abs(new Random().nextLong()));
        permiso.setCarpetaId(carpetaId);
        permiso.setUsuarioId(usuarioId);
        permiso.setOrganizacionId(organizacionId);
        permiso.setNivelAcceso(NivelAcceso.LECTURA);

        when(permisoRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
                .thenReturn(Optional.of(permiso));

        assertThatThrownBy(() -> validator.validarAdministrador(usuarioId, carpetaId, organizacionId))
                .isInstanceOf(SinPermisoCarpetaException.class);
    }

    @Test
    @DisplayName("Debería lanzar 404 si permiso es de otra organización")
    void should_ThrowNotFound_When_TenantMismatch() {
        PermisoCarpetaUsuario permiso = new PermisoCarpetaUsuario();
        permiso.setId(Math.abs(new Random().nextLong()));
        permiso.setCarpetaId(carpetaId);
        permiso.setUsuarioId(usuarioId);
        permiso.setOrganizacionId(organizacionId + 1);
        permiso.setNivelAcceso(NivelAcceso.ADMINISTRACION);

        when(permisoRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
                .thenReturn(Optional.of(permiso));

        assertThatThrownBy(() -> validator.validarAdministrador(usuarioId, carpetaId, organizacionId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
