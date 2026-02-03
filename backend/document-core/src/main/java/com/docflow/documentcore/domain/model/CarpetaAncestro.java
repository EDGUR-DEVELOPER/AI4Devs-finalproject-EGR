package com.docflow.documentcore.domain.model;

/**
 * Modelo de dominio para representar un ancestro en la jerarquía de carpetas.
 */
public final class CarpetaAncestro {

    private final Long id;
    private final String nombre;
    private final int nivel;

    public CarpetaAncestro(Long id, String nombre, int nivel) {
        this.id = id;
        this.nombre = nombre;
        this.nivel = nivel;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getNivel() {
        return nivel;
    }
}
