package com.docflow.documentcore.domain.exception;

/**
 * Excepción lanzada cuando se intenta acceder al contexto de organización (tenant)
 * pero no está disponible en el contexto de la petición actual.
 * 
 * Esta situación indica un problema crítico de seguridad ya que todas las operaciones
 * sobre datos sensibles DEBEN estar aisladas por organización.
 * 
 * Implementa US-AUTH-004: Aislamiento de datos por organización.
 */
public class TenantContextMissingException extends RuntimeException {

    /**
     * Constructor con mensaje descriptivo del contexto donde falló.
     * 
     * @param message descripción del contexto de la operación que requería tenant
     */
    public TenantContextMissingException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa raíz.
     * 
     * @param message descripción del contexto de la operación que requería tenant
     * @param cause la excepción que causó este error
     */
    public TenantContextMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}
