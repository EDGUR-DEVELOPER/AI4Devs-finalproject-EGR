package com.docflow.identity.application.services;

import com.docflow.identity.application.dto.*;
import com.docflow.identity.application.ports.UserWithRolesProjection;
import com.docflow.identity.application.ports.UsuarioOrganizacionRepository;
import com.docflow.identity.application.ports.UsuarioRepository;
import com.docflow.identity.domain.exceptions.AutoDeactivationNotAllowedException;
import com.docflow.identity.domain.exceptions.EmailDuplicadoException;
import com.docflow.identity.domain.exceptions.ResourceNotFoundException;
import com.docflow.identity.domain.model.EstadoMembresia;
import com.docflow.identity.domain.model.Rol;
import com.docflow.identity.domain.model.Usuario;
import com.docflow.identity.domain.model.UsuarioOrganizacion;
import com.docflow.identity.domain.model.UsuarioOrganizacionId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de usuarios por parte de administradores.
 * Implementa operaciones CRUD sobre usuarios dentro del contexto de una
 * organización.
 * 
 * Este servicio sigue el principio de responsabilidad única, enfocándose
 * exclusivamente
 * en la lógica de negocio relacionada con la administración de usuarios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserManagementService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioOrganizacionRepository usuarioOrganizacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDtoMapper userDtoMapper;

    /**
     * Crea un nuevo usuario y lo asocia a la organización especificada.
     * 
     * El proceso incluye:
     * 1. Validación de unicidad del email (case-insensitive)
     * 2. Creación de la entidad Usuario con contraseña hasheada (BCrypt)
     * 3. Creación de la membresía en la organización (estado ACTIVO,
     * es_predeterminada=true)
     * 
     * @param request        Datos del usuario a crear (email, nombre, contraseña)
     * @param organizacionId ID de la organización del administrador que crea el
     *                       usuario
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
                    String.format("Ya existe un usuario registrado con el email: %s", request.email()));
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
                usuarioGuardado.getFechaCreacion());
    }

    /**
     * Lista usuarios de una organización con sus roles asignados, soportando
     * paginación y filtros.
     * 
     * Proceso de ejecución:
     * 1. Validación y normalización de parámetros de paginación (page >= 1, limit
     * <= 100)
     * 2. Query JPQL con INNER/LEFT JOINs para obtener proyecciones (1 fila por
     * usuario-rol)
     * 3. Agrupación en memoria usando streams con LinkedHashMap (preserva orden
     * original)
     * 4. Mapeo de proyecciones a entidades dummy para MapStruct
     * 5. Aplicación de filtros opcionales en memoria (estado, búsqueda)
     * 6. Recálculo de metadata de paginación post-filtro
     * 
     * Seguridad multi-tenant:
     * - organizacionId viene del token JWT (nunca del request)
     * - Query filtra por organizacion_id a nivel de BD
     * - Solo retorna usuarios con membresía en la organización
     * 
     * Performance:
     * - Aprovecha índice idx_usuarios_roles_org
     * - Paginación a nivel de BD (OFFSET/LIMIT)
     * - Filtros aplicados post-query para simplificar lógica JPQL
     * 
     * @param organizacionId ID de la organización (extraído del token JWT)
     * @param page           Número de página (base 1, mínimo 1)
     * @param limit          Elementos por página (mínimo 1, máximo 100 forzado)
     * @param estado         Filtro opcional por estado de membresía (ACTIVO,
     *                       SUSPENDIDO)
     * @param busqueda       Filtro opcional de búsqueda en email o nombre
     *                       (case-insensitive, contains)
     * @return DTO con lista de usuarios paginada y metadata de paginación
     */
    @Transactional(readOnly = true)
    public ListUsersResponseDto listUsers(
            Integer organizacionId,
            Integer page,
            Integer limit,
            Optional<String> estado,
            Optional<String> busqueda) {

        log.info("Listando usuarios de organización {} (page={}, limit={}, estado={}, busqueda={})",
                organizacionId, page, limit, estado.orElse("ALL"), busqueda.orElse("NONE"));

        // 1. Validar y normalizar parámetros de paginación
        int validatedPage = Math.max(1, page); // Mínimo página 1
        int validatedLimit = Math.min(Math.max(1, limit), 100); // Rango [1, 100]

        log.debug("Parámetros validados: page={}, limit={}", validatedPage, validatedLimit);

        // 2. Construir Pageable con ordenamiento por ID de usuario (preserva orden de
        // inserción)
        var pageable = PageRequest.of(
                validatedPage - 1, // Convertir de base-1 a base-0
                validatedLimit,
                Sort.by("usuarioId").ascending());

        // 3. Ejecutar query JPQL con constructor expression
        Page<UserWithRolesProjection> proyeccionesPage = usuarioRepository
                .findUsersWithRolesByOrganizacion(organizacionId, pageable);

        log.debug("Query retornó {} proyecciones de {} totales",
                proyeccionesPage.getContent().size(), proyeccionesPage.getTotalElements());

        // 4. Agrupar proyecciones por usuarioId (1 usuario → N roles)
        // Usar LinkedHashMap para preservar orden original de inserción
        Map<Long, List<UserWithRolesProjection>> usuariosAgrupados = proyeccionesPage
                .getContent()
                .stream()
                .collect(Collectors.groupingBy(
                        UserWithRolesProjection::getUsuarioId,
                        LinkedHashMap::new, // Preserva orden
                        Collectors.toList()));

        log.debug("Proyecciones agrupadas en {} usuarios únicos", usuariosAgrupados.size());

        // 5. Mapear cada grupo de proyecciones a UserWithRolesDto
        List<UserWithRolesDto> usuariosDto = usuariosAgrupados.entrySet().stream()
                .map(entry -> {
                    List<UserWithRolesProjection> proyeccionesUsuario = entry.getValue();

                    // Tomar primera proyección para datos del usuario (todas tienen mismos datos de
                    // usuario)
                    UserWithRolesProjection primeraProyeccion = proyeccionesUsuario.get(0);

                    // Crear entidad Usuario dummy para MapStruct
                    Usuario usuarioDummy = new Usuario();
                    usuarioDummy.setId(primeraProyeccion.getUsuarioId());
                    usuarioDummy.setEmail(primeraProyeccion.getEmail());
                    usuarioDummy.setNombreCompleto(primeraProyeccion.getNombreCompleto());
                    usuarioDummy.setFechaCreacion(primeraProyeccion.getFechaCreacion());

                    List<RoleSummaryDto> rolesDto = proyeccionesUsuario.stream()
                            .filter(p -> p.getRolId() != null) // Excluir usuarios sin roles (LEFT JOIN)
                            .map(p -> {
                                // Crear entidad Rol dummy para MapStruct
                                Rol rolDummy = new Rol();
                                rolDummy.setId(p.getRolId());
                                rolDummy.setCodigo(p.getRolCodigo());
                                rolDummy.setNombre(p.getRolNombre());
                                return userDtoMapper.toRoleSummaryDto(rolDummy);
                            })
                            .distinct() // Evitar duplicados si query retorna mismo rol múltiples veces
                            .toList();

                    // Mapear a UserWithRolesDto usando MapStruct (incluye estado de membresía)
                    return new UserWithRolesDto(
                            usuarioDummy.getId(),
                            usuarioDummy.getEmail(),
                            usuarioDummy.getNombreCompleto(),
                            primeraProyeccion.getEstado(), // Estado de membresía (ACTIVO/SUSPENDIDO)
                            rolesDto,
                            usuarioDummy.getFechaCreacion());
                })
                .toList();

        log.debug("Mapeados {} usuarios a DTOs", usuariosDto.size());

        // 6. Aplicar filtros opcionales en memoria
        List<UserWithRolesDto> usuariosFiltrados = usuariosDto.stream()
                .filter(usuario -> {
                    // Filtro por estado (exacto)
                    boolean matchEstado = estado.isEmpty() ||
                            usuario.estado().equalsIgnoreCase(estado.get());

                    // Filtro por búsqueda (contains en email o nombre, case-insensitive)
                    boolean matchBusqueda = busqueda.isEmpty() ||
                            usuario.email().toLowerCase().contains(busqueda.get().toLowerCase()) ||
                            usuario.nombreCompleto().toLowerCase().contains(busqueda.get().toLowerCase());

                    return matchEstado && matchBusqueda;
                })
                .toList();

        log.debug("Después de filtros: {} usuarios", usuariosFiltrados.size());

        // 7. Recalcular metadata de paginación sobre lista filtrada
        int totalFiltrados = usuariosFiltrados.size();
        int totalPaginas = (int) Math.ceil((double) totalFiltrados / validatedLimit);

        PaginationMetadataDto paginacion = new PaginationMetadataDto(
                totalFiltrados,
                validatedPage,
                validatedLimit,
                Math.max(1, totalPaginas) // Mínimo 1 página incluso si no hay datos
        );

        log.info("Listado completado: {} usuarios retornados de {} totales (página {}/{})",
                usuariosFiltrados.size(), totalFiltrados, validatedPage, totalPaginas);

        // 8. Retornar respuesta con usuarios y metadata
        return new ListUsersResponseDto(usuariosFiltrados, paginacion);
    }

    /**
     * Desactiva un usuario en una organización específica.
     * 
     * <p>
     * Esta operación:
     * <ul>
     * <li>Valida que el usuario exista y pertenezca a la organización</li>
     * <li>Actualiza el estado del usuario a INACTIVO</li>
     * <li>Actualiza la membresía organizacional a INACTIVO</li>
     * <li>Registra la fecha de desactivación para auditoría</li>
     * </ul>
     * 
     * <p>
     * <b>Efecto en autenticación:</b> Los tokens JWT existentes del usuario
     * serán rechazados en el próximo request debido a la validación en tiempo real
     * del filtro de autenticación.
     * 
     * @param usuarioId      ID del usuario a desactivar
     * @param organizacionId ID de la organización del administrador que ejecuta la
     *                       acción
     * @param adminId        ID del administrador ejecutando la acción (para evitar
     *                       auto-desactivación)
     * @return Respuesta con confirmación y timestamp de desactivación
     * @throws AutoDeactivationNotAllowedException si el admin intenta desactivarse
     *                                             a sí mismo
     * @throws ResourceNotFoundException           si el usuario no existe o no
     *                                             pertenece a la organización
     */
    @Transactional
    public DeactivateUserResponse deactivateUser(
            Long usuarioId,
            Integer organizacionId,
            Long adminId) {

        log.info("Iniciando desactivación de usuario {} en organización {} por admin {}",
                usuarioId, organizacionId, adminId);

        // 1. Validación de auto-desactivación
        if (usuarioId.equals(adminId)) {
            log.warn("Admin {} intentó auto-desactivarse", adminId);
            throw new AutoDeactivationNotAllowedException(
                    "Un administrador no puede desactivarse a sí mismo. " +
                            "Solicite a otro administrador realizar esta acción.");
        }

        // 2. Buscar usuario (usar método que excluye soft-deleted)
        var usuario = usuarioRepository.findByIdAndFechaEliminacionIsNull(usuarioId)
                .orElseThrow(() -> {
                    log.error("Usuario {} no encontrado o ya eliminado", usuarioId);
                    return new ResourceNotFoundException(
                            "Usuario no encontrado o ya eliminado",
                            usuarioId);
                });

        // 3. Verificar membresía activa en la organización
        var membresiaId = new UsuarioOrganizacionId(usuarioId, organizacionId);
        var membresia = usuarioOrganizacionRepository.findById(membresiaId)
                .orElseThrow(() -> {
                    log.error("Usuario {} no pertenece a organización {}", usuarioId, organizacionId);
                    return new ResourceNotFoundException(
                            "Usuario no encontrado en esta organización",
                            usuarioId);
                });

        // 4. Validar que no esté ya desactivado (idempotencia parcial)
        if (membresia.getEstado() == EstadoMembresia.INACTIVO) {
            log.info("Usuario {} ya estaba desactivado en organización {}", usuarioId, organizacionId);
            return new DeactivateUserResponse(
                    usuarioId,
                    "Usuario ya estaba desactivado",
                    usuario.getFechaDesactivacion());
        }

        // 5. Desactivar usuario (método de dominio)
        usuario.desactivar();
        usuarioRepository.save(usuario);

        // 6. Desactivar membresía organizacional
        membresia.setEstado(EstadoMembresia.INACTIVO);
        usuarioOrganizacionRepository.save(membresia);

        log.info("Usuario {} desactivado exitosamente en organización {} por admin {}",
                usuarioId, organizacionId, adminId);

        return new DeactivateUserResponse(
                usuarioId,
                "Usuario desactivado exitosamente",
                usuario.getFechaDesactivacion());
    }
}
