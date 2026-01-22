## P0 — Autenticación + Organización

### [US-AUTH-007] Implementación de Refresh Token
---
**Estado:** 🔴 Pendiente (Post-MVP)  
**Prioridad:** Baja  
**Dependencias:** Requiere implementación backend completa

---

## Contexto

Actualmente el sistema utiliza únicamente tokens JWT de acceso con expiración fija. Cuando el token expira, el usuario debe realizar login nuevamente, lo cual puede interrumpir su flujo de trabajo. La implementación de refresh tokens permitirá renovar automáticamente la sesión del usuario sin requerir re-autenticación, mejorando significativamente la experiencia de usuario.

---

## Objetivos

1. **Backend:** Implementar infraestructura de refresh tokens con almacenamiento persistente y rotación segura
2. **Frontend:** Implementar interceptor para renovación automática de tokens antes de su expiración
3. **Seguridad:** Aplicar mejores prácticas de seguridad (token rotation, detección de reutilización)

---

## Backend

### Tarea 1: Diseño de base de datos para Refresh Tokens
* **Objetivo:** Crear la tabla para almacenar refresh tokens de forma persistente
* **Descripción:** Diseñar schema de BD que soporte refresh tokens con información de auditoría y seguridad
* **Entregables:**
  - Tabla `refresh_tokens` con campos:
    - `id` (PK)
    - `user_id` (FK a users)
    - `token` (hash del refresh token)
    - `expires_at` (timestamp)
    - `family_id` (UUID para token rotation)
    - `is_revoked` (boolean)
    - `created_at`, `used_at`, `revoked_at`
  - Índices en `user_id`, `token`, `family_id`
  - Script de migración JPA/Flyway

---

### Tarea 2: Actualizar DTOs de autenticación
* **Objetivo:** Extender los contratos de API para incluir refresh tokens
* **Descripción:** Modificar DTOs existentes para incluir refresh token en respuestas de login
* **Entregables:**
  - Actualizar `LoginResponse` para incluir:
    ```java
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;        // segundos hasta expiración del access token
    private Long refreshExpiresIn; // segundos hasta expiración del refresh token
    ```
  - Crear `RefreshTokenRequest`:
    ```java
    private String refreshToken;
    ```
  - Crear `RefreshTokenResponse` (idéntico a LoginResponse)

---

### Tarea 3: Implementar RefreshTokenService
* **Objetivo:** Crear servicio centralizado para gestión de refresh tokens
* **Descripción:** Implementar lógica de negocio para generación, validación, rotación y revocación de refresh tokens
* **Entregables:**
  - Método `generateRefreshToken(userId)`: Genera refresh token de larga duración (7-30 días)
  - Método `validateAndRotate(token)`: Valida token, revoca el usado, genera nuevo par access/refresh
  - Método `revokeTokenFamily(familyId)`: Revoca familia completa si detecta reutilización
  - Método `cleanupExpiredTokens()`: Tarea programada para limpieza de tokens expirados
  - Configuración de expiración en `application.yml`:
    ```yaml
    jwt:
      access-token-expiration: 900      # 15 minutos
      refresh-token-expiration: 2592000 # 30 días
    ```

---

### Tarea 4: Crear endpoint POST /api/v1/auth/refresh
* **Objetivo:** Exponer endpoint público para renovación de tokens
* **Descripción:** Implementar controller que recibe refresh token y retorna nuevo par de tokens
* **Entregables:**
  - Endpoint `POST /api/v1/auth/refresh`
  - Request body: `RefreshTokenRequest`
  - Response: `RefreshTokenResponse` (200 OK)
  - Manejo de errores:
    - 401: Token inválido, expirado o revocado
    - 403: Detección de reutilización (revoca familia completa)
  - Documentación OpenAPI/Swagger

---

### Tarea 5: Actualizar endpoint de Login
* **Objetivo:** Incluir refresh token en respuesta de login exitoso
* **Descripción:** Modificar `AuthController.login()` para generar y retornar refresh token
* **Entregables:**
  - `POST /api/v1/auth/login` retorna `accessToken` y `refreshToken`
  - Almacenar refresh token en BD con `family_id` único
  - Actualizar tests de integración

---

### Tarea 6: Implementar Refresh Token Rotation
* **Objetivo:** Aplicar patrón de rotación automática para mayor seguridad
* **Descripción:** Cada vez que se usa un refresh token, se invalida y se genera uno nuevo
* **Entregables:**
  - Al usar refresh token:
    1. Validar token actual
    2. Revocar token actual (marcar `is_revoked = true`)
    3. Generar nuevo refresh token con mismo `family_id`
    4. Retornar nuevo par access/refresh
  - Detección de reutilización:
    - Si se intenta usar un token ya revocado, revocar toda la familia
    - Esto fuerza re-login del usuario si hay compromiso

