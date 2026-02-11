# Plan de Implementación Frontend: US-ACL-007 Mostrar Capacidades (Acciones Habilitadas) por Carpeta/Documento

## Vista General

Esta historia implementa la lógica de presentación en la interfaz de usuario para mostrar y habilitar/deshabilitar acciones según los permisos que tiene el usuario actual sobre una carpeta o documento específico. La implementación depende de US-ACL-006 (regla de precedencia de permisos) que determina qué permiso aplica.

**Principios de Arquitectura Frontend:**
- **Feature-Driven Clean Architecture**: Separación clara entre lógica de estado, servicios y presentación
- **Component-Based Design**: Componentes reutilizables con responsabilidad única
- **Type Safety**: TypeScript strict mode con interfaces explícitas
- **Accesibilidad**: Botones deshabilitados con tooltips informativos cuando no hay permiso
- **Responsive Design**: Comportamiento consistente en todos los dispositivos
- **Experiencia de Usuario**: Retroalimentación clara sobre por qué una acción está deshabilitada

**Objetivos Clave de Usuario:**
- Como usuario con solo `LECTURA`, quiero que la interfaz deshabilite "Subir documento" y "Administrar permisos"
- Como usuario con `ESCRITURA`, quiero que la interfaz me permita modificar y subir documentos, pero no administrar permisos
- Como usuario con `ADMINISTRACION`, quiero poder ejecutar todas las acciones sin restricciones
- Como usuario sin permiso en una carpeta, quiero que la interfaz no me muestre opciones de acceso

---

## Contexto de Arquitectura

### Componentes y Servicios Involucrados

**Estructura de Feature (Mejorada):**
```
frontend/src/features/acl/
├── services/
│   ├── nivelAccesoService.ts      # Existente
│   ├── aclCarpetaService.ts       # Existente
│   ├── aclDocumentoService.ts     # Existente
│   └── permissionEnforcementService.ts  # NUEVO - Evalúa permisos
├── hooks/
│   ├── useNivelesAcceso.ts        # Existente
│   ├── useCarpetaPermisos.ts      # Existente
│   ├── useDocumentoPermisos.ts    # Existente
│   └── usePermissionCapabilities.ts  # NUEVO - Hook para capacidades UI
├── types/
│   ├── index.ts                   # Existente, ampliarse
│   └── permissions.types.ts       # NUEVO - Tipos de capacidades
├── components/
│   ├── PermissionAwareButton.tsx  # NUEVO - Botón con validación de permisos
│   ├── PermissionAwareMenu.tsx    # NUEVO - Menú contextual sensible a permisos
│   ├── ActionPicker.tsx           # NUEVO - Selector visual de acciones disponibles
│   └── PermissionTooltip.tsx      # NUEVO - Tooltip explicativo
├── utils/
│   └── permissionEvaluator.ts     # NUEVO - Lógica de evaluación de capacidades
└── index.ts                        # Actualizar exports
```

**Archivos a Modificar/Ampliar:**
- `features/folders/components/FolderExplorer.tsx` - Integrar validación de permisos
- `features/folders/components/FolderItem.tsx` - Habilitar/deshabilitar acciones
- `features/folders/components/FolderContextMenu.tsx` - Filtrar menú según permisos
- `features/documents/components/DocumentActions.tsx` - Habilitar/deshabilitar acciones (si existe)
- `common/constants/permissions.ts` - Ampliarse con mapeos de capacidades

### State Management Approach

**Hook Custom (usePermissionCapabilities):**
```typescript
interface UsePermissionCapabilitiesResult {
  capabilities: {
    canRead: boolean;
    canWrite: boolean;
    canAdminister: boolean;
    canUpload: boolean;
    canDownload: boolean;
    canCreateVersion: boolean;
    canDeleteFolder: boolean;
    canManagePermissions: boolean;
    canChangeVersion: boolean;
  };
  nivelAcceso: CodigoNivelAcceso | null;
  isLoading: boolean;
  error: string | null;
  refreshPermissions: () => Promise<void>;
}
```

**Local Component State:**
- Tooltip visible/hidden para mostrar razón de deshabilitación
- Estado de carga al evaluar permisos

### Consideraciones de Enrutamiento

- No requiere nuevas rutas
- Se integra en rutas existentes: `/carpetas/:id`, `/documentos/:id`
- Evaluación de permisos en cada navegación y cambio de contexto

### Mapeo de Permisos a Acciones

