## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-006] Regla de precedencia de permisos (Documento > Carpeta)

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Definir regla clara de precedencia: Permiso de Documento > Permiso de Carpeta
- Resolver conflictos de permisos automáticamente
- Evaluar permiso efectivo considerando todas las fuentes

### Restricciones Implícitas
- Regla simple para MVP: permiso explícito de documento tiene prioridad
- Si no hay permiso de documento, se usa permiso de carpeta (incluyendo herencia)
- La evaluación debe ser consistente en toda la plataforma
- No hay escalado de permisos (si documento dice LECTURA, no importa si carpeta dice ESCRITURA)

### Riesgos o Ambigüedades
- No se especifica qué pasa si hay permiso de documento pero sin nivel válido
- **Suposición:** Si existe ACL documento, ese es el permiso (incluso si es más restrictivo)
- Esta historia es principalmente de diseño/arquitectura del servicio de evaluación

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** Sin cambios de BD - Reutilizar tablas existentes
* **Objetivo:** Confirmar que no se requieren cambios de esquema.
* **Tipo:** Nota
* **Descripción corta:** Esta historia define lógica de evaluación, no estructura de datos. Usa `ACL_Carpeta` y `ACL_Documento` de historias anteriores.
* **Entregables:**
    - Confirmación de esquema suficiente.

---
### Backend
---

* **Título:** Definir interfaz de evaluación de permisos
* **Objetivo:** Estandarizar el contrato del servicio de evaluación.
* **Tipo:** Diseño
* **Descripción corta:** Definir interface `IEvaluadorPermisos` con método principal `evaluarPermiso(usuarioId, recursoId, tipoRecurso)` que retorne `PermisoEfectivo` con nivel, origen (documento/carpeta/herencia), y recurso fuente.
* **Entregables:**
    - Interface `IEvaluadorPermisos`.
    - DTO `PermisoEfectivo { nivel, origen, recursoFuenteId }`.
    - Enum `OrigenPermiso { DOCUMENTO, CARPETA_DIRECTO, CARPETA_HEREDADO }`.
    - Documentación del contrato.

---

* **Título:** Implementar servicio central de evaluación de permisos
* **Objetivo:** Centralizar toda la lógica de evaluación con regla de precedencia.
* **Tipo:** Historia
* **Descripción corta:** Implementar servicio que implemente el algoritmo: 1) Buscar ACL documento, 2) Si existe, usar ese permiso, 3) Si no, buscar ACL carpeta (directo), 4) Si no, buscar herencia. Retornar null si no hay permiso.
* **Entregables:**
    - Servicio `EvaluadorPermisosService`.
    - Método `evaluarPermisoDocumento(usuarioId, documentoId)`.
    - Método `evaluarPermisoCarpeta(usuarioId, carpetaId)`.
    - Inyección de repositorios de ACL.

---

* **Título:** Algoritmo de evaluación para documentos (con precedencia)
* **Objetivo:** Implementar la regla Documento > Carpeta.
* **Tipo:** Tarea
* **Descripción corta:** Dado un `documentoId` y `usuarioId`: 1) Buscar `ACL_Documento`, 2) Si existe, retornar con `origen=DOCUMENTO`, 3) Si no, obtener `carpeta_id` del documento, 4) Evaluar permiso de carpeta (incluyendo herencia).
* **Entregables:**
    - Método privado `evaluarConPrecedencia()`.
    - Lógica de fallback a carpeta.
    - Integración con servicio de herencia (US-ACL-004).

---

* **Título:** Método auxiliar para verificar nivel de acceso específico
* **Objetivo:** Facilitar verificación de acciones (tiene LECTURA?, tiene ESCRITURA?).
* **Tipo:** Tarea
* **Descripción corta:** Implementar métodos helper: `tieneAcceso(usuarioId, recursoId, nivelRequerido)` que evalúe el permiso efectivo y compare con el nivel requerido. Considerar si niveles son jerárquicos o independientes.
* **Entregables:**
    - Método `tieneAcceso()` retornando boolean.
    - Método `tieneAccesoLectura()`, `tieneAccesoEscritura()`, etc.
    - Documentación de jerarquía de niveles.

---

* **Título:** Integrar evaluador en guards/middlewares existentes
* **Objetivo:** Usar el evaluador centralizado en todo el sistema.
* **Tipo:** Tarea
* **Descripción corta:** Refactorizar guards de autorización para usar `EvaluadorPermisosService` en lugar de consultas directas. Asegurar consistencia en todos los endpoints protegidos.
* **Entregables:**
    - Guards actualizados usando evaluador.
    - Eliminación de lógica duplicada.
    - Tests de regresión de guards.

---

* **Título:** Pruebas unitarias del algoritmo de precedencia
* **Objetivo:** Asegurar que la regla de precedencia funciona correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests que cubran: permiso documento existe (usa documento), permiso documento no existe pero carpeta sí (usa carpeta), ambos existen (usa documento), ninguno existe (sin permiso).
* **Entregables:**
    - Suite completa de tests para `EvaluadorPermisosService`.
    - Matriz de casos de precedencia.
    - Cobertura 100% del algoritmo.

---

