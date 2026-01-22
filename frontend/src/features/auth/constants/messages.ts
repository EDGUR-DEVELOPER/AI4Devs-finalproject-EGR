import type { LogoutReason } from '@core/domain/auth/types';

/**
 * Logout messages mapped by reason
 * Structured to facilitate future i18n implementation
 */
export const LOGOUT_MESSAGES: Record<LogoutReason, string> = {
  manual: 'Sesión cerrada correctamente',
  expired: 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente',
  unauthorized: 'Tu sesión ha sido invalidada. Por favor, inicia sesión nuevamente',
};

/**
 * API endpoints for authentication
 */
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/iam/auth/login',
    SWITCH: '/iam/auth/switch',
  },
} as const;

/**
 * HTTP error messages for user-facing notifications
 * Structured for future i18n implementation
 */
export const HTTP_ERROR_MESSAGES = {
  403: {
    type: 'error' as const,
    message: 'No tienes permisos para acceder a este recurso',
  },
  404: {
    type: 'warning' as const,
    message: 'El recurso solicitado no fue encontrado',
  },
  500: {
    type: 'error' as const,
    message: 'Error interno del servidor. Por favor, intenta nuevamente más tarde',
  },
  502: {
    type: 'error' as const,
    message: 'Error de conexión con el servidor. Por favor, intenta nuevamente',
  },
  503: {
    type: 'error' as const,
    message: 'Servicio temporalmente no disponible. Por favor, intenta más tarde',
  },
  timeout: {
    type: 'error' as const,
    message: 'La petición tardó demasiado tiempo. Por favor, verifica tu conexión',
  },
  network: {
    type: 'error' as const,
    message: 'Error de conexión. Por favor, verifica tu conexión a internet',
  },
  default: {
    type: 'error' as const,
    message: 'Ha ocurrido un error inesperado. Por favor, intenta nuevamente',
  },
} as const;
