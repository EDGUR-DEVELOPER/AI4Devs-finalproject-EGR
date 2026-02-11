package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para representar un elemento en la ruta de carpetas (breadcrumb).
 * Usado para mostrar la navegación jerárquica de una carpeta.
 */
@Schema(description = "Elemento de la ruta de navegación de carpetas")
public class CarpetaRutaDTO {
    
    @JsonProperty("id")
    @Schema(description = "ID de la carpeta", example = "1")
    private Long id;
    
    @JsonProperty("nombre")
    @Schema(description = "Nombre de la carpeta", example = "Documentos")
    private String nombre;
    
    @JsonProperty("nivel")
    @Schema(description = "Nivel de profundidad (0=raíz, 1=primer nivel, etc.)", example = "0")
    private int nivel;
    
    // Constructors
    public CarpetaRutaDTO() {}
    
    public CarpetaRutaDTO(Long id, String nombre, int nivel) {
        this.id = id;
        this.nombre = nombre;
        this.nivel = nivel;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public int getNivel() {
        return nivel;
    }
    
    public void setNivel(int nivel) {
        this.nivel = nivel;
    }
}
