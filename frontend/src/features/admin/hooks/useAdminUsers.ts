import { useState, useEffect, useCallback } from 'react';
import { adminUsersApi } from '../api/adminUsersApi';
import { UI_MESSAGES } from '../constants/messages';
import type { AdminUser } from '../types/user.types';

/** Estado del hook de usuarios */
interface UseAdminUsersState {
    users: AdminUser[];
    isLoading: boolean;
    error: string | null;
}

/** Retorno del hook de usuarios */
interface UseAdminUsersReturn extends UseAdminUsersState {
    /** Recarga la lista de usuarios */
    refetch: () => Promise<void>;
    /** Actualiza un usuario en la lista local (UI optimista) */
    updateUserLocally: (userId: string, updates: Partial<AdminUser>) => void;
}

/**
 * Hook para gestionar la lista de usuarios administrados
 * Maneja estados de carga, error y datos
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const useAdminUsers = (): UseAdminUsersReturn => {
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Función para cargar usuarios desde la API
    const fetchUsers = useCallback(async (page: number = 1, limit: number = 20, estado?: string, busqueda?: string) => {
        setIsLoading(true);
        setError(null);
        try {
            const data = await adminUsersApi.getUsers(page, limit, estado, busqueda);
            setUsers(data);
        } catch (err) {
            console.error('Error cargando usuarios:', err);
            setError(UI_MESSAGES.LOAD_ERROR);
        } finally {
            setIsLoading(false);
        }
    }, []);

    // Actualización local de usuario para UI optimista
    const updateUserLocally = useCallback((userId: string, updates: Partial<AdminUser>) => {
        setUsers((prev) =>
            prev.map((user) => (user.id === userId ? { ...user, ...updates } : user))
        );
    }, []);

    // Carga inicial al montar el componente
    useEffect(() => {
        fetchUsers();
    }, [fetchUsers]);

    return {
        users,
        isLoading,
        error,
        refetch: fetchUsers,
        updateUserLocally,
    };
};
