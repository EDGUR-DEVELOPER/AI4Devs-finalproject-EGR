import { useState, useCallback } from 'react';
import { adminUsersApi } from '../api/adminUsersApi';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';
import { ERROR_MESSAGES, DEFAULT_ERROR_MESSAGE } from '../constants/messages';

/** Tipo para errores de API con response */
interface ApiError {
    response?: {
        status: number;
        data?: {
            detail?: string;
        };
    };
    message?: string;
}

/** Retorno del hook de activación */
interface UseActivateUserReturn {
    /** Función para activar un usuario */
    activateUser: (userId: string) => Promise<boolean>;
    /** Indica si está en proceso de activación */
    isActivating: boolean;
}

/**
 * Hook para activar usuarios
 * Incluye manejo de estado de carga y notificaciones
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const useActivateUser = (): UseActivateUserReturn => {
    const [isActivating, setIsActivating] = useState(false);
    const { showNotification } = useNotificationStore();

    const activateUser = useCallback(
        async (userId: string): Promise<boolean> => {
            setIsActivating(true);
            try {
                console.log(`[useActivateUser] Iniciando activación del usuario: ${userId}`);
                const response = await adminUsersApi.activateUser(userId);
                console.log(`[useActivateUser] Respuesta del servidor:`, response);
                showNotification('Usuario activado exitosamente', 'success');
                return true;
            } catch (error: unknown) {
                console.error(`[useActivateUser] Error al activar usuario:`, error);
                const apiError = error as ApiError;
                const status = apiError.response?.status ?? 500;
                const serverMessage = apiError.response?.data?.detail || apiError.message;
                const message = ERROR_MESSAGES[status] ?? serverMessage ?? DEFAULT_ERROR_MESSAGE;
                showNotification(message, 'error');
                return false;
            } finally {
                setIsActivating(false);
            }
        },
        [showNotification]
    );

    return { activateUser, isActivating };
};
