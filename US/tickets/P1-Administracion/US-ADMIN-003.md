## P1 — Administración (UI mínima Admin/Usuario)

### [US-ADMIN-003] Listar usuarios (API) del organizacion con roles
---
#### Base de Datos
---
* **Título:** Crear vista/query optimizado para listar usuarios con roles
* **Objetivo:** Obtener eficientemente usuarios de una organización con sus roles asociados.
* **Tipo:** Tarea
* **Descripción corta:** Crear query o vista que haga JOIN entre `Usuario`, `Usuario_Organizacion` y `Usuario_Rol` para obtener usuarios con sus roles en una sola consulta. Considerar paginación futura.
* **Entregables:**
    - Query SQL optimizado o vista materializada.
    - Índices necesarios para rendimiento.
    - Documentación del query.
---
* **Título:** Datos semilla para pruebas de listado
* **Objetivo:** Tener datos variados para probar diferentes escenarios de listado.
* **Tipo:** Tarea
* **Descripción corta:** Crear datos de prueba: usuarios con múltiples roles, usuarios sin roles, usuarios en diferentes estados (activo/inactivo), usuarios en diferentes organizaciones.
* **Entregables:**
    - Script de seed con variedad de usuarios.
    - Mínimo 5 usuarios en organización A con diferentes configuraciones.
    - Mínimo 2 usuarios en organización B para validar aislamiento.
---
#### Backend
---
* **Título:** Implementar DTO de respuesta para usuario con roles
* **Objetivo:** Definir la estructura de datos que se retornará en el listado.
* **Tipo:** Tarea
* **Descripción corta:** Crear DTO `UserWithRolesDto` con campos: `id`, `email`, `nombre`, `estado`, `roles[]`, `fecha_creacion`. Los roles deben incluir `id`, `codigo`, `nombre`.
* **Entregables:**
    - Clase/Interface `UserWithRolesDto`.
    - Clase/Interface `RoleSummaryDto` para roles anidados.
    - Mappers de entidad a DTO.
---
* **Título:** Implementar repositorio/query de usuarios por organización
* **Objetivo:** Obtener usuarios filtrados por organización con sus roles.
* **Tipo:** Tarea
* **Descripción corta:** Crear método en repositorio que reciba `organizacion_id` y retorne usuarios con membresía activa en esa organización, incluyendo sus roles. Debe soportar filtros opcionales.
* **Entregables:**
    - Método `findUsersByOrganization(orgId, filters?): UserWithRoles[]`.
    - JOIN eficiente con roles.
    - Filtrado estricto por `organizacion_id`.
---
* **Título:** Implementar servicio de listado de usuarios
* **Objetivo:** Lógica de negocio para obtener y formatear listado de usuarios.
* **Tipo:** Historia
* **Descripción corta:** Invocar repositorio con `organizacion_id` del token, mapear resultados a DTO, aplicar filtros opcionales (por estado, búsqueda por email/nombre).
* **Entregables:**
    - Método `listUsers(orgId, filters?): UserWithRolesDto[]`.
    - Mapeo de entidades a DTOs.
    - Soporte para filtros básicos.
---
* **Título:** Implementar endpoint `GET /admin/users`
* **Objetivo:** Exponer el listado de usuarios de la organización vía API REST.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint protegido que valide rol admin, extraiga `organizacion_id` del token, invoque servicio de listado y retorne `200` con array de usuarios.
* **Entregables:**
    - Ruta/controlador `GET /admin/users`.
    - Validación de rol administrador.
    - Query params opcionales: `estado`, `busqueda`.
    - Respuesta `200` con `{ usuarios: UserWithRolesDto[] }`.
---
* **Título:** Implementar paginación básica (opcional MVP)
* **Objetivo:** Permitir navegación eficiente en organizaciones con muchos usuarios.
* **Tipo:** Tarea
* **Descripción corta:** Agregar soporte para query params `page` y `limit`. Retornar metadata de paginación: `total`, `pagina`, `limite`, `paginas`.
* **Entregables:**
    - Query params `page` (default 1), `limit` (default 20, max 100).
    - Respuesta con metadata de paginación.
    - Queries con OFFSET/LIMIT.
---
* **Título:** Garantizar aislamiento de datos en listado
* **Objetivo:** Asegurar que solo se retornen usuarios de la organización del admin.
* **Tipo:** Tarea
* **Descripción corta:** Verificar que el query SIEMPRE filtre por `organizacion_id` del token. Nunca permitir que el cliente especifique otra organización.
* **Entregables:**
    - Validación de que `organizacion_id` viene del token.
    - Tests que verifiquen aislamiento.
---
* **Título:** Pruebas unitarias del servicio de listado
* **Objetivo:** Asegurar que el listado funciona correctamente con diferentes escenarios.
* **Tipo:** QA
* **Descripción corta:** Tests para: listado exitoso con usuarios, listado vacío, filtro por estado, filtro por búsqueda, verificación de estructura de respuesta.
* **Entregables:**
    - Suite de tests unitarios (mínimo 5 casos).
    - Verificación de estructura de DTOs.
---
* **Título:** Pruebas de integración de `GET /admin/users`
* **Objetivo:** Verificar endpoint completo con aislamiento de datos.
* **Tipo:** QA
* **Descripción corta:** Tests de integración: listado exitoso (200), solo usuarios de la org del token, sin token (401), sin rol admin (403), con filtros.
* **Entregables:**
    - Tests de integración para escenario de aceptación.
    - Verificación de que NO se retornan usuarios de otras organizaciones.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-ADMIN-003
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API, no pantalla.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La visualización del listado corresponde a `US-ADMIN-005`. Se puede crear colección Postman para pruebas.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección de requests para probar la API (Postman/HTTP).
