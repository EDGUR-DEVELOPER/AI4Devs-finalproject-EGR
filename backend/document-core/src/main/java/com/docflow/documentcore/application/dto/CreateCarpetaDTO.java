package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitudes de creación de carpetas.
 * 
 * <p>Define el contrato de entrada para POST /api/carpetas.</p>
 *
 * @author DocFlow Team
 */
public class CreateCarpetaDTO {
    
    @NotNull(message = "carpeta_padre_id es requerido")
    @JsonProperty("carpeta_padre_id")
    private Long carpetaPadreId;
    
    @NotNull(message = "nombre es requerido")
    @Size(min = 1, max = 255, message = "nombre debe tener entre 1 y 255 caracteres")
    @JsonProperty("nombre")
    private String nombre;
    
    @Size(max = 500, message = "descripcion no puede exceder 500 caracteres")
    @JsonProperty("descripcion")
    private String descripcion;
    
    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================
    
    public CreateCarpetaDTO() {
    }
    
    public CreateCarpetaDTO(Long carpetaPadreId, String nombre, String descripcion) {
        this.carpetaPadreId = carpetaPadreId;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }
    
    // ========================================================================
    // GETTERS Y SETTERS
    // ========================================================================
    
    public Long getCarpetaPadreId() {
        return carpetaPadreId;
    }
    
    public void setCarpetaPadreId(Long carpetaPadreId) {
        this.carpetaPadreId = carpetaPadreId;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
