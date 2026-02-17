## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-002] Conceder permiso de carpeta a usuario (crear ACL)

### Descripción Funcional Completa

**Narrativa:** Como administrador de una organización, necesito otorgar permisos granulares sobre carpetas a usuarios específicos, para controlar el acceso a áreas de documentos de forma segura y auditable, permitiendo escalabilidad desde equipos pequeños hasta estructuras organizacionales complejas.

**Objetivo Técnico:** Implementar un sistema de lista de control de acceso (ACL) a nivel de carpeta que permita:
- Asignación de niveles de acceso (`LECTURA`, `ESCRITURA`, `ADMINISTRACION`) a usuarios
- Aplicación recursiva opcional a subcarpetas
- Validación de pertenencia a la misma organización
- Precedencia clara: permiso explícito de documento > permiso de carpeta (recursivo o directo)
- Auditoría inmutable de cambios de permisos

### Criterios de Aceptación Ampliados

| Scenario | Condición Inicial (Given) | Acción (When) | Resultado Esperado (Then) |
|----------|--------------------------|--------------|--------------------------|
| **2.1** | Admin de Org A con rol ADMIN, usuario y carpeta en Org A | Ejecuto `POST /api/carpetas/{id}/permisos` asignando `LECTURA` al usuario | Recibo `201` con registro ACL creado, usuario puede listar/ver carpeta, se registra evento de auditoría |
| **2.2** | Mismo usuario con ACL `LECTURA` en carpeta | Intenta descargar documento en carpeta | Operación exitosa (`200`), descarga completada |
| **2.3** | Mismo usuario con ACL `LECTURA` en carpeta | Intenta subir documento nuevo en carpeta | Operación rechazada (`403 Forbidden`) con mensaje "Requiere permiso de ESCRITURA" |
| **2.4** | Usuario de Org B intenta crear ACL en carpeta de Org A | Ejecuto `POST /api/carpetas/{id}/permisos` desde token de Org B | Recibo `404 Not Found` sin revelar si carpeta existe |
| **2.5** | Admin intenta asignar ACL a usuario de otra organización | Ejecuto POST con `usuario_id` que pertenece a Org B (contexto: Org A) | Recibo `404 Not Found` (usuario no existe en Org A) sin filtrar información |
| **2.6** | Admin con ACL `ADMINISTRACION` en carpeta | Asigno `LECTURA` con `recursivo=true` a usuario | Subcarpetas heredan permiso; usuario puede listar/ver toda rama sin ACL explícita |
| **2.7** | Usuario con `LECTURA` recursivo en carpeta padre, `ESCRITURA` explícita en subcarpeta | Intenta subir documento en subcarpeta | Operación exitosa (permiso explícito prevalece) |
| **2.8** | ACL existente en carpeta | Admin intenta crear ACL duplicada (mismo usuario, mismo nivel) | Recibo `409 Conflict` indicando que el ACL ya existe |
| **2.9** | ACL existente en carpeta | Admin actualiza nivel de `LECTURA` a `ADMINISTRACION` | Recibo `200` con ACL actualizado, auditoría registra cambio |
| **2.10** | Multiple users with different permissions on same folder | Listado de usuarios autenticados accede a carpeta | Cada usuario ve solo contenido permitido por su nivel ACL |

### Campos de Base de Datos

**Tabla: `acl_carpetas`**

| Campo | Tipo | Restricciones | Descripción |
|-------|------|----------------|------------|
| `id` | `Long` | PRIMARY KEY | Identificador único del registro ACL |
| `carpeta_id` | `Long` | NOT NULL, FK → carpetas.id | Referencia a la carpeta |
| `usuario_id` | `Long` | NOT NULL, FK → usuarios.id | Referencia al usuario |
| `organizacion_id` | `Long` | NOT NULL, FK → organizaciones.id | Aislamiento por tenant (desnormalizado para queries rápidas) |
| `nivel_acceso_id` | `Long` | NOT NULL, FK → nivel_acceso.id | Referencia al catálogo de niveles (LECTURA, ESCRITURA, ADMINISTRACION) |
| `recursivo` | `BOOLEAN` | NOT NULL, DEFAULT=false | Si `true`, aplica a subcarpetas heredadas |
| `fecha_creacion` | `TIMESTAMP` | DEFAULT=NOW() | Auditoría |
| `fecha_actualizacion` | `TIMESTAMP` | DEFAULT=NOW() | Auditoría |
| **Índices** | | | `(carpeta_id, usuario_id)` UNIQUE, `(usuario_id, organizacion_id)`, `(organizacion_id, carpeta_id)` |