* **Título:** Pruebas de integración de precedencia de permisos
* **Objetivo:** Verificar precedencia en escenarios E2E.
* **Tipo:** QA
* **Descripción corta:** Tests E2E con combinaciones: usuario con LECTURA en carpeta y ESCRITURA en documento específico, usuario con ESCRITURA en carpeta pero solo LECTURA en documento específico.
* **Entregables:**
    - Tests de integración con datos complejos.
    - Verificación de scenarios 1 y 2 de la historia.
    - Documentación de casos de prueba.

---
### Frontend
---

* **Título:** Actualizar servicio de permisos para usar origen
* **Objetivo:** Consumir información de origen del permiso.
* **Tipo:** Tarea
* **Descripción corta:** Actualizar tipos y servicio para manejar la respuesta enriquecida del backend que incluye origen del permiso. Usar en componentes de UI.
* **Entregables:**
    - Tipos actualizados con `origen` y `recursoFuenteId`.
    - Servicio actualizado para parsear respuesta.

---

* **Título:** Mostrar información de precedencia en UI
* **Objetivo:** Informar al usuario de dónde viene su permiso efectivo.
* **Tipo:** Tarea
* **Descripción corta:** En la vista de documento, mostrar claramente si el permiso viene del documento directamente o de la carpeta. Incluir tooltip explicativo.
* **Entregables:**
    - Indicador visual de origen de permiso en documento.
    - Diferenciación clara documento vs carpeta.
    - Tooltip con explicación de precedencia.

---

* **Título:** Advertencia en UI al asignar permiso documento más restrictivo
* **Objetivo:** Alertar al admin sobre posibles confusiones de permisos.
* **Tipo:** Tarea
* **Descripción corta:** Al asignar permiso de documento, si el permiso de carpeta es más permisivo, mostrar advertencia: "Este usuario tiene ESCRITURA en la carpeta, pero está asignando solo LECTURA al documento".
* **Entregables:**
    - Lógica de comparación de permisos.
    - Mensaje de advertencia en modal.
    - Opción de continuar o cancelar.

---

## 3. Flujo Recomendado de Ejecución

```
1. [BE] Definir interfaz de evaluación de permisos
   ↓
2. [BE] Algoritmo de evaluación con precedencia
   ↓
3. [BE] Implementar servicio central de evaluación
   ↓
4. [BE] Método auxiliar para verificar nivel específico
   ↓
5. [BE] Integrar evaluador en guards existentes
   ↓
6. [QA] Pruebas unitarias + integración
   ↓
7. [FE] Actualizar servicio con origen
   ↓
8. [FE] Mostrar información de precedencia
   ↓
9. [FE] Advertencia de permiso restrictivo
```

### Dependencias entre Tickets
- Depende de US-ACL-002 (ACL Carpeta)
- Depende de US-ACL-004 (Herencia)
- Depende de US-ACL-005 (ACL Documento)
- Es prerrequisito para US-ACL-007 y US-ACL-008 (enforcement)

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Algoritmo de evaluación con precedencia (CRÍTICO - escribir tests primero)
- Servicio central de evaluación (lógica de negocio central)

### Tickets para Escenarios BDD
```gherkin
Feature: Precedencia de permisos (Documento > Carpeta)
  
  Background:
    Given usuario "juan@test.com" del organizacion "A"
    And carpeta "Documentos" del organizacion "A"
    And documento "Contrato.pdf" en carpeta "Documentos"

  Scenario: Permiso explícito de documento prevalece sobre carpeta
    Given Juan tiene permiso "ESCRITURA" sobre carpeta "Documentos"
    And Juan tiene permiso "LECTURA" explícito sobre "Contrato.pdf"
    When se evalúa el permiso de Juan sobre "Contrato.pdf"
    Then el permiso efectivo es "LECTURA"
    And el origen es "DOCUMENTO"

  Scenario: Sin permiso documento, usa permiso de carpeta
    Given Juan tiene permiso "ESCRITURA" sobre carpeta "Documentos"
    And Juan NO tiene permiso explícito sobre "Contrato.pdf"
    When se evalúa el permiso de Juan sobre "Contrato.pdf"
    Then el permiso efectivo es "ESCRITURA"
    And el origen es "CARPETA_DIRECTO"

  Scenario: Sin permiso documento ni carpeta directa, usa herencia
    Given Juan tiene permiso "LECTURA" recursivo sobre carpeta padre "Raiz"
    And Juan NO tiene permiso directo en "Documentos" ni en "Contrato.pdf"
    When se evalúa el permiso de Juan sobre "Contrato.pdf"
    Then el permiso efectivo es "LECTURA"
    And el origen es "CARPETA_HEREDADO"

  Scenario: Sin ningún permiso
    Given Juan NO tiene ningún permiso relacionado
    When se evalúa el permiso de Juan sobre "Contrato.pdf"
    Then no tiene permiso
    And el acceso es denegado
```

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | (Sin cambios - reutiliza tablas existentes) |
| 2 | BE | Definir interfaz de evaluación de permisos |
| 3 | BE | Implementar servicio central de evaluación |
| 4 | BE | Algoritmo de evaluación para documentos |
| 5 | BE | Método auxiliar para verificar nivel específico |
| 6 | BE | Integrar evaluador en guards existentes |
| 7 | QA | Pruebas unitarias del algoritmo de precedencia |
| 8 | QA | Pruebas de integración de precedencia |
| 9 | FE | Actualizar servicio de permisos con origen |
| 10 | FE | Mostrar información de precedencia en UI |
| 11 | FE | Advertencia de permiso restrictivo |
