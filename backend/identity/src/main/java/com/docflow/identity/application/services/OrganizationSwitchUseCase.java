package com.docflow.identity.application.services;

import com.docflow.identity.application.dto.LoginResponse;
import com.docflow.identity.application.dto.SwitchOrganizationRequest;
import com.docflow.identity.application.ports.output.UsuarioOrganizacionRepository;
import com.docflow.identity.domain.exceptions.OrganizacionNoEncontradaException;
import com.docflow.identity.domain.model.EstadoMembresia;
import com.docflow.identity.domain.model.UsuarioOrganizacionId;
import com.docflow.identity.infrastructure.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso para cambio de organización de un usuario autenticado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationSwitchUseCase {

    private final UsuarioOrganizacionRepository repository;
    private final JwtTokenService tokenService;
    private final JwtProperties jwtProperties;

    /**
     * Cambia la organización activa de un usuario, emitiendo un nuevo token.
     *
     * @param usuarioId el ID del usuario autenticado
     * @param request la solicitud con el ID de la organización objetivo
     * @return respuesta con nuevo token JWT
     * @throws OrganizacionNoEncontradaException si el usuario no pertenece a la organización
     */
    @Transactional(readOnly = true)
    public LoginResponse switchOrganization(Long usuarioId, SwitchOrganizationRequest request) {
        log.info("Usuario {} solicita cambio a organización {}", usuarioId, request.organizacionId());

        // Validate membership
        var membership = repository.findById(
            new UsuarioOrganizacionId(usuarioId, request.organizacionId())
        ).orElseThrow(() -> {
            log.warn("Usuario {} no pertenece a organización {}", usuarioId, request.organizacionId());
            return new OrganizacionNoEncontradaException(
                "El usuario no pertenece a la organización solicitada"
            );
        });

        if (membership.getEstado() != EstadoMembresia.ACTIVO) {
            log.warn("Membresía de usuario {} a organización {} está inactiva", 
                usuarioId, request.organizacionId());
            throw new OrganizacionNoEncontradaException(
                "La membresía a esta organización está inactiva"
            );
        }

        // Issue new token
        var token = tokenService.issueToken(usuarioId, request.organizacionId());

        log.info("Cambio de organización exitoso para usuario {} a organización {}", 
            usuarioId, request.organizacionId());

        return new LoginResponse(
            token,
            jwtProperties.expiration() / 1000,
            request.organizacionId()
        );
    }
}
