# Plan de Implementación Frontend: US-DOC-007 Descarga de Documento Actual desde Lista de Documentos

## 1. Descripción General

Esta tarea implementa la funcionalidad para descargar documentos actuales directamente desde la lista de documentos en la interfaz de usuario, sin necesidad de navegar al historial de versiones. Los usuarios autenticados con permisos de LECTURA, ESCRITURA o ADMINISTRACIÓN podrán descargar documentos con retroalimentación visual mediante indicadores de carga, notificaciones de éxito/error y manejo robusto de errores de red.

**Principios de arquitectura frontend:**
- Arquitectura orientada por características (Feature-Driven Clean Architecture)
- Capa de servicios desacoplada del componente UI
- Hooks reutilizables para lógica compartida
- Gestión de estados local y global coherente
- Integración segura con el backend vía Axios y JWT
- Notificaciones de usuario contextuales

---

## 2. Contexto de Arquitectura

### Componentes e Interfaces Involucrados

**Feature: `documents`**
- **Ubicación:** `frontend/src/features/documents/`
- **Estructura existente:**
  - `api/documentService.ts` - Servicios API para operaciones con documentos
  - `components/` - Componentes React (VersionDownloadButton ya existe, ahora reutilizaremos)
  - `hooks/` - Hooks personalizados para lógica de documentos
  - `types/document.types.ts` - Definiciones de tipos TypeScript
  
**Feature: `acl`**
- **Ubicación:** `frontend/src/features/acl/`
- **Uso:** Validación de permisos (`PermissionAwareMenu`, utilidades de evaluación de permisos)
- **Componentes:** `AclDocumentoList.tsx` - Muestra lista de documentos con permisos

**Feature: `folders`**
- **Ubicación:** `frontend/src/features/folders/`
- **Componentes:** `FolderList.tsx`, `FolderItem.tsx` - Renderiza documentos en lista
- **Services:** `useFolderContent()` hook para obtener contenido de carpetas

**Common:**
- **Notificaciones:** `frontend/src/common/components/NotificationCentre.tsx` o sistema de notificaciones existente
- **Permisos:** `frontend/src/common/constants/permissions.ts` - Mapeo de permisos
- **Cliente API:** `frontend/src/core/shared/api/axiosInstance.ts` - Instancia Axios configurada

### Consideraciones de Enrutamiento

- No se requieren nuevas rutas; la descarga ocurre desde vistas existentes (FoldersPage, lista de ACL)
- Los cambios son principalmente en componentes y servicios internos

### Enfoque de Gestión de Estados

- **Estado local:** `isLoading`, `error` para descarga individual (useState en componente o hook)
- **Estado global:** Notificaciones via servicio centralizado (si existe) o Toast/Notification component
- **Datos:** React Query cachea metadatos de documentos; la descarga es una acción one-off

---

## 3. Pasos de Implementación

### **Paso 0: Crear Rama de Características**

**Acción:** Crear y cambiar a una nueva rama de características siguiendo la convención del proyecto.

**Convención de nomenclatura de rama:** `feature/US-DOC-007-frontend` (separación explícita de backend si existe)

**Pasos de implementación:**
1. Verificar que estés en la rama base más reciente (`main` o `develop`):
   ```bash
   git checkout main
   git pull origin main
   ```
2. Crear nueva rama:
   ```bash
   git checkout -b feature/US-DOC-007-frontend
   ```
3. Verificar creación:
   ```bash
   git branch
   ```

**Notas:** 
- Referirse a `ai-specs/specs/frontend-standards.md` sección "Development Workflow" para convenciones específicas
- Asegurar que no exista rama conflictiva (`feature/US-DOC-007` sin sufijo)

---

### **Paso 1: Extender Servicio API (`documentService.ts`)**

**Archivo:** `frontend/src/features/documents/api/documentService.ts`

**Acción:** Agregar función para descargar documento actual (versión activa).

**Firma de función:**
```typescript
export async function downloadCurrentDocument(
  documentId: string,
  fileName?: string
): Promise<Blob>
```

**Pasos de implementación:**
1. Abrir `documentService.ts`
2. Importar tipos necesarios:
   - `Blob` (nativo de navegador)
   - Tipos de error si es necesario
3. Implementar función `downloadCurrentDocument()`:
   - Construir URL: `/api/documentos/{documentId}/download` (endpoint que ya existe en backend)
   - Realizar GET request con `responseType: 'blob'`
   - Retornar blob para que el componente maneje la descarga
