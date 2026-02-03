# Frontend Implementation Plan: US-ACL-003 Revocar permiso de carpeta (eliminar ACL)

## 1. Overview
Implementar el flujo de revocación de permisos ACL de carpeta en el frontend, integrándolo con el servicio existente y asegurando confirmación explícita, manejo de errores y actualización del estado. La solución debe seguir la arquitectura por features, con componentes funcionales en TypeScript, uso de hooks y estilos con Tailwind.

## 2. Architecture Context
- **Componentes involucrados**:
  - `AclCarpetaSection` (orquestación del feature)
  - `AclCarpetaList` (tabla de permisos, acciones de revocación)
  - `AclCarpetaModal` (no aplica directamente a revocación, pero convive en la sección)
- **Servicios**:
  - `aclCarpetaService.ts` (método `deleteAcl` ya disponible)
- **Hooks**:
  - `useAclCarpeta` (método `deleteAcl` ya disponible)
- **Archivos referenciados**:
  - `frontend/src/features/acl/services/aclCarpetaService.ts`
  - `frontend/src/features/acl/hooks/useAclCarpeta.ts`
  - `frontend/src/features/acl/components/AclCarpetaList.tsx`
  - `frontend/src/features/acl/components/AclCarpetaSection.tsx`
  - `frontend/src/features/acl/types/index.ts`
- **Routing**: no se requieren rutas nuevas.
- **Estado**: estado local con `useState` y `useCallback` en hook; control de permisos desde `AclCarpetaSection`.

## 3. Implementation Steps

### Step 1: Validar servicio de revocación y manejo de errores
- **File**: `frontend/src/features/acl/services/aclCarpetaService.ts`
- **Action**: Verificar que `deleteAcl` use el endpoint `DELETE /api/carpetas/{carpetaId}/permisos/{usuarioId}` y que el mapeo de errores sea coherente con los nuevos códigos (404/403/409/500).
- **Function Signature**: `deleteAcl(carpetaId: number, usuarioId: number): Promise<void>`
- **Implementation Steps**:
  1. Confirmar que el endpoint está alineado con la especificación.
  2. Ajustar mensajes de error si es necesario para distinguir 404 vs 403.
- **Dependencies**: `apiClient`, `isAxiosError`.
- **Implementation Notes**: Mantener mensajes en español para el usuario final.

### Step 2: Asegurar confirmación de revocación en UI
- **File**: `frontend/src/features/acl/components/AclCarpetaList.tsx`
- **Action**: Asegurar un diálogo de confirmación explícita antes de revocar. Validar texto y comportamiento.
- **Function/Component Signature**: `AclCarpetaList` con `onDelete(usuarioId)`.
- **Implementation Steps**:
  1. Revisar que la confirmación es clara y accesible.
  2. Mantener o ajustar el texto a: “¿Confirmar eliminación?” o “¿Deseas revocar el acceso?” según UX.
  3. Validar que el error de revocación se muestre en el mismo popup.
- **Dependencies**: `useState`.
- **Implementation Notes**: Respetar estilos Tailwind y accesibilidad (roles/aria).

### Step 3: Control de permisos para mostrar acciones de revocación
- **File**: `frontend/src/features/acl/components/AclCarpetaList.tsx`
- **Action**: Agregar un prop de control (ej. `canManage`) para mostrar/ocultar acciones “Editar” y “Eliminar” según permisos.
- **Function/Component Signature**:
  - `AclCarpetaListProps` → agregar `canManage?: boolean`
- **Implementation Steps**:
  1. Añadir prop opcional con default `false` o heredado desde `AclCarpetaSection`.
  2. Condicionar la columna de acciones y botones a `canManage`.
  3. Propagar la prop desde `AclCarpetaSection` usando `isAdmin`.
- **Dependencies**: Ninguna adicional.
- **Implementation Notes**: Mantener consistencia con reglas de roles (ADMIN / ADMINISTRACION).

### Step 4: Ajustar estado de eliminación y feedback
- **File**: `frontend/src/features/acl/components/AclCarpetaSection.tsx`
- **Action**: Confirmar que el estado `deletingUserIds` maneja correctamente loading y que `onAclDeleted` se invoca tras éxito.
- **Function Signature**: `handleDelete(usuarioId: number): Promise<void>`
- **Implementation Steps**:
  1. Validar que el estado se actualiza antes y después del `await`.
  2. Asegurar que el error se propaga para que el popup lo muestre.
- **Dependencies**: `useAclCarpeta`.
- **Implementation Notes**: Mantener el enfoque de actualización optimista del hook.

### Step 5: Update Technical Documentation
- **Action**: Actualizar documentación técnica relevante en español.
- **Implementation Steps**:
  1. **Review Changes**: Identificar cambios de UI y permisos.
  2. **Identify Documentation Files**:
     - `ai-specs/specs/api-spec.yml` (si falta información de errores en delete)
     - `frontend/README.md` o `frontend/src/features/acl/README.md` si existe guía del feature
  3. **Update Documentation**: Escribir en español y mantener estructura.
  4. **Verify Documentation**: Coherencia con el comportamiento final.
  5. **Report Updates**: Enumerar archivos actualizados.
- **References**: `ai-specs/specs/documentation-standards.md` (idioma español).
- **Notes**: Paso obligatorio antes de considerar la implementación completa.

## 4. Implementation Order
2. Step 1: Validar servicio de revocación y manejo de errores
3. Step 2: Asegurar confirmación de revocación en UI
4. Step 3: Control de permisos para mostrar acciones de revocación
5. Step 4: Ajustar estado de eliminación y feedback
6. Step 5: Update Technical Documentation

## 5. Testing Checklist
- [ ] Revocación retorna 204 y elimina la entrada en UI.
- [ ] Confirmación visible antes de revocar.
- [ ] Errores 403/404 muestran mensaje claro.
- [ ] Botones deshabilitados durante eliminación.
- [ ] Acciones ocultas para usuarios sin permisos.
- [ ] Pruebas de componentes (si existen en el proyecto) para el flujo de revocación.

## 6. Error Handling Patterns
- Capturar errores en `aclCarpetaService` y mapear mensajes claros.
- Propagar error desde `useAclCarpeta.deleteAcl` y mostrarlo en el popup.
- No exponer detalles sensibles en la UI.

## 7. UI/UX Considerations
- Mantener Tailwind para estilos coherentes con el resto del feature.
- Confirmación clara y accesible.
- Indicador de loading en botón de eliminación.
- Mensajes en español para el usuario final.

## 8. Dependencies
- No se requieren nuevas dependencias.
- Reutilizar `apiClient` y estructura actual del feature.

## 9. Notes
- El repositorio no contiene `.claude/sessions/context_session_us-acl-003.md`; si se exige, crear el archivo o confirmar su ausencia.
- Mantener TypeScript en componentes nuevos/actualizados.
- Evitar duplicar lógica de permisos fuera de `AclCarpetaSection`.

## 10. Next Steps After Implementation
- Verificar comportamiento con un token ADMIN y con usuario sin permisos.
- Validar que la API devuelve mensajes correctos.
- Revisar consistencia con el backend (endpoint DELETE).

## 11. Implementation Verification
- **Code Quality**: tipado correcto, sin `any`, naming consistente.
- **Functionality**: confirmación, revocación, actualización de lista.
- **Testing**: checklist completado.
- **Integration**: endpoint y headers correctos.
- **Documentation**: actualizaciones en español completas.
