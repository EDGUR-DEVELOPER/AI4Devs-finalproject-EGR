# Frontend Implementation Plan: US-ACL-005 - Document Permission Management (ACL)

## ✅ IMPLEMENTATION STATUS: COMPLETED

**Completion Date:** 2025-01-29  
**Implementation Summary:** All 8 steps completed successfully. Document ACL UI feature is fully implemented and ready for integration.

## 1. Overview

**Feature:** Implement the user interface to assign, update, and revoke explicit permissions on specific documents, allowing access exceptions without modifying folder permissions.

**Architecture Principles:**
- **Feature-Driven Clean Architecture:** Organized by domain features with clear separation of concerns
- **Service Layer Pattern:** HTTP API communication abstracted in service layer
- **Component-Based Architecture:** Reusable, composable UI components using React functional patterns
- **State Management:** Local state with custom hooks for feature-specific logic

**Key User Story:**
As an administrator, I want to grant explicit permissions to a user on a specific document, to enable access exceptions without modifying the containing folder's permissions.

## 2. Architecture Context

### Components Involved
- **Service Layer:** `aclDocumentoService.ts` (new) - HTTP API operations
- **Custom Hook:** `useAclDocumento.ts` (new) - State management and CRUD operations
- **Modal Component:** `AclDocumentoModal.tsx` (new) - Create/update permission form
- **List Component:** `AclDocumentoList.tsx` (new) - Display and manage document permissions
- **Integration Component:** `AclDocumentoSection.tsx` (new) - Complete feature orchestration

### Files Referenced
- Existing: `src/features/acl/types/index.ts` (extend)
- Existing: `src/features/acl/components/UserSelect.tsx` (reuse)
- Existing: `src/features/acl/components/NivelAccesoSelect.tsx` (reuse)
- Existing: `src/features/acl/components/PermissionBadge.tsx` (reuse)
- Existing: `src/features/acl/index.ts` (update exports)

### Routing Considerations
- No new routes required
- Integration within document detail view or document context menu

### State Management Approach
- **Local State (useAclDocumento hook):**
  - `acls: IAclDocumento[]` - Array of document permission records
  - `loading: boolean` - Data fetching state
  - `error: string | null` - Error messages
  
- **UI State (AclDocumentoSection component):**
  - `isModalOpen: boolean` - Modal visibility toggle
  - `editingAcl: IAclDocumento | null` - Which ACL is being edited (null = create mode)
  - `deletingUserIds: number[]` - Track which ACLs are being deleted for loading UI
  - `isSubmitting: boolean` - Form submission state
  - `submitError: string | null` - Form submission errors

## 3. Implementation Steps

---

### Step 0: Create Feature Branch

**Action:** Create and switch to a new feature branch following the development workflow

**Branch Naming:** `feature/US-ACL-005-frontend` (required, separate from backend branch)

**Implementation Steps:**
1. Ensure you're on the latest `develop` branch
2. Pull latest changes: `git pull origin develop`
3. Create new branch: `git checkout -b feature/US-ACL-005-frontend`
4. Verify branch creation: `git branch`

**Notes:** This must be the FIRST step before any code changes. This ensures separation of concerns between backend and frontend implementation.

---

### Step 1: Extend Type Definitions

**File:** `src/features/acl/types/index.ts`

**Action:** Add TypeScript interfaces for document ACL operations

**Interface Signatures:**

```typescript
/**
 * Document permission (ACL) record
 * Represents an explicit permission granted to a user on a document
 */
export interface IAclDocumento {
  id: number;
  documento_id: number;
  usuario_id: number;
  usuario: IUsuarioResumen;
  nivel_acceso: INivelAcceso;
  fecha_expiracion: string | null;
  fecha_asignacion: string;
}

/**
 * DTO for creating a new document permission
 */
export interface CreateAclDocumentoDTO {
  usuario_id: number;
  nivel_acceso_codigo: CodigoNivelAcceso;
  fecha_expiracion?: string | null;
}

/**
 * DTO for updating an existing document permission
 */
export interface UpdateAclDocumentoDTO {
  nivel_acceso_codigo: CodigoNivelAcceso;
  fecha_expiracion?: string | null;
}

/**
 * API response for single document permission
 */
export interface AclDocumentoApiResponse {
  id: number;
  documento_id: number;
  usuario_id: number;
  usuario: IUsuarioResumen;
  nivel_acceso: INivelAcceso;
  fecha_expiracion: string | null;
  fecha_asignacion: string;
}

/**
 * API response for list of document permissions
 */
export interface ListAclDocumentoApiResponse {
  permisos: AclDocumentoApiResponse[];
}
```

