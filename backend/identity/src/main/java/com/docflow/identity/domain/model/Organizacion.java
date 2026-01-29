package com.docflow.identity.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.docflow.identity.domain.model.object.EstadoOrganizacion;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entidad Organización del sistema DocFlow.
 * Representa una organización a la que pueden pertenecer múltiples usuarios.
 */
@Entity
@Table(name = "organizaciones")
@Getter
@Setter
@NoArgsConstructor
public class Organizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> configuracion = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrganizacion estado = EstadoOrganizacion.ACTIVO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = OffsetDateTime.now();
    }

    /**
     * Verifica si la organización está activa.
     *
     * @return true si el estado es ACTIVO
     */
    public boolean isActiva() {
        return estado == EstadoOrganizacion.ACTIVO;
    }
}
