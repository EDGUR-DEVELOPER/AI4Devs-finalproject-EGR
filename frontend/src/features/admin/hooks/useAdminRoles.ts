import { useState, useEffect } from 'react';
import { adminRolesApi } from '../api/adminRolesApi';
import type { AvailableRole } from '../types/role.types';

/** Retorno del hook de roles */
interface UseAdminRolesReturn {
    /** Lista de roles disponibles para asignar */
    roles: AvailableRole[];
    /** Indica si está cargando los roles */
    isLoading: boolean;
}

/**
 * Hook para obtener roles disponibles
 * Los roles se cargan una vez al montar el componente
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const useAdminRoles = (): UseAdminRolesReturn => {
    const [roles, setRoles] = useState<AvailableRole[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchRoles = async () => {
            try {
                const data = await adminRolesApi.getRoles();
                setRoles(data);
            } catch (error) {
                console.error('Error cargando roles:', error);
                // No mostramos error al usuario, simplemente el dropdown estará vacío
            } finally {
                setIsLoading(false);
            }
        };

        fetchRoles();
    }, []);

    return { roles, isLoading };
};
