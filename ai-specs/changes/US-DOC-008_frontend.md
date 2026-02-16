# Plan de Implementación Frontend: US-DOC-008 Eliminación de Documento desde la UI

## 1. Descripción General

Esta User Story implementa la funcionalidad de eliminación lógica (soft delete) de documentos desde la interfaz de usuario. La eliminación es controlada mediante permisos ACL (Access Control List), requiere confirmación explícita del usuario mediante diálogo modal, y emite eventos de auditoría en el backend para trazabilidad completa.

**Objetivo del frontend:**
- Mostrar botón de eliminación solo si el usuario tiene permisos de ESCRITURA o ADMINISTRACION
- Presentar diálogo de confirmación claro y amigable antes de proceder
- Llamar al endpoint `DELETE /api/documentos/{id}` en el backend
- Manejar errores de forma descriptiva (401, 403, 404, 409)
- Actualizar automáticamente la lista de documentos tras eliminación exitosa
- Proporcionar feedback visual mediante notificaciones de éxito o error

**Principios arquitectónicos aplicados:**
- Componentes React reutilizables y tipados con TypeScript
- Separation of Concerns: lógica en hooks, presentación en componentes
- Clean Component Architecture enfocada en features (`src/features/documents/`)
- React Query para invalidación de cache
- Manejo robusto de errores con mensajes amigables en español

---

## 2. Contexto de Arquitectura

### 2.1 Estructura de Carpetas Frontend

```
frontend/
├── src/
│   ├── features/
│   │   ├── documents/                    # Feature: Gestión de documentos
│   │   │   ├── api/
│   │   │   │   └── documentService.ts    # Funciones API para documentos
│   │   │   ├── components/
│   │   │   │   ├── DocumentDeleteButton.tsx              # NUEVO
│   │   │   │   └── DocumentDeleteConfirmDialog.tsx       # NUEVO
│   │   │   ├── hooks/
│   │   │   │   └── useDocumentDelete.ts                  # NUEVO
│   │   │   └── types/
│   │   │       └── document.types.ts     # Tipos TypeScript
│   │   ├── acl/                          # Feature: Control de permisos
│   │   │   └── components/
│   │   │       └── AclDocumentoList.tsx  # Lista de documentos
│   │   └── folders/                      # Feature: Gestión de carpetas
│   │       └── components/
│   │           └── FolderList.tsx        # Lista de documentos en carpeta
│   ├── common/
│   │   ├── components/
│   │   │   ├── Dialog/                   # Componentes reutilizables
│   │   │   ├── Button/
│   │   │   └── Notification/
│   │   ├── hooks/
│   │   │   └── useNotification.ts        # Hook de notificaciones
│   │   └── api/
│   │       └── axiosInstance.ts          # Cliente axios configurado
│   └── core/
│       ├── shared/
│       │   └── constants/
│       │       └── permissions.ts        # Constantes de permisos
│       └── context/
│           └── AuthContext.tsx           # Contexto de autenticación
```

### 2.2 Dependencias y Librerías

- **React**: Framework UI (v18+)
- **TypeScript**: Tipado estático
- **Axios**: Cliente HTTP para llamadas API
- **React Query**: Gestión de estado y cache de datos
- **Tailwind CSS**: Estilos y componentes
- **Lucide React**: Iconografía
- **Zod**: Validación de esquemas (opcional)
- **Cypress**: Testing E2E

### 2.3 Flujo de Permisos

```
Usuario intenta eliminar documento
    ↓
¿Tiene permiso ESCRITURA o ADMINISTRACION?
    ├─ NO → Botón no se renderiza (null)
    └─ SI → Botón visible
         ↓
Usuario hace clic en botón
    ↓
Diálogo de confirmación se abre
    ↓
¿Usuario confirma eliminación?
    ├─ NO → Diálogo se cierra, documento permanece
    └─ SI → Envía DELETE a backend
         ↓
Backend valida permisos nuevamente
    ├─ Éxito → Notificación de éxito, lista se actualiza
    ├─ Error 403 → Notificación de permiso denegado
    ├─ Error 404 → Notificación de documento no encontrado
    ├─ Error 409 → Notificación de documento ya eliminado
    └─ Error 500 → Notificación de error genérico
```

