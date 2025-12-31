package com.docflow.gateway.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalHeaderFilter.
 */
class GlobalHeaderFilterTest {

    private final GlobalHeaderFilter filter = new GlobalHeaderFilter();

    @Test
    @DisplayName("Should add X-DocFlow-Gateway header to response")
    void shouldAddGatewayHeaderToResponse() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        filter.filter(exchange, chain).block();

        // Then
        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst("X-DocFlow-Gateway")).isEqualTo("v1");
    }

    @Test
    @DisplayName("Should have correct filter order")
    void shouldHaveCorrectFilterOrder() {
        // When
        int order = filter.getOrder();

        // Then
        assertThat(order).isEqualTo(Ordered.LOWEST_PRECEDENCE - 1);
    }

    @Test
    @DisplayName("Should implement GlobalFilter interface")
    void shouldImplementGlobalFilterInterface() {
        assertThat(filter).isInstanceOf(org.springframework.cloud.gateway.filter.GlobalFilter.class);
    }

    @Test
    @DisplayName("Should implement Ordered interface")
    void shouldImplementOrderedInterface() {
        assertThat(filter).isInstanceOf(Ordered.class);
    }
}
