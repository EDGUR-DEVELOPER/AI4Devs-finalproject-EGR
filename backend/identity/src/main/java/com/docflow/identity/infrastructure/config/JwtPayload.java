package com.docflow.identity.infrastructure.config;

import java.time.Instant;
import java.util.List;

/**
 * Representación inmutable del payload de un JWT de DocFlow.
 * Incluye claims estándar y personalizados.
 * 
 * @param usuarioId ID único del usuario (claim 'sub')
 * @param organizacionId ID de la organización activa (claim 'org_id')
 * @param roles Lista inmutable de códigos de roles activos (claim 'roles')
 * @param issuer Emisor del token (claim 'iss')
 * @param issuedAt Timestamp de emisión (claim 'iat')
 * @param expiresAt Timestamp de expiración (claim 'exp')
 */
public record JwtPayload(
    Long usuarioId,
    Integer organizacionId,
    List<String> roles,
    String issuer,
    Instant issuedAt,
    Instant expiresAt
) {
    /**
     * Constructor compacto con validaciones.
     */
    public JwtPayload {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("usuarioId debe ser positivo");
        }
        if (organizacionId == null || organizacionId <= 0) {
            throw new IllegalArgumentException("organizacionId debe ser positivo");
        }
        if (roles == null) {
            throw new IllegalArgumentException("roles no puede ser null (usar List.of() para vacío)");
        }
        // Garantizar inmutabilidad de la lista
        roles = List.copyOf(roles);
    }
}
