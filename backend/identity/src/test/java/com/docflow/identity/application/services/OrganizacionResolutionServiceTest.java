package com.docflow.identity.application.services;

import com.docflow.identity.application.ports.output.UsuarioOrganizacionRepository;
import com.docflow.identity.domain.exceptions.OrganizacionConfigInvalidaException;
import com.docflow.identity.domain.exceptions.SinOrganizacionException;
import com.docflow.identity.domain.model.EstadoMembresia;
import com.docflow.identity.domain.model.UsuarioOrganizacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests para OrganizacionResolutionService.
 * Cubre todos los escenarios del criterio de aceptación US-AUTH-001.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizacionResolutionService - Resolución de Organización en Login")
class OrganizacionResolutionServiceTest {

    @Mock
    private UsuarioOrganizacionRepository repository;

    @InjectMocks
    private OrganizacionResolutionService service;

    @Nested
    @DisplayName("Escenario 1: Usuario con 1 organización activa")
    class UnaOrganizacionActiva {

        @Test
        @DisplayName("Debe retornar la única organización activa")
        void debeRetornarLaUnicaOrganizacion() {
            // Given
            Long usuarioId = 1L;
            var membresia = crearMembresia(usuarioId, 100, true);
            when(repository.findMembresiasActivasOrdenadas(usuarioId))
                .thenReturn(List.of(membresia));

            // When
            var resultado = service.resolveLoginOrganization(usuarioId);

            // Then
            assertThat(resultado).isEqualTo(100);
        }

