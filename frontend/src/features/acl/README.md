# ACL Feature Documentation

## Overview

The ACL (Access Control List) feature provides a complete implementation for managing access levels in the application. It includes a reusable dropdown component (`NivelAccesoSelect`), custom React hook (`useNivelesAcceso`), HTTP service layer, and comprehensive test coverage.

## Quick Start

### Basic Usage

```typescript
import { NivelAccesoSelect } from '@features/acl';
import { useState } from 'react';
import type { CodigoNivelAcceso } from '@features/acl';

function MyComponent() {
  const [selectedLevel, setSelectedLevel] = useState<CodigoNivelAcceso | ''>('');

  return (
    <NivelAccesoSelect
      value={selectedLevel}
      onChange={setSelectedLevel}
      label="Nivel de Acceso"
      required
    />
  );
}
```

### With Validation

```typescript
const [selectedLevel, setSelectedLevel] = useState<CodigoNivelAcceso | ''>('');
const [error, setError] = useState<string | null>(null);

const handleChange = (codigo: CodigoNivelAcceso) => {
  setSelectedLevel(codigo);
  
  // Custom validation
  if (codigo === 'ADMINISTRACION' && !userCanAdminister) {
    setError('No tienes permiso para administración');
  } else {
    setError(null);
  }
};

return (
  <NivelAccesoSelect
    value={selectedLevel}
    onChange={handleChange}
    error={error}
    required
  />
);
```

## Architecture

### Directory Structure

```
features/acl/
├── components/
│   └── NivelAccesoSelect.tsx       # Reusable dropdown component
├── hooks/
│   └── useNivelesAcceso.ts         # Data fetching & caching hook
├── services/
│   └── nivelAccesoService.ts       # HTTP API client
├── types/
│   └── index.ts                     # TypeScript interfaces
├── __tests__/
│   ├── useNivelesAcceso.test.ts    # Hook unit tests
│   ├── NivelAccesoSelect.test.tsx  # Component tests
│   └── acl.integration.test.ts     # Integration tests
├── index.ts                         # Barrel export
└── README.md                        # This file
```

## API Reference

### Types

#### `INivelAcceso`

Main domain model for access levels.

```typescript
interface INivelAcceso {
  id: string;
  codigo: CodigoNivelAcceso;
  nombre: string;
  descripcion: string;
  orden: number;
  activo: boolean;
  accionesPermitidas: AccionPermitida[];
  fechaCreacion: Date;
  fechaActualizacion: Date;
}
```

#### `CodigoNivelAcceso`

Union type for valid access level codes.

```typescript
type CodigoNivelAcceso = 'LECTURA' | 'ESCRITURA' | 'ADMINISTRACION';
```

#### `AccionPermitida`

Union type for valid actions permitted by access levels.

```typescript
type AccionPermitida = 
  | 'ver'
  | 'listar'
  | 'descargar'
  | 'subir'
  | 'modificar'
  | 'crear_version'
  | 'eliminar'
  | 'administrar_permisos'
  | 'cambiar_version_actual';
```

### Hooks

#### `useNivelesAcceso(enableCache?, cacheTTL?)`

Custom hook for fetching and managing access levels with automatic caching.

**Parameters:**
- `enableCache` (boolean, default: true) - Enable localStorage caching
- `cacheTTL` (number, default: 24 hours in ms) - Cache time-to-live

**Returns:**
```typescript
{
  niveles: INivelAcceso[];        // Array of access levels
  loading: boolean;               // Loading state indicator
  error: string | null;           // Error message (null if no error)
  refetch: () => Promise<void>;   // Manual refetch function
}
```

**Example:**
```typescript
const { niveles, loading, error, refetch } = useNivelesAcceso();

if (loading) return <p>Cargando...</p>;
if (error) {
  return (
    <div>
      <p>Error: {error}</p>
      <button onClick={refetch}>Reintentar</button>
    </div>
  );
}

return (
  <select>
    {niveles.map(nivel => (
      <option key={nivel.id} value={nivel.codigo}>
        {nivel.nombre}
      </option>
    ))}
  </select>
);
```

