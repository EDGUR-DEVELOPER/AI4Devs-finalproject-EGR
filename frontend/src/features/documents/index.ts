/**
 * Documents Feature - Public API
 * US-DOC-006: Exports para uso en otras partes de la aplicación
 */

// Components
export { DocumentUpload } from './components/DocumentUpload';
export { VersionHistory } from './components/VersionHistory';
export { UploadProgress } from './components/UploadProgress';
export { UploadError } from './components/UploadError';
export { DocumentDownloadButton } from './components/DocumentDownloadButton';

// Hooks
export { useDocumentUpload } from './hooks/useDocumentUpload';
export { useDocumentVersions } from './hooks/useDocumentVersions';
export { usePermissions } from './hooks/usePermissions';
export { useDocumentDownload } from './hooks/useDocumentDownload';

// Types
export type {
  DocumentDTO,
  DocumentVersionDTO,
  DocumentListResponse,
  DocumentVersionListResponse,
  UserDTO,
} from './types/document.types';

export type {
  UploadState,
  UploadProgress as UploadProgressType,
  UploadErrorData,
  ErrorCodeType,
} from './types/upload.types';

export { ErrorCode } from './types/upload.types';

// Services
export {
  uploadDocument,
  uploadNewVersion,
  getDocumentMetadata,
  getDocumentVersions,
  downloadDocumentVersion,
  downloadCurrentDocument,
} from './api/documentService';
