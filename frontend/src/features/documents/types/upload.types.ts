/**
 * Upload Type Definitions
 * US-DOC-006: Tipos para máquina de estados y eventos de upload
 */

/**
 * Estados posibles del proceso de upload
 */
export type UploadState =
  | 'idle'
  | 'selected'
  | 'uploading'
  | 'processing'
  | 'success'
  | 'error'
  | 'cancelled';

/**
 * Información de progreso de upload
 */
export interface UploadProgress {
  state: UploadState;
  percentage: number;
  bytesUploaded: number;
  bytesTotal: number;
  fileName?: string;
}

/**
 * Información detallada de error de upload
 */
export interface UploadErrorData {
  code: string;
  message: string;
  isRetryable: boolean;
  details?: Record<string, unknown>;
}

/**
 * Códigos de error comunes en proceso de upload
 */
export const ErrorCode = {
  INVALID_FILE_SIZE: 'INVALID_FILE_SIZE',
  INVALID_FILE_TYPE: 'INVALID_FILE_TYPE',
  DUPLICATE_FILENAME: 'DUPLICATE_FILENAME',
  FORBIDDEN: 'FORBIDDEN',
  FOLDER_NOT_FOUND: 'FOLDER_NOT_FOUND',
  NETWORK_ERROR: 'NETWORK_ERROR',
  UNKNOWN_ERROR: 'UNKNOWN_ERROR',
} as const;

export type ErrorCodeType = typeof ErrorCode[keyof typeof ErrorCode];
