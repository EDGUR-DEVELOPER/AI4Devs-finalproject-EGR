## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-009] UI muestra capacidades (acciones habilitadas) por carpeta/documento

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Mostrar en la UI qué acciones puede realizar el usuario según sus permisos
- Habilitar/deshabilitar acciones dinámicamente
- Evitar errores mostrando solo opciones válidas para el usuario

### Restricciones Implícitas
- La UI debe reflejar fielmente los permisos del backend
- Las acciones deshabilitadas deben ser visualmente claras
- No debe mostrar acciones que el usuario nunca podrá realizar
- Debe actualizarse si los permisos cambian (en siguiente carga)

### Riesgos o Ambigüedades
- No se especifica si ocultar o deshabilitar acciones sin permiso
- **Suposición:** Deshabilitar con tooltip explicativo (mejor UX que ocultar)
- Performance: consultar permisos por cada elemento puede ser costoso
- **Suposición:** Consultar permiso de carpeta actual, aplicar a elementos hijos

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** Sin cambios de BD - Reutilizar estructura existente
* **Objetivo:** Confirmar que no se requieren cambios de esquema.
* **Tipo:** Nota
* **Descripción corta:** Esta historia es exclusivamente de UI/UX. Consume los endpoints de permisos existentes.
* **Entregables:**
    - Confirmación de que no se requieren cambios.

---
### Backend
---

* **Título:** Endpoint `GET /carpetas/{id}/capacidades` (acciones permitidas)
* **Objetivo:** Proveer al frontend las capacidades del usuario sobre una carpeta.
* **Tipo:** Historia
* **Descripción corta:** Endpoint que devuelve las acciones que el usuario autenticado puede realizar sobre una carpeta: `puede_leer`, `puede_escribir`, `puede_administrar`. Usar el evaluador de permisos.
* **Entregables:**
    - Ruta/controlador `GET /carpetas/{id}/capacidades`.
    - Respuesta: `{ puede_leer, puede_escribir, puede_administrar }`.
    - Documentación OpenAPI.

---

* **Título:** Endpoint `GET /documentos/{id}/capacidades` (acciones permitidas)
* **Objetivo:** Proveer al frontend las capacidades del usuario sobre un documento.
* **Tipo:** Historia
* **Descripción corta:** Endpoint que devuelve las acciones que el usuario puede realizar sobre un documento específico. Considera precedencia documento > carpeta.
* **Entregables:**
    - Ruta/controlador `GET /documentos/{id}/capacidades`.
    - Respuesta: `{ puede_leer, puede_descargar, puede_escribir, puede_administrar }`.
    - Documentación OpenAPI.

---

* **Título:** Incluir capacidades en respuesta de listado de carpeta
* **Objetivo:** Optimizar llamadas incluyendo capacidades en un solo request.
* **Tipo:** Tarea
* **Descripción corta:** Modificar `GET /carpetas/{id}/contenido` para incluir capacidades del usuario sobre cada elemento listado. Evita N+1 llamadas para obtener permisos por elemento.
* **Entregables:**
    - Respuesta extendida con `capacidades` por elemento.
    - Query optimizado para evaluar permisos en batch.
    - Documentación de nuevo formato de respuesta.

---

* **Título:** Incluir capacidades del usuario en respuesta de detalle de carpeta
* **Objetivo:** Incluir capacidades al consultar una carpeta específica.
* **Tipo:** Tarea
* **Descripción corta:** Modificar `GET /carpetas/{id}` para incluir objeto `mis_capacidades` con las acciones permitidas para el usuario autenticado.
* **Entregables:**
    - Campo `mis_capacidades` en respuesta de carpeta.
    - Evaluación de permisos integrada en query.

---

* **Título:** Incluir capacidades del usuario en respuesta de detalle de documento
* **Objetivo:** Incluir capacidades al consultar un documento específico.
* **Tipo:** Tarea
* **Descripción corta:** Modificar `GET /documentos/{id}` para incluir objeto `mis_capacidades` con las acciones permitidas.
* **Entregables:**
    - Campo `mis_capacidades` en respuesta de documento.
    - Evaluación usando precedencia documento > carpeta.

---

