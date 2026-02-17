/**
 * Hook mutation para crear carpeta
 * Incluye callback de éxito para recargar datos
 */
import { useState, useCallback } from 'react';
import { folderApi } from '../api/folderApi';
import type { CreateFolderRequest, CreateFolderResponse } from '../types/folder.types';

interface UseCreateFolderReturn {
  mutateAsync: (request: CreateFolderRequest) => Promise<CreateFolderResponse>;
  isPending: boolean;
  error: Error | null;
}

export const useCreateFolder = (): UseCreateFolderReturn => {
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const mutateAsync = useCallback(async (request: CreateFolderRequest) => {
    setIsPending(true);
    setError(null);

    try {
      const response = await folderApi.createFolder(request);
      return response;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Error al crear carpeta');
      setError(error);
      throw error;
    } finally {
      setIsPending(false);
    }
  }, []);

  return { mutateAsync, isPending, error };
};
