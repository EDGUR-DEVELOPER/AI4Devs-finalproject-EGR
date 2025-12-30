## P5 — Auditoría: logs inmutables + UI mínima

### [US-AUDIT-001] Emitir evento de auditoría en acciones críticas
---
#### Base de datos
---
* **Título:** Crear modelo de evento de auditoría
* **Objetivo:** Definir la estructura de persistencia para eventos de auditoría del sistema.
* **Tipo:** Tarea
* **Descripción corta:** Implementar tabla `Evento_Auditoria` con campos mínimos: `id`, `codigo_evento`, `organizacion_id`, `usuario_id`, `timestamp`, `tipo_entidad`, `entidad_id`, `datos_adicionales` (JSON). La tabla debe estar optimizada para escrituras frecuentes.
* **Entregables:**
    - Migración SQL con tabla `Evento_Auditoria`.
    - Columna `timestamp` con valor por defecto `CURRENT_TIMESTAMP`.
    - Relaciones FK hacia `Usuario` y `Organizacion`.
---
* **Título:** Crear catálogo de códigos de evento
* **Objetivo:** Estandarizar los tipos de eventos que el sistema puede emitir.
* **Tipo:** Tarea
* **Descripción corta:** Crear tabla `Catalogo_Evento` o enum/constantes con los códigos de evento permitidos. Incluir al menos: `FOLDER_CREATE`, `FOLDER_DELETE`, `DOC_UPLOAD`, `DOC_VERSION_CREATE`, `DOC_DOWNLOAD`, `USER_LOGIN`, `USER_LOGOUT`, `PERMISSION_GRANT`, `PERMISSION_REVOKE`.
* **Entregables:**
    - Tabla `Catalogo_Evento(codigo, descripcion, categoria)` o archivo de constantes.
    - Script seed con códigos iniciales del MVP.
    - Documentación del catálogo de eventos.
---
* **Título:** Datos semilla para pruebas de auditoría
* **Objetivo:** Facilitar pruebas automatizadas y manuales del módulo de auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Crear datos de ejemplo con múltiples eventos de diferentes tipos, usuarios y organizaciones. Incluir eventos con diferentes timestamps para probar filtros de fecha.
* **Entregables:**
    - Script de seed con al menos 50 eventos de prueba.
    - Eventos distribuidos en 2+ organizaciones.
    - Eventos con fechas variadas (últimos 30 días).
---
#### Backend
---
* **Título:** Definir interfaz de evento de auditoría
* **Objetivo:** Estandarizar la estructura de datos para emisión de eventos.
* **Tipo:** Diseño / Tarea
* **Descripción corta:** Crear interface/DTO `AuditEvent` con campos tipados. Definir enum `AuditEventCode` con los códigos del catálogo. Incluir campo opcional `metadata` para datos específicos por tipo de evento.
* **Entregables:**
    - Interface `AuditEvent { codigo, organizacionId, usuarioId, tipoEntidad, entidadId, metadata? }`.
    - Enum `AuditEventCode` con todos los códigos.
    - Enum `EntityType` (FOLDER, DOCUMENT, USER, PERMISSION).
---
* **Título:** Implementar servicio emisor de eventos de auditoría
* **Objetivo:** Crear el componente central responsable de emitir eventos al sistema de auditoría.
* **Tipo:** Historia
* **Descripción corta:** Implementar `AuditService` con método `emit(event: AuditEvent)`. El servicio debe enriquecer el evento con timestamp, validar campos requeridos y delegar la persistencia. Debe ser asíncrono para no bloquear operaciones principales.
* **Entregables:**
    - Clase/Servicio `AuditService` con método `emit()`.
    - Validación de campos obligatorios antes de emitir.
    - Manejo de errores que no interrumpa el flujo principal.
    - Logging de errores de auditoría para troubleshooting.
