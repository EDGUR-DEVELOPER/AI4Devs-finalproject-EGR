package com.docflow.documentcore.infrastructure.adapter.controller;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.docflow.documentcore.application.dto.ContenidoCarpetaDTO;
import com.docflow.documentcore.application.mapper.CarpetaContenidoMapper;
import com.docflow.documentcore.application.service.CarpetaContenidoService;
import com.docflow.documentcore.domain.model.ContenidoCarpeta;
import com.docflow.documentcore.domain.model.OpcionesListado;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST para listar contenido de carpetas.
 * 
 * <p>Endpoints para obtener subcarpetas y documentos de una carpeta con filtrado de permisos.</p>
 *
 * @author DocFlow Team
 */
@RestController
@RequestMapping("/api/carpetas")
@Tag(name = "Carpetas", description = "Operaciones con carpetas")
@SecurityRequirement(name = "Bearer Authentication")
public class CarpetaContenidoController {

    private final CarpetaContenidoService service;
    private final CarpetaContenidoMapper mapper;

    public CarpetaContenidoController(
            CarpetaContenidoService service,
            CarpetaContenidoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /**
     * Lista el contenido (subcarpetas y documentos) de una carpeta específica.
     * 
     * @param carpetaId identificador de la carpeta
     * @param usuarioId identificador del usuario (header X-User-Id)
     * @param organizacionId identificador de la organización (header X-Organization-Id)
     * @param pagina número de página (1-indexed, default: 1)
     * @param tamanio tamaño de página (1-100, default: 20)
     * @param campoOrden campo para ordenamiento (default: "nombre")
     * @param direccion dirección de ordenamiento (ASC/DESC, default: ASC)
     * @return ContenidoCarpetaDTO con subcarpetas y documentos
     */
    @GetMapping("/{carpetaId}/contenido")
    @Operation(
            summary = "Listar contenido de carpeta",
            description = "Obtiene subcarpetas y documentos de una carpeta con filtrado de permisos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contenido obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Carpeta no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado a la carpeta"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ContenidoCarpetaDTO> obtenerContenido(
            @PathVariable
            @Parameter(description = "ID de la carpeta", required = true)
            Long carpetaId,

            @RequestHeader(value = "X-User-Id", required = true)
            @Parameter(description = "ID del usuario", required = true)
            Long usuarioId,

            @RequestHeader(value = "X-Organization-Id", required = true)
            @Parameter(description = "ID de la organización", required = true)
            Long organizacionId,

            @RequestParam(name = "pagina", defaultValue = "1")
            @Parameter(description = "Número de página (1-indexed)", example = "1")
            int pagina,

            @RequestParam(name = "tamanio", defaultValue = "20")
            @Parameter(description = "Tamaño de página (1-100)", example = "20")
            int tamanio,

            @RequestParam(name = "campoOrden", defaultValue = "nombre")
            @Parameter(description = "Campo para ordenamiento", example = "nombre")
            String campoOrden,

            @RequestParam(name = "direccion", defaultValue = "ASC")
            @Parameter(description = "Dirección de ordenamiento", example = "ASC")
            Sort.Direction direccion) {

        // Validar y construir opciones
        OpcionesListado opciones = new OpcionesListado(pagina, tamanio, campoOrden, direccion);

        // Obtener contenido del servicio
        ContenidoCarpeta contenido = service.obtenerContenidoCarpeta(
                carpetaId,
                usuarioId,
                organizacionId,
                opciones);

        // Mapear a DTO y retornar
        ContenidoCarpetaDTO dto = mapper.toContenidoCarpetaDTO(contenido);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    /**
     * Lista el contenido de la carpeta raíz de una organización.
     * 
     * @param usuarioId identificador del usuario (header X-User-Id)
     * @param organizacionId identificador de la organización (header X-Organization-Id)
     * @param pagina número de página (1-indexed, default: 1)
     * @param tamanio tamaño de página (1-100, default: 20)
     * @param campoOrden campo para ordenamiento (default: "nombre")
     * @param direccion dirección de ordenamiento (ASC/DESC, default: ASC)
     * @return ContenidoCarpetaDTO con subcarpetas y documentos de raíz
     */
    @GetMapping("/raiz/contenido")
    @Operation(
            summary = "Listar contenido de carpeta raíz",
            description = "Obtiene subcarpetas y documentos de la carpeta raíz de la organización")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contenido obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Carpeta raíz no encontrada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ContenidoCarpetaDTO> obtenerContenidoRaiz(
            @RequestHeader(value = "X-User-Id", required = true)
            @Parameter(description = "ID del usuario", required = true)
            Long usuarioId,

            @RequestHeader(value = "X-Organization-Id", required = true)
            @Parameter(description = "ID de la organización", required = true)
            Long organizacionId,

            @RequestParam(name = "pagina", defaultValue = "1")
            @Parameter(description = "Número de página (1-indexed)", example = "1")
            int pagina,

            @RequestParam(name = "tamanio", defaultValue = "20")
            @Parameter(description = "Tamaño de página (1-100)", example = "20")
            int tamanio,

            @RequestParam(name = "campoOrden", defaultValue = "nombre")
            @Parameter(description = "Campo para ordenamiento", example = "nombre")
            String campoOrden,

            @RequestParam(name = "direccion", defaultValue = "ASC")
            @Parameter(description = "Dirección de ordenamiento", example = "ASC")
            Sort.Direction direccion) {

        // Validar y construir opciones
        OpcionesListado opciones = new OpcionesListado(pagina, tamanio, campoOrden, direccion);

        // Obtener contenido del servicio
        ContenidoCarpeta contenido = service.obtenerContenidoRaiz(
                usuarioId,
                organizacionId,
                opciones);

        // Mapear a DTO y retornar
        ContenidoCarpetaDTO dto = mapper.toContenidoCarpetaDTO(contenido);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
}
