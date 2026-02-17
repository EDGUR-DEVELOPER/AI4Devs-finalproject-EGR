/**
 * Permission-related constants used throughout the application
 * These constants align with backend ACL definitions
 */

import type { CodigoNivelAcceso, ICapabilities } from '@features/acl/types';

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

/**
 * Access level to capabilities mapping
 * Defines what a user can do based on their access level
 */
export const PERMISSION_TO_CAPABILITIES: Record<
  CodigoNivelAcceso | 'NINGUNO',
  ICapabilities
> = {
  LECTURA: {
    canRead: true,
    canWrite: false,
    canAdminister: false,
    canUpload: false,
    canDownload: true,
    canCreateVersion: false,
    canDeleteFolder: false,
    canManagePermissions: false,
    canChangeVersion: false,
  },
  ESCRITURA: {
    canRead: true,
    canWrite: true,
    canAdminister: false,
    canUpload: true,
    canDownload: true,
    canCreateVersion: true,
    canDeleteFolder: false,
    canManagePermissions: false,
    canChangeVersion: true,
  },
  ADMINISTRACION: {
    canRead: true,
    canWrite: true,
    canAdminister: true,
    canUpload: true,
    canDownload: true,
    canCreateVersion: true,
    canDeleteFolder: true,
    canManagePermissions: true,
    canChangeVersion: true,
  },
  NINGUNO: {
    canRead: false,
    canWrite: false,
    canAdminister: false,
    canUpload: false,
    canDownload: false,
    canCreateVersion: false,
    canDeleteFolder: false,
    canManagePermissions: false,
    canChangeVersion: false,
  },
};

/**
 * Required access level per action
 */
export const ACTION_REQUIREMENTS: Record<string, CodigoNivelAcceso | null> = {
  ver: null,
  listar: PERMISSION_CODES.LECTURA,
  descargar: PERMISSION_CODES.LECTURA,
  subir: PERMISSION_CODES.ESCRITURA,
  modificar: PERMISSION_CODES.ESCRITURA,
  crear_version: PERMISSION_CODES.ESCRITURA,
  cambiar_version_actual: PERMISSION_CODES.ESCRITURA,
  eliminar_carpeta: PERMISSION_CODES.ADMINISTRACION,
  administrar_permisos: PERMISSION_CODES.ADMINISTRACION,
  crear_carpeta: PERMISSION_CODES.ESCRITURA,
};

/**
 * Informational messages for disabled actions
 */
export const DISABLED_ACTION_MESSAGES: Record<string, string> = {
  ver: 'No tienes acceso para ver este recurso',
  listar: 'No tienes acceso para listar este contenido',
  descargar: 'Necesitas permiso de Lectura para descargar documentos',
  subir: 'Necesitas permiso de Escritura para subir documentos',
  modificar: 'Necesitas permiso de Escritura para modificar',
  crear_version: 'Necesitas permiso de Escritura para crear versiones',
  cambiar_version_actual:
    'Necesitas permiso de Escritura para cambiar la versión actual',
  eliminar_carpeta: 'Necesitas permiso de Administración para eliminar carpetas',
  administrar_permisos: 'Solo administradores pueden gestionar permisos',
  crear_carpeta: 'Necesitas permiso de Escritura para crear carpetas',
  no_access: 'No tienes acceso a este recurso',
};
