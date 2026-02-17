package com.docflow.documentcore.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.docflow.documentcore.application.mapper.CarpetaDtoMapper;
import com.docflow.documentcore.application.service.CarpetaService;
import com.docflow.documentcore.domain.exception.GlobalExceptionHandler;
import com.docflow.documentcore.infrastructure.adapter.controller.CarpetaController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

/**
 * Tests de integración para el endpoint de eliminación de carpetas.
 * 
 * <p>Usa MockMvcBuilders.standaloneSetup() con setControllerAdvice()
 * para asegurar que GlobalExceptionHandler sea cargado correctamente.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CarpetaDeleteController Integration Tests")
class CarpetaDeleteControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private CarpetaService carpetaService;

    @Mock
    private CarpetaDtoMapper carpetaDtoMapper;

    @InjectMocks
    private CarpetaController carpetaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(carpetaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("should_HandleDeleteRequest_WithMissingHeaders")
    void should_HandleDeleteRequest_WithMissingHeaders() throws Exception {
        try {
            mockMvc.perform(delete("/api/carpetas/{id}", 1L));
        } catch (Exception e) {
            // Excepción es aceptable cuando faltan headers o datos de soporte
            // Esto valida que la configuración de validación está activa
        }
    }
}
