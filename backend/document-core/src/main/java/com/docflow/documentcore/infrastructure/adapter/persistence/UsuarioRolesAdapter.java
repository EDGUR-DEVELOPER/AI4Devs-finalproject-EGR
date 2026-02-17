package com.docflow.documentcore.infrastructure.adapter.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;

/**
 * Adaptador de persistencia para consultar roles de usuarios.
 * 
 * Consulta la tabla usuarios_roles de la BD de identity para obtener
 * los roles asignados a un usuario en una organización.
 * 
 * Nota: Esta es una consulta nativa a la BD porque la tabla usuarios_roles
 * se gestiona en el servicio de identity, no en document-core.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UsuarioRolesAdapter {

    private final EntityManager entityManager;

    /**
     * Obtiene los IDs de roles asignados a un usuario en una organización.
     * 
     * @param usuarioId ID del usuario
     * @param organizacionId ID de la organización
     * @return lista de IDs de roles activos del usuario
     */
    public List<Long> obtenerRolesDelUsuario(Long usuarioId, Long organizacionId) {
        log.debug("Obteniendo roles del usuario: usuarioId={}, organizacionId={}", 
                usuarioId, organizacionId);

        String sql = 
            "SELECT ur.rol_id FROM usuarios_roles ur " +
            "WHERE ur.usuario_id = :usuarioId " +
            "AND ur.organizacion_id = :organizacionId " +
            "AND ur.activo = true";

        try {
            @SuppressWarnings("unchecked")
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("usuarioId", usuarioId);
            query.setParameter("organizacionId", organizacionId);
            
            @SuppressWarnings("unchecked")
            List<Long> rolesIds = (List<Long>) query.getResultList();
            
            log.debug("Roles encontrados para usuario: usuarioId={}, rolesIds={}", 
                    usuarioId, rolesIds);
            
            return rolesIds;
        } catch (Exception e) {
            log.warn("Error al consultar roles del usuario: usuarioId={}, organizacionId={}, error={}", 
                    usuarioId, organizacionId, e.getMessage());
            return List.of();
        }
    }
}
