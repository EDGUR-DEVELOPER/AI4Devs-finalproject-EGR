## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-002] Conceder permiso de carpeta a usuario (crear ACL)

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Crear ACL (entrada de control de acceso) para una carpeta
- Asignar un nivel de acceso específico a un usuario sobre una carpeta
- Validación de pertenencia al mismo organizacion (aislamiento multi-tenant)

### Restricciones Implícitas
- Solo administradores pueden asignar permisos
- Usuario y carpeta deben pertenecer al mismo organizacion del token
- No debe filtrar información de otros organizacions (404/403 genérico)
- El permiso creado permite al usuario acceder según el nivel asignado

### Riesgos o Ambigüedades
- No se especifica si un usuario puede tener múltiples ACLs con diferentes niveles sobre la misma carpeta
- **Suposición:** Un usuario tiene una única entrada ACL por carpeta (se actualiza si ya existe)
- No se detalla el comportamiento si el usuario ya tiene el permiso
- **Suposición:** Se actualiza el nivel si existe, o se devuelve éxito sin cambio

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** Crear tabla de ACL para carpetas
* **Objetivo:** Persistir los permisos asignados a usuarios sobre carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Implementar tabla `ACL_Carpeta` con campos: `id`, `carpeta_id` (FK), `usuario_id` (FK), `nivel_acceso_id` (FK), `recursivo` (boolean), `organizacion_id` (FK), `creado_por`, `fecha_creacion`, `fecha_modificacion`. Índice único en (`carpeta_id`, `usuario_id`).
* **Entregables:**
    - Migración SQL con tabla `ACL_Carpeta`.
    - Índice único para evitar duplicados por usuario-carpeta.
    - Foreign keys a tablas relacionadas.
    - Índices para queries frecuentes.

---

* **Título:** Datos semilla para pruebas de ACL de carpetas
* **Objetivo:** Facilitar pruebas automatizadas de escenarios ACL.
* **Tipo:** Tarea
* **Descripción corta:** Crear fixtures con: carpetas de prueba en diferentes organizacions, usuarios de prueba, y combinaciones de ACL (con permiso, sin permiso, cross-organizacion).
* **Entregables:**
    - Script de seed para escenarios de ACL.
    - Carpetas y usuarios de prueba por organizacion.
    - Documentación de datos de prueba.

---
### Backend
---

* **Título:** Crear modelo/entidad de ACL de Carpeta
* **Objetivo:** Representar el permiso de carpeta en el dominio.
* **Tipo:** Tarea
* **Descripción corta:** Implementar entidad `AclCarpeta` con relaciones a `Carpeta`, `Usuario`, `NivelAcceso`. Incluir timestamps y campo `recursivo` para herencia.
* **Entregables:**
    - Entidad `AclCarpeta` con mapeo ORM.
    - Relaciones Many-to-One correctamente configuradas.
    - DTOs de request/response.

---

* **Título:** Implementar repositorio de ACL de Carpeta
* **Objetivo:** Encapsular operaciones de persistencia de ACL.
* **Tipo:** Tarea
* **Descripción corta:** Crear repositorio con métodos: `findByUserAndFolder()`, `findByFolder()`, `findByUser()`, `save()`, `delete()`. Todos los métodos deben filtrar por `organizacion_id`.
* **Entregables:**
    - Repositorio `AclCarpetaRepository`.
    - Métodos CRUD con filtro de organizacion.
    - Query optimizado para búsqueda por usuario y carpeta.

---

* **Título:** Servicio de gestión de ACL de carpetas
* **Objetivo:** Centralizar lógica de negocio para asignar permisos.
* **Tipo:** Tarea
* **Descripción corta:** Implementar servicio que valide: existencia de carpeta en organizacion, existencia de usuario en organizacion, validez del nivel de acceso. Si el ACL existe, actualizar; si no, crear.
* **Entregables:**
    - Servicio `AclCarpetaService`.
    - Método `concederPermiso(carpetaId, usuarioId, nivelCodigo, recursivo)`.
    - Validaciones de dominio y errores descriptivos.

---

