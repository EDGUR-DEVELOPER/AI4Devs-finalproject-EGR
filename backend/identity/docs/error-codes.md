# Códigos de Error - Identity Service

## Descripción General

Este documento consolida todos los códigos de error retornados por el servicio Identity, específicamente para el endpoint de autenticación `/api/v1/auth/login`. Todas las respuestas de error siguen el estándar **RFC 7807 (ProblemDetail)** implementado nativamente en Spring Boot 3.

---

## Estructura Base (RFC 7807 ProblemDetail)

Todas las respuestas de error siguen esta estructura JSON:

```json
{
  "type": "URI que identifica el tipo de error",
  "title": "Título legible del error",
  "status": 401,
  "detail": "Mensaje descriptivo específico",
  "instance": "URI del endpoint que generó el error",
  "codigo": "CODIGO_ERROR_PARA_UI"
}
```

### Campos Principales

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `type` | string (URI) | URI que identifica el tipo de problema. Útil para documentación técnica. |
| `title` | string | Título corto y legible del error. |
| `status` | number | Código HTTP del error (400, 401, 403, 409). |
| `detail` | string | Descripción detallada del error específico. |
| `instance` | string (URI) | Ruta del endpoint que generó el error (ej: `/api/v1/auth/login`). |
| `codigo` | string | **Código de error para la UI**. Este es el campo clave que el frontend debe usar para identificar el tipo de error y mostrar el mensaje apropiado. |

---

## Códigos de Error del Endpoint `/api/v1/auth/login`

### 1. VALIDATION_ERROR (HTTP 400)

**Código:** `VALIDATION_ERROR`  
**Status HTTP:** `400 Bad Request`  
**Trigger Técnico:** Errores de validación Bean Validation en `LoginRequest` (email vacío, formato inválido, password < 8 caracteres).

#### Ejemplo de Respuesta

```json
{
  "type": "https://docflow.com/errors/validation-error",
  "title": "Error de Validación",
  "status": 400,
  "detail": "Error de validación en los datos de entrada",
  "instance": "/api/v1/auth/login",
  "codigo": "VALIDATION_ERROR",
  "errors": {
    "email": "El email es obligatorio",
    "password": "La contraseña debe tener al menos 8 caracteres"
  }
}
```

#### Campo Adicional: `errors`

Este código de error incluye un campo adicional `errors` (objeto JSON) con la lista de campos inválidos:

```json
"errors": {
  "email": "El email es obligatorio",
  "password": "La contraseña debe tener al menos 8 caracteres"
}
```

#### Recomendaciones UX

- **Mensaje Principal:** "Por favor, corrija los siguientes errores:"
- **Acción:** Mostrar los errores bajo cada campo del formulario
- **Icono:** ⚠️ Advertencia
- **Color:** Amarillo/Naranja
- **Ejemplo de Mensaje:**
  ```
  Por favor, corrija los siguientes errores:
  • Email: El email es obligatorio
  • Contraseña: La contraseña debe tener al menos 8 caracteres
  ```

---

### 2. CREDENCIALES_INVALIDAS (HTTP 401)

**Código:** `CREDENCIALES_INVALIDAS`  
**Status HTTP:** `401 Unauthorized`  
**Trigger Técnico:** 
- Email no existe en la base de datos, O
- Password no coincide con el hash BCrypt almacenado

**Nota de Seguridad (OWASP):** Por razones de seguridad, **no se diferencia** si el email no existe o si la contraseña es incorrecta. Siempre se retorna el mismo mensaje genérico.

#### Ejemplo de Respuesta

```json
{
  "type": "https://docflow.com/errors/credenciales-invalidas",
  "title": "Credenciales Inválidas",
  "status": 401,
  "detail": "Credenciales inválidas",
  "instance": "/api/v1/auth/login",
  "codigo": "CREDENCIALES_INVALIDAS"
}
```

#### Recomendaciones UX

- **Mensaje Principal:** "Email o contraseña incorrectos"
- **Mensaje Secundario (opcional):** "Verifica tus datos e intenta nuevamente"
- **Acción:** Resaltar ambos campos (email y password) en rojo
- **Icono:** 🔒 Candado o ❌ Error
- **Color:** Rojo
- **Ejemplo de Mensaje:**
  ```
  ❌ Email o contraseña incorrectos
  Verifica tus datos e intenta nuevamente
  ```

---

### 3. SIN_ORGANIZACION (HTTP 403)

**Código:** `SIN_ORGANIZACION`  
**Status HTTP:** `403 Forbidden`  
**Trigger Técnico:** Usuario existe y credenciales son válidas, PERO no tiene ninguna organización activa (tabla `usuario_organizacion` vacía o todas con `estado_membresia = INACTIVO`).

#### Ejemplo de Respuesta

