package com.docflow.documentcore.domain.model;

import org.springframework.data.domain.Sort;

/**
 * Value Object que representa las opciones de listado de contenido.
 * Incluye validaciones para garantizar valores seguros.
 */
public record OpcionesListado(
    int pagina,
    int tamanio,
    String campoOrden,
    Sort.Direction direccion
) {

    public OpcionesListado {
        // Validar página
        if (pagina < 1) {
            throw new IllegalArgumentException("Página debe ser >= 1");
        }

        // Validar tamaño
        if (tamanio < 1 || tamanio > 100) {
            throw new IllegalArgumentException("Tamaño debe estar entre 1 y 100");
        }

        // Validar campo de orden
        if (campoOrden == null || campoOrden.isBlank()) {
            campoOrden = "nombre";
        } else {
            String campo = campoOrden.toLowerCase();
            if (!campo.equals("nombre") && !campo.equals("fecha_creacion") && !campo.equals("fecha_modificacion")) {
                campoOrden = "nombre";
            }
        }

        // Validar dirección
        if (direccion == null) {
            direccion = Sort.Direction.ASC;
        }
    }
}
