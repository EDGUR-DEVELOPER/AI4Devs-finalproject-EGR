package com.docflow.identity.infrastructure.adapters.rest;

import com.docflow.identity.application.dto.*;
import com.docflow.identity.application.services.AdminUserManagementService;
import com.docflow.identity.application.services.RoleAssignmentService;
import com.docflow.identity.application.services.JwtTokenService.TokenValidationResult;
import com.docflow.identity.domain.exception.PermisoInsuficienteException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controlador REST para operaciones administrativas de gestión de usuarios.
 * 
 * Este controlador expone endpoints que requieren autenticación JWT y rol ADMIN
 * en la organización actual del usuario autenticado.
 * 
 * Todos los endpoints respetan el formato ProblemDetail (RFC 7807) para
 * errores.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "User Administration", description = "Endpoints de administración de usuarios (requiere rol ADMIN)")
public class AdminUserController {

    private final AdminUserManagementService adminUserService;
    private final RoleAssignmentService roleAssignmentService;

    /**
     * Crea un nuevo usuario en la organización del administrador autenticado.
     * 
     * El usuario será creado con:
     * - Estado ACTIVO
     * - Contraseña hasheada con BCrypt (cost factor 12)
     * - Asociación automática a la organización del admin (es_predeterminada=true)
     * - MFA deshabilitado por defecto
     * 
     * Validaciones aplicadas:
     * - Email debe ser válido y único en el sistema
     * - Nombre completo debe tener entre 2 y 100 caracteres
     * - Contraseña debe tener mínimo 8 caracteres con mayúscula, minúscula y número
     * 
     * @param request         Datos del usuario a crear (email, nombre, contraseña)
     * @param tokenValidation Información del token JWT del administrador
     *                        autenticado
     * @return Usuario creado con código HTTP 201
     * @throws PermisoInsuficienteException si el usuario no tiene rol ADMIN
     * @throws EmailDuplicadoException      si el email ya existe en el sistema
     */
    @PostMapping
    @Operation(summary = "Crear usuario en la organización", description = "Crea un nuevo usuario y lo asocia a la organización del administrador. "
            +
            "Requiere rol ADMIN. La contraseña se hashea con BCrypt antes de persistir.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación en los datos de entrada"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Usuario sin rol ADMIN"),
            @ApiResponse(responseCode = "409", description = "Email ya registrado en el sistema")
    })
    public ResponseEntity<UserCreatedResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal TokenValidationResult tokenValidation) {

        log.info("Solicitud de creación de usuario por admin: {} (org: {})",
                tokenValidation.usuarioId(), tokenValidation.organizacionId());

        // 1. Verificar rol ADMIN (guard manual hasta implementar @PreAuthorize)
        var hasAdminRole = tokenValidation.roles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));

        if (!hasAdminRole) {
            log.warn("Usuario {} sin permisos de administrador intentó crear usuario",
                    tokenValidation.usuarioId());
            throw new PermisoInsuficienteException(
                    "Se requiere rol ADMIN para crear usuarios en la organización");
        }

        // 2. Delegar creación al servicio con organización del token
        var usuarioCreado = adminUserService.createUser(
                request,
                tokenValidation.organizacionId(),
                tokenValidation.usuarioId());

        log.info("Usuario creado exitosamente con ID: {}", usuarioCreado.id());

        // 3. Retornar 201 Created
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(usuarioCreado);
    }

    /**
     * Asigna un rol a un usuario dentro de la organización del administrador.
     * 
     * El endpoint soporta:
     * - Asignación de roles globales (ADMIN, USER, VIEWER, SUPER_ADMIN)
     * - Asignación de roles custom de la organización
     * - Idempotencia: llamadas repetidas no generan error
     * - Reactivación automática de asignaciones previamente desactivadas
     * 
     * Validaciones de seguridad aplicadas:
     * - Usuario objetivo debe existir y no estar eliminado
     * - Usuario objetivo debe tener membresía activa en la organización
     * - Rol debe existir y estar activo
     * - Rol debe ser global O pertenecer a la organización del admin
     * - Si rol es custom, su organización debe estar activa
     * 
     * Security by obscurity:
     * - Retorna 404 si usuario/rol no existen EN LA ORGANIZACIÓN
     * - No revela información de usuarios/roles de otras organizaciones
     * 
     * MVP: Auditoría sincrónica deshabilitada. Se implementará event publishing en fase 2.
     * 
     * @param usuarioId       ID del usuario al que se asigna el rol
     * @param request         Datos del rol a asignar (rolId)
     * @param tokenValidation Información del token JWT del administrador
     * @return Confirmación de asignación con código HTTP 200
     * @throws PermisoInsuficienteException si el usuario no tiene rol ADMIN (403)
     * @throws ResourceNotFoundException    si usuario/rol no existen en la org
     *                                      (404)
     */
    @PostMapping("/{usuarioId}/roles")
    @Operation(summary = "Asignar rol a usuario en la organización", description = "Permite a un administrador asignar un rol (global o custom) a un usuario de la misma organización. "
            +
            "Soporta idempotencia y reactivación automática.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol asignado exitosamente (o reactivado si existía inactivo)", content = @Content(schema = @Schema(implementation = AssignRoleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Error de validación: rolId inválido o faltante"),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Usuario autenticado no tiene rol ADMIN o SUPER_ADMIN"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado, eliminado, sin membresía activa, o rol no disponible para la organización")
    })
    public ResponseEntity<AssignRoleResponse> assignRoleToUser(
            @PathVariable Long usuarioId,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal TokenValidationResult tokenValidation) {

        log.info("Solicitud de asignación de rol: usuarioId={}, rolId={}, adminId={} (org: {})",
                usuarioId, request.rolId(), tokenValidation.usuarioId(), tokenValidation.organizacionId());

        // 1. Verificar rol ADMIN/SUPER_ADMIN
        var hasAdminRole = tokenValidation.roles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));

        if (!hasAdminRole) {
            log.warn("Usuario {} sin permisos de administrador intentó asignar rol",
                    tokenValidation.usuarioId());
            throw new PermisoInsuficienteException(
                    "Se requiere rol ADMIN o SUPER_ADMIN para asignar roles");
        }

        // 2. Delegar asignación al servicio
        var response = roleAssignmentService.assignRole(
                usuarioId,
                request.rolId(),
                tokenValidation.organizacionId(),
                tokenValidation.usuarioId() // asignadoPor
        );

        log.info("Rol asignado exitosamente: usuarioId={}, rolId={}, reactivado={}",
                usuarioId, request.rolId(), response.reactivado());

        // 3. Retornar 200 OK
        return ResponseEntity.ok(response);
    }

    /**
     * Lista usuarios de la organización con sus roles asignados.
     * 
     * Endpoint con soporte de:
     * - Paginación: page (base 1, default 1), limit (default 20, max 100 forzado)
     * - Búsqueda: busqueda=texto (contains en email o nombre, case-insensitive)
     * - Filtro de estado: ACTIVOS, INACTIVOS, o sin filtro (todos)
     * 
     * Valores de estado soportados:
     * - null o vacío: sin filtro (retorna todos los usuarios)
     * - ACTIVOS: retorna solo usuarios activos
     * - INACTIVOS: retorna solo usuarios desactivados o suspendidos
     * 
     * Seguridad multi-tenant:
     * - organizacionId extraído del token JWT (nunca del request)
     * - Solo retorna usuarios que pertenecen a la organización del admin
     * - Validación manual de rol ADMIN/SUPER_ADMIN
     * 
     * Performance:
     * - Query optimizado con índices (idx_usuarios_roles_org)
     * - Paginación a nivel de base de datos
     * - Agrupación de roles en memoria (eficiente para páginas pequeñas)
     * 
     * Metadata de paginación:
     * - total: total de elementos después de aplicar filtros
     * - pagina: número de página actual (base 1)
     * - limite: elementos por página (forzado a max 100)
     * - totalPaginas: páginas disponibles basado en total/limite
     * 
     * @param tokenValidation Token JWT con información del usuario autenticado
     * @param page            Número de página (base 1, mínimo 1, default 1)
     * @param limit           Elementos por página (mínimo 1, máximo 100, default 20)
     * @param busqueda        Filtro opcional de búsqueda en email o nombre
     * @param estado          Filtro opcional de estado: ACTIVOS, INACTIVOS, o sin valor para todos
     * @return Lista paginada de usuarios con sus roles y metadata de paginación
     */
    @GetMapping
    @Operation(summary = "Listar usuarios de la organización con roles", 
        description = "Retorna lista paginada de usuarios que pertenecen a la organización del token JWT con sus roles asignados. "
            + "Soporta búsqueda por email/nombre y filtro de estado (ACTIVOS/INACTIVOS). " +
            "Límite máximo de 100 elementos por página.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente", content = @Content(schema = @Schema(implementation = ListUsersResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente o inválido"),
            @ApiResponse(responseCode = "403", description = "Usuario sin rol ADMIN o SUPER_ADMIN")
    })
    public ResponseEntity<ListUsersResponseDto> listUsers(
            @AuthenticationPrincipal TokenValidationResult tokenValidation,

            @Parameter(description = "Número de página (base 1)", example = "1") 
            @RequestParam(defaultValue = "1") 
            Integer page,

            @Parameter(description = "Elementos por página (máximo 100)", example = "20") 
            @RequestParam(defaultValue = "20") 
            Integer limit,

            @Parameter(description = "Búsqueda en email o nombre (case-insensitive)", example = "juan") 
            @RequestParam(required = false) 
            String busqueda,

            @Parameter(description = "Filtro de estado: ACTIVOS, INACTIVOS, o sin valor para todos", example = "ACTIVOS") 
            @RequestParam(required = false) 
            String estado
        ) {

        log.info("Solicitud de listado de usuarios por admin: {} (org: {}, page={}, limit={}, busqueda={}, estado={})",
                tokenValidation.usuarioId(), tokenValidation.organizacionId(),
                page, limit, busqueda, estado);

        // 1. Verificar rol ADMIN o SUPER_ADMIN
        var hasAdminRole = tokenValidation.roles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));

        if (!hasAdminRole) {
            log.warn("Usuario {} sin permisos de administrador intentó listar usuarios",
                    tokenValidation.usuarioId());
            throw new PermisoInsuficienteException(
                    "Se requiere rol ADMIN o SUPER_ADMIN para listar usuarios");
        }

        // 2. Extraer organizacionId del token JWT (nunca del request)
        Integer organizacionId = tokenValidation.organizacionId();

        // 3. Delegar al servicio con filtro de estado
        var response = adminUserService.listUsers(
                organizacionId,
                page,
                limit,
                Optional.ofNullable(estado),
                Optional.ofNullable(busqueda));

        log.info("Listado completado: {} usuarios retornados (página {}/{}, estado={})",
                response.usuarios().size(),
                response.paginacion().pagina(),
                response.paginacion().totalPaginas(),
                estado);

        return ResponseEntity.ok(response);
    }

    /**
     * Desactiva un usuario en la organización del administrador.
     * 
     * <p>
     * <b>Efectos:</b>
     * <ul>
     * <li>El usuario no podrá autenticarse nuevamente</li>
     * <li>Los tokens JWT existentes serán rechazados inmediatamente</li>
     * <li>La membresía en la organización se marca como INACTIVA</li>
     * <li>Los datos del usuario se preservan para auditoría</li>
     * </ul>
     * 
     * <p>
     * <b>Restricciones:</b>
     * <ul>
     * <li>Solo usuarios con rol ADMIN o SUPER_ADMIN pueden ejecutar esta
     * acción</li>
     * <li>Un administrador NO puede desactivarse a sí mismo (retorna 400)</li>
     * <li>Solo puede desactivar usuarios de su propia organización (aislamiento
     * multi-tenant)</li>
     * </ul>
     * 
     * @param usuarioId       ID del usuario a desactivar
     * @param tokenValidation Token JWT del administrador (inyectado por Spring
     *                        Security)
     * @return 200 con mensaje de confirmación y timestamp
     */
    @PatchMapping("/{usuarioId}/desactivar")
    @Operation(summary = "Desactivar usuario", description = "Desactiva un usuario en la organización actual sin eliminar sus datos. Los tokens existentes dejarán de funcionar inmediatamente.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "Usuario desactivado exitosamente", content = @Content(schema = @Schema(implementation = DeactivateUserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Intento de auto-desactivación", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente (requiere ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado en la organización", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DeactivateUserResponse> deactivateUser(
            @PathVariable Long usuarioId,
            @AuthenticationPrincipal TokenValidationResult tokenValidation) {

        log.info("Admin {} solicita desactivar usuario {} en organización {}",
                tokenValidation.usuarioId(), usuarioId, tokenValidation.organizacionId());

        // 1. Verificar rol ADMIN o SUPER_ADMIN
        var hasAdminRole = tokenValidation.roles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));

        if (!hasAdminRole) {
            log.warn("Usuario {} sin permisos de administrador intentó desactivar usuario",
                    tokenValidation.usuarioId());
            throw new PermisoInsuficienteException(
                    "Se requiere rol ADMIN o SUPER_ADMIN para desactivar usuarios");
        }

        // 2. Delegar operación al servicio
        var response = adminUserService.deactivateUser(
                usuarioId,
                tokenValidation.organizacionId(),
                tokenValidation.usuarioId() // Para validar auto-desactivación
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Activa un usuario desactivado o suspendido en la organización del administrador.
     * 
     * <p>
     * <b>Efectos:</b>
     * <ul>
     * <li>El usuario podrá autenticarse nuevamente</li>
     * <li>Los nuevos tokens JWT serán válidos</li>
     * <li>La membresía en la organización se marca como ACTIVA</li>
     * </ul>
     * 
     * <p>
     * <b>Restricciones:</b>
     * <ul>
     * <li>Solo usuarios con rol ADMIN o SUPER_ADMIN pueden ejecutar esta acción</li>
     * <li>Solo puede activar usuarios de su propia organización (aislamiento multi-tenant)</li>
     * </ul>
     * 
     * @param usuarioId       ID del usuario a activar
     * @param tokenValidation Token JWT del administrador (inyectado por Spring Security)
     * @return 200 con mensaje de confirmación y timestamp
     */
    @PatchMapping("/{usuarioId}/activar")
    @Operation(summary = "Activar usuario", description = "Activa un usuario desactivado o suspendido en la organización actual permitiéndole autenticarse nuevamente.", security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "Usuario activado exitosamente", content = @Content(schema = @Schema(implementation = DeactivateUserResponse.class))),
            @ApiResponse(responseCode = "403", description = "Rol insuficiente (requiere ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado en la organización", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DeactivateUserResponse> activateUser(
            @PathVariable Long usuarioId,
            @AuthenticationPrincipal TokenValidationResult tokenValidation) {

        log.info("Admin {} solicita activar usuario {} en organización {}",
                tokenValidation.usuarioId(), usuarioId, tokenValidation.organizacionId());

        // 1. Verificar rol ADMIN o SUPER_ADMIN
        var hasAdminRole = tokenValidation.roles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role));

        if (!hasAdminRole) {
            log.warn("Usuario {} sin permisos de administrador intentó activar usuario",
                    tokenValidation.usuarioId());
            throw new PermisoInsuficienteException(
                    "Se requiere rol ADMIN o SUPER_ADMIN para activar usuarios");
        }

        // 2. Delegar operación al servicio
        var response = adminUserService.activateUser(
                usuarioId,
                tokenValidation.organizacionId(),
                tokenValidation.usuarioId() // Para auditoría
        );

        return ResponseEntity.ok(response);
    }
}
