package com.docflow.documentcore.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que extrae los IDs de usuario y organización de los headers HTTP
 * y los inyecta en el SecurityContext de la aplicación.
 * 
 * Este filtro trabaja junto con TenantContextFilter para proporcionar
 * todo el contexto de seguridad necesario para procesar la request.
 * 
 * Flujo de ejecución:
 * 1. El API Gateway valida el JWT y extrae usuario e organización
 * 2. Gateway inyecta headers 'X-User-Id' y 'X-Organization-Id'
 * 3. TenantContextFilter establece el tenant en TenantContextHolder (para Hibernate)
 * 4. Este filtro captura ambos headers y los inyecta en SecurityContext @RequestScope
 * 5. Los servicios de aplicación acceden al contexto vía SecurityContext
 * 
 * Parte de US-AUTH-004: Aislamiento multi-tenant y seguridad de datos.
 */
@Component
@Slf4j
@RequiredArgsConstructor
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

    private final SecurityContext securityContext;

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

            // 2. Poblar SecurityContext si los datos están disponibles
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                try {
                    Long usuarioId = Long.parseLong(userIdHeader);
                    securityContext.setUsuarioId(usuarioId);
                    log.debug("Usuario ID establecido en contexto: usuarioId={}, path={}", 
                        usuarioId, request.getRequestURI());
                } catch (NumberFormatException e) {
                    log.warn("Header {} contiene valor inválido: '{}'. Path: {}", 
                        USER_ID_HEADER, userIdHeader, request.getRequestURI());
                }
            } else {
                log.debug("Header {} no presente en la petición. Path: {}", 
                    USER_ID_HEADER, request.getRequestURI());
            }

            if (organizacionIdHeader != null && !organizacionIdHeader.isBlank()) {
                try {
                    Long organizacionId = Long.parseLong(organizacionIdHeader);
                    securityContext.setOrganizacionId(organizacionId);
                    log.debug("Organización ID establecida en contexto: organizacionId={}, path={}", 
                        organizacionId, request.getRequestURI());
                } catch (NumberFormatException e) {
                    log.warn("Header {} contiene valor inválido: '{}'. Path: {}", 
                        ORGANIZATION_ID_HEADER, organizacionIdHeader, request.getRequestURI());
                }
            } else {
                log.debug("Header {} no presente en la petición. Path: {}", 
                    ORGANIZATION_ID_HEADER, request.getRequestURI());
            }

            // 3. Continuar con la cadena de filtros
            filterChain.doFilter(request, response);

        } finally {
            // NOTA: No limpiar el SecurityContext aquí porque es @RequestScope
            // Spring lo limpiaremos automáticamente al final del request
        }
    }

    /**
     * Determina si este filtro debe ejecutarse para la petición dada.
     * 
     * Ejecuta para TODAS las peticiones excepto endpoints de infraestructura
     * que claramente no requieren contexto de seguridad.
     * 
     * @param request la petición HTTP
     * @return true si el filtro debe ejecutarse, false para saltarlo
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        var path = request.getRequestURI();
        
        // Excluir endpoints de infraestructura que no requieren contexto de seguridad
        return path.startsWith("/actuator/") || 
               path.startsWith("/swagger-ui/") || 
               path.startsWith("/v3/api-docs") ||
               path.equals("/health");
    }
}
