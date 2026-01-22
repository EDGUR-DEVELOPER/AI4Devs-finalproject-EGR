## P5 — Auditoría: logs inmutables + UI mínima

> Estado: **Post-MVP** — Esta historia queda planificada para Post-MVP porque depende de Audit Log y MongoDB/Kafka. En MVP sólo se aceptan controles básicos y registro simple en PostgreSQL. Rehabilitar en fase posterior.

### [US-AUDIT-005] Activación de auditoría de accesos no autorizados (Post-MVP)
---
#### Backend
---
* **Título:** Implementar endpoint POST /api/audit/events
* **Objetivo:** Habilitar la recepción de eventos de auditoría desde el frontend.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint `POST /api/audit/events` que reciba eventos de auditoría desde el cliente. El endpoint debe extraer automáticamente `organizacion_id` del token JWT, validar estructura del evento, y persistir en MongoDB. Debe responder con 201 incluyendo el ID del evento generado.
* **Entregables:**
    - Endpoint `POST /api/audit/events`.
    - DTO `AuditEventRequest` con validaciones (codigoEvento, detallesCambio).
    - Extracción automática de `organizacion_id` desde JWT.
    - Extracción de IP del cliente desde headers.
    - Response DTO con `id` y `fechaEvento`.
    - Documentación OpenAPI/Swagger.
---
* **Título:** Habilitar MongoDB en microservicio auditLog
* **Objetivo:** Activar la persistencia de eventos en MongoDB.
* **Tipo:** Tarea
* **Descripción corta:** Configurar conexión a MongoDB en el microservicio `auditLog`. Actualizar `application.yml` con URL de conexión, credenciales y configuración de pool. Verificar conectividad y creación automática de índices.
* **Entregables:**
    - MongoDB configurado en `application.yml`.
    - String de conexión configurada (dev/prod).
    - Verificación de índices multi-tenant.
    - Health check verificando conexión a MongoDB.
---
* **Título:** Implementar rate limiting por organización
* **Objetivo:** Prevenir spam de eventos de auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Implementar throttling/rate limiting en el endpoint de auditoría. Limitar a X eventos por minuto por `organizacion_id`. Retornar 429 Too Many Requests si se excede el límite. Considerar whitelist para organizaciones de confianza.
* **Entregables:**
    - Rate limiter configurado (ej: 100 eventos/minuto/org).
    - Respuesta 429 con header `Retry-After`.
    - Configuración externalizada (properties/env vars).
    - Logs de eventos bloqueados por rate limit.
---
* **Título:** Implementar interceptor automático de auditoría 403/404
* **Objetivo:** Registrar automáticamente intentos de acceso no autorizado desde el backend.
* **Tipo:** Tarea / Mejora
* **Descripción corta:** Crear interceptor o filtro global que detecte respuestas HTTP 403 Forbidden y 404 Not Found en endpoints de recursos (`/carpetas/:id`, `/documentos/:id`). Emitir evento de auditoría con código `ACCESS_DENIED` o `ACCESS_DENIED_404` automáticamente, incluyendo URL, método, usuario y organización.
* **Entregables:**
    - Interceptor/Filtro de Spring para capturar 403/404.
    - Detección de endpoints de recursos (regex o config).
    - Emisión automática a `AuditService.emit()`.
    - Test de integración verificando registro automático.
---
#### Frontend
---
* **Título:** Activar feature flag de auditoría
* **Objetivo:** Habilitar el envío de eventos de auditoría desde el cliente.
* **Tipo:** Tarea / Configuración
* **Descripción corta:** Cambiar la variable de entorno `VITE_AUDIT_ENABLED=true` en los archivos de configuración de producción (`.env.production`). Verificar en desarrollo que los eventos se envíen correctamente al endpoint. Monitorear logs de console para detectar errores de configuración.
* **Entregables:**
    - `VITE_AUDIT_ENABLED=true` en `.env.production`.
    - Verificación en staging/QA antes de producción.
    - Documentación de rollback (cambiar flag a false).
    - Smoke test: Intentar acceso a recurso de otra org y verificar evento en MongoDB.
---
* **Título:** Validar timeout y manejo de errores de auditoría
* **Objetivo:** Asegurar que fallos de auditoría no afecten la experiencia de usuario.
* **Tipo:** Prueba / QA
* **Descripción corta:** Ejecutar pruebas de carga y stress sobre el cliente de auditoría. Verificar que timeouts (3s) funcionen correctamente. Simular caídas del servicio de auditoría y confirmar que el sistema continúa funcionando con logs silenciosos en console.
* **Entregables:**
    - Test de timeout: Servicio de auditoría con delay > 3s.
    - Test de servicio caído: 503/404 en endpoint de auditoría.
    - Verificar que UX no se interrumpe (sin errores visibles).
    - Verificar logs en console con `console.warn` apropiados.
