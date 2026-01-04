package com.docflow.identity.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Entidad que representa la asignación de un rol a un usuario dentro de una organización.
 */
@Entity
@Table(name = "usuarios_roles")
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
