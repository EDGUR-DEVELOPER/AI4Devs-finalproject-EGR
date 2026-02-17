# Frontend Implementation Plan: US-ACL-001 Access Levels Catalog

## Overview

This document provides a comprehensive step-by-step implementation plan for the Access Levels Catalog (Catálogo de Niveles de Acceso) feature on the frontend. The feature implements a React component-based architecture to display, manage, and interact with the ACL (Access Control List) levels from the backend. The implementation leverages TypeScript for type safety, React hooks for state management, and Tailwind CSS for styling, following the Feature-Driven Clean Architecture pattern established in the project.

**Key Frontend Objectives:**
- Create types and constants for ACL codes (LECTURA, ESCRITURA, ADMINISTRACION)
- Implement HTTP service for consuming ACL endpoints
- Build reusable React hook (`useNivelesAcceso`) with caching capabilities
- Create a reusable dropdown component (`NivelAccesoSelect`) for selecting access levels
- Ensure type safety and maintainability throughout

---

## Architecture Context

### Components and Services Involved

**Feature Structure:**
```
frontend/src/features/acl/
├── services/
│   └── nivelAccesoService.ts          # HTTP service for ACL API calls
├── hooks/
│   └── useNivelesAcceso.ts            # Custom React hook with caching
├── types/
│   └── index.ts                       # TypeScript interfaces (INivelAcceso, etc.)
├── components/
    └── NivelAccesoSelect.tsx          # Reusable dropdown component

```

**Shared Resources:**
- `frontend/src/common/constants/permissions.ts` → Permission codes and action constants
- `frontend/src/core/shared/api/` → Axios instance and interceptors for API calls

### State Management Approach

- **Local Component State:** Form inputs and UI interaction state (loading, error messages)
- **Hook-Level Caching:** `useNivelesAcceso` caches data in memory with 24-hour TTL
- **Optional Global Cache:** LocalStorage fallback for persistent caching across sessions
- **Error States:** Graceful error handling with user-friendly messages in Spanish

### Routing Considerations

No new routes required for this ticket. The `NivelAccesoSelect` component is a reusable dropdown for other features (e.g., ACL assignment modals in document/folder management). It can be imported and used anywhere needed.

### Type Safety

- Full TypeScript implementation (`.ts` and `.tsx` files)
- No `any` types allowed; explicit interfaces for all data structures
- Extension of response envelope types from backend (if exists in project)

---

## Implementation Steps

### **Step 1: Create TypeScript Types and Constants**

**File**: `frontend/src/features/acl/types/index.ts`

**Action**: Define TypeScript interfaces for the ACL data structures based on backend API contracts.

**Function/Interface Signature**:
```typescript
interface INivelAcceso {
  id: string;
  codigo: CodigoNivelAcceso;
  nombre: string;
  descripcion?: string;
  accionesPermitidas: string[];
  orden: number;
  activo: boolean;
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

type CodigoNivelAcceso = 'LECTURA' | 'ESCRITURA' | 'ADMINISTRACION';

interface ApiResponse<T> {
  data: T;
  meta: {
    total?: number;
    timestamp?: string;
  };
}
```

**Implementation Steps**:

1. Create `frontend/src/features/acl/` directory structure:
   ```bash
   mkdir -p frontend/src/features/acl/{services,hooks,types,components,__tests__}
   ```

2. Create `frontend/src/features/acl/types/index.ts` with:
   - Interface `INivelAcceso` matching backend DTO structure
   - Type alias `CodigoNivelAcceso` for access level codes
   - Interface `ApiResponse<T>` for consistent API envelope handling
   - Enum or union type for valid actions (ver, listar, descargar, etc.)

3. Export all types from a barrel export (`index.ts`)

**Dependencies**:
- No external dependencies (TypeScript only)

**Implementation Notes**:
- Align field names with backend API (snake_case in API, camelCase in TS after conversion)
- Use union type for `CodigoNivelAcceso` for strict type checking
- Include optional fields that may not always be present in responses
- Document interfaces with JSDoc comments for IDE autocomplete

---

### **Step 2: Create Permissions Constants**

**File**: `frontend/src/common/constants/permissions.ts`

**Action**: Define constants for access level codes and available actions.

