/**
 * Permission-related constants used throughout the application
 * These constants align with backend ACL definitions
 */

/**
 * Access level codes (códigos de nivel de acceso)
 * Invariable identifiers for permission levels
 */
export const PERMISSION_CODES = {
  LECTURA: 'LECTURA',
  ESCRITURA: 'ESCRITURA',
  ADMINISTRACION: 'ADMINISTRACION',
} as const;

/**
 * Available actions in the system
 * Atomic operations that can be granted or denied by access levels
 */
export const PERMISSION_ACTIONS = {
  VER: 'ver',
  LISTAR: 'listar',
  DESCARGAR: 'descargar',
  SUBIR: 'subir',
  MODIFICAR: 'modificar',
  CREAR_VERSION: 'crear_version',
  ELIMINAR: 'eliminar',
  ADMINISTRAR_PERMISOS: 'administrar_permisos',
  CAMBIAR_VERSION_ACTUAL: 'cambiar_version_actual',
} as const;

/**
 * Human-readable labels for access level codes
 * Used for UI display in Spanish
 */
export const PERMISSION_LABELS: Record<string, string> = {
  [PERMISSION_CODES.LECTURA]: 'Lectura / Consulta',
  [PERMISSION_CODES.ESCRITURA]: 'Escritura / Modificación',
  [PERMISSION_CODES.ADMINISTRACION]: 'Administración / Control Total',
};

/**
 * Default access level code
 * Used as fallback when no specific level is selected
 */
export const DEFAULT_PERMISSION_CODE = PERMISSION_CODES.LECTURA;

/**
 * Type export for accessing permission code values
 * Allows type-safe access to the codes constant
 */
export type PermissionCodeKey = keyof typeof PERMISSION_CODES;
