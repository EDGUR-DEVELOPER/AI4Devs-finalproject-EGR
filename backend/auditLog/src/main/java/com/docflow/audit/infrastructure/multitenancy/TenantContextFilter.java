package com.docflow.audit.infrastructure.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFilter (filtro reactivo) que extrae el ID de organización (tenant) del header HTTP
 * y lo establece en el Reactor Context de la petición actual.
 * 
 * Este filtro es parte crítica del aislamiento multi-tenant (US-AUTH-004) en microservicios reactivos.
 * 
 * Flujo de ejecución:
 * 1. El API Gateway valida el JWT y extrae el claim 'org_id'
 * 2. Gateway inyecta el header 'X-Organization-Id' con el valor extraído
 * 3. Este filtro captura el header y lo establece en el Reactor Context
 * 4. Los servicios downstream usan CurrentTenantService para acceder al tenant
 * 5. El contexto se propaga automáticamente en la cadena reactiva
 * 
 * NOTA: En WebFlux no hay ThreadLocal - usamos Reactor Context para propagación.
 * El header X-Organization-Id es la fuente de verdad del tenant activo.
 */
@Component
@Slf4j
public class TenantContextFilter implements WebFilter {

    /**
     * Nombre del header HTTP que contiene el ID de organización.
     * Inyectado por el API Gateway después de validar el JWT.
     */
    private static final String TENANT_HEADER = "X-Organization-Id";

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        
        var request = exchange.getRequest();
        var path = request.getURI().getPath();
        
        // Excluir endpoints de infraestructura
        if (shouldNotFilter(path)) {
            return chain.filter(exchange);
        }

        // Extraer tenant del header
        var tenantHeader = request.getHeaders().getFirst(TENANT_HEADER);
        
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                var tenantId = Integer.parseInt(tenantHeader);
                
                log.debug("Tenant establecido desde header: organizacionId={}, path={}", 
                    tenantId, 
                    path);
                
                // Establecer tenant en Reactor Context y propagar
                return chain.filter(exchange)
                    .contextWrite(ctx -> TenantContextHolder.setTenantId(ctx, tenantId));
                    
            } catch (NumberFormatException e) {
                log.warn("Header {} contiene valor inválido: '{}'. Path: {}", 
                    TENANT_HEADER, 
                    tenantHeader, 
                    path);
                // Continuar sin tenant - los servicios lanzarán TenantContextMissingException
            }
        } else {
            log.debug("Header {} no presente en la petición. Path: {}", 
                TENANT_HEADER, 
                path);
        }

        // Continuar sin tenant en el contexto
        return chain.filter(exchange);
    }

    /**
     * Determina si este filtro debe ejecutarse para el path dado.
     * 
     * @param path el path de la petición HTTP
     * @return true si el filtro debe saltarse, false para ejecutarlo
     */
    private boolean shouldNotFilter(String path) {
        // Excluir endpoints de infraestructura que no requieren tenant
        return path.startsWith("/actuator/") || 
               path.startsWith("/swagger-ui/") || 
               path.startsWith("/webjars/") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/health");
    }
}
