package com.docflow.identity.infrastructure.multitenancy;

import com.docflow.identity.domain.exception.TenantContextMissingException;

import lombok.extern.slf4j.Slf4j;

/**
 * Holder estático que mantiene el contexto de organización (tenant) del usuario actual
 * usando ThreadLocal para propagación segura en peticiones concurrentes.
 * 
 * Este componente es crítico para el aislamiento multi-tenant (US-AUTH-004).
 * Garantiza que cada hilo de ejecución (request HTTP) tenga su propio contexto
 * de organización aislado, evitando fugas de datos entre peticiones concurrentes.
 * 
 * Flujo típico:
 * 1. El filtro/interceptor extrae organizacion_id del header X-Organization-Id
 * 2. Llama a TenantContextHolder.setTenantId(organizacionId)
 * 3. Los servicios de negocio acceden con TenantContextHolder.getTenantId()
 * 4. El filtro limpia el contexto con TenantContextHolder.clear() al finalizar
 * 
 * IMPORTANTE: Siempre debe limpiarse en un bloque finally para evitar memory leaks
 * y contaminación de contexto entre requests que reutilicen el mismo thread del pool.
 */
@Slf4j
public final class TenantContextHolder {

    private TenantContextHolder() {
        throw new UnsupportedOperationException("Utility class no debe instanciarse");
    }

    private static final ThreadLocal<Integer> TENANT_CONTEXT = new ThreadLocal<>();

    /**
     * Establece el ID de organización (tenant) para el contexto actual del hilo.
     * 
     * @param tenantId el ID de la organización (no puede ser null)
     * @throws IllegalArgumentException si tenantId es null
     */
    public static void setTenantId(Integer tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("El tenant ID no puede ser null");
        }
        TENANT_CONTEXT.set(tenantId);
        log.debug("Contexto tenant establecido: organizacionId={}", tenantId);
    }

    /**
     * Obtiene el ID de organización (tenant) del contexto actual del hilo.
     * 
     * @return el ID de la organización
     * @throws TenantContextMissingException si no hay tenant en el contexto actual
     */
    public static Integer getTenantId() {
        var tenantId = TENANT_CONTEXT.get();
        if (tenantId == null) {
            throw new TenantContextMissingException(
                "No hay contexto de organización disponible. " +
                "Esta operación requiere un tenant activo. " +
                "Verifique que el header X-Organization-Id esté presente o que el JWT sea válido."
            );
        }
        return tenantId;
    }

    /**
     * Obtiene el ID de organización del contexto actual si existe, o null si no está disponible.
     * 
     * Útil para casos donde el tenant es opcional (ej. endpoints públicos).
     * 
     * @return el ID de la organización o null si no hay contexto
     */
    public static Integer getTenantIdOrNull() {
        return TENANT_CONTEXT.get();
    }

    /**
     * Verifica si hay un contexto de tenant establecido.
     * 
     * @return true si hay tenant disponible, false en caso contrario
     */
    public static boolean hasTenant() {
        return TENANT_CONTEXT.get() != null;
    }

    /**
     * Limpia el contexto de tenant del hilo actual.
     * 
     * CRÍTICO: Debe llamarse en un bloque finally después de procesar cada petición
     * para evitar memory leaks y contaminación de contexto entre requests.
     */
    public static void clear() {
        var tenantId = TENANT_CONTEXT.get();
        if (tenantId != null) {
            log.debug("Limpiando contexto tenant: organizacionId={}", tenantId);
        }
        TENANT_CONTEXT.remove();
    }
}
