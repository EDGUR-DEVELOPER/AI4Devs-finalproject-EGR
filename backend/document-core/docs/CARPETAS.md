# Carpetas (Folders) - Documentación Técnica

## Información General

**Módulo:** document-core  
**Feature:** US-FOLDER-001 - Crear Carpeta  
**Fecha:** 2026-01-28  
**Versión:** 1.0.0

## Descripción

El módulo de Carpetas proporciona una estructura jerárquica para organizar documentos en DocFlow. Permite crear, consultar y eliminar (soft delete) carpetas con control granular de permisos y aislamiento multi-tenant estricto.

## Arquitectura

### Capas Implementadas

```
┌─────────────────────────────────────────────────────────────┐
│                     API Layer (REST)                         │
│  CarpetaController, DTOs (CreateCarpetaDTO, CarpetaDTO)     │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────────┐
│                  Application Layer                           │
│  CarpetaService, CarpetaValidator                            │
│  (Orquestación de lógica de negocio)                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────────┐
│                     Domain Layer                             │
│  Carpeta (modelo inmutable), ICarpetaRepository,            │
│  Excepciones, Eventos (CarpetaCreatedEvent)                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────────────┐
│                 Infrastructure Layer                         │
│  CarpetaEntity (JPA), CarpetaJpaRepository,                  │
│  CarpetaRepositoryAdapter, CarpetaMapper                     │
└─────────────────────────────────────────────────────────────┘
```

### Modelo de Dominio

**Carpeta** es una entidad de dominio inmutable que representa una carpeta en la jerarquía:

```java
public final class Carpeta {
    private final UUID id;
    private final UUID organizacionId;
    private final UUID carpetaPadreId;  // null = carpeta raíz
    private final String nombre;
    private final String descripcion;   // opcional
    private final UUID creadoPor;
    private final Instant fechaCreacion;
    private final Instant fechaActualizacion;
    private final Instant fechaEliminacion;  // null = activa
    
    // Métodos de negocio
    public boolean esRaiz();
    public boolean estaActiva();
    public void validarIntegridad();
}
```

**Características:**
- Inmutable (patrón Builder para construcción)
- Validación de reglas de negocio en el constructor
- Sin anotaciones de infraestructura (JPA, JSON)

## Esquema de Base de Datos

### Tabla: carpetas

```sql
CREATE TABLE carpetas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizacion_id     UUID NOT NULL,
    carpeta_padre_id    UUID,
    nombre              VARCHAR(255) NOT NULL,
    descripcion         VARCHAR(500),
    creado_por          UUID NOT NULL,
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion   TIMESTAMP,
    
    CONSTRAINT fk_carpeta_padre FOREIGN KEY (carpeta_padre_id) 
        REFERENCES carpetas(id) ON DELETE SET NULL
);
```

**Índices:**
- `idx_carpetas_org_padre`: (organizacion_id, carpeta_padre_id) - Optimiza consultas de hijos
- `idx_carpetas_org_nombre`: (organizacion_id, nombre) - Búsquedas por nombre
- `idx_carpetas_unique_nombre_por_nivel`: Garantiza unicidad de nombres por nivel
- `idx_carpetas_fecha_eliminacion`: Para filtrado de carpetas eliminadas
- `idx_carpetas_creado_por`: Para consultas de auditoría

## API REST

### Endpoints

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/carpetas` | Crear carpeta | Requerido |
| GET | `/api/carpetas/{id}` | Obtener carpeta por ID | Requerido |
| GET | `/api/carpetas/{id}/hijos` | Listar subcarpetas | Requerido |
| GET | `/api/carpetas/raiz` | Obtener carpeta raíz | Requerido |
| DELETE | `/api/carpetas/{id}` | Eliminar carpeta (soft delete) | Requerido |

### Ejemplo: Crear Carpeta

**Request:**
```http
POST /api/carpetas
Authorization: Bearer {jwt_token}
X-Organization-Id: {uuid}  # Inyectado por gateway desde JWT
X-User-Id: {uuid}          # Inyectado por gateway desde JWT
Content-Type: application/json

{
  "carpeta_padre_id": "770e8400-e29b-41d4-a716-446655440222",
  "nombre": "Proyecto Alpha",
  "descripcion": "Documentos del proyecto Alpha"
}
```

**Response (201 Created):**
```json
{
  "id": "0b033d5c-bf42-41ec-b181-9e332ba79350",
  "organizacion_id": "5eb242bc-180b-46f8-aab3-34abf2a00d84",
  "carpeta_padre_id": "770e8400-e29b-41d4-a716-446655440222",
  "nombre": "Proyecto Alpha",
  "descripcion": "Documentos del proyecto Alpha",
  "creado_por": "b1070b64-642f-4cfc-b93f-146d46d4f5f1",
  "fecha_creacion": "2026-01-28T18:45:08.519Z",
  "fecha_actualizacion": "2026-01-28T18:45:08.519Z",
  "es_raiz": false
}
```

### Códigos de Estado HTTP

| Código | Descripción | Causa |
|--------|-------------|-------|
| 201 | Created | Carpeta creada exitosamente |
| 200 | OK | Consulta exitosa |
| 204 | No Content | Eliminación exitosa |
| 400 | Bad Request | Datos de entrada inválidos |
| 401 | Unauthorized | Token ausente o inválido |
| 403 | Forbidden | Sin permisos en carpeta padre |
| 404 | Not Found | Carpeta padre no existe |
| 409 | Conflict | Nombre duplicado en nivel |
| 500 | Internal Server Error | Error del servidor |

## Reglas de Negocio

### 1. Unicidad de Nombres

Los nombres de carpeta deben ser únicos dentro del mismo nivel jerárquico:
- Misma `organizacion_id`
- Mismo `carpeta_padre_id` (o ambos null para carpetas raíz)
- Solo carpetas activas (`fecha_eliminacion IS NULL`)

**Ejemplo:**
```
✅ Válido:
  - Carpeta "Proyectos" bajo Raíz
  - Carpeta "Proyectos" bajo "Archivados"  (distinto padre)

