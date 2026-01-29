## P3 — Gestión de carpetas: API + UI mínima

### [US-FOLDER-001] Crear carpeta (API) en el organizacion actual

---

**Narrativa:** Como usuario con permisos, quiero crear una carpeta en mi organización, para organizar documentos.

**Criterios de Aceptación:**
- *Scenario 1:* Dado un usuario con `ESCRITURA` (o `ADMINISTRACION`) en la carpeta padre, Cuando crea una carpeta, Entonces recibe `201` y la carpeta pertenece al organizacion del token.
- *Scenario 2:* Dado un usuario sin permiso en la carpeta padre, Cuando crea una carpeta, Entonces recibe `403`.

**Nota de Alcance:** La interfaz de usuario para crear carpetas se implementará en **US-FOLDER-005**. Esta historia solo cubre la API.

---

## [enhanced]

### Descripción Funcional Completa

**Narrativa:** Como usuario autorizado de una organización, necesito crear carpetas dentro de una estructura jerárquica existente, para organizar documentos de forma coherente y respetar los límites de permisos establecidos, permitiendo escalabilidad desde equipos pequeños hasta estructuras organizacionales complejas con múltiples niveles de profundidad.

**Objetivo Técnico:** Implementar un sistema de creación de carpetas que:
- Valide que el usuario tiene permiso `ESCRITURA` o `ADMINISTRACION` en la carpeta padre
- Garantice unicidad de nombre dentro del mismo directorio padre por organización
- Aisle datos por tenant (`organizacion_id`)
- Permita jerarquía indefinida (sin límite de profundidad)
- Soporte soft delete para auditoría
- Proporcione respuestas claras y específicas de error

### Criterios de Aceptación Ampliados

| Scenario | Condición Inicial (Given) | Acción (When) | Resultado Esperado (Then) |
|----------|--------------------------|--------------|--------------------------|
| **1.1** | Usuario autenticado con rol admin/editor, carpeta padre existe en Org A | Ejecuto `POST /api/carpetas` con `carpeta_padre_id` válido, `nombre="Proyecto X"` | Recibo `201` con carpeta creada, `organizacion_id` coincide con token, `carpeta_padre_id` asignado correctamente |
| **1.2** | Mismo usuario con `ESCRITURA` en carpeta padre (permiso heredado recursivo) | Creo subcarpeta en carpeta padre | Operación exitosa (`201`), validación de permisos pasa por herencia |
| **1.3** | Mismo usuario con `ADMINISTRACION` en carpeta padre | Creo subcarpeta | Operación exitosa (`201`), permiso más alto también valida |
| **1.4** | Usuario de Org B intenta crear carpeta en Org A | Ejecuto `POST /api/carpetas` con token de Org B en carpeta de Org A | Recibo `404 Not Found` sin revelar si carpeta existe |
| **1.5** | Usuario intenta crear carpeta en ID de carpeta padre inválido/inexistente | Ejecuto POST con `carpeta_padre_id` no válido | Recibo `404 Not Found` (carpeta padre no existe en su org) |
| **1.6** | Usuario intenta crear 2 carpetas con MISMO nombre en MISMO padre en MISMO nivel | Primera crea exitosa, segunda intenta crear | Primera: `201`, Segunda: `409 Conflict` con mensaje "Nombre duplicado en este directorio" |
| **1.7** | Dos carpetas con MISMO nombre en DIFERENTES padres, MISMA org | Creo "Proyectos" bajo raíz y "Proyectos" bajo carpeta "Administrativo" | Ambas exitosas (`201`), unicidad validada por nivel jerárquico |
| **1.8** | Usuario sin `ESCRITURA` en carpeta padre (tiene `LECTURA`) | Intenta crear carpeta | Recibo `403 Forbidden` con mensaje "Requiere permiso de ESCRITURA o ADMINISTRACION" |
| **1.9** | Usuario sin ningún permiso en carpeta padre | Intenta crear carpeta | Recibo `403 Forbidden` |
| **1.10** | Carpeta raíz de organización (sin padre) | Admin crea subcarpeta bajo raíz | Operación exitosa, `carpeta_padre_id` es UUID de raíz, jerarquía correcta |
| **1.11** | Request con campos inválidos (nombre vacío, descripción >500 chars) | Envío POST con datos inválidos | Recibo `400 Bad Request` con detalles de validación |
| **1.12** | Carpeta eliminada lógicamente (soft delete) | Intento crear carpeta con MISMO nombre en carpeta "eliminada" | Operación exitosa (`201`), validación usa `fecha_eliminacion IS NULL` |

