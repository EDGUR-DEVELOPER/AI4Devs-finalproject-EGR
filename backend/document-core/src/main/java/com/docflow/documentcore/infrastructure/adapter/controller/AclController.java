package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.NivelAccesoDTO;
import com.docflow.documentcore.application.mapper.NivelAccesoDtoMapper;
import com.docflow.documentcore.application.service.NivelAccesoService;
import com.docflow.documentcore.application.validator.NivelAccesoValidator;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Access Level (ACL) operations
 * Base path: /api/acl/niveles
 */
@RestController
@RequestMapping("/api/acl/niveles")
@Tag(name = "ACL - Access Levels", description = "Endpoints for access level catalog management")
public class AclController {
    
    private static final Logger log = LoggerFactory.getLogger(AclController.class);
    
    private final NivelAccesoService service;
    private final NivelAccesoValidator validator;
    private final NivelAccesoDtoMapper mapper;

    public AclController(
            NivelAccesoService service,
            NivelAccesoValidator validator,
            NivelAccesoDtoMapper mapper) {
        this.service = service;
        this.validator = validator;
        this.mapper = mapper;
    }

    /**
     * GET /api/acl/niveles
     * List all active access levels
     */
    @GetMapping
    @Operation(summary = "List all active access levels", 
               description = "Returns all active access levels ordered by 'orden' field")
    public ResponseEntity<List<NivelAccesoDTO>> listActiveAccessLevels() {
        log.info("GET /api/acl/niveles - Listing active access levels");
        List<NivelAcceso> niveles = service.listAllActive();
        List<NivelAccesoDTO> dtos = niveles.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/acl/niveles/all
     * List all access levels including inactive
     */
    @GetMapping("/all")
    @Operation(summary = "List all access levels (including inactive)", 
               description = "Returns all access levels ordered by 'orden' field")
    public ResponseEntity<List<NivelAccesoDTO>> listAllAccessLevels() {
        log.info("GET /api/acl/niveles/all - Listing all access levels");
        List<NivelAcceso> niveles = service.listAll();
        List<NivelAccesoDTO> dtos = niveles.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/acl/niveles/{id}
     * Get access level by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get access level by ID", 
               description = "Returns a specific access level by Long")
    public ResponseEntity<NivelAccesoDTO> getAccessLevelById(@PathVariable Long id) {
        log.info("GET /api/acl/niveles/{} - Fetching access level", id);
        validator.validateExistsById(id);
        NivelAcceso nivel = service.getById(id);
        return ResponseEntity.ok(mapper.toDto(nivel));
    }

    /**
     * GET /api/acl/niveles/codigo/{codigo}
     * Get access level by codigo
     */
    @GetMapping("/codigo/{codigo}")
    @Operation(summary = "Get access level by codigo", 
               description = "Returns a specific access level by codigo (LECTURA, ESCRITURA, ADMINISTRACION)")
    public ResponseEntity<NivelAccesoDTO> getAccessLevelByCodigo(@PathVariable String codigo) {
        log.info("GET /api/acl/niveles/codigo/{} - Fetching access level", codigo);
        CodigoNivelAcceso codigoEnum = validator.validateCodigoFormat(codigo);
        NivelAcceso nivel = service.getByCodigo(codigoEnum);
        return ResponseEntity.ok(mapper.toDto(nivel));
    }

    /**
     * GET /api/acl/niveles/{id}/permisos/{accion}
     * Check if a specific action is permitted for an access level
     */
    @GetMapping("/{id}/permisos/{accion}")
    @Operation(summary = "Check if action is permitted", 
               description = "Verifies if a specific action is allowed for the given access level")
    public ResponseEntity<Boolean> checkPermission(
            @PathVariable Long id,
            @PathVariable String accion) {
        log.info("GET /api/acl/niveles/{}/permisos/{} - Checking permission", id, accion);
        boolean isPermitted = service.isAccionPermitida(id, accion);
        return ResponseEntity.ok(isPermitted);
    }
}
