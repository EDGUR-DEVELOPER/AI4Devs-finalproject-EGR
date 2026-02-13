package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.validator.NivelAccesoValidator;
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

import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NivelAccesoValidator
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NivelAccesoValidator Unit Tests")
class NivelAccesoValidatorTest {

    @Mock
    private INivelAccesoRepository repository;

    @InjectMocks
    private NivelAccesoValidator validator;

    private Long testId;
    private NivelAcceso testNivel;

    @BeforeEach
    void setUp() {
        testId = new Random().nextLong();
        testNivel = NivelAcceso.builder()
                .id(testId)
                .codigo(CodigoNivelAcceso.LECTURA)
                .nombre("Lectura")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("should_PassValidation_When_CodigoExists")
    void should_PassValidation_When_CodigoExists() {
        // Given
        when(repository.existsByCodigo(CodigoNivelAcceso.LECTURA)).thenReturn(true);

        // When / Then
        validator.validateExistsByCodigo(CodigoNivelAcceso.LECTURA);
        verify(repository, times(1)).existsByCodigo(CodigoNivelAcceso.LECTURA);
    }

    @Test
    @DisplayName("should_ThrowException_When_CodigoNotFound")
    void should_ThrowException_When_CodigoNotFound() {
        // Given
        when(repository.existsByCodigo(CodigoNivelAcceso.ESCRITURA)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> validator.validateExistsByCodigo(CodigoNivelAcceso.ESCRITURA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access level not found with codigo");
    }

    @Test
    @DisplayName("should_ThrowException_When_CodigoIsNull")
    void should_ThrowException_When_CodigoIsNull() {
        // When / Then
        assertThatThrownBy(() -> validator.validateExistsByCodigo(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codigo cannot be null");
    }

    @Test
    @DisplayName("should_ReturnValidCodigo_When_StringIsValid")
    void should_ReturnValidCodigo_When_StringIsValid() {
        // When
        CodigoNivelAcceso result = validator.validateCodigoFormat("LECTURA");

        // Then
        assertThat(result).isEqualTo(CodigoNivelAcceso.LECTURA);
    }

    @Test
    @DisplayName("should_ThrowException_When_CodigoStringIsInvalid")
    void should_ThrowException_When_CodigoStringIsInvalid() {
        // When / Then
        assertThatThrownBy(() -> validator.validateCodigoFormat("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid codigo value");
    }

    @Test
    @DisplayName("should_ThrowException_When_CodigoStringIsEmpty")
    void should_ThrowException_When_CodigoStringIsEmpty() {
        // When / Then
        assertThatThrownBy(() -> validator.validateCodigoFormat(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    @DisplayName("should_ThrowException_When_CodigoStringIsNull")
    void should_ThrowException_When_CodigoStringIsNull() {
        // When / Then
        assertThatThrownBy(() -> validator.validateCodigoFormat(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }
}
