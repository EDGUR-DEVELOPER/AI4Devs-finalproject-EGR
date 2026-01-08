# 📋 Bitácora de Desarrollo del Proyecto DocFlow

> **Documento Maestro** - Fuente de verdad para el seguimiento del desarrollo  
> **Última actualización:** 8 de enero de 2026  
> **Deadline MVP:** 16 de enero de 2026

---

## 1. Resumen del Proyecto

### Estado General

| Métrica | Valor |
|---------|-------|
| **Progreso MVP** | 🟡 **18%** (Infraestructura + Autenticación base + Middleware completados) |
| **Tickets MVP** | 3/30 completados |
| **Tickets Post-MVP** | 1/9 planificados |
| **Días restantes** | 8 días (8 ene 2026 → 16 ene 2026) |
| **Velocidad requerida** | ~3.4 tickets/día (con asistencia IA) |

### Stack Principal

#### Backend (Microservicios - Java 21 + Spring Boot 3.5)

| Servicio | Puerto | Tecnologías | Responsabilidad |
|----------|--------|-------------|-----------------|
| `identity-service` | 8081 | Spring Web, JPA, JWT | Autenticación, usuarios, organizaciones |
| `gateway-service` | 8080 | Spring Cloud Gateway, WebFlux | API Gateway, ruteo, rate limiting |
| `document-core-service` | 8082 | Spring Web, JPA, MinIO Client | Carpetas, documentos, versiones |
| `auditlog-service` | 8083 | Spring WebFlux, MongoDB Reactive | Eventos de auditoría inmutables |
| `vault-service` | 8084 | Spring Vault Core | Gestión de secretos |
| `broker-service` | 8085 | Spring Kafka | Mensajería asíncrona |

#### Frontend (React 19 + TypeScript)

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| React | 19.2.0 | UI Framework |
| TypeScript | 5.9.3 | Tipado estático |
| Vite | 7.2.4 | Build tool |
| Tailwind CSS | 4.1.18 | Estilos utilitarios |
| Zustand | 5.0.9 | Estado global |
| Axios | 1.13.2 | HTTP Client |
| React Router | 7.11.0 | Navegación SPA |

#### Infraestructura (Docker Compose) ✅

| Servicio | Versión | Puerto | Estado |
|----------|---------|--------|--------|
| PostgreSQL | 16-alpine | 5432 | ✅ Configurado |
| MongoDB | 7.0 | 27017 | ✅ Configurado |
| MinIO | 2024-01-01 | 9000/9001 | ✅ Configurado |
| Redis | 7-alpine | 6379 | ✅ Configurado |
| Apache Kafka | 3.7.0 (KRaft) | 9092 | ✅ Configurado |
| HashiCorp Vault | 1.17 | 8200 | ✅ Configurado |

---

## 2. Plan de Ejecución MVP (Roadmap Paso a Paso)

### 📅 Calendario de Sprints

```
Dic 31 ─────────────────────────────────────────────────── Ene 16
  │                                                           │
  ├── Fase 1: Infraestructura ✅ (Día 0)                      │
  ├── Fase 2: Autenticación (Días 1-4) ──────────────────┐    │
  ├── Fase 3: Admin + ACL Base (Días 5-9) ───────────────┤    │
  ├── Fase 4: Carpetas + Documentos (Días 10-14) ────────┤    │
  ├── Fase 5: Auditoría Core (Día 15) ───────────────────┤    │
  └── Fase 6: Buffer/QA (Día 16) ────────────────────────┴────┘
```

---

### Fase 0: Infraestructura & Configuración Base ✅

> **Estado:** Completado  
> **Tecnología:** Docker Compose, PostgreSQL, MongoDB, MinIO, Kafka, Vault

- [x] **INFRA-001**: Configuración Docker Compose
    * *Detalle técnico:* Archivo `docker-compose.yml` con todos los servicios de infraestructura configurados.
    * *Ubicación:* `/docker-compose.yml`

- [x] **INFRA-002**: Scaffolding de microservicios
    * *Detalle técnico:* Estructura hexagonal creada en cada servicio backend (application/domain/infrastructure).
    * *Ubicación:* `/backend/*/src/main/java/`

- [x] **INFRA-003**: Scaffolding Frontend
    * *Detalle técnico:* Proyecto React + Vite + TypeScript + Tailwind inicializado con estructura feature-driven.
    * *Ubicación:* `/frontend/src/`

---

### Fase 1: Autenticación Core (P0)

> **Días:** 1-4 (1 Ene - 4 Ene 2026)  
> **Tickets:** 6 | **Servicio principal:** `identity-service`  
> **Bloquea:** Todo el resto del proyecto


