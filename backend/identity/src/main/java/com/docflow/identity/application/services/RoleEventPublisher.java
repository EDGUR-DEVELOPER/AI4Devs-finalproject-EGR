package com.docflow.identity.application.services;

import com.docflow.identity.domain.events.RolAsignadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio responsable de publicar eventos de asignación de roles a Kafka.
 * Maneja retry automático según configuración del producer y logging estructurado de fallos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleEventPublisher {
    
    private static final String TOPIC_ROLE_ASSIGNED = "identity.role.assigned";
    
    private final KafkaTemplate<String, RolAsignadoEvent> kafkaTemplate;
    
    /**
     * Publica un evento de asignación de rol a Kafka para auditoría externa.
     * <p>
     * La key del mensaje es {@code organizacionId-usuarioId} para garantizar ordenamiento
     * dentro de la misma partición. El eventId es un UUID generado automáticamente.
     * <p>
     * Nota: No lanza excepciones para evitar rollback de la transacción principal.
     * Los fallos se loguean estructuradamente para alertas operativas.
     *
     * @param usuarioId ID del usuario al que se asignó el rol
     * @param rolId ID del rol asignado
     * @param organizacionId ID de la organización
     * @param asignadoPor ID del administrador que realizó la asignación
     * @param esReactivacion True si fue reactivación, false si es nueva asignación
     */
    public void publishRolAsignadoEvent(
            Long usuarioId,
            Integer rolId,
            Integer organizacionId,
            Long asignadoPor,
            boolean esReactivacion
    ) {
        var eventId = UUID.randomUUID().toString();
        var evento = new RolAsignadoEvent(
            eventId,
            usuarioId,
            rolId,
            organizacionId,
            asignadoPor,
            esReactivacion
        );
        
        // Key para ordenamiento: organizacionId-usuarioId
        var key = String.format("%d-%d", organizacionId, usuarioId);
        
        try {
            CompletableFuture<SendResult<String, RolAsignadoEvent>> future = 
                kafkaTemplate.send(TOPIC_ROLE_ASSIGNED, key, evento);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Evento de rol asignado publicado exitosamente a Kafka: eventId={}, topic={}, partition={}, offset={}",
                        eventId,
                        TOPIC_ROLE_ASSIGNED,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                    );
                } else {
                    // Log estructurado para alertas operativas (después de reintentos agotados)
                    log.error(
                        "Error publicando evento a Kafka después de reintentos. Datos: {}",
                        Map.of(
                            "eventId", eventId,
                            "topic", TOPIC_ROLE_ASSIGNED,
                            "usuarioId", usuarioId,
                            "rolId", rolId,
                            "organizacionId", organizacionId,
                            "asignadoPor", asignadoPor,
                            "esReactivacion", esReactivacion
                        ),
                        ex
                    );
                    // No relanzamos la excepción para evitar rollback de la transacción de BD
                }
            });
            
        } catch (Exception ex) {
            // Captura excepciones síncronas (ej. serialización)
            log.error(
                "Error al intentar enviar evento a Kafka. Datos: {}",
                Map.of(
                    "eventId", eventId,
                    "topic", TOPIC_ROLE_ASSIGNED,
                    "usuarioId", usuarioId,
                    "rolId", rolId,
                    "organizacionId", organizacionId
                ),
                ex
            );
        }
    }
}
