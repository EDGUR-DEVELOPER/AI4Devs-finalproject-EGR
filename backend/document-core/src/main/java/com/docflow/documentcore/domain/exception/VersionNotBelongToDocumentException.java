package com.docflow.documentcore.domain.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando se intenta realizar una operación con una versión
 * que no pertenece al documento especificado.
 * 
 * <p>Esta excepción se usa principalmente en operaciones de rollback donde
 * es necesario validar que la versión objetivo efectivamente pertenece al
 * documento sobre el cual se desea realizar el cambio.</p>
 * 
 * <p><strong>HTTP Status Code:</strong> 400 BAD REQUEST</p>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>
 * if (!version.getDocumentoId().equals(documentoId)) {
 *     throw new VersionNotBelongToDocumentException(versionId, documentoId);
 * }
 * </pre>
 * 
 * @see com.docflow.documentcore.domain.model.Version
 * @see com.docflow.documentcore.domain.model.Documento
 */
@Getter
public class VersionNotBelongToDocumentException extends RuntimeException {
    
    private final Long versionId;
    private final Long documentoId;
    
    /**
     * Constructor para crear la excepción con los IDs de versión y documento.
     * 
     * @param versionId ID de la versión que no pertenece al documento
     * @param documentoId ID del documento al que se esperaba que perteneciera la versión
     */
    public VersionNotBelongToDocumentException(Long versionId, Long documentoId) {
        super(String.format("La versión %d no pertenece al documento %d", versionId, documentoId));
        this.versionId = versionId;
        this.documentoId = documentoId;
    }
}
