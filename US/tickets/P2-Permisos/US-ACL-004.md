## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-004] Permiso recursivo en carpeta (herencia simple)

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Configurar un permiso de carpeta como recursivo (`recursivo=true`)
- Aplicar permisos heredados a subcarpetas automáticamente
- Diferenciar entre permisos recursivos y no recursivos

### Restricciones Implícitas
- La herencia es de padre a hijos (no inversa)
- El flag `recursivo` determina si aplica a subcarpetas
- La evaluación de herencia debe ser eficiente
- No se implementa herencia compleja (múltiples niveles de override)

### Riesgos o Ambigüedades
- No se especifica límite de profundidad de herencia
- **Suposición:** La herencia aplica a todos los niveles descendientes
- No se especifica qué pasa si hay conflicto padre-hijo
- **Suposición:** Para MVP, el permiso más específico (más cercano) prevalece
- El campo `recursivo` ya fue contemplado en US-ACL-002

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** Agregar índice para consultas de herencia de carpetas
* **Objetivo:** Optimizar queries que buscan permisos en la jerarquía de carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Crear índice compuesto en `ACL_Carpeta` para campos (`usuario_id`, `recursivo`, `organizacion_id`). Asegurar que `Carpeta` tenga índice en `carpeta_padre_id` para navegación eficiente.
* **Entregables:**
    - Migración con índice para queries de herencia.
    - Índice en `Carpeta(carpeta_padre_id)` si no existe.
    - Análisis de plan de ejecución de queries.

---

* **Título:** Query para obtener ruta de ancestros de carpeta
* **Objetivo:** Facilitar evaluación de herencia hacia arriba.
* **Tipo:** Tarea
* **Descripción corta:** Implementar stored procedure o CTE recursivo que dado un `carpeta_id`, devuelva la lista ordenada de ancestros (de hijo a raíz). Útil para evaluar permisos heredados.
* **Entregables:**
    - Query/función `getAncestorPath(carpeta_id)`.
    - Documentación de uso y performance.

---
### Backend
---

* **Título:** Servicio de resolución de ruta de ancestros
* **Objetivo:** Obtener la jerarquía de carpetas para evaluar herencia.
* **Tipo:** Tarea
* **Descripción corta:** Implementar método que dado un `carpeta_id`, retorne la lista de IDs de carpetas ancestras en orden (de más cercano a raíz). Usar caché si la jerarquía es estable.
* **Entregables:**
    - Método `obtenerRutaAncestros(carpetaId)` en `CarpetaService`.
    - Lista ordenada de IDs ancestros.
    - Cache opcional por carpeta.

---

* **Título:** Algoritmo de evaluación de permiso heredado
* **Objetivo:** Determinar si un usuario tiene permiso sobre carpeta por herencia.
* **Tipo:** Tarea
* **Descripción corta:** Implementar algoritmo que: 1) Busque ACL directo en carpeta, 2) Si no existe, recorra ancestros buscando ACL con `recursivo=true`. Retornar el primer permiso encontrado o null.
* **Entregables:**
    - Método `resolverPermisoHeredado(usuarioId, carpetaId)`.
    - Lógica de recorrido de ancestros.
    - Retorno de `NivelAcceso` o null.

---

* **Título:** Integrar herencia en servicio de evaluación de permisos
* **Objetivo:** Unificar evaluación de permisos directos y heredados.
* **Tipo:** Tarea
* **Descripción corta:** Modificar el servicio principal de permisos para que primero busque permiso directo, y si no existe, invoque la resolución de herencia. Este método será usado por guards y endpoints.
* **Entregables:**
    - Método `evaluarPermiso(usuarioId, carpetaId)` integrado.
    - Lógica: directo > heredado > sin permiso.

---

* **Título:** Endpoint para verificar permiso efectivo sobre carpeta
* **Objetivo:** Permitir consultar qué permiso tiene un usuario sobre una carpeta.
* **Tipo:** Historia
* **Descripción corta:** Endpoint `GET /carpetas/{id}/mi-permiso` que devuelve el nivel de acceso efectivo del usuario autenticado, indicando si es directo o heredado.
* **Entregables:**
    - Ruta/controlador `GET /carpetas/{id}/mi-permiso`.
    - Respuesta: `{ nivel_acceso, es_heredado, carpeta_origen_id }`.
    - Documentación OpenAPI.

---

* **Título:** Pruebas unitarias de algoritmo de herencia
* **Objetivo:** Asegurar correcta evaluación de permisos heredados.
* **Tipo:** QA
* **Descripción corta:** Tests que cubran: permiso directo existe (no hereda), permiso recursivo en padre aplica, permiso no recursivo en padre no aplica, herencia en múltiples niveles.
* **Entregables:**
    - Suite de tests para `resolverPermisoHeredado()`.
    - Casos: directo, recursivo, no-recursivo, multinivel.

---

