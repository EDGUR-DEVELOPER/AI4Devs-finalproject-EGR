/**
 * useDocumentUpload Hook
 * US-DOC-006: Orquestación de lógica de upload con estado y validaciones
 */

import { useState, useCallback, useRef } from 'react';
import { uploadDocument } from '../api/documentService';
import { validateFile } from '../utils/fileValidator';
import { getErrorDetails } from '../utils/errorMapper';
import type { UploadState, UploadProgress } from '../types/upload.types';
import type { DocumentDTO } from '../types/document.types';

/**
 * Interfaz de retorno del hook
 */
export interface UseDocumentUploadReturn {
  selectedFile: File | null;
  uploadState: UploadState;
  progress: UploadProgress;
  error: { code: string; message: string } | null;
  selectFile: (file: File) => void;
  uploadFile: () => Promise<DocumentDTO>;
  cancelUpload: () => void;
  clearError: () => void;
  retryUpload: () => Promise<DocumentDTO>;
  reset: () => void;
}

/**
 * Hook para gestionar el upload de documentos
 * @param folderId - ID de la carpeta destino
 * @param onUploadSuccess - Callback opcional cuando el upload es exitoso
 */
export function useDocumentUpload(
  folderId: string,
  onUploadSuccess?: (doc: DocumentDTO) => void
): UseDocumentUploadReturn {
  // Estados
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadState, setUploadState] = useState<UploadState>('idle');
  const [progress, setProgress] = useState<UploadProgress>({
    state: 'idle',
    percentage: 0,
    bytesUploaded: 0,
    bytesTotal: 0,
  });
  const [error, setError] = useState<{
    code: string;
    message: string;
  } | null>(null);

  // Ref para AbortController (para poder cancelar)
  const abortControllerRef = useRef<AbortController | null>(null);

  // Ref para throttle de progreso
  const lastProgressUpdateRef = useRef<number>(0);

  /**
   * Selecciona un archivo y valida
   */
  const selectFile = useCallback((file: File) => {
    // Validar archivo
    const validation = validateFile(file);
    if (!validation.valid) {
      setError({
        code: 'INVALID_FILE',
        message: validation.error || 'Archivo inválido',
      });
      setUploadState('error');
      return;
    }

    // Archivo válido
    setSelectedFile(file);
    setUploadState('selected');
    setError(null);
    setProgress({
      state: 'selected',
      percentage: 0,
      bytesUploaded: 0,
      bytesTotal: file.size,
      fileName: file.name,
    });
  }, []);

  /**
   * Inicia el upload del archivo seleccionado
   */
  const uploadFile = useCallback(async (): Promise<DocumentDTO> => {
    if (!selectedFile) {
      throw new Error('No hay archivo seleccionado');
    }

    setUploadState('uploading');
    setError(null);

    try {
      // Crear AbortController para cancelación
      abortControllerRef.current = new AbortController();

      // Callback de progreso con throttle
      const onProgress = (progressData: UploadProgress) => {
        const now = Date.now();
        // Limitar actualizaciones a una cada 100ms
        if (now - lastProgressUpdateRef.current >= 100) {
          setProgress(progressData);
          lastProgressUpdateRef.current = now;
        }
      };

      // Ejecutar upload
      const result = await uploadDocument(folderId, selectedFile, onProgress);

      // Éxito
      setUploadState('success');
      setProgress((prev) => ({
        ...prev,
        state: 'success',
        percentage: 100,
      }));

      // Callback de éxito
      if (onUploadSuccess) {
        onUploadSuccess(result);
      }

      // Limpiar después de 2 segundos
      setTimeout(() => {
        reset();
      }, 2000);

      return result;
    } catch (err) {
      const errorDetails = getErrorDetails(err);
      setError({
        code: errorDetails.code,
        message: errorDetails.message,
      });
      setUploadState('error');
      throw err;
    }
  }, [selectedFile, folderId, onUploadSuccess]);

  /**
   * Cancela el upload en progreso
   */
  const cancelUpload = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setUploadState('cancelled');
    setProgress((prev) => ({
      ...prev,
      state: 'cancelled',
    }));
  }, []);

  /**
   * Limpia el error y vuelve al estado inicial
   */
  const clearError = useCallback(() => {
    setError(null);
    setUploadState('idle');
    setSelectedFile(null);
    setProgress({
      state: 'idle',
      percentage: 0,
      bytesUploaded: 0,
      bytesTotal: 0,
    });
  }, []);

  /**
   * Reintenta el upload después de un error
   */
  const retryUpload = useCallback(async (): Promise<DocumentDTO> => {
    if (!selectedFile) {
      throw new Error('No hay archivo para reintentar');
    }
    return uploadFile();
  }, [selectedFile, uploadFile]);

  /**
   * Reinicia completamente el estado
   */
  const reset = useCallback(() => {
    setSelectedFile(null);
    setUploadState('idle');
    setError(null);
    setProgress({
      state: 'idle',
      percentage: 0,
      bytesUploaded: 0,
      bytesTotal: 0,
    });
  }, []);

  return {
    selectedFile,
    uploadState,
    progress,
    error,
    selectFile,
    uploadFile,
    cancelUpload,
    clearError,
    retryUpload,
    reset,
  };
}
