package com.docflow.documentcore.application.validator;

import com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso;
import com.docflow.documentcore.domain.repository.INivelAccesoRepository;
import org.springframework.stereotype.Component;

/**
 * Validator for NivelAcceso business rules
 * Centralizes validation logic for access levels
 */
@Component
public class NivelAccesoValidator {
    
    private final INivelAccesoRepository repository;

    public NivelAccesoValidator(INivelAccesoRepository repository) {
        this.repository = repository;
    }

    /**
     * Validates that an access level exists by codigo
     * @param codigo Access level code
     * @throws IllegalArgumentException if not found
     */
    public void validateExistsByCodigo(CodigoNivelAcceso codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("Access level codigo cannot be null");
        }
        if (!repository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException(
                    "Access level not found with codigo: " + codigo.getCodigo());
        }
    }

    /**
     * Validates that a codigo value is valid (exists in enum)
     * @param codigoString String representation of codigo
     * @throws IllegalArgumentException if invalid
     */
    public CodigoNivelAcceso validateCodigoFormat(String codigoString) {
        if (codigoString == null || codigoString.trim().isEmpty()) {
            throw new IllegalArgumentException("Codigo cannot be null or empty");
        }
        try {
            return CodigoNivelAcceso.fromCodigo(codigoString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid codigo value: " + codigoString + ". Valid values: LECTURA, ESCRITURA, ADMINISTRACION");
        }
    }
}
