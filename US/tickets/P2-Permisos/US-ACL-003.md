## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-003] Revocar permiso de carpeta (eliminar ACL)

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Eliminar una entrada ACL existente sobre una carpeta
- Revocar acceso previamente concedido a un usuario
- Verificación inmediata del efecto (usuario recibe 403)

### Restricciones Implícitas
- Solo administradores pueden revocar permisos
- La revocación es efectiva inmediatamente
- Usuario y carpeta deben pertenecer al organizacion del token
- No debe filtrar información de otros organizacions

### Riesgos o Ambigüedades
- No se especifica si es soft delete o hard delete
- **Suposición:** Hard delete para MVP (el permiso deja de existir)
- No se especifica comportamiento si el permiso no existe
- **Suposición:** Devolver 404 si no existe ACL a revocar

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** (Ya implementado en US-ACL-002) Usar tabla ACL_Carpeta existente
* **Objetivo:** Reutilizar la estructura de datos de US-ACL-002.
* **Tipo:** Nota
* **Descripción corta:** No se requiere nueva migración. La operación DELETE usa la misma tabla `ACL_Carpeta` creada en US-ACL-002.
* **Entregables:**
    - Confirmación de que la tabla soporta DELETE.

---
### Backend
---

* **Título:** Extender repositorio con método de eliminación de ACL
* **Objetivo:** Permitir eliminar entradas ACL de carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Agregar método `delete(aclId)` o `deleteByUserAndFolder(usuarioId, carpetaId, organizacionId)` al repositorio existente. Debe retornar indicador de si se eliminó algo.
* **Entregables:**
    - Método `deleteAcl()` en `AclCarpetaRepository`.
    - Retorno de filas afectadas o boolean.

---

* **Título:** Método de servicio para revocar permiso
* **Objetivo:** Encapsular lógica de negocio de revocación.
* **Tipo:** Tarea
* **Descripción corta:** Implementar método `revocarPermiso(carpetaId, usuarioId)` que valide existencia del ACL, pertenencia al organizacion, y elimine la entrada. Devolver error si no existe.
* **Entregables:**
    - Método `revocarPermiso()` en `AclCarpetaService`.
    - Validación de existencia previa.
    - Error 404 si ACL no existe.

---

* **Título:** Implementar endpoint `DELETE /carpetas/{id}/permisos/{usuarioId}`
* **Objetivo:** Cumplir scenario de revocación de la historia.
* **Tipo:** Historia
* **Descripción corta:** Endpoint protegido para eliminar ACL de un usuario sobre una carpeta. Requiere rol admin o permiso ADMINISTRACION sobre la carpeta.
* **Entregables:**
    - Ruta/controlador `DELETE /carpetas/{carpetaId}/permisos/{usuarioId}`.
    - Respuestas: 204 (eliminado), 404 (no existe), 403 (sin permisos).
    - Documentación OpenAPI.

---

* **Título:** Pruebas unitarias de revocación de permisos
* **Objetivo:** Asegurar lógica correcta de eliminación.
* **Tipo:** QA
* **Descripción corta:** Tests que cubran: revocar ACL existente, intentar revocar ACL inexistente, validar que no se puede revocar ACL de otro organizacion.
* **Entregables:**
    - Tests unitarios para método `revocarPermiso()`.
    - Cobertura de casos de éxito y error.

---

* **Título:** Pruebas de integración de endpoint DELETE
* **Objetivo:** Verificar flujo completo de revocación.
* **Tipo:** QA
* **Descripción corta:** Tests E2E que verifiquen: admin puede revocar permiso existente, usuario sin ACL recibe 403 al intentar acceder, no puede revocar en carpeta de otro organizacion.
* **Entregables:**
    - Tests de integración con verificación de efecto.
    - Test de acceso denegado post-revocación.

---
### Frontend
---

* **Título:** Método de servicio para revocar permiso
* **Objetivo:** Consumir API de revocación desde frontend.
* **Tipo:** Tarea
* **Descripción corta:** Agregar método `revocarPermisoCarpeta(carpetaId, usuarioId)` al servicio de ACL existente. Manejar respuesta 204 y errores.
* **Entregables:**
    - Método en servicio `AclCarpetaService`.
    - Manejo de errores HTTP.

---

* **Título:** Componente de lista de permisos con opción de revocar
* **Objetivo:** Mostrar permisos actuales y permitir eliminarlos.
* **Tipo:** Tarea
* **Descripción corta:** Crear componente que liste usuarios con acceso a una carpeta, mostrando nivel de acceso y botón "Revocar" por cada entrada. Incluir confirmación antes de eliminar.
* **Entregables:**
    - Componente `ListaPermisosCarpeta`.
    - Botón "Revocar" por entrada.
    - Diálogo de confirmación.
    - Actualización de lista post-revocación.

---

* **Título:** Integrar lista de permisos en modal de administración
* **Objetivo:** Unificar gestión de permisos en una vista.
* **Tipo:** Tarea
* **Descripción corta:** Integrar la lista de permisos dentro del modal "Administrar permisos" de US-ACL-002. Permitir ver, agregar y revocar permisos desde el mismo lugar.
* **Entregables:**
    - Modal con tabs o secciones (agregar/ver permisos).
    - Flujo integrado de gestión.

---

## 3. Flujo Recomendado de Ejecución

```
1. [BE] Extender repositorio con método de eliminación
   ↓
2. [BE] Método de servicio para revocar permiso
   ↓
3. [BE] Implementar endpoint DELETE
   ↓
4. [QA] Pruebas unitarias + integración
   ↓
5. [FE] Método de servicio para revocar
   ↓
6. [FE] Componente de lista de permisos
   ↓
7. [FE] Integrar en modal de administración
```

### Dependencias entre Tickets
- Depende de US-ACL-002 completado (tabla y servicio base)
- Depende de US-ACL-007 para verificar que usuario sin permiso recibe 403
- Frontend depende de endpoint funcional

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Método de servicio para revocar permiso (lógica de validación)
- Método de repositorio de eliminación

### Tickets para Escenarios BDD
```gherkin
Feature: Revocar permiso de carpeta
  
  Scenario: Admin revoca permiso existente
    Given un administrador autenticado del organizacion "A"
    And un usuario "juan@test.com" con permiso "LECTURA" sobre carpeta "Docs"
    When revoco el permiso del usuario sobre la carpeta
    Then recibo status 204
    And el usuario ya no tiene acceso a la carpeta

  Scenario: Usuario sin permiso intenta acceder post-revocación
    Given un usuario "juan@test.com" al que se le revocó el permiso
    When el usuario intenta listar la carpeta "Docs"
    Then recibe status 403

  Scenario: Intentar revocar permiso inexistente
    Given un administrador autenticado
    And un usuario sin permiso sobre una carpeta
    When intento revocar el permiso
    Then recibo status 404
```

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | (Reutiliza tabla de US-ACL-002) |
| 2 | BE | Extender repositorio con método de eliminación |
| 3 | BE | Método de servicio para revocar permiso |
| 4 | BE | Implementar endpoint DELETE /carpetas/{id}/permisos/{usuarioId} |
| 5 | QA | Pruebas unitarias de revocación |
| 6 | QA | Pruebas de integración del endpoint DELETE |
| 7 | FE | Método de servicio para revocar permiso |
| 8 | FE | Componente de lista de permisos con revocar |
| 9 | FE | Integrar lista en modal de administración |
