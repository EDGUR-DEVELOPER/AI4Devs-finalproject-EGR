# Frontend Implementation Plan: US-FOLDER-005 - UI mínima de navegación por carpetas

## Overview

Esta historia implementa una interfaz de usuario completa tipo explorador de archivos que permite a los usuarios autenticados navegar intuitivamente por la estructura jerárquica de carpetas de su organización. La implementación consume las APIs REST ya desarrolladas en US-FOLDER-001 (crear carpeta), US-FOLDER-002 (listar contenido), y US-FOLDER-004 (eliminar carpeta vacía).

**Principios de Arquitectura Frontend:**
- **Feature-Driven Clean Architecture**: Separación clara de capas (API → Logic/State → Presentation)
- **Component-Based Design**: Componentes reutilizables con responsabilidad única
- **State Management**: React Query para server state + Zustand (si se requiere estado global)
- **Accessibility First**: Navegación por teclado, ARIA labels, contraste WCAG 2.1 AA
- **Responsive Design**: Mobile-first con Tailwind CSS utility classes
- **Type Safety**: TypeScript strict mode, todas las interfaces tipadas

**Restricciones Clave:**
- El feature **NO debe importar otros features** (usar eventos/callbacks si es necesario)
- Todos los textos en español (mensajes, etiquetas, tooltips)
- Código técnico en inglés (nombres de componentes, variables, funciones)
- Gestión de permisos en UI (habilitar/deshabilitar acciones según `puede_escribir`, `puede_administrar`)
- Control de errores específicos (403, 404, 409, 500) con mensajes amigables

---

## Architecture Context

### Feature Structure (Feature-Driven Clean Architecture)

```
frontend/src/features/folders/
├── api/
│   └── folderApi.ts              # API calls (Axios, private to feature)
├── components/
│   ├── FolderExplorer.tsx         # Container principal del explorador
│   ├── Breadcrumb.tsx             # Navegación jerárquica (ruta de carpetas)
│   ├── FolderList.tsx             # Lista de subcarpetas y documentos
│   ├── FolderItem.tsx             # Item individual (carpeta o documento)
│   ├── EmptyFolderState.tsx       # Estado vacío con mensaje y CTA
│   ├── CreateFolderModal.tsx      # Modal de creación de carpeta
│   ├── DeleteFolderDialog.tsx     # Diálogo de confirmación de eliminación
│   └── FolderContextMenu.tsx      # Menú contextual con acciones
├── hooks/
│   ├── useFolderContent.ts        # React Query hook para contenido
│   ├── useCreateFolder.ts         # Mutación de creación
│   ├── useDeleteFolder.ts         # Mutación de eliminación
│   ├── useBreadcrumb.ts           # Lógica de breadcrumb/navegación
│   └── useFolderNavigation.ts     # Hook de navegación (opcional)
├── types/
│   └── folder.types.ts            # Interfaces TypeScript de dominio
└── index.ts                        # Barrel export (public API)
```

### Components/Services Involved

**New Files:**
- `features/folders/api/folderApi.ts`
- `features/folders/types/folder.types.ts`
- `features/folders/components/` (8 componentes)
- `features/folders/hooks/` (5 hooks custom)
- `features/folders/index.ts`

**Modified Files:**
- `core/shared/router/AppRouter.tsx` (agregar rutas `/carpetas` y `/carpetas/:id`)

**Common UI Components Used:**
- `@ui/forms/Button` (acciones crear, eliminar, cancelar)
- `@ui/forms/Input` (campo nombre de carpeta)
- `@ui/notifications/useNotificationStore` (toasts de éxito/error)
- Layout/Page components (si existen)

### Routing Considerations

**Nueva ruta raíz de carpetas:**
- Path: `/carpetas` → Muestra contenido de carpeta raíz
- Component: `<FolderExplorer />`
- Protected: ✅ (requiere autenticación)

**Ruta de carpeta específica:**
- Path: `/carpetas/:id` → Muestra contenido de carpeta con ID dinámica
- Component: `<FolderExplorer />` (mismo componente, ID desde `useParams`)
- Protected: ✅
- Deep linking: ✅ (soporte de URL directas)

### State Management Approach

**Server State (React Query):**
- Cache de contenido de carpetas (5 minutos de `staleTime`)
- Invalidación automática post mutaciones (crear/eliminar)
- Manejo de estados: `isLoading`, `isError`, `error`, `data`

**Local Component State (useState):**
- Modales abiertos/cerrados (crear, eliminar)
- Selección de item actual
- Estados de validación de formularios

**No se requiere Zustand** para este feature (todo es server state o local state)

---

## Implementation Steps

### **Step 1: Create TypeScript Types and Interfaces**

**File**: `frontend/src/features/folders/types/folder.types.ts`

**Action**: Definir todos los tipos TypeScript de dominio para carpetas y documentos

**Implementation Steps**:

1. **Crear archivo de tipos con interfaces de dominio:**

```typescript
/**
 * Tipos de dominio para el feature de carpetas
 * Basados en los contratos de API de US-FOLDER-001, US-FOLDER-002
 */

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
  puede_leer: boolean;
  puede_escribir: boolean;
  puede_administrar: boolean;
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
  puede_leer: boolean;
  puede_escribir: boolean;
  puede_descargar: boolean;
}

/**
 * Contenido completo de una carpeta (respuesta de API)
 */
export interface FolderContent {
  subcarpetas: FolderItem[];
  documentos: DocumentItem[];
  total_subcarpetas: number;
  total_documentos: number;
}

/**
 * Segmento de breadcrumb para navegación
 */
export interface BreadcrumbSegment {
  id: string;
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
  puede_leer: boolean;
  puede_escribir: boolean;
  puede_administrar: boolean;
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
```

2. **Validar sintaxis y tipos:**
   ```bash
   cd frontend
   npm run type-check
   ```

**Dependencies**:
- No requiere imports externos (tipos nativos de TypeScript)

**Implementation Notes**:
- Usar interfaces en lugar de types cuando sea posible (mejor para extensión)
- Todos los nombres de propiedades en inglés técnico (snake_case para match con API)
- Comentarios JSDoc para documentar cada interface
- `as const` en enums para type safety estricto

---

### **Step 2: Create Folder API Service**

**File**: `frontend/src/features/folders/api/folderApi.ts`

**Action**: Implementar cliente HTTP privado para todos los endpoints de carpetas

**Implementation Steps**:

1. **Crear servicio de API con axios:**

```typescript
/**
 * Folder API service
 * Private API client for folder operations (US-FOLDER-001, US-FOLDER-002, US-FOLDER-004)
 * Consumes endpoints from document-core service via gateway
 */
import { apiClient } from '@core/shared/api/axiosInstance';
import type {
  FolderContent,
  BreadcrumbSegment,
  CreateFolderRequest,
  CreateFolderResponse,
} from '../types/folder.types';

const BASE_URL = '/carpetas'; // Via gateway: /api/carpetas

/**
 * Obtener contenido de carpeta raíz
 * GET /api/carpetas/raiz
 */
export const getRootContent = async (): Promise<FolderContent> => {
  const { data } = await apiClient.get<FolderContent>(`${BASE_URL}/raiz`);
  return data;
};

/**
 * Obtener contenido de carpeta específica
 * GET /api/carpetas/{id}/contenido
 */
export const getFolderContent = async (folderId: string): Promise<FolderContent> => {
  const { data } = await apiClient.get<FolderContent>(
    `${BASE_URL}/${folderId}/contenido`
  );
  return data;
};

/**
 * Obtener ruta de navegación (breadcrumb) de una carpeta
 * GET /api/carpetas/{id}/ruta
 */
export const getFolderPath = async (folderId: string): Promise<BreadcrumbSegment[]> => {
  const { data } = await apiClient.get<BreadcrumbSegment[]>(
    `${BASE_URL}/${folderId}/ruta`
  );
  return data;
};

/**
 * Crear nueva carpeta
 * POST /api/carpetas
 */
export const createFolder = async (
  request: CreateFolderRequest
): Promise<CreateFolderResponse> => {
  const { data } = await apiClient.post<CreateFolderResponse>(BASE_URL, request);
  return data;
};

/**
 * Eliminar carpeta vacía (soft delete)
 * DELETE /api/carpetas/{id}
 */
export const deleteFolder = async (folderId: string): Promise<void> => {
  await apiClient.delete(`${BASE_URL}/${folderId}`);
};

// Export default para conveniencia
export const folderApi = {
  getRootContent,
  getFolderContent,
  getFolderPath,
  createFolder,
  deleteFolder,
};
```

