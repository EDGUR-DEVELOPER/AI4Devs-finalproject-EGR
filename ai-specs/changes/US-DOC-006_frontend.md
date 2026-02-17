# Frontend Implementation Plan: US-DOC-006 UI Mínima de Carga y Ver Historial

## 1. Overview

Este ticket implementa la UI frontend para la carga de documentos y visualización del historial de versiones. Se desarrollará una solución modular y completa con componentes React reutilizables, servicios de API, hooks personalizados, validaciones de cliente, manejo de errores y pruebas exhaustivas. El diseño sigue principios de arquitectura hexagonal, separación de responsabilidades y buenas prácticas de React.

**Principios de arquitectura:**
- Componentes funcionales con hooks
- Separación entre servicios (API), componentes (UI) y lógica (hooks)
- Validaciones de cliente robustas
- Manejo de errores granular
- TypeScript para tipado estático
- Tests unitarios y E2E

---

## 2. Architecture Context

### Componentes Principales
- **DocumentUpload.tsx**: Componente principal que maneja el flujo de carga
- **DocumentUploadInput.tsx**: Input de archivo + botón de envío
- **UploadProgress.tsx**: Indicador visual de progreso
- **UploadError.tsx**: Mostrar errores amigables
- **VersionHistory.tsx**: Modal/panel para mostrar versiones
- **VersionItem.tsx**: Fila individual de versión
- **VersionDownloadButton.tsx**: Botón reutilizable para descargar
- **PermissionGate.tsx**: HOC para validar permisos

### Servicios
- **documentService.ts**: Llamadas API (upload, getVersions, download)
- **uploadProgressService.ts**: Manejo de eventos de progreso XHR
- **permissionChecker.ts**: Lógica de verificación de permisos

### Hooks
- **useDocumentUpload.ts**: Orquestación de carga (estado, validación, envío)
- **useDocumentVersions.ts**: Obtención y cacheado de versiones
- **usePermissions.ts**: Verificación de permisos del usuario actual

### Tipos
- **document.types.ts**: Interfaces de Document, DocumentVersion, etc.
- **upload.types.ts**: UploadState, UploadEvent, etc.

### Utilidades
- **fileValidator.ts**: Validaciones de archivo (tamaño, extensión, MIME)
- **errorMapper.ts**: Mapeo de códigos HTTP a mensajes en español
- **permissionChecker.ts**: Lógica de permisos basada en JWT

### Ubicación dentro del proyecto
```
frontend/src/features/documents/
  ├── components/
  ├── services/
  ├── hooks/
  ├── types/
  ├── utils/
  └── __tests__/
```

### Dependencias de enrutamiento
- No requiere nuevas rutas; componentes se usan dentro de la vista de carpeta existente
- Se asume que la ruta `/folders/:folderId` ya existe

### Gestión de estado
- Estado local en componentes (useState para formularios)
- Hooks personalizados para lógica compleja (useDocumentUpload, useDocumentVersions)
- Token JWT del usuario disponible en contexto de autenticación (AuthContext)

---

## 3. Implementation Steps

### **Step 1: Create Type Definitions**

**File**: `frontend/src/features/documents/types/document.types.ts`

**Action**: Definir todas las interfaces TypeScript para documentos y versiones

**Implementation Steps**:
1. Crear interfaz `DocumentDTO` con propiedades de respuesta API
2. Crear interfaz `DocumentVersionDTO` con datos de versión
3. Crear interfaz `DocumentMetadataDTO` para metadatos
4. Crear interfaz `UserDTO` para información del usuario
5. Crear interfaz `DocumentListResponse` para respuestas paginadas

**Dependencies**:
```typescript
// Sin dependencias externas, solo TypeScript
```

**Implementation Notes**:
- Usar UUIDs como strings
- Usar ISO8601 para timestamps (type: string)
- Incluir propiedades opcionales donde corresponda (checksum, description)
- Mantener consistencia con especificación de API en el ticket

**Code Signature**:
```typescript
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
  metadata?: {
    checksum: string;
    storageKey: string;
  };
}

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

export interface UserDTO {
  id: string;
  email: string;
  fullName?: string;
}

export interface DocumentListResponse {
  content: DocumentDTO[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface DocumentVersionListResponse {
  content: DocumentVersionDTO[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
```

---

### **Step 2: Create Upload Type Definitions**

**File**: `frontend/src/features/documents/types/upload.types.ts`

**Action**: Definir tipos para máquina de estados y eventos de upload

**Implementation Steps**:
1. Definir type `UploadState` con estados posibles
2. Definir interface `UploadProgress` con información de progreso
3. Definir interface `UploadError` con detalles de error
4. Definir enum `UploadError` para códigos de error comunes

**Implementation Notes**:
- UploadState: 'idle' | 'selected' | 'uploading' | 'processing' | 'success' | 'error' | 'cancelled'
- Incluir propiedades para progreso (percentage, bytesUploaded, bytesTotal)
- Incluir propiedades para errores (code, message, isRetryable)

**Code Signature**:
```typescript
export type UploadState = 
  | 'idle' 
  | 'selected' 
  | 'uploading' 
  | 'processing' 
  | 'success' 
  | 'error' 
  | 'cancelled';

export interface UploadProgress {
  state: UploadState;
  percentage: number;
  bytesUploaded: number;
  bytesTotal: number;
  fileName?: string;
}

export interface UploadErrorData {
  code: string;
  message: string;
  isRetryable: boolean;
  details?: Record<string, unknown>;
}

export enum ErrorCode {
  INVALID_FILE_SIZE = 'INVALID_FILE_SIZE',
  INVALID_FILE_TYPE = 'INVALID_FILE_TYPE',
  DUPLICATE_FILENAME = 'DUPLICATE_FILENAME',
  FORBIDDEN = 'FORBIDDEN',
  FOLDER_NOT_FOUND = 'FOLDER_NOT_FOUND',
  NETWORK_ERROR = 'NETWORK_ERROR',
  UNKNOWN_ERROR = 'UNKNOWN_ERROR',
}
```

---

### **Step 3: Create File Validator Utility**

**File**: `frontend/src/features/documents/utils/fileValidator.ts`