### Campos de Base de Datos

**Tabla: `carpetas`**

| Campo | Tipo | Restricciones | Descripción |
|-------|------|----------------|------------|
| `id` | `UUID` | PRIMARY KEY | Identificador único de carpeta |
| `organizacion_id` | `UUID` | NOT NULL, FK → organizaciones.id | Aislamiento por tenant (desnormalizado para queries rápidas) |
| `carpeta_padre_id` | `UUID` | FK → carpetas.id (NULL para raíz) | Relación jerárquica (auto-referencia), NULL solo para raíz |
| `nombre` | `VARCHAR(255)` | NOT NULL | Nombre de la carpeta |
| `descripcion` | `TEXT` | NULL, MAX 500 chars | Descripción opcional |
| `creado_por` | `UUID` | NOT NULL, FK → usuarios.id | Auditoría: quién creó |
| `fecha_creacion` | `TIMESTAMP` | DEFAULT=NOW() | Auditoría |
| `fecha_actualizacion` | `TIMESTAMP` | DEFAULT=NOW() | Auditoría |
| `fecha_eliminacion` | `TIMESTAMP` | NULL (soft delete) | NULL si activa, timestamp si eliminada |
| **Índices** | | | `(organizacion_id, carpeta_padre_id)` para lista, `(organizacion_id, carpeta_padre_id, nombre)` UNIQUE WHERE fecha_eliminacion IS NULL, `(organizacion_id)` para queries por org |

**Restricciones de Integridad:**
- Unicidad compuesta parcial: `(organizacion_id, carpeta_padre_id, nombre)` con cláusula WHERE `fecha_eliminacion IS NULL` (permite reusar nombre de carpeta eliminada)
- Validar que `carpeta_padre_id` pertenece a la misma `organizacion_id` (no permitir jerarquía cross-org)
- Validar que `creado_por` existe en `usuarios` de la misma organización

### Estructura de Datos de Solicitud/Respuesta (API)

#### Request: Crear Carpeta

```json
{
  "carpeta_padre_id": "770e8400-e29b-41d4-a716-446655440222",
  "nombre": "Proyecto X - Q1 2026",
  "descripcion": "Documentos de planificación y presupuesto del proyecto"
}
```

**Validaciones de Request:**
- `carpeta_padre_id`: UUID válido, requerido
- `nombre`: string 1-255 caracteres, requerido
- `descripcion`: string 0-500 caracteres, opcional

#### Response: 201 Created

```json
{
  "data": {
    "id": "660e8400-e29b-41d4-a716-446655440111",
    "organizacion_id": "550e8400-e29b-41d4-a716-446655440000",
    "carpeta_padre_id": "770e8400-e29b-41d4-a716-446655440222",
    "nombre": "Proyecto X - Q1 2026",
    "descripcion": "Documentos de planificación y presupuesto del proyecto",
    "creado_por": "880e8400-e29b-41d4-a716-446655440333",
    "fecha_creacion": "2026-01-28T10:30:00Z",
    "fecha_actualizacion": "2026-01-28T10:30:00Z",
    "ruta_completa": "/Raíz/Administración/Proyecto X - Q1 2026"
  },
  "meta": {
    "accion": "CARPETA_CREADA",
    "timestamp": "2026-01-28T10:30:00Z"
  }
}
```

