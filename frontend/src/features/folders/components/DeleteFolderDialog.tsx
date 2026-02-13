/**
 * DeleteFolderDialog - Diálogo de confirmación para eliminar carpeta
 * Solo permite eliminar carpetas vacías (validación en backend)
 */
import React from 'react';
import { Button } from '@ui/forms/Button';
import { useDeleteFolder } from '../hooks/useDeleteFolder';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';
import { FOLDER_ERROR_CODES } from '../types/folder.types';

interface DeleteFolderDialogProps {
  isOpen: boolean;
  onClose: () => void;
  folderId: string;
  folderName: string;
  onFolderDeleted?: () => Promise<void>;
}

export const DeleteFolderDialog: React.FC<DeleteFolderDialogProps> = ({
  isOpen,
  onClose,
  folderId,
  folderName,
  onFolderDeleted,
}) => {
  const deleteFolderMutation = useDeleteFolder();
  const { showNotification } = useNotificationStore();

  const handleDelete = async () => {
    try {
      await deleteFolderMutation.mutateAsync(folderId);

      showNotification(`Carpeta "${folderName}" eliminada exitosamente`, 'success');

      // Refetchar el contenido después de eliminar
      if (onFolderDeleted) {
        await onFolderDeleted();
      }

      onClose();
    } catch (error: any) {
      const errorCode = error.response?.data?.codigo;

      if (errorCode === FOLDER_ERROR_CODES.FOLDER_NOT_EMPTY) {
        showNotification('La carpeta debe estar vacía antes de eliminarla', 'error');
      } else if (errorCode === FOLDER_ERROR_CODES.NO_PERMISSION_ADMIN) {
        showNotification('No tienes permisos para eliminar esta carpeta', 'error');
      } else {
        showNotification('Ocurrió un error al eliminar la carpeta', 'error');
      }
    }
  };

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="dialog-title"
    >
      <div className="bg-white rounded-lg shadow-xl max-w-sm w-full mx-4 p-6">
        {/* Icono de advertencia */}
        <div className="text-4xl text-red-500 mb-4 text-center">
          ⚠️
        </div>

        {/* Mensaje */}
        <h2 id="dialog-title" className="text-lg font-semibold text-gray-900 mb-2 text-center">
          ¿Eliminar carpeta?
        </h2>
        
        <p className="text-sm text-gray-600 text-center mb-6">
          ¿Estás seguro que deseas eliminar la carpeta <strong>"{folderName}"</strong>?
          <br />
          Esta acción no se puede deshacer.
        </p>

        {/* Acciones */}
        <div className="flex gap-3">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            fullWidth
            disabled={deleteFolderMutation.isPending}
          >
            Cancelar
          </Button>
          
          <Button
            type="button"
            onClick={handleDelete}
            loading={deleteFolderMutation.isPending}
            fullWidth
            variant="primary"
          >
            Eliminar
          </Button>
        </div>
      </div>
    </div>
  );
};
