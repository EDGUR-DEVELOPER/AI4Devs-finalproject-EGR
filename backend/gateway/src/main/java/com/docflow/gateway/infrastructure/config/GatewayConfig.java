package com.docflow.gateway.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Gateway routing configuration.
 * <p>
 * Defines routes to backend microservices with path rewriting.
 * Configura CORS centralizado para todos los microservices.
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    // Use environment variables set by Docker Compose for production container names
    // FALLBACK: localhost for local development
    private static final String IAM_SERVICE_URI = System.getenv().getOrDefault("IDENTITY_SERVICE_URL", "http://localhost:8081");
    private static final String DOC_SERVICE_URI = System.getenv().getOrDefault("DOCUMENT_SERVICE_URL", "http://localhost:8082");

    private final CorsProperties corsProperties;
    private final CorsOriginValidator corsOriginValidator;

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
                // Document Service Route: /api/doc/** -> http://localhost:8082/api/**
                .route("doc-service", r -> r
                        .path("/api/doc/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .prefixPath("/api"))
                        .uri(DOC_SERVICE_URI))
                .build();
    }

    /**
     * Configura el filtro CORS global para todos los endpoints del Gateway.
     * <p>
     * Configuración CORS centralizada que se aplica a todos los microservicios
     * enrutados a través del Gateway. Los orígenes permitidos se leen desde
     * application.yml (propiedad cors.allowed-origins).
     * </p>
     * <p>
     * Características de seguridad:
     * <ul>
     *   <li>Solo orígenes explícitos (NO wildcards)</li>
     *   <li>Permite credenciales (cookies, Authorization header)</li>
     *   <li>Métodos permitidos: GET, POST, PUT, DELETE, OPTIONS</li>
     *   <li>Headers permitidos: Authorization, Content-Type</li>
     *   <li>Max age: 3600 segundos (1 hora) para preflight cache</li>
     * </ul>
     * </p>
     *
     * @return CorsWebFilter configurado
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Orígenes permitidos (leídos desde properties)
        corsConfig.setAllowedOrigins(corsProperties.getAllowedOrigins());
        
        // Métodos HTTP permitidos
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Headers permitidos
        corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        
        // Permitir envío de credenciales (cookies, Authorization header)
        corsConfig.setAllowCredentials(true);
        
        // Tiempo de cache para respuestas preflight (1 hora)
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    /**
     * Valida la configuración CORS al iniciar la aplicación.
     * <p>
     * Se ejecuta después de que Spring Boot haya cargado completamente
     * el contexto de aplicación. Verifica patrones inseguros y registra
     * warnings si detecta configuraciones de desarrollo en producción.
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateCorsConfiguration() {
        corsOriginValidator.validate(corsProperties.getAllowedOrigins());
    }
}