**Action**: Crear utilidad para validar archivos antes de envío

**Implementation Steps**:
1. Definir lista de extensiones permitidas (whitelist)
2. Implementar función `isExtensionAllowed(file: File): boolean`
3. Implementar función `isFileSizeValid(file: File): boolean`
4. Implementar función `isMimeTypeAllowed(file: File): boolean`
5. Implementar función `validateFile(file: File): { valid: boolean; error?: string }`
6. Exportar constante `ALLOWED_EXTENSIONS` y `MAX_FILE_SIZE`

**Dependencies**:
```typescript
// Sin dependencias externas
```

**Implementation Notes**:
- MAX_FILE_SIZE = 100 * 1024 * 1024 (100 MB en bytes)
- ALLOWED_EXTENSIONS = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'csv', 'jpg', 'jpeg', 'png', 'gif', 'zip', 'rar']
- MIME_TYPE_WHITELIST = correspondiente a las extensiones
- Validar tanto extensión como MIME type

**Code Signature**:
```typescript
export const ALLOWED_EXTENSIONS = [...];
export const MAX_FILE_SIZE = 100 * 1024 * 1024;
export const MIME_TYPE_WHITELIST = new Set([...]);

export function isExtensionAllowed(fileName: string): boolean { ... }
export function isFileSizeValid(file: File): boolean { ... }
export function isMimeTypeAllowed(file: File): boolean { ... }
export function validateFile(file: File): { valid: boolean; error?: string } { ... }
export function formatFileSize(bytes: number): string { ... }
```

---

### **Step 4: Create Error Mapper Utility**

**File**: `frontend/src/features/documents/utils/errorMapper.ts`

**Action**: Mapear códigos HTTP y códigos de error a mensajes amigables en español

**Implementation Steps**:
1. Crear función `mapApiErrorToMessage(error: AxiosError): string`
2. Crear función `mapErrorCodeToMessage(code: string): string`
3. Crear función `isRetryableError(statusCode: number): boolean`
4. Mapear todos los códigos de error mencionados en el ticket

**Implementation Notes**:
- Todos los mensajes en español (como requisita el proyecto)
- Mensajes cortos y directos para el usuario
- Incluir códigos HTTP 400, 403, 404, 409, 413, 503

**Code Signature**:
```typescript
export function mapApiErrorToMessage(error: AxiosError): string { ... }
export function mapErrorCodeToMessage(code: string): string { ... }
export function isRetryableError(statusCode?: number): boolean { ... }
export function getErrorDetails(error: unknown): { code: string; message: string; isRetryable: boolean } { ... }
```

**Mappings**:
```typescript
const ERROR_MESSAGES: Record<string, string> = {
  INVALID_FILE_SIZE: 'El archivo excede 100 MB',
  INVALID_FILE_TYPE: 'Tipo de archivo no permitido',
  DUPLICATE_FILENAME: 'Ya existe un documento con ese nombre',
  FORBIDDEN: 'No tienes permiso para subir aquí',
  FOLDER_NOT_FOUND: 'Carpeta no encontrada',
  DOCUMENT_NOT_FOUND: 'Documento no disponible',
  VERSION_NOT_FOUND: 'Versión no disponible',
  CONFLICT: 'Versión vigente cambió. Por favor recarga.',
  SERVICE_UNAVAILABLE: 'Servicio temporalmente no disponible',
  NETWORK_ERROR: 'Error de conexión. Verifica tu internet.',
  UNKNOWN_ERROR: 'Error desconocido. Intenta nuevamente.',
};
```

---

### **Step 5: Create Permission Checker Utility**

**File**: `frontend/src/features/documents/utils/permissionChecker.ts`

**Action**: Verificar permisos del usuario basado en JWT

**Implementation Steps**:
1. Crear función `parseJwt(token: string): any` para decodificar JWT
2. Crear función `canUserWriteToFolder(token: string, folderId: string): boolean`
3. Crear función `canUserReadFolder(token: string, folderId: string): boolean`
4. Crear función `extractUserInfo(token: string): { id: string; email: string; roles: string[] }`
5. Crear función `hasPermission(token: string, permission: string): boolean`

**Dependencies**:
```typescript
// Usar jwt-decode si está disponible, sino decodificar manualmente
// import { jwtDecode } from 'jwt-decode'; // opcional
```

**Implementation Notes**:
- Los permisos vienen en el JWT con formato `{resourceType}:{action}:{resourceId}`
- Ejemplo: `documents:write:c123e4567890` (puede escribir en documento/carpeta c123e4567890)
- Verificar que el permiso existe en el JWT antes de permitir acción
- No confiar solo en verificación de cliente (para validar UX, no seguridad)

**Code Signature**:
```typescript
export function parseJwt(token: string): Record<string, unknown> | null { ... }
export function canUserWriteToFolder(token: string, folderId: string): boolean { ... }
export function canUserReadFolder(token: string, folderId: string): boolean { ... }
export function extractUserInfo(token: string): { id: string; email: string; roles: string[] } { ... }
export function hasPermission(token: string, resourceType: string, action: string, resourceId: string): boolean { ... }
```

---

### **Step 6: Create Document Service**

**File**: `frontend/src/features/documents/services/documentService.ts`

**Action**: Implementar funciones de API para upload, obtener versiones, descargar

**Implementation Steps**:
1. Crear función `uploadDocument(folderId: string, file: File, onProgress?: ProgressCallback): Promise<DocumentDTO>`
2. Crear función `getDocumentMetadata(documentId: string): Promise<DocumentDTO>`
3. Crear función `getDocumentVersions(documentId: string, page: number = 0, size: number = 20): Promise<DocumentVersionListResponse>`
4. Crear función `downloadDocumentVersion(documentId: string, versionId: string, inline: boolean = false): Promise<Blob>`
5. Crear función `uploadNewVersion(documentId: string, file: File, description?: string, onProgress?: ProgressCallback): Promise<DocumentVersionDTO>`

**Dependencies**:
```typescript
import axios, { AxiosProgressEvent } from 'axios';
import { DocumentDTO, DocumentVersionDTO, DocumentVersionListResponse } from '../types/document.types';
import { validateFile } from '../utils/fileValidator';
import { mapApiErrorToMessage } from '../utils/errorMapper';
```