4. Notas técnicas:
   - URL base ya incluida vía `apiClient` (Axios instance)
   - Headers de autorización (JWT) se agregan automáticamente via middleware
   - Error handling: dejar que errors fluyan hacia componente para notificaciones contextuales

**Dependencias requeridas:**
```typescript
import { apiClient } from '@core/shared/api/axiosInstance';
```

**Notas de implementación:**
- No manejar blob file creation aquí; dejar responsabilidad en hook/componente
- Mantener función pura y enfocada en comunicación API
- Compatible con la función existente `downloadDocumentVersion()` (mismos patrones)

---

### **Paso 2: Crear Hook Reutilizable `useDocumentDownload`**

**Archivo:** `frontend/src/features/documents/hooks/useDocumentDownload.ts` (crear nuevo)

**Acción:** Crear hook personalizado que orqueste lógica de descarga con manejo de estado, notificaciones y reintentos.

**Firma de función:**
```typescript
export function useDocumentDownload() {
  const [isDownloading, setIsDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const download = async (documentId: string, fileName: string): Promise<void>
  const clearError = (): void
  
  return { isDownloading, error, download, clearError };
}
```

**Pasos de implementación:**
1. Crear archivo `useDocumentDownload.ts` en `frontend/src/features/documents/hooks/`
2. Importar dependencias:
   - `useState` (React)
   - `downloadCurrentDocument()` del servicio
   - Sistema de notificaciones (ej. `useNotification` hook o `NotificationService`)
3. Implementar estados:
   - `isDownloading`: boolean para mostrar spinner
   - `error`: string | null para capturar y mostrar errores
4. Implementar función `download(documentId, fileName)`:
   - Validaciones iniciales (documentId no vacío)
   - Setear `isDownloading = true` y `error = null`
   - Try/catch block:
     - Llamar `downloadCurrentDocument(documentId)`
     - Al éxito: crear link temporal (`<a>` DOM element), trigger click, cleanup
     - Al error: capturar mensaje, mostrar notificación de error
   - Finalmente: setear `isDownloading = false`
5. Implementar función `clearError()`:
   - Resetear `error = null`
6. Retornar objeto con estados y funciones

**Dependencias requeridas:**
```typescript
import { useState } from 'react';
import { downloadCurrentDocument } from '../api/documentService';
import { useNotification } from '@common/hooks/useNotification'; // o equivalente
```

**Notas de implementación:**
- Mantener compatible con `VersionDownloadButton` existente (mismo patrón de manejo de blob)
- Notificación de éxito: "Descarga iniciada: [nombre_archivo]"
- Notificación de error: mostrar código HTTP y mensaje (ej. "403: Acceso denegado")
- Cleanup de URL.createObjectURL() después de descarga
- Retry logic opcional: permitir reintentar después de error (botón reutilizable)

---

### **Paso 3: Crear Componente Botón de Descarga `DocumentDownloadButton`**

**Archivo:** `frontend/src/features/documents/components/DocumentDownloadButton.tsx` (crear nuevo)

**Acción:** Crear botón reutilizable para descargar documento actual con indicador de carga y manejo de permisos.

**Firma de Componente:**
```typescript
interface DocumentDownloadButtonProps {
  documentId: string;
  fileName: string;
  canDownload: boolean;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'primary' | 'secondary' | 'danger';
  showLabel?: boolean;
  className?: string;
}

export const DocumentDownloadButton: React.FC<DocumentDownloadButtonProps> = ({
  documentId,
  fileName,
  canDownload,
  disabled = false,
  size = 'md',
  variant = 'primary',
  showLabel = true,
  className = ''
}) => { ... }
```

**Pasos de implementación:**
1. Crear archivo `DocumentDownloadButton.tsx` en `frontend/src/features/documents/components/`
2. Importar dependencias:
   - React (useState implícito en hook)
   - `useDocumentDownload` hook personalizado
   - Componentes UI (botones, spinner de Tailwind o librería UI)
   - Ícono de descarga (heroicons o similar)
3. Diseño del componente:
   - Props para customización: documentId, fileName, permisos, tamaño, variant
   - Condicional: si no hay permiso → button disabled + tooltip "Requiere permiso de lectura"
   - Estado: mostrar spinner mientras `isDownloading === true`
   - Ícono: ícono de descarga (↓ o cloud-download)
   - Texto: "Descargar" (configurable via `showLabel`)
   - Manejo de click: llamar `download(documentId, fileName)`
4. Estilos Tailwind:
   - Responsive para mobile (botón pequeño, ícono solo en sm)
   - Estados: hover, active, disabled, loading
   - Color según variant (primary: blue, secondary: gray, danger: red)
