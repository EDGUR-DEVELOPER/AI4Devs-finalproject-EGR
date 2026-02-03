package com.docflow.documentcore.domain.repository;

/**
 * Proyección para rutas de ancestros de carpetas.
 */
public interface CarpetaAncestroProjection {

    Long getId();

    String getNombre();

    Integer getNivel();
}
