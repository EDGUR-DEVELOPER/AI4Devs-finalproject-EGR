# [US-ACL-001] Definir niveles de acceso estándar (catálogo mínimo)

## [original]

**Narrativa:** Como sistema, quiero un conjunto mínimo y consistente de niveles de acceso, para evaluar permisos de forma uniforme.

**Criterios de Aceptación:**
- *Scenario 1:* Dado el sistema inicializado, Cuando se consultan niveles, Entonces existen al menos `LECTURA`, `ESCRITURA`, `ADMINISTRACION`.

**Notas Técnicas/Datos:** El nivel controla acciones (ver/listar/descargar vs. subir/modificar vs. administrar permisos).

---

## [enhanced]

### Descripción Funcional Completa

**Narrativa:** Como sistema de gestión documental, necesito un catálogo centralizado y consistente de niveles de acceso (ACL) para evaluar y controlar permisos en toda la plataforma de forma uniforme y escalable.

**Objetivo Técnico:** Establecer la base para el modelo de control de acceso basado en roles (RBAC) al nivel de carpeta y documento, asegurando que todo el sistema usa los mismos niveles estándar.

### Criterios de Aceptación Ampliados

| Scenario | Condición Inicial (Given) | Acción (When) | Resultado Esperado (Then) |
|----------|--------------------------|--------------|--------------------------|
| **1.1** | Sistema recién inicializado | Consulto `GET /acl/niveles` sin token (o con token válido) | Recibo `200` con array de mínimo 3 niveles: `LECTURA`, `ESCRITURA`, `ADMINISTRACION` |
| **1.2** | Base de datos cargada con niveles | Consulto niveles por código (ej. `codigo='LECTURA'`) | Cada nivel tiene `id`, `codigo` único, `nombre`, `descripcion`, `acciones_permitidas[]`, `activo=true` |
| **1.3** | Aplicación iniciada sin datos | Sistema ejecuta seed al arrancar | Se insertan automáticamente los 3 niveles estándar sin duplicación (idempotencia) |
| **1.4** | Validación de un nivel inválido | Intento usar `codigo='PERMISOS_ESPECIALES'` en una ACL | Recibo error de validación indicando que el nivel no existe en el catálogo |
| **1.5** | Nivel estándar desactivado (mantenimiento) | Intento usar un nivel con `activo=false` en una ACL | Recibo error indicando que el nivel no está disponible |

### Campos de Base de Datos

**Tabla: `Nivel_Acceso`**

| Campo | Tipo | Restricciones | Descripción |
|-------|------|----------------|------------|
| `id` | `SERIAL` / `UUID` | PRIMARY KEY | Identificador único del nivel |
| `codigo` | `VARCHAR(50)` | NOT NULL, UNIQUE | Código invariable (ej. `LECTURA`, `ESCRITURA`, `ADMINISTRACION`) para referencias en el código |
| `nombre` | `VARCHAR(100)` | NOT NULL | Nombre legible para UI (ej. "Lectura / Consulta") |
| `descripcion` | `TEXT` | NULL | Explicación breve del nivel y sus capacidades |
| `acciones_permitidas` | `JSONB` / `JSON` | NOT NULL | Array de acciones habilitadas por este nivel (ej. `["view","download","list"]`) |
| `orden` | `INT` | NULL | Posición en UI/listados (para UI jerarquía visual) |
| `activo` | `BOOLEAN` | NOT NULL, DEFAULT=true | Soft flag para desactivar sin eliminar |
| `fecha_creacion` | `TIMESTAMP` | DEFAULT=NOW() | Auditoría |
| `fecha_actualizacion` | `TIMESTAMP` | DEFAULT=NOW() | Auditoría |

