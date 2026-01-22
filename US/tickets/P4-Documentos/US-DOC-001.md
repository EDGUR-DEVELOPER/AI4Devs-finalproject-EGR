## P4 — Documentos + Versionado Lineal

### [US-DOC-001] Subir documento (API) crea documento + versión 1

---

## 1. Resumen de alcance detectado

### Capacidades encontradas:
- Subida de documentos a una carpeta específica
- Creación automática del registro de documento con metadatos
- Creación automática de la primera versión (numero_secuencial=1)
- Almacenamiento del binario del archivo
- Validación de permisos de ESCRITURA en la carpeta destino
- Respuesta estructurada con documento_id y version_actual

### Restricciones implícitas:
- El usuario debe tener permiso de ESCRITURA en la carpeta destino
- El documento debe pertenecer al organizacion_id del token
- La versión inicial siempre es numero_secuencial=1
- El binario debe almacenarse de forma segura (filesystem o blob storage)

### Riesgos o ambigüedades:
- No se especifica límite de tamaño de archivo
- No se definen tipos de archivo permitidos/bloqueados
- No se define estrategia de nombrado de archivos en storage
- No se especifica si se debe validar contenido malicioso

---

## 2. Lista de tickets necesarios

---
### Base de datos
---

* **Título:** Crear modelo de Documento con metadatos básicos
* **Objetivo:** Persistir la información principal del documento para su gestión.
* **Tipo:** Tarea
* **Descripción corta:** Implementar la tabla `Documento` con campos mínimos: `id`, `nombre`, `extension`, `carpeta_id`, `organizacion_id`, `version_actual_id`, `creado_por`, `fecha_creacion`, `fecha_eliminacion` (soft delete).
* **Entregables:**
    - Migración SQL para tabla `Documento`.
    - Índices para búsquedas por `carpeta_id` y `organizacion_id`.
    - Constraint de FK hacia `Carpeta` y `Organizacion`.

---

* **Título:** Crear modelo de Version_Documento para versionado lineal
* **Objetivo:** Almacenar cada versión del documento con su secuencia.
* **Tipo:** Tarea
* **Descripción corta:** Implementar la tabla `Version_Documento` con: `id`, `documento_id`, `numero_secuencial`, `ruta_almacenamiento`, `tamanio_bytes`, `hash_contenido`, `creado_por`, `fecha_creacion`.
* **Entregables:**
    - Migración SQL para tabla `Version_Documento`.
    - Índice único compuesto `(documento_id, numero_secuencial)`.
    - Constraint de FK hacia `Documento`.

---

* **Título:** Crear índices y constraints para integridad de documentos
* **Objetivo:** Garantizar integridad referencial y rendimiento en consultas.
* **Tipo:** Tarea
* **Descripción corta:** Agregar constraints para evitar documentos huérfanos, índices para consultas frecuentes y trigger/validación para numero_secuencial incremental.
* **Entregables:**
    - Constraint de integridad documento-carpeta-organizacion.
    - Índice para `version_actual_id` en `Documento`.
    - Documentación de reglas de integridad.

---

* **Título:** Datos semilla para pruebas de documentos
* **Objetivo:** Facilitar pruebas automatizadas y manuales con datos realistas.
* **Tipo:** Tarea
* **Descripción corta:** Crear datos de ejemplo: carpetas con y sin permisos, usuarios con diferentes niveles de acceso para probar subida de documentos.
* **Entregables:**
    - Script de seed con escenarios de prueba.
    - Documentación de datos de prueba.

---
### Backend
---

* **Título:** Implementar servicio de almacenamiento de archivos
* **Objetivo:** Abstraer la persistencia física del binario del documento.
* **Tipo:** Tarea
* **Descripción corta:** Crear servicio/interfaz `StorageService` con método `upload(file, path)` que retorne la ruta de almacenamiento. Implementación inicial en filesystem local con estructura `/{organizacion_id}/{carpeta_id}/{documento_id}/{version}/`.
* **Entregables:**
    - Interface `IStorageService` con métodos `upload`, `download`, `delete`.
    - Implementación `LocalStorageService`.
    - Configuración de directorio base por entorno.

---

* **Título:** Implementar validador de permisos de carpeta para escritura
* **Objetivo:** Verificar que el usuario tiene ESCRITURA en la carpeta destino.
* **Tipo:** Tarea
* **Descripción corta:** Crear método que dado `usuario_id`, `carpeta_id` y `organizacion_id`, verifique si existe permiso de ESCRITURA (directo o heredado).
* **Entregables:**
    - Método `hasWritePermission(userId, folderId, orgId)`.
    - Integración con sistema ACL existente (P2).

---

* **Título:** Implementar servicio de creación de documento
* **Objetivo:** Lógica de negocio para crear documento con su primera versión.
* **Tipo:** Historia
* **Descripción corta:** Crear `DocumentService.create()` que: valide permisos, persista metadatos en `Documento`, suba el binario, cree `Version_Documento` con `numero_secuencial=1`, actualice `version_actual_id`.
* **Entregables:**
    - Método `createDocument(file, folderId, userId, orgId)`.
    - Transacción atómica (rollback si falla storage).
    - DTO de respuesta con `documento_id` y `version_actual`.

---

