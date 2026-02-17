/**
 * Custom React hook for managing folder ACL (Access Control List)
 * Handles state management for folder permissions including create, read, update, delete operations
 * Provides optimistic updates and comprehensive error handling
 */

import { useState, useCallback, useEffect } from 'react';
import { aclCarpetaApi } from '../services/aclCarpetaService';
import type { IAclCarpeta, CreateAclCarpetaDTO, UpdateAclCarpetaDTO } from '../types';

/**
 * Interface for the hook's return value
 */
export interface UseAclCarpetaReturn {
  /** Array of ACL records for the current folder */
  acls: IAclCarpeta[];

  /** Loading state during data fetch */
  loading: boolean;

  /** Error message from API or processing (null if no error) */
  error: string | null;

  /** Create new ACL record */
  createAcl: (payload: CreateAclCarpetaDTO) => Promise<void>;

  /** Update existing ACL record */
  updateAcl: (usuarioId: number, payload: UpdateAclCarpetaDTO) => Promise<void>;

  /** Delete ACL record for a user */
  deleteAcl: (usuarioId: number) => Promise<void>;

  /** Manually refresh ACL list from API */
  refetch: () => Promise<void>;

  /** Clear current error message */
  clearError: () => void;
}

/**
 * Internal state interface
 */
interface AclState {
  acls: IAclCarpeta[];
  loading: boolean;
  error: string | null;
}

/**
 * Custom hook to manage folder ACL with CRUD operations
 *
 * Features:
 * - Automatic loading of ACLs on mount
 * - Optimistic updates (update UI before API response)
 * - Comprehensive error handling with user-friendly messages
 * - Manual refresh capability
 * - State cleanup and error clearing
 *
 * @param carpetaId - The folder ID to manage ACLs for
 * @param autoLoad - Whether to auto-load ACLs on mount (default: true)
 * @returns Object with ACL data, loading state, operations, and error handling
 *
 * @example
 * const { acls, loading, error, createAcl } = useAclCarpeta(123);
 *
 * // Create new permission
 * await createAcl({
 *   usuario_id: 456,
 *   nivel_acceso_codigo: 'LECTURA',
 *   recursivo: false,
 * });
 *
 * // Update permission
 * await updateAcl(456, {
 *   nivel_acceso_codigo: 'ESCRITURA',
 *   recursivo: true,
 * });
 *
 * // Delete permission
 * await deleteAcl(456);
 */
export const useAclCarpeta = (
  carpetaId: number,
  autoLoad: boolean = true
): UseAclCarpetaReturn => {
  const [state, setState] = useState<AclState>({
    acls: [],
    loading: false,
    error: null,
  });

  /**
   * Fetch all ACLs for the current folder
   * Updates component state with results or error
   */
  const loadAcls = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true, error: null }));
    try {
      const data = await aclCarpetaApi.listAcls(carpetaId);
      setState((prev) => ({
        ...prev,
        acls: data,
        loading: false,
      }));
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Error al cargar permisos';
      setState((prev) => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
    }
  }, [carpetaId]);

  /**
   * Create new ACL record
   * Optimistically updates UI, then syncs with API
   */
  const createAclLocal = useCallback(
    async (payload: CreateAclCarpetaDTO) => {
      setState((prev) => ({ ...prev, error: null }));
      try {
        const newAcl = await aclCarpetaApi.createAcl(carpetaId, payload);
        setState((prev) => ({
          ...prev,
          acls: [...prev.acls, newAcl],
        }));
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Error al crear permiso';
        setState((prev) => ({
          ...prev,
          error: errorMessage,
        }));
        throw err; // Re-throw to allow caller to handle
      }
    },
    [carpetaId]
  );

  /**
   * Update existing ACL record
   * Optimistically updates UI, then syncs with API
   */
  const updateAclLocal = useCallback(
    async (usuarioId: number, payload: UpdateAclCarpetaDTO) => {
      setState((prev) => ({ ...prev, error: null }));
      try {
        const updatedAcl = await aclCarpetaApi.updateAcl(
          carpetaId,
          usuarioId,
          payload
        );

        setState((prev) => ({
          ...prev,
          acls: prev.acls.map((acl) =>
            acl.usuario_id === usuarioId ? updatedAcl : acl
          ),
        }));
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : 'Error al actualizar permiso';
        setState((prev) => ({
          ...prev,
          error: errorMessage,
        }));
        throw err;
      }
    },
    [carpetaId]
  );

  /**
   * Delete ACL record
   * Optimistically removes from UI, then syncs with API
   */
  const deleteAclLocal = useCallback(
    async (usuarioId: number) => {
      // Optimistic update: remove from UI first
      setState((prev) => ({
        ...prev,
        error: null,
        acls: prev.acls.filter((acl) => acl.usuario_id !== usuarioId),
      }));

      try {
        await aclCarpetaApi.deleteAcl(carpetaId, usuarioId);
      } catch (err) {
        // Restore on error
        const errorMessage =
          err instanceof Error ? err.message : 'Error al eliminar permiso';
        await loadAcls(); // Reload to restore state
        setState((prev) => ({
          ...prev,
          error: errorMessage,
        }));
        throw err;
      }
    },
    [carpetaId, loadAcls]
  );

  /**
   * Clear error message
   */
  const clearError = useCallback(() => {
    setState((prev) => ({
      ...prev,
      error: null,
    }));
  }, []);

  /**
   * Auto-load ACLs on mount
   */
  useEffect(() => {
    if (autoLoad) {
      loadAcls();
    }
  }, [autoLoad, loadAcls]);

  return {
    acls: state.acls,
    loading: state.loading,
    error: state.error,
    createAcl: createAclLocal,
    updateAcl: updateAclLocal,
    deleteAcl: deleteAclLocal,
    refetch: loadAcls,
    clearError,
  };
};
