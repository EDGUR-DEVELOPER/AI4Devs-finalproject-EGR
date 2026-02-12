/**
 * File Validation Utilities
 * US-DOC-006: Validación de archivos del lado del cliente
 */

/**
 * Tamaño máximo de archivo permitido: 100 MB en bytes
 */
export const MAX_FILE_SIZE = 100 * 1024 * 1024;

/**
 * Extensiones de archivo permitidas
 */
export const ALLOWED_EXTENSIONS = [
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'ppt',
  'pptx',
  'txt',
  'csv',
  'jpg',
  'jpeg',
  'png',
  'gif',
  'zip',
  'rar',
];

/**
 * MIME types permitidos (whitelist)
 */
export const MIME_TYPE_WHITELIST = new Set([
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'text/plain',
  'text/csv',
  'image/jpeg',
  'image/png',
  'image/gif',
  'application/zip',
  'application/x-rar-compressed',
  'application/vnd.rar',
]);

/**
 * Valida si la extensión del archivo está permitida
 */
export function isExtensionAllowed(fileName: string): boolean {
  const extension = fileName.split('.').pop()?.toLowerCase();
  return extension ? ALLOWED_EXTENSIONS.includes(extension) : false;
}

/**
 * Valida si el tamaño del archivo está dentro del límite
 */
export function isFileSizeValid(file: File): boolean {
  return file.size <= MAX_FILE_SIZE;
}

/**
 * Valida si el MIME type del archivo está permitido
 */
export function isMimeTypeAllowed(file: File): boolean {
  return MIME_TYPE_WHITELIST.has(file.type);
}

/**
 * Valida un archivo completo (extensión, tamaño, MIME type)
 * @returns Objeto con resultado de validación y mensaje de error si aplica
 */
export function validateFile(
  file: File
): { valid: boolean; error?: string } {
  // Validar extensión
  if (!isExtensionAllowed(file.name)) {
    return {
      valid: false,
      error: `Tipo de archivo no permitido. Permitidos: ${ALLOWED_EXTENSIONS.join(', ')}`,
    };
  }

  // Validar tamaño
  if (!isFileSizeValid(file)) {
    return {
      valid: false,
      error: `El archivo excede el tamaño máximo de ${formatFileSize(MAX_FILE_SIZE)}`,
    };
  }

  // Validar MIME type
  if (!isMimeTypeAllowed(file)) {
    return {
      valid: false,
      error: 'El tipo de contenido del archivo no está permitido',
    };
  }

  return { valid: true };
}

/**
 * Formatea un tamaño en bytes a formato legible
 * @param bytes - Tamaño en bytes
 * @returns String formateado (ej: "2.5 MB")
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}