```json
{
  "type": "https://docflow.com/errors/sin-organizacion",
  "title": "Sin Organización",
  "status": 403,
  "detail": "El usuario no tiene organizaciones activas",
  "instance": "/api/v1/auth/login",
  "codigo": "SIN_ORGANIZACION"
}
```

#### Recomendaciones UX

- **Mensaje Principal:** "Tu cuenta no tiene organizaciones activas"
- **Mensaje Secundario:** "Contacta al administrador del sistema para obtener acceso"
- **Acción:** Mostrar alerta modal con botón "Cerrar" o "Contactar Soporte"
- **Icono:** ℹ️ Información o 🏢 Edificio
- **Color:** Azul (informativo)
- **Ejemplo de Mensaje:**
  ```
  ℹ️ Tu cuenta no tiene organizaciones activas
  
  Contacta al administrador del sistema para obtener acceso
  a una organización y poder usar la plataforma.
  
  [Contactar Soporte] [Cerrar]
  ```

---

### 4. ORGANIZACION_CONFIG_INVALIDA (HTTP 409)

**Código:** `ORGANIZACION_CONFIG_INVALIDA`  
**Status HTTP:** `409 Conflict`  
**Trigger Técnico:** Usuario tiene **2 o más organizaciones activas** PERO **ninguna tiene `es_predeterminada = true`** en la tabla `usuario_organizacion`.

**Contexto de Negocio:** El sistema requiere que usuarios multi-organización tengan una organización predeterminada para saber a cuál org emitir el token JWT en el login.

#### Ejemplo de Respuesta

```json
{
  "type": "https://docflow.com/errors/organizacion-config-invalida",
  "title": "Configuración de Organización Inválida",
  "status": 409,
  "detail": "Usuario con múltiples organizaciones debe tener una predeterminada",
  "instance": "/api/v1/auth/login",
  "codigo": "ORGANIZACION_CONFIG_INVALIDA"
}
```

#### Recomendaciones UX

- **Mensaje Principal:** "Tu cuenta requiere configuración adicional"
- **Mensaje Secundario:** "Tienes múltiples organizaciones pero no hay una predeterminada. Contacta al administrador para configurarla."
- **Acción:** Mostrar alerta modal con botón "Contactar Administrador"
- **Icono:** ⚙️ Configuración o ⚠️ Advertencia
- **Color:** Naranja (advertencia)
- **Ejemplo de Mensaje:**
  ```
  ⚙️ Tu cuenta requiere configuración adicional
  
  Tienes múltiples organizaciones pero no hay una predeterminada.
  Contacta al administrador para configurarla antes de continuar.
  
  [Contactar Administrador] [Cerrar]
  ```

---

## Tabla Resumen de Códigos de Error

| Código | HTTP | Trigger | Mensaje UX Sugerido | Color |
|--------|------|---------|---------------------|-------|
| `VALIDATION_ERROR` | 400 | Campos inválidos (email vacío, password corto) | "Por favor, corrija los siguientes errores:" | 🟡 Amarillo |
| `CREDENCIALES_INVALIDAS` | 401 | Email no existe O password incorrecta | "Email o contraseña incorrectos" | 🔴 Rojo |
| `SIN_ORGANIZACION` | 403 | Usuario sin organizaciones activas | "Tu cuenta no tiene organizaciones activas" | 🔵 Azul |
| `ORGANIZACION_CONFIG_INVALIDA` | 409 | Usuario con 2+ orgs sin predeterminada | "Tu cuenta requiere configuración adicional" | 🟠 Naranja |

---

## Manejo de Errores en el Frontend

### Estrategia de Implementación

```typescript
// Ejemplo en TypeScript/React
async function handleLogin(email: string, password: string) {
  try {
    const response = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    if (!response.ok) {
      const problemDetail = await response.json();
      handleLoginError(problemDetail);
      return;
    }

    const loginResponse = await response.json();
    // Guardar token y redirigir...
  } catch (error) {
    showErrorToast('Error de conexión. Intenta nuevamente.');
  }
}

function handleLoginError(problemDetail: ProblemDetail) {
  switch (problemDetail.codigo) {
    case 'VALIDATION_ERROR':
      showValidationErrors(problemDetail.errors);
      break;
    case 'CREDENCIALES_INVALIDAS':
      showErrorToast('Email o contraseña incorrectos');
      break;
    case 'SIN_ORGANIZACION':
      showModal({
        title: 'Sin Organización',
        message: 'Tu cuenta no tiene organizaciones activas. Contacta al administrador.',
        actions: [{ label: 'Contactar Soporte', onClick: openSupportChat }]
      });
      break;
    case 'ORGANIZACION_CONFIG_INVALIDA':
      showModal({
        title: 'Configuración Requerida',
        message: 'Tu cuenta requiere configuración. Contacta al administrador.',
        actions: [{ label: 'Contactar Administrador', onClick: openAdminContact }]
      });
      break;
    default:
      showErrorToast('Error inesperado. Intenta nuevamente.');
  }
}
```

