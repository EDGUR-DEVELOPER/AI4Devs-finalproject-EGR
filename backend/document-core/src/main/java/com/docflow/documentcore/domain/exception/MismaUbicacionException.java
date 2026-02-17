package com.docflow.documentcore.domain.exception;

/**
 * Excepción lanzada cuando se intenta mover un documento a la misma carpeta en la que ya está.
 * 
 * <p>Esta excepción se utiliza para validar la regla de negocio que impide
 * movimientos redundantes de documentos.</p>
 *
 * @author DocFlow Team
 */
public class MismaUbicacionException extends DomainException {
    
    public MismaUbicacionException(Long documentoId, Long carpetaId) {
        super(
            String.format(
                "El documento %d ya se encuentra en la carpeta %d", 
                documentoId, 
                carpetaId
            ),
            "MISMA_UBICACION"
        );
    }
}
