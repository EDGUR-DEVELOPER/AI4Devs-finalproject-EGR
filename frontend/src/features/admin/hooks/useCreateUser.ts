import { useState, useCallback } from 'react';
import { adminUsersApi } from '../api/adminUsersApi';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';
import { SUCCESS_MESSAGES, ERROR_MESSAGES, DEFAULT_ERROR_MESSAGE } from '../constants/messages';
import type { CreateUserRequest, CreateUserFormData, AdminUser } from '../types/user.types';

/** Tipo para errores de API con response */
interface ApiError {
    response?: {
        status: number;
    };
}

/** Retorno del hook de creación de usuario */
interface UseCreateUserReturn {
    /** Función para crear un nuevo usuario */
    createUser: (data: CreateUserFormData, onSuccess?: () => void) => Promise<AdminUser | null>;
    /** Indica si está en proceso de creación */
    isCreating: boolean;
}

/**
 * Hook para crear nuevos usuarios
 * Incluye manejo de estado de carga y notificaciones
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const useCreateUser = (): UseCreateUserReturn => {
    const [isCreating, setIsCreating] = useState(false);
    const { showNotification } = useNotificationStore();

    const createUser = useCallback(
        async (data: CreateUserFormData, onSuccess?: () => void): Promise<AdminUser | null> => {
            setIsCreating(true);
            try {
                // Mapear datos del formulario al DTO del backend
                const apiRequest: CreateUserRequest = {
                    email: data.email,
                    nombreCompleto: data.nombre,
                    password: data.password,
                    rolId: data.rolId
                };

                const newUser = await adminUsersApi.createUser(apiRequest);
                showNotification(SUCCESS_MESSAGES.USER_CREATED, 'success');
                // Ejecutar callback de éxito si se proporciona
                if (onSuccess) {
                    onSuccess();
                }
                return newUser;
            } catch (error: unknown) {
                const apiError = error as ApiError;
                const status = apiError.response?.status ?? 500;
                const message = ERROR_MESSAGES[status] ?? DEFAULT_ERROR_MESSAGE;
                showNotification(message, 'error');
                return null;
            } finally {
                setIsCreating(false);
            }
        },
        [showNotification]
    );

    return { createUser, isCreating };
};