### Components

#### `NivelAccesoSelect`

Reusable dropdown component for selecting access levels.

**Props:**
```typescript
interface NivelAccesoSelectProps {
  value: CodigoNivelAcceso | '';     // Currently selected level
  onChange: (codigo: CodigoNivelAcceso) => void;  // Selection change handler
  label?: string;                    // Label text
  disabled?: boolean;                // Disable the select
  required?: boolean;                // Mark as required
  error?: string | null;             // Validation error message
  size?: 'sm' | 'md' | 'lg';        // Input size
  className?: string;                // Additional CSS classes
  placeholder?: string;              // Placeholder text
  showDefaultOption?: boolean;       // Show default "Select..." option
}
```

**Example:**
```typescript
<NivelAccesoSelect
  value={selectedLevel}
  onChange={setSelectedLevel}
  label="Nivel de Acceso"
  required
  error={validationError}
  size="md"
  placeholder="Elige un nivel..."
/>
```

### Services

#### `aclApi`

HTTP client for ACL API communication.

**Methods:**

**`getNivelesAcceso(): Promise<INivelAcceso[]>`**
- Fetches all available access levels
- Endpoint: `GET /api/acl/niveles`
- Caching: Handled by `useNivelesAcceso` hook

**`getNivelAccesoByCodigo(codigo: CodigoNivelAcceso): Promise<INivelAcceso>`**
- Fetches a single access level by code
- Endpoint: `GET /api/acl/niveles/{codigo}`
- Direct API call without caching

**Example:**
```typescript
import { aclApi } from '@features/acl';

// Get all levels
const allLevels = await aclApi.getNivelesAcceso();

// Get specific level
const readOnlyLevel = await aclApi.getNivelAccesoByCodigo('LECTURA');
```

## Features

### Automatic Caching

The `useNivelesAcceso` hook implements localStorage caching with:
- **Configurable TTL** (default: 24 hours)
- **Automatic expiration** based on timestamp
- **Cache invalidation** via `refetch()` function
- **Fallback to API** if cache is invalid or missing

```typescript
// Hook handles caching automatically
const { niveles, refetch } = useNivelesAcceso(true, 60 * 60 * 1000); // 1 hour TTL

// Manual cache invalidation
await refetch();
```

### Error Handling

Comprehensive error handling at multiple levels:

1. **Hook Level**: API errors with Spanish user messages
2. **Component Level**: Validation errors separate from API errors
3. **Recovery**: Automatic retry mechanism for API failures

```typescript
<NivelAccesoSelect
  value={nivel}
  onChange={setNivel}
  error={userValidationError}  // Prioritized over API error
/>
// Component shows API error with retry button if no validation error
```

### Accessibility

Full accessibility support including:
- ARIA attributes (`aria-invalid`, `aria-describedby`)
- Label associations via `htmlFor`
- Keyboard navigation support
- Screen reader compatibility
- Error announcements

### Type Safety

Strict TypeScript with:
- No `any` types
- Union types for valid codes
- Interface-based props
- Type exports for consumers

## Testing

### Unit Tests

**Hook Tests** (`useNivelesAcceso.test.ts`):
- Initial state and data fetching
- Cache validation and TTL
- Error handling and recovery
- Refetch functionality
- Configuration options

Run:
```bash
npm test -- useNivelesAcceso.test.ts
```

**Component Tests** (`NivelAccesoSelect.test.tsx`):
- Rendering and option display
- Selection handling
- Loading/error states
- Accessibility
- Size variants

Run:
```bash
npm test -- NivelAccesoSelect.test.tsx
```

### Integration Tests

**Integration Tests** (`acl.integration.test.ts`):
- Complete user flows
- Form integration
- Caching across instances
- Error recovery workflows
- Performance testing

Run:
```bash
npm test -- acl.integration.test.ts
```

### Run All ACL Tests

```bash
npm test -- src/features/acl/__tests__
```

