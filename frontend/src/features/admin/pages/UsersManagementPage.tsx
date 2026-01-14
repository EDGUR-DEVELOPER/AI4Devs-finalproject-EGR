import React, { useState } from 'react';
import { useAdminUsers } from '../hooks/useAdminUsers';
import { useAdminRoles } from '../hooks/useAdminRoles';
import { useCreateUser } from '../hooks/useCreateUser';
import { useDeactivateUser } from '../hooks/useDeactivateUser';
import { useActivateUser } from '../hooks/useActivateUser';
import { useAssignRole } from '../hooks/useAssignRole';
import { UsersTable } from '../components/UsersTable';
import { CreateUserModal } from '../components/CreateUserModal';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { UI_MESSAGES, FORM_LABELS } from '../constants/messages';
import type { CreateUserFormData } from '../types/user.types';

/** Estado para modal de desactivación */
interface UserToDeactivate {
    id: string;
    nombreCompleto: string;
}

/**
 * Página principal de gestión de usuarios
 * Orquesta todos los componentes y hooks del módulo de administración
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const UsersManagementPage: React.FC = () => {
    // Estado de modales
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [userToDeactivate, setUserToDeactivate] = useState<UserToDeactivate | null>(null);

    // Hooks de datos y operaciones
    const { users, isLoading, error, refetch, updateUserLocally } = useAdminUsers();
    const { roles } = useAdminRoles();
    const { createUser, isCreating } = useCreateUser();
    const { deactivateUser, isDeactivating } = useDeactivateUser();
    const { activateUser, isActivating } = useActivateUser();
    const { assignRole, isAssigning } = useAssignRole();

    // Handler: Crear usuario
    const handleCreateUser = async (data: CreateUserFormData) => {
        await createUser(data, () => {
            // Cerrar modal automáticamente cuando la petición finaliza exitosamente
            setIsCreateModalOpen(false);
            refetch(); // Recargar lista de usuarios
        });
    };

    // Handler: Iniciar desactivación (abrir modal de confirmación)
    const handleInitiateDeactivate = (userId: string) => {
        const user = users.find((u) => u.id === userId);
        if (user) {
            setUserToDeactivate({ id: userId, nombreCompleto: user.nombreCompleto });
        }
    };

    // Handler: Confirmar desactivación
    const handleConfirmDeactivate = async () => {
        if (!userToDeactivate) return;

        const success = await deactivateUser(userToDeactivate.id);
        if (success) {
            // Actualizar UI de forma optimista
            updateUserLocally(userToDeactivate.id, { estado: 'INACTIVO' });
            setUserToDeactivate(null);
            // Recargar después de un pequeño delay para asegurar que el servidor procesó
            setTimeout(() => {
                refetch();
            }, 500);
        }
    };

    // Handler: Activar usuario
    const handleActivateUser = async (userId: string) => {
        const success = await activateUser(userId);
        if (success) {
            // Actualizar UI de forma optimista
            updateUserLocally(userId, { estado: 'ACTIVO' });
            // Recargar después de un pequeño delay
            setTimeout(() => {
                refetch();
            }, 500);
        }
    };

    // Handler: Asignar rol
    const handleAssignRole = async (userId: string, roleId: number) => {
        const success = await assignRole(userId, roleId);
        if (success) {
            refetch(); // Recargar para obtener roles actualizados
        }
    };

    // Indicador de operación en progreso
    const isProcessing = isDeactivating || isAssigning || isActivating;

    return (
        <div className="min-h-screen bg-gray-50">
            {/* Header de página */}
            <div className="bg-white shadow">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                        <div>
                            <h1 className="text-2xl font-bold text-gray-900">
                                Gestión de Usuarios
                            </h1>
                            <p className="mt-1 text-sm text-gray-500">
                                Administra los usuarios de tu organización
                            </p>
                        </div>
                        <button
                            onClick={() => setIsCreateModalOpen(true)}
                            className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition-colors"
                        >
                            <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                            </svg>
                            {FORM_LABELS.CREATE_USER}
                        </button>
                    </div>
                </div>
            </div>

            {/* Contenido principal */}
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="bg-white rounded-lg shadow overflow-hidden">
                    <UsersTable
                        users={users}
                        availableRoles={roles}
                        isLoading={isLoading}
                        error={error}
                        onRetry={refetch}
                        onDeactivate={handleInitiateDeactivate}
                        onActivate={handleActivateUser}
                        onAssignRole={handleAssignRole}
                        isProcessing={isProcessing}
                    />
                </div>
            </main>

            {/* Modal de creación de usuario */}
            <CreateUserModal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                onSubmit={handleCreateUser}
                isLoading={isCreating}
                availableRoles={roles}
            />

            {/* Modal de confirmación de desactivación */}
            <ConfirmationModal
                isOpen={!!userToDeactivate}
                title="Desactivar Usuario"
                message={UI_MESSAGES.CONFIRM_DEACTIVATE(userToDeactivate?.nombreCompleto ?? '')}
                confirmLabel={FORM_LABELS.DEACTIVATE}
                onConfirm={handleConfirmDeactivate}
                onCancel={() => setUserToDeactivate(null)}
                isLoading={isDeactivating}
                variant="danger"
            />
        </div>
    );
};
