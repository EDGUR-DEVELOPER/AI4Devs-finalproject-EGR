/**
 * DocumentUploadInput Component
 * US-DOC-006: Input de archivo con drag & drop y validación visual
 */

import React, { useRef, useState } from 'react';
import { ALLOWED_EXTENSIONS } from '../utils/fileValidator';

interface DocumentUploadInputProps {
  onFileSelected: (file: File) => void;
  onUpload: () => Promise<void>;
  isLoading: boolean;
  isDisabled: boolean;
  selectedFileName?: string;
}

/**
 * Componente de input para seleccionar archivos con drag & drop
 */
export const DocumentUploadInput: React.FC<DocumentUploadInputProps> = ({
  onFileSelected,
  onUpload,
  isLoading,
  isDisabled,
  selectedFileName,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);

  // Extensiones permitidas para el input
  const acceptedExtensions = ALLOWED_EXTENSIONS.map((ext) => `.${ext}`).join(
    ','
  );

  /**
   * Maneja el cambio de archivo en el input
   */
  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      onFileSelected(file);
    }
  };

  /**
   * Maneja el click en el botón de seleccionar
   */
  const handleSelectClick = () => {
    fileInputRef.current?.click();
  };

  /**
   * Maneja eventos de drag & drop
   */
  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const file = e.dataTransfer.files?.[0];
    if (file) {
      onFileSelected(file);
    }
  };

  return (
    <div className="w-full space-y-4">
      {/* Zona de drag & drop */}
      <div
        onDragEnter={handleDragEnter}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={`
          border-2 border-dashed rounded-lg p-6 text-center cursor-pointer
          transition-colors duration-200
          ${
            isDragging
              ? 'border-blue-500 bg-blue-50'
              : 'border-gray-300 hover:border-gray-400'
          }
          ${isDisabled ? 'opacity-50 cursor-not-allowed' : ''}
        `}
        onClick={!isDisabled ? handleSelectClick : undefined}
      >
        {/* Icono de upload */}
        <svg
          className={`mx-auto h-12 w-12 ${
            isDragging ? 'text-blue-500' : 'text-gray-400'
          }`}
          stroke="currentColor"
          fill="none"
          viewBox="0 0 48 48"
          aria-hidden="true"
        >
          <path
            d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>

        {/* Texto */}
        <div className="mt-4">
          {selectedFileName ? (
            <p className="text-sm font-medium text-gray-900">
              {selectedFileName}
            </p>
          ) : (
            <>
              <p className="text-sm font-medium text-gray-900">
                Arrastra un archivo aquí o haz clic para seleccionar
              </p>
              <p className="text-xs text-gray-500 mt-1">
                PDF, DOC, XLSX, PNG, etc. hasta 100 MB
              </p>
            </>
          )}
        </div>

        {/* Input oculto */}
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          accept={acceptedExtensions}
          onChange={handleFileChange}
          disabled={isDisabled}
        />
      </div>

      {/* Botón de subir */}
      {selectedFileName && (
        <button
          type="button"
          onClick={onUpload}
          disabled={isDisabled || isLoading}
          className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors duration-200"
        >
          {isLoading ? 'Subiendo...' : 'Subir documento'}
        </button>
      )}
    </div>
  );
};
