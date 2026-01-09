package com.docflow.identity.application.services;

import com.docflow.identity.application.dto.CreateUserRequest;
import com.docflow.identity.application.dto.UserCreatedResponse;
import com.docflow.identity.application.ports.output.UsuarioOrganizacionRepository;
import com.docflow.identity.application.ports.output.UsuarioRepository;
import com.docflow.identity.domain.exceptions.EmailDuplicadoException;
import com.docflow.identity.domain.model.EstadoMembresia;
import com.docflow.identity.domain.model.Usuario;
import com.docflow.identity.domain.model.UsuarioOrganizacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestión de usuarios por parte de administradores.
 * Implementa operaciones CRUD sobre usuarios dentro del contexto de una organización.
 * 
 * Este servicio sigue el principio de responsabilidad única, enfocándose exclusivamente
 * en la lógica de negocio relacionada con la administración de usuarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserManagementService {
    
    private final UsuarioRepository usuarioRepository;
    private final UsuarioOrganizacionRepository usuarioOrganizacionRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Crea un nuevo usuario y lo asocia a la organización especificada.
     * 
     * El proceso incluye:
     * 1. Validación de unicidad del email (case-insensitive)
     * 2. Creación de la entidad Usuario con contraseña hasheada (BCrypt)
     * 3. Creación de la membresía en la organización (estado ACTIVO, es_predeterminada=true)
     * 
     * @param request Datos del usuario a crear (email, nombre, contraseña)
     * @param organizacionId ID de la organización del administrador que crea el usuario
     * @return Datos del usuario creado (sin exponer hash de contraseña)
     * @throws EmailDuplicadoException si el email ya existe en el sistema
     */
    @Transactional
    public UserCreatedResponse createUser(CreateUserRequest request, Integer organizacionId) {
        log.info("Iniciando creación de usuario con email: {} en organización: {}", 
            request.email(), organizacionId);
        
        // 1. Validar unicidad del email (case-insensitive)
        if (usuarioRepository.existsByEmail(request.email())) {
            log.warn("Intento de crear usuario con email duplicado: {}", request.email());
            throw new EmailDuplicadoException(
                String.format("Ya existe un usuario registrado con el email: %s", request.email())
            );
        }
        
        // 2. Crear entidad Usuario con contraseña hasheada
        var nuevoUsuario = new Usuario();
        nuevoUsuario.setEmail(request.email().toLowerCase()); // Normalizar email
        nuevoUsuario.setNombreCompleto(request.nombreCompleto().trim());
        nuevoUsuario.setHashContrasena(passwordEncoder.encode(request.password()));
        nuevoUsuario.setMfaHabilitado(false); // MFA deshabilitado por defecto
        
        var usuarioGuardado = usuarioRepository.save(nuevoUsuario);
        log.debug("Usuario persistido con ID: {}", usuarioGuardado.getId());
        
        // 3. Crear membresía en la organización (es_predeterminada=true)
        var membresia = new UsuarioOrganizacion();
        membresia.setUsuarioId(usuarioGuardado.getId());
        membresia.setOrganizacionId(organizacionId);
        membresia.setEstado(EstadoMembresia.ACTIVO);
        membresia.setEsPredeterminada(true); // Primera organización del usuario
        
        usuarioOrganizacionRepository.save(membresia);
        log.info("Usuario {} asignado a organización {} exitosamente", 
            usuarioGuardado.getId(), organizacionId);
        
        // 4. Mapear a DTO de respuesta (sin exponer hash de contraseña)
        return new UserCreatedResponse(
            usuarioGuardado.getId(),
            usuarioGuardado.getEmail(),
            usuarioGuardado.getNombreCompleto(),
            organizacionId,
            usuarioGuardado.getCreatedAt()
        );
    }
}
