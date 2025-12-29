## P0 — Autenticación + Organizacion

### [US-AUTH-001] Login multi-organizacion (organización predeterminada)
---
#### Base de datos
---
* **Título:** Crear modelo de membresía usuario–organización para login
* **Objetivo:** Persistir pertenencias y predeterminada para resolver el organizacion al autenticar.
* **Tipo:** Tarea
* **Descripción corta:** Implementa (o ajusta) tablas/columnas mínimas para `Usuario`, `Organizacion` y `Usuario_Organizacion` con `estado` y `es_predeterminada`. Debe permitir consultar “organizaciones activas” por usuario y su predeterminada.
* **Entregables:**
    - Migración SQL con `Usuario_Organizacion( usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion )`.
    - Definición de “ACTIVO” para membresía (y organización, si aplica).
---
* **Título:** Garantizar unicidad de organización predeterminada activa por usuario
* **Objetivo:** Evitar configuraciones inválidas (múltiples predeterminadas activas).
* **Tipo:** Tarea
* **Descripción corta:** Agrega la restricción/índice único parcial para asegurar como máximo 1 membresía activa marcada como predeterminada por usuario.
* **Entregables:**
    - Índice único parcial `ux_usuario_org_default_activa` (o equivalente en tu tecnología de migraciones).
    - Nota breve en doc técnica de la regla que hace cumplir.
---
* **Título:** Datos semilla para probar escenarios de Organizacion (0,1,2,>2 organizaciones)
* **Objetivo:** Facilitar QA y pruebas automatizadas reproduciendo escenarios del criterio de aceptación.
* **Tipo:** Tarea
* **Descripción corta:** Crea datos de ejemplo: usuario sin orgs activas, usuario con 1 org activa, usuario con 2 orgs activas con y sin predeterminada, y usuario con >2 orgs activas.
**Entregables:**
    - Script de seed (SQL o fixture) para los 5 escenarios.
    - Documentación de credenciales/datos de prueba (solo entorno local).
---
#### Backend
---
* **Título:** Implementar servicio de validación de credenciales
* **Objetivo:** Autenticar usuario por email/contraseña para habilitar `POST /auth/login`.
* **Tipo:** Tarea
* **Descripción corta:** Implementa lookup por email y verificación segura de contraseña. Debe devolver “credenciales inválidas” sin filtrar detalles.
* **Entregables:**
    - Método/servicio `authenticate(email, contrasena)`.
    - Mapeo de error a `401` para credenciales inválidas.
---
* **Título:** Implementar resolución de organización en login (reglas MVP)
* **Objetivo:** Seleccionar el `organizacion_id` correcto según membresías activas y predeterminada.
* **Tipo:** Tarea
* **Descripción corta:** Dado `usuario_id`, obtiene membresías activas y aplica reglas: 0→403, 1→ok, 2→requiere predeterminada, >2→409. No debe depender de input del cliente.
* **Entregables:**
    - Función/servicio `resolveLoginOrganization(usuario_id)`.
    - Errores normalizados: `SIN_ORGANIZACION` (403) y `Organizacion_CONFIG_INVALIDA` (409).
---
* **Título:** Emitir token en contexto de organización
* **Objetivo:** Generar token “emitido para la organización” seleccionada.
* **Tipo:** Tarea
* **Descripción corta:** Implementa emisión de token incluyendo, como mínimo, `usuario_id` y `organizacion_id` (claim acordado). La expiración debe ser consistente con `expira_en`.
* **Entregables:**
    - Servicio `issueToken({ usuario_id, organizacion_id })`.
    - Configuración de expiración y secreto/llave (por entorno).
---
* **Título:** Implementar endpoint `POST /auth/login` con contrato de respuesta
* **Objetivo:** Cumplir escenarios 1, 1b, 2, 2b, 3 y 4.
* **Tipo:** Historia
* **Descripción corta:** Endpoint que valida credenciales, resuelve organización, emite token y devuelve estructura de respuesta. Debe devolver `401/403/409` según corresponda.
* **Entregables:**
    - Ruta/controlador `POST /auth/login`.
    - Respuesta 200 con `token` (y, si aplica por contrato, `tipo_token`, `expira_en`, `organizaciones`).
---
* **Título:** Implementar autorización mínima para `POST /auth/switch`
* **Objetivo:** Requerir sesión válida para cambiar de organización.
* **Tipo:** Tarea
* **Descripción corta:** Protege el endpoint con verificación de token (mínima para este caso) y extrae `usuario_id` desde el token para validar membresía.
* **Entregables:**
    - Middleware/guard mínimo para token en `/auth/switch`.
    - Extracción de `usuario_id` y `organizacion_id` desde claims.
---
* **Título:** Implementar endpoint `POST /auth/switch` con validación de membresía
* **Objetivo:** Cumplir escenario 2c (cambio de organizacion emitiendo nuevo token).
* **Tipo:** Historia
* **Descripción corta:** Valida que `organizacion_id` solicitada pertenece al usuario y está activa. Emite un nuevo token en ese contexto y devuelve `200`.
* **Entregables:**
    - Ruta/controlador `POST /auth/switch`.
    - Validación de pertenencia activa + manejo de errores (`403` o `404` según convención definida).
---
* **Título:** Normalizar errores y códigos de negocio para autenticación/Organizacion
* **Objetivo:** Hacer verificables y consistentes las respuestas de error.
* **Tipo:** Tarea
* **Descripción corta:** Centraliza el shape de error (`codigo`, `mensaje`) y asegura que `/auth/login` use `SIN_ORGANIZACION` (403) y `Organizacion_CONFIG_INVALIDA` (409), y credenciales inválidas usen `401`.
* **Entregables:**
    - Mapper/handler de errores para auth.
    - Casos de prueba de serialización de error.
---
* **Título:** Pruebas unitarias de resolución de organización (0/1/2/>2)
* **Objetivo:** Asegurar reglas MVP y prevenir regresiones.
* **Tipo:** QA
* **Descripción corta:** Tests puros sobre `resolveLoginOrganization` cubriendo todos los escenarios de aceptación y bordes (p. ej. 2 activas con 2 predeterminadas → invalida).
* **Entregables:**
    - Suite de unit tests con 5 escenarios mínimos.
    - Reporte de cobertura (si existe en el stack).
---
* **Título:** Pruebas de integración de `POST /auth/login` (200/401/403/409)
* **Objetivo:** Verificar endpoint y contrato HTTP extremo a extremo.
* **Tipo:** QA
* **Descripción corta:** Ejecuta requests reales contra el servidor con datos seed, validando status codes y campos requeridos de la respuesta.
* **Entregables:**
    - Tests de integración para escenarios 1, 1b, 2, 2b, 3, 4.
    - Validación del shape de respuesta 200.
---
* **Título:** Pruebas de integración de `POST /auth/switch` (200 + validación de pertenencia)
* **Objetivo:** Verificar que el cambio de organización solo funciona con membresía activa.
* **Tipo:** QA
* **Descripción corta:** Con token inicial, solicita cambio a otra org válida y verifica nuevo token; intenta cambiar a org no perteneciente/inactiva y verifica rechazo.
* **Entregables:**
    - Tests de integración para escenario 2c y negativos.
    - Verificación de que el nuevo token refleja el `organizacion_id` solicitado.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-AUTH-001
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API, no pantalla.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La pantalla de login corresponde a `US-AUTH-005`.
* **Entregables:**
    - Confirmación de “no aplica” en planning.
    - (Opcional) Colección de requests para probar la API (Postman/HTTP) si el equipo la usa.