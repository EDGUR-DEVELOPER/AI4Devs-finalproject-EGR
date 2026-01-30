# Plan de Implementación Frontend: US-ACL-002 — Permisos de Carpeta (ACL)

## Descripción General

Este ticket implementa listas de control de acceso (ACLs) granulares a nivel de carpeta en el frontend, permitiendo a los administradores asignar y gestionar permisos de usuario (LECTURA, ESCRITURA, ADMINISTRACION) en carpetas con herencia recursiva opcional a subcarpetas. La implementación sigue una arquitectura impulsada por componentes React utilizando principios de Diseño Atómico (átomos → moléculas → organismos → integración de página), TypeScript para seguridad de tipos y Tailwind CSS para estilos.

**Características principales:**
- Formulario modal fácil de usar para crear/actualizar ACLs
- Selector de usuario con búsqueda y exclusión de usuarios ya asignados
- Insignias visuales de permisos con código de colores
- Indicador de herencia recursiva
- Vista de tabla de permisos existentes con acciones editar/eliminar
- Manejo integral de errores y validación
- Estados de carga y actualizaciones optimistas de UI

## Contexto de Arquitectura

### Componentes Involucrados

**Estructura de Características:** `src/features/acl/`
- **Átomos:** `PermissionBadge.tsx`, `RecursiveIndicator.tsx` (elementos UI de bajo nivel)
- **Moléculas:** `NivelAccesoSelect.tsx`, `UserSelect.tsx` (componentes de campos de formulario)
- **Organismos:** `AclCarpetaModal.tsx`, `AclCarpetaList.tsx` (componentes compuestos complejos)
- **Integración:** `AclCarpetaSection.tsx` (características completas empaquetadas para fácil integración en páginas)

### Servicios y Hooks

- **Servicio HTTP:** `aclCarpetaService.ts` — Maneja todas las operaciones HTTP (crear, listar, actualizar, eliminar ACLs)
- **Hook Personalizado:** `useAclCarpeta.ts` — Administra estado de ACL, carga, errores y operaciones
- **Definiciones de Tipos:** `types/index.ts` — Interfaces TypeScript completas para el dominio ACL

### Puntos de Integración

- **Características de Carpeta:** `src/features/folders/components/FolderDetail.tsx` — Importará y mostrará el componente `AclCarpetaSection`
- **UI Compartida:** Utiliza el componente `NivelAccesoSelect` existente de la característica ACL para consistencia
- **Capa API:** Se comunica con puntos finales del backend:
  - `POST /api/carpetas/{carpeta_id}/permisos` (crear ACL)
  - `PATCH /api/carpetas/{carpeta_id}/permisos/{usuario_id}` (actualizar ACL)
  - `GET /api/carpetas/{carpeta_id}/permisos` (listar ACLs)
  - `DELETE /api/carpetas/{carpeta_id}/permisos/{usuario_id}` (eliminar ACL)

### Enfoque de Gestión de Estado

**Estado Local (hook useAclCarpeta):**
- `acls: IAclCarpeta[]` — Array de registros ACL para la carpeta actual
- `loading: boolean` — Estado de obtención de datos
- `error: string | null` — Mensajes de error

**Estado de UI (componente AclCarpetaSection):**
- `isModalOpen: boolean` — Alternar visibilidad del modal
- `editingAcl: IAclCarpeta | null` — Qué ACL se está editando (null = modo crear)
- `deletingUserIds: number[]` — Rastrear qué ACLs se están eliminando para UI de carga
- `isSubmitting: boolean` — Estado de envío de formulario
- `submitError: string | null` — Errores de envío de formulario

**No se necesita gestión de estado global** — Todo el estado es de alcance de características y se administra localmente.

### Estrategia de Estilos

- **Tailwind CSS** para todo el estilo (configuración de proyecto existente)
- **Código de Colores:** 
  - LECTURA → Azul (`bg-blue-100`, `text-blue-800`)
  - ESCRITURA → Ámbar (`bg-amber-100`, `text-amber-800`)
  - ADMINISTRACION → Rojo (`bg-red-100`, `text-red-800`)
- **Diseño Responsivo:** Enfoque mobile-first con puntos de ruptura para capacidad de respuesta de tabla
- **Accesibilidad:** Etiquetas ARIA, HTML semántico, navegación por teclado (Esc para cerrar modales)

## Pasos de Implementación
### Paso 1: Extender Definiciones de Tipos

**Archivo:** `src/features/acl/types/index.ts`

**Acción:** Agregar interfaces TypeScript completas para el modelo de dominio de ACL de carpeta, contratos de API y manejo de errores.

**Tipos Nuevos a Agregar:**
```typescript
// Interfaz de usuario (información mínima para asignación de ACL)
export interface IUsuario {
  id: number;
  email: string;
  nombre: string;
}

// Registro de ACL de carpeta (modelo de dominio principal)
export interface IAclCarpeta {
  id: number;
  carpeta_id: number;
  usuario_id: number;
  usuario: IUsuario;
  nivel_acceso: INivelAcceso;
  recursivo: boolean;
  fecha_creacion: string;
  fecha_actualizacion: string;
}

// Carga útil de solicitud para crear/actualizar
export interface CreateUpdateAclCarpetaDTO {
  usuario_id: number;
  nivel_acceso_codigo: CodigoNivelAcceso;
  recursivo: boolean;
  comentario_opcional?: string;
}

// Envoltorios de respuesta
export interface AclCarpetaApiResponse {
  data: IAclCarpeta;
  meta: { accion: string; timestamp: string; };
}

export interface ListAclCarpetaApiResponse {
  data: IAclCarpeta[];
  meta: { total: number; carpeta_id: number; };
}

export interface AclErrorResponse {
  error: {
    codigo: string;
    mensaje: string;
    detalles?: Record<string, unknown>;
  };
}
```

