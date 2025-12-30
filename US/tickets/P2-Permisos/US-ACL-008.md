## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-008] Enforzar permisos de escritura en endpoints de creación/actualización

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Bloquear operaciones de escritura a usuarios sin permiso `ESCRITURA`
- Proteger endpoints de subida de documentos
- Proteger endpoints de creación de subcarpetas
- Retornar 403 cuando no hay permiso de escritura

### Restricciones Implícitas
- La verificación debe usar el evaluador de permisos (US-ACL-006)
- Aplica a todos los endpoints de creación y modificación
- Debe considerar herencia y precedencia
- ESCRITURA es diferente de LECTURA (puede tener uno sin el otro según diseño)

### Riesgos o Ambigüedades
- No se especifica si ESCRITURA implica LECTURA
- **Suposición MVP:** Son independientes, pero en la práctica un admin asignará ambos
- No se especifica permiso para actualizar metadatos vs contenido
- **Suposición:** ESCRITURA permite ambas operaciones

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** Sin cambios de BD - Reutilizar estructura existente
* **Objetivo:** Confirmar que no se requieren cambios de esquema.
* **Tipo:** Nota
* **Descripción corta:** Esta historia es sobre enforcement en capa de aplicación. Usa tablas ACL existentes y nivel ESCRITURA del catálogo.
* **Entregables:**
    - Confirmación de esquema suficiente.

---
### Backend
---

* **Título:** Crear Guard genérico de permiso de escritura
* **Objetivo:** Reutilizar verificación de ESCRITURA en múltiples endpoints.
* **Tipo:** Tarea
* **Descripción corta:** Implementar guard/decorator `@RequiereEscritura(tipoRecurso)` similar al de lectura. Extraer ID del recurso, evaluar permiso ESCRITURA, retornar 403 si no tiene.
* **Entregables:**
    - Guard `RequiereEscrituraGuard`.
    - Decorator `@RequiereEscritura('carpeta' | 'documento')`.
    - Respuesta 403 estandarizada.

---

* **Título:** Aplicar guard de escritura a endpoint `POST /carpetas/{id}/subcarpetas`
* **Objetivo:** Proteger creación de subcarpetas.
* **Tipo:** Tarea
* **Descripción corta:** Decorar endpoint de creación de subcarpeta con `@RequiereEscritura('carpeta')`. Usuario sin ESCRITURA en carpeta padre recibe 403.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test de creación autorizada y denegada.

---

* **Título:** Aplicar guard de escritura a endpoint `POST /carpetas/{id}/documentos`
* **Objetivo:** Proteger subida de documentos a carpeta.
* **Tipo:** Historia
* **Descripción corta:** Proteger endpoint de subida de documento. Usuario sin ESCRITURA en la carpeta destino recibe 403. Endpoint crítico para control de contenido.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test de subida autorizada y denegada.
    - Logging de intentos denegados.

---

* **Título:** Aplicar guard de escritura a endpoint `PUT /carpetas/{id}`
* **Objetivo:** Proteger actualización de metadatos de carpeta.
* **Tipo:** Tarea
* **Descripción corta:** Proteger endpoint de actualización de carpeta (nombre, descripción). Requiere ESCRITURA sobre la carpeta.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test de actualización autorizada y denegada.

---

* **Título:** Aplicar guard de escritura a endpoint `POST /documentos/{id}/versiones`
* **Objetivo:** Proteger subida de nueva versión de documento.
* **Tipo:** Historia
* **Descripción corta:** Proteger creación de nuevas versiones. Usuario sin ESCRITURA sobre el documento (o carpeta si no hay ACL documento) recibe 403.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test con permiso de documento vs carpeta.

---

* **Título:** Aplicar guard de escritura a endpoint `PUT /documentos/{id}`
* **Objetivo:** Proteger actualización de metadatos de documento.
* **Tipo:** Tarea
* **Descripción corta:** Proteger actualización de nombre, descripción u otros metadatos del documento. Requiere ESCRITURA.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test de actualización autorizada y denegada.

---

