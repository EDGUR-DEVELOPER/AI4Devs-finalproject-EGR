package com.docflow.documentcore.domain.exception.carpeta;

import com.docflow.documentcore.domain.exception.DomainException;

/**
 * Excepción lanzada cuando se intenta eliminar una carpeta que no está vacía.
 * 
 * <p>Se produce cuando:
 * <ul>
 *   <li>La carpeta contiene subcarpetas activas</li>
 *   <li>La carpeta contiene documentos activos</li>
 *   <li>Se intenta eliminar una carpeta sin vaciar su contenido primero</li>
 * </ul>
 * </p>
 *
 * <p><strong>HTTP Status Code:</strong> 409 CONFLICT</p>
 *
 * @author DocFlow Team
 */
public class CarpetaNoVaciaException extends DomainException {
    
    private static final String ERROR_CODE = "CARPETA_NO_VACIA";
    
    private final Long carpetaId;
    private final int subcarpetasActivas;
    private final int documentosActivos;
    
    public CarpetaNoVaciaException(Long carpetaId, int subcarpetasActivas, int documentosActivos) {
        super(
                String.format(
                        "La carpeta con ID %d no está vacía: %d subcarpetas activas, %d documentos activos. " +
                        "Debe vaciarse antes de eliminarla.",
                        carpetaId, subcarpetasActivas, documentosActivos
                ),
                ERROR_CODE
        );
        this.carpetaId = carpetaId;
        this.subcarpetasActivas = subcarpetasActivas;
        this.documentosActivos = documentosActivos;
    }
    
    public Long getCarpetaId() {
        return carpetaId;
    }
    
    public int getSubcarpetasActivas() {
        return subcarpetasActivas;
    }
    
    public int getDocumentosActivos() {
        return documentosActivos;
    }
    
}
