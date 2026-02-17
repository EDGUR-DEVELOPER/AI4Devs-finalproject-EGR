package com.docflow.documentcore.domain.model;

/**
 * Enum representing the type of resource in the system.
 * 
 * <p>Used to distinguish between documents and folders when evaluating permissions,
 * as the precedence rules differ between resource types.</p>
 * 
 * <p>This enum is used in:</p>
 * <ul>
 *   <li>Permission evaluation service method signatures</li>
 *   <li>Security annotations to specify resource types</li>
 *   <li>DTOs for permission queries</li>
 * </ul>
 */
public enum TipoRecurso {
    /**
     * Document resource type.
     * Documents can have explicit ACLs that take precedence over folder permissions.
     */
    DOCUMENTO,
    
    /**
     * Folder resource type.
     * Folders can have direct ACLs and support recursive inheritance.
     */
    CARPETA
}