| Acción | LECTURA | ESCRITURA | ADMINISTRACION | Sin Permiso |
|--------|---------|-----------|-----------------|-------------|
| Ver/Listar | ✅ | ✅ | ✅ | ❌ |
| Descargar | ✅ | ✅ | ✅ | ❌ |
| Subir Documento | ❌ | ✅ | ✅ | ❌ |
| Modificar Documento | ❌ | ✅ | ✅ | ❌ |
| Crear Versión | ❌ | ✅ | ✅ | ❌ |
| Cambiar Versión Actual | ❌ | ✅ | ✅ | ❌ |
| Eliminar Carpeta | ❌ | ❌ | ✅ | ❌ |
| Administrar Permisos | ❌ | ❌ | ✅ | ❌ |
| Crear Carpeta | ❌ | ✅ | ✅ | ❌ |

---

## Pasos de Implementación

### **Paso 0: Crear Rama de Feature**

**Acción:** Crear y cambiar a una rama de feature nueva siguiendo el flujo de desarrollo

**Nombre de Rama:** `feature/US-ACL-007-frontend` (requerido, separado de rama de backend si existe)

**Pasos de Implementación:**
1. Asegurarse de estar en rama `develop` o `main` más reciente
2. Ejecutar: `git pull origin develop` (o `main`)
3. Crear rama: `git checkout -b feature/US-ACL-007-frontend`
4. Verificar creación: `git branch`

**Notas:** Este debe ser el PRIMER paso antes de cualquier cambio de código. Garantiza separación de responsabilidades.

---

### **Paso 1: Extender Definiciones de Tipos TypeScript**

**Archivo:** `frontend/src/features/acl/types/index.ts`

**Acción:** Agregar interfaces TypeScript para capacidades de usuario y evaluación de permisos

**Firmas de Interfaz:**

```typescript
/**
 * Capacidades de usuario derivadas del nivel de acceso
 * Define qué acciones puede realizar el usuario
 */
export interface ICapabilities {
  canRead: boolean;           // Leer/ver/listar contenido
  canWrite: boolean;          // Escribir/modificar contenido
  canAdminister: boolean;     // Administrar permisos y configuración
  canUpload: boolean;         // Subir documentos a carpeta
  canDownload: boolean;       // Descargar documentos
  canCreateVersion: boolean;  // Crear nuevas versiones de documentos
  canDeleteFolder: boolean;   // Eliminar carpeta
  canManagePermissions: boolean; // Gestionar permisos de carpeta/documento
  canChangeVersion: boolean;  // Cambiar versión actual de documento
}

/**
 * Resultado de evaluación de permisos
 */
export interface IPermissionEvaluationResult {
  nivelAcceso: CodigoNivelAcceso | null;
  origen: 'documento' | 'carpeta' | 'ninguno';
  capabilities: ICapabilities;
  hasAnyPermission: boolean;
}

/**
 * Contexto de evaluación de permisos
 */
export interface IPermissionContext {
  entityId: number;
  entityType: 'documento' | 'carpeta';
  usuarioId: number;
}

/**
 * Datos para tooltip informativo
 */
export interface IDisabledActionTooltip {
  action: string;
  reason: string;
  requiredLevel?: CodigoNivelAcceso;
}

/**
 * Mapa de acciones a permisos requeridos
 */
export type ActionRequirement = Record<string, CodigoNivelAcceso | null>;
```

**Pasos de Implementación:**
1. Abrir `frontend/src/features/acl/types/index.ts`
2. Agregar interfaz `ICapabilities` con todos los campos
3. Agregar interfaz `IPermissionEvaluationResult`
4. Agregar interfaz `IPermissionContext`
5. Agregar interfaz `IDisabledActionTooltip`
6. Exportar todos los tipos desde barrel export
7. Asegurarse que `CodigoNivelAcceso` esté disponible

**Dependencias:**
- Tipos existentes: `CodigoNivelAcceso`, `INivelAcceso`

**Notas de Implementación:**
- Las capacidades se derivan directamente del nivel de acceso
- `origen` rastrean si el permiso viene de documento, carpeta, o ninguno
- Usar booleanos en lugar de strings para mejor rendimiento en condicionales

---

### **Paso 2: Crear Constantes de Evaluación de Permisos**

**Archivo:**  `frontend/src/common/constants/permissions.ts` (ampliarse)

**Acción:** Definir mapeos de nivel de acceso a capacidades y requisitos de acciones

**Definiciones de Constantes:**

