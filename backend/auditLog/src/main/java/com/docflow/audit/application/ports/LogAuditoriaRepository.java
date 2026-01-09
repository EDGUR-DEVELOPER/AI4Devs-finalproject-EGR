package com.docflow.audit.application.ports;

import com.docflow.audit.domain.model.LogAuditoria;
import com.docflow.audit.infrastructure.multitenancy.TenantContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * Repositorio reactivo para la entidad LogAuditoria con filtrado automático por tenant.
 * 
 * IMPORTANTE: Todos los métodos de consulta DEBEN incluir el filtro organizacionId
 * para garantizar aislamiento multi-tenant (US-AUTH-004).
 * 
 * Los métodos usan el patrón de obtener el tenant desde TenantContextHolder
 * y aplicarlo en la query de MongoDB.
 */
@Repository
public interface LogAuditoriaRepository extends ReactiveMongoRepository<LogAuditoria, String> {

    /**
     * Busca logs de auditoría por código de evento dentro de la organización actual.
     * 
     * Filtra automáticamente por organizacionId del contexto.
     * 
     * @param codigoEvento el código del evento a buscar
     * @param organizacionId el ID de la organización del contexto
     * @param pageable configuración de paginación
     * @return Flux con los logs encontrados
     */
    @Query("{ 'organizacionId': ?1, 'codigoEvento': ?0 }")
    Flux<LogAuditoria> findByCodigoEventoAndOrganizacionId(
        String codigoEvento, 
        Integer organizacionId, 
        Pageable pageable
    );

    /**
     * Busca logs de auditoría por usuario dentro de la organización actual.
     * 
     * @param usuarioId el ID del usuario
     * @param organizacionId el ID de la organización del contexto
     * @param pageable configuración de paginación
     * @return Flux con los logs encontrados
     */
    @Query("{ 'organizacionId': ?1, 'usuarioId': ?0 }")
    Flux<LogAuditoria> findByUsuarioIdAndOrganizacionId(
        Long usuarioId, 
        Integer organizacionId, 
        Pageable pageable
    );

    /**
     * Busca logs de auditoría en un rango de fechas dentro de la organización actual.
     * 
     * @param desde fecha de inicio (inclusive)
     * @param hasta fecha de fin (inclusive)
     * @param organizacionId el ID de la organización del contexto
     * @param pageable configuración de paginación
     * @return Flux con los logs encontrados
     */
    @Query("{ 'organizacionId': ?2, 'fechaEvento': { $gte: ?0, $lte: ?1 } }")
    Flux<LogAuditoria> findByFechaEventoBetweenAndOrganizacionId(
        OffsetDateTime desde, 
        OffsetDateTime hasta, 
        Integer organizacionId, 
        Pageable pageable
    );

    /**
     * Busca todos los logs de la organización actual (con paginación).
     * 
     * @param organizacionId el ID de la organización del contexto
     * @param pageable configuración de paginación
     * @return Flux con los logs encontrados
     */
    @Query("{ 'organizacionId': ?0 }")
    Flux<LogAuditoria> findAllByOrganizacionId(Integer organizacionId, Pageable pageable);

    /**
     * Cuenta los logs de auditoría por código de evento en la organización actual.
     * 
     * @param codigoEvento el código del evento
     * @param organizacionId el ID de la organización del contexto
     * @return Mono con el conteo
     */
    @Query(value = "{ 'organizacionId': ?1, 'codigoEvento': ?0 }", count = true)
    Mono<Long> countByCodigoEventoAndOrganizacionId(String codigoEvento, Integer organizacionId);

    /**
     * Método de utilidad para buscar logs con filtrado automático por tenant del contexto.
     * 
     * Wrapper que obtiene el tenant del TenantContextHolder y delega a findAllByOrganizacionId.
     * 
     * @param pageable configuración de paginación
     * @return Flux con los logs de la organización actual
     */
    default Flux<LogAuditoria> findAllFiltered(Pageable pageable) {
        return TenantContextHolder.getTenantId()
            .flatMapMany(tenantId -> findAllByOrganizacionId(tenantId, pageable));
    }

    /**
     * Método de utilidad para buscar por código de evento con filtrado automático.
     * 
     * @param codigoEvento el código del evento
     * @param pageable configuración de paginación
     * @return Flux con los logs encontrados
     */
    default Flux<LogAuditoria> findByCodigoEventoFiltered(String codigoEvento, Pageable pageable) {
        return TenantContextHolder.getTenantId()
            .flatMapMany(tenantId -> 
                findByCodigoEventoAndOrganizacionId(codigoEvento, tenantId, pageable)
            );
    }

    /**
     * Método de utilidad para buscar por usuario con filtrado automático.
     * 
     * @param usuarioId el ID del usuario
     * @param pageable configuración de paginación
     * @return Flux con los logs encontrados
     */
    default Flux<LogAuditoria> findByUsuarioIdFiltered(Long usuarioId, Pageable pageable) {
        return TenantContextHolder.getTenantId()
            .flatMapMany(tenantId -> 
                findByUsuarioIdAndOrganizacionId(usuarioId, tenantId, pageable)
            );
    }
}
