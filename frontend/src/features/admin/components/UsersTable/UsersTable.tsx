import React from 'react';
import type { AdminUser } from '../../types/user.types';
import type { AvailableRole } from '../../types/role.types';
import { UsersTableRow } from './UsersTableRow';
import { UsersTableSkeleton } from './UsersTableSkeleton';
import { UI_MESSAGES } from '../../constants/messages';

interface UsersTableProps {
    /** Lista de usuarios a mostrar */
    users: AdminUser[];
    /** Roles disponibles para asignar */
    availableRoles: AvailableRole[];
    /** Indica si está cargando */
    isLoading: boolean;
    /** Mensaje de error si existe */
    error: string | null;
    /** Callback para reintentar carga */
    onRetry: () => void;
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
 * Tabla principal de usuarios con estados de carga, vacío y error
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const UsersTable: React.FC<UsersTableProps> = ({
    users,
    availableRoles,
    isLoading,
    error,
    onRetry,
    onDeactivate,
    onActivate,
    onAssignRole,
    isProcessing,
}) => {
    // Estado de carga
    if (isLoading) {
        return <UsersTableSkeleton />;
    }

    // Estado de error
    if (error) {
        return (
            <div className="text-center py-12">
                <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-red-100 mb-4">
                    <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                </div>
                <p className="text-red-600 mb-4">{error}</p>
                <button
                    onClick={onRetry}
                    className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors"
                >
                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    {UI_MESSAGES.RETRY_BUTTON}
                </button>
            </div>
        );
    }

    // Estado vacío
    if (!users || users.length === 0) {
        return (
            <div className="text-center py-12">
                <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-gray-100 mb-4">
                    <svg className="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                    </svg>
                </div>
                <p className="text-gray-500">{UI_MESSAGES.EMPTY_TABLE}</p>
            </div>
        );
    }

    // Tabla con datos
    return (
        <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                    <tr>
                        <th
                            scope="col"
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                        >
                            Email
                        </th>
                        <th
                            scope="col"
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                        >
                            Nombre
                        </th>
                        <th
                            scope="col"
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                        >
                            Estado
                        </th>
                        <th
                            scope="col"
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                        >
                            Roles
                        </th>
                        <th
                            scope="col"
                            className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider"
                        >
                            Acciones
                        </th>
                    </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                    {users.map((user) => (
                        <UsersTableRow
                            key={user.id}
                            user={user}
                            availableRoles={availableRoles}
                            onDeactivate={onDeactivate}
                            onActivate={onActivate}
                            onAssignRole={onAssignRole}
                            isProcessing={isProcessing}
                        />
                    ))}
                </tbody>
            </table>
        </div>
    );
};