**Pasos de Implementación:**
1. Abrir `src/features/acl/types/index.ts`
2. Localizar la definición de tipo `AccionPermitida` existente
3. Agregar nuevas interfaces después de los tipos existentes (mantener agrupación alfabética)
4. Asegurar que todas las interfaces tengan comentarios JSDoc explicando propósito y campos
5. Exportar todos los tipos nuevos en las exportaciones del módulo

**Dependencias:** Ninguna (interfaces TypeScript puras)

**Notas de Implementación:**
- Los tipos se alinean con los contratos de API del backend de la especificación US-ACL-002
- Usar el tipo `CodigoNivelAcceso` ya definido en el archivo
- Mantener coherencia con patrones TypeScript existentes del proyecto
- Todos los timestamps en formato ISO 8601 (tipo string)

---

### Paso 2: Crear Capa de Servicio HTTP

**Archivo:** `src/features/acl/services/aclCarpetaService.ts` (nuevo archivo)

**Acción:** Implementar cliente HTTP para todas las operaciones de ACL de carpeta con manejo de errores.

**Firmas de Función:**
```typescript
export const aclCarpetaApi = {
  createAcl: (carpetaId: number, payload: CreateUpdateAclCarpetaDTO) => Promise<IAclCarpeta>,
  listAcls: (carpetaId: number) => Promise<IAclCarpeta[]>,
  updateAcl: (carpetaId: number, usuarioId: number, payload: Partial<CreateUpdateAclCarpetaDTO>) => Promise<IAclCarpeta>,
  deleteAcl: (carpetaId: number, usuarioId: number) => Promise<void>,
};
```

**Pasos de Implementación:**
1. Crear nuevo archivo `src/features/acl/services/aclCarpetaService.ts`
2. Importar axios y utilidades de manejo de errores: `import { isAxiosError } from 'axios'; import { apiClient } from '@core/shared/api/axiosInstance';`
3. Definir constantes de puntos finales para las cuatro operaciones CRUD
4. Implementar método `createAcl` con solicitud POST, extracción de errores
5. Implementar método `listAcls` con solicitud GET
6. Implementar método `updateAcl` con solicitud PATCH
7. Implementar método `deleteAcl` con solicitud DELETE
8. Crear función centralizada de extracción de mensajes de error (`extractErrorMessage`)
9. Agregar comentarios JSDoc completos para cada método con ejemplos

**Dependencias:** 
- `axios` (ya instalado)
- `apiClient` de `@core/shared/api/axiosInstance`
- Tipos personalizados de `../types`

**Notas de Implementación:**
- Todos los puntos finales siguen el patrón: `/api/carpetas/{carpetaId}/permisos[/{usuarioId}]`
- Las respuestas de error se analizan a partir de la interfaz `AclErrorResponse`
- Mensajes de error fáciles de usar para códigos de estado HTTP comunes (400, 403, 404, 409)
- No se necesita lógica de reintento (manejada por interceptores)
- Todas las operaciones son basadas en async/Promise

---

### Paso 3: Crear Hook Personalizado para Gestión de Estado

**Archivo:** `src/features/acl/hooks/useAclCarpeta.ts` (nuevo archivo)

**Acción:** Implementar hook de React para administrar estado de ACL, obtención y mutaciones con caché local.

**Firma de Hook:**
```typescript
export const useAclCarpeta = (
  carpetaId: number,
  autoLoad: boolean = true
): UseAclCarpetaReturn => { /* ... */ }

export interface UseAclCarpetaReturn {
  acls: IAclCarpeta[];
  loading: boolean;
  error: string | null;
  refreshAcls: () => Promise<void>;
  createAcl: (payload: CreateUpdateAclCarpetaDTO) => Promise<IAclCarpeta>;
  updateAcl: (usuarioId: number, payload: Partial<CreateUpdateAclCarpetaDTO>) => Promise<IAclCarpeta>;
  deleteAcl: (usuarioId: number) => Promise<void>;
  clearError: () => void;
}
```

**Pasos de Implementación:**
1. Crear nuevo archivo `src/features/acl/hooks/useAclCarpeta.ts`
2. Importar hooks de React: `import { useState, useCallback, useEffect } from 'react';`
3. Importar servicio: `import { aclCarpetaApi } from '../services/aclCarpetaService';`
4. Definir interfaz `AclState` para estructura de estado interna
5. Implementar hook `useAclCarpeta` con:
   - Inicialización de estado usando `useState`
   - Callback `refreshAcls` para obtener de API
   - Callback `createAcl` que agrega a estado local
   - Callback `updateAcl` que actualiza en estado local
   - Callback `deleteAcl` que elimina de estado local
   - Callback `clearError` para restablecer estado de error
   - `useEffect` para auto-cargar al montar si `autoLoad = true`
6. Memoizar callbacks usando `useCallback` para optimización de dependencias
7. Agregar JSDoc completo con ejemplos de uso

**Dependencias:**
- Hooks de React: `useState`, `useCallback`, `useEffect`
- Servicio: `aclCarpetaApi`
- Tipos: `IAclCarpeta`, `CreateUpdateAclCarpetaDTO`

**Notas de Implementación:**
- El hook administra tanto estado de datos como estado de UI (carga, error)
- Actualizaciones optimistas: modificar estado local antes de la llamada API
- Extracción de errores: convertir errores capturados a cadenas fáciles de usar
- `autoLoad=true` por defecto para conveniencia
- Arrays de dependencia configurados correctamente para useCallback/useEffect

---

### Paso 4: Crear Componentes Atómicos

**Archivo 1:** `src/features/acl/components/PermissionBadge.tsx` (nuevo archivo)

**Acción:** Implementar componente de insignia visual para mostrar nivel de permiso con código de colores.