* **Título:** Implementar endpoint `POST /api/folders/{folderId}/documents`
* **Objetivo:** Exponer API REST para subida de documentos.
* **Tipo:** Historia
* **Descripción corta:** Endpoint que recibe archivo multipart, valida token y permisos, invoca servicio de creación y retorna respuesta estructurada con `201` o `403`.
* **Entregables:**
    - Controlador con ruta `POST /api/folders/{folderId}/documents`.
    - Validación de multipart/form-data.
    - Respuesta JSON con `documento_id`, `nombre`, `version_actual`.

---

* **Título:** Implementar validaciones de archivo (tamaño, tipo)
* **Objetivo:** Prevenir subidas inválidas o potencialmente peligrosas.
* **Tipo:** Tarea
* **Descripción corta:** Agregar validaciones configurables: tamaño máximo de archivo, extensiones permitidas/bloqueadas. Retornar `400` con mensaje descriptivo si no cumple.
* **Entregables:**
    - Configuración de límites por entorno.
    - Middleware/validador de archivos.
    - Mensajes de error claros.

---

* **Título:** Emitir evento de auditoría al crear documento
* **Objetivo:** Registrar la acción para trazabilidad (P5).
* **Tipo:** Tarea
* **Descripción corta:** Al completar la creación exitosa, emitir evento de auditoría con `codigo_evento=DOCUMENTO_CREADO`, `documento_id`, `usuario_id`, `organizacion_id`.
* **Entregables:**
    - Integración con servicio de auditoría.
    - Evento emitido tras commit de transacción.

---

* **Título:** Pruebas unitarias del servicio de documentos
* **Objetivo:** Asegurar lógica de negocio correcta y prevenir regresiones.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios para `DocumentService.create()` cubriendo: creación exitosa, fallo por permisos, fallo por storage, validaciones de archivo.
* **Entregables:**
    - Suite de tests unitarios (mínimo 6 casos).
    - Mocks de storage y repositorios.

---

* **Título:** Pruebas de integración del endpoint de subida
* **Objetivo:** Verificar flujo completo HTTP a base de datos.
* **Tipo:** QA
* **Descripción corta:** Tests de integración para `POST /api/folders/{folderId}/documents` validando: 201 con datos correctos, 403 sin permisos, 400 con archivo inválido.
* **Entregables:**
    - Tests de integración para escenarios de aceptación.
    - Verificación de persistencia en BD.
    - Verificación de archivo en storage.

---
### Frontend
---

* **Título:** Sin cambios de UI para US-DOC-001
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La interfaz de carga corresponde a `US-DOC-006`. Se puede crear colección de requests para testing manual.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.

---

## 3. Flujo recomendado de ejecución

```
1. [BD] Crear modelo de Documento
   ↓
2. [BD] Crear modelo de Version_Documento
   ↓
3. [BD] Crear índices y constraints
   ↓
4. [Backend] Implementar servicio de almacenamiento
   ↓
5. [Backend] Implementar validador de permisos
   ↓
6. [Backend] Implementar servicio de creación de documento
   ↓
7. [Backend] Implementar endpoint POST
   ↓
8. [Backend] Implementar validaciones de archivo
   ↓
9. [Backend] Emitir evento de auditoría
   ↓
10. [QA] Pruebas unitarias
    ↓
11. [QA] Pruebas de integración
    ↓
12. [BD] Datos semilla (paralelo o al inicio)
```

### Dependencias entre tickets:
- Modelos de BD son prerequisito para todo el backend
- Servicio de storage es prerequisito para servicio de documentos
- Validador de permisos depende del sistema ACL (P2)
- Endpoint depende del servicio de documentos
- Auditoría depende del sistema de auditoría (P5)
- QA depende de todos los componentes implementados

---

## 4. Recomendación TDD/BDD

### Tickets que deberían tener pruebas primero (TDD):
1. **Servicio de almacenamiento** - Lógica crítica de persistencia
2. **Validador de permisos** - Seguridad, debe ser robusto
3. **Servicio de creación de documento** - Lógica de negocio central
4. **Validaciones de archivo** - Casos de borde importantes

### Tickets que se prestan a escenarios BDD:
```gherkin
Feature: Subir documento a carpeta

  Scenario: Subida exitosa con permisos de escritura
    Given un usuario autenticado con permiso ESCRITURA en carpeta "Proyectos"
    When el usuario sube un archivo "informe.pdf" de 2MB
    Then recibe respuesta 201
    And la respuesta incluye "documento_id" y "version_actual"
    And "version_actual.numero_secuencial" es 1

  Scenario: Subida rechazada sin permisos
    Given un usuario autenticado sin permiso en carpeta "Confidencial"
    When el usuario intenta subir un archivo
    Then recibe respuesta 403
    And el mensaje indica "Sin permisos de escritura"

  Scenario: Subida rechazada por archivo muy grande
    Given un usuario autenticado con permiso ESCRITURA
    And el límite de tamaño configurado es 10MB
    When el usuario intenta subir un archivo de 15MB
    Then recibe respuesta 400
    And el mensaje indica "Archivo excede tamaño máximo"
```

---

## 5. Archivos generados

| Archivo | Contenido |
|---------|-----------|
| `US-DOC-001.md` | Este archivo - Subir documento (API) |
| `US-DOC-002.md` | Descargar versión actual (API) |
| `US-DOC-003.md` | Subir nueva versión (API) |
| `US-DOC-004.md` | Listar versiones (API) |
| `US-DOC-005.md` | Cambiar versión actual - rollback (API) |
| `US-DOC-006.md` | UI mínima de carga y ver historial |