* **Título:** Validación de pertenencia a organizacion para carpeta
* **Objetivo:** Asegurar aislamiento multi-tenant en operaciones de ACL.
* **Tipo:** Tarea
* **Descripción corta:** Implementar o reutilizar servicio que valide que una carpeta pertenece al organizacion del token. Devolver 404 (no filtrar información) si no pertenece.
* **Entregables:**
    - Método `validarCarpetaEnOrganizacion(carpetaId, organizacionId)`.
    - Error genérico 404 para no filtrar datos.

---

* **Título:** Validación de pertenencia a organizacion para usuario destino
* **Objetivo:** Evitar asignar permisos a usuarios de otros organizacions.
* **Tipo:** Tarea
* **Descripción corta:** Validar que el usuario al que se asigna el permiso tiene membresía activa en el organizacion del token.
* **Entregables:**
    - Método `validarUsuarioEnOrganizacion(usuarioId, organizacionId)`.
    - Error 404 si usuario no pertenece (sin filtrar).

---

* **Título:** Implementar endpoint `POST /carpetas/{id}/permisos` (crear ACL)
* **Objetivo:** Cumplir scenario 1 y 2 de la historia.
* **Tipo:** Historia
* **Descripción corta:** Endpoint protegido que recibe `usuario_id` y `nivel_acceso` (y opcionalmente `recursivo`). Valida pertenencia, crea/actualiza ACL y devuelve 201/200.
* **Entregables:**
    - Ruta/controlador `POST /carpetas/{id}/permisos`.
    - Request body: `{ usuario_id, nivel_acceso, recursivo }`.
    - Respuestas: 201 (creado), 200 (actualizado), 404 (carpeta/usuario no encontrado), 403 (sin permisos admin).
    - Documentación OpenAPI.

---

* **Título:** Guard/Middleware de autorización para administradores
* **Objetivo:** Proteger endpoint de asignación de permisos.
* **Tipo:** Tarea
* **Descripción corta:** Implementar guard que verifique que el usuario autenticado tiene rol de administrador o permiso `ADMINISTRACION` sobre la carpeta para poder asignar permisos.
* **Entregables:**
    - Guard `RequiereAdminOAdministracionCarpeta`.
    - Integración con endpoint de permisos.
    - Respuesta 403 si no tiene permisos.

---

* **Título:** Pruebas unitarias del servicio de ACL de carpetas
* **Objetivo:** Asegurar lógica de negocio correcta.
* **Tipo:** QA
* **Descripción corta:** Tests que cubran: crear nuevo ACL, actualizar ACL existente, validar carpeta inexistente, validar usuario de otro organizacion, validar nivel de acceso inválido.
* **Entregables:**
    - Suite de tests unitarios para `AclCarpetaService`.
    - Mocks de repositorios y validadores.
    - Cobertura de casos happy path y error.

---

* **Título:** Pruebas de integración del endpoint de crear ACL
* **Objetivo:** Verificar flujo completo y aislamiento multi-tenant.
* **Tipo:** QA
* **Descripción corta:** Tests E2E que verifiquen: admin puede crear ACL en su organizacion, no puede crear ACL en carpeta de otro organizacion (404), no puede asignar a usuario de otro organizacion (404).
* **Entregables:**
    - Tests de integración con datos seed.
    - Verificación de aislamiento multi-tenant.
    - Validación de persistencia correcta.

---
### Frontend
---

* **Título:** Servicio para gestionar ACL de carpetas
* **Objetivo:** Consumir API de permisos desde el frontend.
* **Tipo:** Tarea
* **Descripción corta:** Implementar servicio con método `asignarPermisoCarpeta(carpetaId, usuarioId, nivelAcceso, recursivo)`. Manejar errores y transformar respuestas.
* **Entregables:**
    - Servicio `AclCarpetaService` en frontend.
    - Tipos TypeScript para request/response.
    - Manejo de errores HTTP.

---

* **Título:** Modal/Formulario para asignar permiso a carpeta
* **Objetivo:** Permitir a administradores asignar permisos desde la UI.
* **Tipo:** Tarea
* **Descripción corta:** Crear modal con: selector de usuario (del organizacion), selector de nivel de acceso, checkbox para recursivo. Validar campos y mostrar feedback de éxito/error.
* **Entregables:**
    - Componente `AsignarPermisoCarpetaModal`.
    - Integración con selector de usuarios.
    - Integración con selector de niveles de acceso.
    - Feedback visual de operación.

