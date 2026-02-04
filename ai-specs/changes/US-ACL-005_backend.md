# Backend Implementation Plan: US-ACL-005 - Permisos Explícitos de Documento

## 1. Overview

**Feature:** Implementar la funcionalidad de asignar permisos explícitos a usuarios sobre documentos específicos (ACL de documento), permitiendo excepciones de acceso sin modificar los permisos de la carpeta contenedora.

**Architecture Principles Applied:**
- **Domain-Driven Design (DDD):** Manteniendo la lógica de negocio en la capa de dominio con agregados bien delimitados.
- **Layered Architecture:** Separación clara entre Domain, Application, e Infrastructure layers.
- **Hexagonal Architecture / Ports & Adapters:** Controladores como adaptadores de entrada, repositorios como puertos de persistencia.

**Key Business Rules:**
1. Solo administradores o usuarios con permiso `ADMINISTRACION` sobre la carpeta padre pueden asignar/revocar permisos de documento.
2. Los permisos deben estar aislados multi-tenant (validar organización del JWT).
3. Un único ACL por (documento, usuario): crear nuevo o actualizar existente.
4. Los permisos de documento no alteran la herencia de carpeta.

## 2. Architecture Context

### Layers Involved

| Layer | Components |
|-------|------------|
| **Domain** | `PermisoDocumentoUsuario` (Entity), Domain Events, Exceptions, Repository Interfaces |
| **Application** | Services (`PermisoDocumentoUsuarioService`), Validators, DTOs, Mappers |
| **Infrastructure** | JPA Repositories, Controllers, Event Listeners, DB Migrations |

### Existing Components to Reuse

- **`NivelAcceso`**: Enumeración de niveles de acceso (LECTURA, ESCRITURA, ADMINISTRACION, NINGUNO)
- **`PermisoCarpetaUsuarioService`**: Patrón de servicio a replicar
- **`CurrentTenantService`**: Para validar multi-tenancy
- **`PermisoHerenciaService`**: Para comprender modelo de herencia (NO se verá afectado por este ticket)
- **Multi-tenant filters** (Hibernate Filter, TenantEntityListener)

### Database Table (Already Exists)

```sql
CREATE TABLE permiso_documento_usuario (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    documento_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    organizacion_id BIGINT NOT NULL,
    nivel_acceso VARCHAR(20) NOT NULL,
    fecha_expiracion TIMESTAMP NULL,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (documento_id, usuario_id),
    FOREIGN KEY (documento_id) REFERENCES documento(id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    FOREIGN KEY (organizacion_id) REFERENCES organizacion(id)
);
```

## 3. Implementation Steps

---

### Step 1: Create DTOs and Validators

**Files to Create:**
- `src/main/java/com/docflow/documentcore/application/dto/CreatePermisoDocumentoUsuarioDTO.java`
- `src/main/java/com/docflow/documentcore/application/dto/UpdatePermisoDocumentoUsuarioDTO.java`
- `src/main/java/com/docflow/documentcore/application/dto/PermisoDocumentoUsuarioDTO.java`
- `src/main/java/com/docflow/documentcore/application/validator/PermisoDocumentoUsuarioValidator.java`

**Action:** Definir DTOs de entrada/salida y validadores de negocio.

**Function Signature:**

```java
// CreatePermisoDocumentoUsuarioDTO
public class CreatePermisoDocumentoUsuarioDTO {
    private Long usuarioId;
    private String nivelAccesoCodigo; // e.g., "LECTURA", "ESCRITURA"
    private OffsetDateTime fechaExpiracion; // nullable, para accesos temporales
}

// UpdatePermisoDocumentoUsuarioDTO
public class UpdatePermisoDocumentoUsuarioDTO {
    private String nivelAccesoCodigo;
    private OffsetDateTime fechaExpiracion;
}

// PermisoDocumentoUsuarioDTO (respuesta)
public class PermisoDocumentoUsuarioDTO {
    private Long id;
    private Long documentoId;
    private Long usuarioId;
    private UsuarioResumenDTO usuario; // populated en el controlador
    private NivelAccesoDTO nivelAcceso;
    private OffsetDateTime fechaExpiracion;
    private OffsetDateTime fechaAsignacion;
}

// PermisoDocumentoUsuarioValidator
public class PermisoDocumentoUsuarioValidator {
    // Métodos de validación (análogos a PermisoCarpetaUsuarioValidator)
}
```

**Implementation Steps:**

1. **Crear `CreatePermisoDocumentoUsuarioDTO`:**
   - Propiedad `usuarioId` (Long, @NotNull)
   - Propiedad `nivelAccesoCodigo` (String, @NotBlank, validación contra enum)
   - Propiedad `fechaExpiracion` (OffsetDateTime, nullable)
   - Aplicar `@Valid` en controlador

