/**
 * Folder ACL API service
 * Handles all HTTP operations for folder-level user permissions (ACL)
 * Manages creating, retrieving, updating, and deleting explicit user permissions on folders
 */

import { isAxiosError } from 'axios';
import { apiClient } from '@core/shared/api/axiosInstance';
import type {
  IAclCarpeta,
  CreateAclCarpetaDTO,
  UpdateAclCarpetaDTO,
  IPermisoEfectivo,
  AclCarpetaApiResponse,
  ListAclCarpetaApiResponse,
  AclErrorResponse,
} from '../types';

/**
 * Base endpoint templates for folder ACL operations
 */
const ACL_CARPETA_ENDPOINTS = {
  LIST: (carpetaId: number) => `/api/carpetas/${carpetaId}/permisos`,
  CREATE: (carpetaId: number) => `/api/carpetas/${carpetaId}/permisos`,
  UPDATE: (carpetaId: number, usuarioId: number) =>
    `/api/carpetas/${carpetaId}/permisos/${usuarioId}`,
  DELETE: (carpetaId: number, usuarioId: number) =>
    `/api/carpetas/${carpetaId}/permisos/${usuarioId}`,
  MI_PERMISO: (carpetaId: number) => `/api/carpetas/${carpetaId}/mi-permiso`,
} as const;

/**
 * Centralized error message extraction from API responses
 * Converts Axios errors to user-friendly messages
 *
 * @param error - The error object (Axios or generic)
 * @returns User-friendly error message
 */
const extractErrorMessage = (error: unknown): string => {
  if (isAxiosError(error)) {
    // Try to extract error from response body
    const data = error.response?.data as AclErrorResponse | undefined;
    if (data?.error?.message) {
      return data.error.message;
    }

    // Status-specific error messages
    switch (error.response?.status) {
      case 400:
        return 'Solicitud inválida. Verifica los datos ingresados.';
      case 403:
        return 'No tienes permiso para realizar esta acción.';
      case 404:
        return 'Recurso no encontrado (carpeta, usuario o permiso).';
      case 409:
        return 'Este usuario ya tiene un permiso en la carpeta.';
      case 500:
        return 'Error del servidor. Intenta de nuevo más tarde.';
      default:
        return error.message || 'Error desconocido. Intenta de nuevo.';
    }
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Error desconocido. Intenta de nuevo más tarde.';
};

/**
 * Folder ACL API service
 * Provides methods for managing explicit user permissions on folders
 */
export const aclCarpetaApi = {
  /**
   * Fetch effective permission for the current user on a folder
   * Returns direct or inherited permission details
   *
   * @param carpetaId - The folder ID
   * @returns IPermisoEfectivo
   * @throws Error with user-friendly message if request fails
   */
  getMiPermiso: async (carpetaId: number): Promise<IPermisoEfectivo> => {
    try {
      const response = await apiClient.get<IPermisoEfectivo>(
        ACL_CARPETA_ENDPOINTS.MI_PERMISO(carpetaId)
      );
      return response.data;
    } catch (error: unknown) {
      const message = extractErrorMessage(error);
      throw new Error(message);
    }
  },
  /**
   * Fetch all explicit ACLs for a specific folder
   * Returns the list of user permissions granted on this folder
   *
   * @param carpetaId - The folder ID to fetch permissions for
   * @returns Array of IAclCarpeta objects
   * @throws Error with user-friendly message if request fails
   *
   * @example
   * const permisos = await aclCarpetaApi.listAcls(123);
   * console.log(permisos.length); // Number of users with permissions
   */
  listAcls: async (carpetaId: number): Promise<IAclCarpeta[]> => {
    try {
      const response = await apiClient.get<ListAclCarpetaApiResponse>(
        ACL_CARPETA_ENDPOINTS.LIST(carpetaId)
      );
      return response.data.data;
    } catch (error: unknown) {
      const message = extractErrorMessage(error);
      throw new Error(message);
    }
  },

  /**
   * Create a new explicit ACL for a user on a folder
   * Grants a specific access level to a user on a folder
   *
   * @param carpetaId - The folder ID
   * @param payload - CreateAclCarpetaDTO with usuario_id, nivel_acceso_codigo, recursivo
   * @returns The created IAclCarpeta record
   * @throws Error with user-friendly message if request fails
   *
   * @example
   * const nuevoPermiso = await aclCarpetaApi.createAcl(123, {
   *   usuario_id: 456,
   *   nivel_acceso_codigo: 'LECTURA',
   *   recursivo: false,
   * });
   */
  createAcl: async (
    carpetaId: number,
    payload: CreateAclCarpetaDTO
  ): Promise<IAclCarpeta> => {
    try {
      const response = await apiClient.post<AclCarpetaApiResponse>(
        ACL_CARPETA_ENDPOINTS.CREATE(carpetaId),
        payload
      );
      return response.data.data;
    } catch (error: unknown) {
      const message = extractErrorMessage(error);
      throw new Error(message);
    }
  },

  /**
   * Update an existing ACL for a user on a folder
   * Modifies access level or recursive flag
   *
   * @param carpetaId - The folder ID
   * @param usuarioId - The user ID whose permission to update
   * @param payload - UpdateAclCarpetaDTO with nivel_acceso_codigo and optional recursivo
   * @returns The updated IAclCarpeta record
   * @throws Error with user-friendly message if request fails
   *
   * @example
   * const permisoActualizado = await aclCarpetaApi.updateAcl(123, 456, {
   *   nivel_acceso_codigo: 'ESCRITURA',
   *   recursivo: true,
   * });
   */
  updateAcl: async (
    carpetaId: number,
    usuarioId: number,
    payload: UpdateAclCarpetaDTO
  ): Promise<IAclCarpeta> => {
    try {
      const response = await apiClient.patch<AclCarpetaApiResponse>(
        ACL_CARPETA_ENDPOINTS.UPDATE(carpetaId, usuarioId),
        payload
      );
      return response.data.data;
    } catch (error: unknown) {
      const message = extractErrorMessage(error);
      throw new Error(message);
    }
  },

  /**
   * Delete an explicit ACL for a user on a folder
   * Revokes all permissions this user has on this folder
   *
   * @param carpetaId - The folder ID
   * @param usuarioId - The user ID whose permission to revoke
   * @returns void (no response body expected)
   * @throws Error with user-friendly message if request fails
   *
   * @example
   * await aclCarpetaApi.deleteAcl(123, 456);
   * console.log('Permiso eliminado');
   */
  deleteAcl: async (carpetaId: number, usuarioId: number): Promise<void> => {
    try {
      await apiClient.delete(
        ACL_CARPETA_ENDPOINTS.DELETE(carpetaId, usuarioId)
      );
    } catch (error: unknown) {
      const message = extractErrorMessage(error);
      throw new Error(message);
    }
  },
};
