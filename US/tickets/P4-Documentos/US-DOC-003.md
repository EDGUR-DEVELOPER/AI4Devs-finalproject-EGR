## P4 — Documentos + Versionado Lineal

### [US-DOC-003] Subir nueva versión (API) incrementa secuencia

---

## 1. Resumen de alcance detectado

### Capacidades encontradas:
- Subida de nueva versión a un documento existente
- Incremento automático de `numero_secuencial`
- Actualización de `version_actual_id` al nuevo registro
- Preservación del historial de versiones anteriores
- Almacenamiento del nuevo binario
- Validación de permisos de ESCRITURA

### Restricciones implícitas:
- El documento debe existir previamente
- El usuario debe tener permiso de ESCRITURA sobre el documento/carpeta
- El numero_secuencial debe ser estrictamente incremental
- La nueva versión se convierte automáticamente en `version_actual`
- El documento debe pertenecer al organizacion_id del token

### Riesgos o ambigüedades:
- No se especifica manejo de concurrencia (dos usuarios subiendo versión simultáneamente)
- No se define si se permite cambiar nombre/extensión en nueva versión
- No se menciona límite de versiones por documento
- No se especifica si se debe notificar a otros usuarios

---

## 2. Lista de tickets necesarios

---
### Base de datos
---

* **Título:** Función/procedimiento para obtener siguiente numero_secuencial
* **Objetivo:** Calcular de forma atómica el siguiente número de versión.
* **Tipo:** Tarea
* **Descripción corta:** Crear función que dado `documento_id`, retorne `MAX(numero_secuencial) + 1`. Debe manejar locks para evitar condiciones de carrera en concurrencia.
* **Entregables:**
    - Función `getNextVersionNumber(documentId)`.
    - Manejo de concurrencia (SELECT FOR UPDATE o equivalente).
    - Test de integridad con accesos concurrentes.

---

* **Título:** Constraint para unicidad de numero_secuencial por documento
* **Objetivo:** Garantizar que no existan versiones duplicadas.
* **Tipo:** Tarea
* **Descripción corta:** Verificar/agregar constraint único en `(documento_id, numero_secuencial)` para prevenir violaciones de integridad en caso de fallo en lógica de aplicación.
* **Entregables:**
    - Índice único compuesto validado.
    - Documentación de comportamiento esperado.

---
### Backend
---

* **Título:** Implementar servicio de validación de documento existente
* **Objetivo:** Verificar existencia del documento en el tenant antes de crear versión.
* **Tipo:** Tarea
* **Descripción corta:** Crear método que valide que el documento existe, pertenece al `organizacion_id` del token y no está eliminado (soft delete).
* **Entregables:**
    - Método `validateDocumentExists(documentId, orgId)`.
    - Excepciones específicas: `DocumentNotFoundException`, `DocumentDeletedException`.

---

* **Título:** Implementar reutilización de validador de permisos de escritura
* **Objetivo:** Verificar ESCRITURA sobre documento para nueva versión.
* **Tipo:** Tarea
* **Descripción corta:** Extender/reutilizar el validador de permisos para verificar ESCRITURA sobre el documento (aplica regla documento > carpeta).
* **Entregables:**
    - Método `hasWritePermissionOnDocument(userId, documentId, orgId)`.
    - Integración con ACL existente.

---

* **Título:** Implementar servicio de creación de nueva versión
* **Objetivo:** Lógica de negocio para agregar versión a documento existente.
* **Tipo:** Historia
* **Descripción corta:** Crear `DocumentService.createVersion()` que: valide documento existente, valide permisos, calcule siguiente `numero_secuencial`, suba binario, cree registro de versión, actualice `version_actual_id` del documento.
* **Entregables:**
    - Método `createVersion(documentId, file, userId, orgId)`.
    - Transacción atómica completa.
    - DTO de respuesta con detalles de la nueva versión.

---

* **Título:** Implementar manejo de concurrencia en versionado
* **Objetivo:** Prevenir conflictos cuando múltiples usuarios suben versión simultáneamente.
* **Tipo:** Tarea
* **Descripción corta:** Implementar bloqueo optimista o pesimista para garantizar que `numero_secuencial` sea único. Retornar error claro si hay conflicto.
* **Entregables:**
    - Mecanismo de lock (optimistic locking con versión o row lock).
    - Manejo de `ConflictException` con mensaje descriptivo.
    - Reintento automático o respuesta 409.

---

* **Título:** Implementar endpoint `POST /api/documents/{documentId}/versions`
* **Objetivo:** Exponer API REST para subida de nueva versión.
* **Tipo:** Historia
* **Descripción corta:** Endpoint que recibe archivo multipart, valida token, invoca servicio de creación de versión y retorna respuesta con `201` incluyendo datos de la nueva versión.
* **Entregables:**
    - Controlador con ruta `POST /api/documents/{documentId}/versions`.
    - Validación de multipart/form-data.
    - Respuesta JSON con `version_id`, `numero_secuencial`, `fecha_creacion`.

---

* **Título:** Emitir evento de auditoría al crear nueva versión
* **Objetivo:** Registrar la acción de versionado para trazabilidad.
* **Tipo:** Tarea
* **Descripción corta:** Al completar creación exitosa, emitir evento con `codigo_evento=VERSION_CREADA`, `documento_id`, `version_id`, `numero_secuencial`, `usuario_id`.
* **Entregables:**
    - Integración con servicio de auditoría.
    - Evento emitido tras commit de transacción.

