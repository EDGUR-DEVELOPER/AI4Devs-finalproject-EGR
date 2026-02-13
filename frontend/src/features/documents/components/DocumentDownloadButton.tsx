/**
 * DocumentDownloadButton Component
 * US-DOC-007: Botón reutilizable para descargar documento actual desde lista
 */

import React, { useState } from 'react';
import { useDocumentDownload } from '../hooks/useDocumentDownload';

interface DocumentDownloadButtonProps {
  documentId: string | number;
  fileName: string;
  canDownload: boolean;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'primary' | 'secondary' | 'danger';
  showLabel?: boolean;
  className?: string;
}

/**
 * Botón para descargar documento actual (versión activa)
 * 
 * Maneja estado de descarga, spinner, y notificaciones automáticamente.
 * Respeta permisos del usuario: botón deshabilitado sin permiso LECTURA.
 * 
 * @component
 * @example
 * <DocumentDownloadButton
 *   documentId="doc-123"
 *   fileName="contrato.pdf"
 *   canDownload={true}
 *   size="sm"
 *   showLabel={false}
 * />
 */
export const DocumentDownloadButton: React.FC<DocumentDownloadButtonProps> = ({
  documentId,
  fileName,
  canDownload,
  disabled = false,
  size = 'md',
  variant = 'primary',
  showLabel = true,
  className = '',
}) => {
  const { isDownloading, error, download, clearError } =
    useDocumentDownload();
  const [showTooltip, setShowTooltip] = useState(false);

  const handleClick = async () => {
    await download(documentId, fileName);
  };

  const isDisabled = disabled || !canDownload;

  // Tamaños
  const sizeClasses = {
    sm: 'px-2 py-1 text-xs',
    md: 'px-3 py-2 text-sm',
    lg: 'px-4 py-2.5 text-base',
  };

  // Variantes de color
  const variantClasses = {
    primary: 'text-blue-600 hover:text-blue-800',
    secondary: 'text-gray-600 hover:text-gray-800',
    danger: 'text-red-600 hover:text-red-800',
  };

  // Estados disabled
  const disabledClasses = isDisabled
    ? 'text-gray-400 cursor-not-allowed opacity-60'
    : 'cursor-pointer';

  // Ícono descarga (SVG)
  const DownloadIcon = () => (
    <svg
      className={`h-4 w-4 ${showLabel ? 'mr-2' : ''}`}
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 20 20"
      fill="currentColor"
    >
      <path
        fillRule="evenodd"
        d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z"
        clipRule="evenodd"
      />
    </svg>
  );

  // Spinner (SVG)
  const Spinner = () => (
    <svg
      className={`animate-spin h-4 w-4 ${showLabel ? 'mr-2' : ''}`}
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
  );

  return (
    <div className="relative">
      <button
        type="button"
        onClick={handleClick}
        disabled={isDisabled || isDownloading}
        className={`
          inline-flex items-center font-medium rounded transition-colors
          ${sizeClasses[size]}
          ${variantClasses[variant]}
          ${disabledClasses}
          ${isDownloading ? 'opacity-75' : ''}
          ${className}
        `}
        aria-label={`Descargar documento: ${fileName}`}
        aria-disabled={isDisabled}
        onMouseEnter={() => {
          if (isDisabled) setShowTooltip(true);
        }}
        onMouseLeave={() => setShowTooltip(false)}
      >
        {isDownloading ? (
          <>
            <Spinner />
            {showLabel && 'Descargando...'}
          </>
        ) : (
          <>
            <DownloadIcon />
            {showLabel && 'Descargar'}
          </>
        )}
      </button>

      {/* Tooltip para botón deshabilitado */}
      {showTooltip && isDisabled && (
        <div
          className="absolute left-0 mt-1 bg-gray-800 text-white text-xs px-2 py-1 rounded whitespace-nowrap z-50 pointer-events-none"
          role="tooltip"
        >
          Requiere permiso de Lectura
          <div className="absolute bottom-full left-2 border-4 border-transparent border-b-gray-800" />
        </div>
      )}

      {/* Mensaje de error */}
      {error && (
        <div className="absolute left-0 mt-1 bg-red-100 border border-red-400 text-red-700 text-xs px-2 py-1 rounded whitespace-nowrap z-50">
          {error}
          <button
            onClick={clearError}
            className="ml-1 text-red-700 hover:text-red-900 font-bold"
          >
            ✕
          </button>
        </div>
      )}
    </div>
  );
};
