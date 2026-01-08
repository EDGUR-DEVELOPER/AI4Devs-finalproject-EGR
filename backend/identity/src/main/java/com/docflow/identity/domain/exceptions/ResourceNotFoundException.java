package com.docflow.identity.domain.exceptions;

/**
 * Excepción lanzada cuando se intenta acceder a un recurso que no existe
 * o que no pertenece a la organización del usuario actual.
 * 
 * SEGURIDAD (Security by Obscurity):
 * Esta excepción se mapea a HTTP 404 en lugar de 403 para no revelar
 * la existencia de recursos de otras organizaciones.
 * 
 * Ejemplo: Usuario de Org A intenta acceder a documento ID=999 de Org B
 * → Retorna 404 "Recurso no encontrado" en lugar de 403 "Sin permisos"
 * 
 * Implementa US-AUTH-004: Aislamiento de datos por organización.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructor con mensaje descriptivo del recurso no encontrado.
     * 
     * @param message descripción del recurso que no se encontró
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa raíz.
     * 
     * @param message descripción del recurso que no se encontró
     * @param cause la excepción que causó este error
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor conveniente para recursos por ID.
     * 
     * @param resourceType tipo de recurso (ej. "Documento", "Carpeta")
     * @param resourceId el ID del recurso
     */
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s con ID '%s' no encontrado", resourceType, resourceId));
    }
}
