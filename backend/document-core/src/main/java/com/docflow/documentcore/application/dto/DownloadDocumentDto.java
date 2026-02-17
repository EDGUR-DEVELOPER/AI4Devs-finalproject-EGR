package com.docflow.documentcore.application.dto;

import java.io.InputStream;

/**
 * DTO para encapsular datos de respuesta de descarga de documento.
 * 
 * <p>Separa los datos de streaming de la lógica de entidad,
 * permitiendo un manejo eficiente de recursos y respuestas HTTP.</p>
 * 
 * <p><strong>Responsabilidad del Caller:</strong> El stream debe ser cerrado
 * apropiadamente después de la transmisión. El DTO solo transporta el stream,
 * no lo gestiona.</p>
 * 
 * <p><strong>US-DOC-002:</strong> Descarga de versión actual de documento.</p>
 * 
 * @param stream InputStream para leer datos del archivo (debe cerrarse después de uso)
 * @param filename Nombre de archivo original para header Content-Disposition
 * @param extension Extensión del archivo (ej. "pdf", "docx")
 * @param mimeType Tipo MIME para header Content-Type (ej. "application/pdf")
 * @param sizeBytes Tamaño del archivo en bytes para header Content-Length
 * 
 * @author DocFlow Team
 */
public record DownloadDocumentDto(
    InputStream stream,
    String filename,
    String extension,
    String mimeType,
    Long sizeBytes
) {
    /**
     * Validación de parámetros en tiempo de construcción.
     * 
     * @throws IllegalArgumentException si algún parámetro requerido es null
     */
    public DownloadDocumentDto {
        if (stream == null) {
            throw new IllegalArgumentException("stream cannot be null");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename cannot be null or blank");
        }
        if (sizeBytes == null || sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be non-null and non-negative");
        }
        // mimeType y extension pueden ser null (valores por defecto se aplican en el resolver)
    }
    
    /**
     * Retorna el nombre completo del archivo con extensión.
     * 
     * @return nombre de archivo con extensión si existe, o solo nombre si no existe extensión
     */
    public String getFullFilename() {
        if (extension != null && !extension.isBlank()) {
            return filename + "." + extension;
        }
        return filename;
    }
}