#### Response: 400 Bad Request (Validación)

```json
{
  "error": {
    "codigo": "VALIDACION_FALLIDA",
    "mensaje": "Error en validación de entrada",
    "detalles": {
      "nombre": "Longitud máxima 255 caracteres",
      "descripcion": "Longitud máxima 500 caracteres"
    }
  }
}
```

#### Response: 403 Forbidden (Sin Permisos)

```json
{
  "error": {
    "codigo": "SIN_PERMISO_CARPETA",
    "mensaje": "Requiere permiso ESCRITURA o ADMINISTRACION en la carpeta padre",
    "detalles": {
      "carpeta_padre_id": "770e8400-e29b-41d4-a716-446655440222",
      "permiso_actual": "LECTURA",
      "permiso_requerido": ["ESCRITURA", "ADMINISTRACION"]
    }
  }
}
```

#### Response: 404 Not Found (Carpeta Padre No Existe)

```json
{
  "error": {
    "codigo": "CARPETA_NO_ENCONTRADA",
    "mensaje": "Carpeta padre no existe o no pertenece a tu organización",
    "detalles": {
      "carpeta_padre_id": "770e8400-e29b-41d4-a716-446655440222"
    }
  }
}
```

#### Response: 409 Conflict (Nombre Duplicado)

```json
{
  "error": {
    "codigo": "NOMBRE_DUPLICADO",
    "mensaje": "Ya existe una carpeta con este nombre en el mismo directorio",
    "detalles": {
      "carpeta_padre_id": "770e8400-e29b-41d4-a716-446655440222",
      "nombre": "Proyecto X - Q1 2026"
    }
  }
}
```

### Endpoints y Contratos

#### Crear Carpeta

```http
POST /api/carpetas
Authorization: Bearer {token}
Content-Type: application/json

{
  "carpeta_padre_id": "770e8400-e29b-41d4-a716-446655440222",
  "nombre": "Proyecto X",
  "descripcion": "Documentación técnica"
}
```

**Autenticación y Autorización:**
- Token requerido (Bearer)
- Extraer `organizacion_id` y `usuario_id` del token
- Validar que `carpeta_padre_id` pertenece a la misma `organizacion_id`
- Validar permiso del usuario en carpeta padre: `ESCRITURA` o `ADMINISTRACION` (usar evaluador de permisos de US-ACL-006)

**Validaciones de Entrada:**
- `carpeta_padre_id` válido como UUID
- `nombre` no vacío, ≤255 caracteres
- `descripcion` ≤500 caracteres (si presente)
- No contiene caracteres especiales peligrosos (sanitizar para evitar inyecciones)

**Lógica de Negocio:**
1. Validar que carpeta padre existe y está activa (`fecha_eliminacion IS NULL`)
2. Validar permisos del usuario en carpeta padre
3. Verificar unicidad: no existe carpeta con mismo nombre, mismo padre, misma org, activa
4. Crear registro en tabla `carpetas`
5. Emitir evento `CARPETA_CREADA` para auditoría
6. Retornar carpeta creada con `201 Created`

**Respuestas:**
- `201 Created`: Carpeta creada exitosamente
- `400 Bad Request`: Validación de entrada falló (nombre/descripción inválidos)
- `403 Forbidden`: Usuario sin permisos `ESCRITURA`/`ADMINISTRACION` en carpeta padre
- `404 Not Found`: Carpeta padre no existe (sin exponer información)
- `409 Conflict`: Nombre duplicado en el mismo directorio

---

### Archivos a Crear/Modificar

#### Base de Datos y Migraciones

**Migraciones SQL:**
- `src/main/resources/db/migration/V003__Create_Carpetas_Table.sql` — Tabla `carpetas` con índices
- `src/main/resources/db/migration/V004__Create_Carpeta_Raiz_Por_Organizacion.sql` — Script para crear raíz por org

