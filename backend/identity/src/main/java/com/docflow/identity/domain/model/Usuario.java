package com.docflow.identity.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Entidad Usuario del sistema DocFlow.
 * Representa un usuario que puede pertenecer a múltiples organizaciones.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "hash_contrasena", nullable = false)
    private String hashContrasena;

    @Column(name = "nombre_completo", length = 100, nullable = false)
    private String nombreCompleto;

    @Column(name = "mfa_habilitado", nullable = false)
    private Boolean mfaHabilitado = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(name = "fecha_desactivacion")
    private OffsetDateTime fechaDesactivacion;

    @Column(name = "fecha_eliminacion")
    private OffsetDateTime fechaEliminacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Verifica si el usuario ha sido eliminado (soft delete).
     *
     * @return true si el usuario está eliminado
     */
    public boolean isDeleted() {
        return fechaEliminacion != null;
    }

    /**
     * Verifica si el usuario puede autenticarse activamente.
     *
     * @return true si el usuario está activo y no eliminado
     */
    public boolean isActivo() {
        return estado == EstadoUsuario.ACTIVO && fechaEliminacion == null;
    }

    /**
     * Desactiva el usuario estableciendo estado INACTIVO y registrando timestamp.
     */
    public void desactivar() {
        this.estado = EstadoUsuario.INACTIVO;
        this.fechaDesactivacion = OffsetDateTime.now();
    }
}
