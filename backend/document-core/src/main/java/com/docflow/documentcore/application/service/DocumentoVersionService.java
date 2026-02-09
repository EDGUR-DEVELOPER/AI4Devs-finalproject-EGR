package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.VersionItemResponse;
import com.docflow.documentcore.application.dto.VersionListResponse;
import com.docflow.documentcore.application.mapper.VersionListMapper;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para gestión de listado de versiones de documentos.
 * 
 * US-DOC-004: Proporciona funcionalidad para listar el historial completo de versiones
 * de un documento, con soporte para paginación opcional y validación de permisos.
 * 
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Validar aislamiento multi-tenant (organizacionId)</li>
 *   <li>Validar permisos de LECTURA del usuario sobre el documento</li>
 *   <li>Obtener versiones con o sin paginación</li>
 *   <li>Mapear entidades a DTOs con información completa</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoVersionService {
    
    private final DocumentoRepository documentoRepository;
    private final VersionRepository versionRepository;
    private final IEvaluadorPermisos evaluadorPermisos;
    private final VersionListMapper versionListMapper;
    
    /**
     * Lista todas las versiones de un documento con soporte de paginación opcional.
     * 
     * <p>Flujo de ejecución:</p>
     * <ol>
     *   <li>Validar que el documento existe y pertenece a la organización</li>
     *   <li>Validar que el usuario tiene permiso de LECTURA</li>
     *   <li>Obtener versiones (con o sin paginación)</li>
     *   <li>Mapear a DTOs con flag esVersionActual calculado</li>
     *   <li>Construir y retornar respuesta con metadatos</li>
     * </ol>
     * 
     * @param documentoId ID del documento a consultar
     * @param usuarioId ID del usuario solicitante (desde header X-User-Id)
     * @param organizacionId ID de organización/tenant (desde header X-Organization-Id)
     * @param pagina Número de página opcional (base 1, null = sin paginación)
     * @param tamanio Tamaño de página opcional (default 20, max 100)
     * @return DTO con lista de versiones y metadatos de paginación (si aplica)
     * @throws ResourceNotFoundException si el documento no existe o no pertenece al tenant
     * @throws AccessDeniedException si el usuario no tiene permiso de LECTURA
     */
    @Transactional(readOnly = true)
    public VersionListResponse listarVersiones(
            Long documentoId,
            Long usuarioId,
            Long organizacionId,
            Integer pagina,
            Integer tamanio
    ) {
        log.info("Listando versiones del documento {} para usuario {} en organización {}",
                documentoId, usuarioId, organizacionId);
        
        // Paso 1: Validar que el documento existe y pertenece al tenant
        Documento documento = documentoRepository.findByIdAndOrganizacionId(documentoId, organizacionId)
                .orElseThrow(() -> {
                    log.warn("Documento {} no encontrado o no pertenece a organización {}",
                            documentoId, organizacionId);
                    return new ResourceNotFoundException("Documento", documentoId);
                });
        
        // Paso 2: Validar que el usuario tiene permiso de LECTURA
        boolean tienePermiso = evaluadorPermisos.tieneAcceso(
                usuarioId,
                documentoId,
                TipoRecurso.DOCUMENTO,
                NivelAcceso.LECTURA,
                organizacionId
        );
        
        if (!tienePermiso) {
            log.warn("Usuario {} no tiene permiso de LECTURA sobre documento {} en organización {}",
                    usuarioId, documentoId, organizacionId);
            throw new AccessDeniedException("No tiene permiso para ver las versiones de este documento");
        }
        
        // Paso 3: Obtener versiones (con o sin paginación)
        List<Version> versiones;
        VersionListResponse.PaginacionInfo paginacionInfo = null;
        
        if (pagina != null && pagina > 0) {
            // Con paginación
            int tamanioPagina = (tamanio != null && tamanio > 0) ? tamanio : 20; // Default 20
            Pageable pageable = PageRequest.of(
                    pagina - 1, // Convertir de base 1 a base 0
                    tamanioPagina,
                    Sort.by(Sort.Direction.ASC, "numeroSecuencial")
            );
            
            Page<Version> page = versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(
                    documentoId,
                    pageable
            );
            
            versiones = page.getContent();
            
            // Construir metadatos de paginación
            paginacionInfo = VersionListResponse.PaginacionInfo.builder()
                    .paginaActual(pagina)
                    .tamanio(tamanioPagina)
                    .totalPaginas(page.getTotalPages())
                    .totalElementos((int) page.getTotalElements())
                    .primeraPagina(page.isFirst())
                    .ultimaPagina(page.isLast())
                    .build();
            
            log.debug("Obtenidas {} versiones de la página {} para documento {}",
                    versiones.size(), pagina, documentoId);
        } else {
            // Sin paginación - retornar todas las versiones
            versiones = versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(documentoId);
            
            log.debug("Obtenidas {} versiones (sin paginación) para documento {}",
                    versiones.size(), documentoId);
        }
        
        // Paso 4: Mapear a DTOs
        List<VersionItemResponse> versionesDto = versionListMapper.toItemResponseList(
                versiones,
                documento.getVersionActualId()
        );
        
        // Paso 5: Construir y retornar respuesta
        VersionListResponse response = VersionListResponse.builder()
                .versiones(versionesDto)
                .documentoId(documentoId)
                .totalVersiones(documento.getNumeroVersiones())
                .paginacion(paginacionInfo)
                .build();
        
        log.info("Listado de versiones completado: {} versiones retornadas para documento {}",
                versionesDto.size(), documentoId);
        
        return response;
    }
}