5. Error handling:
   - Si `error` del hook → mostrar tooltip/popover con mensaje de error
   - Botón vuelve a estado normal y permite reintentar

**Dependencias requeridas:**
```typescript
import React, { useState } from 'react';
import { useDocumentDownload } from '../hooks/useDocumentDownload';
import { ChevronDownIcon, CloudDownloadIcon } from '@heroicons/react/solid'; // o similar
```

**Notas de implementación:**
- Reutilizable en múltiples contextos (FolderItem, AclDocumentoList, detalles de documento)
- Patrón similar a `VersionDownloadButton` existente para consistencia
- Accesibilidad: aria-label, aria-disabled, role="button"
- Tooltip descriptivo cuando está deshabilitado

---

### **Paso 4: Integrar Botón en `FolderItem` Component**

**Archivo:** `frontend/src/features/folders/components/FolderItem.tsx`

**Acción:** Agregar botón de descarga para documentos en la tarjeta de item de documento.

**Pasos de implementación:**
1. Abrir `FolderItem.tsx`
2. Importar `DocumentDownloadButton`:
   ```typescript
   import { DocumentDownloadButton } from '@features/documents/components/DocumentDownloadButton';
   ```
3. Localizar sección de documento (condición `!isFolder`)
4. Agregar botón de descarga en la UI:
   - Ubicación: junto a acciones existentes o en menú contextual
   - Props:
     - `documentId={item.id}`
     - `fileName={item.nombre}` (nombre original del archivo)
     - `canDownload={capabilities.canDownload}` (ya existe capability)
     - `size="sm"` (para mejor UX en tarjetas)
     - `showLabel={false}` si espacio limitado (solo ícono) o `true` si hay espacio
5. Actualizar tipos TypeScript si es necesario:
   - `ContentItem` tipo ya debe tener `nombre` y permisos

**Dependencias requeridas:**
```typescript
import { DocumentDownloadButton } from '@features/documents/components/DocumentDownloadButton';
```

**Notas de implementación:**
- No modificar lógica existente de carpetas
- Mostrar botón solo si `item.tipo === 'documento'`
- Usar prop `showLabel` para controlar visibilidad de texto según espacio disponible
- Mantener consistencia visual con otros botones de acción

---

### **Paso 5: Integrar Botón en `AclDocumentoList` Component (Opcional)**

**Archivo:** `frontend/src/features/acl/components/AclDocumentoList.tsx`

**Acción:** Agregar botón de descarga en lista de documentos ACL para consistencia UI.

**Pasos de implementación:**
1. Abrir `AclDocumentoList.tsx`
2. Importar `DocumentDownloadButton`
3. Localizar donde se renderizan documentos en la tabla/lista
4. Agregar columna o acción de descarga:
   - Ubicación: columna de acciones junto a eliminar, cambiar permisos
   - Props similares a Paso 4
5. Ajustar ancho/layout según necesidad

**Notas:**
- Este paso es opcional si ya existe descarga en otra parte
- Priorizar FolderItem como punto principal de entrada

---

### **Paso 6: Exportar desde Index del Feature**

**Archivo:** `frontend/src/features/documents/index.ts`

**Acción:** Exportar nuevos componentes y hooks para disponibilidad en toda la aplicación.

**Pasos de implementación:**
1. Abrir `frontend/src/features/documents/index.ts`
2. Verificar exportaciones existentes (ya existen `downloadDocumentVersion`, etc.)
3. Agregar nuevas exportaciones:
   ```typescript
   export { useDocumentDownload } from './hooks/useDocumentDownload';
   export { DocumentDownloadButton } from './components/DocumentDownloadButton';
   export { downloadCurrentDocument } from './api/documentService';
   ```
4. Mantener orden: servicios API, hooks, componentes

---

### **Paso 7: Actualizar Tipos TypeScript**

**Archivo:** `frontend/src/features/documents/types/document.types.ts`

**Acción:** Verificar/agregar tipos necesarios para nueva funcionalidad.

**Pasos de implementación:**
1. Abrir archivo de tipos `document.types.ts`
2. Verificar que `DocumentDTO` incluye campos necesarios:
   - `id`: identificador del documento
   - `nombre`: nombre original del archivo con extensión
   - `version_actual`: número de versión actual (para UI)
   - `fecha_modificacion` o timestamp
3. Si falta algo, agregar tipos:
   ```typescript
   export interface DocumentDownloadOptions {
     documentId: string;
     fileName: string;
     canDownload: boolean;
   }
   ```
