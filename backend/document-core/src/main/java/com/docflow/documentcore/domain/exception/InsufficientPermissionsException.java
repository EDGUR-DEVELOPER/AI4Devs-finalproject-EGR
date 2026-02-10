package com.docflow.documentcore.domain.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando un usuario no posee el nivel de permiso requerido
 * para realizar una operación sobre un recurso.
 * 
 * <p>Esta excepción se diferencia de {@link AccessDeniedException} en que
 * proporciona información más específica sobre el nivel de permiso requerido
 * y el tipo de recurso involucrado.</p>
 * 
 * <p><strong>HTTP Status Code:</strong> 403 FORBIDDEN</p>
 * 
 * <p><strong>Ejemplo de uso:</strong></p>
 * <pre>
 * if (!evaluadorPermisos.tieneAcceso(usuarioId, documentoId, TipoRecurso.DOCUMENTO, 
 *                                     NivelAcceso.ADMINISTRACION, organizacionId)) {
 *     throw new InsufficientPermissionsException("ADMINISTRACION", "DOCUMENTO");
 * }
 * </pre>
 * 
 * @see AccessDeniedException
 * @see com.docflow.documentcore.domain.service.IEvaluadorPermisos
 */
@Getter
public class InsufficientPermissionsException extends RuntimeException {
    
    private final String nivelRequerido;
    private final String tipoRecurso;
    
    /**
     * Constructor para crear la excepción con el nivel de permiso requerido y tipo de recurso.
     * 
     * @param nivelRequerido Nivel de acceso requerido (ej. "LECTURA", "ESCRITURA", "ADMINISTRACION")
     * @param tipoRecurso Tipo de recurso sobre el cual se requiere el permiso (ej. "DOCUMENTO", "CARPETA")
     */
    public InsufficientPermissionsException(String nivelRequerido, String tipoRecurso) {
        super(String.format("Se requiere permiso de %s sobre el recurso de tipo %s", 
                          nivelRequerido, tipoRecurso));
        this.nivelRequerido = nivelRequerido;
        this.tipoRecurso = tipoRecurso;
    }
    
    /**
     * Constructor sobrecargado con mensaje personalizado.
     * 
     * @param mensaje Mensaje personalizado de error
     */
    public InsufficientPermissionsException(String mensaje) {
        super(mensaje);
        this.nivelRequerido = null;
        this.tipoRecurso = null;
    }
}