#### Día 1 (1 Ene)

- [x] **US-AUTH-001**: Login multi-organización ✅
    * *Detalle técnico:* Crear modelos `Usuario`, `Organizacion`, `Usuario_Organizacion` en PostgreSQL. Endpoint `POST /auth/login` que resuelve membresías. Si usuario pertenece a 1 org → token directo; si >1 → retornar lista para selección.
    * *Servicio:* `identity-service` (Spring Data JPA)
    * *Tablas:* `usuarios`, `organizaciones`, `usuarios_organizaciones`
    * *Dependencia:* Ninguna (es el punto de partida)
    * *Estado:* Completado el 4 Ene 2026

- [x] **US-AUTH-002**: Token JWT con claims de org/roles ✅
    * *Detalle técnico:* Implementar generación de JWT con claims `org_id`, `roles[]`, `user_id`, `exp`. Crear `JwtService` usando `io.jsonwebtoken`. Definir interface `JwtPayload` en frontend.
    * *Servicio:* `identity-service`
    * *Dependencia:* US-AUTH-001
    * *Estado:* Completado el 5 Ene 2026 (Frontend y Backend)

#### Día 2 (2 Ene)

- [x] **US-AUTH-003**: Middleware de autenticación ✅
    * *Detalle técnico:* Crear `JwtAuthenticationFilter` que valide token en cada request. Extraer claims e inyectar en `SecurityContext`. Configurar rutas públicas (`/auth/**`, `/health`).
    * *Servicio:* `identity-service`, `gateway-service`
    * *Dependencia:* US-AUTH-002
    * *Estado:* Completado el 8 Ene 2026

- [ ] **US-AUTH-004**: Aislamiento de datos por tenant
    * *Detalle técnico:* Agregar columna `organizacion_id` a todas las tablas de negocio. Crear `TenantContext` que extraiga `org_id` del token. Implementar `@TenantFilter` para auto-filtrar queries JPA.
    * *Servicio:* `identity-service`, `document-core-service`
    * *Dependencia:* US-AUTH-003

#### Día 3-4 (3-4 Ene)

- [ ] **US-AUTH-005**: UI de Login
    * *Detalle técnico:* Crear `LoginPage.tsx` con formulario email/password. Integrar con `POST /auth/login`. Manejar selección de organización si múltiples. Guardar token en `localStorage`. Usar Zustand para estado de sesión.
    * *Servicio:* `frontend` (React + Zustand)
    * *Ruta:* `/login`
    * *Dependencia:* US-AUTH-001, US-AUTH-002

- [ ] **US-AUTH-006**: Manejo de sesión expirada
    * *Detalle técnico:* Crear interceptor Axios que detecte 401. Limpiar token y redirigir a `/login` con mensaje. Implementar `useAuth` hook para validar sesión activa.
    * *Servicio:* `frontend`
    * *Dependencia:* US-AUTH-005

---

### Fase 2: Administración de Usuarios (P1)

> **Días:** 5-6 (5-6 Ene 2026)  
> **Tickets:** 5 | **Servicio principal:** `identity-service`

#### Día 5 (5 Ene)

- [ ] **US-ADMIN-001**: Crear usuario (API)
    * *Detalle técnico:* Endpoint `POST /admin/users` que crea usuario dentro de la organización del admin. Hash de password con BCrypt. Validar unicidad de email por organización.
    * *Servicio:* `identity-service`
    * *Guard:* Requiere rol `ADMIN`
    * *Dependencia:* US-AUTH-003, US-AUTH-004

- [ ] **US-ADMIN-002**: Asignar rol (API)
    * *Detalle técnico:* Endpoint `POST /admin/users/{userId}/roles`. Crear tabla `roles` y `usuarios_roles`. Roles iniciales: `ADMIN`, `EDITOR`, `VIEWER`.
    * *Servicio:* `identity-service`
    * *Dependencia:* US-ADMIN-001

- [ ] **US-ADMIN-003**: Listar usuarios (API)
    * *Detalle técnico:* Endpoint `GET /admin/users` con paginación. Filtrar automáticamente por `organizacion_id` del token. Incluir roles de cada usuario en respuesta.
    * *Servicio:* `identity-service`
    * *Dependencia:* US-ADMIN-001

#### Día 6 (6 Ene)

- [ ] **US-ADMIN-004**: Desactivar usuario (API)
    * *Detalle técnico:* Endpoint `PATCH /admin/users/{userId}/deactivate`. Soft-delete con campo `activo=false`. Invalidar tokens activos del usuario. Prevenir auto-desactivación.
    * *Servicio:* `identity-service`
    * *Dependencia:* US-ADMIN-001