4. Notas: mantener consistencia con backend (API spec)

---

### **Paso 8: Documentación de Componentes**

**Archivos:** Componentes y hooks creados

**Acción:** Agregar JSDoc completo a funciones y componentes.

**Pasos de implementación:**
1. En `useDocumentDownload.ts`:
   - Documentar hook: qué hace, cuándo usarlo, ejemplo de uso
   - Documentar cada función: parámetros, retorno, excepciones
2. En `DocumentDownloadButton.tsx`:
   - Documentar componente: propósito, props, ejemplo
   - Documentar cada sección compleja
3. En `documentService.ts`:
   - Documentar nueva función `downloadCurrentDocument()`
   - Incluir ejemplo de error handling

Ejemplo:
```typescript
/**
 * Hook personalizado para descargar documentos actual con gestión de estado
 * 
 * Maneja lógica de descarga, errores, notificaciones y spinner de carga.
 * Compatible con múltiples descargas secuenciales.
 * 
 * @returns {Object} Estados y funciones
 * @returns {boolean} isDownloading - Indica descarga en progreso
 * @returns {string|null} error - Mensaje de error si ocurrió
 * @returns {Function} download - Inicia descarga: (documentId, fileName) => Promise<void>
 * @returns {Function} clearError - Limpia estado de error
 * 
 * @example
 * const { isDownloading, error, download } = useDocumentDownload();
 * await download('doc-123', 'contrato.pdf');
 */
export function useDocumentDownload() { ... }
```

---

### **Paso 9: Escribir Tests Unitarios**

**Archivos:**
- `frontend/src/features/documents/hooks/useDocumentDownload.test.ts`
- `frontend/src/features/documents/components/DocumentDownloadButton.test.tsx`

**Acción:** Crear tests para validar funcionamiento de hook y componente.

**Pasos de implementación:**

#### **Test: `useDocumentDownload.test.ts`**
Crear con Vitest + React Testing Library:
```typescript
describe('useDocumentDownload', () => {
  test('debería descargar documento cuando usuario tiene permiso', async () => {
    // Mock downloadCurrentDocument
    // Renderizar hook
    // Validar que se llama API
    // Validar descarga inicia
  });
  
  test('debería mostrar notificación de éxito', async () => {
    // Similar al anterior
    // Validar notificación de éxito se muestra
  });
  
  test('debería mostrar notificación de error y permitir reintento', async () => {
    // Mock error en API
    // Validar que muestra error
    // Validar que se puede reintentar
  });
  
  test('debería limpiar error con clearError()', () => {
    // Setear error
    // Llamar clearError()
    // Validar error es null
  });
  
  test('debería desabilitar botón mientras descarga', async () => {
    // Validar isDownloading es true durante descarga
  });
});
```

#### **Test: `DocumentDownloadButton.test.tsx`**
```typescript
describe('DocumentDownloadButton', () => {
  test('debería mostrar botón habilitado si usuario tiene permiso', () => {
    // Renderizar con canDownload=true
    // Validar button no tiene disabled
  });
  
  test('debería deshabilitar botón si usuario NO tiene permiso', () => {
    // Renderizar con canDownload=false
    // Validar button tiene disabled
  });
  
  test('debería llamar download al hacer clic', async () => {
    // Mock download function
    // Simular click
    // Validar se llama download con documentId y fileName
  });
  
  test('debería mostrar spinner durante descarga', async () => {
    // Renderizar
    // Simular descarga (isDownloading=true)
    // Validar spinner visible
  });
  
  test('debería mostrar error si descarga falla', async () => {
    // Mock error
    // Simular click
    // Validar error message visible
  });
  
  test('debería mostrar tooltip si botón deshabilitado', () => {
    // Renderizar con canDownload=false
    // Validar tooltip "Requiere permiso"
  });
});
```

**Herramientas**:
- Vitest para ejecución de tests
- React Testing Library para renderizar componentes
- `@testing-library/user-event` para simular clicks
- `vi.mock()` para mockear servicios

---

### **Paso 10: Tests E2E (Cypress)**

**Archivo:** `frontend/cypress/e2e/documents-download.cy.ts` (crear)

**Acción:** Crear tests end-to-end para validar flujo completo de descarga desde UI.

**Pasos de implementación:**

