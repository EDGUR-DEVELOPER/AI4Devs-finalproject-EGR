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

---

## Archivos Relacionados

### Implementación Backend

- [GlobalExceptionHandler.java](../src/main/java/com/docflow/identity/infrastructure/adapters/input/rest/GlobalExceptionHandler.java) - Manejo global de excepciones
- [AuthenticationController.java](../src/main/java/com/docflow/identity/infrastructure/adapters/input/rest/AuthenticationController.java) - Endpoint `/auth/login`
- [Excepciones de Dominio](../src/main/java/com/docflow/identity/domain/exceptions/) - Excepciones de negocio

### Tests

- [AuthLoginIntegrationTest.java](../src/test/java/com/docflow/identity/infrastructure/adapters/input/rest/AuthLoginIntegrationTest.java) - Tests de integración con todos los escenarios

---

## Versionamiento

**Versión:** 1.0.0  
**Última Actualización:** 8 de enero de 2026  
**Contacto:** Equipo Backend DocFlow
