package com.docflow.gateway.infrastructure.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Filtro global para extraer el ID de organización y usuario del JWT y propagarlos
 * como headers HTTP a los servicios downstream.
 * 
 * FLUJO:
 * 1. Extrae el token JWT del header "Authorization: Bearer <token>"
 * 2. Parsea el token y extrae:
 *    - El claim "org_id" (organizacionId)
 *    - El subject "sub" (usuarioId)
 * 3. Inyecta los headers:
 *    - "X-Organization-Id: <org_id>"
 *    - "X-User-Id: <user_id>"
 * 4. Los servicios backend (identity, document-core, auditLog) leen estos headers
 *    vía TenantContextFilter para establecer el contexto tenant y usuario
 * 
 * SEGURIDAD:
 * - El Gateway es el único punto de entrada confiable (trusted boundary)
 * - Los servicios backend confían ciegamente en los headers X-Organization-Id y X-User-Id
 * - Los servicios backend NO deben ser accesibles directamente desde internet
 * - JWT debe estar firmado con la misma secret key que identity-service
 * 
 * Implementa US-AUTH-004: Aislamiento de datos por organización.
 * 
 * @author DocFlow Team
 */
@Component
@Slf4j
public class TenantPropagationFilter implements GlobalFilter, Ordered {

    /**
     * Header HTTP que contiene el token JWT.
     */
    private static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION;

    /**
     * Prefijo del token Bearer.
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Header HTTP que se inyecta con el ID de organización.
     * Los servicios downstream leen este header para establecer el contexto tenant.
     */
    private static final String TENANT_HEADER = "X-Organization-Id";

    /**
     * Header HTTP que se inyecta con el ID de usuario.
     * Los servicios downstream leen este header para establecer el contexto del usuario.
     */
    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * Nombre del claim en el JWT que contiene el ID de organización.
     */
    private static final String ORG_ID_CLAIM = "org_id";

    /**
     * Clave secreta para verificar la firma del JWT.
     * Debe ser la misma que usa identity-service para firmar los tokens.
     */
    private final SecretKey secretKey;

    /**
     * Constructor que inicializa la clave secreta desde application.yml.
     * 
     * @param jwtSecret la clave secreta configurada (jwt.secret)
     */
    public TenantPropagationFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("TenantPropagationFilter inicializado con clave JWT (longitud: {} bytes)", 
                jwtSecret.length());
    }

    /**
     * Filtro principal que procesa cada request.
     * 
     * @param exchange el contexto de la request/response
     * @param chain la cadena de filtros
     * @return Mono<Void> que completa cuando el filtro termina
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extrae el header Authorization
        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);

        // Si no hay header o no es Bearer, continúa sin modificar (endpoints públicos)
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Request sin token JWT, continuando sin contexto tenant: {}", 
                    exchange.getRequest().getPath());
            return chain.filter(exchange);
        }

        // Extrae el token JWT
        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Parsea y valida el JWT
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Extrae el org_id del claim
            Integer orgId = claims.get(ORG_ID_CLAIM, Integer.class);

            if (orgId == null) {
                log.warn("Token JWT válido pero sin claim 'org_id'. Usuario: {}, Subject: {}",
                        claims.get("username"), claims.getSubject());
                return chain.filter(exchange);
            }

            // Extrae el user_id del subject (es parte estándar del JWT)
            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                log.warn("Token JWT válido pero sin subject (user_id). Org: {}", orgId);
                return chain.filter(exchange);
            }

            // Inyecta AMBOS headers en la request downstream
            ServerHttpRequest modifiedRequest = exchange.getRequest()
                    .mutate()
                    .header(TENANT_HEADER, orgId.toString())
                    .header(USER_ID_HEADER, userId)
                    .build();

            log.debug("Inyectados headers - {}: {}, {}: {} para request: {}", 
                    TENANT_HEADER, orgId, USER_ID_HEADER, userId, exchange.getRequest().getPath());

            // Continúa con la request modificada
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (JwtException e) {
            // Token inválido o expirado
            log.warn("JWT inválido o expirado: {}. Request: {}", 
                    e.getMessage(), exchange.getRequest().getPath());
            
            // IMPORTANTE: No bloqueamos la request, dejamos que identity-service
            // maneje el error de autenticación (401)
            return chain.filter(exchange);
        } catch (Exception e) {
            // Error inesperado al parsear el JWT
            log.error("Error inesperado al parsear JWT: ", e);
            return chain.filter(exchange);
        }
    }

    /**
     * Orden de ejecución del filtro.
     * 
     * HIGHEST_PRECEDENCE garantiza que se ejecute ANTES que cualquier
     * otro filtro de Spring Cloud Gateway, para que el header X-Organization-Id
     * esté disponible para filtros posteriores si los necesitan.
     * 
     * @return la prioridad del filtro
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
