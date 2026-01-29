package com.docflow.documentcore.domain.exception.carpeta;

import com.docflow.documentcore.domain.exception.DomainException;

/**
 * Excepción lanzada cuando se intenta crear una carpeta con un nombre duplicado.
 * 
 * <p>Los nombres de carpeta deben ser únicos dentro del mismo nivel jerárquico
 * (mismo carpeta_padre_id) y organización.</p>
 *
 * <p><strong>HTTP Status Code:</strong> 409 CONFLICT</p>
 *
 * @author DocFlow Team
 */
public class CarpetaNombreDuplicadoException extends DomainException {
    
    private static final String ERROR_CODE = "NOMBRE_DUPLICADO";
    
    public CarpetaNombreDuplicadoException(String nombre, Long carpetaPadreId) {
        super(
                String.format(
                        "Ya existe una carpeta con el nombre '%s' en %s",
                        nombre,
                        carpetaPadreId == null ? "la raíz" : "esta ubicación"
                ),
                ERROR_CODE
        );
    }
    
    public CarpetaNombreDuplicadoException(String mensaje) {
        super(mensaje, ERROR_CODE);
    }
}