```typescript
describe('Document Download from List', () => {
  beforeEach(() => {
    // Login con usuario que tiene permiso LECTURA
    cy.login('user@example.com', 'password');
    // Navegar a lista de documentos (carpeta)
    cy.visit('/documentos/carpeta/123');
    cy.wait('@getFolderContent');
  });
  
  it('debería descargar documento con click en botón', () => {
    // Validar documento visible
    cy.get('[data-testid="document-item-doc-1"]').should('be.visible');
    // Click en botón descargar
    cy.get('[data-testid="download-button-doc-1"]').click();
    // Validar notificación de éxito
    cy.get('[data-testid="notification-success"]').should('contain', 'Descarga iniciada');
  });
  
  it('debería mostrar spinner mientras descarga', () => {
    // Interceptar descarga lenta
    cy.intercept('GET', '**/documentos/*/download', (req) => {
      req.reply((res) => {
        res.delay(1000);
      });
    });
    cy.get('[data-testid="download-button-doc-1"]').click();
    // Validar spinner visible
    cy.get('[data-testid="download-spinner"]').should('be.visible');
    // Esperar a que termine
    cy.get('[data-testid="download-spinner"]').should('not.exist');
  });
  
  it('debería mostrar error si falla descarga (403)', () => {
    // Interceptar con 403
    cy.intercept('GET', '**/documentos/*/download', { statusCode: 403, body: {} });
    cy.get('[data-testid="download-button-doc-1"]').click();
    // Validar notificación de error
    cy.get('[data-testid="notification-error"]').should('contain', 'Acceso denegado');
  });
  
  it('debería deshabilitar botón si usuario NO tiene permiso LECTURA', () => {
    // Login con usuario sin permiso
    cy.login('unprivileged@example.com', 'password');
    cy.visit('/documentos/carpeta/456');
    cy.wait('@getFolderContent');
    // Validar botón deshabilitado
    cy.get('[data-testid="download-button-doc-2"]').should('be.disabled');
    // Validar tooltip
    cy.get('[data-testid="download-button-doc-2"]').trigger('hover');
    cy.get('[data-testid="tooltip"]').should('contain', 'permiso');
  });
  
  it('debería permitir reintento después de error', () => {
    // Primera llamada: error
    cy.intercept('GET', '**/documentos/*/download', { statusCode: 500 }).as('failDownload');
    cy.get('[data-testid="download-button-doc-1"]').click();
    cy.wait('@failDownload');
    // Validar error visible
    cy.get('[data-testid="notification-error"]').should('be.visible');
    
    // Segunda llamada: éxito
    cy.intercept('GET', '**/documentos/*/download', { statusCode: 200, body: 'file content' }).as('successDownload');
    // Button debe estar habilitado para reintento
    cy.get('[data-testid="download-button-doc-1"]').click();
    cy.wait('@successDownload');
    // Validar éxito
    cy.get('[data-testid="notification-success"]').should('be.visible');
  });
});
```

**Notas:**
- Usar `data-testid` en componentes para identificación en tests
- Validar flujos con permisos variados (LECTURA, ESCRITURA, ADMINISTRACION, NINGUNO)
- Validar manejo de errores HTTP (403, 404, 500)
- Incluir tests de accesibilidad básicos

---

### **Paso 11: Actualizar Documentación Técnica**

**Acción:** Revisar y actualizar documentación según cambios realizados.

**Pasos de implementación:**

1. **Revisar Cambios Realizados:**
   - 3 archivos creados: hook, componente, tests
   - 3 archivos modificados: FolderItem, AclDocumentoList, documentService
   - 1 archivo actualizado: index.ts del feature

2. **Identificar Archivos de Documentación a Actualizar:**
   - `ai-specs/specs/frontend-standards.md` → Agregar sección sobre descarga de documentos (patrón de manejo de blobs, notificaciones)
   - `ai-specs/specs/api-spec.yml` → Agregar endpoint GET `/documentos/{id}/download` (ya existe en backend, confirmar en OpenAPI)

3. **Actualizar Documentación:**

   **En `frontend-standards.md`:**
   - Agregar sección: "Descarga de Archivos (File Downloads)"
   - Documentar patrón:
     ```markdown
     ### Descarga de Archivos
     
     Para descargar archivos desde API:
     
     1. **Servicio API:** Retorna `Blob` con `responseType: 'blob'`
     ```typescript
     export async function downloadCurrentDocument(documentId: string): Promise<Blob> {
       const response = await apiClient.get(`/api/documentos/${documentId}/download`, {
         responseType: 'blob'
       });
       return response.data;
     }
     ```
     
     2. **Hook:** Maneja estado de descarga y notificaciones
     ```typescript
     const { isDownloading, error, download } = useDocumentDownload();
     await download(documentId, fileName);
     ```
     
     3. **Componente:** Botón descarga con indicador de carga
     ```typescript
     <DocumentDownloadButton documentId={id} fileName={name} canDownload={permission} />
     ```
     ```

   **En `api-spec.yml`:**
   - Confirmar endpoint:
     ```yaml
     /documentos/{id}/download:
       get:
         summary: Descargar documento actual
         parameters:
           - name: id
             in: path
             required: true
             schema:
               type: string
         responses:
           200:
             description: Archivo descargado exitosamente
             content:
               application/octet-stream: {}
           403:
             description: Acceso denegado
           404:
             description: Documento no encontrado
     ```

