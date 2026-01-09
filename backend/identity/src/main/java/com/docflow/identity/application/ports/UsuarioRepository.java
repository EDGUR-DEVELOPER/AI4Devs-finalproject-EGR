package com.docflow.identity.application.ports;

import com.docflow.identity.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para la entidad Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por email que no esté eliminado (soft delete).
     *
     * @param email el email del usuario
     * @return Optional con el usuario si existe y no está eliminado
     */
    Optional<Usuario> findByEmailAndFechaEliminacionIsNull(String email);

    /**
     * Verifica si existe un usuario con el email dado.
     *
     * @param email el email a verificar
     * @return true si existe un usuario con ese email
     */
    boolean existsByEmail(String email);
    
    /**
     * Busca un usuario por ID que no esté eliminado (soft delete).
     * Utilizado para validar que el usuario objetivo existe y está activo.
     *
     * @param id ID del usuario
     * @return Optional con el usuario si existe y no está eliminado
     */
    Optional<Usuario> findByIdAndFechaEliminacionIsNull(Long id);
}
