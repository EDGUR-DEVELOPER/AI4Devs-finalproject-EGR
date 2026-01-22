## P4 — Documentos + Versionado Lineal

### [US-DOC-002] Descargar versión actual (API)

---

## 1. Resumen de alcance detectado

### Capacidades encontradas:
- Descarga del binario de la versión actual del documento
- Validación de permisos de LECTURA sobre el documento
- Streaming del archivo al cliente
- Headers HTTP apropiados (Content-Type, Content-Disposition)

### Restricciones implícitas:
- Solo usuarios con permiso LECTURA pueden descargar
- Se descarga la versión marcada como `version_actual_id`
- El documento debe pertenecer al organizacion_id del token
- Debe manejar archivos de cualquier tamaño eficientemente

### Riesgos o ambigüedades:
- No se especifica si se debe validar existencia del archivo físico
- No se define comportamiento si el archivo está corrupto
- No se menciona soporte para descarga parcial (Range headers)
- No se especifica logging de descargas

---

## 2. Lista de tickets necesarios

---
### Base de datos
---

* **Título:** Query optimizado para obtener documento con versión actual
* **Objetivo:** Recuperar eficientemente el documento con su versión actual y ruta de almacenamiento.
* **Tipo:** Tarea
* **Descripción corta:** Crear consulta o método de repositorio que dado `documento_id` y `organizacion_id`, retorne el documento con join a su `version_actual` incluyendo `ruta_almacenamiento`.
* **Entregables:**
    - Método `findDocumentWithCurrentVersion(documentId, orgId)`.
    - Índice optimizado si es necesario.
    - Test de integración de BD.

---
### Backend
---

* **Título:** Implementar validador de permisos de lectura para documento
* **Objetivo:** Verificar que el usuario tiene LECTURA sobre el documento.
* **Tipo:** Tarea
* **Descripción corta:** Crear método que dado `usuario_id`, `documento_id` y `organizacion_id`, verifique permiso LECTURA aplicando regla de precedencia (documento > carpeta > herencia).
* **Entregables:**
    - Método `hasReadPermission(userId, documentId, orgId)`.
    - Integración con sistema ACL (P2).
    - Soporte para permisos explícitos de documento y heredados de carpeta.

---

* **Título:** Implementar servicio de descarga de archivo
* **Objetivo:** Recuperar el binario desde el storage para streaming.
* **Tipo:** Tarea
* **Descripción corta:** Agregar método `download(path)` al `StorageService` que retorne un stream del archivo. Manejar caso de archivo no encontrado.
* **Entregables:**
    - Método `IStorageService.download(path): Stream`.
    - Implementación en `LocalStorageService`.
    - Excepción específica `FileNotFoundException`.

---

* **Título:** Implementar servicio de descarga de documento
* **Objetivo:** Orquestar validación de permisos y recuperación del archivo.
* **Tipo:** Historia
* **Descripción corta:** Crear `DocumentService.download()` que: valide existencia del documento en el tenant, valide permisos de lectura, obtenga la versión actual y retorne stream con metadatos.
* **Entregables:**
    - Método `downloadDocument(documentId, userId, orgId)`.
    - DTO con stream, nombre, extensión, tamaño.
    - Manejo de errores: 404 si no existe, 403 si no tiene permiso.

---

* **Título:** Implementar endpoint `GET /api/documents/{documentId}/download`
* **Objetivo:** Exponer API REST para descarga de documentos.
* **Tipo:** Historia
* **Descripción corta:** Endpoint que valida token, invoca servicio de descarga y retorna el binario con headers apropiados: `Content-Type`, `Content-Disposition`, `Content-Length`.
* **Entregables:**
    - Controlador con ruta `GET /api/documents/{documentId}/download`.
    - Headers HTTP correctos para forzar descarga.
    - Respuesta 200 con binario, 403 sin permiso, 404 si no existe.

---

* **Título:** Configurar MIME types para descarga
* **Objetivo:** Retornar el Content-Type correcto según extensión del archivo.
* **Tipo:** Tarea
* **Descripción corta:** Implementar mapeo de extensiones a MIME types. Usar `application/octet-stream` como fallback para extensiones desconocidas.
* **Entregables:**
    - Utilidad `getMimeType(extension)`.
    - Mapeo de extensiones comunes (pdf, docx, xlsx, jpg, png, etc.).

---

