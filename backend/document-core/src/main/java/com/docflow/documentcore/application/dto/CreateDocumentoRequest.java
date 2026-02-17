package com.docflow.documentcore.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO para solicitud de creación de documento.
 * 
 * US-DOC-001: Contiene el archivo a cargar y metadatos asociados.
 * Soporta multipart/form-data desde el controlador REST.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentoRequest {
    
    /**
     * Archivo a cargar.
     * Requerido y validado por DocumentValidator.
     */
    @NotNull(message = "El archivo es requerido")
    private MultipartFile file;
    
    /**
     * Comentario opcional para la primera versión.
     * Máximo 500 caracteres.
     */
    private String comentarioCambio;
    
    /**
     * Nombre personalizado para el documento (opcional).
     * Si no se proporciona, se usa el nombre del archivo.
     */
    private String nombrePersonalizado;
}
