# DocFlow Frontend

Sistema de Gestión Documental - Frontend

## 📋 Descripción

Frontend del sistema DocFlow construido con una **Arquitectura Híbrida Feature-Driven** combinada con principios de **Clean Architecture**. Esta arquitectura separa la lógica de negocio pura de la infraestructura y la UI, facilitando el mantenimiento, testing y escalabilidad del proyecto.

## 🛠 Tecnologías Utilizadas

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **React** | 18+ | Biblioteca UI |
| **Vite** | 6.x | Build tool y dev server |
| **TypeScript** | 5.x | Tipado estático (Strict Mode) |
| **Tailwind CSS** | 3.x | Framework de estilos utility-first |
| **Zustand** | 5.x | Gestión de estado global |
| **Axios** | 1.x | Cliente HTTP |
| **React Router** | 6.x | Enrutamiento SPA |

## 🏗 Arquitectura Implementada

### Feature-Driven Clean Architecture

```
src/
├── core/                    # Lógica pura (Domain + Shared)
│   ├── domain/              # Interfaces, tipos y modelos puros
│   │   └── .gitkeep         # (Sin dependencias externas)
│   └── shared/              # Configuraciones globales
│       ├── api/             # Instancia Axios singleton
│       ├── constants/       # Constantes y endpoints
│       └── router/          # Configuración de rutas base
│
├── features/                # Funcionalidades por dominio
│   └── [feature-name]/      # Cada feature es autocontenida
│       ├── api/             # Llamadas HTTP de la feature
│       ├── components/      # Componentes internos
│       ├── hooks/           # Stores Zustand y hooks
│       ├── pages/           # Páginas/vistas
│       └── index.ts         # Barrel exports
│
├── common/
│   └── ui/                  # Componentes atómicos reutilizables
│       └── .gitkeep         # (Button, Layout, etc.)
│
├── App.tsx                  # Componente raíz
├── main.tsx                 # Punto de entrada
└── index.css                # Estilos globales Tailwind
```

### Principios Clave

1. **Separación de Capas**: El código de UI nunca accede directamente a la API. Todo fluye a través de hooks y stores (Zustand).

2. **Features Autocontenidas**: Cada funcionalidad tiene su propia carpeta con api, componentes, hooks y páginas.

3. **Domain Puro**: Las interfaces y tipos en `/core/domain` no tienen dependencias externas.

4. **Alias de Ruta**: Importaciones limpias con `@core`, `@features`, `@ui`.

## 🚀 Instalación y Levantamiento

### Requisitos Previos

- Node.js 18+ (recomendado: 20 LTS)
- npm 9+

### Instalación

```bash
# Navegar al directorio frontend
cd frontend

# Instalar dependencias
npm install
```

### Desarrollo

```bash
# Iniciar servidor de desarrollo
npm run dev
```

El servidor estará disponible en: `http://localhost:3000`

### Build de Producción

```bash
# Generar build optimizado
npm run build

# Preview del build
npm run preview
```

### Linting

```bash
# Ejecutar ESLint
npm run lint
```

## 📁 Estructura de Carpetas Clave

| Carpeta | Propósito |
|---------|-----------|
| `/src/core/domain` | Interfaces y tipos TypeScript puros. Sin lógica, sin dependencias. |
| `/src/core/shared` | Configuraciones compartidas: Axios, Router, constantes globales. |
| `/src/features` | Módulos funcionales. Cada feature encapsula su propia lógica completa. |
| `/src/common/ui` | Componentes de UI reutilizables globalmente (botones, layouts, modales). |

## 🔧 Configuración

### Variables de Entorno

Crear archivo `.env.local` en la raíz:

```env
VITE_API_BASE_URL=/api
```

### Proxy de Desarrollo

El servidor de desarrollo está configurado para hacer proxy de las peticiones `/api` al backend en `http://localhost:8080` (Gateway).

## 📖 Guía de Desarrollo

### Crear una Nueva Feature

1. Crear carpeta en `/src/features/[nombre-feature]/`
2. Agregar subcarpetas: `api/`, `components/`, `hooks/`, `pages/`
3. Crear `index.ts` con barrel exports
4. Registrar rutas en `/src/core/shared/router/AppRouter.tsx`

### Convenciones de Código

- Componentes funcionales con `function` keyword
- Named exports (no default exports en componentes)
- Nombres de archivos: `PascalCase` para componentes, `camelCase` para utilidades
- Directorios en `kebab-case`

## 📏 Reglas de desarrollo frontend

Las reglas detalladas para el desarrollo de la aplicación frontend se encuentran en:

- [.github/rules-frontend.md](../.github/rules-frontend.md)
- Índice general de reglas del proyecto: [.github/RULES.md](../.github/RULES.md)

## ACL (Access Control List) Patterns

### Feature Structure
The ACL feature demonstrates the recommended patterns for implementing dropdown selectors with backend data:

