package com.docflow.identity.domain.model;

import com.docflow.identity.infrastructure.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.OffsetDateTime;

/**
 * Entidad que representa la asignación de un rol a un usuario dentro de una organización.
 * 
 * AISLAMIENTO MULTI-TENANT (US-AUTH-004):
 * - Cada asignación está vinculada a una organización específica (organizacionId NOT NULL)
 * - Hibernate Filter 'tenantFilter' aplica automáticamente WHERE organizacion_id = :tenantId
 * - TenantEntityListener inyecta organizacionId en @PrePersist/@PreUpdate
 * - Un mismo usuario puede tener diferentes roles en diferentes organizaciones
 */
@Entity
@Table(name = "usuarios_roles")
@EntityListeners(TenantEntityListener.class)
@Filter(name = "tenantFilter", condition = "organizacion_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRol {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
    
    @Column(name = "rol_id", nullable = false)
    private Integer rolId;
    
    @Column(name = "organizacion_id", nullable = false)
    private Integer organizacionId;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    @Column(name = "fecha_asignacion", nullable = false)
    private OffsetDateTime fechaAsignacion = OffsetDateTime.now();
    
    @Column(name = "asignado_por")
    private Long asignadoPor;
}
