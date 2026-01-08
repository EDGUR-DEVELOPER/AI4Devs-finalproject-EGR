/**
 * API de auditoría para registro de eventos de seguridad
 * Preparado para integración Post-MVP con backend
 */

import axios from 'axios';
import type { AxiosInstance } from 'axios';
import type {
  AuditEventRequest,
  AuditEventResponse,
} from '../types';
import { AuditEventCode } from '../types';
import {
  AUDIT_ENDPOINTS,
  AUDIT_TIMEOUT_MS,
  AUDIT_ENABLED,
  AUDIT_MESSAGES,
} from '../constants/endpoints';

/**
 * Instancia de Axios dedicada para auditoría
 * Configuración independiente con timeout reducido (3s)
 */
const auditClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: AUDIT_TIMEOUT_MS,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Interceptor de request: Inyecta token JWT si está disponible
 */
auditClient.interceptors.request.use((config) => {
  // Obtener token del localStorage
  const authStorage = localStorage.getItem('auth-storage');
  if (authStorage) {
    try {
      const { state } = JSON.parse(authStorage);
      if (state?.token) {
        config.headers.Authorization = `Bearer ${state.token}`;
      }
    } catch (error) {
      // Ignorar error de parsing, continuar sin token
    }
  }
  return config;
});

/**
 * Registra un evento de intento de acceso denegado (404 de recurso)
 * 
 * @param url - URL del recurso al que se intentó acceder
 * @param method - Método HTTP usado
 * @param organizacionId - ID de la organización del usuario (para contexto)
 * 
 * @returns Promise que resuelve silenciosamente (no lanza excepciones)
 * 
 * Comportamiento:
 * - Si VITE_AUDIT_ENABLED=false: Log de debug y retorna
 * - Si endpoint no existe (404/503): console.warn y retorna
 * - Si hay error de red/timeout: console.warn y retorna
 * - No interrumpe el flujo principal de la aplicación
 */
export async function logAccessDenied(
  url: string,
  method: string,
  _organizacionId?: number // Parámetro para contexto, extraído del token en backend
): Promise<void> {
  // Verificar si la auditoría está habilitada
  if (!AUDIT_ENABLED) {
    console.debug(AUDIT_MESSAGES.DISABLED);
    return;
  }

  try {
    const eventRequest: AuditEventRequest = {
      codigoEvento: AuditEventCode.ACCESS_DENIED_404,
      detallesCambio: {
        recurso: url,
        metodo: method.toUpperCase(),
        statusCode: 404,
        mensaje: 'Intento de acceso a recurso de otra organización o inexistente',
      },
      metadatos: {
        userAgent: navigator.userAgent,
        timestamp: new Date().toISOString(),
      },
    };

    // Intentar registrar el evento
    const response = await auditClient.post<AuditEventResponse>(
      AUDIT_ENDPOINTS.EVENTS,
      eventRequest
    );

    console.debug(AUDIT_MESSAGES.LOG_SUCCESS, response.data);
  } catch (error) {
    // Fallback silencioso: No interrumpir UX por fallo de auditoría
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      
      // Endpoint no existe aún (Post-MVP)
      if (status === 404 || status === 503) {
        console.warn(AUDIT_MESSAGES.ENDPOINT_NOT_AVAILABLE);
        return;
      }
      
      // Timeout o error de red
      if (error.code === 'ECONNABORTED' || !error.response) {
        console.warn(AUDIT_MESSAGES.LOG_ERROR, 'Timeout o error de red');
        return;
      }
      
      // Otro error HTTP
      console.warn(
        AUDIT_MESSAGES.LOG_ERROR,
        `HTTP ${status}: ${error.response?.data?.detail || error.message}`
      );
    } else {
      // Error inesperado
      console.warn(AUDIT_MESSAGES.LOG_ERROR, error);
    }
  }
}

/**
 * Exportación del cliente para posibles extensiones futuras
 */
export { auditClient };