**Implementation Notes**:
- Usar axios para peticiones HTTP
- Interceptar eventos de progreso para actualizar UI
- Incluir header Authorization con Bearer token
- Manejar errores con mapeo a mensajes amigables
- Implementar retry automático para errores retryables (503, network errors)
- Validar archivo antes de enviar (cliente)

**Code Signature**:
```typescript
export type ProgressCallback = (progress: UploadProgress) => void;

export const documentApi = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

export async function uploadDocument(
  folderId: string,
  file: File,
  token: string,
  onProgress?: ProgressCallback
): Promise<DocumentDTO> { ... }

export async function getDocumentMetadata(documentId: string, token: string): Promise<DocumentDTO> { ... }

export async function getDocumentVersions(
  documentId: string,
  token: string,
  page: number = 0,
  size: number = 20
): Promise<DocumentVersionListResponse> { ... }

export async function downloadDocumentVersion(
  documentId: string,
  versionId: string,
  token: string,
  inline: boolean = false
): Promise<Blob> { ... }

export async function uploadNewVersion(
  documentId: string,
  file: File,
  token: string,
  description?: string,
  onProgress?: ProgressCallback
): Promise<DocumentVersionDTO> { ... }
```

**Error Handling**:
- Capturar AxiosError y mapear a código/mensaje
- Implementar exponential backoff para reintentos (1s, 2s, 4s)
- Lanzar error con estructura: `{ code, message, isRetryable, status }`

---

### **Step 7: Create useDocumentUpload Hook**

**File**: `frontend/src/features/documents/hooks/useDocumentUpload.ts`

**Action**: Orquestar lógica de upload (estado, validación, progreso, errores)

**Implementation Steps**:
1. Crear hook que gestione estado de upload (selectedFile, uploadState, progress, error)
2. Crear función `selectFile(file: File): void` que valide y guarde archivo
3. Crear función `uploadFile(): Promise<void>` que inicie upload
4. Crear función `cancelUpload(): void` que cancele en progreso
5. Crear función `clearError(): void` que limpie error y vuelva a idle
6. Crear función `retryUpload(): Promise<void>` que reintente después de error
7. Manejar progreso y actualizar porcentaje en tiempo real
8. Implementar exponential backoff para reintentos automáticos

**Dependencies**:
```typescript
import { useState, useCallback, useRef } from 'react';
import { uploadDocument } from '../services/documentService';
import { validateFile } from '../utils/fileValidator';
import { mapApiErrorToMessage, isRetryableError } from '../utils/errorMapper';
import { UploadState, UploadProgress } from '../types/upload.types';
```

**Implementation Notes**:
- Estados posibles: idle → selected → uploading → processing → success/error → idle
- Guardar AbortController para poder cancelar
- Implementar throttle en callback de progreso (update cada 100ms max)
- Guardar el archivo en ref para poder reintentarlo
- Después de éxito exitoso, limpiar estado en 2 segundos o esperar a que usuario cierre modal

**Code Signature**:
```typescript
interface UseDocumentUploadReturn {
  selectedFile: File | null;
  uploadState: UploadState;
  progress: UploadProgress;
  error: { code: string; message: string } | null;
  selectFile: (file: File) => void;
  uploadFile: () => Promise<DocumentDTO>;
  cancelUpload: () => void;
  clearError: () => void;
  retryUpload: () => Promise<DocumentDTO>;
  reset: () => void;
}

export function useDocumentUpload(
  folderId: string,
  token: string,
  onUploadSuccess?: (doc: DocumentDTO) => void
): UseDocumentUploadReturn { ... }
```

**Progress Throttle Implementation**:
```typescript
const throttledProgress = useCallback(
  throttle((progress: UploadProgress) => {
    setProgress(progress);
  }, 100),
  []
);
```

---

### **Step 8: Create useDocumentVersions Hook**

**File**: `frontend/src/features/documents/hooks/useDocumentVersions.ts`

**Action**: Obtener y cachear historial de versiones con paginación

**Implementation Steps**:
1. Crear hook que gestione lista de versiones con paginación
2. Implementar función `fetchVersions(page: number): Promise<void>`
3. Crear estado para: versions, currentPage, totalPages, isLoading, error
4. Implementar caché con invalidación manual
5. Auto-refresh cada 3 minutos si modal está abierto
6. Manejar errores de API

**Dependencies**:
```typescript
import { useState, useEffect, useCallback, useRef } from 'react';
import { getDocumentVersions } from '../services/documentService';
import { DocumentVersionDTO } from '../types/document.types';
```

**Implementation Notes**:
- Caché: no cacheable por defecto, pero permite invalidación manual
- Auto-refresh: solo si modal está abierto (pasar prop `isOpen` al hook)
- Paginación: mostrar página actual, total de páginas
- Versiones ordenadas DESC por versionNumber (más reciente primero)

**Code Signature**:
```typescript
interface UseDocumentVersionsReturn {
  versions: DocumentVersionDTO[];
  currentPage: number;
  totalPages: number;
  isLoading: boolean;
  error: string | null;
  fetchVersions: (page: number) => Promise<void>;
  invalidateCache: () => void;
  goToPage: (page: number) => void;
}

export function useDocumentVersions(
  documentId: string,
  token: string,
  isOpen: boolean = false
): UseDocumentVersionsReturn { ... }
```

---

### **Step 9: Create usePermissions Hook**

**File**: `frontend/src/features/documents/hooks/usePermissions.ts`

**Action**: Hook para verificar permisos del usuario actual

**Implementation Steps**:
1. Obtener token del contexto de autenticación
2. Crear función `canWrite(): boolean` para verificar permiso de escritura
3. Crear función `canRead(): boolean` para verificar permiso de lectura
4. Manejar caso de token ausente o inválido
5. Cachear resultado (permisos no cambian en sesión actual)

**Dependencies**:
```typescript
import { useContext, useMemo } from 'react';
import { AuthContext } from '../../../core/auth/AuthContext'; // ajustar según proyecto
import { canUserWriteToFolder, canUserReadFolder } from '../utils/permissionChecker';
```

