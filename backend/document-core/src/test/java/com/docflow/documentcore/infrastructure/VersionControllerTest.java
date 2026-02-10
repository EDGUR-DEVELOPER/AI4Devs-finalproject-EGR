package com.docflow.documentcore.infrastructure;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.docflow.documentcore.application.dto.DocumentoResponse;
import com.docflow.documentcore.application.dto.VersionItemResponse;
import com.docflow.documentcore.application.dto.VersionListResponse;
import com.docflow.documentcore.application.service.DocumentoVersionChangeService;
import com.docflow.documentcore.application.service.DocumentoVersionService;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.GlobalExceptionHandler;
import com.docflow.documentcore.domain.exception.InsufficientPermissionsException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.exception.VersionNotBelongToDocumentException;
import com.docflow.documentcore.infrastructure.adapter.controller.VersionController;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = VersionController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("VersionController - Tests Unitarios")
class VersionControllerTest {

    private static final Long DOCUMENTO_ID = 100L;
    private static final Long USUARIO_ID = 99L;
    private static final Long ORGANIZACION_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentoVersionService documentoVersionService;
    
    @MockitoBean
    private DocumentoVersionChangeService documentoVersionChangeService;

    @Test
    @DisplayName("should_Return200_When_ListVersions")
    void shouldReturn200WhenListVersions() throws Exception {
        VersionItemResponse version1 = VersionItemResponse.builder()
            .id(200L)
            .numeroSecuencial(1)
            .tamanioBytes(1024L)
            .hashContenido("hash1")
            .comentarioCambio("Inicial")
            .creadoPor(new VersionItemResponse.CreadorInfo(
                USUARIO_ID,
                "Usuario Test",
                "test@docflow.com"))
            .fechaCreacion(OffsetDateTime.parse("2024-01-01T10:00:00Z"))
            .descargas(1)
            .ultimaDescargaEn(OffsetDateTime.parse("2024-01-02T10:00:00Z"))
            .esVersionActual(false)
            .build();

        VersionItemResponse version2 = VersionItemResponse.builder()
            .id(201L)
            .numeroSecuencial(2)
            .tamanioBytes(2048L)
            .hashContenido("hash2")
            .comentarioCambio("Actualizacion")
            .creadoPor(new VersionItemResponse.CreadorInfo(
                USUARIO_ID,
                "Usuario Test",
                "test@docflow.com"))
            .fechaCreacion(OffsetDateTime.parse("2024-01-03T10:00:00Z"))
            .descargas(3)
            .ultimaDescargaEn(null)
            .esVersionActual(true)
            .build();

        VersionListResponse response = VersionListResponse.builder()
            .versiones(List.of(version1, version2))
            .documentoId(DOCUMENTO_ID)
            .totalVersiones(2)
            .paginacion(VersionListResponse.PaginacionInfo.builder()
                .paginaActual(1)
                .tamanio(20)
                .totalPaginas(1)
                .totalElementos(2)
                .primeraPagina(true)
                .ultimaPagina(true)
                .build())
            .build();

        when(documentoVersionService.listarVersiones(DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, 1, 20))
            .thenReturn(response);

        mockMvc.perform(get("/api/documentos/{documentoId}/versiones", DOCUMENTO_ID)
                .queryParam("pagina", "1")
                .header("X-Organization-Id", ORGANIZACION_ID)
                .header("X-User-Id", USUARIO_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.documentoId").value(DOCUMENTO_ID))
            .andExpect(jsonPath("$.totalVersiones").value(2))
            .andExpect(jsonPath("$.versiones.length()").value(2))
            .andExpect(jsonPath("$.versiones[0].numeroSecuencial").value(1))
            .andExpect(jsonPath("$.versiones[1].esVersionActual").value(true))
            .andExpect(jsonPath("$.paginacion.paginaActual").value(1))
            .andExpect(jsonPath("$.paginacion.totalElementos").value(2));

        verify(documentoVersionService)
            .listarVersiones(DOCUMENTO_ID, USUARIO_ID, ORGANIZACION_ID, 1, 20);
    }

    @Test
    @DisplayName("should_Return400_When_InvalidPagination")
    void shouldReturn400WhenInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/documentos/{documentoId}/versiones", DOCUMENTO_ID)
                .queryParam("pagina", "0")
                .header("X-Organization-Id", ORGANIZACION_ID)
                .header("X-User-Id", USUARIO_ID))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should_Return404_When_DocumentNotFound")
    void shouldReturn404WhenDocumentNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Documento", DOCUMENTO_ID))
            .when(documentoVersionService)
            .listarVersiones(eq(DOCUMENTO_ID), eq(USUARIO_ID), eq(ORGANIZACION_ID), eq(1), eq(20));

        mockMvc.perform(get("/api/documentos/{documentoId}/versiones", DOCUMENTO_ID)
                .queryParam("pagina", "1")
                .header("X-Organization-Id", ORGANIZACION_ID)
                .header("X-User-Id", USUARIO_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should_Return403_When_UserHasNoPermission")
    void shouldReturn403WhenUserHasNoPermission() throws Exception {
        doThrow(new AccessDeniedException("No tiene permiso"))
            .when(documentoVersionService)
            .listarVersiones(eq(DOCUMENTO_ID), eq(USUARIO_ID), eq(ORGANIZACION_ID), eq(1), eq(20));

        mockMvc.perform(get("/api/documentos/{documentoId}/versiones", DOCUMENTO_ID)
                .queryParam("pagina", "1")
                .header("X-Organization-Id", ORGANIZACION_ID)
                .header("X-User-Id", USUARIO_ID))
            .andExpect(status().isForbidden());
    }
    
    @Nested
    @DisplayName("Cambiar Versión Actual (Rollback)")
    class CambiarVersionActualTests {
        
        private static final Long VERSION_ID = 201L;
        
        @Test
        @DisplayName("should_Return200_When_ChangeVersionSucceeds")
        void shouldReturn200WhenChangeVersionSucceeds() throws Exception {
            // Given
            DocumentoResponse.VersionInfoDTO versionInfo = DocumentoResponse.VersionInfoDTO.builder()
                .id(VERSION_ID)
                .numeroSecuencial(2)
                .tamanioBytes(2048L)
                .hashContenido("hash2")
                .fechaCreacion(OffsetDateTime.parse("2024-01-03T10:00:00Z"))
                .build();
                
            DocumentoResponse response = DocumentoResponse.builder()
                .id(DOCUMENTO_ID)
                .nombre("documento-test.pdf")
                .extension("pdf")
                .tipoContenido("application/pdf")
                .tamanioBytes(2048L)
                .carpetaId(10L)
                .versionActual(versionInfo)
                .numeroVersiones(5)
                .bloqueado(false)
                .etiquetas(new String[]{})
                .fechaCreacion(OffsetDateTime.parse("2024-01-01T10:00:00Z"))
                .fechaActualizacion(OffsetDateTime.now())
                .build();
            
            when(documentoVersionChangeService.cambiarVersionActual(
                eq(DOCUMENTO_ID), eq(VERSION_ID), eq(USUARIO_ID), eq(ORGANIZACION_ID)))
                .thenReturn(response);
            
            String requestBody = """
                {
                    "versionId": %d
                }
                """.formatted(VERSION_ID);
            
            // When & Then
            mockMvc.perform(patch("/api/documentos/{documentoId}/version-actual", DOCUMENTO_ID)
                    .header("X-Organization-Id", ORGANIZACION_ID)
                    .header("X-User-Id", USUARIO_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DOCUMENTO_ID))
                .andExpect(jsonPath("$.versionActual.id").value(VERSION_ID))
                .andExpect(jsonPath("$.versionActual.numeroSecuencial").value(2))
                .andExpect(jsonPath("$.numeroVersiones").value(5))
                .andExpect(jsonPath("$.fechaActualizacion").exists());
            
            verify(documentoVersionChangeService)
                .cambiarVersionActual(DOCUMENTO_ID, VERSION_ID, USUARIO_ID, ORGANIZACION_ID);
        }
        
        @Test
        @DisplayName("should_Return400_When_VersionIdIsNull")
        void shouldReturn400WhenVersionIdIsNull() throws Exception {
            String requestBody = """
                {
                }
                """;
            
            mockMvc.perform(patch("/api/documentos/{documentoId}/version-actual", DOCUMENTO_ID)
                    .header("X-Organization-Id", ORGANIZACION_ID)
                    .header("X-User-Id", USUARIO_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("should_Return400_When_VersionIdIsNegative")
        void shouldReturn400WhenVersionIdIsNegative() throws Exception {
            String requestBody = """
                {
                    "versionId": -1
                }
                """;
            
            mockMvc.perform(patch("/api/documentos/{documentoId}/version-actual", DOCUMENTO_ID)
                    .header("X-Organization-Id", ORGANIZACION_ID)
                    .header("X-User-Id", USUARIO_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("should_Return400_When_VersionNotBelongToDocument")
        void shouldReturn400WhenVersionNotBelongToDocument() throws Exception {
            doThrow(new VersionNotBelongToDocumentException(VERSION_ID, DOCUMENTO_ID))
                .when(documentoVersionChangeService)
                .cambiarVersionActual(eq(DOCUMENTO_ID), eq(VERSION_ID), eq(USUARIO_ID), eq(ORGANIZACION_ID));
            
            String requestBody = """
                {
                    "versionId": %d
                }
                """.formatted(VERSION_ID);
            
            mockMvc.perform(patch("/api/documentos/{documentoId}/version-actual", DOCUMENTO_ID)
                    .header("X-Organization-Id", ORGANIZACION_ID)
                    .header("X-User-Id", USUARIO_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Versión No Válida"));
        }
        
        @Test
        @DisplayName("should_Return403_When_UserLacksAdminPermission")
        void shouldReturn403WhenUserLacksAdminPermission() throws Exception {
            doThrow(new InsufficientPermissionsException("ADMINISTRACION", "DOCUMENTO"))
                .when(documentoVersionChangeService)
                .cambiarVersionActual(eq(DOCUMENTO_ID), eq(VERSION_ID), eq(USUARIO_ID), eq(ORGANIZACION_ID));
            
            String requestBody = """
                {
                    "versionId": %d
                }
                """.formatted(VERSION_ID);
            
            mockMvc.perform(patch("/api/documentos/{documentoId}/version-actual", DOCUMENTO_ID)
                    .header("X-Organization-Id", ORGANIZACION_ID)
                    .header("X-User-Id", USUARIO_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Permisos Insuficientes"));
        }
        
        @Test
        @DisplayName("should_Return404_When_DocumentNotFound")
        void shouldReturn404WhenDocumentNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Documento", DOCUMENTO_ID))
                .when(documentoVersionChangeService)
                .cambiarVersionActual(eq(DOCUMENTO_ID), eq(VERSION_ID), eq(USUARIO_ID), eq(ORGANIZACION_ID));
            
            String requestBody = """
                {
                    "versionId": %d
                }
                """.formatted(VERSION_ID);
            
            mockMvc.perform(patch("/api/documentos/{documentoId}/version-actual", DOCUMENTO_ID)
                    .header("X-Organization-Id", ORGANIZACION_ID)
                    .header("X-User-Id", USUARIO_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound());
        }
        
        @Test
        @DisplayName("should_Return400_When_HeaderXUserIdMissing")
        void shouldReturn400WhenHeaderXUserIdMissing() throws Exception {
            String requestBody = """
                {
                    "versionId": %d
                }
                """.formatted(VERSION_ID);
            
            mockMvc.perform(patch("/api/documentos/{documentoId}/version-actual", DOCUMENTO_ID)
                    .header("X-Organization-Id", ORGANIZACION_ID)
                    // Sin header X-User-Id
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("should_Return400_When_HeaderXOrganizationIdMissing")
        void shouldReturn400WhenHeaderXOrganizationIdMissing() throws Exception {
            String requestBody = """
                {
                    "versionId": %d
                }
                """.formatted(VERSION_ID);
            
            mockMvc.perform(patch("/api/documentos/{documentoId}/version-actual", DOCUMENTO_ID)
                    .header("X-User-Id", USUARIO_ID)
                    // Sin header X-Organization-Id
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }
    }
}