2. **Validar imports y compilación:**
   ```bash
   npm run type-check
   ```

**Dependencies**:
- `@core/shared/api/axiosInstance` (cliente axios configurado)
- `../types/folder.types` (interfaces locales)

**Implementation Notes**:
- Todas las funciones son `async`
- Usar desestructuración `{ data }` para extraer respuesta
- Errores son manejados automáticamente por interceptor de axios (401, 403, etc.)
- No exponer `apiClient` directamente, solo funciones específicas

---

### **Step 3: Create React Query Hooks**

**Files**: 
- `frontend/src/features/folders/hooks/useFolderContent.ts`
- `frontend/src/features/folders/hooks/useCreateFolder.ts`
- `frontend/src/features/folders/hooks/useDeleteFolder.ts`

**Action**: Implementar hooks personalizados para gestión de server state con React Query

**Implementation Steps**:

1. **Hook para obtener contenido (Query):**

**File**: `useFolderContent.ts`

```typescript
/**
 * React Query hook para obtener contenido de carpeta
 * Maneja cache, loading states, y error handling
 */
import { useQuery } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';

export const useFolderContent = (folderId: string | 'root') => {
  return useQuery({
    queryKey: ['folderContent', folderId],
    queryFn: () => 
      folderId === 'root' 
        ? folderApi.getRootContent() 
        : folderApi.getFolderContent(folderId),
    staleTime: 1000 * 60 * 5, // 5 minutos - contenido relativamente estable
    retry: 2, // Reintentar 2 veces antes de fallar
    refetchOnWindowFocus: false, // No refetch automático al volver a la tab
  });
};
```

2. **Hook para crear carpeta (Mutation):**

**File**: `useCreateFolder.ts`

```typescript
/**
 * React Query mutation hook para crear carpeta
 * Invalida cache automáticamente después de creación exitosa
 */
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';
import type { CreateFolderRequest } from '../types/folder.types';

export const useCreateFolder = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateFolderRequest) => folderApi.createFolder(request),
    onSuccess: (data, variables) => {
      // Invalida cache de carpeta padre para refrescar lista
      const parentId = variables.carpeta_padre_id || 'root';
      queryClient.invalidateQueries({ 
        queryKey: ['folderContent', parentId] 
      });
      
      // También invalidar breadcrumb si existe
      queryClient.invalidateQueries({ 
        queryKey: ['folderPath'] 
      });
    },
  });
};
```

3. **Hook para eliminar carpeta (Mutation):**

**File**: `useDeleteFolder.ts`

```typescript
/**
 * React Query mutation hook para eliminar carpeta vacía
 * Invalida cache y navega a carpeta padre si es necesario
 */
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';

export const useDeleteFolder = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (folderId: string) => folderApi.deleteFolder(folderId),
    onSuccess: () => {
      // Invalida todas las queries de contenido para refrescar
      queryClient.invalidateQueries({ queryKey: ['folderContent'] });
      queryClient.invalidateQueries({ queryKey: ['folderPath'] });
    },
  });
};
```

**Dependencies**:
- `@tanstack/react-query` (useQuery, useMutation, useQueryClient)
- `../api/folderApi` (funciones de API)
- `../types/folder.types` (tipos)

**Implementation Notes**:
- `staleTime: 5min` es apropiado para carpetas (no cambian frecuentemente)
- `retry: 2` balancea UX (no dar up muy rápido) vs latencia
- Invalidación de cache es clave para consistencia de datos
- No incluir lógica de navegación en mutations (separar concerns)

---

### **Step 4: Create Breadcrumb Hook and Component**

**Files**:
- `frontend/src/features/folders/hooks/useBreadcrumb.ts`
- `frontend/src/features/folders/components/Breadcrumb.tsx`

**Action**: Implementar navegación jerárquica visual (breadcrumb)

**Implementation Steps**:

1. **Hook para obtener ruta de navegación:**

**File**: `useBreadcrumb.ts`

```typescript
/**
 * Hook para obtener ruta de breadcrumb de una carpeta
 */
import { useQuery } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';

export const useBreadcrumb = (folderId: string | 'root') => {
  const query = useQuery({
    queryKey: ['folderPath', folderId],
    queryFn: () => folderApi.getFolderPath(folderId),
    enabled: folderId !== 'root', // No ejecutar query si estamos en raíz
    staleTime: 1000 * 60 * 10, // 10 minutos - rutas no cambian
  });

  // Si estamos en raíz, retornar breadcrumb manualmente
  const breadcrumb = folderId === 'root' 
    ? [{ id: 'root', nombre: 'Raíz' }]
    : query.data || [];

  return {
    breadcrumb,
    isLoading: query.isLoading,
    error: query.error,
  };
};
```

2. **Componente visual de Breadcrumb:**

**File**: `Breadcrumb.tsx`

```typescript
/**
 * Breadcrumb component - Navegación jerárquica de carpetas
 * Muestra ruta clickeable para navegar a carpetas padre
 */
import React from 'react';
import type { BreadcrumbSegment } from '../types/folder.types';

interface BreadcrumbProps {
  segments: BreadcrumbSegment[];
  onNavigate: (folderId: string) => void;
}

export const Breadcrumb: React.FC<BreadcrumbProps> = ({ segments, onNavigate }) => {
  if (segments.length === 0) {
    return null;
  }

  return (
    <nav 
      className="flex items-center space-x-2 text-sm text-gray-600 mb-4 py-2" 
      aria-label="Breadcrumb"
    >
      {segments.map((segment, index) => {
        const isLast = index === segments.length - 1;
        
        return (
          <React.Fragment key={segment.id}>
            {index > 0 && (
              <span className="text-gray-400" aria-hidden="true">
                /
              </span>
            )}
            
            {isLast ? (
              <span 
                className="font-semibold text-gray-900"
                aria-current="page"
              >
                {segment.nombre}
              </span>
            ) : (
              <button
                onClick={() => onNavigate(segment.id)}
                className="hover:underline hover:text-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded px-1"
                data-testid={`breadcrumb-segment-${segment.id}`}
              >
                {segment.nombre}
              </button>
            )}
          </React.Fragment>
        );
      })}
    </nav>
  );
};
```

**Dependencies**:
- React
- `../types/folder.types`

**Implementation Notes**:
- Último segmento NO es clickeable (página actual)
- Separador visual "/" entre segmentos
- `aria-label` para accesibilidad
- Focus ring visible para navegación por teclado
- Estilo Tailwind responsive

---

### **Step 5: Create Folder List Components**