* **Título:** Pruebas de endpoints de capacidades
* **Objetivo:** Verificar que capacidades reflejan permisos correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests que verifiquen: usuario con LECTURA tiene `puede_leer=true, puede_escribir=false`, usuario con ESCRITURA tiene ambos true, admin tiene todo true.
* **Entregables:**
    - Tests por combinación de permisos.
    - Verificación de consistencia con guards de enforcement.

---
### Frontend
---

* **Título:** Servicio/hook para obtener capacidades de carpeta
* **Objetivo:** Consumir y cachear capacidades del usuario.
* **Tipo:** Tarea
* **Descripción corta:** Implementar hook `useCapacidadesCarpeta(carpetaId)` que consulte capacidades y las cachee. Exponer booleans para usar en componentes.
* **Entregables:**
    - Hook `useCapacidadesCarpeta()`.
    - Cache por carpeta (invalidar al navegar).
    - Tipos TypeScript para capacidades.

---

* **Título:** Servicio/hook para obtener capacidades de documento
* **Objetivo:** Consumir capacidades de documento individual.
* **Tipo:** Tarea
* **Descripción corta:** Implementar hook `useCapacidadesDocumento(documentoId)` similar al de carpeta. Usar en vista de detalle de documento.
* **Entregables:**
    - Hook `useCapacidadesDocumento()`.
    - Cache por documento.

---

* **Título:** Componente de toolbar contextual según capacidades
* **Objetivo:** Mostrar solo acciones permitidas en la barra de herramientas.
* **Tipo:** Tarea
* **Descripción corta:** Crear componente `ToolbarCarpeta` que reciba capacidades y renderice botones habilitados/deshabilitados según permisos: "Subir" (escritura), "Nueva carpeta" (escritura), "Permisos" (admin).
* **Entregables:**
    - Componente `ToolbarCarpeta`.
    - Botones condicionalmente habilitados.
    - Tooltips explicativos en botones deshabilitados.

---

* **Título:** Componente de menú contextual según capacidades
* **Objetivo:** Mostrar opciones de clic derecho según permisos.
* **Tipo:** Tarea
* **Descripción corta:** Modificar menú contextual de carpetas/documentos para habilitar/deshabilitar opciones según capacidades: "Abrir" (lectura), "Descargar" (lectura), "Editar" (escritura), "Mover" (escritura), "Permisos" (admin).
* **Entregables:**
    - Menú contextual actualizado.
    - Opciones condicionalmente disponibles.
    - Separadores para agrupar por tipo de acción.

---

* **Título:** Indicador visual de nivel de acceso en listado
* **Objetivo:** Mostrar rápidamente el nivel de acceso a cada elemento.
* **Tipo:** Tarea
* **Descripción corta:** En la tabla/grid de contenido de carpeta, agregar columna o icono indicando nivel de acceso del usuario: icono de ojo (lectura), lápiz (escritura), engranaje (admin).
* **Entregables:**
    - Columna/icono de nivel de acceso.
    - Tooltip con descripción del nivel.
    - Leyenda de iconos (opcional).

---

* **Título:** Estado de carga para capacidades
* **Objetivo:** Manejar UX mientras se cargan permisos.
* **Tipo:** Tarea
* **Descripción corta:** Mientras se consultan capacidades, mostrar botones en estado de carga (skeleton/spinner). Evitar mostrar botones habilitados que luego se deshabiliten.
* **Entregables:**
    - Estado de loading en toolbar y menú.
    - Skeleton o spinner en botones.
    - Transición suave al estado final.

---

* **Título:** Actualización de capacidades al cambiar de carpeta
* **Objetivo:** Refrescar capacidades al navegar.
* **Tipo:** Tarea
* **Descripción corta:** Al cambiar de carpeta en la navegación, invalidar cache de capacidades y consultar las nuevas. Asegurar que UI refleja permisos de carpeta actual.
* **Entregables:**
    - Invalidación de cache en navegación.
    - Re-fetch de capacidades.
    - Sincronización de estado de UI.

---

* **Título:** Mensaje informativo para usuarios sin permisos
* **Objetivo:** Guiar a usuarios que no pueden realizar acciones.
* **Tipo:** Tarea
* **Descripción corta:** Si un usuario tiene permisos muy limitados (solo lectura en toda la app), mostrar mensaje informativo "Sus permisos solo permiten visualizar contenido. Contacte al administrador para solicitar más acceso."
* **Entregables:**
    - Mensaje informativo contextual.
    - Link o botón para contactar admin (si aplica).

