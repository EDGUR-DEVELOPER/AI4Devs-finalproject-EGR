## P3 — Gestión de carpetas: API + UI mínima

### [US-FOLDER-005] UI mínima de navegación por carpetas

---

**Narrativa:** Como usuario, quiero una vista tipo explorador para entrar/salir de carpetas, para encontrar mis documentos.

**Criterios de Aceptación:**
- *Scenario 1:* Dado un usuario autenticado, Cuando entra a una carpeta desde la UI, Entonces ve su contenido y puede navegar a subcarpetas.

**Nota de Alcance:** Esta historia implementa la interfaz de usuario para navegar carpetas, consumiendo las APIs definidas en **US-FOLDER-001**, **US-FOLDER-002**, y **US-FOLDER-004**.

### Descripción Funcional Completa

**Narrativa:** Como usuario autenticado de una organización, necesito una interfaz visual tipo explorador de archivos que me permita navegar intuitivamente por la estructura jerárquica de carpetas de mi organización, visualizando el contenido disponible según mis permisos, y ejecutando acciones (crear, eliminar) de manera contextual y clara, para gestionar eficientemente mis documentos sin necesidad de conocer rutas o IDs técnicos.

**Objetivo Técnico:** Implementar una interfaz de usuario completa que:
- Consuma las APIs de carpetas ya implementadas (US-FOLDER-001, US-FOLDER-002, US-FOLDER-004)
- Proporcione navegación fluida y bidireccional (entrar/salir de carpetas)
- Muestre claramente la jerarquía de navegación (breadcrumb)
- Diferencie visualmente carpetas de documentos
- Controle la visibilidad y habilitación de acciones según permisos del usuario (`puede_escribir`, `puede_administrar`)
- Maneje estados de carga, error y contenido vacío de forma amigable
- Soporte navegación con historial del navegador (back/forward)
- Sea responsive y accesible (teclado, lectores de pantalla)

### Criterios de Aceptación Ampliados