- [ ] **US-ADMIN-005**: UI Gestión de Usuarios
    * *Detalle técnico:* Crear `UsersPage.tsx` con tabla de usuarios, modal de creación, acción de desactivar, selector de roles. Usar componentes Tailwind.
    * *Servicio:* `frontend`
    * *Ruta:* `/admin/users`
    * *Dependencia:* US-ADMIN-001 a 004

---

### Fase 3: Sistema de Permisos ACL (P2)

> **Días:** 7-9 (7-9 Ene 2026)  
> **Tickets:** 6 MVP + 3 Post-MVP | **Servicio principal:** `document-core-service`

#### Día 7 (7 Ene)

- [ ] **US-ACL-001**: Catálogo de niveles de acceso
    * *Detalle técnico:* Crear tabla `niveles_acceso` con valores: `LECTURA`, `ESCRITURA`, `ADMINISTRACION`. Endpoint `GET /niveles-acceso`. Seed inicial via Flyway/Liquibase.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-AUTH-004

- [ ] **US-ACL-002**: Otorgar permiso en carpeta (API)
    * *Detalle técnico:* Crear tabla `acl_carpetas` (carpeta_id, usuario_id, nivel_acceso_id, recursivo). Endpoint `POST /carpetas/{id}/permisos`. Validar que carpeta pertenezca al tenant.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-ACL-001

- [ ] **US-ACL-003**: Revocar permiso en carpeta (API)
    * *Detalle técnico:* Endpoint `DELETE /carpetas/{id}/permisos/{usuarioId}`. Efecto inmediato, eliminar registro de ACL.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-ACL-002

#### Día 8 (8 Ene)

- [ ] **US-ACL-004**: Permisos recursivos en subcarpetas
    * *Detalle técnico:* Campo `recursivo` en `acl_carpetas`. Implementar `resolverPermisoEfectivo()` que evalúe path ancestro. Usar materialized path o closure table para jerarquía.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-ACL-002

- [ ] **US-ACL-005**: Permisos explícitos en documentos
    * *Detalle técnico:* Crear tabla `acl_documentos` (documento_id, usuario_id, nivel_acceso_id). Endpoint `POST /documentos/{id}/permisos`. Permiso explícito override permiso de carpeta.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-ACL-001

#### Día 9 (9 Ene)

- [ ] **US-ACL-006**: Evaluador de permisos con precedencia
    * *Detalle técnico:* Crear `EvaluadorPermisos` service. Reglas: Documento explícito > Carpeta directa > Carpeta heredada. Implementar interface `IEvaluadorPermisos`.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-ACL-004, US-ACL-005

- [ ] **US-ACL-007**: Enforcement de permisos lectura
    * *Detalle técnico:* Crear annotation `@RequiereLectura`. Aspect que valida permiso antes de ejecutar método. Retornar 403 si no tiene acceso.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-ACL-006

- [ ] **US-ACL-008**: Enforcement de permisos escritura
    * *Detalle técnico:* Crear annotation `@RequiereEscritura`. Similar a lectura pero para operaciones de modificación.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-ACL-006

---

### Fase 4: Gestión de Carpetas (P3)

> **Días:** 10-11 (10-11 Ene 2026)  
> **Tickets:** 5 | **Servicio principal:** `document-core-service`

#### Día 10 (10 Ene)

- [ ] **US-FOLDER-001**: Crear carpeta (API)
    * *Detalle técnico:* Crear tabla `carpetas` (id, nombre, carpeta_padre_id, organizacion_id, path, activo). Endpoint `POST /api/carpetas`. Validar nombre único por nivel. Crear carpeta raíz automática por organización.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-AUTH-004

- [ ] **US-FOLDER-002**: Listar contenido de carpeta (API)
    * *Detalle técnico:* Endpoint `GET /api/carpetas/{id}/contenido`. Retornar subcarpetas y documentos. Filtrar por permisos de lectura del usuario. Incluir campo `mis_capacidades` por item.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-FOLDER-001, US-ACL-007

#### Día 11 (11 Ene)

- [ ] **US-FOLDER-003**: Mover documento entre carpetas (API)
    * *Detalle técnico:* Endpoint `PATCH /api/documentos/{id}/mover`. Validar permiso ESCRITURA en carpeta origen y destino. Emitir evento de auditoría.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-FOLDER-001, US-DOC-001, US-ACL-008

