package com.docflow.documentcore.domain.service;

import java.time.OffsetDateTime;

/**
 * Puerto (interfaz) para publicar eventos de auditoría relacionados con cambios de versión.
 * 
 * <p>Esta interfaz define el contrato para la publicación de eventos de auditoría
 * siguiendo el patrón de Inversión de Dependencias (Dependency Inversion Principle).
 * La capa de aplicación depende de esta abstracción, no de la implementación concreta.</p>
 * 
 * <h3>Patrón de Diseño: Port (Hexagonal Architecture)</h3>
 * <p>Esta interfaz representa un <strong>puerto de salida</strong> en la arquitectura
 * hexagonal. El adaptador (infraestructura) implementará esta interfaz para
 * persistir eventos de auditoría, enviarlo a un message broker, o cualquier
 * otra estrategia de almacenamiento/publicación.</p>
 * 
 * <h3>Características</h3>
 * <ul>
 *   <li><strong>Desacoplamiento:</strong> La lógica de negocio no conoce los detalles
 *       de implementación de la auditoría</li>
 *   <li><strong>Testabilidad:</strong> Fácil de mockear en pruebas unitarias</li>
 *   <li><strong>Flexibilidad:</strong> La implementación puede cambiar sin afectar
 *       el servicio de aplicación</li>
 * </ul>
 * 
 * <h3>Implementaciones Esperadas</h3>
 * <ul>
 *   <li>Persistencia en base de datos (tabla audit_event)</li>
 *   <li>Publicación en message broker (RabbitMQ, Kafka, etc.)</li>
 *   <li>Escritura en archivo de log estructurado</li>
 * </ul>
 * 
 * @see com.docflow.documentcore.domain.model.Documento
 * @see com.docflow.documentcore.domain.model.Version
 */
public interface VersionChangeEventPublisher {
    
    /**
     * Publica un evento de auditoría para un rollback de versión.
     * 
     * <p>Este método debe ser llamado cada vez que se cambia la versión actual
     * de un documento, registrando quién realizó el cambio, cuándo y qué versiones
     * estuvieron involucradas.</p>
     * 
     * <p><strong>Características del evento:</strong></p>
     * <ul>
     *   <li>Sincrónico: Debe completarse antes de confirmar la transacción</li>
     *   <li>Idempotente: Puede ser llamado varias veces con los mismos parámetros</li>
     *   <li>Atómico: Si falla, debe hacer rollback de toda la transacción</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>
     * auditPublisher.publishVersionRollbackEvent(
     *     1L,           // usuarioId
     *     100L,         // documentoId
     *     1L,           // organizacionId
     *     205L,         // versionAnteriorId (versión antes del cambio)
     *     201L,         // versionNuevaId (versión después del cambio)
     *     OffsetDateTime.now()
     * );
     * </pre>
     * 
     * @param usuarioId ID del usuario que ejecuta la operación de rollback
     * @param documentoId ID del documento sobre el cual se realiza el rollback
     * @param organizacionId ID de la organización/tenant (para aislamiento multi-tenant)
     * @param versionAnteriorId ID de la versión que era actual antes del rollback
     * @param versionNuevaId ID de la versión que se marca como actual después del rollback
     * @param timestamp Momento exacto en que se realizó la operación
     * 
     * @throws RuntimeException si falla la publicación del evento (causará rollback de transacción)
     */
    void publishVersionRollbackEvent(
        Long usuarioId,
        Long documentoId,
        Long organizacionId,
        Long versionAnteriorId,
        Long versionNuevaId,
        OffsetDateTime timestamp
    );
}
