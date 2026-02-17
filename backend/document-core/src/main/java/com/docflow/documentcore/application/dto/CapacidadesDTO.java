package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO que representa las capacidades (permisos) de un usuario sobre un recurso.
 *
 * @author DocFlow Team
 */
public class CapacidadesDTO {

    @JsonProperty("puede_leer")
    private boolean puedeLeer;

    @JsonProperty("puede_escribir")
    private boolean puedeEscribir;

    @JsonProperty("puede_administrar")
    private boolean puedeAdministrar;

    @JsonProperty("puede_descargar")
    private boolean puedeDescargar;

    public CapacidadesDTO() {
    }

    public CapacidadesDTO(
            boolean puedeLeer,
            boolean puedeEscribir,
            boolean puedeAdministrar,
            boolean puedeDescargar) {
        this.puedeLeer = puedeLeer;
        this.puedeEscribir = puedeEscribir;
        this.puedeAdministrar = puedeAdministrar;
        this.puedeDescargar = puedeDescargar;
    }

    // ========================================================================
    // GETTERS & SETTERS
    // ========================================================================

    public boolean isPuedeLeer() {
        return puedeLeer;
    }

    public void setPuedeLeer(boolean puedeLeer) {
        this.puedeLeer = puedeLeer;
    }

    public boolean isPuedeEscribir() {
        return puedeEscribir;
    }

    public void setPuedeEscribir(boolean puedeEscribir) {
        this.puedeEscribir = puedeEscribir;
    }

    public boolean isPuedeAdministrar() {
        return puedeAdministrar;
    }

    public void setPuedeAdministrar(boolean puedeAdministrar) {
        this.puedeAdministrar = puedeAdministrar;
    }

    public boolean isPuedeDescargar() {
        return puedeDescargar;
    }

    public void setPuedeDescargar(boolean puedeDescargar) {
        this.puedeDescargar = puedeDescargar;
    }
}
