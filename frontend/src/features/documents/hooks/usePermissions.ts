/**
 * usePermissions Hook
 * US-DOC-006: Hook para verificar permisos del usuario actual
 */

import { useMemo } from 'react';
import { useAuth } from '@features/auth/hooks/useAuth';
import {
  canUserWriteToFolder,
  canUserReadFolder,
  extractUserInfo,
} from '../utils/permissionChecker';

/**
 * Interfaz de retorno del hook
 */
export interface UsePermissionsReturn {
  canWrite: boolean;
  canRead: boolean;
  userId: string | null;
  userEmail: string | null;
  isLoading: boolean;
}

/**
 * Hook para verificar permisos del usuario en una carpeta
 * @param folderId - ID de la carpeta a verificar
 */
export function usePermissions(folderId: string): UsePermissionsReturn {
  const { token, isAuthenticated } = useAuth();

  // Extraer información del usuario desde el token
  const userInfo = useMemo(() => {
    if (!token) return null;
    return extractUserInfo(token);
  }, [token]);

  // Verificar permisos de escritura y lectura
  const canWrite = useMemo(() => {
    if (!token || !isAuthenticated) return false;
    return canUserWriteToFolder(token, folderId);
  }, [token, isAuthenticated, folderId]);

  const canRead = useMemo(() => {
    if (!token || !isAuthenticated) return false;
    return canUserReadFolder(token, folderId);
  }, [token, isAuthenticated, folderId]);

  return {
    canWrite,
    canRead,
    userId: userInfo?.id || null,
    userEmail: userInfo?.email || null,
    isLoading: false, // El token se obtiene sincrónicamente del store
  };
}