```
features/acl/
├── components/        # UI-specific components
│   └── NivelAccesoSelect.tsx   # Reusable dropdown selector component
├── hooks/            # Custom React hooks
│   └── useNivelesAcceso.ts     # Hook with caching & data fetching
├── services/         # API communication layer
│   └── nivelAccesoService.ts   # HTTP service with error handling
├── types/            # TypeScript interfaces
│   └── index.ts      # Domain models (INivelAcceso, CodigoNivelAcceso)
└── __tests__/        # Comprehensive test coverage
    ├── useNivelesAcceso.test.ts
    ├── NivelAccesoSelect.test.tsx
    └── acl.integration.test.ts
```

### Data Fetching with Caching

**Custom Hook Pattern** (`useNivelesAcceso.ts`):
```typescript
interface UseNivelesAccesoReturn {
  niveles: INivelAcceso[];
  loading: boolean;
  error: string | null;
  refetch: () => Promise<void>;
}

export const useNivelesAcceso = (
  enableCache: boolean = true,
  cacheTTL: number = 24 * 60 * 60 * 1000 // 24 hours default
): UseNivelesAccesoReturn => {
  // Auto-fetch on mount
  // localStorage caching with configurable TTL
  // Graceful error handling
  // Manual refetch capability
};
```

**Key Features:**
- **Automatic fetching** on component mount via `useEffect`
- **localStorage caching** with configurable time-to-live (TTL)
- **Cache expiration** logic to prevent stale data
- **Manual refetch** function to invalidate cache and reload
- **Optimized** to prevent unnecessary API calls
- **Typed return** with loading and error states

### API Service Pattern

**HTTP Service** (`nivelAccesoService.ts`):
```typescript
export const aclApi = {
  getNivelesAcceso: async (): Promise<INivelAcceso[]> => {
    // GET /api/acl/niveles
    // Extract data from envelope: response.data.data
    // Handle errors with Spanish messages
  },

  getNivelAccesoByCodigo: async (codigo: CodigoNivelAcceso): Promise<INivelAcceso> => {
    // GET /api/acl/niveles/{codigo}
  }
};
```

**Service Characteristics:**
- **Object-based export** (not class-based)
- **Async methods** with Promise return types
- **Error handling** with user-friendly Spanish messages
- **Type-safe** with full TypeScript support
- **Centralized axios instance** from `@core/shared/api/axiosInstance`

### Component Implementation

**Reusable Dropdown** (`NivelAccesoSelect.tsx`):
```typescript
export interface NivelAccesoSelectProps {
  value: CodigoNivelAcceso | '';
  onChange: (codigo: CodigoNivelAcceso) => void;
  label?: string;
  disabled?: boolean;
  required?: boolean;
  error?: string | null;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  placeholder?: string;
  showDefaultOption?: boolean;
}

export const NivelAccesoSelect: React.FC<NivelAccesoSelectProps> = ({
  value,
  onChange,
  label,
  disabled,
  required,
  error,
  size,
  className,
  placeholder,
  showDefaultOption
}) => {
  const { niveles, loading, error: fetchError, refetch } = useNivelesAcceso();
  // Component implementation with:
  // - Integrated hook for data management
  // - Tailwind CSS styling with size variants
  // - Error handling with retry capability
  // - Full accessibility support
  // - Loading and empty states
};
```

**Component Features:**
- **Integrated hook usage** for automatic data fetching
- **Multiple error sources** (validation + API)
- **Loading state** with visual feedback
- **Disabled state** support
- **Size variants** (sm, md, lg)
- **Accessibility**: labels, aria-invalid, aria-describedby
- **Empty state** messaging
- **Retry functionality** for API errors

### Testing Strategy

**Hook Testing** (40+ test cases):
- Data fetching and initial state
- Cache validation and TTL expiration
- Error handling and recovery
- Refetch functionality
- State transitions
- Configuration options

**Component Testing** (30+ test cases):
- Rendering and option display
- Selection change handling
- Loading states
- Error display and retry
- Disabled state
- Size and styling variants
- Accessibility compliance
- Keyboard navigation

**Integration Testing** (15+ scenarios):
- Complete user flows
- Form integration
- Multi-component interactions
- Performance with caching
- Error recovery workflows

```typescript
// Example: Hook test for caching
it('should use cached data on subsequent calls', async () => {
  const { result: result1 } = renderHook(() => useNivelesAcceso());
  await waitFor(() => expect(result1.current.loading).toBe(false));
  
  const { result: result2 } = renderHook(() => useNivelesAcceso());
  await waitFor(() => expect(result2.current.loading).toBe(false));
  
  // Should return cached data without additional API calls
  expect(result2.current.niveles).toEqual(mockNiveles);
});
```

### Constants Organization

**Permission Constants** (`src/common/constants/permissions.ts`):
```typescript
export const PERMISSION_CODES = {
  LECTURA: 'LECTURA',
  ESCRITURA: 'ESCRITURA',
  ADMINISTRACION: 'ADMINISTRACION',
} as const;

export const PERMISSION_LABELS: Record<CodigoNivelAcceso, string> = {
  LECTURA: 'Lectura',
  ESCRITURA: 'Escritura',
  ADMINISTRACION: 'Administración',
};

export type PermissionCodeKey = keyof typeof PERMISSION_CODES;
```

## 📝 Licencia

Proyecto privado - Todos los derechos reservados
