package com.docflow.documentcore.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import lombok.Builder;

/**
 * Modelo de dominio que representa el permiso efectivo de un usuario sobre un recurso (documento o carpeta).
 * 
 * <p>Este modelo rastrea no solo el nivel de acceso sino también el origen del permiso,
 * implementando la regla de precedencia:
 * <strong>ACL de Documento > ACL Directo de Carpeta > ACL Heredado de Carpeta</strong></p>
 * 
 * <p>Objeto de valor inmutable que proporciona métodos de fábrica para crear permisos
 * desde diferentes fuentes.</p>
 * 
 * @see OrigenPermiso
 * @see TipoRecurso
 * @see NivelAcceso
 */
@Builder
public final class PermisoEfectivo {

    private final NivelAcceso nivelAcceso;
    private final OrigenPermiso origen;
    private final TipoRecurso tipoRecurso;
    private final Long recursoOrigenId;
    private final String recursoOrigenNombre;
    private final List<String> rutaHerencia;
    private final OffsetDateTime evaluadoEn;

    private PermisoEfectivo(
            NivelAcceso nivelAcceso,
            OrigenPermiso origen,
            TipoRecurso tipoRecurso,
            Long recursoOrigenId,
            String recursoOrigenNombre,
            List<String> rutaHerencia,
            OffsetDateTime evaluadoEn
    ) {
        this.nivelAcceso = Objects.requireNonNull(nivelAcceso, "nivelAcceso cannot be null");
        this.origen = Objects.requireNonNull(origen, "origen cannot be null");
        this.tipoRecurso = Objects.requireNonNull(tipoRecurso, "tipoRecurso cannot be null");
        this.recursoOrigenId = Objects.requireNonNull(recursoOrigenId, "recursoOrigenId cannot be null");
        this.recursoOrigenNombre = recursoOrigenNombre; // Puede ser null para documentos
        this.rutaHerencia = rutaHerencia != null ? List.copyOf(rutaHerencia) : null;
        this.evaluadoEn = evaluadoEn != null ? evaluadoEn : OffsetDateTime.now();
    }

    /**
     * Crea un permiso desde un ACL explícito de documento.
     * Este tiene la mayor precedencia en la jerarquía de evaluación de permisos.
     * 
     * @param nivelAcceso el nivel de acceso otorgado
     * @param documentoId el ID del documento con el ACL explícito
     * @return una nueva instancia de PermisoEfectivo
     */
    public static PermisoEfectivo documento(NivelAcceso nivelAcceso, Long documentoId) {
        return new PermisoEfectivo(
            nivelAcceso,
            OrigenPermiso.DOCUMENTO,
            TipoRecurso.DOCUMENTO,
            documentoId,
            null, // Los documentos no tienen nombres en este contexto
            null, // Sin ruta de herencia para documentos
            OffsetDateTime.now()
        );
    }

    /**
     * Crea un permiso desde un ACL directo de carpeta.
     * Se usa cuando no existe un permiso a nivel de documento.
     * 
     * @param nivelAcceso el nivel de acceso otorgado
     * @param carpetaId el ID de la carpeta con el ACL directo
     * @param carpetaNombre el nombre de la carpeta (para propósitos de visualización)
     * @return una nueva instancia de PermisoEfectivo
     */
    public static PermisoEfectivo carpetaDirecto(
            NivelAcceso nivelAcceso,
            Long carpetaId,
            String carpetaNombre
    ) {
        return new PermisoEfectivo(
            nivelAcceso,
            OrigenPermiso.CARPETA_DIRECTO,
            TipoRecurso.CARPETA,
            carpetaId,
            carpetaNombre,
            null, // Sin ruta de herencia para permisos directos
            OffsetDateTime.now()
        );
    }

    /**
     * Crea un permiso heredado desde una carpeta ancestro.
     * Este tiene la menor precedencia en la jerarquía de evaluación de permisos.
     * 
     * @param nivelAcceso el nivel de acceso otorgado
     * @param carpetaOrigenId el ID de la carpeta ancestro que otorgó el permiso
     * @param carpetaOrigenNombre el nombre de la carpeta ancestro
     * @param rutaHerencia la ruta de nombres de carpetas desde el ancestro hasta el destino
     * @return una nueva instancia de PermisoEfectivo
     */
    public static PermisoEfectivo carpetaHeredado(
            NivelAcceso nivelAcceso,
            Long carpetaOrigenId,
            String carpetaOrigenNombre,
            List<String> rutaHerencia
    ) {
        return new PermisoEfectivo(
            nivelAcceso,
            OrigenPermiso.CARPETA_HEREDADO,
            TipoRecurso.CARPETA,
            carpetaOrigenId,
            carpetaOrigenNombre,
            rutaHerencia,
            OffsetDateTime.now()
        );
    }

    // Métodos de conveniencia

    /**
     * Verifica si este permiso se origina desde un ACL de documento.
     * 
     * @return true si el permiso es de un documento, false en caso contrario
     */
    public boolean isDesdeDocumento() {
        return origen == OrigenPermiso.DOCUMENTO;
    }

    /**
     * Verifica si este permiso se origina desde una carpeta (directa o heredada).
     * 
     * @return true si el permiso es de una carpeta, false en caso contrario
     */
    public boolean isDesdeCarpeta() {
        return origen == OrigenPermiso.CARPETA_DIRECTO || origen == OrigenPermiso.CARPETA_HEREDADO;
    }

    /**
     * Verifica si este permiso es heredado desde una carpeta ancestro.
     * 
     * @return true si el permiso es heredado, false en caso contrario
     */
    public boolean isHeredado() {
        return origen == OrigenPermiso.CARPETA_HEREDADO;
    }

    // Getters

    public NivelAcceso getNivelAcceso() {
        return nivelAcceso;
    }

    public OrigenPermiso getOrigen() {
        return origen;
    }

    public TipoRecurso getTipoRecurso() {
        return tipoRecurso;
    }

    public Long getRecursoOrigenId() {
        return recursoOrigenId;
    }

    public String getRecursoOrigenNombre() {
        return recursoOrigenNombre;
    }

    public List<String> getRutaHerencia() {
        return rutaHerencia;
    }

    public OffsetDateTime getEvaluadoEn() {
        return evaluadoEn;
    }

    @Override
    public String toString() {
        return "PermisoEfectivo{" +
                "nivelAcceso=" + nivelAcceso +
                ", origen=" + origen +
                ", tipoRecurso=" + tipoRecurso +
                ", recursoOrigenId=" + recursoOrigenId +
                ", recursoOrigenNombre='" + recursoOrigenNombre + '\'' +
                ", evaluadoEn=" + evaluadoEn +
                '}';
    }
}
