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
import java.util.HashMap;
import java.util.Map;

/**
 * Entidad física del documento (el archivo inmutable en un momento del tiempo).
 * 
 * Cada versión es inmutable - representa un snapshot del archivo.
 * El documento padre puede tener múltiples versiones.
 * 
 * AISLAMIENTO MULTI-TENANT (US-AUTH-004):
 * - Hibernate Filter 'tenantFilter' aplica automáticamente WHERE organizacion_id = :tenantId
 * - TenantEntityListener inyecta organizacionId en @PrePersist/@PreUpdate
 * - organizacionId debe coincidir con el del documento padre (constraint en DB)
 */
@Entity
@Table(name = "versiones")
@EntityListeners(TenantEntityListener.class)
@Filter(name = "tenantFilter", condition = "organizacion_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Version {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "documento_id", nullable = false)
    private Long documentoId;
    
    @Column(name = "organizacion_id", nullable = false)
    private Integer organizacionId;
    
    @Column(name = "numero_secuencial", nullable = false)
    private Integer numeroSecuencial;
    
    @Column(name = "etiqueta_version", length = 50)
    private String etiquetaVersion;
    
    @Column(name = "ruta_almacenamiento", nullable = false, length = 500)
    private String rutaAlmacenamiento;
    
    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;
    
    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;
    
    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;
    
    @Column(name = "creador_id", nullable = false)
    private Long creadorId;
    
    @Type(JsonBinaryType.class)
    @Column(name = "metadatos_version", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadatosVersion = new HashMap<>();
    
    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();
}
