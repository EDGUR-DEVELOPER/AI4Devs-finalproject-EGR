package com.docflow.identity.application.services;

import com.docflow.identity.application.dto.LoginRequest;
import com.docflow.identity.application.dto.LoginResponse;
import com.docflow.identity.infrastructure.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de autenticación de usuario.
 * Orquesta la validación de credenciales, resolución de organización y emisión de token.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationUseCase {

    private final CredentialValidationService credentialService;
    private final OrganizacionResolutionService orgService;
    private final JwtTokenService tokenService;
    private final JwtProperties jwtProperties;

    /**
     * Ejecuta el flujo completo de autenticación.
     *
     * @param request la solicitud de login con email y contraseña
     * @return respuesta con token JWT y datos de organización
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Iniciando proceso de login para usuario: {}", request.email());

        // Step 1: Validate credentials
        var usuario = credentialService.authenticate(request.email(), request.password());

        // Step 2: Resolve organization
        var organizacionId = orgService.resolveLoginOrganization(usuario.getId());

        // Step 3: Issue token
        var token = tokenService.issueToken(usuario.getId(), organizacionId);

        log.info("Login exitoso para usuario {} en organización {}", 
            usuario.getId(), organizacionId);

        return new LoginResponse(
            token,
            jwtProperties.expiration() / 1000, // Convert to seconds
            organizacionId
        );
    }
}
