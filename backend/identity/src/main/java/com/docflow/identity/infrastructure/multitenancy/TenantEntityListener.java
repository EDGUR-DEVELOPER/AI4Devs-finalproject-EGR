package com.docflow.identity.infrastructure.multitenancy;

import com.docflow.identity.domain.exceptions.TenantContextMissingException;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Listener de entidades JPA que intercepta operaciones de persistencia (@PrePersist)
 * y actualización (@PreUpdate) para inyectar automáticamente el organizacion_id
 * del contexto tenant actual.
 * 
 * Este listener es parte crítica del aislamiento multi-tenant (US-AUTH-004).
 * Garantiza que:
 * 1. NUNCA se guarden entidades sin organizacion_id en rutas protegidas
 * 2. El organizacion_id se sobrescribe con el del token JWT (ignora input del cliente)
 * 3. No es posible accidentalmente guardar datos en la organización incorrecta
 * 
 * Uso en entidades:
 * <pre>
 * {@code
 * @Entity
 * @EntityListeners(TenantEntityListener.class)
 * public class MiEntidad {
 *     @Column(name = "organizacion_id", nullable = false)
 *     private Integer organizacionId;
 *     // ... otros campos
 * }
 * }
 * </pre>
 * 
 * IMPORTANTE: Solo aplica a entidades que tengan el campo 'organizacionId' (Integer).
 * Las entidades globales (como Usuario, Organizacion) NO deben usar este listener.
 */
@Component
@Slf4j
public class TenantEntityListener {

    /**
     * Nombre del campo que se inyectará automáticamente en las entidades.
     */
    private static final String TENANT_FIELD_NAME = "organizacionId";

    /**
     * Hook que se ejecuta ANTES de persistir una nueva entidad.
     * Inyecta el organizacion_id del TenantContextHolder.
     * 
     * @param entity la entidad que está por guardarse
     * @throws TenantContextMissingException si no hay tenant en el contexto
     */
    @PrePersist
    public void setTenantOnCreate(Object entity) {
        setTenantId(entity, "create");
    }

    /**
     * Hook que se ejecuta ANTES de actualizar una entidad existente.
     * Inyecta el organizacion_id del TenantContextHolder (sobrescribe valor actual).
     * 
     * SEGURIDAD: Esto previene que un atacante modifique el organizacion_id de una
     * entidad para moverla a otra organización mediante una petición HTTP manipulada.
     * 
     * @param entity la entidad que está por actualizarse
     * @throws TenantContextMissingException si no hay tenant en el contexto
     */
    @PreUpdate
    public void setTenantOnUpdate(Object entity) {
        setTenantId(entity, "update");
    }

    /**
     * Inyecta el tenant ID en el campo 'organizacionId' de la entidad usando reflection.
     * 
     * @param entity la entidad a modificar
     * @param operation tipo de operación para logging ("create" o "update")
     */
    private void setTenantId(Object entity, String operation) {
        try {
            // 1. Obtener tenant del contexto actual
            var tenantId = TenantContextHolder.getTenantId();
            
            // 2. Buscar el campo 'organizacionId' en la entidad
            Field field = findTenantField(entity.getClass());
            
            if (field != null) {
                field.setAccessible(true);
                var currentValue = (Integer) field.get(entity);
                
                // 3. Inyectar tenant (sobrescribir cualquier valor existente)
                field.set(entity, tenantId);
                
                // 4. Log de auditoría si el valor fue sobrescrito
                if (currentValue != null && !currentValue.equals(tenantId)) {
                    log.warn(
                        "SEGURIDAD: Sobrescrito organizacion_id en {}. " +
                        "Valor del cliente: {}, Valor del token: {}. " +
                        "Operación: {}, Entidad: {}",
                        entity.getClass().getSimpleName(),
                        currentValue,
                        tenantId,
                        operation,
                        entity
                    );
                } else {
                    log.debug(
                        "Tenant inyectado en {}: organizacionId={}. Operación: {}",
                        entity.getClass().getSimpleName(),
                        tenantId,
                        operation
                    );
                }
            } else {
                // La entidad no tiene campo organizacionId - no aplica tenant isolation
                log.trace(
                    "Entidad {} no tiene campo 'organizacionId' - omitiendo tenant injection",
                    entity.getClass().getSimpleName()
                );
            }
            
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                "No se pudo acceder al campo organizacionId en " + 
                entity.getClass().getSimpleName(), 
                e
            );
        }
    }

    /**
     * Busca el campo 'organizacionId' en la jerarquía de clases de la entidad.
     * 
     * @param entityClass la clase de la entidad
     * @return el campo encontrado o null si no existe
     */
    private Field findTenantField(Class<?> entityClass) {
        Class<?> currentClass = entityClass;
        
        // Buscar en la clase y sus superclases
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(TENANT_FIELD_NAME);
            } catch (NoSuchFieldException e) {
                // No está en esta clase, buscar en la superclase
                currentClass = currentClass.getSuperclass();
            }
        }
        
        return null; // Campo no encontrado
    }
}