---

* **Título:** Pruebas E2E de UI de capacidades
* **Objetivo:** Verificar que UI refleja permisos correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests automatizados de UI que verifiquen: usuario con solo LECTURA ve botón "Subir" deshabilitado, usuario con ESCRITURA lo ve habilitado, tooltip muestra mensaje correcto.
* **Entregables:**
    - Tests E2E con Cypress/Playwright.
    - Casos por perfil de permisos.
    - Screenshots de estados esperados.

---

## 3. Flujo Recomendado de Ejecución

```
1. [BE] Endpoint GET /carpetas/{id}/capacidades
   ↓
2. [BE] Endpoint GET /documentos/{id}/capacidades
   ↓
3. [BE] Incluir capacidades en listado y detalles
   ↓
4. [QA] Pruebas de endpoints de capacidades
   ↓
5. [FE] Hooks para obtener capacidades
   ↓
6. [FE] Toolbar contextual según capacidades
   ↓
7. [FE] Menú contextual según capacidades
   ↓
8. [FE] Indicador visual en listado
   ↓
9. [FE] Estados de carga y actualización
   ↓
10. [FE] Mensaje informativo
   ↓
11. [QA] Pruebas E2E de UI
```

### Dependencias entre Tickets
- Depende de US-ACL-006 (Evaluador de permisos)
- Depende de US-ACL-007 y US-ACL-008 (enforcement en backend)
- Se beneficia de endpoints de P3 y P4 existentes

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Endpoints de capacidades (definir contrato de respuesta primero)
- Hooks de capacidades (definir comportamiento esperado)

### Tickets para Escenarios BDD
```gherkin
Feature: UI muestra capacidades según permisos
  
  Background:
    Given usuarios del organizacion "A":
      | email             | permiso_carpeta |
      | lector@test.com   | LECTURA         |
      | escritor@test.com | ESCRITURA       |
      | admin@test.com    | ADMINISTRACION  |

  Scenario: Usuario con solo LECTURA ve botón Subir deshabilitado
    Given usuario "lector@test.com" autenticado
    When navega a carpeta "Documentos"
    Then el botón "Subir documento" está deshabilitado
    And el tooltip dice "Requiere permiso de escritura"

  Scenario: Usuario con ESCRITURA ve botón Subir habilitado
    Given usuario "escritor@test.com" autenticado
    When navega a carpeta "Documentos"
    Then el botón "Subir documento" está habilitado
    And puede hacer click para abrir modal

  Scenario: Usuario con LECTURA no ve opción de Permisos
    Given usuario "lector@test.com" autenticado
    When abre menú contextual de carpeta
    Then la opción "Administrar permisos" está deshabilitada
    Or la opción "Administrar permisos" no aparece

  Scenario: Admin ve todas las opciones habilitadas
    Given usuario "admin@test.com" autenticado
    When navega a carpeta "Documentos"
    Then todos los botones están habilitados
    And puede acceder a "Administrar permisos"

  Scenario: Indicador visual muestra nivel de acceso
    Given usuario "lector@test.com" autenticado
    When ve el listado de carpeta
    Then cada elemento muestra icono de nivel de acceso
    And "Documentos" muestra icono de "solo lectura"
```

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | (Sin cambios) |
| 2 | BE | Endpoint GET /carpetas/{id}/capacidades |
| 3 | BE | Endpoint GET /documentos/{id}/capacidades |
| 4 | BE | Incluir capacidades en respuesta de listado |
| 5 | BE | Incluir capacidades en detalle de carpeta |
| 6 | BE | Incluir capacidades en detalle de documento |
| 7 | QA | Pruebas de endpoints de capacidades |
| 8 | FE | Hook useCapacidadesCarpeta |
| 9 | FE | Hook useCapacidadesDocumento |
| 10 | FE | Componente toolbar según capacidades |
| 11 | FE | Menú contextual según capacidades |
| 12 | FE | Indicador visual de nivel en listado |
| 13 | FE | Estado de carga para capacidades |
| 14 | FE | Actualización al cambiar de carpeta |
| 15 | FE | Mensaje informativo para usuarios limitados |
| 16 | QA | Pruebas E2E de UI de capacidades |