```typescript
/**
 * Mapeo de nivel de acceso a capacidades
 * Define qué puede hacer un usuario según su nivel
 */
export const PERMISSION_TO_CAPABILITIES: Record<CodigoNivelAcceso | 'NINGUNO', ICapabilities> = {
  'LECTURA': {
    canRead: true,
    canWrite: false,
    canAdminister: false,
    canUpload: false,
    canDownload: true,
    canCreateVersion: false,
    canDeleteFolder: false,
    canManagePermissions: false,
    canChangeVersion: false,
  },
  'ESCRITURA': {
    canRead: true,
    canWrite: true,
    canAdminister: false,
    canUpload: true,
    canDownload: true,
    canCreateVersion: true,
    canDeleteFolder: false,
    canManagePermissions: false,
    canChangeVersion: true,
  },
  'ADMINISTRACION': {
    canRead: true,
    canWrite: true,
    canAdminister: true,
    canUpload: true,
    canDownload: true,
    canCreateVersion: true,
    canDeleteFolder: true,
    canManagePermissions: true,
    canChangeVersion: true,
  },
  'NINGUNO': {
    canRead: false,
    canWrite: false,
    canAdminister: false,
    canUpload: false,
    canDownload: false,
    canCreateVersion: false,
    canDeleteFolder: false,
    canManagePermissions: false,
    canChangeVersion: false,
  },
};

/**
 * Nivel de acceso requerido para cada acción
 */
export const ACTION_REQUIREMENTS: Record<string, CodigoNivelAcceso | null> = {
  'view': null,                           // Sin requisito (información pública)
  'download': 'LECTURA',
  'upload': 'ESCRITURA',
  'modify': 'ESCRITURA',
  'create_version': 'ESCRITURA',
  'change_version': 'ESCRITURA',
  'delete_folder': 'ADMINISTRACION',
  'manage_permissions': 'ADMINISTRACION',
  'create_folder': 'ESCRITURA',
};

/**
 * Mensajes informativos cuando una acción está deshabilitada
 */
export const DISABLED_ACTION_MESSAGES: Record<string, string> = {
  'upload': 'Necesitas permiso de Escritura para subir documentos',
  'modify': 'Necesitas permiso de Escritura para modificar',
  'delete_folder': 'Necesitas permiso de Administración para eliminar carpetas',
  'manage_permissions': 'Solo administradores pueden gestionar permisos',
  'create_version': 'Necesitas permiso de Escritura para crear versiones',
  'change_version': 'Necesitas permiso de Escritura para cambiar versión',
  'create_folder': 'Necesitas permiso de Escritura para crear carpetas',
  'no_access': 'No tienes acceso a este recurso',
};
```

**Pasos de Implementación:**
1. Abrir `frontend/src/common/constants/permissions.ts`
2. Agregar constante `PERMISSION_TO_CAPABILITIES`
3. Agregar constante `ACTION_REQUIREMENTS`
4. Agregar constante `DISABLED_ACTION_MESSAGES`
5. Exportar todas las constantes
6. Verificar consistencia: cada acción en `ACTION_REQUIREMENTS` debe estar en `DISABLED_ACTION_MESSAGES`

**Dependencias:**
- Tipos: `ICapabilities`, `CodigoNivelAcceso`

**Notas de Implementación:**
- Las constantes son la "tabla de verdad" del sistema de permisos
- Todos los textos en español siguiendo convenciones del proyecto
- Usar snake_case para claves de acciones (consistente con backend)

---

### **Paso 3: Crear Servicio de Evaluación de Permisos**

**Archivo:** `frontend/src/features/acl/utils/permissionEvaluator.ts` (nuevo)

**Acción:** Implementar lógica de evaluación de permisos sin dependencias de hooks

**Firmas de Función:**

```typescript
/**
 * Evaluador de permisos - Lógica pura para calcular capacidades
 * No depende de React (sin hooks)
 */

/**
 * Convierte nivel de acceso a capacidades del usuario
 * @param nivelAcceso - Código de nivel ('LECTURA' | 'ESCRITURA' | 'ADMINISTRACION')
 * @returns Objeto con capacidades del usuario
 */
export function getCapabilitiesFromLevel(
  nivelAcceso: CodigoNivelAcceso | null
): ICapabilities

/**
 * Evalúa si una acción específica está permitida
 * @param capabilities - Capacidades del usuario
 * @param action - Código de acción
 * @returns true si la acción está permitida
 */
export function canPerformAction(
  capabilities: ICapabilities,
  action: string
): boolean

/**
 * Obtiene mensaje descriptivo para acción deshabilitada
 * @param action - Código de acción
 * @param currentLevel - Nivel actual del usuario
 * @returns Mensaje descriptivo o null si la acción está habilitada
 */
export function getDisabledActionMessage(
  action: string,
  currentLevel: CodigoNivelAcceso | null
): string | null

/**
 * Filtra acciones disponibles basadas en capacidades
 * @param allActions - Lista de todas las acciones posibles
 * @param capabilities - Capacidades del usuario
 * @returns Array con acciones permitidas
 */
export function filterAvailableActions(
  allActions: string[],
  capabilities: ICapabilities
): string[]
```

