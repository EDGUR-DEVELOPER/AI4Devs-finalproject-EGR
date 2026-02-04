package com.docflow.documentcore.domain.model;

import com.docflow.documentcore.infrastructure.multitenancy.TenantEntityListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.OffsetDateTime;

/**
 * Permisos explícitos de usuarios sobre carpetas (ACL).
 * 
 * AISLAMIENTO MULTI-TENANT (US-AUTH-004):
 * - Hibernate Filter 'tenantFilter' aplica automáticamente WHERE organizacion_id = :tenantId
 * - TenantEntityListener inyecta organizacionId en @PrePersist/@PreUpdate
 */
@Entity
@Table(name = "permiso_carpeta_usuario")
@EntityListeners(TenantEntityListener.class)

@Filter(name = "tenantFilter", condition = "organizacion_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermisoCarpetaUsuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "carpeta_id", nullable = false)
    private Long carpetaId;
    
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
    
    @Column(name = "organizacion_id", nullable = false)
    private Long organizacionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_acceso", nullable = false, length = 20)
    private NivelAcceso nivelAcceso;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean recursivo = false;
    
    @Column(name = "fecha_asignacion", nullable = false)
    @Builder.Default
    private OffsetDateTime fechaAsignacion = OffsetDateTime.now();
}
