package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * DTO para respuestas con información de carpetas.
 * 
 * <p>Define el contrato de salida para operaciones con carpetas.</p>
 *
 * @author DocFlow Team
 */
public class CarpetaDTO {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("organizacion_id")
    private Long organizacionId;
    
    @JsonProperty("carpeta_padre_id")
    private Long carpetaPadreId;
    
    @JsonProperty("nombre")
    private String nombre;
    
    @JsonProperty("descripcion")
    private String descripcion;
    
    @JsonProperty("creado_por")
    private Long creadoPor;
    
    @JsonProperty("fecha_creacion")
    private Instant fechaCreacion;
    
    @JsonProperty("fecha_actualizacion")
    private Instant fechaActualizacion;
    
    @JsonProperty("es_raiz")
    private boolean esRaiz;
    
    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================
    
    public CarpetaDTO() {
    }
    
    // ========================================================================
    // GETTERS Y SETTERS
    // ========================================================================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrganizacionId() {
        return organizacionId;
    }
    
    public void setOrganizacionId(Long organizacionId) {
        this.organizacionId = organizacionId;
    }
    
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
    
    public Long getCreadoPor() {
        return creadoPor;
    }
    
    public void setCreadoPor(Long creadoPor) {
        this.creadoPor = creadoPor;
    }
    
    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }
    
    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
    
    public boolean isEsRaiz() {
        return esRaiz;
    }
    
    public void setEsRaiz(boolean esRaiz) {
        this.esRaiz = esRaiz;
    }
}
