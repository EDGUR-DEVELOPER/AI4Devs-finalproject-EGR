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

/** Retorno del hook de asignación de rol */
interface UseAssignRoleReturn {
    /** Función para asignar un rol a un usuario */
    assignRole: (userId: string, roleId: number) => Promise<boolean>;
    /** Indica si está en proceso de asignación */
    isAssigning: boolean;
}

/**
 * Hook para asignar roles a usuarios
 * Incluye manejo de estado de carga y notificaciones
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const useAssignRole = (): UseAssignRoleReturn => {
    const [isAssigning, setIsAssigning] = useState(false);
    const { showNotification } = useNotificationStore();

    const assignRole = useCallback(
        async (userId: string, roleId: number): Promise<boolean> => {
            setIsAssigning(true);
            try {
                await adminUsersApi.assignRole(userId, roleId);
                showNotification(SUCCESS_MESSAGES.ROLE_ASSIGNED, 'success');
                return true;
            } catch (error: unknown) {
                const apiError = error as ApiError;
                const status = apiError.response?.status ?? 500;
                const message = ERROR_MESSAGES[status] ?? DEFAULT_ERROR_MESSAGE;
                showNotification(message, 'error');
                return false;
            } finally {
                setIsAssigning(false);
            }
        },
        [showNotification]
    );

    return { assignRole, isAssigning };
};
