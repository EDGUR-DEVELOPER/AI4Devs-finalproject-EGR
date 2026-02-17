package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * DTO que representa una carpeta dentro de una respuesta de listado de contenido.
 *
 * @author DocFlow Team
 */
public class CarpetaItemDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("descripcion")
    private String descripcion;

    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @JsonProperty("num_subcarpetas")
    private int numSubcarpetas;

    @JsonProperty("num_documentos")
    private int numDocumentos;

    @JsonProperty("capacidades")
    private CapacidadesDTO capacidades;

    public CarpetaItemDTO() {
    }

    public CarpetaItemDTO(
            Long id,
            String nombre,
            String descripcion,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaModificacion,
            int numSubcarpetas,
            int numDocumentos,
            CapacidadesDTO capacidades) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
        this.numSubcarpetas = numSubcarpetas;
        this.numDocumentos = numDocumentos;
        this.capacidades = capacidades;
    }

    // ========================================================================
    // GETTERS & SETTERS
    // ========================================================================

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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public int getNumSubcarpetas() {
        return numSubcarpetas;
    }

    public void setNumSubcarpetas(int numSubcarpetas) {
        this.numSubcarpetas = numSubcarpetas;
    }

    public int getNumDocumentos() {
        return numDocumentos;
    }

    public void setNumDocumentos(int numDocumentos) {
        this.numDocumentos = numDocumentos;
    }

    public CapacidadesDTO getCapacidades() {
        return capacidades;
    }

    public void setCapacidades(CapacidadesDTO capacidades) {
        this.capacidades = capacidades;
    }
}
