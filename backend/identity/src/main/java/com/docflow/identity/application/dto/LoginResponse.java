package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para la respuesta de login exitoso.
 */
@Schema(description = "Respuesta de autenticación exitosa")
public record LoginResponse(
    
    @Schema(description = "Token JWT de autenticación", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,
    
    @Schema(description = "Tipo de token", example = "Bearer")
    String tipoToken,
    
    @Schema(description = "Tiempo de expiración en segundos", example = "86400")
    Long expiraEn,
    
    @Schema(description = "ID de la organización asignada", example = "1")
    Integer organizacionId
) {
    /**
     * Constructor conveniente que asume tipo Bearer.
     *
     * @param token el token JWT
     * @param expiraEn tiempo de expiración en segundos
     * @param organizacionId ID de la organización
     */
    public LoginResponse(String token, Long expiraEn, Integer organizacionId) {
        this(token, "Bearer", expiraEn, organizacionId);
    }
}