**Constant Definitions**:
```typescript
export const PERMISSION_CODES = {
  LECTURA: 'LECTURA',
  ESCRITURA: 'ESCRITURA',
  ADMINISTRACION: 'ADMINISTRACION',
} as const;

export const PERMISSION_ACTIONS = {
  VER: 'ver',
  LISTAR: 'listar',
  DESCARGAR: 'descargar',
  SUBIR: 'subir',
  MODIFICAR: 'modificar',
  CREAR_VERSION: 'crear_version',
  ELIMINAR: 'eliminar',
  ADMINISTRAR_PERMISOS: 'administrar_permisos',
  CAMBIAR_VERSION_ACTUAL: 'cambiar_version_actual',
} as const;

export const PERMISSION_LABELS: Record<string, string> = {
  [PERMISSION_CODES.LECTURA]: 'Lectura / Consulta',
  [PERMISSION_CODES.ESCRITURA]: 'Escritura / Modificación',
  [PERMISSION_CODES.ADMINISTRACION]: 'Administración / Control Total',
};
```

**Implementation Steps**:

1. Create or update `frontend/src/common/constants/permissions.ts`

2. Define `PERMISSION_CODES` object with all three access levels as constants

3. Define `PERMISSION_ACTIONS` object with all 9 available actions

4. Define `PERMISSION_LABELS` mapping for UI display

5. Export all constants for use throughout the application

**Dependencies**:
- No external dependencies (constants only)

**Implementation Notes**:
- Use `as const` assertion for strict type inference
- Labels should match backend descriptions
- Easy to extend for new actions without code refactoring
- Import these constants in services and components

---

### **Step 3: Create HTTP Service for ACL API**

**File**: `frontend/src/features/acl/services/nivelAccesoService.ts`

**Action**: Implement Axios-based service to communicate with backend ACL endpoints.

**Function Signatures**:
```typescript
export const getNivelesAcceso = async (): Promise<INivelAcceso[]>

export const getNivelAccesoByCodigo = async (codigo: CodigoNivelAcceso): Promise<INivelAcceso>
```

**Implementation Steps**:

1. Create `frontend/src/features/acl/services/nivelAccesoService.ts`

