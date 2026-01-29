package com.docflow.identity.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

import com.docflow.identity.domain.model.object.EstadoMembresia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Entidad que representa la membresía de un usuario a una organización.
 * Incluye información sobre el estado de la membresía y si es la organización predeterminada.
 */
@Entity
@Table(name = "usuarios_organizaciones")
@IdClass(UsuarioOrganizacionId.class)
@Getter
@Setter
@NoArgsConstructor
public class UsuarioOrganizacion {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Id
    @Column(name = "organizacion_id")
    private Integer organizacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMembresia estado = EstadoMembresia.ACTIVO;

    @Column(name = "es_predeterminada", nullable = false)
    private Boolean esPredeterminada = false;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private OffsetDateTime fechaAsignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacion_id", insertable = false, updatable = false)
    private Organizacion organizacion;

    @PrePersist
    protected void onCreate() {
        fechaAsignacion = OffsetDateTime.now();
    }

    /**
     * Verifica si la membresía está activa.
     *
     * @return true si el estado es ACTIVO
     */
    public boolean isActiva() {
        return estado == EstadoMembresia.ACTIVO;
    }
}
