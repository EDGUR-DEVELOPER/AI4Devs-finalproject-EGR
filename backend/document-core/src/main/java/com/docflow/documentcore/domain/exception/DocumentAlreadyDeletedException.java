package com.docflow.documentcore.domain.exception;

/**
 * Excepcion lanzada cuando se intenta eliminar un documento ya eliminado.
 */
public class DocumentAlreadyDeletedException extends DomainException {

    private static final String ERROR_CODE = "DOCUMENT_ALREADY_DELETED";

    public DocumentAlreadyDeletedException(Long documentoId) {
        super(String.format("El documento con ID '%s' ya esta eliminado", documentoId), ERROR_CODE);
    }
}
