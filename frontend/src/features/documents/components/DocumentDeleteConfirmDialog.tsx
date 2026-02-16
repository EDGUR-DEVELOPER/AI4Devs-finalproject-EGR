/**
 * DocumentDeleteConfirmDialog Component
 * US-DOC-008: Diálogo de confirmación para eliminación de documento
 */

import React from 'react';

export interface DocumentDeleteConfirmDialogProps {
  /** ¿Diálogo está visible? */
  isOpen: boolean;
  /** Callback cuando cierra */
  onClose: () => void;
  /** Callback cuando confirma eliminar */
  onConfirm: () => void;
  /** Nombre del documento */
  documentName: string;
  /** ¿Está eliminando? (mostrar spinner) */
  isDeleting: boolean;
}

/**
 * Diálogo modal de confirmación para eliminación de documento
 * 
 * Presenta advertencia clara y amigable antes de proceder
 * con la eliminación (soft delete).
 * 
 * @component
 * @example
 * <DocumentDeleteConfirmDialog
 *   isOpen={showDialog}
 *   onClose={() => setShowDialog(false)}
 *   onConfirm={handleDeleteConfirm}
 *   documentName="contrato.pdf"
 *   isDeleting={false}
 * />
 */
export const DocumentDeleteConfirmDialog: React.FC<DocumentDeleteConfirmDialogProps> = ({
  isOpen,
  onClose,
  onConfirm,
  documentName,
  isDeleting,
}) => {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-dialog-title"
    >
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
        {/* Header con icono de alerta */}
        <div className="flex items-start mb-4">
          <svg
            className="w-6 h-6 text-red-500 shrink-0 mt-0.5"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
              clipRule="evenodd"
            />
          </svg>
          <div className="ml-3 flex-1">
            <h2 id="delete-dialog-title" className="text-lg font-semibold text-gray-900">
              Confirmar eliminación
            </h2>
          </div>
          <button
            onClick={onClose}
            disabled={isDeleting}
            className="text-gray-400 hover:text-gray-600 text-2xl leading-none focus:outline-none focus:ring-2 focus:ring-red-500 rounded disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label="Cerrar"
          >
            ×
          </button>
        </div>

        {/* Contenido */}
        <div className="space-y-4">
          {/* Pregunta de confirmación */}
          <p className="text-sm text-gray-700">
            ¿Está seguro que desea eliminar el siguiente documento?
          </p>

          {/* Nombre del documento destacado */}
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-3">
            <p className="text-sm font-semibold text-gray-900 break-words">{documentName}</p>
          </div>

          {/* Caja de advertencia */}
          <div className="bg-red-50 border border-red-200 rounded-lg p-3">
            <div className="flex items-start">
              <svg
                className="w-5 h-5 text-red-600 shrink-0 mt-0.5"
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                  clipRule="evenodd"
                />
              </svg>
              <p className="ml-2 text-xs text-red-800">
                Esta acción no se puede deshacer desde la interfaz de usuario. El documento quedará
                marcado como eliminado pero sus archivos se conservarán de forma segura.
              </p>
            </div>
          </div>
        </div>

        {/* Botones de acción */}
        <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200">
          <button
            onClick={onClose}
            disabled={isDeleting}
            className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-gray-300"
            type="button"
          >
            Cancelar
          </button>
          <button
            onClick={onConfirm}
            disabled={isDeleting}
            className="px-4 py-2 bg-red-600 text-white hover:bg-red-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-red-500 flex items-center gap-2"
            type="button"
          >
            {isDeleting ? (
              <>
                <svg
                  className="animate-spin h-4 w-4"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                Eliminando...
              </>
            ) : (
              <>
                <span>Eliminar documento</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
