package com.docflow.documentcore.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO para solicitud de creación de nueva versión de documento.
 * 
 * US-DOC-003: Permite subir una nueva versión de un documento existente
 * con un comentario opcional de cambios.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVersionRequest {
    
    /**
     * Archivo a cargar como nueva versión.
     * Requerido y validado por DocumentValidator.
     */
    @NotNull(message = "El archivo es requerido")
    private MultipartFile file;
    
    /**
     * Comentario opcional describiendo los cambios en esta versión.
     * Máximo 500 caracteres.
     */
    @Size(max = 500, message = "El comentario no puede exceder 500 caracteres")
    private String comentarioCambio;
}
