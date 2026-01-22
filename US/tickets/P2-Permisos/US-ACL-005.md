## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-005] Conceder permiso explícito a documento

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Crear ACL (entrada de control de acceso) directamente sobre un documento
- Asignar nivel de acceso específico a un usuario sobre un documento
- Manejar excepciones de acceso a nivel de documento individual

### Restricciones Implícitas
- Solo administradores pueden asignar permisos a documentos
- Documento y usuario deben pertenecer al mismo organizacion
- El permiso de documento es independiente del permiso de carpeta contenedora
- Sirve para casos excepcionales (dar acceso específico a un documento)

### Riesgos o Ambigüedades
- No se especifica si un usuario puede tener múltiples ACLs sobre el mismo documento
- **Suposición:** Un usuario tiene una única entrada ACL por documento (se actualiza)
- Interacción con permiso de carpeta se define en US-ACL-006

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** Crear tabla de ACL para documentos
* **Objetivo:** Persistir los permisos asignados a usuarios sobre documentos individuales.
* **Tipo:** Tarea
* **Descripción corta:** Implementar tabla `ACL_Documento` con campos: `id`, `documento_id` (FK), `usuario_id` (FK), `nivel_acceso_id` (FK), `organizacion_id` (FK), `creado_por`, `fecha_creacion`, `fecha_modificacion`. Índice único en (`documento_id`, `usuario_id`).
* **Entregables:**
    - Migración SQL con tabla `ACL_Documento`.
    - Índice único para evitar duplicados.
    - Foreign keys a tablas relacionadas.
    - Índices para queries frecuentes.

---

* **Título:** Datos semilla para pruebas de ACL de documentos
* **Objetivo:** Facilitar pruebas automatizadas de escenarios ACL de documentos.
* **Tipo:** Tarea
* **Descripción corta:** Crear fixtures con: documentos de prueba en diferentes carpetas/organizacions, combinaciones de ACL documento + carpeta para probar precedencia.
* **Entregables:**
    - Script de seed para escenarios de ACL documento.
    - Documentos de prueba con y sin permisos.
    - Casos para probar US-ACL-006 (precedencia).

---
### Backend
---

* **Título:** Crear modelo/entidad de ACL de Documento
* **Objetivo:** Representar el permiso de documento en el dominio.
* **Tipo:** Tarea
* **Descripción corta:** Implementar entidad `AclDocumento` con relaciones a `Documento`, `Usuario`, `NivelAcceso`. Similar a `AclCarpeta` pero sin campo `recursivo`.
* **Entregables:**
    - Entidad `AclDocumento` con mapeo ORM.
    - Relaciones Many-to-One configuradas.
    - DTOs de request/response.

---

* **Título:** Implementar repositorio de ACL de Documento
* **Objetivo:** Encapsular operaciones de persistencia de ACL de documentos.
* **Tipo:** Tarea
* **Descripción corta:** Crear repositorio con métodos: `findByUserAndDocument()`, `findByDocument()`, `findByUser()`, `save()`, `delete()`. Todos filtran por `organizacion_id`.
* **Entregables:**
    - Repositorio `AclDocumentoRepository`.
    - Métodos CRUD con filtro de organizacion.
    - Query optimizado por usuario y documento.

---

* **Título:** Servicio de gestión de ACL de documentos
* **Objetivo:** Centralizar lógica de negocio para asignar permisos a documentos.
* **Tipo:** Tarea
* **Descripción corta:** Implementar servicio que valide: existencia de documento en organizacion, existencia de usuario en organizacion, validez del nivel de acceso. Crear o actualizar ACL.
* **Entregables:**
    - Servicio `AclDocumentoService`.
    - Método `concederPermiso(documentoId, usuarioId, nivelCodigo)`.
    - Validaciones de dominio y errores descriptivos.

---

* **Título:** Validación de pertenencia a organizacion para documento
* **Objetivo:** Asegurar aislamiento multi-tenant en operaciones de ACL de documentos.
* **Tipo:** Tarea
* **Descripción corta:** Implementar o reutilizar método que valide que un documento pertenece al organizacion del token. Devolver 404 si no pertenece.
* **Entregables:**
    - Método `validarDocumentoEnOrganizacion(documentoId, organizacionId)`.
    - Error genérico 404 para no filtrar datos.

---

* **Título:** Implementar endpoint `POST /documentos/{id}/permisos` (crear ACL documento)
* **Objetivo:** Cumplir scenario de la historia.
* **Tipo:** Historia
* **Descripción corta:** Endpoint protegido que recibe `usuario_id` y `nivel_acceso`. Valida pertenencia, crea/actualiza ACL y devuelve 201/200.
* **Entregables:**
    - Ruta/controlador `POST /documentos/{id}/permisos`.
    - Request body: `{ usuario_id, nivel_acceso }`.
    - Respuestas: 201, 200, 404, 403.
    - Documentación OpenAPI.

---

* **Título:** Implementar endpoint `DELETE /documentos/{id}/permisos/{usuarioId}` (revocar)
* **Objetivo:** Permitir revocar permisos de documento.
* **Tipo:** Historia
* **Descripción corta:** Endpoint para eliminar ACL de documento. Similar a revocación de carpeta. Requiere permisos de administración.
* **Entregables:**
    - Ruta/controlador `DELETE /documentos/{id}/permisos/{usuarioId}`.
    - Respuestas: 204, 404, 403.
    - Documentación OpenAPI.

---

* **Título:** Guard de autorización para administración de permisos de documento
* **Objetivo:** Proteger endpoints de asignación de permisos de documento.
* **Tipo:** Tarea
* **Descripción corta:** Verificar que el usuario tiene rol admin o permiso `ADMINISTRACION` sobre la carpeta contenedora del documento para poder asignar permisos al documento.
* **Entregables:**
    - Guard `RequiereAdminOAdministracionDocumento`.
    - Lógica: buscar carpeta padre y evaluar permiso.
    - Respuesta 403 si no tiene permisos.

