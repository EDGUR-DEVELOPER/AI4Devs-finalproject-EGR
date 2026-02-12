/**
 * VersionHistory Component
 * US-DOC-006: Componente principal para mostrar historial de versiones
 */

import React, { useState } from 'react';
import { useDocumentVersions } from '../hooks/useDocumentVersions';
import { VersionItem } from './VersionItem';
import { DocumentUpload } from './DocumentUpload';

interface VersionHistoryProps {
  documentId: string;
  documentName: string;
  documentExtension: string;
  folderId: string;
  isOpen: boolean;
  onNewVersionUpload?: (versionId: string) => void;
}

/**
 * Componente que muestra el historial completo de versiones de un documento
 */
export const VersionHistory: React.FC<VersionHistoryProps> = ({
  documentId,
  documentName,
  documentExtension,
  folderId,
  isOpen,
  onNewVersionUpload,
}) => {
  const [showUploadForm, setShowUploadForm] = useState(false);

  const {
    versions,
    currentPage,
    totalPages,
    isLoading,
    error,
    invalidateCache,
    goToPage,
  } = useDocumentVersions(documentId, isOpen);

  /**
   * Maneja el click en "Nueva versión"
   */
  const handleUploadNewVersion = () => {
    setShowUploadForm(true);
  };

  /**
   * Maneja el éxito de subir nueva versión
   */
  const handleUploadSuccess = (newDocId: string) => {
    setShowUploadForm(false);
    invalidateCache(); // Recargar versiones
    if (onNewVersionUpload) {
      onNewVersionUpload(newDocId);
    }
  };

  return (
    <div className="space-y-4">
      {/* Header con título y acciones */}
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold text-gray-900">
          Historial de Versiones
        </h3>
        <button
          type="button"
          onClick={invalidateCache}
          disabled={isLoading}
          className="inline-flex items-center px-3 py-1.5 text-sm font-medium text-gray-700 hover:text-gray-900 disabled:text-gray-400"
          aria-label="Refrescar versiones"
        >
          <svg
            className={`-ml-1 mr-2 h-4 w-4 ${isLoading ? 'animate-spin' : ''}`}
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z"
              clipRule="evenodd"
            />
          </svg>
          Refrescar
        </button>
      </div>

      {/* Formulario de nueva versión (si está visible) */}
      {showUploadForm && (
        <div className="border-2 border-dashed border-gray-300 rounded-lg p-4 bg-gray-50">
          <div className="flex items-center justify-between mb-4">
            <h4 className="text-sm font-medium text-gray-900">
              Subir nueva versión
            </h4>
            <button
              type="button"
              onClick={() => setShowUploadForm(false)}
              className="text-gray-400 hover:text-gray-600"
            >
              <svg
                className="h-5 w-5"
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                  clipRule="evenodd"
                />
              </svg>
            </button>
          </div>
          <DocumentUpload
            folderId={folderId}
            onUploadSuccess={handleUploadSuccess}
          />
        </div>
      )}

      {/* Loading */}
      {isLoading && versions.length === 0 && (
        <div className="flex justify-center items-center py-12">
          <svg
            className="animate-spin h-8 w-8 text-blue-600"
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
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {/* Lista de versiones */}
      {!isLoading && !error && versions.length > 0 && (
        <div className="space-y-3">
          {versions.map((version) => (
            <VersionItem
              key={version.id}
              version={version}
              documentName={documentName}
              documentExtension={documentExtension}
              onUploadNewVersion={
                version.isCurrentVersion ? handleUploadNewVersion : undefined
              }
              isLoading={isLoading}
            />
          ))}
        </div>
      )}

      {/* Empty state */}
      {!isLoading && !error && versions.length === 0 && (
        <div className="text-center py-12">
          <svg
            className="mx-auto h-12 w-12 text-gray-400"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-gray-900">
            No hay versiones
          </h3>
          <p className="mt-1 text-sm text-gray-500">
            Este documento aún no tiene versiones registradas.
          </p>
        </div>
      )}

      {/* Paginación */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-gray-200 pt-4">
          <div className="text-sm text-gray-700">
            Página {currentPage + 1} de {totalPages}
          </div>
          <div className="flex space-x-2">
            <button
              type="button"
              onClick={() => goToPage(currentPage - 1)}
              disabled={currentPage === 0 || isLoading}
              className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed"
            >
              Anterior
            </button>
            <button
              type="button"
              onClick={() => goToPage(currentPage + 1)}
              disabled={currentPage >= totalPages - 1 || isLoading}
              className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:bg-gray-100 disabled:text-gray-400 disabled:cursor-not-allowed"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
