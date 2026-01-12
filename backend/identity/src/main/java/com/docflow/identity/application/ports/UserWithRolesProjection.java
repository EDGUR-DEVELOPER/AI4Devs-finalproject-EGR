package com.docflow.identity.application.ports;

import java.time.OffsetDateTime;

/**
 * Proyección para queries JPQL que retornan usuarios con información de roles.
 * Usado en constructor expressions para eficiencia en consultas de base de
 * datos.
 * 
 * Esta proyección permite obtener datos desnormalizados (1 fila por
 * usuario-rol)
 * que luego se agrupan en memoria para construir UserWithRolesDto.
 */
public interface UserWithRolesProjection {

    /** ID único del usuario */
    Long getUsuarioId();

    /** Email del usuario */
    String getEmail();

    /** Nombre completo del usuario */
    String getNombreCompleto();

    /** Estado de membresía en la organización (ACTIVO, SUSPENDIDO) */
    String getEstado();

    /** ID del rol asignado (NULL si usuario sin roles) */
    Long getRolId();

    /** Código del rol (NULL si usuario sin roles) */
    String getRolCodigo();

    /** Nombre del rol (NULL si usuario sin roles) */
    String getRolNombre();

    /** Fecha de creación del usuario */
    OffsetDateTime getFechaCreacion();
}
