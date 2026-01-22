package com.docflow.identity.infrastructure.config;

/**
 * Nombres estandarizados de claims JWT según RFC 7519 y personalizados de DocFlow.
 * Evita el uso de strings literales dispersos en el código.
 */
public final class JwtClaimNames {
    
    // Claims estándar JWT (RFC 7519)
    public static final String SUBJECT = "sub";           // Usuario ID
    public static final String ISSUER = "iss";            // Emisor del token
    public static final String ISSUED_AT = "iat";         // Fecha de emisión
    public static final String EXPIRATION = "exp";        // Fecha de expiración
    
    // Claims personalizados DocFlow
    public static final String ORGANIZATION_ID = "org_id"; // ID de organización activa
    public static final String ROLES = "roles";            // Array de códigos de rol
    
    private JwtClaimNames() {
        throw new UnsupportedOperationException("Clase de constantes no instanciable");
    }
}
