package com.docflow.gateway.infrastructure.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that adds custom headers to all gateway responses.
 * <p>
 * Adds the X-DocFlow-Gateway header to identify responses
 * that have passed through the gateway.
 * </p>
 */
@Component
public class GlobalHeaderFilter implements GlobalFilter, Ordered {

    private static final String GATEWAY_HEADER_NAME = "X-DocFlow-Gateway";
    private static final String GATEWAY_HEADER_VALUE = "v1";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> 
                    exchange.getResponse().getHeaders()
                            .add(GATEWAY_HEADER_NAME, GATEWAY_HEADER_VALUE)
                ));
    }

    @Override
    public int getOrder() {
        // Execute after other filters but before response is committed
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
