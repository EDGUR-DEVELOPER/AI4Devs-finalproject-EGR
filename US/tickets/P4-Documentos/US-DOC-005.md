## P4 — Documentos + Versionado Lineal

### [US-DOC-005] Cambiar versión actual (API) (rollback)

---

## 1. Resumen de alcance detectado

### Capacidades encontradas:
- Selección de una versión anterior como versión actual
- Actualización de `version_actual_id` del documento
- Rollback sin eliminar versiones existentes
- Registro de auditoría de la operación
- Validación de permisos especiales (ADMINISTRACION o similar)

### Restricciones implícitas:
- La versión seleccionada debe existir y pertenecer al documento
- El documento debe pertenecer al organizacion_id del token
- No se elimina ninguna versión (solo cambia el puntero)
- Debe registrarse auditoría obligatoriamente
- Requiere permiso elevado (no solo ESCRITURA)

### Riesgos o ambigüedades:
- No se especifica qué permiso exacto se requiere (¿ADMINISTRACION?)
- No se define si se puede seleccionar la misma versión actual
- No se menciona si hay restricciones en documentos con una sola versión
- No se especifica si afecta el numero_secuencial de futuras versiones

---

## 2. Lista de tickets necesarios

---
### Base de datos
---

* **Título:** Query para validar versión pertenece a documento
* **Objetivo:** Verificar que la versión solicitada existe y pertenece al documento.
* **Tipo:** Tarea
* **Descripción corta:** Crear consulta que dado `version_id`, `documento_id` y `organizacion_id`, valide que la versión existe, pertenece al documento y el documento pertenece al tenant.
* **Entregables:**
    - Método `validateVersionBelongsToDocument(versionId, documentId, orgId)`.
    - Test de integración de BD.

---

* **Título:** Query atómico para actualizar version_actual_id
* **Objetivo:** Actualizar el puntero de versión actual de forma segura.
* **Tipo:** Tarea
* **Descripción corta:** Implementar update atómico que modifique `version_actual_id` en la tabla `Documento`, incluyendo validación de existencia en la misma operación.
* **Entregables:**
    - Método `updateCurrentVersion(documentId, newVersionId, orgId)`.
    - Retorno de filas afectadas para verificación.

---
### Backend
---

* **Título:** Definir permiso requerido para rollback
* **Objetivo:** Establecer qué nivel de permiso permite cambiar la versión actual.
* **Tipo:** Diseño / Tarea
* **Descripción corta:** Documentar y configurar el permiso necesario para rollback. Recomendación: `ADMINISTRACION` sobre el documento/carpeta, o un permiso específico `ROLLBACK` si el modelo lo soporta.
* **Entregables:**
    - Documentación de decisión de diseño.
    - Configuración en sistema ACL si es necesario.
    - Constante `PERMISSION_ROLLBACK` o similar.

---

* **Título:** Implementar validador de permisos para rollback
* **Objetivo:** Verificar que el usuario tiene permiso para cambiar versión actual.
* **Tipo:** Tarea
* **Descripción corta:** Crear método que valide el permiso definido (ej. ADMINISTRACION) sobre el documento/carpeta.
* **Entregables:**
    - Método `hasRollbackPermission(userId, documentId, orgId)`.
    - Integración con sistema ACL.

---

* **Título:** Implementar servicio de cambio de versión actual
* **Objetivo:** Lógica de negocio para ejecutar el rollback.
* **Tipo:** Historia
* **Descripción corta:** Crear `DocumentService.setCurrentVersion()` que: valide documento existente, valide versión pertenece al documento, valide permisos de rollback, actualice `version_actual_id`, emita evento de auditoría.
* **Entregables:**
    - Método `setCurrentVersion(documentId, versionId, userId, orgId)`.
    - Validaciones completas antes de actualizar.
    - DTO de respuesta con nuevo estado del documento.

---

* **Título:** Implementar endpoint `PATCH /api/documents/{documentId}/current-version`
* **Objetivo:** Exponer API REST para operación de rollback.
* **Tipo:** Historia
* **Descripción corta:** Endpoint que recibe `version_id` en body, valida token, invoca servicio de cambio y retorna estado actualizado del documento.
* **Entregables:**
    - Controlador con ruta `PATCH /api/documents/{documentId}/current-version`.
    - Body request: `{ "version_id": "uuid" }`.
    - Respuesta 200 con documento actualizado.
    - Respuesta 400 si versión no válida, 403 sin permisos, 404 no existe.

---

* **Título:** Emitir evento de auditoría para rollback
* **Objetivo:** Registrar obligatoriamente la operación de rollback.
* **Tipo:** Tarea
* **Descripción corta:** Al completar rollback exitoso, emitir evento con `codigo_evento=VERSION_ROLLBACK`, `documento_id`, `version_anterior_id`, `version_nueva_id`, `usuario_id`.
* **Entregables:**
    - Integración con servicio de auditoría.
    - Evento incluye versión anterior y nueva para trazabilidad completa.