---

### Tarea 7: Endpoint de revocación de tokens
* **Objetivo:** Permitir logout explícito revocando refresh tokens
* **Descripción:** Crear endpoint para revocar tokens del usuario actual
* **Entregables:**
  - Endpoint `POST /api/v1/auth/revoke`
  - Requiere autenticación (access token válido)
  - Revoca todos los refresh tokens del usuario autenticado
  - Respuesta 204 No Content

---

## Frontend

### Tarea 8: Actualizar tipos TypeScript
* **Objetivo:** Reflejar cambios de backend en tipos frontend
* **Descripción:** Actualizar interfaces de autenticación para incluir refresh token
* **Entregables:**
  - Actualizar `LoginResponse` en `src/core/domain/auth/types.ts`:
    ```typescript
    export interface LoginResponse {
      token: string;           // access token
      refreshToken: string;    // refresh token
      userId: string;
      organizacionId: number;
      organizacionNombre: string;
      roles: string[];
      expiresIn: number;       // segundos
      refreshExpiresIn: number;
    }
    ```
  - Agregar interfaces:
    ```typescript
    export interface RefreshTokenRequest {
      refreshToken: string;
    }
    export interface RefreshTokenResponse extends LoginResponse {}
    ```

---

### Tarea 9: Actualizar AuthStore para manejar Refresh Token
* **Objetivo:** Extender store de autenticación para almacenar y gestionar refresh token
* **Descripción:** Modificar Zustand store para incluir refresh token en estado persistido
* **Entregables:**
  - Agregar `refreshToken` y `tokenExpiresAt` al estado del store
  - Actualizar `login()` para guardar refresh token en localStorage:
    ```typescript
    localStorage.setItem('authToken', response.token);
    localStorage.setItem('refreshToken', response.refreshToken);
    ```
  - Actualizar `logout()` para limpiar refresh token:
    ```typescript
    localStorage.removeItem('refreshToken');
    ```
  - Calcular y almacenar `tokenExpiresAt` basado en `expiresIn`

---

### Tarea 10: Crear servicio de Refresh Token
* **Objetivo:** Encapsular lógica de renovación de token
* **Descripción:** Crear utilidad para llamar al endpoint de refresh
* **Entregables:**
  - Archivo `src/features/auth/api/refreshTokenApi.ts`:
    ```typescript
    export const refreshTokenApi = {
      refresh: async (refreshToken: string): Promise<RefreshTokenResponse> => {
        const { data } = await apiClient.post('/iam/auth/refresh', {
          refreshToken,
        });
        return data;
      },
    };
    ```
  - Nota: Usar `apiClient` directamente (no axios nuevo) para aprovechar interceptores

---

### Tarea 11: Implementar interceptor de Auto-Refresh
* **Objetivo:** Renovar access token automáticamente antes de su expiración
* **Descripción:** Agregar lógica al request interceptor para verificar expiración y renovar si es necesario
* **Entregables:**
  - En `axiosInstance.ts`, modificar request interceptor:
    ```typescript
    apiClient.interceptors.request.use(async (config) => {
      const isPublicEndpoint = PUBLIC_ENDPOINTS.some(...);
      if (isPublicEndpoint) return config;

      const tokenExpiresAt = useAuthStore.getState().tokenExpiresAt;
      const now = Date.now();
      const fiveMinutes = 5 * 60 * 1000;

      // Si el token expira en menos de 5 minutos, renovarlo
      if (tokenExpiresAt && tokenExpiresAt - now < fiveMinutes) {
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
          try {
            const response = await refreshTokenApi.refresh(refreshToken);
            // Actualizar store y localStorage con nuevos tokens
            useAuthStore.getState().updateTokens(response);
          } catch (error) {
            // Si falla refresh, hacer logout
            useAuthStore.getState().logout('expired');
            return Promise.reject(error);
          }
        }
      }

      const token = localStorage.getItem('authToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
    ```
  - Agregar método `updateTokens()` al AuthStore
  - Excluir `/iam/auth/refresh` de la lista de endpoints públicos para evitar ciclos

---

### Tarea 12: Manejo de errores en Refresh Token
* **Objetivo:** Gestionar casos donde refresh token falla o es inválido
* **Descripción:** Actualizar response interceptor para intentar refresh en 401 antes de logout
* **Entregables:**
  - Modificar response interceptor de 401:
    1. Si el endpoint fallido es `/auth/refresh`, hacer logout inmediato
    2. Si no, intentar refresh token una vez
    3. Si refresh exitoso, reintentar request original
    4. Si refresh falla, hacer logout
  - Usar flag global para evitar múltiples intentos de refresh simultáneos
  - Agregar cola de requests pendientes durante refresh

---

