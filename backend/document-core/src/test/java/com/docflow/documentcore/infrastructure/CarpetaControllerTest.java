package com.docflow.documentcore.infrastructure;

import com.docflow.documentcore.application.mapper.CarpetaDtoMapper;
import com.docflow.documentcore.application.service.CarpetaService;
import com.docflow.documentcore.domain.exception.GlobalExceptionHandler;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNoVaciaException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaRaizNoEliminableException;
import com.docflow.documentcore.infrastructure.adapter.controller.CarpetaController;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitarios para CarpetaController.
 * 
 * <p>Usa MockMvcBuilders.standaloneSetup() en lugar de @WebMvcTest
 * para control total del setup y asegurar que GlobalExceptionHandler
 * se carga correctamente con setControllerAdvice().</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CarpetaController - Tests Unitarios")
class CarpetaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CarpetaService carpetaService;

    @Mock
    private CarpetaDtoMapper carpetaDtoMapper;

    @InjectMocks
    private CarpetaController carpetaController;

    @BeforeEach
    void setUp() {
        // Configurar MockMvc con:
        // 1. Controller específico
        // 2. GlobalExceptionHandler registrado como @ControllerAdvice
        // 3. Jackson JSON converter para serialización
        mockMvc = MockMvcBuilders
                .standaloneSetup(carpetaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("should_Return_204_When_EmptyFolderDeleted")
    void should_Return_204_When_EmptyFolderDeleted() throws Exception {
        doNothing().when(carpetaService).eliminarCarpeta(10L, 99L, 1L);

        mockMvc.perform(delete("/api/carpetas/10")
                        .header("X-Organization-Id", "1")
                        .header("X-User-Id", "99"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should_Return_404_When_CarpetaNotFound")
    void should_Return_404_When_CarpetaNotFound() throws Exception {
        doThrow(new CarpetaNotFoundException(10L))
                .when(carpetaService).eliminarCarpeta(10L, 99L, 1L);

        mockMvc.perform(delete("/api/carpetas/10")
                        .header("X-Organization-Id", "1")
                        .header("X-User-Id", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should_Return_400_When_RootFolderDeletion")
    void should_Return_400_When_RootFolderDeletion() throws Exception {
        doThrow(new CarpetaRaizNoEliminableException(10L))
                .when(carpetaService).eliminarCarpeta(10L, 99L, 1L);

        mockMvc.perform(delete("/api/carpetas/10")
                        .header("X-Organization-Id", "1")
                        .header("X-User-Id", "99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should_Return_409_When_CarpetaNotEmpty")
    void should_Return_409_When_CarpetaNotEmpty() throws Exception {
        doThrow(new CarpetaNoVaciaException(10L, 2, 1))
                .when(carpetaService).eliminarCarpeta(10L, 99L, 1L);

        mockMvc.perform(delete("/api/carpetas/10")
                        .header("X-Organization-Id", "1")
                        .header("X-User-Id", "99"))
                .andExpect(status().isConflict());
    }
}