### 2.4 Gestión de Estado

- **Permisos**: Obtenidos del atributo `permisoEfectivo` del documento (del backend via ACL)
- **Estado de eliminación**: Hook local con `useState` en `useDocumentDelete`
- **Cache de documentos**: React Query invalidación mediante `queryClient.invalidateQueries`
- **Notificaciones**: Sistema centralizado `useNotification` hook

---

## 3. Plan de Implementación Frontend

### Paso 0: Crear Rama de Características

**Acción**: Crear rama de feature siguiendo convenciones del proyecto

**Pasos de implementación:**
1. Asegurar que está en rama base (`develop` o `main`)
2. Ejecutar: `git pull origin develop` (o rama base del proyecto)
3. Crear rama: `git checkout -b feature/US-DOC-008-delete-document`
4. Verificar creación: `git branch` (debe mostrar rama activa)
5. Hacer push de rama: `git push origin feature/US-DOC-008-delete-document`

**Notas:**
- Usar convención `feature/[TICKET-ID]-[descripción-en-kebab-case]`
- Esto separará la rama frontend de cualquier rama general del ticket
- Ver `ai-specs/specs/frontend-standards.mdc` para más detalles de workflow

---

### Paso 1: Extender Servicio API — documentService.ts

**Archivo**: `frontend/src/features/documents/api/documentService.ts`

**Acción**: Agregar función `deleteDocument()` para llamar al endpoint DELETE del backend

**Firma de función:**
```typescript
export async function deleteDocument(documentId: string): Promise<void>
```

**Pasos de implementación:**
1. Abrir archivo `frontend/src/features/documents/api/documentService.ts`
2. Importar cliente axios: `import { apiClient } from '@core/shared/api/axiosInstance';`
3. Agregar nueva función después de función `updateDocument()` o similar:
   ```typescript
   /**
    * Elimina (soft delete) un documento del sistema.
    * 
    * Marca el documento con fecha_eliminacion sin eliminar físicamente
    * el registro ni los archivos de versiones.
    * 
    * @param documentId ID del documento a eliminar
    * @throws AxiosError con códigos:
    *   - 401: No autenticado (token JWT expirado o inválido)
    *   - 403: Sin permisos de ESCRITURA o ADMINISTRACION
    *   - 404: Documento no encontrado o pertenece a otra organización
    *   - 409: Documento ya está eliminado
    *   - 500: Error interno del servidor
    * 
    * @example
    * try {
    *   await deleteDocument("123");
    *   console.log("Documento eliminado exitosamente");
    * } catch (error) {
    *   if (error.response?.status === 403) {
    *     console.error("Sin permisos para eliminar");
    *   }
    * }
    */
   export async function deleteDocument(documentId: string): Promise<void> {
     await apiClient.delete(`/api/documentos/${documentId}`);
   }
   ```
4. Guardar cambios

**Dependencias:**
```typescript
import { apiClient } from '@core/shared/api/axiosInstance';
```

**Notas de implementación:**
- Los headers JWT, X-User-Id, X-Organization-Id se agregan automáticamente por interceptores
- La función no retorna datos (backend retorna 204 No Content)
- Los errores se propagan al caller para manejo en hook o componente
- TypeScript infiere `Promise<void>` automáticamente

---

### Paso 2: Crear Hook Personalizado — useDocumentDelete

**Archivo**: `frontend/src/features/documents/hooks/useDocumentDelete.ts` (NUEVO)

**Acción**: Crear hook que orqueste lógica de eliminación con estados y notificaciones

**Firma del hook:**
```typescript
export function useDocumentDelete(): UseDocumentDeleteReturn
```

