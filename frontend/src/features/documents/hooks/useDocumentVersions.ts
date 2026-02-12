/**
 * useDocumentVersions Hook
 * US-DOC-006: Gestión de historial de versiones con paginación
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { getDocumentVersions } from '../api/documentService';
import type { DocumentVersionDTO } from '../types/document.types';
import { getErrorDetails } from '../utils/errorMapper';

/**
 * Interfaz de retorno del hook
 */
export interface UseDocumentVersionsReturn {
  versions: DocumentVersionDTO[];
  currentPage: number;
  totalPages: number;
  isLoading: boolean;
  error: string | null;
  fetchVersions: (page: number) => Promise<void>;
  invalidateCache: () => void;
  goToPage: (page: number) => void;
}

/**
 * Hook para gestionar el historial de versiones de un documento
 * @param documentId - ID del documento
 * @param isOpen - Si el modal/panel está abierto (para auto-refresh)
 */
export function useDocumentVersions(
  documentId: string,
  isOpen: boolean = false
): UseDocumentVersionsReturn {
  // Estados
  const [versions, setVersions] = useState<DocumentVersionDTO[]>([]);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Ref para auto-refresh
  const refreshIntervalRef = useRef<number | null>(null);

  /**
   * Obtiene las versiones de una página específica
   */
  const fetchVersions = useCallback(
    async (page: number = 0) => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await getDocumentVersions(documentId, page, 20);
        setVersions(response.content);
        setCurrentPage(response.pageNumber);
        setTotalPages(response.totalPages);
      } catch (err) {
        const errorDetails = getErrorDetails(err);
        setError(errorDetails.message);
      } finally {
        setIsLoading(false);
      }
    },
    [documentId]
  );

  /**
   * Invalida el caché y recarga la página actual
   */
  const invalidateCache = useCallback(() => {
    fetchVersions(currentPage);
  }, [fetchVersions, currentPage]);

  /**
   * Navega a una página específica
   */
  const goToPage = useCallback(
    (page: number) => {
      if (page >= 0 && page < totalPages) {
        fetchVersions(page);
      }
    },
    [fetchVersions, totalPages]
  );

  // Cargar versiones inicial cuando el modal se abre
  useEffect(() => {
    if (isOpen && documentId) {
      fetchVersions(0);
    }
  }, [isOpen, documentId, fetchVersions]);

  // Auto-refresh cada 3 minutos si el modal está abierto
  useEffect(() => {
    if (isOpen) {
      // Configurar intervalo de refresh
      refreshIntervalRef.current = setInterval(() => {
        fetchVersions(currentPage);
      }, 3 * 60 * 1000); // 3 minutos

      // Limpiar al cerrar
      return () => {
        if (refreshIntervalRef.current) {
          clearInterval(refreshIntervalRef.current);
          refreshIntervalRef.current = null;
        }
      };
    }
  }, [isOpen, currentPage, fetchVersions]);

  return {
    versions,
    currentPage,
    totalPages,
    isLoading,
    error,
    fetchVersions,
    invalidateCache,
    goToPage,
  };
}
