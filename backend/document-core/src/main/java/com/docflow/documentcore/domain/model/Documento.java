package com.docflow.documentcore.domain.model;

import com.docflow.documentcore.infrastructure.multitenancy.TenantEntityListener;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entidad lógica del documento (el "sobre" que contiene la historia del archivo).
 * 
 * Representa un documento en el sistema con su historial de versiones.
 * Los archivos físicos están en la entidad Version.
 * 
 * AISLAMIENTO MULTI-TENANT (US-AUTH-004):
 * - Hibernate Filter 'tenantFilter' aplica automáticamente WHERE organizacion_id = :tenantId
 * - TenantEntityListener inyecta organizacionId en @PrePersist/@PreUpdate
 * - Soft delete mediante fecha_eliminacion
 */
@Entity
@Table(name = "documentos")
@EntityListeners(TenantEntityListener.class)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Integer.class))
@Filter(name = "tenantFilter", condition = "organizacion_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Documento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String nombre;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
    
    @Column(name = "organizacion_id", nullable = false)
    private Integer organizacionId;
    
    @Column(name = "carpeta_id", nullable = false)
    private Long carpetaId;
    
    @Column(name = "propietario_id", nullable = false)
    private Long propietarioId;
    
    @Column(name = "version_actual_id")
    private Long versionActualId;
    
    @Type(JsonBinaryType.class)
    @Column(name = "metadatos_globales", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadatosGlobales = new HashMap<>();
    
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();
    
    @Column(name = "fecha_eliminacion")
    private OffsetDateTime fechaEliminacion;
    
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion = OffsetDateTime.now();
}
