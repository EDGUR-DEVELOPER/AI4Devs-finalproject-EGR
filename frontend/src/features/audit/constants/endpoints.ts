/**
 * Constantes y configuración del módulo de auditoría
 */

/**
 * Configuración de endpoints de auditoría
 */
export const AUDIT_ENDPOINTS = {
  BASE: '/api/audit',
  EVENTS: '/api/audit/events',          // POST: Registrar evento de auditoría
  QUERY: '/api/audit/query',            // GET: Consultar auditoría (Admin)
  EVENT_CODES: '/api/audit/event-codes', // GET: Catálogo de códigos
} as const;

/**
 * Configuración de timeout específico para llamadas de auditoría
 * Timeout más corto (3s) para no bloquear el flujo principal
 */
export const AUDIT_TIMEOUT_MS = 3000;

/**
 * Feature flag: Habilitar/deshabilitar auditoría
 * Se configura en .env.development / .env.production
 */
export const AUDIT_ENABLED = import.meta.env.VITE_AUDIT_ENABLED === 'true';

/**
 * Mensajes de log para debugging
 */
export const AUDIT_MESSAGES = {
  DISABLED: '[Audit] Feature deshabilitada (VITE_AUDIT_ENABLED=false)',
  ENDPOINT_NOT_AVAILABLE: '[Audit] Endpoint no disponible (Post-MVP)',
  LOG_SUCCESS: '[Audit] Evento registrado exitosamente',
  LOG_ERROR: '[Audit] Error al registrar evento (fallback silencioso)',
} as const;