**Firma de Componente:**
```typescript
export interface PermissionBadgeProps {
  nivel: CodigoNivelAcceso;
  tooltip?: string;
  className?: string;
  showLabel?: boolean;
}

export const PermissionBadge: React.FC<PermissionBadgeProps> = ({ ... }) => JSX.Element
```

**Pasos de Implementación:**
1. Crear `PermissionBadge.tsx`
2. Definir constante `PERMISSION_STYLES` mapeando cada nivel a colores y etiquetas
3. Implementar componente para renderizar `<span>` con clases Tailwind apropiadas
4. Agregar atributo `title` para tooltip al pasar el mouse
5. Agregar `role="status"` y `aria-label` para accesibilidad
6. Manejar prop `showLabel` para alternar visualización de texto

**Dependencias:** Ninguna (React puro + Tailwind)

**Notas de Implementación:**
- Colores: LECTURA=azul, ESCRITURA=ámbar, ADMINISTRACION=rojo
- Estilo de insignia en línea con clases de utilidad Tailwind
- Reutilizable en toda la UI de ACL (tablas, listas, pantallas)

---

**Archivo 2:** `src/features/acl/components/RecursiveIndicator.tsx` (nuevo archivo)

**Acción:** Implementar componente indicador mostrando si el permiso es recursivo (heredado a subcarpetas).

**Firma de Componente:**
```typescript
export interface RecursiveIndicatorProps {
  recursivo: boolean;
  className?: string;
  showLabel?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

export const RecursiveIndicator: React.FC<RecursiveIndicatorProps> = ({ ... }) => JSX.Element
```

**Pasos de Implementación:**
1. Crear `RecursiveIndicator.tsx`
2. Implementar renderizado condicional: recursivo → icono de carpeta anidada verde, no recursivo → icono de carpeta gris único
3. Agregar mapeo `sizeClasses` para tamaños de iconos (sm, md, lg)
4. Renderizar iconos SVG en línea o usar librería de iconos si está disponible
5. Agregar atributos de accesibilidad y etiquetas
6. Soportar prop `showLabel` para mostrar/ocultar texto "Recursivo" o "Directo"

**Dependencias:** Ninguna (iconos SVG en línea)

**Notas de Implementación:**
- Dos estados visuales: recursivo (verde, icono de árbol) vs directo (gris, icono de carpeta)
- Iconos renderizados en línea como SVG para personalización total
- Accesible a través de aria-label y role="status"

---

### Paso 5: Crear Componentes de Moléculas (Campos de Formulario)

**Archivo 1:** `src/features/acl/components/UserSelect.tsx` (nuevo archivo)

**Acción:** Implementar dropdown de usuario con búsqueda para formulario de ACL con filtrado de exclusión.

**Firma de Componente:**
```typescript
export interface UserSelectProps {
  users: IUsuario[];
  value: number | '';
  onChange: (usuarioId: number) => void;
  label?: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  error?: string | null;
  className?: string;
  loading?: boolean;
  excludeUserIds?: number[];
  showSearch?: boolean;
}

export const UserSelect: React.FC<UserSelectProps> = ({ ... }) => JSX.Element
```

**Pasos de Implementación:**
1. Crear `UserSelect.tsx`
2. Implementar botón dropdown (muestra usuario seleccionado o placeholder)
3. Agregar campo de entrada de búsqueda (visible para >5 usuarios o si `showSearch=true`)
4. Filtrar usuarios basándose en:
   - Término de búsqueda (nombre o email contiene término)
   - Lista de exclusión (eliminar usuarios ya asignados)
5. Renderizar lista dropdown con avatares/iniciales de usuarios y email
6. Manejar selección y cerrar dropdown
7. Mostrar estado de carga si `loading=true`
8. Mostrar mensaje de error si se proporciona
9. Agregar indicador requerido si `required=true`
10. Implementar navegación por teclado (Esc para cerrar)

**Dependencias:** Hooks de React (useState, useMemo, useEffect)

**Notas de Implementación:**
- Usa patrón de dropdown no controlado (estado abierto/cerrado interno)
- Término de búsqueda limpiado cuando se cierra dropdown
- Usuario seleccionado mostrado con nombre y email
- Usuarios excluidos filtrados automáticamente
- Accesible con atributos ARIA y HTML semántico

---

### Paso 6: Crear Componentes de Organismos (Formularios y Tablas)

**Archivo 1:** `src/features/acl/components/AclCarpetaModal.tsx` (nuevo archivo)

**Acción:** Implementar diálogo modal de formulario para crear y actualizar permisos de ACL.

**Firma de Componente:**
```typescript
export interface AclCarpetaModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (acl: IAclCarpeta) => void;
  users: IUsuario[];
  usersLoading?: boolean;
  editingAcl?: IAclCarpeta | null;
  assignedUserIds?: number[];
  onSubmit: (payload: CreateUpdateAclCarpetaDTO) => Promise<IAclCarpeta>;
  isSubmitting?: boolean;
  submitError?: string | null;
  title?: string;
}

export const AclCarpetaModal: React.FC<AclCarpetaModalProps> = ({ ... }) => JSX.Element | null
```

**Pasos de Implementación:**
1. Crear `AclCarpetaModal.tsx`
2. Implementar estructura modal: telón de fondo + diálogo centrado
3. Agregar encabezado de formulario con título y botón de cierre
4. Implementar campos de formulario:
   - Componente `UserSelect` (deshabilitado en modo edición)
   - Componente `NivelAccesoSelect` para nivel de acceso
   - Casilla de verificación para permiso recursivo
5. Agregar validación de formulario:
   - Requerido: usuario_id, nivel_acceso_codigo
   - Mostrar errores de validación encima del formulario
6. Implementar controlador de envío de formulario:
   - Validar formulario
   - Llamar prop `onSubmit`
   - Manejar éxito (llamar `onSuccess`, cerrar modal)
   - Manejar error (mostrar mensaje de error)
