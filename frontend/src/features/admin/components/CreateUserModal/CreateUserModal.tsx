import React from 'react';
import { CreateUserForm } from './CreateUserForm';
import type { CreateUserFormData, UserRole } from '../../types/user.types';

interface CreateUserModalProps {
    /** Indica si el modal está abierto */
    isOpen: boolean;
    /** Callback para cerrar el modal */
    onClose: () => void;
    /** Callback al enviar el formulario */
    onSubmit: (data: CreateUserFormData) => void;
    /** Indica si está procesando */
    isLoading: boolean;
    /** Roles disponibles */
    availableRoles: UserRole[];
}

/**
 * Modal contenedor para el formulario de creación de usuario
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const CreateUserModal: React.FC<CreateUserModalProps> = ({
    isOpen,
    onClose,
    onSubmit,
    isLoading,
    availableRoles,
}) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
            {/* Overlay */}
            <div
                className="absolute inset-0 bg-black bg-opacity-50 transition-opacity"
                onClick={onClose}
                aria-hidden="true"
            />

            {/* Contenido */}
            <div
                className="relative bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 p-6 transform transition-all"
                role="dialog"
                aria-modal="true"
                aria-labelledby="create-user-title"
            >
                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <h2
                        id="create-user-title"
                        className="text-xl font-semibold text-gray-900"
                    >
                        Crear Nuevo Usuario
                    </h2>
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isLoading}
                        className="text-gray-400 hover:text-gray-500 focus:outline-none disabled:opacity-50"
                        aria-label="Cerrar modal"
                    >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {/* Formulario */}
                <CreateUserForm
                    onSubmit={onSubmit}
                    isLoading={isLoading}
                    onCancel={onClose}
                    availableRoles={availableRoles}
                />
            </div>
        </div>
    );
};
