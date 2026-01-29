package com.docflow.identity.application.services;

import com.docflow.identity.domain.exception.OrganizacionConfigInvalidaException;
import com.docflow.identity.domain.exception.SinOrganizacionException;
import com.docflow.identity.domain.model.UsuarioOrganizacion;
import com.docflow.identity.domain.repository.UsuarioOrganizacionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de dominio para resolver la organización de login según las reglas MVP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizacionResolutionService {

    private final UsuarioOrganizacionRepository repository;

    /**
     * Resuelve la organización de login para un usuario según las reglas de negocio:
     * - 0 organizaciones activas → SinOrganizacionException (403)
     * - 1 organización activa → retorna esa organización
     * - 2+ organizaciones CON predeterminada → retorna la predeterminada
     * - 2+ organizaciones SIN predeterminada → OrganizacionConfigInvalidaException (409)
     *
     * @param usuarioId el ID del usuario
     * @return el ID de la organización resuelta
     * @throws SinOrganizacionException si el usuario no tiene organizaciones activas
     * @throws OrganizacionConfigInvalidaException si la configuración es inválida
     */
    public Integer resolveLoginOrganization(Long usuarioId) {
        var membresias = repository.findMembresiasActivasOrdenadas(usuarioId);

        return switch (membresias.size()) {
            case 0 -> {
                log.warn("Usuario {} no tiene organizaciones activas", usuarioId);
                throw new SinOrganizacionException(
                    "El usuario no tiene organizaciones activas"
                );
            }
            case 1 -> {
                log.info("Usuario {} tiene 1 organización activa: {}", 
                    usuarioId, membresias.getFirst().getOrganizacionId());
                yield membresias.getFirst().getOrganizacionId();
            }
            default -> resolveMultipleOrganizations(membresias, usuarioId);
        };
    }

    /**
     * Resuelve el caso de múltiples organizaciones activas.
     */
    private Integer resolveMultipleOrganizations(
            List<UsuarioOrganizacion> membresias, 
            Long usuarioId) {

        var predeterminadas = membresias.stream()
            .filter(UsuarioOrganizacion::getEsPredeterminada)
            .toList();

        if (predeterminadas.size() == 1) {
            log.info("Usuario {} tiene {} organizaciones activas, usando predeterminada: {}", 
                usuarioId, membresias.size(), predeterminadas.getFirst().getOrganizacionId());
            return predeterminadas.getFirst().getOrganizacionId();
        }

        if (predeterminadas.size() > 1) {
            // No debería ocurrir por índice único, pero defensive programming
            log.error("Usuario {} tiene múltiples organizaciones predeterminadas (violación de integridad)", 
                usuarioId);
            throw new OrganizacionConfigInvalidaException(
                "Configuración de organizaciones inválida: múltiples predeterminadas"
            );
        }

        // Sin predeterminada y >1 organización
        log.warn("Usuario {} tiene {} organizaciones activas sin predeterminada configurada", 
            usuarioId, membresias.size());
        throw new OrganizacionConfigInvalidaException(
            "El usuario tiene múltiples organizaciones sin predeterminada configurada"
        );
    }
}
