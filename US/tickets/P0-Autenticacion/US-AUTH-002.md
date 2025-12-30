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
