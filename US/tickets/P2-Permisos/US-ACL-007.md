## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-007] Enforzar permisos de lectura en endpoints de consulta/descarga

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Bloquear acceso de lectura a usuarios sin permiso `LECTURA`
- Proteger endpoints de listado de carpetas
- Proteger endpoints de descarga de documentos
- Retornar 403 cuando no hay permiso

### Restricciones Implícitas
- La verificación debe usar el evaluador de permisos (US-ACL-006)
- Aplica a todos los endpoints de consulta y descarga
- No debe filtrar información sensible en respuestas de error
- Debe considerar herencia y precedencia ya implementadas

### Riesgos o Ambigüedades
- No se especifica si el listado debe filtrar elementos individuales o denegar toda la operación
- **Suposición MVP:** Si no tiene LECTURA en carpeta, se deniega toda la operación (403)
- Para documentos dentro de carpeta con permiso, se asume que hereda LECTURA

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** Sin cambios de BD - Reutilizar estructura existente
* **Objetivo:** Confirmar que no se requieren cambios de esquema.
* **Tipo:** Nota
* **Descripción corta:** Esta historia es sobre enforcement en capa de aplicación, no datos. Usa tablas ACL existentes.
* **Entregables:**
    - Confirmación de esquema suficiente.

---
### Backend
---

* **Título:** Crear Guard genérico de permiso de lectura
* **Objetivo:** Reutilizar verificación de LECTURA en múltiples endpoints.
* **Tipo:** Tarea
* **Descripción corta:** Implementar guard/decorator `@RequiereLectura(tipoRecurso)` que extraiga el ID del recurso de los parámetros de ruta, evalúe el permiso usando `EvaluadorPermisosService`, y retorne 403 si no tiene LECTURA.
* **Entregables:**
    - Guard `RequiereLecturaGuard`.
    - Decorator `@RequiereLectura('carpeta' | 'documento')`.
    - Extracción automática de ID de ruta.
    - Respuesta 403 estandarizada.

---

* **Título:** Aplicar guard de lectura a endpoint `GET /carpetas/{id}`
* **Objetivo:** Proteger consulta de detalle de carpeta.
* **Tipo:** Tarea
* **Descripción corta:** Decorar endpoint de detalle de carpeta con `@RequiereLectura('carpeta')`. Verificar que sin LECTURA retorna 403.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test de acceso denegado.

---

* **Título:** Aplicar guard de lectura a endpoint `GET /carpetas/{id}/contenido`
* **Objetivo:** Proteger listado de contenido de carpeta.
* **Tipo:** Tarea
* **Descripción corta:** Proteger endpoint que lista subcarpetas y documentos de una carpeta. Usuario sin LECTURA recibe 403.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test de acceso denegado.

---

* **Título:** Aplicar guard de lectura a endpoint `GET /documentos/{id}`
* **Objetivo:** Proteger consulta de metadatos de documento.
* **Tipo:** Tarea
* **Descripción corta:** Decorar endpoint de detalle de documento con `@RequiereLectura('documento')`. Usa evaluador con precedencia.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test con permiso de documento vs carpeta.

---

* **Título:** Aplicar guard de lectura a endpoint `GET /documentos/{id}/descargar`
* **Objetivo:** Proteger descarga de archivo de documento.
* **Tipo:** Historia
* **Descripción corta:** Proteger endpoint de descarga de documento. Usuario sin LECTURA recibe 403. Endpoint crítico para protección de información.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test de descarga autorizada y denegada.
    - Logging de intentos denegados.

---

* **Título:** Aplicar guard de lectura a endpoint `GET /documentos/{id}/versiones`
* **Objetivo:** Proteger listado de versiones de documento.
* **Tipo:** Tarea
* **Descripción corta:** Proteger endpoint que lista historial de versiones. Requiere LECTURA sobre el documento.
* **Entregables:**
    - Endpoint protegido con guard.
    - Test de acceso denegado.

---

* **Título:** Filtrado de elementos en listado por permisos (opcional MVP)
* **Objetivo:** Mostrar solo elementos visibles en listado de carpeta.
* **Tipo:** Tarea (Opcional)
* **Descripción corta:** Modificar query de listado de carpeta para filtrar subcarpetas y documentos según permisos del usuario. Alternativa a denegar todo si no tiene permiso en carpeta padre.
* **Entregables:**
    - Lógica de filtrado en servicio.
    - Query optimizado con JOIN a ACLs.
    - Documentación de comportamiento.

---

* **Título:** Pruebas unitarias de guard de lectura
* **Objetivo:** Asegurar funcionamiento correcto del guard.
* **Tipo:** QA
* **Descripción corta:** Tests del guard aislado: con permiso permite, sin permiso deniega, con token inválido deniega. Mockear evaluador de permisos.
* **Entregables:**
    - Tests unitarios del guard.
    - Mocks de evaluador y request.

---

* **Título:** Pruebas de integración de enforcement de lectura
* **Objetivo:** Verificar protección E2E de endpoints.
* **Tipo:** QA
* **Descripción corta:** Tests E2E que verifiquen cada endpoint protegido: usuario con LECTURA puede acceder, usuario sin LECTURA recibe 403, usuario de otro organizacion recibe 404.
* **Entregables:**
    - Suite de tests por endpoint protegido.
    - Casos: con permiso, sin permiso, otro organizacion.
    - Verificación de status codes correctos.

---

