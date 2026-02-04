package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.CreatePermisoDocumentoUsuarioDTO;
import com.docflow.documentcore.application.validator.PermisoDocumentoUsuarioValidator;
import com.docflow.documentcore.domain.event.PermisoDocumentoUsuarioCreatedEvent;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.permiso.PermisoDocumentoUsuario;
import com.docflow.documentcore.domain.repository.IPermisoDocumentoUsuarioRepository;
import com.docflow.documentcore.domain.repository.UsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PermisoDocumentoUsuarioService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermisoDocumentoUsuarioService - Tests Unitarios")
class PermisoDocumentoUsuarioServiceTest {

    @Mock
    private IPermisoDocumentoUsuarioRepository permisoRepository;

    @Mock
    private PermisoDocumentoUsuarioValidator validator;

    @Mock
    private UsuarioJpaRepository usuarioRepository;

    @Mock
    private NivelAccesoService nivelAccesoService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PermisoDocumentoUsuarioService service;

    private CreatePermisoDocumentoUsuarioDTO createDTO;
    private PermisoDocumentoUsuario permisoMock;
    private static final Long DOCUMENTO_ID = 100L;
    private static final Long USUARIO_ID = 200L;
    private static final Long ORGANIZACION_ID = 1L;
    private static final Long ADMIN_ID = 300L;

    @BeforeEach
    void setUp() {
        createDTO = new CreatePermisoDocumentoUsuarioDTO();
        createDTO.setUsuarioId(USUARIO_ID);
        createDTO.setNivelAccesoCodigo("LECTURA");
        createDTO.setFechaExpiracion(null);

        permisoMock = new PermisoDocumentoUsuario();
        permisoMock.setId(1L);
        permisoMock.setDocumentoId(DOCUMENTO_ID);
        permisoMock.setUsuarioId(USUARIO_ID);
        permisoMock.setOrganizacionId(ORGANIZACION_ID);
        permisoMock.setNivelAcceso(NivelAcceso.LECTURA);
        permisoMock.setFechaAsignacion(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Debe crear permiso nuevo exitosamente")
    void should_CreateNewPermiso_When_NotExists() {
        // Arrange
        when(validator.validarNivelAccesoCodigo("LECTURA")).thenReturn(CodigoNivelAcceso.LECTURA);
        when(permisoRepository.findByDocumentoIdAndUsuarioId(DOCUMENTO_ID, USUARIO_ID))
                .thenReturn(Optional.empty());
        when(permisoRepository.save(any(PermisoDocumentoUsuario.class))).thenReturn(permisoMock);

        // Act
        PermisoDocumentoUsuario resultado = service.crearOActualizarPermiso(
                DOCUMENTO_ID, createDTO, ORGANIZACION_ID, ADMIN_ID
        );

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getDocumentoId()).isEqualTo(DOCUMENTO_ID);
        assertThat(resultado.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(resultado.getNivelAcceso()).isEqualTo(NivelAcceso.LECTURA);

        // Verificar validaciones
        verify(validator).validarDocumentoEnOrganizacion(DOCUMENTO_ID, ORGANIZACION_ID);
        verify(validator).validarAdministrador(ADMIN_ID, DOCUMENTO_ID, ORGANIZACION_ID);
        verify(validator).validarUsuarioPerteneceOrganizacion(USUARIO_ID, ORGANIZACION_ID);

        // Verificar que se guardó
        verify(permisoRepository).save(any(PermisoDocumentoUsuario.class));

        // Verificar que se publicó evento
        ArgumentCaptor<PermisoDocumentoUsuarioCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(PermisoDocumentoUsuarioCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDocumentoId()).isEqualTo(DOCUMENTO_ID);
    }

    @Test
    @DisplayName("Debe actualizar permiso existente")
    void should_UpdatePermiso_When_AlreadyExists() {
        // Arrange
        PermisoDocumentoUsuario permisoExistente = new PermisoDocumentoUsuario();
        permisoExistente.setId(1L);
        permisoExistente.setDocumentoId(DOCUMENTO_ID);
        permisoExistente.setUsuarioId(USUARIO_ID);
        permisoExistente.setOrganizacionId(ORGANIZACION_ID);
        permisoExistente.setNivelAcceso(NivelAcceso.LECTURA);

        createDTO.setNivelAccesoCodigo("ESCRITURA");

        when(validator.validarNivelAccesoCodigo("ESCRITURA")).thenReturn(CodigoNivelAcceso.ESCRITURA);
        when(permisoRepository.findByDocumentoIdAndUsuarioId(DOCUMENTO_ID, USUARIO_ID))
                .thenReturn(Optional.of(permisoExistente));
        when(permisoRepository.save(any(PermisoDocumentoUsuario.class))).thenReturn(permisoExistente);

        // Act
        PermisoDocumentoUsuario resultado = service.crearOActualizarPermiso(
                DOCUMENTO_ID, createDTO, ORGANIZACION_ID, ADMIN_ID
        );

        // Assert
        assertThat(resultado).isNotNull();
        verify(permisoRepository).save(any(PermisoDocumentoUsuario.class));
    }

    @Test
    @DisplayName("Debe fallar cuando documento no existe")
    void should_ThrowException_When_DocumentoNotFound() {
        // Arrange
        doThrow(new ResourceNotFoundException("Documento", DOCUMENTO_ID))
                .when(validator).validarDocumentoEnOrganizacion(DOCUMENTO_ID, ORGANIZACION_ID);

        // Act & Assert
        assertThatThrownBy(() -> service.crearOActualizarPermiso(
                DOCUMENTO_ID, createDTO, ORGANIZACION_ID, ADMIN_ID
        )).isInstanceOf(ResourceNotFoundException.class);

        verify(permisoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Debe revocar permiso exitosamente")
    void should_RevokePermiso_When_Exists() {
        // Arrange
        when(permisoRepository.findByDocumentoIdAndUsuarioId(DOCUMENTO_ID, USUARIO_ID))
                .thenReturn(Optional.of(permisoMock));
        doNothing().when(permisoRepository).delete(any(PermisoDocumentoUsuario.class));

        // Act
        service.revocarPermiso(DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, ADMIN_ID);

        // Assert
        verify(validator).validarDocumentoEnOrganizacion(DOCUMENTO_ID, ORGANIZACION_ID);
        verify(validator).validarAdministrador(ADMIN_ID, DOCUMENTO_ID, ORGANIZACION_ID);
        verify(permisoRepository).delete(permisoMock);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Debe fallar al revocar permiso que no existe")
    void should_ThrowException_When_PermisoNotFound() {
        // Arrange
        when(permisoRepository.findByDocumentoIdAndUsuarioId(DOCUMENTO_ID, USUARIO_ID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.revocarPermiso(
                DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, ADMIN_ID
        )).isInstanceOf(ResourceNotFoundException.class);

        verify(permisoRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