**Implementation Notes**:
- Obtener token de AuthContext (asumiendo que existe)
- Usememo para cachear resultado de verificación
- Si token no existe, retornar false (usuario no autenticado)

**Code Signature**:
```typescript
interface UsePermissionsReturn {
  canWrite: () => boolean;
  canRead: () => boolean;
  userId: string | null;
  userEmail: string | null;
  isLoading: boolean;
}

export function usePermissions(folderId: string): UsePermissionsReturn { ... }
```

---

### **Step 10: Create UploadProgress Component**

**File**: `frontend/src/features/documents/components/UploadProgress.tsx`

**Action**: Componente visual de progreso durante upload

**Implementation Steps**:
1. Crear componente que muestre barra de progreso
2. Mostrar porcentaje (0-100%)
3. Mostrar nombre de archivo
4. Mostrar tamaño actual / tamaño total
5. Mostrar estado (uploading, processing, success)
6. Hacer responsive para mobile y desktop

**Dependencies**:
```typescript
import React from 'react';
import { ProgressBar } from 'react-bootstrap'; // si Bootstrap está disponible
import { UploadProgress as UploadProgressType } from '../types/upload.types';
```

**Implementation Notes**:
- Usar ProgressBar de Bootstrap o crear con CSS
- Mostrar diferentes colores según estado (info para uploading, success para éxito, danger para error)
- Accessibility: role="progressbar", aria-valuenow, aria-valuemin, aria-valuemax
- Formato: "Subiendo documento.pdf (2.5 MB / 10 MB) - 25%"

**Code Signature**:
```typescript
interface UploadProgressProps {
  progress: UploadProgressType;
  onCancel?: () => void;
}

export const UploadProgress: React.FC<UploadProgressProps> = ({ progress, onCancel }) => { ... }
```

---

### **Step 11: Create UploadError Component**

**File**: `frontend/src/features/documents/components/UploadError.tsx`

**Action**: Mostrar errores con opción de reintentar

**Implementation Steps**:
1. Mostrar icono de error
2. Mostrar mensaje de error (desde errorMapper)
3. Mostrar opción de reintentar si es retryable
4. Mostrar opción de limpiar error
5. Estilo Bootstrap con alerta

**Dependencies**:
```typescript
import React from 'react';
import { Alert, Button } from 'react-bootstrap';
```

**Implementation Notes**:
- Alert variant="danger" para errores
- Incluir botón "Reintentar" si onRetry está disponible
- Incluir botón "Descartar" para limpiar
- Mostrar código de error en texto pequeño para debugging

**Code Signature**:
```typescript
interface UploadErrorProps {
  error: { code: string; message: string };
  isRetryable: boolean;
  onRetry?: () => void;
  onDismiss: () => void;
}

export const UploadError: React.FC<UploadErrorProps> = ({
  error,
  isRetryable,
  onRetry,
  onDismiss,
}) => { ... }
```

---

### **Step 12: Create DocumentUploadInput Component**

**File**: `frontend/src/features/documents/components/DocumentUploadInput.tsx`

**Action**: Componente para seleccionar archivo y botón de envío

**Implementation Steps**:
1. Crear input de tipo file con accept limitado a extensiones permitidas
2. Mostrar nombre de archivo seleccionado
3. Mostrar botón "Subir" deshabilitado hasta que se seleccione archivo
4. Mostrar validación visual en tiempo real
5. Limpiar input después de envío exitoso
6. Manejo de drag & drop (opcional pero recomendado)

**Dependencies**:
```typescript
import React, { useRef } from 'react';
import { Form, Button } from 'react-bootstrap';
import { ALLOWED_EXTENSIONS } from '../utils/fileValidator';
```

**Implementation Notes**:
- Input con accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.jpg,.jpeg,.png,.gif,.zip,.rar"
- Label asociado con htmlFor para accesibilidad
- Mostrar mensaje explicativo: "Selecciona archivos (PDF, DOC, XLSX, etc. hasta 100 MB)"
- Opcionalmente: drag & drop zone para mejorar UX
- Limpiar input y setear archivo en parent mediante callback

**Code Signature**:
```typescript
interface DocumentUploadInputProps {
  onFileSelected: (file: File) => void;
  onUpload: () => Promise<void>;
  isLoading: boolean;
  isDisabled: boolean;
  selectedFileName?: string;
}

export const DocumentUploadInput: React.FC<DocumentUploadInputProps> = ({
  onFileSelected,
  onUpload,
  isLoading,
  isDisabled,
  selectedFileName,
}) => { ... }
```

---

### **Step 13: Create DocumentUpload Component**

**File**: `frontend/src/features/documents/components/DocumentUpload.tsx`

**Action**: Componente principal que orquesta todo el flujo de upload

**Implementation Steps**:
1. Usar hook `useDocumentUpload` para gestionar estado
2. Orquestar componentes: UploadInput → UploadProgress → UploadError
3. Renderizar flujo según estado actual
4. Mostrar Toast de éxito al terminar
5. Callback para notificar a componente padre (refrescar listado)
6. Manejar permisos (mostrar/deshabilitar según canWrite)

**Dependencies**:
```typescript
import React, { FC, useEffect } from 'react';
import { usePermissions } from '../hooks/usePermissions';
import { useDocumentUpload } from '../hooks/useDocumentUpload';
import { DocumentUploadInput } from './DocumentUploadInput';
import { UploadProgress } from './UploadProgress';
import { UploadError } from './UploadError';
import { useToast } from '../../../core/toast/useToast'; // asumiendo que existe
```

**Implementation Notes**:
- Mostrar mensaje "Solo lectura: no puedes subir documentos" si !canWrite
- Mostrar Toast verde "Documento subido exitosamente" en éxito
- Llamar callback `onUploadSuccess` para que padre recargue listado
- Limpiar estado después de 2 segundos de éxito
- Permitir subir otro archivo después de éxito

**Code Signature**:
```typescript
interface DocumentUploadProps {
  folderId: string;
  onUploadSuccess?: (documentId: string, fileName: string) => void;
}

export const DocumentUpload: FC<DocumentUploadProps> = ({
  folderId,
  onUploadSuccess,
}) => { ... }
```