**Pasos de Implementación:**
1. Crear `frontend/src/features/acl/utils/permissionEvaluator.ts`
2. Importar constantes: `PERMISSION_TO_CAPABILITIES`, `ACTION_REQUIREMENTS`, `DISABLED_ACTION_MESSAGES`
3. Importar tipos: `ICapabilities`, `CodigoNivelAcceso`
4. Implementar `getCapabilitiesFromLevel()`:
   - Recibir nivel de acceso
   - Retornar capacidades mapeadas de `PERMISSION_TO_CAPABILITIES`
   - Si nivel es null, retornar todas las capacidades en false
5. Implementar `canPerformAction()`:
   - Recibir capacidades y código de acción
   - Mapear acción a propiedad de `ICapabilities`
   - Retornar boolean
6. Implementar `getDisabledActionMessage()`:
   - Recibir acción y nivel actual
   - Buscar en `DISABLED_ACTION_MESSAGES`
   - Retornar mensaje o null si está habilitada
7. Implementar `filterAvailableActions()`:
   - Recibir array de acciones y capacidades
   - Retornar solo las acciones permitidas

**Dependencias:**
- Constantes: `PERMISSION_TO_CAPABILITIES`, `ACTION_REQUIREMENTS`, `DISABLED_ACTION_MESSAGES`
- Tipos: `ICapabilities`, `CodigoNivelAcceso`

**Notas de Implementación:**
- Esta es lógica pura (sin efectos secundarios)
- Fácil de testear (no requiere mocks de React)
- Reutilizable en cualquier contexto (componentes, hooks, servicios)
- Rendimiento: O(1) para búsquedas de capacidades

---

### **Paso 4: Crear Hook Custom para Capacidades de Permisos**

**Archivo:** `frontend/src/features/acl/hooks/usePermissionCapabilities.ts` (nuevo)

**Acción:** Implementar hook custom que combina datos de permisos con evaluación de capacidades

**Firma de Hook:**

```typescript
/**
 * Hook personalizado para obtener capacidades basadas en permisos del usuario
 * 
 * @param context - Contexto de evaluación (entityId, entityType, usuarioId)
 * @returns Objeto con capacidades, nivel de acceso, estado de carga y error
 * 
 * @example
 * const { capabilities, nivelAcceso, isLoading } = usePermissionCapabilities({
 *   entityId: folderId,
 *   entityType: 'carpeta',
 *   usuarioId: currentUser.id
 * })
 */
export function usePermissionCapabilities(
  context: IPermissionContext
): UsePermissionCapabilitiesResult
```

**Pasos de Implementación:**
1. Crear `frontend/src/features/acl/hooks/usePermissionCapabilities.ts`
2. Importar hooks existentes:
   - `useCarpetaPermisos()` o servicio equivalente
   - `useDocumentoPermisos()` o servicio equivalente
3. Importar funciones de evaluación:
   - `getCapabilitiesFromLevel()` de `permissionEvaluator.ts`
4. Importar tipos necesarios
5. Implementar lógica del hook:
   ```typescript
   - Determinar qué servicio usar según `entityType`
   - Consultar permisos del usuario para la entidad
   - Evaluar qué nivel tiene el usuario (documento > carpeta por precedencia)
   - Convertir nivel a capacidades usando `getCapabilitiesFromLevel()`
   - Retornar objeto con: capabilities, nivelAcceso, isLoading, error, refreshPermissions
   ```
6. Manejar estados:
   - Mientras carga: isLoading = true
   - Si error: mostrar error relevante
   - Si sin permisos: nivelAcceso = null, todas las capacidades = false
7. Agregar función `refreshPermissions()` para invalidar caché si es necesario (React Query)