**Implementation Steps:**
1. Open `src/features/acl/types/index.ts`
2. Add `IAclDocumento` interface with all required fields
3. Add `CreateAclDocumentoDTO` interface (similar to folder ACL but simpler - no recursive field)
4. Add `UpdateAclDocumentoDTO` interface
5. Add API response interfaces for type safety
6. Ensure `IUsuarioResumen` and `INivelAcceso` are already exported (from existing ACL types)
7. Verify `CodigoNivelAcceso` type is available

**Dependencies:**
- Existing types: `IUsuarioResumen`, `INivelAcceso`, `CodigoNivelAcceso`

**Implementation Notes:**
- Document ACL is simpler than folder ACL: no `recursivo` field
- `fecha_expiracion` is nullable for temporary access
- Follow naming conventions: snake_case for API fields, camelCase for internal use
- Reuse existing user and access level types for consistency

---

### Step 2: Create HTTP Service Layer

**File:** `src/features/acl/services/aclDocumentoService.ts` (new file)

**Action:** Implement HTTP service for document ACL API operations

**Service Signature:**

```typescript
/**
 * Document ACL API Service
 * Handles all HTTP operations for document-level user permissions
 */

export const aclDocumentoApi = {
  /**
   * List all permissions for a document
   * @param documentoId - Document ID
   * @returns Promise with array of document permissions
   */
  list: (documentoId: number): Promise<IAclDocumento[]>

  /**
   * Create or update a document permission (upsert behavior)
   * @param documentoId - Document ID
   * @param payload - Permission data
   * @returns Promise with created/updated permission
   */
  createOrUpdate: (documentoId: number, payload: CreateAclDocumentoDTO): Promise<IAclDocumento>

  /**
   * Revoke (delete) a document permission
   * @param documentoId - Document ID
   * @param usuarioId - User ID
   * @returns Promise (void on success)
   */
  revoke: (documentoId: number, usuarioId: number): Promise<void>
}
```

**Implementation Steps:**
1. Create `src/features/acl/services/aclDocumentoService.ts`
2. Import `apiClient` from `@core/shared/api/axiosInstance`
3. Import types: `IAclDocumento`, `CreateAclDocumentoDTO`, etc.
4. Define endpoint templates:
   ```typescript
   const ACL_DOCUMENTO_ENDPOINTS = {
     LIST: (documentoId: number) => `/api/documentos/${documentoId}/permisos`,
     CREATE: (documentoId: number) => `/api/documentos/${documentoId}/permisos`,
     REVOKE: (documentoId: number, usuarioId: number) => 
       `/api/documentos/${documentoId}/permisos/${usuarioId}`
   }
   ```
5. Implement `list` method:
   - GET request to list endpoint
   - Map API response to `IAclDocumento[]`
   - Error handling with user-friendly messages
6. Implement `createOrUpdate` method:
   - POST request to create endpoint
   - Backend returns 201 if created, 200 if updated
   - Map response to `IAclDocumento`
   - Handle validation errors (400), authorization errors (403), not found (404)
7. Implement `revoke` method:
   - DELETE request to revoke endpoint
   - No response body (204 No Content)
   - Handle authorization errors (403), not found (404)
8. Add error handling:
   - Use `isAxiosError` to check for Axios errors
   - Map HTTP status codes to user-friendly messages in Spanish
   - Throw errors with descriptive messages

**Dependencies:**
- `apiClient` from `@core/shared/api/axiosInstance`
- `isAxiosError` from `axios`
- Types from `../types`

**Implementation Notes:**
- Follow existing pattern from `aclCarpetaService.ts`
- API uses snake_case, convert to camelCase in responses
- Backend implements upsert behavior (POST creates or updates)
- Error messages in Spanish for end users
- Keep service layer stateless and pure

---

### Step 3: Create Custom Hook for State Management

**File:** `src/features/acl/hooks/useAclDocumento.ts` (new file)

**Action:** Implement custom React hook for document ACL state management

**Hook Signature:**

```typescript
export interface UseAclDocumentoReturn {
  // State
  acls: IAclDocumento[];
  loading: boolean;
  error: string | null;

  // Operations
  createOrUpdateAcl: (payload: CreateAclDocumentoDTO) => Promise<IAclDocumento>;
  revokeAcl: (usuarioId: number) => Promise<void>;
  refresh: () => Promise<void>;
  clearError: () => void;
}

/**
 * Custom hook to manage document ACL with CRUD operations
 * @param documentoId - Document ID to manage permissions for
 * @returns Hook interface with state and operations
 */
export const useAclDocumento = (documentoId: number): UseAclDocumentoReturn
```

