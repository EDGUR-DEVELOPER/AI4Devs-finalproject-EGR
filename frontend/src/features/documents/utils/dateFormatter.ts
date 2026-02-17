/**
 * Date Formatter Utilities
 * US-DOC-006: Formateo de fechas al español
 */

/**
 * Formatea una fecha ISO8601 a formato localizado español
 * @param iso8601 - Fecha en formato ISO8601
 * @returns Fecha formateada (ej: "5-ene-2026")
 */
export function formatDate(iso8601: string): string {
  const date = new Date(iso8601);
  
  const day = date.getDate();
  const month = date.toLocaleString('es', { month: 'short' });
  const year = date.getFullYear();

  return `${day}-${month}-${year}`;
}

/**
 * Formatea una hora ISO8601 a formato 24h
 * @param iso8601 - Fecha en formato ISO8601
 * @returns Hora formateada (ej: "14:30")
 */
export function formatTime(iso8601: string): string {
  const date = new Date(iso8601);
  
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');

  return `${hours}:${minutes}`;
}

/**
 * Formatea una fecha y hora ISO8601 completa
 * @param iso8601 - Fecha en formato ISO8601
 * @returns Fecha y hora formateada (ej: "5-ene-2026 14:30")
 */
export function formatDateTime(iso8601: string): string {
  return `${formatDate(iso8601)} ${formatTime(iso8601)}`;
}
