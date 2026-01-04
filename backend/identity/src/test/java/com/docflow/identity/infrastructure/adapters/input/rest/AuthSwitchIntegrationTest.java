package com.docflow.identity.infrastructure.adapters.input.rest;

import com.docflow.identity.application.dto.LoginRequest;
import com.docflow.identity.application.dto.LoginResponse;
import com.docflow.identity.application.dto.SwitchOrganizationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests para el endpoint POST /api/v1/auth/switch.
 * Verifica el cambio de organización con token válido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Sql(scripts = "/db/DB_AUTH_1.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "/db/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
@DisplayName("POST /api/v1/auth/switch - Integration Tests")
class AuthSwitchIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("docflow_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Escenario 2c: Cambio de organización exitoso")
    class SwitchExitoso {

        @Test
        @DisplayName("Usuario con 2 organizaciones puede cambiar a la otra")
        void puedeCambiarAOtraOrganizacion() throws Exception {
            // Given: Primero hacer login como user3 (Alpha predeterminada)
            var loginRequest = new LoginRequest("user3@docflow.com", "Test123!");
            var loginResponseString = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

            var loginResponse = objectMapper.readValue(loginResponseString, LoginResponse.class);
            assertThat(loginResponse.organizacionId()).isEqualTo(1); // Alpha

            // When: Cambiar a Beta (ID 3)
            var switchRequest = new SwitchOrganizationRequest(3);
            var switchResponseString = mockMvc.perform(post("/api/v1/auth/switch")
                    .header("Authorization", "Bearer " + loginResponse.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.organizacionId").value(3))
                .andReturn().getResponse().getContentAsString();

            // Then: Verificar que el nuevo token es diferente
            var switchResponse = objectMapper.readValue(switchResponseString, LoginResponse.class);
            assertThat(switchResponse.token()).isNotEqualTo(loginResponse.token());
            assertThat(switchResponse.organizacionId()).isEqualTo(3);
        }

        @Test
        @DisplayName("Puede cambiar de vuelta a la organización original")
        void puedeCambiarDeVueltaAOriginal() throws Exception {
            // Given: Login y switch a otra organización
            var loginRequest = new LoginRequest("user3@docflow.com", "Test123!");
            var loginResponseString = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
            var loginResponse = objectMapper.readValue(loginResponseString, LoginResponse.class);

            var switchRequest1 = new SwitchOrganizationRequest(3);
            var switchResponse1String = mockMvc.perform(post("/api/v1/auth/switch")
                    .header("Authorization", "Bearer " + loginResponse.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(switchRequest1)))
                .andReturn().getResponse().getContentAsString();
            var switchResponse1 = objectMapper.readValue(switchResponse1String, LoginResponse.class);

            // When: Cambiar de vuelta a Alpha
            var switchRequest2 = new SwitchOrganizationRequest(1);
            mockMvc.perform(post("/api/v1/auth/switch")
                    .header("Authorization", "Bearer " + switchResponse1.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(switchRequest2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizacionId").value(1));
        }
    }

    @Nested
    @DisplayName("Validación de pertenencia")
    class ValidacionPertenencia {

        @Test
        @DisplayName("No puede cambiar a organización a la que no pertenece")
        void noPuedeCambiarAOrganizacionNoPertenece() throws Exception {
            // Given: Login como user2 (solo pertenece a Dev Team - ID 2)
            var loginRequest = new LoginRequest("user2@docflow.com", "Test123!");
            var loginResponseString = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
            var loginResponse = objectMapper.readValue(loginResponseString, LoginResponse.class);

            // When: Intentar cambiar a Alpha (ID 1) - no pertenece
            var switchRequest = new SwitchOrganizationRequest(1);
            mockMvc.perform(post("/api/v1/auth/switch")
                    .header("Authorization", "Bearer " + loginResponse.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ORGANIZACION_NO_ENCONTRADA"));
        }

        @Test
        @DisplayName("No puede cambiar a organización inexistente")
        void noPuedeCambiarAOrganizacionInexistente() throws Exception {
            // Given
            var loginRequest = new LoginRequest("user2@docflow.com", "Test123!");
            var loginResponseString = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
            var loginResponse = objectMapper.readValue(loginResponseString, LoginResponse.class);

            // When: Intentar cambiar a organización inexistente
            var switchRequest = new SwitchOrganizationRequest(999);
            mockMvc.perform(post("/api/v1/auth/switch")
                    .header("Authorization", "Bearer " + loginResponse.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ORGANIZACION_NO_ENCONTRADA"));
        }
    }

    @Nested
    @DisplayName("Validación de autenticación")
    class ValidacionAutenticacion {

        @Test
        @DisplayName("Sin token debe retornar 401")
        void sinTokenDebeRetornar401() throws Exception {
            // Given
            var switchRequest = new SwitchOrganizationRequest(1);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/switch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Con token inválido debe retornar 401")
        void conTokenInvalidoDebeRetornar401() throws Exception {
            // Given
            var switchRequest = new SwitchOrganizationRequest(1);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/switch")
                    .header("Authorization", "Bearer invalid.token.here")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Con token malformado (sin Bearer prefix) debe retornar error")
        void conTokenMalformadoDebeRetornarError() throws Exception {
            // Given
            var switchRequest = new SwitchOrganizationRequest(1);

            // When & Then: Espera error por formato incorrecto
            mockMvc.perform(post("/api/v1/auth/switch")
                    .header("Authorization", "invalid-format-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().is5xxServerError()); // IllegalArgumentException capturado como 500
        }
    }

    @Nested
    @DisplayName("Validación de entrada")
    class ValidacionEntrada {

        @Test
        @DisplayName("Con organizacionId nulo debe retornar 400")
        void conOrganizacionIdNuloDebeRetornar400() throws Exception {
            // Given
            var loginRequest = new LoginRequest("user2@docflow.com", "Test123!");
            var loginResponseString = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
            var loginResponse = objectMapper.readValue(loginResponseString, LoginResponse.class);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/switch")
                    .header("Authorization", "Bearer " + loginResponse.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"organizacionId\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Con organizacionId negativo debe retornar 400")
        void conOrganizacionIdNegativoDebeRetornar400() throws Exception {
            // Given
            var loginRequest = new LoginRequest("user2@docflow.com", "Test123!");
            var loginResponseString = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();
            var loginResponse = objectMapper.readValue(loginResponseString, LoginResponse.class);

            var switchRequest = new SwitchOrganizationRequest(-1);

            // When & Then
            mockMvc.perform(post("/api/v1/auth/switch")
                    .header("Authorization", "Bearer " + loginResponse.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(switchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));
        }
    }
}