| Scenario | Condición Inicial (Given) | Acción (When) | Resultado Esperado (Then) |
|----------|--------------------------|--------------|--------------------------|
| **1.1 - Navegación básica: Ver raíz** | Usuario autenticado con sesión válida, acceso a carpeta raíz de su organización | Accede a la ruta `/carpetas` o `/explorador` | Ve la vista del explorador mostrando el contenido de la carpeta raíz (subcarpetas y documentos), breadcrumb muestra "Raíz" o nombre de organización |
| **1.2 - Navegación: Entrar a subcarpeta** | Usuario en vista de carpeta padre, visualizando lista de subcarpetas | Hace clic en nombre o icono de una subcarpeta | Navega a la carpeta seleccionada, URL se actualiza a `/carpetas/{id}`, contenido de la nueva carpeta se carga, breadcrumb agrega nuevo nivel |
| **1.3 - Navegación: Salir de carpeta** | Usuario en carpeta nivel 2 o superior, breadcrumb muestra ruta completa | Hace clic en segmento de breadcrumb de carpeta padre | Navega a la carpeta padre seleccionada, URL se actualiza, contenido se recarga |
| **1.4 - Historial del navegador** | Usuario navegó: Raíz → Proyectos → 2024 | Presiona botón "Atrás" del navegador | Retrocede a carpeta "Proyectos", contenido se carga correctamente, breadcrumb se actualiza |
| **1.5 - Deep linking** | Usuario recibe enlace directo `/carpetas/{id}` | Pega URL en navegador y presiona Enter | Carga directamente la carpeta especificada (si tiene permisos), breadcrumb muestra ruta completa desde raíz |
| **1.6 - Contenido filtrado por permisos** | Usuario tiene LECTURA en carpeta padre pero NO en subcarpeta "Confidencial" | Visualiza contenido de carpeta padre | NO ve subcarpeta "Confidencial" en la lista (filtrado por backend según US-FOLDER-002) |
| **1.7 - Diferenciación visual** | Usuario en carpeta con 3 subcarpetas y 5 documentos | Visualiza lista de contenido | Carpetas muestran icono de folder 📁, documentos muestran icono de archivo 📄, se distinguen claramente |
| **1.8 - Estado vacío** | Usuario entra a carpeta recién creada sin contenido | Visualiza carpeta vacía | Ve mensaje "Esta carpeta está vacía" con ícono ilustrativo, botón "Crear subcarpeta" visible si tiene `puede_escribir=true` |
| **1.9 - Estado de carga** | Usuario hace clic en carpeta con 100+ documentos (carga lenta) | Durante tiempo de espera de respuesta de API | Ve skeleton loader o spinner, UI no queda bloqueada |
| **1.10 - Creación de carpeta** | Usuario en carpeta con `puede_escribir=true` | Hace clic en botón "Nueva carpeta", ingresa nombre "Informes 2024", confirma | Modal se cierra, nueva carpeta aparece en lista, notificación de éxito, llamada a `POST /api/carpetas` |
| **1.11 - Creación: validación** | Usuario intenta crear carpeta con nombre vacío | Ingresa "" en campo nombre, intenta crear | Campo muestra error "El nombre es requerido", botón Crear deshabilitado |
| **1.12 - Creación: nombre duplicado** | Usuario intenta crear carpeta con nombre existente en mismo nivel | Ingresa nombre duplicado, confirma | Recibe error 409 de API, muestra mensaje "Ya existe una carpeta con ese nombre" |
| **1.13 - Eliminación de carpeta vacía** | Usuario con `puede_administrar=true` en carpeta vacía | Hace clic en acción "Eliminar", confirma en diálogo | Carpeta desaparece de lista, notificación de éxito, llamada a `DELETE /api/carpetas/{id}` |
| **1.14 - Eliminación: confirmación** | Usuario selecciona eliminar carpeta "Archivos Viejos" | Hace clic en eliminar | Ve diálogo "¿Está seguro que desea eliminar la carpeta 'Archivos Viejos'?", opciones Cancelar/Eliminar |
| **1.15 - Eliminación: carpeta no vacía** | Usuario intenta eliminar carpeta con contenido | Confirma eliminación | Recibe error 409 de API, muestra mensaje "La carpeta debe vaciarse antes de eliminarla" |
| **1.16 - Control de permisos: sin escritura** | Usuario con solo LECTURA en carpeta | Visualiza contenido | Botón "Nueva carpeta" NO visible o deshabilitado con tooltip "No tiene permisos para crear carpetas" |
| **1.17 - Control de permisos: sin administración** | Usuario sin permiso ADMINISTRACIÓN en carpeta específica | Ve menú contextual de carpeta | Opción "Eliminar" NO visible o deshabilitada |
| **1.18 - Error de red** | Usuario navegando, conexión a backend falla | Intenta cargar contenido de carpeta | Ve mensaje de error con botón "Reintentar", puede volver a intentar carga |
| **1.19 - Error 403 (sin permiso)** | Usuario intenta acceder a carpeta sin permisos vía URL directa | Carga `/carpetas/{id-sin-permiso}` | Ve mensaje "No tiene permisos para acceder a esta carpeta" con opción de volver a raíz |
| **1.20 - Error 404 (carpeta inexistente)** | Usuario intenta acceder a ID de carpeta inválido | Carga `/carpetas/{id-invalido}` | Ve mensaje "La carpeta no existe" con opción de volver a raíz |
| **1.21 - Accesibilidad: navegación por teclado** | Usuario usa solo teclado (sin mouse) | Presiona Tab, Enter, Escape | Puede navegar por carpetas, abrir modales, confirmar/cancelar acciones, navegación lógica |
| **1.22 - Responsive: pantalla pequeña** | Usuario en dispositivo móvil (ancho < 768px) | Visualiza explorador | Breadcrumb trunca niveles intermedios, lista de contenido se adapta, acciones accesibles |

### Estructura de Componentes Frontend

#### Arquitectura de Feature (Feature-Driven Clean Architecture)