**Dependencias:**
- Hooks: `useCarpetaPermisos`, `useDocumentoPermisos`
- Funciones: `getCapabilitiesFromLevel()`
- Tipos: `IPermissionContext`, `ICapabilities`, `CodigoNivelAcceso`

**Notas de Implementación:**
- El hook abstrae complejidad de consultar múltiples servicios
- Sigue patrón de React Query if applicable (manejo de caché)
- Manejo robusto de errores con mensajes amigables al usuario
- Soporte para refrescar en caso de cambios de permisos

---

### **Paso 5: Crear Componente de Botón Sensible a Permisos**

**Archivo:** `frontend/src/features/acl/components/PermissionAwareButton.tsx` (nuevo)

**Acción:** Implementar componente reutilizable de botón que se deshabilita automáticamente si no hay permiso

**Firma de Componente:**

```typescript
/**
 * Botón que se deshabilita automáticamente si el usuario no tiene permisos
 * Muestra tooltip explicativo cuando está deshabilitado
 */

interface PermissionAwareButtonProps extends ButtonProps {
  // Botón base
  children: React.ReactNode;
  action: string;  // Código de acción: 'upload', 'delete_folder', etc.
  
  // Permisos
  capabilities: ICapabilities;
  disabledMessage?: string;  // Mensaje personalizado (opcional)
  
  // Comportamiento
  onClick: () => void | Promise<void>;
  showTooltipOnDisabled?: boolean;  // Default: true
  
  // Styling (Tailwind)
  variant?: 'primary' | 'secondary' | 'danger';  // Default: 'primary'
  size?: 'sm' | 'md' | 'lg';                      // Default: 'md'
}

export const PermissionAwareButton: React.FC<PermissionAwareButtonProps>
```

**Pasos de Implementación:**
1. Crear `frontend/src/features/acl/components/PermissionAwareButton.tsx`
2. Importar:
   - `React`, `forwardRef` para ref forwarding
   - Componente `Button` base (si existe en proyecto)
   - `Tooltip` component (usar existente o Radix UI)
   - `canPerformAction()` de `permissionEvaluator.ts`
   - `getDisabledActionMessage()` de `permissionEvaluator.ts`
3. Implementar componente:
   - Calcular si acción está permitida usando `canPerformAction()`
   - Si está deshabilitada:
     - Obtener mensaje usando `getDisabledActionMessage()`
     - Envolver en `<Tooltip>` con mensaje
     - Deshabilitar atributo `disabled`
   - Si está habilitada:
     - Renderizar botón normal con onClick funcional
4. Estilos Tailwind:
   - Estado deshabilitado: `cursor-not-allowed opacity-50`
   - Con tooltip: agregar pequeño icono de interrogación o exclamación
5. Accessibility:
   - `aria-disabled` reflexionando estado
   - `aria-label` descriptivo
   - Soporte para keyboard navigation

**Dependencias:**
- Componentes: `Button`, `Tooltip`
- Funciones: `canPerformAction()`, `getDisabledActionMessage()`
- Tipos: `ICapabilities`

**Notas de Implementación:**
- Componente very reutilizable, aplicable en cualquier interfaz
- Tooltips mejoran UX explicando por qué algo está deshabilitado
- Soportar ref forwarding para integración con forms
- Consistencia visual con design system del proyecto

---

### **Paso 6: Crear Componente de Menú Sensible a Permisos**

**Archivo:** `frontend/src/features/acl/components/PermissionAwareMenu.tsx` (nuevo)

**Acción:** Implementar menú contextual que filtra/deshabilita opciones según permisos

**Firma de Componente:**

```typescript
/**
 * Menú contextual que filtra opciones basado en capacidades del usuario
 * Muestra tooltips para opciones deshabilitadas
 */

interface MenuAction {
  id: string;             // 'upload', 'delete_folder', etc.
  label: string;          // Texto visible
  icon?: React.ReactNode;
  onClick: () => void | Promise<void>;
  variant?: 'default' | 'danger';  // Default: 'default'
}

interface PermissionAwareMenuProps {
  actions: MenuAction[];
  capabilities: ICapabilities;
  trigger?: React.ReactNode;  // Componente botón trigger
  showDisabledItems?: boolean;  // Default: true (mostrar deshabilitados con tooltip)
}

export const PermissionAwareMenu: React.FC<PermissionAwareMenuProps>
```

**Pasos de Implementación:**
1. Crear `frontend/src/features/acl/components/PermissionAwareMenu.tsx`
2. Importar:
   - Componente `Menu` o `Dropdown` (usar Radix UI, Headless UI, o similar)
   - `canPerformAction()`, `getDisabledActionMessage()`
   - `Tooltip` component
