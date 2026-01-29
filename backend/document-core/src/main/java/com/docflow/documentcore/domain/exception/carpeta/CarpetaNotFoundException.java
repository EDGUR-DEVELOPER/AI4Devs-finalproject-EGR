package com.docflow.documentcore.domain.exception.carpeta;

import com.docflow.documentcore.domain.exception.DomainException;

/**
 * Excepción lanzada cuando no se encuentra una carpeta solicitada.
 * 
 * <p>Se produce cuando:
 * <ul>
 *   <li>Se busca una carpeta por ID que no existe</li>
 *   <li>Se intenta acceder a una carpeta de otra organización</li>
 *   <li>La carpeta fue eliminada lógicamente</li>
 * </ul>
 * </p>
 *
 * <p><strong>HTTP Status Code:</strong> 404 NOT FOUND</p>
 *
 * @author DocFlow Team
 */
public class CarpetaNotFoundException extends DomainException {
    
    private static final String ERROR_CODE = "CARPETA_NO_ENCONTRADA";
    
    public CarpetaNotFoundException(Long id) {
        super(
                String.format("No se encontró la carpeta con ID: %s", id),
                ERROR_CODE
        );
    }
    
    public CarpetaNotFoundException(String mensaje) {
        super(mensaje, ERROR_CODE);
    }
}
