package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO de respuesta para permiso efectivo de carpeta (directo o heredado).
 */
public class PermisoEfectivoDTO {

    @JsonProperty("nivel_acceso")
    private String nivelAcceso;

    @JsonProperty("es_heredado")
    private boolean esHeredado;

    @JsonProperty("carpeta_origen_id")
    private Long carpetaOrigenId;

    @JsonProperty("carpeta_origen_nombre")
    private String carpetaOrigenNombre;

    @JsonProperty("ruta_herencia")
    private List<String> rutaHerencia;

    public PermisoEfectivoDTO() {
    }

    public String getNivelAcceso() {
        return nivelAcceso;
    }

    public void setNivelAcceso(String nivelAcceso) {
        this.nivelAcceso = nivelAcceso;
    }

    public boolean isEsHeredado() {
        return esHeredado;
    }

    public void setEsHeredado(boolean esHeredado) {
        this.esHeredado = esHeredado;
    }

    public Long getCarpetaOrigenId() {
        return carpetaOrigenId;
    }

    public void setCarpetaOrigenId(Long carpetaOrigenId) {
        this.carpetaOrigenId = carpetaOrigenId;
    }

    public String getCarpetaOrigenNombre() {
        return carpetaOrigenNombre;
    }

    public void setCarpetaOrigenNombre(String carpetaOrigenNombre) {
        this.carpetaOrigenNombre = carpetaOrigenNombre;
    }

    public List<String> getRutaHerencia() {
        return rutaHerencia;
    }

    public void setRutaHerencia(List<String> rutaHerencia) {
        this.rutaHerencia = rutaHerencia;
    }
}