### Tarea 13: Actualizar constantes y mensajes
* **Objetivo:** Agregar endpoints y mensajes relacionados con refresh token
* **Descripción:** Extender constantes existentes
* **Entregables:**
  - Actualizar `API_ENDPOINTS` en `messages.ts`:
    ```typescript
    export const API_ENDPOINTS = {
      AUTH: {
        LOGIN: '/iam/auth/login',
        SWITCH: '/iam/auth/switch',
        REFRESH: '/iam/auth/refresh',
        REVOKE: '/iam/auth/revoke',
      },
    } as const;
    ```
  - Agregar mensajes de error específicos para refresh token

---

## QA / Testing

### Tarea 14: Tests de integración Backend
* **Objetivo:** Verificar flujo completo de refresh tokens
* **Descripción:** Crear suite de tests que cubra escenarios happy path y edge cases
* **Entregables:**
  - Test: Login retorna access + refresh token
  - Test: Refresh token válido retorna nuevo par de tokens
  - Test: Refresh token usado es revocado
  - Test: Reutilización de token revoca familia completa
  - Test: Refresh token expirado retorna 401
  - Test: Revoke invalida todos los tokens del usuario
  - Test: Limpieza automática de tokens expirados

---

### Tarea 15: Tests de integración Frontend
* **Objetivo:** Verificar comportamiento de interceptor y auto-refresh
* **Descripción:** Simular escenarios de expiración y renovación de tokens
* **Entregables:**
  - Test: Token próximo a expirar se renueva automáticamente
  - Test: Request se completa exitosamente tras auto-refresh
  - Test: Fallo en refresh trigger logout automático
  - Test: Múltiples requests simultáneos usan mismo refresh
  - Test: Usuario con refresh token expirado es redirigido a login

---

### Tarea 16: Pruebas de seguridad
* **Objetivo:** Validar robustez del sistema contra ataques
* **Descripción:** Ejecutar pruebas de penetración básicas
* **Entregables:**
  - Test: Refresh token robado y reutilizado revoca familia
  - Test: Access token no puede ser usado después de logout
  - Test: Refresh token de un usuario no puede usarse para otro
  - Test: Tokens almacenados están hasheados en BD
  - Reporte de hallazgos de seguridad

---

## Consideraciones de Implementación

### Seguridad
- **Hash de tokens en BD:** Almacenar hash SHA-256 del refresh token, no texto plano
- **Rotation obligatoria:** Cada uso de refresh token genera uno nuevo
- **Detección de reutilización:** Sistema de familias de tokens para detectar compromiso
- **HTTPOnly cookies (opcional):** Considerar almacenar refresh token en cookie HTTPOnly en vez de localStorage para mayor seguridad contra XSS

### Performance
- **Caché de validación:** Considerar cache de tokens válidos para reducir carga en BD
- **Limpieza programada:** Job nocturno para eliminar tokens expirados hace más de 30 días
- **Índices de BD:** Asegurar índices en columnas de búsqueda frecuente

### UX
- **Renovación proactiva:** Renovar 5 minutos antes de expiración (configurable)
- **Indicador visual (opcional):** Mostrar brevemente "Renovando sesión..." si la renovación toma tiempo
- **Logout silencioso:** Si refresh falla, logout sin notificación intrusiva (ya se muestra en interceptor)

---

## Criterios de Aceptación

### Backend
- [ ] Endpoint `/api/v1/auth/refresh` implementado y documentado
- [ ] Refresh tokens almacenados en BD con hash
- [ ] Token rotation funcional: cada refresh genera nuevo token
- [ ] Detección de reutilización revoca familia completa
- [ ] Login retorna access token + refresh token
- [ ] Tests de integración pasan con >80% cobertura

### Frontend
- [ ] AuthStore almacena y gestiona refresh token
- [ ] Interceptor renueva token automáticamente 5 minutos antes de expiración
- [ ] Requests se completan exitosamente tras auto-refresh
- [ ] Logout limpia ambos tokens (access y refresh)
- [ ] Error en refresh trigger logout automático
- [ ] No hay dependencias circulares ni loops infinitos

### Documentación
- [ ] README actualizado con flujo de refresh token
- [ ] Diagramas de secuencia para login y refresh
- [ ] Variables de entorno documentadas (expiración de tokens)
- [ ] Guía de troubleshooting para problemas comunes

---

## Estimación

- **Backend:** 5-7 días
- **Frontend:** 3-4 días
- **Testing & QA:** 2-3 días
- **Total:** 10-14 días

---

## Referencias

- [RFC 6749 - OAuth 2.0 Refresh Token](https://datatracker.ietf.org/doc/html/rfc6749#section-1.5)
- [OWASP - Token Refresh Best Practices](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html#token-sidejacking)
- [Auth0 - Refresh Token Rotation](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)
