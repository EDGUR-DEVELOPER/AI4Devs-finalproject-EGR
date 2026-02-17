/**
 * Custom hook to fetch effective permission for current user on a document
 * Handles loading and error states for GET /api/permisos/documentos/{id}/mi-permiso
 */

import { useCallback, useEffect, useState } from 'react';
import { aclDocumentoApi } from '../services/aclDocumentoService';
import type { IPermisoEfectivo } from '../types';

export interface UseMiPermisoDocumentoReturn {
  /** Effective permission for current user (null if not available) */
  permiso: IPermisoEfectivo | null;

  /** Loading state for permission fetch */
  loading: boolean;

  /** Error message if request fails */
  error: string | null;

  /** Manually re-fetch effective permission */
  refetch: () => Promise<void>;
}

interface MiPermisoState {
  permiso: IPermisoEfectivo | null;
  loading: boolean;
  error: string | null;
}

/**
 * Hook to load effective permission for the current user on a document
 */
export const useMiPermisoDocumento = (documentoId: number): UseMiPermisoDocumentoReturn => {
  const [state, setState] = useState<MiPermisoState>({
    permiso: null,
    loading: false,
    error: null,
  });

  const loadPermiso = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true, error: null }));
    try {
      const permiso = await aclDocumentoApi.getMiPermisoDocumento(documentoId);
      setState({ permiso, loading: false, error: null });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Error al cargar permiso efectivo';
      setState({ permiso: null, loading: false, error: message });
    }
  }, [documentoId]);

  useEffect(() => {
    if (documentoId) {
      loadPermiso();
    }
  }, [documentoId, loadPermiso]);

  return {
    permiso: state.permiso,
    loading: state.loading,
    error: state.error,
    refetch: loadPermiso,
  };
};