**Interfaz de retorno:**
```typescript
interface UseDocumentDeleteReturn {
  isDeleting: boolean;           // Indica si eliminación está en progreso
  error: string | null;          // Mensaje de error si lo hay
  deleteDocumentWithConfirmation: (documentId: string, documentName: string) => Promise<boolean>;
  clearError: () => void;        // Función para limpiar error
}
```

**Pasos de implementación:**
1. Crear archivo `frontend/src/features/documents/hooks/useDocumentDelete.ts`
2. Implementar interfaz de retorno al inicio del archivo
3. Importar dependencias necesarias:
   ```typescript
   import { useState } from 'react';
   import { deleteDocument } from '../api/documentService';
   import { useNotification } from '@common/hooks/useNotification';
   import { AxiosError } from 'axios';
   ```
4. Implementar hook principal:
   - Inicializar estado `isDeleting` como `false`
   - Inicializar estado `error` como `null`
   - Obtener funciones `showSuccess` y `showError` de hook `useNotification`
   - Implementar función `deleteDocumentWithConfirmation`:
     * Validar que `documentId` no esté vacío
     * Setear `isDeleting` a `true` y `error` a `null`
     * Envoltura try-catch:
       - Try: Llamar `deleteDocument(documentId)`, mostrar notificación de éxito, retornar `true`
       - Catch: Mapear código HTTP a mensaje amigable, setear `error`, mostrar notificación, retornar `false`
     * Finally: Setear `isDeleting` a `false`
   - Implementar función `clearError` que setea `error` a `null`
   - Retornar objeto con todos los estados y funciones
5. Guardado automático

**Mapeo de errores HTTP:**
| Código | Mensaje | Razón |
|--------|---------|-------|
| 401 | Sesión expirada. Por favor, inicie sesión nuevamente | Token JWT inválido o expirado |
| 403 | No tiene permisos para eliminar este documento | Falta permiso ESCRITURA o ADMINISTRACION |
| 404 | Documento no encontrado | Documento no existe o pertenece otra org |
| 409 | El documento ya está eliminado | Soft delete ya realizado previamente |
| 5xx | Error al eliminar el documento. Por favor, intente nuevamente | Error servidor |

**Dependencias:**
```typescript
import { useState } from 'react';
import { deleteDocument } from '../api/documentService';
import { useNotification } from '@common/hooks/useNotification';
import { AxiosError } from 'axios';
```

**Notas de implementación:**
- El hook NO debe renderizar nada (es lógica pura)
- `deleteDocumentWithConfirmation` debe ser función `async`
- Mensajes de error en español y amigables para el usuario
- Estados `isDeleting` se usan para deshabilitar botones durante operación
- Función `clearError` se usa cuando usuario cierra diálogo o navega

---

### Paso 3: Crear Componente Diálogo de Confirmación

**Archivo**: `frontend/src/features/documents/components/DocumentDeleteConfirmDialog.tsx` (NUEVO)

**Acción**: Crear componente modal de confirmación reutilizable

**Firma del componente:**
```typescript
export function DocumentDeleteConfirmDialog(props: DocumentDeleteConfirmDialogProps): React.ReactNode
```

**Props esperadas:**
```typescript
interface DocumentDeleteConfirmDialogProps {
  isOpen: boolean;                    // ¿Diálogo está visible?
  onClose: () => void;                // Callback cuando cierra
  onConfirm: () => void;              // Callback cuando confirma eliminar
  documentName: string;               // Nombre del documento
  isDeleting: boolean;                // ¿Está eliminando? (mostrar spinner)
}
```

**Pasos de implementación:**
1. Crear archivo `frontend/src/features/documents/components/DocumentDeleteConfirmDialog.tsx`
2. Importar componentes base:
   ```typescript
   import React from 'react';
   import { AlertTriangle } from 'lucide-react';
   // Componentes comunes del proyecto (ajustar según proyecto)
   import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@common/components/Dialog';
   import { Button } from '@common/components/Button';
   ```
