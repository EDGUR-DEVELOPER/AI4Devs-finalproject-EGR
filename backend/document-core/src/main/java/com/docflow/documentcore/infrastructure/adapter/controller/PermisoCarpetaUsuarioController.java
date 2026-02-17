package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.CreatePermisoCarpetaUsuarioDTO;
import com.docflow.documentcore.application.dto.NivelAccesoDTO;
import com.docflow.documentcore.application.dto.PermisoCarpetaUsuarioDTO;
import com.docflow.documentcore.application.dto.UpdatePermisoCarpetaUsuarioDTO;
import com.docflow.documentcore.application.dto.UsuarioResumenDTO;
import com.docflow.documentcore.application.mapper.NivelAccesoDtoMapper;
import com.docflow.documentcore.application.mapper.PermisoCarpetaUsuarioMapper;
import com.docflow.documentcore.application.service.PermisoCarpetaUsuarioService;
import com.docflow.documentcore.domain.model.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para gestión de permisos de carpeta por usuario.
 */
@RestController
@RequestMapping("/api/carpetas/{carpetaId}/permisos")
@Tag(name = "ACL - Permisos Carpetas", description = "Endpoints para administrar ACL carpetas")
@SecurityRequirement(name = "bearer-jwt")
public class PermisoCarpetaUsuarioController {

    private final PermisoCarpetaUsuarioService service;
    private final PermisoCarpetaUsuarioMapper permisoMapper;
    private final NivelAccesoDtoMapper nivelAccesoMapper;

    public PermisoCarpetaUsuarioController(
            PermisoCarpetaUsuarioService service,
            PermisoCarpetaUsuarioMapper permisoMapper,
            NivelAccesoDtoMapper nivelAccesoMapper
    ) {
        this.service = service;
        this.permisoMapper = permisoMapper;
        this.nivelAccesoMapper = nivelAccesoMapper;
    }

    @PostMapping
    @Operation(summary = "Crear permiso de carpeta para usuario")
    public ResponseEntity<PermisoCarpetaUsuarioDTO> crearPermiso(
            @PathVariable Long carpetaId,
            @Valid @RequestBody CreatePermisoCarpetaUsuarioDTO request,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioAdminId
    ) {
        PermisoCarpetaUsuario permiso = service.crearPermiso(
                carpetaId,
                request,
                organizacionId,
                usuarioAdminId
        );

        PermisoCarpetaUsuarioDTO response = permisoMapper.toDto(permiso);
        Map<Long, UsuarioResumenDTO> usuarios = service.obtenerUsuariosResumen(List.of(permiso), organizacionId);
        Map<CodigoNivelAcceso, com.docflow.documentcore.domain.model.acl.NivelAcceso> niveles =
                service.obtenerNivelesAccesoActivos();

        response.setUsuario(usuarios.getOrDefault(
                permiso.getUsuarioId(),
                new UsuarioResumenDTO(permiso.getUsuarioId(), null, null)
        ));
        response.setNivelAcceso(resolverNivelAccesoDTO(permiso, niveles));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{usuarioId}")
    @Operation(summary = "Actualizar permiso de carpeta para usuario")
    public ResponseEntity<PermisoCarpetaUsuarioDTO> actualizarPermiso(
            @PathVariable Long carpetaId,
            @PathVariable Long usuarioId,
            @Valid @RequestBody UpdatePermisoCarpetaUsuarioDTO request,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioAdminId
    ) {
        PermisoCarpetaUsuario permiso = service.actualizarPermiso(
                carpetaId,
                usuarioId,
                request,
                organizacionId,
                usuarioAdminId
        );

        PermisoCarpetaUsuarioDTO response = permisoMapper.toDto(permiso);
        Map<Long, UsuarioResumenDTO> usuarios = service.obtenerUsuariosResumen(List.of(permiso), organizacionId);
        Map<CodigoNivelAcceso, com.docflow.documentcore.domain.model.acl.NivelAcceso> niveles =
                service.obtenerNivelesAccesoActivos();

        response.setUsuario(usuarios.getOrDefault(
                permiso.getUsuarioId(),
                new UsuarioResumenDTO(permiso.getUsuarioId(), null, null)
        ));
        response.setNivelAcceso(resolverNivelAccesoDTO(permiso, niveles));

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Listar permisos de una carpeta")
    public ResponseEntity<List<PermisoCarpetaUsuarioDTO>> listarPermisos(
            @PathVariable Long carpetaId,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId
    ) {
        List<PermisoCarpetaUsuario> permisos = service.listarPermisos(carpetaId, organizacionId);
        Map<Long, UsuarioResumenDTO> usuarios = service.obtenerUsuariosResumen(permisos, organizacionId);
        Map<CodigoNivelAcceso, com.docflow.documentcore.domain.model.acl.NivelAcceso> niveles =
                service.obtenerNivelesAccesoActivos();

        List<PermisoCarpetaUsuarioDTO> response = permisos.stream()
                .map(permiso -> {
                    PermisoCarpetaUsuarioDTO dto = permisoMapper.toDto(permiso);
                    UsuarioResumenDTO usuario = usuarios.getOrDefault(
                            permiso.getUsuarioId(),
                            new UsuarioResumenDTO(permiso.getUsuarioId(), null, null)
                    );
                    dto.setUsuario(usuario);
                    dto.setNivelAcceso(resolverNivelAccesoDTO(permiso, niveles));
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

        @DeleteMapping("/{usuarioId}")
        @Operation(summary = "Revocar permiso de carpeta para usuario")
        public ResponseEntity<Void> revocarPermiso(
                        @PathVariable Long carpetaId,
                        @PathVariable Long usuarioId,
                        @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId,
                        @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioAdminId
        ) {
                service.revocarPermiso(carpetaId, usuarioId, organizacionId, usuarioAdminId);
                return ResponseEntity.noContent().build();
        }

    private NivelAccesoDTO resolverNivelAccesoDTO(
            PermisoCarpetaUsuario permiso,
            Map<CodigoNivelAcceso, com.docflow.documentcore.domain.model.acl.NivelAcceso> niveles
    ) {
        if (permiso == null || permiso.getNivelAcceso() == null) {
            return null;
        }
        CodigoNivelAcceso codigo = CodigoNivelAcceso.fromCodigo(permiso.getNivelAcceso().name());
        com.docflow.documentcore.domain.model.acl.NivelAcceso nivel = niveles.get(codigo);
        if (nivel == null) {
            NivelAccesoDTO dto = new NivelAccesoDTO();
            dto.setCodigo(codigo.getCodigo());
            dto.setNombre(codigo.getCodigo());
            return dto;
        }
        return nivelAccesoMapper.toDto(nivel);
    }
}
