/**
 * VersionItem Component
 * US-DOC-006: Fila individual de versión en el historial
 */

import React from 'react';
import type { DocumentVersionDTO } from '../types/document.types';
import { formatFileSize } from '../utils/fileValidator';
import { formatDateTime } from '../utils/dateFormatter';
import { VersionDownloadButton } from './VersionDownloadButton';

interface VersionItemProps {
  version: DocumentVersionDTO;
  documentName: string;
  documentExtension: string;
  onUploadNewVersion?: () => void;
  isLoading: boolean;
}

/**
 * Componente que muestra una fila de versión individual
 */
export const VersionItem: React.FC<VersionItemProps> = ({
  version,
  documentName,
  documentExtension,
  onUploadNewVersion,
  isLoading,
}) => {
  return (
    <div
      className={`
        flex items-center justify-between p-4 border rounded-lg
        ${version.isCurrentVersion ? 'border-blue-200 bg-blue-50' : 'border-gray-200 bg-white'}
        hover:shadow-sm transition-shadow duration-200
      `}
    >
      {/* Información de la versión */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center space-x-2">
          {/* Número de versión */}
          <span className="font-semibold text-gray-900">
            v{version.versionNumber}
          </span>

          {/* Badge de versión actual */}
          {version.isCurrentVersion && (
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
              Actual
            </span>
          )}
        </div>

        {/* Metadatos */}
        <div className="mt-1 flex items-center space-x-4 text-sm text-gray-500">
          {/* Usuario */}
          <span className="flex items-center">
            <svg
              className="mr-1.5 h-4 w-4 text-gray-400"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fillRule="evenodd"
                d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
                clipRule="evenodd"
              />
            </svg>
            {version.createdBy.fullName || version.createdBy.email}
          </span>

          {/* Fecha */}
          <span className="flex items-center">
            <svg
              className="mr-1.5 h-4 w-4 text-gray-400"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fillRule="evenodd"
                d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
                clipRule="evenodd"
              />
            </svg>
            {formatDateTime(version.createdAt)}
          </span>

          {/* Tamaño */}
          <span className="flex items-center">
            <svg
              className="mr-1.5 h-4 w-4 text-gray-400"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fillRule="evenodd"
                d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z"
                clipRule="evenodd"
              />
            </svg>
            {formatFileSize(version.size)}
          </span>
        </div>

        {/* Descripción (si existe) */}
        {version.description && (
          <div className="mt-2 text-sm text-gray-600 italic">
            {version.description}
          </div>
        )}
      </div>

      {/* Acciones */}
      <div className="ml-4 flex items-center space-x-2">
        {/* Botón de descarga */}
        <VersionDownloadButton
          documentId={version.documentId}
          versionId={version.id}
          versionNumber={version.versionNumber}
          documentName={documentName}
          extension={documentExtension}
          isLoading={isLoading}
        />

        {/* Botón de nueva versión (solo en versión actual) */}
        {version.isCurrentVersion && onUploadNewVersion && (
          <button
            type="button"
            onClick={onUploadNewVersion}
            disabled={isLoading}
            className="inline-flex items-center px-3 py-1.5 text-sm font-medium text-green-600 hover:text-green-800 disabled:text-gray-400 disabled:cursor-not-allowed"
          >
            <svg
              className="-ml-1 mr-2 h-4 w-4"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fillRule="evenodd"
                d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z"
                clipRule="evenodd"
              />
            </svg>
            Nueva versión
          </button>
        )}
      </div>
    </div>
  );
};
