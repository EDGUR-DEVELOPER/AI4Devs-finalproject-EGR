# Frontend Implementation Plan: US-ACL-004 Recursive Folder Permission Inheritance

## 1. Overview
Implement UI support for recursive folder permissions by surfacing the effective permission origin (direct vs inherited), enabling a clear recursive checkbox with tooltip, and visualizing inherited permissions in the ACL list. Follow the feature-based structure in frontend/ and keep API communication in the service layer, state in hooks, and UI in components.

## 2. Architecture Context
- Components involved:
  - frontend/src/features/acl/components/AclCarpetaSection.tsx
  - frontend/src/features/acl/components/AclCarpetaList.tsx
  - frontend/src/features/acl/components/AclCarpetaModal.tsx
  - frontend/src/features/acl/components/PermissionBadge.tsx (if badge extensions are needed)
  - frontend/src/features/acl/components/RecursiveIndicator.tsx (reused for recursive state)
- Services:
  - frontend/src/features/acl/services/aclCarpetaService.ts
- Hooks:
  - frontend/src/features/acl/hooks/useAclCarpeta.ts
  - (New) frontend/src/features/acl/hooks/useMiPermisoCarpeta.ts
- Types:
  - frontend/src/features/acl/types/index.ts
- Routing considerations:
  - "Go to origin folder" should use existing folder navigation route. Confirm the canonical folder route in App routing before implementation.
- State management:
  - Local state in hooks (consistent with current ACL feature pattern). If React Query exists elsewhere, keep parity with current ACL hook style.

## 3. Implementation Steps

### Step 1: Align Types for Effective Permission and Inherited ACL
- File: frontend/src/features/acl/types/index.ts
- Action: Add types for the effective permission response and inherited ACL list entries.
- Function/Component Signature:
  - interface IPermisoEfectivo
  - type AclCarpetaListItem = IAclCarpeta | IAclCarpetaHeredado (if backend supports inherited list)
- Implementation Steps:
  1. Add type to represent GET /api/carpetas/{id}/mi-permiso response (nivel_acceso, es_heredado, carpeta_origen_id, carpeta_origen_nombre, ruta_herencia).
  2. Define a list item shape for inherited permissions (same display fields plus metadata to distinguish origin).
  3. Ensure type names and fields align with backend contract and naming conventions.
- Dependencies: none
- Implementation Notes: Keep the existing IAclCarpeta unchanged for explicit permissions and introduce a distinct type for inherited items to avoid overloading.

### Step 2: Extend ACL Service with Effective Permission Endpoint
- File: frontend/src/features/acl/services/aclCarpetaService.ts
- Action: Add a method for GET /api/carpetas/{id}/mi-permiso and (if available) list of effective permissions.
- Function/Component Signature:
  - getMiPermiso(carpetaId: number): Promise<IPermisoEfectivo>
  - (Optional) listAclsEfectivos(carpetaId: number): Promise<AclCarpetaListItem[]>
- Implementation Steps:
  1. Add endpoint constant for mi-permiso.
  2. Add service method using apiClient.get and reuse extractErrorMessage.
  3. If backend exposes a list of inherited permissions, add a method for it and ensure proper error mapping.
- Dependencies: new types from Step 1
- Implementation Notes: If the backend does not provide inherited permissions yet, document the dependency and keep a placeholder method or feature flag so the UI can evolve without breaking.

### Step 3: Add Hook for Effective Permission
- File: frontend/src/features/acl/hooks/useMiPermisoCarpeta.ts (new)
- Action: Encapsulate loading/error state for the effective permission for the current user.
- Function/Component Signature:
  - useMiPermisoCarpeta(carpetaId: number): { permiso, loading, error, refetch }
- Implementation Steps:
  1. Mirror the pattern used in useAclCarpeta for consistency.
  2. Load on mount and expose refetch.
  3. Provide a clear loading/error contract for UI consumption.
- Dependencies: aclCarpetaApi.getMiPermiso
- Implementation Notes: Keep errors user-friendly; reuse messages from service.

### Step 4: Surface Effective Permission in UI
- File: frontend/src/features/acl/components/AclCarpetaSection.tsx
- Action: Render a badge/tooltip for the user’s effective permission origin near the section header.
- Implementation Steps:
  1. Use useMiPermisoCarpeta to fetch the effective permission.
  2. Show “Directo” vs “Heredado de [carpeta]” with a subtle badge.
  3. When inherited, show tooltip with ruta_herencia and carpeta_origen_nombre.
  4. Add loading placeholder and error fallback (e.g., “Permiso no disponible”).
