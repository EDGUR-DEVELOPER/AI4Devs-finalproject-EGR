## P4 — Documentos + Versionado Lineal

### [US-DOC-004] Listar versiones (API) ordenadas

---

## 1. Resumen de alcance detectado

### Capacidades encontradas:
- Consulta del historial de versiones de un documento
- Ordenamiento ascendente por `numero_secuencial`
- Información de cada versión (número, fecha, usuario creador)
- Identificación de cuál es la versión actual
- Validación de permisos de LECTURA

### Restricciones implícitas:
- El usuario debe tener permiso LECTURA sobre el documento
- El documento debe pertenecer al organizacion_id del token
- El listado debe estar siempre ordenado ascendentemente
- Debe incluir metadatos suficientes para identificar cada versión

### Riesgos o ambigüedades:
- No se especifica paginación (¿documentos con muchas versiones?)
- No se define qué metadatos incluir por versión
- No se menciona si incluir usuario que creó cada versión
- No se especifica formato de fecha

---

## 2. Lista de tickets necesarios

---
### Base de datos
---

* **Título:** Query optimizado para listar versiones de documento
* **Objetivo:** Recuperar eficientemente todas las versiones ordenadas con metadatos.
* **Tipo:** Tarea
* **Descripción corta:** Crear consulta que dado `documento_id` y `organizacion_id`, retorne todas las versiones con join al usuario creador, ordenadas ascendentemente por `numero_secuencial`.
* **Entregables:**
    - Método `findVersionsByDocumentId(documentId, orgId)`.
    - Índice en `(documento_id, numero_secuencial)` validado.
    - Test de integración verificando orden correcto.

---

* **Título:** Índice para ordenamiento eficiente de versiones
* **Objetivo:** Garantizar rendimiento en consultas de historial.
* **Tipo:** Tarea
* **Descripción corta:** Verificar/crear índice que optimice la consulta de versiones ordenadas por `numero_secuencial` para un documento específico.
* **Entregables:**
    - Índice `idx_version_documento_secuencial` si no existe.
    - Análisis de plan de ejecución.

---
### Backend
---

* **Título:** Definir DTO de respuesta para lista de versiones
* **Objetivo:** Estandarizar la estructura de datos retornada.
* **Tipo:** Diseño / Tarea
* **Descripción corta:** Definir interface/clase para respuesta de listado incluyendo: `version_id`, `numero_secuencial`, `fecha_creacion`, `creado_por` (nombre/email), `tamanio_bytes`, `es_version_actual`.
* **Entregables:**
    - DTO `VersionListItemDto`.
    - DTO `VersionListResponseDto` con array y metadata.
    - Documentación de campos.

---

* **Título:** Implementar servicio de listado de versiones
* **Objetivo:** Orquestar validación y consulta de versiones.
* **Tipo:** Historia
* **Descripción corta:** Crear `DocumentService.listVersions()` que: valide existencia del documento en el tenant, valide permisos de lectura, consulte versiones ordenadas y mapee a DTOs incluyendo flag `es_version_actual`.
* **Entregables:**
    - Método `listVersions(documentId, userId, orgId)`.
    - Mapeo de entidades a DTOs.
    - Cálculo de `es_version_actual` comparando con `version_actual_id`.

---

* **Título:** Implementar endpoint `GET /api/documents/{documentId}/versions`
* **Objetivo:** Exponer API REST para consulta de historial.
* **Tipo:** Historia
* **Descripción corta:** Endpoint que valida token, invoca servicio de listado y retorna array de versiones ordenadas con metadatos.
* **Entregables:**
    - Controlador con ruta `GET /api/documents/{documentId}/versions`.
    - Respuesta 200 con array de versiones.
    - Respuesta 403 sin permisos, 404 documento inexistente.

---

* **Título:** Implementar paginación opcional para versiones
* **Objetivo:** Manejar documentos con muchas versiones eficientemente.
* **Tipo:** Tarea
* **Descripción corta:** Agregar parámetros opcionales `page` y `limit` al endpoint. Si no se envían, retornar todas las versiones. Incluir metadata de paginación en respuesta.
* **Entregables:**
    - Query params `?page=1&limit=20`.
    - Respuesta con `total`, `page`, `limit`, `versions[]`.
    - Sin parámetros = todas las versiones.

---

