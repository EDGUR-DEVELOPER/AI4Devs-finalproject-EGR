package com.docflow.documentcore.integration;

import com.docflow.documentcore.api.dto.NivelAccesoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AclController
 * Tests full stack: Controller -> Service -> Repository -> Database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ACL Integration Tests")
class AclControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("should_ReturnActiveAccessLevels_When_GetAllActive")
    void should_ReturnActiveAccessLevels_When_GetAllActive() throws Exception {
        // When / Then
        MvcResult result = mockMvc.perform(get("/api/acl/niveles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        List<NivelAccesoDTO> niveles = objectMapper.readValue(
                jsonResponse, new TypeReference<List<NivelAccesoDTO>>() {});

        assertThat(niveles).isNotEmpty();
        assertThat(niveles).allMatch(n -> n.getActivo());
    }

    @Test
    @DisplayName("should_ReturnAllAccessLevels_When_GetAll")
    void should_ReturnAllAccessLevels_When_GetAll() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/acl/niveles/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("should_ReturnLecturaLevel_When_GetByCodigoLECTURA")
    void should_ReturnLecturaLevel_When_GetByCodigoLECTURA() throws Exception {
        // When / Then
        MvcResult result = mockMvc.perform(get("/api/acl/niveles/codigo/LECTURA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("LECTURA"))
                .andExpect(jsonPath("$.nombre").exists())
                .andExpect(jsonPath("$.acciones_permitidas").isArray())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        NivelAccesoDTO nivel = objectMapper.readValue(jsonResponse, NivelAccesoDTO.class);

        assertThat(nivel.getCodigo()).isEqualTo("LECTURA");
        assertThat(nivel.getAccionesPermitidas()).contains("ver", "listar", "descargar");
    }

    @Test
    @DisplayName("should_ReturnEscrituraLevel_When_GetByCodigoESCRITURA")
    void should_ReturnEscrituraLevel_When_GetByCodigoESCRITURA() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/acl/niveles/codigo/ESCRITURA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ESCRITURA"))
                .andExpect(jsonPath("$.acciones_permitidas").isArray());
    }

    @Test
    @DisplayName("should_ReturnAdministracionLevel_When_GetByCodigoADMINISTRACION")
    void should_ReturnAdministracionLevel_When_GetByCodigoADMINISTRACION() throws Exception {
        // When / Then
        MvcResult result = mockMvc.perform(get("/api/acl/niveles/codigo/ADMINISTRACION")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ADMINISTRACION"))
                .andExpect(jsonPath("$.acciones_permitidas").isArray())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        NivelAccesoDTO nivel = objectMapper.readValue(jsonResponse, NivelAccesoDTO.class);

        assertThat(nivel.getAccionesPermitidas()).contains(
                "ver", "listar", "descargar", "subir", "modificar", 
                "crear_version", "eliminar", "administrar_permisos", "cambiar_version_actual");
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_GetByInvalidCodigo")
    void should_ReturnBadRequest_When_GetByInvalidCodigo() throws Exception {
        // When / Then
        mockMvc.perform(get("/api/acl/niveles/codigo/INVALID")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_GetByNonExistingId")
    void should_ReturnBadRequest_When_GetByNonExistingId() throws Exception {
        // When / Then
        String fakeUuid = "00000000-0000-0000-0000-000000000000";
        mockMvc.perform(get("/api/acl/niveles/" + fakeUuid)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
