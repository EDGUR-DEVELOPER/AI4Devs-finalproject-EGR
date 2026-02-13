/**
 * DocumentUploadModal - Modal para subir documentos
 * Envuelve DocumentUpload en un modal reutilizable
 * US-DOC-006
 */
import React from 'react';
import { DocumentUpload } from './DocumentUpload';

interface DocumentUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  folderId: string;
  onUploadSuccess?: (documentId: string, fileName: string) => void;
  puede_escribir: boolean;
}

/**
 * Modal para subir documentos
 * Proporciona un overlay reutilizable alrededor de DocumentUpload
 */
export const DocumentUploadModal: React.FC<DocumentUploadModalProps> = ({
  isOpen,
  onClose,
  folderId,
  onUploadSuccess,
  puede_escribir,
}) => {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="upload-modal-title"
    >
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 id="upload-modal-title" className="text-xl font-semibold text-gray-900">
            Subir Documento
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl leading-none focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
            aria-label="Cerrar"
          >
            ×
          </button>
        </div>

        {/* Content */}
        <div className="space-y-4">
          <DocumentUpload
            folderId={folderId}
            canWrite={puede_escribir}
            onUploadSuccess={(documentId, fileName) => {
              if (onUploadSuccess) {
                onUploadSuccess(documentId, fileName);
              }
              onClose();
            }}
          />
        </div>

        {/* Close Button */}
        <div className="flex justify-end mt-4 pt-4 border-t border-gray-200">
          <button
            onClick={onClose}
            className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md transition-colors"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
};
