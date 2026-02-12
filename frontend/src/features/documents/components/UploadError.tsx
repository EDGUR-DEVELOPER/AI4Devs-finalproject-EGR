/**
 * UploadError Component
 * US-DOC-006: Mostrar errores con opción de reintentar
 */

import React from 'react';

interface UploadErrorProps {
  error: { code: string; message: string };
  isRetryable: boolean;
  onRetry?: () => void;
  onDismiss: () => void;
}

/**
 * Componente que muestra errores de upload con opciones de acción
 */
export const UploadError: React.FC<UploadErrorProps> = ({
  error,
  isRetryable,
  onRetry,
  onDismiss,
}) => {
  return (
    <div className="bg-red-50 border border-red-200 rounded-lg p-4" role="alert">
      {/* Icono y mensaje */}
      <div className="flex items-start">
        <div className="shrink-0">
          <svg
            className="h-5 w-5 text-red-400"
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 20 20"
            fill="currentColor"
            aria-hidden="true"
          >
            <path
              fillRule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z"
              clipRule="evenodd"
            />
          </svg>
        </div>
        <div className="ml-3 flex-1">
          <h3 className="text-sm font-medium text-red-800">
            Error al subir documento
          </h3>
          <div className="mt-2 text-sm text-red-700">
            <p>{error.message}</p>
          </div>
          {/* Código de error para debugging */}
          <div className="mt-1 text-xs text-red-600">
            Código: {error.code}
          </div>
        </div>
      </div>

      {/* Botones de acción */}
      <div className="mt-4 flex space-x-3">
        {isRetryable && onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="bg-red-100 px-3 py-2 rounded-md text-sm font-medium text-red-800 hover:bg-red-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
          >
            Reintentar
          </button>
        )}
        <button
          type="button"
          onClick={onDismiss}
          className="bg-white px-3 py-2 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 border border-gray-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500"
        >
          Descartar
        </button>
      </div>
    </div>
  );
};