**Files**:
- `frontend/src/features/folders/components/FolderList.tsx`
- `frontend/src/features/folders/components/FolderItem.tsx`

**Action**: Implementar lista visual de carpetas y documentos

**Implementation Steps**:

1. **Componente de lista contenedor:**

**File**: `FolderList.tsx`

```typescript
/**
 * FolderList - Lista de subcarpetas y documentos
 * Muestra contenido completo con separación visual entre tipos
 */
import React from 'react';
import { FolderItem } from './FolderItem';
import type { FolderContent } from '../types/folder.types';

interface FolderListProps {
  content: FolderContent;
  onFolderClick: (folderId: string) => void;
  onDeleteClick: (folderId: string) => void;
}

export const FolderList: React.FC<FolderListProps> = ({
  content,
  onFolderClick,
  onDeleteClick,
}) => {
  const hasFolders = content.subcarpetas.length > 0;
  const hasDocuments = content.documentos.length > 0;

  return (
    <div className="space-y-6" data-testid="folder-list">
      {/* Subcarpetas */}
      {hasFolders && (
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wide">
            Carpetas ({content.total_subcarpetas})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {content.subcarpetas.map((folder) => (
              <FolderItem
                key={folder.id}
                item={folder}
                onClick={() => onFolderClick(folder.id)}
                onDeleteClick={onDeleteClick}
              />
            ))}
          </div>
        </section>
      )}

      {/* Documentos */}
      {hasDocuments && (
        <section>
          <h2 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wide">
            Documentos ({content.total_documentos})
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {content.documentos.map((document) => (
              <FolderItem
                key={document.id}
                item={document}
                onClick={() => {
                  // TODO: Navegación a detalle de documento (Post-MVP)
                  console.log('Ver documento:', document.id);
                }}
                onDeleteClick={onDeleteClick}
              />
            ))}
          </div>
        </section>
      )}
    </div>
  );
};
```

2. **Componente de item individual:**

**File**: `FolderItem.tsx`

```typescript
/**
 * FolderItem - Tarjeta individual de carpeta o documento
 * Diferenciación visual por tipo, con acciones contextuales
 */
import React, { useState } from 'react';
import type { ContentItem } from '../types/folder.types';

interface FolderItemProps {
  item: ContentItem;
  onClick: () => void;
  onDeleteClick: (id: string) => void;
}

export const FolderItem: React.FC<FolderItemProps> = ({
  item,
  onClick,
  onDeleteClick,
}) => {
  const [menuOpen, setMenuOpen] = useState(false);
  const isFolder = item.tipo === 'carpeta';
  
  // Determinar si usuario puede eliminar
  const canDelete = 'puede_administrar' in item && item.puede_administrar;

  return (
    <div
      className="relative bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer group"
      onClick={onClick}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onClick();
        }
      }}
      role="button"
      tabIndex={0}
      aria-label={`${isFolder ? 'Carpeta' : 'Documento'}: ${item.nombre}`}
    >
      {/* Icono */}
      <div className="flex items-start justify-between">
        <div className="flex items-center space-x-3">
          <div className="text-3xl" aria-hidden="true">
            {isFolder ? '📁' : '📄'}
          </div>
          
          <div className="flex-1 min-w-0">
            <h3 className="text-sm font-medium text-gray-900 truncate">
              {item.nombre}
            </h3>
            
            {/* Info adicional */}
            <p className="text-xs text-gray-500 mt-1">
              {isFolder ? (
                <>
                  {item.num_subcarpetas || 0} carpetas, {item.num_documentos || 0} documentos
                </>
              ) : (
                <>
                  Versión {item.version_actual} · {formatDate(item.fecha_modificacion)}
                </>
              )}
            </p>
          </div>
        </div>

        {/* Menú contextual (solo carpetas con permiso admin) */}
        {isFolder && canDelete && (
          <div className="relative">
            <button
              onClick={(e) => {
                e.stopPropagation();
                setMenuOpen(!menuOpen);
              }}
              className="p-1 text-gray-400 hover:text-gray-600 rounded opacity-0 group-hover:opacity-100 transition-opacity focus:opacity-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
              aria-label="Opciones"
              aria-haspopup="true"
              aria-expanded={menuOpen}
            >
              ⋮
            </button>

            {menuOpen && (
              <div 
                className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg border border-gray-200 z-10"
                role="menu"
              >
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onDeleteClick(item.id);
                    setMenuOpen(false);
                  }}
                  className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 rounded-md"
                  role="menuitem"
                >
                  Eliminar carpeta
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Badges de permisos (desarrollo/debug) */}
      {process.env.NODE_ENV === 'development' && (
        <div className="flex gap-1 mt-2">
          {item.puede_leer && <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">Leer</span>}
          {'puede_escribir' in item && item.puede_escribir && (
            <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">Escribir</span>
          )}
          {'puede_administrar' in item && item.puede_administrar && (
            <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded">Admin</span>
          )}
        </div>
      )}
    </div>
  );
};

/**
 * Formatea fecha ISO a formato humano
 */
function formatDate(isoDate: string): string {
  const date = new Date(isoDate);
  return new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
}
```

**Dependencies**:
- React (useState)
- `../types/folder.types`

**Implementation Notes**:
- Grid responsive: 1 columna móvil, 2 tablet, 3 desktop
- Icono emoji temporal (cambiar por iconos SVG en futuro)
- Menú contextual solo visible en hover o focus (accesibilidad)
- `e.stopPropagation()` para evitar navegación al hacer clic en menú
- Badges de permisos solo en desarrollo para debugging

---

### **Step 6: Create Empty State Component**

**File**: `frontend/src/features/folders/components/EmptyFolderState.tsx`

**Action**: Implementar estado vacío con CTA para crear carpeta

**Implementation Steps**:

```typescript
/**
 * EmptyFolderState - Estado vacío cuando carpeta no tiene contenido
 * Muestra mensaje descriptivo y botón de acción si usuario tiene permisos
 */
import React from 'react';
import { Button } from '@ui/forms/Button';

interface EmptyFolderStateProps {
  canWrite: boolean;
  onCreateClick: () => void;
}

export const EmptyFolderState: React.FC<EmptyFolderStateProps> = ({
  canWrite,
  onCreateClick,
}) => {
  return (
    <div 
      className="flex flex-col items-center justify-center py-16 px-4 text-center"
      role="status"
      aria-live="polite"
    >
      {/* Icono ilustrativo */}
      <div className="text-6xl mb-4" aria-hidden="true">
        📂
      </div>

      {/* Mensaje */}
      <h3 className="text-lg font-medium text-gray-900 mb-2">
        Esta carpeta está vacía
      </h3>
      
      <p className="text-sm text-gray-500 mb-6 max-w-md">
        {canWrite 
          ? 'Comienza creando una nueva carpeta para organizar tus documentos.'
          : 'No hay contenido disponible en esta carpeta.'}
      </p>

      {/* CTA solo si tiene permisos */}
      {canWrite && (
        <Button
          onClick={onCreateClick}
          variant="primary"
          fullWidth={false}
        >
          + Nueva carpeta
        </Button>
      )}
    </div>
  );
};
```

**Dependencies**:
- `@ui/forms/Button`

**Implementation Notes**:
- Mostrar CTA solo si `canWrite === true`
- `aria-live="polite"` para lectores de pantalla
- Mensaje diferente según permisos
- Icono grande para claridad visual

---

### **Step 7: Create Create Folder Modal**

**File**: `frontend/src/features/folders/components/CreateFolderModal.tsx`

**Action**: Implementar modal de creación de carpeta con validación

**Implementation Steps**:

