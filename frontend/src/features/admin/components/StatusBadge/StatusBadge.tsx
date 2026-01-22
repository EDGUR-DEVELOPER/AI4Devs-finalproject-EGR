import React from 'react';
import type { UserStatus } from '../../types/user.types';

interface StatusBadgeProps {
    /** Estado del usuario */
    status: UserStatus;
}

/**
 * Badge visual para mostrar estado de usuario
 * Verde = Activo, Gris = Inactivo
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
    const isActive = status === 'ACTIVO';

    return (
        <span
            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${isActive
                    ? 'bg-green-100 text-green-800'
                    : 'bg-gray-100 text-gray-800'
                }`}
        >
            {/* Indicador circular de color */}
            <span
                className={`w-2 h-2 mr-1.5 rounded-full ${isActive ? 'bg-green-500' : 'bg-gray-400'
                    }`}
            />
            {isActive ? 'Activo' : 'Inactivo'}
        </span>
    );
};