7. Agregar controlador de evento de teclado (Esc para cerrar)
8. Soportar modos "Crear" y "Editar":
   - Modo edición: pre-rellenar formulario con valores existentes, deshabilitar selector de usuario
   - Modo crear: formulario vacío, habilitar todos los campos
9. Mostrar spinner de carga en botón de envío cuando `isSubmitting=true`

**Dependencias:**
- Hooks de React: useState, useEffect
- Componentes: UserSelect, NivelAccesoSelect
- Tipos: IAclCarpeta, CreateUpdateAclCarpetaDTO, CodigoNivelAcceso

**Notas de Implementación:**
- Modal devuelve `null` si no está abierto (renderizado eficiente)
- Estado del formulario restablecido cuando modal se abre/cierra
- Prevenir clic en telón de fondo si formulario se está enviando
- Pre-rellenar formulario desde `editingAcl` cuando está en modo edición
- Excluir usuarios ya asignados del dropdown (excepto usuario actual si se edita)

---

**Archivo 2:** `src/features/acl/components/AclCarpetaList.tsx` (nuevo archivo)

**Acción:** Implementar componente de tabla mostrando ACLs de carpeta con acciones editar/eliminar.

**Firma de Componente:**
```typescript
export interface AclCarpetaListProps {
  acls: IAclCarpeta[];
  loading?: boolean;
  error?: string | null;
  onEdit: (acl: IAclCarpeta) => void;
  onDelete: (acl: IAclCarpeta) => Promise<void>;
  deletingUserIds?: number[];
  emptyMessage?: string;
  showHeader?: boolean;
  className?: string;
}

export const AclCarpetaList: React.FC<AclCarpetaListProps> = ({ ... }) => JSX.Element
```

**Pasos de Implementación:**
1. Crear `AclCarpetaList.tsx`
2. Implementar estructura de tabla: `<table>` con thead y tbody
3. Agregar columnas de tabla:
   - Usuario (nombre + email)
   - Permiso (usar componente `PermissionBadge`)
   - Alcance (usar componente `RecursiveIndicator`)
   - Fecha Asignación (timestamp formateado)
   - Acciones (botones Editar + Eliminar)
4. Implementar estado de carga: spinner + mensaje centrado
5. Implementar estado de error: caja de alerta roja con mensaje de error
6. Implementar estado vacío: ícono + mensaje personalizado
7. Implementar botones de acción:
   - Editar: abre modal para que usuario edite (llamar `onEdit`)
   - Eliminar: muestra popup de confirmación, luego llama `onDelete`
8. Agregar popup de confirmación de eliminación (estilo en línea o tooltip)
9. Mostrar spinner de carga en botón eliminar cuando se elimina (verificar `deletingUserIds`)
10. Formatear timestamp a formato legible específico de locale (Español)
11. Efectos hover y ajustes responsivos

**Dependencias:**
- Hooks de React: useState
- Componentes: PermissionBadge, RecursiveIndicator
- Tipos: IAclCarpeta

**Notas de Implementación:**
- Estado de carga mostrado solo durante obtención de datos inicial
- Confirmación de eliminación mostrada en línea junto a botón de eliminación
- Efecto hover de fila para mejor UX
- Timestamps formateados en locale de español
- Capacidad de respuesta móvil: considerar scroll horizontal para tabla en pantallas pequeñas
- Estado vacío mostrado cuando `acls.length === 0`

---

### Paso 7: Crear Componente de Integración de Características

**Archivo:** `src/features/acl/components/AclCarpetaSection.tsx` (nuevo archivo)

**Acción:** Implementar paquete de características completo combinando modal, lista y lógica de gestión.

**Firma de Componente:**
```typescript
export interface AclCarpetaSectionProps {
  carpetaId: number;
  organizationUsers: IUsuario[];
  usersLoading?: boolean;
  usersError?: string | null;
  onAclCreated?: (acl: IAclCarpeta) => void;
  onAclUpdated?: (acl: IAclCarpeta) => void;
  onAclDeleted?: (aclId: number, usuarioId: number) => void;
  isAdmin?: boolean;
  title?: string;
  className?: string;
}

export const AclCarpetaSection: React.FC<AclCarpetaSectionProps> = ({ ... }) => JSX.Element
```

**Pasos de Implementación:**
1. Crear `AclCarpetaSection.tsx`
2. Usar hook `useAclCarpeta` para administrar estado de ACL para la carpeta
3. Agregar encabezado de sección con título y botón "Otorgar Permiso" (solo admin)
4. Implementar controlador de clic de botón: abrir modal en modo crear
5. Renderizar componente `AclCarpetaList` con:
   - ACLs desde estado hook
   - Callback editar: abrir modal en modo edición para ACL seleccionado
   - Callback eliminar: llamar método `deleteAcl` del hook
6. Renderizar componente `AclCarpetaModal` con:
   - Abierto/cerrado controlado por estado local
   - Envío de formulario llama `createAcl` o `updateAcl` del hook dependiendo de modo
   - Usuarios pasados desde props
   - IDs de usuario asignados filtrados de registros ACL actual
7. Mostrar error de carga de usuario si se proporciona en props
8. Mostrar controles solo para admin (botón + modal) condicionalmente basándose en prop `isAdmin`
9. Llamar callbacks de padre (`onAclCreated`, `onAclUpdated`, `onAclDeleted`) en operaciones exitosas

**Dependencias:**
- Hooks de React: useState, useEffect
- Hook personalizado: useAclCarpeta
- Componentes: AclCarpetaModal, AclCarpetaList
- Tipos: IAclCarpeta, IUsuario, CreateUpdateAclCarpetaDTO