---
* **Título:** Crear decorador/interceptor para captura automática de eventos
* **Objetivo:** Facilitar la emisión de eventos sin modificar cada controlador manualmente.
* **Tipo:** Tarea
* **Descripción corta:** Implementar un decorador `@Auditable(eventCode)` o interceptor que capture automáticamente el contexto (usuario, organización) del request y emita el evento tras completar la operación exitosamente.
* **Entregables:**
    - Decorador `@Auditable(code: AuditEventCode)` para métodos de controlador.
    - Extracción automática de `usuario_id` y `organizacion_id` del token.
    - Emisión solo en caso de éxito (status 2xx).
---
* **Título:** Integrar auditoría en acción "Crear carpeta"
* **Objetivo:** Emitir evento `FOLDER_CREATE` al crear una carpeta exitosamente.
* **Tipo:** Tarea
* **Descripción corta:** Aplicar el decorador `@Auditable` o invocar `AuditService.emit()` en el endpoint/servicio de creación de carpetas. El evento debe incluir el `carpeta_id` creado como `entidad_id`.
* **Entregables:**
    - Endpoint `POST /folders` emitiendo evento de auditoría.
    - Metadata con `nombre_carpeta` y `carpeta_padre_id`.
---
* **Título:** Integrar auditoría en acción "Subir documento"
* **Objetivo:** Emitir evento `DOC_UPLOAD` al subir un documento exitosamente.
* **Tipo:** Tarea
* **Descripción corta:** Aplicar auditoría en el endpoint de subida de documentos. El evento debe incluir `documento_id`, `version_id` y metadatos relevantes (nombre, tamaño, tipo MIME).
* **Entregables:**
    - Endpoint `POST /documents` emitiendo evento de auditoría.
    - Metadata con `nombre_documento`, `tamano_bytes`, `mime_type`, `carpeta_id`.
---
* **Título:** Integrar auditoría en otras acciones críticas del MVP
* **Objetivo:** Completar la cobertura de auditoría para todas las acciones críticas.
* **Tipo:** Tarea
* **Descripción corta:** Aplicar auditoría en: login exitoso, cambio de organización, creación de versión, descarga de documento, asignación/revocación de permisos, creación/desactivación de usuario, eliminación de carpeta.
* **Entregables:**
    - Eventos para `USER_LOGIN`, `USER_SWITCH_ORG`.
    - Eventos para `DOC_VERSION_CREATE`, `DOC_DOWNLOAD`.
    - Eventos para `PERMISSION_GRANT`, `PERMISSION_REVOKE`.
    - Eventos para `USER_CREATE`, `USER_DEACTIVATE`.
    - Eventos para `FOLDER_DELETE`.
---
#### QA / Testing
---
* **Título:** Pruebas unitarias del servicio emisor de eventos
* **Objetivo:** Verificar la lógica de emisión y validación de eventos.
* **Tipo:** QA
* **Descripción corta:** Crear suite de tests unitarios para `AuditService`. Mockear dependencias de persistencia. Verificar validaciones, enriquecimiento de datos y manejo de errores.
* **Entregables:**
    - Tests para emisión exitosa con todos los campos.
    - Tests para validación de campos requeridos.
    - Tests para manejo de errores sin interrupción.
    - Cobertura mínima del 80% en `AuditService`.
---
* **Título:** Pruebas de integración de emisión de eventos
* **Objetivo:** Verificar que las acciones críticas generan eventos correctamente.
* **Tipo:** QA
* **Descripción corta:** Ejecutar acciones (crear carpeta, subir documento) y verificar que se persisten eventos con los datos correctos. Validar estructura completa del evento.
* **Entregables:**
    - Test: crear carpeta → evento con `FOLDER_CREATE`.
    - Test: subir documento → evento con `DOC_UPLOAD`.
    - Validación de `organizacion_id` y `usuario_id` desde token.
    - Validación de `entidad_id` coincide con recurso creado.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-AUDIT-001
* **Objetivo:** Aclarar alcance: esta historia define emisión de eventos en backend.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La visualización de auditoría corresponde a `US-AUDIT-004`.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
