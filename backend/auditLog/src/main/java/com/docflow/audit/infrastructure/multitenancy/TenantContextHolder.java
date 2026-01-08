package com.docflow.audit.infrastructure.multitenancy;

import com.docflow.audit.domain.exceptions.TenantContextMissingException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Holder para el contexto de organización (tenant) en entornos reactivos (WebFlux).
 * 
 * A diferencia del modelo Thread-per-request (ThreadLocal), en programación reactiva
 * usamos el Reactor Context para propagar datos a través de la cadena de operadores.
 * 
 * Este componente es crítico para el aislamiento multi-tenant (US-AUTH-004)
 * en microservicios reactivos basados en Spring WebFlux.
 * 
 * Flujo típico:
 * 1. WebFilter extrae organizacion_id del header X-Organization-Id
 * 2. Se establece en el Reactor Context con TenantContextHolder.TENANT_KEY
 * 3. Los servicios acceden con TenantContextHolder.getTenantId()
 * 4. El contexto se propaga automáticamente en la cadena reactiva
 */
@Slf4j
public final class TenantContextHolder {

    private TenantContextHolder() {
        throw new UnsupportedOperationException("Utility class no debe instanciarse");
    }

    /**
     * Clave para almacenar el tenant ID en el Reactor Context.
     */
    public static final String TENANT_KEY = "organizacionId";

    /**
     * Obtiene el ID de organización (tenant) del Reactor Context actual.
     * 
     * @return Mono con el ID de la organización
     * @throws TenantContextMissingException si no hay tenant en el contexto
     */
    public static Mono<Integer> getTenantId() {
        return Mono.deferContextual(ctx -> {
            if (!ctx.hasKey(TENANT_KEY)) {
                return Mono.error(new TenantContextMissingException(
                    "No hay contexto de organización disponible. " +
                    "Esta operación requiere un tenant activo. " +
                    "Verifique que el header X-Organization-Id esté presente."
                ));
            }
            Integer tenantId = ctx.get(TENANT_KEY);
            log.debug("Tenant obtenido del contexto reactivo: organizacionId={}", tenantId);
            return Mono.just(tenantId);
        });
    }

    /**
     * Obtiene el ID de organización del contexto si existe, o null si no está disponible.
     * 
     * @return Mono con el ID de la organización o Mono vacío si no hay contexto
     */
    public static Mono<Integer> getTenantIdOrEmpty() {
        return Mono.deferContextual(ctx -> {
            if (!ctx.hasKey(TENANT_KEY)) {
                return Mono.empty();
            }
            return Mono.just(ctx.get(TENANT_KEY));
        });
    }

    /**
     * Verifica si hay un contexto de tenant establecido.
     * 
     * @return Mono<Boolean> - true si hay tenant disponible, false en caso contrario
     */
    public static Mono<Boolean> hasTenant() {
        return Mono.deferContextual(ctx -> 
            Mono.just(ctx.hasKey(TENANT_KEY))
        );
    }

    /**
     * Establece el tenant en el contexto reactivo.
     * 
     * Usado por filtros para inyectar el tenant en la cadena reactiva.
     * 
     * @param context el contexto reactivo actual
     * @param tenantId el ID de la organización
     * @return el contexto modificado con el tenant
     */
    public static Context setTenantId(Context context, Integer tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant ID no puede ser null");
        }
        log.debug("Contexto tenant establecido en Reactor Context: organizacionId={}", tenantId);
        return context.put(TENANT_KEY, tenantId);
    }
}