```typescript
/**
 * CreateFolderModal - Modal para crear nueva carpeta
 * Incluye validación de formulario y manejo de errores
 */
import React, { useState, useEffect } from 'react';
import { Button } from '@ui/forms/Button';
import { Input } from '@ui/forms/Input';
import { useCreateFolder } from '../hooks/useCreateFolder';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';
import { FOLDER_ERROR_CODES } from '../types/folder.types';

interface CreateFolderModalProps {
  isOpen: boolean;
  onClose: () => void;
  parentFolderId: string | null;
}

export const CreateFolderModal: React.FC<CreateFolderModalProps> = ({
  isOpen,
  onClose,
  parentFolderId,
}) => {
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const createFolderMutation = useCreateFolder();
  const { addNotification } = useNotificationStore();

  // Reset form cuando se cierra modal
  useEffect(() => {
    if (!isOpen) {
      setNombre('');
      setDescripcion('');
      setErrorMessage('');
    }
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    // Validación local
    if (!nombre.trim()) {
      setErrorMessage('El nombre es requerido');
      return;
    }

    if (nombre.length > 255) {
      setErrorMessage('El nombre no puede exceder 255 caracteres');
      return;
    }

    try {
      await createFolderMutation.mutateAsync({
        nombre: nombre.trim(),
        descripcion: descripcion.trim() || undefined,
        carpeta_padre_id: parentFolderId,
      });

      addNotification({
        type: 'success',
        message: `Carpeta "${nombre}" creada exitosamente`,
      });

      onClose();
    } catch (error: any) {
      // Mapear errores específicos del backend
      const errorCode = error.response?.data?.codigo;
      
      if (errorCode === FOLDER_ERROR_CODES.DUPLICATE_NAME) {
        setErrorMessage('Ya existe una carpeta con ese nombre en esta ubicación');
      } else if (errorCode === FOLDER_ERROR_CODES.NO_PERMISSION_WRITE) {
        setErrorMessage('No tienes permisos para crear carpetas aquí');
      } else {
        setErrorMessage('Ocurrió un error al crear la carpeta. Intenta nuevamente.');
      }
    }
  };

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
    >
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <h2 id="modal-title" className="text-xl font-semibold text-gray-900">
            Nueva carpeta
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl leading-none focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
            aria-label="Cerrar"
          >
            ×
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Nombre *"
            name="nombre"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            placeholder="Ej: Informes 2024"
            autoFocus
            maxLength={255}
            required
          />

          <div>
            <label htmlFor="descripcion" className="block text-sm font-medium text-gray-700 mb-1">
              Descripción (opcional)
            </label>
            <textarea
              id="descripcion"
              name="descripcion"
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
              placeholder="Agrega una descripción..."
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              rows={3}
              maxLength={500}
            />
            <p className="text-xs text-gray-500 mt-1">
              {descripcion.length}/500 caracteres
            </p>
          </div>

          {/* Error message */}
          {errorMessage && (
            <div 
              className="p-3 bg-red-50 border border-red-200 rounded-md"
              role="alert"
            >
              <p className="text-sm text-red-800">{errorMessage}</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <Button
              type="button"
              variant="secondary"
              onClick={onClose}
              fullWidth
              disabled={createFolderMutation.isPending}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={createFolderMutation.isPending}
              disabled={!nombre.trim()}
              fullWidth
            >
              Crear carpeta
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
```

**Dependencies**:
- `@ui/forms/Button`, `@ui/forms/Input`
- `@ui/notifications/useNotificationStore`
- React hooks (useState, useEffect)
- `useCreateFolder` hook
- `../types/folder.types`

**Implementation Notes**:
- Validación local + manejo de errores del servidor
- Disabled submit si nombre vacío
- Loading state mientras crea (botón con spinner)
- Toast de éxito después de creación
- Limpieza de formulario al cerrar modal
- Backdrop oscuro para enfocar atención

---

### **Step 8: Create Delete Folder Dialog**

**File**: `frontend/src/features/folders/components/DeleteFolderDialog.tsx`

**Action**: Implementar diálogo de confirmación para eliminación

**Implementation Steps**:

```typescript
/**
 * DeleteFolderDialog - Diálogo de confirmación para eliminar carpeta
 * Solo permite eliminar carpetas vacías (validación en backend)
 */
import React from 'react';
import { Button } from '@ui/forms/Button';
import { useDeleteFolder } from '../hooks/useDeleteFolder';
import { useNotificationStore } from '@ui/notifications/useNotificationStore';
import { FOLDER_ERROR_CODES } from '../types/folder.types';

interface DeleteFolderDialogProps {
  isOpen: boolean;
  onClose: () => void;
  folderId: string;
  folderName: string;
}

export const DeleteFolderDialog: React.FC<DeleteFolderDialogProps> = ({
  isOpen,
  onClose,
  folderId,
  folderName,
}) => {
  const deleteFolderMutation = useDeleteFolder();
  const { addNotification } = useNotificationStore();

  const handleDelete = async () => {
    try {
      await deleteFolderMutation.mutateAsync(folderId);

      addNotification({
        type: 'success',
        message: `Carpeta "${folderName}" eliminada exitosamente`,
      });

      onClose();
    } catch (error: any) {
      const errorCode = error.response?.data?.codigo;

      if (errorCode === FOLDER_ERROR_CODES.FOLDER_NOT_EMPTY) {
        addNotification({
          type: 'error',
          message: 'La carpeta debe estar vacía antes de eliminarla',
        });
      } else if (errorCode === FOLDER_ERROR_CODES.NO_PERMISSION_ADMIN) {
        addNotification({
          type: 'error',
          message: 'No tienes permisos para eliminar esta carpeta',
        });
      } else {
        addNotification({
          type: 'error',
          message: 'Ocurrió un error al eliminar la carpeta',
        });
      }
    }
  };

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="dialog-title"
    >
      <div className="bg-white rounded-lg shadow-xl max-w-sm w-full mx-4 p-6">
        {/* Icono de advertencia */}
        <div className="text-4xl text-red-500 mb-4 text-center">
          ⚠️
        </div>

        {/* Mensaje */}
        <h2 id="dialog-title" className="text-lg font-semibold text-gray-900 mb-2 text-center">
          ¿Eliminar carpeta?
        </h2>
        
        <p className="text-sm text-gray-600 text-center mb-6">
          ¿Estás seguro que deseas eliminar la carpeta <strong>"{folderName}"</strong>?
          <br />
          Esta acción no se puede deshacer.
        </p>

        {/* Acciones */}
        <div className="flex gap-3">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            fullWidth
            disabled={deleteFolderMutation.isPending}
          >
            Cancelar
          </Button>
          
          <Button
            type="button"
            onClick={handleDelete}
            loading={deleteFolderMutation.isPending}
            fullWidth
            className="bg-red-600 hover:bg-red-700 text-white"
          >
            Eliminar
          </Button>
        </div>
      </div>
    </div>
  );
};
```

**Dependencies**:
- `@ui/forms/Button`
- `@ui/notifications/useNotificationStore`
- `useDeleteFolder` hook
- `../types/folder.types`

**Implementation Notes**:
- Confirmación explícita antes de eliminar
- Icono de advertencia para claridad
- Nombre de carpeta en negrita para contexto
- Botón rojo para acción destructiva
- Manejo de error específico cuando carpeta no está vacía

---

### **Step 9: Create Main FolderExplorer Component**

**File**: `frontend/src/features/folders/components/FolderExplorer.tsx`

**Action**: Implementar componente contenedor principal que orquesta todo el feature