---
* **Título:** Configurar observabilidad de eventos de auditoría
* **Objetivo:** Monitorear la salud del sistema de auditoría.
* **Tipo:** Tarea / Infraestructura
* **Descripción corta:** Integrar métricas de auditoría con sistema de observabilidad (Datadog, Prometheus, etc.). Trackear: eventos enviados vs fallidos, latencia promedio, rate de timeouts, distribución de códigos de evento. Crear alertas para tasa de fallo > 5%.
* **Entregables:**
    - Métricas instrumentadas en `auditApi.ts`.
    - Dashboard con gráficas de eventos enviados/fallidos.
    - Alerta cuando tasa de error > 5% en 5min.
    - Documentación de runbook para troubleshooting.
---
#### Documentación
---
* **Título:** Actualizar documentación de sistema de auditoría
* **Objetivo:** Documentar el flujo completo de auditoría para el equipo.
* **Tipo:** Documentación
* **Descripción corta:** Crear documento técnico explicando el flujo de auditoría end-to-end: desde detección de 404 en frontend, pasando por interceptor, cliente de auditoría, endpoint backend, hasta persistencia en MongoDB. Incluir diagramas de secuencia, catálogo de códigos de evento, y guía de troubleshooting.
* **Entregables:**
    - Documento `AUDIT_SYSTEM.md` en raíz del proyecto.
    - Diagrama de secuencia (mermaid o similar).
    - Tabla de códigos de evento con descripción y uso.
    - FAQ: "¿Por qué no veo eventos en MongoDB?", "¿Cómo habilitar/deshabilitar auditoría?".
    - Referencias a `US-AUTH-004` y `US-AUDIT-001`.
---
#### Testing
---
* **Título:** Suite de tests E2E para auditoría de aislamiento
* **Objetivo:** Verificar que el aislamiento multi-tenant registra correctamente en auditoría.
* **Tipo:** Prueba / Automatización
* **Descripción corta:** Crear tests E2E que simulen: 1) Usuario de Org A intenta acceder a documento de Org B, 2) Verificar respuesta 404, 3) Verificar evento `ACCESS_DENIED_404` en MongoDB con datos correctos (URL, organizacionId, método). Debe ejecutarse en pipeline CI/CD.
* **Entregables:**
    - Test E2E con Playwright/Cypress.
    - Setup de datos: 2 orgs, recursos en cada una.
    - Assertions: HTTP 404, evento en DB, campos correctos.
    - Integración en pipeline de CI/CD.
    - Tiempo de ejecución < 30s.
---
* **Título:** Performance test del sistema de auditoría
* **Objetivo:** Validar que el sistema soporta carga esperada sin degradación.
* **Tipo:** Prueba / Performance
* **Descripción corta:** Ejecutar test de carga generando 1000 eventos de auditoría por minuto distribuidos en 10 organizaciones. Medir: latencia p50/p95/p99 del endpoint POST, tasa de error, throughput de MongoDB. Verificar que rate limiting funciona correctamente. Establecer SLO: p95 < 200ms, error rate < 1%.
* **Entregables:**
    - Script de carga con JMeter/k6.
    - Reporte con métricas de latencia y throughput.
    - Verificación de rate limiting (429 responses).
    - Gráficas de uso de CPU/memoria del servicio auditLog.
    - SLO documentados y validados.
---
### Criterios de Aceptación Global
* ✅ Feature flag `VITE_AUDIT_ENABLED=true` activado en producción
* ✅ Endpoint `POST /api/audit/events` disponible y funcional
* ✅ MongoDB configurado con índices multi-tenant optimizados
* ✅ Rate limiting funcionando (100 eventos/min/org)
* ✅ Interceptor backend registra automáticamente 403/404 de recursos
* ✅ Frontend envía eventos sin afectar UX (timeout 3s, fallback silencioso)
* ✅ Suite E2E validando flujo completo de aislamiento + auditoría
* ✅ Performance test con p95 < 200ms y error rate < 1%
* ✅ Documentación técnica completa con diagramas
* ✅ Métricas y alertas configuradas en observabilidad
* ✅ Smoke test en producción: Evento 404 registrado correctamente en MongoDB
---
### Dependencias
* Requiere: `US-AUDIT-001` (Emitir evento de auditoría) - **Backend auditLog base**
* Requiere: `US-AUDIT-002` (Persistencia inmutable) - **Modelo de datos MongoDB**
* Requiere: `US-AUTH-004` (Aislamiento multi-tenant) - **Implementación frontend completa**
* Bloquea: `US-AUDIT-004` (UI mínima de auditoría) - **Requiere datos en MongoDB para mostrar**
---
### Notas Técnicas
* **Security by Obscurity**: No revelar en logs públicos si recurso existe o no
* **GDPR Compliance**: Solo capturar datos mínimos (URL, método, timestamp, user agent)
* **Idempotencia**: Endpoint debe ser idempotente (mismo evento duplicado = ignorar)
* **Monitoreo**: Configurar alertas si tasa de `ACCESS_DENIED_404` > umbral normal (posible ataque)
* **Rollback Plan**: Si problemas, cambiar `VITE_AUDIT_ENABLED=false` sin redeploy de código