* **Título:** Pruebas unitarias del servicio de listado
* **Objetivo:** Asegurar lógica correcta de consulta y ordenamiento.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios para `DocumentService.listVersions()` cubriendo: listado ordenado correctamente, flag `es_version_actual` correcto, documento no existe, sin permisos.
* **Entregables:**
    - Suite de tests unitarios (mínimo 5 casos).
    - Verificación de orden ascendente.
    - Verificación de mapeo a DTO.

---

* **Título:** Pruebas de integración del endpoint de listado
* **Objetivo:** Verificar flujo completo HTTP y formato de respuesta.
* **Tipo:** QA
* **Descripción corta:** Tests de integración para `GET /api/documents/{id}/versions` validando: 200 con versiones ordenadas, estructura de respuesta correcta, 403 sin permisos, 404 documento inexistente.
* **Entregables:**
    - Tests de integración para escenarios de aceptación.
    - Verificación de orden en respuesta JSON.
    - Verificación de paginación (si implementada).

---
### Frontend
---

* **Título:** Sin cambios de UI para US-DOC-004
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La interfaz de visualización de historial corresponde a `US-DOC-006`.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.

---

## 3. Flujo recomendado de ejecución

```
1. [BD] Índice para ordenamiento eficiente
   ↓
2. [BD] Query optimizado para listar versiones
   ↓
3. [Backend] Definir DTO de respuesta
   ↓
4. [Backend] Implementar servicio de listado
   ↓
5. [Backend] Implementar endpoint GET versions
   ↓
6. [Backend] Implementar paginación opcional
   ↓
7. [QA] Pruebas unitarias
   ↓
8. [QA] Pruebas de integración
```

### Dependencias entre tickets:
- Depende de US-DOC-001 y US-DOC-003 (modelos y versiones existentes)
- Índice de BD es prerequisito para query optimizado
- Query es prerequisito para servicio de listado
- DTO puede definirse en paralelo con query
- Paginación es mejora sobre endpoint básico
- QA requiere todo implementado

---

## 4. Recomendación TDD/BDD

### Tickets que deberían tener pruebas primero (TDD):
1. **Query de listado** - Verificar ordenamiento correcto
2. **Servicio de listado** - Lógica de mapeo y flag version_actual
3. **Paginación** - Cálculos de offset y límite

### Tickets que se prestan a escenarios BDD:
```gherkin
Feature: Listar versiones de documento

  Scenario: Listado ordenado ascendentemente
    Given un documento con versiones 1, 2, 3 creadas en ese orden
    And un usuario autenticado con permiso LECTURA
    When el usuario consulta las versiones del documento
    Then recibe respuesta 200
    And la primera versión tiene numero_secuencial 1
    And la segunda versión tiene numero_secuencial 2
    And la tercera versión tiene numero_secuencial 3

  Scenario: Listado indica versión actual
    Given un documento con versiones 1, 2, 3
    And version_actual_id apunta a la versión 3
    When el usuario consulta las versiones
    Then la versión 3 tiene es_version_actual = true
    And las versiones 1 y 2 tienen es_version_actual = false

  Scenario: Listado incluye metadatos de cada versión
    Given un documento con una versión creada por "juan@empresa.com"
    When el usuario consulta las versiones
    Then cada versión incluye version_id, numero_secuencial, fecha_creacion
    And cada versión incluye creado_por con información del usuario

  Scenario: Listado rechazado sin permisos
    Given un documento existente
    And un usuario autenticado sin permiso LECTURA
    When el usuario intenta consultar las versiones
    Then recibe respuesta 403

  Scenario: Listado de documento inexistente
    Given un usuario autenticado
    When el usuario consulta versiones de documento "uuid-inexistente"
    Then recibe respuesta 404

  Scenario: Listado con paginación
    Given un documento con 50 versiones
    When el usuario consulta con page=2 y limit=10
    Then recibe versiones 11 a 20
    And la respuesta incluye total=50, page=2, limit=10
```

---

## 5. Archivos generados

| Archivo | Contenido |
|---------|-----------|
| `US-DOC-001.md` | Subir documento (API) |
| `US-DOC-002.md` | Descargar versión actual (API) |
| `US-DOC-003.md` | Subir nueva versión (API) |
| `US-DOC-004.md` | Este archivo - Listar versiones (API) |
| `US-DOC-005.md` | Cambiar versión actual - rollback (API) |
| `US-DOC-006.md` | UI mínima de carga y ver historial |
