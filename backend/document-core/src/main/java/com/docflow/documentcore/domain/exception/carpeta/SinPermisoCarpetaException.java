package com.docflow.documentcore.domain.exception.carpeta;

import com.docflow.documentcore.domain.exception.DomainException;

/**
 * Excepción lanzada cuando un usuario intenta realizar una operación
 * sin los permisos necesarios sobre una carpeta.
 * 
 * <p>Se requiere permiso de ESCRITURA o ADMINISTRACION para crear carpetas.</p>
 *
 * <p><strong>HTTP Status Code:</strong> 403 FORBIDDEN</p>
 *
 * @author DocFlow Team
 */
public class SinPermisoCarpetaException extends DomainException {
    
    private static final String ERROR_CODE = "SIN_PERMISO_CARPETA";
    
    public SinPermisoCarpetaException(Long carpetaPadreId) {
        super(
                String.format(
                        "No tiene permisos suficientes para crear carpetas en la carpeta con ID: %s",
                        carpetaPadreId
                ),
                ERROR_CODE
        );
    }
    
    public SinPermisoCarpetaException(String mensaje) {
        super(mensaje, ERROR_CODE);
    }
}