4. **Verificar Documentación:**
   - Confirmar coherencia con estándares
   - Asegurar ejemplos son claros y ejecutables
   - Validar estructura y formato

5. **Reportar Actualizaciones:**
   - Listar archivos actualizados
   - Resumir cambios clave
   - Confirmar que documentación es accesible al equipo

---

## 4. Orden de Implementación

1. **Paso 0:** Crear Rama de Características
2. **Paso 1:** Extender Servicio API (`documentService.ts`)
3. **Paso 2:** Crear Hook `useDocumentDownload`
4. **Paso 3:** Crear Componente `DocumentDownloadButton`
5. **Paso 4:** Integrar en `FolderItem`
6. **Paso 5:** Integrar en `AclDocumentoList` (opcional)
7. **Paso 6:** Exportar desde Index del Feature
8. **Paso 7:** Actualizar Tipos TypeScript
9. **Paso 8:** Agregar Documentación JSDoc
10. **Paso 9:** Escribir Tests Unitarios
11. **Paso 10:** Tests E2E (Cypress)
12. **Paso 11:** Actualizar Documentación Técnica

---

## 5. Checklist de Tests

### Tests Unitarios (Vitest + RTL)

- [ ] Hook `useDocumentDownload`:
  - [ ] Descarga exitosa
  - [ ] Notificación de éxito
  - [ ] Error handling
  - [ ] Reintento después de error
  - [ ] Estado `isDownloading` correcto
  - [ ] Limpieza de error

- [ ] Componente `DocumentDownloadButton`:
  - [ ] Botón habilitado con permiso
  - [ ] Botón deshabilitado sin permiso
  - [ ] Click trigger descarga
  - [ ] Spinner visible durante descarga
  - [ ] Error message visible
  - [ ] Tooltip en botón deshabilitado
  - [ ] Responsive en mobile

### Tests E2E (Cypress)

- [ ] Flujo completo desde lista de documentos
- [ ] Descarga exitosa
- [ ] Indicador de carga visible
- [ ] Manejo de error 403
- [ ] Manejo de error 500
- [ ] Botón deshabilitado sin permiso
- [ ] Reintento después de error
- [ ] Permisos variados (LECTURA, ESCRITURA, ADMINISTRACION)
- [ ] Cross-browser (Chrome, Firefox, Safari si aplica)

### Validación Manual

- [ ] Verificar descarga real en navegador
- [ ] Validar nombre de archivo correcto
- [ ] Verificar notificaciones contextuales
- [ ] Probar con diferentes tipos de documento (PDF, Word, Excel, etc.)
- [ ] Validar en responsive design (mobile, tablet, desktop)

---

## 6. Patrones de Manejo de Errores

### En Hook `useDocumentDownload`

```typescript
const download = async (documentId: string, fileName: string): Promise<void> => {
  try {
    setIsDownloading(true);
    setError(null);
    
    const blob = await downloadCurrentDocument(documentId);
    
    // Crear descarga
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName || 'document';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    
    // Notificación de éxito
    showNotification('success', `Descarga iniciada: ${fileName}`);
  } catch (err: unknown) {
    const message = extractErrorMessage(err);
    setError(message);
    showNotification('error', `Error descargando: ${message}`);
  } finally {
    setIsDownloading(false);
  }
};

function extractErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    if (err.response?.status === 403) {
      return 'Acceso denegado: No tienes permiso para descargar este documento';
    }
    if (err.response?.status === 404) {
      return 'Documento no encontrado o eliminado';
    }
    if (err.response?.status === 500) {
      return 'Error de servidor. Intenta más tarde.';
    }
    return err.response?.data?.message || 'Error desconocido';
  }
  if (err instanceof Error) {
    return err.message;
  }
  return 'Error desconocido';
}
```

### En Componente `DocumentDownloadButton`

