package com.docflow.gateway.infrastructure.adapters.input.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Unit tests for HealthController.
 */
@WebFluxTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Should return health status OK")
    void shouldReturnHealthStatusOk() {
        webTestClient.get()
                .uri("/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");
    }

    @Test
    @DisplayName("Should return JSON content type")
    void shouldReturnJsonContentType() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("Should return single status field")
    void shouldReturnSingleStatusField() {
        webTestClient.get()
                .uri("/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists()
                .jsonPath("$.*").value(list -> {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> values = (java.util.List<Object>) list;
                    org.assertj.core.api.Assertions.assertThat(values).hasSize(1);
                });
    }
}
