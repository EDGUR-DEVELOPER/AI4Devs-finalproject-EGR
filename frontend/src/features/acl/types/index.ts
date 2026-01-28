/**
 * Type definitions for ACL (Access Control List) feature
 * Aligned with backend API contracts for access level management
 */

/**
 * Enumeration of valid access level codes
 * These are invariable codes used throughout the system
 */
export type CodigoNivelAcceso = 'LECTURA' | 'ESCRITURA' | 'ADMINISTRACION';

/**
 * Access Level domain model
 * Represents a permission level that controls user actions on resources
 */
export interface INivelAcceso {
  /** Unique identifier (UUID) */
  id: string;

  /** Invariable code: LECTURA, ESCRITURA, or ADMINISTRACION */
  codigo: CodigoNivelAcceso;

  /** Human-readable name for UI display (e.g., "Lectura / Consulta") */
  nombre: string;

  /** Detailed description of the access level capabilities */
  descripcion?: string;

  /** Array of allowed actions at this access level */
  accionesPermitidas: string[];

  /** Display order in UI/lists (lower numbers appear first) */
  orden: number;

  /** Soft-delete flag: true if active, false if deactivated */
  activo: boolean;

  /** Timestamp of creation (ISO 8601 format) */
  fechaCreacion?: string;

  /** Timestamp of last update (ISO 8601 format) */
  fechaActualizacion?: string;
}

/**
 * Standard API response envelope for single or multiple items
 * Aligns with backend response structure
 */
export interface ApiResponse<T> {
  /** The main data payload */
  data: T;

  /** Metadata about the response */
  meta: {
    /** Total count of items (optional for list endpoints) */
    total?: number;

    /** Timestamp when response was generated */
    timestamp?: string;
  };
}

/**
 * Type for valid action names in the system
 * These are the atomic operations that access levels grant or deny
 */
export type AccionPermitida =
  | 'ver'
  | 'listar'
  | 'descargar'
  | 'subir'
  | 'modificar'
  | 'crear_version'
  | 'eliminar'
  | 'administrar_permisos'
  | 'cambiar_version_actual';