- [ ] **US-FOLDER-004**: Eliminar carpeta vacía (API)
    * *Detalle técnico:* Endpoint `DELETE /api/carpetas/{id}`. Soft-delete solo si no tiene contenido. Prevenir eliminación de carpeta raíz. Requiere permiso ADMINISTRACION.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-FOLDER-001

- [ ] **US-FOLDER-005**: UI Navegación de carpetas
    * *Detalle técnico:* Crear `FolderExplorer.tsx` con breadcrumb, lista de contenido, acciones contextuales. Iconos diferenciados para carpetas/documentos. Estados de loading.
    * *Servicio:* `frontend`
    * *Ruta:* `/folders`, `/folders/{id}`
    * *Dependencia:* US-FOLDER-001 a 004

---

### Fase 5: Documentos y Versionado (P4)

> **Días:** 12-14 (12-14 Ene 2026)  
> **Tickets:** 5 MVP + 1 Post-MVP | **Servicio principal:** `document-core-service`

#### Día 12 (12 Ene)

- [ ] **US-DOC-001**: Subir documento (API)
    * *Detalle técnico:* Crear tablas `documentos` y `versiones_documento`. Endpoint `POST /api/folders/{id}/documents` (multipart). Guardar archivo en MinIO. Crear versión 1 automáticamente.
    * *Servicio:* `document-core-service`
    * *Storage:* MinIO (S3-compatible)
    * *Dependencia:* US-FOLDER-001, US-ACL-008

- [ ] **US-DOC-002**: Descargar versión actual (API)
    * *Detalle técnico:* Endpoint `GET /api/documents/{id}/download`. Stream desde MinIO con MIME type correcto. Validar permiso LECTURA. Registrar evento de auditoría.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-DOC-001, US-ACL-007

#### Día 13 (13 Ene)

- [ ] **US-DOC-003**: Subir nueva versión (API)
    * *Detalle técnico:* Endpoint `POST /api/documents/{id}/versions`. Incrementar número de secuencia atómicamente. Manejar concurrencia con optimistic locking. Actualizar puntero de versión actual.
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-DOC-001

- [ ] **US-DOC-004**: Listar historial de versiones (API)
    * *Detalle técnico:* Endpoint `GET /api/documents/{id}/versions`. Retornar lista ordenada por secuencia descendente. Marcar cuál es la versión actual. Incluir metadata (fecha, usuario, tamaño).
    * *Servicio:* `document-core-service`
    * *Dependencia:* US-DOC-003

#### Día 14 (14 Ene)

- [ ] **US-DOC-006**: UI Subida y versionado de documentos
    * *Detalle técnico:* Crear `DocumentUpload.tsx` con drag & drop, progress bar, validación de tipos. `VersionHistory.tsx` con tabla de versiones y badge de versión actual.
    * *Servicio:* `frontend`
    * *Dependencia:* US-DOC-001 a 004

---

### Fase 6: Auditoría Core (P5)

> **Día:** 15 (15 Ene 2026)  
> **Tickets:** 3 MVP + 1 Post-MVP | **Servicio principal:** `auditlog-service`

#### Día 15 (15 Ene)

- [ ] **US-AUDIT-001**: Emisión de eventos de auditoría
    * *Detalle técnico:* Definir catálogo de eventos (`LOGIN`, `LOGOUT`, `DOCUMENT_UPLOAD`, `DOCUMENT_DOWNLOAD`, `PERMISSION_GRANTED`, etc.). Crear annotation `@Auditable`. Publicar a Kafka topic `audit-events`.
    * *Servicio:* `broker-service`, todos los servicios
    * *Dependencia:* Infraestructura Kafka

- [ ] **US-AUDIT-002**: Persistencia inmutable
    * *Detalle técnico:* Consumir de Kafka y persistir en MongoDB colección `audit_events`. Crear índices por fecha, usuario, evento. Implementar hash de integridad SHA-256. Bloquear UPDATE/DELETE a nivel de aplicación.
    * *Servicio:* `auditlog-service` (MongoDB Reactive)
    * *Dependencia:* US-AUDIT-001

- [ ] **US-AUDIT-003**: Consulta de auditoría (API)
    * *Detalle técnico:* Endpoint `GET /audit` con filtros: `fechaDesde`, `fechaHasta`, `codigoEvento`, `usuarioId`. Paginación. Solo accesible por rol ADMIN.
    * *Servicio:* `auditlog-service`
    * *Dependencia:* US-AUDIT-002

---

### Fase 7: Buffer & QA

> **Día:** 16 (16 Ene 2026)  
> **Actividades:** Pruebas de integración, fixes críticos, documentación