**Restricciones de Integridad:**
- Clave única compuesta: `(carpeta_id, usuario_id, nivel_acceso_id)` para evitar duplicación (incluyendo nivel, para permitir actualizar a diferente nivel)
- Validar que `usuario_id` pertenece a la misma `organizacion_id`
- Validar que `carpeta_id` pertenece a la misma `organizacion_id`

### Estructura de Datos de Solicitud/Respuesta (API)

#### Request: Otorgar/Actualizar ACL

```json
{
  "usuario_id": 1,
  "nivel_acceso_codigo": "LECTURA",
  "recursivo": false,
  "comentario_opcional": "Acceso a documentos de proyecto X"
}
```

#### Response: 201 Created / 200 OK (Update)

```json
{
  "data": {
    "id": 2,
    "carpeta_id": 3,
    "usuario_id": 1,
    "usuario": {
      "id": 1,
      "email": "usuario@org.com",
      "nombre": "Usuario Test"
    },
    "nivel_acceso": {
      "id": 4,
      "codigo": "LECTURA",
      "nombre": "Lectura / Consulta"
    },
    "recursivo": false,
    "fecha_creacion": "2026-01-28T10:30:00Z",
    "fecha_actualizacion": "2026-01-28T10:30:00Z"
  },
  "meta": {
    "accion": "PERMISO_CREADO",
    "timestamp": "2026-01-28T10:30:00Z"
  }
}
```

#### Response: 409 Conflict (Duplicate)

```json
{
  "error": {
    "codigo": "ACL_DUPLICATE",
    "mensaje": "Ya existe un permiso para este usuario sobre esta carpeta",
    "detalles": {
      "carpeta_id": 3,
      "usuario_id": 1
    }
  }
}
```

### Endpoints y Contratos

#### 1. Otorgar Permiso a Usuario en Carpeta (Crear ACL)

```http
POST /api/carpetas/{carpeta_id}/permisos
Authorization: Bearer {token}
Content-Type: application/json

{
  "usuario_id": 1,
  "nivel_acceso_codigo": "LECTURA",
  "recursivo": false
}
```

**Validaciones:**
- Admin de la organización (verificar role en token)
- `carpeta_id` existe y pertenece a la organización del token
- `usuario_id` existe y pertenece a la misma organización
- `nivel_acceso_codigo` es válido (existe en catálogo y está activo)
- No existe ACL duplicado para este usuario en esta carpeta

**Respuesta 201 Created:** ACL creado exitosamente

**Respuesta 400 Bad Request:** Validación falló (usuario no de la org, nivel inválido, etc.)

**Respuesta 403 Forbidden:** Usuario sin permisos ADMINISTRACION

**Respuesta 404 Not Found:** Carpeta o usuario no existen (sin revelar cuál)

**Respuesta 409 Conflict:** ACL duplicado para este usuario en carpeta

---

#### 2. Actualizar Permiso (Cambiar Nivel o Recursividad)

```http
PATCH /api/carpetas/{carpeta_id}/permisos/{usuario_id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "nivel_acceso_codigo": "ESCRITURA",
  "recursivo": true
}
```

**Respuesta 200 OK:** ACL actualizado

**Respuesta 404 Not Found:** ACL no existe

---

#### 3. Listar ACLs de una Carpeta

```http
GET /api/carpetas/{carpeta_id}/permisos
Authorization: Bearer {token}
```

**Respuesta 200 OK:**
```json
{
  "data": [
    { /* ACL 1 */ },
    { /* ACL 2 */ }
  ],
  "meta": {
    "total": 2,
    "carpeta_id": 3
  }
}
```