**Notas de Implementación:**
- Este componente es el punto de entrada para gestión de permisos de carpeta
- Maneja toda la orquestación de flujos crear/editar/eliminar
- Fácil de integrar en componente `FolderDetail`
- Controles admin ocultos para usuarios no admin
- Auto-carga de ACLs en montar a través del hook
- Los callbacks permiten que componente padre reaccione a cambios

---

### Paso 8: Actualizar Exportaciones de Barril de Características

**Archivo:** `src/features/acl/index.ts`

**Acción:** Extender exportaciones de barril para incluir todos los componentes nuevos, hooks, servicios y tipos.

**Pasos de Implementación:**
1. Abrir `src/features/acl/index.ts`
2. Agregar exportaciones para tipos nuevos:
   ```typescript
   export type {
     IUsuario,
     IAclCarpeta,
     CreateUpdateAclCarpetaDTO,
     AclCarpetaApiResponse,
     ListAclCarpetaApiResponse,
     AclErrorResponse,
   } from './types';
   ```
3. Agregar exportaciones para nuevo hook:
   ```typescript
   export { useAclCarpeta } from './hooks/useAclCarpeta';
   export type { UseAclCarpetaReturn } from './hooks/useAclCarpeta';
   ```
4. Agregar exportaciones para componentes atómicos:
   ```typescript
   export { PermissionBadge } from './components/PermissionBadge';
   export type { PermissionBadgeProps } from './components/PermissionBadge';
   export { RecursiveIndicator } from './components/RecursiveIndicator';
   export type { RecursiveIndicatorProps } from './components/RecursiveIndicator';
   ```
5. Agregar exportaciones para componentes de moléculas:
   ```typescript
   export { UserSelect } from './components/UserSelect';
   export type { UserSelectProps } from './components/UserSelect';
   ```
6. Agregar exportaciones para componentes de organismos:
   ```typescript
   export { AclCarpetaModal } from './components/AclCarpetaModal';
   export type { AclCarpetaModalProps } from './components/AclCarpetaModal';
   export { AclCarpetaList } from './components/AclCarpetaList';
   export type { AclCarpetaListProps } from './components/AclCarpetaList';
   ```
7. Agregar exportaciones para componente de integración:
   ```typescript
   export { AclCarpetaSection } from './components/AclCarpetaSection';
   export type { AclCarpetaSectionProps } from './components/AclCarpetaSection';
   ```
8. Agregar exportación para nuevo servicio:
   ```typescript
   export { aclCarpetaApi } from './services/aclCarpetaService';
   ```

**Dependencias:** Ninguna (solo sentencias de exportación)

**Notas de Implementación:**
- Mantener orden alfabético dentro de cada categoría para legibilidad
- Exportar tanto componentes como sus interfaces de props
- Punto de exportación centralizado permite a consumidores importar desde `@features/acl`

---

### Paso 9: Actualizar Documentación del Feature README

**Archivo:** `src/features/acl/README.md`

**Acción:** Actualizar la documentación con nuevos componentes, hooks, servicios y ejemplos de integración.

**Pasos de Implementación:**
1. Abrir `src/features/acl/README.md` existente
2. Actualizar sección de Descripción General para incluir gestión de ACL de carpeta
3. Agregar sección de Arquitectura que describa:
   - Jerarquía de componentes (Atomic Design)
   - Estructura de archivos
   - Flujo de datos
4. Agregar sección de Inicio Rápido con ejemplos de uso básico
5. Agregar sección de Componentes documentando:
   - `PermissionBadge` con props y uso
   - `RecursiveIndicator` con props y uso
   - `UserSelect` con props y uso
   - `AclCarpetaModal` con props y uso
   - `AclCarpetaList` con props y uso
   - `AclCarpetaSection` con props y uso
6. Agregar sección de Hooks documentando:
   - `useAclCarpeta` con ejemplos de uso y valores de retorno
   - `useNivelesAcceso` (existente)
7. Agregar sección de Servicios documentando:
   - Métodos de `aclCarpetaApi`
   - `aclApi` (existente)
8. Agregar sección de Definiciones de Tipos con interfaces clave
9. Agregar Ejemplo de Integración mostrando cómo usar en una página
10. Agregar sección de Patrones de Manejo de Errores
11. Agregar sección de Notas de Prueba para pruebas de componentes

**Dependencias:** Ninguna (solo documentación)

**Notas de Implementación:**
- Usar ejemplos de código estilo JSDoc
- Incluir patrones de uso tanto básicos como avanzados
- Vincular a interfaces de props de componentes para referencia

---

### Paso 10: Integración en Característica de Carpetas

**Archivo:** `src/features/folders/components/FolderDetail.tsx`

**Acción:** Integrar componente `AclCarpetaSection` en vista de detalles de carpeta (opcional, puede ser parte de US separada).

**Pasos de Implementación:**
1. Abrir `src/features/folders/components/FolderDetail.tsx`
2. Importar `AclCarpetaSection` desde la característica ACL:
   ```typescript
   import { AclCarpetaSection } from '@features/acl';
   ```
3. Importar hooks/servicios relacionados con usuarios según sea necesario para obtener usuarios de organización
4. Agregar sección para renderizar permisos de ACL:
   ```tsx
   {canManagePermissions && (
     <AclCarpetaSection
       carpetaId={folderId}
       organizationUsers={organizationUsers}
       isAdmin={userIsAdmin}
       onAclCreated={handleAclCreated}
       onAclUpdated={handleAclUpdated}
       onAclDeleted={handleAclDeleted}
     />
   )}
   ```
5. Implementar callbacks para mostrar mensajes de éxito/error al usuario
6. Probar que los permisos se muestren y se puedan gestionar

**Dependencias:** Componente AclCarpetaSection

**Notas de Implementación:**
- Este paso puede diferirse a un ticket separado para mejora de detalles de carpeta
- Asegurar que el ID de carpeta esté disponible antes de renderizar el componente
- Pasar `isAdmin` basado en verificación de rol de usuario
- Obtener usuarios de la organización si no están disponibles en el componente

