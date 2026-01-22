## P4 — Documentos + Versionado Lineal

### [US-DOC-006] UI mínima de carga y ver historial

---

## 1. Resumen de alcance detectado

### Capacidades encontradas:
- Interfaz para subir documentos desde el navegador
- Visualización del documento en el listado de la carpeta
- Interfaz para ver historial de versiones
- Identificación visual de la versión actual
- Navegación intuitiva entre carpeta y versiones
- Feedback visual de operaciones (carga, errores)

### Restricciones implícitas:
- La UI debe respetar los permisos del usuario (habilitar/deshabilitar acciones)
- Debe integrarse con las APIs desarrolladas en US-DOC-001 a US-DOC-005
- Diseño minimalista y funcional (MVP)
- Responsive o al menos usable en desktop

### Riesgos o ambigüedades:
- No se especifica si debe soportar drag & drop
- No se define el flujo exacto para ver versiones (¿modal, nueva página?)
- No se menciona si incluir funcionalidad de rollback en la UI mínima
- No se especifica indicador de progreso de carga

---

## 2. Lista de tickets necesarios

---
### Base de Datos
---

* **Título:** Verificar datos semilla para pruebas de UI
* **Objetivo:** Disponer de documentos y versiones de prueba para validar la UI.
* **Tipo:** Tarea
* **Descripción corta:** Asegurar que existen datos de prueba: carpetas con documentos, documentos con múltiples versiones, usuarios con diferentes permisos.
* **Entregables:**
    - Script de seed actualizado con documentos y versiones.
    - Documentación de escenarios de prueba disponibles.

---
### Backend
---

* **Título:** Verificar CORS y headers para upload desde frontend
* **Objetivo:** Asegurar que las APIs de documentos aceptan requests del frontend.
* **Tipo:** Tarea
* **Descripción corta:** Verificar configuración de CORS para endpoints de documentos. Habilitar headers necesarios para multipart/form-data desde el origen del frontend.
* **Entregables:**
    - Configuración CORS validada para endpoints `/api/documents/*`.
    - Headers `Access-Control-Allow-*` correctos.

---

* **Título:** Agregar endpoint de metadatos de documento para UI
* **Objetivo:** Proveer información del documento sin descargar el binario.
* **Tipo:** Tarea
* **Descripción corta:** Si no existe, crear `GET /api/documents/{documentId}` que retorne metadatos: nombre, extensión, version_actual, fecha_creacion, carpeta_id. Útil para mostrar información sin descargar.
* **Entregables:**
    - Endpoint `GET /api/documents/{documentId}`.
    - DTO con metadatos del documento.

---
### Frontend
---

* **Título:** Crear componente de subida de documento (Upload)
* **Objetivo:** Permitir al usuario seleccionar y subir archivos.
* **Tipo:** Historia
* **Descripción corta:** Implementar componente con input file y botón de subida. Mostrar nombre del archivo seleccionado antes de enviar. Soportar clic y opcionalmente drag & drop.
* **Entregables:**
    - Componente `DocumentUpload`.
    - Input file con estilos.
    - Preview del archivo seleccionado.

---

* **Título:** Integrar servicio de subida con API
* **Objetivo:** Conectar el componente de upload con el backend.
* **Tipo:** Tarea
* **Descripción corta:** Implementar llamada `POST /api/folders/{folderId}/documents` con multipart/form-data. Manejar estado de "cargando" y "error".
* **Entregables:**
    - Servicio/función `uploadDocument(folderId, file)`.
    - Manejo de FormData para multipart.
    - Estados de loading y error.

---

* **Título:** Implementar indicador de progreso de carga
* **Objetivo:** Informar al usuario del avance de la subida.
* **Tipo:** Tarea
* **Descripción corta:** Mostrar barra de progreso o spinner durante la subida. Usar eventos de progreso de XHR/fetch si es posible.
* **Entregables:**
    - Componente de progreso (barra o spinner).
    - Integración con evento onUploadProgress.

---

* **Título:** Mostrar documento en listado de carpeta post-subida
* **Objetivo:** Actualizar la vista de carpeta tras subir exitosamente.
* **Tipo:** Tarea
* **Descripción corta:** Al completar subida con éxito, refrescar el listado de la carpeta para mostrar el nuevo documento. Mostrar mensaje de éxito.
* **Entregables:**
    - Refresh automático del listado de carpeta.
    - Toast/alerta de "Documento subido exitosamente".

---

* **Título:** Crear componente de historial de versiones
* **Objetivo:** Mostrar lista de versiones de un documento.
* **Tipo:** Historia
* **Descripción corta:** Implementar componente que muestre tabla/lista con: número de versión, fecha, usuario creador, indicador de "actual". Accesible desde el documento en el listado.
* **Entregables:**
    - Componente `VersionHistory`.
    - Tabla/lista con datos de versiones.
    - Badge o indicador para versión actual.