2. Import necessary dependencies:
   - `axios` for HTTP requests (use project's axios instance)
   - Types from `../types`

3. Implement `getNivelesAcceso()`:
   - Call `GET /api/acl/niveles`
   - Extract `data` array from API response
   - Return array of `INivelAcceso` objects
   - Handle errors with descriptive messages

4. Implement `getNivelAccesoByCodigo(codigo)`:
   - Call `GET /api/acl/niveles/${codigo}`
   - Extract single object from API response
   - Return single `INivelAcceso` object
   - Throw error if not found (404)

5. Add JSDoc comments for each function:
   - Purpose, parameters, return type, potential errors

**Dependencies**:
```typescript
import axios from 'axios'; // Or use project's configured axios instance
import { INivelAcceso, CodigoNivelAcceso, ApiResponse } from '../types';
```

**Implementation Notes**:
- Use async/await pattern (not .then() chains)
- Extract `data` from API response envelope
- Add try/catch error handling to be delegated to hook
- Include BASE_URL from environment or constants
- Add timeout configuration for requests
- Consider adding request/response logging for debugging

---

### **Step 4: Create Custom React Hook with Caching**

**File**: `frontend/src/features/acl/hooks/useNivelesAcceso.ts`

**Action**: Implement a custom React hook that fetches access levels, handles loading/error states, and caches data.

**Function Signature**:
```typescript
interface UseNivelesAccesoReturn {
  niveles: INivelAcceso[];
  loading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

export const useNivelesAcceso = (
  enableCache: boolean = true,
  cacheTTL: number = 24 * 60 * 60 * 1000 // 24 hours
): UseNivelesAccesoReturn
```

**Implementation Steps**:

1. Create `frontend/src/features/acl/hooks/useNivelesAcceso.ts`

2. Implement custom hook with:
   - `useState` for `niveles` (empty array), `loading` (true), `error` (null)
   - `useEffect` for side effects
   - `useCallback` for `refetch` function

3. Implement caching logic:
   - Check localStorage (if `enableCache=true`) on mount
   - Use cache if valid (not expired beyond `cacheTTL`)
   - Otherwise fetch from API
   - Store fetched data in localStorage with timestamp

4. Implement error handling:
   - Catch API errors and set error state
   - User-friendly Spanish error messages
   - Avoid console.error spam; use logging service if available

5. Implement `refetch` callback:
   - Clear cache and refetch data
   - Useful for manual cache invalidation

6. Return object with `{ niveles, loading, error, refetch }`

**Dependencies**:
```typescript
import { useState, useEffect, useCallback } from 'react';
import { getNivelesAcceso } from '../services/nivelAccesoService';
import { INivelAcceso } from '../types';
```

**Implementation Notes**:
- Use `useCallback` to memoize `refetch` to prevent infinite loops
- Cleanup function in `useEffect` to prevent memory leaks
- Cache key could be `'ACL_NIVELES_CACHE'`
- Store both data and timestamp in cache
- Handle edge cases: no cache, expired cache, network errors
- Avoid fetching on every render; use dependency array wisely

---

### **Step 5: Create Reusable NivelAccesoSelect Component**

**File**: `frontend/src/features/acl/components/NivelAccesoSelect.tsx`

**Action**: Build a reusable dropdown component for selecting access levels with loading and error states.

**Component Signature**:
```typescript
interface NivelAccesoSelectProps {
  value?: CodigoNivelAcceso | null;
  onChange: (codigo: CodigoNivelAcceso) => void;
  disabled?: boolean;
  required?: boolean;
  label?: string;
  error?: string;
  size?: 'sm' | 'md' | 'lg';
}

export const NivelAccesoSelect: React.FC<NivelAccesoSelectProps>
```

**Implementation Steps**:

1. Create `frontend/src/features/acl/components/NivelAccesoSelect.tsx`

2. Define props interface:
   - `value`: selected access level code
   - `onChange`: callback for selection change
   - `disabled`: disable dropdown (loading, etc.)
   - `required`: mark field as required
   - `label`: custom label text (default: "Nivel de Acceso")
   - `error`: error message to display
   - `size`: Tailwind size variant

3. Use `useNivelesAcceso()` hook to fetch data:
   - Display loading spinner while fetching
   - Display error message if fetch fails with retry button
   - Disable dropdown while loading

4. Render dropdown with:
   - Label element (if provided)
   - Select HTML element with Tailwind classes
   - Option elements for each nivel
   - Display `nombre` to user, use `codigo` as value
   - Ordered by `orden` field
   - Error message below select (if error prop provided)

5. Handle selection change:
   - Validate selected value is valid `CodigoNivelAcceso`
   - Call `onChange` callback with selected code
   - Clear error message on selection

6. Add accessibility:
   - Proper `htmlFor` binding on label
   - `aria-label` on select
   - `aria-describedby` for error messages
   - Semantic HTML structure

**Dependencies**:
```typescript
import React, { useMemo } from 'react';
import { useNivelesAcceso } from '../hooks/useNivelesAcceso';
import { CodigoNivelAcceso, INivelAcceso } from '../types';
import { PERMISSION_LABELS } from '../../common/constants/permissions';
```

**Implementation Notes**:
- Use Tailwind CSS for styling (no CSS files)
- Extract option rendering to separate function for readability
- Implement proper focus management for accessibility
- Show loading state with spinner (can use existing UI component)
- Implement retry mechanism for failed fetches
- Sort options by `orden` field from API
- Disabled state should be visually distinct

---

### **Step 6: Update Frontend Routes/App.tsx (if needed)**

**File**: `frontend/src/App.tsx`

**Action**: Verify/update application routes to ensure ACL feature is accessible (if needed).

**Implementation Steps**:

1. Review `frontend/src/App.tsx` routing structure

2. Determine if ACL management UI needs a dedicated route:
   - If NO route needed (component used in other features): Skip this step
   - If YES (standalone ACL admin page): Add route

3. If new route needed:
   - Create lazy-loaded page component: `frontend/src/features/acl/pages/AclCatalogPage.tsx`
   - Add route in App.tsx using React Router patterns
   - Ensure route is protected (authentication check if needed)

4. Verify imports and exports:
   - Component is properly exported
   - Route path matches API documentation

**Implementation Notes**:
- Based on US-ACL-001, component is primarily for selection (dropdown)
- Route might not be necessary unless building dedicated admin page
- If component is reusable across features, no route modification needed
- Lazy loading routes improves bundle performance

---

### **Step 7: Write Integration Tests (E2E Simulation)**

**File**: `frontend/src/features/acl/__tests__/acl.integration.test.ts`

**Action**: Create integration tests simulating end-to-end user workflows.

**Test Scenarios**:

```typescript
describe('ACL Feature Integration', () => {
  // Complete workflow: Load niveles → Select level → Verify selection
  // Error recovery: Handle network failure → Show retry → Successful retry
  // Cache behavior: Load → Cache → Load again → Verify cache hit
  // Multi-component: Use hook + component together
})
```

**Implementation Steps**:

1. Create `frontend/src/features/acl/__tests__/acl.integration.test.ts`

2. Set up integration test fixtures:
   - Mock complete API responses from backend
   - Create realistic test data matching backend structure
   - Reuse mock utils from Step 6-7 tests

3. Test complete workflows:
   - User opens component → sees niveles loading → niveles appear
   - User selects nivel → onChange fires → selection updates
   - API fails → retry shown → user retries → success
   - Subsequent opens use cache → no loading state

4. Verify data flow:
   - Service receives correct API calls
   - Hook processes responses correctly
   - Component displays correct data
   - User interactions trigger callbacks

**Dependencies**:
```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as nivelAccesoService from '../services/nivelAccesoService';
import { useNivelesAcceso } from '../hooks/useNivelesAcceso';
import NivelAccesoSelect from '../components/NivelAccesoSelect';
```

**Implementation Notes**:
- Test interactions between service, hook, and component
- Verify data transformations at each layer
- Use same mock data as unit tests for consistency
- Include realistic scenarios (delays, timeouts, errors)
- Document test flows with comments

---

### **Step 8: Documentation and Code Quality**

**File**: `frontend/README.md`

**Action**: Create feature documentation and ensure code follows project standards.

**Implementation Steps**:

1. UPDATE `frontend/README.md`:
   - Overview of ACL feature module
   - How to use `useNivelesAcceso` hook
   - How to use `NivelAccesoSelect` component
   - Example code snippets
   - Known limitations or TODOs

2. Add JSDoc comments to all exported functions:
   - Type signatures
   - Parameter descriptions
   - Return type descriptions
   - Usage examples
   - @deprecated, @internal tags if applicable

3. Verify TypeScript compilation:
   ```bash
   npm run build
   ```

4. Verify all tests pass:
   ```bash
   npm run test -- features/acl
   ```

**Dependencies**:
- ESLint configuration from project
- TypeScript compiler configuration
- Testing framework configuration

**Implementation Notes**:
- Documentation helps future developers understand feature
- JSDoc comments improve IDE autocomplete
- Linting catches style issues before review
- Type checking prevents runtime errors
- Test coverage validates implementation

---

### **Step 9: Update Technical Documentation**

**File**: `ai-specs/specs/frontend-standards.mdc`

**Action**: Review and update technical documentation to reflect ACL feature implementation.

**Implementation Steps**:

1. **Review Changes**: Analyze all code changes made:
   - New feature directory structure
   - Custom hook pattern for data fetching with caching
   - Reusable component pattern for selections
   - Service layer for API communication
   - Test patterns used (unit, component, integration)

2. **Identify Documentation Updates Needed**:
   - **Service Layer Patterns** → Add `nivelAccesoService` example
   - **Custom Hooks** → Document `useNivelesAcceso` pattern and caching strategy
   - **Component Patterns** → Add `NivelAccesoSelect` as reusable component example
   - **TypeScript Patterns** → Document interface definitions for API responses
   - **Testing Patterns** → Include hook and component testing examples
   - **Constants** → Document `permissions.ts` constant organization

3. **Update `ai-specs/specs/frontend-standards.mdc`**:
   - Find "Service Layer Architecture" section → Add service pattern example
   - Find "State Management" section → Add hook with caching pattern
   - Find "Component Conventions" section → Add reusable component example
   - Find "Testing Standards" section → Add hook and component test examples
   - Find "Naming Conventions" section → Verify ACL naming follows conventions

4. **Update `ai-specs/specs/api-spec.yml`** (if applicable):
   - Add `/api/acl/niveles` endpoint documentation
   - Request/response schema for ACL endpoints
   - Error codes and messages

5. **Verify Documentation**:
   - All code examples are accurate
   - Patterns match current implementation
   - English language compliance (per base-standards.mdc)
   - Formatting consistency with existing docs

6. **Report Updates**:
   - Document which files were updated
   - List specific sections added/modified
   - Include before/after if major changes

**References**:
- `ai-specs/specs/documentation-standards.mdc` for format
- `ai-specs/specs/frontend-standards.mdc` for structure

**Implementation Notes**:
- Documentation updates are MANDATORY
- All updates must be in English (code comments may be Spanish for user messages)
- Examples should be copy-paste ready
- Include links to related standards and patterns

---

## Implementation Order

2. **Step 1**: Create TypeScript Types and Constants (`types/index.ts`)
3. **Step 2**: Create Permissions Constants (`common/constants/permissions.ts`)
4. **Step 3**: Create HTTP Service (`services/nivelAccesoService.ts`)
5. **Step 4**: Create Custom React Hook (`hooks/useNivelesAcceso.ts`)
6. **Step 5**: Create Reusable Component (`components/NivelAccesoSelect.tsx`)
8. **Step 6**: Integration Tests (`__tests__/acl.integration.test.ts`)
9. **Step 7**: Code Quality and Linting
10. **Step 8**: Update Technical Documentation (`ai-specs/specs/frontend-standards.mdc`)

---

## Testing Checklist

### Post-Implementation Verification

#### Unit Tests
- [ ] `useNivelesAcceso` hook fetches data correctly
- [ ] Hook caches data and respects TTL
- [ ] Hook handles API errors gracefully
- [ ] Hook refetch clears cache and reloads
- [ ] All hook state transitions work correctly

#### Component Tests
- [ ] `NivelAccesoSelect` renders dropdown with label
- [ ] All 3 niveles appear as options in dropdown
- [ ] Selection change fires onChange callback
- [ ] Loading state displays while fetching
- [ ] Error state shows error message and retry button
- [ ] Disabled prop disables the dropdown
- [ ] Required prop shows required indicator
- [ ] Custom props (label, size, error) are respected
- [ ] Accessibility attributes are present (aria-*, htmlFor)

#### Integration Tests
- [ ] Complete workflow: fetch → display → select works end-to-end
- [ ] Error recovery workflow: error → retry → success
- [ ] Cache behavior: first load fetches, subsequent uses cache
- [ ] Multiple component instances work independently

#### Functional Verification
- [ ] Dropdown displays correct access level names (Lectura, Escritura, Administración)
- [ ] Selected value is properly set and retrieved
- [ ] Options are ordered by `orden` field from API
- [ ] Loading spinner appears during fetch
- [ ] Error messages are in Spanish and user-friendly
- [ ] Retry button triggers refetch
- [ ] No console errors or warnings in browser DevTools

#### Type Safety
- [ ] No TypeScript compilation errors: `npm run type-check` passes
- [ ] No `any` types used in implementation
- [ ] All props are properly typed
- [ ] All return values are properly typed

#### Code Quality
- [ ] ESLint checks pass: `npm run lint` succeeds
- [ ] Code follows naming conventions from standards
- [ ] Functions have JSDoc comments
- [ ] No dead code or unused imports
- [ ] Code is properly formatted

#### Performance
- [ ] Component renders efficiently (no unnecessary re-renders)
- [ ] Cache prevents repeated API calls
- [ ] Bundle size impact is minimal
- [ ] No memory leaks in hooks (cleanup functions present)

---

## Error Handling Patterns

### Service Layer (nivelAccesoService.ts)

```typescript
// Pattern for API error handling
try {
  const response = await axios.get<ApiResponse<INivelAcceso[]>>('/api/acl/niveles');
  return response.data.data; // Extract data from envelope
} catch (error) {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 404) {
      throw new Error('Los niveles de acceso no fueron encontrados');
    }
    throw new Error(error.response?.data?.message || 'Error al cargar niveles de acceso');
  }
  throw new Error('Error de conexión. Intenta de nuevo más tarde.');
}
```

### Hook Layer (useNivelesAcceso.ts)

```typescript
// Pattern for hook error handling
try {
  const data = await getNivelesAcceso();
  setNiveles(data);
  setError(null);
} catch (err) {
  const errorMessage = err instanceof Error ? err.message : 'Error desconocido';
  setError(errorMessage);
  setNiveles([]);
}
```

### Component Layer (NivelAccesoSelect.tsx)

```typescript
// Pattern for component error display
{error && (
  <div className="text-red-600 text-sm mt-1">
    <p>{error}</p>
    <button onClick={() => refetch()} className="text-blue-600 underline mt-1">
      Intentar de nuevo
    </button>
  </div>
)}
```

### User-Friendly Error Messages (Spanish)

| Scenario | Message |
|----------|---------|
| Network timeout | "Sin conexión. Por favor, intenta de nuevo." |
| 404 Not Found | "Los niveles de acceso no existen." |
| 500 Server Error | "Error del servidor. Intenta más tarde." |
| Invalid selection | "Nivel de acceso inválido. Selecciona uno válido." |
| Cache error | "Error al cargar datos. Recargando..." |

---

## UI/UX Considerations

### Tailwind Components

**Select Dropdown**:
- Use native HTML `<select>` element (semantic, accessible)
- Apply Tailwind classes: `px-3 py-2 border rounded-md focus:outline-none focus:ring`
- Disabled state: `opacity-50 cursor-not-allowed`
- Error state: `border-red-600`

**Label**:
- Use `<label>` element with `htmlFor` binding
- Apply Tailwind: `block text-sm font-medium mb-2`
- Add required indicator: `<span className="text-red-600">*</span>`

**Error Message**:
- Apply Tailwind: `text-red-600 text-sm mt-1`
- Include clear, actionable message

**Loading State**:
- Show spinner using project's existing UI component
- Disable dropdown during load
- Display "Cargando..." text

**Retry Button**:
- Style as secondary button
- Position below error message
- Clear call-to-action text

### Responsive Design

- Component is inline/block, adapts to container width
- Works on mobile, tablet, desktop
- Touch-friendly dropdown (native select on mobile)
- No fixed widths; use flexible layout

### Accessibility Requirements

**Keyboard Navigation**:
- Tab key navigates to select
- Tab focuses on retry button if present
- Arrow keys navigate options
- Enter/Space selects option

**Screen Readers**:
- Proper `<label>` with `htmlFor`
- `aria-describedby` linking label to select
- `aria-required="true"` when required
- `aria-disabled="true"` when disabled
- `aria-label` on retry button

**Focus Management**:
- Clear focus outline (Tailwind's `focus:ring`)
- Focus moves logically (label → select → error → retry)
- Focus not trapped or lost

**Color Contrast**:
- Error message (red) passes WCAG AA
- Disabled state still readable (not just grey)
- Sufficient contrast between background and text

### Loading States and Feedback

**Initial Load**:
```
[Loading spinner] Cargando niveles de acceso...
```

**Data Loaded**:
```
Nivel de Acceso
[Dropdown with options ▼]
```

**Error State**:
```
Nivel de Acceso
[Dropdown - disabled] ▼

⚠️ Error al cargar niveles de acceso
[Intentar de nuevo] button
```

**Selected State**:
```
Nivel de Acceso
[Lectura / Consulta ▼] (selected option highlighted)
```

---

## Dependencies

### External Libraries

| Library | Purpose | Version | Status |
|---------|---------|---------|--------|
| `react` | UI framework | ^18.x | Existing |
| `axios` | HTTP client | ^1.x | Existing |
| `typescript` | Type safety | ^5.x | Existing |
| `vitest` or `jest` | Testing framework | Latest | Existing |

### Internal Dependencies

| Module | Import Path | Purpose |
|--------|------------|---------|
| React Hooks | `react` | useState, useEffect, useCallback |
| Axios Instance | `core/shared/api/` | HTTP client (if project has shared instance) |
| Type Definitions | `features/acl/types/` | INivelAcceso, CodigoNivelAcceso |
| Constants | `common/constants/permissions` | Permission codes and labels |

### No Additional Dependencies Required

- Tailwind CSS already configured in project
- No new npm packages needed
- Use project's existing axios, testing, and build setup

---

## Notes

### Important Reminders

1. **Language Requirements**:
   - All code (variables, functions, interfaces) in **English**
   - All UI text and error messages in **Spanish**
   - Comments in code may be in either language for clarity

2. **TypeScript Strict Mode**:
   - No `any` types allowed
   - All functions must have typed parameters and returns
   - All API responses must be typed
   - Use `unknown` instead of `any` if necessary

3. **Architectural Patterns**:
   - Service Layer: Data fetching and API communication
   - Custom Hooks: State management and side effects
   - Components: UI rendering and user interaction
   - Types: Shared data structures
   - Constants: Immutable configuration values

4. **Performance Considerations**:
   - Cache niveles for 24 hours (reduce API calls)
   - Use `useMemo()` if rendering many options
   - Lazy load ACL feature module if separate route
   - Avoid re-fetching on every render

5. **Security Considerations**:
   - Validate API responses against type definitions
   - No sensitive data in localStorage (cache only public info)
   - Use HTTPOnly cookies for auth tokens (handled by interceptors)
   - Sanitize error messages (no sensitive details)

6. **Testing Strategy**:
   - Write tests for hook behavior (fetching, caching, errors)
   - Write tests for component rendering and interaction
   - Mock external dependencies (API service, localStorage)
   - Test both happy paths and error scenarios
   - Aim for >80% code coverage

7. **Browser Compatibility**:
   - Target modern browsers (Chrome, Firefox, Safari, Edge latest)
   - Use native HTML select (broad compatibility)
   - Polyfills for older browsers handled by project

8. **Accessibility is Mandatory**:
   - Every component must be keyboard navigable
   - Every form field must have a label
   - Error messages must be linked to form fields
   - Color should not be the only indicator (use icons/text)

---

## Next Steps After Implementation

### Post-Implementation Tasks

1. **Code Review**:
   - Request review from backend and frontend team leads
   - Ensure adherence to project standards
   - Validate type safety and error handling

2. **QA Testing**:
   - Manual testing in development environment
   - Cross-browser testing (Chrome, Firefox, Safari)
   - Mobile/responsive testing
   - Accessibility testing with screen reader

3. **Integration**:
   - Merge `feature/US-ACL-001-frontend` to develop branch
   - Deploy to staging environment
   - Integration test with backend API
   - Verify cache behavior in staging

4. **Documentation**:
   - Update project README with ACL feature info
   - Create Storybook stories if applicable
   - Update API documentation with examples
   - Communicate changes to team

5. **Related Tickets**:
   - Backend tests may depend on frontend consuming API
   - Subsequent ACL features (folder ACL, document ACL) will use this component
   - Admin dashboard may use this feature
   - Permission assignment features will build on this

6. **Performance Monitoring**:
   - Monitor cache hit rates in production
   - Track API response times
   - Monitor bundle size impact
   - Check for memory leaks in production

---

## Implementation Verification

### Final Verification Checklist

**Code Quality**
- [ ] TypeScript compilation succeeds without errors
- [ ] ESLint checks pass with zero warnings
- [ ] All code follows naming conventions from standards
- [ ] JSDoc comments present on all exported functions
- [ ] No console.error or console.warn in production code
- [ ] No `any` types used

**Functionality**
- [ ] Service makes correct API calls to backend endpoints
- [ ] Hook fetches and caches data correctly
- [ ] Component renders dropdown with all options
- [ ] Selection change fires onChange callback
- [ ] Loading state displays while fetching
- [ ] Error state displays with retry option
- [ ] Disabled state works properly
- [ ] Required prop shows indicator

**Testing**
- [ ] All unit tests pass: `npm test`
- [ ] All component tests pass
- [ ] All integration tests pass
- [ ] Coverage report generated (target: >80%)
- [ ] No test warnings or errors in console

**Integration**
- [ ] Backend API returns expected responses
- [ ] Frontend types match backend response structure
- [ ] Cache mechanism works end-to-end
- [ ] Error scenarios handled gracefully
- [ ] No CORS issues with API calls
- [ ] Authentication/Authorization working (if required)

**UI/UX**
- [ ] Component renders properly on all screen sizes
- [ ] Dropdown is keyboard accessible
- [ ] Error messages are clear and actionable
- [ ] Loading indicator is visible
- [ ] Retry button works
- [ ] Selected value visually highlighted

**Documentation**
- [ ] Feature documentation complete
- [ ] Technical standards updated
- [ ] API documentation updated
- [ ] Code examples provided
- [ ] Known limitations documented

**Browser Compatibility**
- [ ] Works in Chrome (latest)
- [ ] Works in Firefox (latest)
- [ ] Works in Safari (latest)
- [ ] Works in Edge (latest)
- [ ] Responsive on mobile browsers

**Performance**
- [ ] No console errors on page load
- [ ] Page loads in <3 seconds on 4G
- [ ] Component renders in <100ms
- [ ] Cache prevents repeated API calls
- [ ] No memory leaks detected

**Ready for Merge**
- [ ] All above items verified ✓
- [ ] Code review approved
- [ ] QA sign-off received
- [ ] Documentation reviewed
- [ ] Branch ready to merge to develop

---

**Status**: Ready for Development  
**Created**: 28 January 2026  
**Last Updated**: 28 January 2026  
**Frontend Feature Classification**: ACL Catalog (Reusable Component + Data Fetching)