* **Título:** Aplicar guard de escritura a endpoint `PATCH /documentos/{id}/mover`
* **Objetivo:** Proteger operación de mover documento entre carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Verificar ESCRITURA tanto en carpeta origen como destino. Si falta en cualquiera, retornar 403.
* **Entregables:**
    - Lógica de doble verificación (origen + destino).
    - Endpoint protegido.
    - Test de mover con diferentes combinaciones de permisos.

---

* **Título:** Aplicar guard de escritura a endpoint `DELETE /carpetas/{id}` (soft delete)
* **Objetivo:** Proteger eliminación de carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Proteger eliminación (soft delete) de carpetas vacías. Puede requerir ESCRITURA o ADMINISTRACION según definición del negocio.
* **Entregables:**
    - Endpoint protegido con guard apropiado.
    - Documentación de nivel requerido para eliminar.

---

* **Título:** Pruebas unitarias de guard de escritura
* **Objetivo:** Asegurar funcionamiento correcto del guard.
* **Tipo:** QA
* **Descripción corta:** Tests del guard aislado: con ESCRITURA permite, sin ESCRITURA deniega, con solo LECTURA deniega. Mockear evaluador.
* **Entregables:**
    - Tests unitarios del guard.
    - Verificación de independencia LECTURA/ESCRITURA.

---

* **Título:** Pruebas de integración de enforcement de escritura
* **Objetivo:** Verificar protección E2E de endpoints de escritura.
* **Tipo:** QA
* **Descripción corta:** Tests E2E por endpoint: usuario con ESCRITURA puede crear/modificar, usuario sin ESCRITURA recibe 403, usuario de otro organizacion recibe 404.
* **Entregables:**
    - Suite de tests por endpoint protegido.
    - Casos: con permiso, sin permiso, solo lectura.

---

* **Título:** Pruebas de seguridad de endpoints de escritura
* **Objetivo:** Verificar que no hay bypass de permisos de escritura.
* **Tipo:** QA
* **Descripción corta:** Tests de seguridad: CSRF, escalación de privilegios, manipulación de IDs en request body vs URL.
* **Entregables:**
    - Tests de seguridad específicos.
    - Verificación de que organizacion_id viene del token, no del body.

---
### Frontend
---

* **Título:** Deshabilitar acciones de escritura sin permiso
* **Objetivo:** Evitar que usuarios intenten acciones que serán denegadas.
* **Tipo:** Tarea
* **Descripción corta:** En listados y vistas de detalle, deshabilitar botones "Subir documento", "Nueva carpeta", "Editar" si el usuario no tiene ESCRITURA. Mostrar tooltip explicativo.
* **Entregables:**
    - Lógica condicional en botones de acción.
    - Tooltip "Requiere permiso de escritura".
    - Estado deshabilitado visual.

---

* **Título:** Validación previa de permisos antes de abrir formularios
* **Objetivo:** Evitar frustración de llenar formulario que fallará.
* **Tipo:** Tarea
* **Descripción corta:** Antes de mostrar modal de subir documento o crear carpeta, verificar permisos. Si no tiene ESCRITURA, mostrar mensaje en lugar del formulario.
* **Entregables:**
    - Verificación de permisos al abrir modal.
    - Mensaje "No tiene permiso para esta acción".
    - Alternativa: no mostrar opción (relacionado con US-ACL-009).

---

* **Título:** Manejo de error 403 en operaciones de escritura
* **Objetivo:** Manejar caso donde permiso cambió durante la operación.
* **Tipo:** Tarea
* **Descripción corta:** Si el usuario tenía permiso pero fue revocado mientras llenaba formulario, manejar el 403 resultante con mensaje claro y opción de reintentar o cancelar.
* **Entregables:**
    - Manejo específico de 403 en operaciones de escritura.
    - Mensaje "Su permiso fue modificado. Contacte al administrador."
    - No perder datos ingresados si es posible.

---

* **Título:** Indicador visual de capacidades de escritura en carpeta
* **Objetivo:** Mostrar claramente si el usuario puede escribir.
* **Tipo:** Tarea
* **Descripción corta:** En la vista de carpeta, mostrar badge o icono indicando si tiene capacidad de escritura. Ayuda a entender rápidamente qué puede hacer.
* **Entregables:**
    - Indicador visual (ej: "Puede editar" badge).
    - Icono diferenciado para carpetas con/sin escritura.

