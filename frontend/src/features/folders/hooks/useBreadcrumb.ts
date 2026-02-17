/**
 * Hook para obtener ruta de breadcrumb de una carpeta
 */
import { useState, useEffect } from 'react';
import { folderApi } from '../api/folderApi';
import type { BreadcrumbSegment } from '../types/folder.types';

interface UseBreadcrumbReturn {
  breadcrumb: BreadcrumbSegment[];
  isLoading: boolean;
  error: Error | null;
}

export const useBreadcrumb = (folderId: string | undefined): UseBreadcrumbReturn => {
  const [breadcrumb, setBreadcrumb] = useState<BreadcrumbSegment[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    // Si es raíz, retornar breadcrumb manual
    if (!folderId) {
      setBreadcrumb([{ id: undefined, nombre: 'Raiz' }]);
      setIsLoading(false);
      return;
    }

    const fetchBreadcrumb = async () => {
      setIsLoading(true);
      setError(null);
      
      try {
        const [path, folder] = await Promise.all([
          folderApi.getFolderPath(folderId),
          folderApi.getFolderById(folderId),
        ]);

        if (folder.es_raiz) {
          setBreadcrumb([{ id: undefined, nombre: 'Raiz' }]);
          return;
        }

        const rootToParent = [...path]
          .reverse()
          .map((segment, index) =>
            index === 0
              ? { ...segment, nombre: 'Raiz' }
              : segment
          );

        setBreadcrumb([
          ...rootToParent,
          { id: folderId, nombre: folder.nombre },
        ]);
      } catch (err) {
        setError(err instanceof Error ? err : new Error('Error desconocido'));
      } finally {
        setIsLoading(false);
      }
    };

    fetchBreadcrumb();
  }, [folderId]);

  return { breadcrumb, isLoading, error };
};