* **Título:** Pruebas de integración de herencia de permisos
* **Objetivo:** Verificar herencia en escenarios reales.
* **Tipo:** QA
* **Descripción corta:** Tests E2E con jerarquía de carpetas (abuelo/padre/hijo). Verificar que usuario con permiso recursivo en abuelo puede acceder a nieto, y sin recursivo no puede.
* **Entregables:**
    - Tests con estructura de carpetas multinivel.
    - Verificación de scenario 1 y 2 de la historia.

---
### Frontend
---

* **Título:** Mostrar origen de permiso en UI
* **Objetivo:** Informar al usuario de dónde viene su permiso.
* **Tipo:** Tarea
* **Descripción corta:** En la vista de carpeta, mostrar badge o tooltip indicando si el permiso es "directo" o "heredado de [carpeta padre]". Usar el endpoint de permiso efectivo.
* **Entregables:**
    - Indicador visual de origen de permiso.
    - Tooltip con nombre de carpeta origen si es heredado.

---

* **Título:** Checkbox "Aplicar a subcarpetas" en formulario de permisos
* **Objetivo:** Permitir configurar recursividad al asignar permisos.
* **Tipo:** Tarea
* **Descripción corta:** En el modal de asignar permiso (US-ACL-002), agregar checkbox "Aplicar a subcarpetas (recursivo)" con valor por defecto según contexto. Incluir tooltip explicativo.
* **Entregables:**
    - Checkbox en formulario de asignación.
    - Tooltip explicando el comportamiento.
    - Valor enviado en request.

---

* **Título:** Visualizar permisos heredados en lista de permisos
* **Objetivo:** Distinguir permisos directos de heredados en la administración.
* **Tipo:** Tarea
* **Descripción corta:** En la lista de permisos de carpeta (US-ACL-003), mostrar tanto permisos directos como heredados, diferenciándolos visualmente. Los heredados no deben tener opción de revocar directo.
* **Entregables:**
    - Lista mostrando permisos directos y heredados.
    - Estilo diferenciado (icono/color/badge).
    - Opción "Ir a carpeta origen" para heredados.

---

## 3. Flujo Recomendado de Ejecución

```
1. [BD] Agregar índice para consultas de herencia
   ↓
2. [BD] Query para obtener ruta de ancestros
   ↓
3. [BE] Servicio de resolución de ruta de ancestros
   ↓
4. [BE] Algoritmo de evaluación de permiso heredado
   ↓
5. [BE] Integrar herencia en servicio de evaluación
   ↓
6. [BE] Endpoint GET /carpetas/{id}/mi-permiso
   ↓
7. [QA] Pruebas unitarias + integración
   ↓
8. [FE] Checkbox "Aplicar a subcarpetas"
   ↓
9. [FE] Mostrar origen de permiso en UI
   ↓
10. [FE] Visualizar permisos heredados en lista
```

### Dependencias entre Tickets
- Depende de US-ACL-002 (tabla ACL con campo `recursivo`)
- Depende de P3 (tabla Carpeta con `carpeta_padre_id`)
- El algoritmo de herencia es usado por US-ACL-006, US-ACL-007, US-ACL-008

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Algoritmo de evaluación de permiso heredado (lógica compleja, crítica)
- Query de ruta de ancestros (performance importante)

### Tickets para Escenarios BDD
```gherkin
Feature: Herencia de permisos en subcarpetas
  
  Background:
    Given existe jerarquía: "Raiz" > "Padre" > "Hijo" > "Nieto"
    And usuario "juan@test.com" del organizacion "A"

  Scenario: Permiso recursivo aplica a subcarpetas
    Given permiso "LECTURA" con recursivo=true para Juan en "Padre"
    When Juan accede a carpeta "Nieto"
    Then tiene permiso "LECTURA"
    And el permiso es heredado de "Padre"

  Scenario: Permiso no recursivo no aplica a subcarpetas
    Given permiso "LECTURA" con recursivo=false para Juan en "Padre"
    When Juan accede a carpeta "Hijo"
    Then no tiene permiso
    And recibe status 403

  Scenario: Permiso directo tiene prioridad sobre heredado
    Given permiso "LECTURA" con recursivo=true para Juan en "Raiz"
    And permiso "ESCRITURA" directo para Juan en "Hijo"
    When Juan consulta su permiso en "Hijo"
    Then tiene permiso "ESCRITURA"
    And el permiso es directo
```

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | Agregar índice para consultas de herencia |
| 2 | BD | Query para obtener ruta de ancestros |
| 3 | BE | Servicio de resolución de ruta de ancestros |
| 4 | BE | Algoritmo de evaluación de permiso heredado |
| 5 | BE | Integrar herencia en servicio de evaluación |
| 6 | BE | Endpoint GET /carpetas/{id}/mi-permiso |
| 7 | QA | Pruebas unitarias de algoritmo de herencia |
| 8 | QA | Pruebas de integración de herencia |
| 9 | FE | Checkbox "Aplicar a subcarpetas" |
| 10 | FE | Mostrar origen de permiso en UI |
| 11 | FE | Visualizar permisos heredados en lista |
