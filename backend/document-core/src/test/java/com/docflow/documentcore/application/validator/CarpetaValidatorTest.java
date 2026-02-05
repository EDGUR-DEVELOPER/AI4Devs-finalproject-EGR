package com.docflow.documentcore.application.validator;

import com.docflow.documentcore.domain.exception.carpeta.CarpetaNoVaciaException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaRaizNoEliminableException;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarpetaValidator - Tests Unitarios")
class CarpetaValidatorTest {

    @Mock
    private ICarpetaRepository carpetaRepository;

    @InjectMocks
    private CarpetaValidator validator;

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
    @DisplayName("should_Pass_When_CarpetaIsEmpty")
    void should_Pass_When_CarpetaIsEmpty() {
        when(carpetaRepository.estaVacia(carpetaId, organizacionId)).thenReturn(true);

        validator.validarCarpetaVacia(carpetaId, organizacionId);
    }

    @Test
    @DisplayName("should_Throw_CarpetaNoVacia_When_HasSubcarpetas")
    void should_Throw_CarpetaNoVacia_When_HasSubcarpetas() {
        when(carpetaRepository.estaVacia(carpetaId, organizacionId)).thenReturn(false);
        when(carpetaRepository.contarSubcarpetasActivas(carpetaId, organizacionId)).thenReturn(2);
        when(carpetaRepository.contarDocumentosActivos(carpetaId, organizacionId)).thenReturn(0);

        assertThatThrownBy(() -> validator.validarCarpetaVacia(carpetaId, organizacionId))
                .isInstanceOf(CarpetaNoVaciaException.class)
                .hasMessageContaining("no está vacía");
    }

    @Test
    @DisplayName("should_Throw_CarpetaNoVacia_When_HasDocumentos")
    void should_Throw_CarpetaNoVacia_When_HasDocumentos() {
        when(carpetaRepository.estaVacia(carpetaId, organizacionId)).thenReturn(false);
        when(carpetaRepository.contarSubcarpetasActivas(carpetaId, organizacionId)).thenReturn(0);
        when(carpetaRepository.contarDocumentosActivos(carpetaId, organizacionId)).thenReturn(3);

        assertThatThrownBy(() -> validator.validarCarpetaVacia(carpetaId, organizacionId))
                .isInstanceOf(CarpetaNoVaciaException.class)
                .hasMessageContaining("no está vacía");
    }

    @Test
    @DisplayName("should_Throw_CarpetaNoVacia_When_HasBoth")
    void should_Throw_CarpetaNoVacia_When_HasBoth() {
        when(carpetaRepository.estaVacia(carpetaId, organizacionId)).thenReturn(false);
        when(carpetaRepository.contarSubcarpetasActivas(carpetaId, organizacionId)).thenReturn(1);
        when(carpetaRepository.contarDocumentosActivos(carpetaId, organizacionId)).thenReturn(4);

        assertThatThrownBy(() -> validator.validarCarpetaVacia(carpetaId, organizacionId))
                .isInstanceOf(CarpetaNoVaciaException.class)
                .hasMessageContaining("no está vacía");
    }

    @Test
    @DisplayName("should_Pass_When_NotRootFolder")
    void should_Pass_When_NotRootFolder() {
        Carpeta carpeta = Carpeta.builder()
                .id(carpetaId)
                .organizacionId(organizacionId)
                .carpetaPadreId(10L)
                .nombre("Test")
                .creadoPor(usuarioId)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();

        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId))
                .thenReturn(Optional.of(carpeta));

        validator.validarNoEsRaiz(carpetaId, organizacionId);
    }

    @Test
    @DisplayName("should_Throw_CarpetaRaizNoEliminable_When_IsRootFolder")
    void should_Throw_CarpetaRaizNoEliminable_When_IsRootFolder() {
        Carpeta carpeta = Carpeta.builder()
                .id(carpetaId)
                .organizacionId(organizacionId)
                .carpetaPadreId(null)
                .nombre("Root")
                .creadoPor(usuarioId)
                .fechaCreacion(Instant.now())
                .fechaActualizacion(Instant.now())
                .build();

        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId))
                .thenReturn(Optional.of(carpeta));

        assertThatThrownBy(() -> validator.validarNoEsRaiz(carpetaId, organizacionId))
                .isInstanceOf(CarpetaRaizNoEliminableException.class)
                .hasMessageContaining("carpeta raíz");
    }

    @Test
    @DisplayName("should_Throw_CarpetaNotFound_When_CarpetaDoesNotExist")
    void should_Throw_CarpetaNotFound_When_CarpetaDoesNotExist() {
        when(carpetaRepository.obtenerPorId(carpetaId, organizacionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validarNoEsRaiz(carpetaId, organizacionId))
                .isInstanceOf(CarpetaNotFoundException.class);
    }
}