**Data Seed (Pruebas):**
- `src/main/resources/db/seed/S002__Seed_Carpetas_Jerarquia.sql` — Estructura de prueba multinivel

#### Backend (Java/Spring Boot - `document-core-service`)

**Domain:**
- `src/main/java/.../domain/model/carpeta/Carpeta.java` — Entidad de dominio inmutable con Builder
- `src/main/java/.../domain/repository/ICarpetaRepository.java` — Interface de repositorio

**Application:**
- `src/main/java/.../application/service/CarpetaService.java` — Orquestación (crear, validar permisos)
- `src/main/java/.../application/validator/CarpetaValidator.java` — Validaciones de negocio (unicidad, permisos)

**Infrastructure:**
- `src/main/java/.../infrastructure/adapter/persistence/entity/CarpetaEntity.java` — Entidad JPA
- `src/main/java/.../infrastructure/adapter/persistence/jpa/CarpetaJpaRepository.java` — JPA Repository
- `src/main/java/.../infrastructure/adapter/persistence/CarpetaRepositoryAdapter.java` — Adapter (Hexagonal)
- `src/main/java/.../infrastructure/adapter/persistence/mapper/CarpetaMapper.java` — MapStruct mapper

**API:**
- `src/main/java/.../api/dto/CreateCarpetaDTO.java` — DTO de entrada
- `src/main/java/.../api/dto/CarpetaDTO.java` — DTO de respuesta
- `src/main/java/.../api/dto/CarpetaResponseDTO.java` — Respuesta con ruta completa
- `src/main/java/.../api/mapper/CarpetaDtoMapper.java` — MapStruct DTO mapper
- `src/main/java/.../api/controller/CarpetaController.java` — Endpoint POST /api/carpetas

**Exception Handling:**
- `src/main/java/.../infrastructure/adapter/exception/CarpetaNotFoundException.java`
- `src/main/java/.../infrastructure/adapter/exception/CarpetaNombreDuplicadoException.java`
- `src/main/java/.../infrastructure/adapter/exception/SinPermisoCarpetaException.java`

**Testing:**
- `src/test/java/.../application/service/CarpetaServiceTest.java` — Tests unitarios (TDD)

**Documentación:**
- Actualizar `ai-specs/specs/api-spec.yml` con endpoint POST /api/carpetas
- Crear `backend/document-core/docs/CARPETAS.md` con ejemplos y flujos

---

### Requisitos No Funcionales

| Aspecto | Requerimiento |
|--------|--------------|
| **Seguridad** | 1. Validar `organizacion_id` del token en todas las operaciones 2. No permitir creación cross-org (carpeta padre de otra org) 3. Evaluar permisos en servidor, no confiar en cliente 4. Sanitizar `nombre` y `descripcion` para prevenir inyecciones |
| **Performance** | 1. Índice en `(organizacion_id, carpeta_padre_id)` para búsquedas rápidas 2. Índice único parcial en `(organizacion_id, carpeta_padre_id, nombre)` optimiza unicidad 3. Query de validación de permisos debe usar índices existentes (de US-ACL-002) |
| **Auditoría** | 1. Emitir evento `CARPETA_CREADA` con `usuario_id`, `carpeta_id`, `organizacion_id`, `carpeta_padre_id` 2. Incluir `creado_por` en registro de BD |
| **Escalabilidad** | 1. Soportar jerarquía indefinida (sin límite de profundidad) 2. Estructura flexible para miles de carpetas por organización |
| **Usabilidad** | 1. Errores específicos: `CARPETA_NO_ENCONTRADA`, `SIN_PERMISO_CARPETA`, `NOMBRE_DUPLICADO` 2. Mensajes de error en español con detalles útiles |
| **Integridad de Datos** | 1. Transacción atómica: crear carpeta + emitir auditoría 2. Rollback automático si auditoría falla 3. Soft delete permite reusar nombres (con validación lógica) |
| **Compatibilidad** | 1. Integrar con evaluador de permisos de US-ACL-006 (cuando disponible) 2. Respetar modelo de carpeta raíz de US-FOLDER-001 (inicialización de org) |