---

### Paso 11: Actualizar Documentación Técnica

**Acción:** Revisar y actualizar archivos de documentación técnica para reflejar cambios realizados durante la implementación.

**Pasos de Implementación:**

1. **Revisar Cambios:**
   - Analizar todos los cambios de código: nuevos servicios, hooks, componentes, tipos
   - Identificar qué archivos de documentación necesitan actualizaciones

2. **Identificar Archivos de Documentación a Actualizar:**
   - `ai-specs/specs/frontend-standards.mdc` — Patrones de características ACL, jerarquía de componentes
   - `ai-specs/specs/api-spec.yml` — Endpoints de API para operaciones ACL (revisar contratos de backend)
   - Feature README: `src/features/acl/README.md` (ya actualizado en Paso 9)

3. **Actualizar Archivos de Documentación:**

   **A. frontend-standards.mdc:**
   - Agregar sección: "Patrón de Implementación de Características ACL"
   - Documentar:
     - Uso de Atomic Design (átomos → moléculas → organismos)
     - Arquitectura impulsada por componentes para permisos
     - Patrones de validación de formularios usados en AclCarpetaModal
     - Gestión de estado con hook useAclCarpeta
     - Patrones de manejo de errores en capa de servicio
   - Agregar ejemplo: "Implementando Formularios de Características con Validación"
   - Referencia: AclCarpetaModal como ejemplo

   **B. api-spec.yml:**
   - Documentar endpoints de ACL:
     - `POST /api/carpetas/{carpeta_id}/permisos` — Crear ACL
     - `PATCH /api/carpetas/{carpeta_id}/permisos/{usuario_id}` — Actualizar ACL
     - `GET /api/carpetas/{carpeta_id}/permisos` — Listar ACLs
     - `DELETE /api/carpetas/{carpeta_id}/permisos/{usuario_id}` — Eliminar ACL
   - Incluir esquemas de solicitud/respuesta
   - Documentar respuestas de error (400, 403, 404, 409)

4. **Verificar Documentación:**
   - Confirmar que todos los cambios se reflejan con precisión
   - Verificar consistencia de estructura con documentación existente
   - Asegurar que ejemplos de código sean válidos y probados

5. **Reportar Actualizaciones:**
   - Documentar qué archivos fueron actualizados y qué cambios se realizaron
   - Nota: Toda la documentación debe estar en inglés según estándares

**Referencias:**
- Seguir `ai-specs/specs/documentation-standards.mdc`
- Toda documentación debe estar en inglés

**Notas:** Este paso es OBLIGATORIO antes de considerar la implementación completa.

---

## Orden de Implementación

1. **Paso 0:** Crear Rama de Características (`feature/US-ACL-002-frontend`)
2. **Paso 1:** Extender Definiciones de Tipos (`types/index.ts`)
3. **Paso 2:** Crear Capa de Servicio HTTP (`services/aclCarpetaService.ts`)
4. **Paso 3:** Crear Hook Personalizado (`hooks/useAclCarpeta.ts`)
5. **Paso 4:** Crear Componentes Atómicos (`PermissionBadge.tsx`, `RecursiveIndicator.tsx`)
6. **Paso 5:** Crear Componentes de Moléculas (`UserSelect.tsx`)
7. **Paso 6:** Crear Componentes de Organismos (`AclCarpetaModal.tsx`, `AclCarpetaList.tsx`)
8. **Paso 7:** Crear Componente de Integración de Características (`AclCarpetaSection.tsx`)
9. **Paso 8:** Actualizar Exportaciones de Barril de Características (`index.ts`)
10. **Paso 9:** Actualizar Documentación del Feature README (`README.md`)
11. **Paso 10:** Integración en Característica de Carpetas (Opcional - diferir si es necesario)
12. **Paso 11:** Actualizar Documentación Técnica (frontend-standards.mdc, api-spec.yml)

---

## Lista de Verificación de Pruebas

### Pruebas de Funcionalidad de Componentes

- [ ] **PermissionBadge:**
  - [ ] Se renderiza con color correcto para cada nivel de permiso
  - [ ] Muestra etiqueta cuando `showLabel=true`
  - [ ] Muestra tooltip en mouse over
  - [ ] Accesible con etiquetas ARIA

- [ ] **RecursiveIndicator:**
  - [ ] Muestra ícono anidado verde para `recursivo=true`
  - [ ] Muestra ícono de carpeta gris para `recursivo=false`
  - [ ] Variantes de tamaño funcionan correctamente (sm, md, lg)

- [ ] **UserSelect:**
  - [ ] Dropdown se abre/cierra al hacer clic
  - [ ] Selección de usuario actualiza el valor
  - [ ] La búsqueda filtra usuarios por nombre y correo electrónico
  - [ ] Usuarios excluidos no se muestran en la lista
  - [ ] Muestra estado de carga cuando `loading=true`
  - [ ] Muestra mensaje de error si se proporciona

- [ ] **AclCarpetaModal:**
  - [ ] Modal se abre/cierra correctamente
  - [ ] Validación de formulario funciona (campos requeridos)
  - [ ] Modo crear: formulario vacío, todos los campos habilitados
  - [ ] Modo editar: formulario pre-rellenado, campo de usuario deshabilitado
  - [ ] Botón enviar muestra spinner de carga durante envío
  - [ ] Mensaje de error mostrado al fallar el envío
  - [ ] Tecla Esc cierra modal
  - [ ] Clic en fondo cierra modal

- [ ] **AclCarpetaList:**
  - [ ] Muestra todos los registros ACL en tabla
  - [ ] Muestra spinner de carga cuando `loading=true`
  - [ ] Muestra alerta de error si hay error
  - [ ] Muestra mensaje vacío cuando no hay ACLs
  - [ ] Botón editar abre modal con datos de ACL
  - [ ] Botón eliminar muestra popup de confirmación
  - [ ] Confirmar eliminación llama controlador `onDelete`
  - [ ] Marcas de tiempo formateadas en configuración regional española

