package com.docflow.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Punto de entrada personalizado para manejar errores de autenticación (401).
 * Retorna respuestas en formato RFC 7807 (ProblemDetail) en lugar del HTML por defecto.
 * 
 * Este componente implementa el manejo de errores requerido en US-AUTH-003.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Maneja peticiones no autenticadas retornando un ProblemDetail JSON.
     * 
     * @param request la petición HTTP que falló en autenticación
     * @param response la respuesta HTTP donde se escribirá el error
     * @param authException la excepción de autenticación que ocurrió
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        log.warn("Acceso no autorizado a: {} desde IP: {}", 
            request.getRequestURI(), 
            request.getRemoteAddr());

        // Crear ProblemDetail según RFC 7807
        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Token JWT inválido, expirado o ausente. Por favor, autentíquese usando /api/v1/auth/login"
        );
        
        problemDetail.setTitle("No Autenticado");
        problemDetail.setType(URI.create("urn:problem-type:auth/unauthorized"));
        problemDetail.setProperty("path", request.getRequestURI());
        problemDetail.setProperty("timestamp", System.currentTimeMillis());

        // Escribir respuesta JSON
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }
}
