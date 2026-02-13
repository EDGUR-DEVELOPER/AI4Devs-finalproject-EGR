package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import com.docflow.documentcore.domain.repository.INivelAccesoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application Service for NivelAcceso
 * Orchestrates business logic for access level operations
 */
@Service
@Transactional(readOnly = true)
public class NivelAccesoService {
    
    private static final Logger log = LoggerFactory.getLogger(NivelAccesoService.class);
    
    private final INivelAccesoRepository repository;

    public NivelAccesoService(INivelAccesoRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieve an access level by codigo
     * @param codigo Access level code enum
     * @return NivelAcceso domain object
     * @throws IllegalArgumentException if not found
     */
    public NivelAcceso getByCodigo(CodigoNivelAcceso codigo) {
        log.debug("Fetching access level by codigo: {}", codigo);
        return repository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Access level not found with codigo: " + codigo.getCodigo()));
    }

    /**
     * List all active access levels ordered by 'orden'
     * @return List of active access levels
     */
    public List<NivelAcceso> listAllActive() {
        log.debug("Listing all active access levels");
        return repository.findAllActiveOrderByOrden();
    }
}
