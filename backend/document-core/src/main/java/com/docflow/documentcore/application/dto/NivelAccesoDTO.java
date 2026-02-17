package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
/**
 * Data Transfer Object for NivelAcceso
 * Used for API request/response serialization
 */
public class NivelAccesoDTO {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("codigo")
    private String codigo;
    
    @JsonProperty("nombre")
    private String nombre;
    
    @JsonProperty("descripcion")
    private String descripcion;
    
    @JsonProperty("acciones_permitidas")
    private List<String> accionesPermitidas;
    
    @JsonProperty("orden")
    private Integer orden;
    
    @JsonProperty("activo")
    private Boolean activo;
    
    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;
    
    @JsonProperty("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // Constructors
    public NivelAccesoDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
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

    public List<String> getAccionesPermitidas() {
        return accionesPermitidas;
    }

    public void setAccionesPermitidas(List<String> accionesPermitidas) {
        this.accionesPermitidas = accionesPermitidas;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