**State Transitions**:
```
idle
  ↓ (onFileSelect)
selected (mostrar nombre archivo, botón Subir habilitado)
  ↓ (onUpload)
uploading (mostrar progreso 0-99%)
  ↓ (evento onUploadComplete)
processing (mostrar progreso 100%)
  ↓ (respuesta API 201)
success (mostrar "Documento subido exitosamente", limpiar en 2s)
  ↓
idle (permitir subir otro)

O en case de error:
processing
  ↓ (respuesta API 4xx/5xx)
error (mostrar mensaje, opción Reintentar si retryable)
  ↓ (onRetry)
uploading (reintentar)
```

---

### **Step 14: Create VersionItem Component**

**File**: `frontend/src/features/documents/components/VersionItem.tsx`

**Action**: Componente para mostrar una fila de versión en el historial

**Implementation Steps**:
1. Mostrar número de versión
2. Mostrar fecha y hora de creación (formato localizado al español)
3. Mostrar usuario que creó la versión
4. Mostrar tamaño en bytes formateado
5. Mostrar etiqueta "[Actual]" si es versión vigente
6. Botón "Descargar" para descargar versión
7. Botón "Nueva versión" si es versión actual (para subir actualización)
8. Fila con alternancia de colores (zebra striping)

**Dependencies**:
```typescript
import React, { FC } from 'react';
import { DocumentVersionDTO } from '../types/document.types';
import { formatFileSize } from '../utils/fileValidator';
import { formatDate, formatTime } from '../utils/dateFormatter'; // crear si no existe
```

**Implementation Notes**:
- Formato fecha: "5-ene-2026" (español)
- Formato hora: "14:30" (formato 24h)
- Tamaño: "2.5 MB" formateado
- Badge Bootstrap para etiqueta "[Actual]"
- Botones como iconos o texto pequeño
- Responsive: en mobile, colapsar detalles en accordion si necesario

**Code Signature**:
```typescript
interface VersionItemProps {
  version: DocumentVersionDTO;
  onDownload: (versionId: string) => void;
  onUploadNewVersion?: () => void;
  isLoading: boolean;
}

export const VersionItem: FC<VersionItemProps> = ({
  version,
  onDownload,
  onUploadNewVersion,
  isLoading,
}) => { ... }
```

---

### **Step 15: Create VersionDownloadButton Component**

**File**: `frontend/src/features/documents/components/VersionDownloadButton.tsx`

**Action**: Botón reutilizable para descargar versión

**Implementation Steps**:
1. Crear botón con icono de descarga
2. Mostrar loading durante descarga
3. Manejar progreso de descarga (opcional)
4. Implementar descarga como blob (no cacheable)
5. Cambiar nombre dinámico del archivo descargado
6. Verificar checksum si está disponible (security)

**Dependencies**:
```typescript
import React, { FC, useState } from 'react';
import { Button } from 'react-bootstrap';
import { downloadDocumentVersion } from '../services/documentService';
```

**Implementation Notes**:
- Usar createElement para trigger automático de descarga
- Nombre del archivo: `{documentName}_v{versionNumber}.{extension}`
- Mostrar error si descarga falla
- Opcionalmente validar checksum (SHA256 del Blob vs header X-Content-Checksum)

**Code Signature**:
```typescript
interface VersionDownloadButtonProps {
  documentId: string;
  versionId: string;
  versionNumber: number;
  documentName: string;
  extension: string;
  isLoading?: boolean;
}

export const VersionDownloadButton: FC<VersionDownloadButtonProps> = ({
  documentId,
  versionId,
  versionNumber,
  documentName,
  extension,
  isLoading = false,
}) => { ... }
```

---

### **Step 16: Create VersionHistory Component**

**File**: `frontend/src/features/documents/components/VersionHistory.tsx`

**Action**: Componente principal para mostrar historial de versiones

**Implementation Steps**:
1. Usar hook `useDocumentVersions` para obtener versiones
2. Mostrar loading spinner mientras carga
3. Renderizar tabla/lista de versiones usando VersionItem
4. Implementar paginación si hay > 20 versiones
5. Manejar error si no puede obtener versiones
6. Auto-refresh cada 3 minutos
7. Botón para refrescar manualmente
8. Botón para subir nueva versión (abre DocumentUpload anidado)

**Dependencies**:
```typescript
import React, { FC, useState, useEffect } from 'react';
import { Spinner, Alert, Pagination, Button } from 'react-bootstrap';
import { useDocumentVersions } from '../hooks/useDocumentVersions';
import { VersionItem } from './VersionItem';
import { DocumentUpload } from './DocumentUpload';
```

**Implementation Notes**:
- Mostrar "No hay versiones" si lista vacía
- Accesibilidad: tabla con thead/tbody, role="table"
- Paginación: solo mostrar si totalPages > 1
- Auto-refresh: solo si modal está abierto (pasar `isOpen` prop)
- Ordenar DESC por versionNumber (verificar en backend)

**Code Signature**:
```typescript
interface VersionHistoryProps {
  documentId: string;
  documentName: string;
  folderId: string;
  isOpen: boolean;
  onNewVersionUpload?: (versionId: string) => void;
}

export const VersionHistory: FC<VersionHistoryProps> = ({
  documentId,
  documentName,
  folderId,
  isOpen,
  onNewVersionUpload,
}) => { ... }
```

---

### **Step 17: Create PermissionGate HOC (Optional Enhancement)**

**File**: `frontend/src/features/documents/components/PermissionGate.tsx`

**Action**: HOC para envolver componentes y protegerlos con permisos

**Implementation Steps**:
1. Crear HOC que verifique permisos
2. Mostrar componente si usuario tiene permiso
3. Mostrar fallback message si no tiene permiso
4. Pasar rol requerido como prop

**Dependencies**:
```typescript
import React, { FC } from 'react';
import { usePermissions } from '../hooks/usePermissions';
import { Alert } from 'react-bootstrap';
```

**Implementation Notes**:
- Parámetro: requiredPermission ('read' | 'write')
- Mostrar mensaje amigable en español si no tiene permiso
- Aún validar en servidor (esto es solo para UX)

