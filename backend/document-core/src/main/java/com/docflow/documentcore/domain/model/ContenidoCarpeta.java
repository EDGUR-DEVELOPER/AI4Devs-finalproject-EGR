package com.docflow.documentcore.domain.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Value Object que representa el contenido completo de una carpeta.
 * Contiene subcarpetas, documentos y metadatos de paginación.
 */
public record ContenidoCarpeta(
    List<CarpetaItem> subcarpetas,
    List<DocumentoItem> documentos,
    int totalSubcarpetas,
    int totalDocumentos,
    int paginaActual,
    int totalPaginas
) implements Serializable {

    public ContenidoCarpeta {
        // Validaciones
        if (subcarpetas == null) {
            throw new IllegalArgumentException("subcarpetas no puede ser nulo");
        }
        if (documentos == null) {
            throw new IllegalArgumentException("documentos no puede ser nulo");
        }
        if (paginaActual < 1) {
            throw new IllegalArgumentException("paginaActual debe ser >= 1");
        }
        if (totalSubcarpetas < 0) {
            throw new IllegalArgumentException("totalSubcarpetas no puede ser negativo");
        }
        if (totalDocumentos < 0) {
            throw new IllegalArgumentException("totalDocumentos no puede ser negativo");
        }
        if (totalPaginas < 0) {
            throw new IllegalArgumentException("totalPaginas no puede ser negativo");
        }

        // Hacer inmutables las listas
        subcarpetas = Collections.unmodifiableList(subcarpetas);
        documentos = Collections.unmodifiableList(documentos);
    }
}
