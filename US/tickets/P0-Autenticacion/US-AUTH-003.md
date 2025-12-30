## P0 — Autenticación + Organizacion

### [US-AUTH-003] Middleware de autenticación para endpoints protegidos
---
#### Backend
---
* **Título:** Implementar lógica de verificación de JWT
* **Objetivo:** Crear la función o servicio central que sea capaz de decodificar y verificar la firma de un token entrante.
* **Tipo:** Tarea
* **Descripción corta:** Implementar una utilidad que reciba un string (token), verifique su firma usando la clave secreta del servidor y compruebe que no ha expirado (Exp claim).
* **Entregables:**
    - Función VerifyToken(token) implementada.
    - Manejo de excepciones específicas: TokenExpiredError, JsonWebTokenError.
---
* **Título:** Crear Middleware de Autenticación HTTP
* **Objetivo:** Interceptar las peticiones HTTP para proteger las rutas.
* **Tipo:** Tarea
* **Descripción corta:** Crear un middleware que extraiga el header Authorization: Bearer <token>, invoque la validación y permita el paso o retorne error.
* **Entregables:**
    - Middleware configurado.
    - Respuesta 401 Unauthorized si no hay header o token inválido.
    - Inyección del payload del usuario en el objeto request.
---
* **Título:** Aplicar Middleware a rutas protegidas (Smoke Test)
* **Objetivo:** Asegurar que el middleware bloquea efectivamente el acceso.
* **Tipo:** Tarea
* **Descripción corta:** Configurar el middleware para que proteja un endpoint de prueba. Verificar que sin token no se puede acceder.
* **Entregables:**
    - Configuración de rutas actualizada.
    - Endpoint de prueba respondiendo 401 sin token y 200 con token válido.
---
#### Frontend
---
* **Título:** Configurar Interceptor HTTP para inyección de Token
* **Objetivo:** Automatizar el envío del token en cada petición sin repetirlo en cada llamada.
* **Tipo:** Tarea
* **Descripción corta:** Configurar el cliente HTTP para que, antes de enviar cualquier request, busque el token en el almacenamiento local y lo agregue al header Authorization.
* **Entregables:**
    - Interceptor de "Request" implementado.
    - Lógica para excluir endpoints públicos si es necesario.
---
* **Título:** Manejo global de errores 401 (Logout automático)
* **Objetivo:** Reaccionar cuando el token expira o es inválido durante una sesión.
* **Tipo:** Tarea
* **Descripción corta:** Configurar un interceptor de "Response" en el cliente HTTP. Si el backend retorna 401, el sistema debe limpiar el token local y redirigir al usuario a la pantalla de Login.
* **Entregables:**
    - Interceptor de "Response" implementado.
    - Redirección automática a /login ante un 401.
    - Limpieza de datos de sesión.
---
#### QA / Testing
---
* **Título:** Pruebas de integración de seguridad
* **Objetivo:** Verificar que la seguridad no puede ser evadida.
* **Tipo:** QA
* **Descripción corta:** Ejecutar batería de pruebas contra endpoints protegidos (sin token, token expirado, token manipulado, token válido).
* **Entregables:**
    - Test Cases documentados y ejecutados.
    - Reporte de pruebas de seguridad.
