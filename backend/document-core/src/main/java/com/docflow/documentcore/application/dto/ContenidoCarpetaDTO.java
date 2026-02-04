package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO que representa el contenido completo de una carpeta (respuesta de listado).
 *
 * <p>Estructura de respuesta para GET /api/carpetas/{id}/contenido y GET /api/carpetas/raiz/contenido.</p>
 *
 * @author DocFlow Team
 */
public class ContenidoCarpetaDTO {

    @JsonProperty("subcarpetas")
    private List<CarpetaItemDTO> subcarpetas;

    @JsonProperty("documentos")
    private List<DocumentoItemDTO> documentos;

    @JsonProperty("total_subcarpetas")
    private int totalSubcarpetas;

    @JsonProperty("total_documentos")
    private int totalDocumentos;

    @JsonProperty("pagina_actual")
    private int paginaActual;

    @JsonProperty("total_paginas")
    private int totalPaginas;

    public ContenidoCarpetaDTO() {
    }

    public ContenidoCarpetaDTO(
            List<CarpetaItemDTO> subcarpetas,
            List<DocumentoItemDTO> documentos,
            int totalSubcarpetas,
            int totalDocumentos,
            int paginaActual,
            int totalPaginas) {
        this.subcarpetas = subcarpetas;
        this.documentos = documentos;
        this.totalSubcarpetas = totalSubcarpetas;
        this.totalDocumentos = totalDocumentos;
        this.paginaActual = paginaActual;
        this.totalPaginas = totalPaginas;
    }

    // ========================================================================
    // GETTERS & SETTERS
    // ========================================================================

    public List<CarpetaItemDTO> getSubcarpetas() {
        return subcarpetas;
    }

    public void setSubcarpetas(List<CarpetaItemDTO> subcarpetas) {
        this.subcarpetas = subcarpetas;
    }

    public List<DocumentoItemDTO> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<DocumentoItemDTO> documentos) {
        this.documentos = documentos;
    }

    public int getTotalSubcarpetas() {
        return totalSubcarpetas;
    }

    public void setTotalSubcarpetas(int totalSubcarpetas) {
        this.totalSubcarpetas = totalSubcarpetas;
    }

    public int getTotalDocumentos() {
        return totalDocumentos;
    }

    public void setTotalDocumentos(int totalDocumentos) {
        this.totalDocumentos = totalDocumentos;
    }

    public int getPaginaActual() {
        return paginaActual;
    }

    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public void setTotalPaginas(int totalPaginas) {
        this.totalPaginas = totalPaginas;
    }
}
