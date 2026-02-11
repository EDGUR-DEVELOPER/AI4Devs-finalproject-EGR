package com.docflow.documentcore.domain.model;

import org.springframework.data.domain.Sort;

/**
 * Value Object que representa las opciones de listado de contenido.
 * Incluye validaciones para garantizar valores seguros.
 */
public class OpcionesListado {
    private final int pagina;
    private final int tamanio;
    private final String campoOrden;
    private final Sort.Direction direccion;

    public OpcionesListado(int pagina, int tamanio, String campoOrden, Sort.Direction direccion) {
        // Validar página
        if (pagina < 1) {
            throw new IllegalArgumentException("Página debe ser >= 1");
        }

        // Validar tamaño
        if (tamanio < 1 || tamanio > 100) {
            throw new IllegalArgumentException("Tamaño debe estar entre 1 y 100");
        }

        // Normalizar y validar campo de orden
        String campoValidado = campoOrden;
        if (campoValidado == null || campoValidado.isBlank()) {
            campoValidado = "nombre";
        } else {
            String campo = campoValidado.toLowerCase();
            if (!campo.equals("nombre") && !campo.equals("fecha_creacion") && !campo.equals("fecha_modificacion")) {
                campoValidado = "nombre";
            }
        }

        // Normalizar dirección
        Sort.Direction direccionValidada = direccion != null ? direccion : Sort.Direction.ASC;

        this.pagina = pagina;
        this.tamanio = tamanio;
        this.campoOrden = campoValidado;
        this.direccion = direccionValidada;
    }

    public int getPagina() {
        return pagina;
    }

    public int getTamanio() {
        return tamanio;
    }

    public String getCampoOrden() {
        return campoOrden;
    }

    public Sort.Direction getDireccion() {
        return direccion;
    }

    @Override
    public String toString() {
        return "OpcionesListado{" +
                "pagina=" + pagina +
                ", tamanio=" + tamanio +
                ", campoOrden='" + campoOrden + '\'' +
                ", direccion=" + direccion +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OpcionesListado that = (OpcionesListado) o;

        if (pagina != that.pagina) return false;
        if (tamanio != that.tamanio) return false;
        if (!campoOrden.equals(that.campoOrden)) return false;
        return direccion == that.direccion;
    }

    @Override
    public int hashCode() {
        int result = pagina;
        result = 31 * result + tamanio;
        result = 31 * result + campoOrden.hashCode();
        result = 31 * result + direccion.hashCode();
        return result;
    }
}