### Estructura de Datos de Respuesta (API)

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "codigo": "LECTURA",
      "nombre": "Lectura / Consulta",
      "descripcion": "Permite ver, listar y descargar documentos. Sin capacidad de modificación.",
      "acciones_permitidas": ["ver", "listar", "descargar"],
      "orden": 1,
      "activo": true
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "codigo": "ESCRITURA",
      "nombre": "Escritura / Modificación",
      "descripcion": "Permite subir nuevas versiones, renombrar y modificar metadatos de documentos.",
      "acciones_permitidas": ["ver", "listar", "descargar", "subir", "modificar", "crear_version"],
      "orden": 2,
      "activo": true
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "codigo": "ADMINISTRACION",
      "nombre": "Administración / Control Total",
      "descripcion": "Acceso total: crear, modificar, eliminar carpetas/documentos y gestionar permisos granulares.",
      "acciones_permitidas": ["ver", "listar", "descargar", "subir", "modificar", "crear_version", "eliminar", "administrar_permisos"],
      "orden": 3,
      "activo": true
    }
  ],
  "meta": {
    "total": 3,
    "timestamp": "2026-01-27T10:30:00Z"
  }
}
```

### Definición de Acciones Permitidas

Cada nivel agrupa un conjunto de **acciones atómicas** que se evalúan al validar una solicitud:

| Acción | Descripción | Niveles que incluyen |
|--------|------------|----------------------|
| `ver` | Ver propiedades y metadatos de un documento/carpeta | LECTURA, ESCRITURA, ADMINISTRACION |
| `listar` | Listar contenido de una carpeta | LECTURA, ESCRITURA, ADMINISTRACION |
| `descargar` | Descargar binario de un documento | LECTURA, ESCRITURA, ADMINISTRACION |
| `subir` | Subir un documento nuevo o nueva versión | ESCRITURA, ADMINISTRACION |
| `modificar` | Cambiar metadatos (nombre, descripción) | ESCRITURA, ADMINISTRACION |
| `crear_version` | Crear nueva versión (incrementar secuencial) | ESCRITURA, ADMINISTRACION |
| `eliminar` | Soft-delete de carpeta/documento | ADMINISTRACION |
| `administrar_permisos` | Crear, modificar, revocar ACLs en un objeto | ADMINISTRACION |
| `cambiar_version_actual` | Rollback a versión anterior | ADMINISTRACION |

### Endpoints y Contratos

#### 1. Consultar todos los niveles de acceso

```http
GET /acl/niveles
Authorization: Bearer {token}  # Opcional (para casos públicos, puede no requerirse)
```

**Respuesta 200 OK:**
```json
{
  "data": [ ... ],
  "meta": { "total": 3, "timestamp": "..." }
}
```

**Respuesta 401 Unauthorized** (si se requiere autenticación): Usuario sin token válido.

---

#### 2. Consultar un nivel por código

```http
GET /acl/niveles/{codigo}
Authorization: Bearer {token}
```

**Ejemplo:** `GET /acl/niveles/LECTURA`

**Respuesta 200 OK:**
```json
{
  "data": { "id": "...", "codigo": "LECTURA", ... }
}
```

**Respuesta 404 Not Found:** El código no existe en el catálogo.

---

### Archivos a Modificar / Crear

#### Backend (Java/Spring Boot)

**Servicio `document-core` o `gateway`:**
- **Entidad:** `src/main/java/com/docflow/domain/acl/NivelAcceso.java`
- **Repository:** `src/main/java/com/docflow/infrastructure/adapters/persistence/NivelAccesoRepository.java`
- **Servicio:** `src/main/java/com/docflow/application/services/NivelAccesoService.java`
- **DTO (Response):** `src/main/java/com/docflow/infrastructure/api/dto/NivelAccesoDTO.java`
- **Controlador:** `src/main/java/com/docflow/infrastructure/api/controllers/AclController.java` (método `getNiveles()`)
- **Mapper:** `src/main/java/com/docflow/infrastructure/api/mappers/NivelAccesoMapper.java` (si usa MapStruct)

**Scripts SQL:**
- `db/migrations/V001__Crear_tabla_Nivel_Acceso.sql` (crear tabla)
- `db/seeds/S001__Seed_Niveles_Acceso.sql` (insertar datos estándar)

**Tests:**
- `src/test/java/com/docflow/application/services/NivelAccesoServiceTest.java`
- `src/test/java/com/docflow/infrastructure/api/controllers/AclControllerTest.java`

#### Frontend (React/TypeScript)

**Componentes y Hooks:**
- `src/features/acl/services/nivelAccesoService.ts` (servicio HTTP)
- `src/features/acl/hooks/useNivelesAcceso.ts` (hook para consumir data)
- `src/features/acl/types/index.ts` (tipos TS: `INivelAcceso`, etc.)
- `src/features/acl/components/NivelAccesoSelect.tsx` (dropdown reutilizable)
- `src/common/constants/permissions.ts` (constantes con códigos: `LECTURA`, `ESCRITURA`, etc.)

**Tests:**
- `src/features/acl/__tests__/useNivelesAcceso.test.ts`
- `src/features/acl/__tests__/NivelAccesoSelect.test.tsx`

---

### Requisitos No Funcionales

| Aspecto | Requerimiento |
|--------|--------------|
| **Seguridad** | Endpoint de consulta de niveles puede ser público (sin JWT), pero listados en ACL posteriores deben requerir autenticación y validar `organizacion_id` |
| **Performance** | Cachear niveles en backend (memoria) y frontend (estado global/localStorage) por 24h o hasta cambio manual |
| **Auditoría** | Cambios al catálogo (agregar/desactivar niveles) deben quedar registrados en tabla `Auditoria` con `codigo_evento='NIVEL_ACCESO_MODIFICADO'` |
| **Escalabilidad** | Estructura flexible en JSON para `acciones_permitidas` permite agregar nuevas acciones sin migración de BD |
| **Documentación** | Generar automáticamente con Springdoc OpenAPI (Swagger UI) en cada servicio |

---

## Flujo Recomendado de Ejecución

```
Phase 1: Base de Datos
  1. [BD] Crear tabla Nivel_Acceso con estructura JSONB
  2. [BD] Seed idempotente: LECTURA, ESCRITURA, ADMINISTRACION
     ↓