**Implementation Steps:**
1. Create `src/features/acl/hooks/useAclDocumento.ts`
2. Import React hooks: `useState`, `useCallback`, `useEffect`
3. Import service: `aclDocumentoApi`
4. Import types: `IAclDocumento`, `CreateAclDocumentoDTO`
5. Define internal state interface:
   ```typescript
   interface AclState {
     acls: IAclDocumento[];
     loading: boolean;
     error: string | null;
   }
   ```
6. Initialize state with `useState<AclState>`
7. Implement `loadAcls` function (internal):
   - Set loading to true
   - Call `aclDocumentoApi.list(documentoId)`
   - Update state with ACLs on success
   - Handle errors and set error message
   - Set loading to false
8. Implement `createOrUpdateAcl` with `useCallback`:
   - Validate documentoId
   - Call `aclDocumentoApi.createOrUpdate(documentoId, payload)`
   - On success: update local state (add or update ACL in array)
   - Clear error on success
   - Return created/updated ACL
   - Handle errors and rethrow with message
9. Implement `revokeAcl` with `useCallback`:
   - Call `aclDocumentoApi.revoke(documentoId, usuarioId)`
   - On success: remove ACL from local state
   - Clear error
   - Handle errors and rethrow
10. Implement `refresh` with `useCallback`:
    - Call `loadAcls()` to reload from server
11. Implement `clearError` with `useCallback`:
    - Set error to null
12. Add `useEffect` to load ACLs on mount:
    - Dependency: `[documentoId]`
    - Call `loadAcls()`
13. Return hook interface with all methods and state

**Dependencies:**
- React hooks: `useState`, `useCallback`, `useEffect`
- Service: `aclDocumentoApi`
- Types: `IAclDocumento`, `CreateAclDocumentoDTO`

**Implementation Notes:**
- Follow existing pattern from `useAclCarpeta.ts`
- Optimistic updates: update UI immediately, revert on error
- Use `useCallback` to memoize functions and prevent unnecessary re-renders
- Auto-load ACLs when documentoId changes
- Comprehensive error handling with Spanish messages
- Clear separation between loading state and error state

---

### Step 4: Create Modal Component

**File:** `src/features/acl/components/AclDocumentoModal.tsx` (new file)

**Action:** Implement modal dialog for creating and updating document permissions

**Component Signature:**

```typescript
export interface AclDocumentoModalProps {
  /** Whether modal is open */
  isOpen: boolean;

  /** Close modal callback */
  onClose: () => void;

  /** Submit callback for new ACL */
  onSubmit: (payload: CreateAclDocumentoDTO) => Promise<void>;

  /** List of available users */
  users: IUsuario[];

  /** User IDs already assigned to this document */
  assignedUserIds: number[];

  /** ACL being edited (null = create mode) */
  editingAcl: IAclDocumento | null;

  /** Whether submit is in progress */
  isSubmitting?: boolean;

  /** Submit error message */
  submitError?: string | null;

  /** Modal title */
  title?: string;

  /** Custom className */
  className?: string;
}

export const AclDocumentoModal: React.FC<AclDocumentoModalProps>
```

**Implementation Steps:**
1. Create `src/features/acl/components/AclDocumentoModal.tsx`
2. Import React hooks: `useState`, `useEffect`
3. Import reusable components: `UserSelect`, `NivelAccesoSelect`
4. Import types: `IUsuario`, `IAclDocumento`, `CreateAclDocumentoDTO`, `CodigoNivelAcceso`
5. Define internal form state:
   ```typescript
   interface FormState {
     usuarioId: number | null;
     nivelAccesoCodigo: string;
     fechaExpiracion: string;
   }
   ```
6. Implement modal structure:
   - Backdrop (onClick closes modal)
   - Centered dialog container
   - Header with title and close button
   - Form body with fields
   - Footer with cancel and submit buttons
7. Implement form fields:
   - `UserSelect` component (disabled in edit mode)
   - `NivelAccesoSelect` component for access level
   - Date input for `fechaExpiracion` (optional)
   - Note: No recursive field (unlike folder ACL)
8. Add form validation:
   - Required: `usuarioId`, `nivelAccesoCodigo`
   - Optional: `fechaExpiracion` (for temporary access)
   - Disable submit button if invalid
9. Implement form submission:
   - Build payload: `CreateAclDocumentoDTO`
   - Call `onSubmit(payload)`
   - Close modal on success
   - Show error if fails
10. Add keyboard handling:
    - Esc key closes modal
    - Enter key submits form (if valid)
11. Add loading state:
    - Disable form during submission
    - Show spinner on submit button
12. Initialize form when modal opens:
    - Create mode: empty form
    - Edit mode: pre-fill from `editingAcl`
13. Reset form when modal closes