        @Test
        @DisplayName("Debe retornar la organización incluso si no está marcada como predeterminada")
        void debeRetornarOrganizacionSinMarcaPredeterminada() {
            // Given
            Long usuarioId = 1L;
            var membresia = crearMembresia(usuarioId, 100, false);
            when(repository.findMembresiasActivasOrdenadas(usuarioId))
                .thenReturn(List.of(membresia));

            // When
            var resultado = service.resolveLoginOrganization(usuarioId);

            // Then
            assertThat(resultado).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Escenario 1b: Usuario con 2+ organizaciones y predeterminada")
    class VariasConPredeterminada {

        @Test
        @DisplayName("Con 2 organizaciones debe retornar la predeterminada")
        void conDosOrganizacionesDebeRetornarLaPredeterminada() {
            // Given
            Long usuarioId = 2L;
            var membresias = List.of(
                crearMembresia(usuarioId, 200, true),  // Predeterminada
                crearMembresia(usuarioId, 201, false)
            );
            when(repository.findMembresiasActivasOrdenadas(usuarioId))
                .thenReturn(membresias);

            // When
            var resultado = service.resolveLoginOrganization(usuarioId);

            // Then
            assertThat(resultado).isEqualTo(200);
        }

        @Test
        @DisplayName("Con 3+ organizaciones debe retornar la predeterminada")
        void conTresOMasOrganizacionesDebeRetornarLaPredeterminada() {
            // Given
            Long usuarioId = 2L;
            var membresias = List.of(
                crearMembresia(usuarioId, 200, false),
                crearMembresia(usuarioId, 201, true),  // Predeterminada
                crearMembresia(usuarioId, 202, false)
            );
            when(repository.findMembresiasActivasOrdenadas(usuarioId))
                .thenReturn(membresias);

            // When
            var resultado = service.resolveLoginOrganization(usuarioId);

            // Then
            assertThat(resultado).isEqualTo(201);
        }
    }

    @Nested
    @DisplayName("Escenario 2: Usuario con 2+ organizaciones sin predeterminada")
    class VariasSinPredeterminada {

        @Test
        @DisplayName("Con 2 organizaciones sin predeterminada debe lanzar OrganizacionConfigInvalida")
        void conDosOrganizacionesSinPredeterminadaDebeLanzarExcepcion() {
            // Given
            Long usuarioId = 3L;
            var membresias = List.of(
                crearMembresia(usuarioId, 300, false),
                crearMembresia(usuarioId, 301, false)
            );
            when(repository.findMembresiasActivasOrdenadas(usuarioId))
                .thenReturn(membresias);

            // When & Then
            assertThatThrownBy(() -> service.resolveLoginOrganization(usuarioId))
                .isInstanceOf(OrganizacionConfigInvalidaException.class)
                .hasMessageContaining("múltiples organizaciones sin predeterminada");
        }

        @Test
        @DisplayName("Con 3+ organizaciones sin predeterminada debe lanzar OrganizacionConfigInvalida")
        void conTresOMasOrganizacionesSinPredeterminadaDebeLanzarExcepcion() {
            // Given
            Long usuarioId = 3L;
            var membresias = List.of(
                crearMembresia(usuarioId, 300, false),
                crearMembresia(usuarioId, 301, false),
                crearMembresia(usuarioId, 302, false)
            );
            when(repository.findMembresiasActivasOrdenadas(usuarioId))
                .thenReturn(membresias);

            // When & Then
            assertThatThrownBy(() -> service.resolveLoginOrganization(usuarioId))
                .isInstanceOf(OrganizacionConfigInvalidaException.class)
                .hasMessageContaining("múltiples organizaciones sin predeterminada");
        }
    }

    @Nested
    @DisplayName("Escenario 4: Usuario sin organizaciones activas")
    class SinOrganizaciones {

        @Test
        @DisplayName("Sin organizaciones activas debe lanzar SinOrganizacion")
        void sinOrganizacionesDebeLanzarExcepcion() {
            // Given
            Long usuarioId = 4L;
            when(repository.findMembresiasActivasOrdenadas(usuarioId))
                .thenReturn(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> service.resolveLoginOrganization(usuarioId))
                .isInstanceOf(SinOrganizacionException.class)
                .hasMessageContaining("no tiene organizaciones activas");
        }
    }

    @Nested
    @DisplayName("Casos borde y defensive programming")
    class CasosBorde {

        @Test
        @DisplayName("Con múltiples predeterminadas debe lanzar OrganizacionConfigInvalida")
        void conMultiplesPredeterminadasDebeLanzarConfigInvalida() {
            // Given: Esto no debería pasar por el índice único, pero defensive programming
            Long usuarioId = 5L;
            var membresias = List.of(
                crearMembresia(usuarioId, 500, true),
                crearMembresia(usuarioId, 501, true)
            );
            when(repository.findMembresiasActivasOrdenadas(usuarioId))
                .thenReturn(membresias);

            // When & Then
            assertThatThrownBy(() -> service.resolveLoginOrganization(usuarioId))
                .isInstanceOf(OrganizacionConfigInvalidaException.class)
                .hasMessageContaining("múltiples predeterminadas");
        }

        @Test
        @DisplayName("Con 3 predeterminadas debe lanzar OrganizacionConfigInvalida")
        void conTresPredeterminadasDebeLanzarConfigInvalida() {
            // Given
            Long usuarioId = 5L;
            var membresias = List.of(
                crearMembresia(usuarioId, 500, true),
                crearMembresia(usuarioId, 501, true),
                crearMembresia(usuarioId, 502, true)
            );
            when(repository.findMembresiasActivasOrdenadas(usuarioId))
                .thenReturn(membresias);

            // When & Then
            assertThatThrownBy(() -> service.resolveLoginOrganization(usuarioId))
                .isInstanceOf(OrganizacionConfigInvalidaException.class)
                .hasMessageContaining("múltiples predeterminadas");
        }
    }

    // Helper methods

    private UsuarioOrganizacion crearMembresia(
            Long usuarioId,
            Integer orgId,
            boolean esPredeterminada) {
        var uo = new UsuarioOrganizacion();
        uo.setUsuarioId(usuarioId);
        uo.setOrganizacionId(orgId);
        uo.setEstado(EstadoMembresia.ACTIVO);
        uo.setEsPredeterminada(esPredeterminada);
        return uo;
    }
}
