import { useState } from 'react';
import axios from 'axios';
import { downloadCurrentDocument } from '../api/documentService';
import { useNotificationStore } from '@common/ui/notifications/useNotificationStore';

/**
 * Hook personalizado para descargar documentos actual con gestión de estado
 * 
 * Maneja lógica de descarga, errores, notificaciones y spinner de carga.
 * Compatible con múltiples descargas secuenciales (solo una activa a la vez).
 * 
 * US-DOC-007: Descarga de documento actual desde lista de documentos
 * 
 * @returns {Object} Estados y funciones
 * @returns {boolean} isDownloading - Indica si descarga está en progreso
 * @returns {string|null} error - Mensaje de error si ocurrió durante descarga
 * @returns {Function} download - Inicia descarga: (documentId: string, fileName: string) => Promise<void>
 * @returns {Function} clearError - Limpia estado de error actual
 * 
 * @example
 * const { isDownloading, error, download, clearError } = useDocumentDownload();
 * 
 * const handleDownload = async () => {
 *   try {
 *     await download('doc-123', 'contrato.pdf');
 *     // Notificación de éxito se muestra automáticamente
 *   } catch (err) {
 *     console.error('Error descargando:', err);
 *   }
 * };
 * 
 * return (
 *   <>
 *     <button onClick={handleDownload} disabled={isDownloading}>
 *       {isDownloading ? 'Descargando...' : 'Descargar'}
 *     </button>
 *     {error && <p style={{ color: 'red' }}>{error}</p>}
 *   </>
 * );
 */
export function useDocumentDownload() {
  const [isDownloading, setIsDownloading] = useState(false);
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
        case 400:
          return 'Datos inválidos. Contacta con soporte.';
        case 401:
          return 'Sesión expirada. Por favor, inicia sesión nuevamente.';
        case 403:
          return 'No tienes permiso para descargar este documento.';
        case 404:
          return 'Documento no encontrado o fue eliminado.';
        case 500:
          return 'Error de servidor. Intenta más tarde.';
        default:
          return message || 'Error desconocido al descargar documento.';
      }
    }

    if (err instanceof TypeError && err.message === 'Failed to fetch') {
      return 'Sin conexión. Verifica tu internet e intenta de nuevo.';
    }

    if (err instanceof Error) {
      return err.message;
    }

    return 'Error desconocido al descargar documento.';
  };

  /**
   * Descarga documento actual desde el servidor
   * Maneja creación de descarga en navegador y notificaciones
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

  const download = async (documentId: string | number, fileName: string): Promise<void> => {
    const normalizedDocumentId = normalizeDocumentId(documentId);

    // Validaciones iniciales
    if (!normalizedDocumentId) {
      const errorMsg = 'ID de documento inválido';
      setError(errorMsg);
      showNotification(errorMsg, 'error');
      return;
    }

    if (!fileName || !fileName.trim()) {
      fileName = 'documento';
    }

    try {
      setIsDownloading(true);
      setError(null);

      // Descargar blob desde backend
      const blob = await downloadCurrentDocument(normalizedDocumentId);

      // Crear descarga en navegador
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      // Notificación de éxito
      const successMsg = `Descarga iniciada: ${fileName}`;
      showNotification(successMsg, 'success');
    } catch (err: unknown) {
      const errorMessage = extractErrorMessage(err);
      setError(errorMessage);
      showNotification(`Error descargando: ${errorMessage}`, 'error');
    } finally {
      setIsDownloading(false);
    }
  };

  /**
   * Limpia estado de error
   */
  const clearError = (): void => {
    setError(null);
  };

  return {
    isDownloading,
    error,
    download,
    clearError,
  };
}