---

## 3. Flujo Recomendado de Ejecución

```
1. [BE] Crear Guard genérico de permiso de escritura
   ↓
2. [BE] Aplicar a endpoints de carpetas (POST subcarpetas, PUT, DELETE)
   ↓
3. [BE] Aplicar a endpoints de documentos (POST, PUT, POST versiones)
   ↓
4. [BE] Aplicar a endpoint de mover documento
   ↓
5. [QA] Pruebas unitarias del guard
   ↓
6. [QA] Pruebas de integración por endpoint
   ↓
7. [QA] Pruebas de seguridad
   ↓
8. [FE] Deshabilitar acciones sin permiso
   ↓
9. [FE] Validación previa de permisos
   ↓
10. [FE] Manejo de error 403
   ↓
11. [FE] Indicador visual de capacidades
```

### Dependencias entre Tickets
- Depende de US-ACL-006 (Evaluador de permisos)
- Depende de US-ACL-001 (Nivel ESCRITURA en catálogo)
- Depende de endpoints de P3 (Carpetas) y P4 (Documentos)
- Se relaciona con US-ACL-009 (UI de capacidades)

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Guard genérico de permiso de escritura (CRÍTICO para seguridad)
- Lógica de doble verificación para mover documento

### Tickets para Escenarios BDD
```gherkin
Feature: Enforcement de permisos de escritura
  
  Background:
    Given usuarios del organizacion "A":
      | email             | permiso_carpeta | 
      | escritor@test.com | ESCRITURA       |
      | lector@test.com   | LECTURA         |
      | admin@test.com    | ADMINISTRACION  |

  Scenario: Usuario con ESCRITURA puede subir documento
    Given usuario "escritor@test.com" autenticado
    When sube un documento a carpeta /1
    Then recibe status 201
    And el documento se crea correctamente

  Scenario: Usuario con solo LECTURA no puede subir documento
    Given usuario "lector@test.com" autenticado
    When intenta subir un documento a carpeta /1
    Then recibe status 403
    And el mensaje indica "Requiere permiso de escritura"

  Scenario: Usuario con ESCRITURA puede crear subcarpeta
    Given usuario "escritor@test.com" autenticado
    When crea subcarpeta en carpeta /1
    Then recibe status 201

  Scenario: Usuario sin ESCRITURA no puede crear subcarpeta
    Given usuario "lector@test.com" autenticado
    When intenta crear subcarpeta en carpeta /1
    Then recibe status 403

  Scenario: Mover documento requiere ESCRITURA en origen y destino
    Given usuario con ESCRITURA en carpeta /1 pero no en carpeta /2
    When intenta mover documento de /1 a /2
    Then recibe status 403
    And el mensaje indica "Sin permiso en carpeta destino"

  Scenario: Usuario de otro organizacion no puede escribir
    Given usuario del organizacion "B" autenticado
    When intenta subir documento a carpeta de organizacion "A"
    Then recibe status 404
```

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | (Sin cambios) |
| 2 | BE | Crear Guard genérico de permiso de escritura |
| 3 | BE | Aplicar guard a POST /carpetas/{id}/subcarpetas |
| 4 | BE | Aplicar guard a POST /carpetas/{id}/documentos |
| 5 | BE | Aplicar guard a PUT /carpetas/{id} |
| 6 | BE | Aplicar guard a POST /documentos/{id}/versiones |
| 7 | BE | Aplicar guard a PUT /documentos/{id} |
| 8 | BE | Aplicar guard a PATCH /documentos/{id}/mover |
| 9 | BE | Aplicar guard a DELETE /carpetas/{id} |
| 10 | QA | Pruebas unitarias de guard de escritura |
| 11 | QA | Pruebas de integración por endpoint |
| 12 | QA | Pruebas de seguridad |
| 13 | FE | Deshabilitar acciones sin permiso |
| 14 | FE | Validación previa de permisos |
| 15 | FE | Manejo de error 403 en escritura |
| 16 | FE | Indicador visual de capacidades |
