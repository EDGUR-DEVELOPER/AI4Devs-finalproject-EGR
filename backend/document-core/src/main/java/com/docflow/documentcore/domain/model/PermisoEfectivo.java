package com.docflow.documentcore.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Modelo de dominio que representa el permiso efectivo de un usuario sobre una carpeta.
 */
public final class PermisoEfectivo {

    private final NivelAcceso nivelAcceso;
    private final boolean esHeredado;
    private final Long carpetaOrigenId;
    private final String carpetaOrigenNombre;
    private final List<String> rutaHerencia;

    private PermisoEfectivo(
            NivelAcceso nivelAcceso,
            boolean esHeredado,
            Long carpetaOrigenId,
            String carpetaOrigenNombre,
            List<String> rutaHerencia
    ) {
        this.nivelAcceso = Objects.requireNonNull(nivelAcceso, "nivelAcceso no puede ser nulo");
        this.carpetaOrigenId = Objects.requireNonNull(carpetaOrigenId, "carpetaOrigenId no puede ser nulo");
        this.carpetaOrigenNombre = Objects.requireNonNull(carpetaOrigenNombre, "carpetaOrigenNombre no puede ser nulo");
        this.esHeredado = esHeredado;
        this.rutaHerencia = rutaHerencia != null ? List.copyOf(rutaHerencia) : null;
    }

    public static PermisoEfectivo directo(NivelAcceso nivelAcceso, Long carpetaId, String carpetaNombre) {
        return new PermisoEfectivo(nivelAcceso, false, carpetaId, carpetaNombre, null);
    }

    public static PermisoEfectivo heredado(
            NivelAcceso nivelAcceso,
            Long carpetaOrigenId,
            String carpetaOrigenNombre,
            List<String> rutaHerencia
    ) {
        return new PermisoEfectivo(nivelAcceso, true, carpetaOrigenId, carpetaOrigenNombre, rutaHerencia);
    }

    public NivelAcceso getNivelAcceso() {
        return nivelAcceso;
    }

    public boolean isEsHeredado() {
        return esHeredado;
    }

    public Long getCarpetaOrigenId() {
        return carpetaOrigenId;
    }

    public String getCarpetaOrigenNombre() {
        return carpetaOrigenNombre;
    }

    public List<String> getRutaHerencia() {
        return rutaHerencia;
    }
}