3. Implementar lógica:
   - Para cada acción en `actions` array:
     - Calcular si está permitida con `canPerformAction()`
     - Si `showDisabledItems` es true:
       - Renderizar item deshabilitado con tooltip
       - Tooltip muestra mensaje por qué está deshabilitada
     - Si `showDisabledItems` es false:
       - Omitir acción deshabilitada del render
4. Styling:
   - Usar Tailwind consistente con design system del proyecto
   - Items deshabilitados: `text-gray-400 cursor-not-allowed`
   - Items peligro (danger): texto rojo si está habilitado
5. Accesibilidad:
   - `aria-disabled` en items deshabilitados
   - `role="menuitem"`

**Dependencias:**
- Componentes: `Menu/Dropdown`, `Tooltip`
- Funciones: `canPerformAction()`, `getDisabledActionMessage()`
- Tipos: `ICapabilities`

**Notas de Implementación:**
- Componente muy usado en carpetas y vista de documentos
- Opción `showDisabledItems` permite dos UX diferentes
- Mantener orden de acciones original
- Soportar iconos para mejor visual scanning

---

### **Paso 7: Integrar en Componentes Existentes (Carpetas)**

**Archivos:** `frontend/src/features/folders/components/FolderExplorer.tsx`, `FolderItem.tsx`, `FolderContextMenu.tsx`

**Acción:** Integrar capacidades de permisos en componentes de navegación de carpetas

**Pasos de Implementación:**

1. **En FolderExplorer.tsx:**
   - Usar hook `usePermissionCapabilities()` con contexto de carpeta actual
   - Pasar `capabilities` a componentes hijos
   - Mostrar estado de carga mientras se evalúan permisos
   - Mostrar mensajes si no hay acceso a la carpeta

2. **En FolderItem.tsx (para cada item):**
   - Recibir `capabilities` como prop
   - Si es carpeta:
     - Botón eliminar: mostrar solo si `capabilities.canDeleteFolder`
     - Click para navegar: mostrar solo si `capabilities.canRead`
   - Si es documento:
     - Botón descargar: mostrar si `capabilities.canDownload`
     - Botón editar: mostrar si `capabilities.canWrite`
     - Con `PermissionAwareButton` para manejo automático

3. **En FolderContextMenu.tsx:**
   - Usar `PermissionAwareMenu` para acciones contextuales
   - Acciones para carpetas:
     - Crear carpeta: requiere `canWrite`
     - Eliminar carpeta: requiere `canAdminister`
     - Gestionar permisos: requiere `canAdminister`
   - Acciones para documentos:
     - Descarga: requiere `canDownload`
     - Modificar: requiere `canWrite`
     - Suprimir versión: requiere `canAdminister`

4. **En CreateFolderModal.tsx (si existe):**
   - Deshabilitar form si usuario no tiene `canWrite`
   - Mostrar mensaje claro de permiso insuficiente

**Dependencias:**
- Hook: `usePermissionCapabilities()`
- Componentes: `PermissionAwareButton`, `PermissionAwareMenu`
- Funciones: evaluadores de permisos

**Notas de Implementación:**
- Pasar `capabilities` mediante prop drilling o Context si hay muchos niveles
- Manejar loading state mientras se cargan permisos
- Validación cliente + validación servidor (nunca confiar solo en cliente)
- Logs en consola para debugging de qué permisos se detectan

---

### **Paso 8: Integrar en Componentes de Documentos**

**Archivos:** `frontend/src/features/documents/components/DocumentActions.tsx`, `DocumentVersionModal.tsx`

**Acción:** Integrar capacidades de permisos en acciones de documentos

**Pasos de Implementación:**

1. **En DocumentActions.tsx (si existe):**
   - Usar `usePermissionCapabilities()` con contexto de documento
   - Botón Descargar: `PermissionAwareButton` con acción 'download'
   - Botón Crear Versión: `PermissionAwareButton` con acción 'create_version'
   - Botón Cambiar Versión: `PermissionAwareButton` con acción 'change_version'
   - Menú Más Opciones: `PermissionAwareMenu` para acciones adicionales

2. **En DocumentVersionModal.tsx:**
   - Si `!capabilities.canChangeVersion`: deshabilitar selector de versión
   - Mostrar tooltip explicativo