2. **Crear `UpdatePermisoDocumentoUsuarioDTO`:**
   - Propiedad `nivelAccesoCodigo` (String, @NotNull)
   - Propiedad `fechaExpiracion` (OffsetDateTime, nullable)

3. **Crear `PermisoDocumentoUsuarioDTO`:**
   - Getters/setters para `id`, `documentoId`, `usuarioId`, `nivelAcceso`, `fechaExpiracion`, `fechaAsignacion`
   - Propiedad `usuario` (UsuarioResumenDTO) para enriquecer respuesta en controlador

4. **Crear `PermisoDocumentoUsuarioValidator`:**
   - Método `validarAdministrador(usuarioAdminId, documentoId, organizacionId)`:
     - Validar que usuario sea admin O tenga permiso ADMINISTRACION en carpeta padre del documento
     - Lanzar `AccessDeniedException` si no cumple
   - Método `validarDocumentoExiste(documentoId, organizacionId)`:
     - Usar repositorio de documentos para validar existencia
     - Lanzar `ResourceNotFoundException` si no existe
   - Método `validarUsuarioPerteneceOrganizacion(usuarioId, organizacionId)`:
     - Validar que usuario exista en la organización
     - Lanzar `ResourceNotFoundException` si no existe
   - Método `validarNivelAccesoCodigo(codigo: String): CodigoNivelAcceso`:
     - Validar que el código sea válido (enum)
     - Lanzar `InvalidRequestException` si es inválido
   - Método `validarNoDuplicado(documentoId, usuarioId)`:
     - Si existe ACL, permitir (será actualización)
   - Método `validarDocumentoEnOrganizacion(documentoId, organizacionId)`:
     - Validar que documento pertenezca a la organización (contra filtro multi-tenant)
     - Lanzar `ResourceNotFoundException` genérico si no pertenece

**Dependencies:**
- `com.docflow.documentcore.domain.model.acl.CodigoNivelAcceso`
- `com.docflow.documentcore.domain.exception.AccessDeniedException`
- `com.docflow.documentcore.domain.exception.ResourceNotFoundException`
- `jakarta.validation.constraints.*`

