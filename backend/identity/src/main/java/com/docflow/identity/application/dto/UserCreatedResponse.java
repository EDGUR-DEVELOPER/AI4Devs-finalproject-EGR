package com.docflow.identity.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Record que representa la respuesta exitosa de creación de usuario.
 * Contiene los datos del usuario creado sin exponer información sensible (contraseña).
 * 
 * @param id Identificador único del usuario creado
 * @param email Email del usuario
 * @param nombreCompleto Nombre completo del usuario
 * @param organizacionId ID de la organización a la que fue asignado
 * @param fechaCreacion Fecha y hora de creación del usuario
 */
@Schema(description = "Usuario creado exitosamente")
public record UserCreatedResponse(
    
    @Schema(description = "ID único del usuario", example = "42")
    Long id,
    
    @Schema(description = "Email del usuario", example = "nuevo.usuario@docflow.com")
    String email,
    
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez García")
    String nombreCompleto,
    
    @Schema(description = "ID de la organización asignada", example = "1")
    Integer organizacionId,
    
    @Schema(description = "Fecha de creación", example = "2026-01-09T10:30:00Z")
    OffsetDateTime fechaCreacion
) {}
