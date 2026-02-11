import axios from 'axios';
import { mapHttpErrorToMessage } from './errorHandler';
import { jwtDecode } from 'jwt-decode';
import type { JwtPayload } from '@core/domain/auth/types';

/**
 * Endpoints públicos que no requieren token de autenticación
 */
const PUBLIC_ENDPOINTS = ['/iam/auth/login', '/iam/auth/register'];

/**
 * Instancia de Axios configurada para comunicación con la API
 * Incluye inyección automática de token y manejo de errores
 */
export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 30000, // Timeout estándar de 30 segundos
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * Interceptor de Request - agrega token de autenticación a endpoints protegidos
 * US-AUTH-006: Valida expiración del token proactivamente antes de cada request
 */
apiClient.interceptors.request.use(
  (config) => {
    // Omitir inyección de token para endpoints públicos
    const isPublicEndpoint = PUBLIC_ENDPOINTS.some((endpoint) =>
      config.url?.includes(endpoint)
    );

    if (!isPublicEndpoint) {
      const token = localStorage.getItem('authToken');
      if (token) {
        // US-AUTH-006: Validación proactiva de expiración
        try {
          const decoded = jwtDecode<JwtPayload>(token);
          const isExpired = decoded.exp * 1000 <= Date.now();
          
          if (isExpired) {
            // Token expirado - disparar logout preventivo
            import('@features/auth/hooks/useAuthStore').then(({ useAuthStore }) => {
              const authStore = useAuthStore.getState();
              authStore.logout('expired');
            });
            // Rechazar request para evitar llamada con token inválido
            return Promise.reject(new Error('Token expired'));
          }
        } catch (error) {
          // Si hay error al decodificar, el token es inválido
          console.error('Error decodificando token:', error);
          import('@features/auth/hooks/useAuthStore').then(({ useAuthStore }) => {
            const authStore = useAuthStore.getState();
            authStore.logout('unauthorized');
          });
          return Promise.reject(new Error('Invalid token'));
        }
        
        config.headers.Authorization = `Bearer ${token}`;
        
        // Inyectar headers de contexto multi-tenant desde el almacenamiento de auth
        // Intentar obtener del store de auth (estado actual)
        const authStorage = localStorage.getItem('auth-storage');
        if (authStorage) {
          try {
            const { state } = JSON.parse(authStorage);
            if (state?.userId) {
              config.headers['X-User-Id'] = state.userId;
            }
            if (state?.organizacionId) {
              config.headers['X-Organization-Id'] = state.organizacionId;
            }
          } catch (parseError) {
            // Ignorar error de parsing - los headers no serán inyectados
            console.warn('Error extrayendo headers de auth storage:', parseError);
          }
        }
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Interceptor de Response - maneja errores y dispara acciones apropiadas
 * - 401: Logout automático con lazy import para evitar dependencias circulares
 * - 404 de recursos: Auditoría + evento resource-not-found para navegación
 * - Otros errores: Muestra notificaciones amigables al usuario
 */
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Manejar 401 no autorizado - disparar logout
    if (error.response?.status === 401) {
      // Lazy import para evitar dependencias circulares
      import('@features/auth/hooks/useAuthStore').then(({ useAuthStore }) => {
        const authStore = useAuthStore.getState();
        authStore.logout('unauthorized');
      });
    } else {
      // Manejar otros errores con notificaciones
      const notification = mapHttpErrorToMessage(error);
      if (notification) {
        // Detectar si es un 404 de recurso específico (aislamiento multi-tenant)
        if (error.response?.status === 404 && notification.isResourceNotFound) {
          // 1. Registrar intento de acceso en auditoría (Post-MVP)
          import('@features/audit/api/auditApi').then(({ logAccessDenied }) => {
            // Extraer organizacionId del token JWT si está disponible
            const authStorage = localStorage.getItem('auth-storage');
            let organizacionId: number | undefined;
            
            try {
              if (authStorage) {
                const { state } = JSON.parse(authStorage);
                organizacionId = state?.organizacionId;
              }
            } catch (parseError) {
              // Ignorar error de parsing
            }
            
            // Registrar evento de auditoría (fallback silencioso)
            logAccessDenied(
              error.config?.url || '',
              error.config?.method || 'GET',
              organizacionId
            );
          });
          
          // 2. Emitir evento personalizado para navegación a página 404
          const event = new CustomEvent('resource-not-found', {
            detail: {
              url: error.config?.url,
              organizacionId: (() => {
                try {
                  const authStorage = localStorage.getItem('auth-storage');
                  if (authStorage) {
                    const { state } = JSON.parse(authStorage);
                    return state?.organizacionId;
                  }
                } catch {
                  return undefined;
                }
              })(),
            },
          });
          window.dispatchEvent(event);
        }
        
        // Lazy import del store de notificaciones para evitar dependencias circulares
        import('@ui/notifications/useNotificationStore').then(
          ({ useNotificationStore }) => {
            const notificationStore = useNotificationStore.getState();
            notificationStore.showNotification(notification.message, notification.type);
          }
        );
      }
    }
    return Promise.reject(error);
  }
);
