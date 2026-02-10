package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.CarpetaDTO;
import com.docflow.documentcore.application.dto.CreateCarpetaDTO;
import com.docflow.documentcore.application.mapper.CarpetaDtoMapper;
import com.docflow.documentcore.application.service.CarpetaService;
import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.infrastructure.security.RequierePermiso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para operaciones con carpetas.
 * 
 * <p>Expone endpoints para gestión de la estructura jerárquica de carpetas
 * en DocFlow.</p>
 *
 * <p><strong>Endpoints:</strong>
 * <ul>
 *   <li>POST /api/carpetas - Crear carpeta</li>
 *   <li>GET /api/carpetas/{id} - Obtener carpeta por ID</li>
 *   <li>GET /api/carpetas/{id}/hijos - Listar carpetas hijas</li>
 *   <li>GET /api/carpetas/raiz - Obtener carpeta raíz de la organización</li>
 *   <li>DELETE /api/carpetas/{id} - Eliminar carpeta (soft delete)</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@RestController
@RequestMapping("/api/carpetas")
@Tag(name = "Carpetas", description = "API para gestión de carpetas jerárquicas")
@SecurityRequirement(name = "bearer-jwt")
public class CarpetaController {
    
    private final CarpetaService carpetaService;
    private final CarpetaDtoMapper mapper;
    
    public CarpetaController(CarpetaService carpetaService, CarpetaDtoMapper mapper) {
        this.carpetaService = carpetaService;
        this.mapper = mapper;
    }
    
    /**
     * Crea una nueva carpeta.
     * 
     * <p>Los valores de organizacion_id y usuario_id se extraen del token JWT,
     * NO del cuerpo de la petición (seguridad multi-tenant).</p>
     */
    @PostMapping
    @Operation(
        summary = "Crear una nueva carpeta",
        description = "Crea una carpeta dentro de una carpeta padre existente. Requiere permisos de ESCRITURA o ADMINISTRACION en la carpeta padre."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Carpeta creada exitosamente",
            content = @Content(schema = @Schema(implementation = CarpetaDTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "403", description = "Sin permisos para crear carpetas en esta ubicación"),
        @ApiResponse(responseCode = "404", description = "Carpeta padre no encontrada"),
        @ApiResponse(responseCode = "409", description = "Ya existe una carpeta con ese nombre en esta ubicación")
    })
    public ResponseEntity<CarpetaDTO> crear(
            @Valid @RequestBody CreateCarpetaDTO request,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") 
            @Parameter(description = "ID de la organización desde el token JWT", hidden = true)
            Long organizacionId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") 
            @Parameter(description = "ID del usuario desde el token JWT", hidden = true)
            Long usuarioId
    ) {
        Carpeta carpeta = carpetaService.crear(
                request.getNombre(),
                request.getDescripcion(),
                request.getCarpetaPadreId(),
                organizacionId,
                usuarioId
        );
        
        CarpetaDTO response = mapper.toDto(carpeta);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Obtiene una carpeta por su ID.
     */
    @GetMapping("/{id}")
    @RequierePermiso(
        tipoRecurso = TipoRecurso.CARPETA,
        nivelRequerido = CodigoNivelAcceso.LECTURA,
        errorMessage = "No tienes permisos para ver esta carpeta"
    )
    @Operation(
        summary = "Obtener carpeta por ID",
        description = "Busca una carpeta específica por su identificador único."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Carpeta encontrada",
            content = @Content(schema = @Schema(implementation = CarpetaDTO.class))
        ),
        @ApiResponse(responseCode = "403", description = "Sin permisos para ver esta carpeta"),
        @ApiResponse(responseCode = "404", description = "Carpeta no encontrada")
    })
    public ResponseEntity<CarpetaDTO> obtenerPorId(
            @PathVariable Long id,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId
    ) {
        Carpeta carpeta = carpetaService.obtenerPorId(id, organizacionId);
        CarpetaDTO response = mapper.toDto(carpeta);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lista las carpetas hijas de una carpeta padre.
     */
    @GetMapping("/{id}/hijos")
    @RequierePermiso(
        tipoRecurso = TipoRecurso.CARPETA,
        nivelRequerido = CodigoNivelAcceso.LECTURA,
        errorMessage = "No tienes permisos para ver el contenido de esta carpeta"
    )
    @Operation(
        summary = "Listar carpetas hijas",
        description = "Obtiene todas las subcarpetas directas de una carpeta padre."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de carpetas hijas",
            content = @Content(schema = @Schema(implementation = CarpetaDTO.class))
        ),
        @ApiResponse(responseCode = "403", description = "Sin permisos para ver el contenido de esta carpeta")
    })
    public ResponseEntity<List<CarpetaDTO>> obtenerHijos(
            @PathVariable Long id,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId
    ) {
        List<Carpeta> hijos = carpetaService.obtenerHijos(id, organizacionId);
        List<CarpetaDTO> response = mapper.toDtoList(hijos);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtiene la carpeta raíz de la organización.
     */
    @GetMapping("/raiz")
    @Operation(
        summary = "Obtener carpeta raíz",
        description = "Obtiene la carpeta raíz de la organización actual."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Carpeta raíz encontrada",
            content = @Content(schema = @Schema(implementation = CarpetaDTO.class))
        ),
        @ApiResponse(responseCode = "404", description = "Carpeta raíz no encontrada")
    })
    public ResponseEntity<CarpetaDTO> obtenerRaiz(
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId
    ) {
        Carpeta raiz = carpetaService.obtenerRaiz(organizacionId);
        CarpetaDTO response = mapper.toDto(raiz);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Elimina lógicamente una carpeta vacía.
     */
    @DeleteMapping("/{id}")
    @RequierePermiso(
        tipoRecurso = TipoRecurso.CARPETA,
        nivelRequerido = CodigoNivelAcceso.ADMINISTRACION,
        errorMessage = "No tienes permisos de administración para eliminar esta carpeta"
    )
    @Operation(
        summary = "Eliminar carpeta vacía",
        description = "Realiza eliminación lógica de una carpeta vacía (soft delete). "
                + "Requiere permisos de ADMINISTRACION y que no tenga subcarpetas ni documentos activos."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Carpeta eliminada exitosamente"),
        @ApiResponse(responseCode = "400", description = "No se puede eliminar una carpeta raíz"),
        @ApiResponse(responseCode = "403", description = "Sin permisos para eliminar esta carpeta"),
        @ApiResponse(responseCode = "404", description = "Carpeta no encontrada"),
        @ApiResponse(responseCode = "409", description = "Carpeta contiene subcarpetas o documentos activos")
    })
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1")
            @Parameter(description = "ID de la organización desde el token JWT", hidden = true)
            Long organizacionId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1")
            @Parameter(description = "ID del usuario desde el token JWT", hidden = true)
            Long usuarioId
    ) {
        carpetaService.eliminarCarpeta(id, usuarioId, organizacionId);
        return ResponseEntity.noContent().build();
    }
}
