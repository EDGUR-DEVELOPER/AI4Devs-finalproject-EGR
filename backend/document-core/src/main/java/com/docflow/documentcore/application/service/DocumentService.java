package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.CreateDocumentoRequest;
import com.docflow.documentcore.application.dto.CreateVersionRequest;
import com.docflow.documentcore.application.dto.DocumentoResponse;
import com.docflow.documentcore.application.dto.DownloadDocumentDto;
import com.docflow.documentcore.application.dto.VersionResponse;
import com.docflow.documentcore.application.mapper.DocumentoMapper;
import com.docflow.documentcore.application.validator.DocumentValidator;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.DocumentValidationException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.exception.StorageException;
import com.docflow.documentcore.domain.event.DocumentDownloadedEvent;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import com.docflow.documentcore.infrastructure.security.SecurityContext;
import com.docflow.documentcore.infrastructure.util.MimeTypeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;

/**
 * Servicio de aplicación para gestión de documentos.
 * 
 * US-DOC-001: Orquesta la creación de documentos con versionado y almacenamiento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentoRepository documentoRepository;
    private final VersionRepository versionRepository;
    private final StorageService storageService;
    private final DocumentValidator documentValidator;
    private final DocumentoMapper documentoMapper;
    private final SecurityContext securityContext;
    private final IEvaluadorPermisos evaluadorPermisos;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Crea un nuevo documento con su primera versión.
     * 
     * Flujo:
     * 1. Validar permisos de carpeta
     * 2. Validar archivo
     * 3. Guardar archivo en almacenamiento
     * 4. Crear registro de documento en BD
     * 5. Crear registro de versión en BD
     * 6. Actualizar referencia de versión actual
     *
     * @param carpetaId ID de la carpeta destino
     * @param request Solicitud con archivo y metadatos
     * @return DTO con información del documento creado
     */
    @Transactional
    public DocumentoResponse createDocument(Long carpetaId, CreateDocumentoRequest request) {
        
        // 1. Obtener contexto de seguridad
        Long organizacionId = securityContext.getOrganizacionId();
        Long usuarioId = securityContext.getUsuarioId();
        
        log.info("Iniciando creación de documento en carpeta {} para usuario {} de organización {}", 
            carpetaId, usuarioId, organizacionId);
        
        // 2. Validar archivo
        documentValidator.validateFile(request.getFile());
        
        // 3. Extraer información del archivo
        String originalFilename = request.getFile().getOriginalFilename();
        String nombre = request.getNombrePersonalizado() != null 
            ? request.getNombrePersonalizado() 
            : documentValidator.getBaseName(originalFilename);
        String extension = documentValidator.getExtension(originalFilename);
        String contentType = request.getFile().getContentType();
        long fileSize = request.getFile().getSize();
        
        // Validar nombre
        documentValidator.validateDocumentName(nombre);
        
        // 4. Verificar duplicados
        boolean exists = documentoRepository.existsByNombreAndCarpetaIdAndOrganizacionIdAndFechaEliminacionIsNull(
            nombre, carpetaId, organizacionId
        );
        if (exists) {
            throw new DocumentValidationException(
                String.format("Ya existe un documento con el nombre '%s' en esta carpeta", nombre)
            );
        }
        
        // 5. Crear entidad Documento (sin guardar aún)
        Documento documento = new Documento();
        documento.setOrganizacionId(organizacionId);
        documento.setCarpetaId(carpetaId);
        documento.setNombre(nombre);
        documento.setExtension(extension);
        documento.setTipoContenido(contentType);
        documento.setTamanioBytes(fileSize);
        documento.setNumeroVersiones(1);
        documento.setBloqueado(false);
        documento.setCreadoPor(usuarioId);
        documento.setFechaCreacion(OffsetDateTime.now());
        documento.setFechaActualizacion(OffsetDateTime.now());
        
        // Guardar documento para obtener ID
        documento = documentoRepository.save(documento);
        log.info("Documento creado con ID: {}", documento.getId());
        
        try {
            // 6. Calcular hash SHA256 del contenido
            String hashContenido = calculateSHA256(request.getFile().getBytes());
            
            // 7. Guardar archivo en almacenamiento
            String storagePath = storageService.upload(
                organizacionId,
                carpetaId,
                documento.getId(),
                1, // Primera versión
                request.getFile().getInputStream(),
                fileSize
            );
            
            // 8. Crear versión
            Version version = new Version();
            version.setDocumentoId(documento.getId());
            version.setNumeroSecuencial(1);
            version.setTamanioBytes(fileSize);
            version.setRutaAlmacenamiento(storagePath);
            version.setHashContenido(hashContenido);
            version.setComentarioCambio(request.getComentarioCambio());
            version.setCreadoPor(usuarioId);
            version.setFechaCreacion(OffsetDateTime.now());
            version.setDescargas(0);
            
            version = versionRepository.save(version);
            log.info("Versión 1 creada con ID: {}", version.getId());
            
            // 9. Actualizar referencia a versión actual en documento
            documento.setVersionActualId(version.getId());
            documento = documentoRepository.save(documento);
            
            // 10. Construir respuesta
            DocumentoResponse response = documentoMapper.toResponse(documento);
            response.setVersionActual(documentoMapper.toVersionInfoDTO(version));
            
            log.info("Documento {} creado exitosamente con versión 1", documento.getId());
            return response;
            
        } catch (IOException e) {
            // Rollback: eliminar documento si falla el almacenamiento
            documentoRepository.delete(documento);
            throw new StorageException("Error al procesar el archivo", e);
        } catch (Exception e) {
            // Rollback automático por @Transactional
            log.error("Error al crear documento", e);
            throw new RuntimeException("Error al crear documento: " + e.getMessage(), e);
        }
    }
    
    /**
     * Crea una nueva versión de un documento existente.
     * 
     * <p>Este método implementa US-DOC-003 y realiza las siguientes operaciones:</p>
     * <ol>
     *   <li>Validar que el documento existe y pertenece a la organización (tenant isolation)</li>
     *   <li>Validar que el usuario tiene permiso de ESCRITURA sobre el documento</li>
     *   <li>Validar el archivo (tamaño, formato, no vacío)</li>
     *   <li>Calcular hash SHA256 del contenido</li>
     *   <li>Obtener siguiente número secuencial (última versión + 1)</li>
     *   <li>Subir archivo a almacenamiento</li>
     *   <li>Crear registro de versión en BD</li>
     *   <li>Actualizar referencia de versión actual en documento</li>
     * </ol>
     *
     * @param documentId ID del documento al que agregar nueva versión
     * @param request Solicitud con archivo y comentario opcional
     * @return DTO con información de la nueva versión creada
     * @throws ResourceNotFoundException si el documento no existe o no pertenece a la organización (404)
     * @throws AccessDeniedException si el usuario no tiene permiso de ESCRITURA (403)
     * @throws DocumentValidationException si el archivo no es válido (400)
     * @throws StorageException si falla el almacenamiento (500)
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public VersionResponse createVersion(Long documentId, CreateVersionRequest request) {
        // 1. Obtener contexto de seguridad
        Long organizacionId = securityContext.getOrganizacionId();
        Long usuarioId = securityContext.getUsuarioId();
        
        log.info("Iniciando creación de nueva versión - documentoId={}, usuarioId={}, organizacionId={}", 
            documentId, usuarioId, organizacionId);
        
        // 2. Validar que documento existe y pertenece a la organización
        Documento documento = documentoRepository.findByIdAndOrganizacionId(
            documentId, organizacionId
        ).orElseThrow(() -> new ResourceNotFoundException(
            "Documento no encontrado con id: " + documentId
        ));
        
        log.debug("Documento encontrado: id={}, nombre={}", documento.getId(), documento.getNombre());
        
        // 3. Validar permisos de ESCRITURA
        if (!evaluadorPermisos.tieneAcceso(
            usuarioId, 
            documentId,
            TipoRecurso.DOCUMENTO,
            NivelAcceso.ESCRITURA,
            organizacionId
        )) {
            log.warn("Acceso denegado: usuario {} no tiene permiso de ESCRITURA en documento {}", 
                usuarioId, documentId);
            throw new AccessDeniedException(
                "No tiene permisos para crear versiones en este documento"
            );
        }
        
        // 4. Validar archivo
        documentValidator.validateFile(request.getFile());
        long fileSize = request.getFile().getSize();
        
        log.debug("Archivo validado: tamaño={} bytes", fileSize);
        
        try {
            // 5. Calcular hash SHA256 del contenido
            String hashContenido = calculateSHA256(request.getFile().getBytes());
            log.debug("Hash calculado: {}", hashContenido);
            
            // 6. Obtener siguiente número secuencial
            Integer ultimoNumero = documento.getNumeroVersiones();
            Integer nuevoNumero = ultimoNumero + 1;
            
            log.debug("Número de versión calculado: {} (anterior: {})", nuevoNumero, ultimoNumero);
            
            // 7. Guardar archivo en almacenamiento
            String storagePath = storageService.upload(
                organizacionId,
                documento.getCarpetaId(),
                documento.getId(),
                nuevoNumero,
                request.getFile().getInputStream(),
                fileSize
            );
            
            log.debug("Archivo almacenado en: {}", storagePath);
            
            // 8. Crear versión
            Version version = new Version();
            version.setDocumentoId(documento.getId());
            version.setNumeroSecuencial(nuevoNumero);
            version.setTamanioBytes(fileSize);
            version.setRutaAlmacenamiento(storagePath);
            version.setHashContenido(hashContenido);
            version.setComentarioCambio(request.getComentarioCambio());
            version.setCreadoPor(usuarioId);
            version.setFechaCreacion(OffsetDateTime.now());
            version.setDescargas(0);
            
            version = versionRepository.save(version);
            log.info("Versión {} creada con ID: {}", nuevoNumero, version.getId());
            
            // 9. Actualizar documento
            documento.setVersionActualId(version.getId());
            documento.setNumeroVersiones(nuevoNumero);
            documento.setFechaActualizacion(OffsetDateTime.now());
            documento.setTamanioBytes(fileSize); // Actualizar tamaño con el de la nueva versión
            documentoRepository.save(documento);
            
            log.info("Documento {} actualizado con nueva versión {} (ID: {})", 
                documento.getId(), nuevoNumero, version.getId());
            
            // 10. Construir respuesta
            VersionResponse response = VersionResponse.builder()
                .id(version.getId())
                .documentoId(version.getDocumentoId())
                .numeroSecuencial(version.getNumeroSecuencial())
                .tamanioBytes(version.getTamanioBytes())
                .hashContenido(version.getHashContenido())
                .comentarioCambio(version.getComentarioCambio())
                .creadoPor(version.getCreadoPor())
                .fechaCreacion(version.getFechaCreacion())
                .esVersionActual(true)
                .build();
            
            return response;
            
        } catch (IOException e) {
            log.error("Error al procesar archivo para nueva versión: documentoId={}", documentId, e);
            throw new StorageException("Error al procesar el archivo", e);
        } catch (StorageException e) {
            // Re-lanzar StorageException tal como es (sin envolver)
            throw e;
        } catch (Exception e) {
            log.error("Error al crear nueva versión: documentoId={}", documentId, e);
            throw new RuntimeException("Error al crear nueva versión: " + e.getMessage(), e);
        }
    }
    
    /**
     * Descarga la versión actual de un documento.
     * 
     * <p>Este método implementa US-DOC-002 y realiza las siguientes validaciones:</p>
     * <ol>
     *   <li>Validar que el documento existe y pertenece a la organización (tenant isolation)</li>
     *   <li>Validar que el usuario tiene permiso de LECTURA sobre el documento</li>
     *   <li>Obtener la versión actual del documento</li>
     *   <li>Descargar el archivo desde el almacenamiento</li>
     *   <li>Resolver el tipo MIME a partir de la extensión</li>
     *   <li>Registrar evento de auditoría DOCUMENTO_DESCARGADO</li>
     * </ol>
     *
     * @param documentId ID del documento a descargar
     * @return DTO con InputStream del archivo y metadatos
     * @throws ResourceNotFoundException si el documento no existe o no pertenece a la organización (404)
     * @throws AccessDeniedException si el usuario no tiene permiso de LECTURA (403)
     * @throws StorageException si el archivo no está disponible en almacenamiento (500)
     */
    @Transactional(readOnly = true)
    public DownloadDocumentDto downloadDocument(Long documentId) {
        // 1. Obtener contexto de seguridad
        Long organizacionId = securityContext.getOrganizacionId();
        Long usuarioId = securityContext.getUsuarioId();
        
        log.info("Iniciando descarga - documentoId={}, usuarioId={}, organizacionId={}", 
            documentId, usuarioId, organizacionId);
        
        // 2. Validar que documento existe y pertenece a la organización
        Documento documento = documentoRepository.findByIdAndOrganizacionId(documentId, organizacionId)
            .orElseThrow(() -> {
                log.warn("Documento no encontrado o no pertenece a la organización: documentoId={}, organizacionId={}", 
                    documentId, organizacionId);
                return new ResourceNotFoundException("Documento", documentId);
            });
        
        // 3. Validar permiso de lectura
        boolean tienePermiso = evaluadorPermisos.tieneAcceso(
            usuarioId,
            documentId,
            TipoRecurso.DOCUMENTO,
            NivelAcceso.LECTURA,
            organizacionId
        );
        
        if (!tienePermiso) {
            log.warn("Acceso denegado: usuarioId={} no tiene permiso de LECTURA sobre documentoId={}", 
                usuarioId, documentId);
            throw new AccessDeniedException("No tiene permisos de lectura sobre este documento");
        }
        
        // 4. Obtener versión actual
        if (documento.getVersionActualId() == null) {
            log.error("Documento sin versión actual: documentoId={}", documentId);
            throw new StorageException("El documento no tiene una versión actual asignada");
        }
        
        Version versionActual = versionRepository.findById(documento.getVersionActualId())
            .orElseThrow(() -> {
                log.error("Versión actual no encontrada: versionId={}, documentoId={}", 
                    documento.getVersionActualId(), documentId);
                return new StorageException("La versión actual del documento no está disponible");
            });
        
        log.debug("Versión actual encontrada: versionId={}, numeroSecuencial={}, rutaAlmacenamiento={}", 
            versionActual.getId(), versionActual.getNumeroSecuencial(), versionActual.getRutaAlmacenamiento());
        
        // 5. Descargar archivo desde almacenamiento
        InputStream stream;
        try {
            stream = storageService.download(versionActual.getRutaAlmacenamiento());
            log.debug("Archivo descargado exitosamente desde almacenamiento: {}", versionActual.getRutaAlmacenamiento());
        } catch (StorageException e) {
            log.error("Error al descargar archivo desde almacenamiento: documentoId={}, versionId={}, ruta={}", 
                documentId, versionActual.getId(), versionActual.getRutaAlmacenamiento(), e);
            throw new StorageException("El archivo del documento no está disponible en el almacenamiento");
        }
        
        // 6. Resolver tipo MIME
        String mimeType  = MimeTypeResolver.getMimeType(documento.getExtension());
        log.debug("Tipo MIME resuelto: extension={}, mimeType={}", documento.getExtension(), mimeType);
        
        // 7. Crear DTO de respuesta
        DownloadDocumentDto downloadDto = new DownloadDocumentDto(
            stream,
            documento.getNombre(),
            documento.getExtension(),
            mimeType,
            documento.getTamanioBytes()
        );
        
        // 8. Emitir evento de auditoría (asíncrono, no bloquea la descarga)
        try {
            DocumentDownloadedEvent event = new DocumentDownloadedEvent(
                this,
                documentId,
                versionActual.getId(),
                usuarioId,
                organizacionId,
                documento.getTamanioBytes()
            );
            eventPublisher.publishEvent(event);
            log.debug("Evento de auditoría DOCUMENTO_DESCARGADO publicado: documentoId={}, usuarioId={}", 
                documentId, usuarioId);
        } catch (Exception e) {
            // No fallar la descarga si falla el evento de auditoría
            log.error("Error al publicar evento de auditoría para descarga: documentoId={}, usuarioId={}", 
                documentId, usuarioId, e);
        }
        
        log.info("Descarga completada exitosamente: documentoId={}, versionId={}, usuarioId={}, tamanioBytes={}", 
            documentId, versionActual.getId(), usuarioId, documento.getTamanioBytes());
        
        return downloadDto;
    }
    
    /**
     * Calcula el hash SHA256 de un array de bytes.
     */
    private String calculateSHA256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            
            // Convertir a hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 no disponible", e);
        }
    }
}
