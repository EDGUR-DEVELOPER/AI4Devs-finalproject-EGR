package com.docflow.documentcore.infrastructure.multitenancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspecto AOP que intercepta llamadas a repositorios JPA y habilita automáticamente
 * el filtro de Hibernate para el tenant actual.
 * 
 * Este aspecto garantiza que TODAS las consultas a la base de datos se filtren
 * automáticamente por organizacion_id sin que los desarrolladores tengan que
 * recordar habilitarlo manualmente en cada query.
 * 
 * Intercepta:
 * - Todos los métodos de clases anotadas con @Repository
 * - Métodos en paquetes com.docflow.documentcore.application.ports.output.*
 * 
 * Parte de US-AUTH-004: Aislamiento multi-tenant.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantFilterAspect {

    private final HibernateTenantFilter hibernateTenantFilter;

    /**
     * Advice que intercepta llamadas a repositorios y habilita el filtro tenant.
     * 
     * @param joinPoint el punto de unión de la ejecución del método
     * @return el resultado de la ejecución del método del repositorio
     * @throws Throwable si ocurre error durante la ejecución
     */
    @Around("within(@org.springframework.stereotype.Repository *) && " +
            "execution(* com.docflow.documentcore.application.ports.output..*(..))")
    public Object enableTenantFilterForRepositories(ProceedingJoinPoint joinPoint) throws Throwable {
        
        var methodName = joinPoint.getSignature().toShortString();
        
        try {
            // Habilitar filtro si hay contexto tenant
            if (TenantContextHolder.hasTenant()) {
                hibernateTenantFilter.enableTenantFilter();
                log.trace("Filtro tenant habilitado para: {}", methodName);
            } else {
                log.trace("Sin contexto tenant para: {} - continuando sin filtro", methodName);
            }
            
            // Ejecutar método del repositorio con filtro activo
            return joinPoint.proceed();
            
        } catch (Exception e) {
            log.error("Error ejecutando método de repositorio con filtro tenant: {}", methodName, e);
            throw e;
        }
    }

    /**
     * Advice para métodos que explícitamente requieren desactivar el filtro.
     * 
     * @param joinPoint el punto de unión de la ejecución del método
     * @return el resultado de la ejecución del método
     * @throws Throwable si ocurre error durante la ejecución
     */
    @Around("@annotation(com.docflow.documentcore.infrastructure.multitenancy.BypassTenantFilter)")
    public Object bypassTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        var methodName = joinPoint.getSignature().toShortString();
        
        log.warn("SEGURIDAD: Ejecutando método SIN filtro tenant: {}. " +
                 "Validar que esto es intencional para operaciones administrativas.", methodName);
        
        try {
            // Deshabilitar filtro explícitamente
            hibernateTenantFilter.disableTenantFilter();
            return joinPoint.proceed();
        } finally {
            // Re-habilitar si hay contexto
            if (TenantContextHolder.hasTenant()) {
                hibernateTenantFilter.enableTenantFilter();
            }
        }
    }
}
