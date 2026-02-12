/**
 * Error Mapper Utilities
 * US-DOC-006: Mapeo de errores de API a mensajes amigables en español
 */

import axios, { AxiosError } from 'axios';
import { ErrorCode } from '../types/upload.types';

/**
 * Mensajes de error en español para el usuario final
 */
const ERROR_MESSAGES: Record<string, string> = {
  INVALID_FILE_SIZE: 'El archivo excede 100 MB',
  INVALID_FILE_TYPE: 'Tipo de archivo no permitido',
  DUPLICATE_FILENAME: 'Ya existe un documento con ese nombre',
  FORBIDDEN: 'No tienes permiso para subir aquí',
  FOLDER_NOT_FOUND: 'Carpeta no encontrada',
  DOCUMENT_NOT_FOUND: 'Documento no disponible',
  VERSION_NOT_FOUND: 'Versión no disponible',
  CONFLICT: 'Versión vigente cambió. Por favor recarga.',
  SERVICE_UNAVAILABLE: 'Servicio temporalmente no disponible',
  NETWORK_ERROR: 'Error de conexión. Verifica tu internet.',
  UNKNOWN_ERROR: 'Error desconocido. Intenta nuevamente.',
};

/**
 * Mapea un error de Axios a un mensaje amigable
 */
export function mapApiErrorToMessage(error: AxiosError): string {
  if (!error.response) {
    return ERROR_MESSAGES.NETWORK_ERROR;
  }

  const status = error.response.status;
  const errorCode = (error.response.data as any)?.code;

  // Mapeo por código de error específico del backend
  if (errorCode && ERROR_MESSAGES[errorCode]) {
    return ERROR_MESSAGES[errorCode];
  }

  // Mapeo por código HTTP
  switch (status) {
    case 400:
      return 'Solicitud inválida. Verifica los datos.';
    case 403:
      return ERROR_MESSAGES.FORBIDDEN;
    case 404:
      return ERROR_MESSAGES.FOLDER_NOT_FOUND;
    case 409:
      return ERROR_MESSAGES.CONFLICT;
    case 413:
      return ERROR_MESSAGES.INVALID_FILE_SIZE;
    case 503:
      return ERROR_MESSAGES.SERVICE_UNAVAILABLE;
    default:
      return ERROR_MESSAGES.UNKNOWN_ERROR;
  }
}

/**
 * Mapea un código de error a un mensaje amigable
 */
export function mapErrorCodeToMessage(code: string): string {
  return ERROR_MESSAGES[code] || ERROR_MESSAGES.UNKNOWN_ERROR;
}

/**
 * Determina si un error es reintentable (retry automático recomendado)
 */
export function isRetryableError(statusCode?: number): boolean {
  if (!statusCode) return false;

  // Errores 5xx son reintentables
  if (statusCode >= 500 && statusCode < 600) return true;

  // 408 (Request Timeout) y 429 (Too Many Requests) son reintentables
  if (statusCode === 408 || statusCode === 429) return true;

  return false;
}

/**
 * Extrae detalles completos de error desde cualquier tipo de error
 */
export function getErrorDetails(error: unknown): {
  code: string;
  message: string;
  isRetryable: boolean;
  status?: number;
} {
  // Error de Axios
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError;
    const status = axiosError.response?.status;
    const errorCode =
      (axiosError.response?.data as any)?.code || ErrorCode.UNKNOWN_ERROR;

    return {
      code: errorCode,
      message: mapApiErrorToMessage(axiosError),
      isRetryable: isRetryableError(status),
      status,
    };
  }

  // Error de red (sin respuesta del servidor)
  if (error instanceof Error && error.message === 'Network Error') {
    return {
      code: ErrorCode.NETWORK_ERROR,
      message: ERROR_MESSAGES[ErrorCode.NETWORK_ERROR],
      isRetryable: true,
    };
  }

  // Error genérico
  return {
    code: ErrorCode.UNKNOWN_ERROR,
    message: ERROR_MESSAGES[ErrorCode.UNKNOWN_ERROR],
    isRetryable: false,
  };
}
