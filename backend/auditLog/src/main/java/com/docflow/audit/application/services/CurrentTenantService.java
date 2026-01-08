package com.docflow.audit.application.services;

import com.docflow.audit.infrastructure.multitenancy.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Servicio de aplicación que proporciona acceso al contexto de organización (tenant) actual
 * en entornos reactivos.
 * 
 * Encapsula el acceso al TenantContextHolder proporcionando una API de alto nivel
 * para los casos de uso reactivos que necesitan conocer la organización activa del usuario.
 * 
 * Este servicio es parte de la implementación de US-AUTH-004: Aislamiento de datos
 * por organización (multi-tenancy) en microservicios reactivos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentTenantService {

    /**
     * Obtiene el ID de la organización (tenant) del contexto reactivo actual.
     * 
     * @return Mono con el ID de la organización activa (nunca null dentro del Mono)
     */
    public Mono<Integer> getOrganizacionId() {
        return TenantContextHolder.getTenantId();
    }

    /**
     * Verifica si existe un contexto de tenant establecido.
     * 
     * @return Mono<Boolean> - true si hay una organización en el contexto, false en caso contrario
     */
    public Mono<Boolean> hasOrganizacionContext() {
        return TenantContextHolder.hasTenant();
    }

    /**
     * Obtiene el ID de organización si está disponible, o Mono vacío si no hay contexto.
     * 
     * @return Mono con el ID de la organización o Mono.empty()
     */
    public Mono<Integer> getOrganizacionIdOrEmpty() {
        return TenantContextHolder.getTenantIdOrEmpty();
    }
}
