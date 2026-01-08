package com.docflow.audit.infrastructure.multitenancy;

import com.docflow.audit.domain.model.LogAuditoria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * Listener de eventos de MongoDB que intercepta operaciones de inserción (BeforeConvert)
 * para inyectar automáticamente el organizacion_id del contexto tenant actual.
 * 
 * Este listener es parte crítica del aislamiento multi-tenant (US-AUTH-004) en MongoDB.
 * Garantiza que:
 * 1. NUNCA se guarden logs de auditoría sin organizacion_id
 * 2. El organizacion_id se sobrescribe con el del Reactor Context (ignora input del cliente)
 * 3. Se establece automáticamente la fecha del evento si no está presente
 * 
 * IMPORTANTE: En entornos reactivos, el listener se ejecuta en el mismo hilo/contexto
 * que la operación de MongoDB, por lo que el Reactor Context está disponible.
 */
@Component
@Slf4j
public class MongoTenantListener extends AbstractMongoEventListener<LogAuditoria> {

    /**
     * Hook que se ejecuta ANTES de convertir la entidad a documento BSON (antes de insert).
     * 
     * @param event el evento de conversión con la entidad LogAuditoria
     */
    @Override
    public void onBeforeConvert(BeforeConvertEvent<LogAuditoria> event) {
        var logAuditoria = event.getSource();
        
        // Inyectar tenant desde Reactor Context
        TenantContextHolder.getTenantId()
            .doOnNext(tenantId -> {
                var currentValue = logAuditoria.getOrganizacionId();
                
                // Sobrescribir con tenant del contexto
                logAuditoria.setOrganizacionId(tenantId);
                
                // Log de auditoría si el valor fue sobrescrito
                if (currentValue != null && !currentValue.equals(tenantId)) {
                    log.warn(
                        "SEGURIDAD: Sobrescrito organizacion_id en LogAuditoria. " +
                        "Valor del cliente: {}, Valor del token: {}. " +
                        "Evento: {}",
                        currentValue,
                        tenantId,
                        logAuditoria.getCodigoEvento()
                    );
                } else {
                    log.debug(
                        "Tenant inyectado en LogAuditoria: organizacionId={}. Evento: {}",
                        tenantId,
                        logAuditoria.getCodigoEvento()
                    );
                }
                
                // Establecer fecha del evento si no está presente
                if (logAuditoria.getFechaEvento() == null) {
                    logAuditoria.setFechaEvento(OffsetDateTime.now());
                }
            })
            .onErrorResume(ex -> {
                log.error(
                    "Error al inyectar tenant en LogAuditoria. Evento: {}, Error: {}",
                    logAuditoria.getCodigoEvento(),
                    ex.getMessage()
                );
                // No propagar el error - dejar que MongoDB falle si organizacion_id es null
                return Mono.empty();
            })
            .block(); // Bloquear porque el listener no es reactivo (sincronía requerida por MongoDB driver)
    }
}