**Code Signature**:
```typescript
interface PermissionGateProps {
  folderId: string;
  requiredPermission: 'read' | 'write';
  fallback?: React.ReactNode;
}

export function PermissionGate<P>(
  Component: FC<P>,
  { requiredPermission, fallback }: PermissionGateProps
): FC<P> { ... }
```

---

### **Step 18: Create Unit Tests - Services**

**File**: `frontend/src/features/documents/__tests__/documentService.test.ts`

**Action**: Tests unitarios para documentService

**Implementation Steps**:
1. Test uploadDocument: simular POST exitoso, validar FormData
2. Test uploadDocument error: simular validación de archivo fallida
3. Test uploadDocument error: simular respuesta 413 (payload too large)
4. Test getDocumentVersions: simular GET exitoso, validar paginación
5. Test downloadDocumentVersion: simular GET de blob
6. Mockear axios usando jest.mock

**Dependencies**:
```typescript
import axios from 'axios';
import * as documentService from '../services/documentService';
jest.mock('axios');
```

**Implementation Notes**:
- Usar jest.mock para mockear axios
- Validar que los headers incluyen Authorization
- Validar URLs correctas
- Test error handling

**Code Coverage Target**: > 80% statements en documentService

---

### **Step 19: Create Unit Tests - Hooks**

**File**: `frontend/src/features/documents/__tests__/useDocumentUpload.test.ts`

**Action**: Tests unitarios para hooks

**Implementation Steps**:
1. Test useDocumentUpload: flujo completo idle → success
2. Test useDocumentUpload: validación de archivo fallida
3. Test useDocumentUpload: error en upload, reintento exitoso
4. Test useDocumentUpload: cancelación de upload
5. Test useDocumentVersions: carga de versiones
6. Test useDocumentVersions: paginación
7. Usar @testing-library/react para testing de hooks

**Dependencies**:
```typescript
import { renderHook, act, waitFor } from '@testing-library/react';
import { useDocumentUpload } from '../hooks/useDocumentUpload';
```

**Code Coverage Target**: > 80% statements en hooks

---

### **Step 20: Create Unit Tests - Utils**

**File**: `frontend/src/features/documents/__tests__/fileValidator.test.ts` y `errorMapper.test.ts`

**Action**: Tests unitarios para utilidades

**Implementation Steps**:
1. Test fileValidator: validar extensiones permitidas
2. Test fileValidator: rechazar .exe, .bat, etc.
3. Test fileValidator: validar tamaño máximo
4. Test fileValidator: MIME type mismatch
5. Test errorMapper: mapear código 400 → mensaje
6. Test errorMapper: mapear código 403 → mensaje
7. Test errorMapper: clasificar errores como retryable

**Code Coverage Target**: 100% statements en utils (criterio estricto para funciones puras)

---

### **Step 21: Create Component Tests - DocumentUpload**

**File**: `frontend/src/features/documents/__tests__/DocumentUpload.test.tsx`

**Action**: Tests unitarios para componente principal

**Implementation Steps**:
1. Test renderizado inicial
2. Test selección de archivo
3. Test flujo de upload exitoso (end-to-end)
4. Test mostrar Toast al éxito
5. Test mostrar error y permitir reintentar
6. Test permisos: deshabilitar botón si no canWrite
7. Test Toast de error en caso de fallo
8. Usar @testing-library/react para testing de componentes

**Dependencies**:
```typescript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DocumentUpload } from '../components/DocumentUpload';
```

**Code Coverage Target**: > 80% statements en componentes

---

### **Step 22: Create Cypress E2E Tests**

**File**: `frontend/cypress/e2e/documents/upload.e2e.ts` y `versions.e2e.ts`

**Action**: Tests E2E de flujos completos

**Implementation Steps**:
1. E2E: Usuario autenticado sube documento exitosamente
2. E2E: Documento aparece en listado de carpeta
3. E2E: Usuario ve historial de versiones
4. E2E: Usuario descarga versión anterior
5. E2E: Usuario ve error si intenta subir .exe
6. E2E: Usuario ve error si intenta subir archivo > 100 MB
7. E2E: Usuario sin permiso no ve botón "Subir"
8. E2E: Usuario reintenta después de error de conexión

**Dependencies**:
```typescript
// Cypress (ya debe estar configurado en proyecto)
describe('Document Upload Flow', () => { ... });
```

**Implementation Notes**:
- Mock servidor si es necesario para simular errores
- Usar Page Objects para maintainability
- Test casos:
  - Happy path (éxito)
  - Error de validación (extensión no permitida)
  - Error de tamaño
  - Error de conexión (503)
  - Error de permiso (403)
  - Reintento automático

**Code Coverage Target**: Todos los happy paths + 3 error paths principales

---

### **Step 23: Create helper utilities**

**File**: `frontend/src/features/documents/utils/dateFormatter.ts`

**Action**: Utilidades para formatear fechas al español

**Implementation Steps**:
1. Función `formatDate(iso8601: string): string` → "5-ene-2026"
2. Función `formatTime(iso8601: string): string` → "14:30"
3. Función `formatDateTime(iso8601: string): string` → "5-ene-2026 14:30"
4. Usar luxon o date-fns para parseado seguro

**Dependencies**:
```typescript
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
```

**Implementation Notes**:
- Todas las fechas en español
- Formato coherente con especificación del ticket
- Considerar zona horaria del usuario

---

### **Step 24: Update Technical Documentation**

**File**: `ai-specs/specs/frontend-standards.mdc`

**Action**: Actualizar documentación de estándares frontend

**Implementation Steps**:
1. Revisar cambios realizados en implementación
2. Identificar nuevos patrones y convenciones aplicadas
3. Actualizar sección de "Document Upload" en standards
4. Documentar estructura de `features/documents/`
5. Agregar ejemplos de uso de hooks
6. Documentar flujo de manejo de errores
7. Agregar ejemplos de componentes
8. Documentar estrategia de testing
9. Verificar que documentación esté en inglés (como dice el estándar)
10. Crear/actualizar `features/documents/README.md` con instrucciones de uso

