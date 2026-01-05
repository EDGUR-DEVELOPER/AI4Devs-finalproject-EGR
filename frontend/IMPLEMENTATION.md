# Sistema de Autenticación JWT - Frontend

## 📋 Resumen

Implementación completa del sistema de autenticación JWT para el frontend, cumpliendo con los requisitos de **US-AUTH-002** y extensiones adicionales.

## ✅ Componentes Implementados

### 1. **Tipos TypeScript** (`src/core/domain/`)

#### Auth Types ([types.ts](src/core/domain/auth/types.ts))
- `JwtPayload`: Estructura del token JWT del backend
- `UserContext`: Contexto de usuario extraído del token
- `LoginResponse`: Respuesta de la API de login
- `LogoutReason`: Razones de cierre de sesión

#### Notification Types ([types.ts](src/core/domain/notifications/types.ts))
- `Notification`: Estructura de notificación
- `NotificationType`: Tipos de notificación (success, error, warning, info)

---

### 2. **Utilidades JWT** ([jwt.ts](src/core/shared/utils/jwt.ts))

#### `getUserContextFromToken(token: string | null): UserContext`
**✅ Requisito US-AUTH-002**

Función principal que decodifica el token JWT y extrae:
- `userId` (del claim `sub`)
- `organizacionId` (del claim `organizacion_id`)
- `roles` (array de códigos de rol)
- `isAuthenticated` (estado de autenticación)

**Características:**
- Manejo de errores con try/catch
- Validación de expiración automática
- Limpieza de localStorage en caso de token inválido
- Retorna contexto vacío si el token es null o inválido

#### `isTokenExpired(decoded: JwtPayload): boolean`
Verifica si un token ha expirado comparando `exp` con el timestamp actual.

---

### 3. **Store de Autenticación** ([useAuthStore.ts](src/features/auth/hooks/useAuthStore.ts))

**✅ Requisito US-AUTH-002: Estado global con info del token**

Store Zustand con persistencia en localStorage que implementa:

#### Estado
- `token`: JWT token string
- `userId`, `organizacionId`, `roles`: Información extraída del token
- `isAuthenticated`: Flag de autenticación
- `isLoggingOut`: Flag para prevenir race conditions

#### Acciones
- `setToken(token)`: Decodifica y valida el token, actualiza el estado
- `logout(reason)`: Cierra sesión, limpia estado, notifica al usuario
- `checkTokenExpiration()`: Verifica expiración del token

#### Características Avanzadas
✅ **Middleware `persist`**: Persistencia automática en localStorage con key `'auth-storage'`
✅ **Validación en hidratación**: Verifica expiración al cargar desde localStorage
✅ **Sincronización entre pestañas**: Listener del evento `storage` que propaga logout
✅ **Prevención de race conditions**: Flag `isLoggingOut` para evitar múltiples logout simultáneos

---

### 4. **Sistema de Notificaciones**

#### Store ([useNotificationStore.ts](src/common/ui/notifications/useNotificationStore.ts))
- Gestión de notificaciones con auto-dismiss en 5 segundos
- Métodos: `showNotification(message, type)`, `dismissNotification(id)`

#### Componentes UI
- **Toast** ([Toast.tsx](src/common/ui/notifications/Toast.tsx)): Notificación individual con estilos por tipo
- **ToastContainer** ([ToastContainer.tsx](src/common/ui/notifications/ToastContainer.tsx)): Contenedor fijo en esquina superior derecha

#### Estilos
Animación `slide-in` agregada en [index.css](src/index.css):
```css
@keyframes slide-in {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}
```

---

### 5. **Instancia Axios con Interceptores** ([axiosInstance.ts](src/core/shared/api/axiosInstance.ts))

#### Request Interceptor
- Agrega automáticamente `Authorization: Bearer ${token}` a todas las peticiones
- Lee el token desde localStorage

#### Response Interceptor
✅ **Manejo de 401 Unauthorized**
- Detecta respuestas 401 del backend
- Ejecuta `logout('unauthorized')` automáticamente
- Usa lazy import para evitar dependencias circulares

---

### 6. **Servicio de API** ([authApi.ts](src/features/auth/api/authApi.ts))

#### Métodos
- `login(email, password)`: Login con credenciales
- `switchOrganization(organizacionId)`: Cambio de contexto organizacional

**Nota:** El backend espera el campo `contrasena` (no `password`).

---

### 7. **Hook Personalizado `useAuth`** ([useAuth.ts](src/features/auth/hooks/useAuth.ts))

