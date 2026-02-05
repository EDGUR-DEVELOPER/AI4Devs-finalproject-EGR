package com.docflow.documentcore.infrastructure;

import com.docflow.documentcore.application.mapper.CarpetaDtoMapper;
import com.docflow.documentcore.application.service.CarpetaService;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNoVaciaException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaNotFoundException;
import com.docflow.documentcore.domain.exception.carpeta.CarpetaRaizNoEliminableException;
import com.docflow.documentcore.infrastructure.adapter.controller.CarpetaController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CarpetaController.class)
@DisplayName("CarpetaController - Tests Unitarios")
class CarpetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CarpetaService carpetaService;

    @MockitoBean
    private CarpetaDtoMapper carpetaDtoMapper;

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