- Dependencies: new hook and types
- Implementation Notes: Ensure no UI regression for admin users; keep layout compact.

### Step 5: Update ACL List to Display Inherited Entries
- File: frontend/src/features/acl/components/AclCarpetaList.tsx
- Action: Display inherited permissions distinctly and hide revoke/edit actions for inherited entries.
- Implementation Steps:
  1. Update props to accept a list item union (direct + inherited).
  2. Add a badge/label indicating “Heredado” and show origin folder name.
  3. Disable or hide edit/delete for inherited entries.
  4. Add “Ir a carpeta origen” link/button that navigates to the origin folder route.
- Dependencies: list item type and (optional) service that provides inherited entries
- Implementation Notes: Use existing design tokens and keep the table consistent. If the inherited list is not available yet, keep the UI ready but render only direct entries.

### Step 6: Enhance Recursive Checkbox UX
- File: frontend/src/features/acl/components/AclCarpetaModal.tsx
- Action: Add tooltip/help text to explain recursive behavior and ensure default value rules are explicit.
- Implementation Steps:
  1. Add tooltip icon or helper text next to the checkbox label.
  2. Ensure default value is false (or align with business rules if context should change default).
  3. Keep the checkbox value wired to payload as currently implemented.
- Dependencies: none
- Implementation Notes: Avoid introducing new UI libraries; use existing Tailwind styling.

### Step 7: Update ACL Hook to Support Effective List (if API available)
- File: frontend/src/features/acl/hooks/useAclCarpeta.ts
- Action: Optionally merge direct and inherited items for list display.
- Implementation Steps:
  1. If a list-effective endpoint exists, fetch it instead of only direct list.
  2. Maintain backward compatibility by falling back to listAcls.
  3. Keep optimistic updates only for direct entries.
- Dependencies: service method for effective list
- Implementation Notes: Avoid mutation of inherited entries; treat them read-only.

### Step 8: Update Technical Documentation
- Action: Review and update technical documentation according to changes made.
- Implementation Steps:
  1. Review code changes and list impacted docs.
  2. Update relevant docs in English:
     - If frontend behavior changes or new UI patterns are introduced, update ai-specs/specs/frontend-standards.md.
     - If API contract usage changes, ensure ai-specs/specs/api-spec.yml remains aligned.
  3. Verify structure and formatting per documentation standards.
  4. Report updated files in the PR description.
- Notes: This step is mandatory.

## 4. Implementation Order
2. Step 1: Align Types for Effective Permission and Inherited ACL
3. Step 2: Extend ACL Service with Effective Permission Endpoint
4. Step 3: Add Hook for Effective Permission
5. Step 4: Surface Effective Permission in UI
6. Step 5: Update ACL List to Display Inherited Entries
7. Step 6: Enhance Recursive Checkbox UX
8. Step 7: Update ACL Hook to Support Effective List (if API available)
9. Step 8: Update Technical Documentation

## 5. Testing Checklist
- Component rendering:
  - Effective permission badge shows correct origin (direct vs inherited).
  - ACL list shows inherited entries with disabled actions.
- Error handling:
  - mi-permiso errors show friendly message and do not block page rendering.
- Manual QA:
  - Create permission with recursivo=true and confirm “Heredado” renders in child.
  - “Ir a carpeta origen” navigates to the correct folder.

## 6. Error Handling Patterns
- Reuse extractErrorMessage in services.
- In components, show inline alerts for API failures.
- For missing inherited data, show a neutral fallback (e.g., “Sin herencia”).

## 7. UI/UX Considerations
- Use badges with subtle colors for “Heredado” vs “Directo”.
- Keep the table compact; do not overload columns.
- Provide tooltips for recursive checkbox and inheritance route.
- Ensure keyboard and screen-reader accessibility (aria-labels, role=tooltip where needed).

## 8. Dependencies
- No new external libraries required.
- Continue using existing Tailwind styles and component patterns.

## 9. Notes
- All documentation updates must be in Spanish.
- Inherited permissions are read-only; do not allow edit or delete actions.
- If the backend does not yet expose an effective list endpoint, coordinate and defer Step 5 data wiring while keeping the UI ready.

## 10. Next Steps After Implementation
- Verify integration with backend endpoints in a staging environment.
- Align any new API changes with the backend team before release.

## 11. Implementation Verification
- Code Quality: lint passes and TypeScript types are clean.
- Functionality: direct and inherited permissions display correctly.
- Testing: checklist completed, errors handled.
- Integration: API contracts aligned.
- Documentation: updates completed and verified.
