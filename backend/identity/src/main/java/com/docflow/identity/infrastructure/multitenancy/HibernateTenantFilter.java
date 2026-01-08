package com.docflow.identity.infrastructure.multitenancy;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Componente que habilita el filtro de Hibernate 'tenantFilter' para el tenant actual.
 * 
 * Este componente debe invocarse antes de ejecutar cualquier consulta JPA que requiera
 * filtrado por organización. Se integra con el patrón de interceptor/aspect para
 * habilitación automática.
 * 
 * Flujo típico:
 * 1. El TenantContextFilter establece el tenant en TenantContextHolder
 * 2. Un aspecto AOP detecta llamadas a repositorios JPA
 * 3. Se invoca enableTenantFilter() antes de la consulta
 * 4. Hibernate aplica automáticamente WHERE organizacion_id = :tenantId
 * 
 * Parte de la implementación de US-AUTH-004: Aislamiento multi-tenant.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HibernateTenantFilter {

    private final EntityManager entityManager;

    /**
     * Nombre del filtro Hibernate definido en las entidades con @FilterDef.
     */
    private static final String TENANT_FILTER_NAME = "tenantFilter";

    /**
     * Nombre del parámetro del filtro (debe coincidir con @ParamDef).
     */
    private static final String TENANT_PARAM_NAME = "tenantId";

    /**
     * Habilita el filtro Hibernate para el tenant actual del contexto.
     * 
     * Este método obtiene el tenant del TenantContextHolder y lo configura
     * en el filtro de Hibernate. Todas las consultas subsiguientes en esta
     * sesión filtrarán automáticamente por organizacion_id.
     * 
     * @throws com.docflow.identity.domain.exceptions.TenantContextMissingException
     *         si no hay tenant en el contexto actual
     */
    public void enableTenantFilter() {
        if (!TenantContextHolder.hasTenant()) {
            log.trace("No hay contexto tenant - omitiendo habilitación de filtro Hibernate");
            return;
        }

        var tenantId = TenantContextHolder.getTenantId();
        var session = entityManager.unwrap(Session.class);
        
        // Habilitar filtro y establecer parámetro
        var filter = session.enableFilter(TENANT_FILTER_NAME);
        filter.setParameter(TENANT_PARAM_NAME, tenantId);
        
        log.debug("Filtro Hibernate habilitado: tenantFilter con organizacionId={}", tenantId);
    }

    /**
     * Deshabilita el filtro Hibernate para permitir consultas sin restricción de tenant.
     * 
     * Usar con extrema precaución: solo en casos edge como reportes administrativos
     * globales o procesos batch que operan sobre todas las organizaciones.
     */
    public void disableTenantFilter() {
        var session = entityManager.unwrap(Session.class);
        session.disableFilter(TENANT_FILTER_NAME);
        log.debug("Filtro Hibernate deshabilitado: tenantFilter");
    }

    /**
     * Verifica si el filtro Hibernate está actualmente habilitado.
     * 
     * @return true si el filtro está activo, false en caso contrario
     */
    public boolean isTenantFilterEnabled() {
        var session = entityManager.unwrap(Session.class);
        return session.getEnabledFilter(TENANT_FILTER_NAME) != null;
    }
}