- [ ] **AclCarpetaSection:**
  - [ ] Botón "Otorgar Permiso" visible solo para admins
  - [ ] Clic de botón abre modal en modo crear
  - [ ] Lista muestra todas las ACLs para la carpeta
  - [ ] Flujos crear/actualizar/eliminar funcionan de extremo a extremo
  - [ ] Callbacks disparados en operaciones exitosas
  - [ ] Error de carga de usuario mostrado si se proporciona

### Pruebas de Integración

- [ ] **Integración de Hook:**
  - [ ] `useAclCarpeta` carga automáticamente ACLs al montarse
  - [ ] Operación crear agrega ACL al estado local
  - [ ] Operación actualizar modifica ACL en estado local
  - [ ] Operación eliminar elimina ACL del estado local
  - [ ] Mensajes de error extraídos y mostrados

- [ ] **Integración del Servicio de API:**
  - [ ] Las solicitudes HTTP usan endpoints correctos
  - [ ] Las cargas útiles de solicitud coinciden con contratos de backend
  - [ ] Las respuestas de error se analizan correctamente
  - [ ] Se muestran mensajes de error amigables para el usuario

### Pruebas de Accesibilidad

- [ ] [ ] Modal tiene rol y etiquetas ARIA modales
- [ ] [ ] Los botones tienen etiquetas accesibles
- [ ] [ ] Los campos de formulario tienen etiquetas y mensajes de error
- [ ] [ ] Navegación de teclado (Tab, Enter, Esc) funciona
- [ ] [ ] El color no es el único indicador (también usar iconos/texto)
- [ ] [ ] Los iconos tienen aria-labels o son decorativos

### Pruebas de UI/UX

- [ ] Diseño responsivo en móvil/tablet/desktop
- [ ] Los estados de carga proporcionan retroalimentación visual
- [ ] Los mensajes de error son claros y accionables
- [ ] La confirmación de eliminación previene eliminación accidental
- [ ] La validación de formulario previene envíos inválidos
- [ ] El fondo modal previene interacción con la página
- [ ] Los efectos hover proporcionan retroalimentación visual

---

## Patrones de Manejo de Errores

### Capa de Servicio (`aclCarpetaService.ts`)

```typescript
// Extracción centralizada de errores
const extractErrorMessage = (error: unknown): string => {
  if (isAxiosError(error)) {
    // Intenta extraer mensaje de error del backend
    const data = error.response?.data as AclErrorResponse | undefined;
    if (data?.error?.mensaje) return data.error.mensaje;
    
    // Vuelve a caer en mensajes basados en estado HTTP
    switch (error.response?.status) {
      case 400: return 'Datos inválidos. Verifica los campos e intenta de nuevo.';
      case 403: return 'No tienes permisos para realizar esta acción.';
      case 404: return 'La carpeta o usuario no existe.';
      case 409: return 'Este permiso ya existe para el usuario en esta carpeta.';
      case 500: return 'Error en el servidor. Intenta de nuevo más tarde.';
    }
  }
  if (error instanceof Error) return error.message;
  return 'Error desconocido. Intenta de nuevo más tarde.';
};

// Todos los métodos lanzan Error con mensaje amigable para el usuario
export const aclCarpetaApi = {
  createAcl: async (carpetaId: number, payload) => {
    try {
      const response = await apiClient.post(...);
      return response.data.data;
    } catch (error) {
      throw new Error(extractErrorMessage(error));
    }
  },
};
```

### Capa de Componente (Modal y Sección)

```typescript
// Errores de validación de formulario
const validateForm = (): string | null => {
  if (!usuarioId) return 'Debes seleccionar un usuario';
  if (!nivelAccesoCodigo) return 'Debes seleccionar un nivel de acceso';
  return null;
};

// Mostrar errores de validación en cuadro de alerta arriba del formulario
{(formError || submitError) && (
  <div className="rounded-md bg-red-50 p-4 border border-red-200">
    <p className="text-sm font-medium text-red-800">{formError || submitError}</p>
  </div>
)}
```

### Capa de Hook (`useAclCarpeta`)

```typescript
// Operaciones asincrónicas envueltas en try-catch con actualizaciones de estado
const createAclLocal = useCallback(async (payload) => {
  setState((prev) => ({ ...prev, error: null }));
  try {
    const newAcl = await aclCarpetaApi.createAcl(carpetaId, payload);
    setState((prev) => ({ ...prev, acls: [...prev.acls, newAcl] }));
    return newAcl;
  } catch (err) {
    const message = err instanceof Error ? err.message : 'Error desconocido';
    setState((prev) => ({ ...prev, error: message }));
    throw err;
  }
}, [carpetaId]);
```

---

## Consideraciones de UI/UX

### Diseño de Componentes

- **Tailwind CSS:** Todo el estilo usando configuración Tailwind existente
- **Código de Color:** Niveles de permisos visualmente distintos (azul, ámbar, rojo)
- **Iconos:** Iconos SVG en línea para permisos e indicador recursivo
- **Tipografía:** Jerarquía clara con pesos y tamaños de fuente

### Diseño Responsivo

- **Móvil:** Dropdown/modal se apila verticalmente, columna única
- **Tablet:** La tabla puede desplazarse horizontalmente si es necesario
- **Desktop:** Disposición de tabla completa con todas las columnas visibles

### Carga y Retroalimentación

- **Carga de Datos:** Spinner con texto centrado
- **Envío de Formulario:** Botón muestra spinner, estado deshabilitado
- **Eliminación:** Popup de confirmación en línea
- **Errores:** Cuadro de alerta rojo con mensaje claro

### Accesibilidad

