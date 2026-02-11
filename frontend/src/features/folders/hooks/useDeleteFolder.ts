/**
 * Hook mutation para eliminar carpeta vacía
 */
import { useState, useCallback } from 'react';
import { folderApi } from '../api/folderApi';

interface UseDeleteFolderReturn {
  mutateAsync: (folderId: string) => Promise<void>;
  isPending: boolean;
  error: Error | null;
}

export const useDeleteFolder = (): UseDeleteFolderReturn => {
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const mutateAsync = useCallback(async (folderId: string) => {
    setIsPending(true);
    setError(null);

    try {
      await folderApi.deleteFolder(folderId);
    } catch (err) {
      const error = err instanceof Error ? err : new Error('Error al eliminar carpeta');
      setError(error);
      throw error;
    } finally {
      setIsPending(false);
    }
  }, []);

  return { mutateAsync, isPending, error };
};
