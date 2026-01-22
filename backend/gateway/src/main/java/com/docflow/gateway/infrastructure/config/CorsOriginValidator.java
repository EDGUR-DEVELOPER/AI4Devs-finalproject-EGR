package com.docflow.gateway.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validador de orígenes CORS para garantizar configuraciones seguras.
 * <p>
 * Valida que los orígenes configurados no contengan patrones inseguros
 * y emite warnings cuando se detectan configuraciones de desarrollo en producción.
 * </p>
 */
@Slf4j
@Component
public class CorsOriginValidator {

    private final Environment environment;

    public CorsOriginValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * Valida la lista de orígenes permitidos para CORS.
     * <p>
     * Verificaciones realizadas:
     * <ul>
     *   <li>Rechaza wildcard (*) por ser inseguro</li>
     *   <li>Registra warnings si se usan orígenes localhost en producción</li>
     * </ul>
     * </p>
     *
     * @param allowedOrigins lista de orígenes a validar
     * @throws IllegalArgumentException si se detecta un patrón inseguro
     */
    public void validate(List<String> allowedOrigins) {
        log.info("Validando configuración CORS con {} orígenes", allowedOrigins.size());

        // Validar que no se use wildcard inseguro
        if (allowedOrigins.contains("*")) {
            String errorMessage = "Configuración CORS insegura: El origen wildcard '*' está prohibido. " +
                                 "Especifique orígenes explícitos (ej: http://localhost:5173, https://app.docflow.com)";
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        // Advertir si se usan orígenes localhost en producción
        if (isProductionProfile() && hasLocalhostOrigins(allowedOrigins)) {
            log.warn("⚠️ ADVERTENCIA DE SEGURIDAD: Se detectaron orígenes localhost en perfil de producción. " +
                    "Orígenes configurados: {}. Esto puede ser un error de configuración.", allowedOrigins);
        }

        log.info("✓ Validación CORS exitosa. Orígenes permitidos: {}", allowedOrigins);
    }

    /**
     * Verifica si el perfil activo es 'production'.
     */
    private boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si algún origen contiene 'localhost' o '127.0.0.1'.
     */
    private boolean hasLocalhostOrigins(List<String> origins) {
        return origins.stream()
                .anyMatch(origin -> origin.contains("localhost") || origin.contains("127.0.0.1"));
    }
}