```
frontend/src/features/folders/
├── api/
│   └── folderApi.ts              # Llamadas HTTP privadas (axios)
├── components/
│   ├── FolderExplorer.tsx         # Componente contenedor principal
│   ├── Breadcrumb.tsx             # Navegación jerárquica
│   ├── FolderList.tsx             # Lista de carpetas y documentos
│   ├── FolderItem.tsx             # Item individual (carpeta o documento)
│   ├── EmptyFolderState.tsx       # Estado vacío
│   ├── CreateFolderModal.tsx      # Modal de creación
│   ├── DeleteFolderDialog.tsx     # Confirmación de eliminación
│   └── FolderContextMenu.tsx      # Menú contextual de acciones
├── hooks/
│   ├── useFolderNavigation.ts     # Lógica de navegación y estado
│   ├── useFolderContent.ts        # React Query para contenido
│   ├── useCreateFolder.ts         # Mutación crear carpeta
│   ├── useDeleteFolder.ts         # Mutación eliminar carpeta
│   └── useBreadcrumb.ts           # Lógica de breadcrumb
├── types/
│   └── folder.types.ts            # Interfaces TypeScript
└── index.ts                        # Exports públicos del feature
```

### Tipos y Contratos de Datos (TypeScript)

#### Interfaces de Dominio

```typescript
// features/folders/types/folder.types.ts

export interface FolderItem {
  id: string;
  nombre: string;
  tipo: 'carpeta';
  fecha_creacion: string;
  puede_escribir: boolean;
  puede_administrar: boolean;
}

export interface DocumentItem {
  id: string;
  nombre: string;
  tipo: 'documento';
  version_actual: number;
  fecha_modificacion: string;
  puede_escribir: boolean;
}

export interface FolderContent {
  subcarpetas: FolderItem[];
  documentos: DocumentItem[];
  total_subcarpetas: number;
  total_documentos: number;
}

export interface BreadcrumbSegment {
  id: string;
  nombre: string;
}

export interface CreateFolderRequest {
  nombre: string;
  descripcion?: string;
  carpeta_padre_id: string;
}

export interface FolderPermissions {
  puede_leer: boolean;
  puede_escribir: boolean;
  puede_administrar: boolean;
}
```

### Estructura de Endpoints Consumidos

**Base URL:** `/api/carpetas`

#### 1. Obtener contenido de carpeta raíz
```http
GET /api/carpetas/raiz
Authorization: Bearer {token}

Response 200:
{
  "subcarpetas": [...],
  "documentos": [...],
  "total_subcarpetas": 5,
  "total_documentos": 12
}
```

#### 2. Obtener contenido de carpeta específica
```http
GET /api/carpetas/{id}/contenido
Authorization: Bearer {token}

Response 200: (igual que raíz)
Response 403: { "codigo": "SIN_PERMISO_LECTURA", "mensaje": "..." }
Response 404: { "codigo": "CARPETA_NO_ENCONTRADA", "mensaje": "..." }
```

#### 3. Obtener ruta de navegación (breadcrumb)
```http
GET /api/carpetas/{id}/ruta
Authorization: Bearer {token}

Response 200:
[
  { "id": "root-id", "nombre": "Raíz" },
  { "id": "parent-id", "nombre": "Proyectos" },
  { "id": "current-id", "nombre": "2024" }
]
```

#### 4. Crear carpeta
```http
POST /api/carpetas
Authorization: Bearer {token}
Content-Type: application/json

{
  "nombre": "Nueva Carpeta",
  "descripcion": "Descripción opcional",
  "carpeta_padre_id": "parent-id"
}

Response 201:
{
  "id": "new-id",
  "nombre": "Nueva Carpeta",
  ...
}

Response 400: Validación
Response 403: Sin permisos
Response 409: Nombre duplicado
```

#### 5. Eliminar carpeta
```http
DELETE /api/carpetas/{id}
Authorization: Bearer {token}

Response 204: (sin contenido)
Response 403: Sin permisos
Response 404: No existe
Response 409: { "codigo": "CARPETA_NO_VACIA", "mensaje": "..." }
```

### Archivos a Modificar/Crear

#### 1. Nuevos archivos del feature

