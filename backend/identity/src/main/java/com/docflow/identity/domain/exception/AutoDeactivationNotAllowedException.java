package com.docflow.identity.domain.exception;

/**
 * Excepción lanzada cuando un administrador intenta desactivarse a sí mismo.
 * Esto está prohibido para prevenir pérdida de acceso administrativo accidental.
 */
public class AutoDeactivationNotAllowedException extends RuntimeException {
    public AutoDeactivationNotAllowedException(String mensaje) {
        super(mensaje);
    }
}
