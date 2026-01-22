## P0 — Autenticación + Organizacion

### [US-AUTH-006] Manejo de sesión expirada
---
#### Backend
---
* **Título:** Validar respuesta HTTP 401 específica para Token Expirado
* **Objetivo:** Asegurar que el middleware de autenticación devuelva un 401 limpio cuando el JWT ha expirado, distinguible de otros errores de servidor.
* **Tipo:** Tarea
* **Descripción corta:** Verificar que la validación del JWT en el middleware existente retorne `401 Unauthorized` inmediatamente al detectar `exp` (expiration time) vencido. Asegurar que el cuerpo de la respuesta sea consistente (ej. `{"error": "Unauthorized", "message": "Token expired"}`).
* **Entregables:**
    - Endpoint protegido de prueba que retorna 401 con token expirado.
    - Test unitario de controlador/middleware simulando token vencido.
---
#### Frontend
---
* **Título:** Implementar Interceptor HTTP global para errores 401
* **Objetivo:** Centralizar el manejo de errores de autenticación para no repetir lógica en cada llamada a la API.
* **Tipo:** Historia Técnica
* **Descripción corta:** Configurar el cliente HTTP (ej. Axios/Fetch wrapper) para interceptar respuestas. Si el status es 401 y la petición *no* es de login, debe disparar el flujo de cierre de sesión forzado.
* **Entregables:**
    - Archivo de configuración de interceptores (ej. `axios.interceptor.response`).
    - Lógica que detecta el 401.
---
* **Título:** Implementar lógica de Logout forzado y limpieza de estado
* **Objetivo:** Eliminar datos de sesión corruptos o viejos para evitar inconsistencias antes de redirigir.
* **Tipo:** Tarea
* **Descripción corta:** Crear una función/servicio `forceLogout()` que: 1. Elimine el token del almacenamiento local. 2. Limpie el estado global de usuario. 3. Redirija a la ruta `/login` pasando un parámetro `?reason=expired`.
* **Entregables:**
    - Función de limpieza de sesión.
    - Redirección funcional a la pantalla de login.
---
* **Título:** Mostrar alerta de "Sesión Expirada" en pantalla de Login
* **Objetivo:** Informar al usuario por qué fue redirigido al login, mejorando la UX.
* **Tipo:** Tarea
* **Descripción corta:** Modificar la página de Login para leer los query params (o estado de navegación) al montarse. Si existe `reason=expired`, mostrar un mensaje tipo Toast o Alerta: "Tu sesión ha expirado. Por favor, ingresa nuevamente".
* **Entregables:**
    - Componente de Login capaz de leer parámetros de URL.
    - Componente visual de Alerta/Toast integrado.
---
#### QA
---
* **Título:** Pruebas de regresión y expiración de sesión
* **Objetivo:** Validar el flujo completo desde la perspectiva del usuario final.
* **Tipo:** QA
* **Descripción corta:** Ejecutar casos de prueba manuales o automatizados: 1. Loguearse. 2. Manipular el token localmente (o esperar expiración corta configurada). 3. Intentar navegar/ejecutar acción. 4. Verificar redirección y mensaje.
* **Entregables:**
    - Reporte de ejecución de pruebas (Pass/Fail).
