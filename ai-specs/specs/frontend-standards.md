---
description: Frontend development standards, best practices, and conventions for the app React application. Includes component patterns, state management, Feature-Driven Clean architecture, UI/UX guidelines, and testing standards.
globs: ["frontend/src/**/*.{js,jsx,ts,tsx}", "frontend/tsconfig.json", "frontend/package.json"]
alwaysApply: true
---

# Frontend Project Configuration and Best Practices - app

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
  - [Core Technologies](#core-technologies)
  - [UI Framework](#ui-framework)
  - [State Management & Data Flow](#state-management--data-flow)
  - [Testing Framework](#testing-framework)
  - [Development Tools](#development-tools)
- [Project Structure](#project-structure)
  - [Feature-Driven Clean Architecture](#feature-driven-clean-architecture)
- [Coding Standards](#coding-standards)
  - [Naming Conventions](#naming-conventions)
  - [Component Conventions](#component-conventions)
  - [State Management](#state-management)
  - [Service Layer Architecture](#service-layer-architecture)
- [UI/UX Standards](#uiux-standards)
  - [Tailwind CSS Integration](#tailwind-css-integration)
  - [Form Handling](#form-handling)
  - [Navigation Patterns](#navigation-patterns)
  - [Accessibility](#accessibility)
- [Testing Standards](#testing-standards)
- [Configuration Standards](#configuration-standards)
  - [TypeScript Configuration](#typescript-configuration)
  - [ESLint Configuration](#eslint-configuration)
  - [Environment Configuration](#environment-configuration)
- [Performance Best Practices](#performance-best-practices)
  - [Component Optimization](#component-optimization)
  - [Bundle Optimization](#bundle-optimization)
  - [API Efficiency](#api-efficiency)
- [Development Workflow](#development-workflow)
  - [Git Workflow](#git-workflow)
  - [Development Scripts](#development-scripts)
  - [Code Quality](#code-quality)
- [Migration Strategy](#migration-strategy)
  - [TypeScript Migration](#typescript-migration)
  - [Component Modernization](#component-modernization)
- [Best Practices Summary](#best-practices-summary)

---

## Overview

This document describes the best practices, conventions, and standards used in the app frontend application. These practices ensure code consistency, maintainability, and an optimal development experience.

## Technology Stack

### Core Technologies
- **React 19.2.0**: Modern React with functional components and hooks
- **TypeScript 5.9.3**: Static typing and improved development experience
- **Vite 7.2.4**: High-speed build tool and development server
- **React Router DOM 7.9.6**: Client-side routing and navigation
- **Node 18+**: JavaScript runtime for development

### UI Framework
- **Tailwind CSS 4.1.17**: CSS framework with utility-first and responsive approach
- **PostCSS 8.5.6**: CSS processor for advanced transformations
- **Autoprefixer 10.4.22**: Automatic vendor prefix aggregation

### State Management & Data Flow
- **React Hooks**: useState, useEffect for local state
- **Zustand 5.0.9**: Simple and flexible global state management
- **TanStack Query (React Query) 5.90.11**: Caching, synchronization, and server data management
- **TanStack Query DevTools 5.91.1**: Development tool for React Query
- **Axios 1.13.2**: HTTP client for API communication
- **JWT-decode 4.0.0**: Decoding and validating JWT tokens (optional)

### Development Tools
- **ESLint 9.39.1**: Code linting with TypeScript and React support
- **TypeScript ESLint 8.46.4**: TypeScript integration with ESLint
- **ESLint Plugin React Hooks 7.0.1**: React Hooks rules validation
- **ESLint Plugin React Refresh 0.4.24**: Validation for react-refresh

## Project Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── core/              # Pure logic and centralized configurations
│   │   ├── domain/        # Interfaces, types, and domain models
│   │   └── shared/        # Shared utilities
│   │       ├── api/       # Axios client, React Query client
│   │       ├── constants/ # Global constants, endpoints
│   │       ├── providers/ # Context providers (Query, Router)
│   │       └── router/    # Route configuration
│   │
│   ├── features/          # Functionality by domain
│   │   ├── auth/          # Authentication feature
│   │   │   ├── api/       # API calls (private)
│   │   │   ├── components/# UI-specific components
│   │   │   ├── hooks/     # Custom hooks + Zustand store
│   │   │   ├── pages/     # Pages/views
│   │   │   ├── types/     # Local TypeScript types
│   │   │   └── index.ts   # Barrel export (public API)
│   │   ├── conceptos/     # Payroll concepts feature
│   │   ├── dashboard/     # Dashboard feature
│   │   └── index.ts       # Main features barrel export
│   │
│   ├── common/            # Reusable components
│   │   └── ui/            # Atomic components (Button, Input, Layout...)
│   │
│   ├── App.tsx            # Root application component
│   ├── main.tsx           # Application entry point
│   └── index.css          # Global styles
│
├── index.html             # Entry point HTML
├── vite.config.ts         # Vite configuration
├── tsconfig.json          # TypeScript configuration
├── eslint.config.js       # ESLint configuration
├── tailwind.config.js     # Tailwind CSS configuration
├── postcss.config.js      # PostCSS configuration
├── package.json           # Dependencies and scripts
└── nginx.conf             # Nginx configuration (production)
```

### Feature-Driven Clean Architecture

The project architecture combines **Clean Architecture** with **Feature-Driven Development**:

**Clean Architecture Layers:**
1. **Core** (Innermost layer): Pure logic, types, configurations
2. **Features** (Application layer): Functionality organized by domain
3. **Common** (Shared components): Reusable UI

**Feature Organization:**
- Each feature is **self-contained** and exposes only its public API
- Features **don't import each other** (communication via events/callbacks)
- **Barrel exports** for clean encapsulation

**Principles:**
- **Layer separation**: Components never call the API directly
- **Encapsulation**: Each feature exposes only what's necessary
- **Independence**: Features can be developed in parallel
- **Path aliases**: Clean imports (@core, @features, @ui)

## Coding Standards

### Naming Conventions

- **Components**: Use PascalCase (e.g., `UserProfile`, `ConceptoCard`, `LayoutDashboard`)
- **Variables and functions**: Use camelCase (e.g., `userId`, `handleSubmit`, `fetchData`)
- **Constants**: Use UPPER_SNAKE_CASE (e.g., `API_BASE_URL`, `MAX_ITEMS`, `DEFAULT_PAGE_SIZE`)
- **Types and interfaces**: Use PascalCase (e.g., `UserData`, `ButtonProps`, `ConceptoNomina`)
- **Component files**: Use PascalCase (e.g., `UserCard.tsx`, `ConceptoForm.tsx`)
- **Utility files**: Use camelCase (e.g., `apiClient.ts`, `validators.ts`, `constants.ts`)
- **CSS classes**: Use kebab-case (e.g., `user-card`, `dashboard-header`, `btn-primary`)
- **Custom hooks**: Use camelCase with `use` prefix (e.g., `useAuth`, `useConceptoData`, `usePagination`)
- **Zustand stores**: Use camelCase with `Store` suffix (e.g., `authStore`, `dashboardStore`)
- **Props interfaces**: Prefix with component name (e.g., `ButtonProps`, `UserCardProps`)

**Examples:**

```typescript
// ✅ Correct: Clean code in English
import React, { useState } from 'react';

type UserCardProps = {
    user: User;
    index: number;
    onClick: (user: User) => void;
};

const UserCard: React.FC<UserCardProps> = ({ user, index, onClick }) => {
    const [isLoading, setIsLoading] = useState(false);
    
    const handleCardClick = () => {
        onClick(user);
    };
    
    return (
        <div className="user-card" onClick={handleCardClick}>
            {/* Component JSX */}
        </div>
    );
};

// ❌ Avoid: Spanish names
const TarjetaUsuario: React.FC<PropsTarjetaUsuario> = ({ usuario, indice, alHacerClic }) => {
    const [estaCargando, setEstaCargando] = useState(false);
    
    const manejarClicTarjeta = () => {
        alHacerClic(usuario);
    };
    
    return (
        <div className="tarjeta-usuario" onClick={manejarClicTarjeta}>
            {/* JSX */}
        </div>
    );
};
```

**Error messages and logs:**

```typescript
// ✅ Correct: Spanish messages with English code names
catch (error) {
    console.error('Error al obtener datos:', error);
    setError('No se pudieron cargar los datos. Por favor, intente más tarde.');
}
```

**Examples with project features:**

```typescript
// ✅ Concepts Feature
type ConceptCardProps = {
    concept: ConceptPayroll;
    onSelect: (id: number) => void;
};

const ConceptCard: React.FC<ConceptCardProps> = ({ concept, onSelect }) => {
    return (
        <div className="concept-card" onClick={() => onSelect(concept.id)}>
            {concept.name}
        </div>
    );
};

// ✅ API Service
export const conceptService = {
    getAllConcepts: async () => {
        try {
            const response = await apiClient.get('/concepts');
            return response.data;
        } catch (error) {
            console.error('Error fetching concepts:', error);
            throw error;
        }
    }
};

// ✅ Zustand Store
export const useAuthStore = create((set) => ({
    isAuthenticated: false,
    user: null,
    login: (credentials) => {
        // Login logic
    }
}));
```

### Component Conventions

#### Functional Components
- **Always use functional components** with hooks
- Use **TypeScript for new components**
- Components must be strongly typed

```typescript
// ✅ Preferred: TypeScript functional component
import React, { useState, useEffect } from 'react';

type ConceptPayroll = {
    id: number;
    name: string;
    type: 'Income' | 'Deduction' | 'Informational';
    active: boolean;
};

const ConceptsList: React.FC = () => {
    const [concepts, setConcepts] = useState<ConceptPayroll[]>([]);
    const [loading, setLoading] = useState(false);
    
    // Component logic
    
    return (
        // JSX
    );
};

export default ConceptosList;
```

#### Component Props
- **Define TypeScript interfaces/types** for props
- Use **destructuring** for props
- Include **default values** where appropriate

```typescript
type ConceptCardProps = {
    concept: ConceptPayroll;
    onEdit: (concept: ConceptPayroll) => void;
    onDelete: (id: number) => void;
};

const ConceptCard: React.FC<ConceptCardProps> = ({ concept, onEdit, onDelete }) => {
    return (
        // Component implementation
    );
};
```

#### Documentation with JSDoc
- Document complex components with JSDoc comments
- Include description, parameters, and types

```typescript
/**
 * Component for displaying a payroll concept card
 * 
 * Allows viewing concept details and provides
 * actions for editing or deleting.
 * 
 * @component
 * @example
 * const concept = { id: 1, name: 'Salary', type: 'Income', active: true };
 * return <ConceptCard concept={concept} onEdit={handleEdit} onDelete={handleDelete} />
 */
type ConceptCardProps = {
    /** Concept object to display */
    concept: ConceptPayroll;
    /** Callback when edit is clicked */
    onEdit: (concept: ConceptPayroll) => void;
    /** Callback when delete is clicked */
    onDelete: (id: number) => void;
};

const ConceptCard: React.FC<ConceptCardProps> = ({ concept, onEdit, onDelete }) => {
    // Implementation
};
```

### State Management

#### Local State with Hooks
- Use **useState** for component-level state
- Use **useEffect** with correct dependencies for side effects
- **Extract custom hooks** for reusable logic
- Use **useReducer** for complex state with multiple transitions

```typescript
// ✅ Simple state with useState
const [formData, setFormData] = useState({
    name: '',
    type: 'Income',
    active: true
});

const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
        ...prev,
        [name]: value
    }));
};

// ✅ Complex state with useReducer
type FormState = {
    name: string;
    type: string;
    active: boolean;
};

type FormAction = 
    | { type: 'UPDATE_FIELD'; field: keyof FormState; value: string | boolean }
    | { type: 'RESET' };

const formReducer = (state: FormState, action: FormAction): FormState => {
    switch (action.type) {
        case 'UPDATE_FIELD':
            return { ...state, [action.field]: action.value };
        case 'RESET':
            return { name: '', type: 'Income', active: true };
        default:
            return state;
    }
};

const [state, dispatch] = useReducer(formReducer, initialState);
```

#### Loading and Error States
- **Always handle loading states** for async operations
- **Implement error handling** with user-friendly messages
- **Display user feedback** with visual components

```typescript
const [data, setData] = useState<ConceptPayroll[]>([]);
const [loading, setLoading] = useState(true);
const [error, setError] = useState<string | null>(null);
const [success, setSuccess] = useState<string | null>(null);

const fetchConcepts = async () => {
    try {
        setLoading(true);
        setError(null);
        const result = await conceptService.getAllConcepts();
        setData(result);
        setSuccess('Concepts loaded successfully');
    } catch (error) {
        const message = error instanceof Error ? error.message : 'Unknown error';
        setError(`Error loading concepts: ${message}`);
        console.error('Error fetching concepts:', error);
    } finally {
        setLoading(false);
    }
};

useEffect(() => {
    fetchConcepts();
}, []);

// Render states
if (loading) return <LoadingSpinner />;
if (error) return <ErrorAlert message={error} />;
if (success) return <SuccessAlert message={success} />;

return <ConceptsList concepts={data} />;
```

#### Global State Management with Zustand
- Use **Zustand** for global state shared across multiple features
- Keep **stores focused** on a single responsibility
- Avoid **excessive prop drilling**

```typescript
// authStore.ts
import { create } from 'zustand';

type AuthStore = {
    isAuthenticated: boolean;
    user: User | null;
    token: string | null;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
};

export const useAuthStore = create<AuthStore>((set) => ({
    isAuthenticated: false,
    user: null,
    token: null,
    login: async (credentials) => {
        try {
            const response = await authService.login(credentials);
            set({
                isAuthenticated: true,
                user: response.user,
                token: response.token
            });
        } catch (error) {
            console.error('Error logging in:', error);
            throw error;
        }
    },
    logout: () => {
        set({
            isAuthenticated: false,
            user: null,
            token: null
        });
    }
}));

// Usage in components
const LoginForm: React.FC = () => {
    const { login } = useAuthStore();
    
    const handleSubmit = async (credentials: LoginCredentials) => {
        await login(credentials);
    };
};
```

#### React Query for Server State
- Use **TanStack Query** for server data caching and synchronization
- Configure **staleTime** and **gcTime** appropriately
- Keep server state in sync

```typescript
// hooks/useConcepts.ts
import { useQuery, useMutation } from '@tanstack/react-query';
import { conceptService } from '@features/concepts/api';

const CONCEPTS_QUERY_KEY = ['concepts'];

export const useConcepts = () => {
    return useQuery({
        queryKey: CONCEPTS_QUERY_KEY,
        queryFn: () => conceptService.getAllConcepts(),
        staleTime: 5 * 60 * 1000, // 5 minutes
    });
};

export const useUpdateConcept = () => {
    const queryClient = useQueryClient();
    
    return useMutation({
        mutationFn: (data: ConceptUpdateData) => 
            conceptService.updateConcept(data.id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ 
                queryKey: CONCEPTS_QUERY_KEY 
            });
        },
    });
};

// Usage in components
const ConceptForm: React.FC = () => {
    const { mutate: updateConcept, isPending } = useUpdateConcept();
    
    const handleSubmit = (formData: ConceptUpdateData) => {
        updateConcept(formData);
    };
};
```

### Service Layer Architecture

#### API Services with Axios
- **Centralize all API calls** in service files
- Use **Axios with typed client** for HTTP requests
- **Export service objects** with grouped methods
- **Handle errors at service level** when appropriate
- Use **interceptors** for global error handling and authentication

```typescript
// core/shared/api/apiClient.ts
import axios, { AxiosError } from 'axios';
import { useAuthStore } from '@features/auth/hooks';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3010';

export const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor to add authentication token
apiClient.interceptors.request.use((config) => {
    const token = useAuthStore.getState().token;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Interceptor to handle errors
apiClient.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
        if (error.response?.status === 401) {
            // Redirect to login
            useAuthStore.getState().logout();
        }
        return Promise.reject(error);
    }
);

export default apiClient;

// features/concepts/api/conceptService.ts
import { apiClient } from '@core/shared/api';
import { ConceptPayroll, CreateConceptDTO } from '../types';

type ConceptResponse = {
    success: boolean;
    data: ConceptPayroll[];
};

export const conceptService = {
    /**
     * Gets all payroll concepts
     * @returns Promise with array of concepts
     */
    getAllConcepts: async (): Promise<ConceptPayroll[]> => {
        try {
            const response = await apiClient.get<ConceptResponse>('/concepts');
            return response.data.data;
        } catch (error) {
            console.error('Error fetching concepts:', error);
            throw error;
        }
    },

    /**
     * Gets a specific concept by ID
     * @param id - Concept ID
     * @returns Promise with concept data
     */
    getConceptById: async (id: number): Promise<ConceptPayroll> => {
        try {
            const response = await apiClient.get<ConceptResponse>(`/concepts/${id}`);
            return response.data.data[0];
        } catch (error) {
            console.error(`Error fetching concept ${id}:`, error);
            throw error;
        }
    },

    /**
     * Creates a new payroll concept
     * @param conceptData - New concept data
     * @returns Promise with created concept
     */
    createConcept: async (conceptData: CreateConceptDTO): Promise<ConceptPayroll> => {
        try {
            const response = await apiClient.post<ConceptResponse>('/concepts', conceptData);
            return response.data.data[0];
        } catch (error) {
            console.error('Error creating concept:', error);
            throw error;
        }
    },

    /**
     * Updates an existing concept
     * @param id - Concept ID
     * @param conceptData - Updated data
     * @returns Promise with updated concept
     */
    updateConcept: async (id: number, conceptData: Partial<CreateConceptDTO>): Promise<ConceptPayroll> => {
        try {
            const response = await apiClient.put<ConceptResponse>(`/concepts/${id}`, conceptData);
            return response.data.data[0];
        } catch (error) {
            console.error(`Error updating concept ${id}:`, error);
            throw error;
        }
    },

    /**
     * Deletes a concept
     * @param id - Concept ID to delete
     */
    deleteConcept: async (id: number): Promise<void> => {
        try {
            await apiClient.delete(`/concepts/${id}`);
        } catch (error) {
            console.error(`Error deleting concept ${id}:`, error);
            throw error;
        }
    }
};
```

#### Type Structure
- Centralize types in `types/` folder within each feature
- Use **type** for unions and simple types
- Use **interface** for extensible objects
- Separate API, component, and store types

```typescript
// features/concepts/types/index.ts
/**
 * Types for Payroll Concepts feature
 */

/**
 * Domain model for a payroll concept
 */
export type ConceptPayroll = {
    id: number;
    name: string;
    type: 'Income' | 'Deduction' | 'Informational';
    description?: string;
    active: boolean;
    createdAt: string;
    updatedAt: string;
};

/**
 * DTO for creating a concept
 */
export type CreateConceptDTO = Omit<ConceptPayroll, 'id' | 'createdAt' | 'updatedAt'>;

/**
 * ConceptCard component props
 */
export type ConceptCardProps = {
    concept: ConceptPayroll;
    onEdit?: (concept: ConceptPayroll) => void;
    onDelete?: (id: number) => void;
};

/**
 * Concepts store state
 */
export type ConceptStoreState = {
    concepts: ConceptPayroll[];
    selectedConcept: ConceptPayroll | null;
    setSelectedConcept: (concept: ConceptPayroll | null) => void;
    updateConceptInStore: (concept: ConceptPayroll) => void;
};
```

## UI/UX Standards

### Tailwind CSS Integration
- Usar **Tailwind CSS** para todos los estilos
- Implementar **utility-first CSS** approach
- Seguir **sistema de diseño consistente** (espaciado, colores, tipografía)
- Mantener **grid responsivo** (mobile-first)
- Usar **clases condicionales** para estados dinámicos

```tsx
// ✅ Correcto: Uso de Tailwind CSS
type ConceptoCardProps = {
    concepto: ConceptoNomina;
    isSelected?: boolean;
};

const ConceptoCard: React.FC<ConceptoCardProps> = ({ concepto, isSelected = false }) => {
    return (
        <div
            className={`
                p-4 rounded-lg border-2 cursor-pointer
                transition-all duration-200
                ${isSelected 
                    ? 'border-blue-500 bg-blue-50 shadow-md' 
                    : 'border-gray-200 hover:border-blue-300 hover:shadow-sm'
                }
            `}
        >
            <h3 className="text-lg font-semibold text-gray-900">
                {concepto.nombre}
            </h3>
            <p className="mt-1 text-sm text-gray-600">
                {concepto.tipo}
            </p>
            {concepto.descripcion && (
                <p className="mt-2 text-sm text-gray-700">
                    {concepto.descripcion}
                </p>
            )}
        </div>
    );
};

// ❌ Evitar: Estilos en línea o CSS tradicional
<div style={{
    padding: '16px',
    borderRadius: '8px',
    border: isSelected ? '2px solid #3b82f6' : '2px solid #e5e7eb'
}}>
    {/* Contenido */}
</div>
```

### Form Handling
- Use **controlled components** with onChange handlers
- Implement **real-time validation** when appropriate
- **Disable buttons** during submission
- **Clear form state** after successful submission
- Show **clear and specific error messages**

```typescript
type ConceptoFormProps = {
    onSubmit: (data: CreateConceptoDTO) => Promise<void>;
};

const ConceptoForm: React.FC<ConceptoFormProps> = ({ onSubmit }) => {
    const [formData, setFormData] = useState<CreateConceptoDTO>({
        nombre: '',
        tipo: 'Ingreso',
        descripcion: '',
        activo: true,
    });
    const [errors, setErrors] = useState<Record<string, string>>({});
    const [isSubmitting, setIsSubmitting] = useState(false);

    const validateForm = (): boolean => {
        const newErrors: Record<string, string> = {};
        
        if (!formData.name.trim()) {
            newErrors.name = 'Name is required';
        }
        if (formData.name.length < 3) {
            newErrors.name = 'Name must be at least 3 characters';
        }
        
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const { name, value, type } = e.target;
        const finalValue = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;
        
        setFormData(prev => ({
            ...prev,
            [name]: finalValue
        }));
        
        // Clear error for this field
        if (errors[name]) {
            setErrors(prev => ({
                ...prev,
                [name]: ''
            }));
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        
        if (!validateForm()) return;
        
        try {
            setIsSubmitting(true);
            await onSubmit(formData);
            // Clear form
            setFormData({
                name: '',
                type: 'Income',
                description: '',
                active: true,
            });
        } catch (error) {
            setErrors({ submit: 'Error saving. Please try again.' });
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            {errors.submit && (
                <div className="p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
                    {errors.submit}
                </div>
            )}
            
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                    Name *
                </label>
                <input
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleInputChange}
                    className={`
                        w-full px-3 py-2 border rounded-md text-sm
                        focus:outline-none focus:ring-2 focus:ring-blue-500
                        ${errors.name ? 'border-red-500' : 'border-gray-300'}
                    `}
                    placeholder="Ex: Base Salary"
                />
                {errors.name && (
                    <p className="mt-1 text-sm text-red-600">{errors.name}</p>
                )}
            </div>

            <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                    Type *
                </label>
                <select
                    name="type"
                    value={formData.type}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                    <option value="Income">Income</option>
                    <option value="Deduction">Deduction</option>
                    <option value="Informational">Informational</option>
                </select>
            </div>

            <button
                type="submit"
                disabled={isSubmitting}
                className={`
                    w-full px-4 py-2 rounded-md font-medium text-white
                    transition-colors duration-200
                    ${isSubmitting
                        ? 'bg-gray-400 cursor-not-allowed'
                        : 'bg-blue-600 hover:bg-blue-700 active:bg-blue-800'
                    }
                `}
            >
                {isSubmitting ? 'Saving...' : 'Save'}
            </button>
        </form>
    );
};
```

### Navigation Patterns
- Use **React Router** for all navigation
- Implement **breadcrumbs** for context
- Use **programmatic navigation** with `useNavigate`
- Handle loading states in transitions

```typescript
import { useNavigate } from 'react-router-dom';

const ConceptsList: React.FC = () => {
    const navigate = useNavigate();
    
    const handleViewConcept = (id: number) => {
        navigate(`/concepts/${id}`);
    };
    
    const handleBack = () => {
        navigate(-1);
    };
    
    return (
        <div>
            <button onClick={handleBack} className="text-blue-600 hover:underline">
                ← Back
            </button>
            {/* Content */}
        </div>
    );
};
```

### Accessibility
- Include **aria-*** attributes when necessary
- Use **semantic HTML** elements (button, input, form, etc.)
- Ensure **keyboard navigation** in interactive components
- Maintain **proper color contrast** (WCAG AA minimum)
- Provide **alternative text** for images

```tsx
// ✅ Correct: Accessible
<button
    onClick={handleDelete}
    aria-label="Delete payroll concept"
    className="p-2 text-red-600 hover:bg-red-50 rounded"
>
    <TrashIcon />
</button>

<input
    type="search"
    placeholder="Search concepts"
    aria-label="Search concepts by name or type"
    className="w-full px-3 py-2 border border-gray-300 rounded-md"
/>

// ❌ Avoid: Not accessible
<div onClick={handleDelete} className="cursor-pointer">
    🗑️
</div>
```

## Testing Standards

### End-to-End Testing with Cypress
- **Test user workflows** rather than implementation details
- Use **data-testid** attributes for reliable element selection
- **Organize tests by feature** (candidates.cy.ts, positions.cy.ts)
- **Include API testing** alongside UI testing

```typescript
describe('Positions API - Update', () => {
    beforeEach(() => {
        cy.window().then((win) => {
            win.localStorage.clear();
        });
    });

    it('should update a position successfully', () => {
        const updateData = {
            title: 'Updated Test Position',
            status: 'Open'
        };

        cy.request({
            method: 'PUT',
            url: `${API_URL}/positions/${testPositionId}`,
            body: updateData
        }).then((response) => {
            expect(response.status).to.eq(200);
            expect(response.body.data.title).to.eq(updateData.title);
        });
    });
});
```

### Test Organization
- **Group related tests** with describe blocks
- **Use descriptive test names** that explain the expected behavior
- **Test both success and error scenarios**
- **Include edge cases** and validation testing


## Configuration Standards

### TypeScript Configuration
The project is configured with **strict mode enabled** for maximum type safety:

```json
// tsconfig.app.json
{
    "compilerOptions": {
        "strict": true,
        "target": "ES2020",
        "module": "ESNext",
        "lib": ["ES2020", "DOM", "DOM.Iterable"],
        "jsx": "react-jsx",
        "baseUrl": ".",
        "paths": {
            "@core/*": ["src/core/*"],
            "@features/*": ["src/features/*"],
            "@ui/*": ["src/common/ui/*"],
            "@common/*": ["src/common/*"]
        },
        "declaration": true,
        "declarationMap": true,
        "sourceMap": true,
        "noImplicitAny": true,
        "strictNullChecks": true,
        "strictFunctionTypes": true,
        "noImplicitThis": true,
        "alwaysStrict": true,
        "noUnusedLocals": true,
        "noUnusedParameters": true,
        "noImplicitReturns": true,
        "noFallthroughCasesInSwitch": true
    }
}
```
### Path Aliases
Use path aliases for cleaner imports:

```typescript
// Instead of:
import { UserCard } from '../../../common/ui/components/UserCard';
import { authService } from '../../../core/services/authService';

// Use:
import { UserCard } from '@ui/components/UserCard';
import { authService } from '@core/services/authService';
```

### ESLint Configuration
The project uses **ESLint 9** with modern configuration:

```javascript
// eslint.config.js - Includes:
// - @eslint/js - Recommended JavaScript rules
// - typescript-eslint - TypeScript linting rules
// - react-hooks - React Hooks rules validation
// - react-refresh - Support for react-refresh in Vite

// Main rules:
// - Strict type checking
// - React Hooks best practices
// - No unused imports or variables
// - Consistent naming and formatting
```

### Environment Configuration
Use **environment variables** for environment-specific configuration:

```env
# .env.development
VITE_API_URL=http://localhost:3010
VITE_ENABLE_DEVTOOLS=true

# .env.production
VITE_API_URL=https://api.production.com
VITE_ENABLE_DEVTOOLS=false
```

```typescript
// core/shared/constants/apiConfig.ts
export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3010';
export const ENABLE_DEVTOOLS = import.meta.env.VITE_ENABLE_DEVTOOLS === 'true';
```
### Vite Configuration
Located in `vite.config.ts`:

- **Port**: 3000 (development server)
- **API Proxy**: Routes `/api/*` to backend (http://localhost:8080)
- **Path Aliases**: Matches TypeScript aliases for imports
- **Plugins**: React fast refresh enabled

```typescript
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@core': path.resolve(__dirname, './src/core'),
      '@features': path.resolve(__dirname, './src/features'),
      '@ui': path.resolve(__dirname, './src/common/ui'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

## Performance Best Practices

### Component Optimization
- **Lazy load** heavy components with React.lazy and Suspense
- Use **React.memo()** to prevent unnecessary re-renders
- Use **useMemo** for expensive calculations
- Use **useCallback** for functions passed as props
- Avoid **list re-renders** by providing unique keys

```typescript
// ✅ Lazy loading routes
import { lazy, Suspense } from 'react';
import { LoadingSpinner } from '@common/ui';

const ConceptPage = lazy(() => import('@features/concepts/pages/ConceptPage'));
const DashboardPage = lazy(() => import('@features/dashboard/pages/DashboardPage'));

const routes = [
    {
        path: '/concepts',
        element: (
            <Suspense fallback={<LoadingSpinner />}>
                <ConceptPage />
            </Suspense>
        ),
    },
];

// ✅ Memoization of expensive components
interface ConceptListProps {
    concepts: ConceptPayroll[];
    onSelect: (id: number) => void;
}

const ConceptList = React.memo<ConceptListProps>(({ concepts, onSelect }) => {
    return (
        <div>
            {concepts.map((concept) => (
                <ConceptCard 
                    key={concept.id} 
                    concept={concept}
                    onSelect={onSelect}
                />
            ))}
        </div>
    );
});

// ✅ useMemo for processed data
const Dashboard: React.FC = () => {
    const { data: concepts } = useConcepts();
    
    // Only recalculate when concepts change
    const conceptsByType = useMemo(() => {
        return concepts?.reduce((acc, concept) => {
            if (!acc[concept.type]) {
                acc[concept.type] = [];
            }
            acc[concept.type].push(concept);
            return acc;
        }, {} as Record<string, ConceptPayroll[]>) || {};
    }, [concepts]);
    
    return (
        <div>
            {Object.entries(conceptsByType).map(([type, items]) => (
                <div key={type}>
                    <h3>{type}</h3>
                    <ConceptList concepts={items} />
                </div>
            ))}
        </div>
    );
};

// ✅ useCallback for callback props
const ConceptForm: React.FC<ConceptFormProps> = ({ onSubmit }) => {
    const handleSubmit = useCallback(
        async (data: CreateConceptDTO) => {
            await onSubmit(data);
        },
        [onSubmit]
    );
    
    return <form onSubmit={handleSubmit}>{/* JSX */}</form>;
};
```

### Bundle Optimization
- **Code splitting** automatic at main routes
- **Tree shaking** enabled to remove unused code
- **Minification** in production build
- **Lazy image loading**
- Use **dynamic imports** for large modules

```typescript
// Dynamic import for modal with heavy logic
const ConceptModal = lazy(() => import('./ConceptModal'));

// Usage in component
{showModal && (
    <Suspense fallback={null}>
        <ConceptModal onClose={() => setShowModal(false)} />
    </Suspense>
)}
```

### API Efficiency
- **Implement caching** with React Query
- **Deduplicate requests** automatically
- **Invalidate cache selectively** after mutations
- **Use polling with interval** when necessary
- **Limit results** with pagination

```typescript
// React Query configuration with staleTime
export const useConcepts = (pageSize = 20) => {
    return useQuery({
        queryKey: ['concepts'],
        queryFn: () => conceptService.getAllConcepts(),
        staleTime: 5 * 60 * 1000, // 5 minutes
        gcTime: 10 * 60 * 1000,   // 10 minutes
    });
};

// Selective refetch after update
const { mutate } = useMutation({
    mutationFn: (data) => conceptService.updateConcept(data),
    onSuccess: () => {
        queryClient.invalidateQueries({
            queryKey: ['concepts'],
            exact: false,
        });
    },
});
```

## Development Workflow

- **Feature Branches**: Develop features in separate branches, adding descriptive suffix "-frontend" to allow working in parallel and avoid conflicts or collisions
- **Descriptive Commits**: Write descriptive commit messages in English
- **Code Review**: Code review before merging
- **Small Branches**: Keep branches small and focused

### Development Scripts
```bash
npm start          # Development server
npm test           # Run unit tests
npm run build      # Production build
npm run cypress:open    # Open Cypress test runner
npm run cypress:run     # Run Cypress tests headlessly
```

### Code Quality
- **ESLint validation** before commits
- **TypeScript compilation** without errors
- **All tests passing** before deployment
- **Performance monitoring** with Web Vitals

## Migration Strategy

### TypeScript Migration (Gradual)
- **New components in TypeScript** by default
- **Keep JavaScript** for existing stable components
- **Add types incrementally** to existing code
- **Don't migrate everything at once**

```typescript
// ✅ New component: TypeScript from the start
const NewConceptForm: React.FC<ConceptFormProps> = ({ onSubmit }) => {
    // Typed implementation
};

// Legacy component - migrate gradually
const LegacyComponent = ({ data }) => {
    // Migrate in the future when refactored
};
```

### Component Modernization
- **Functional components** instead of class components
- **Hooks** instead of lifecycle methods
- **Tailwind CSS** instead of CSS modules or styled-components
- **React Router v6+** for modern routing

This document should be consulted regularly during development to ensure consistency and quality throughout the application.
