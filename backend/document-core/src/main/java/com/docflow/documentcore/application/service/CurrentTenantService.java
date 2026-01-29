package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.exception.TenantContextMissingException;
import com.docflow.documentcore.infrastructure.multitenancy.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de aplicación que proporciona acceso al contexto de organización (tenant) actual.
 * 
 * Encapsula el acceso al TenantContextHolder proporcionando una API de alto nivel
 * para los casos de uso que necesitan conocer la organización activa del usuario.
 * 
 * Este servicio es parte de la implementación de US-AUTH-004: Aislamiento de datos
 * por organización (multi-tenancy).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentTenantService {

    /**
     * Obtiene el ID de la organización (tenant) del contexto actual del usuario autenticado.
     * 
     * @return el ID de la organización activa (nunca null)
     * @throws TenantContextMissingException si no hay contexto de organización disponible
     */
    public Integer getOrganizacionId() {
        return TenantContextHolder.getTenantId();
    }

    /**
     * Verifica si existe un contexto de tenant establecido.
     * 
     * @return true si hay una organización en el contexto, false en caso contrario
     */
    public boolean hasOrganizacionContext() {
        return TenantContextHolder.hasTenant();
    }

    /**
     * Obtiene el ID de organización si está disponible, o null si no hay contexto.
     * 
     * @return el ID de la organización o null si no hay contexto
     */
    public Integer getOrganizacionIdOrNull() {
        return TenantContextHolder.getTenantIdOrNull();
    }

    /**
     * Valida que existe un contexto de tenant y lanza excepción descriptiva si no.
     * 
     * @param operacion descripción de la operación que requiere tenant (para logging)
     * @throws TenantContextMissingException si no hay tenant en el contexto
     */
    public void requireTenantContext(String operacion) {
        if (!TenantContextHolder.hasTenant()) {
            var mensaje = String.format(
                "La operación '%s' requiere contexto de organización pero no está disponible. " +
                "Verifique que el usuario esté autenticado y tenga una organización activa.",
                operacion
            );
            log.error(mensaje);
            throw new TenantContextMissingException(mensaje);
        }
    }
}