```typescript
const handleClick = async () => {
  try {
    await download(documentId, fileName);
  } catch (err) {
    // Error ya manejado en hook
    // Aquí solo actualizar UI si es necesario
  }
};

// Render con error state
{error && (
  <div className="text-red-600 text-sm mt-1">
    {error}
    <button onClick={clearError} className="ml-2 underline">
      Descartar
    </button>
  </div>
)}
```

### Códigos de Error Esperados

| Código | Causa | Mensaje Usuario |
|--------|-------|-----------------|
| 400 | Datos inválidos | "Datos inválidos. Contacta soporte." |
| 401 | Token expirado | "Sesión expirada. Por favor, inicia sesión." |
| 403 | Sin permiso | "No tienes permiso para descargar este documento." |
| 404 | Documento no existe | "Documento no encontrado o fue eliminado." |
| 500 | Error servidor | "Error de servidor. Intenta más tarde." |
| Network | Sin conexión | "Sin conexión. Verifica tu internet e intenta de nuevo." |

---

## 7. Consideraciones UI/UX

### Indicador de Carga

- **Spinner:** Mostrar spinner tipo "loading circle" durante descarga
- **Botón deshabilitado:** No permitir múltiples clics simultáneos
- **Duración:** Mostrar spinner mínimo 0.5s (para clicks muy rápidos)
- **Texto:** Cambiar botón a "Descargando..." si hay espacio

### Estados del Botón

```
Habilitado (con permiso):
- Normal: "⬇ Descargar" (blue)
- Hover: más oscuro, cursor pointer
- Click: deshabilitado temporalmente + spinner
- Disabled (descargando): gris, sin cursor

Deshabilitado (sin permiso):
- Gris claro, sin cursor
- Hover: mostrar tooltip "Requiere permiso de Lectura"
- Sin click handler
```

### Notificaciones

**Éxito:**
- Posición: esquina superior derecha
- Icono: ✓ verde
- Mensaje: "Descarga iniciada: [nombre_archivo]"
- Duración: 3 segundos auto-dismiss
- Acción: botón cerrar

**Error:**
- Posición: esquina superior derecha
- Icono: ✗ rojo
- Mensaje: "[Código HTTP]: [Detalles]"
- Duración: 5 segundos (más tiempo para leer error)
- Acción: botón cerrar

### Responsiveness

- **Desktop:** Botón con texto "Descargar" + ícono
- **Tablet:** Botón con ícono + texto pequeño
- **Mobile:** Solo ícono en botón pequeño, tooltip en hover/long-press

### Accesibilidad

- `aria-label="Descargar documento: [nombre_archivo]"`
- `aria-disabled="true"` cuando deshabilitado
- `role="button"` en elemento
- Tooltip accesible con `aria-describedby`
- Validar focus order en listas
- Color no es único indicador (usar ícono + texto/tooltip)

---

## 8. Dependencias

### Librerías Existentes (Ya Instaladas)

- **React 19.2.0** - Componentes y hooks
- **React Router DOM 7.9.6** - Enrutamiento (si aplica)
- **Axios 1.13.2** - HTTP client (via `apiClient`)
- **Tailwind CSS 4.1.17** - Estilos
- **TypeScript 5.9.3** - Tipos estáticos

### Librerías a Verificar/Instalar

- **Heroicons o React Icons** - Ícono de descarga
  ```bash
  npm install @heroicons/react
  # o
  npm install react-icons
  ```
  
- **Sistema de Notificaciones** - Si no existe, usar Zustand + componente custom
  - Verificar si ya existe `useNotification()` hook
  - Si no: crear servicio centralizado de notificaciones

### Imports Principales

```typescript
import React, { useState } from 'react'; // React 19
import { apiClient } from '@core/shared/api/axiosInstance'; // Axios customizado
import { CloudDownloadIcon } from '@heroicons/react/solid'; // Ícono
import { useNotification } from '@common/hooks/useNotification'; // Notificaciones (si existe)
```

---

## 9. Notas Importantes

### Reglas de Negocio

1. **Descarga solo documento actual:** NO historial de versiones (para eso existe VersionDownloadButton)
2. **Validación de permisos en backend:** Frontend solo muestra/oculta botón; backend valida en GET
3. **Auditoría automática:** Backend emite evento "DOCUMENTO_DESCARGADO"
4. **Soft delete:** Documentos eliminados lógicamente no son descargables (403 o 404 del backend)

### Restricciones Técnicas

1. **Seguridad:**
   - Nunca confiar en validación de cliente (permisos)
   - JWT enviado automáticamente vía headers (middleware de Axios)
   - CORS validado en backend

