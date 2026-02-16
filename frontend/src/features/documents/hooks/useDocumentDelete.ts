/**
 * Hook personalizado para eliminar documentos
 * US-DOC-008: Eliminación de documento desde la UI
 */

import { useState } from 'react';
import axios from 'axios';
import { deleteDocument } from '../api/documentService';
import { useNotificationStore } from '@common/ui/notifications/useNotificationStore';

/**
 * Respuesta del hook useDocumentDelete
 */
interface UseDocumentDeleteReturn {
  /** Indica si eliminación está en progreso */
  isDeleting: boolean;
  /** Mensaje de error si lo hay */
  error: string | null;
  /** Función para eliminar documento con confirmación */
  deleteDocumentWithConfirmation: (documentId: string | number, documentName: string) => Promise<boolean>;
  /** Función para limpiar estado de error */
  clearError: () => void;
}

/**
 * Hook personalizado para eliminar documentos con gestión de estado
 * 
 * Maneja lógica de eliminación, errores, notificaciones y spinner de carga.
 * Compatible con múltiples eliminaciones secuenciales (solo una activa a la vez).
 * 
 * @returns {UseDocumentDeleteReturn} Estados y funciones
 * 
 * @example
 * const { isDeleting, error, deleteDocumentWithConfirmation, clearError } = useDocumentDelete();
 * 
 * const handleDelete = async () => {
 *   const success = await deleteDocumentWithConfirmation('doc-123', 'documento.pdf');
 *   if (success) {
 *     // Refrescar lista de documentos
 *   }
 * };
 */
export function useDocumentDelete(): UseDocumentDeleteReturn {
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { showNotification } = useNotificationStore();

  /**
   * Extrae mensaje de error de respuesta Axios
   */
  const extractErrorMessage = (err: unknown): string => {
    if (axios.isAxiosError(err)) {
      const status = err.response?.status;
      const message = err.response?.data?.message;

      switch (status) {
        case 401:
          return 'Sesión expirada. Por favor, inicie sesión nuevamente.';
        case 403:
          return 'No tiene permisos para eliminar este documento.';
        case 404:
          return 'Documento no encontrado.';
        case 409:
          return 'El documento ya está eliminado.';
        case 500:
          return 'Error al eliminar el documento. Por favor, intente nuevamente.';
        default:
          return message || 'Error desconocido al eliminar documento.';
      }
    }

    if (err instanceof TypeError && err.message === 'Failed to fetch') {
      return 'Sin conexión. Verifica tu internet e intenta de nuevo.';
    }

    if (err instanceof Error) {
      return err.message;
    }

    return 'Error desconocido al eliminar documento.';
  };

  /**
   * Normaliza ID de documento a string
   */
  const normalizeDocumentId = (value: unknown): string | null => {
    if (typeof value === 'string') {
      const trimmed = value.trim();
      return trimmed.length > 0 ? trimmed : null;
    }

    if (typeof value === 'number' && Number.isFinite(value)) {
      return String(value);
    }

    return null;
  };

  /**
   * Elimina un documento con confirmación previa
   */
  const deleteDocumentWithConfirmation = async (
    documentId: string | number,
    documentName: string
  ): Promise<boolean> => {
    const normalizedDocumentId = normalizeDocumentId(documentId);

    if (!normalizedDocumentId) {
      setError('ID de documento inválido');
      return false;
    }

    setIsDeleting(true);
    setError(null);

    try {
      await deleteDocument(normalizedDocumentId);
      showNotification(`"${documentName}" se ha eliminado correctamente.`, 'success');
      return true;
    } catch (err) {
      const errorMessage = extractErrorMessage(err);
      setError(errorMessage);
      showNotification(errorMessage, 'error');
      return false;
    } finally {
      setIsDeleting(false);
    }
  };

  /**
   * Limpia estado de error actual
   */
  const clearError = () => {
    setError(null);
  };

  return {
    isDeleting,
    error,
    deleteDocumentWithConfirmation,
    clearError,
  };
}
