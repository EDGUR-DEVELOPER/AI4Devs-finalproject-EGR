package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * DTO que representa un documento dentro de una respuesta de listado de contenido.
 *
 * @author DocFlow Team
 */
public class DocumentoItemDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("extension")
    private String extension;

    @JsonProperty("tamanio_bytes")
    private long tamanioBytes;

    @JsonProperty("version_actual")
    private int versionActual;

    @JsonProperty("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @JsonProperty("fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @JsonProperty("creado_por")
    private UsuarioResumenDTO creadoPor;

    @JsonProperty("capacidades")
    private CapacidadesDTO capacidades;

    public DocumentoItemDTO() {
    }

    public DocumentoItemDTO(
            Long id,
            String nombre,
            String extension,
            long tamanioBytes,
            int versionActual,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaModificacion,
            UsuarioResumenDTO creadoPor,
            CapacidadesDTO capacidades) {
        this.id = id;
        this.nombre = nombre;
        this.extension = extension;
        this.tamanioBytes = tamanioBytes;
        this.versionActual = versionActual;
        this.fechaCreacion = fechaCreacion;
        this.fechaModificacion = fechaModificacion;
        this.creadoPor = creadoPor;
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

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public long getTamanioBytes() {
        return tamanioBytes;
    }

    public void setTamanioBytes(long tamanioBytes) {
        this.tamanioBytes = tamanioBytes;
    }

    public int getVersionActual() {
        return versionActual;
    }

    public void setVersionActual(int versionActual) {
        this.versionActual = versionActual;
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

    public UsuarioResumenDTO getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(UsuarioResumenDTO creadoPor) {
        this.creadoPor = creadoPor;
    }

    public CapacidadesDTO getCapacidades() {
        return capacidades;
    }

    public void setCapacidades(CapacidadesDTO capacidades) {
        this.capacidades = capacidades;
    }
}