- [ ] **QA-001**: Pruebas E2E flujo completo
    * *Detalle técnico:* Validar: Login → Navegar carpetas → Subir documento → Descargar → Ver versiones → Logout.

- [ ] **QA-002**: Validación de permisos
    * *Detalle técnico:* Probar escenarios: usuario sin permiso no ve carpeta, usuario con LECTURA no puede subir, etc.

- [ ] **QA-003**: Deploy de validación
    * *Detalle técnico:* Levantar todos los servicios con `docker-compose up` y validar integración.

---

## 3. Registro de Progreso (Ga30 tickets)

| Fase | Tickets Pendientes |
|------|-------------------|
| Autenticación | US-AUTH-004, US-AUTH-005, US-AUTH-006 |
| Administración | US-ADMIN-001, US-ADMIN-002, US-ADMIN-003, US-ADMIN-004, US-ADMIN-005 |
| Permisos ACL | US-ACL-001, US-ACL-002, US-ACL-003, US-ACL-004, US-ACL-005, US-ACL-006, US-ACL-007, US-ACL-008 |
| Carpetas | US-FOLDER-001, US-FOLDER-002, US-FOLDER-003, US-FOLDER-004, US-FOLDER-005 |
| Documentos | US-DOC-001, US-DOC-002, US-DOC-003, US-DOC-004, US-DOC-006 |
| Auditoría | US-AUDIT-001, US-AUDIT-002, US-AUDIT-003 |

### 🟡 Post-MVP (9 tickets) - Implementar después del 16 Ene

| Ticket | Descripción | Justificación Diferir |
|--------|-------------|----------------------|
| US-DOC-005 | Rollback a versión anterior | Feature avanzado, no crítico para MVP |
| US-ACL-009 | UI muestra capacidades por recurso | UX enhancement, funcionalidad base OK sin esto |
| US-AUDIT-004 | UI de consulta de auditoría | Admin puede consultar vía API/DB directamente |
| US-SEARCH-001 | Búsqueda de documentos (API) | Feature completo, requiere indexación |
| US-SEARCH-002 | Búsqueda con filtro de permisos | Depende de US-SEARCH-001 |
| US-SEARCH-003 | UI de búsqueda | Depende de US-SEARCH-001, 002 |
| US-AUTH-007 | Implementación de Refresh Token | Mejora UX y seguridad, no bloquea MVP |

### 🟢 Completado

| Item | Descripción | Fecha |
|------|-------------|-------|
| INFRA-001 | Docker Compose configurado con PostgreSQL, MongoDB, MinIO, Redis, Kafka, Vault | 31 Dic 2025 |
| INFRA-002 | Scaffolding backend (6 microservicios con arquitectura hexagonal) | 31 Dic 2025 |
| INFRA-003 | Scaffolding frontend (React + Vite + TypeScript + Tailwind) | 31 Dic 2025 |
| US-AUTH-001 | Login multi-organización (Backend completo: modelos, endpoint `/auth/login`, lógica de membresías) | 4 Ene 2026 |
| US-AUTH-002 | Token JWT con claims de org/roles (Backend y Frontend) | 5 Ene 2026 |
| US-AUTH-003 | Middleware de autenticación (Backend y Frontend: filtro JWT, interceptor Axios, manejo global de 401) | 8 Ene 2026 |

---

## 4. Próximos Pasos Recomendados

**Siguiente objetivo: consolidar autenticación y avanzar con administración.**

1. **Iniciar US-AUTH-004 - Aislamiento de datos por tenant** (`identity-service`, `document-core-service`):
    - Agregar columna `organizacion_id` a todas las tablas de negocio.
    - Implementar `TenantContext` y filtros automáticos en JPA.

2. **Desarrollar US-AUTH-005 y US-AUTH-006 en frontend** para completar el flujo de login y manejo de sesión expirada.

3. **Preparar inicio de US-ADMIN-001 (Crear usuario)**, ya desbloqueado por el avance en autenticación.

**Nota:** US-AUTH-003 completado. Se recomienda priorizar US-AUTH-004 y la UI de login para habilitar administración y permisos. US-AUTH-007 (Refresh Token) queda planificado como mejora post-MVP.

---

## 5. Notas de Desarrollo

### Día 8 (8 Ene 2026)
- [x] US-AUTH-003 completado: Middleware de autenticación implementado en backend y frontend. Endpoints protegidos, manejo global de 401 y pruebas de integración exitosas.

---

*Documento generado el 8 de enero de 2026. Actualizar diariamente con el progreso.*
