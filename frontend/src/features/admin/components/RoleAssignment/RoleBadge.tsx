import React from 'react';
import type { UserRole } from '../../types/user.types';

interface RoleBadgeProps {
    /** Rol a mostrar */
    role: UserRole;
}

/**
 * Badge para mostrar un rol individual
 * Usa estilo de chip azul
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const RoleBadge: React.FC<RoleBadgeProps> = ({ role }) => {
    return (
        <span className="inline-flex items-center px-2 py-1 rounded text-xs font-medium bg-blue-100 text-blue-800 mr-1 mb-1">
            {role.nombre}
        </span>
    );
};