* **Título:** Emitir evento de auditoría al descargar documento
* **Objetivo:** Registrar la acción de descarga para trazabilidad.
* **Tipo:** Tarea
* **Descripción corta:** Al completar descarga exitosa, emitir evento con `codigo_evento=DOCUMENTO_DESCARGADO`, `documento_id`, `version_id`, `usuario_id`.
* **Entregables:**
    - Integración con servicio de auditoría.
    - Evento emitido al inicio del streaming.

---

* **Título:** Pruebas unitarias del servicio de descarga
* **Objetivo:** Asegurar lógica correcta de validación y recuperación.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios para `DocumentService.download()` cubriendo: descarga exitosa, documento no encontrado, sin permisos, archivo físico no existe.
* **Entregables:**
    - Suite de tests unitarios (mínimo 5 casos).
    - Mocks de storage y repositorios.

---

* **Título:** Pruebas de integración del endpoint de descarga
* **Objetivo:** Verificar flujo completo HTTP incluyendo streaming.
* **Tipo:** QA
* **Descripción corta:** Tests de integración para `GET /api/documents/{id}/download` validando: 200 con contenido correcto, headers apropiados, 403 sin permisos, 404 documento inexistente.
* **Entregables:**
    - Tests de integración para escenarios de aceptación.
    - Verificación de headers HTTP.
    - Verificación de contenido del archivo.

---
### Frontend
---

* **Título:** Sin cambios de UI para US-DOC-002
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La funcionalidad de descarga en UI corresponde a `US-DOC-006`. Se puede crear colección de requests para testing manual.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.

---

## 3. Flujo recomendado de ejecución

```
1. [BD] Query optimizado documento + versión actual
   ↓
2. [Backend] Implementar validador de permisos de lectura
   ↓
3. [Backend] Implementar servicio de descarga de archivo (storage)
   ↓
4. [Backend] Configurar MIME types
   ↓
5. [Backend] Implementar servicio de descarga de documento
   ↓
6. [Backend] Implementar endpoint GET download
   ↓
7. [Backend] Emitir evento de auditoría
   ↓
8. [QA] Pruebas unitarias
   ↓
9. [QA] Pruebas de integración
```

### Dependencias entre tickets:
- Depende de US-DOC-001 (modelo de Documento y Version_Documento)
- Query de BD es prerequisito para servicio de descarga
- Storage service debe existir (de US-DOC-001) para agregar método download
- Validador de permisos depende del sistema ACL (P2)
- MIME types puede ejecutarse en paralelo
- Auditoría depende del sistema de auditoría (P5)

---

## 4. Recomendación TDD/BDD

### Tickets que deberían tener pruebas primero (TDD):
1. **Validador de permisos de lectura** - Seguridad crítica
2. **Servicio de descarga de archivo** - Manejo de streams y errores
3. **Servicio de descarga de documento** - Lógica de negocio central
4. **MIME types** - Mapeo con casos de borde

### Tickets que se prestan a escenarios BDD:
```gherkin
Feature: Descargar versión actual de documento

  Scenario: Descarga exitosa con permiso de lectura
    Given un usuario autenticado con permiso LECTURA sobre documento "informe.pdf"
    And el documento tiene version_actual con numero_secuencial 3
    When el usuario solicita descarga del documento
    Then recibe respuesta 200
    And el header "Content-Type" es "application/pdf"
    And el header "Content-Disposition" contiene "informe.pdf"
    And el contenido es el binario de la versión 3

  Scenario: Descarga rechazada sin permisos
    Given un usuario autenticado sin permiso sobre documento "confidencial.docx"
    When el usuario intenta descargar el documento
    Then recibe respuesta 403
    And el mensaje indica "Sin permisos de lectura"

  Scenario: Descarga de documento inexistente
    Given un usuario autenticado
    When el usuario solicita descarga del documento "uuid-inexistente"
    Then recibe respuesta 404
    And el mensaje indica "Documento no encontrado"

  Scenario: Descarga respeta aislamiento de tenant
    Given un usuario autenticado en organizacion "A"
    And existe documento "reporte.xlsx" en organizacion "B"
    When el usuario intenta descargar "reporte.xlsx"
    Then recibe respuesta 404
    And no se revela que el documento existe en otro tenant
```

---

## 5. Archivos generados

| Archivo | Contenido |
|---------|-----------|
| `US-DOC-001.md` | Subir documento (API) |
| `US-DOC-002.md` | Este archivo - Descargar versión actual (API) |
| `US-DOC-003.md` | Subir nueva versión (API) |
| `US-DOC-004.md` | Listar versiones (API) |
| `US-DOC-005.md` | Cambiar versión actual - rollback (API) |
| `US-DOC-006.md` | UI mínima de carga y ver historial |
