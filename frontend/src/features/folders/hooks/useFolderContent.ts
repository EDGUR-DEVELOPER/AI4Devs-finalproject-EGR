/**
 * Hook para obtener contenido de una carpeta
 * Maneja raíz (folderId = undefined) y carpetas específicas
 */
import { useState, useEffect, useCallback } from 'react';
import { folderApi } from '../api/folderApi';
import type { FolderContent } from '../types/folder.types';

interface UseFolderContentReturn {
  data: FolderContent | null;
  isLoading: boolean;
  error: Error | null;
  /** Recarga el contenido de la carpeta actual */
  refetch: () => Promise<void>;
}

export const useFolderContent = (folderId: string | undefined): UseFolderContentReturn => {
  const [data, setData] = useState<FolderContent | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  /** Fetch logic extraído para reutilización en useCallback */
  const fetchContent = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      const content = folderId 
        ? await folderApi.getFolderContent(folderId)
        : await folderApi.getRootContent();
      
      setData(content);
    } catch (err) {
      setError(err instanceof Error ? err : new Error('Error desconocido'));
    } finally {
      setIsLoading(false);
    }
  }, [folderId]);

  useEffect(() => {
    fetchContent();
  }, [fetchContent]);

  return { data, isLoading, error, refetch: fetchContent };
};
