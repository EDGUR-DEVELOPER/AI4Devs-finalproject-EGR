package com.docflow.documentcore.domain.model;

import com.docflow.documentcore.infrastructure.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.OffsetDateTime;

/**
 * Entidad que representa una carpeta en el sistema de gestión documental.
 * 
 * Estructura jerárquica para organizar documentos.
 * Soporta carpetas anidadas mediante carpeta_padre_id (self-referencing).
 * 
 * AISLAMIENTO MULTI-TENANT (US-AUTH-004):
 * - Hibernate Filter 'tenantFilter' aplica automáticamente WHERE organizacion_id = :tenantId
 * - TenantEntityListener inyecta organizacionId en @PrePersist/@PreUpdate
 * - Soft delete mediante fecha_eliminacion
 */
@Entity
@Table(name = "carpetas")
@EntityListeners(TenantEntityListener.class)
@Filter(name = "tenantFilter", condition = "organizacion_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Carpeta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String nombre;
    
    @Column(name = "carpeta_padre_id")
    private Long carpetaPadreId;
    
    @Column(name = "organizacion_id", nullable = false)
    private Integer organizacionId;
    
    @Column(name = "propietario_id", nullable = false)
    private Long propietarioId;
    
    @Column(name = "ruta_jerarquia", length = 500)
    private String rutaJerarquia;
    
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();
    
    @Column(name = "fecha_eliminacion")
    private OffsetDateTime fechaEliminacion;
    
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion = OffsetDateTime.now();
}
