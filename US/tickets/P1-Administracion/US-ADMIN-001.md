## P1 — Administración (UI mínima Admin/Usuario)

### [US-ADMIN-001] Crear usuario (API) dentro del organizacion
---
#### Base de Datos
---
* **Título:** Crear/verificar modelo de Usuario con campos mínimos
* **Objetivo:** Persistir usuarios con los campos requeridos para el sistema DocFlow.
* **Tipo:** Tarea
* **Descripción corta:** Verificar que existe la tabla `Usuario` con campos mínimos: `id`, `email`, `password_hash`, `nombre`, `estado`, `fecha_creacion`, `fecha_actualizacion`. Si no existe, crear migración.
* **Entregables:**
    - Migración SQL para tabla `Usuario` (si no existe).
    - Constraint UNIQUE en campo `email` (unicidad global).
    - Documentación de campos y tipos de datos.
---
* **Título:** Crear/verificar tabla de membresía Usuario_Organizacion
* **Objetivo:** Permitir asociar usuarios a organizaciones con estado y fecha.
* **Tipo:** Tarea
* **Descripción corta:** Implementar o verificar tabla `Usuario_Organizacion` con `usuario_id`, `organizacion_id`, `estado`, `es_predeterminada`, `fecha_asignacion`. Incluir restricciones de integridad referencial.
* **Entregables:**
    - Migración SQL para tabla `Usuario_Organizacion`.
    - FK hacia `Usuario` y `Organizacion`.
    - Índice compuesto único (`usuario_id`, `organizacion_id`).
---
* **Título:** Datos semilla para pruebas de creación de usuario
* **Objetivo:** Facilitar pruebas con escenarios de email existente/nuevo y organizaciones.
* **Tipo:** Tarea
* **Descripción corta:** Crear datos de prueba: admin con permisos, usuario existente para probar duplicidad, organización activa para asignación.
* **Entregables:**
    - Script de seed con admin de prueba.
    - Usuario existente para validar duplicidad de email.
    - Organización activa para pruebas.
---
#### Backend
---
* **Título:** Implementar DTO de entrada para creación de usuario
* **Objetivo:** Definir y validar la estructura de datos esperada en el endpoint.
* **Tipo:** Tarea
* **Descripción corta:** Crear DTO `CreateUserDto` con validaciones: `email` (formato válido, requerido), `nombre` (requerido, longitud máxima), `password` (requerido, políticas mínimas).
* **Entregables:**
    - Clase/Interface `CreateUserDto`.
    - Validaciones con decoradores o esquema de validación.
    - Mensajes de error claros para cada validación.
---
* **Título:** Implementar servicio de validación de email único
* **Objetivo:** Verificar que el email no exista en el sistema antes de crear usuario.
* **Tipo:** Tarea
* **Descripción corta:** Crear método que consulte si existe un usuario con el email proporcionado. Debe retornar error normalizado si existe duplicado.
* **Entregables:**
    - Método `checkEmailExists(email): boolean`.
    - Manejo de error `EMAIL_DUPLICADO` con código `409`.
---
* **Título:** Implementar servicio de hash de contraseña
* **Objetivo:** Almacenar contraseñas de forma segura usando algoritmo de hash.
* **Tipo:** Tarea
* **Descripción corta:** Implementar servicio de hashing (bcrypt o argon2) para contraseñas. Configurar salt rounds según buenas prácticas de seguridad.
* **Entregables:**
    - Método `hashPassword(password): string`.
    - Configuración de salt rounds en variables de entorno.
---
* **Título:** Implementar servicio de creación de usuario
* **Objetivo:** Lógica de negocio para crear usuario y asociarlo a la organización del admin.
* **Tipo:** Historia
* **Descripción corta:** Crear usuario en tabla `Usuario`, generar hash de contraseña, crear registro en `Usuario_Organizacion` con el `organizacion_id` extraído del token del admin autenticado.
* **Entregables:**
    - Método `createUser(dto, organizacionId): Usuario`.
    - Transacción que crea Usuario + Usuario_Organizacion.
    - Retorno de usuario creado sin exponer password_hash.
---
* **Título:** Implementar endpoint `POST /admin/users`
* **Objetivo:** Exponer la funcionalidad de creación de usuario vía API REST.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint protegido que valide rol admin, reciba datos del usuario, invoque servicio de creación y retorne `201` con datos del usuario creado.
* **Entregables:**
    - Ruta/controlador `POST /admin/users`.
    - Validación de rol administrador en middleware/guard.
    - Respuesta `201` con `{ id, email, nombre, organizacion_id }`.
    - Respuesta `400/409` para errores de validación/duplicidad.
---
* **Título:** Implementar guard/middleware de autorización para admin
* **Objetivo:** Restringir acceso solo a usuarios con rol de administrador.
* **Tipo:** Tarea
* **Descripción corta:** Crear guard que verifique que el usuario autenticado tiene rol `ADMIN` o `ADMINISTRADOR` en la organización actual (según claims del token).
* **Entregables:**
    - Guard `AdminGuard` o middleware equivalente.
    - Verificación de rol en claims del token.
    - Respuesta `403` si no tiene permisos.
---
* **Título:** Normalizar respuestas de error para creación de usuario
* **Objetivo:** Mantener consistencia en formato de errores del módulo admin.
* **Tipo:** Tarea
* **Descripción corta:** Definir estructura de error estándar (`codigo`, `mensaje`, `detalles`) para errores `400` (validación), `409` (duplicidad) y `403` (permisos).
* **Entregables:**
    - Mapper de errores para módulo admin.
    - Documentación de códigos de error.
---
* **Título:** Pruebas unitarias del servicio de creación de usuario
* **Objetivo:** Asegurar que la lógica de negocio funciona correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests para: creación exitosa, email duplicado, validaciones de DTO, hash de contraseña correcto, creación de membresía.
* **Entregables:**
    - Suite de tests unitarios (mínimo 5 casos).
    - Cobertura de escenarios positivos y negativos.
---
* **Título:** Pruebas de integración de `POST /admin/users`
* **Objetivo:** Verificar endpoint completo incluyendo BD y autenticación.
* **Tipo:** QA
* **Descripción corta:** Tests de integración: crear usuario exitoso (201), email duplicado (409), sin token (401), sin rol admin (403), datos inválidos (400).
* **Entregables:**
    - Tests de integración para todos los escenarios de aceptación.
    - Verificación de persistencia en BD.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-ADMIN-001
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API, no pantalla.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La pantalla de gestión corresponde a `US-ADMIN-005`. Se puede crear colección Postman para pruebas de API.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección de requests para probar la API (Postman/HTTP).
