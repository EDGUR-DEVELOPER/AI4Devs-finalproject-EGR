## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-001] Definir niveles de acceso estándar (catálogo mínimo)

---

## 1. Resumen de Alcance Detectado

### Capacidades Encontradas
- Definición de catálogo de niveles de acceso estándar del sistema
- Niveles mínimos requeridos: `LECTURA`, `ESCRITURA`, `ADMINISTRACION`
- Consulta de niveles de acceso disponibles

### Restricciones Implícitas
- Los niveles deben ser consistentes y uniformes en toda la plataforma
- Cada nivel controla acciones específicas:
  - `LECTURA`: ver, listar, descargar
  - `ESCRITURA`: subir, modificar
  - `ADMINISTRACION`: administrar permisos
- Los niveles deben existir desde la inicialización del sistema

### Riesgos o Ambigüedades
- No está claro si los niveles son jerárquicos (ADMINISTRACION incluye ESCRITURA que incluye LECTURA) o independientes
- **Suposición:** Para MVP, se asume que son independientes pero se puede asignar múltiples niveles

---

## 2. Lista de Tickets Necesarios

---
### Base de Datos
---

* **Título:** Crear tabla de catálogo de niveles de acceso
* **Objetivo:** Persistir los niveles de acceso estándar del sistema para referencia en ACLs.
* **Tipo:** Tarea
* **Descripción corta:** Implementar tabla `Nivel_Acceso` con campos mínimos: `id`, `codigo`, `nombre`, `descripcion`, `acciones_permitidas` (JSON/array), `activo`, `fecha_creacion`. El código debe ser único y servir como referencia en otras tablas.
* **Entregables:**
    - Migración SQL con tabla `Nivel_Acceso`.
    - Índice único en `codigo`.
    - Documentación de estructura de tabla.

---

* **Título:** Seed de niveles de acceso estándar (LECTURA, ESCRITURA, ADMINISTRACION)
* **Objetivo:** Garantizar que el sistema tenga los niveles mínimos desde su inicialización.
* **Tipo:** Tarea
* **Descripción corta:** Crear script de seed que inserte los tres niveles mínimos con sus acciones asociadas. Debe ser idempotente (no duplicar si ya existen).
* **Entregables:**
    - Script de seed idempotente para niveles de acceso.
    - Definición de acciones por nivel (documentada).
    - Fixture para tests con niveles estándar.

---
### Backend
---

* **Título:** Crear modelo/entidad de Nivel de Acceso
* **Objetivo:** Representar el catálogo de niveles en el dominio de la aplicación.
* **Tipo:** Tarea
* **Descripción corta:** Implementar entidad/modelo `NivelAcceso` con propiedades correspondientes a la tabla. Incluir enumeración o constantes para los códigos estándar (`LECTURA`, `ESCRITURA`, `ADMINISTRACION`).
* **Entregables:**
    - Entidad `NivelAcceso` con mapeo ORM.
    - Enum/Constantes `NivelAccesoCodigo`.
    - DTOs de respuesta para API.

---

* **Título:** Implementar repositorio de niveles de acceso
* **Objetivo:** Proveer acceso a datos de niveles de acceso de forma encapsulada.
* **Tipo:** Tarea
* **Descripción corta:** Crear repositorio con métodos para consultar niveles: `findAll()`, `findByCodigo()`, `findById()`. Implementar caché si es necesario dado que los datos son estáticos.
* **Entregables:**
    - Repositorio `NivelAccesoRepository`.
    - Métodos de consulta básicos.
    - Cache opcional para optimizar consultas frecuentes.

---

* **Título:** Implementar endpoint `GET /niveles-acceso` (consultar catálogo)
* **Objetivo:** Permitir consultar los niveles de acceso disponibles en el sistema.
* **Tipo:** Historia
* **Descripción corta:** Endpoint público (o con autenticación básica) que devuelve la lista de niveles de acceso activos. Útil para UI y validaciones cliente.
* **Entregables:**
    - Ruta/controlador `GET /niveles-acceso` o `GET /acl/niveles`.
    - Respuesta JSON con array de niveles (`id`, `codigo`, `nombre`, `descripcion`).
    - Documentación OpenAPI/Swagger del endpoint.

---

* **Título:** Servicio de validación de nivel de acceso
* **Objetivo:** Centralizar la validación de niveles en operaciones de ACL.
* **Tipo:** Tarea
* **Descripción corta:** Crear servicio que valide si un código de nivel es válido y activo. Será usado por otros servicios de ACL para validar antes de asignar permisos.
* **Entregables:**
    - Método `validarNivelAcceso(codigo)` → boolean/throw.
    - Manejo de error si nivel no existe o no está activo.

