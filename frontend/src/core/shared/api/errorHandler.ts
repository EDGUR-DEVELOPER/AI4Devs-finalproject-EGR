import type { AxiosError } from 'axios';
import { HTTP_ERROR_MESSAGES } from '@features/auth/constants/messages';

/**
 * Estructura de notificación de error retornada por el handler
 */
export interface ErrorNotification {
  type: 'error' | 'warning' | 'info';
  message: string;
  isResourceNotFound?: boolean; // Flag para diferenciar 404 de recursos vs páginas
}

/**
 * Valida si una URL corresponde a un endpoint de recurso específico
 * Estos endpoints representan recursos individuales que pueden estar sujetos a
 * aislamiento multi-tenant (organizacion_id)
 * 
 * @param url - URL de la petición HTTP
 * @returns true si es un recurso específico, false en caso contrario
 * 
 * @example
 * isResourceEndpoint('/api/documentos/123') // true
 * isResourceEndpoint('/api/carpetas/456') // true
 * isResourceEndpoint('/api/usuarios/789') // true
 * isResourceEndpoint('/api/documentos') // false (lista)
 * isResourceEndpoint('/login') // false (página)
 */
function isResourceEndpoint(url: string): boolean {
  // Patrón regex para detectar endpoints de recursos específicos
  // Formato: /[cualquier-cosa]/(carpetas|documentos|usuarios)/[número]
  const resourcePattern = /\/(?:carpetas|documentos|usuarios)\/\d+(?:\/|$)/;
  return resourcePattern.test(url);
}

/**
 * Mapea errores HTTP a mensajes de notificación amigables para el usuario
 * 
 * @param error - Objeto de error de Axios proveniente del interceptor
 * @returns Objeto de notificación con tipo y mensaje, o null si no se necesita notificación
 * 
 * @example
 * ```typescript
 * const notification = mapHttpErrorToMessage(axiosError);
 * if (notification) {
 *   useNotificationStore.getState().addNotification(notification);
 * }
 * ```
 */
export function mapHttpErrorToMessage(
  error: AxiosError
): ErrorNotification | null {
  // Errores de red (sin respuesta del servidor)
  if (!error.response) {
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      return HTTP_ERROR_MESSAGES.timeout;
    }
    return HTTP_ERROR_MESSAGES.network;
  }

  const status = error.response.status;

  // 401 se maneja separadamente por la lógica de logout - no se necesita notificación aquí
  if (status === 401) {
    return null;
  }

  // Mapear códigos de estado conocidos a mensajes
  switch (status) {
    case 403:
      return HTTP_ERROR_MESSAGES[403];
    case 404: {
      // Detectar si el 404 es de un recurso específico o una página
      const url = error.config?.url || '';
      const isResource = isResourceEndpoint(url);
      
      return {
        ...HTTP_ERROR_MESSAGES[404],
        isResourceNotFound: isResource, // Metadata para el interceptor
      };
    }
    case 500:
      return HTTP_ERROR_MESSAGES[500];
    case 502:
      return HTTP_ERROR_MESSAGES[502];
    case 503:
      return HTTP_ERROR_MESSAGES[503];
    default:
      // Para otros errores 5xx, usar mensaje de 500
      if (status >= 500) {
        return HTTP_ERROR_MESSAGES[500];
      }
      // Para otros errores 4xx, usar mensaje por defecto
      if (status >= 400) {
        return HTTP_ERROR_MESSAGES.default;
      }
      return null;
  }
}
