package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.CreatePermisoDocumentoUsuarioDTO;
import com.docflow.documentcore.application.dto.NivelAccesoDTO;
import com.docflow.documentcore.application.dto.PermisoDocumentoUsuarioDTO;
import com.docflow.documentcore.application.dto.UsuarioResumenDTO;
import com.docflow.documentcore.application.mapper.NivelAccesoDtoMapper;
import com.docflow.documentcore.application.mapper.PermisoDocumentoUsuarioMapper;
import com.docflow.documentcore.application.service.PermisoDocumentoUsuarioService;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.permiso.PermisoDocumentoUsuario;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de permisos explícitos de documento por usuario.
 */
@RestController
@RequestMapping("/api/documentos/{documentoId}/permisos")
@Tag(name = "ACL - Document Permissions", description = "Endpoints for document ACL management")
@SecurityRequirement(name = "bearer-jwt")
public class PermisoDocumentoUsuarioController {

    private final PermisoDocumentoUsuarioService service;
    private final PermisoDocumentoUsuarioMapper permisoMapper;
    private final NivelAccesoDtoMapper nivelAccesoMapper;

    public PermisoDocumentoUsuarioController(
            PermisoDocumentoUsuarioService service,
            PermisoDocumentoUsuarioMapper permisoMapper,
            NivelAccesoDtoMapper nivelAccesoMapper
    ) {
        this.service = service;
        this.permisoMapper = permisoMapper;
        this.nivelAccesoMapper = nivelAccesoMapper;
    }

    @PostMapping
    @Operation(summary = "Crear o actualizar permiso de documento para usuario")
    public ResponseEntity<PermisoDocumentoUsuarioDTO> crearOActualizarPermiso(
            @PathVariable Long documentoId,
            @Valid @RequestBody CreatePermisoDocumentoUsuarioDTO request,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioAdminId
    ) {
        // Verificar si existe (para retornar status correcto)
        boolean existePrevio = service.obtenerUsuariosResumen(
                service.listarPermisos(documentoId, organizacionId).stream()
                        .filter(p -> p.getUsuarioId().equals(request.getUsuarioId()))
                        .toList(),
                organizacionId
        ).containsKey(request.getUsuarioId());

        PermisoDocumentoUsuario permiso = service.crearOActualizarPermiso(
                documentoId,
                request,
                organizacionId,
                usuarioAdminId
        );

        PermisoDocumentoUsuarioDTO response = permisoMapper.toDto(permiso);
        Map<Long, UsuarioResumenDTO> usuarios = service.obtenerUsuariosResumen(List.of(permiso), organizacionId);
        Map<CodigoNivelAcceso, com.docflow.documentcore.domain.model.acl.NivelAcceso> niveles =
                service.obtenerNivelesAccesoActivos();

        response.setUsuario(usuarios.getOrDefault(
                permiso.getUsuarioId(),
                new UsuarioResumenDTO(permiso.getUsuarioId(), null, null)
        ));
        response.setNivelAcceso(resolverNivelAccesoDTO(permiso, niveles));

        HttpStatus status = existePrevio ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @DeleteMapping("/{usuarioId}")
    @Operation(summary = "Revocar permiso de documento para usuario")
    public ResponseEntity<Void> revocarPermiso(
            @PathVariable Long documentoId,
            @PathVariable Long usuarioId,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioAdminId
    ) {
        service.revocarPermiso(documentoId, usuarioId, organizacionId, usuarioAdminId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Listar permisos de documento")
    public ResponseEntity<List<PermisoDocumentoUsuarioDTO>> listarPermisos(
            @PathVariable Long documentoId,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId
    ) {
        List<PermisoDocumentoUsuario> permisos = service.listarPermisos(documentoId, organizacionId);
        List<PermisoDocumentoUsuarioDTO> response = permisoMapper.toDtoList(permisos);

        // Enriquecer con datos de usuarios y niveles de acceso
        Map<Long, UsuarioResumenDTO> usuarios = service.obtenerUsuariosResumen(permisos, organizacionId);
        Map<CodigoNivelAcceso, com.docflow.documentcore.domain.model.acl.NivelAcceso> niveles =
                service.obtenerNivelesAccesoActivos();

        for (int i = 0; i < permisos.size(); i++) {
            PermisoDocumentoUsuario permiso = permisos.get(i);
            PermisoDocumentoUsuarioDTO dto = response.get(i);

            dto.setUsuario(usuarios.getOrDefault(
                    permiso.getUsuarioId(),
                    new UsuarioResumenDTO(permiso.getUsuarioId(), null, null)
            ));
            dto.setNivelAcceso(resolverNivelAccesoDTO(permiso, niveles));
        }

        return ResponseEntity.ok(response);
    }

    private NivelAccesoDTO resolverNivelAccesoDTO(
            PermisoDocumentoUsuario permiso,
            Map<CodigoNivelAcceso, com.docflow.documentcore.domain.model.acl.NivelAcceso> niveles
    ) {
        CodigoNivelAcceso codigo = CodigoNivelAcceso.valueOf(permiso.getNivelAcceso().name());
        com.docflow.documentcore.domain.model.acl.NivelAcceso nivelCatalogo = niveles.get(codigo);
        if (nivelCatalogo != null) {
            return nivelAccesoMapper.toDto(nivelCatalogo);
        }
        // Fallback si no hay dato en catálogo
        NivelAccesoDTO fallback = new NivelAccesoDTO();
        fallback.setCodigo(codigo.getCodigo());
        fallback.setNombre(permiso.getNivelAcceso().name());
        return fallback;
    }
}
