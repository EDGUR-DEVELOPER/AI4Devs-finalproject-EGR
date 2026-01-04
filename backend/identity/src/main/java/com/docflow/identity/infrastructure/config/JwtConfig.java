package com.docflow.identity.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de JWT.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
}
