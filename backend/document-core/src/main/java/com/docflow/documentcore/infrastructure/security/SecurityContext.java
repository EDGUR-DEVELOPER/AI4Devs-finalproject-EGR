package com.docflow.documentcore.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Contexto de seguridad para la solicitud actual.
 * 
 * Almacena información del usuario autenticado y organización.
 * Poblado por filtros de seguridad/JWT.
 */
@Getter
@Setter
@Component
@RequestScope
public class SecurityContext {
    
    private Long usuarioId;
    private Long organizacionId;
    private String username;
    
    /**
     * Obtiene el ID de usuario actual.
     * 
     * @return ID del usuario autenticado
     * @throws IllegalStateException Si no hay usuario autenticado
     */
    public Long getUsuarioId() {
        if (usuarioId == null) {
            throw new IllegalStateException("No hay usuario autenticado en el contexto");
        }
        return usuarioId;
    }
    
    /**
     * Obtiene el ID de organización actual.
     * 
     * @return ID de la organización del usuario
     * @throws IllegalStateException Si no hay organización en el contexto
     */
    public Long getOrganizacionId() {
        if (organizacionId == null) {
            throw new IllegalStateException("No hay organización en el contexto");
        }
        return organizacionId;
    }
}