## Constants

Permission constants are centralized in `src/common/constants/permissions.ts`:

```typescript
import { PERMISSION_CODES, PERMISSION_LABELS } from '@common/constants/permissions';

// Usage
const allCodes = Object.values(PERMISSION_CODES);
const label = PERMISSION_LABELS['LECTURA']; // "Lectura"
```

## Best Practices

### ✅ Do's

- Use the hook for automatic data management
- Leverage caching for performance
- Provide descriptive labels
- Show errors when they occur
- Handle loading states
- Keep components focused and small

### ❌ Don'ts

- Don't make direct API calls in components
- Don't cache-bust unnecessarily
- Don't ignore loading/error states
- Don't use `any` types
- Don't duplicate type definitions
- Don't mix validation and API errors

## Common Patterns

### Form Integration

```typescript
const [formData, setFormData] = useState({
  nombre: '',
  nivelAcceso: '' as CodigoNivelAcceso | '',
  descripcion: ''
});

const [errors, setErrors] = useState<Record<string, string>>({});

const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault();
  
  // Validate
  if (!formData.nivelAcceso) {
    setErrors(prev => ({ ...prev, nivelAcceso: 'Campo requerido' }));
    return;
  }
  
  // Submit...
};

return (
  <form onSubmit={handleSubmit}>
    <NivelAccesoSelect
      value={formData.nivelAcceso}
      onChange={(codigo) => setFormData(prev => ({ ...prev, nivelAcceso: codigo }))}
      error={errors.nivelAcceso}
      required
    />
  </form>
);
```

### Custom Validation

```typescript
const isAdminAllowed = userRole === 'admin';

const handleChange = (codigo: CodigoNivelAcceso) => {
  setNivel(codigo);
  
  if (codigo === 'ADMINISTRACION' && !isAdminAllowed) {
    setError('No tienes permiso para administración');
  } else {
    setError(null);
  }
};
```

### Conditional Rendering

```typescript
const { niveles } = useNivelesAcceso();

const availableLevels = niveles.filter(n => n.activo);

return (
  <NivelAccesoSelect
    value={nivel}
    onChange={setNivel}
    disabled={availableLevels.length === 0}
  />
);
```

## Performance Considerations

1. **Caching**: Enabled by default with 24-hour TTL
2. **Memoization**: Options are memoized internally
3. **Optimized Re-renders**: Hook uses proper dependency arrays
4. **Lazy Loading**: Data loaded on mount, not on import
5. **Bundle Size**: Tree-shakeable exports

## Troubleshooting

### Data Not Loading

```typescript
const { niveles, loading, error, refetch } = useNivelesAcceso(false); // Disable cache
if (error) {
  // Handle error
  await refetch();
}
```

### Cache Not Working

```typescript
// Check localStorage
const cached = localStorage.getItem('ACL_NIVELES_CACHE');
console.log('Cached data:', cached);

// Clear cache manually
localStorage.removeItem('ACL_NIVELES_CACHE');
```

### Tests Failing

Ensure mocks are set up:
```typescript
vi.spyOn(nivelAccesoService, 'aclApi', 'get').mockReturnValue({
  getNivelesAcceso: vi.fn().mockResolvedValue(mockNiveles),
});
```

## Contributing

When modifying the ACL feature:

1. Update types in `types/index.ts` if API contracts change
2. Update service methods in `services/nivelAccesoService.ts`
3. Update hook logic if caching strategy changes
4. Update component if new props are added
5. Add tests for new functionality
6. Update this README with new patterns

## Related Documentation

- Frontend Standards: `ai-specs/specs/frontend-standards.md`
- API Specification: `ai-specs/specs/api-spec.yml`
- Backend Implementation: `backend/document-core/src/main/.../acl`
- User Stories: `US/tickets/P2-Permisos/US-ACL-001.md`

## Support

For questions or issues with the ACL feature:
1. Check this README
2. Review test files for usage examples
3. Check the API specification
4. Consult backend documentation
