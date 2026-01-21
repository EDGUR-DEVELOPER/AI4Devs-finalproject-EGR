package com.docflow.gateway.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Configuración de Spring WebFlux para el Gateway.
 * Configura gestión de recursos estáticos.
 * 
 * Nota: El Gateway usa WebFlux (reactivo), no MVC.
 * Esta configuración es menor ya que el gateway principalmente
 * redirige tráfico a microservicios.
 */
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {
    
    // No registramos handlers de recursos estáticos personalizados
    // El gateway no sirve contenido estático - solo redirige requests
}