2. **Performance:**
   - No cachear archivos descargados (cada descarga es fresh)
   - Streaming en backend (frontend recibe blob completo)
   - Usar `responseType: 'blob'` para memory efficiency

3. **Compatibilidad:**
   - Soportar navegadores modernos (Chrome, Firefox, Safari, Edge)
   - Fallback para navegadores sin `URL.createObjectURL` (legacy browsers)

### Requisitos Idioma

- **Frontend UI:** Español únicamente
- **Mensajes de error:** Españolización clara y comprensible
- **JSDoc/Comments:** Español en código de negocio, inglés en tecnicismos si es necesario

### Consideraciones TypeScript

- Componente funcional con `React.FC<Props>`
- Props interface obligatoria
- Retorno explícito de types en funciones
- Error handling con types nunca `any`

---

## 10. Próximos Pasos Después de la Implementación

1. **Crear Pull Request (PR):**
   - Descripción: Enlazar ticket US-DOC-007
   - Template: Seguir template de PR del proyecto
   - Checklist: Marcar todos los items completados

2. **Code Review:**
   - Esperar revisión de equipo
   - Responder comentarios
   - Realizar cambios si se solicitan

3. **Testing en Staging:**
   - Desplegar rama a ambiente staging (si aplica)
   - Validar en navegadores reales
   - Pruebas con usuarios de verdad

4. **Merge a Rama Principal:**
   - Esperar aprobación de review
   - Merge a `main` o `develop` según flujo
   - Cerrar rama feature después del merge

5. **Seguimiento Post-Deploy:**
   - Monitorear errores en producción (logs, error tracking)
   - Validar auditoría de descargas se registra
   - Recoger feedback de usuarios

6. **Documentación Adicional:**
   - Actualizar README si aplica
   - Crear issue para backlogs futuros (optimizaciones, rate limiting, etc.)

---

## 11. Verificación de Implementación

### Checklist Final de Calidad de Código

- [ ] **Sintaxis y Formato:**
  - [ ] Sin errores TypeScript (`tsc` compila sin warnings)
  - [ ] Formateado con Prettier (si está configurado)
  - [ ] ESLint sin errores ni warnings

- [ ] **Funcionalidad:**
  - [ ] Descarga funciona en navegador real
  - [ ] Permisos validados correctamente
  - [ ] Notificaciones activan sistema
  - [ ] Spinner muestra durante descarga
  - [ ] Error handling funciona

- [ ] **Testing:**
  - [ ] Tests unitarios pasan (Vitest)
  - [ ] Tests E2E pasan (Cypress)
  - [ ] Cobertura >= 80%
  - [ ] Sin skipped tests

- [ ] **Integración:**
  - [ ] Componente integrado en FolderItem correctamente
  - [ ] Hook reutilizable sin efectos secundarios
  - [ ] Servicio API no tiene breaking changes
  - [ ] Tipos TypeScript consistentes

- [ ] **Documentación:**
  - [ ] JSDoc completo en funciones/componentes
  - [ ] README actualizado si aplica
  - [ ] Documentación técnica actualizada
  - [ ] Ejemplos funcionales en comentarios

- [ ] **Accesibilidad y Responsiveness:**
  - [ ] Funciona en mobile/tablet/desktop
  - [ ] Navegación por teclado posible
  - [ ] Screen reader compatible
  - [ ] Colores cumplen WCAG AA

- [ ] **Performance:**
  - [ ] No memory leaks (cleanup en useEffect)
  - [ ] Transiciones suaves (sin lag)
  - [ ] Tiempos razonables de respuesta
  - [ ] Bundle size no aumentó significativamente

### Entrega Completa

✅ Todos los pasos completados
✅ Tests pasando
✅ Documentación actualizada
✅ PR creado y mergeado
✅ Listo para código review y deployment

---

## 12. Referencias y Links Útiles

- **Documento de Especificaciones:** [US-DOC-007](../../US/tickets/P4-Documentos/US-DOC-007.md)
- **Estándares Frontend:** [frontend-standards.md](../specs/frontend-standards.md)
- **Documentación API:** [api-spec.yml](../specs/api-spec.yml)
- **Componente Similar Existente:** [VersionDownloadButton.tsx](../../frontend/src/features/documents/components/VersionDownloadButton.tsx)
- **Servicio Existente:** [documentService.ts](../../frontend/src/features/documents/api/documentService.ts)

---

**Plan Completado:** Este documento proporciona una guía paso a paso lista para implementación. Procede con cada paso en orden y refiere aquí si necesitas aclaraciones.
