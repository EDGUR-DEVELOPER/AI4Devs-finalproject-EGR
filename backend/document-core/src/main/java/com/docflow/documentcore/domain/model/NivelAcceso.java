package com.docflow.documentcore.domain.model;

/**
 * Enum que representa los niveles de acceso en el sistema ACL.
 * 
 * Define los tres niveles de permisos para carpetas y documentos.
 */
public enum NivelAcceso {
    /**
     * Permite leer el recurso (ver contenido, descargar).
     */
    LECTURA,
    
    /**
     * Permite leer y modificar el recurso (crear, actualizar, subir versiones).
     */
    ESCRITURA,
    
    /**
     * Permite leer, modificar y administrar permisos del recurso (otorgar/revocar accesos, eliminar).
     */
    ADMINISTRACION
}
