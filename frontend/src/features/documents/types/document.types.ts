/**
 * Document Type Definitions
 * US-DOC-006: Tipos para documentos y versiones
 */

/**
 * Usuario que ha creado documentos o versiones
 */
export interface UserDTO {
  id: string;
  email: string;
  fullName?: string;
}

/**
 * Metadatos internos del documento
 */
export interface DocumentMetadataDTO {
  checksum: string;
  storageKey: string;
}

/**
 * Documento completo con información de versión actual
 */
export interface DocumentDTO {
  id: string;
  folderId: string;
  name: string;
  extension: string;
  mimeType: string;
  size: number;
  currentVersionId: string;
  createdAt: string;
  createdBy: UserDTO;
  metadata?: DocumentMetadataDTO;
}

/**
 * Versión individual de un documento
 */
export interface DocumentVersionDTO {
  id: string;
  documentId: string;
  versionNumber: number;
  size: number;
  mimeType: string;
  createdAt: string;
  createdBy: UserDTO;
  isCurrentVersion: boolean;
  checksum: string;
  description?: string;
}

/**
 * Respuesta paginada de documentos
 */
export interface DocumentListResponse {
  content: DocumentDTO[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

/**
 * Respuesta paginada de versiones de documentos
 */
export interface DocumentVersionListResponse {
  content: DocumentVersionDTO[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