Phase 2: Backend - Core Domain & Persistence
  3. [BE] Crear entidad NivelAcceso + enums de códigos
  4. [BE] Implementar repositorio y métodos de búsqueda
  5. [BE] Crear servicio de validación NivelAccesoValidator
     ↓
Phase 3: Backend - API & Documentation
  6. [BE] Implementar endpoint GET /acl/niveles + GET /acl/niveles/{codigo}
  7. [BE] Configurar Springdoc OpenAPI para documentación automática
     ↓
Phase 4: Testing (Paralelo con Phase 3)
  8. [QA] Tests unitarios NivelAccesoServiceTest (TDD)
  9. [QA] Tests integración GET /acl/niveles
     ↓
Phase 5: Frontend
  10. [FE] Tipos TypeScript y constantes (CODIGO_LECTURA, etc.)
  11. [FE] Hook useNivelesAcceso + caché
  12. [FE] Componente NivelAccesoSelect (dropdown reutilizable)
```

### Dependencias Críticas
- BD debe completarse antes que backend
- Backend lógica debe estar lista antes de tests de integración
- API debe estar funcional antes de FE
- Tests pueden iniciarse cuando las clases base existen (TDD)

---

## Recomendación TDD/BDD

### Tickets para Desarrollar con TDD (Red → Green → Refactor)

**1. NivelAccesoValidatorTest.java**
```java
@Test
void shouldReturnTrueForValidCodigoLectura() {
  assertTrue(validator.esValido("LECTURA"));
}

@Test
void shouldReturnFalseForInvalidCodigo() {
  assertFalse(validator.esValido("PERMISOS_ESPECIALES"));
}

@Test
void shouldThrowExceptionIfCodigo_IsInactive() {
  assertThrows(NivelAccesoInactivoException.class, 
    () -> validator.validar("LECTURA", /* activo=false */));
}
```

**2. NivelAccesoRepositoryTest.java**
```java
@Test
void shouldReturnAllThreeStandardLevelsOnInitialization() {
  List<NivelAcceso> niveles = repository.findAll();
  assertThat(niveles).hasSize(3)
    .extracting("codigo")
    .containsExactlyInAnyOrder("LECTURA", "ESCRITURA", "ADMINISTRACION");
}

@Test
void shouldFindByCodigoLectura() {
  Optional<NivelAcceso> nivel = repository.findByCodigo("LECTURA");
  assertTrue(nivel.isPresent());
}
```

### Escenarios BDD para API

```gherkin
Feature: Catálogo de Niveles de Acceso - API REST
  
  Background:
    Given la base de datos fue inicializada con los 3 niveles estándar
    And el endpoint está disponible en GET /acl/niveles

  Scenario: Obtener lista completa de niveles
    When realizo GET /acl/niveles
    Then recibo status 200
    And la respuesta incluye "LECTURA" con acciones ["ver", "listar", "descargar"]
    And la respuesta incluye "ESCRITURA" con acciones ["ver", "listar", "descargar", "subir", "modificar", "crear_version"]
    And la respuesta incluye "ADMINISTRACION" con todas las acciones

  Scenario: Obtener un nivel por código
    When realizo GET /acl/niveles/LECTURA
    Then recibo status 200
    And la respuesta contiene { "codigo": "LECTURA", "activo": true }

  Scenario: Intentar obtener nivel inválido
    When realizo GET /acl/niveles/NIVEL_INEXISTENTE
    Then recibo status 404
    And el mensaje de error es claro: "Nivel de acceso no encontrado"

  Scenario: Validar que niveles son inmutables (no borrados)
    Given realicé una consulta a los niveles hace 1 hora
    When vuelvo a consultar GET /acl/niveles
    Then los 3 niveles siguen presentes y sin cambios
