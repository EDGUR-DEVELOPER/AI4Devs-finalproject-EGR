package com.docflow.gateway.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests de integración para validar la configuración CORS del Gateway.
 * <p>
 * Verifica que:
 * - Se permitan requests desde orígenes configurados
 * - Se rechacen requests desde orígenes no autorizados
 * - Las respuestas preflight (OPTIONS) contengan los headers CORS correctos
 * - Los requests reales (POST) incluyan headers CORS en la respuesta
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@DisplayName("Tests de Configuración CORS del Gateway")
class GatewayCorsIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final String ALLOWED_ORIGIN_1 = "http://localhost:5173";
    private static final String ALLOWED_ORIGIN_2 = "http://localhost:3000";
    private static final String FORBIDDEN_ORIGIN = "http://malicious-site.com";
    private static final String IAM_LOGIN_ENDPOINT = "/api/iam/login";

    @BeforeEach
    void setUp() {
        // WebTestClient está configurado automáticamente por @AutoConfigureWebTestClient
    }

    @Nested
    @DisplayName("Preflight Requests (OPTIONS)")
    class PreflightRequests {

        @Test
        @DisplayName("Debe retornar headers CORS válidos para origen permitido (Vite)")
        void shouldReturnCorsHeadersForAllowedOriginVite() {
            webTestClient
                .options()
                .uri(IAM_LOGIN_ENDPOINT)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_1)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_1)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)
                .expectHeader().exists(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
        }

        @Test
        @DisplayName("Debe retornar headers CORS válidos para origen permitido (React)")
        void shouldReturnCorsHeadersForAllowedOriginReact() {
            webTestClient
                .options()
                .uri(IAM_LOGIN_ENDPOINT)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_2)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_2)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        @Test
        @DisplayName("Debe rechazar preflight de origen no permitido")
        void shouldRejectPreflightFromForbiddenOrigin() {
            webTestClient
                .options()
                .uri(IAM_LOGIN_ENDPOINT)
                .header(HttpHeaders.ORIGIN, FORBIDDEN_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .exchange()
                .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("Debe permitir métodos HTTP configurados")
        void shouldAllowConfiguredHttpMethods() {
            webTestClient
                .options()
                .uri(IAM_LOGIN_ENDPOINT)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_1)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    methods -> {
                        assert methods.contains("GET");
                        assert methods.contains("POST");
                        assert methods.contains("PUT");
                        assert methods.contains("DELETE");
                        assert methods.contains("OPTIONS");
                    }
                );
        }

        @Test
        @DisplayName("Debe permitir headers Authorization y Content-Type")
        void shouldAllowAuthorizationAndContentTypeHeaders() {
            webTestClient
                .options()
                .uri(IAM_LOGIN_ENDPOINT)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_1)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    headers -> {
                        assert headers.contains("Authorization");
                        assert headers.contains("Content-Type");
                    }
                );
        }

        @Test
        @DisplayName("Debe configurar Max-Age para cache de preflight (1 hora)")
        void shouldSetMaxAgeForPreflightCache() {
            webTestClient
                .options()
                .uri(IAM_LOGIN_ENDPOINT)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_1)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"); // 1 hora
        }
    }

    @Nested
    @DisplayName("Actual Requests (POST)")
    class ActualRequests {

        @Test
        @DisplayName("Debe incluir header CORS en respuesta POST desde origen permitido")
        void shouldIncludeCorsHeaderInPostResponse() {
            String requestBody = """
                {
                  "email": "test@docflow.com",
                  "password": "password123"
                }
                """;

            webTestClient
                .post()
                .uri(IAM_LOGIN_ENDPOINT)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_1)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(requestBody)
                .exchange()
                // Nota: El status puede ser 401/500 si el backend no está corriendo,
                // pero el header CORS debe estar presente
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_1)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        @Test
        @DisplayName("Debe incluir header CORS en respuesta GET desde origen permitido")
        void shouldIncludeCorsHeaderInGetResponse() {
            webTestClient
                .get()
                .uri("/health")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN_2)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_2)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        @Test
        @DisplayName("No debe incluir header CORS si no se envía Origin")
        void shouldNotIncludeCorsHeaderWithoutOrigin() {
            webTestClient
                .get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        }
    }
}
