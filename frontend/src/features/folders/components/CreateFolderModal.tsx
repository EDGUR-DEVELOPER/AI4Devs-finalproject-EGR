/**
 * CreateFolderModal - Modal para crear nueva carpeta
 * Incluye validación de formulario y manejo de errores
 */
import React, { useState, useEffect } from 'react';
import { Button } from '@ui/forms/Button';
import { Input } from '@ui/forms/Input';
import { useCreateFolder } from '../hooks/useCreateFolder';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';
import { FOLDER_ERROR_CODES } from '../types/folder.types';

interface CreateFolderModalProps {
  isOpen: boolean;
  onClose: () => void;
  parentFolderId: string | null;
  canWrite?: boolean;
  onFolderCreated?: () => Promise<void>;
}

export const CreateFolderModal: React.FC<CreateFolderModalProps> = ({
  isOpen,
  onClose,
  parentFolderId,
  canWrite = true,
  onFolderCreated,
}) => {
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const createFolderMutation = useCreateFolder();
  const { showNotification } = useNotificationStore();
  const isReadOnly = !canWrite;

  // Reset form cuando se cierra modal
  useEffect(() => {
    if (!isOpen) {
      setNombre('');
      setDescripcion('');
      setErrorMessage('');
    }
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    if (isReadOnly) {
      setErrorMessage('No tienes permisos para crear carpetas aqui');
      return;
    }

    // Validación local
    if (!nombre.trim()) {
      setErrorMessage('El nombre es requerido');
      return;
    }

    if (nombre.length > 255) {
      setErrorMessage('El nombre no puede exceder 255 caracteres');
      return;
    }

    try {
      await createFolderMutation.mutateAsync({
        nombre: nombre.trim(),
        descripcion: descripcion.trim() || undefined,
        carpeta_padre_id: parentFolderId,
      });

      showNotification(`Carpeta "${nombre}" creada exitosamente`, 'success');

      // Refetchar el contenido de la carpeta padre después de crear
      if (onFolderCreated) {
        await onFolderCreated();
      }

      onClose();
    } catch (error: any) {
      // Mapear errores específicos del backend
      const errorCode = error.response?.data?.codigo;
      
      if (errorCode === FOLDER_ERROR_CODES.DUPLICATE_NAME) {
        setErrorMessage('Ya existe una carpeta con ese nombre en esta ubicación');
      } else if (errorCode === FOLDER_ERROR_CODES.NO_PERMISSION_WRITE) {
        setErrorMessage('No tienes permisos para crear carpetas aquí');
      } else {
        setErrorMessage('Ocurrió un error al crear la carpeta. Intenta nuevamente.');
      }
    }
  };

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 id="modal-title" className="text-xl font-semibold text-gray-900">
            Nueva carpeta
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl leading-none focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
            aria-label="Cerrar"
          >
            ×
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {isReadOnly && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-md" role="status">
              <p className="text-sm text-amber-800">
                No tienes permisos para crear carpetas en esta ubicacion.
              </p>
            </div>
          )}
          <div>
            <label htmlFor="nombre" className="block text-sm font-medium text-gray-700 mb-1">
              Nombre *
            </label>
            <Input
              id="nombre"
              name="nombre"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              placeholder="Ej: Informes 2024"
              autoFocus
              maxLength={255}
              required
              disabled={isReadOnly}
            />
          </div>

          <div>
            <label htmlFor="descripcion" className="block text-sm font-medium text-gray-700 mb-1">
              Descripción (opcional)
            </label>
            <textarea
              id="descripcion"
              name="descripcion"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              placeholder="Agrega una descripción..."
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              rows={3}
              maxLength={500}
              disabled={isReadOnly}
            />
            <p className="text-xs text-gray-500 mt-1">
              {descripcion.length}/500 caracteres
            </p>
          </div>

          {/* Error message */}
          {errorMessage && (
            <div 
              className="p-3 bg-red-50 border border-red-200 rounded-md"
              role="alert"
            >
              <p className="text-sm text-red-800">{errorMessage}</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              onClick={onClose}
              fullWidth
              disabled={createFolderMutation.isPending}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={createFolderMutation.isPending}
              disabled={isReadOnly || !nombre.trim()}
              fullWidth
            >
              Crear carpeta
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
