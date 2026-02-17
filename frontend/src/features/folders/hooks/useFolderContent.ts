/**
 * Hook para obtener contenido de una carpeta
 * Utiliza React Query para cacheo automático y manejo eficiente de estado
 */
import { useQuery } from '@tanstack/react-query';
import { isAxiosError } from 'axios';
import { folderApi } from '../api/folderApi';
import type { FolderContent } from '../types/folder.types';

export const useFolderContent = (folderId: string | undefined) => {
  return useQuery<FolderContent>({
    queryKey: ['folderContent', folderId],
    queryFn: async () => {
      const content = folderId 
        ? await folderApi.getFolderContent(folderId)
        : await folderApi.getRootContent();
      
      return content;
    },
    staleTime: 5 * 60 * 1000, // 5 minutos
    gcTime: 10 * 60 * 1000, // 10 minutos (antes conocido como cacheTime)
    retry: (failureCount, error) => {
      if (isAxiosError(error) && error.response?.status === 403) {
        return false;
      }
      return failureCount < 1;
    },
    retryOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
};
