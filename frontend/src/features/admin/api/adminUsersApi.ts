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
     * @param page - Número de página (base 1, default 1)
     * @param limit - Elementos por página (default 20, max 100)
     * @param estado - Filtro opcional por estado (ACTIVO, SUSPENDIDO, etc.)
     * @param busqueda - Filtro opcional de búsqueda en email o nombre
     * @returns Lista de usuarios con paginación
     */
    getUsers: async (page: number = 1, limit: number = 20, estado?: string, busqueda?: string): Promise<AdminUser[]> => {
        const params = new URLSearchParams();
        params.append('page', page.toString());
        params.append('limit', limit.toString());
        if (estado) params.append('estado', estado);
        if (busqueda) params.append('busqueda', busqueda);

        const { data } = await apiClient.get<GetUsersResponse>(
            `${ADMIN_ENDPOINTS.USERS.LIST}?${params.toString()}`
        );
        return data?.usuarios || [];
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
     * Activa un usuario existente (desactivado o suspendido)
     * @param userId - ID del usuario a activar
     */
    activateUser: async (userId: string): Promise<void> => {
        await apiClient.patch(ADMIN_ENDPOINTS.USERS.ACTIVATE(userId));
    },

    /**
     * Asigna un rol a un usuario
     * @param userId - ID del usuario
     * @param roleId - ID del rol a asignar
     */
    assignRole: async (userId: string, roleId: number): Promise<void> => {
        await apiClient.post(ADMIN_ENDPOINTS.USERS.ASSIGN_ROLE(userId), { rolId: roleId });
    },
};
