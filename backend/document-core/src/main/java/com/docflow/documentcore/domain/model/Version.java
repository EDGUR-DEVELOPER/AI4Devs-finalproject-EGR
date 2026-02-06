package com.docflow.documentcore.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Entidad física del documento (el archivo inmutable en un momento del tiempo).
 * 
 * Cada versión es inmutable - representa un snapshot del archivo.
 * El documento padre puede tener múltiples versiones.
 * 
 * US-DOC-001: Actualizada para incluir información de seguimiento de descargas
 * y optimización de consultas.
 * 
 * AISLAMIENTO MULTI-TENANT (US-AUTH-004):
 * - Hibernate Filter 'tenantFilter' aplica automáticamente WHERE organizacion_id = :tenantId (heredado de documento)
 * - Las versiones heredan organizacionId del documento padre
 * -organizacionId debe coincidir con el del documento padre (constraint en DB)
 */
@Entity
@Table(name = "documento_version", indexes = {
    @Index(name = "idx_documento_version_documento_id", columnList = "documento_id"),
    @Index(name = "idx_documento_version_creado_por", columnList = "creado_por"),
    @Index(name = "idx_documento_version_ruta", columnList = "ruta_almacenamiento"),
    @Index(name = "idx_documento_version_hash", columnList = "hash_contenido"),
    @Index(name = "idx_documento_version_fecha_creacion", columnList = "fecha_creacion")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_documento_version_documento_numero", columnNames = {"documento_id", "numero_secuencial"})
})
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
    
    @Column(name = "numero_secuencial", nullable = false)
    private Integer numeroSecuencial;
    
    @Column(name = "tamanio_bytes", nullable = false)
    private Long tamanioBytes;
    
    @Column(name = "ruta_almacenamiento", nullable = false, length = 500)
    private String rutaAlmacenamiento;
    
    @Column(name = "hash_contenido", nullable = false, length = 64)
    private String hashContenido;
    
    @Column(name = "comentario_cambio", length = 500)
    private String comentarioCambio;
    
    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;
    
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion = OffsetDateTime.now();
    
    @Column(nullable = false)
    private Integer descargas = 0;
    
    @Column(name = "ultima_descarga_en")
    private OffsetDateTime ultimaDescargaEn;
}