---

* **Título:** Pruebas unitarias del servicio de rollback
* **Objetivo:** Asegurar lógica correcta de cambio de versión.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios para `DocumentService.setCurrentVersion()` cubriendo: rollback exitoso, versión no pertenece al documento, documento no existe, sin permisos, versión no existe.
* **Entregables:**
    - Suite de tests unitarios (mínimo 6 casos).
    - Verificación de auditoría emitida.

---

* **Título:** Pruebas de integración del endpoint de rollback
* **Objetivo:** Verificar flujo completo HTTP y persistencia.
* **Tipo:** QA
* **Descripción corta:** Tests de integración para `PATCH /api/documents/{id}/current-version` validando: 200 con version_actual actualizado, 403 sin permisos, 404 documento/versión inexistente, 400 versión no válida.
* **Entregables:**
    - Tests de integración para escenarios de aceptación.
    - Verificación de `version_actual_id` en BD.
    - Verificación de evento de auditoría creado.

---
### Frontend
---

* **Título:** Sin cambios de UI para US-DOC-005
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La funcionalidad de rollback en UI corresponde a `US-DOC-006` (si se incluye en la UI mínima).
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.

---

## 3. Flujo recomendado de ejecución

```
1. [Diseño] Definir permiso requerido para rollback
   ↓
2. [BD] Query validar versión pertenece a documento
   ↓
3. [BD] Query atómico para actualizar version_actual_id
   ↓
4. [Backend] Implementar validador de permisos para rollback
   ↓
5. [Backend] Implementar servicio de cambio de versión
   ↓
6. [Backend] Implementar endpoint PATCH
   ↓
7. [Backend] Emitir evento de auditoría
   ↓
8. [QA] Pruebas unitarias
   ↓
9. [QA] Pruebas de integración
```

### Dependencias entre tickets:
- Depende de US-DOC-001, US-DOC-003 (modelos y versiones)
- Depende de US-DOC-004 implícitamente (para ver qué versiones existen)
- Decisión de diseño de permisos es prerequisito
- Queries de BD son prerequisito para servicio
- Auditoría depende del sistema de auditoría (P5)
- Auditoría es crítica y no opcional para esta historia

---

## 4. Recomendación TDD/BDD

### Tickets que deberían tener pruebas primero (TDD):
1. **Validación versión pertenece a documento** - Seguridad
2. **Validador de permisos rollback** - Autorización crítica
3. **Servicio de cambio de versión** - Lógica de negocio central
4. **Emisión de auditoría** - Trazabilidad obligatoria

### Tickets que se prestan a escenarios BDD:
```gherkin
Feature: Cambiar versión actual (rollback)

  Scenario: Rollback exitoso a versión anterior
    Given un documento con versiones 1, 2, 3
    And version_actual es la versión 3
    And un usuario autenticado con permiso ADMINISTRACION
    When el usuario selecciona la versión 2 como actual
    Then recibe respuesta 200
    And version_actual del documento ahora es la versión 2
    And se registra evento de auditoría VERSION_ROLLBACK

  Scenario: Rollback preserva todas las versiones
    Given un documento con versiones 1, 2, 3
    When el usuario hace rollback a versión 1
    Then las versiones 1, 2 y 3 siguen existiendo
    And solo cambia el puntero version_actual_id

  Scenario: Rollback rechazado sin permiso elevado
    Given un documento existente
    And un usuario autenticado con solo permiso ESCRITURA (no ADMINISTRACION)
    When el usuario intenta hacer rollback
    Then recibe respuesta 403
    And el mensaje indica "Permiso insuficiente para rollback"

  Scenario: Rollback rechazado con versión de otro documento
    Given documento A con versión "v-abc"
    And documento B con versión "v-xyz"
    When el usuario intenta hacer rollback de documento A a versión "v-xyz"
    Then recibe respuesta 400
    And el mensaje indica "Versión no pertenece al documento"

  Scenario: Auditoría de rollback incluye versiones anterior y nueva
    Given un documento con version_actual = versión 3
    When el usuario hace rollback a versión 1
    Then el evento de auditoría incluye version_anterior_id = versión 3
    And el evento de auditoría incluye version_nueva_id = versión 1

  Scenario: Rollback respeta aislamiento de tenant
    Given un documento en organizacion A
    And un usuario autenticado en organizacion B
    When el usuario intenta hacer rollback
    Then recibe respuesta 404
    And no se revela que el documento existe en otro tenant
```

---

## 5. Archivos generados

| Archivo | Contenido |
|---------|-----------|
| `US-DOC-001.md` | Subir documento (API) |
| `US-DOC-002.md` | Descargar versión actual (API) |
| `US-DOC-003.md` | Subir nueva versión (API) |
| `US-DOC-004.md` | Listar versiones (API) |
| `US-DOC-005.md` | Este archivo - Cambiar versión actual (rollback) |
| `US-DOC-006.md` | UI mínima de carga y ver historial |
