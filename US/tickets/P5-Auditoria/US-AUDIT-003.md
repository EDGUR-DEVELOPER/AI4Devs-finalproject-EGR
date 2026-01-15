## P5 — Auditoría: logs inmutables + UI mínima

> Estado: **Post-MVP** — Auditoría asíncrona e indexada fue retirada del MVP. Para MVP, priorizar consultas básicas sobre la tabla `Evento_Auditoria` en PostgreSQL; la API paginada e indexación avanzada quedan para Post-MVP.

### [US-AUDIT-003] Consultar auditoría (API) con paginación y fechas
---
#### Base de datos
---
* **Título:** Optimizar consultas de auditoría con índices adicionales
* **Objetivo:** Asegurar rendimiento óptimo para consultas con filtros combinados.
* **Tipo:** Tarea
* **Descripción corta:** Revisar y crear índices adicionales si las consultas con múltiples filtros (organización + fechas + código evento) no usan los índices existentes eficientemente. Analizar planes de ejecución.
* **Entregables:**
    - Análisis de query plan para consultas típicas.
    - Índice adicional si el análisis lo justifica.
    - Documentación de estrategia de indexación.
---
#### Backend
---
* **Título:** Definir DTOs de request y response para consulta de auditoría
* **Objetivo:** Estandarizar el contrato de API para consultas de auditoría.
* **Tipo:** Diseño / Tarea
* **Descripción corta:** Crear interfaces/DTOs para los parámetros de consulta (`desde`, `hasta`, `codigo_evento`, `usuario_id`, `page`, `limit`) y para la respuesta paginada (`items`, `total`, `page`, `totalPages`).
* **Entregables:**
    - DTO `AuditQueryParams { desde?, hasta?, codigoEvento?, usuarioId?, page?, limit? }`.
    - DTO `AuditEventResponse { id, codigoEvento, usuarioId, usuarioEmail, timestamp, tipoEntidad, entidadId, metadata }`.
    - DTO `PaginatedResponse<T> { items: T[], total, page, limit, totalPages }`.
---
* **Título:** Implementar servicio de consulta de auditoría
* **Objetivo:** Crear la lógica de negocio para filtrar y paginar eventos de auditoría.
* **Tipo:** Historia
* **Descripción corta:** Implementar `AuditQueryService` con método `findByFilters(params, organizacionId)`. El servicio debe aplicar filtros de fecha inclusivos, filtrar siempre por `organizacion_id` del token, y devolver resultados paginados ordenados por fecha descendente.
* **Entregables:**
    - Método `findByFilters(params: AuditQueryParams, orgId: string): Promise<PaginatedResponse<AuditEvent>>`.
    - Filtro obligatorio por `organizacion_id` (no opcional, viene del token).
    - Filtros opcionales por `desde`, `hasta`, `codigo_evento`, `usuario_id`.
    - Ordenamiento por `timestamp DESC` por defecto.
---
* **Título:** Implementar endpoint GET /audit con filtros y paginación
* **Objetivo:** Exponer la API de consulta de auditoría para administradores.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint `GET /audit` que reciba query params para filtros y paginación. Debe extraer `organizacion_id` del token (nunca del cliente). Retornar 200 con resultados paginados.
* **Entregables:**
    - Endpoint `GET /audit?desde=&hasta=&codigoEvento=&usuarioId=&page=&limit=`.
    - Extracción de `organizacion_id` del token JWT.
    - Respuesta 200 con estructura `PaginatedResponse<AuditEventResponse>`.
    - Validación de formato de fechas (ISO 8601).
---
* **Título:** Implementar validación de permisos de administrador
* **Objetivo:** Restringir acceso a auditoría solo a usuarios con rol de administrador.
* **Tipo:** Tarea
* **Descripción corta:** Aplicar guard/middleware que verifique que el usuario tiene rol `ADMIN` o `ADMINISTRADOR` en la organización actual antes de permitir acceso al endpoint de auditoría.
* **Entregables:**
    - Guard `@RequireRole('ADMIN')` o middleware equivalente.
    - Respuesta 403 si el usuario no es administrador.
    - Log de intento de acceso no autorizado.