❌ Inválido:
  - Crear segunda carpeta "Proyectos" bajo Raíz
```

### 2. Permisos

Se requiere permiso de **ESCRITURA** o **ADMINISTRACION** en la carpeta padre para crear subcarpetas.

> **Nota:** Actualmente usa un stub de validación de permisos. Integración real con US-ACL-006 pendiente.

### 3. Multi-Tenancy

Todas las operaciones están estrictamente aisladas por `organizacion_id`:
- Se extrae del token JWT (no del cuerpo de la petición)
- Inyectado por el gateway como header `X-Organization-Id`
- Filtro automático en todas las consultas

**Seguridad:** Si una carpeta no existe o pertenece a otra organización, se retorna **404** (no 403) para evitar filtración de información.

### 4. Soft Delete

Las carpetas no se eliminan físicamente:
- Se establece `fecha_eliminacion` al timestamp actual
- Automáticamente excluidas de consultas (via `@Where` clause)
- Permite reutilizar nombres después de eliminación

### 5. Carpeta Raíz

Cada organización debe tener exactamente una carpeta raíz:
- `carpeta_padre_id IS NULL`
- Creada automáticamente por migración `V004`
- Nombre por defecto: "Raíz"

## Eventos de Dominio

### CarpetaCreatedEvent

Emitido cuando se crea una carpeta exitosamente.

```java
public record CarpetaCreatedEvent(
    UUID carpetaId,
    UUID organizacionId,
    UUID usuarioId,
    String nombre,
    UUID carpetaPadreId,
    Instant timestamp
)
```

**Consumidores:**
- Sistema de auditoría (logging de operaciones)

## Manejo de Errores

Todas las excepciones de dominio extienden `DomainException` y se mapean a respuestas HTTP via `GlobalExceptionHandler`:

```java
// Excepciones disponibles
CarpetaNotFoundException        → 404 NOT FOUND
CarpetaNombreDuplicadoException → 409 CONFLICT
SinPermisoCarpetaException      → 403 FORBIDDEN
```

**Formato de Error (RFC 7807):**
```json
{
  "type": "https://docflow.com/errors/carpeta-not-found",
  "title": "Carpeta No Encontrada",
  "status": 404,
  "detail": "No se encontró la carpeta con ID: {uuid}",
  "timestamp": "2026-01-28T18:45:08.519Z",
  "errorCode": "CARPETA_NO_ENCONTRADA"
}
```

## Testing

### Tests Unitarios

**Archivo:** `CarpetaServiceTest.java`  
**Cobertura:** 100% (11 tests)

**Casos cubiertos:**
- ✅ Creación exitosa con datos válidos
- ✅ Creación sin descripción (campo opcional)
- ✅ Validación de carpeta padre inexistente
- ✅ Validación de nombre duplicado
- ✅ Obtener carpeta por ID
- ✅ Carpeta no encontrada
- ✅ Listar carpetas hijas
- ✅ Obtener carpeta raíz
- ✅ Eliminación lógica
- ✅ Eliminar carpeta inexistente
- ✅ Emisión de evento CarpetaCreatedEvent

**Comando:**
```bash
mvn test -Dtest=CarpetaServiceTest
```

### Tests de Integración

Pendiente: Agregar tests de integración con base de datos H2 in-memory.

## Dependencias

```xml
<!-- Core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Mappers -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>

<!-- Documentación -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc-openapi.version}</version>
</dependency>
```

## Integraciones

### Servicios Externos

| Servicio | Integración | Estado |
|----------|-------------|--------|
| Sistema ACL (US-ACL-006) | Validación de permisos | Stub (pendiente) |
| Sistema de Auditoría | Eventos de dominio | Implementado |
| Gateway | Inyección de headers | Requerido |

### Servicios Dependientes

- **document-core**: Módulo actual
- **gateway**: Enrutamiento y autenticación
- **identity**: Generación de tokens JWT

## Migraciones

**V003_Create_Carpetas_Table.sql:**
- Crea tabla `carpetas`
- Define índices para optimización
- Establece restricciones de integridad

**V004_Create_Carpeta_Raiz_Por_Organizacion.sql:**
- Inicializa carpeta raíz para organizaciones existentes
- Documenta proceso para nuevas organizaciones

## Roadmap

### Mejoras Futuras

- [ ] Mover carpetas (cambiar `carpeta_padre_id`)
- [ ] Renombrar carpetas
- [ ] Copiar carpetas con contenido
- [ ] Ruta completa jerárquica (breadcrumb)
- [ ] Búsqueda avanzada por nombre
- [ ] Ordenamiento personalizado
- [ ] Integración real con sistema ACL
- [ ] Tests de integración
- [ ] Caché de consultas frecuentes

## Referencias

- [Plan de Implementación](../../ai-specs/changes/US-FOLDER-001_backend.md)
- [Especificación API](../../ai-specs/specs/api-spec.yml)
- [Modelo de Datos](../../ai-specs/specs/data-model.md)
- [US-FOLDER-001](../../US/tickets/P3-Gestion/US-FOLDER-001.md)

---

**Última actualización:** 2026-01-28  
**Autor:** DocFlow Team
