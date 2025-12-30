## P1 — Administración (UI mínima Admin/Usuario)

### [US-ADMIN-005] UI mínima de gestión de usuarios
---
#### Base de Datos
---
* **Título:** Datos semilla completos para escenarios de UI
* **Objetivo:** Disponer de datos variados para probar la interfaz de gestión.
* **Tipo:** Tarea
* **Descripción corta:** Crear datos de prueba con variedad: usuarios activos e inactivos, usuarios con múltiples roles, usuarios sin roles, para visualizar diferentes estados en la tabla.
* **Entregables:**
    - Script de seed con mínimo 10 usuarios variados.
    - Documentación de escenarios de prueba.
---
#### Backend
---
* **Título:** Verificar endpoints de administración habilitados con CORS
* **Objetivo:** Asegurar que el frontend puede consumir las APIs de admin.
* **Tipo:** Tarea
* **Descripción corta:** Verificar configuración CORS para permitir requests desde el origen del frontend. Asegurar que todos los endpoints (`GET/POST /admin/users`, `PATCH deactivate`, `POST roles`) están accesibles.
* **Entregables:**
    - CORS configurado correctamente.
    - Endpoints verificados y documentados.
---
* **Título:** Endpoint para obtener roles disponibles
* **Objetivo:** Proveer lista de roles para el dropdown de asignación en UI.
* **Tipo:** Tarea
* **Descripción corta:** Crear endpoint `GET /admin/roles` que retorne los roles disponibles para asignar en la organización actual (globales + específicos de org).
* **Entregables:**
    - Ruta/controlador `GET /admin/roles`.
    - Respuesta con `{ roles: [{ id, codigo, nombre }] }`.
---
#### Frontend
---
* **Título:** Crear estructura de página de gestión de usuarios
* **Objetivo:** Establecer el layout base para la sección de administración de usuarios.
* **Tipo:** Tarea
* **Descripción corta:** Crear componente/página `UsersManagementPage` con estructura: header con título, área de acciones (botón crear), área de contenido para tabla, estilos base responsive.
* **Entregables:**
    - Componente `UsersManagementPage`.
    - Ruta `/admin/users` configurada.
    - Layout responsive básico.
---
* **Título:** Implementar servicio/cliente API para usuarios
* **Objetivo:** Centralizar las llamadas a la API de administración de usuarios.
* **Tipo:** Tarea
* **Descripción corta:** Crear servicio con métodos: `getUsers()`, `createUser(data)`, `assignRole(userId, roleId)`, `deactivateUser(userId)`, `getRoles()`. Incluir manejo de token en headers.
* **Entregables:**
    - Servicio `AdminUsersService` o similar.
    - Interceptor/config para incluir token en requests.
    - Manejo de errores HTTP.
---
* **Título:** Implementar tabla de usuarios
* **Objetivo:** Mostrar listado de usuarios con email, estado y roles.
* **Tipo:** Historia
* **Descripción corta:** Crear componente de tabla que muestre columnas: Email, Nombre, Estado (badge/chip), Roles (lista), Acciones. Debe consumir `GET /admin/users` al cargar.
* **Entregables:**
    - Componente `UsersTable`.
    - Columnas: Email, Nombre, Estado, Roles, Acciones.
    - Indicador de estado con colores (verde=activo, gris=inactivo).
    - Lista de roles en formato legible.
---
* **Título:** Implementar estado de carga y vacío en tabla
* **Objetivo:** Mejorar UX mostrando estados de la tabla.
* **Tipo:** Tarea
* **Descripción corta:** Mostrar spinner/skeleton mientras carga datos. Mostrar mensaje "No hay usuarios" cuando la lista está vacía. Manejar errores de carga con mensaje y opción de reintentar.
* **Entregables:**
    - Estado de carga (loading).
    - Estado vacío con mensaje.
    - Estado de error con botón de reintento.
---
* **Título:** Implementar modal/formulario de creación de usuario
* **Objetivo:** Permitir crear nuevos usuarios desde la UI.
* **Tipo:** Historia
* **Descripción corta:** Crear modal con formulario: campos Email, Nombre, Contraseña. Validaciones en frontend. Al guardar, llamar `POST /admin/users` y actualizar tabla.
* **Entregables:**
    - Componente `CreateUserModal` o `CreateUserForm`.
    - Validaciones: email formato válido, campos requeridos.
    - Feedback de éxito/error al crear.
    - Actualización de tabla tras creación exitosa.