**✅ Requisito US-AUTH-002: Hook que expone user, organizationId y roles**

#### API Pública
```typescript
const {
  isAuthenticated,     // boolean
  userId,              // string
  organizacionId,      // number
  roles,               // string[]
  token,               // string | null
  login,               // (email, password) => Promise
  logout,              // () => void
  switchOrganization   // (orgId) => Promise
} = useAuth();
```

#### Características
- Encapsula la lógica del store
- Manejo de errores con notificaciones
- Mensajes de éxito/error automáticos

---

### 8. **Constantes y Mensajes** ([messages.ts](src/features/auth/constants/messages.ts))

#### `LOGOUT_MESSAGES`
Objeto mapeando `LogoutReason` a mensajes en español:
- `manual`: "Sesión cerrada correctamente"
- `expired`: "Tu sesión ha expirado. Por favor, inicia sesión nuevamente"
- `unauthorized`: "Tu sesión ha sido invalidada. Por favor, inicia sesión nuevamente"

**Estructura preparada para i18n futuro.**

#### `API_ENDPOINTS`
```typescript
const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/iam/auth/login',
    SWITCH: '/iam/auth/switch',
  },
};
```

---

### 9. **Integración en App.tsx** ([App.tsx](src/App.tsx))

#### Componente `AuthLogoutListener`
✅ **Listener de eventos custom `auth:logout`**
- Escucha eventos emitidos por el store al hacer logout
- Redirige a `/login` usando React Router
- Desacopla la lógica de navegación de los stores

#### Renderizado
```tsx
<BrowserRouter>
  <AuthLogoutListener />
  <AppRouter />
  <ToastContainer />
</BrowserRouter>
```

---

### 10. **Router** ([AppRouter.tsx](src/core/shared/router/AppRouter.tsx))

Router básico con rutas:
- `/` → Redirige a `/login`
- `/login` → Página de login (placeholder)
- `/dashboard` → Dashboard (placeholder)
- `*` → 404

---

### 11. **Barrel Exports**

#### Auth Feature ([index.ts](src/features/auth/index.ts))
```typescript
export { useAuth } from './hooks/useAuth';
export { getUserContextFromToken } from '@core/shared/utils/jwt';
export type { UserContext, LoginResponse, LogoutReason };
```

#### UI Components ([index.ts](src/common/ui/index.ts))
```typescript
export { ToastContainer, Toast, useNotificationStore };
export type { Notification, NotificationType };
```

---

## 🎯 Requisitos Cumplidos (US-AUTH-002)

### Base de Datos ✅
- Query de contexto de usuario y roles (backend)

### Backend ✅
- Interfaz JwtPayload definida
- Servicio de generación de tokens (backend)
- Integración en flujo de login (backend)

### Frontend ✅
1. **Función `getUserContextFromToken()`** ✅
   - Implementada en [jwt.ts](src/core/shared/utils/jwt.ts)
   - Decodifica token JWT
   - Extrae `userId`, `organizacionId`, `roles`
   - Manejo robusto de errores

2. **Estado global actualizado con info del token** ✅
   - Store Zustand en [useAuthStore.ts](src/features/auth/hooks/useAuthStore.ts)
   - Persistencia automática con `persist` middleware
   - Sincronización entre pestañas
   - Hook público `useAuth` para componentes

---

## 🚀 Características Adicionales Implementadas

### ✅ Validación de Expiración
- Logout automático cuando el token expira
- Verificación en cada operación crítica
- Limpieza de localStorage en tokens inválidos

### ✅ Sincronización entre Pestañas
- Listener del evento `storage`
- Logout propagado a todas las pestañas abiertas
- Prevención de race conditions con flag `isLoggingOut`

### ✅ Interceptor Axios 401
- Logout automático en respuestas 401
- Token invalidado por el backend
- Notificación al usuario

### ✅ Sistema de Notificaciones
- Toast notifications con Tailwind CSS
- Auto-dismiss en 5 segundos
- Tipos: success, error, warning, info
- Mensajes contextuales por razón de logout

### ✅ Arquitectura Desacoplada
- Eventos custom para comunicación entre capas
- Router desacoplado de la lógica de autenticación
- Barrel exports para APIs públicas limpias

---

## 📦 Dependencias Instaladas

```json
{
  "jwt-decode": "^4.0.0"  // Decodificación de JWT
}
```

---

## 🔧 Configuración del Proyecto

### Alias de Paths (tsconfig.json)
```json
{
  "@core/*": ["src/core/*"],
  "@features/*": ["src/features/*"],
  "@ui/*": ["src/common/ui/*"]
}
```

