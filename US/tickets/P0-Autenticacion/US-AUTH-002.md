## P0 — Autenticación + Organizacion

### [US-AUTH-002] Token con claims de organizacion y roles
---
#### Base de datos
---
* **Título:** Query de contexto de usuario y roles
* **Objetivo:** Obtener eficientemente los roles de un usuario filtrados por la organización en la que está haciendo login.
* **Tipo:** Tarea
* **Descripción corta:** Crear una consulta o método de repositorio que, dado un `usuario_id` y un `organizacion_id`, devuelva la lista de códigos de roles activos. Debe manejar el caso donde el usuario existe pero no tiene roles en esa organización.
* **Entregables:**
    - Método en repositorio (ej. `findRolesByUserIdAndOrgId`).
    - Test de integración de base de datos verificando la recuperación correcta.
---
#### Backend
---
* **Título:** Definición de interfaz de Payload y Claims
* **Objetivo:** Estandarizar la estructura del token para todo el sistema.
* **Tipo:** Diseño / Tarea
* **Descripción corta:** Definir la interfaz o clase DTO que represente el contenido del token. Debe incluir los campos estándar (`sub`, `iat`, `exp`) y los personalizados (`org_id`, `roles`).
* **Entregables:**
    - Interface/Type `JwtPayload` (ej. `{ uid: string, org: string, roles: string[] }`).
    - Constantes para los nombres de los claims (evitar "magic strings").
---
* **Título:** Implementación del Servicio de Generación de Tokens (JWT)
* **Objetivo:** Lógica central para crear y firmar el token.
* **Tipo:** Historia / Tarea
* **Descripción corta:** Implementar un servicio que reciba el objeto usuario y la organización de contexto, mapee los datos al `JwtPayload` definido, y genere el string firmado usando una librería JWT estándar.
* **Entregables:**
    - Clase/Servicio `TokenService` con método `generateToken(user, orgId, roles)`.
    - Configuración de variables de entorno para `JWT_SECRET` y `JWT_EXPIRATION`.
---
* **Título:** Integración en flujo de Login
* **Objetivo:** Conectar el servicio de login (US-AUTH-001) con el generador de tokens.
* **Tipo:** Tarea
* **Descripción corta:** Modificar el controlador/servicio de Login existente. Una vez validadas las credenciales, invocar al repositorio para obtener roles y luego al `TokenService` para devolver el token enriquecido en la respuesta HTTP.
* **Entregables:**
    - Endpoint `/auth/login` devolviendo el token con los nuevos claims.
    - Endpoint `/auth/switch` (si aplica en esta fase) devolviendo nuevo token con claims actualizados.
---
* **Título:** Tests Unitarios de Claims
* **Objetivo:** Asegurar que el token siempre tenga la estructura esperada.
* **Tipo:** QA / Tarea
* **Descripción corta:** Crear pruebas unitarias específicas que decodifiquen un token generado y aserten que `org_id` y `roles` están presentes y son correctos.
* **Entregables:**
    - Suite de tests unitarios para `TokenService`.
---
#### Frontend
---
* **Título:** Utilidad de decodificación de Token
* **Objetivo:** Permitir al frontend leer los claims sin llamar al backend.
* **Tipo:** Tarea
* **Descripción corta:** Implementar o configurar una utilidad (ej. usando `jwt-decode`) para parsear el token almacenado. Crear un hook o servicio de autenticación que exponga `user`, `organizationId` y `roles` al resto de la app.
* **Entregables:**
    - Función `getUserContextFromToken()`.
    - Estado global (Context/Store) actualizado con la info del token.
---
* **Título:** Sincronización de sesión entre pestañas
* **Objetivo:** Garantizar que el logout se propague a todas las pestañas abiertas.
* **Tipo:** Tarea
* **Descripción corta:** Implementar listener del evento `storage` en el store de Zustand para detectar cambios en localStorage y sincronizar el estado de autenticación entre múltiples pestañas del navegador.
* **Entregables:**
    - Listener `storage` event en `useAuthStore`.
    - Logout automático en todas las pestañas cuando una cierra sesión.
    - Flag `isLoggingOut` para prevenir race conditions.
---
* **Título:** Interceptor Axios para manejo de 401 Unauthorized
* **Objetivo:** Invalidar sesión automáticamente cuando el backend rechaza el token.
* **Tipo:** Tarea
* **Descripción corta:** Configurar interceptor de respuesta en la instancia de Axios que detecte códigos 401, ejecute el logout automático con razón 'unauthorized' y emita evento custom para redirección.
* **Entregables:**
    - Response interceptor en `axiosInstance.ts`.
    - Integración con `useAuthStore.logout('unauthorized')`.
    - Evento custom `auth:logout` con payload de razón.
---
* **Título:** Sistema de notificaciones globales
* **Objetivo:** Informar al usuario sobre eventos de sesión (expiración, logout forzado).
* **Tipo:** Tarea
* **Descripción corta:** Implementar store de notificaciones con Zustand y componente `ToastContainer` que muestre mensajes contextuales cuando la sesión expira o es invalidada por el backend.
* **Entregables:**
    - Store `useNotificationStore` con método `showNotification(message, type)`.
    - Componente UI `<Toast />` en `src/common/ui/`.
    - Integración con `logout(reason)` para mostrar mensaje apropiado.
    - Constantes de mensajes mapeados por `LogoutReason` para facilitar futura i18n.
---
* **Título:** Listener de eventos de logout en App
* **Objetivo:** Centralizar la lógica de redirección post-logout.
* **Tipo:** Tarea
* **Descripción corta:** Implementar listener del evento custom `auth:logout` en el componente raíz (`App.tsx`) que maneje la navegación a `/login` usando React Router, desacoplando la lógica de routing de servicios y stores.
* **Entregables:**
    - Event listener en `App.tsx` o layout principal.
    - Redirección con `useNavigate()` de React Router.
---
#### Backlog / Fase Posterior
---
* **TODO:** Implementar refresh automático de token antes de expiración.
* **Descripción:** Agregar lógica para renovar el token X minutos antes de `exp` (requiere endpoint `/auth/refresh` en backend). Evita que el usuario pierda sesión mientras trabaja activamente.
* **Prioridad:** Baja (post-MVP).