---
* **Título:** Implementar acción de desactivar usuario
* **Objetivo:** Permitir desactivar usuarios desde la tabla.
* **Tipo:** Historia
* **Descripción corta:** Agregar botón/ícono "Desactivar" en columna de acciones (solo para usuarios activos). Mostrar confirmación antes de desactivar. Llamar `PATCH /admin/users/:id/deactivate` y actualizar tabla.
* **Entregables:**
    - Botón de desactivar en cada fila (condicional).
    - Modal de confirmación "¿Desactivar usuario X?".
    - Actualización de estado en tabla tras desactivación.
    - Ocultar botón si usuario ya está inactivo.
---
* **Título:** Implementar visualización/edición de roles
* **Objetivo:** Permitir ver y asignar roles a usuarios.
* **Tipo:** Historia
* **Descripción corta:** Mostrar roles actuales del usuario. Agregar opción para asignar nuevo rol (dropdown con roles disponibles de `GET /admin/roles`). Llamar `POST /admin/users/:id/roles` al asignar.
* **Entregables:**
    - Visualización de roles por usuario.
    - Dropdown o modal para asignar rol.
    - Llamada a API y actualización de UI.
---
* **Título:** Implementar protección de ruta para administradores
* **Objetivo:** Restringir acceso a la página solo a usuarios con rol admin.
* **Tipo:** Tarea
* **Descripción corta:** Crear guard de ruta que verifique si el usuario tiene rol `ADMIN` en el token. Redirigir a página de acceso denegado o dashboard si no tiene permisos.
* **Entregables:**
    - Guard `AdminRouteGuard` o similar.
    - Redirección si no es admin.
    - Mensaje de acceso denegado.
---
* **Título:** Implementar navegación a sección de usuarios
* **Objetivo:** Permitir acceso a la gestión de usuarios desde el menú principal.
* **Tipo:** Tarea
* **Descripción corta:** Agregar enlace "Usuarios" o "Administración > Usuarios" en el menú/sidebar. El enlace solo debe ser visible para usuarios con rol admin.
* **Entregables:**
    - Enlace en menú principal.
    - Visibilidad condicional según rol.
---
* **Título:** Manejo de errores y feedback visual
* **Objetivo:** Informar al usuario sobre éxitos y errores en operaciones.
* **Tipo:** Tarea
* **Descripción corta:** Implementar notificaciones toast/snackbar para: "Usuario creado exitosamente", "Usuario desactivado", "Rol asignado", errores de API (409 duplicado, 403 permisos, etc.).
* **Entregables:**
    - Componente de notificaciones.
    - Mensajes de éxito/error para cada acción.
    - Traducciones de códigos de error a mensajes legibles.
---
#### QA / Testing
---
* **Título:** Pruebas E2E de flujo de gestión de usuarios
* **Objetivo:** Validar el flujo completo desde la UI.
* **Tipo:** QA
* **Descripción corta:** Crear tests E2E que cubran: login como admin, navegar a usuarios, ver tabla, crear usuario nuevo, asignar rol, desactivar usuario. Verificar que los datos se reflejan correctamente.
* **Entregables:**
    - Suite de tests E2E (Cypress, Playwright, o similar).
    - Cobertura del criterio de aceptación.
---
* **Título:** Pruebas de accesibilidad (a11y)
* **Objetivo:** Asegurar que la UI es accesible.
* **Tipo:** QA
* **Descripción corta:** Verificar que la tabla y formularios son navegables por teclado, tienen labels apropiados, contrastes adecuados y lectores de pantalla pueden interpretarlos.
* **Entregables:**
    - Reporte de accesibilidad.
    - Correcciones de issues encontrados.
---
* **Título:** Pruebas manuales de escenarios de aceptación
* **Objetivo:** Validar manualmente todos los criterios de la historia.
* **Tipo:** QA
* **Descripción corta:** Ejecutar pruebas manuales: admin ve tabla con email, estado y roles; verificar que datos corresponden a la organización del admin; verificar que las acciones funcionan correctamente.
* **Entregables:**
    - Checklist de pruebas ejecutadas.
    - Bugs reportados si existen.