**Implementation Steps**:

```typescript
/**
 * FolderExplorer - Componente principal del explorador de carpetas
 * Orquesta navegación, listado, creación y eliminación
 */
import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useFolderContent } from '../hooks/useFolderContent';
import { useBreadcrumb } from '../hooks/useBreadcrumb';
import { Breadcrumb } from './Breadcrumb';
import { FolderList } from './FolderList';
import { EmptyFolderState } from './EmptyFolderState';
import { CreateFolderModal } from './CreateFolderModal';
import { DeleteFolderDialog } from './DeleteFolderDialog';
import { Button } from '@ui/forms/Button';

export const FolderExplorer: React.FC = () => {
  const { folderId = 'root' } = useParams<{ folderId: string }>();
  const navigate = useNavigate();

  // State local para modales
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; name: string } | null>(null);

  // React Query hooks
  const { data, isLoading, error, refetch } = useFolderContent(folderId);
  const { breadcrumb, isLoading: breadcrumbLoading } = useBreadcrumb(folderId);

  // Handlers de navegación
  const handleNavigate = (targetId: string) => {
    if (targetId === 'root') {
      navigate('/carpetas');
    } else {
      navigate(`/carpetas/${targetId}`);
    }
  };

  const handleDeleteClick = (id: string) => {
    const folder = data?.subcarpetas.find((f) => f.id === id);
    if (folder) {
      setDeleteTarget({ id, name: folder.nombre });
    }
  };

  // Estados de carga
  if (isLoading || breadcrumbLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" />
        <span className="ml-3 text-gray-600">Cargando...</span>
      </div>
    );
  }

  // Estado de error
  if (error) {
    const errorCode = (error as any).response?.data?.codigo;
    const is403 = (error as any).response?.status === 403;
    const is404 = (error as any).response?.status === 404;

    return (
      <div className="flex flex-col items-center justify-center min-h-screen px-4">
        <div className="text-6xl mb-4">🚫</div>
        <h2 className="text-2xl font-semibold text-gray-900 mb-2">
          {is403 && 'Sin permisos'}
          {is404 && 'Carpeta no encontrada'}
          {!is403 && !is404 && 'Error al cargar'}
        </h2>
        <p className="text-gray-600 mb-6 text-center max-w-md">
          {is403 && 'No tienes permisos para acceder a esta carpeta.'}
          {is404 && 'La carpeta no existe o ha sido eliminada.'}
          {!is403 && !is404 && 'Ocurrió un error al cargar el contenido.'}
        </p>
        <div className="flex gap-3">
          <Button onClick={() => refetch()} variant="secondary" fullWidth={false}>
            Reintentar
          </Button>
          <Button onClick={() => navigate('/carpetas')} variant="primary" fullWidth={false}>
            Ir a raíz
          </Button>
        </div>
      </div>
    );
  }

  // Sin datos (no debería ocurrir)
  if (!data) return null;

  const isEmpty = data.total_subcarpetas === 0 && data.total_documentos === 0;
  
  // Determinar permisos del usuario en carpeta actual
  // (asumimos que si puede listar, puede leer; verificar puede_escribir desde primer item)
  const canWrite = data.subcarpetas[0]?.puede_escribir ?? false;

  return (
    <div className="max-w-7xl mx-auto px-4 py-6">
      {/* Header con breadcrumb y acciones */}
      <div className="flex items-center justify-between mb-6">
        <Breadcrumb segments={breadcrumb} onNavigate={handleNavigate} />
        
        {canWrite && (
          <Button
            onClick={() => setIsCreateModalOpen(true)}
            variant="primary"
            fullWidth={false}
          >
            + Nueva carpeta
          </Button>
        )}
      </div>

      {/* Contenido principal */}
      {isEmpty ? (
        <EmptyFolderState
          canWrite={canWrite}
          onCreateClick={() => setIsCreateModalOpen(true)}
        />
      ) : (
        <FolderList
          content={data}
          onFolderClick={handleNavigate}
          onDeleteClick={handleDeleteClick}
        />
      )}

      {/* Modales */}
      <CreateFolderModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        parentFolderId={folderId === 'root' ? null : folderId}
      />

      {deleteTarget && (
        <DeleteFolderDialog
          isOpen={true}
          onClose={() => setDeleteTarget(null)}
          folderId={deleteTarget.id}
          folderName={deleteTarget.name}
        />
      )}
    </div>
  );
};
```

**Dependencies**:
- React (useState)
- react-router-dom (useParams, useNavigate)
- All custom hooks y components del feature
- `@ui/forms/Button`

**Implementation Notes**:
- Componente orquestador: coordina navegación, modales, estados
- Manejo explícito de loading, error (403, 404), y empty states
- Permisos determinados desde primer elemento (todos deberían tener mismos permisos en carpeta)
- Navegación con `useNavigate` para soporte de historial del navegador
- Layout responsive (`max-w-7xl mx-auto`)

---

### **Step 10: Create Feature Barrel Export**

**File**: `frontend/src/features/folders/index.ts`

**Action**: Definir API pública del feature (barrel export)

**Implementation Steps**:

```typescript
/**
 * Folders feature barrel export
 * Public API for the folders feature
 */

// Main component (public)
export { FolderExplorer } from './components/FolderExplorer';

// Types for external use
export type {
  FolderItem,
  DocumentItem,
  FolderContent,
  BreadcrumbSegment,
} from './types/folder.types';

// Hooks re-exported for testing or advanced usage (optional)
export { useFolderContent } from './hooks/useFolderContent';
export { useCreateFolder } from './hooks/useCreateFolder';
export { useDeleteFolder } from './hooks/useDeleteFolder';
```

**Implementation Notes**:
- Solo exportar lo necesario para uso externo
- No exportar componentes internos (FolderList, FolderItem, etc.) para mantener encapsulación
- Hooks pueden ser opcionales en export si no se usan fuera del feature

---

### **Step 11: Add Routes to Router**

**File**: `frontend/src/core/shared/router/AppRouter.tsx`

**Action**: Agregar rutas protegidas para explorador de carpetas

**Implementation Steps**:

1. **Importar componente del feature:**

```typescript
import { FolderExplorer } from '@features/folders';
```

2. **Agregar rutas dentro del componente de rutas protegidas:**

```typescript
// Dentro del router (buscar sección de rutas protegidas)
<Route
  path="/carpetas"
  element={
    <PrivateRoute>
      <FolderExplorer />
    </PrivateRoute>
  }
/>

<Route
  path="/carpetas/:id"
  element={
    <PrivateRoute>
      <FolderExplorer />
    </PrivateRoute>
  }
/>
```

**Implementation Notes**:
- Ambas rutas usan el mismo componente `<FolderExplorer />`
- `/carpetas` muestra carpeta raíz (sin parámetro)
- `/carpetas/:id` muestra carpeta específica por ID
- Ambas son rutas protegidas (requieren autenticación)
- Soporte de deep linking automático

---

### **Step 12: Write Unit Tests for Components**

**Files**: 
- `frontend/src/features/folders/components/__tests__/FolderList.test.tsx`
- `frontend/src/features/folders/components/__tests__/FolderItem.test.tsx`
- `frontend/src/features/folders/components/__tests__/Breadcrumb.test.tsx`

**Action**: Escribir tests unitarios para componentes clave

**Implementation Steps**:

1. **Test para FolderList:**

**File**: `FolderList.test.tsx`

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { FolderList } from '../FolderList';
import type { FolderContent } from '../../types/folder.types';

