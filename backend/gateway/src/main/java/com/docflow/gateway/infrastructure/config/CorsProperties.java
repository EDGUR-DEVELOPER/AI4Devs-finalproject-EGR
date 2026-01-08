package com.docflow.gateway.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Propiedades de configuración para CORS (Cross-Origin Resource Sharing).
 * <p>
 * Mapea las propiedades del prefijo 'cors' desde application.yml,
 * permitiendo externalizar los orígenes permitidos por entorno.
 * </p>
 * <p>
 * Ejemplo de configuración en application.yml:
 * <pre>
 * cors:
 *   allowed-origins:
 *     - http://localhost:5173
 *     - http://localhost:3000
 * </pre>
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * Lista de orígenes permitidos para peticiones CORS.
     * <p>
     * Cada origen debe incluir el protocolo completo (http:// o https://).
     * NO se permiten wildcards (*) por razones de seguridad.
     * </p>
     * <p>
     * Ejemplos válidos:
     * <ul>
     *   <li>http://localhost:5173 (Vite dev server)</li>
     *   <li>http://localhost:3000 (React/Next.js dev)</li>
     * </ul>
     * </p>
     */
    private List<String> allowedOrigins = List.of(
        "http://localhost:5173",
        "http://localhost:3000"
    );
}
