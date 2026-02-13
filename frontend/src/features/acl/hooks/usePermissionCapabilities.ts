/**
 * Hook to evaluate permission capabilities for a given context
 */

import { useMemo } from 'react';
import { useMiPermisoDocumento } from './useMiPermisoDocumento';
import { useMiPermisoCarpeta } from './useMiPermisoCarpeta';
import { getCapabilitiesFromLevel } from '../utils/permissionEvaluator';
import type {
  CodigoNivelAcceso,
  ICapabilities,
  IPermissionContext,
} from '../types';

export interface UsePermissionCapabilitiesResult {
  capabilities: ICapabilities;
  nivelAcceso: CodigoNivelAcceso | null;
  origen: 'documento' | 'carpeta' | 'ninguno';
  hasAnyPermission: boolean;
  isLoading: boolean;
  error: string | null;
  refreshPermissions: () => Promise<void>;
}

const EMPTY_CAPABILITIES = getCapabilitiesFromLevel(null);

export function usePermissionCapabilities(
  context: IPermissionContext
): UsePermissionCapabilitiesResult {
  const isDocumento = context.entityType === 'documento';
  const isCarpeta = context.entityType === 'carpeta';

  const {
    permiso: permisoDocumento,
    loading: documentoLoading,
    error: documentoError,
    refetch: refetchDocumento,
  } = useMiPermisoDocumento(isDocumento ? context.entityId : 0);

  const {
    permiso: permisoCarpeta,
    loading: carpetaLoading,
    error: carpetaError,
    refetch: refetchCarpeta,
  } = useMiPermisoCarpeta(isCarpeta ? context.entityId : 0);

  const evaluation = useMemo(() => {
    if (!context.usuarioId) {
      return {
        nivelAcceso: null as CodigoNivelAcceso | null,
        origen: 'ninguno' as 'documento' | 'carpeta' | 'ninguno',
      };
    }

    if (isDocumento) {
      return {
        nivelAcceso: permisoDocumento?.nivel_acceso ?? null,
        origen: (permisoDocumento?.nivel_acceso ? 'documento' : 'ninguno') as 'documento' | 'carpeta' | 'ninguno',
      };
    }

    if (isCarpeta) {
      return {
        nivelAcceso: permisoCarpeta?.nivel_acceso ?? null,
        origen: (permisoCarpeta?.nivel_acceso ? 'carpeta' : 'ninguno') as 'documento' | 'carpeta' | 'ninguno',
      };
    }

    return {
      nivelAcceso: null as CodigoNivelAcceso | null,
      origen: 'ninguno' as 'documento' | 'carpeta' | 'ninguno',
    };
  }, [context.usuarioId, isDocumento, isCarpeta, permisoDocumento?.nivel_acceso, permisoCarpeta?.nivel_acceso]);

  const capabilities = evaluation.nivelAcceso
    ? getCapabilitiesFromLevel(evaluation.nivelAcceso)
    : EMPTY_CAPABILITIES;

  const isLoading = isDocumento ? documentoLoading : carpetaLoading;
  const error = isDocumento ? documentoError : carpetaError;

  return {
    capabilities,
    nivelAcceso: evaluation.nivelAcceso,
    origen: evaluation.origen,
    hasAnyPermission: Boolean(evaluation.nivelAcceso),
    isLoading,
    error,
    refreshPermissions: isDocumento ? refetchDocumento : refetchCarpeta,
  };
}