3. **En Upload Component:**
   - Usar `PermissionAwareButton` para botón "Subir Documento"
   - Si `!capabilities.canUpload`: deshabilitado con mensaje claro
   - Input file también deshabilitado en HTML

4. **En Document List/Detail View:**
   - Mostrar "badge" o indicador de nivel de acceso actual
   - Ejemplo: "Lectura" en color gris, "Escritura" en azul, "Admin" en rojo

**Dependencias:**
- Hook: `usePermissionCapabilities()`
- Componentes: `PermissionAwareButton`, `PermissionAwareMenu`

**Notas de Implementación:**
- Cascada de deshabilitación: si `!canRead`, ocultarcomponente completo
- Mensajes de error específicos si se intenta acción sin permiso
- Validación servidor debe duplicar esta lógica (nunca confiar solo en cliente)

---

### **Paso 9: Actualizar Documentación Técnica**

**Acción:** Revisar y actualizar documentación según cambios realizados

**Pasos de Implementación:**
1. **Revisar Cambios:** Analizar todos los cambios de código realizados:
   - Hook `usePermissionCapabilities()`
   - Servicios de evaluación de permisos
   - 3 nuevos componentes (Button, Menu, etc.)
   - Integraciones en carpetas y documentos

2. **Identificar Archivos de Documentación:**
   - `ai-specs/specs/frontend-standards.mdc` → Actualizar sección de componentes
   - `ai-specs/specs/api-spec.yml` → No requiere cambios (APIs ya existen)
   - `README.md` (si aplica) → Agregar nota sobre control de acceso en UI
   - Crear nuevo archivo: `ai-specs/specs/acl-frontend-guide.md` (guía de uso de permisos)

3. **Actualizar Documentación:**

   **frontend-standards.mdc:**
   - Agregar sección: "Permission-Aware Components and Patterns"
   - Documentar uso de `usePermissionCapabilities()`
   - Ejemplos de `PermissionAwareButton` y `PermissionAwareMenu`
   - Best practices para visualización de permisos

   **Crear acl-frontend-guide.md:**
   ```markdown
   # Guía de Control de Acceso en Frontend (ACL)

   ## Concepto General
   - describes el sistema de niveles de acceso
   - mapping a capacidades de usuario
   - patrón de "Permission-Aware" components

   ## Usar Capacidades en Componentes
   1. Hook: `usePermissionCapabilities()`
   2. Pasar `capabilities` a subcomponentes
   3. Usar `PermissionAwareButton` para botones
   4. Usar `PermissionAwareMenu` para menús
   5. Custom logic con evaluadores (canPerformAction)

   ## Ejemplos de Código
   - Ejemplo: Botón de Subir Condicionado
   - Ejemplo: Menú Contextual Filtrado
   - Ejemplo: Evaluación Manual de Permisos

   ## Testing
   - Cómo testear componentes con capacidades
   - Mock data para diferentes niveles
   ```

4. **Verificar Documentación:**
   - Confirmar que todos los cambios están documentados
   - Mantener consistencia con estructura de documentación existente
   - Verificar referencias cruzadas

**Referencias:**
- Seguir proceso en `ai-specs/specs/documentation-standards.mdc`
- Mantener consistencia en idioma inglés para documentación técnica
- Nombres de componentes, funciones y tipos en inglés

**Notas:**
- Paso MANDATORIO antes de considerar implementación completa
- No saltar actualización de documentación

---

## Orden de Implementación

1. **Paso 0:** Crear rama feature
2. **Paso 1:** Extender tipos TypeScript
3. **Paso 2:** Crear constantes de evaluación
4. **Paso 3:** Servicio/utilidad de evaluación (sin React)
5. **Paso 4:** Hook custom `usePermissionCapabilities`
6. **Paso 5:** Componente `PermissionAwareButton`
7. **Paso 6:** Componente `PermissionAwareMenu`
8. **Paso 7:** Integración en componentes de carpetas
9. **Paso 8:** Integración en componentes de documentos
10. **Paso 9:** Actualizar documentación técnica
11. **Paso 10:** Commit y Push a rama feature
12. **Paso 11:** Crear Pull Request

---

## Lista de Verificación de Testing

### Componentes y Hooks
- ✅ Hook `usePermissionCapabilities` retorna capacidades correctas
- ✅ `PermissionAwareButton` se deshabilita cuando no hay permiso
- ✅ `PermissionAwareButton` muestra tooltip en estado deshabilitado
- ✅ `PermissionAwareMenu` filtra acciones según capacidades
- ✅ `PermissionAwareMenu` muestra tooltip para acciones deshabilitadas

