/**
 * DocumentUpload Component
 * US-DOC-006: Componente principal que orquesta todo el flujo de upload
 */

import React from 'react';
import { useDocumentUpload } from '../hooks/useDocumentUpload';
import { DocumentUploadInput } from './DocumentUploadInput';
import { UploadProgress } from './UploadProgress';
import { UploadError } from './UploadError';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';

interface DocumentUploadProps {
  folderId: string;
  canWrite: boolean;
  onUploadSuccess?: (documentId: string, fileName: string) => void;
}

/**
 * Componente principal para subir documentos
 */
export const DocumentUpload: React.FC<DocumentUploadProps> = ({
  folderId,
  canWrite,
  onUploadSuccess,
}) => {
  const showNotification = useNotificationStore(
    (state) => state.showNotification
  );

  const {
    selectedFile,
    uploadState,
    progress,
    error,
    selectFile,
    uploadFile,
    cancelUpload,
    clearError,
    retryUpload,
  } = useDocumentUpload(folderId, (doc) => {
    // Callback de éxito
    showNotification('Documento subido exitosamente', 'success');
    if (onUploadSuccess) {
      onUploadSuccess(doc.id, doc.name);
    }
  });

  // Renderizar según permisos
  if (!canWrite) {
    return (
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <div className="flex items-start">
          <div className="shrink-0">
            <svg
              className="h-5 w-5 text-yellow-400"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fillRule="evenodd"
                d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 5zm0 9a1 1 0 100-2 1 1 0 000 2z"
                clipRule="evenodd"
              />
            </svg>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-yellow-800">
              Solo lectura
            </h3>
            <div className="mt-2 text-sm text-yellow-700">
              <p>No tienes permiso para subir documentos en esta carpeta.</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Renderizar según estado
  return (
    <div className="space-y-4">
      {/* Input de archivo */}
      {uploadState !== 'uploading' &&
        uploadState !== 'processing' &&
        uploadState !== 'success' && (
          <DocumentUploadInput
            onFileSelected={selectFile}
            onUpload={async () => {
              await uploadFile();
            }}
            isLoading={['uploading', 'processing'].includes(uploadState)}
            isDisabled={['uploading', 'processing'].includes(uploadState)}
            selectedFileName={selectedFile?.name}
          />
        )}

      {/* Progreso de upload */}
      {(uploadState === 'uploading' ||
        uploadState === 'processing' ||
        uploadState === 'success') && (
        <UploadProgress
          progress={progress}
          onCancel={uploadState === 'uploading' ? cancelUpload : undefined}
        />
      )}

      {/* Error de upload */}
      {uploadState === 'error' && error && (
        <UploadError
          error={error}
          isRetryable={true}
          onRetry={retryUpload}
          onDismiss={clearError}
        />
      )}
    </div>
  );
};
