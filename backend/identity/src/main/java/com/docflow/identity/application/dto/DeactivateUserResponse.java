package com.docflow.identity.application.dto;

import java.time.OffsetDateTime;

/**
 * Respuesta al desactivar un usuario.
 *
 * @param usuarioId ID del usuario desactivado
 * @param mensaje Mensaje de confirmación
 * @param fechaDesactivacion Timestamp de cuándo se desactivó el usuario
 */
public record DeactivateUserResponse(
    Long usuarioId,
    String mensaje,
    OffsetDateTime fechaDesactivacion
) {}
