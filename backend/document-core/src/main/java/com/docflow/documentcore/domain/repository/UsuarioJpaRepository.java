package com.docflow.documentcore.domain.repository;

import com.docflow.documentcore.domain.model.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para lectura de usuarios y pertenencia a organización.
 */
@Repository
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    @Query("""
        SELECT u
        FROM UsuarioEntity u
        JOIN UsuarioOrganizacionEntity uo ON u.id = uo.usuarioId
        WHERE u.id = :usuarioId
          AND uo.organizacionId = :organizacionId
          AND uo.estado = 'ACTIVO'
    """)
    Optional<UsuarioEntity> findActiveByIdAndOrganizacionId(
            @Param("usuarioId") Long usuarioId,
            @Param("organizacionId") Long organizacionId
    );

    @Query("""
        SELECT u
        FROM UsuarioEntity u
        JOIN UsuarioOrganizacionEntity uo ON u.id = uo.usuarioId
        WHERE u.id IN :usuarioIds
          AND uo.organizacionId = :organizacionId
          AND uo.estado = 'ACTIVO'
    """)
    List<UsuarioEntity> findActiveByIdsAndOrganizacionId(
            @Param("usuarioIds") List<Long> usuarioIds,
            @Param("organizacionId") Long organizacionId
    );
}
