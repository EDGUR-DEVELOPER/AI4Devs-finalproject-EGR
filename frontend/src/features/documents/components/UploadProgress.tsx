/**
 * UploadProgress Component
 * US-DOC-006: Indicador visual de progreso durante upload
 */

import React from 'react';
import type { UploadProgress as UploadProgressType } from '../types/upload.types';
import { formatFileSize } from '../utils/fileValidator';

interface UploadProgressProps {
  progress: UploadProgressType;
  onCancel?: () => void;
}

/**
 * Componente que muestra el progreso de upload con barra visual
 */
export const UploadProgress: React.FC<UploadProgressProps> = ({
  progress,
  onCancel,
}) => {
  const { percentage, bytesUploaded, bytesTotal, fileName, state } = progress;

  // Determinar color según estado
  const getProgressColor = () => {
    switch (state) {
      case 'uploading':
        return 'bg-blue-500';
      case 'processing':
        return 'bg-yellow-500';
      case 'success':
        return 'bg-green-500';
      case 'error':
        return 'bg-red-500';
      default:
        return 'bg-gray-300';
    }
  };

  // Texto del estado
  const getStateText = () => {
    switch (state) {
      case 'uploading':
        return 'Subiendo';
      case 'processing':
        return 'Procesando';
      case 'success':
        return 'Completado';
      case 'error':
        return 'Error';
      case 'cancelled':
        return 'Cancelado';
      default:
        return '';
    }
  };

  return (
    <div className="w-full space-y-2">
      {/* Información del archivo */}
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium text-gray-700 truncate">
          {fileName || 'Archivo'}
        </span>
        <span className="text-gray-500 ml-2">
          {getStateText()} - {percentage}%
        </span>
      </div>

      {/* Barra de progreso */}
      <div className="w-full bg-gray-200 rounded-full h-2.5 overflow-hidden">
        <div
          className={`h-2.5 rounded-full transition-all duration-300 ${getProgressColor()}`}
          style={{ width: `${percentage}%` }}
          role="progressbar"
          aria-valuenow={percentage}
          aria-valuemin={0}
          aria-valuemax={100}
        />
      </div>

      {/* Información de tamaño */}
      <div className="flex items-center justify-between text-xs text-gray-500">
        <span>
          {formatFileSize(bytesUploaded)} / {formatFileSize(bytesTotal)}
        </span>
        {onCancel && state === 'uploading' && (
          <button
            onClick={onCancel}
            className="text-red-600 hover:text-red-800 font-medium"
            type="button"
          >
            Cancelar
          </button>
        )}
      </div>
    </div>
  );
};
