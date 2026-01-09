package com.docflow.identity.application.ports;

import java.time.OffsetDateTime;

/**
 * Proyección para queries JPQL que retornan usuarios con información de roles.
 * Usado en constructor expressions para eficiencia en consultas de base de datos.
 * 
 * Esta proyección permite obtener datos desnormalizados (1 fila por usuario-rol)
 * que luego se agrupan en memoria para construir UserWithRolesDto.
 * 
 * @param usuarioId ID único del usuario
 * @param email Email del usuario
 * @param nombreCompleto Nombre completo del usuario
 * @param estado Estado de membresía en la organización (ACTIVO, SUSPENDIDO)
 * @param rolId ID del rol asignado (NULL si usuario sin roles)
 * @param rolCodigo Código del rol (NULL si usuario sin roles)
 * @param rolNombre Nombre del rol (NULL si usuario sin roles)
 * @param fechaCreacion Fecha de creación del usuario
 */
public record UserWithRolesProjection(
    Long usuarioId,
    String email,
    String nombreCompleto,
    String estado,
    Integer rolId,
    String rolCodigo,
    String rolNombre,
    OffsetDateTime fechaCreacion
) {
}