**Implementation Notes**:
- Seguir formato y estructura de documentación existente
- Incluir ejemplos de código
- Documentar convenciones de nombres específicas del feature
- Agregar diagrama de flujo de upload (ASCII art o Mermaid)
- Documentar patrones de autorización y manejo de permisos

**Files to update**:
- `ai-specs/specs/frontend-standards.mdc` (main standards)
- `frontend/src/features/documents/README.md` (feature-specific)
- `ai-specs/specs/api-spec.yml` (if API endpoints changed)

---

## 4. Implementation Order

* **Step 1**: Create Type Definitions (document.types.ts)
* **Step 2**: Create Upload Type Definitions (upload.types.ts)
* **Step 3**: Create File Validator Utility
* **Step 4**: Create Error Mapper Utility
* **Step 5**: Create Permission Checker Utility
* **Step 6**: Create Document Service
* **Step 7**: Create useDocumentUpload Hook
* **Step 8**: Create useDocumentVersions Hook
* **Step 9**: Create usePermissions Hook
* **Step 10**: Create UploadProgress Component
* **Step 11**: Create UploadError Component
* **Step 12**: Create DocumentUploadInput Component
* **Step 13**: Create DocumentUpload Component
* **Step 14**: Create VersionItem Component
* **Step 15**: Create VersionDownloadButton Component
* **Step 16**: Create VersionHistory Component
* **Step 17**: Create PermissionGate HOC (Optional)
* **Step 18**: Create Unit Tests - Services
* **Step 19**: Create Unit Tests - Hooks
* **Step 20**: Create Unit Tests - Utils
* **Step 21**: Create Component Tests - DocumentUpload
* **Step 22**: Create Cypress E2E Tests
* **Step 23**: Create helper utilities (dateFormatter)
* **Step 24**: Update Technical Documentation

---

## 5. Testing Checklist

### Post-Implementation Verification

- [ ] **Build succeeds**: `npm run build` completes without errors
- [ ] **Linting passes**: `npm run lint` with no warnings
- [ ] **Type checking passes**: `tsc -b` with no errors
- [ ] **Unit tests pass**: `npm run test` with > 80% coverage
- [ ] **E2E tests pass**: `npm run test:e2e` (todos los happy paths)
- [ ] **No console errors**: Verificar console en DevTools
- [ ] **Responsive design**: Test en mobile (320px), tablet (768px), desktop (1024px+)
- [ ] **Accessibility**: Validar con Wave o axe DevTools

### Component Functionality

- [ ] File selection: Usuario puede seleccionar archivo
- [ ] File validation: Extensión no permitida rechazada con error
- [ ] File size validation: Archivo > 100 MB rechazado
- [ ] Upload progress: Barra de progreso visible durante upload
- [ ] Upload success: Toast verde y documento aparece en listado
- [ ] Upload error: Mensaje de error visible y reintentar disponible
- [ ] Permission gate: Botón deshabilitado si no tiene permiso
- [ ] Version history: Versiones listadas correctamente ordenadas DESC
- [ ] Version download: Archivo se descarga correctamente
- [ ] Version metadata: Usuario, fecha, tamaño mostrados correctamente

### Error Handling

- [ ] INVALID_FILE_SIZE: Usuario ve "El archivo excede 100 MB"
- [ ] INVALID_FILE_TYPE: Usuario ve "Tipo de archivo no permitido"
- [ ] FORBIDDEN (403): Usuario ve "No tienes permiso para subir aquí"
- [ ] FOLDER_NOT_FOUND (404): Usuario ve "Carpeta no encontrada"
- [ ] SERVICE_UNAVAILABLE (503): Usuario ve "Servicio temporalmente no disponible" con reintento
- [ ] NETWORK_ERROR: Usuario ve "Error de conexión" con reintento automático

### API Integration

- [ ] POST /api/folders/{folderId}/documents: Funciona correctamente
- [ ] GET /api/documents/{documentId}: Obtiene metadatos correctos
- [ ] GET /api/documents/{documentId}/versions: Paginación funciona
- [ ] GET /api/documents/{documentId}/versions/{versionId}/download: Descarga blob
- [ ] POST /api/documents/{documentId}/versions: Sube nueva versión
- [ ] Headers correctos: Authorization, Content-Type, etc.

---

## 6. Error Handling Patterns

### Component Level

```typescript
// En DocumentUpload.tsx
try {
  await uploadFile();
  // Success
  showToast('Documento subido exitosamente', 'success');
} catch (error) {
  // Handled in useDocumentUpload hook
  // Component displays error via UploadError component
}
```

### Service Level

```typescript
// En documentService.ts
export async function uploadDocument(...) {
  try {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await documentApi.post(
      `/folders/${folderId}/documents`,
      formData,
      { headers: { 'Authorization': `Bearer ${token}` } }
    );
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const { code, message, isRetryable } = getErrorDetails(error);
      throw { code, message, isRetryable, status: error.response?.status };
    }
    throw { code: 'UNKNOWN_ERROR', message: 'Error desconocido', isRetryable: false };
  }
}
```

### Hook Level

```typescript
// En useDocumentUpload.ts
const [error, setError] = useState<{ code: string; message: string } | null>(null);

const uploadFile = useCallback(async () => {
  try {
    setUploadState('uploading');
    const result = await uploadDocument(folderId, selectedFile, token, onProgress);
    setUploadState('success');
    return result;
  } catch (err) {
    const { code, message } = getErrorDetails(err);
    setError({ code, message });
    setUploadState('error');
    throw err;
  }
}, [...]);
```

### User-Facing Messages

- Todos los mensajes de error en español
- Mensajes cortos y directos (máx 100 caracteres)
- Incluir acción recomendada cuando corresponda
- Ejemplo: "Tipo de archivo no permitido. Permitidos: PDF, DOC, XLSX, etc."

---

## 7. UI/UX Considerations

### Bootstrap Components

- **Form.Group**: Para inputs de archivo
- **Form.Control**: Para file input styling
- **ProgressBar**: Para mostrar progreso de upload
- **Alert**: Para mostrar errores
- **Button**: Para acciones (Upload, Download, Retry)
- **Badge**: Para etiqueta "[Actual]" en versiones
- **Spinner**: Para loading durante operaciones
- **Modal**: Para modal de historial (si aplica)
- **Pagination**: Para paginación de versiones