```

---

## Validación de Completitud (Checklist)

Para validar que esta historia está lista para desarrollo, verifica:

### ✅ Especificación Funcional
- [x] Descripción clara de la funcionalidad en lenguaje de negocio
- [x] Criterios de Aceptación específicos y medibles (5 escenarios)
- [x] Tabla con campos precisos de la BD
- [x] Estructura JSON de respuesta definida
- [x] Mapeo de acciones a niveles (8 acciones distintas)

### ✅ Contratos de API
- [x] Endpoints definidos: `GET /acl/niveles`, `GET /acl/niveles/{codigo}`
- [x] Respuestas 200 OK con ejemplo completo
- [x] Errores esperados: 404 Not Found, 401 Unauthorized (si aplica)
- [x] Códigos HTTP consistentes

### ✅ Arquitectura y Archivos
- [x] Entidad en dominio: `NivelAcceso.java`
- [x] Repositorio con métodos específicos
- [x] Servicio de validación centralizado
- [x] Controlador REST mapeado
- [x] DTOs de respuesta con MapStruct
- [x] Scripts SQL: migración + seed

### ✅ Frontend
- [x] Tipos TypeScript definidos
- [x] Hook `useNivelesAcceso` con caché
- [x] Componente select reutilizable
- [x] Manejo de estados (loading, error)

### ✅ Testing & QA
- [x] Tests unitarios con TDD (repositorio, servicio)
- [x] Tests de integración de endpoint
- [x] Escenarios BDD documentados
- [x] Casos positivos, negativos y edge cases

### ✅ Requisitos No Funcionales
- [x] Caching en backend (catálogo estático)
- [x] Seguridad: validación de `organizacion_id` en futuros endpoints ACL
- [x] Auditoría: cambios al catálogo registrados
- [x] Performance: JSONB para flexibilidad de acciones
- [x] Documentación: Swagger/OpenAPI automático

---

## Resumen de Entregables

| Capa | Componente | Archivo | Responsable |
|------|-----------|---------|------------|
| **BD** | Tabla Nivel_Acceso | `db/migrations/V001__Nivel_Acceso.sql` | DBA/Backend |
| **BD** | Seed datos | `db/seeds/S001__Seed_Niveles_Acceso.sql` | DBA/Backend |
| **Backend** | Entidad | `src/main/java/.../domain/acl/NivelAcceso.java` | Backend |
| **Backend** | Repository | `src/main/java/.../infrastructure/adapters/persistence/NivelAccesoRepository.java` | Backend |
| **Backend** | Service | `src/main/java/.../application/services/NivelAccesoService.java` | Backend |
| **Backend** | Validator | `src/main/java/.../application/services/NivelAccesoValidator.java` | Backend |
| **Backend** | DTO | `src/main/java/.../infrastructure/api/dto/NivelAccesoDTO.java` | Backend |
| **Backend** | Controller | `src/main/java/.../infrastructure/api/controllers/AclController.java` | Backend |
| **Backend** | Mapper | `src/main/java/.../infrastructure/api/mappers/NivelAccesoMapper.java` | Backend |
| **Testing** | Unit Tests | `src/test/java/.../services/NivelAccesoServiceTest.java` | QA/Backend |
| **Testing** | Integration Tests | `src/test/java/.../controllers/AclControllerTest.java` | QA |
| **Frontend** | Types | `src/features/acl/types/index.ts` | Frontend |
| **Frontend** | Hook | `src/features/acl/hooks/useNivelesAcceso.ts` | Frontend |
| **Frontend** | Service | `src/features/acl/services/nivelAccesoService.ts` | Frontend |
| **Frontend** | Component | `src/features/acl/components/NivelAccesoSelect.tsx` | Frontend |
| **Frontend** | Constants | `src/common/constants/permissions.ts` | Frontend |
| **Docs** | OpenAPI | Automático via Springdoc | Backend |

---

## Notas Finales

1. **Independencia y Reutilización:** El catálogo de niveles es completamente independiente; otros módulos de ACL (carpetas, documentos) lo referenciarán.

2. **Idempotencia:** El seed SQL debe garantizar que múltiples ejecuciones no creen duplicados (usar `ON CONFLICT DO NOTHING`).

3. **Arquitectura Hexagonal:** Los niveles se definen en el dominio (`domain/acl/`), implementados en el adaptador de persistencia, expuestos por la API y consumidos por el frontend.

4. **Extensibilidad:** La estructura JSONB de `acciones_permitidas` permite agregar nuevas acciones sin cambios en BD.

5. **Seguridad:** Aunque el catálogo es público, futuras historias de ACL deben validar `organizacion_id` del token contra permisos sobre carpetas/documentos específicas.

---

**Estado:** Enriquecida y lista para estimación y asignación  
**Fecha:** 27 de enero de 2026  
**Clasificación:** Epic base para P2 (Permisos Granulares)
