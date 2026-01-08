package com.docflow.documentcore.infrastructure.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que extrae el ID de organización (tenant) del header HTTP y lo establece
 * en el contexto de la petición actual usando TenantContextHolder.
 * 
 * Este filtro es parte crítica del aislamiento multi-tenant (US-AUTH-004).
 * 
 * Flujo de ejecución:
 * 1. El API Gateway valida el JWT y extrae el claim 'org_id'
 * 2. Gateway inyecta el header 'X-Organization-Id' con el valor extraído
 * 3. Este filtro captura el header y lo establece en TenantContextHolder
 * 4. Los servicios downstream usan CurrentTenantService para acceder al tenant
 * 5. El filtro limpia el contexto en finally para evitar memory leaks
 * 
 * NOTA: Los microservicios backend confían en el Gateway para validación JWT.
 * El header X-Organization-Id es la fuente de verdad del tenant activo.
 */
@Component
@Slf4j
public class TenantContextFilter extends OncePerRequestFilter {

    /**
     * Nombre del header HTTP que contiene el ID de organización.
     * Inyectado por el API Gateway después de validar el JWT.
     */
    private static final String TENANT_HEADER = "X-Organization-Id";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. Extraer tenant del header
            var tenantHeader = request.getHeader(TENANT_HEADER);
            
            if (tenantHeader != null && !tenantHeader.isBlank()) {
                try {
                    var tenantId = Integer.parseInt(tenantHeader);
                    TenantContextHolder.setTenantId(tenantId);
                    
                    log.debug("Tenant establecido desde header: organizacionId={}, path={}", 
                        tenantId, 
                        request.getRequestURI());
                        
                } catch (NumberFormatException e) {
                    log.warn("Header {} contiene valor inválido: '{}'. Path: {}", 
                        TENANT_HEADER, 
                        tenantHeader, 
                        request.getRequestURI());
                    // No establecemos tenant - los servicios lanzarán TenantContextMissingException
                }
            } else {
                // Header no presente - puede ser endpoint público o error de configuración
                log.debug("Header {} no presente en la petición. Path: {}", 
                    TENANT_HEADER, 
                    request.getRequestURI());
            }

            // 2. Continuar con la cadena de filtros
            filterChain.doFilter(request, response);
            
        } finally {
            // 3. CRÍTICO: Limpiar contexto para evitar memory leaks y contaminación entre requests
            TenantContextHolder.clear();
        }
    }

    /**
     * Determina si este filtro debe ejecutarse para la petición dada.
     * 
     * @param request la petición HTTP
     * @return true si el filtro debe ejecutarse, false para saltarlo
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        var path = request.getRequestURI();
        
        // Excluir endpoints de infraestructura que no requieren tenant
        return path.startsWith("/actuator/") || 
               path.startsWith("/swagger-ui/") || 
               path.startsWith("/v3/api-docs") ||
               path.equals("/health");
    }
}