| Archivo | Ubicación | Descripción |
|---------|-----------|-------------|
| `folderApi.ts` | `features/folders/api/` | Cliente HTTP con llamadas a endpoints de carpetas |
| `folder.types.ts` | `features/folders/types/` | Interfaces TypeScript de dominio |
| `FolderExplorer.tsx` | `features/folders/components/` | Componente raíz del explorador |
| `Breadcrumb.tsx` | `features/folders/components/` | Componente de navegación jerárquica |
| `FolderList.tsx` | `features/folders/components/` | Lista de contenido (subcarpetas + documentos) |
| `FolderItem.tsx` | `features/folders/components/` | Representación de item individual |
| `EmptyFolderState.tsx` | `features/folders/components/` | Estado vacío con mensaje y CTA |
| `CreateFolderModal.tsx` | `features/folders/components/` | Modal para crear carpeta |
| `DeleteFolderDialog.tsx` | `features/folders/components/` | Diálogo de confirmación de eliminación |
| `FolderContextMenu.tsx` | `features/folders/components/` | Menú contextual de acciones |
| `useFolderNavigation.ts` | `features/folders/hooks/` | Hook de lógica de navegación |
| `useFolderContent.ts` | `features/folders/hooks/` | React Query hook para contenido |
| `useCreateFolder.ts` | `features/folders/hooks/` | Mutación de creación |
| `useDeleteFolder.ts` | `features/folders/hooks/` | Mutación de eliminación |
| `useBreadcrumb.ts` | `features/folders/hooks/` | Hook de breadcrumb |
| `index.ts` | `features/folders/` | Exports públicos |

#### 2. Configuración de rutas

| Archivo | Ubicación | Cambio |
|---------|-----------|--------|
| `router/index.tsx` | `core/shared/router/` | Agregar rutas `/carpetas` y `/carpetas/:id` |

#### 3. Componentes comunes reutilizables

| Componente | Ubicación | Uso |
|------------|-----------|-----|
| `Button.tsx` | `common/ui/` | Botones de acciones (crear, eliminar, cancelar) |
| `Modal.tsx` | `common/ui/` | Base para modales de creación y confirmación |
| `Spinner.tsx` | `common/ui/` | Indicador de carga |
| `Toast.tsx` | `common/ui/` | Notificaciones de éxito/error |
| `ContextMenu.tsx` | `common/ui/` | Menú contextual genérico |

### Implementación de Servicios (API Client)

#### folderApi.ts

```typescript
// features/folders/api/folderApi.ts
import { axiosClient } from '@/core/shared/api/axiosClient';
import type { 
  FolderContent, 
  BreadcrumbSegment, 
  CreateFolderRequest,
  FolderItem 
} from '../types/folder.types';

const BASE_URL = '/api/carpetas';

export const folderApi = {
  /**
   * Obtiene el contenido de la carpeta raíz
   */
  getRootContent: async (): Promise<FolderContent> => {
    const { data } = await axiosClient.get<FolderContent>(`${BASE_URL}/raiz`);
    return data;
  },

  /**
   * Obtiene el contenido de una carpeta específica
   */
  getFolderContent: async (folderId: string): Promise<FolderContent> => {
    const { data } = await axiosClient.get<FolderContent>(
      `${BASE_URL}/${folderId}/contenido`
    );
    return data;
  },

  /**
   * Obtiene la ruta de navegación (breadcrumb) de una carpeta
   */
  getFolderPath: async (folderId: string): Promise<BreadcrumbSegment[]> => {
    const { data } = await axiosClient.get<BreadcrumbSegment[]>(
      `${BASE_URL}/${folderId}/ruta`
    );
    return data;
  },

  /**
   * Crea una nueva carpeta
   */
  createFolder: async (request: CreateFolderRequest): Promise<FolderItem> => {
    const { data } = await axiosClient.post<FolderItem>(BASE_URL, request);
    return data;
  },

  /**
   * Elimina una carpeta (solo si está vacía)
   */
  deleteFolder: async (folderId: string): Promise<void> => {
    await axiosClient.delete(`${BASE_URL}/${folderId}`);
  },
};
```

### Implementación de Hooks Personalizados

#### useFolderContent.ts (React Query)

```typescript
// features/folders/hooks/useFolderContent.ts
import { useQuery } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';

export const useFolderContent = (folderId: string | 'root') => {
  return useQuery({
    queryKey: ['folderContent', folderId],
    queryFn: () => 
      folderId === 'root' 
        ? folderApi.getRootContent() 
        : folderApi.getFolderContent(folderId),
    staleTime: 1000 * 60 * 5, // 5 minutos
    retry: 2,
  });
};
```

#### useCreateFolder.ts (Mutación)

