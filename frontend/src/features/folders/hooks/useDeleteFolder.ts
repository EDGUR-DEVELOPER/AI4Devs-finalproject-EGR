/**
 * Hook mutation para eliminar carpeta vacía
 */
import { useQueryClient } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';
import { useMutation } from '@tanstack/react-query';

export const useDeleteFolder = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (folderId: string) => {
      await folderApi.deleteFolder(folderId);
    },
    onSuccess: () => {
      // Invalidar todas las consultas de contenido de carpetas
      // para que se refresquen cuando sea necesario
      queryClient.invalidateQueries({
        queryKey: ['folderContent'],
      });
      
      // También invalidar breadcrumb
      queryClient.invalidateQueries({
        queryKey: ['breadcrumb'],
      });
    },
  });
};