3. Definir interfaz `DocumentDeleteConfirmDialogProps` con propiedades del punto anterior
4. Implementar componente con estructura:
   - **Dialog wrapper** con props `open={isOpen}` y `onOpenChange={onClose}`
   - **Header** con título "Confirmar eliminación" e icono de alerta (`AlertTriangle`)
   - **Contenido**:
     * Párrafo introductorio: "¿Está seguro que desea eliminar el siguiente documento?"
     * Nombre del documento en **bold** y más grande
     * Caja de advertencia roja con borde: "⚠️ Esta acción no se puede deshacer desde la interfaz de usuario. El documento quedará marcado como eliminado."
   - **Footer** con botones:
     * Botón "Cancelar" (variant secondary, disabled mientras `isDeleting === true`)
     * Botón "Eliminar documento" (variant danger/destructive, disabled mientras `isDeleting === true`, icono spinner si `isDeleting === true`)
5. Guardar cambios

**Ejemplo de estructura visual:**
```
┌─────────────────────────────────┐
│ ⚠️ Confirmar eliminación        │ [X]
├─────────────────────────────────┤
│                                 │
│ ¿Está seguro que desea          │
│ eliminar el siguiente documento?│
│                                 │
│ Documento Importante.pdf         │
│                                 │
│ ┌────────────────────────────┐  │
│ │ ⚠️ Esta acción no se puede │  │
│ │ deshacer desde la interfaz.│  │
│ │ El documento quedarà...    │  │
│ └────────────────────────────┘  │
│                                 │
├─────────────────────────────────┤
│      [Cancelar]  [Eliminar...] │
└─────────────────────────────────┘
```

**Dependencias:**
```typescript
import React from 'react';
import { AlertTriangle } from 'lucide-react';
// Componentes comunes (ajustar según nombres reales en proyecto)
```