describe('FolderList', () => {
  const mockContent: FolderContent = {
    subcarpetas: [
      {
        id: '1',
        nombre: 'Proyectos',
        tipo: 'carpeta',
        fecha_creacion: '2024-01-01T00:00:00Z',
        puede_leer: true,
        puede_escribir: true,
        puede_administrar: true,
        num_subcarpetas: 2,
        num_documentos: 5,
      },
    ],
    documentos: [],
    total_subcarpetas: 1,
    total_documentos: 0,
  };

  it('should_renderFolderItems_when_dataProvided', () => {
    render(
      <FolderList
        content={mockContent}
        onFolderClick={jest.fn()}
        onDeleteClick={jest.fn()}
      />
    );

    expect(screen.getByText('Proyectos')).toBeInTheDocument();
    expect(screen.getByText(/Carpetas \(1\)/i)).toBeInTheDocument();
  });

  it('should_callOnFolderClick_when_folderClicked', () => {
    const mockOnClick = jest.fn();

    render(
      <FolderList
        content={mockContent}
        onFolderClick={mockOnClick}
        onDeleteClick={jest.fn()}
      />
    );

    fireEvent.click(screen.getByText('Proyectos'));

    expect(mockOnClick).toHaveBeenCalledWith('1');
  });

  it('should_renderBothSections_when_foldersAndDocumentsPresent', () => {
    const contentWithBoth = {
      ...mockContent,
      documentos: [
        {
          id: 'doc1',
          nombre: 'Informe.pdf',
          tipo: 'documento' as const,
          version_actual: 1,
          fecha_creacion: '2024-01-01T00:00:00Z',
          fecha_modificacion: '2024-01-01T00:00:00Z',
          puede_leer: true,
          puede_escribir: false,
          puede_descargar: true,
        },
      ],
      total_documentos: 1,
    };

    render(
      <FolderList
        content={contentWithBoth}
        onFolderClick={jest.fn()}
        onDeleteClick={jest.fn()}
      />
    );

    expect(screen.getByText(/Carpetas \(1\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Documentos \(1\)/i)).toBeInTheDocument();
  });
});
```

2. **Test para Breadcrumb:**

**File**: `Breadcrumb.test.tsx`

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { Breadcrumb } from '../Breadcrumb';
import type { BreadcrumbSegment } from '../../types/folder.types';

describe('Breadcrumb', () => {
  const mockSegments: BreadcrumbSegment[] = [
    { id: 'root', nombre: 'Raíz' },
    { id: '1', nombre: 'Proyectos' },
    { id: '2', nombre: '2024' },
  ];

  it('should_renderAllSegments_when_provided', () => {
    render(<Breadcrumb segments={mockSegments} onNavigate={jest.fn()} />);

    expect(screen.getByText('Raíz')).toBeInTheDocument();
    expect(screen.getByText('Proyectos')).toBeInTheDocument();
    expect(screen.getByText('2024')).toBeInTheDocument();
  });

  it('should_makeLastSegmentNonClickable_when_current', () => {
    render(<Breadcrumb segments={mockSegments} onNavigate={jest.fn()} />);

    const lastSegment = screen.getByText('2024');
    expect(lastSegment.tagName).toBe('SPAN'); // No es button
  });

  it('should_callOnNavigate_when_segmentClicked', () => {
    const mockOnNavigate = jest.fn();

    render(<Breadcrumb segments={mockSegments} onNavigate={mockOnNavigate} />);

    fireEvent.click(screen.getByText('Raíz'));

    expect(mockOnNavigate).toHaveBeenCalledWith('root');
  });

  it('should_renderNull_when_noSegments', () => {
    const { container } = render(
      <Breadcrumb segments={[]} onNavigate={jest.fn()} />
    );

    expect(container.firstChild).toBeNull();
  });
});
```

**Dependencies**:
- `@testing-library/react`
- `jest` (configurado en proyecto)

**Implementation Notes**:
- Usar convención `should_action_when_condition` para nombres de tests
- Mocks explícitos para funciones callback
- Verificar tanto renderizado como interacción (eventos)
- Tests focused: un aspecto por test

---

### **Step 13: Write Integration Tests for Hooks**

**Files**: 
- `frontend/src/features/folders/hooks/__tests__/useFolderContent.test.ts`
- `frontend/src/features/folders/hooks/__tests__/useCreateFolder.test.ts`

**Action**: Escribir tests de integración para React Query hooks

**Implementation Steps**:

**File**: `useFolderContent.test.ts`

```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useFolderContent } from '../useFolderContent';
import { folderApi } from '../../api/folderApi';

// Mock del módulo de API
jest.mock('../../api/folderApi');

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('useFolderContent', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should_fetchRootContent_when_folderIdIsRoot', async () => {
    const mockData = { subcarpetas: [], documentos: [], total_subcarpetas: 0, total_documentos: 0 };
    (folderApi.getRootContent as jest.Mock).mockResolvedValue(mockData);

    const { result } = renderHook(() => useFolderContent('root'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(folderApi.getRootContent).toHaveBeenCalled();
    expect(result.current.data).toEqual(mockData);
  });

  it('should_fetchFolderContent_when_folderIdProvided', async () => {
    const mockData = { subcarpetas: [], documentos: [], total_subcarpetas: 0, total_documentos: 0 };
    (folderApi.getFolderContent as jest.Mock).mockResolvedValue(mockData);

    const { result } = renderHook(() => useFolderContent('folder-123'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(folderApi.getFolderContent).toHaveBeenCalledWith('folder-123');
    expect(result.current.data).toEqual(mockData);
  });

  it('should_handleError_when_apiFails', async () => {
    const mockError = new Error('Network error');
    (folderApi.getRootContent as jest.Mock).mockRejectedValue(mockError);

    const { result } = renderHook(() => useFolderContent('root'), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.error).toBeDefined();
  });
});
```

**Dependencies**:
- `@testing-library/react` (renderHook, waitFor)
- `@tanstack/react-query`
- `jest`

**Implementation Notes**:
- Wrapper con QueryClient para tests
- `retry: false` para tests más rápidos
- Mocks de API module completo
- `waitFor` para esperar actualizaciones asíncronas

---

### **Step 14: Update Technical Documentation**

**Action**: Actualizar documentación técnica según cambios implementados

**Implementation Steps**:

1. **Revisar archivos de documentación afectados:**
   - `ai-specs/specs/api-spec.yml` (si hay cambios en endpoints, pero no aplica aquí)
   - `ai-specs/specs/frontend-standards.md` (si hay nuevos patrones o componentes)
   - `frontend/README.md` (agregar sección de features)

2. **Actualizar `ai-specs/specs/api-spec.yml`:**
   - Verificar que endpoints `/api/carpetas` estén documentados correctamente
   - Si faltan, agregar ejemplos de uso desde frontend

3. **Actualizar `ai-specs/specs/frontend-standards.md`:**
   - Agregar ejemplo de feature completo (folders) en sección "Feature-Driven Clean Architecture"
   - Documentar patrón de barrel exports si no está explicado

Ejemplo de sección a agregar:

```markdown
## Feature Example: Folders

The **folders** feature implements a complete file explorer UI following Feature-Driven Clean Architecture:

**Structure:**
- `api/folderApi.ts` - Private HTTP client (Axios)
- `types/folder.types.ts` - Domain interfaces
- `components/` - UI components (8 components)
- `hooks/` - React Query custom hooks (5 hooks)
- `index.ts` - Barrel export (public API)

**Key Patterns:**
- **Encapsulation**: API client is private to feature
- **React Query**: Server state management with automatic cache invalidation
- **Type Safety**: All props and responses fully typed
- **Permissions UI**: Actions enabled/disabled based on `puede_escribir`, `puede_administrar`
- **Error Handling**: Specific error codes mapped to user-friendly messages
```

4. **Actualizar `frontend/README.md`:**
   - Agregar feature "folders" a lista de features disponibles
   - Incluir rutas y componente principal

Ejemplo:

```markdown
## Features

### Folders (`features/folders`)
- **Component**: `<FolderExplorer />`
- **Routes**: 
  - `/carpetas` - Root folder content
  - `/carpetas/:id` - Specific folder content
- **Capabilities**: Navigate, create, delete (empty) folders
- **APIs Consumed**: 
  - GET `/api/carpetas/raiz`
  - GET `/api/carpetas/{id}/contenido`
  - POST `/api/carpetas`
  - DELETE `/api/carpetas/{id}`
```

5. **Verificar cambios en documentación:**
   - Compilar proyecto: `npm run build`
   - Revisar que no haya enlaces rotos o referencias incorrectas

**Notes**: 
- Documentación en **inglés** (estándar técnico del proyecto)
- Mantener consistencia con estructura existente
- Este paso es **OBLIGATORIO** antes de considerar implementación completa

---

## Implementation Order

Seguir este orden secuencial para máxima efectividad:

* ***Step 1**: Create TypeScript Types and Interfaces
* ***Step 2**: Create Folder API Service
* ***Step 3**: Create React Query Hooks
* ***Step 4**: Create Breadcrumb Hook and Component
* ***Step 5**: Create Folder List Components
* ***Step 6**: Create Empty State Component
* ***Step 7**: Create Create Folder Modal
* ***Step 8**: Create Delete Folder Dialog
* **Step 9**: Create Main FolderExplorer Component
* **Step 10**: Create Feature Barrel Export
* **Step 11**: Add Routes to Router
* **Step 12**: Write Unit Tests for Components
* **Step 13**: Write Integration Tests for Hooks
* **Step 14**: Update Technical Documentation

**Total Estimated Time**: 8-12 horas (dependiendo de experiencia con stack)

---

## Testing Checklist

### Post-Implementation Manual Testing

- [ ] **Navegación básica:**
  - [ ] Acceder a `/carpetas` muestra contenido de raíz
  - [ ] Hacer clic en carpeta navega a `/carpetas/:id`
  - [ ] Breadcrumb permite volver a carpetas padre
  - [ ] Botón "Atrás" del navegador funciona correctamente

- [ ] **Creación de carpeta:**
  - [ ] Botón "Nueva carpeta" visible si tiene permisos escritura
  - [ ] Modal se abre al hacer clic
  - [ ] Validación local: nombre requerido, max 255 caracteres
  - [ ] Creación exitosa muestra toast y carpeta aparece en lista
  - [ ] Error 409 (nombre duplicado) muestra mensaje específico

- [ ] **Eliminación de carpeta:**
  - [ ] Menú contextual visible solo en carpetas con permiso admin
  - [ ] Diálogo de confirmación antes de eliminar
  - [ ] Eliminación exitosa muestra toast
  - [ ] Error 409 (carpeta no vacía) muestra mensaje específico

- [ ] **Estados visuales:**
  - [ ] Loading spinner durante carga de contenido
  - [ ] Estado vacío con mensaje apropiado
  - [ ] Error 403 muestra "Sin permisos"
  - [ ] Error 404 muestra "Carpeta no encontrada"

- [ ] **Permisos:**
  - [ ] Botón crear carpeta invisible si no tiene `puede_escribir`
  - [ ] Botón eliminar invisible si no tiene `puede_administrar`

- [ ] **Responsive:**
  - [ ] Layout funcional en móvil (< 640px)
  - [ ] Grid de carpetas adapta columnas (1 móvil, 2 tablet, 3 desktop)
  - [ ] Breadcrumb legible en pantallas pequeñas

- [ ] **Accesibilidad:**
  - [ ] Navegación por teclado (Tab, Enter, Escape)
  - [ ] Focus visible en elementos interactivos
  - [ ] Lectores de pantalla anuncian contenido (verificar con NVDA/JAWS)

### Automated Test Coverage

- [ ] **Unit Tests:**
  - [ ] `FolderList.test.tsx` (3+ tests)
  - [ ] `FolderItem.test.tsx` (3+ tests)
  - [ ] `Breadcrumb.test.tsx` (4+ tests)
  
- [ ] **Integration Tests:**
  - [ ] `useFolderContent.test.ts` (3+ tests)
  - [ ] `useCreateFolder.test.ts` (2+ tests)
  - [ ] `useDeleteFolder.test.ts` (2+ tests)

- [ ] **Coverage Target:** > 80% en componentes críticos

---

## Error Handling Patterns

### Error Code Mapping (Frontend ↔ Backend)

| HTTP Status | Backend Code                  | User Message (ES)                                              | UI Action                              |
|-------------|-------------------------------|---------------------------------------------------------------|----------------------------------------|
| 400         | `VALIDACION`                  | "Por favor, corrija los errores en el formulario"            | Mostrar errores de campo               |
| 403         | `SIN_PERMISO_LECTURA`         | "No tiene permisos para acceder a esta carpeta"              | Botón "Volver a inicio"                |
| 403         | `SIN_PERMISO_ESCRITURA`       | "No tiene permisos para crear carpetas aquí"                 | Deshabilitar botón crear               |
| 403         | `SIN_PERMISO_ADMINISTRACION`  | "No tiene permisos para eliminar esta carpeta"               | Deshabilitar botón eliminar            |
| 404         | `CARPETA_NO_ENCONTRADA`       | "La carpeta no existe o fue eliminada"                       | Botón "Volver a inicio"                |
| 409         | `NOMBRE_DUPLICADO`            | "Ya existe una carpeta con ese nombre en esta ubicación"     | Focus en campo nombre                  |
| 409         | `CARPETA_NO_VACIA`            | "La carpeta debe vaciarse antes de eliminarla"               | Cerrar diálogo, mostrar toast error    |
| 500         | `ERROR_SERVIDOR`              | "Ocurrió un error. Por favor, intente nuevamente"           | Botón "Reintentar"                     |
| Network     | `SIN_CONEXION`                | "No hay conexión. Verifique su red e intente nuevamente"    | Botón "Reintentar"                     |

### Error Handling Implementation

**En API Service** (`folderApi.ts`):
- Errores propagados automáticamente por interceptor de axios
- No catch genérico en funciones de API

**En React Query Hooks**:
- Error handling en `onError` callback (opcional)
- Acceso a error via `mutation.error` o `query.error`

**En Componentes**:
- Mapeo explícito de códigos de error a mensajes
- Toast notifications para feedback inmediato
- Estados de error con botones de acción ("Reintentar", "Volver")

---

## UI/UX Considerations

### Tailwind CSS Component Patterns

**Layout:**
- Container: `max-w-7xl mx-auto px-4 py-6`
- Grid responsive: `grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3`
- Flexbox: `flex items-center justify-between`

**Buttons:**
- Primary: `btn-primary` (clase global o definir inline)
- Secondary: `btn-secondary`
- Danger: `bg-red-600 hover:bg-red-700 text-white`

**Cards:**
- Base: `bg-white border border-gray-200 rounded-lg p-4`
- Hover: `hover:shadow-md transition-shadow`
- Interactive: `cursor-pointer`

**Modals:**
- Backdrop: `fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50`
- Content: `bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6`

### Responsive Breakpoints

- **Mobile**: < 640px (1 columna)
- **Tablet**: 640px - 1024px (2 columnas)
- **Desktop**: > 1024px (3 columnas)

### Accessibility Requirements

- **WCAG 2.1 Level AA** compliance
- Tab navigation: Todos los elementos interactivos accesibles por teclado
- Focus indicators: Visible focus ring (`focus:ring-2 focus:ring-blue-500`)
- ARIA attributes:
  - `role="dialog"`, `aria-modal="true"` en modales
  - `role="button"` en elementos clickeables no-nativos
  - `aria-label` descriptivos
- Contraste: ≥ 4.5:1 para texto normal
- Semántica HTML: `<nav>`, `<button>`, `<section>`

### Loading States

- Skeleton loaders (opcional): Para mejor UX en carga inicial
- Spinner full-screen: Para navegación entre carpetas
- Button loading: Spinner inline en botones durante mutaciones

---

## Dependencies

### External Libraries

**Core:**
- `react` ^19.2.0
- `react-dom` ^19.2.0
- `react-router-dom` ^7.9.6
- `typescript` ^5.9.3

**State Management:**
- `@tanstack/react-query` ^5.90.11
- `@tanstack/react-query-devtools` ^5.91.1

**HTTP Client:**
- `axios` ^1.13.2

**Testing:**
- `@testing-library/react` (version en package.json)
- `@testing-library/jest-dom`
- `jest` (configurado en proyecto)

**Styling:**
- `tailwindcss` ^4.1.17

### Internal Dependencies (Path Aliases)

- `@core/*` → `src/core/*`
- `@features/*` → `src/features/*`
- `@ui/*` → `src/common/ui/*`

### Package Installation

No se requiere instalación de paquetes adicionales - todas las dependencias ya están en `package.json`.

---

## Notes

### Important Reminders

1. **Lenguaje:**
   - Código técnico (componentes, variables, funciones): **Inglés**
   - Mensajes de usuario (UI, errores, notificaciones): **Español**
   - Documentación técnica: **Inglés**

2. **Branch Strategy:**
   - SIEMPRE crear rama específica `feature/US-FOLDER-005-frontend`
   - No trabajar en rama general del ticket si existe
   - Separar commits por paso lógico (no commits masivos)

3. **Testing:**
   - TDD preferido: escribir tests antes si es posible
   - Mínimo: tests después de implementación
   - Coverage target: > 80% en componentes críticos

4. **Permisos:**
   - Verificar `puede_escribir` antes de mostrar botón crear
   - Verificar `puede_administrar` antes de mostrar botón eliminar
   - Backend es fuente de verdad - frontend solo controla UI

5. **Errores:**
   - Mapear códigos específicos del backend
   - Mensajes amigables en español
   - Acciones claras (reintentar, volver, cancelar)

6. **Performance:**
   - React Query cachea 5 minutos (`staleTime`)
   - Invalidación automática post-mutaciones
   - No re-fetch innecesarios (`refetchOnWindowFocus: false`)

### Business Rules

- Solo carpetas vacías pueden ser eliminadas (validación en backend)
- Nombres de carpeta únicos por nivel (validación backend + feedback frontend)
- Jerarquía de permisos: ADMINISTRACIÓN > ESCRITURA > LECTURA
- Multi-tenancy: Usuario solo ve carpetas de su organización

### Language Requirements

**Spanish Only (User-Facing):**
- UI labels: "Nueva carpeta", "Eliminar", "Cancelar"
- Error messages: "Ya existe una carpeta con ese nombre"
- Notifications: "Carpeta creada exitosamente"
- Empty states: "Esta carpeta está vacía"

**English Only (Technical):**
- Component names: `FolderExplorer`, `CreateFolderModal`
- Variables: `folderId`, `handleNavigate`, `isLoading`
- Types: `FolderContent`, `BreadcrumbSegment`
- Function names: `createFolder`, `deleteFolder`

---

## Next Steps After Implementation

### Integration Tasks

1. **Dashboard Integration:**
   - Agregar botón "Explorar carpetas" en dashboard principal
   - Link directo: `<Link to="/carpetas">Mis Carpetas</Link>`

2. **Navigation Menu:**
   - Agregar item "Carpetas" en sidebar/navbar global
   - Icono: 📁 o SVG apropiado

3. **Documents Feature (Post-MVP):**
   - Cuando se implemente US-DOC-*, integrar navegación desde carpetas a documentos
   - Link en `FolderItem` para documentos: `onClick={() => navigate(`/documentos/${doc.id}`)}`

### Deployment Tasks

1. **Environment Variables:**
   - Verificar que `VITE_API_BASE_URL` esté configurado
   - No requiere variables adicionales

2. **Build:**
   ```bash
   npm run build
   npm run preview # Test production build locally
   ```

3. **Documentation:**
   - Actualizar changelog con feature completado
   - Agregar screenshots a wiki (opcional)

---

## Implementation Verification

### Final Checklist Before Marking as "Done"

#### Code Quality
- [ ] No errores de TypeScript (`npm run type-check`)
- [ ] No errores de ESLint (`npm run lint`)
- [ ] Código formateado (Prettier si aplica)
- [ ] No console.logs en producción (solo desarrollo)

#### Functionality
- [ ] Todos los criterios de aceptación implementados (ver US-FOLDER-005)
- [ ] Navegación funciona (entrar/salir carpetas, breadcrumb, browser back/forward)
- [ ] Crear carpeta con validación y manejo de errores
- [ ] Eliminar carpeta vacía con confirmación
- [ ] Permisos controlados en UI

#### Testing
- [ ] Tests unitarios escritos y pasando (`npm run test`)
- [ ] Tests de integración pasando
- [ ] Coverage > 80% en componentes críticos
- [ ] Manual testing completo (ver Testing Checklist)

#### Integration
- [ ] Rutas agregadas a router
- [ ] Feature exportado correctamente (`index.ts`)
- [ ] No importa otros features directamente

#### Documentation
- [ ] `ai-specs/specs/frontend-standards.md` actualizado
- [ ] `frontend/README.md` actualizado con nuevo feature
- [ ] Comentarios JSDoc en funciones públicas
- [ ] Este plan de implementación archivado en `ai-specs/changes/`

#### Deployment
- [ ] Build de producción exitoso (`npm run build`)
- [ ] No warnings críticos en build
- [ ] Validado en staging (si aplica)

#### Code Review
- [ ] Pull Request creado con descripción clara
- [ ] Al menos 1 peer reviewer aprobó
- [ ] Todas las conversaciones resueltas

---

## Summary

Esta implementación crea un **explorador de carpetas completo y funcional** siguiendo los principios de **Feature-Driven Clean Architecture** del proyecto. El feature está completamente encapsulado, consume las APIs REST ya implementadas en backend, y proporciona una experiencia de usuario intuitiva con:

- ✅ Navegación bidireccional (entrar/salir carpetas + breadcrumb)
- ✅ Operaciones CRUD básicas (crear carpeta, eliminar carpeta vacía)
- ✅ Control de permisos visual (acciones habilitadas/deshabilitadas)
- ✅ Manejo robusto de errores con mensajes específicos
- ✅ Estados de UI claros (loading, error, empty)
- ✅ Responsive y accesible (teclado, ARIA, móvil)
- ✅ Tests automatizados (unitarios + integración)

El desarrollo sigue estrictamente las convenciones del proyecto (nombres en inglés, mensajes en español, separación de capas, barrel exports) y está listo para integrarse con futuros features de documentos (US-DOC-*).

**Tiempo estimado total**: 8-12 horas para desarrollador con experiencia en React + TypeScript + React Query.
