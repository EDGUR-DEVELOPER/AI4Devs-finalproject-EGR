package com.docflow.documentcore.domain.exception.permiso;

import com.docflow.documentcore.domain.exception.DomainException;

/**
 * Excepción lanzada cuando se intenta crear un permiso duplicado sobre una carpeta.
 */
public class PermisoCarpetaDuplicadoException extends DomainException {

    private static final String ERROR_CODE = "ACL_DUPLICADO";

    public PermisoCarpetaDuplicadoException(Long carpetaId, Long usuarioId) {
        super(
            String.format(
                "Ya existe un permiso para el usuario %d sobre la carpeta %d",
                usuarioId,
                carpetaId
            ),
            ERROR_CODE
        );
    }
}
