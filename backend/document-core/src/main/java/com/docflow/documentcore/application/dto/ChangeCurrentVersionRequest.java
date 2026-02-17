package com.docflow.documentcore.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitud de cambio de versión actual de un documento (rollback).
 * 
 * <p>Este DTO permite especificar qué versión debe ser marcada como la versión
 * actual de un documento, permitiendo efectivamente realizar rollback a versiones
 * anteriores sin perder el historial completo.</p>
 * 
 * <p><strong>Validaciones:</strong></p>
 * <ul>
 *   <li>El ID de versión es obligatorio (no puede ser null)</li>
 *   <li>El ID de versión debe ser un número positivo</li>
 * </ul>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>
 * {
 *   "versionId": 201
 * }
 * </pre>
 * 
 * @see com.docflow.documentcore.domain.model.Version
 * @see com.docflow.documentcore.application.dto.DocumentoResponse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeCurrentVersionRequest {
    
    /**
     * ID de la versión que debe ser marcada como versión actual del documento.
     * 
     * <p>Este ID debe corresponder a una versión existente que pertenezca al
     * documento sobre el cual se está realizando la operación. Si la versión
     * no existe o pertenece a otro documento, la operación fallará con un error 400.</p>
     */
    @NotNull(message = "El ID de versión es obligatorio")
    @Positive(message = "El ID de versión debe ser positivo")
    private Long versionId;
}
