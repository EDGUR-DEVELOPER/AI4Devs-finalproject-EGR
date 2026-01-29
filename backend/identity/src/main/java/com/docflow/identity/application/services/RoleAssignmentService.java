package com.docflow.identity.application.services;

import com.docflow.identity.application.dto.AssignRoleResponse;
import com.docflow.identity.domain.exception.ResourceNotFoundException;
import com.docflow.identity.domain.model.UsuarioOrganizacionId;
import com.docflow.identity.domain.model.UsuarioRol;
import com.docflow.identity.domain.model.object.EstadoMembresia;
import com.docflow.identity.domain.model.object.EstadoOrganizacion;
import com.docflow.identity.domain.repository.OrganizacionRepository;
import com.docflow.identity.domain.repository.RolRepository;
import com.docflow.identity.domain.repository.UsuarioOrganizacionRepository;
import com.docflow.identity.domain.repository.UsuarioRepository;
import com.docflow.identity.domain.repository.UsuarioRolRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Servicio de aplicación para asignación de roles a usuarios.
 * Maneja la lógica de negocio de asignación con validaciones de seguridad multi-tenant
 * y reactivación de asignaciones soft-deleted.
 * 
 * MVP: Auditoría de eventos deshabilitada. Se implementará en fase post-MVP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleAssignmentService {
    
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final UsuarioOrganizacionRepository usuarioOrganizacionRepository;
    private final OrganizacionRepository organizacionRepository;
    
    /**
     * Asigna un rol a un usuario dentro de una organización.
     * <p>
     * Validaciones realizadas:
     * <ul>
     *   <li>Usuario existe y no está eliminado (soft delete)</li>
     *   <li>Usuario pertenece a la organización (membresía activa)</li>
     *   <li>Rol existe y está activo</li>
     *   <li>Rol es global O pertenece a la organización del admin</li>
     *   <li>Si rol es custom, su organización está activa</li>
     * </ul>
     * <p>
     * Comportamiento idempotente:
     * <ul>
     *   <li>Si asignación existe y está activa: retorna éxito sin modificar</li>
     *   <li>Si asignación existe pero está inactiva: reactiva con nuevo timestamp</li>
     *   <li>Si no existe: crea nueva asignación</li>
     * </ul>
     * <p>
     * Publica evento {@link com.docflow.identity.domain.events.RolAsignadoEvent} a Kafka
     * para auditoría externa (no bloquea la transacción si falla).
     *
     * @param usuarioId ID del usuario al que se asigna el rol
     * @param rolId ID del rol a asignar
     * @param organizacionId ID de la organización en la que se hace la asignación
     * @param asignadoPor ID del administrador que realiza la asignación
     * @return DTO con información de la asignación realizada
     * @throws ResourceNotFoundException si usuario, rol o membresía no existen (404 - security by obscurity)
     */
    @Transactional
    public AssignRoleResponse assignRole(
            Long usuarioId,
            Integer rolId,
            Integer organizacionId,
            Long asignadoPor
    ) {
        log.debug("Iniciando asignación de rol: usuarioId={}, rolId={}, organizacionId={}, asignadoPor={}",
            usuarioId, rolId, organizacionId, asignadoPor);
        
        // 1. Validar que el usuario existe y no está eliminado
        usuarioRepository.findByIdAndFechaEliminacionIsNull(usuarioId)
            .orElseThrow(() -> {
                log.warn("Usuario no encontrado o eliminado: usuarioId={}", usuarioId);
                return new ResourceNotFoundException("Usuario", usuarioId);
            });
        
        // 2. Validar que el usuario pertenece a la organización (membresía activa)
        var membresiaId = new UsuarioOrganizacionId(usuarioId, organizacionId);
        var membresia = usuarioOrganizacionRepository.findById(membresiaId)
            .orElseThrow(() -> {
                log.warn("Usuario no pertenece a la organización: usuarioId={}, organizacionId={}",
                    usuarioId, organizacionId);
                return new ResourceNotFoundException("Usuario", usuarioId);
            });
        
        if (membresia.getEstado() != EstadoMembresia.ACTIVO) {
            log.warn("Membresía no activa: usuarioId={}, organizacionId={}, estado={}",
                usuarioId, organizacionId, membresia.getEstado());
            throw new ResourceNotFoundException("Usuario", usuarioId);
        }
        
        // 3. Validar que el rol existe y está activo
        var rol = rolRepository.findByIdAndActivoTrue(rolId)
            .orElseThrow(() -> {
                log.warn("Rol no encontrado o inactivo: rolId={}", rolId);
                return new ResourceNotFoundException("Rol", rolId);
            });
        
        // 4. Validar aislamiento: rol debe ser global O pertenecer a la organización del admin
        var rolOrganizacionId = rol.getOrganizacionId();
        boolean esRolGlobal = rolOrganizacionId == null;
        boolean esRolDeLaMismaOrg = rolOrganizacionId != null && rolOrganizacionId.equals(organizacionId);
        
        if (!esRolGlobal && !esRolDeLaMismaOrg) {
            log.warn("Intento de asignar rol de otra organización: rolId={}, rolOrgId={}, adminOrgId={}",
                rolId, rolOrganizacionId, organizacionId);
            throw new ResourceNotFoundException("Rol", rolId);
        }
        
        // 5. Si rol es custom (no global), validar que su organización esté activa
        if (!esRolGlobal) {
            boolean orgActiva = organizacionRepository.existsByIdAndEstado(
                rolOrganizacionId,
                EstadoOrganizacion.ACTIVO
            );
            if (!orgActiva) {
                log.warn("Rol pertenece a organización no activa: rolId={}, rolOrgId={}",
                    rolId, rolOrganizacionId);
                throw new ResourceNotFoundException("Rol", rolId);
            }
        }
        
        // 6. Buscar asignación existente (activa o inactiva)
        var asignacionExistente = usuarioRolRepository.findByUsuarioIdAndRolIdAndOrganizacionId(
            usuarioId, rolId, organizacionId
        );
        
        boolean esReactivacion = false;
        UsuarioRol usuarioRol;
        
        if (asignacionExistente.isPresent()) {
            usuarioRol = asignacionExistente.get();
            
            if (usuarioRol.getActivo()) {
                // Asignación ya existe y está activa: idempotencia
                log.info("Asignación ya existe y está activa (idempotencia): usuarioId={}, rolId={}, orgId={}",
                    usuarioId, rolId, organizacionId);
            } else {
                // Reactivar asignación soft-deleted
                esReactivacion = true;
                usuarioRol.setActivo(true);
                usuarioRol.setFechaAsignacion(OffsetDateTime.now());
                usuarioRol.setAsignadoPor(asignadoPor);
                usuarioRolRepository.save(usuarioRol);
                
                log.info("Reactivando asignación rol {} para usuario {} en org {}",
                    rolId, usuarioId, organizacionId);
            }
        } else {
            // Crear nueva asignación
            usuarioRol = new UsuarioRol();
            usuarioRol.setUsuarioId(usuarioId);
            usuarioRol.setRolId(rolId);
            usuarioRol.setOrganizacionId(organizacionId);
            usuarioRol.setActivo(true);
            usuarioRol.setFechaAsignacion(OffsetDateTime.now());
            usuarioRol.setAsignadoPor(asignadoPor);
            usuarioRolRepository.save(usuarioRol);
            
            log.info("Nueva asignación creada: usuarioId={}, rolId={}, orgId={}", 
                usuarioId, rolId, organizacionId);
        }
        
        // 7. Construir respuesta
        var mensaje = esReactivacion 
            ? "Rol reactivado correctamente" 
            : "Rol asignado correctamente";
        
        return new AssignRoleResponse(
            mensaje,
            usuarioId,
            rolId,
            rol.getCodigo(),
            usuarioRol.getFechaAsignacion(),
            esReactivacion
        );
    }
}