```typescript
// features/folders/hooks/useCreateFolder.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';
import type { CreateFolderRequest } from '../types/folder.types';

export const useCreateFolder = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateFolderRequest) => folderApi.createFolder(request),
    onSuccess: (_, variables) => {
      // Invalida cache de carpeta padre para refrescar lista
      queryClient.invalidateQueries({ 
        queryKey: ['folderContent', variables.carpeta_padre_id] 
      });
    },
  });
};
```

#### useDeleteFolder.ts (Mutación)

```typescript
// features/folders/hooks/useDeleteFolder.ts
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { folderApi } from '../api/folderApi';

export const useDeleteFolder = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (folderId: string) => folderApi.deleteFolder(folderId),
    onSuccess: () => {
      // Invalida todas las queries de contenido para refrescar
      queryClient.invalidateQueries({ queryKey: ['folderContent'] });
    },
  });
};
```

### Implementación de Componente Principal

#### FolderExplorer.tsx (esquema)

```typescript
// features/folders/components/FolderExplorer.tsx
import { useParams, useNavigate } from 'react-router-dom';
import { useFolderContent } from '../hooks/useFolderContent';
import { useBreadcrumb } from '../hooks/useBreadcrumb';
import { Breadcrumb } from './Breadcrumb';
import { FolderList } from './FolderList';
import { EmptyFolderState } from './EmptyFolderState';
import { Spinner } from '@/common/ui/Spinner';
import { ErrorState } from '@/common/ui/ErrorState';

export const FolderExplorer: React.FC = () => {
  const { folderId = 'root' } = useParams<{ folderId: string }>();
  const navigate = useNavigate();
  
  const { data, isLoading, error, refetch } = useFolderContent(folderId);
  const { breadcrumb } = useBreadcrumb(folderId);

  const handleNavigate = (id: string) => {
    navigate(`/carpetas/${id}`);
  };

  if (isLoading) return <Spinner />;
  if (error) return <ErrorState error={error} onRetry={refetch} />;
  if (!data) return null;

  const isEmpty = data.total_subcarpetas === 0 && data.total_documentos === 0;

  return (
    <div className="folder-explorer">
      <Breadcrumb segments={breadcrumb} onNavigate={handleNavigate} />
      
      {isEmpty ? (
        <EmptyFolderState folderId={folderId} />
      ) : (
        <FolderList 
          content={data} 
          onFolderClick={handleNavigate}
        />
      )}
    </div>
  );
};
```

### Manejo de Errores

#### Tipos de Error y Mensajes de Usuario

| Código HTTP | Código Error | Mensaje Usuario | Acción UI |
|-------------|--------------|-----------------|-----------|
| 400 | `VALIDACION` | "Por favor, corrija los errores en el formulario" | Mostrar errores de campo |
| 403 | `SIN_PERMISO_LECTURA` | "No tiene permisos para acceder a esta carpeta" | Botón "Volver a inicio" |
| 403 | `SIN_PERMISO_ESCRITURA` | "No tiene permisos para crear carpetas aquí" | Deshabilitar acción |
| 404 | `CARPETA_NO_ENCONTRADA` | "La carpeta no existe o fue eliminada" | Botón "Volver a inicio" |
| 409 | `NOMBRE_DUPLICADO` | "Ya existe una carpeta con ese nombre en esta ubicación" | Focus en campo nombre |
| 409 | `CARPETA_NO_VACIA` | "La carpeta debe vaciarse antes de eliminarla" | Cerrar diálogo, mostrar toast |
| 500 | `ERROR_SERVIDOR` | "Ocurrió un error. Por favor, intente nuevamente" | Botón "Reintentar" |
| Network | `SIN_CONEXION` | "No hay conexión. Verifique su red e intente nuevamente" | Botón "Reintentar" |

### Pasos de Implementación (Orden Secuencial)

