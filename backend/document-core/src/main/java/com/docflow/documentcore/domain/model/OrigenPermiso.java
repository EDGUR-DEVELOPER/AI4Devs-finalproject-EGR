package com.docflow.documentcore.domain.model;

/**
 * Enum representing the origin of an effective permission.
 * 
 * <p>Used to track where a permission comes from in the precedence hierarchy:
 * <strong>DOCUMENTO > CARPETA_DIRECTO > CARPETA_HEREDADO</strong></p>
 * 
 * <p>This information is useful for:</p>
 * <ul>
 *   <li>UI display: showing users why they have access to a resource</li>
 *   <li>Debugging: understanding permission resolution paths</li>
 *   <li>Auditing: tracking permission sources</li>
 * </ul>
 */
public enum OrigenPermiso {
    /**
     * Permission is explicitly set on the document itself.
     * This has the highest precedence and always wins over folder permissions.
     */
    DOCUMENTO,
    
    /**
     * Permission comes from a direct ACL on the containing folder.
     * Used when no document-level permission exists.
     */
    CARPETA_DIRECTO,
    
    /**
     * Permission is inherited from an ancestor folder with recursive flag.
     * This has the lowest precedence and is only used when no document
     * or direct folder permission exists.
     */
    CARPETA_HEREDADO
}
