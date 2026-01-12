import { apiClient } from '@core/shared/api/axiosInstance';
import { ADMIN_ENDPOINTS } from '../constants/endpoints';
import type { AvailableRole, GetRolesResponse } from '../types/role.types';

/**
 * Servicio de API para roles disponibles
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const adminRolesApi = {
    /**
     * Obtiene la lista de roles disponibles para asignar
     * Incluye roles globales y específicos de la organización
     * @returns Lista de roles disponibles
     */
    getRoles: async (): Promise<AvailableRole[]> => {
        const { data } = await apiClient.get<GetRolesResponse>(
            ADMIN_ENDPOINTS.ROLES.LIST
        );
        return data.roles;
    },
};
