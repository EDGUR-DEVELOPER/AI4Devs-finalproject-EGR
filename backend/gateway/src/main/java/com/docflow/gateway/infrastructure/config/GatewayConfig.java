package com.docflow.gateway.infrastructure.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway routing configuration.
 * <p>
 * Defines routes to backend microservices with path rewriting.
 * </p>
 */
@Configuration
public class GatewayConfig {

    private static final String IAM_SERVICE_URI = "http://localhost:8081";
    private static final String DOC_SERVICE_URI = "http://localhost:8082";

    /**
     * Configures the routes for the API Gateway.
     *
     * @param builder the RouteLocatorBuilder
     * @return the configured RouteLocator
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // IAM Service Route: /api/iam/** -> http://localhost:8081/api/v1/**
                .route("iam-service", r -> r
                        .path("/api/iam/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .prefixPath("/api/v1"))
                        .uri(IAM_SERVICE_URI))
                // Document Service Route: /api/doc/** -> http://localhost:8082/**
                .route("doc-service", r -> r
                        .path("/api/doc/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri(DOC_SERVICE_URI))
                .build();
    }
}
