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

/**
 * User summary for ACL assignment display
 * Contains minimal user information needed for ACL operations
 */
export interface IUsuario {
  /** Unique user identifier */
  id: number;

  /** User email address */
  email: string;

  /** User full name */
  nombre: string;
}

/**
 * Folder ACL record representing explicit user permissions on a folder
 * Main domain model for folder-level access control
 */
export interface IAclCarpeta {
  /** Unique permission record identifier */
  id: number;

  /** Folder ID this permission applies to */
  carpeta_id: number;

  /** User ID who has this permission */
  usuario_id: number;

  /** User details (embedded object) */
  usuario: IUsuario;

  /** Access level granted (LECTURA, ESCRITURA, ADMINISTRACION) */
  nivel_acceso: INivelAcceso;

  /** Whether permission applies recursively to subfolders */
  recursivo: boolean;

  /** ISO 8601 timestamp when permission was created */
  fecha_creacion: string;

  /** ISO 8601 timestamp when permission was last updated */
  fecha_actualizacion: string;
}

/**
 * Request payload for creating a new folder ACL
 */
export interface CreateAclCarpetaDTO {
  /** User ID to grant access to */
  usuario_id: number;

  /** Access level code (LECTURA, ESCRITURA, ADMINISTRACION) */
  nivel_acceso_codigo: CodigoNivelAcceso;

  /** Whether to apply recursively to subfolders */
  recursivo: boolean;

  /** Optional audit comment */
  comentario_opcional?: string;
}

/**
 * Request payload for updating an existing folder ACL
 */
export interface UpdateAclCarpetaDTO {
  /** New access level code */
  nivel_acceso_codigo: CodigoNivelAcceso;

  /** Updated recursive flag (optional) */
  recursivo?: boolean;
}

/**
 * API response envelope for a single ACL record
 */
export interface AclCarpetaApiResponse {
  data: IAclCarpeta;
  meta: {
    accion: string;
    timestamp: string;
  };
}

/**
 * API response envelope for list of ACL records
 */
export interface ListAclCarpetaApiResponse {
  data: IAclCarpeta[];
  meta: {
    total: number;
    carpeta_id: number;
  };
}

/**
 * Standard error response from API
 */
export interface AclErrorResponse {
  error: {
    /** Error code (e.g., "ACL_DUPLICADO", "FORBIDDEN") */
    code: string;

    /** User-friendly error message */
    message: string;

    /** Additional error details if available */
    details?: Record<string, unknown>;
  };
}