---

## Otros Endpoints

### `/api/v1/auth/switch` (Cambio de Organización)

Este endpoint también retorna ProblemDetail con los siguientes códigos:

- `TOKEN_INVALIDO` (401) - Token JWT expirado o inválido
- `ORGANIZACION_NO_ACCESIBLE` (403) - Usuario no pertenece a la org solicitada

---

## Errores de Autenticación JWT (Endpoints Protegidos)

**Ubicación:** Manejado por `JwtAuthenticationEntryPoint.java`  
**Código:** No tiene campo `codigo` personalizado (solo ProblemDetail estándar)  
**Status HTTP:** `401 Unauthorized`

### Ejemplo de Respuesta

```json
{
  "type": "urn:problem-type:auth/unauthorized",
  "title": "No Autenticado",
  "status": 401,
  "detail": "Token JWT inválido, expirado o ausente. Por favor, autentíquese usando /api/v1/auth/login",
  "instance": "/api/v1/protected/endpoint"
}
```

### Recomendaciones UX

- **Acción:** Redirigir automáticamente a la página de login
- **Mensaje (opcional):** "Tu sesión ha expirado. Por favor, inicia sesión nuevamente."

---

## Códigos de Error del Endpoint `/api/v1/admin/users/:userId/roles` (US-ADMIN-002)

### 1. VALIDATION_ERROR (HTTP 400)

**Código:** `VALIDATION_ERROR`  
**Status HTTP:** `400 Bad Request`  
**Trigger Técnico:** Errores de validación Bean Validation en `AssignRoleRequest` (rolId null, no positivo).

#### Ejemplo de Respuesta

```json
{
  "type": "https://docflow.com/errors/validation-error",
  "title": "Error de Validación",
  "status": 400,
  "detail": "Error de validación en los datos de entrada",
  "instance": "/api/v1/admin/users/100/roles",
  "codigo": "VALIDATION_ERROR",
  "errors": {
    "rolId": "El ID del rol es obligatorio"
  }
}
```

#### Recomendaciones UX

- **Mensaje Principal:** "Datos de entrada inválidos"
- **Acción:** Mostrar errores de validación en el formulario
- **Icono:** ⚠️ Advertencia

---

### 2. USUARIO_NO_ENCONTRADO (HTTP 404)

**Código:** `USUARIO_NO_ENCONTRADO`  
**Status HTTP:** `404 Not Found`  
**Trigger Técnico:** 
- Usuario no existe en la base de datos, O
- Usuario está eliminado (soft delete), O
- Usuario no pertenece a la organización del administrador, O
- Usuario no tiene membresía activa en la organización

**Nota de Seguridad (Security by Obscurity):** Por razones de seguridad, **no se diferencia** entre usuario inexistente, eliminado o de otra organización. Siempre se retorna el mismo mensaje genérico para no revelar información sobre usuarios de otras organizaciones.

#### Ejemplo de Respuesta

```json
{
  "type": "https://docflow.com/errors/usuario-no-encontrado",
  "title": "Usuario No Encontrado",
  "status": 404,
  "detail": "Usuario con ID '100' no encontrado",
  "instance": "/api/v1/admin/users/100/roles",
  "codigo": "USUARIO_NO_ENCONTRADO"
}
```

#### Recomendaciones UX

- **Mensaje Principal:** "Usuario no encontrado"
- **Mensaje Secundario:** "El usuario no existe o no pertenece a tu organización"
- **Icono:** 🔍 No encontrado
- **Color:** Gris
- **Acción:** Redirigir a lista de usuarios o permitir reintentar

---

### 3. ROL_NO_ENCONTRADO (HTTP 404)

**Código:** `ROL_NO_ENCONTRADO`  
**Status HTTP:** `404 Not Found`  
**Trigger Técnico:** 
- Rol no existe en la base de datos, O
- Rol está inactivo, O
- Rol custom pertenece a otra organización, O
- Rol custom pertenece a una organización suspendida/archivada

**Nota de Seguridad (Security by Obscurity):** Por razones de seguridad, **no se diferencia** entre rol inexistente, inactivo o de otra organización. Siempre se retorna el mismo mensaje genérico para no revelar información sobre roles de otras organizaciones.

#### Ejemplo de Respuesta

```json
{
  "type": "https://docflow.com/errors/rol-no-encontrado",
  "title": "Rol No Encontrado",
  "status": 404,
  "detail": "Rol con ID '5' no encontrado",
  "instance": "/api/v1/admin/users/100/roles",
  "codigo": "ROL_NO_ENCONTRADO"
}
```

