package com.docflow.identity.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger para documentación de la API.
 * Define el esquema de seguridad JWT Bearer Token.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "DocFlow Identity Service API",
        version = "1.0.0",
        description = "API de autenticación y gestión de identidades para DocFlow. " +
                     "Maneja login, registro, gestión de usuarios y organizaciones.",
        contact = @Contact(
            name = "DocFlow Team",
            email = "dev@docflow.com"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8081", description = "Local Development"),
        @Server(url = "http://localhost:8080/api/iam", description = "Via API Gateway")
    }
)
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtenido del endpoint /api/v1/auth/login. " +
                 "Formato: Authorization: Bearer {token}"
)
public class OpenApiConfig {
    // La configuración se realiza mediante anotaciones
}