**Notas de implementación:**
- Usar colores rojos (#ef4444, #fee2e2) para indicar acción destructiva
- Icono de alerta para captar atención del usuario
- Mensaje claro y en español sobre irreversibilidad
- Botones deshabilitados durante operación para evitar clicks múltiples
- El diálogo se abre/cierra via prop `isOpen`

---

### Paso 4: Crear Componente Botón de Eliminación

**Archivo**: `frontend/src/features/documents/components/DocumentDeleteButton.tsx` (NUEVO)

**Acción**: Crear botón reutilizable que orqueste diálogo + eliminación

**Firma del componente:**
```typescript
export function DocumentDeleteButton(props: DocumentDeleteButtonProps): React.ReactNode | null
```

**Props esperadas:**
```typescript
interface DocumentDeleteButtonProps {
  documentId: string;              // ID del documento
  documentName: string;            // Nombre para mostrar en diálogo
  canDelete: boolean;              // ¿Usuario tiene permisos?
  onDeleteSuccess?: () => void;    // Callback tras eliminación exitosa
  disabled?: boolean;              // ¿Deshabilitar botón?
  size?: 'sm' | 'md' | 'lg';      // Tamaño del botón
  variant?: 'danger' | 'ghost' | 'outline'; // Estilo del botón
  showLabel?: boolean;             // ¿Mostrar texto "Eliminar"?
  className?: string;              // Clases CSS adicionales
}
```

**Pasos de implementación:**
1. Crear archivo `frontend/src/features/documents/components/DocumentDeleteButton.tsx`
2. Importar dependencias:
   ```typescript
   import React, { useState } from 'react';
   import { Trash2 } from 'lucide-react';
   import { Button } from '@common/components/Button';
   import { DocumentDeleteConfirmDialog } from './DocumentDeleteConfirmDialog';
   import { useDocumentDelete } from '../hooks/useDocumentDelete';
   ```
3. Definir interfaz `DocumentDeleteButtonProps`
4. Implementar componente con lógica:
   - Early return `null` si `canDelete === false` (no renderizar si sin permisos)
   - Estado local `isDialogOpen` para controlar visibilidad del diálogo
   - Obtener hook `{ isDeleting, deleteDocumentWithConfirmation }` de `useDocumentDelete()`
   - Handler `handleDeleteClick()`: abre diálogo si tiene permisos
   - Handler `handleConfirmDelete()`: ejecuta eliminación, cierra diálogo si exitosa
   - Handler `handleCancelDelete()`: cierra diálogo sin hacer nada
   - Render:
     * Botón con icono `Trash2`, texto opcional, title descriptivo
     * Diálogo: `<DocumentDeleteConfirmDialog ... />`
5. Guardar cambios

**Comportamiento del botón:**
- Si `canDelete === false` → No se renderiza (simplifica lógica en componentes padre)
- Si `canDelete === true`:
  - Click → Abre diálogo de confirmación
  - Diálogo "Cancelar" → Cierra sin cambios
  - Diálogo "Eliminar" → Ejecuta eliminación
    * Si éxito → Llama `onDeleteSuccess()` callback, cierra diálogo
    * Si error → Muestra error en notificación, diálogo permanece abierto

**Dependencias:**
```typescript
import React, { useState } from 'react';
import { Trash2 } from 'lucide-react';
import { Button } from '@common/components/Button';
import { DocumentDeleteConfirmDialog } from './DocumentDeleteConfirmDialog';
import { useDocumentDelete } from '../hooks/useDocumentDelete';
```

**Notas de implementación:**
- Devolver `null` si no tiene permisos (React renderiza nada)
- El botón NO valida permisos nuevamente (confianza en prop `canDelete`)
- Callback `onDeleteSuccess` es para invalidar React Query cache
- Estados `isDeleting` deshabilita botón durante operación
- Icono `Trash2` de Lucide para consistencia visual

---

### Paso 5: Integrar Botón en Lista de Documentos

**Archivo(s)**: 
- `frontend/src/features/folders/components/FolderList.tsx` (MODIFICAR)
- O `frontend/src/features/acl/components/AclDocumentoList.tsx` (MODIFICAR)

**Acción**: Agregar componente `DocumentDeleteButton` en cada fila de documento

**Pasos de implementación:**
1. Identificar archivo donde se listan documentos (probablemente `FolderList.tsx` o `AclDocumentoList.tsx`)
2. Importar dependencias:
   ```typescript
   import { DocumentDeleteButton } from '@features/documents/components/DocumentDeleteButton';
   import { useQueryClient } from '@tanstack/react-query';
   ```
3. Dentro del componente:
   - Obtener `queryClient` via hook: `const queryClient = useQueryClient();`
   - Crear handler de callback:
     ```typescript
     const handleDocumentDeleteSuccess = () => {
       // Invalidar cache de documentos para refrescar lista
       // Ajustar nombre de query key según proyecto
       queryClient.invalidateQueries(['documentos', folderId]);
       // O si usa otro nombre:
       // queryClient.invalidateQueries(['documents', folderId]);
     };
     ```
4. En el render de cada documento (en tabla/grid), agregar botón:
   ```typescript
   <DocumentDeleteButton
     documentId={documento.id}
     documentName={documento.nombre}
     canDelete={
       ['ESCRITURA', 'ADMINISTRACION'].includes(
         documento.permisoEfectivo
       )
     }
     onDeleteSuccess={handleDocumentDeleteSuccess}
     size="sm"
     variant="ghost"
   />
   ```
5. Posicionar botón lógicamente:
   - En tabla: columna de acciones (junto a editar, descargar)
   - En grid: menú contextual o esquina de cada tarjeta
6. Guardar cambios

**Cálculo de permiso:**
```typescript
// Determinar si usuario puede eliminar
const canDelete = 
  documento.permisoEfectivo === 'ESCRITURA' || 
  documento.permisoEfectivo === 'ADMINISTRACION';

// O forma más limpia:
const canDelete = ['ESCRITURA', 'ADMINISTRACION'].includes(
  documento.permisoEfectivo
);
```

**Notas de implementación:**
- El campo `permisoEfectivo` viene del backend/ACL
- React Query: usar `invalidateQueries` con la key exacta del query
- El handler `handleDocumentDeleteSuccess` refrescarà la lista automáticamente
- Posicionar botón "después" de botones de solo lectura (descargar, ver detalles)
- Considerar iconografía consistente con resto del proyecto

---

### Paso 6: Actualizar Documentación Técnica

**Acción**: Revisar y actualizar documentación según cambios implementados

**Pasos de implementación:**

#### 6.1 Actualizar `ai-specs/specs/api-spec.yml` (OpenAPI/Swagger)
1. Abrir archivo `ai-specs/specs/api-spec.yml`
2. Buscar sección de `/api/documentos` endpoints
3. Agregar endpoint DELETE (si no existe):
   ```yaml
   /api/documentos/{id}:
     delete:
       operationId: deleteDocument
       summary: Eliminar documento (soft delete)
       description: Marca un documento como eliminado sin borrar físicamente
       tags:
         - Documentos
       parameters:
         - name: id
           in: path
           required: true
           schema:
             type: integer
       responses:
         204:
           description: Documento eliminado exitosamente
         401:
           description: No autenticado
         403:
           description: Sin permisos
         404:
           description: Documento no encontrado
         409:
           description: Documento ya eliminado
         500:
           description: Error interno del servidor
   ```
4. Guardar cambios


### Paso 7: Verificación Final de Calidad

**Acción**: Ejecutar verificaciones de calidad antes de crear Pull Request

**Pasos de verificación:**

1. **Linting y Formato**
   ```bash
   npm run lint          # ESLint para errores
   npm run lint:fix      # Arreglar automáticamente
   npm run format        # Prettier para formato
   ```

2. **TypeScript Compilation**
   ```bash
   npm run type-check    # o tsc -b (según configuración)
   ```

3. **Tests Unitarios**
   ```bash
   npm run test:unit     # Tests del hook y componentes
   npm run test:unit -- --coverage  # Con cobertura
   ```

4. **Tests E2E**
   ```bash
   npm run cypress:run   # Ejecutar todos los tests E2E
   # O si está en desarrollo:
   npm run cypress:open  # Modo interactivo
   ```

5. **Build del Proyecto**
   ```bash
   npm run build         # Vite build
   # Verificar que no hay errores
   ```

6. **Tamaño de Bundle**
   ```bash
   npm run build:analyze # Si existe script
   # Verificar que nuevos componentes no aumentan significativamente
   ```

**Checklist de Calidad:**
- [ ] Todos los tests pasan (unitarios + E2E)
- [ ] Cobertura >= 90%
- [ ] Sin errores de linting
- [ ] TypeScript compila sin errores
- [ ] Build exitoso
- [ ] Sin warnings en console
- [ ] Documentación actualizada
- [ ] Cambios seguyen standards del proyecto

---

## 4. Orden de Implementación

1. ✅ **Paso 0**: Crear rama `feature/US-DOC-008-delete-document`
2. ✅ **Paso 1**: Extender `documentService.ts` con función `deleteDocument()`
3. ✅ **Paso 2**: Crear hook `useDocumentDelete.ts`
4. ✅ **Paso 3**: Crear componente `DocumentDeleteConfirmDialog.tsx`
5. ✅ **Paso 4**: Crear componente `DocumentDeleteButton.tsx`
6. ✅ **Paso 5**: Integrar botón en lista de documentos (`FolderList.tsx` o similar)
10. ✅ **Paso 6**: Actualizar documentación técnica
11. ✅ **Paso 7**: Ejecutar verificaciones de calidad

**Estimación de tiempo:**
- Pasos 1-5: 3-4 horas (implementación core)
- Pasos 6-8: 2-3 horas (tests)
- Pasos 9-10: 1 hora (documentación + QA)
- **Total**: 6-8 horas

---

## 6. Consideraciones de UX/UI

### Diseño Visual
- **Colores**: Icono/botón rojo para acción destructiva
- **Icono**: Trash/Papelera de Lucide React
- **Diálogo**: Fondo semi-trasparente, contenido centrado
- **Advertencia**: Caja roja con icono ⚠️

### Estados del Botón
| Estado | Visual | Acción |
|--------|--------|--------|
| Con permisos | Visible, enabled | Click abre diálogo |
| Sin permisos | No se renderiza | — |
| Eliminando | Visible, disabled, spinner | — |
| Error | Visible, enabled, mensaje | Reintentar |

### Flujo UX
```
Página de carpeta
    ↓
Usuario hace clic en botón "Eliminar"
    ↓
Diálogo modal aparece (scroll bloqueado)
    ↓
↙─────────────────────────┐
│                         │
Cancelar → Cierra         Confirmar ↓
           (sin cambios)   Muestra "Eliminando..."
                          ↓ Éxito
                          Cierra diálogo
                          Notificación de éxito
                          Documento desaparece
                          ↓ Error
                          Muestra mensaje de error
                          Permite reintentar
```

### Accesibilidad
- [ ] Dialog con roles ARIA correctos
- [ ] Focus management (focus → botón de confirmar por defecto)
- [ ] Elementos interactivos con labels accesibles
- [ ] Mensajes descriptivos en español
- [ ] Contraste de colores >= 4.5:1
- [ ] Iconos + texto complementario

---

## 7. Patrones y Convenciones Aplicados

### Patrones Frontend Utilizados
1. **Custom Hook Pattern**: `useDocumentDelete` encapsula lógica reutilizable
2. **Controlled Component Pattern**: `DocumentDeleteButton` controla estado de diálogo
3. **Composition Pattern**: Botón componible con props flexibles
4. **Error Boundary Pattern**: Manejo de errores graceful en hook y componentes
5. **Notification Pattern**: Integración con sistema centralizado de notificaciones

### Convenciones de Código
- **Nombres**: PascalCase para componentes, camelCase para funciones/hooks
- **Tipos**: TypeScript interfaces para props, enums para constantes
- **Imports**: Organized by type (React, libs, features, components)
- **Testing**: Co-located tests en carpeta `__tests__` paralela
- **Comments**: JSDoc para funciones públicas, inline para lógica compleja

### Standards del Proyecto Aplicados
- Feature-based folder structure (`src/features/`)
- Separation of concerns (api / components / hooks)
- React Query para state management
- Axios para HTTP requests
- Tailwind CSS para estilos
- Cypress para E2E testing

---

## 8. Manejo de Errores

### Errores HTTP Mapeados
```typescript
const errorMessages: Record<number, string> = {
  401: 'Sesión expirada. Por favor, inicie sesión nuevamente',
  403: 'No tiene permisos para eliminar este documento',
  404: 'Documento no encontrado',
  409: 'El documento ya está eliminado',
  500: 'Error al eliminar el documento. Por favor, intente nuevamente'
};
```

### Validaciones Frontend
- ID de documento no vacío
- Nombre de documento no vacío
- Propiedades requeridas en componentes

### Recuperación de Errores
- Usuario puede reintentar eliminación
- Error se muestra en notificación + estado del hook
- Diálogo permanece abierto si hay error (permitir reintentar)
- Botón se habilita nuevamente tras error

---

## 9. Dependencias Requeridas

### Paquetes NPM Necesarios (Verificar)
```json
{
  "react": "^18.0.0",
  "react-dom": "^18.0.0",
  "axios": "^1.6.0",
  "@tanstack/react-query": "^4.0.0 o ^5.0.0",
  "tailwindcss": "^3.0.0",
  "lucide-react": "^0.200.0+",
  "@testing-library/react": "^14.0.0",
  "@testing-library/jest-dom": "^5.16.0+",
  "vitest": "^0.34.0+",
  "cypress": "^13.0.0+"
}
```

### Componentes Comunes que Deben Existir
- `@common/components/Dialog` (DialogContent, DialogHeader, DialogTitle, DialogFooter)
- `@common/components/Button` (con props: variant, size, disabled, isLoading)
- `@common/hooks/useNotification` (showSuccess, showError)
- `@core/shared/api/axiosInstance` (cliente HTTP configurado)

---

## 10. Notas Importantes

### Seguridad
- [ ] Backend valida permisos nuevamente (no confiar en frontend)
- [ ] No exponer tokens en logs o errores
- [ ] Validar `organizacionId` en backend para tenant isolation
- [ ] Usar HTTPS para todas las llamadas API

### Performance
- [ ] React Query invalida cache correctamente
- [ ] Hook no renderiza componentes (lógica pura)
- [ ] Diálogo usa lazy rendering si es necesario
- [ ] No hay memory leaks en efectos si los hay

### Internacionalización
- [ ] Todos los mensajes en español (usuario final)
- [ ] Documentación/comentarios en inglés (developers)
- [ ] Mensajes de error claros y accesibles

### Compatibilidad
- [ ] Navegadores modernos: Chrome, Firefox, Safari, Edge (últimas 2 versiones)
- [ ] Mobile responsive (considerar touch en diálogo)
- [ ] Teclado accesible (Tab para navegar, Escape para cerrar)

---

## 11. Próximos Pasos Tras Implementación

1. **Crear Pull Request**
   - Base: `develop` o `main` (según proyecto)
   - Head: `feature/US-DOC-008-delete-document`
   - Description: Incluir checklist de criterios de aceptación
   - Linking: Referenciar issue/ticket original

2. **Code Review**
   - Esperar revisión de al menos 1-2 developers
   - Atender comentarios y hacer adjustments
   - Verificar que tests siguen pasando

3. **Integración Continua**
   - GitHub Actions / GitLab CI ejecuta tests
   - Cobertura debe ser >= 90%
   - Linting debe pasar sin warnings

4. **Merge y Deployment**
   - Squash and merge a `develop/main`
   - Deploy a staging environment
   - Validación funcional en staging
   - Release a Production (según ciclo del proyecto)

5. **Post-Implementation**
   - Monitorear errores en producción
   - Recopilar feedback de usuarios
   - Documentar lecciones aprendidas

---

## 12. Checklist de Implementación Final

### ✅ Checklist de Completitud
- [ ] Función `deleteDocument()` en `documentService.ts`
- [ ] Hook `useDocumentDelete.ts` con estados y handlers
- [ ] Componente `DocumentDeleteConfirmDialog.tsx` con UX clara
- [ ] Componente `DocumentDeleteButton.tsx` reutilizable
- [ ] Integración en lista de documentos (`FolderList.tsx`)
- [ ] Tests unitarios: hook (5+ tests)
- [ ] Tests unitarios: botón (5+ tests)
- [ ] Tests unitarios: diálogo (4+ tests)
- [ ] Tests E2E: 9+ scenarios con Cypress
- [ ] Documentación actualizada (standards, API spec)
- [ ] Linting: `npm run lint:fix` sin errores
- [ ] TypeScript: `npm run type-check` sin errores
- [ ] Build: `npm run build` exitoso
- [ ] Tests: `npm run test:unit` 90%+ cobertura
- [ ] E2E: `npm run cypress:run` todos pasan
- [ ] Code Review guidelines cumplidas
- [ ] Rama pusheada a repositorio

### ✅ Criterios de Aceptación Verificados
- [ ] Botón no visible si no tiene permisos ESCRITURA/ADMINISTRACION
- [ ] Botón visible y funcional con permisos correctos
- [ ] Diálogo de confirmación se abre antes de eliminar
- [ ] Cancelar cierra diálogo sin cambios
- [ ] Confirmar ejecuta eliminación
- [ ] Notificación de éxito tras eliminación
- [ ] Lista se actualiza automáticamente (documento desaparece)
- [ ] Errores mostrados con mensajes amigables en español
- [ ] Botón deshabilitado y mostrando spinner durante operación
- [ ] Se puede reintentar tras error
- [ ] Tenant isolation funcionando (404 para otras orgs)

---

**Fin del Plan de Implementación Frontend: US-DOC-008**

Última actualización: 16 de febrero de 2026
