import React, { useState } from 'react';
import type { AvailableRole } from '../../types/role.types';
import { UI_MESSAGES } from '../../constants/messages';

interface RoleDropdownProps {
    /** Roles disponibles para asignar */
    availableRoles: AvailableRole[];
    /** IDs de roles ya asignados al usuario */
    assignedRoleIds: number[];
    /** Callback cuando se selecciona un rol */
    onSelect: (roleId: number) => void;
    /** Indica si está procesando una asignación */
    disabled?: boolean;
}

/**
 * Dropdown para asignar nuevos roles a un usuario
 * Filtra roles ya asignados de las opciones
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const RoleDropdown: React.FC<RoleDropdownProps> = ({
    availableRoles,
    assignedRoleIds,
    onSelect,
    disabled = false,
}) => {
    const [isOpen, setIsOpen] = useState(false);

    // Filtrar roles que aún no están asignados
    const unassignedRoles = availableRoles.filter(
        (role) => !assignedRoleIds.includes(role.id)
    );

    // Si no hay roles disponibles para asignar, no mostrar dropdown
    if (unassignedRoles.length === 0) {
        return null;
    }

    const handleSelect = (roleId: number) => {
        onSelect(roleId);
        setIsOpen(false);
    };

    return (
        <div className="relative inline-block mt-1">
            <button
                type="button"
                onClick={() => setIsOpen(!isOpen)}
                disabled={disabled}
                className="inline-flex items-center px-2 py-1 text-xs font-medium text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded disabled:opacity-50 disabled:cursor-not-allowed"
            >
                <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                {UI_MESSAGES.ADD_ROLE}
            </button>

            {/* Menú dropdown */}
            {isOpen && (
                <>
                    {/* Overlay para cerrar al hacer clic fuera */}
                    <div
                        className="fixed inset-0 z-10"
                        onClick={() => setIsOpen(false)}
                    />
                    <div className="absolute left-0 mt-1 w-48 bg-white rounded-md shadow-lg z-20 border border-gray-200">
                        <div className="py-1">
                            {unassignedRoles.map((role) => (
                                <button
                                    key={role.id}
                                    onClick={() => handleSelect(role.id)}
                                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                >
                                    {role.nombre}
                                </button>
                            ))}
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};