---

* **Título:** Integrar servicio de listado de versiones con API
* **Objetivo:** Conectar el componente de historial con el backend.
* **Tipo:** Tarea
* **Descripción corta:** Implementar llamada `GET /api/documents/{documentId}/versions` y mapear respuesta al componente.
* **Entregables:**
    - Servicio/función `getDocumentVersions(documentId)`.
    - Manejo de estados loading, error, success.

---

* **Título:** Implementar navegación a historial de versiones
* **Objetivo:** Permitir al usuario acceder al historial desde el documento.
* **Tipo:** Tarea
* **Descripción corta:** Agregar botón/link "Ver versiones" en cada documento del listado. Al hacer clic, abrir modal o navegar a vista de historial.
* **Entregables:**
    - Botón "Versiones" en item de documento.
    - Modal o ruta `/documents/{id}/versions`.
    - Breadcrumb o forma de volver al listado.

---

* **Título:** Implementar acción de descarga desde UI
* **Objetivo:** Permitir descargar el documento desde el listado o historial.
* **Tipo:** Tarea
* **Descripción corta:** Agregar botón de descarga que invoque `GET /api/documents/{id}/download`. Iniciar descarga del binario en el navegador.
* **Entregables:**
    - Botón de descarga en listado y en historial.
    - Función que inicia descarga (blob + anchor click).

---

* **Título:** Habilitar/deshabilitar acciones según permisos
* **Objetivo:** Reflejar en la UI las capacidades del usuario.
* **Tipo:** Tarea
* **Descripción corta:** Consultar permisos del usuario (desde token o API) y habilitar/deshabilitar botones: "Subir" si tiene ESCRITURA, "Versiones" si tiene LECTURA. Integrar con sistema de permisos de P2.
* **Entregables:**
    - Lógica de verificación de permisos en frontend.
    - Botones deshabilitados visualmente con tooltip explicativo.

---

* **Título:** Manejo de errores y feedback visual
* **Objetivo:** Informar al usuario de errores de forma clara.
* **Tipo:** Tarea
* **Descripción corta:** Capturar errores 403 (permisos), 404 (no existe), 400 (validación) y mostrar mensajes amigables al usuario.
* **Entregables:**
    - Componente de Toast/Alert reutilizable.
    - Mensajes de error mapeados por código HTTP.

---

* **Título:** Implementar subida de nueva versión desde UI
* **Objetivo:** Permitir agregar versiones desde el historial.
* **Tipo:** Tarea
* **Descripción corta:** En la vista de historial, agregar botón "Subir nueva versión" que abra el uploader y llame a `POST /api/documents/{id}/versions`.
* **Entregables:**
    - Botón "Nueva versión" en componente de historial.
    - Reutilización del componente de upload.
    - Refresh del historial post-subida.

---

* **Título:** Estilos y diseño responsive para componentes de documentos
* **Objetivo:** Asegurar usabilidad y apariencia consistente.
* **Tipo:** Tarea
* **Descripción corta:** Aplicar estilos CSS/SASS a todos los componentes de documentos. Asegurar que funcionen en pantallas de escritorio. Diseño minimalista alineado con el resto de la app.
* **Entregables:**
    - Estilos para Upload, VersionHistory, botones de acción.
    - Consistencia con design system existente.

---
### QA / Testing
---

* **Título:** Pruebas E2E de flujo de subida de documento
* **Objetivo:** Validar el flujo completo de subida desde la UI.
* **Tipo:** QA
* **Descripción corta:** Ejecutar escenarios: seleccionar archivo, subir, verificar aparece en listado, verificar mensaje de éxito.
* **Entregables:**
    - Tests E2E (Cypress/Playwright) para flujo de subida.
    - Cobertura de casos de éxito y error.

---

* **Título:** Pruebas E2E de visualización de historial
* **Objetivo:** Validar el flujo de ver versiones desde la UI.
* **Tipo:** QA
* **Descripción corta:** Ejecutar escenarios: navegar a documento, abrir historial, verificar versiones ordenadas, verificar indicador de versión actual.
* **Entregables:**
    - Tests E2E para flujo de historial.
    - Verificación de orden y datos mostrados.

---

* **Título:** Pruebas de permisos en UI
* **Objetivo:** Validar que la UI respeta los permisos del usuario.
* **Tipo:** QA
* **Descripción corta:** Con diferentes usuarios (con/sin permisos), verificar que botones se habilitan/deshabilitan correctamente.
* **Entregables:**
    - Tests con usuario con LECTURA (no puede subir).
    - Tests con usuario con ESCRITURA (puede subir).

