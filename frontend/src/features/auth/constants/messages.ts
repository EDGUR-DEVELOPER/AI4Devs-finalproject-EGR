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