---

* **Título:** Integrar acción "Administrar permisos" en vista de carpetas
* **Objetivo:** Acceder al modal de permisos desde la navegación.
* **Tipo:** Tarea
* **Descripción corta:** Agregar botón/opción "Administrar permisos" en el menú contextual o toolbar de carpetas. Visible solo para usuarios con permiso de administración.
* **Entregables:**
    - Botón/menú "Administrar permisos".
    - Condicional de visibilidad por permisos.
    - Apertura del modal al hacer click.

---

## 3. Flujo Recomendado de Ejecución

```
1. [BD] Crear tabla de ACL para carpetas
   ↓
2. [BD] Datos semilla para pruebas
   ↓
3. [BE] Crear modelo/entidad de ACL de Carpeta
   ↓
4. [BE] Implementar repositorio de ACL
   ↓
5. [BE] Validación de pertenencia a organizacion (carpeta + usuario)
   ↓
6. [BE] Servicio de gestión de ACL de carpetas
   ↓
7. [BE] Guard de autorización para administradores
   ↓
8. [BE] Implementar endpoint POST /carpetas/{id}/permisos
   ↓
9. [QA] Pruebas unitarias + integración
   ↓
10. [FE] Servicio para gestionar ACL
   ↓
11. [FE] Modal para asignar permiso
   ↓
12. [FE] Integrar acción en vista de carpetas
```

### Dependencias entre Tickets
- Requiere tabla `Carpeta` de épica P3 (Gestión de carpetas)
- Requiere tabla `Nivel_Acceso` de US-ACL-001
- Frontend depende de endpoint funcional
- Guard depende de US-ACL-006 (evaluación de permisos) para verificar ADMINISTRACION

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Servicio de gestión de ACL (lógica de negocio compleja)
- Validaciones de pertenencia a organizacion
- Repositorio de ACL

### Tickets para Escenarios BDD
```gherkin
Feature: Conceder permiso de carpeta a usuario
  
  Scenario: Admin concede permiso LECTURA a usuario de su organizacion
    Given un administrador autenticado del organizacion "A"
    And una carpeta "Documentos" del organizacion "A"
    And un usuario "juan@test.com" del organizacion "A"
    When asigno permiso "LECTURA" al usuario sobre la carpeta
    Then recibo status 201
    And el usuario puede listar la carpeta

  Scenario: Admin intenta asignar permiso a carpeta de otro organizacion
    Given un administrador autenticado del organizacion "A"
    And una carpeta "Confidencial" del organizacion "B"
    When intento asignar permiso sobre la carpeta
    Then recibo status 404
    And no se revela que la carpeta existe

  Scenario: Admin intenta asignar permiso a usuario de otro organizacion
    Given un administrador autenticado del organizacion "A"
    And una carpeta "Documentos" del organizacion "A"
    And un usuario "maria@test.com" del organizacion "B"
    When intento asignar permiso al usuario
    Then recibo status 404
```

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | Crear tabla de ACL para carpetas |
| 2 | BD | Datos semilla para pruebas de ACL |
| 3 | BE | Crear modelo/entidad de ACL de Carpeta |
| 4 | BE | Implementar repositorio de ACL de Carpeta |
| 5 | BE | Servicio de gestión de ACL de carpetas |
| 6 | BE | Validación de pertenencia a organizacion (carpeta) |
| 7 | BE | Validación de pertenencia a organizacion (usuario) |
| 8 | BE | Implementar endpoint POST /carpetas/{id}/permisos |
| 9 | BE | Guard/Middleware de autorización admin |
| 10 | QA | Pruebas unitarias del servicio ACL |
| 11 | QA | Pruebas de integración del endpoint |
| 12 | FE | Servicio para gestionar ACL |
| 13 | FE | Modal para asignar permiso a carpeta |
| 14 | FE | Integrar acción "Administrar permisos" |