1. **Setup de tipos e interfaces** (`folder.types.ts`)
2. **Implementar cliente API** (`folderApi.ts`)
3. **Configurar rutas en router** (`router/index.tsx`)
4. **Implementar hooks de React Query** (`useFolderContent`, `useCreateFolder`, `useDeleteFolder`)
5. **Implementar hook de navegación** (`useFolderNavigation`, `useBreadcrumb`)
6. **Crear componente Breadcrumb** (navegación jerárquica)
7. **Crear componentes de lista** (`FolderList`, `FolderItem`)
8. **Crear estado vacío** (`EmptyFolderState`)
9. **Crear modal de creación** (`CreateFolderModal`)
10. **Crear diálogo de eliminación** (`DeleteFolderDialog`)
11. **Crear menú contextual** (`FolderContextMenu`)
12. **Integrar en componente principal** (`FolderExplorer`)
13. **Implementar estados de carga y error**
14. **Implementar control de permisos** (habilitar/deshabilitar acciones)
15. **Pruebas unitarias de componentes** (con mocks)
16. **Pruebas de integración de hooks**
17. **Pruebas E2E de flujo completo**
18. **Revisión de accesibilidad** (teclado, ARIA)
19. **Revisión responsive** (móvil, tablet)
20. **Documentación de componentes** (Storybook opcional)

### Consideraciones de Testing

#### Tests Unitarios (Componentes)

```typescript
// features/folders/components/__tests__/FolderList.test.tsx
import { render, screen } from '@testing-library/react';
import { FolderList } from '../FolderList';

describe('FolderList', () => {
  it('should_renderFolderItems_when_dataProvided', () => {
    const mockData = {
      subcarpetas: [
        { id: '1', nombre: 'Proyectos', tipo: 'carpeta', ... }
      ],
      documentos: [],
      total_subcarpetas: 1,
      total_documentos: 0,
    };

    render(<FolderList content={mockData} onFolderClick={jest.fn()} />);

    expect(screen.getByText('Proyectos')).toBeInTheDocument();
    expect(screen.getByTestId('folder-icon')).toBeInTheDocument();
  });

  it('should_callOnFolderClick_when_folderClicked', () => {
    const mockOnClick = jest.fn();
    const mockData = { ... };

    render(<FolderList content={mockData} onFolderClick={mockOnClick} />);
    
    fireEvent.click(screen.getByText('Proyectos'));

    expect(mockOnClick).toHaveBeenCalledWith('1');
  });

  it('should_differentiateFoldersAndDocuments_when_bothPresent', () => {
    // Verificar iconos diferentes para carpetas vs documentos
  });

  it('should_disableDeleteAction_when_userLacksPermission', () => {
    // Verificar botón eliminar deshabilitado si puede_administrar=false
  });
});
```

#### Tests de Integración (Hooks)

```typescript
// features/folders/hooks/__tests__/useFolderContent.test.ts
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useFolderContent } from '../useFolderContent';
import { folderApi } from '../../api/folderApi';

jest.mock('../../api/folderApi');

describe('useFolderContent', () => {
  it('should_fetchRootContent_when_folderIdIsRoot', async () => {
    (folderApi.getRootContent as jest.Mock).mockResolvedValue({ ... });

    const { result } = renderHook(() => useFolderContent('root'), {
      wrapper: ({ children }) => (
        <QueryClientProvider client={new QueryClient()}>
          {children}
        </QueryClientProvider>
      ),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(folderApi.getRootContent).toHaveBeenCalled();
  });

  it('should_handleError_when_apiFails', async () => {
    (folderApi.getFolderContent as jest.Mock).mockRejectedValue(
      new Error('Network error')
    );

    const { result } = renderHook(() => useFolderContent('folder-id'), { ... });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
```

#### Tests E2E

```typescript
// e2e/folders/navigation.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Folder Navigation', () => {
  test('should_navigateThroughFolders_when_userClicks', async ({ page }) => {
    await page.goto('/carpetas');
    await page.waitForSelector('[data-testid="folder-list"]');

    // Ver raíz
    await expect(page.locator('[data-testid="breadcrumb"]')).toContainText('Raíz');

    // Entrar a subcarpeta
    await page.click('text=Proyectos');
    await expect(page.locator('[data-testid="breadcrumb"]')).toContainText('Raíz / Proyectos');

    // Volver con breadcrumb
    await page.click('[data-testid="breadcrumb-segment-root"]');
    await expect(page.locator('[data-testid="breadcrumb"]')).toContainText('Raíz');
  });

  test('should_createFolder_when_userHasPermission', async ({ page }) => {
    await page.goto('/carpetas');
    await page.click('button:has-text("Nueva carpeta")');
    
    await page.fill('input[name="nombre"]', 'Nueva Carpeta Test');
    await page.click('button:has-text("Crear")');

    await expect(page.locator('text=Nueva Carpeta Test')).toBeVisible();
  });

  test('should_showError_when_deletingNonEmptyFolder', async ({ page }) => {
    // Simular eliminación de carpeta con contenido
    await page.goto('/carpetas/folder-with-content');
    await page.click('[data-testid="folder-context-menu"]');
    await page.click('text=Eliminar');
    await page.click('button:has-text("Eliminar")');

    await expect(page.locator('text=La carpeta debe vaciarse antes de eliminarla'))
      .toBeVisible();
  });
});
```

