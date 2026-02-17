/**
 * Document Service
 * US-DOC-006: Servicios de API para operaciones con documentos
 */

import { apiClient } from '@core/shared/api/axiosInstance';
import type {
  DocumentDTO,
  DocumentVersionDTO,
  DocumentVersionListResponse,
} from '../types/document.types';
import type { UploadProgress } from '../types/upload.types';
import { validateFile } from '../utils/fileValidator';

/**
 * Callback para reportar progreso de upload
 */
export type ProgressCallback = (progress: UploadProgress) => void;

/**
 * Sube un documento a una carpeta
 * @param folderId - ID de la carpeta destino
 * @param file - Archivo a subir
 * @param onProgress - Callback opcional para reportar progreso
 * @returns Documento creado
 */
export async function uploadDocument(
  folderId: string,
  file: File,
  onProgress?: ProgressCallback
): Promise<DocumentDTO> {
  // Validación de cliente
  const validation = validateFile(file);
  if (!validation.valid) {
    throw new Error(validation.error);
  }

  // Crear FormData para envío multipart
  const formData = new FormData();
  formData.append('file', file);

  // Configurar progreso
  const config = {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent: any) => {
      if (onProgress && progressEvent.total) {
        const percentage = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        );

        onProgress({
          state: percentage < 100 ? 'uploading' : 'processing',
          percentage,
          bytesUploaded: progressEvent.loaded,
          bytesTotal: progressEvent.total,
          fileName: file.name,
        });
      }
    },
  };

  const response = await apiClient.post<DocumentDTO>(
    `/doc/v1/folders/${folderId}/documents`,
    formData,
    config
  );

  return response.data;
}

/**
 * Obtiene metadatos de un documento
 * @param documentId - ID del documento
 * @returns Metadatos del documento
 */
export async function getDocumentMetadata(
  documentId: string
): Promise<DocumentDTO> {
  const response = await apiClient.get<DocumentDTO>(
    `/doc/documentos/${documentId}`
  );
  return response.data;
}

/**
 * Obtiene el historial de versiones de un documento
 * @param documentId - ID del documento
 * @param page - Número de página (0-indexed)
 * @param size - Tamaño de página
 * @returns Lista paginada de versiones
 */
export async function getDocumentVersions(
  documentId: string,
  page: number = 0,
  size: number = 20
): Promise<DocumentVersionListResponse> {
  const response = await apiClient.get<DocumentVersionListResponse>(
    `/doc/documentos/${documentId}/versiones`,
    {
      params: { page, size },
    }
  );
  return response.data;
}

/**
 * Descarga una versión específica de un documento
 * @param documentId - ID del documento
 * @param versionId - ID de la versión
 * @param inline - true para visualizar en navegador, false para descargar
 * @returns Blob del archivo
 */
export async function downloadDocumentVersion(
  documentId: string,
  versionId: string,
  inline: boolean = false
): Promise<Blob> {
  const response = await apiClient.get<Blob>(
    `/doc/documentos/${documentId}/versiones/${versionId}/download`,
    {
      params: { inline },
      responseType: 'blob',
    }
  );
  return response.data;
}

/**
 * Sube una nueva versión de un documento existente
 * @param documentId - ID del documento
 * @param file - Archivo de la nueva versión
 * @param description - Descripción opcional de cambios
 * @param onProgress - Callback opcional para reportar progreso
 * @returns Nueva versión creada
 */
export async function uploadNewVersion(
  documentId: string,
  file: File,
  description?: string,
  onProgress?: ProgressCallback
): Promise<DocumentVersionDTO> {
  // Validación de cliente
  const validation = validateFile(file);
  if (!validation.valid) {
    throw new Error(validation.error);
  }

  // Crear FormData
  const formData = new FormData();
  formData.append('file', file);
  if (description) {
    formData.append('description', description);
  }

  // Configurar progreso
  const config = {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent: any) => {
      if (onProgress && progressEvent.total) {
        const percentage = Math.round(
          (progressEvent.loaded * 100) / progressEvent.total
        );

        onProgress({
          state: percentage < 100 ? 'uploading' : 'processing',
          percentage,
          bytesUploaded: progressEvent.loaded,
          bytesTotal: progressEvent.total,
          fileName: file.name,
        });
      }
    },
  };

  const response = await apiClient.post<DocumentVersionDTO>(
    `/doc/documentos/${documentId}/versiones`,
    formData,
    config
  );

  return response.data;
}

/**
 * Descarga la versión actual de un documento
 * 
 * US-DOC-007: Descarga directo desde lista de documentos
 * 
 * @param documentId - ID del documento a descargar
 * @returns Blob del archivo para descarga
 * @throws Error si falla la descarga (403, 404, 500, etc.)
 * 
 * @example
 * const blob = await downloadCurrentDocument('doc-123');
 * // crear descarga en navegador
 * const url = URL.createObjectURL(blob);
 * const link = document.createElement('a');
 * link.href = url;
 * link.download = 'documento.pdf';
 * link.click();
 * URL.revokeObjectURL(url);
 */
export async function downloadCurrentDocument(
  documentId: string
): Promise<Blob> {
  const response = await apiClient.get<Blob>(
    `/doc/documentos/${documentId}/download`,
    {
      responseType: 'blob',
    }
  );
  return response.data;
}

/**
 * Elimina (soft delete) un documento del sistema.
 * 
 * US-DOC-008: Eliminación de documento desde la UI
 * 
 * Marca el documento con fecha_eliminacion sin eliminar físicamente
 * el registro ni los archivos de versiones.
 * 
 * @param documentId ID del documento a eliminar
 * @throws AxiosError con códigos:
 *   - 401: No autenticado (token JWT expirado o inválido)
 *   - 403: Sin permisos de ESCRITURA o ADMINISTRACION
 *   - 404: Documento no encontrado o pertenece a otra organización
 *   - 409: Documento ya está eliminado
 *   - 500: Error interno del servidor
 * 
 * @example
 * try {
 *   await deleteDocument("123");
 *   console.log("Documento eliminado exitosamente");
 * } catch (error) {
 *   if (error.response?.status === 403) {
 *     console.error("Sin permisos para eliminar");
 *   }
 * }
 */
export async function deleteDocument(documentId: string): Promise<void> {
  await apiClient.delete(`/doc/documentos/${documentId}`);
}
