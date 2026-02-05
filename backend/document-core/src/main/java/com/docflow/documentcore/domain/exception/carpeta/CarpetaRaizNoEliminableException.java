package com.docflow.documentcore.domain.exception.carpeta;

import com.docflow.documentcore.domain.exception.DomainException;

/**
 * Excepción lanzada cuando se intenta eliminar una carpeta raíz.
 * 
 * <p>Las carpetas raíz son protegidas y no pueden ser eliminadas, ya que representan
 * el punto de entrada de la jerarquía de carpetas de una organización.</p>
 *
 * <p><strong>HTTP Status Code:</strong> 400 BAD REQUEST</p>
 *
 * @author DocFlow Team
 */
public class CarpetaRaizNoEliminableException extends DomainException {
    
    private static final String ERROR_CODE = "CARPETA_RAIZ_NO_ELIMINABLE";
    
    private final Long carpetaId;
    
    public CarpetaRaizNoEliminableException(Long carpetaId) {
        super(
                String.format(
                        "No se puede eliminar una carpeta raíz (ID: %d). Las carpetas raíz son protegidas.",
                        carpetaId
                ),
                ERROR_CODE
        );
        this.carpetaId = carpetaId;
    }
    
    public Long getCarpetaId() {
        return carpetaId;
    }
    
}
