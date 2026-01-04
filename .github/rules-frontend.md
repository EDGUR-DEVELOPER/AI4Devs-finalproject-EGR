# Reglas de desarrollo Frontend

Estas reglas aplican a la aplicación **React + Vite + TypeScript** ubicada en la carpeta `frontend/`.

## 1. Arquitectura y componentes

- Usar componentes funcionales con TypeScript.
- Organizar el código por **features/módulos**, siguiendo la estructura definida en el proyecto.
- Separar componentes de presentación, lógica de estado y utilidades cuando tenga sentido.
- Reutilizar componentes y evitar duplicación de lógica.

## 2. Estándares de código

- Usar **TypeScript** en todos los archivos de lógica (`.ts` / `.tsx`).
- Preferir tipos explícitos y evitar `any` salvo casos muy justificados.
- Usar nombres descriptivos (`isLoading`, `currentUser`, etc.).
- Mantener una estructura clara en cada archivo: componente principal, subcomponentes, helpers, tipos.
- Usar las reglas de ESLint configuradas en el proyecto (`npm run lint` debe pasar siempre).

## 3. Estado, hooks y side effects

- Usar hooks de React (`useState`, `useEffect`, `useMemo`, etc.) respetando las reglas de hooks.
- Centralizar estado global usando las herramientas definidas en el proyecto (por ejemplo Zustand).
- Evitar lógica compleja en componentes de UI; extraer a hooks personalizados o helpers.

## 4. Estilos y UI

- Usar Tailwind CSS según la configuración del proyecto.
- Respetar el diseño responsivo y la experiencia de usuario definida.
- Mantener consistencia visual en componentes reutilizables.

## 5. Pruebas en frontend

- Definir criterios de aceptación y escenarios antes de implementar nuevas funcionalidades.
- Incluir pruebas de componentes y lógica crítica cuando exista infraestructura de testing (Vitest/Jest, etc.).
- Nombrar tests de forma descriptiva (`should_render_X_when_Y`).

## 6. Referencias

- Índice general de reglas: [RULES.md](./RULES.md)