### Responsive Design

- **Mobile (320px)**: Single column, touch-friendly buttons
- **Tablet (768px)**: Optimizar spacing
- **Desktop (1024px+)**: Multi-column layout si corresponde
- **Progreso bar**: Siempre visible
- **Tabla de versiones**: Puede convertirse en lista en mobile

### Accessibility Requirements

- [ ] Input file con label asociado (`<label htmlFor="fileInput">`)
- [ ] Botones con `aria-label` descriptivos
- [ ] Progreso barra: `role="progressbar"` con `aria-valuenow`, `aria-valuemin`, `aria-valuemax`
- [ ] Toasts: `role="alert"` para screen readers
- [ ] Keyboard navigation: Tab entre elementos, Enter para activar, ESC para cerrar
- [ ] Contraste: Ratio > 4.5:1 foreground/background
- [ ] Focus visible: outline visible al navegar con teclado

### Loading States

- Barra de progreso durante upload
- Spinner mientras carga versiones
- Botones deshabilitados durante operaciones
- Feedback inmediato en clicks (state change < 100ms)

---

## 8. Dependencies

### External Libraries (already in project or to add)

- **axios**: HTTP client (probablemente ya instalado)
- **react-bootstrap**: UI components (probablemente ya instalado)
- **date-fns**: Date formatting (puede que necesite instalarse)
- **jwt-decode**: JWT decoding (opcional, si no hay alternativa)

### React Hooks (built-in)

- useState
- useCallback
- useEffect
- useContext
- useRef
- useMemo

### Testing Libraries

- **jest**: Unit testing framework
- **@testing-library/react**: React component testing
- **@testing-library/user-event**: User interaction simulation
- **cypress**: E2E testing (probablemente ya instalado)

---

## 9. Notes

### Important Reminders

1. **Todos los mensajes en español**: El proyecto requiere que la UI esté en español. Verificar todos los strings.
2. **No confiar solo en validación de cliente**: La autorización siempre se valida en servidor.
3. **Exponential backoff para reintentos**: 1s, 2s, 4s para no saturar servidor.
4. **Caché estratégico**: Metadatos cacheables por 5 min, versiones no cacheables.
5. **Seguridad de headers**: Incluir `X-Content-Type-Options: nosniff`, `Cache-Control: private`.

### Business Rules

1. Máximo 100 MB por archivo
2. Solo extensiones whitelistadas permitidas
3. Un usuario solo puede subir si tiene permiso `documents:write:{folderId}`
4. Un usuario solo puede descargar si tiene permiso `documents:read:{folderId}`
5. Las versiones forman un historial lineal (no branches)
6. Exactamente una versión marcada como "current" en cualquier momento

### TypeScript Considerations

- Usar `strictNullChecks: true` en tsconfig.json
- Tipado fuerte en servicios y hooks
- Interfaces bien documentadas
- Evitar `any`; usar `unknown` si es necesario y luego castear

### Testing Considerations

- **Unit tests**: Cobertura > 80%
- **E2E tests**: Todos los happy paths + 3 error paths principales
- **Manual testing**: Validar con diferentes navegadores (Chrome, Firefox, Safari)
- **Accessibility testing**: Usar Wave, axe, o Lighthouse

---

## 10. Next Steps After Implementation

1. **Code Review**: Pasar por revisión de equipo (pedir feedback en permisos, error handling, testing)
2. **Manual QA**: Testing exhaustivo en dev environment
3. **Staging Deployment**: Deployar a staging y validar con data real
4. **Performance Testing**: Verificar upload de archivos grandes, velocidades de conexión lentas
5. **Security Review**: Validar que no hay inyección, XSS, CSRF
6. **Monitoring**: Configurar alertas para errores en producción
7. **User Documentation**: Actualizar guía de usuario con instrucciones de carga y historial
8. **Release Notes**: Documentar feature para release notes
9. **Feedback Loop**: Recolectar feedback de usuarios y iterar si necesario
10. **Related Features**: Planificar next phase (ej. búsqueda, notificaciones, restauración de versiones)

---

## 11. Implementation Verification

### Final Checklist

- [ ] **Code Quality**
  - [ ] ESLint: 0 errores, 0 warnings
  - [ ] Prettier: Código formateado correctamente
  - [ ] TypeScript: 0 errores de tipo
  - [ ] Duplicación: Código DRY, sin copypaste

- [ ] **Functionality**
  - [ ] Upload: Flujo completo funciona
  - [ ] Historial: Versionado lineal visible
  - [ ] Permisos: Correctamente respetados
  - [ ] Errores: Todos los casos handleados

- [ ] **Testing**
  - [ ] Unit tests: > 80% coverage
  - [ ] E2E tests: Happy paths + error paths
  - [ ] No console errors
  - [ ] No memory leaks

- [ ] **Integration**
  - [ ] APIs funcionan correctamente
  - [ ] Headers correctos (Authorization, etc.)
  - [ ] Respuestas mapean a tipos TypeScript
  - [ ] Errores HTTP manejados

- [ ] **Documentation**
  - [ ] Code comments donde necesarios
  - [ ] Documentación técnica actualizada
  - [ ] Feature README.md creado/actualizado
  - [ ] API spec actualizado

- [ ] **Accessibility & Performance**
  - [ ] WCAG 2.1 AA compliant
  - [ ] Upload time < 500ms inicial
  - [ ] Historial load < 500ms
  - [ ] Mobile responsive

---

## 12. Summary

Este plan proporciona una guía detallada paso-a-paso para implementar el frontend de US-DOC-006. Incluye:

✅ Estructura de tipos TypeScript completa  
✅ Servicios API robustos con manejo de errores  
✅ Hooks personalizados para lógica de negocio  
✅ Componentes React reutilizables  
✅ Validaciones de cliente exhaustivas  
✅ Verificación de permisos basada en JWT  
✅ Tests unitarios y E2E  
✅ Documentación técnica actualizada  
✅ Consideraciones de UX y accesibilidad  
✅ Patrones de error handling  

El equipo de desarrollo puede seguir estos pasos de forma autónoma y completar la implementación de forma confiable.
