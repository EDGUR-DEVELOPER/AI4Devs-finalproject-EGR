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
 * Effective permission response for current user on a folder
 * Returned by GET /api/carpetas/{carpetaId}/mi-permiso
 */
export interface IPermisoEfectivo {
  /** Effective access level for the current user */
  nivel_acceso: CodigoNivelAcceso;

  /** Whether the permission is inherited from an ancestor */
  es_heredado: boolean;

  /** Origin folder ID when inherited (null if direct or unknown) */
  carpeta_origen_id?: number | null;

  /** Origin folder name when inherited (null if direct or unknown) */
  carpeta_origen_nombre?: string | null;

  /** Inheritance path from origin to current folder */
  ruta_herencia?: string[] | null;
}

/**
 * List item type for ACL table, supports direct and inherited entries
 */
export type AclCarpetaListItem = IAclCarpeta & {
  /** Whether this entry is inherited */
  es_heredado?: boolean;

  /** Origin folder ID for inherited entries */
  carpeta_origen_id?: number | null;

  /** Origin folder name for inherited entries */
  carpeta_origen_nombre?: string | null;

  /** Inheritance path from origin to current folder */
  ruta_herencia?: string[] | null;
};

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

// ============================================================================
// DOCUMENT ACL TYPES
// ============================================================================

/**
 * Document ACL record representing explicit user permissions on a document
 * Simpler than folder ACL: no recursive field, supports expiration date
 */
export interface IAclDocumento {
  /** Unique permission record identifier */
  id: number;

  /** Document ID this permission applies to */
  documento_id: number;

  /** User ID who has this permission */
  usuario_id: number;

  /** User details (embedded object) */
  usuario: IUsuario;

  /** Access level granted (LECTURA, ESCRITURA, ADMINISTRACION) */
  nivel_acceso: INivelAcceso;

  /** Optional expiration date for temporary access (ISO 8601 format) */
  fecha_expiracion: string | null;

  /** ISO 8601 timestamp when permission was assigned */
  fecha_asignacion: string;
}

/**
 * Request payload for creating a new document ACL
 * Backend implements upsert behavior (creates or updates)
 */
export interface CreateAclDocumentoDTO {
  /** User ID to grant access to */
  usuario_id: number;

  /** Access level code (LECTURA, ESCRITURA, ADMINISTRACION) */
  nivel_acceso_codigo: CodigoNivelAcceso;

  /** Optional expiration date for temporary access (ISO 8601 format) */
  fecha_expiracion?: string | null;
}

/**
 * Request payload for updating an existing document ACL
 * Note: Backend uses same endpoint as create (upsert)
 */
export interface UpdateAclDocumentoDTO {
  /** New access level code */
  nivel_acceso_codigo: CodigoNivelAcceso;

  /** Updated expiration date (optional) */
  fecha_expiracion?: string | null;
}

/**
 * API response envelope for a single document ACL record
 */
export interface AclDocumentoApiResponse {
  id: number;
  documento_id: number;
  usuario_id: number;
  usuario: IUsuario;
  nivel_acceso: INivelAcceso;
  fecha_expiracion: string | null;
  fecha_asignacion: string;
}

/**
 * API response envelope for list of document ACL records
 */
export interface ListAclDocumentoApiResponse {
  permisos: AclDocumentoApiResponse[];
}
