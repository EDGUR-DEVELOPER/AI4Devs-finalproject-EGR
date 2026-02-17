/**
 * Tipos de dominio para el feature de carpetas
 * Basados en los contratos de API de US-FOLDER-001, US-FOLDER-002
 */

/**
 * Capacidades/permisos del usuario sobre un recurso (carpeta o documento)
 * Refleja la estructura de CapacidadesDTO del backend
 */
export interface Capacidades {
  puede_leer: boolean;
  puede_escribir: boolean;
  puede_administrar: boolean;
  puede_descargar?: boolean;
}

/**
 * Item de carpeta en lista de contenido
 */
export interface FolderItem {
  id: string;
  nombre: string;
  descripcion?: string;
  tipo: 'carpeta';
  fecha_creacion: string; // ISO 8601
  fecha_modificacion?: string;
  num_subcarpetas?: number;
  num_documentos?: number;
  capacidades: Capacidades;
}

/**
 * Item de documento en lista de contenido
 */
export interface DocumentItem {
  id: string;
  nombre: string;
  extension?: string;
  tipo: 'documento';
  version_actual: number;
  tamanio_bytes?: number;
  fecha_creacion: string;
  fecha_modificacion: string;
  creado_por?: {
    id: string;
    nombre: string;
    email: string;
  };
  capacidades: Capacidades;
}

/**
 * Permisos del usuario en una carpeta
 * @deprecated Usar Capacidades directamente
 */
export interface FolderPermissions {
  puede_leer: boolean;
  puede_escribir: boolean;
  puede_administrar: boolean;
}

/**
 * Contenido completo de una carpeta (respuesta de API)
 */
export interface FolderContent {
  subcarpetas: FolderItem[];
  documentos: DocumentItem[];
  total_subcarpetas: number;
  total_documentos: number;
  permisos: Capacidades;
}

/**
 * Detalle de carpeta (GET /api/carpetas/{id})
 */
export interface FolderDetail {
  id: number;
  organizacion_id?: number;
  carpeta_padre_id?: number | null;
  nombre: string;
  descripcion?: string | null;
  creado_por?: number;
  fecha_creacion?: string;
  fecha_actualizacion?: string;
  es_raiz: boolean;
}

/**
 * Segmento de breadcrumb para navegación
 */
export interface BreadcrumbSegment {
  id: string | undefined;  // undefined para raíz
  nombre: string;
}

/**
 * Request para crear carpeta (POST /api/carpetas)
 */
export interface CreateFolderRequest {
  nombre: string;
  descripcion?: string;
  carpeta_padre_id: string | null; // null para raíz
}

/**
 * Response de creación de carpeta
 */
export interface CreateFolderResponse {
  id: string;
  nombre: string;
  descripcion?: string;
  fecha_creacion: string;
  carpeta_padre_id: string | null;
  capacidades: Capacidades;
}

/**
 * Tipo unión para items de lista (carpeta o documento)
 */
export type ContentItem = FolderItem | DocumentItem;

/**
 * Errores específicos de carpetas (desde backend)
 */
export const FOLDER_ERROR_CODES = {
  NOT_FOUND: 'CARPETA_NO_ENCONTRADA',
  NO_PERMISSION_READ: 'SIN_PERMISO_LECTURA',
  NO_PERMISSION_WRITE: 'SIN_PERMISO_ESCRITURA',
  NO_PERMISSION_ADMIN: 'SIN_PERMISO_ADMINISTRACION',
  DUPLICATE_NAME: 'NOMBRE_DUPLICADO',
  FOLDER_NOT_EMPTY: 'CARPETA_NO_VACIA',
  ROOT_FOLDER_NOT_FOUND: 'CARPETA_RAIZ_NO_ENCONTRADA',
  VALIDATION_ERROR: 'VALIDACION',
} as const;

export type FolderErrorCode = typeof FOLDER_ERROR_CODES[keyof typeof FOLDER_ERROR_CODES];