### Integración en Carpetas
- ✅ Carpeta con `LECTURA`: botón "Crear carpeta" deshabilitado
- ✅ Carpeta con `LECTURA`: botón "Eliminar" deshabilitado
- ✅ Carpeta con `LECTURA`: botón "Gestionar Permisos" deshabilitado
- ✅ Carpeta con `ESCRITURA`: botón "Crear carpeta" habilitado
- ✅ Carpeta con `ADMINISTRACION`: todos los botones habilitados
- ✅ Carpeta sin permiso: UI muestra mensaje de acceso denegado

### Integración en Documentos
- ✅ Documento con `LECTURA`: botón "Subir Versión" deshabilitado
- ✅ Documento con `LECTURA`: botón "Descargar" habilitado
- ✅ Documento con `ESCRITURA`: botón "Subir Versión" habilitado
- ✅ Documento con `ADMINISTRACION`: botón "Gestionar Permisos" habilitado
- ✅ Documento sin permiso: no está visible en lista

### Validación de UX
- ✅ Tooltips tienen mensaje claro en español
- ✅ Iconos visuales indican permisos (si aplica)
- ✅ Retroalimentación visual clara de estado deshabilitado
- ✅ Navegación accesible con teclado
- ✅ Lectores de pantalla anuncian estado deshabilitado

### Edge Cases
- ✅ Usuario sin token: todas las acciones deshabilitadas
- ✅ Cambio dinámico de permisos: UI se actualiza en tiempo real
- ✅ Error al cargar permisos: UI muestra estado de error
- ✅ Datos corruptos/inconsistentes: UI maneja gracefully

---

## Patrones de Manejo de Errores

### Errores Comunes

**401 Unauthorized (Token Expirado):**
- Redirigir a login
- Mostrar: "Tu sesión expiró, por favor inicia sesión nuevamente"

**403 Forbidden (Sin Permiso):**
- No mostrar acción deshabilitada
- Log en consola (nunca exponer en UI)
- Mensaje: "No tienes permiso para realizar esta acción"

**404 Not Found (Entidad no existe o no accesible):**
- Mostrar: "Este recurso no está disponible"
- Redirigir a vista anterior

**500 Server Error:**
- Mostrar: "Error al evaluار permisos. Por favor recarga la página"
- Permitir reintento con `refreshPermissions()`

---

## Notas Importantes

### Seguridad
- **Cliente-side NO es suficiente:** Las validaciones de servidor son obligatorias
- **Token claims:** Backend debe validar permisos en cada request
- **NUNCA confiar en UI:** Usuario malicioso puede modificar DOM y hacer requests no autorizados
- **Auditoria:** Loguear intentos de acciones no autorizados

### Performance
- **Caching:** Hook reutiliza datos cachés para evitar requests excesivos
- **Invalidación:** Actualizar caché cuando hay cambios de permisos
- **Lazy evaluation:** Evaluar permisos solo cuando necesario

### UX
- **Anticipar Errores:** Mostrar qué no pueden hacer en lugar de esperar error del servidor
- **Mensajes Amigables:** Todos en español, claros y accionables
- **Modo Offline:** Si se pierde connección, asumir que permisos siguen vigentes (con caveat)

---

## Dependencias y Prerrequisitos

### Tasks Bloqueantes (Deben estar completas)
- ✅ US-ACL-001: Catálogo de niveles (backend + frontend)
- ✅ US-ACL-002: Crear ACL de carpeta (backend + frontend)
- ✅ US-ACL-006: Precedencia de permisos (backend - **CRÍTICO**)
- ✅ US-FOLDER-005: UI de navegación de carpetas
- ✅ US-DOC-001 a US-DOC-004: Gestión de documentos (backend + UI mínima)

### Tasks Desbloqueadas por Esta
- ⏳ US-ACL-008: Enforcement ESCRITURA (dependencia: esta tarea)
- ⏳ US-FOLDER-003: Mover documento (requiere validación UI de permisos)
- ⏳ Auditoría: Registrar intentos fallidos por falta de permiso

---

## Referencias Cruzadas

- Backend: `US-ACL-006_backend.md` (Enforcement de precedencia)
- Frontend: `US-ACL-001_frontend.md` (Tipos y servicios ACL)
- Frontend: `US-ACL-002_frontend.md` (Creación y gestión de ACL)
- Frontend: `US-FOLDER-005_frontend.md` (Navegación de carpetas)
