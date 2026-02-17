package com.docflow.documentcore.infrastructure;

import com.docflow.documentcore.application.service.DocumentService;
import com.docflow.documentcore.application.service.DocumentoMoverService;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.DocumentAlreadyDeletedException;
import com.docflow.documentcore.domain.exception.GlobalExceptionHandler;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.infrastructure.adapter.controller.DocumentoController;
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
 * Tests unitarios para el endpoint de eliminacion de documentos.
 *
 * <p>Usa MockMvcBuilders.standaloneSetup() con GlobalExceptionHandler
 * para validar el mapeo de errores RFC 7807.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoController - Delete Tests")
class DocumentoDeleteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DocumentoMoverService documentoMoverService;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentoController documentoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(documentoController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .build();
    }

    @Test
    @DisplayName("should_Return_204_When_DocumentDeleted")
    void shouldReturn204WhenDocumentDeleted() throws Exception {
        doNothing().when(documentService).deleteDocument(10L);

        mockMvc.perform(delete("/api/documentos/10"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should_Return_404_When_DocumentNotFound")
    void shouldReturn404WhenDocumentNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Documento", 10L))
            .when(documentService).deleteDocument(10L);

        mockMvc.perform(delete("/api/documentos/10"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should_Return_403_When_UserHasNoPermission")
    void shouldReturn403WhenUserHasNoPermission() throws Exception {
        doThrow(new AccessDeniedException("No tiene permisos para eliminar este documento"))
            .when(documentService).deleteDocument(10L);

        mockMvc.perform(delete("/api/documentos/10"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should_Return_409_When_DocumentAlreadyDeleted")
    void shouldReturn409WhenDocumentAlreadyDeleted() throws Exception {
        doThrow(new DocumentAlreadyDeletedException(10L))
            .when(documentService).deleteDocument(10L);

        mockMvc.perform(delete("/api/documentos/10"))
            .andExpect(status().isConflict());
    }
}
