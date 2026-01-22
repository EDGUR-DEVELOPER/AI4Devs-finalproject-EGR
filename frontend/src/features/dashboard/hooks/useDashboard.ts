import { useMemo } from 'react';
import { useAuth } from '@features/auth/hooks/useAuth';
import { SYSTEM_MODULES, QUICK_ACTIONS, SYSTEM_INFO } from '../constants/modules';
import type { SystemModule, QuickAction } from '../types/dashboard.types';

/**
 * Retorno del hook de dashboard
 */
interface UseDashboardReturn {
    /** Nombre del usuario actual */
    userName: string;
    /** Módulos disponibles para el usuario */
    availableModules: SystemModule[];
    /** Acciones rápidas */
    quickActions: QuickAction[];
    /** Información del sistema */
    systemInfo: typeof SYSTEM_INFO;
    /** Si el usuario tiene rol de admin */
    isAdmin: boolean;
    /** Roles del usuario */
    userRoles: string[];
}

/**
 * Hook principal del dashboard
 * Filtra módulos según roles del usuario y proporciona información contextual
 */
export const useDashboard = (): UseDashboardReturn => {
    const { roles, userId } = useAuth();

    // Determinar si es admin
    const isAdmin = useMemo(() => {
        return roles.includes('ADMIN');
    }, [roles]);

    // Filtrar módulos según roles del usuario
    const availableModules = useMemo(() => {
        return SYSTEM_MODULES.filter((module) => {
            // Si no está habilitado, mostrarlo pero marcarlo como próximamente
            // Si requiere roles, verificar que el usuario tenga al menos uno
            if (module.requiredRoles && module.requiredRoles.length > 0) {
                return module.requiredRoles.some((role) => roles.includes(role));
            }
            return true;
        });
    }, [roles]);

    // Nombre del usuario (simplificado, usar email/nombre real cuando esté disponible)
    const userName = useMemo(() => {
        // Por ahora usar userId, idealmente vendría del perfil
        return userId ? `Usuario` : 'Invitado';
    }, [userId]);

    return {
        userName,
        availableModules,
        quickActions: QUICK_ACTIONS,
        systemInfo: SYSTEM_INFO,
        isAdmin,
        userRoles: roles,
    };
};
