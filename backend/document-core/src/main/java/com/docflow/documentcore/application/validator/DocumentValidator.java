package com.docflow.documentcore.application.validator;

import com.docflow.documentcore.domain.exception.DocumentValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validador de documentos y archivos.
 * 
 * US-DOC-001: Valida archivos, nombres y reglas de negocio antes de crear documentos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentValidator {
    
    // Configuración por defecto (debería venir de DocumentValidationConfig)
    private static final long MAX_FILE_SIZE = 524288000L; // 500MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", 
        "txt", "csv", "png", "jpg", "jpeg", "gif", "zip", "rar"
    );
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "application/pdf", "application/msword", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain", "text/csv",
        "image/png", "image/jpeg", "image/gif",
        "application/zip", "application/x-rar-compressed"
    );
    
    // Patrón para nombres de archivo válidos (sin caracteres especiales peligrosos)
    private static final Pattern VALID_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._\\-\\s()]+$");
    
    /**
     * Valida un archivo cargado.
     *
     * @param file Archivo a validar
     * @throws DocumentValidationException Si el archivo no es válido
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentValidationException("El archivo no puede estar vacío");
        }
        
        // Validar tamaño
        if (file.getSize() == 0) {
            throw new DocumentValidationException("El archivo está vacío (0 bytes)");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new DocumentValidationException(
                String.format("El archivo excede el tamaño máximo permitido de %d MB", 
                    MAX_FILE_SIZE / 1024 / 1024)
            );
        }
        
        // Validar extensión
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new DocumentValidationException("El nombre del archivo no es válido");
        }
        
        String extension = getExtension(filename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new DocumentValidationException(
                String.format("Extensión de archivo no permitida: .%s. Extensiones permitidas: %s", 
                    extension, String.join(", ", ALLOWED_EXTENSIONS))
            );
        }
        
        // Validar tipo de contenido
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            log.warn("Tipo de contenido no reconocido o no permitido: {}", contentType);
            // No lanzar excepción para permitir tipos MIME flexibles
        }
        
        log.debug("Archivo validado exitosamente: {} ({} bytes, tipo: {})", 
            filename, file.getSize(), contentType);
    }
    
    /**
     * Valida el nombre de un documento.
     *
     * @param nombre Nombre a validar
     * @throws DocumentValidationException Si el nombre no es válido
     */
    public void validateDocumentName(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DocumentValidationException("El nombre del documento no puede estar vacío");
        }
        
        if (nombre.length() > 255) {
            throw new DocumentValidationException("El nombre del documento no puede exceder 255 caracteres");
        }
        
        // Validar caracteres peligrosos (path traversal)
        if (nombre.contains("..") || nombre.contains("/") || nombre.contains("\\")) {
            throw new DocumentValidationException(
                "El nombre del documento contiene caracteres no permitidos (.., /, \\)"
            );
        }
        
        // Validar patrón de nombre seguro
        if (!VALID_FILENAME_PATTERN.matcher(nombre).matches()) {
            throw new DocumentValidationException(
                "El nombre del documento contiene caracteres especiales no permitidos"
            );
        }
    }
    
    /**
     * Extrae la extensión de un nombre de archivo.
     *
     * @param filename Nombre del archivo
     * @return Extensión sin el punto, o null si no tiene extensión
     */
    public String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == filename.length() - 1) {
            return null; // Termina en punto
        }
        return filename.substring(lastDot + 1);
    }
    
    /**
     * Obtiene el nombre base sin extensión.
     *
     * @param filename Nombre del archivo
     * @return Nombre sin extensión
     */
    public String getBaseName(String filename) {
        if (filename == null || !filename.contains(".")) {
            return filename;
        }
        int lastDot = filename.lastIndexOf('.');
        return filename.substring(0, lastDot);
    }
}
