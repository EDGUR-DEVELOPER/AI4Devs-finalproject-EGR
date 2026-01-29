package com.docflow.documentcore.infrastructure.adapter.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.Instant;
/**
 * Entidad JPA que representa una Carpeta en la base de datos.
 * 
 * <p>Esta clase pertenece a la capa de infraestructura y está acoplada
 * a JPA. Se mapea a la tabla 'carpetas' en PostgreSQL.</p>
 *
 * <p><strong>Características:</strong>
 * <ul>
 *   <li>Soft Delete: @SQLDelete y @Where automatizan la eliminación lógica</li>
 *   <li>Auto-timestamping: @PrePersist y @PreUpdate</li>
 *   <li>Long como primary key</li>
 *   <li>Relación auto-referencial (carpeta padre)</li>
 * </ul>
 * </p>
 *
 * @author DocFlow Team
 */
@Entity
@Table(
    name = "carpetas",
    indexes = {
        @Index(name = "idx_carpetas_org_padre", columnList = "organizacion_id, carpeta_padre_id"),
        @Index(name = "idx_carpetas_org_nombre", columnList = "organizacion_id, nombre"),
        @Index(name = "idx_carpetas_creado_por", columnList = "creado_por")
    }
)
@SQLDelete(sql = "UPDATE carpetas SET fecha_eliminacion = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "fecha_eliminacion IS NULL")
public class CarpetaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;
    
    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;
    
    @Column(name = "carpeta_padre_id")
    private Long carpetaPadreId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carpeta_padre_id", insertable = false, updatable = false)
    private CarpetaEntity carpetaPadre;
    
    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;
    
    @Column(name = "descripcion", length = 500)
    private String descripcion;
    
    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;
    
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;
    
    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;
    
    @Column(name = "fecha_eliminacion")
    private Instant fechaEliminacion;
    
    // ========================================================================
    // LIFECYCLE CALLBACKS
    // ========================================================================
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (fechaCreacion == null) {
            fechaCreacion = now;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = now;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = Instant.now();
    }
    
    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================
    
    public CarpetaEntity() {
    }
    
    // ========================================================================
    // GETTERS Y SETTERS
    // ========================================================================
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrganizacionId() {
        return organizacionId;
    }
    
    public void setOrganizacionId(Long organizacionId) {
        this.organizacionId = organizacionId;
    }
    
    public Long getCarpetaPadreId() {
        return carpetaPadreId;
    }
    
    public void setCarpetaPadreId(Long carpetaPadreId) {
        this.carpetaPadreId = carpetaPadreId;
    }
    
    public CarpetaEntity getCarpetaPadre() {
        return carpetaPadre;
    }
    
    public void setCarpetaPadre(CarpetaEntity carpetaPadre) {
        this.carpetaPadre = carpetaPadre;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public Long getCreadoPor() {
        return creadoPor;
    }
    
    public void setCreadoPor(Long creadoPor) {
        this.creadoPor = creadoPor;
    }
    
    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
    
    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    
    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }
    
    public void setFechaActualizacion(Instant fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
    
    public Instant getFechaEliminacion() {
        return fechaEliminacion;
    }
    
    public void setFechaEliminacion(Instant fechaEliminacion) {
        this.fechaEliminacion = fechaEliminacion;
    }
}
