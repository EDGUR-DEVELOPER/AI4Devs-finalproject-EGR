package com.docflow.identity.infrastructure.security;

import com.docflow.identity.application.services.JwtTokenService;
import com.docflow.identity.application.services.JwtTokenService.TokenValidationResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticación JWT que intercepta todas las peticiones HTTP.
 * Extrae el token del header Authorization, lo valida usando JwtTokenService,
 * y popula el SecurityContext con la información del usuario autenticado.
 * 
 * Este filtro implementa US-AUTH-003: Middleware de autenticación para endpoints protegidos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtTokenService jwtTokenService;

    /**
     * Método principal del filtro que se ejecuta una vez por petición.
     * 
     * @param request la petición HTTP entrante
     * @param response la respuesta HTTP
     * @param filterChain la cadena de filtros de Spring Security
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extraer header Authorization
        var authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        
        // Si no hay header o no empieza con "Bearer ", continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraer token (eliminar el prefijo "Bearer ")
        var token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // 3. Validar token usando el servicio existente
            var validationResult = jwtTokenService.validateToken(token);

            // Si el token es válido, establecer autenticación en el contexto
            if (validationResult.isValid()) {
                authenticateUser(validationResult, request);
                log.debug("Token JWT válido para usuario: {}, organización: {}", 
                    validationResult.usuarioId(), 
                    validationResult.organizacionId());
            } else {
                log.warn("Token JWT inválido recibido desde IP: {}, Path: {}", 
                    request.getRemoteAddr(), 
                    request.getRequestURI());
                // No establecemos autenticación, Spring Security manejará el 401
                SecurityContextHolder.clearContext();
            }

        } catch (Exception e) {
            log.error("Error inesperado validando token JWT: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        // 7. Continuar la cadena de filtros (autenticado o no)
        filterChain.doFilter(request, response);
    }

    /**
     * Establece la autenticación en el SecurityContext de Spring Security.
     * 
     * @param validationResult resultado de la validación del token con los datos del usuario
     * @param request la petición HTTP actual
     */
    private void authenticateUser(TokenValidationResult validationResult, HttpServletRequest request) {
        // Convertir roles a authorities de Spring Security con prefijo ROLE_
        var authorities = validationResult.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        // Crear objeto de autenticación con TokenValidationResult como principal
        // Esto permite acceder a userId, orgId y roles con @AuthenticationPrincipal
        var authentication = new UsernamePasswordAuthenticationToken(
                validationResult,    // Principal - accesible con @AuthenticationPrincipal
                null,                // Credentials - no las guardamos por seguridad
                authorities          // Authorities - para @PreAuthorize y hasRole()
        );
        
        // Agregar detalles de la petición (IP, session ID, etc.)
        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );

        // Inyectar autenticación en el SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
