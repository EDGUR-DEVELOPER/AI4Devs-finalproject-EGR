/**
 * VersionDownloadButton Component
 * US-DOC-006: Botón reutilizable para descargar versión de documento
 */

import React, { useState } from 'react';
import { downloadDocumentVersion } from '../api/documentService';
import { getErrorDetails } from '../utils/errorMapper';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';

interface VersionDownloadButtonProps {
  documentId: string;
  versionId: string;
  versionNumber: number;
  documentName: string;
  extension: string;
  isLoading?: boolean;
}

/**
 * Botón para descargar una versión específica de un documento
 */
export const VersionDownloadButton: React.FC<VersionDownloadButtonProps> = ({
  documentId,
  versionId,
  versionNumber,
  documentName,
  extension,
  isLoading = false,
}) => {
  const [downloading, setDownloading] = useState(false);
  const showNotification = useNotificationStore(
    (state) => state.showNotification
  );

  /**
   * Maneja la descarga del archivo
   */
  const handleDownload = async () => {
    setDownloading(true);
    try {
      // Descargar como blob
      const blob = await downloadDocumentVersion(documentId, versionId, false);

      // Crear link temporal para descargar
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      
      // Nombre del archivo: {nombreBase}_v{version}.{extension}
      const fileName = `${documentName.replace(
        new RegExp(`\\.${extension}$`),
        ''
      )}_v${versionNumber}.${extension}`;
      link.download = fileName;
      
      // Trigger descarga
      document.body.appendChild(link);
      link.click();
      
      // Limpiar
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      showNotification('Descarga iniciada', 'success');
    } catch (error) {
      const errorDetails = getErrorDetails(error);
      showNotification(
        `Error al descargar: ${errorDetails.message}`,
        'error'
      );
    } finally {
      setDownloading(false);
    }
  };

  return (
    <button
      type="button"
      onClick={handleDownload}
      disabled={isLoading || downloading}
      className="inline-flex items-center px-3 py-1.5 text-sm font-medium text-blue-600 hover:text-blue-800 disabled:text-gray-400 disabled:cursor-not-allowed"
      aria-label={`Descargar versión ${versionNumber}`}
    >
      {downloading ? (
        <>
          <svg
            className="animate-spin -ml-1 mr-2 h-4 w-4"
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
            ></circle>
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
            ></path>
          </svg>
          Descargando...
        </>
      ) : (
        <>
          <svg
            className="-ml-1 mr-2 h-4 w-4"
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
          Descargar
        </>
      )}
    </button>
  );
};