* **Título:** Pruebas de seguridad de endpoints de lectura
* **Objetivo:** Verificar que no hay bypass de permisos.
* **Tipo:** QA
* **Descripción corta:** Tests de seguridad: intentar acceder con token alterado, sin token, con usuario desactivado, con organizacion cambiado manualmente.
* **Entregables:**
    - Tests de seguridad específicos.
    - Reporte de vulnerabilidades encontradas/corregidas.

---
### Frontend
---

* **Título:** Manejo global de error 403 en cliente HTTP
* **Objetivo:** Manejar respuestas de acceso denegado consistentemente.
* **Tipo:** Tarea
* **Descripción corta:** En el interceptor de respuestas, detectar 403 y mostrar notificación amigable "No tiene permiso para acceder a este recurso". No redirigir a login (es diferente de 401).
* **Entregables:**
    - Lógica en interceptor para 403.
    - Notificación/toast de acceso denegado.
    - Diferenciación de 401 (sesión) vs 403 (permiso).

---

* **Título:** Ocultar elementos no accesibles en listado de carpetas
* **Objetivo:** Evitar mostrar opciones que resultarán en error.
* **Tipo:** Tarea
* **Descripción corta:** Antes de renderizar acciones sobre carpetas/documentos, verificar permisos del usuario. Ocultar o deshabilitar "Abrir" si no tiene LECTURA. (Se conecta con US-ACL-009).
* **Entregables:**
    - Lógica condicional en componentes de listado.
    - Verificación de permisos antes de mostrar acciones.

---

* **Título:** Página de error "Sin acceso" para navegación directa
* **Objetivo:** Manejar caso de URL directa a recurso sin permiso.
* **Tipo:** Tarea
* **Descripción corta:** Si usuario navega directamente a `/carpetas/123` o `/documentos/456` y recibe 403, mostrar página informativa "No tiene acceso a este recurso" con opción de volver.
* **Entregables:**
    - Componente `SinAcceso` o página de error.
    - Manejo en router/guards de navegación.
    - Botón "Volver" o "Ir al inicio".

---

## 3. Flujo Recomendado de Ejecución

```
1. [BE] Crear Guard genérico de permiso de lectura
   ↓
2. [BE] Aplicar a endpoints de carpetas (GET /carpetas/{id}, /contenido)
   ↓
3. [BE] Aplicar a endpoints de documentos (GET, /descargar, /versiones)
   ↓
4. [QA] Pruebas unitarias del guard
   ↓
5. [QA] Pruebas de integración por endpoint
   ↓
6. [QA] Pruebas de seguridad
   ↓
7. [FE] Manejo global de error 403
   ↓
8. [FE] Ocultar elementos no accesibles
   ↓
9. [FE] Página de error "Sin acceso"
```

### Dependencias entre Tickets
- Depende de US-ACL-006 (Evaluador de permisos)
- Depende de endpoints de P3 (Carpetas) y P4 (Documentos)
- Frontend puede iniciar en paralelo una vez el guard esté funcional

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Guard genérico de permiso de lectura (CRÍTICO para seguridad)
- Pruebas de seguridad (escribir escenarios de ataque primero)

### Tickets para Escenarios BDD
```gherkin
Feature: Enforcement de permisos de lectura
  
  Background:
    Given usuarios del organizacion "A":
      | email            | permiso_carpeta | permiso_documento |
      | admin@test.com   | ADMINISTRACION  | -                 |
      | lector@test.com  | LECTURA         | -                 |
      | externo@test.com | -               | -                 |

  Scenario: Usuario con LECTURA puede listar carpeta
    Given usuario "lector@test.com" autenticado
    When solicita GET /carpetas/1/contenido
    Then recibe status 200
    And recibe lista de elementos

  Scenario: Usuario sin LECTURA no puede listar carpeta
    Given usuario "externo@test.com" autenticado
    When solicita GET /carpetas/1/contenido
    Then recibe status 403
    And el mensaje indica "Sin permiso de lectura"

  Scenario: Usuario con LECTURA puede descargar documento
    Given usuario "lector@test.com" autenticado
    When solicita GET /documentos/10/descargar
    Then recibe status 200
    And recibe el archivo

  Scenario: Usuario sin LECTURA no puede descargar documento
    Given usuario "externo@test.com" autenticado
    When solicita GET /documentos/10/descargar
    Then recibe status 403

  Scenario: Usuario de otro organizacion recibe 404
    Given usuario del organizacion "B" autenticado
    When solicita GET /carpetas/1 (de organizacion A)
    Then recibe status 404
    And no se revela existencia del recurso
```

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | (Sin cambios) |
| 2 | BE | Crear Guard genérico de permiso de lectura |
| 3 | BE | Aplicar guard a GET /carpetas/{id} |
| 4 | BE | Aplicar guard a GET /carpetas/{id}/contenido |
| 5 | BE | Aplicar guard a GET /documentos/{id} |
| 6 | BE | Aplicar guard a GET /documentos/{id}/descargar |
| 7 | BE | Aplicar guard a GET /documentos/{id}/versiones |
| 8 | BE | Filtrado de elementos en listado (opcional) |
| 9 | QA | Pruebas unitarias de guard de lectura |
| 10 | QA | Pruebas de integración por endpoint |
| 11 | QA | Pruebas de seguridad |
| 12 | FE | Manejo global de error 403 |
| 13 | FE | Ocultar elementos no accesibles |
| 14 | FE | Página de error "Sin acceso" |
