package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import com.docflow.documentcore.domain.repository.INivelAccesoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NivelAccesoService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NivelAccesoService Unit Tests")
class NivelAccesoServiceTest {

    @Mock
    private INivelAccesoRepository repository;

    @InjectMocks
    private NivelAccesoService service;

    private Long testId;
    private NivelAcceso testNivel;

    @BeforeEach
    void setUp() {
        testId = new Random().nextLong();
        testNivel = NivelAcceso.builder()
                .id(testId)
                .codigo(CodigoNivelAcceso.LECTURA)
                .nombre("Lectura")
                .accionesPermitidas(Arrays.asList("ver", "listar", "descargar"))
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("should_ReturnNivelAcceso_When_GetByCodigoWithExistingCodigo")
    void should_ReturnNivelAcceso_When_GetByCodigoWithExistingCodigo() {
        // Given
        when(repository.findByCodigo(CodigoNivelAcceso.LECTURA)).thenReturn(Optional.of(testNivel));

        // When
        NivelAcceso result = service.getByCodigo(CodigoNivelAcceso.LECTURA);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCodigo()).isEqualTo(CodigoNivelAcceso.LECTURA);
        verify(repository, times(1)).findByCodigo(CodigoNivelAcceso.LECTURA);
    }

    @Test
    @DisplayName("should_ThrowException_When_GetByCodigoWithNonExistingCodigo")
    void should_ThrowException_When_GetByCodigoWithNonExistingCodigo() {
        // Given
        when(repository.findByCodigo(CodigoNivelAcceso.ADMINISTRACION)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getByCodigo(CodigoNivelAcceso.ADMINISTRACION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access level not found");
    }

    @Test
    @DisplayName("should_ReturnActiveList_When_ListAllActive")
    void should_ReturnActiveList_When_ListAllActive() {
        // Given
        List<NivelAcceso> mockList = Arrays.asList(testNivel);
        when(repository.findAllActiveOrderByOrden()).thenReturn(mockList);

        // When
        List<NivelAcceso> result = service.listAllActive();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActivo()).isTrue();
        verify(repository, times(1)).findAllActiveOrderByOrden();
    }
}