### Proxy de Vite (vite.config.ts)
```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // Gateway
      changeOrigin: true
    }
  }
}
```

---

## 📝 Uso del Hook `useAuth`

### Ejemplo básico
```tsx
import { useAuth } from '@features/auth';

function MyComponent() {
  const { 
    isAuthenticated, 
    userId, 
    organizacionId, 
    roles,
    login,
    logout 
  } = useAuth();

  if (!isAuthenticated) {
    return <LoginForm onLogin={login} />;
  }

  return (
    <div>
      <p>Usuario: {userId}</p>
      <p>Organización: {organizacionId}</p>
      <p>Roles: {roles.join(', ')}</p>
      <button onClick={logout}>Cerrar Sesión</button>
    </div>
  );
}
```

### Login
```tsx
const { login } = useAuth();

const handleLogin = async (email: string, password: string) => {
  try {
    await login(email, password);
    // Redirigir al dashboard
  } catch (error) {
    // Error ya manejado con notificación
  }
};
```

### Verificación de Roles
```tsx
const { roles } = useAuth();

const canEditDocuments = roles.includes('EDITOR');
const isAdmin = roles.includes('ADMIN');
```

---

## 🧪 Testing

### Build Exitoso ✅
```bash
npm run build
# ✓ 49 modules transformed
# ✓ built in 6.95s
```

### Sin Errores de Tipos ✅
```bash
tsc -b
# No errors found
```

---

## 🔐 Seguridad

### Prácticas Implementadas
- Token almacenado solo en localStorage (no en cookies HTTP-only por decisión de arquitectura)
- Validación de expiración en cada operación
- Limpieza automática de tokens inválidos
- No se envía token a rutas no autenticadas
- Response interceptor para manejar tokens revocados

### Consideraciones Futuras
- Migrar a HTTP-only cookies para mayor seguridad
- Implementar refresh token (endpoint `/auth/refresh`)
- Rate limiting en intentos de login
- CSRF protection

---

## 📚 Arquitectura

### Estructura de Carpetas Creada
```
frontend/src/
├── core/
│   ├── domain/
│   │   ├── auth/
│   │   │   └── types.ts                    ✅ Interfaces Auth
│   │   └── notifications/
│   │       └── types.ts                    ✅ Interfaces Notificaciones
│   └── shared/
│       ├── api/
│       │   └── axiosInstance.ts            ✅ Cliente HTTP
│       ├── router/
│       │   ├── AppRouter.tsx               ✅ Router
│       │   └── index.ts                    ✅ Barrel export
│       └── utils/
│           └── jwt.ts                      ✅ Helpers JWT
│
├── features/
│   └── auth/
│       ├── api/
│       │   └── authApi.ts                  ✅ Servicio API
│       ├── hooks/
│       │   ├── useAuthStore.ts             ✅ Store Zustand
│       │   └── useAuth.ts                  ✅ Hook público
│       ├── constants/
│       │   └── messages.ts                 ✅ Constantes
│       └── index.ts                        ✅ Barrel export
│
├── common/
│   └── ui/
│       ├── notifications/
│       │   ├── useNotificationStore.ts     ✅ Store notificaciones
│       │   ├── Toast.tsx                   ✅ Componente Toast
│       │   └── ToastContainer.tsx          ✅ Contenedor
│       └── index.ts                        ✅ Barrel export
│
├── App.tsx                                 ✅ Root + Listener
├── main.tsx                                (sin cambios)
└── index.css                               ✅ Animaciones Toast
```

---

## 🎨 Estilos (Tailwind CSS)

### Colores por Tipo de Notificación
- **Success**: `bg-green-500 text-white`
- **Error**: `bg-red-500 text-white`
- **Warning**: `bg-yellow-500 text-white`
- **Info**: `bg-blue-500 text-white`

### Animación Slide-In
Duración: 0.3s con `ease-out`

---

## 🚧 Próximos Pasos (Backlog)

### Fase Posterior
- [ ] Crear componente LoginForm
- [ ] Crear componente Dashboard
- [ ] Implementar rutas protegidas (PrivateRoute)

---

## 📄 Documentación Relacionada

- [US-AUTH-002.md](../../US/tickets/P0-Autenticacion/US-AUTH-002.md) - Historia de usuario
- [README.md](README.md) - Documentación general del frontend

---

## ✍️ Autor

Implementado siguiendo los estándares de:
- TypeScript estricto (sin `any`)
- Feature-driven architecture
- Clean code principles
- Mobile-first responsive design

**Fecha:** 5 de enero de 2026
