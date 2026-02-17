package com.docflow.documentcore.domain.exception;

/**
 * Excepción de dominio lanzada cuando no existe carpeta raíz para una organización.
 */
public class CarpetaRaizNoEncontradaException extends DomainException {

    public CarpetaRaizNoEncontradaException(Long organizacionId) {
        super(
            String.format("No existe carpeta raíz para organización %d", organizacionId),
            "CARPETA_RAIZ_NO_ENCONTRADA"
        );
    }
}
