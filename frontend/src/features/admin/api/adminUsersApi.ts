import { apiClient } from '@core/shared/api/axiosInstance';
import { ADMIN_ENDPOINTS } from '../constants/endpoints';
import type {
    AdminUser,
    CreateUserRequest,
    CreateUserResponse,
    GetUsersResponse
} from '../types/user.types';

/**
 * Servicio de API para administración de usuarios
 * Centraliza todas las llamadas al backend relacionadas con usuarios
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const adminUsersApi = {
    /**
     * Obtiene la lista de usuarios de la organización
     * @returns Lista de usuarios con sus roles y estado
     */
    getUsers: async (): Promise<AdminUser[]> => {
        const { data } = await apiClient.get<GetUsersResponse>(
            ADMIN_ENDPOINTS.USERS.LIST
        );
        return data.users;
    },

    /**
     * Crea un nuevo usuario en la organización
     * @param userData - Datos del usuario a crear
     * @returns Usuario creado
     */
    createUser: async (userData: CreateUserRequest): Promise<AdminUser> => {
        const { data } = await apiClient.post<CreateUserResponse>(
            ADMIN_ENDPOINTS.USERS.CREATE,
            userData
        );
        return data.user;
    },

    /**
     * Desactiva un usuario existente
     * @param userId - ID del usuario a desactivar
     */
    deactivateUser: async (userId: string): Promise<void> => {
        await apiClient.patch(ADMIN_ENDPOINTS.USERS.DEACTIVATE(userId));
    },

    /**
     * Asigna un rol a un usuario
     * @param userId - ID del usuario
     * @param roleId - ID del rol a asignar
     */
    assignRole: async (userId: string, roleId: number): Promise<void> => {
        await apiClient.post(ADMIN_ENDPOINTS.USERS.ASSIGN_ROLE(userId), { roleId });
    },
};