- **Navegación de Teclado:** Tab para navegar, Enter para seleccionar, Esc para cerrar
- **Lectores de Pantalla:** Roles ARIA, etiquetas y regiones activas
- **Color:** Nunca como único indicador; usar texto, iconos o patrones
- **Enfoque:** Indicadores de enfoque visibles en todos los elementos interactivos

---

## Dependencias

### Librerías Externas
- **React:** Framework principal (ya instalado)
- **Axios:** Cliente HTTP (ya instalado)
- **Tailwind CSS:** Estilos (ya configurado)

### Dependencias Internas
- `@core/shared/api/axiosInstance` — Instancia de Axios configurada
- `@features/acl/hooks/useNivelesAcceso` — Hook existente para niveles de acceso
- `@features/acl/components/NivelAccesoSelect` — Componente existente para selección de nivel

### Tipos de TypeScript
- Tipos estándar de React: `React.FC`, `React.ReactNode`, etc.
- Tipos de proyecto desde `types/index.ts`

---

## Notas

### Recordatorios Importantes

1. **Gestión de Rama:** Siempre trabajar en rama `feature/US-ACL-002-frontend`
2. **Seguridad de Tipos:** Usar TypeScript estrictamente; evitar tipo `any`
3. **Accesibilidad:** Cada componente debe ser navegable por teclado y amigable con lectores de pantalla
4. **Mensajes de Error:** Todos los mensajes de error en español (cara al usuario); inglés en comentarios de código
5. **Clases de Tailwind:** Usar solo utilidades de Tailwind; no se necesita CSS personalizado
6. **Reutilización de Componentes:** Componentes atómicos (PermissionBadge, RecursiveIndicator) reutilizables en toda la aplicación

### Reglas de Negocio

- Solo los admins pueden crear/actualizar/eliminar ACLs
- Los usuarios no pueden tener permisos duplicados asignados en la misma carpeta
- El permiso recursivo se aplica a todas las subcarpetas (backend lo garantiza)
- Los niveles de permisos tienen acciones permitidas específicas (definidas en backend)
- Se registra registro de auditoría para todos los cambios de permisos (backend lo maneja)

### Estándares de Código

- **Idioma:** Inglés para código; español para UI/mensajes de usuario
- **TypeScript:** Todos los archivos usan extensiones `.ts` o `.tsx`
- **Componentes:** Componentes funcionales con hooks (sin componentes de clase)
- **Nombres:** 
  - Componentes: PascalCase (`AclCarpetaModal`)
  - Funciones/variables: camelCase (`useAclCarpeta`)
  - Interfaces: PascalCase con prefijo `I` (`IAclCarpeta`)
  - Tipos: PascalCase (`CodigoNivelAcceso`)
- **Exportaciones:** Exportaciones nombradas para componentes y hooks
- **Comentarios:** JSDoc para APIs públicas, comentarios en línea para lógica compleja

---

## Próximos Pasos Después de la Implementación

1. **Revisión de Código:** Enviar PR para revisión de pares en rama de características
2. **Pruebas:** Ejecutar pruebas de componentes y lista de verificación de pruebas manuales
3. **Integración:** Fusionar en rama main/develop después de aprobación
4. **Documentación:** Asegurar que todos los documentos estén actualizados y vinculados en recursos centrales
5. **Integración de Backend:** Coordinar con equipo de backend para preparación de API
6. **Desplegar:** Incluir en próxima versión/implementación
7. **Monitorear:** Rastrear uso y recopilar retroalimentación de usuarios

---

## Verificación de Implementación

### Lista de Verificación Final

#### Calidad de Código
- [ ] Todos los errores de compilación de TypeScript resueltos
- [ ] ESLint pasa sin advertencias
- [ ] Sin errores o advertencias de consola en navegador
- [ ] No se usaron tipos `any` sin justificación
- [ ] El código sigue convenciones de nombres del proyecto
- [ ] Comentarios JSDoc en todas las APIs públicas

#### Funcionalidad
- [ ] Los 11 pasos de implementación completados
- [ ] Los componentes se renderizan sin errores
- [ ] La validación de formulario funciona correctamente
- [ ] Las llamadas de API usan endpoints y cargas útiles correctas
- [ ] El manejo de errores muestra mensajes amigables para el usuario
- [ ] Los estados de carga proporcionan retroalimentación visual
- [ ] La navegación de teclado modal funciona (Esc para cerrar)

#### Pruebas
- [ ] Las pruebas unitarias de componentes pasan
- [ ] Las pruebas de integración pasan
- [ ] La lista de verificación de pruebas manuales completada
- [ ] Las pruebas de accesibilidad pasan
- [ ] El diseño responsivo verificado en múltiples dispositivos

#### Integración
- [ ] Exportaciones de características adecuadas en `index.ts`
- [ ] Puede importarse desde `@features/acl`
- [ ] Sin conflictos con código existente
- [ ] Listo para integración con característica de carpetas

#### Documentación
- [ ] Feature README actualizado con ejemplos
- [ ] Definiciones de tipos bien documentadas con JSDoc
- [ ] Documentos técnicos (frontend-standards.mdc) actualizados
- [ ] Especificación de API (api-spec.yml) actualizada
- [ ] Toda documentación en inglés

#### Git y Rama
- [ ] Trabajar en rama `feature/US-ACL-002-frontend`
- [ ] Los commits son atómicos y bien descritos
- [ ] La rama lista para fusionar a main/develop
- [ ] Sin conflictos de fusión con rama base

---

**Estado de Implementación:** Listo para comenzar  
**Duración Estimada:** 2-3 días  
**Complejidad:** Medio-Alto (10+ componentes/hooks, 4 operaciones de API, UI completa)  
**Dependencias:** US-ACL-001 (niveles de backend completados), US-FOLDER-001 (carpetas existen en backend)