---

* **Título:** Pruebas unitarias del servicio de niveles de acceso
* **Objetivo:** Asegurar funcionamiento correcto del catálogo.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios que verifiquen: existencia de los 3 niveles estándar al inicializar, consulta por código, validación de niveles inválidos.
* **Entregables:**
    - Suite de tests unitarios para `NivelAccesoService`.
    - Cobertura de casos: existencia, consulta exitosa, consulta fallida.

---

* **Título:** Pruebas de integración del endpoint de niveles
* **Objetivo:** Verificar endpoint y contrato HTTP.
* **Tipo:** QA
* **Descripción corta:** Tests de integración que consulten `GET /niveles-acceso` y verifiquen que retorna los 3 niveles mínimos con estructura correcta.
* **Entregables:**
    - Test de integración para endpoint.
    - Validación de estructura de respuesta.

---
### Frontend
---

* **Título:** Servicio/hook para obtener niveles de acceso
* **Objetivo:** Consumir el catálogo de niveles desde la API.
* **Tipo:** Tarea
* **Descripción corta:** Implementar servicio o hook (`useNivelesAcceso`) que consulte y cachee los niveles de acceso disponibles. Exponer como constantes o estado global.
* **Entregables:**
    - Hook/servicio `getNivelesAcceso()`.
    - Cache local de niveles (evitar múltiples llamadas).
    - Tipos TypeScript para `NivelAcceso`.

---

* **Título:** Componente selector de nivel de acceso
* **Objetivo:** Reutilizar selector en formularios de ACL.
* **Tipo:** Tarea
* **Descripción corta:** Crear componente dropdown/select que muestre los niveles de acceso disponibles. Debe cargar dinámicamente desde el servicio y manejar estados de carga/error.
* **Entregables:**
    - Componente `NivelAccesoSelect`.
    - Props para valor seleccionado y onChange.
    - Estados de loading y error.

---

## 3. Flujo Recomendado de Ejecución

```
1. [BD] Crear tabla de catálogo de niveles de acceso
   ↓
2. [BD] Seed de niveles de acceso estándar
   ↓
3. [BE] Crear modelo/entidad de Nivel de Acceso
   ↓
4. [BE] Implementar repositorio de niveles de acceso
   ↓
5. [BE] Servicio de validación de nivel de acceso
   ↓
6. [BE] Implementar endpoint GET /niveles-acceso
   ↓
7. [QA] Pruebas unitarias + integración
   ↓
8. [FE] Servicio/hook para obtener niveles
   ↓
9. [FE] Componente selector de nivel de acceso
```

### Dependencias entre Tickets
- Tickets de Backend dependen de BD completada
- Endpoint depende de repositorio y modelo
- Frontend depende de endpoint funcional
- QA puede iniciarse en paralelo con desarrollo de FE

---

## 4. Recomendación TDD/BDD

### Tickets con Pruebas Primero (TDD)
- Servicio de validación de nivel de acceso (lógica pura, fácil de testear)
- Repositorio de niveles de acceso (queries específicos)

### Tickets para Escenarios BDD
```gherkin
Feature: Catálogo de Niveles de Acceso
  
  Scenario: Sistema inicializado con niveles estándar
    Given el sistema está recién inicializado
    When consulto los niveles de acceso
    Then existen al menos los niveles "LECTURA", "ESCRITURA", "ADMINISTRACION"
    And cada nivel tiene un código único
    And cada nivel está activo

  Scenario: Consultar niveles de acceso via API
    Given un usuario autenticado
    When realizo GET /niveles-acceso
    Then recibo status 200
    And la respuesta contiene un array con los 3 niveles mínimos
```

---

## 5. Resumen de Archivos/Tickets

| # | Capa | Ticket |
|---|------|--------|
| 1 | BD | Crear tabla de catálogo de niveles de acceso |
| 2 | BD | Seed de niveles de acceso estándar |
| 3 | BE | Crear modelo/entidad de Nivel de Acceso |
| 4 | BE | Implementar repositorio de niveles de acceso |
| 5 | BE | Implementar endpoint GET /niveles-acceso |
| 6 | BE | Servicio de validación de nivel de acceso |
| 7 | QA | Pruebas unitarias del servicio de niveles |
| 8 | QA | Pruebas de integración del endpoint |
| 9 | FE | Servicio/hook para obtener niveles de acceso |
| 10 | FE | Componente selector de nivel de acceso |