### Requisitos No Funcionales

#### Rendimiento
- **Tiempo de carga inicial:** < 1 segundo para lista de hasta 100 items
- **Tiempo de navegación:** < 500ms entre carpetas
- **Cache de contenido:** Usar React Query con `staleTime` de 5 minutos
- **Lazy loading:** Implementar paginación virtual si carpeta tiene > 100 items

#### Accesibilidad (WCAG 2.1 Nivel AA)
- Navegación completa por teclado (Tab, Enter, Escape, Arrow keys)
- Roles ARIA apropiados: `role="navigation"` (breadcrumb), `role="list"` (contenido)
- Contraste de colores ≥ 4.5:1 para texto normal
- Focus visible en todos los elementos interactivos
- Labels descriptivos para lectores de pantalla

#### Seguridad
- Validación de permisos en frontend (UI) y backend (obligatorio)
- No exponer IDs técnicos en mensajes de error
- Sanitizar nombres de carpeta para prevenir XSS

#### Responsive
- Breakpoints: Mobile (< 640px), Tablet (640-1024px), Desktop (> 1024px)
- Breadcrumb truncado en móvil (mostrar solo último nivel + dropdown)
- Acciones contextuales adaptadas a touch (botones más grandes)

### Documentación Requerida

#### Para Desarrolladores
- **README del feature:** Arquitectura, hooks disponibles, ejemplos de uso
- **Storybook:** Stories para componentes reutilizables (Breadcrumb, FolderList, Modals)
- **Comentarios JSDoc:** En funciones públicas de API y hooks

#### Para QA
- **Casos de prueba:** Matriz de criterios de aceptación vs tests
- **Escenarios de error:** Cómo reproducir cada tipo de error (403, 404, 409, etc.)

#### Para Usuarios Finales
- **Guía de usuario:** Cómo navegar, crear carpetas, permisos (en wiki o docs del producto)

### Definición de "Completo" (Definition of Done)

- [ ] Todos los componentes implementados y funcionando según especificación
- [ ] Consumo correcto de APIs (US-FOLDER-001, 002, 004)
- [ ] Control de permisos implementado y visible en UI
- [ ] Estados de carga, error, y vacío implementados
- [ ] Navegación con historial del navegador funcionando
- [ ] Responsive y accesible (teclado, ARIA)
- [ ] Tests unitarios escritos (cobertura > 80%)
- [ ] Tests de integración de hooks ejecutándose
- [ ] Al menos 1 test E2E de flujo completo pasando
- [ ] Revisión de código aprobada por al menos 1 peer
- [ ] Documentación técnica actualizada (README del feature)
- [ ] Validado en staging por QA (todos los criterios de aceptación)
- [ ] Sin errores de ESLint ni warnings de TypeScript

---

### Resumen Ejecutivo

Esta historia implementa una **interfaz de usuario completa tipo explorador de archivos** que permite a los usuarios navegar intuitivamente por la estructura jerárquica de carpetas de su organización. Consume las APIs ya desarrolladas en historias previas (US-FOLDER-001, 002, 004) y proporciona una experiencia visual clara con:

- **Navegación bidireccional** (entrar/salir de carpetas con breadcrumb)
- **Control de permisos visual** (acciones habilitadas/deshabilitadas según `puede_escribir`, `puede_administrar`)
- **Estados de UI claros** (carga, error, vacío)
- **Operaciones CRUD básicas** (crear carpeta, eliminar carpeta vacía)
- **Accesibilidad y responsive** (teclado, ARIA, móvil)

El desarrollo sigue la **arquitectura Feature-Driven Clean** del proyecto, separando API calls, lógica de negocio (hooks), y presentación (componentes), con integración de **React Query** para gestión de estado del servidor y **React Router** para navegación con historial.
