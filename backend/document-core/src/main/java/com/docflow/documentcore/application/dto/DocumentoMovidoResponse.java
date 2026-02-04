package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para respuesta después de mover un documento.
 * 
 * <p>Define el contrato de salida para la operación de mover documento.</p>
 *
 * @author DocFlow Team
 */
@Schema(description = "Response after moving a document")
public class DocumentoMovidoResponse {
    
    @JsonProperty("documento_id")
    @Schema(description = "ID of the moved document", example = "100")
    private Long documentoId;
    
    @JsonProperty("carpeta_origen_id")
    @Schema(description = "ID of the origin folder", example = "10")
    private Long carpetaOrigenId;
    
    @JsonProperty("carpeta_destino_id")
    @Schema(description = "ID of the destination folder", example = "25")
    private Long carpetaDestinoId;
    
    @JsonProperty("mensaje")
    @Schema(description = "Success message", example = "Documento movido exitosamente")
    private String mensaje;
    
    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================
    
    public DocumentoMovidoResponse() {
    }
    
    public DocumentoMovidoResponse(
        Long documentoId, 
        Long carpetaOrigenId, 
        Long carpetaDestinoId, 
        String mensaje
    ) {
        this.documentoId = documentoId;
        this.carpetaOrigenId = carpetaOrigenId;
        this.carpetaDestinoId = carpetaDestinoId;
        this.mensaje = mensaje;
    }
    
    // ========================================================================
    // GETTERS AND SETTERS
    // ========================================================================
    
    public Long getDocumentoId() {
        return documentoId;
    }
    
    public void setDocumentoId(Long documentoId) {
        this.documentoId = documentoId;
    }
    
    public Long getCarpetaOrigenId() {
        return carpetaOrigenId;
    }
    
    public void setCarpetaOrigenId(Long carpetaOrigenId) {
        this.carpetaOrigenId = carpetaOrigenId;
    }
    
    public Long getCarpetaDestinoId() {
        return carpetaDestinoId;
    }
    
    public void setCarpetaDestinoId(Long carpetaDestinoId) {
        this.carpetaDestinoId = carpetaDestinoId;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
