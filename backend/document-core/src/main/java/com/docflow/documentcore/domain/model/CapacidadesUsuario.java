package com.docflow.documentcore.domain.model;

import java.io.Serializable;

/**
 * Value Object que representa las capacidades de un usuario sobre un recurso.
 * Invariante: puedeLeer siempre es true para items en listado (filtrado previo en BD).
 *
 * @param puedeLeer si el usuario puede leer (siempre true)
 * @param puedeEscribir si el usuario puede modificar contenido
 * @param puedeAdministrar si el usuario puede gestionar permisos
 * @param puedeDescargar si el usuario puede descargar (solo para documentos)
 */
public record CapacidadesUsuario(
    boolean puedeLeer,
    boolean puedeEscribir,
    boolean puedeAdministrar,
    boolean puedeDescargar
) implements Serializable {

    public CapacidadesUsuario {
        if (!puedeLeer) {
            throw new IllegalArgumentException("puedeLeer debe ser true en items listados");
        }
    }
}
