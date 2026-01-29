package com.docflow.documentcore.domain.model.acl;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain Entity: Access Level (Nivel de Acceso)
 * Represents an access permission level in the RBAC system.
 * Immutable by design following DDD principles.
 */
public class NivelAcceso {
    
    private final Long id;
    private final CodigoNivelAcceso codigo;
    private final String nombre;
    private final String descripcion;
    private final List<String> accionesPermitidas;
    private final Integer orden;
    private final boolean activo;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaActualizacion;

    // Private constructor to enforce builder pattern
    private NivelAcceso(Builder builder) {
        this.id = builder.id;
        this.codigo = builder.codigo;
        this.nombre = builder.nombre;
        this.descripcion = builder.descripcion;
        this.accionesPermitidas = builder.accionesPermitidas != null 
            ? List.copyOf(builder.accionesPermitidas) : List.of();
        this.orden = builder.orden;
        this.activo = builder.activo;
        this.fechaCreacion = builder.fechaCreacion;
        this.fechaActualizacion = builder.fechaActualizacion;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public CodigoNivelAcceso getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public List<String> getAccionesPermitidas() {
        return accionesPermitidas;
    }

    public Integer getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    // Domain method: check if a specific action is allowed
    public boolean puedeRealizarAccion(String accion) {
        return activo && accionesPermitidas.contains(accion);
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private CodigoNivelAcceso codigo;
        private String nombre;
        private String descripcion;
        private List<String> accionesPermitidas;
        private Integer orden;
        private boolean activo = true;
        private LocalDateTime fechaCreacion;
        private LocalDateTime fechaActualizacion;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder codigo(CodigoNivelAcceso codigo) {
            this.codigo = codigo;
            return this;
        }

        public Builder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public Builder descripcion(String descripcion) {
            this.descripcion = descripcion;
            return this;
        }

        public Builder accionesPermitidas(List<String> accionesPermitidas) {
            this.accionesPermitidas = accionesPermitidas;
            return this;
        }

        public Builder orden(Integer orden) {
            this.orden = orden;
            return this;
        }

        public Builder activo(boolean activo) {
            this.activo = activo;
            return this;
        }

        public Builder fechaCreacion(LocalDateTime fechaCreacion) {
            this.fechaCreacion = fechaCreacion;
            return this;
        }

        public Builder fechaActualizacion(LocalDateTime fechaActualizacion) {
            this.fechaActualizacion = fechaActualizacion;
            return this;
        }

        public NivelAcceso build() {
            return new NivelAcceso(this);
        }
    }
}