---
* **Título:** Implementar validación de parámetros de consulta
* **Objetivo:** Asegurar que los parámetros de filtro son válidos y seguros.
* **Tipo:** Tarea
* **Descripción corta:** Validar que `desde` <= `hasta`, que `page` y `limit` son números positivos, que `limit` no excede un máximo (ej. 100), y que `codigo_evento` existe en el catálogo.
* **Entregables:**
    - Validación de rango de fechas (desde <= hasta).
    - Límite máximo de `limit` (ej. 100).
    - Valores por defecto: `page=1`, `limit=20`.
    - Respuesta 400 con detalle de validación fallida.
---
* **Título:** Enriquecer respuesta con datos de usuario
* **Objetivo:** Incluir información legible del usuario en cada evento de auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Al retornar eventos, incluir `usuario_email` o `usuario_nombre` además de `usuario_id` para facilitar la lectura sin necesidad de consultas adicionales desde el frontend.
* **Entregables:**
    - JOIN con tabla `Usuario` para obtener email/nombre.
    - Campo `usuarioEmail` en `AuditEventResponse`.
    - Manejo de usuario eliminado (mostrar "Usuario eliminado" o similar).
---
* **Título:** Implementar paginación consistente (offset-based)
* **Objetivo:** Asegurar que la paginación sea consistente y predecible.
* **Tipo:** Tarea
* **Descripción corta:** Implementar paginación offset-based para MVP. Calcular `totalPages` correctamente. Asegurar que cambios en datos durante navegación no causen duplicados/omisiones significativas.
* **Entregables:**
    - Cálculo correcto de offset: `(page - 1) * limit`.
    - Campo `totalPages`: `Math.ceil(total / limit)`.
    - Documentación de limitaciones de offset-based pagination.
---
#### QA / Testing
---
* **Título:** Pruebas unitarias del servicio de consulta
* **Objetivo:** Verificar la lógica de filtrado y paginación.
* **Tipo:** QA
* **Descripción corta:** Crear tests unitarios para `AuditQueryService`. Mockear repositorio. Verificar aplicación correcta de filtros, cálculo de paginación y ordenamiento.
* **Entregables:**
    - Test: filtro por rango de fechas aplica correctamente.
    - Test: filtro por `organizacion_id` siempre se aplica.
    - Test: paginación calcula offset y totalPages correctamente.
    - Test: ordenamiento DESC por timestamp.
---
* **Título:** Pruebas de integración del endpoint de auditoría
* **Objetivo:** Verificar el endpoint completo con base de datos real.
* **Tipo:** QA
* **Descripción corta:** Ejecutar requests contra `GET /audit` con diferentes combinaciones de filtros. Verificar que solo se retornan eventos de la organización del token. Probar paginación con múltiples páginas.
* **Entregables:**
    - Test: admin org A solo ve eventos de org A.
    - Test: filtro `desde/hasta` retorna rango correcto.
    - Test: navegación entre páginas es consistente.
    - Test: usuario no-admin recibe 403.
---
* **Título:** Pruebas de seguridad de aislamiento de datos
* **Objetivo:** Verificar que no hay fuga de datos entre organizaciones.
* **Tipo:** QA
* **Descripción corta:** Intentar manipular `organizacion_id` en query params o headers. Verificar que siempre se usa el valor del token. Probar con tokens de diferentes organizaciones.
* **Entregables:**
    - Test: enviar `organizacion_id` en query param → ignorado.
    - Test: token org A nunca retorna eventos de org B.
    - Documentación de casos de seguridad probados.
---
#### Frontend
---
* **Título:** Crear servicio/cliente API para consulta de auditoría
* **Objetivo:** Encapsular las llamadas al endpoint de auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Implementar servicio `AuditApiService` con método para consultar auditoría. Manejar parámetros de filtro y paginación. Tipar respuestas con los DTOs definidos.
* **Entregables:**
    - Método `getAuditLogs(filters: AuditFilters): Promise<PaginatedAuditResponse>`.
    - Interfaz `AuditFilters { desde?, hasta?, codigoEvento?, page?, limit? }`.
    - Interfaz `AuditEvent` para tipado de respuesta.
