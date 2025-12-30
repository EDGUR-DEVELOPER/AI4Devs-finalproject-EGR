## P0 — Autenticación + Organizacion

### [US-AUTH-005] UI mínima de Login (Admin/Usuario)
---
#### Base de Datos
---
* **Título:** Datos semilla (Seeding) para escenarios de Login
* **Objetivo:** Disponer de usuarios de prueba en la BD que cumplan las condiciones de los escenarios de aceptación.
* **Tipo:** Tarea
* **Descripción corta:** Crear scripts o datos manuales para usuarios con 1 org, 2 orgs (1 default), >2 orgs, y sin org default.
* **Entregables:**
    - Script SQL o JSON de seed.
    - Lista de credenciales de prueba documentada.
---
#### Backend
---
* **Título:** Verificación de respuestas de error para UI
* **Objetivo:** Asegurar que el endpoint /auth/login devuelva errores interpretables por la UI.
* **Tipo:** Tarea
* **Descripción corta:** Verificar que el endpoint retorne códigos HTTP diferenciados (401, 409) y un payload JSON con códigos de error legibles. Habilitar CORS.
* **Entregables:**
    - Endpoint /auth/login validado.
    - Documentación de estructura de respuesta de error.
---
#### Frontend
---
* **Título:** Maquetación de Pantalla de Login (Layout)
* **Objetivo:** Crear la estructura visual y estilos de la página de inicio de sesión.
* **Tipo:** Tarea
* **Descripción corta:** Implementar HTML/CSS para el formulario de login (email, password, botón, mensajes). Diseño minimalista y responsive.
* **Entregables:**
    - Componente LoginPage implementado.
    - Estilos CSS/SASS aplicados.
---
* **Título:** Integración de Servicio de Autenticación (API Client)
* **Objetivo:** Conectar el formulario con el Backend.
* **Tipo:** Historia
* **Descripción corta:** Implementar la llamada POST /auth/login al enviar el formulario. Manejar el estado de "cargando".
* **Entregables:**
    - Función/Servicio login(credentials) en el frontend.
    - Conexión exitosa con el backend verificada.
---
* **Título:** Manejo de Sesión y Almacenamiento de Token
* **Objetivo:** Persistir la sesión del usuario tras un login exitoso.
* **Tipo:** Tarea
* **Descripción corta:** Al recibir el 200 OK, guardar el token en almacenamiento local (LocalStorage/Cookie) y actualizar el estado global de la app.
* **Entregables:**
    - Lógica de guardado de token.
    - Actualización del estado de usuario en la app.
---
* **Título:** Redirección y Protección de Rutas
* **Objetivo:** Redirigir al usuario a la pantalla principal y evitar acceso al login si ya está autenticado.
* **Tipo:** Tarea
* **Descripción corta:** Si login es exitoso -> Redirigir a /dashboard. Si usuario ya tiene token y entra a /login -> Redirigir a /dashboard.
* **Entregables:**
    - Lógica de enrutamiento configurada.
---
* **Título:** Manejo de Errores y Feedback Visual
* **Objetivo:** Informar al usuario cuando el login falla.
* **Tipo:** Tarea
* **Descripción corta:** Capturar errores 401 (Credenciales) y 409 (Problemas Org). Mostrar alertas visuales claras al usuario.
* **Entregables:**
    - Componentes de Alerta/Toast funcionando.
---
#### QA / Testing
---
* **Título:** Ejecución de Pruebas E2E/Manuales
* **Objetivo:** Validar que se cumplen todos los Criterios de Aceptación.
* **Tipo:** QA
* **Descripción corta:** Ejecutar los escenarios definidos en la historia usando los datos semilla. Verificar tanto el camino feliz como los casos de borde.
* **Entregables:**
    - Reporte de pruebas (Pass/Fail).
    - Bugs reportados.