**Implementation Notes:**
- Los DTOs deben ser **DTOs de aplicación** (no confundir con entidades JPA).
- Usar Lombok (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`) para reducir boilerplate.
- El validador debe inyectarse en el servicio y en el controlador.
- Mantener los errores genéricos para no exponer datos de otras organizaciones.

---

### Step 2: Create Domain Event Classes

**Files to Create:**
- `src/main/java/com/docflow/documentcore/domain/event/PermisoDocumentoUsuarioCreatedEvent.java`
- `src/main/java/com/docflow/documentcore/domain/event/PermisoDocumentoUsuarioUpdatedEvent.java`
- `src/main/java/com/docflow/documentcore/domain/event/PermisoDocumentoUsuarioRevokedEvent.java`

**Action:** Definir eventos de dominio para auditoría y eventos asincronos.

**Function Signature:**

```java
public class PermisoDocumentoUsuarioCreatedEvent {
    private final Long permisoId;
    private final Long documentoId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final String nivelAcceso;
    private final Long otorgadoPor;
    private final Instant timestamp;
}

public class PermisoDocumentoUsuarioUpdatedEvent {
    private final Long permisoId;
    private final Long documentoId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final String nivelAccesoAnterior;
    private final String nivelAccesoNuevo;
    private final Long actualizadoPor;
    private final Instant timestamp;
}

public class PermisoDocumentoUsuarioRevokedEvent {
    private final Long permisoId;
    private final Long documentoId;
    private final Long usuarioId;
    private final Long organizacionId;
    private final String nivelAccesoAnterior;
    private final Long revocadoPor;
    private final Instant timestamp;
}
```

**Implementation Steps:**

1. Crear clase `PermisoDocumentoUsuarioCreatedEvent` con:
   - Constructor que acepte todos los parámetros
   - Getters (sin setters, events son inmutables)
   - Usar `@Record` de Java 14+ o clases finales con constructores
   - Documentar que es un evento de dominio

2. Crear clase `PermisoDocumentoUsuarioUpdatedEvent` similar, incluyendo:
   - `nivelAccesoAnterior` y `nivelAccesoNuevo` para auditar cambios

3. Crear clase `PermisoDocumentoUsuarioRevokedEvent` similar

**Dependencies:**
- `java.time.Instant`
- Ningún framework (eventos de dominio puros)

**Implementation Notes:**
- Los eventos son registros inmutables de cambios.
- Se publican vía `ApplicationEventPublisher` de Spring.
- Pueden consumirse desde listeners async (auditoría, notificaciones).
- No incluir datos sensibles en los eventos.

---

### Step 3: Create Application Service

**File to Create:**
- `src/main/java/com/docflow/documentcore/application/service/PermisoDocumentoUsuarioService.java`

**Action:** Implementar la lógica de aplicación para crear, actualizar y revocar permisos de documento.

**Function Signature:**

```java
@Service
@Transactional
public class PermisoDocumentoUsuarioService {
    
    // Crear o actualizar permiso explícito de documento
    public PermisoDocumentoUsuario crearOActualizarPermiso(
        Long documentoId,
        CreatePermisoDocumentoUsuarioDTO dto,
        Long organizacionId,
        Long usuarioAdminId
    ) -> PermisoDocumentoUsuario;
    
    // Revocar permiso de documento
    public void revocarPermiso(
        Long documentoId,
        Long usuarioId,
        Long organizacionId,
        Long usuarioAdminId
    ) -> void;
    
    // Listar permisos de un documento
    @Transactional(readOnly = true)
    public List<PermisoDocumentoUsuario> listarPermisos(
        Long documentoId,
        Long organizacionId
    ) -> List<PermisoDocumentoUsuario>;
    
    // Obtener resúmenes de usuarios para respuesta
    @Transactional(readOnly = true)
    public Map<Long, UsuarioResumenDTO> obtenerUsuariosResumen(
        List<PermisoDocumentoUsuario> permisos,
        Long organizacionId
    ) -> Map<Long, UsuarioResumenDTO>;
}
```

**Implementation Steps:**

1. **Método `crearOActualizarPermiso`:**
   - Validar que el documento existe en la organización (`validator.validarDocumentoEnOrganizacion`)
   - Validar que el usuario admin tiene permisos (`validator.validarAdministrador`)
   - Validar que el usuario destino pertenece a la organización
   - Validar el código de nivel de acceso
   - Si existe ACL, actualizar `nivelAcceso` y `fechaExpiracion`
   - Si no existe, crear nueva ACL con `fechaAsignacion = now()`
   - Publicar evento correspondiente (Created o Updated)
   - Retornar la entidad persistida
   - Log INFO: "Creando/Actualizando permiso de documento para usuario {userId} en documento {documentoId}"

2. **Método `revocarPermiso`:**
   - Validar permisos del admin
   - Buscar ACL por (documentoId, usuarioId)
   - Si no existe, lanzar `ResourceNotFoundException`
   - Leer nivel anterior (para evento)
   - Eliminar del repositorio
   - Publicar evento Revoked
   - Log INFO: "Revocando permiso de documento para usuario {userId} del documento {documentoId}"

3. **Método `listarPermisos`:**
   - Validar que documento existe en organización
   - Retornar lista de ACLs del documento (filtradas por tenant automáticamente)

4. **Método `obtenerUsuariosResumen`:**
   - Extraer IDs de usuarios únicos de la lista de permisos
   - Llamar a `usuarioRepository.findActiveByIdsAndOrganizacionId(...)`
   - Construir mapa userId -> UsuarioResumenDTO
   - Retornar mapa (puede ser vacío si no hay usuarios)

**Dependencies:**
- `IPermisoDocumentoUsuarioRepository`
- `DocumentoJpaRepository` (o similar)
- `UsuarioJpaRepository`
- `PermisoDocumentoUsuarioValidator`
- `ApplicationEventPublisher` (Spring)

**Implementation Notes:**
- Usar `@Transactional` para asegurar atomicidad
- Los eventos se publican dentro de la transacción (se envían al finalizar exitosamente)
- Mantener logs descriptivos pero sin datos sensibles
- Reutilizar validador y patrón de `PermisoCarpetaUsuarioService`
- No incluir lógica de herencia (responsabilidad de `PermisoHerenciaService`)

---

### Step 4: Create Mapper

**File to Create:**
- `src/main/java/com/docflow/documentcore/application/mapper/PermisoDocumentoUsuarioMapper.java`

**Action:** Mapear entidades de dominio a DTOs de respuesta.

**Function Signature:**

```java
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PermisoDocumentoUsuarioMapper {
    
    PermisoDocumentoUsuarioDTO toDto(PermisoDocumentoUsuario entity);
    
    List<PermisoDocumentoUsuarioDTO> toDtoList(List<PermisoDocumentoUsuario> entities);
}
```

**Implementation Steps:**

1. Crear interfaz con `@Mapper` (MapStruct)
2. Definir método `toDto` que mapee campos básicos:
   - `id`, `documentoId`, `usuarioId`, `nivelAcceso`, `fechaExpiracion`, `fechaAsignacion`
3. La propiedad `usuario` (UsuarioResumenDTO) se enriquece en el controlador
4. Definir método `toDtoList` para listas

**Dependencies:**
- `org.mapstruct.Mapper`
- `org.mapstruct.InjectionStrategy`

**Implementation Notes:**
- Usar MapStruct como en el proyecto (ver `pom.xml`)
- La propiedad `usuario` se completa en el controlador, no en el mapper
- El mapper es **stateless** y reutilizable

---

### Step 5: Create JPA Repository

**File to Create:**
- `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/jpa/PermisoDocumentoUsuarioJpaRepository.java`

**Action:** Implementar repositorio JPA con queries específicas.

**Function Signature:**

```java
public interface PermisoDocumentoUsuarioJpaRepository extends JpaRepository<PermisoDocumentoUsuario, Long> {
    
    Optional<PermisoDocumentoUsuario> findByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    
    List<PermisoDocumentoUsuario> findByDocumentoId(Long documentoId);
    
    List<PermisoDocumentoUsuario> findByUsuarioId(Long usuarioId);
    
    void deleteByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
}
```

**Implementation Steps:**

1. Extender `JpaRepository<PermisoDocumentoUsuario, Long>`
2. Definir método `findByDocumentoIdAndUsuarioId`:
   - Retorna Optional (para validar existencia antes de actualizar)
   - Query derivado por nombres de propiedades
3. Definir método `findByDocumentoId`:
   - Retorna lista de ACLs para un documento
4. Definir método `findByUsuarioId`:
   - Retorna lista de ACLs asignados a un usuario (útil para auditoría)
5. Definir método `deleteByDocumentoIdAndUsuarioId`:
   - Para revocar permisos

**Dependencies:**
- `org.springframework.data.jpa.repository.JpaRepository`
- `PermisoDocumentoUsuario` (entity)

**Implementation Notes:**
- El filtro multi-tenant (Hibernate) se aplica automáticamente
- No añadir `@Query` a menos que la query derivada sea insuficiente
- Los índices están definidos en la entity (`@UniqueConstraint`, `@Index`)

---

### Step 6: Create Domain Repository Interface

**File to Create:**
- `src/main/java/com/docflow/documentcore/domain/repository/IPermisoDocumentoUsuarioRepository.java`

**Action:** Definir interfaz de repositorio en capa de dominio (puerto hexagonal).

**Function Signature:**

```java
public interface IPermisoDocumentoUsuarioRepository {
    
    PermisoDocumentoUsuario save(PermisoDocumentoUsuario permiso);
    
    Optional<PermisoDocumentoUsuario> findByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
    
    List<PermisoDocumentoUsuario> findByDocumentoId(Long documentoId);
    
    void delete(PermisoDocumentoUsuario permiso);
    
    void deleteByDocumentoIdAndUsuarioId(Long documentoId, Long usuarioId);
}
```

**Implementation Steps:**

1. Definir métodos de persistencia que necesita el dominio
2. Retornar tipos de dominio (`PermisoDocumentoUsuario`, Optional)
3. No exponer detalles de JPA (sin `Page`, `Pageable`, etc.)

**Implementation Notes:**
- Este es el **puerto** (interfaz) del Hexagonal
- El adaptador (JpaRepository) implementa esta interfaz
- Permite cambiar de estrategia de persistencia sin tocar el dominio

---

### Step 7: Create Repository Adapter

**File to Create:**
- `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/PermisoDocumentoUsuarioRepositoryAdapter.java`

**Action:** Adaptador que implementa la interfaz de dominio usando el JPA repository.

**Function Signature:**

```java
@Repository
public class PermisoDocumentoUsuarioRepositoryAdapter implements IPermisoDocumentoUsuarioRepository {
    
    private final PermisoDocumentoUsuarioJpaRepository jpaRepository;
    
    public PermisoDocumentoUsuarioRepositoryAdapter(
        PermisoDocumentoUsuarioJpaRepository jpaRepository
    ) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public PermisoDocumentoUsuario save(PermisoDocumentoUsuario permiso) {
        return jpaRepository.save(permiso);
    }
    
    // ... delegación a JPA repository
}
```

**Implementation Steps:**

1. Anotar con `@Repository`
2. Inyectar `PermisoDocumentoUsuarioJpaRepository`
3. Implementar todos los métodos de la interfaz delegando al JPA repository
4. No añadir lógica adicional (responsabilidad del servicio)

**Implementation Notes:**
- Patrón adapter: traduce de dominio a infraestructura
- Reutilizar patrón de `PermisoCarpetaUsuarioRepositoryAdapter` si existe

---

### Step 8: Create REST Controller

**File to Create:**
- `src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/PermisoDocumentoUsuarioController.java`

**Action:** Implementar endpoints REST para gestión de permisos de documento.

**Endpoints:**

```
POST   /api/documentos/{documentoId}/permisos
PATCH  /api/documentos/{documentoId}/permisos/{usuarioId}
DELETE /api/documentos/{documentoId}/permisos/{usuarioId}
GET    /api/documentos/{documentoId}/permisos
```

**Function Signature:**

```java
@RestController
@RequestMapping("/api/documentos/{documentoId}/permisos")
@Tag(name = "ACL - Document Permissions", description = "Endpoints for document ACL management")
@SecurityRequirement(name = "bearer-jwt")
public class PermisoDocumentoUsuarioController {
    
    // Crear o actualizar permiso
    @PostMapping
    public ResponseEntity<PermisoDocumentoUsuarioDTO> crearPermiso(
        @PathVariable Long documentoId,
        @Valid @RequestBody CreatePermisoDocumentoUsuarioDTO request,
        @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId,
        @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioAdminId
    ) -> ResponseEntity<PermisoDocumentoUsuarioDTO>;
    
    // Revocar permiso
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> revocarPermiso(
        @PathVariable Long documentoId,
        @PathVariable Long usuarioId,
        @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId,
        @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioAdminId
    ) -> ResponseEntity<Void>;
    
    // Listar permisos de documento
    @GetMapping
    public ResponseEntity<List<PermisoDocumentoUsuarioDTO>> listarPermisos(
        @PathVariable Long documentoId,
        @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId
    ) -> ResponseEntity<List<PermisoDocumentoUsuarioDTO>>;
}
```

**Implementation Steps:**

1. **Método `crearPermiso` (POST):**
   - Path: `POST /api/documentos/{documentoId}/permisos`
   - Parsear request body con `@Valid`
   - Llamar a `service.crearOActualizarPermiso(...)`
   - Enriquecer respuesta con usuarios resumen
   - Retornar `201 Created` si es nuevo, `200 OK` si es actualización
   - En caso de error: `400 Bad Request` (nivel inválido), `403 Forbidden` (sin autorización), `404 Not Found` (documento/usuario no existe)

2. **Método `revocarPermiso` (DELETE):**
   - Path: `DELETE /api/documentos/{documentoId}/permisos/{usuarioId}`
   - Llamar a `service.revocarPermiso(...)`
   - Retornar `204 No Content` si exitoso
   - En caso de error: `403 Forbidden`, `404 Not Found`

3. **Método `listarPermisos` (GET):**
   - Path: `GET /api/documentos/{documentoId}/permisos`
   - Llamar a `service.listarPermisos(...)`
   - Enriquecer respuesta con usuarios resumen
   - Retornar `200 OK` con lista de DTOs

**Dependencies:**
- `PermisoDocumentoUsuarioService`
- `PermisoDocumentoUsuarioMapper`
- Swagger annotations (`@Operation`, `@Tag`, `@SecurityRequirement`)

**Implementation Notes:**
- Usar `@PathVariable` para IDs en URL
- Validar con `@Valid` en DTOs de entrada
- Headers `X-Organization-Id` y `X-User-Id` (seguir patrón existente en otros controladores)
- Documentar con Swagger (anotaciones OpenAPI v3)
- Mantener errores genéricos para seguridad (404 si documento no pertenece a org)
- Enriquecer respuesta con datos de usuario en controlador (no en mapper)

---

### Step 9: Create Unit Tests

**Files to Create:**
- `src/test/java/com/docflow/documentcore/application/service/PermisoDocumentoUsuarioServiceTest.java`
- `src/test/java/com/docflow/documentcore/application/validator/PermisoDocumentoUsuarioValidatorTest.java`
- `src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/PermisoDocumentoUsuarioControllerTest.java`

**Action:** Escribir tests unitarios siguiendo patrón BDD y TDD.

**Test Scenarios - Service:**

```gherkin
# PermisoDocumentoUsuarioServiceTest

Scenario: Crear permiso nuevo de documento exitosamente
  Given un documento "Contrato.pdf" en organización 1
  And un usuario admin en organización 1
  And un usuario sin permisos en organización 1
  When creo permiso LECTURA para el usuario en el documento
  Then se crea nueva ACL
  And se emite evento PermisoDocumentoUsuarioCreatedEvent
  And status 201

Scenario: Actualizar permiso existente a nivel superior
  Given un documento con permiso LECTURA asignado a usuario
  When actualizo a ESCRITURA
  Then ACL se actualiza (no se crea duplicado)
  And se emite evento PermisoDocumentoUsuarioUpdatedEvent
  And status 200

Scenario: Validación falla - documento no existe
  When intento crear permiso en documento inexistente
  Then lanza ResourceNotFoundException
  And status 404

Scenario: Validación falla - usuario de otra organización
  Given usuario de organización 2
  And documento de organización 1
  When intento crear permiso
  Then lanza ResourceNotFoundException (genérico)
  And status 404

Scenario: Autorización falla - usuario no es admin
  Given usuario sin rol admin
  And documento en carpeta sin permiso ADMINISTRACION
  When intento crear permiso
  Then lanza AccessDeniedException
  And status 403

Scenario: Revocar permiso exitosamente
  Given documento con permiso asignado
  And usuario admin
  When revoco el permiso
  Then se elimina ACL
  And se emite evento PermisoDocumentoUsuarioRevokedEvent
  And status 204

Scenario: Revocar permiso inexistente
  When intento revocar permiso que no existe
  Then lanza ResourceNotFoundException
  And status 404

Scenario: Nivel de acceso inválido
  When intento crear con nivelAccesoCodigo = "INVALIDO"
  Then validador rechaza
  And status 400
```

**Implementation Steps:**

1. **PermisoDocumentoUsuarioServiceTest:**
   - Setup: Mockear `IPermisoDocumentoUsuarioRepository`, `PermisoDocumentoUsuarioValidator`, `UsuarioJpaRepository`, `ApplicationEventPublisher`
   - Test `crearOActualizarPermiso_nuevo_exitoso()`: Verificar que se llama validator, se guarda, se publica evento
   - Test `crearOActualizarPermiso_actualiza_existente()`: Verificar upsert behavior
   - Test `crearOActualizarPermiso_documentoNoExiste()`: Verificar lanzamiento de excepción
   - Test `crearOActualizarPermiso_usuarioOtraOrganizacion()`: Verificar 404 genérico
   - Test `revocarPermiso_exitoso()`: Verificar delete y evento
   - Test `revocarPermiso_noExiste()`: Verificar excepción
   - Test `listarPermisos_retorna_lista()`: Verificar query
   - Test `obtenerUsuariosResumen_mapa_correcto()`: Verificar mapeo

2. **PermisoDocumentoUsuarioValidatorTest:**
   - Test `validarAdministrador_admin()`: Debe pasar
   - Test `validarAdministrador_sinPermiso()`: Debe fallar
   - Test `validarDocumentoEnOrganizacion_perteneceOrg()`: Debe pasar
   - Test `validarDocumentoEnOrganizacion_noPertenece()`: Debe fallar
   - Test `validarNivelAccesoCodigo_valido()`: Debe retornar enum
   - Test `validarNivelAccesoCodigo_invalido()`: Debe fallar

3. **PermisoDocumentoUsuarioControllerTest:**
   - Setup: Mockear service, mapper, y simular requests HTTP
   - Test `crearPermiso_201_exitoso()`: Verificar status y body
   - Test `crearPermiso_200_actualiza()`: Verificar actualización
   - Test `crearPermiso_403_sinAutorizacion()`: Verificar error handling
   - Test `crearPermiso_404_documentoNoExiste()`: Verificar error handling
   - Test `revocarPermiso_204_exitoso()`: Verificar delete
   - Test `listarPermisos_200_retorna_lista()`: Verificar GET

**Dependencies:**
- JUnit 5 (`@Test`, `@DisplayName`)
- Mockito (`@Mock`, `@InjectMocks`, `when`, `verify`)
- AssertJ (`assertThat`, `assertThatThrownBy`)
- Spring Boot Test (`@WebMvcTest`, `MockMvc`) para controlador

**Implementation Notes:**
- Naming: `should_DoSomething_When_Condition` o `test_scenarioName_expected_result`
- Mock objetos externos, no testear comportamiento de frameworks
- Verificar que eventos se publican (mock `ApplicationEventPublisher` y verificar interacción)
- Coverage objetivo: >90% por línea y rama
- Usar `@DisplayName` con descripciones claras en español

---

### Step 10: Create Database Migration

**File to Create:**
- `src/main/resources/db/migration/VXXX__create_permiso_documento_usuario.sql` (si la tabla no existe)

**Action:** Script de migración Flyway (o validar que tabla existe).

**Implementation Steps:**

1. Verificar en `README-docker.md` si la tabla ya fue creada
2. Si no existe, crear script Flyway:
   ```sql
   CREATE TABLE permiso_documento_usuario (
       id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
       documento_id BIGINT NOT NULL,
       usuario_id BIGINT NOT NULL,
       organizacion_id BIGINT NOT NULL,
       nivel_acceso VARCHAR(20) NOT NULL,
       fecha_expiracion TIMESTAMP NULL,
       fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       CONSTRAINT uk_permiso_doc_user UNIQUE (documento_id, usuario_id),
       CONSTRAINT fk_permiso_doc FOREIGN KEY (documento_id) REFERENCES documento(id),
       CONSTRAINT fk_permiso_user FOREIGN KEY (usuario_id) REFERENCES usuario(id),
       CONSTRAINT fk_permiso_org FOREIGN KEY (organizacion_id) REFERENCES organizacion(id)
   );
   
   CREATE INDEX idx_permiso_documento ON permiso_documento_usuario(documento_id);
   CREATE INDEX idx_permiso_usuario ON permiso_documento_usuario(usuario_id);
   CREATE INDEX idx_permiso_organizacion ON permiso_documento_usuario(organizacion_id);
   ```
3. Si la tabla ya existe, validar que esquema coincida con `PermisoDocumentoUsuario.java`

**Dependencies:**
- Flyway (ya incluido en Spring Boot)

**Implementation Notes:**
- Version: incrementar número secuencial de migraciones existentes
- Índices: en `documento_id`, `usuario_id`, `organizacion_id` para queries eficientes
- Unique constraint: `(documento_id, usuario_id)` para evitar duplicados

---

### Step 11: Update API Documentation

**Files to Update:**
- `ai-specs/specs/api-spec.yml`

**Action:** Documentar nuevos endpoints en OpenAPI spec.

**Implementation Steps:**

1. Abrir `ai-specs/specs/api-spec.yml`
2. Añadir path `/documentos/{documentoId}/permisos` con operaciones:
   - `post`: Crear/actualizar permiso
   - `delete`: Revocar permiso
   - `get`: Listar permisos
3. Documentar request/response schemas, códigos de status, ejemplos
4. Referenciar DTOs como componentes reutilizables

**Implementation Notes:**
- Documentación en inglés (seguir `documentation-standards.md`)
- Incluir descripciones claras de validaciones y errores
- Ejemplos de request/response JSON
- Status codes: 201, 200, 204, 400, 403, 404

---

### Step 12: Update Data Model Documentation

**File to Update:**
- `ai-specs/specs/data-model.md`

**Action:** Documentar nueva tabla `permiso_documento_usuario` en diagrama y descripción.

**Implementation Steps:**

1. Revisar estructura actual de `data-model.md`
2. Añadir tabla `permiso_documento_usuario` con:
   - Columnas, tipos, constraints
   - Relaciones con `documento`, `usuario`, `organizacion`
   - Índices
3. Actualizar diagrama ER si existe (incluir nueva tabla)
4. Notas sobre herencia y multi-tenancy

**Implementation Notes:**
- Mantener consistencia de nombres (snake_case en BD, camelCase en código)
- Documentar que filtro Hibernate aplica automáticamente `organizacion_id = :tenantId`

---

### Step 13: Update Backend Standards

**File to Update:**
- `ai-specs/specs/backend-standards.md`

**Action:** Agregar referencias a patrones aplicados (si es necesario).

**Implementation Steps:**

1. Si se crean nuevas excepciones, documentarlas en sección de error handling
2. Si se usan nuevos validadores, mencionar patrón en sección de validación
3. Confirmar que implementación sigue estándares existentes
4. Actualizar ejemplos si aplica

**Implementation Notes:**
- La mayoría de estándares ya están cubiertos en el documento
- Solo añadir si se introduce nueva convención o patrón

---

## 4. Implementation Order

✅ Step 1: Create DTOs and Validators
✅ Step 2: Create Domain Event Classes
✅ Step 3: Create Application Service
✅ Step 4: Create Mapper
✅ Step 5: Create JPA Repository
✅ Step 6: Create Domain Repository Interface
✅ Step 7: Create Repository Adapter
✅ Step 8: Create REST Controller
✅ Step 9: Create Unit Tests
✅ Step 10: Create Integration Tests
✅ Step 11: Create Database Migration (si aplica)
✅ Step 12: Update API Documentation
✅ Step 13: Update Data Model Documentation
✅ Step 14: Update Backend Standards (si aplica)

---

## 5. Testing Checklist

### Unit Tests
- [ ] `PermisoDocumentoUsuarioService`: crear, actualizar, revocar, listar
- [ ] `PermisoDocumentoUsuarioValidator`: todas las validaciones
- [ ] `PermisoDocumentoUsuarioController`: mapeos y responses

### Manual Testing
- [ ] Crear permiso LECTURA vía POST (201)
- [ ] Actualizar a ESCRITURA vía PATCH (200)
- [ ] Revocar vía DELETE (204)
- [ ] Listar permisos de documento vía GET (200)
- [ ] Validar que usuario sin admin no puede asignar (403)
- [ ] Validar que usuario de otra org recibe 404 genérico

---

## 6. Error Response Format

**Standard Error Response (application/json):**

```json
{
  "error": "RESOURCE_NOT_FOUND",
  "message": "Documento no encontrado",
  "status": 404,
  "timestamp": "2026-02-03T10:30:00Z",
  "path": "/api/documentos/999/permisos"
}
```

**Status Code Mapping:**

| Caso | Status | Error |
|------|--------|-------|
| Crear permiso exitoso | 201 | N/A |
| Actualizar permiso exitoso | 200 | N/A |
| Revocar exitoso | 204 | N/A |
| Nivel inválido | 400 | `INVALID_NIVEL_ACCESO` |
| Sin autorización | 403 | `ACCESS_DENIED` |
| Documento/usuario no existe | 404 | `RESOURCE_NOT_FOUND` |
| Servidor interno | 500 | `INTERNAL_ERROR` |

---

## 7. Partial Update Support

**Comportamiento PATCH en `/documentos/{documentoId}/permisos/{usuarioId}`:**

- Actualizar `nivelAcceso`: requerido
- Actualizar `fechaExpiracion`: opcional (null lo mantiene vacío)
- Si ACL no existe, **crear** (upsert behavior)
- Publicar evento Updated (o Created si es nuevo)

---

## 8. Dependencies

**Backend - Maven (check `pom.xml`):**
- Spring Boot 3.x
- Spring Data JPA
- Hibernate (incluido)
- MapStruct (mapping DTOs)
- Lombok (reduce boilerplate)
- JUnit 5 (testing)
- Mockito (mocking)
- Jakarta/Java validation

**Existing Components:**
- `PermisoDocumentoUsuario` (entity)
- `PermisoCarpetaUsuarioService` (patrón a reutilizar)
- `CurrentTenantService` (multi-tenancy)
- `NivelAcceso` (enum)

---

## 9. Notes

### Business Rules Recap
1. Solo admin o usuarios con ADMINISTRACION en carpeta padre pueden asignar permisos.
2. Aislamiento multi-tenant: todos los checks deben validar `organizacionId`.
3. Upsert behavior: crear si no existe, actualizar si existe.
4. No confundir con precedencia carpeta vs documento (US-ACL-006).

### Security Considerations
- Mantener errores genéricos (404 incluso si documento existe pero es de otra org)
- No exponer `organizacionId` en errores
- Validar JWT y claims antes de procesar
- Log eventos sin datos sensibles

### Performance Notes
- Índices en `documento_id`, `usuario_id`, `organizacion_id`
- Unique constraint en `(documento_id, usuario_id)` evita duplicados
- Filtro Hibernate aplicado automáticamente (sin overhead si bien configurado)

### Architecture Notes
- Mantener separación Domain/Application/Infrastructure
- Reutilizar patrones de `PermisoCarpetaUsuario`
- Eventos de dominio para auditoría (no estado en BD)

---

## 10. Next Steps After Implementation

1. **Code Review:** Revisar PR en rama `feature/US-ACL-005-backend` con equipo
2. **Merge:** Una vez aprobado, merge a `develop`
3. **Integration Testing:** Verificar que endpoints funcionan con frontend mock
4. **Deployment:** Desplegar a ambiente de staging
5. **Frontend Implementation:** Equipo frontend implementa consumo de endpoints
6. **End-to-End Testing:** Flujo completo con UI

---

## 11. Implementation Verification Checklist

### Code Quality
- [ ] Sin warnings de compilación (Maven)
- [ ] ESLint/checkstyle pasan
- [ ] Nombres descriptivos en todas las clases y métodos
- [ ] Comments en lógica compleja (sin exceso)

### Functionality
- [ ] Crear permiso nuevo (201)
- [ ] Actualizar permiso existente (200)
- [ ] Revocar permiso (204)
- [ ] Listar permisos de documento (200)
- [ ] Errores retornan status correcto (400, 403, 404)

### Testing
- [ ] >90% coverage en unitarias
- [ ] Integración con contexto Spring
- [ ] Aislamiento multi-tenant validado
- [ ] Casos edge testeados (sin permisos, documento inexistente, etc.)

### Integration
- [ ] Eventos se publican correctamente
- [ ] Auditoría registra cambios
- [ ] No conflictos con herencia (US-ACL-006)

### Documentation
- [ ] `api-spec.yml` actualizado con nuevos endpoints
- [ ] `data-model.md` refleja tabla `permiso_documento_usuario`
- [ ] `backend-standards.md` referencias si aplica
- [ ] Swagger/OpenAPI funciona en endpoint `/swagger-ui.html`

