/**
 * Document ACL API service
 * Handles all HTTP operations for document-level user permissions (ACL)
 * Manages creating, updating, and revoking explicit user permissions on documents
 */

import { isAxiosError } from 'axios';
import { apiClient } from '@core/shared/api/axiosInstance';
import type {
  IAclDocumento,
  CreateAclDocumentoDTO,
  IPermisoEfectivo,
  AclDocumentoApiResponse,
  ListAclDocumentoApiResponse,
  AclErrorResponse,
} from '../types';

/**
 * Base endpoint templates for document ACL operations
 * All routes go through gateway: /api/doc/** -> document-core /api/**
 */
const ACL_DOCUMENTO_ENDPOINTS = {
  LIST: (documentoId: number) => `/doc/documentos/${documentoId}/permisos`,
  CREATE: (documentoId: number) => `/doc/documentos/${documentoId}/permisos`,
  REVOKE: (documentoId: number, usuarioId: number) =>
    `/doc/documentos/${documentoId}/permisos/${usuarioId}`,
  MI_PERMISO: (documentoId: number) => `/doc/permisos/documentos/${documentoId}/mi-permiso`,
} as const;

/**
 * Centralized error message extraction from API responses
 * Converts Axios errors to user-friendly Spanish messages
 *
 * @param error - The error object (Axios or generic)
 * @returns User-friendly error message in Spanish
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
        return 'No tienes permiso para administrar este documento.';
      case 404:
        return 'Documento o usuario no encontrado.';
      case 409:
        return 'Ya existe un permiso para este usuario en el documento.';
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
 * Document ACL API service
 * Provides methods for managing explicit user permissions on documents
 */
export const aclDocumentoApi = {
  /**
   * Fetch effective permission for the current user on a document
   * Returns direct or inherited permission details
   *
   * @param documentoId - The document ID
   * @returns IPermisoEfectivo
   * @throws Error with user-friendly message if request fails
   */
  getMiPermisoDocumento: async (documentoId: number): Promise<IPermisoEfectivo> => {
    try {
      const response = await apiClient.get<IPermisoEfectivo>(
        ACL_DOCUMENTO_ENDPOINTS.MI_PERMISO(documentoId)
      );
      return response.data;
    } catch (error: unknown) {
      const message = extractErrorMessage(error);
      throw new Error(message);
    }
  },

  /**
   * Fetch all explicit ACLs for a specific document
   * Returns the list of user permissions granted on this document
   *
   * @param documentoId - The document ID to fetch permissions for
   * @returns Promise<IAclDocumento[]> - Array of document permissions
   * @throws Error with user-friendly message if request fails
   */
  list: async (documentoId: number): Promise<IAclDocumento[]> => {
    try {
      const response = await apiClient.get<ListAclDocumentoApiResponse>(
        ACL_DOCUMENTO_ENDPOINTS.LIST(documentoId)
      );
      return response.data.permisos;
    } catch (error: unknown) {
      const message = extractErrorMessage(error);
      throw new Error(`Error al cargar permisos del documento: ${message}`);
    }
  },

  /**
   * Create or update a document permission (upsert behavior)
   * Backend automatically creates new or updates existing based on (documento_id, usuario_id)
   * Returns 201 if created, 200 if updated
   *
   * @param documentoId - The document ID
   * @param payload - Permission data (usuario_id, nivel_acceso_codigo, fecha_expiracion)
   * @returns Promise<IAclDocumento> - Created or updated permission record
   * @throws Error with user-friendly message if request fails
   */
  createOrUpdate: async (
    documentoId: number,
    payload: CreateAclDocumentoDTO
  ): Promise<IAclDocumento> => {
    try {
      const response = await apiClient.post<AclDocumentoApiResponse>(
        ACL_DOCUMENTO_ENDPOINTS.CREATE(documentoId),
        payload
      );
      return response.data;
    } catch (error: unknown) {
      const message = extractErrorMessage(error);
      throw new Error(`Error al asignar permiso: ${message}`);
    }
  },

  /**
   * Revoke (delete) a document permission
   * Removes explicit permission for a specific user on a document
   * Returns 204 No Content on success
   *
   * @param documentoId - The document ID
   * @param usuarioId - The user ID whose permission will be revoked
   * @returns Promise<void>
   * @throws Error with user-friendly message if request fails
   */
  revoke: async (documentoId: number, usuarioId: number): Promise<void> => {
    try {
      await apiClient.delete(
        ACL_DOCUMENTO_ENDPOINTS.REVOKE(documentoId, usuarioId)
      );
    } catch (error: unknown) {
      const message = extractErrorMessage(error);
      throw new Error(`Error al revocar permiso: ${message}`);
    }
  },
};
