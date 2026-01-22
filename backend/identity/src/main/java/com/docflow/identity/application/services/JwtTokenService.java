package com.docflow.identity.application.services;

import com.docflow.identity.infrastructure.config.JwtClaimNames;
import com.docflow.identity.infrastructure.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Servicio para generación y validación de tokens JWT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

    private final JwtProperties properties;

    /**
     * Genera un token JWT para un usuario en el contexto de una organización.
     *
     * @param usuarioId el ID del usuario
     * @param organizacionId el ID de la organización
     * @param roles lista de códigos de roles del usuario en la organización
     * @return el token JWT firmado
     */
    public String issueToken(Long usuarioId, Integer organizacionId, List<String> roles) {
        var now = Instant.now();
        var expiresAt = now.plusMillis(properties.expiration());

        return Jwts.builder()
            .subject(usuarioId.toString())
            .claim(JwtClaimNames.ORGANIZATION_ID, organizacionId)
            .claim(JwtClaimNames.ROLES, roles)
            .issuer(properties.issuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Valida un token JWT y extrae sus claims.
     *
     * @param token el token a validar
     * @return resultado de la validación con los datos del usuario, organización y roles
     */
    public TokenValidationResult validateToken(String token) {
        try {
            var claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            var roles = claims.get(JwtClaimNames.ROLES, List.class);
            if (roles == null) {
                roles = List.of(); // Compatibilidad con tokens antiguos sin claim roles
            }

            return new TokenValidationResult(
                Long.parseLong(claims.getSubject()),
                claims.get(JwtClaimNames.ORGANIZATION_ID, Integer.class),
                roles,
                true
            );
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return new TokenValidationResult(null, null, List.of(), false);
        }
    }

    /**
     * Obtiene la clave de firma a partir del secret configurado.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Resultado de la validación de un token.
     */
    public record TokenValidationResult(
        Long usuarioId,
        Integer organizacionId,
        List<String> roles,
        boolean isValid
    ) {}
}
