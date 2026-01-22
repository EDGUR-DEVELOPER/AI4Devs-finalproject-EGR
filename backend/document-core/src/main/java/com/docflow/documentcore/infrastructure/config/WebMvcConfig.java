package com.docflow.documentcore.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de Spring MVC para el servicio Document Core.
 * Configura gestión de recursos estáticos y exclusiones.
 * 
 * Soluciona el issue donde endpoints como /actuator/health eran tratados
 * como recursos estáticos en lugar de endpoints REST dinámicos.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Configura los manejadores de recursos estáticos.
     * 
     * IMPORTANTE: Al registrar resource handlers explícitos con addResourceHandler(),
     * deshabilitamos el default resource handler que intentaba servir TODOS los paths
     * como recursos estáticos. Esto previene que endpoints dinámicos (como /actuator)
     * sean interceptados erróneamente.
     * 
     * Los recursos estáticos solo se sirven desde /static y /public.
     * Todos los otros paths (/actuator, /api, /swagger-ui, etc.) son manejados
     * por DispatcherServlet y los controladores respectivos.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Solo registrar handlers explícitos para ubicaciones reales de recursos estáticos
        registry
            .addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(31536000); // 1 año

        registry
            .addResourceHandler("/public/**")
            .addResourceLocations("classpath:/public/")
            .setCachePeriod(31536000); // 1 año

        // NOTA: NO agregamos un handler para "/**" porque eso haría que TODOS los paths
        // sean tratados como recursos estáticos. Spring ya lo hace por defecto si no
        // registramos ningún handler explícito (lo cual es correcto para una API).
    }
}
