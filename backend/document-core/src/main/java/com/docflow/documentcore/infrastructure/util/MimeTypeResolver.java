package com.docflow.documentcore.infrastructure.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utilidad para resolver tipos MIME a partir de extensiones de archivo.
 * 
 * <p>Proporciona mapeo estático de extensiones comunes a sus tipos MIME
 * correspondientes según estándares RFC 2046, RFC 6838, y registros IANA.</p>
 * 
 * <p><strong>Características:</strong></p>
 * <ul>
 *   <li>Búsqueda insensible a mayúsculas/minúsculas</li>
 *   <li>Soporta extensiones con o sin punto inicial</li>
 *   <li>Valor por defecto configurable</li>
 *   <li>Cobertura de tipos office, imágenes, PDFs, compresión, y texto</li>
 * </ul>
 * 
 * <p><strong>US-DOC-002:</strong> Resolución de Content-Type para descarga de documentos.</p>
 * 
 * @author DocFlow Team
 */
public class MimeTypeResolver {
    
    /**
     * Tipo MIME por defecto para extensiones desconocidas.
     * RFC 2046: application/octet-stream indica datos binarios sin tipo específico.
     */
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    
    /**
     * Mapa estático de extensiones a tipos MIME.
     * Inicializado en tiempo de carga de clase.
     */
    private static final Map<String, String> MIME_TYPE_MAP;
    
    static {
        MIME_TYPE_MAP = new HashMap<>();
        
        // Documentos PDF
        MIME_TYPE_MAP.put("pdf", "application/pdf");
        
        // Microsoft Office modernos (Office 2007+)
        MIME_TYPE_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPE_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MIME_TYPE_MAP.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        
        // Microsoft Office legacy (Office 97-2003)
        MIME_TYPE_MAP.put("doc", "application/msword");
        MIME_TYPE_MAP.put("xls", "application/vnd.ms-excel");
        MIME_TYPE_MAP.put("ppt", "application/vnd.ms-powerpoint");
        
        // Imágenes
        MIME_TYPE_MAP.put("jpg", "image/jpeg");
        MIME_TYPE_MAP.put("jpeg", "image/jpeg");
        MIME_TYPE_MAP.put("png", "image/png");
        MIME_TYPE_MAP.put("gif", "image/gif");
        MIME_TYPE_MAP.put("bmp", "image/bmp");
        MIME_TYPE_MAP.put("svg", "image/svg+xml");
        MIME_TYPE_MAP.put("webp", "image/webp");
        MIME_TYPE_MAP.put("ico", "image/x-icon");
        
        // Texto plano
        MIME_TYPE_MAP.put("txt", "text/plain");
        MIME_TYPE_MAP.put("csv", "text/csv");
        MIME_TYPE_MAP.put("html", "text/html");
        MIME_TYPE_MAP.put("htm", "text/html");
        MIME_TYPE_MAP.put("xml", "application/xml");
        MIME_TYPE_MAP.put("json", "application/json");
        
        // Compresión
        MIME_TYPE_MAP.put("zip", "application/zip");
        MIME_TYPE_MAP.put("rar", "application/x-rar-compressed");
        MIME_TYPE_MAP.put("7z", "application/x-7z-compressed");
        MIME_TYPE_MAP.put("tar", "application/x-tar");
        MIME_TYPE_MAP.put("gz", "application/gzip");
        
        // Otros formatos comunes
        MIME_TYPE_MAP.put("mp3", "audio/mpeg");
        MIME_TYPE_MAP.put("mp4", "video/mp4");
        MIME_TYPE_MAP.put("avi", "video/x-msvideo");
        MIME_TYPE_MAP.put("mov", "video/quicktime");
    }
    
    /**
     * Constructor privado para prevenir instanciación.
     * Esta clase solo proporciona métodos estáticos.
     */
    private MimeTypeResolver() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Resuelve el tipo MIME para una extensión de archivo.
     * 
     * <p>La búsqueda es insensible a mayúsculas. Si la extensión no se encuentra,
     * retorna {@link #DEFAULT_MIME_TYPE}.</p>
     * 
     * <p><strong>Ejemplos:</strong></p>
     * <pre>
     * getMimeType("pdf")     → "application/pdf"
     * getMimeType(".pdf")    → "application/pdf"
     * getMimeType("PDF")     → "application/pdf"
     * getMimeType("docx")    → "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
     * getMimeType("unknown") → "application/octet-stream"
     * getMimeType(null)      → "application/octet-stream"
     * </pre>
     * 
     * @param extension extensión del archivo (con o sin punto inicial, ej. "pdf" o ".pdf")
     * @return tipo MIME correspondiente, o {@link #DEFAULT_MIME_TYPE} si no se encuentra
     */
    public static String getMimeType(String extension) {
        return getMimeType(extension, DEFAULT_MIME_TYPE);
    }
    
    /**
     * Resuelve el tipo MIME para una extensión de archivo con tipo por defecto personalizado.
     * 
     * <p>Similar a {@link #getMimeType(String)} pero permite especificar
     * un tipo MIME por defecto alternativo.</p>
     * 
     * @param extension extensión del archivo (con o sin punto inicial)
     * @param defaultMimeType tipo MIME a retornar si la extensión no se encuentra
     * @return tipo MIME correspondiente, o defaultMimeType si no se encuentra
     */
    public static String getMimeType(String extension, String defaultMimeType) {
        if (extension == null || extension.isBlank()) {
            return defaultMimeType;
        }
        
        // Normalizar: eliminar punto inicial y convertir a minúsculas
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        
        return MIME_TYPE_MAP.getOrDefault(normalized, defaultMimeType);
    }
    
    /**
     * Verifica si una extensión tiene un tipo MIME conocido registrado.
     * 
     * @param extension extensión a verificar
     * @return true si la extensión está mapeada, false en caso contrario
     */
    public static boolean isKnownExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        
        return MIME_TYPE_MAP.containsKey(normalized);
    }
}