---

#### 4. Revocar Permiso (Eliminar ACL) — Implementado en US-ACL-003

```http
DELETE /api/carpetas/{carpeta_id}/permisos/{usuario_id}
Authorization: Bearer {token}
```

---

### Archivos a Crear/Modificar

#### Backend (Java/Spring Boot - `document-core-service`)

**Persistencia:**
- `src/main/resources/db/migration/V002__Create_ACL_Carpetas_Table.sql` — Nueva migración

**Domain:**
- `src/main/java/.../domain/model/acl/AclCarpeta.java` — Entidad de dominio inmutable
- `src/main/java/.../domain/repository/IAclCarpetaRepository.java` — Interface de repositorio

**Application:**
- `src/main/java/.../application/service/AclCarpetaService.java` — Orquestación y validación
- `src/main/java/.../application/validator/AclCarpetaValidator.java` — Reglas de negocio

**Infrastructure:**
- `src/main/java/.../infrastructure/adapter/persistence/entity/AclCarpetaEntity.java` — Entidad JPA
- `src/main/java/.../infrastructure/adapter/persistence/jpa/AclCarpetaJpaRepository.java` — JPA Repository
- `src/main/java/.../infrastructure/adapter/persistence/AclCarpetaRepositoryAdapter.java` — Adapter (Hexagonal)
- `src/main/java/.../infrastructure/adapter/persistence/mapper/AclCarpetaMapper.java` — MapStruct mapper

**API:**
- `src/main/java/.../api/dto/CreateAclCarpetaDTO.java` — DTO de entrada
- `src/main/java/.../api/dto/AclCarpetaDTO.java` — DTO de respuesta
- `src/main/java/.../api/dto/AclCarpetaResponseDTO.java` — Respuesta con usuario embebido
- `src/main/java/.../api/mapper/AclCarpetaDtoMapper.java` — MapStruct DTO mapper
- `src/main/java/.../api/controller/AclCarpetaController.java` — Endpoints REST (métodos de crear, actualizar, listar)

**Testing:**
- `src/test/java/.../application/service/AclCarpetaServiceTest.java` — Tests unitarios

**Documentación:**
- Actualizar `ai-specs/specs/api-spec.yml` con endpoints de ACL de carpeta

#### Frontend (React/TypeScript)

**Servicios:**
- `src/features/acl/services/aclCarpetaService.ts` — Servicio HTTP para ACL de carpeta

**Hooks:**
- `src/features/acl/hooks/useAclCarpeta.ts` — Hook para manejar ACL de carpeta (crear, actualizar, listar)

**Tipos:**
- `src/features/acl/types/index.ts` — Agregar tipos `IAclCarpeta`, `CreateAclCarpetaDTO`

**Componentes:**
- `src/features/acl/components/AclCarpetaModal.tsx` — Modal para otorgar permisos (form + dropdown usuario + nivel + checkbox recursivo)
- `src/features/acl/components/AclCarpetaList.tsx` — Tabla de ACLs existentes con acciones (editar, eliminar)

**Integración:**
- `src/features/folders/components/FolderDetail.tsx` — Agregar sección de "Permisos" con `AclCarpetaList` y botón "Otorgar Permiso"

---

### Requisitos No Funcionales

| Aspecto | Requerimiento |
|--------|--------------|
| **Seguridad** | 1. Validar `organizacion_id` en token contra carpeta y usuario (no confiar en cliente) 2. Prevenir escalada de privilegios (usuario no ADMIN no puede crear/modificar ACL) 3. No exponer información de usuario/carpeta si no existen o no pertenecen a org |
| **Performance** | 1. Índice en `(carpeta_id, usuario_id)` para búsquedas rápidas 2. Caché en frontend del listado de ACL por carpeta (invalidar al crear/actualizar) 3. Query optimizada para listar con usuario embebido (JOIN, no N+1) |
| **Auditoría** | 1. Emitir evento `ACL_CARPETA_CREADO`, `ACL_CARPETA_ACTUALIZADO` con `usuario_id`, `carpeta_id`, `nivel_anterior`, `nivel_nuevo` 2. Incluir cambio de `recursivo` en auditoría |
| **Escalabilidad** | 1. Estructura flexible: soportar múltiples usuarios por carpeta 2. Índices para queries por usuario (listar todas las carpetas a las que tengo acceso) |
| **Usabilidad** | 1. UI muestra nivel de acceso actual en tooltip/overlay 2. Confirmación antes de cambiar a nivel más restrictivo 3. Indicador visual de ACLs recursivos vs. directos |
| **Integridad de Datos** | 1. Transacción atómica: crear ACL + registrar auditoría 2. Rollback automático si auditoría falla |

