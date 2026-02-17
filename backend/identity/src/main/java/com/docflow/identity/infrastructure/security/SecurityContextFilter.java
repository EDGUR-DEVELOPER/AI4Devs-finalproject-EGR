package com.docflow.identity.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que extrae los IDs de usuario y organización de los headers HTTP.
 * 
 * Este filtro trabaja junto con TenantContextFilter para proporcionar
 * todo el contexto de seguridad necesario para procesar la request.
 * 
 * Flujo de ejecución:
 * 1. El API Gateway valida el JWT y extrae usuario e organización
 * 2. Gateway inyecta headers 'X-User-Id' y 'X-Organization-Id'
 * 3. TenantContextFilter establece el tenant en TenantContextHolder
 * 4. Este filtro captura ambos headers y los pone en atributos del request
 * 5. Los servicios de aplicación acceden al contexto vía TenantContextHolder
 * 
 * Parte de US-AUTH-004: Aislamiento multi-tenant y seguridad de datos.
 */
@Component
@Slf4j
public class SecurityContextFilter extends OncePerRequestFilter {

    /**
     * Nombre del header HTTP que contiene el ID de usuario.
     * Inyectado por el API Gateway después de validar el JWT.
     */
    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * Nombre del header HTTP que contiene el ID de organización.
     * Inyectado por el API Gateway después de validar el JWT.
     */
    private static final String ORGANIZATION_ID_HEADER = "X-Organization-Id";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. Extraer headers de seguridad del request
            String userIdHeader = request.getHeader(USER_ID_HEADER);
            String organizacionIdHeader = request.getHeader(ORGANIZATION_ID_HEADER);

            // 2. Validar y loguear contexto
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                try {
                    Long usuarioId = Long.parseLong(userIdHeader);
                    log.debug("Usuario ID en contexto: usuarioId={}, path={}", 
                        usuarioId, request.getRequestURI());
                } catch (NumberFormatException e) {
                    log.warn("Header {} contiene valor inválido: '{}'. Path: {}", 
                        USER_ID_HEADER, userIdHeader, request.getRequestURI());
                }
            }

            if (organizacionIdHeader != null && !organizacionIdHeader.isBlank()) {
                try {
                    Long organizacionId = Long.parseLong(organizacionIdHeader);
                    log.debug("Organización ID en contexto: organizacionId={}, path={}", 
                        organizacionId, request.getRequestURI());
                } catch (NumberFormatException e) {
                    log.warn("Header {} contiene valor inválido: '{}'. Path: {}", 
                        ORGANIZATION_ID_HEADER, organizacionIdHeader, request.getRequestURI());
                }
            }

            // 3. Continuar con la cadena de filtros
            filterChain.doFilter(request, response);

        } finally {
            // No necesita limpiar - los headers se descartan con el request
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
        
        // Excluir endpoints de infraestructura
        return path.startsWith("/actuator/") || 
               path.startsWith("/swagger-ui/") || 
               path.startsWith("/v3/api-docs") ||
               path.equals("/health");
    }
}
