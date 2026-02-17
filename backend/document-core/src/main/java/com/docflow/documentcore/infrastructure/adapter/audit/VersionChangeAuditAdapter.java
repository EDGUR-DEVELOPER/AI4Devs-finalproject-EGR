package com.docflow.documentcore.infrastructure.adapter.audit;

import com.docflow.documentcore.domain.service.VersionChangeEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Adaptador de infraestructura para la publicación de eventos de auditoría de cambios de versión.
 * 
 * <p>Este adaptador implementa el puerto {@link VersionChangeEventPublisher} definido en la
 * capa de dominio, siguiendo el patrón de Arquitectura Hexagonal (Ports & Adapters).</p>
 * 
 * <h3>Implementación Actual: Logging Estructurado</h3>
 * <p>La implementación actual registra eventos de auditoría mediante logging estructurado.
 * Esto permite:</p>
 * <ul>
 *   <li>Trazabilidad completa de cambios de versión</li>
 *   <li>Integración con sistemas de agregación de logs (ELK, Splunk, etc.)</li>
 *   <li>Análisis y alertas basadas en patrones de uso</li>
 * </ul>
 * 
 * <h3>Evolución Futura</h3>
 * <p>Este adaptador puede ser extendido o reemplazado para soportar:</p>
 * <ul>
 *   <li><strong>Persistencia en BD:</strong> Tabla audit_event con campos estructurados</li>
 *   <li><strong>Message Broker:</strong> Publicación asíncrona en RabbitMQ/Kafka</li>
 *   <li><strong>Sistema externo:</strong> Integración con herramientas de auditoría empresarial</li>
 * </ul>
 * 
 * <p>El cambio de implementación no requiere modificar el servicio de aplicación,
 * solo reemplazar este componente gracias al desacoplamiento proporcionado por
 * la arquitectura hexagonal.</p>
 * 
 * <h3>Formato de Log</h3>
 * <p>Los eventos se registran en formato estructurado con nivel INFO:</p>
 * <pre>
 * AUDIT_EVENT: VERSION_ROLLBACK | Usuario: 1 | Documento: 100 | Org: 1 | 
 *              Versión anterior: 205 | Versión nueva: 201 | Timestamp: 2026-02-10T14:30:00Z
 * </pre>
 * 
 * @see VersionChangeEventPublisher
 * @see com.docflow.documentcore.application.service.DocumentoVersionChangeService
 */
@Component
@Slf4j
public class VersionChangeAuditAdapter implements VersionChangeEventPublisher {
    
    /**
     * {@inheritDoc}
     * 
     * <p>Esta implementación registra el evento de rollback mediante logging estructurado
     * con nivel INFO. El log incluye todos los campos relevantes para auditoría y
     * trazabilidad.</p>
     * 
     * <h3>Características de la Implementación</h3>
     * <ul>
     *   <li><strong>Síncrono:</strong> Ejecuta en el mismo thread de la transacción</li>
     *   <li><strong>Rápido:</strong> Operación de bajo costo (solo logging)</li>
     *   <li><strong>Idempotente:</strong> Múltiples llamadas con mismos datos loguean múltiples eventos</li>
     * </ul>
     * 
     * <h3>Formato del Mensaje</h3>
     * <p>El mensaje sigue un formato estructurado para facilitar parsing automático:</p>
     * <ul>
     *   <li><strong>Tipo:</strong> VERSION_ROLLBACK (constante para identificación)</li>
     *   <li><strong>IDs:</strong> Usuario, Documento, Organización, Versiones</li>
     *   <li><strong>Timestamp:</strong> Momento exacto de la operación</li>
     * </ul>
     *
     * @throws RuntimeException Esta implementación no lanza excepciones (logging nunca falla la operación)
     */
    @Override
    public void publishVersionRollbackEvent(
        Long usuarioId,
        Long documentoId,
        Long organizacionId,
        Long versionAnteriorId,
        Long versionNuevaId,
        OffsetDateTime timestamp
    ) {
        // Registrar evento de auditoría con todos los detalles relevantes
        log.info(
            "AUDIT_EVENT: VERSION_ROLLBACK | Usuario: {} | Documento: {} | Organización: {} | " +
            "Versión anterior: {} | Versión nueva: {} | Timestamp: {}",
            usuarioId,
            documentoId,
            organizacionId,
            versionAnteriorId,
            versionNuevaId,
            timestamp
        );
        
        // Log adicional para debugging (menos estructurado, más legible)
        log.debug(
            "Evento de rollback registrado -> Usuario #{} cambió documento #{} de versión #{} a versión #{} en organización #{}",
            usuarioId, documentoId, versionAnteriorId, versionNuevaId, organizacionId
        );
    }
    
    // NOTA: Métodos futuros para otros tipos de eventos de versión pueden agregarse aquí
    // Ejemplos: publishVersionCreatedEvent, publishVersionDeletedEvent, etc.
}
