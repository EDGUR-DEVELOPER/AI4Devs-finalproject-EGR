/**
 * Hook to evaluate permission capabilities for a given context
 */

import { useMemo } from 'react';
import { useAclDocumento } from './useAclDocumento';
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

  const { acls, loading: documentoLoading, error: documentoError, refresh } = useAclDocumento(
    isDocumento ? context.entityId : 0
  );

  const {
    permiso,
    loading: carpetaLoading,
    error: carpetaError,
    refetch,
  } = useMiPermisoCarpeta(isCarpeta ? context.entityId : 0);

  const evaluation = useMemo(() => {
    if (!context.usuarioId) {
      return {
        nivelAcceso: null as CodigoNivelAcceso | null,
        origen: 'ninguno' as 'documento' | 'carpeta' | 'ninguno',
      };
    }

    if (isDocumento) {
      const userAcl = acls.find((acl) => acl.usuario_id === context.usuarioId) || null;
      return {
        nivelAcceso: userAcl?.nivel_acceso?.codigo ?? null,
        origen: (userAcl ? 'documento' : 'ninguno') as 'documento' | 'carpeta' | 'ninguno',
      };
    }

    if (isCarpeta) {
      return {
        nivelAcceso: permiso?.nivel_acceso ?? null,
        origen: (permiso?.nivel_acceso ? 'carpeta' : 'ninguno') as 'documento' | 'carpeta' | 'ninguno',
      };
    }

    return {
      nivelAcceso: null as CodigoNivelAcceso | null,
      origen: 'ninguno' as 'documento' | 'carpeta' | 'ninguno',
    };
  }, [acls, context.usuarioId, isDocumento, isCarpeta, permiso?.nivel_acceso]);

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
    refreshPermissions: isDocumento ? refresh : refetch,
  };
}
