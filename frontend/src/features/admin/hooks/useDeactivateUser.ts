import { useState, useCallback } from 'react';
import { adminUsersApi } from '../api/adminUsersApi';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';
import { SUCCESS_MESSAGES, ERROR_MESSAGES, DEFAULT_ERROR_MESSAGE } from '../constants/messages';

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

/** Retorno del hook de desactivación */
interface UseDeactivateUserReturn {
    /** Función para desactivar un usuario */
    deactivateUser: (userId: string) => Promise<boolean>;
    /** Indica si está en proceso de desactivación */
    isDeactivating: boolean;
}

/**
 * Hook para desactivar usuarios
 * Incluye manejo de estado de carga y notificaciones
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const useDeactivateUser = (): UseDeactivateUserReturn => {
    const [isDeactivating, setIsDeactivating] = useState(false);
    const { showNotification } = useNotificationStore();

    const deactivateUser = useCallback(
        async (userId: string): Promise<boolean> => {
            setIsDeactivating(true);
            try {
                console.log(`[useDeactivateUser] Iniciando desactivación del usuario: ${userId}`);
                const response = await adminUsersApi.deactivateUser(userId);
                console.log(`[useDeactivateUser] Respuesta del servidor:`, response);
                showNotification(SUCCESS_MESSAGES.USER_DEACTIVATED, 'success');
                return true;
            } catch (error: unknown) {
                console.error(`[useDeactivateUser] Error al desactivar usuario:`, error);
                const apiError = error as ApiError;
                const status = apiError.response?.status ?? 500;
                const serverMessage = apiError.response?.data?.detail || apiError.message;
                const message = ERROR_MESSAGES[status] ?? serverMessage ?? DEFAULT_ERROR_MESSAGE;
                showNotification(message, 'error');
                return false;
            } finally {
                setIsDeactivating(false);
            }
        },
        [showNotification]
    );

    return { deactivateUser, isDeactivating };
};
