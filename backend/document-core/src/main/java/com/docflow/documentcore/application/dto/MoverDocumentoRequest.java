package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

/**
 * DTO para solicitud de mover documento entre carpetas.
 * 
 * <p>Define el contrato de entrada para la operación de mover documento.</p>
 *
 * @author DocFlow Team
 */
@Schema(description = "Request to move a document to another folder")
public class MoverDocumentoRequest {
    
    @JsonProperty("carpeta_destino_id")
    @NotNull(message = "carpeta_destino_id es requerido")
    @Positive(message = "carpeta_destino_id debe ser positivo")
    @Schema(description = "ID of the destination folder", example = "25", requiredMode = RequiredMode.REQUIRED )
    private Long carpetaDestinoId;
    
    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================
    
    public MoverDocumentoRequest() {
    }
    
    public MoverDocumentoRequest(Long carpetaDestinoId) {
        this.carpetaDestinoId = carpetaDestinoId;
    }
    
    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================
    
    public Long getCarpetaDestinoId() {
        return carpetaDestinoId;
    }
    
    public void setCarpetaDestinoId(Long carpetaDestinoId) {
        this.carpetaDestinoId = carpetaDestinoId;
    }
}
