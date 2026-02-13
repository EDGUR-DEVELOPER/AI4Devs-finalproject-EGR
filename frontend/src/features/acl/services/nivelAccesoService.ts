/**
 * ACL (Access Control List) API service
 * Handles communication with the backend ACL (niveles de acceso) endpoints
 * Manages access level catalog retrieval and validation
 */

import { isAxiosError } from 'axios';
import { apiClient } from '@core/shared/api/axiosInstance';
import type { INivelAcceso, CodigoNivelAcceso, ApiResponse } from '../types';

/**
 * Base endpoint for ACL operations
 * Routes through gateway: /api/doc/** -> document-core /api/**
 */
const ACL_ENDPOINTS = {
  NIVELES: '/doc/acl/niveles',
  NIVEL_BY_CODIGO: (codigo: CodigoNivelAcceso) => `/doc/acl/niveles/${codigo}`,
} as const;

/**
 * ACL API service object
 * Provides methods for fetching access levels from the backend
 */
export const aclApi = {
  /**
   * Fetch all active access levels from the backend
   * Returns the three standard access levels: LECTURA, ESCRITURA, ADMINISTRACION
   *
   * @returns Array of INivelAcceso objects
   * @throws Error if the API request fails
   *
   * @example
   * const niveles = await aclApi.getNivelesAcceso();
   * console.log(niveles.length); // 3
   */
  getNivelesAcceso: async (): Promise<INivelAcceso[]> => {
    try {
      const response = await apiClient.get<ApiResponse<INivelAcceso[]>>(
        ACL_ENDPOINTS.NIVELES
      );
      return response.data.data;
    } catch (error: unknown) {
      if (isAxiosError(error)) {
        if (error.response?.status === 404) {
          throw new Error('Los niveles de acceso no fueron encontrados');
        }
        if (error.response?.status === 500) {
          throw new Error('Error del servidor al cargar niveles de acceso');
        }
        const message =
          error.response?.data?.message ||
          'Error al cargar niveles de acceso';
        throw new Error(message);
      }
      throw new Error('Error de conexión. Intenta de nuevo más tarde.');
    }
  },

  /**
   * Fetch a specific access level by its codigo
   * Validates that the access level exists and is active
   *
   * @param codigo - Access level code (LECTURA, ESCRITURA, or ADMINISTRACION)
   * @returns Single INivelAcceso object
   * @throws Error if the codigo is invalid or API request fails
   *
   * @example
   * const lecturaLevel = await aclApi.getNivelAccesoByCodigo('LECTURA');
   * console.log(lecturaLevel.nombre); // "Lectura / Consulta"
   */
  getNivelAccesoByCodigo: async (
    codigo: CodigoNivelAcceso
  ): Promise<INivelAcceso> => {
    try {
      const response = await apiClient.get<ApiResponse<INivelAcceso>>(
        ACL_ENDPOINTS.NIVEL_BY_CODIGO(codigo)
      );
      return response.data.data;
    } catch (error: unknown) {
      if (isAxiosError(error)) {
        if (error.response?.status === 404) {
          throw new Error(
            `Nivel de acceso '${codigo}' no encontrado en el catálogo`
          );
        }
        if (error.response?.status === 400) {
          throw new Error(`Código de nivel inválido: ${codigo}`);
        }
        const message =
          error.response?.data?.message ||
          `Error al cargar el nivel '${codigo}'`;
        throw new Error(message);
      }
      throw new Error('Error de conexión. Intenta de nuevo más tarde.');
    }
  },
};
