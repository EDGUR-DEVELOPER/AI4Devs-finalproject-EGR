package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.CreateDocumentoRequest;
import com.docflow.documentcore.application.dto.DocumentoResponse;
import com.docflow.documentcore.application.mapper.DocumentoMapper;
import com.docflow.documentcore.application.validator.DocumentValidator;
import com.docflow.documentcore.domain.exception.DocumentValidationException;
import com.docflow.documentcore.domain.exception.StorageException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
import com.docflow.documentcore.infrastructure.security.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
