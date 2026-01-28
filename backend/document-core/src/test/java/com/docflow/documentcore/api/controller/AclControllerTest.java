package com.docflow.documentcore.api.controller;

import com.docflow.documentcore.api.dto.NivelAccesoDTO;
import com.docflow.documentcore.api.mapper.NivelAccesoDtoMapper;
import com.docflow.documentcore.application.service.NivelAccesoService;
import com.docflow.documentcore.application.validator.NivelAccesoValidator;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AclController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AclController Unit Tests")
class AclControllerTest {

    @Mock
    private NivelAccesoService service;

    @Mock
    private NivelAccesoValidator validator;

    @Mock
    private NivelAccesoDtoMapper mapper;

    @InjectMocks
    private AclController controller;

    private UUID testId;
    private NivelAcceso testNivel;
    private NivelAccesoDTO testDto;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testNivel = NivelAcceso.builder()
                .id(testId)
                .codigo(CodigoNivelAcceso.LECTURA)
                .nombre("Lectura")
                .activo(true)
                .build();

        testDto = new NivelAccesoDTO();
        testDto.setId(testId);
        testDto.setCodigo("LECTURA");
        testDto.setNombre("Lectura");
        testDto.setActivo(true);
    }

    @Test
    @DisplayName("should_ReturnActiveList_When_ListActiveAccessLevels")
    void should_ReturnActiveList_When_ListActiveAccessLevels() {
        // Given
        List<NivelAcceso> niveles = Arrays.asList(testNivel);
        when(service.listAllActive()).thenReturn(niveles);
        when(mapper.toDto(testNivel)).thenReturn(testDto);

        // When
        ResponseEntity<List<NivelAccesoDTO>> response = controller.listActiveAccessLevels();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getCodigo()).isEqualTo("LECTURA");
        verify(service, times(1)).listAllActive();
    }

    @Test
    @DisplayName("should_ReturnAllList_When_ListAllAccessLevels")
    void should_ReturnAllList_When_ListAllAccessLevels() {
        // Given
        List<NivelAcceso> niveles = Arrays.asList(testNivel);
        when(service.listAll()).thenReturn(niveles);
        when(mapper.toDto(testNivel)).thenReturn(testDto);

        // When
        ResponseEntity<List<NivelAccesoDTO>> response = controller.listAllAccessLevels();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(service, times(1)).listAll();
    }

    @Test
    @DisplayName("should_ReturnNivelAcceso_When_GetAccessLevelById")
    void should_ReturnNivelAcceso_When_GetAccessLevelById() {
        // Given
        doNothing().when(validator).validateExistsById(testId);
        when(service.getById(testId)).thenReturn(testNivel);
        when(mapper.toDto(testNivel)).thenReturn(testDto);

        // When
        ResponseEntity<NivelAccesoDTO> response = controller.getAccessLevelById(testId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(testId);
        verify(validator, times(1)).validateExistsById(testId);
        verify(service, times(1)).getById(testId);
    }

    @Test
    @DisplayName("should_ThrowException_When_GetAccessLevelByIdNotFound")
    void should_ThrowException_When_GetAccessLevelByIdNotFound() {
        // Given
        UUID nonExistingId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Access level not found"))
                .when(validator).validateExistsById(nonExistingId);

        // When / Then
        assertThatThrownBy(() -> controller.getAccessLevelById(nonExistingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access level not found");
    }

    @Test
    @DisplayName("should_ReturnNivelAcceso_When_GetAccessLevelByCodigo")
    void should_ReturnNivelAcceso_When_GetAccessLevelByCodigo() {
        // Given
        when(validator.validateCodigoFormat("LECTURA")).thenReturn(CodigoNivelAcceso.LECTURA);
        when(service.getByCodigo(CodigoNivelAcceso.LECTURA)).thenReturn(testNivel);
        when(mapper.toDto(testNivel)).thenReturn(testDto);

        // When
        ResponseEntity<NivelAccesoDTO> response = controller.getAccessLevelByCodigo("LECTURA");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCodigo()).isEqualTo("LECTURA");
        verify(validator, times(1)).validateCodigoFormat("LECTURA");
        verify(service, times(1)).getByCodigo(CodigoNivelAcceso.LECTURA);
    }

    @Test
    @DisplayName("should_ThrowException_When_GetAccessLevelByInvalidCodigo")
    void should_ThrowException_When_GetAccessLevelByInvalidCodigo() {
        // Given
        when(validator.validateCodigoFormat("INVALID"))
                .thenThrow(new IllegalArgumentException("Invalid codigo value"));

        // When / Then
        assertThatThrownBy(() -> controller.getAccessLevelByCodigo("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid codigo value");
    }

    @Test
    @DisplayName("should_ReturnTrue_When_CheckPermissionForAllowedAction")
    void should_ReturnTrue_When_CheckPermissionForAllowedAction() {
        // Given
        when(service.isAccionPermitida(testId, "ver")).thenReturn(true);

        // When
        ResponseEntity<Boolean> response = controller.checkPermission(testId, "ver");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
        verify(service, times(1)).isAccionPermitida(testId, "ver");
    }

    @Test
    @DisplayName("should_ReturnFalse_When_CheckPermissionForForbiddenAction")
    void should_ReturnFalse_When_CheckPermissionForForbiddenAction() {
        // Given
        when(service.isAccionPermitida(testId, "eliminar")).thenReturn(false);

        // When
        ResponseEntity<Boolean> response = controller.checkPermission(testId, "eliminar");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
        verify(service, times(1)).isAccionPermitida(testId, "eliminar");
    }
}