---

* **Título:** Pruebas unitarias del servicio de ACL de documentos
* **Objetivo:** Asegurar lógica de negocio correcta.
* **Tipo:** QA
* **Descripción corta:** Tests que cubran: crear nuevo ACL documento, actualizar existente, validar documento inexistente, validar usuario de otro organizacion.
* **Entregables:**
    - Suite de tests unitarios para `AclDocumentoService`.
    - Cobertura de casos happy path y error.

---

* **Título:** Pruebas de integración de endpoints de ACL documento
* **Objetivo:** Verificar flujo completo de gestión de permisos de documento.
* **Tipo:** QA
* **Descripción corta:** Tests E2E para crear y revocar permisos de documento. Verificar aislamiento multi-tenant y efecto en acceso.
* **Entregables:**
    - Tests de integración POST y DELETE.
    - Verificación de aislamiento multi-tenant.

---
### Frontend
---

* **Título:** Servicio para gestionar ACL de documentos
* **Objetivo:** Consumir API de permisos de documentos.
* **Tipo:** Tarea
* **Descripción corta:** Implementar servicio con métodos `asignarPermisoDocumento()` y `revocarPermisoDocumento()`. Manejar errores y transformar respuestas.
* **Entregables:**
    - Servicio `AclDocumentoService` en frontend.
    - Tipos TypeScript para request/response.
    - Manejo de errores HTTP.

---

* **Título:** Modal para asignar permiso a documento
* **Objetivo:** Permitir administrar permisos de un documento individual.
* **Tipo:** Tarea
* **Descripción corta:** Crear modal similar al de carpetas pero para documentos. Incluir selector de usuario, nivel de acceso, y lista de permisos actuales con opción de revocar.
* **Entregables:**
    - Componente `AdministrarPermisosDocumentoModal`.
    - Formulario de asignación.
    - Lista de permisos actuales.
    - Acciones de agregar/revocar.

---

* **Título:** Integrar acción "Permisos" en menú de documento
* **Objetivo:** Acceder a administración de permisos desde la vista de documento.
* **Tipo:** Tarea
* **Descripción corta:** Agregar opción "Administrar permisos" en el menú contextual de documentos. Visible solo para usuarios con permiso de administración.
* **Entregables:**
    - Opción en menú contextual de documento.
    - Condicional de visibilidad por permisos.
    - Apertura del modal al hacer click.

---

## 3. Flujo Recomendado de Ejecución

```
1. [BD] Crear tabla de ACL para documentos
   ↓
2. [BD] Datos semilla para pruebas
   ↓
3. [BE] Crear modelo/entidad de ACL de Documento
   ↓
4. [BE] Implementar repositorio de ACL
   ↓
5. [BE] Validación de pertenencia a organizacion
   ↓
6. [BE] Servicio de gestión de ACL de documentos
   ↓
7. [BE] Guard de autorización
   ↓
8. [BE] Endpoints POST y DELETE
   ↓
9. [QA] Pruebas unitarias + integración
   ↓
10. [FE] Servicio para gestionar ACL
   ↓
11. [FE] Modal de permisos de documento
   ↓
12. [FE] Integrar en menú de documento
```

### Dependencias entre Tickets
- Requiere tabla `Documento` de épica P4
- Requiere tabla `Nivel_Acceso` de US-ACL-001
- Frontend depende de endpoints funcionales
- Se relaciona con US-ACL-006 para precedencia de permisos

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Servicio de gestión de ACL de documentos
- Validación de pertenencia a organizacion

### Tickets para Escenarios BDD
```gherkin
Feature: Permiso explícito a documento
  
  Scenario: Admin asigna permiso LECTURA a documento
    Given un administrador autenticado del organizacion "A"
    And un documento "Contrato.pdf" en carpeta "Docs" del organizacion "A"
    And un usuario "juan@test.com" sin permiso en la carpeta
    When asigno permiso "LECTURA" explícito al usuario sobre el documento
    Then recibo status 201
    And el usuario puede acceder al documento

  Scenario: Usuario con permiso explícito accede a documento
    Given usuario "juan@test.com" con permiso explícito "LECTURA" sobre "Contrato.pdf"
    When el usuario descarga el documento
    Then recibe status 200
    And obtiene el archivo

  Scenario: Admin revoca permiso explícito de documento
    Given un administrador autenticado
    And usuario "juan@test.com" con permiso sobre documento "Contrato.pdf"
    When revoco el permiso del documento
    Then recibo status 204
    And el usuario ya no puede acceder al documento
```

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | Crear tabla de ACL para documentos |
| 2 | BD | Datos semilla para pruebas de ACL documentos |
| 3 | BE | Crear modelo/entidad de ACL de Documento |
| 4 | BE | Implementar repositorio de ACL de Documento |
| 5 | BE | Servicio de gestión de ACL de documentos |
| 6 | BE | Validación de pertenencia a organizacion |
| 7 | BE | Implementar endpoint POST /documentos/{id}/permisos |
| 8 | BE | Implementar endpoint DELETE /documentos/{id}/permisos/{usuarioId} |
| 9 | BE | Guard de autorización para permisos documento |
| 10 | QA | Pruebas unitarias del servicio ACL documento |
| 11 | QA | Pruebas de integración de endpoints |
| 12 | FE | Servicio para gestionar ACL de documentos |
| 13 | FE | Modal para administrar permisos de documento |
| 14 | FE | Integrar acción "Permisos" en menú de documento |
