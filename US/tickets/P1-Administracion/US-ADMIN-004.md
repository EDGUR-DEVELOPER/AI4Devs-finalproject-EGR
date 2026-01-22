## P1 — Administración (UI mínima Admin/Usuario)

### [US-ADMIN-004] Desactivar usuario (API) sin borrado
---
#### Base de Datos
---
* **Título:** Verificar campo de estado en tabla Usuario
* **Objetivo:** Asegurar que existe el campo para marcar usuarios como inactivos.
* **Tipo:** Tarea
* **Descripción corta:** Verificar que la tabla `Usuario` tiene campo `estado` (ENUM o VARCHAR) con valores posibles: `ACTIVO`, `INACTIVO`. Si no existe, crear migración para agregarlo.
* **Entregables:**
    - Migración SQL si es necesario.
    - Valor por defecto `ACTIVO`.
    - Documentación de estados válidos.
---
* **Título:** Agregar campo de fecha de desactivación (opcional)
* **Objetivo:** Registrar cuándo se desactivó un usuario para auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Agregar campo `fecha_desactivacion` nullable en tabla `Usuario`. Se poblará cuando el estado cambie a `INACTIVO`.
* **Entregables:**
    - Migración SQL para campo `fecha_desactivacion`.
    - Campo nullable (NULL cuando está activo).
---
* **Título:** Actualizar estado en Usuario_Organizacion
* **Objetivo:** Marcar la membresía como inactiva además del usuario global.
* **Tipo:** Tarea
* **Descripción corta:** Verificar que `Usuario_Organizacion.estado` se actualice a `INACTIVO` cuando se desactiva un usuario en esa organización. Permite desactivación por org sin afectar otras membresías.
* **Entregables:**
    - Definición de si desactivación es global o por organización.
    - Migración si se requiere ajuste de estructura.
---
* **Título:** Datos semilla para pruebas de desactivación
* **Objetivo:** Tener usuarios activos para probar desactivación.
* **Tipo:** Tarea
* **Descripción corta:** Crear datos de prueba: usuario activo para desactivar, usuario ya inactivo, usuario de otra organización para validar aislamiento.
* **Entregables:**
    - Script de seed con usuarios en diferentes estados.
    - Credenciales de prueba documentadas.
---
#### Backend
---
* **Título:** Implementar servicio de validación de usuario para desactivar
* **Objetivo:** Verificar que el usuario existe y pertenece a la organización del admin.
* **Tipo:** Tarea
* **Descripción corta:** Reutilizar/crear método que verifique membresía activa del usuario en la organización. Retornar `404` si no existe o no pertenece a la org.
* **Entregables:**
    - Método `findUserInOrganization(userId, orgId): Usuario`.
    - Manejo de error `USUARIO_NO_ENCONTRADO` (404).
---
* **Título:** Implementar servicio de desactivación de usuario
* **Objetivo:** Lógica de negocio para cambiar estado de usuario a inactivo.
* **Tipo:** Historia
* **Descripción corta:** Actualizar `Usuario.estado` a `INACTIVO`, registrar `fecha_desactivacion`, actualizar `Usuario_Organizacion.estado`. No borrar datos para mantener historial.
* **Entregables:**
    - Método `deactivateUser(userId, orgId): void`.
    - Actualización transaccional de estados.
    - Registro de fecha de desactivación.
---
* **Título:** Implementar invalidación de tokens de usuario desactivado
* **Objetivo:** Asegurar que tokens existentes del usuario desactivado no funcionen.
* **Tipo:** Tarea
* **Descripción corta:** Agregar verificación de estado de usuario en el middleware de autenticación. Si usuario está `INACTIVO`, rechazar con `401` o `403` aunque el token sea válido.
* **Entregables:**
    - Modificación de middleware de autenticación.
    - Verificación de estado en cada request protegida.
    - Respuesta `401/403` para usuarios inactivos.
---
* **Título:** Implementar endpoint `PATCH /admin/users/:userId/deactivate`
* **Objetivo:** Exponer la funcionalidad de desactivación vía API REST.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint protegido que valide rol admin, verifique pertenencia del usuario a la org, invoque servicio de desactivación y retorne `200`.
* **Entregables:**
    - Ruta/controlador `PATCH /admin/users/:userId/deactivate`.
    - Alternativa: `PATCH /admin/users/:userId` con body `{ estado: "INACTIVO" }`.
    - Validación de rol administrador.
    - Respuesta `200` con `{ mensaje: "Usuario desactivado" }`.
    - Respuesta `404` si usuario no existe en la organización.
---
* **Título:** Prevenir auto-desactivación de administrador
* **Objetivo:** Evitar que un admin se desactive a sí mismo y pierda acceso.
* **Tipo:** Tarea
* **Descripción corta:** Validar que `userId` del request sea diferente al `usuario_id` del token. Si intenta auto-desactivarse, retornar `400` con mensaje apropiado.
* **Entregables:**
    - Validación de auto-desactivación.
    - Mensaje de error claro.
---
* **Título:** Pruebas unitarias del servicio de desactivación
* **Objetivo:** Asegurar que la lógica de desactivación funciona correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests para: desactivación exitosa, usuario no encontrado, usuario de otra org, usuario ya inactivo (idempotencia), auto-desactivación bloqueada.
* **Entregables:**
    - Suite de tests unitarios (mínimo 5 casos).
    - Verificación de actualización de campos.
---
* **Título:** Pruebas de integración de desactivación
* **Objetivo:** Verificar endpoint y efecto en autenticación.
* **Tipo:** QA
* **Descripción corta:** Tests de integración: desactivación exitosa (200), usuario inexistente (404), usuario de otra org (404), sin token (401), sin rol admin (403). Verificar que login posterior falla con `403`.
* **Entregables:**
    - Tests de integración para escenarios de aceptación.
    - Test de login con usuario desactivado retorna `403`.
    - Test de request protegida con token de usuario desactivado retorna `401/403`.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-ADMIN-004
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API, no pantalla.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La acción de desactivar se expondrá en `US-ADMIN-005`. Se puede crear colección Postman para pruebas.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección de requests para probar la API (Postman/HTTP).
