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

## 📝 Licencia

Proyecto privado - Todos los derechos reservados