---

## 3. Flujo recomendado de ejecución

```
FASE 1 - Preparación:
1. [BD] Verificar datos semilla
2. [Backend] Verificar CORS y headers
3. [Backend] Agregar endpoint de metadatos (si no existe)

FASE 2 - Componente de Subida:
4. [Frontend] Crear componente de upload
   ↓
5. [Frontend] Integrar servicio de subida
   ↓
6. [Frontend] Implementar indicador de progreso
   ↓
7. [Frontend] Mostrar documento post-subida

FASE 3 - Historial de Versiones:
8. [Frontend] Crear componente de historial
   ↓
9. [Frontend] Integrar servicio de listado
   ↓
10. [Frontend] Implementar navegación a historial
    ↓
11. [Frontend] Implementar descarga desde UI
    ↓
12. [Frontend] Implementar subida nueva versión

FASE 4 - Pulido:
13. [Frontend] Habilitar/deshabilitar según permisos
    ↓
14. [Frontend] Manejo de errores
    ↓
15. [Frontend] Estilos y responsive

FASE 5 - QA:
16. [QA] Pruebas E2E subida
17. [QA] Pruebas E2E historial
18. [QA] Pruebas de permisos
```

### Dependencias entre tickets:
- Depende de todas las APIs (US-DOC-001 a US-DOC-005)
- Datos semilla son prerequisito para QA
- CORS es prerequisito para cualquier integración
- Componente upload es prerequisito para subida de nueva versión
- Componente historial es prerequisito para navegación y acciones en historial
- Estilos pueden ejecutarse en paralelo con funcionalidad
- QA requiere todo implementado

---

## 4. Recomendación TDD/BDD

### Tickets que deberían tener pruebas primero (TDD):
1. **Servicio de subida** - Lógica de FormData y manejo de errores
2. **Servicio de listado de versiones** - Mapeo de datos
3. **Lógica de permisos en frontend** - Seguridad visual

### Tickets que se prestan a escenarios BDD (E2E):
```gherkin
Feature: Subir documentos desde la UI

  Scenario: Subida exitosa muestra documento en carpeta
    Given un usuario autenticado con permiso ESCRITURA en carpeta "Proyectos"
    And estoy en la vista de carpeta "Proyectos"
    When selecciono el archivo "informe.pdf" para subir
    And hago clic en "Subir"
    Then veo un indicador de progreso
    And al completar, veo mensaje "Documento subido exitosamente"
    And el documento "informe.pdf" aparece en el listado de la carpeta

  Scenario: Subida muestra error sin permisos
    Given un usuario autenticado con solo permiso LECTURA
    Then el botón "Subir documento" está deshabilitado
    And al pasar el cursor veo "No tienes permisos de escritura"

Feature: Ver historial de versiones desde la UI

  Scenario: Historial muestra versiones ordenadas
    Given un documento "contrato.docx" con versiones 1, 2 y 3
    And la versión actual es la 3
    When navego al documento y abro "Versiones"
    Then veo una lista con 3 versiones
    And la versión 1 aparece primero
    And la versión 3 tiene indicador "Actual"

  Scenario: Descargar documento desde historial
    Given estoy en el historial de versiones de "reporte.xlsx"
    When hago clic en "Descargar" en la versión 2
    Then se inicia la descarga del archivo
    And el archivo tiene el contenido de la versión 2

  Scenario: Subir nueva versión desde historial
    Given estoy en el historial de versiones de un documento con 2 versiones
    And tengo permiso ESCRITURA
    When hago clic en "Nueva versión"
    And selecciono un archivo y confirmo
    Then el historial se actualiza mostrando 3 versiones
    And la versión 3 tiene indicador "Actual"
```

---

## 5. Archivos generados

| Archivo | Contenido |
|---------|-----------|
| `US-DOC-001.md` | Subir documento (API) |
| `US-DOC-002.md` | Descargar versión actual (API) |
| `US-DOC-003.md` | Subir nueva versión (API) |
| `US-DOC-004.md` | Listar versiones (API) |
| `US-DOC-005.md` | Cambiar versión actual - rollback (API) |
| `US-DOC-006.md` | Este archivo - UI mínima de carga y ver historial |

---

## 6. Resumen de componentes UI a desarrollar

| Componente | Funcionalidad |
|------------|---------------|
| `DocumentUpload` | Selección y subida de archivos |
| `UploadProgress` | Indicador de progreso de carga |
| `VersionHistory` | Lista de versiones de un documento |
| `VersionItem` | Fila individual de versión con acciones |
| `DownloadButton` | Botón reutilizable para descargar |
| `PermissionGate` | Wrapper para habilitar/deshabilitar por permisos |