---

### Evaluador de Permisos (Integración con US-ACL-006)

La implementación de US-ACL-002 proporciona datos para que **US-ACL-006** (Evaluador de Permisos) resuelva:

```java
// Pseudo-código
PermissionLevel resolveEffectivePermission(Usuario usuario, Carpeta carpeta) {
  // 1. Buscar ACL explícito en documento (si existe) → return ese
  // 2. Buscar ACL directo en carpeta → return ese
  // 3. Buscar ACL recursivo en carpeta padre → return ese
  // 4. Buscar heredado en antepasados → return ese
  // 5. Denegar acceso (NONE)
}
```

---

### Plan de Ejecución Recomendado

1. **Paso 1 (Backend):** Crear migración de tabla, entidad, repositorio, servicio con validaciones
2. **Paso 2 (Backend):** Implementar endpoints (crear, actualizar, listar)
3. **Paso 3 (Testing):** Tests unitarios TDD (rojo → verde → refactor)
4. **Paso 4 (Frontend):** Crear servicio HTTP, hook y componentes
5. **Paso 5 (Integración):** Tests end-to-end validando ACL en operaciones de lectura/escritura
6. **Paso 6 (Documentación):** Actualizar OpenAPI y README

---

### Validación de Completitud

Para considerar US-ACL-002 **completada**, verificar:

✅ **Especificación Funcional**
- [ ] 10 escenarios de aceptación documentados con entrada/salida clara
- [ ] Tabla de BD con restricciones y índices definidos
- [ ] Estructura JSON de request/response con ejemplos reales
- [ ] Endpoints REST con validaciones específicas

✅ **Contratos de API**
- [ ] POST para crear ACL con validaciones
- [ ] PATCH para actualizar nivel/recursividad
- [ ] GET para listar ACLs
- [ ] Respuestas de error (400, 403, 404, 409) claras

✅ **Arquitectura y Código**
- [ ] Entidad de dominio inmutable (`AclCarpeta`)
- [ ] Repositorio con queries optimizadas
- [ ] Servicio de validación centralizado
- [ ] Controlador REST mapeado correctamente
- [ ] DTOs de entrada/salida con MapStruct

✅ **Testing**
- [ ] Tests unitarios de servicio y repositorio (TDD)
- [ ] Tests de integración de endpoint
- [ ] Validación de aislamiento por organización
- [ ] Cobertura >90%

✅ **Frontend**
- [ ] Servicio HTTP con manejo de errores
- [ ] Hook personalizado para estado de ACL
- [ ] Modal de creación con formulario validado
- [ ] Lista de ACLs con acciones (editar, eliminar)
- [ ] Integración en UI de detalle de carpeta

✅ **Documentación**
- [ ] OpenAPI/Swagger actualizado
- [ ] README de feature con ejemplos
- [ ] Notas técnicas de evaluador de permisos
- [ ] Ejemplos de payloads

✅ **Requisitos No Funcionales**
- [ ] Auditoría de cambios de ACL
- [ ] Índices para performance
- [ ] Validación de seguridad sin exponer info
- [ ] Caché en frontend con invalidación

---

**Status:** Lista para estimación y desarrollo  
**Complejidad Estimada:** Media-Alta (2-3 días con asistencia IA)  
**Dependencias:** US-ACL-001 (catálogo de niveles completado), US-FOLDER-001 (carpetas existen)
