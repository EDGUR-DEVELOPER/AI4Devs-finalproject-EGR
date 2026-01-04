package com.docflow.identity.application.ports.output;

import com.docflow.identity.domain.model.EstadoMembresia;
import com.docflow.identity.domain.model.UsuarioOrganizacion;
import com.docflow.identity.domain.model.UsuarioOrganizacionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad UsuarioOrganizacion.
 */
@Repository
public interface UsuarioOrganizacionRepository extends JpaRepository<UsuarioOrganizacion, UsuarioOrganizacionId> {

    /**
     * Obtiene todas las membresías activas de un usuario, ordenadas primero por predeterminada
     * y luego por fecha de asignación.
     * 
     * @param usuarioId el ID del usuario
     * @return lista de membresías activas ordenadas
     */
    @Query("""
        SELECT uo FROM UsuarioOrganizacion uo
        WHERE uo.usuarioId = :usuarioId 
        AND uo.estado = 'ACTIVO'
        ORDER BY uo.esPredeterminada DESC, uo.fechaAsignacion ASC
        """)
    List<UsuarioOrganizacion> findMembresiasActivasOrdenadas(@Param("usuarioId") Long usuarioId);

    /**
     * Busca membresías por usuario, estado y si es predeterminada.
     *
     * @param usuarioId el ID del usuario
     * @param estado el estado de la membresía
     * @param esPredeterminada si es la organización predeterminada
     * @return lista de membresías que cumplen los criterios
     */
    List<UsuarioOrganizacion> findByUsuarioIdAndEstadoAndEsPredeterminada(
        Long usuarioId, 
        EstadoMembresia estado, 
        Boolean esPredeterminada
    );

    /**
     * Cuenta el número de organizaciones activas para un usuario.
     *
     * @param usuarioId el ID del usuario
     * @param estado el estado de membresía
     * @return número de organizaciones activas
     */
    long countByUsuarioIdAndEstado(Long usuarioId, EstadoMembresia estado);
}
