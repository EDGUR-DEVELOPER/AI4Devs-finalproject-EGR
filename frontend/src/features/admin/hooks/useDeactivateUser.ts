import { useState, useCallback } from 'react';
import { adminUsersApi } from '../api/adminUsersApi';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';
import { SUCCESS_MESSAGES, ERROR_MESSAGES, DEFAULT_ERROR_MESSAGE } from '../constants/messages';

/** Tipo para errores de API con response */
interface ApiError {
    response?: {
        status: number;
    };
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
                await adminUsersApi.deactivateUser(userId);
                showNotification(SUCCESS_MESSAGES.USER_DEACTIVATED, 'success');
                return true;
            } catch (error: unknown) {
                const apiError = error as ApiError;
                const status = apiError.response?.status ?? 500;
                const message = ERROR_MESSAGES[status] ?? DEFAULT_ERROR_MESSAGE;
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
