package com.docflow.documentcore.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Modelo de dominio inmutable que representa una Carpeta en DocFlow.
 * 
 * <p>Esta clase sigue los principios de Domain-Driven Design (DDD):
 * - Inmutable para garantizar consistencia y thread-safety
 * - Contiene lógica de negocio y reglas de validación
 * - Independiente de la capa de infraestructura (sin anotaciones JPA)
 * - Utiliza patrón Builder para construcción flexible
 * </p>
 *
 * <p><strong>Reglas de Negocio:</strong>
 * <ul>
 *   <li>El nombre es obligatorio y no puede exceder 255 caracteres</li>
 *   <li>La descripción es opcional pero limitada a 500 caracteres</li>
 *   <li>Las carpetas raíz tienen carpetaPadreId = null</li>
 *   <li>Todas las carpetas deben pertenecer a una organización</li>
 *   <li>Soft delete mediante fechaEliminacion</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 * @see Builder
 */
public final class Carpeta {
    
    private final Long id;
    private final Long organizacionId;
    private final Long carpetaPadreId;  // NULL para carpeta raíz
    private final String nombre;
    private final String descripcion;
    private final Long creadoPor;
    private final Instant fechaCreacion;
    private final Instant fechaActualizacion;
    private final Instant fechaEliminacion;  // NULL = activa, NOT NULL = eliminada lógicamente
    
    // ========================================================================
    // CONSTRUCTOR PRIVADO
    // ========================================================================
    
    private Carpeta(Builder builder) {
        this.id = builder.id;
        this.organizacionId = Objects.requireNonNull(builder.organizacionId, "organizacionId no puede ser nulo");
        this.carpetaPadreId = builder.carpetaPadreId;  // Puede ser null (carpeta raíz)
        this.nombre = Objects.requireNonNull(builder.nombre, "nombre no puede ser nulo");
        this.descripcion = builder.descripcion;
        this.creadoPor = Objects.requireNonNull(builder.creadoPor, "creadoPor no puede ser nulo");
        this.fechaCreacion = builder.fechaCreacion != null ? builder.fechaCreacion : Instant.now();
        this.fechaActualizacion = builder.fechaActualizacion != null ? builder.fechaActualizacion : Instant.now();
        this.fechaEliminacion = builder.fechaEliminacion;
    }
    
    // ========================================================================
    // GETTERS
    // ========================================================================
    
    public Long getId() {
        return id;
    }
    
    public Long getOrganizacionId() {
        return organizacionId;
    }
    
    public Long getCarpetaPadreId() {
        return carpetaPadreId;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public Long getCreadoPor() {
        return creadoPor;
    }
    
    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
    
    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }
    
    public Instant getFechaEliminacion() {
        return fechaEliminacion;
    }
    
    // ========================================================================
    // MÉTODOS DE LÓGICA DE NEGOCIO
    // ========================================================================
    
    /**
     * Verifica si la carpeta es una carpeta raíz (sin padre).
     * 
     * @return true si es carpeta raíz, false en caso contrario
     */
    public boolean esRaiz() {
        return carpetaPadreId == null;
    }
    
    /**
     * Verifica si la carpeta está activa (no eliminada lógicamente).
     * 
     * @return true si está activa, false si fue eliminada
     */
    public boolean estaActiva() {
        return fechaEliminacion == null;
    }
    
    /**
     * Verifica si la carpeta fue eliminada lógicamente.
     * 
     * @return true si fue eliminada, false en caso contrario
     */
    public boolean estaEliminada() {
        return fechaEliminacion != null;
    }
    
    /**
     * Valida la integridad del modelo de dominio.
     * 
     * @throws IllegalStateException si el modelo no cumple las reglas de negocio
     */
    public void validarIntegridad() {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalStateException("El nombre de la carpeta no puede estar vacío");
        }
        
        if (nombre.length() > 255) {
            throw new IllegalStateException("El nombre de la carpeta no puede exceder 255 caracteres");
        }
        
        if (descripcion != null && descripcion.length() > 500) {
            throw new IllegalStateException("La descripción de la carpeta no puede exceder 500 caracteres");
        }
        
        if (organizacionId == null) {
            throw new IllegalStateException("La carpeta debe pertenecer a una organización");
        }
        
        if (creadoPor == null) {
            throw new IllegalStateException("La carpeta debe tener un creador");
        }
    }
    
    // ========================================================================
    // EQUALS & HASHCODE (basado en ID únicamente)
    // ========================================================================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Carpeta carpeta = (Carpeta) o;
        return Objects.equals(id, carpeta.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Carpeta{" +
                "id=" + id +
                ", organizacionId=" + organizacionId +
                ", carpetaPadreId=" + carpetaPadreId +
                ", nombre='" + nombre + '\'' +
                ", estaActiva=" + estaActiva() +
                '}';
    }
    
    // ========================================================================
    // BUILDER PATTERN
    // ========================================================================
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private Long id;
        private Long organizacionId;
        private Long carpetaPadreId;
        private String nombre;
        private String descripcion;
        private Long creadoPor;
        private Instant fechaCreacion;
        private Instant fechaActualizacion;
        private Instant fechaEliminacion;
        
        private Builder() {
        }
        
        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        
        public Builder organizacionId(Long organizacionId) {
            this.organizacionId = organizacionId;
            return this;
        }
        
        public Builder carpetaPadreId(Long carpetaPadreId) {
            this.carpetaPadreId = carpetaPadreId;
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
        
        public Builder creadoPor(Long creadoPor) {
            this.creadoPor = creadoPor;
            return this;
        }
        
        public Builder fechaCreacion(Instant fechaCreacion) {
            this.fechaCreacion = fechaCreacion;
            return this;
        }
        
        public Builder fechaActualizacion(Instant fechaActualizacion) {
            this.fechaActualizacion = fechaActualizacion;
            return this;
        }
        
        public Builder fechaEliminacion(Instant fechaEliminacion) {
            this.fechaEliminacion = fechaEliminacion;
            return this;
        }
        
        /**
         * Construye la instancia inmutable de Carpeta.
         * 
         * @return nueva instancia de Carpeta
         * @throws IllegalStateException si el modelo no cumple las reglas de negocio
         */
        public Carpeta build() {
            Carpeta carpeta = new Carpeta(this);
            carpeta.validarIntegridad();
            return carpeta;
        }
    }
}