---

### Inicialización de Carpeta Raíz por Organización

**Comportamiento esperado:**
- Cuando se crea una nueva organización (en US-ADMIN-001 o fixture de test), se inserta automáticamente una carpeta raíz con:
  - `id`: UUID único
  - `organizacion_id`: ID de la organización
  - `carpeta_padre_id`: NULL (indica raíz)
  - `nombre`: `"Raíz"` o `"Root"` o ID de org (definir con equipo)
  - `creado_por`: Sistema (NULL o UUID especial)
  - `fecha_creacion`: NOW()

**Implementación:**
- Opción A: Trigger en BD que inserte raíz al crear organización
- Opción B: Migración V004 que crea raíz para org existentes + lógica en servicio de org
- Recomendado: Opción B (más explícita y testeable)

---

### Plan de Ejecución Recomendado

1. **Paso 1 (BD):** Crear migración de tabla `carpetas` con índices y restricciones
2. **Paso 2 (BD):** Crear migración V004 para insertar raíz por organización
3. **Paso 3 (Backend - Domain):** Crear entidad `Carpeta` inmutable con validaciones
4. **Paso 4 (Backend - Persistencia):** Implementar repositorio JPA con métodos de búsqueda
5. **Paso 5 (Backend - Service):** Implementar `CarpetaService` con lógica de creación y validaciones
6. **Paso 6 (Backend - API):** Implementar controlador y DTOs
7. **Paso 7 (Backend - Testing):** Tests unitarios (TDD) + tests de integración
9. **Paso 8 (Documentación):** Actualizar OpenAPI y README

---

### Validación de Completitud

Para considerar US-FOLDER-001 **completada**, verificar:

✅ **Especificación Funcional**
- [ ] 12 escenarios de aceptación documentados (crear, permisos, duplicación, aislamiento, validación, herencia)
- [ ] Tabla `carpetas` con índices y restricciones definidas
- [ ] Estructura JSON de request/response con ejemplos reales
- [ ] Endpoint REST con validaciones específicas

✅ **Estructura de Datos**
- [ ] Tabla con campos: id, org_id, padre_id, nombre, descripcion, creado_por, fechas, fecha_eliminacion
- [ ] Índices: búsqueda por org, padre; unicidad compuesta; soft delete

✅ **Arquitectura y Código**
- [ ] Entidad de dominio inmutable (`Carpeta`)
- [ ] Repositorio con queries para padre, org, búsqueda
- [ ] Servicio de validación centralizado (permisos, unicidad)
- [ ] Controlador REST mapeado `/api/carpetas` POST
- [ ] DTOs con MapStruct

✅ **Lógica de Negocio**
- [ ] Validación de permisos en carpeta padre
- [ ] Validación de unicidad por nivel (mismo padre)
- [ ] Aislamiento por organización
- [ ] Manejo de soft delete

✅ **Testing**
- [ ] Tests unitarios de servicio y repositorio (TDD, >90% cobertura)
- [ ] Validación de aislamiento, permisos, duplicación, 404

✅ **Documentación**
- [ ] OpenAPI/Swagger actualizado
- [ ] README de feature con ejemplos
- [ ] Seed data para pruebas

✅ **Requisitos No Funcionales**
- [ ] Auditoría de creación
- [ ] Inicialización de carpeta raíz
- [ ] Validación de seguridad (org isolation)
- [ ] Errores específicos y mensajes claros

---

**Status:** Lista para estimación y desarrollo  
**Complejidad Estimada:** Media (1.5-2 días con asistencia IA)  
**Dependencias:** US-ACL-001 (catálogo completado), US-ACL-002 (opcional si permisos disponibles), US-ACL-006 (evaluador de permisos, puede usar stub mientras se desarrolla)  
**Bloqueadores:** Ninguno (puede iniciar con evaluador de permisos simplificado)
