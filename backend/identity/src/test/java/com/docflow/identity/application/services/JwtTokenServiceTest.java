package com.docflow.identity.application.services;

import com.docflow.identity.infrastructure.config.JwtClaimNames;
import com.docflow.identity.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenService - Tests Unitarios de Claims")
class JwtTokenServiceTest {
    
    private JwtTokenService tokenService;
    private JwtProperties properties;
    private SecretKey signingKey;
    
    @BeforeEach
    void setUp() {
        properties = new JwtProperties(
            "test-secret-key-minimum-256-bits-for-hmac-sha256-algorithm-security",
            86400000L, // 24 horas
            "test-issuer"
        );
        signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes());
        tokenService = new JwtTokenService(properties);
    }
    
    @Test
    @DisplayName("Debe generar token con claim 'roles' como array JSON")
    void debeGenerarTokenConRolesComoArray() {
        // Arrange
        var usuarioId = 12345L;
        var organizacionId = 1;
        var roles = List.of("ADMIN", "USER");
        
        // Act
        var token = tokenService.issueToken(usuarioId, organizacionId, roles);
        
        // Assert
        var claims = decodificarToken(token);
        
        assertThat(claims.getSubject())
            .isEqualTo(String.valueOf(usuarioId));
        
        assertThat(claims.get(JwtClaimNames.ORGANIZATION_ID, Integer.class))
            .isEqualTo(organizacionId);
        
        assertThat(claims.get(JwtClaimNames.ROLES, List.class))
            .containsExactly("ADMIN", "USER");
        
        assertThat(claims.getIssuer())
            .isEqualTo("test-issuer");
    }
    
    @Test
    @DisplayName("Debe manejar lista vacía de roles sin errores")
    void debePermitirListaVaciaDeRoles() {
        // Arrange
        var usuarioId = 99999L;
        var organizacionId = 5;
        var rolesVacios = List.<String>of();
        
        // Act
        var token = tokenService.issueToken(usuarioId, organizacionId, rolesVacios);
        
        // Assert
        var claims = decodificarToken(token);
        
        assertThat(claims.get(JwtClaimNames.ROLES, List.class))
            .isEmpty();
    }
    
    @Test
    @DisplayName("Debe validar token y extraer roles correctamente")
    void debeValidarYExtraerRolesCorrectamente() {
        // Arrange
        var usuarioId = 77777L;
        var organizacionId = 3;
        var roles = List.of("VIEWER", "AUDITOR", "CONTRIBUTOR");
        var token = tokenService.issueToken(usuarioId, organizacionId, roles);
        
        // Act
        var resultado = tokenService.validateToken(token);
        
        // Assert
        assertThat(resultado.isValid()).isTrue();
        assertThat(resultado.usuarioId()).isEqualTo(usuarioId);
        assertThat(resultado.organizacionId()).isEqualTo(organizacionId);
        assertThat(resultado.roles())
            .containsExactlyInAnyOrder("VIEWER", "AUDITOR", "CONTRIBUTOR");
    }
    
    @Test
    @DisplayName("Debe rechazar token expirado")
    void debeRechazarTokenExpirado() {
        // Arrange: Token expirado hace 1 hora
        var tokenExpirado = Jwts.builder()
            .subject("12345")
            .claim(JwtClaimNames.ORGANIZATION_ID, 1)
            .claim(JwtClaimNames.ROLES, List.of("USER"))
            .issuer("test-issuer")
            .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
            .expiration(Date.from(Instant.now().minusSeconds(3600)))
            .signWith(signingKey)
            .compact();
        
        // Act
        var resultado = tokenService.validateToken(tokenExpirado);
        
        // Assert
        assertThat(resultado.isValid()).isFalse();
        assertThat(resultado.roles()).isEmpty();
    }
    
    @Test
    @DisplayName("Debe manejar roles con guiones bajos y mayúsculas")
    void debePermitirRolesConFormatoValido() {
        // Arrange
        var roles = List.of("SUPER_ADMIN", "DATA_ANALYST", "QA_TESTER");
        
        // Act
        var token = tokenService.issueToken(1L, 1, roles);
        
        // Assert
        var claims = decodificarToken(token);
        assertThat(claims.get(JwtClaimNames.ROLES, List.class))
            .containsExactly("SUPER_ADMIN", "DATA_ANALYST", "QA_TESTER");
    }
    
    @Test
    @DisplayName("Debe incluir timestamps iat y exp válidos")
    void debeIncluirTimestampsValidos() {
        // Arrange
        var ahora = Instant.now();
        
        // Act
        var token = tokenService.issueToken(1L, 1, List.of("USER"));
        
        // Assert
        var claims = decodificarToken(token);
        var iat = claims.getIssuedAt().toInstant();
        var exp = claims.getExpiration().toInstant();
        
        assertThat(iat).isBetween(ahora.minusSeconds(5), ahora.plusSeconds(5));
        assertThat(exp).isAfter(iat);
        assertThat(exp.toEpochMilli() - iat.toEpochMilli())
            .isEqualTo(properties.expiration());
    }
    
    @Test
    @DisplayName("Debe manejar token sin claim roles (compatibilidad con tokens antiguos)")
    void debeManejarTokenSinClaimRoles() {
        // Arrange: Token antiguo sin claim roles
        var tokenAntiguo = Jwts.builder()
            .subject("54321")
            .claim(JwtClaimNames.ORGANIZATION_ID, 2)
            // No incluye claim 'roles'
            .issuer("test-issuer")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(signingKey)
            .compact();
        
        // Act
        var resultado = tokenService.validateToken(tokenAntiguo);
        
        // Assert
        assertThat(resultado.isValid()).isTrue();
        assertThat(resultado.usuarioId()).isEqualTo(54321L);
        assertThat(resultado.organizacionId()).isEqualTo(2);
        assertThat(resultado.roles()).isEmpty(); // Debe retornar lista vacía
    }
    
    @Test
    @DisplayName("Debe rechazar token con firma incorrecta")
    void debeRechazarTokenConFirmaIncorrecta() {
        // Arrange: Token firmado con una clave diferente
        var otraKey = Keys.hmacShaKeyFor("otra-clave-secreta-diferente-con-minimo-256-bits-requeridos".getBytes());
        var tokenInvalido = Jwts.builder()
            .subject("99999")
            .claim(JwtClaimNames.ORGANIZATION_ID, 1)
            .claim(JwtClaimNames.ROLES, List.of("USER"))
            .issuer("test-issuer")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .signWith(otraKey) // Firma con clave incorrecta
            .compact();
        
        // Act
        var resultado = tokenService.validateToken(tokenInvalido);
        
        // Assert
        assertThat(resultado.isValid()).isFalse();
        assertThat(resultado.usuarioId()).isNull();
        assertThat(resultado.organizacionId()).isNull();
        assertThat(resultado.roles()).isEmpty();
    }
    
    @Test
    @DisplayName("Debe generar token con múltiples roles (más de 10)")
    void debeGenerarTokenConMultiplesRoles() {
        // Arrange: Simular usuario con muchos roles
        var roles = List.of(
            "ADMIN", "USER", "VIEWER", "EDITOR", "AUDITOR",
            "CONTRIBUTOR", "MANAGER", "ANALYST", "OPERATOR", "SUPERVISOR",
            "COORDINATOR", "SPECIALIST"
        );
        
        // Act
        var token = tokenService.issueToken(1L, 1, roles);
        
        // Assert
        var claims = decodificarToken(token);
        assertThat(claims.get(JwtClaimNames.ROLES, List.class))
            .hasSize(12)
            .containsAll(roles);
        
        // Verificar que el token no es excesivamente grande (< 2KB)
        assertThat(token.length()).isLessThan(2048);
    }
    
    @Test
    @DisplayName("Debe usar constantes JwtClaimNames en lugar de strings literales")
    void debeUsarConstantesParaClaims() {
        // Arrange
        var token = tokenService.issueToken(1L, 1, List.of("USER"));
        
        // Act
        var claims = decodificarToken(token);
        
        // Assert: Verificar que los claims existen con las constantes
        assertThat(claims.containsKey(JwtClaimNames.ORGANIZATION_ID)).isTrue();
        assertThat(claims.containsKey(JwtClaimNames.ROLES)).isTrue();
        assertThat(claims.getIssuer()).isNotNull();
        assertThat(claims.getSubject()).isNotNull();
    }
    
    // Método auxiliar para decodificar y validar tokens
    private Claims decodificarToken(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
