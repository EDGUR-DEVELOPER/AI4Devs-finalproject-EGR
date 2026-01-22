import React from 'react';
import type { AdminUser } from '../../types/user.types';
import type { AvailableRole } from '../../types/role.types';
import { StatusBadge } from '../StatusBadge';
import { RoleBadge, RoleDropdown } from '../RoleAssignment';
import { UI_MESSAGES } from '../../constants/messages';

interface UsersTableRowProps {
    /** Usuario a mostrar */
    user: AdminUser;
    /** Roles disponibles para asignar */
    availableRoles: AvailableRole[];
    /** Callback para desactivar usuario */
    onDeactivate: (userId: string) => void;
    /** Callback para activar usuario */
    onActivate: (userId: string) => void;
    /** Callback para asignar rol */
    onAssignRole: (userId: string, roleId: number) => void;
    /** Indica si hay una operación en progreso */
    isProcessing: boolean;
}

/**
 * Fila individual de la tabla de usuarios
 * Muestra email, nombre, estado, roles y acciones
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const UsersTableRow: React.FC<UsersTableRowProps> = ({
    user,
    availableRoles,
    onDeactivate,
    onActivate,
    onAssignRole,
    isProcessing,
}) => {
    const isActive = user.estado === 'ACTIVO';

    return (
        <tr className="hover:bg-gray-50 transition-colors">
            {/* Columna Email */}
            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                {user.email}
            </td>

            {/* Columna Nombre */}
            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {user.nombreCompleto}
            </td>

            {/* Columna Estado */}
            <td className="px-6 py-4 whitespace-nowrap">
                <StatusBadge status={user.estado} />
            </td>

            {/* Columna Roles */}
            <td className="px-6 py-4">
                <div className="flex flex-wrap">
                    {user.roles.length > 0 ? (
                        user.roles.map((role) => <RoleBadge key={role.id} role={role} />)
                    ) : (
                        <span className="text-sm text-gray-400 italic">{UI_MESSAGES.NO_ROLES}</span>
                    )}
                </div>
                {/* Dropdown para asignar nuevo rol (solo si está activo) */}
                {isActive && (
                    <RoleDropdown
                        availableRoles={availableRoles}
                        assignedRoleIds={user.roles.map((r) => r.id)}
                        onSelect={(roleId) => onAssignRole(user.id, roleId)}
                        disabled={isProcessing}
                    />
                )}
            </td>

            {/* Columna Acciones */}
            <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                {isActive ? (
                    <button
                        onClick={() => onDeactivate(user.id)}
                        disabled={isProcessing}
                        className="text-red-600 hover:text-red-900 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {isProcessing ? 'Procesando...' : 'Desactivar'}
                    </button>
                ) : (
                    <button
                        onClick={() => onActivate(user.id)}
                        disabled={isProcessing}
                        className="text-green-600 hover:text-green-900 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {isProcessing ? 'Procesando...' : 'Activar'}
                    </button>
                )}
            </td>
        </tr>
    );
};
