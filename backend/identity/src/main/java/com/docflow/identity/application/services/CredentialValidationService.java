package com.docflow.identity.application.services;

import com.docflow.identity.application.ports.output.UsuarioRepository;
import com.docflow.identity.domain.exceptions.InvalidCredentialsException;
import com.docflow.identity.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio para validación de credenciales de usuario.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredentialValidationService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Autentica un usuario validando sus credenciales.
     * No revela si el error es por email inexistente o contraseña incorrecta (OWASP best practice).
     *
     * @param email el email del usuario
     * @param rawPassword la contraseña en texto plano
     * @return el usuario autenticado
     * @throws InvalidCredentialsException si las credenciales son inválidas
     */
    public Usuario authenticate(String email, String rawPassword) {
        var usuario = usuarioRepository
            .findByEmailAndFechaEliminacionIsNull(email)
            .orElseThrow(() -> {
                log.warn("Intento de login con email inexistente o usuario eliminado: {}", email);
                return new InvalidCredentialsException("Credenciales inválidas");
            });

        if (!passwordEncoder.matches(rawPassword, usuario.getHashContrasena())) {
            log.warn("Intento de login con contraseña incorrecta para usuario: {}", email);
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        log.info("Usuario autenticado exitosamente: {}", email);
        return usuario;
    }
}
