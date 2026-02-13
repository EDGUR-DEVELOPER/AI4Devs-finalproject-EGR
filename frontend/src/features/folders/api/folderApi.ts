/**
 * Folder API service
 * Private API client for folder operations (US-FOLDER-001, US-FOLDER-002, US-FOLDER-004)
 * Consumes endpoints from document-core service via gateway at /api/doc/**
 */
import { apiClient } from '@core/shared/api/axiosInstance';
import type {
  FolderContent,
  BreadcrumbSegment,
  CreateFolderRequest,
  CreateFolderResponse,
} from '../types/folder.types';

const BASE_URL = '/doc/carpetas'; // Via gateway: /api/doc/carpetas -> document-core service

/**
 * Obtener carpeta raíz de la organización
 * GET /api/carpetas/raiz
 */
export const getRootFolder = async (): Promise<{ id: number }> => {
  const { data } = await apiClient.get<{ id: number }>(`${BASE_URL}/raiz`);
  return data;
};

/**
 * Obtener contenido de carpeta raíz (subcarpetas + documentos)  
 * @deprecated Use getRootFolder() y luego getFolderContent() con el ID
 */
export const getRootContent = async (): Promise<FolderContent> => {
  // Fallback: obtener raíz y luego su contenido
  const root = await getRootFolder();
  return getFolderContent(root.id.toString());
};

/**
 * Obtener contenido de carpeta específica
 * GET /api/carpetas/{id}/contenido
 */
export const getFolderContent = async (folderId: string): Promise<FolderContent> => {
  const { data } = await apiClient.get<FolderContent>(
    `${BASE_URL}/${folderId}/contenido`
  );
  return data;
};

/**
 * Obtener ruta de navegación (breadcrumb) de una carpeta
 * GET /api/carpetas/{id}/ruta
 */
export const getFolderPath = async (folderId: string): Promise<BreadcrumbSegment[]> => {
  const { data } = await apiClient.get<BreadcrumbSegment[]>(
    `${BASE_URL}/${folderId}/ruta`
  );
  return data;
};

/**
 * Crear nueva carpeta
 * POST /api/carpetas
 */
export const createFolder = async (
  request: CreateFolderRequest
): Promise<CreateFolderResponse> => {
  const { data } = await apiClient.post<CreateFolderResponse>(BASE_URL, request);
  return data;
};

/**
 * Eliminar carpeta vacía (soft delete)
 * DELETE /api/carpetas/{id}
 */
export const deleteFolder = async (folderId: string): Promise<void> => {
  await apiClient.delete(`${BASE_URL}/${folderId}`);
};

// Export default para conveniencia
export const folderApi = {
  getRootFolder,
  getRootContent,
  getFolderContent,
  getFolderPath,
  createFolder,
  deleteFolder,
};

export default folderApi;