#### Recomendaciones UX

- **Mensaje Principal:** "Rol no encontrado"
- **Mensaje Secundario:** "El rol no existe o no está disponible para tu organización"
- **Icono:** 🔍 No encontrado
- **Color:** Gris
- **Acción:** Mostrar lista de roles disponibles

---

### 4. PERMISO_INSUFICIENTE (HTTP 403)

**Código:** `PERMISO_INSUFICIENTE`  
**Status HTTP:** `403 Forbidden`  
**Trigger Técnico:** Usuario autenticado no tiene rol `ADMIN` o `SUPER_ADMIN` en su organización.

#### Ejemplo de Respuesta

```json
{
  "type": "https://docflow.com/errors/permiso-insuficiente",
  "title": "Permiso Insuficiente",
  "status": 403,
  "detail": "Se requiere rol ADMIN o SUPER_ADMIN para asignar roles",
  "instance": "/api/v1/admin/users/100/roles",
  "codigo": "PERMISO_INSUFICIENTE"
}
```

#### Recomendaciones UX

- **Mensaje Principal:** "No tienes permisos para realizar esta acción"
- **Mensaje Secundario:** "Contacta a un administrador de tu organización"
- **Icono:** 🔒 Bloqueado
- **Color:** Rojo
- **Acción:** Ocultar funcionalidad de asignación de roles en la UI

---

### 5. TOKEN_AUSENTE_O_INVALIDO (HTTP 401)

**Código:** `TOKEN_AUSENTE_O_INVALIDO`  
**Status HTTP:** `401 Unauthorized`  
**Trigger Técnico:** Token JWT ausente, expirado, o con firma inválida.

#### Ejemplo de Respuesta

```json
{
  "type": "https://docflow.com/errors/unauthorized",
  "title": "No Autorizado",
  "status": 401,
  "detail": "Token JWT inválido o expirado",
  "instance": "/api/v1/admin/users/100/roles",
  "codigo": "TOKEN_AUSENTE_O_INVALIDO"
}
```

#### Recomendaciones UX

- **Acción:** Redirigir automáticamente a la página de login
- **Mensaje (opcional):** "Tu sesión ha expirado. Por favor, inicia sesión nuevamente."

---

## Logs y Observabilidad

### Logs Recomendados

Para facilitar el debugging y monitoreo:

```java
// En GlobalExceptionHandler
log.warn("Login fallido - Credenciales inválidas para email: {}", 
    maskedEmail(request.getParameter("email")));

log.warn("Login fallido - Usuario sin organizaciones: {}", 
    maskedEmail(email));

log.error("Login fallido - Configuración inválida para usuario ID: {}", 
    usuarioId);
```

### Métricas (Micrometer)

Considerar agregar métricas para:
- Contador de errores `CREDENCIALES_INVALIDAS` (detectar ataques de fuerza bruta)
- Contador de errores `SIN_ORGANIZACION` (usuarios sin onboarding completo)
- Contador de errores `ORGANIZACION_CONFIG_INVALIDA` (problemas de configuración de datos)
- Contador de asignaciones de roles por organización (auditoría)
- Contador de reactivaciones de roles (análisis de patrones)
- Contador de errores `USUARIO_NO_ENCONTRADO` y `ROL_NO_ENCONTRADO` por organización

---

## Archivos Relacionados

### Implementación Backend

- [GlobalExceptionHandler.java](../src/main/java/com/docflow/identity/infrastructure/exception/GlobalExceptionHandler.java) - Manejo global de excepciones
- [AuthenticationController.java](../src/main/java/com/docflow/identity/infrastructure/adapters/rest/AuthenticationController.java) - Endpoint `/auth/login`
- [AdminUserController.java](../src/main/java/com/docflow/identity/infrastructure/adapters/rest/AdminUserController.java) - Endpoint `/admin/users/:userId/roles`
- [Excepciones de Dominio](../src/main/java/com/docflow/identity/domain/exceptions/) - Excepciones de negocio

### Tests

- [AuthLoginIntegrationTest.java](../src/test/java/com/docflow/identity/infrastructure/adapters/rest/AuthLoginIntegrationTest.java) - Tests de integración login
- [RoleAssignmentServiceTest.java](../src/test/java/com/docflow/identity/application/services/RoleAssignmentServiceTest.java) - Tests unitarios asignación de roles

---

## Versionamiento

**Versión:** 1.1.0  
**Última Actualización:** 9 de enero de 2026  
**Changelog:**
- v1.1.0 (2026-01-09): Agregados códigos de error para US-ADMIN-002 (asignación de roles)
- v1.0.0 (2026-01-08): Versión inicial con códigos de autenticación

**Contacto:** Equipo Backend DocFlow
