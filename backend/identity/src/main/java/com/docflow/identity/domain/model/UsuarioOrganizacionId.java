package com.docflow.identity.domain.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite key para la entidad UsuarioOrganizacion.
 * Representa la relación de pertenencia entre usuario y organización.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioOrganizacionId implements Serializable {
    
    private Long usuarioId;
    private Integer organizacionId;
}
