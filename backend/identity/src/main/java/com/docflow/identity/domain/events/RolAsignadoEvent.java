package com.docflow.identity.domain.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * Evento de dominio emitido cuando se asigna un rol a un usuario.
 * Se publica a Kafka para auditoría y procesamiento asíncrono por otros microservicios.
 * <p>
 * Usa JSON simple con versionado manual para compatibilidad y evolución del esquema.
 * El campo {@code eventId} garantiza idempotencia en consumers.
 */
@Schema(description = "Evento de asignación de rol a usuario para auditoría")
public record RolAsignadoEvent(
    
    @JsonProperty("event_id")
    @Schema(
        description = "ID único del evento (UUID) para deduplicación en consumers",
        example = "550e8400-e29b-41d4-a716-446655440000",
        required = true
    )
    String eventId,
    
    @JsonProperty("event_version")
    @Schema(
        description = "Versión del esquema del evento para evolución compatible",
        example = "1.0",
        required = true
    )
    String eventVersion,
    
    @JsonProperty("usuario_id")
    @Schema(
        description = "ID del usuario al que se asignó el rol",
        example = "100",
        required = true
    )
    Long usuarioId,
    
    @JsonProperty("rol_id")
    @Schema(
        description = "ID del rol asignado",
        example = "2",
        required = true
    )
    Integer rolId,
    
    @JsonProperty("organizacion_id")
    @Schema(
        description = "ID de la organización en la que se hizo la asignación",
        example = "1",
        required = true
    )
    Integer organizacionId,
    
    @JsonProperty("asignado_por")
    @Schema(
        description = "ID del usuario administrador que realizó la asignación",
        example = "50",
        required = true
    )
    Long asignadoPor,
    
    @JsonProperty("es_reactivacion")
    @Schema(
        description = "Indica si fue una reactivación de asignación previamente desactivada",
        example = "false",
        required = true
    )
    boolean esReactivacion,
    
    @JsonProperty("timestamp")
    @Schema(
        description = "Fecha y hora en que ocurrió la asignación (ISO 8601)",
        example = "2026-01-09T10:30:00Z",
        required = true
    )
    OffsetDateTime timestamp
    
) {
    
    /**
     * Constructor de conveniencia sin eventId (se genera automáticamente).
     * El eventId debe ser generado por el publisher usando UUID.randomUUID().
     */
    public RolAsignadoEvent(
            String eventId,
            Long usuarioId,
            Integer rolId,
            Integer organizacionId,
            Long asignadoPor,
            boolean esReactivacion
    ) {
        this(
            eventId,
            "1.0", // Versión actual del evento
            usuarioId,
            rolId,
            organizacionId,
            asignadoPor,
            esReactivacion,
            OffsetDateTime.now()
        );
    }
}