**Dependencies:**
- React hooks: `useState`, `useEffect`
- Components: `UserSelect`, `NivelAccesoSelect` (reuse from folder ACL)
- Types: `IUsuario`, `IAclDocumento`, `CreateAclDocumentoDTO`

**Implementation Notes:**
- Follow existing pattern from `AclCarpetaModal.tsx`
- Simpler than folder modal: no recursive field
- Tailwind CSS for styling
- Accessible: proper ARIA attributes, keyboard navigation
- Form validation with user-friendly error messages in Spanish
- Disabled user select in edit mode (can't change user, only access level)
- Optional expiration date for temporary access

---

### Step 5: Create List Component

**File:** `src/features/acl/components/AclDocumentoList.tsx` (new file)

**Action:** Implement table component to display and manage document permissions

**Component Signature:**

```typescript
export interface AclDocumentoListProps {
  /** List of document permissions */
  acls: IAclDocumento[];

  /** Loading state */
  loading?: boolean;

  /** Error message */
  error?: string | null;

  /** Edit callback */
  onEdit: (acl: IAclDocumento) => void;

  /** Delete callback */
  onDelete: (acl: IAclDocumento) => Promise<void>;

  /** User IDs being deleted (for loading state) */
  deletingUserIds?: number[];

  /** Empty message */
  emptyMessage?: string;

  /** Whether to show header */
  showHeader?: boolean;

  /** Whether user can manage (edit/delete) */
  canManage?: boolean;

  /** Custom className */
  className?: string;
}

export const AclDocumentoList: React.FC<AclDocumentoListProps>
```

**Implementation Steps:**
1. Create `src/features/acl/components/AclDocumentoList.tsx`
2. Import React: `useState`
3. Import components: `PermissionBadge` (reuse from folder ACL)
4. Import types: `IAclDocumento`
5. Implement table structure:
   - Table with responsive design (Tailwind)
   - Columns: User, Access Level, Expiration, Assigned Date, Actions
   - Fixed header (optional via prop)
6. Implement loading state:
   - Show skeleton/spinner when loading
   - Disable actions during loading
7. Implement error state:
   - Show error message if error prop provided
   - Red border or alert style
8. Implement empty state:
   - Show custom message when no ACLs
   - Default: "No hay permisos explícitos asignados"
9. Implement row rendering:
   - User name and email
   - Access level badge (using `PermissionBadge`)
   - Expiration date (formatted, show "Sin expiración" if null)
   - Assigned date (formatted)
   - Action buttons (edit, delete)
10. Implement action buttons:
    - Edit button: calls `onEdit(acl)` (only if canManage)
    - Delete button: shows confirm dialog, calls `onDelete(acl)` (only if canManage)
    - Loading spinner on delete button if user is in `deletingUserIds`
11. Add confirmation dialog for delete:
    - Simple browser confirm or custom modal
    - Message: "¿Está seguro de revocar el permiso de {usuario}?"
12. Format dates:
    - Use `new Date(dateString).toLocaleDateString('es-ES')`
    - Handle null dates gracefully

**Dependencies:**
- React: `useState` (for confirm dialog state if custom)
- Components: `PermissionBadge` (reuse)
- Types: `IAclDocumento`

**Implementation Notes:**
- Follow existing pattern from `AclCarpetaList.tsx`
- Simpler than folder list: no recursive indicator
- Responsive table design with Tailwind
- Action buttons only visible if `canManage` is true
- Loading state per row (via `deletingUserIds`)
- Spanish messages for all UI text
- Accessible: proper table semantics, button labels

---

### Step 6: Create Integration Component

**File:** `src/features/acl/components/AclDocumentoSection.tsx` (new file)

**Action:** Implement complete feature orchestration component

**Component Signature:**

```typescript
export interface AclDocumentoSectionProps {
  /** Document ID for which to manage ACLs */
  documentoId: number;

  /** List of available users to assign permissions to */
  users: IUsuario[];

  /** Whether current user is admin (controls visibility of controls) */
  isAdmin?: boolean;

  /** Error message to display (e.g., if users failed to load) */
  loadingError?: string | null;

  /** Callback fired when new ACL is created successfully */
  onAclCreated?: (acl: IAclDocumento) => void;

  /** Callback fired when ACL is updated successfully */
  onAclUpdated?: (acl: IAclDocumento) => void;

  /** Callback fired when ACL is deleted successfully */
  onAclDeleted?: (usuarioId: number) => void;

  /** Custom className */
  className?: string;
}

export const AclDocumentoSection: React.FC<AclDocumentoSectionProps>
```

**Implementation Steps:**
1. Create `src/features/acl/components/AclDocumentoSection.tsx`
2. Import React: `useState`, `useMemo`
3. Import components: `AclDocumentoModal`, `AclDocumentoList`
4. Import hook: `useAclDocumento`
5. Import types: `IUsuario`, `IAclDocumento`, `CreateAclDocumentoDTO`
6. Use `useAclDocumento` hook:
   - Pass `documentoId` to hook
   - Destructure: `acls`, `loading`, `error`, `createOrUpdateAcl`, `revokeAcl`
7. Add local UI state:
   - `isModalOpen: boolean` - modal visibility
   - `editingAcl: IAclDocumento | null` - which ACL is being edited
   - `deletingUserIds: number[]` - track deleting ACLs
   - `isSubmitting: boolean` - form submission state
   - `submitError: string | null` - form errors
8. Implement section header:
   - Title: "Permisos del Documento"
   - "Asignar Permiso" button (only if isAdmin)
9. Implement "Asignar Permiso" button click:
   - Set `editingAcl` to null (create mode)
   - Clear `submitError`
   - Open modal
10. Implement edit handler:
    - Set `editingAcl` to selected ACL
    - Clear `submitError`
    - Open modal
11. Implement delete handler:
    - Add userId to `deletingUserIds`
    - Call `revokeAcl(usuarioId)`
    - On success: remove from `deletingUserIds`, call `onAclDeleted` callback
    - On error: remove from `deletingUserIds`, show error
12. Implement modal submit handler:
    - Set `isSubmitting` to true
    - Call `createOrUpdateAcl(payload)`
    - On success: close modal, call `onAclCreated` or `onAclUpdated` callback
    - On error: set `submitError`, keep modal open
    - Set `isSubmitting` to false
13. Render `AclDocumentoList`:
    - Pass `acls`, `loading`, `error` from hook
    - Pass `onEdit`, `onDelete` handlers
    - Pass `deletingUserIds` for loading state
    - Pass `canManage={isAdmin}`
14. Render `AclDocumentoModal` (if isAdmin):
    - Control open state with `isModalOpen`
    - Pass `users`, `editingAcl`, `isSubmitting`, `submitError`
    - Handle close: reset all modal state
15. Compute `assignedUserIds`:
    - Use `useMemo` to extract user IDs from `acls`
    - Pass to modal to exclude already assigned users (in create mode)
16. Show combined error:
    - Combine `loadingError` prop and hook `error`
    - Display above list

**Dependencies:**
- React hooks: `useState`, `useMemo`
- Custom hook: `useAclDocumento`
- Components: `AclDocumentoModal`, `AclDocumentoList`
- Types: `IUsuario`, `IAclDocumento`, `CreateAclDocumentoDTO`

**Implementation Notes:**
- Follow existing pattern from `AclCarpetaSection.tsx`
- Complete feature orchestration: handles all CRUD flows
- Admin-only controls: hide modal and create button if not admin
- Callbacks allow parent component to react to changes
- Auto-loads ACLs on mount via hook
- Comprehensive error handling
- Spanish messages for all UI text

---

### Step 7: Update Feature Exports

**File:** `src/features/acl/index.ts`

**Action:** Export new document ACL components, hooks, and services

**Implementation Steps:**
1. Open `src/features/acl/index.ts`
2. Add type exports from updated types file:
   ```typescript
   export type { IAclDocumento, CreateAclDocumentoDTO, UpdateAclDocumentoDTO } from './types';
   ```
3. Add service export:
   ```typescript
   export { aclDocumentoApi } from './services/aclDocumentoService';
   ```
4. Add hook export:
   ```typescript
   export { useAclDocumento } from './hooks/useAclDocumento';
   export type { UseAclDocumentoReturn } from './hooks/useAclDocumento';
   ```
5. Add component exports:
   ```typescript
   export { AclDocumentoModal } from './components/AclDocumentoModal';
   export type { AclDocumentoModalProps } from './components/AclDocumentoModal';
   
   export { AclDocumentoList } from './components/AclDocumentoList';
   export type { AclDocumentoListProps } from './components/AclDocumentoList';
   
   export { AclDocumentoSection } from './components/AclDocumentoSection';
   export type { AclDocumentoSectionProps } from './components/AclDocumentoSection';
   ```
6. Verify existing exports remain intact (folder ACL components)

**Dependencies:**
- None (just exports)

**Implementation Notes:**
- Maintain existing exports for folder ACL
- Follow existing barrel export pattern
- Export both components and their prop types for external use
- Organize exports by category (types, services, hooks, components)

---

### Step 8: Update Technical Documentation

**Action:** Review and update technical documentation according to changes made

**Implementation Steps:**

1. **Review Changes:**
   - New service: `aclDocumentoService.ts`
   - New hook: `useAclDocumento.ts`
   - New components: `AclDocumentoModal`, `AclDocumentoList`, `AclDocumentoSection`
   - Extended types in `types/index.ts`
   - Updated exports in `index.ts`

2. **Identify Documentation Files:**
   - `ai-specs/specs/api-spec.yml` - Already updated in backend step
   - `ai-specs/specs/frontend-standards.md` - May need update if new patterns introduced

3. **Update Documentation:**
   - **Frontend Standards (`ai-specs/specs/frontend-standards.md`):**
     - If any new patterns were introduced (e.g., date handling, expiration fields), document them
     - Update feature structure example to include document ACL
     - Add any new reusable components to the component library section
   
   - **API Spec (`ai-specs/specs/api-spec.yml`):**
     - Verify backend team already updated this file
     - If not, coordinate with backend to add document endpoints
   
   - **Feature Documentation (if applicable):**
     - Consider creating a brief README in `src/features/acl/` explaining the dual ACL system (folder + document)

4. **Verify Documentation:**
   - All new components documented with JSDoc comments (already done in code)
   - API endpoints match backend implementation
   - Type definitions are clear and consistent

5. **Report Updates:**
   - Document in PR description which files were updated
   - List any new patterns or conventions introduced

**References:**
- Follow process described in `ai-specs/specs/documentation-standards.mdc`
- All documentation must be written in English

**Notes:**
- Most documentation is inline in code (JSDoc comments)
- Main focus is ensuring consistency with existing patterns
- Coordinate with backend team on API documentation

---

## 4. Implementation Order

**Step 1:** Extend Type Definitions
**Step 2:** Create HTTP Service Layer
**Step 3:** Create Custom Hook for State Management
**Step 4:** Create Modal Component
**Step 5:** Create List Component
**Step 6:** Create Integration Component
**Step 7:** Update Feature Exports
**Step 8:** Update Technical Documentation

## 5. Testing Checklist

### Component Functionality
- [ ] **AclDocumentoModal:**
  - [ ] Opens and closes correctly
  - [ ] Form validation works (required fields)
  - [ ] Create mode: empty form, all fields enabled
  - [ ] Edit mode: form pre-filled, user field disabled
  - [ ] Submit button shows loading spinner during submission
  - [ ] Error message displayed when submission fails
  - [ ] Esc key closes modal
  - [ ] Backdrop click closes modal
  - [ ] Date picker works for expiration date
  - [ ] Optional expiration date can be left empty

- [ ] **AclDocumentoList:**
  - [ ] Displays list of permissions correctly
  - [ ] Shows user name and email
  - [ ] Access level badge displays correctly
  - [ ] Expiration date formatted correctly (or "Sin expiración")
  - [ ] Assigned date formatted correctly
  - [ ] Edit button calls onEdit handler
  - [ ] Delete button shows confirmation dialog
  - [ ] Delete button shows spinner during deletion
  - [ ] Empty state shows appropriate message
  - [ ] Loading state displays correctly
  - [ ] Error state displays correctly
  - [ ] Action buttons hidden when canManage=false

- [ ] **AclDocumentoSection:**
  - [ ] Auto-loads permissions on mount
  - [ ] "Asignar Permiso" button opens modal in create mode
  - [ ] Edit action opens modal in edit mode
  - [ ] Delete action calls hook and updates UI
  - [ ] Modal submit creates or updates permission
  - [ ] Callbacks (onAclCreated, onAclUpdated, onAclDeleted) are called
  - [ ] Admin-only controls hidden when isAdmin=false
  - [ ] Error messages displayed correctly
  - [ ] Loading states work correctly

### Hook Functionality
- [ ] **useAclDocumento:**
  - [ ] Loads ACLs on mount
  - [ ] createOrUpdateAcl creates new permission
  - [ ] createOrUpdateAcl updates existing permission (upsert)
  - [ ] revokeAcl deletes permission
  - [ ] refresh reloads data from server
  - [ ] clearError clears error state
  - [ ] Error handling works correctly
  - [ ] Optimistic updates work (UI updates immediately)

### Service Functionality
- [ ] **aclDocumentoService:**
  - [ ] list method fetches permissions
  - [ ] createOrUpdate method posts data correctly
  - [ ] revoke method deletes permission
  - [ ] Error handling maps status codes to messages
  - [ ] Network errors handled gracefully
  - [ ] Authorization errors (403) handled
  - [ ] Not found errors (404) handled

### Integration Testing
- [ ] Can create permission for a user on a document
- [ ] Can update existing permission (change access level)
- [ ] Can revoke permission
- [ ] Can set expiration date for temporary access
- [ ] Cannot assign duplicate permission (UI prevents it)
- [ ] Admin sees all controls, non-admin sees only list
- [ ] Multi-tenant isolation works (organization filtering)
- [ ] UI updates immediately after operations (optimistic updates)

## 6. Error Handling Patterns

### Service Layer (`aclDocumentoService.ts`)
```typescript
// Example error handling
try {
  const response = await apiClient.get(endpoint);
  return response.data.permisos;
} catch (err) {
  if (isAxiosError(err)) {
    if (err.response?.status === 403) {
      throw new Error('No tiene permisos para ver los permisos de este documento');
    }
    if (err.response?.status === 404) {
      throw new Error('Documento no encontrado');
    }
  }
  throw new Error('Error al cargar permisos del documento');
}
```

### Hook Layer (`useAclDocumento.ts`)
- Catch errors from service layer
- Set error state for UI display
- Provide clear Spanish error messages
- Allow user to retry operations

### Component Layer
- Display errors from hook or service
- Provide user-friendly error messages
- Allow dismissing errors
- Show inline validation errors in forms

## 7. UI/UX Considerations

### Tailwind CSS Usage
- Follow existing design system from folder ACL components
- Reuse utility classes for consistency
- Responsive design: mobile-first approach

### Form Handling
- Clear validation messages
- Disable submit when form is invalid
- Loading states on submit buttons
- Reset form on close/success

### Accessibility
- Proper ARIA labels on all interactive elements
- Keyboard navigation (Tab, Esc, Enter)
- Focus management in modals
- Screen reader friendly

### Loading States
- Skeleton loaders for list loading
- Spinner on submit buttons
- Disable actions during loading
- Loading indicator per row when deleting

### User Feedback
- Success messages (optional, can use callbacks)
- Clear error messages in Spanish
- Confirmation dialogs for destructive actions
- Immediate UI updates (optimistic)

## 8. Notes

### Important Reminders
- **Language:** All UI text in Spanish
- **Separation:** Keep backend and frontend branches separate
- **Consistency:** Follow existing patterns from folder ACL
- **TypeScript:** Use explicit types, avoid `any`
- **Testing:** Manually test all CRUD operations

### Business Rules
- Only admins or users with ADMINISTRACION on parent folder can manage document permissions
- Multi-tenant isolation: users only see their organization's data
- Upsert behavior: one permission per (document, user) pair
- Document permissions don't affect folder permissions (separate systems)
- Expiration date optional for temporary access

### Differences from Folder ACL
- **Simpler:** No recursive field in document ACL
- **Temporary Access:** Supports expiration date
- **Upsert:** Backend automatically creates or updates (single endpoint)
- **Lighter:** Fewer fields, simpler logic

### Code Quality
- Follow ESLint rules (run `npm run lint` before commit)
- Use TypeScript strict mode
- Add JSDoc comments to all exported functions
- Keep components small and focused
- Extract complex logic to custom hooks

## 10. Next Steps After Implementation

1. **Manual Testing:**
   - Test all CRUD operations in browser
   - Verify admin/non-admin behavior
   - Test error scenarios (network errors, validation)
   - Test responsive design on mobile

2. **Integration:**
   - Integrate `AclDocumentoSection` into document detail view
   - Add to document context menu if applicable
   - Coordinate with backend team on deployment

3. **Code Review:**
   - Create pull request with descriptive summary
   - Request review from frontend lead
   - Address feedback and iterate

4. **Deployment:**
   - Merge to develop branch
   - Deploy to staging environment
   - Verify functionality in staging
   - Deploy to production

## 11. Implementation Verification

### Code Quality
- [ ] No ESLint errors or warnings
- [ ] TypeScript compiles without errors
- [ ] All functions have JSDoc comments
- [ ] Component props have TypeScript interfaces
- [ ] No `any` types used

### Functionality
- [ ] All CRUD operations work correctly
- [ ] Admin controls visible only to admins
- [ ] Multi-tenant isolation verified
- [ ] Error handling works correctly
- [ ] Loading states display correctly

### Testing
- [ ] Manual testing completed for all scenarios
- [ ] Edge cases tested (network errors, validation)
- [ ] Responsive design verified on mobile
- [ ] Accessibility verified (keyboard navigation)

### Integration
- [ ] Components integrate with existing UI
- [ ] API calls work with backend endpoints
- [ ] State management works correctly
- [ ] Callbacks fire correctly

### Documentation
- [ ] JSDoc comments on all exports
- [ ] README updated if necessary
- [ ] Technical documentation updated
- [ ] PR description includes summary of changes

---

## IMPLEMENTATION SUMMARY

### Files Created (7 new files)

1. **src/features/acl/services/aclDocumentoService.ts** (144 lines)
   - HTTP service with 3 API methods: list, createOrUpdate (upsert), revoke
   - Error handling with Spanish user-friendly messages
   - Follows existing `aclCarpetaService.ts` pattern

2. **src/features/acl/hooks/useAclDocumento.ts** (207 lines)
   - Custom React hook for document ACL state management
   - Auto-loads ACLs on mount
   - Provides CRUD operations with optimistic updates
   - Returns: `acls`, `loading`, `error`, `createOrUpdateAcl`, `revokeAcl`, `refresh`, `clearError`

3. **src/features/acl/components/AclDocumentoModal.tsx** (370 lines)
   - Modal form for creating/editing document permissions
   - Includes: user select (disabled in edit mode), access level select, date picker for expiration
   - Validation: required fields, expiration date must be today or future
   - Supports both create and edit modes (upsert behavior)

4. **src/features/acl/components/AclDocumentoList.tsx** (513 lines)
   - Table component displaying document ACLs
   - Features: user avatar, permission badge, expiration date with status indicators (expired/expiring soon)
   - Actions: edit, revoke (with confirmation popup)
   - Loading, error, and empty states

5. **src/features/acl/components/AclDocumentoSection.tsx** (237 lines)
   - Integration component orchestrating modal + list + hook
   - Admin-only "Otorgar Permiso" button (controlled by `canManage` prop)
   - Handles all CRUD callbacks and error states
   - Provides callbacks: `onAclCreated`, `onAclUpdated`, `onAclRevoked`

### Files Extended (2 files)

6. **src/features/acl/types/index.ts** (+93 lines)
   - Added `IAclDocumento` interface (8 fields)
   - Added `CreateAclDocumentoDTO` (3 fields)
   - Added `UpdateAclDocumentoDTO` (2 fields)
   - Added `AclDocumentoApiResponse` and `ListAclDocumentoApiResponse`

7. **src/features/acl/index.ts** (+12 lines)
   - Exported document ACL types (5 new exports)
   - Exported `useAclDocumento` hook + return type (2 new exports)
   - Exported 3 components: `AclDocumentoModal`, `AclDocumentoList`, `AclDocumentoSection` (6 new exports)
   - Exported `aclDocumentoApi` service (1 new export)

### Technical Highlights

- **Upsert Behavior:** Backend POST endpoint returns 201 (created) or 200 (updated). Frontend uses same method for both.
- **Expiration Dates:** Optional field for temporary access. UI shows visual indicators (expired/expiring soon badges).
- **Simpler than Folder ACL:** No recursive field, no inherited permissions, no origin folder navigation.
- **Reused Components:** `UserSelect`, `NivelAccesoSelect`, `PermissionBadge` shared with folder ACL.
- **Spanish UI:** All user-facing text in Spanish (labels, errors, confirmations).
- **Multi-tenant Isolation:** Handled automatically by backend via `organizacionId`.

### Integration Points

To integrate this feature into a document detail view:

```tsx
import { AclDocumentoSection } from '@/features/acl';

<AclDocumentoSection
  documentoId={documentId}
  users={users}
  canManage={isAdminOrHasAdminAccess}
  onAclCreated={(acl) => console.log('Created:', acl)}
  onAclUpdated={(acl) => console.log('Updated:', acl)}
  onAclRevoked={(userId) => console.log('Revoked:', userId)}
/>
```

### Next Steps for Developers

1. **Integration:** Add `<AclDocumentoSection>` to document detail page or modal
2. **User Loading:** Fetch users from API and pass to component
3. **Permission Check:** Determine `canManage` prop based on user role (admin) or parent folder permission (ADMINISTRACION)
4. **Testing:** Manual UI testing for all CRUD operations, error scenarios, expiration date edge cases
5. **Edge Cases:** Test expired permissions, permissions expiring in <7 days, permanent permissions

### Architecture Compliance

✅ **Feature-Driven Clean Architecture:** All files organized under `features/acl`  
✅ **Service Layer Pattern:** HTTP abstraction in `aclDocumentoService.ts`  
✅ **Component-Based Architecture:** Reusable atomic components (modal, list, section)  
✅ **State Management:** Local state with custom hook `useAclDocumento`  
✅ **TypeScript Strict Mode:** All types explicitly defined  
✅ **Spanish Localization:** User-facing text in Spanish  
✅ **Error Handling:** Comprehensive HTTP error mapping  
✅ **Accessibility:** ARIA labels, keyboard navigation (ESC to close modal)

---

**Related Tickets:**
- US-ACL-005 (Backend) - Backend implementation
- US-ACL-002 - Folder ACL implementation (reference pattern)
- US-ACL-006 - Permission precedence logic (future ticket)

