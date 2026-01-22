package com.docflow.identity.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de configuración para JWT.
 */
@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
    
    @NotBlank(message = "El secret JWT es obligatorio")
    String secret,
    
    @Positive(message = "La expiración debe ser positiva")
    Long expiration,
    
    @NotBlank(message = "El issuer JWT es obligatorio")
    String issuer
) {}
