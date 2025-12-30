## P1 — Administración (UI mínima Admin/Usuario)

### [US-ADMIN-002] Asignar rol a usuario (API) en el organizacion
---
#### Base de Datos
---
* **Título:** Crear/verificar modelo de Rol
* **Objetivo:** Persistir el catálogo de roles disponibles en el sistema.
* **Tipo:** Tarea
* **Descripción corta:** Verificar o crear tabla `Rol` con campos: `id`, `codigo`, `nombre`, `descripcion`, `organizacion_id` (NULL para roles globales), `activo`. Roles pueden ser globales o específicos de organización.
* **Entregables:**
    - Migración SQL para tabla `Rol`.
    - Constraint UNIQUE en (`codigo`, `organizacion_id`).
    - Documentación de estructura.
---
* **Título:** Crear tabla de asignación Usuario_Rol
* **Objetivo:** Permitir asignar múltiples roles a usuarios en contexto de organización.
* **Tipo:** Tarea
* **Descripción corta:** Crear tabla `Usuario_Rol` con `id`, `usuario_id`, `rol_id`, `organizacion_id`, `fecha_asignacion`, `asignado_por`. Debe vincular usuario, rol y organización.
* **Entregables:**
    - Migración SQL para tabla `Usuario_Rol`.
    - FK hacia `Usuario`, `Rol` y `Organizacion`.
    - Índice compuesto único (`usuario_id`, `rol_id`, `organizacion_id`).
---
* **Título:** Datos semilla de roles base del sistema
* **Objetivo:** Tener roles mínimos disponibles para asignación.
* **Tipo:** Tarea
* **Descripción corta:** Crear roles base: `ADMIN` (administrador), `USER` (usuario estándar), `VIEWER` (solo lectura). Estos serán roles globales disponibles para todas las organizaciones.
* **Entregables:**
    - Script de seed con roles base.
    - Documentación de permisos asociados a cada rol.
---
* **Título:** Datos semilla para pruebas de asignación de rol
* **Objetivo:** Facilitar pruebas con usuarios de misma/diferente organización.
* **Tipo:** Tarea
* **Descripción corta:** Crear datos de prueba: usuario del organizacion A (para asignación válida), usuario del organizacion B (para probar aislamiento), roles existentes.
* **Entregables:**
    - Script de seed con escenarios de prueba.
    - Usuarios en diferentes organizaciones.
---
#### Backend
---
* **Título:** Implementar DTO de entrada para asignación de rol
* **Objetivo:** Definir y validar la estructura de datos para asignar rol.
* **Tipo:** Tarea
* **Descripción corta:** Crear DTO `AssignRoleDto` con validaciones: `usuario_id` (UUID válido, requerido), `rol_id` (UUID válido, requerido).
* **Entregables:**
    - Clase/Interface `AssignRoleDto`.
    - Validaciones de formato UUID.
    - Mensajes de error claros.
---
* **Título:** Implementar servicio de validación de pertenencia a organización
* **Objetivo:** Verificar que el usuario objetivo pertenece a la misma organización del admin.
* **Tipo:** Tarea
* **Descripción corta:** Crear método que verifique si un usuario tiene membresía activa en una organización específica. Retorna `404` si no pertenece (sin filtrar información).
* **Entregables:**
    - Método `validateUserBelongsToOrg(userId, orgId): boolean`.
    - Manejo de error `USUARIO_NO_ENCONTRADO` (404).
---
* **Título:** Implementar servicio de validación de rol existente
* **Objetivo:** Verificar que el rol a asignar existe y está disponible para la organización.
* **Tipo:** Tarea
* **Descripción corta:** Verificar que el `rol_id` corresponde a un rol activo que es global o pertenece a la organización del admin.
* **Entregables:**
    - Método `validateRoleExists(rolId, orgId): Rol`.
    - Manejo de error `ROL_NO_ENCONTRADO` (404).
---
* **Título:** Implementar servicio de asignación de rol
* **Objetivo:** Lógica de negocio para asignar un rol a un usuario en la organización.
* **Tipo:** Historia
* **Descripción corta:** Validar pertenencia del usuario, validar existencia del rol, crear registro en `Usuario_Rol`. Si ya existe la asignación, retornar éxito sin duplicar.
* **Entregables:**
    - Método `assignRole(userId, roleId, orgId, assignedBy): void`.
    - Verificación de asignación existente (idempotencia).
    - Registro de quién asignó el rol.
---
* **Título:** Implementar endpoint `POST /admin/users/:userId/roles`
* **Objetivo:** Exponer la funcionalidad de asignación de rol vía API REST.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint protegido que valide rol admin, reciba `rol_id` en body, invoque servicio de asignación y retorne `200` confirmando la asignación.
* **Entregables:**
    - Ruta/controlador `POST /admin/users/:userId/roles`.
    - Validación de rol administrador.
    - Respuesta `200` con `{ mensaje: "Rol asignado correctamente" }`.
    - Respuesta `404` para usuario/rol no encontrado.
    - Respuesta `403` si usuario pertenece a otra organización.
---
* **Título:** Implementar validación de aislamiento por organización
* **Objetivo:** Garantizar que no se exponga información de otras organizaciones.
* **Tipo:** Tarea
* **Descripción corta:** Al buscar usuario o rol, filtrar siempre por `organizacion_id` del token. Si no existe en esa organización, retornar `404` genérico sin indicar si existe en otra.
* **Entregables:**
    - Queries filtrados por `organizacion_id`.
    - Respuestas que no filtran información entre organizaciones.
---
* **Título:** Pruebas unitarias del servicio de asignación de rol
* **Objetivo:** Asegurar que la lógica de negocio funciona correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests para: asignación exitosa, usuario no encontrado, rol no encontrado, asignación duplicada (idempotencia), usuario de otra organización.
* **Entregables:**
    - Suite de tests unitarios (mínimo 5 casos).
    - Cobertura de escenarios de aislamiento.
---
* **Título:** Pruebas de integración de `POST /admin/users/:userId/roles`
* **Objetivo:** Verificar endpoint completo con validación de seguridad.
* **Tipo:** QA
* **Descripción corta:** Tests de integración: asignación exitosa (200), usuario inexistente (404), usuario de otra org (404/403), rol inexistente (404), sin token (401), sin rol admin (403).
* **Entregables:**
    - Tests de integración para escenarios de aceptación.
    - Verificación de que no se filtra información entre organizaciones.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-ADMIN-002
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API, no pantalla.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La funcionalidad de asignación de roles se expondrá en `US-ADMIN-005`. Se puede crear colección Postman para pruebas.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección de requests para probar la API (Postman/HTTP).
