/**
 * Custom React hook for managing document ACL (Access Control List)
 * Handles state management for document permissions including create, update, revoke operations
 * Provides optimistic updates and comprehensive error handling
 */

import { useState, useCallback, useEffect } from 'react';
import { aclDocumentoApi } from '../services/aclDocumentoService';
import type { IAclDocumento, CreateAclDocumentoDTO } from '../types';

/**
 * Interface for the hook's return value
 */
export interface UseAclDocumentoReturn {
  /** Array of ACL records for the current document */
  acls: IAclDocumento[];

  /** Loading state during data fetch */
  loading: boolean;

  /** Error message from API or processing (null if no error) */
  error: string | null;

  /** Create or update ACL record (upsert) */
  createOrUpdateAcl: (payload: CreateAclDocumentoDTO) => Promise<IAclDocumento>;

  /** Revoke (delete) ACL record for a user */
  revokeAcl: (usuarioId: number) => Promise<void>;

  /** Manually refresh ACL list from API */
  refresh: () => Promise<void>;

  /** Clear current error message */
  clearError: () => void;
}

/**
 * Internal state interface
 */
interface AclState {
  acls: IAclDocumento[];
  loading: boolean;
  error: string | null;
}

/**
 * Custom hook to manage document ACL with CRUD operations
 *
 * Features:
 * - Automatic loading of ACLs on mount
 * - Optimistic updates (update UI immediately)
 * - Comprehensive error handling with user-friendly Spanish messages
 * - Manual refresh capability
 * - State cleanup and error clearing
 * - Upsert behavior: creates or updates based on backend logic
 *
 * @param documentoId - The document ID to manage ACLs for
 * @returns Object with ACL data, loading state, operations, and error handling
 *
 * @example
 * const { acls, loading, error, createOrUpdateAcl } = useAclDocumento(123);
 *
 * // Create or update permission
 * await createOrUpdateAcl({
 *   usuario_id: 456,
 *   nivel_acceso_codigo: 'LECTURA',
 *   fecha_expiracion: '2026-12-31T23:59:59Z',
 * });
 *
 * // Revoke permission
 * await revokeAcl(456);
 */
export const useAclDocumento = (documentoId: number): UseAclDocumentoReturn => {
  const [state, setState] = useState<AclState>({
    acls: [],
    loading: false,
    error: null,
  });

  /**
   * Fetch all ACLs for the current document
   * Updates component state with results or error
   */
  const loadAcls = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true, error: null }));
    try {
      const data = await aclDocumentoApi.list(documentoId);
      setState((prev) => ({
        ...prev,
        acls: data,
        loading: false,
      }));
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Error al cargar permisos del documento';
      setState((prev) => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
    }
  }, [documentoId]);

  /**
   * Create or update ACL record (upsert behavior)
   * Optimistically updates UI, then syncs with API
   * Backend automatically creates if not exists, updates if exists
   */
  const createOrUpdateAclLocal = useCallback(
    async (payload: CreateAclDocumentoDTO): Promise<IAclDocumento> => {
      setState((prev) => ({ ...prev, error: null }));
      
      try {
        const newAcl = await aclDocumentoApi.createOrUpdate(documentoId, payload);
        
        // Update local state: replace if exists, add if new
        setState((prev) => {
          const existingIndex = prev.acls.findIndex(
            (acl) => acl.usuario_id === newAcl.usuario_id
          );

          if (existingIndex >= 0) {
            // Update existing
            const updatedAcls = [...prev.acls];
            updatedAcls[existingIndex] = newAcl;
            return { ...prev, acls: updatedAcls };
          } else {
            // Add new
            return { ...prev, acls: [...prev.acls, newAcl] };
          }
        });

        return newAcl;
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Error al asignar permiso';
        setState((prev) => ({ ...prev, error: errorMessage }));
        throw err;
      }
    },
    [documentoId]
  );

  /**
   * Revoke (delete) ACL record for a user
   * Optimistically removes from UI, then syncs with API
   */
  const revokeAclLocal = useCallback(
    async (usuarioId: number): Promise<void> => {
      setState((prev) => ({ ...prev, error: null }));

      // Store previous state for rollback
      const previousAcls = state.acls;

      try {
        // Optimistic update: remove from UI immediately
        setState((prev) => ({
          ...prev,
          acls: prev.acls.filter((acl) => acl.usuario_id !== usuarioId),
        }));

        await aclDocumentoApi.revoke(documentoId, usuarioId);
      } catch (err) {
        // Rollback on error
        setState((prev) => ({
          ...prev,
          acls: previousAcls,
          error: err instanceof Error ? err.message : 'Error al revocar permiso',
        }));
        throw err;
      }
    },
    [documentoId, state.acls]
  );

  /**
   * Manually refresh ACL list from API
   * Useful after external changes or to retry after errors
   */
  const refreshLocal = useCallback(async () => {
    await loadAcls();
  }, [loadAcls]);

  /**
   * Clear current error message
   * Useful for dismissing error alerts
   */
  const clearErrorLocal = useCallback(() => {
    setState((prev) => ({ ...prev, error: null }));
  }, []);

  /**
   * Auto-load ACLs when component mounts or documentoId changes
   */
  useEffect(() => {
    if (documentoId) {
      loadAcls();
    }
  }, [documentoId, loadAcls]);

  return {
    acls: state.acls,
    loading: state.loading,
    error: state.error,
    createOrUpdateAcl: createOrUpdateAclLocal,
    revokeAcl: revokeAclLocal,
    refresh: refreshLocal,
    clearError: clearErrorLocal,
  };
};