---

* **Título:** Pruebas unitarias del servicio de versionado
* **Objetivo:** Asegurar lógica correcta de incremento y validación.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios para `DocumentService.createVersion()` cubriendo: versión creada correctamente, numero_secuencial incrementado, documento no existe, sin permisos, conflicto de concurrencia.
* **Entregables:**
    - Suite de tests unitarios (mínimo 6 casos).
    - Mocks de storage y repositorios.
    - Test específico de incremento secuencial.

---

* **Título:** Pruebas de integración del endpoint de versionado
* **Objetivo:** Verificar flujo completo HTTP y consistencia de datos.
* **Tipo:** QA
* **Descripción corta:** Tests de integración para `POST /api/documents/{id}/versions` validando: 201 con version incrementada, `version_actual_id` actualizado, 403 sin permisos, 404 documento inexistente.
* **Entregables:**
    - Tests de integración para escenarios de aceptación.
    - Verificación de persistencia de nueva versión.
    - Verificación de actualización de `version_actual_id`.

---

* **Título:** Pruebas de concurrencia en versionado
* **Objetivo:** Verificar comportamiento bajo acceso concurrente.
* **Tipo:** QA
* **Descripción corta:** Tests que simulen múltiples requests simultáneos de nueva versión. Verificar que no hay duplicados y que todos los números son únicos.
* **Entregables:**
    - Test de carga/concurrencia (mínimo 10 requests simultáneos).
    - Verificación de integridad post-ejecución.

---
### Frontend
---

* **Título:** Sin cambios de UI para US-DOC-003
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La interfaz de subir nueva versión corresponde a `US-DOC-006`.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.

---

## 3. Flujo recomendado de ejecución

```
1. [BD] Constraint unicidad numero_secuencial (verificar)
   ↓
2. [BD] Función para siguiente numero_secuencial
   ↓
3. [Backend] Servicio validación documento existente
   ↓
4. [Backend] Reutilizar validador de permisos escritura
   ↓
5. [Backend] Servicio de creación de nueva versión
   ↓
6. [Backend] Manejo de concurrencia
   ↓
7. [Backend] Implementar endpoint POST versions
   ↓
8. [Backend] Emitir evento de auditoría
   ↓
9. [QA] Pruebas unitarias
   ↓
10. [QA] Pruebas de integración
    ↓
11. [QA] Pruebas de concurrencia
```

### Dependencias entre tickets:
- Depende de US-DOC-001 (modelos y storage service)
- Función de BD es prerequisito para servicio de versionado
- Validación de documento es prerequisito para servicio de versionado
- Manejo de concurrencia es crítico para servicio de versionado
- Endpoint depende del servicio completo
- Pruebas de concurrencia requieren todo implementado

---

## 4. Recomendación TDD/BDD

### Tickets que deberían tener pruebas primero (TDD):
1. **Función siguiente numero_secuencial** - Lógica crítica de secuencia
2. **Validación de documento existente** - Seguridad
3. **Servicio de creación de versión** - Lógica de negocio central
4. **Manejo de concurrencia** - Integridad de datos

### Tickets que se prestan a escenarios BDD:
```gherkin
Feature: Subir nueva versión de documento

  Scenario: Nueva versión incrementa secuencia correctamente
    Given un documento "informe.pdf" con version_actual numero_secuencial 2
    And un usuario autenticado con permiso ESCRITURA sobre el documento
    When el usuario sube una nueva versión del archivo
    Then recibe respuesta 201
    And la nueva versión tiene numero_secuencial 3
    And version_actual del documento apunta a la nueva versión

  Scenario: Nueva versión rechazada sin permisos
    Given un documento "reporte.xlsx" existente
    And un usuario autenticado sin permiso ESCRITURA
    When el usuario intenta subir nueva versión
    Then recibe respuesta 403
    And el mensaje indica "Sin permisos de escritura"

  Scenario: Nueva versión de documento inexistente
    Given un usuario autenticado con permiso ESCRITURA
    When el usuario intenta subir versión a documento "uuid-inexistente"
    Then recibe respuesta 404
    And el mensaje indica "Documento no encontrado"

  Scenario: Concurrencia en versionado mantiene integridad
    Given un documento "contrato.pdf" con version_actual numero_secuencial 1
    And dos usuarios autenticados con permiso ESCRITURA
    When ambos usuarios suben nueva versión simultáneamente
    Then una versión recibe numero_secuencial 2
    And la otra versión recibe numero_secuencial 3
    And no hay duplicados de numero_secuencial

  Scenario: Historial de versiones se preserva
    Given un documento con 3 versiones (1, 2, 3)
    When el usuario sube una nueva versión
    Then las versiones 1, 2 y 3 siguen existiendo
    And la versión 4 se crea correctamente
```

---

## 5. Archivos generados

| Archivo | Contenido |
|---------|-----------|
| `US-DOC-001.md` | Subir documento (API) |
| `US-DOC-002.md` | Descargar versión actual (API) |
| `US-DOC-003.md` | Este archivo - Subir nueva versión (API) |
| `US-DOC-004.md` | Listar versiones (API) |
| `US-DOC-005.md` | Cambiar versión actual - rollback (API) |
| `US-DOC-006.md` | UI mínima de carga y ver historial |
