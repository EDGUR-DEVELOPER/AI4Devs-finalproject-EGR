package com.docflow.identity.infrastructure.security;

import com.docflow.identity.application.services.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración para el middleware de autenticación JWT.
 * Verifica todos los escenarios de US-AUTH-003.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("JWT Authentication Middleware Integration Tests")
class JwtAuthenticationFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Protected Endpoint Tests")
    class ProtectedEndpointTests {

        @Test
        @DisplayName("Debe retornar 401 cuando no se proporciona token")
        void shouldReturn401WhenNoTokenProvided() throws Exception {
            mockMvc.perform(get("/api/v1/protected/test"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("urn:problem-type:auth/unauthorized"))
                    .andExpect(jsonPath("$.title").value("No Autenticado"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value(containsString("Token JWT")))
                    .andExpect(jsonPath("$.path").value("/api/v1/protected/test"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Debe retornar 401 cuando el token es inválido")
        void shouldReturn401WhenTokenIsInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token-xyz-123"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        @DisplayName("Debe retornar 401 cuando el header no tiene prefijo Bearer")
        void shouldReturn401WhenNoBearerPrefix() throws Exception {
            var validToken = jwtTokenService.issueToken(1L, 100, List.of("USER"));

            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, validToken)) // Sin "Bearer "
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Debe retornar 401 cuando el token tiene firma incorrecta")
        void shouldReturn401WhenTokenHasWrongSignature() throws Exception {
            // Token con firma manipulada (cambiar último carácter)
            var validToken = jwtTokenService.issueToken(1L, 100, List.of("USER"));
            var manipulatedToken = validToken.substring(0, validToken.length() - 1) + "X";

            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + manipulatedToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Debe retornar 200 cuando el token es válido")
        void shouldReturn200WhenTokenIsValid() throws Exception {
            // Generar token válido
            var userId = 999L;
            var orgId = 555;
            var roles = List.of("ADMIN", "USER", "MANAGER");
            var validToken = jwtTokenService.issueToken(userId, orgId, roles);

            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Acceso concedido - JWT válido"))
                    .andExpect(jsonPath("$.userId").value(userId))
                    .andExpect(jsonPath("$.organizationId").value(orgId))
                    .andExpect(jsonPath("$.roles").isArray())
                    .andExpect(jsonPath("$.roles", hasSize(3)))
                    .andExpect(jsonPath("$.roles", containsInAnyOrder("ADMIN", "USER", "MANAGER")))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("Debe permitir múltiples peticiones con el mismo token")
        void shouldAllowMultipleRequestsWithSameToken() throws Exception {
            var validToken = jwtTokenService.issueToken(123L, 456, List.of("USER"));

            // Primera petición
            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(123));

            // Segunda petición con el mismo token
            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(123));
        }

        @Test
        @DisplayName("Debe extraer correctamente userId y orgId del token")
        void shouldExtractUserIdAndOrgIdCorrectly() throws Exception {
            var userId = 42L;
            var orgId = 789;
            var token = jwtTokenService.issueToken(userId, orgId, List.of());

            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(userId))
                    .andExpect(jsonPath("$.organizationId").value(orgId))
                    .andExpect(jsonPath("$.roles").isEmpty());
        }
    }

    @Nested
    @DisplayName("Public Endpoint Tests")
    class PublicEndpointTests {

        @Test
        @DisplayName("Debe permitir acceso a /health sin token")
        void shouldAllowAccessToHealthWithoutToken() throws Exception {
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Debe permitir acceso a /hello sin token")
        void shouldAllowAccessToHelloWithoutToken() throws Exception {
            mockMvc.perform(get("/hello"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Debe permitir POST a /api/v1/auth/login sin token")
        void shouldAllowLoginWithoutToken() throws Exception {
            var loginRequest = Map.of(
                    "email", "test@docflow.com",
                    "password", "password123",
                    "organizacionId", 1
            );

            // Aunque falle por credenciales inválidas, no debe ser 401 (sino 400 o 500)
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().is(not(401))); // No debe ser unauthorized
        }
    }

    @Nested
    @DisplayName("Admin Role Tests")
    class AdminRoleTests {

        @Test
        @DisplayName("Debe permitir acceso a endpoint admin con rol ADMIN")
        void shouldAllowAccessToAdminEndpointWithAdminRole() throws Exception {
            var token = jwtTokenService.issueToken(1L, 1, List.of("ADMIN", "USER"));

            mockMvc.perform(get("/api/v1/protected/admin-only")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("administrativo")));
        }

        @Test
        @DisplayName("Debe retornar 403 en endpoint admin sin rol ADMIN")
        void shouldReturn403OnAdminEndpointWithoutAdminRole() throws Exception {
            var token = jwtTokenService.issueToken(1L, 1, List.of("USER"));

            mockMvc.perform(get("/api/v1/protected/admin-only")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.message").value(containsString("ADMIN")));
        }

        @Test
        @DisplayName("Debe retornar 401 en endpoint admin sin token")
        void shouldReturn401OnAdminEndpointWithoutToken() throws Exception {
            mockMvc.perform(get("/api/v1/protected/admin-only"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Switch Organization Tests")
    class SwitchOrganizationTests {

        @Test
        @DisplayName("Debe proteger /api/v1/auth/switch - requiere JWT")
        void shouldProtectSwitchEndpointRequiresJwt() throws Exception {
            var switchRequest = Map.of("organizacionId", 999);

            mockMvc.perform(post("/api/v1/auth/switch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(switchRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        @DisplayName("Debe permitir switch con token válido")
        void shouldAllowSwitchWithValidToken() throws Exception {
            var token = jwtTokenService.issueToken(1L, 100, List.of("USER"));
            var switchRequest = Map.of("organizacionId", 200);

            // El endpoint puede fallar por lógica de negocio, pero NO debe ser 401
            mockMvc.perform(post("/api/v1/auth/switch")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(switchRequest)))
                    .andExpect(status().is(not(401))); // No debe ser unauthorized
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Debe manejar token vacío después de Bearer")
        void shouldHandleEmptyTokenAfterBearer() throws Exception {
            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer "))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Debe manejar múltiples espacios en Bearer token")
        void shouldHandleMultipleSpacesInBearerToken() throws Exception {
            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer  invalid-token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Debe ser case-sensitive con Bearer prefix")
        void shouldBeCaseSensitiveWithBearerPrefix() throws Exception {
            var token = jwtTokenService.issueToken(1L, 1, List.of("USER"));

            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "bearer " + token)) // lowercase
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Debe manejar token con espacios adicionales")
        void shouldHandleTokenWithExtraSpaces() throws Exception {
            var token = jwtTokenService.issueToken(1L, 1, List.of("USER"));

            mockMvc.perform(get("/api/v1/protected/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token + " "))
                    .andExpect(status().isUnauthorized()); // Espacios al final invalidan token
        }
    }
}
