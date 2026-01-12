import React from 'react';
import { FORM_LABELS } from '../../constants/messages';

interface ConfirmationModalProps {
    /** Indica si el modal está abierto */
    isOpen: boolean;
    /** Título del modal */
    title: string;
    /** Mensaje de confirmación */
    message: string;
    /** Texto del botón de confirmar */
    confirmLabel?: string;
    /** Texto del botón de cancelar */
    cancelLabel?: string;
    /** Callback al confirmar */
    onConfirm: () => void;
    /** Callback al cancelar */
    onCancel: () => void;
    /** Indica si está procesando */
    isLoading?: boolean;
    /** Variante de estilo del botón de confirmar */
    variant?: 'danger' | 'primary';
}

/**
 * Modal genérico de confirmación reutilizable
 * Usado para confirmar acciones como desactivar usuarios
 * US-ADMIN-005: UI mínima de gestión de usuarios
 */
export const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
    isOpen,
    title,
    message,
    confirmLabel = FORM_LABELS.CONFIRM,
    cancelLabel = FORM_LABELS.CANCEL,
    onConfirm,
    onCancel,
    isLoading = false,
    variant = 'danger',
}) => {
    if (!isOpen) return null;

    const confirmButtonClasses = variant === 'danger'
        ? 'bg-red-600 hover:bg-red-700 focus:ring-red-500'
        : 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500';

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
            {/* Overlay oscuro */}
            <div
                className="absolute inset-0 bg-black bg-opacity-50 transition-opacity"
                onClick={onCancel}
                aria-hidden="true"
            />

            {/* Contenido del modal */}
            <div
                className="relative bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6 transform transition-all"
                role="dialog"
                aria-modal="true"
                aria-labelledby="modal-title"
            >
                <h3
                    id="modal-title"
                    className="text-lg font-semibold text-gray-900 mb-2"
                >
                    {title}
                </h3>
                <p className="text-gray-600 mb-6">{message}</p>

                <div className="flex justify-end gap-3">
                    <button
                        type="button"
                        onClick={onCancel}
                        disabled={isLoading}
                        className="px-4 py-2 text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {cancelLabel}
                    </button>
                    <button
                        type="button"
                        onClick={onConfirm}
                        disabled={isLoading}
                        className={`px-4 py-2 text-white rounded-md focus:outline-none focus:ring-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors ${confirmButtonClasses}`}
                    >
                        {isLoading ? (
                            <span className="flex items-center gap-2">
                                <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                </svg>
                                Procesando...
                            </span>
                        ) : (
                            confirmLabel
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};
