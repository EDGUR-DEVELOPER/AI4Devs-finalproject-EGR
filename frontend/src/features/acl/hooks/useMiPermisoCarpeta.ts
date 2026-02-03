/**
 * Custom hook to fetch effective permission for current user on a folder
 * Handles loading and error states for GET /api/carpetas/{id}/mi-permiso
 */

import { useCallback, useEffect, useState } from 'react';
import { aclCarpetaApi } from '../services/aclCarpetaService';
import type { IPermisoEfectivo } from '../types';

export interface UseMiPermisoCarpetaReturn {
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
 * Hook to load effective permission for the current user on a folder
 */
export const useMiPermisoCarpeta = (carpetaId: number): UseMiPermisoCarpetaReturn => {
  const [state, setState] = useState<MiPermisoState>({
    permiso: null,
    loading: false,
    error: null,
  });

  const loadPermiso = useCallback(async () => {
    setState((prev) => ({ ...prev, loading: true, error: null }));
    try {
      const permiso = await aclCarpetaApi.getMiPermiso(carpetaId);
      setState({ permiso, loading: false, error: null });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Error al cargar permiso efectivo';
      setState({ permiso: null, loading: false, error: message });
    }
  }, [carpetaId]);

  useEffect(() => {
    if (carpetaId) {
      loadPermiso();
    }
  }, [carpetaId, loadPermiso]);

  return {
    permiso: state.permiso,
    loading: state.loading,
    error: state.error,
    refetch: loadPermiso,
  };
};
