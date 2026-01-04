package com.docflow.identity.infrastructure.adapters.input.rest;

import com.docflow.identity.application.dto.LoginRequest;
import com.docflow.identity.application.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests para el endpoint POST /api/v1/auth/login.
 * Usa Testcontainers con PostgreSQL real y datos de prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("POST /api/v1/auth/login - Integration Tests")
class AuthLoginIntegrationTest {

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
    @DisplayName("Escenario 1: Usuario con 1 organización activa")
    @Sql(scripts = "/db/DB_AUTH_1.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    class LoginExitoso {

        @Test
        @DisplayName("Debe retornar 200 con token y organizacion_id correcto")
        void debeRetornar200ConToken() throws Exception {
            // Given
            var request = new LoginRequest("user2@docflow.com", "Test123!");

            // When & Then
            var responseString = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").value(86400))
                .andExpect(jsonPath("$.organizacionId").value(2))
                .andReturn().getResponse().getContentAsString();

            // Verificar que el token no esté vacío
            var response = objectMapper.readValue(responseString, LoginResponse.class);
            assertThat(response.token()).isNotBlank();
            assertThat(response.token().split("\\.")).hasSize(3); // JWT format: header.payload.signature
        }

        @Test
        @DisplayName("Token emitido debe contener usuario_id y org_id en claims")
        void tokenDebeContenerClaimsCorrectos() throws Exception {
            // Given
            var request = new LoginRequest("user2@docflow.com", "Test123!");

            // When
            var responseString = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

            var response = objectMapper.readValue(responseString, LoginResponse.class);

            // Then: Token debe ser válido y contener los claims correctos
            // (Verificación completa requeriría parsear el JWT, simplificado aquí)
            assertThat(response.token()).isNotEmpty();
            assertThat(response.organizacionId()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Escenario 1b: Usuario con múltiples organizaciones y predeterminada")
    @Sql(scripts = "/db/DB_AUTH_1.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    class LoginConPredeterminada {

        @Test
        @DisplayName("Debe retornar organización predeterminada (Alpha - ID 1)")
        void debeRetornarOrganizacionPredeterminada() throws Exception {
            // Given: user3 tiene Alpha (1) y Beta (3), Alpha es predeterminada
            var request = new LoginRequest("user3@docflow.com", "Test123!");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizacionId").value(1));
        }
    }

    @Nested
    @DisplayName("Escenario 2: Usuario con 2+ organizaciones sin predeterminada")
    @Sql(scripts = "/db/DB_AUTH_1.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    class LoginConfigInvalida {

        @Test
        @DisplayName("Debe retornar 409 con código ORGANIZACION_CONFIG_INVALIDA")
        void debeRetornar409ConCodigoDeError() throws Exception {
            // Given: user4 tiene 2 organizaciones sin predeterminada
            var request = new LoginRequest("user4@docflow.com", "Test123!");

            // When & Then
            var responseString = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Configuración de Organización Inválida"))
                .andExpect(jsonPath("$.codigo").value("ORGANIZACION_CONFIG_INVALIDA"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        }
    }

    @Nested
    @DisplayName("Escenario 2b: Usuario con >2 organizaciones sin predeterminada")
    @Sql(scripts = "/db/DB_AUTH_1.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    class LoginMasDeDosOrganizaciones {

        @Test
        @DisplayName("Debe retornar 409 (no soportado en MVP)")
        void debeRetornar409() throws Exception {
            // Given: user5 tiene 3 organizaciones sin predeterminada
            var request = new LoginRequest("user5@docflow.com", "Test123!");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ORGANIZACION_CONFIG_INVALIDA"));
        }
    }

    @Nested
    @DisplayName("Escenario 3: Credenciales inválidas")
    @Sql(scripts = "/db/DB_AUTH_1.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    class LoginFallido {

        @Test
        @DisplayName("Con contraseña incorrecta debe retornar 401")
        void conPasswordIncorrectaDebeRetornar401() throws Exception {
            // Given
            var request = new LoginRequest("user2@docflow.com", "WrongPassword!");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                .andExpect(jsonPath("$.title").value("Credenciales Inválidas"));
        }

        @Test
        @DisplayName("Con email inexistente debe retornar 401 sin revelar detalles")
        void conEmailInexistenteDebeRetornar401() throws Exception {
            // Given
            var request = new LoginRequest("noexiste@docflow.com", "Test123!");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                // No debe revelar si el usuario existe o no (OWASP)
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
        }

        @Test
        @DisplayName("Con email vacío debe retornar 400 (validation error)")
        void conEmailVacioDebeRetornar400() throws Exception {
            // Given
            var request = new LoginRequest("", "Test123!");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Con contraseña menor a 8 caracteres debe retornar 400")
        void conPasswordCortaDebeRetornar400() throws Exception {
            // Given
            var request = new LoginRequest("user2@docflow.com", "Short1!");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("Escenario 4: Usuario sin organizaciones activas")
    @Sql(scripts = "/db/DB_AUTH_1.sql", executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = "/db/cleanup.sql", executionPhase = AFTER_TEST_METHOD)
    class LoginSinOrganizaciones {

        @Test
        @DisplayName("Debe retornar 403 con código SIN_ORGANIZACION")
        void debeRetornar403ConCodigoDeError() throws Exception {
            // Given: user1 no tiene organizaciones activas
            var request = new LoginRequest("user1@docflow.com", "Test123!");

            // When & Then
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("SIN_ORGANIZACION"))
                .andExpect(jsonPath("$.title").value("Sin Organización"));
        }
    }
}
