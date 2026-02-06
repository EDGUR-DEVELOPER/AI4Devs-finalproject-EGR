package com.docflow.documentcore.domain.model;

import com.docflow.documentcore.infrastructure.multitenancy.TenantEntityListener;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entidad lógica del documento (el "sobre" que contiene la historia del archivo).
 * 
 * Representa un documento en el sistema con su historial de versiones.
 * Los archivos físicos están en la entidad Version.
 * 
 * US-DOC-001: Actualizada para incluir información de archivo (extensión, tipo, tamaño)
 * y control de versiones mejorado.
 * 
 * AISLAMIENTO MULTI-TENANT (US-AUTH-004):
 * - Hibernate Filter 'tenantFilter' aplica automáticamente WHERE organizacion_id = :tenantId
 * - TenantEntityListener inyecta organizacionId en @PrePersist/@PreUpdate
 * - Soft delete mediante fecha_eliminacion
 */
@Entity
@Table(name = "documento", indexes = {
    @Index(name = "idx_documento_organizacion_id", columnList = "organizacion_id"),
    @Index(name = "idx_documento_carpeta_id", columnList = "carpeta_id"),
    @Index(name = "idx_documento_creado_por", columnList = "creado_por"),
    @Index(name = "idx_documento_version_actual_id", columnList = "version_actual_id"),
    @Index(name = "idx_documento_fecha_eliminacion", columnList = "fecha_eliminacion"),
    @Index(name = "idx_documento_nombre", columnList = "nombre"),
    @Index(name = "idx_documento_tipo_contenido", columnList = "tipo_contenido")
})
@EntityListeners(TenantEntityListener.class)
@Filter(name = "tenantFilter", condition = "organizacion_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Documento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;
    
    @Column(name = "carpeta_id")
    private Long carpetaId;
    
    @Column(nullable = false, length = 255)
    private String nombre;
    
    @Column(length = 50)
    private String extension;
    
    @Column(name = "tipo_contenido", nullable = false, length = 100)
    private String tipoContenido;
    
    @Column(name = "tamanio_bytes", nullable = false)
    private Long tamanioBytes;
    
    @Column(name = "version_actual_id")
    private Long versionActualId;
    
    @Column(name = "numero_versiones", nullable = false)
    private Integer numeroVersiones = 1;
    
    @Column(nullable = false)
    private Boolean bloqueado = false;
    
    @Column(name = "bloqueado_por")
    private Long bloqueadoPor;
    
    @Column(name = "bloqueado_en")
    private OffsetDateTime bloqueadoEn;
    
    @ElementCollection
    @CollectionTable(name = "documento_etiqueta", 
                     joinColumns = @JoinColumn(name = "documento_id"),
                     indexes = @Index(name = "idx_documento_etiqueta_documento_id", columnList = "documento_id"))
    @Column(name = "etiqueta", length = 100)
    private List<String> etiquetas = new ArrayList<>();
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadatos = new HashMap<>();
    
    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;
    
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();
    
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion = OffsetDateTime.now();
    
    @Column(name = "fecha_eliminacion")
    private OffsetDateTime fechaEliminacion;
    
    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = OffsetDateTime.now();
    }
}
