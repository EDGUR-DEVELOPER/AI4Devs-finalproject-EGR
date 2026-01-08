package com.docflow.documentcore.infrastructure.multitenancy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para marcar métodos de repositorio que deben ejecutarse
 * SIN el filtro automático de tenant.
 * 
 * ADVERTENCIA: Usar con extrema precaución. Solo para casos edge como:
 * - Reportes administrativos globales que requieren datos de todas las organizaciones
 * - Procesos batch de sincronización o migración
 * - Endpoints de super-administrador con permisos especiales
 * 
 * Los métodos anotados deben tener validaciones de seguridad adicionales
 * para prevenir fugas de datos cross-tenant.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BypassTenantFilter {
    
    /**
     * Razón por la cual se necesita bypass del filtro tenant.
     * Obligatorio para fines de auditoría y documentación.
     * 
     * @return justificación del bypass
     */
    String reason() default "Operación administrativa global";
}
