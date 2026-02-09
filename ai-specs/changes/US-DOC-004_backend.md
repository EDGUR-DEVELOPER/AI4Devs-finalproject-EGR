# Plan de Implementación Backend: US-DOC-004 Listar versiones (API) ordenadas

## Resumen General

Implementación de un endpoint REST para listar el historial completo de versiones de un documento, ordenadas ascendentemente por número secuencial. El endpoint incluye soporte para paginación opcional y validación de permisos de lectura. Sigue la arquitectura hexagonal del servicio `document-core` con las capas Domain, Application, Infrastructure y Presentation.

**Historia de Usuario**: Como usuario, quiero listar el historial de versiones de un documento para entender su evolución.

**Principios aplicados**: 
- Domain-Driven Design (DDD)
- Arquitectura Hexagonal (Ports & Adapters)
- Principios SOLID
- Clean Architecture

## Contexto de Arquitectura

### Capas Involucradas

**Capa de Dominio** (`domain/`)
- `model/Version.java` - Entidad de dominio (ya existe)
- `model/Documento.java` - Entidad de dominio (ya existe)
- `repository/VersionRepository.java` - Puerto de repositorio (ya existe, requiere extensión)

**Capa de Aplicación** (`application/`)
- `dto/VersionItemResponse.java` - DTO para item de versión (⚠️ crear)
- `dto/VersionListResponse.java` - DTO para lista de versiones (⚠️ crear)
- `service/DocumentoVersionService.java` - Servicio de aplicación (⚠️ crear)
- `mapper/VersionListMapper.java` - Mapper con MapStruct (⚠️ crear)

**Capa de Infraestructura** (`infrastructure/`)
- `adapter/controller/VersionController.java` - Controlador REST (⚠️ crear)
- `config/SecurityConfig.java` - Configuración de seguridad (revisar)

**Base de Datos**
- Índice compuesto: `idx_documento_version_doc_numero` (verificar/crear)

### Servicio: document-core

```
backend/document-core/
├── src/main/java/com/docflow/documentcore/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Version.java [✓ existe]
│   │   │   └── Documento.java [✓ existe]
│   │   ├── repository/
│   │   │   └── VersionRepository.java [✓ existe, extender]
│   │   ├── service/
│   │   │   └── IEvaluadorPermisos.java [✓ existe]
│   │   └── exception/
│   │       ├── ResourceNotFoundException.java [✓ existe]
│   │       └── AccessDeniedException.java [✓ existe]
│   ├── application/
│   │   ├── dto/
│   │   │   ├── VersionItemResponse.java [⚠️ crear]
│   │   │   └── VersionListResponse.java [⚠️ crear]
│   │   ├── service/
│   │   │   └── DocumentoVersionService.java [⚠️ crear]
│   │   └── mapper/
│   │       └── VersionListMapper.java [⚠️ crear]
│   └── infrastructure/
│       └── adapter/
│           └── controller/
│               └── VersionController.java [⚠️ crear]
└── src/test/java/com/docflow/documentcore/
    ├── application/service/
    │   └── DocumentoVersionServiceTest.java [⚠️ crear]
    └── infrastructure/adapter/controller/
        └── VersionControllerIT.java [⚠️ crear]
```

## Pasos de Implementación

### Paso 1: Crear DTOs de Respuesta

#### Paso 1.1: Crear DTO VersionItemResponse

- **Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/VersionItemResponse.java`
- **Acción**: Crear DTO para elemento individual de versión en la lista
- **Pasos de Implementación**:
  1. Crear el paquete `application/dto` si no existe
  2. Crear la clase `VersionItemResponse` con anotaciones de Lombok:
     - `@Data` para getters/setters
     - `@Builder` para patrón builder
     - `@NoArgsConstructor` y `@AllArgsConstructor` para constructores
  3. Agregar campos:
     - `Long id` - ID único de versión
     - `Integer numeroSecuencial` - Número de versión secuencial (1, 2, 3...)
     - `Long tamanioBytes` - Tamaño del archivo en bytes
     - `String hashContenido` - Hash SHA256 del contenido
     - `String comentarioCambio` - Comentario de cambio (nullable)
     - `CreadorInfo creadoPor` - Información del creador (DTO anidado)
     - `OffsetDateTime fechaCreacion` - Marca de tiempo de creación
     - `Integer descargas` - Contador de descargas
     - `OffsetDateTime ultimaDescargaEn` - Última marca de tiempo de descarga (nullable)
     - `Boolean esVersionActual` - Flag indicando si esta es la versión actual
  4. Crear clase estática anidada `CreadorInfo`:
     - `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
     - Campos: `Long id`, `String nombreCompleto`, `String email`
  5. Agregar comentarios Javadoc describiendo el propósito del DTO y significado de los campos
- **Dependencias**:
  ```java
  import lombok.AllArgsConstructor;
  import lombok.Builder;
  import lombok.Data;
  import lombok.NoArgsConstructor;
  import java.time.OffsetDateTime;
  ```
- **Notas de Implementación**:
  - Usar `OffsetDateTime` para marcas de tiempo con zona horaria
  - `esVersionActual` se calcula comparando Version.id con Documento.versionActualId
  - `CreadorInfo` inicialmente retornará solo el ID de usuario; nombre completo y email requieren integración con servicio de identidad (se puede mejorar después)
- **Ejemplo de Estructura**:
  ```java
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class VersionItemResponse {
      private Long id;
      private Integer numeroSecuencial;
      // ... otros campos
      private Boolean esVersionActual;
      
      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class CreadorInfo {
          private Long id;
          private String nombreCompleto;
          private String email;
      }
  }
  ```

#### Paso 1.2: Crear DTO VersionListResponse

- **Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/VersionListResponse.java`
- **Acción**: Crear DTO para respuesta de lista de versiones con soporte de paginación
- **Pasos de Implementación**:
  1. Crear la clase `VersionListResponse` con anotaciones de Lombok
  2. Agregar campos:
     - `List<VersionItemResponse> versiones` - Lista de versiones (ordenadas ascendentemente)
     - `Long documentoId` - ID del documento consultado
     - `Integer totalVersiones` - Recuento total de versiones (independiente de la paginación)
     - `PaginacionInfo paginacion` - Metadatos de paginación (null si no hay paginación)
  3. Crear clase estática anidada `PaginacionInfo`:
     - Campos: 
       - `Integer paginaActual` - Número de página actual (base 1)
       - `Integer tamanio` - Tamaño de página
       - `Integer totalPaginas` - Total de páginas disponibles
       - `Integer totalElementos` - Total de elementos/versiones
       - `Boolean primeraPagina` - True si es la primera página
       - `Boolean ultimaPagina` - True si es la última página
  4. Agregar Javadoc explicando el comportamiento de paginación
- **Dependencias**:
  ```java
  import java.util.List;
  import lombok.AllArgsConstructor;
  import lombok.Builder;
  import lombok.Data;
  import lombok.NoArgsConstructor;
  ```
- **Notas de Implementación**:
  - El campo `paginacion` debe ser null cuando no se proporcionan parámetros de paginación
  - Cuando se usa paginación, `versiones` contiene solo los elementos de la página solicitada
  - `totalVersiones` siempre representa el recuento completo, independientemente de la paginación

---

### Paso 2: Crear Mapper de Lista de Versiones

- **Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/application/mapper/VersionListMapper.java`
- **Acción**: Crear mapper de MapStruct para convertir entidades Version a DTOs VersionItemResponse
- **Pasos de Implementación**:
  1. Crear la interfaz `VersionListMapper` anotada con:
     - `@Mapper(componentModel = "spring")` para integración con Spring
     - `@Component` para registro de bean de Spring
  2. Agregar método `toItemResponseList`:
     - Parámetros: `List<Version> versiones`, `Long versionActualId`
     - Retorna: `List<VersionItemResponse>`
     - Implementación por defecto itera las versiones y llama a `toItemResponse` para cada una
  3. Agregar método `toItemResponse`:
     - Parámetros: `Version version`, `Long versionActualId`
     - Retorna: `VersionItemResponse`
     - Usar anotaciones `@Mapping` de MapStruct:
       - `@Mapping(target = "esVersionActual", expression = "java(version.getId().equals(versionActualId))")`
       - `@Mapping(target = "creadoPor", source = "version", qualifiedByName = "mapCreadorInfo")`
  4. Agregar método `mapCreadorInfo`:
     - Anotado con `@Named("mapCreadorInfo")`
     - Parámetros: `Version version`
     - Retorna: `VersionItemResponse.CreadorInfo`
     - Implementación:
       - Extraer ID de `creadoPor` de la versión
       - Para MVP: retornar CreadorInfo con solo el ID poblado
       - Agregar comentario TODO para integración futura con servicio de identidad
       - Valores temporales de marcador de posición para `nombreCompleto` y `email`
  5. Agregar Javadoc explicando el propósito del mapper y mejoras futuras
- **Dependencias**:
  ```java
  import com.docflow.documentcore.domain.model.Version;
  import org.mapstruct.*;
  import org.springframework.stereotype.Component;
  import java.util.List;
  ```
- **Notas de Implementación**:
  - MapStruct generará la implementación en tiempo de compilación
  - El flag `esVersionActual` se calcula dinámicamente comparando IDs
  - La integración de información del creador con el servicio de identidad está fuera del alcance de esta US (mejora futura)
  - Por ahora, usar valores de marcador de posición para nombre/email del creador basados en ID
- **Mejoras Futuras**:
  - Opción A: Inyectar UserRepository o UserService para obtener detalles del usuario
  - Opción B: Agregar join en el query de VersionRepository para traer datos del usuario
  - Opción C: Usar proyección `@EntityGraph` para carga eager
  - Recomendado: Agregar un método en el servicio de aplicación para enriquecer la información del creador después del mapeo inicial

---

### Paso 3: Crear/Extender Repositorio de Versiones

- **Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/domain/repository/VersionRepository.java`
- **Acción**: Agregar firma de método de consulta con soporte Pageable para paginación
- **Pasos de Implementación**:
  1. Abrir la interfaz `VersionRepository` existente
  2. Verificar la existencia del método:
     ```java
     List<Version> findByDocumentoIdOrderByNumeroSecuencialAsc(Long documentoId);
     ```
  3. Agregar nueva firma de método con soporte Pageable:
     ```java
     @Query("SELECT v FROM Version v WHERE v.documentoId = :documentoId ORDER BY v.numeroSecuencial ASC")
     Page<Version> findByDocumentoIdOrderByNumeroSecuencialAsc(
         @Param("documentoId") Long documentoId, 
         Pageable pageable
     );
     ```
  4. Agregar Javadoc explicando ambas variantes del método
- **Dependencias**:
  ```java
  import org.springframework.data.domain.Page;
  import org.springframework.data.domain.Pageable;
  import org.springframework.data.jpa.repository.Query;
  import org.springframework.data.repository.query.Param;
  ```
- **Notas de Implementación**:
  - Spring Data JPA auto-implementará la variante con Pageable
  - La consulta usa anotación `@Query` explícita para asegurar ordenamiento correcto
  - Mantener la versión sin Pageable para casos donde se necesitan todas las versiones
  - El optimizador de consultas usará el índice `idx_documento_version_doc_numero`
- **Verificación de Índice de Base de Datos**:
  - El índice `idx_documento_version_doc_numero (documento_id, numero_secuencial)` debe existir
  - Este índice permite filtrado y ordenamiento eficientes sin escaneo completo de tabla
  - Será verificado en el Paso 8

---

### Paso 4: Crear Servicio de Aplicación

- **Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/application/service/DocumentoVersionService.java`
- **Acción**: Crear servicio con lógica de negocio para listar versiones con validación de permisos
- **Pasos de Implementación**:
  1. Crear la clase `DocumentoVersionService` anotada con:
     - `@Service` para bean de servicio de Spring
     - `@Slf4j` para logging
     - `@RequiredArgsConstructor` para inyección por constructor
  2. Inyectar dependencias vía constructor:
     - `DocumentoRepository documentoRepository`
     - `VersionRepository versionRepository`
     - `IEvaluadorPermisos evaluadorPermisos`
     - `VersionListMapper versionListMapper`
  3. Crear método `listarVersiones`:
     - Parámetros:
       - `Long documentoId` - ID del documento a consultar
       - `Long usuarioId` - ID del usuario solicitante (desde header X-User-Id)
       - `Long organizacionId` - ID de organización/tenant (desde header X-Organization-Id)
       - `Integer pagina` - Número de página opcional (base 1, null = sin paginación)
       - `Integer tamanio` - Tamaño de página opcional (default 20, max 100)
     - Retorna: `VersionListResponse`
     - Anotar con `@Transactional(readOnly = true)` para transacción de solo lectura
  4. Implementar lógica de negocio:
     - **Paso 4.1**: Validar que el documento existe y pertenece al tenant
       - Consulta: `documentoRepository.findById(documentoId)`
       - Si no se encuentra: lanzar `ResourceNotFoundException("Documento", documentoId)`
       - Si `documento.organizacionId != organizacionId`: lanzar `ResourceNotFoundException`
     - **Paso 4.2**: Validar que el usuario tiene permiso de LECTURA
       - Llamar: `evaluadorPermisos.tienePermisoLectura(usuarioId, documentoId, organizacionId)`
       - Si es false: registrar advertencia y lanzar `AccessDeniedException("No tiene permiso para ver las versiones de este documento")`
     - **Paso 4.3**: Consultar versiones (con o sin paginación)
       - Si `pagina != null && pagina > 0`:
         - Crear PageRequest: `PageRequest.of(pagina - 1, tamanio, Sort.by(Sort.Direction.ASC, "numeroSecuencial"))`
         - Consultar: `versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(documentoId, pageRequest)`
         - Extraer: `versiones = page.getContent()`
         - Construir PaginacionInfo desde metadatos de página
       - Si no (sin paginación):
         - Consultar: `versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(documentoId)`
         - Establecer `paginacionInfo = null`
     - **Paso 4.4**: Mapear a DTOs
       - Llamar: `versionListMapper.toItemResponseList(versiones, documento.getVersionActualId())`
     - **Paso 4.5**: Construir y retornar respuesta
       - Crear `VersionListResponse` con todos los campos poblados
  5. Agregar logging comprensivo:
     - Log de entrada: "Listando versiones del documento {} para usuario {} en organización {}"
     - Log de advertencia al denegar permiso
  6. Agregar Javadoc con descripciones de parámetros, valor de retorno y excepciones lanzadas
- **Dependencias**:
  ```java
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.data.domain.Page;
  import org.springframework.data.domain.PageRequest;
  import org.springframework.data.domain.Pageable;
  import org.springframework.data.domain.Sort;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;
  ```
- **Notas de Implementación**:
  - El aislamiento multi-tenancy se aplica verificando `organizacionId`
  - La validación de permisos ocurre ANTES de consultar versiones (fail-fast)
  - La paginación usa indexación base 1 en API pero base 0 en Spring (convertir con `pagina - 1`)
  - El tamaño de página por defecto es 20 si no se especifica
  - La transacción es de solo lectura para optimización de rendimiento
- **Escenarios de Error**:
  - Documento no encontrado → `ResourceNotFoundException`
  - Documento de diferente tenant → `ResourceNotFoundException` (no revelar existencia)
  - Sin permiso de lectura → `AccessDeniedException`
  - Parámetros de paginación inválidos → Manejados por validación del controlador

---

### Paso 5: Crear Controlador REST

- **Archivo**: `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/VersionController.java`
- **Acción**: Crear controlador REST que expone el endpoint de listado de versiones
- **Pasos de Implementación**:
  1. Crear clase `VersionController` anotada con:
     - `@RestController` para endpoint REST
     - `@RequestMapping("/api/documentos")` para path base
     - `@RequiredArgsConstructor` para inyección por constructor
     - `@Slf4j` para logging
     - `@Validated` para validación de parámetros
     - `@Tag(name = "Versiones", description = "Gestión de versiones de documentos")` para OpenAPI
  2. Inyectar dependencia:
     - `DocumentoVersionService documentoVersionService`
  3. Crear método `listarVersiones`:
     - Método HTTP: GET
     - Path: `/{documentoId}/versiones`
     - Parámetros:
       - `@PathVariable Long documentoId` - ID del documento
       - `@RequestHeader("X-User-Id") Long usuarioId` - ID de usuario desde gateway
       - `@RequestHeader("X-Organization-Id") Long organizacionId` - ID de organización desde gateway
       - `@RequestParam(required = false) @Min(1) Integer pagina` - Número de página opcional
       - `@RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer tamanio` - Tamaño de página opcional
     - Retorna: `ResponseEntity<VersionListResponse>`
  4. Agregar anotaciones OpenAPI:
     - `@Operation(summary = "Listar versiones de documento", description = "...")`
     - `@ApiResponses`:
       - 200: Éxito
       - 400: Parámetros de paginación inválidos
       - 401: No autorizado
       - 403: Acceso denegado
       - 404: Documento no encontrado
       - 500: Error interno del servidor
     - Anotaciones `@Parameter` para cada parámetro
  5. Implementar lógica del método:
     - Registrar petición entrante con todos los parámetros
     - Llamar: `documentoVersionService.listarVersiones(documentoId, usuarioId, organizacionId, pagina, tamanio)`
     - Retornar: `ResponseEntity.ok(response)`
  6. Agregar Javadoc
- **Dependencias**:
  ```java
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.Parameter;
  import io.swagger.v3.oas.annotations.responses.ApiResponse;
  import io.swagger.v3.oas.annotations.responses.ApiResponses;
  import io.swagger.v3.oas.annotations.tags.Tag;
  import jakarta.validation.constraints.Max;
  import jakarta.validation.constraints.Min;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.http.ResponseEntity;
  import org.springframework.validation.annotation.Validated;
  import org.springframework.web.bind.annotation.*;
  ```
- **Notas de Implementac\ión**:
  - Headers `X-User-Id` and `X-Organization-Id` are injected by the Gateway (verified by security config)
  - Validation constraints are automatically enforced:
    - `pagina >= 1` if provided
    - `tamanio` between 1 and 100 (default 20)
  - Exception handling is done by global exception handler (already exists)
  - CORS is configured at Gateway level (not in this controller)
- **API Contract**:
  - Endpoint: `GET /api/documentos/{documentoId}/versiones`
  - Query params: `?pagina=2&tamanio=10` (optional)
  - Response 200: `VersionListResponse` JSON
  - Error responses follow standard error format

---

### Paso 6: Escribir Pruebas Unitarias para el Servicio

- **Archivo**: `backend/document-core/src/test/java/com/docflow/documentcore/application/service/DocumentoVersionServiceTest.java`
- **Acción**: Write comprehensive unit tests for `DocumentoVersionService`
- **Pasos de Implementación**:
  1. Crear clase de prueba con anotaciones:
     - `@ExtendWith(MockitoExtension.class)` for Mockito support
     - `@DisplayName("DocumentoVersionService - Listar Versiones")`
  2. Configurar mocks y sujeto de prueba:
     - `@Mock DocumentoRepository documentoRepository`
     - `@Mock VersionRepository versionRepository`
     - `@Mock IEvaluadorPermisos evaluadorPermisos`
     - `@Mock VersionListMapper versionListMapper`
     - `@InjectMocks DocumentoVersionService service`
  3. Configurar datos de prueba en `@BeforeEach`:
     - Create test `Documento` with:
       - `id = 42L`
       - `organizacionId = 10L`
       - `versionActualId = 102L`
       - `numeroVersiones = 2`
     - Create test `Version` list (version 1 and version 2)
  4. Escribir casos de prueba:

#### Test 6.1: Casos Exitosos
- **Test**: `should_ListVersions_When_UserHasPermission`
  - Mock: Document exists, user has permission, versions found
  - Assert: Response contains correct documentoId, totalVersiones, no pagination
  - Verify: All repository and service methods called correctly
  - Verify: Mapper called with versions and versionActualId

- **Test**: `should_ReturnOrderedVersions_When_MultipleVersionsExist`
  - Mock: Document with 3+ versions
  - Assert: Versions are in ascending order by numeroSecuencial

#### Test 6.2: Errores de No Encontrado
- **Test**: `should_ThrowNotFoundException_When_DocumentNotExists`
  - Mock: `documentoRepository.findById()` returns empty
  - Assert: `ResourceNotFoundException` thrown
  - Verify: No interaction with versionRepository

- **Test**: `should_ThrowNotFoundException_When_DocumentNotInTenant`
  - Mock: Document with different organizacionId
  - Assert: `ResourceNotFoundException` thrown
  - Verify: Multi-tenancy isolation works

#### Test 6.3: Errores de Permisos
- **Test**: `should_ThrowAccessDenied_When_UserHasNoPermission`
  - Mock: Document exists but `evaluadorPermisos.tienePermisoLectura()` returns false
  - Assert: `AccessDeniedException` thrown
  - Verify: No interaction with versionRepository

#### Test 6.4: Pruebas de Paginación
- **Test**: `should_ReturnPaginationInfo_When_PaginationParametersProvided`
  - Mock: `versionRepository` returns `Page<Version>` with metadata
  - Assert: Response includes paginacion with correct values
  - Assert: `paginaActual`, `totalPaginas`, `primeraPagina`, `ultimaPagina` are correct

- **Test**: `should_ReturnAllVersions_When_NoPaginationProvided`
  - Mock: Call without pagina parameter
  - Assert: Response.paginacion is null
  - Verify: Non-pageable repository method called

#### Test 6.5: Casos Lìmite
- **Test**: `should_ReturnEmptyList_When_DocumentHasNoVersions`
  - Mock: Document exists but no versions found
  - Assert: Response with empty versiones list
  - Assert: totalVersiones = 0

  5. Usar AssertJ para aserciones fluidas:
     ```java
     assertThat(response).isNotNull();
     assertThat(response.getDocumentoId()).isEqualTo(42L);
     ```
  6. Usar verificación de Mockito:
     ```java
     verify(documentoRepository).findById(documentoId);
     verify(evaluadorPermisos).tienePermisoLectura(usuarioId, documentoId, organizacionId);
     ```
  7. Seguir convención de nombres: `should_[ExpectedBehavior]_When_[Condition]`

- **Dependencias**:
  ```java
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.InjectMocks;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import static org.assertj.core.api.Assertions.*;
  import static org.mockito.ArgumentMatchers.*;
  import static org.mockito.Mockito.*;
  ```

- **Notas de Implementación**:
  - Aim for 100% code coverage of service class
  - Test all success paths and all error paths
  - Verify correct exception types and messages
  - Verify correct interactions between components
  - Use descriptive test names and DisplayName annotations

---

### Paso 7: Escribir Pruebas de Integración para el Controlador

- **Archivo**: `backend/document-core/src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/VersionControllerIT.java`
- **Acción**: Write integration tests for the REST endpoint
- **Pasos de Implementación**:
  1. Crear clase de prueba con anotaciones:
     - `@SpringBootTest` for full Spring context
     - `@AutoConfigureMockMvc` for MockMvc support
     - `@ActiveProfiles("test")` for test profile
     - `@Transactional` for test isolation
     - `@DisplayName("VersionController - Integration Tests")`
  2. Inyectar dependencias:
     - `@Autowired MockMvc mockMvc`
     - `@Autowired DocumentoRepository documentoRepository`
     - `@Autowired VersionRepository versionRepository`
  3. Configurar datos de prueba en `@BeforeEach`:
     - Crear y persistir Documento
     - Crear y persistir Version entities (version 1, version 2)
     - Update documento.versionActualId
  4. Escribir casos de prueba:

#### Test 7.1: Peticiones Exitosas
- **Test**: `should_Return200WithOrderedVersions_When_ValidRequest`
  - Petición: GET `/api/documentos/{id}/versiones` con headers válidos
  - Afirmar HTTP 200
  - Assert JSON response structure matches VersionListResponse
  - Assert versiones array has 2 items
  - Assert versiones[0].numeroSecuencial = 1, esVersionActual = false
  - Assert versiones[1].numeroSecuencial = 2, esVersionActual = true
  - Assert paginacion does not exist (no pagination)

- **Test**: `should_ReturnPaginatedResults_When_PaginationParametersProvided`
  - Petición: GET with `?pagina=1&tamanio=1`
  - Afirmar HTTP 200
  - Assert versiones array has 1 item
  - Assert paginacion exists with correct values:
    - paginaActual = 1
    - tamanio = 1
    - totalPaginas = 2
    - primeraPagina = true
    - ultimaPagina = false

#### Test 7.2: Errores de Validación
- **Test**: `should_Return400_When_PageSizeExceedsLimit`
  - Petición: GET with `?tamanio=200`
  - Afirmar HTTP 400
  - Assert error JSON contains validation message about max size 100

- **Test**: `should_Return400_When_PageNumberIsZero`
  - Petición: GET with `?pagina=0`
  - Afirmar HTTP 400
  - Assert error contains "página debe ser mayor o igual a 1"

#### Test 7.3: Errores de No Encontrado
- **Test**: `should_Return404_When_DocumentNotFound`
  - Petición: GET con  no existente documentoId (99999)
  - Afirmar HTTP 404
  - Assert error.error = "DOCUMENT_NOT_FOUND"

#### Test 7.4: Errores de Permisos
- **Test**: `should_Return403_When_NoPermission`
  - Petición: GET with user ID that doesn't have permission
  - Afirmar HTTP 403
  - Assert error.error = "ACCESS_DENIED"
  - Nota: Esto requiere mockear or setting up test data for IEvaluadorPermisos

  5. Usar constructores de petición Spring MockMvc:
     ```java
     mockMvc.perform(get("/api/documentos/{id}/versiones", documento.getId())
             .header("X-User-Id", "5")
             .header("X-Organization-Id", "10")
             .contentType(MediaType.APPLICATION_JSON))
         .andExpect(status().isOk())
         .andExpect(jsonPath("$.documentoId").value(documento.getId()))
         .andExpect(jsonPath("$.versiones", hasSize(2)));
     ```
  6. Use Hamcrest matchers for JSON assertions:
     - `hasSize()`, `is()`, `containsString()`
  7. Clean up test data automatically with `@Transactional` rollback

- **Dependencias**:
  ```java
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.http.MediaType;
  import org.springframework.test.context.ActiveProfiles;
  import org.springframework.test.web.servlet.MockMvc;
  import org.springframework.transaction.annotation.Transactional;
  import static org.hamcrest.Matchers.*;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
  ```

- **Notas de Implementac\ión**:
  - Integration tests verify end-to-end behavior through HTTP layer
  - Use real database (test profile with H2 or test Postgres)
  - Test transaction rollback ensures test isolation
  - Verify actual JSON response structure
  - Test realistic scenarios including edge cases

---

### Paso 8: Verify and Create Database Index

- **Acción**: Verify existence of optimized database index and create if missing
- **Pasos de Implementación**:
  1. Connect to the PostgreSQL database (test or dev environment)
  2. Check if index exists:
     ```sql
     SELECT indexname, indexdef 
     FROM pg_indexes 
     WHERE tablename = 'documento_version' 
       AND indexname = 'idx_documento_version_doc_numero';
     ```
  3. If index doesn't exist, create it:
     ```sql
     CREATE INDEX IF NOT EXISTS idx_documento_version_doc_numero 
     ON documento_version (documento_id, numero_secuencial);
     ```
  4. Verify constraint exists:
     ```sql
     SELECT constraint_name, constraint_type
     FROM information_schema.table_constraints
     WHERE table_name = 'documento_version'
       AND constraint_name = 'uk_documento_version_documento_numero';
     ```
  5. If constraint doesn't exist, create it:
     ```sql
     ALTER TABLE documento_version 
     ADD CONSTRAINT uk_documento_version_documento_numero 
     UNIQUE (documento_id, numero_secuencial);
     ```
  6. Test query performance with EXPLAIN ANALYZE:
     ```sql
     EXPLAIN ANALYZE
     SELECT v.*
     FROM documento_version v
     WHERE v.documento_id = 42
     ORDER BY v.numero_secuencial ASC;
     ```
  7. Verify execution plan uses the index:
     - Look for "Index Scan using idx_documento_version_doc_numero"
     - Cost should be low (< 10 for small datasets)
  8. Document index in migration file (if using Flyway/Liquibase):
     - Create migration: `VXX__add_version_index.sql`
     - Include both index and constraint creation

- **Notas de Implementac\ión**:
  - Composite index (documento_id, numero_secuencial) enables:
    - Fast filtering by documento_id
    - Efficient ordering by numero_secuencial without additional sort
  - Index should already exist from US-DOC-003, verify rather than create
  - Run EXPLAIN ANALYZE in test environment before deploying to production
  - Expected performance: < 10ms for 50 versions, < 100ms for 500 versions

- **Migration File Example** (if needed):
  ```sql
  -- VXX__optimize_version_queries.sql
  
  -- Create composite index for efficient version listing
  CREATE INDEX IF NOT EXISTS idx_documento_version_doc_numero 
  ON documento_version (documento_id, numero_secuencial);
  
  -- Ensure uniqueness constraint exists
  ALTER TABLE documento_version 
  ADD CONSTRAINT uk_documento_version_documento_numero 
  UNIQUE (documento_id, numero_secuencial)
  ON CONFLICT DO NOTHING;
  
  -- Add comment explaining index purpose
  COMMENT ON INDEX idx_documento_version_doc_numero IS 
  'Composite index for efficient version listing and ordering (US-DOC-004)';
  ```

---

### Paso 9: Actualizar Documentación Técnica

- **Acción**: Review and Actualizar Documentación Técnica according to changes made
- **Pasos de Implementación**:

  1. **Review Changes**: Analyze all code changes made during implementation:
     - New DTOs: VersionItemResponse, VersionListResponse
     - New service: DocumentoVersionService
     - New mapper: VersionListMapper
     - New controller: VersionController
     - Extended repository: VersionRepository (new Pageable method)
     - Database: Verified/created index

  2. **Identify Documentation Files to Update**:
     - `ai-specs/specs/api-spec.yml` - New endpoint specification
     - `backend/document-core/README.md` - Service capabilities
     - `ai-specs/specs/data-model.md` - Index documentation if applicable

  3. **Update API Specification** (`ai-specs/specs/api-spec.yml`):
     - Add endpoint definition:
       ```yaml
       /api/documentos/{documentoId}/versiones:
         get:
           summary: Listar versiones de un documento
           description: |
             Obtiene el historial completo de versiones ordenadas ascendentemente.
             Soporta paginación opcional. Requiere permiso LECTURA.
           tags:
             - Versiones
           security:
             - bearerAuth: []
           parameters:
             - name: documentoId (path parameter, required)
             - name: X-User-Id (header, required)
             - name: X-Organization-Id (header, required)
             - name: pagina (query, optional, min 1)
             - name: tamanio (query, optional, min 1, max 100, default 20)
           responses:
             200: Success with VersionListResponse schema
             400: Invalid pagination parameters
             401: Unauthorized
             403: Access denied
             404: Document not found
             500: Internal server error
       ```
     - Add schema definitions:
       - `VersionListResponse`
       - `VersionItemResponse`
       - `CreadorInfo`
       - `PaginacionInfo`
     - Add example responses for each status code

  4. **Update Service README** (`backend/document-core/README.md`):
     - Add to "Available Endpoints" section:
       ```markdown
       ### Version Management
       - `GET /api/documentos/{id}/versiones` - List document versions with optional pagination
       ```
     - Update "Features" section to mention version history listing
     - Add note about pagination support (1-100 items per page)

  5. **Update Data Model Documentation** (if applicable):
     - Document the composite index `idx_documento_version_doc_numero`
     - Explain its purpose for query optimization
     - Include performance expectations

  6. **Verify Documentation Consistency**:
     - All endpoint parameters match implementation
     - Response schemas match actual DTOs
     - Error codes and messages are accurate
     - Examples are realistic and helpful
     - All documentation is in English (as per standards)

  7. **Git Commit Documentation Changes**:
     - Stage documentation files: `git add ai-specs/specs/api-spec.yml backend/document-core/README.md`
     - Commit: `git commit -m "docs(US-DOC-004): update API spec and README for version listing endpoint"`

- **References**: 
  - Follow process described in `ai-specs/specs/documentation-standards.mdc`
  - All documentation must be written in English

- **Notas de Implementac\ión**:
  - Documentation updates must be included in the same PR as code changes
  - API specification should be updated before code review
  - Include all new schemas and examples in OpenAPI spec
  - Ensure Swagger UI will render correctly when service runs
  - Documentation is considered part of the implementation, not optional

---

## Orden de Implementaci\u00f3n

Execute steps in the following sequence:

2. **Paso 1**: Crear DTOs de Respuesta (VersionItemResponse, VersionListResponse)
3. **Paso 2**: Crear Mapper de Lista de Versiones
4. **Paso 3**: Extender Repositorio de Versiones (agregar método Pageable)
5. **Paso 4**: Crear Servicio de Aplicación (DocumentoVersionService)
6. **Paso 5**: Crear Controlador REST (VersionController)
7. **Paso 6**: Escribir Pruebas Unitarias para el Servicio
8. **Paso 7**: Escribir Pruebas de Integración para el Controlador
9. **Paso 8**: Verificar/Crear Índice de Base de Datos
10. **Paso 9**: Actualizar Documentación Técnica

**Importante**: 
- Ejecutar pruebas después de cada paso importante para asegurar progreso incremental
- Hacer commits del código frecuentemente con mensajes de commit significativos
- No proceder al siguiente paso si las pruebas actuales están fallando

---

## Lista de Verificaci\u00f3n de Pruebas

After completing implementation, verify:

### Pruebas Unitarias
- [ ] Las pruebas del servicio cubren todos los escenarios exitosos
- [ ] Las pruebas del servicio cubren todos los escenarios de error (404, 403)
- [ ] Las pruebas del servicio verifican la validación de permisos
- [ ] Las pruebas del servicio verifican el aislamiento multi-tenancy
- [ ] Las pruebas del servicio verifican la lógica de paginación
- [ ] Las pruebas del mapper verifican el cálculo del flag esVersionActual
- [ ] Todas las pruebas unitarias pasan: `mvn test`
- [ ] Cobertura de pruebas ≥ 90% para las clases nuevas

### Pruebas de Integración
- [ ] La prueba del controlador retorna 200 con versiones ordenadas
- [ ] La prueba del controlador retorna estructura JSON correcta
- [ ] La prueba del controlador valida parámetros de paginación
- [ ] La prueba del controlador retorna 400 para parámetros inválidos
- [ ] La prueba del controlador retorna 404 para documento no existente
- [ ] La prueba del controlador retorna 403 para acceso no autorizado
- [ ] Todas las pruebas de integración pasan: `mvn verify`

### Pruebas Manuales
- [ ] Endpoint accesible en `http://localhost:8082/api/documentos/{id}/versiones`
- [ ] La respuesta incluye todos los campos esperados
- [ ] Versiones ordenadas ascendentemente por numeroSecuencial
- [ ] El flag esVersionActual es correcto
- [ ] La paginación funciona correctamente con parámetros de consulta
- [ ] Las respuestas de error siguen el formato estándar
- [ ] Swagger UI muestra el endpoint correctamente
- [ ] Rendimiento: La consulta se ejecuta en < 100ms para 50 versiones

### Base de Datos
- [ ] El índice `idx_documento_version_doc_numero` existe
- [ ] EXPLAIN ANALYZE muestra uso del índice
- [ ] La restricción `uk_documento_version_documento_numero` existe
- [ ] No hay problemas de consultas N+1

### Documentación
- [ ] Especificación OpenAPI actualizada con definición del endpoint
- [ ] Todos los esquemas definidos correctamente
- [ ] Respuestas de ejemplo incluidas
- [ ] README actualizado con el nuevo endpoint
- [ ] Javadoc completo para todos los métodos públicos

---

## Formato de Respuesta de Error

All error responses follow the standard format already established in the project:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message in Spanish",
  "timestamp": "2026-02-09T14:30:00Z",
  "path": "/api/documentos/42/versiones"
}
```

### HTTP Status Code Mapping

| Code | Error Type | Error Code | Example Message |
|------|------------|------------|-----------------|
| 400 | Bad Request | `INVALID_PAGINATION` | "El tamaño de página debe estar entre 1 y 100" |
| 400 | Bad Request | `INVALID_PARAMETER` | "La página debe ser mayor o igual a 1" |
| 401 | Unauthorized | `UNAUTHORIZED` | "Token de autenticación requerido" |
| 403 | Forbidden | `ACCESS_DENIED` | "No tiene permiso para ver las versiones de este documento" |
| 404 | Not Found | `DOCUMENT_NOT_FOUND` | "Documento con ID 42 no encontrado" |
| 500 | Internal Error | `INTERNAL_ERROR` | "Error al procesar la solicitud" |

**Implementation Notes**:
- Error responses are handled by the global exception handler (already exists)
- Validation errors from `@Min`, `@Max` are automatically converted to 400 responses
- Don't reveal whether document exists if user lacks permission (always return 404)
- Log errors with sufficient context for debugging but don't expose internal details to client

---

## Comportamiento de Paginaci\u00f3n

### Without Pagination Parameters

**Request**: `GET /api/documentos/42/versiones`

**Response**:
```json
{
  "versiones": [ /* all versions */ ],
  "documentoId": 42,
  "totalVersiones": 50,
  "paginacion": null
}
```

### With Pagination Parameters

**Request**: `GET /api/documentos/42/versiones?pagina=2&tamanio=10`

**Response**:
```json
{
  "versiones": [ /* versions 11-20 */ ],
  "documentoId": 42,
  "totalVersiones": 50,
  "paginacion": {
    "paginaActual": 2,
    "tamanio": 10,
    "totalPaginas": 5,
    "totalElementos": 50,
    "primeraPagina": false,
    "ultimaPagina": false
  }
}
```

**Pagination Rules**:
- API uses 1-based page numbering (first page = 1)
- Spring Data uses 0-based indexing (convert: `pagina - 1`)
- Default page size: 20 if tamanio not specified
- Maximum page size: 100 (validated by `@Max`)
- Minimum page size: 1 (validated by `@Min`)
- If page number exceeds total pages, return empty array (not an error)

---

## Dependencias

### Maven Dependencies (Already Present)

Verify these dependencies exist in `backend/document-core/pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Boot Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- Spring Boot Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>
    
    <!-- SpringDoc OpenAPI (Swagger) -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.7.0</version>
    </dependency>
    
    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Test Dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Complementos de Build (Verificar)

```xml
<build>
    <plugins>
        <!-- Spring Boot Maven Plugin -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        
        <!-- Maven Compiler Plugin (for Lombok and MapStruct) -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>17</source>
                <target>17</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>1.5.5.Final</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Notes**: 
- No new dependencies are required for this US
- All necessary libraries are already in the project
- MapStruct processor must be configured correctly for code generation

---

## Notas

### Reglas de Negocio
1. **Ordenamiento**: Las versiones DEBEN estar ordenadas ascendentemente por `numero_secuencial` (1, 2, 3, ...)
2. **Flag de Versión Actual**: Solo una versión por documento debe tener `esVersionActual = true`
3. **Multi-tenancy**: Los documentos están aislados por `organizacionId` - aplicar validación estricta
4. **Permisos**: Requerir permiso READ sobre el documento antes de listar versiones
5. **Paginación**: Opcional - si no se proporciona, retornar todas las versiones
6. **Información del Creador**: Para el MVP, retornar solo el ID de usuario; detalles completos requieren integración con el servicio de identidad

### Consideraciones de Seguridad
- **Validación JWT**: Manejada por Gateway (fuera del alcance de este servicio)
- **Headers**: `X-User-Id` y `X-Organization-Id` son confiables como validados por Gateway
- **Multi-tenancy**: Siempre validar `documento.organizacionId == organizacionId`
- **Verificación de Permisos**: Llamar a `IEvaluadorPermisos` antes de consultar versiones
- **Mensajes de Error**: No revelar la existencia del documento si el usuario carece de acceso (usar 404)
- **Logging**: Registrar intentos de acceso pero no incluir datos sensibles

### Consideraciones de Rendimiento
- **Uso de Índice**: El índice compuesto `idx_documento_version_doc_numero` es crítico
- **Optimización de Consultas**: Consulta única con ordenamiento basado en índice (sin ordenamiento separado)
- **Transacción de Solo Lectura**: El método del servicio usa `@Transactional(readOnly = true)`
- **Paginación**: Usar para documentos con 50+ versiones para reducir tamaño de payload
- **Rendimiento Esperado**:
  - ≤ 10ms para documentos con < 10 versiones
  - ≤ 50ms para documentos con < 50 versiones
  - ≤ 200ms para documentos con < 500 versiones

### Mejoras Futuras (Fuera del Alcance)
1. **Información del Creador**: Integrar con el servicio de identidad para obtener detalles completos del usuario
   - Opciones: Llamada REST, cola de mensajes, o vista de base de datos
2. **Caché**: Agregar caché Redis para listas de versiones frecuentemente accedidas
3. **Compresión**: Soportar compresión gzip para listas de versiones grandes
4. **Filtrado**: Agregar parámetros de consulta para filtrar por rango de fechas, creador, etc.
5. **Ordenamiento**: Permitir ordenamiento personalizado (actualmente fijo como ascendente)
6. **Comparación de Versiones**: Endpoint para comparar dos versiones (US-DOC-005)
7. **Estadísticas de Descarga**: Rastrear quién descargó qué versión y cuándo

### Requisitos de Idioma
- **Código**: Todo el código (clases, métodos, variables) en Inglés
- **Comentarios**: Javadoc y comentarios en línea en Español
- **Mensajes de Error**: Mensajes de cara al usuario en Español
- **Documentación**: Documentación técnica en Inglés (especificación API, README)
- **Logging**: Mensajes de log en Inglés para consistencia con logs del sistema

---

## Siguientes Pasos Después de la Implementación

1. **Revisión de Código (Code Review)**:
   - Crear Pull Request desde `feature/US-DOC-004-backend` hacia `develop` o `main`
   - Solicitar revisión de al menos un miembro del equipo
   - Atender todos los comentarios de revisión
   - Asegurar que el pipeline CI/CD pase (build, tests, linting)

2. **Pruebas de QA**:
   - Desplegar en el entorno QA
   - Validar endpoint con colección de Postman
   - Probar con volúmenes de datos realistas
   - Verificar métricas de rendimiento
   - Probar escenarios de error manualmente

3. **Integración con Frontend**:
   - Proporcionar contrato de API al equipo de frontend
   - Coordinar sobre manejo de errores
   - Validar que el formato de respuesta coincida con las expectativas del frontend
   - Probar configuración CORS si es necesario

4. **Configuración de Monitoreo**:
   - Agregar métricas de aplicación para el endpoint (si no es automático)
   - Configurar alertas para tasas de error > 5%
   - Monitorear tiempo de respuesta (debe ser < 100ms p95)
   - Rastrear frecuencia de listado de versiones

5. **Documentación**:
   - Asegurar que Swagger UI sea accesible y correcto
   - Actualizar colección de Postman con ejemplos
   - Agregar endpoint al portal de documentación de API
   - Documentar limitaciones conocidas y mejoras futuras

6. **Despliegue a Producción**:
   - Desplegar migraciones de base de datos (creación de índice) primero
   - Desplegar servicio backend
   - Realizar smoke test en producción
   - Monitorear logs y métricas durante las primeras 24 horas
   - Preparar plan de rollback si surgen problemas

---

## Verificación de Implementación

Before marking the ticket as complete, verify the following:

### Calidad de Código
- [ ] Todo el código sigue las convenciones del proyecto (clases en PascalCase, métodos en camelCase)
- [ ] Se usa inyección por constructor (no inyección por campo)
- [ ] Sin valores hardcodeados (usar propiedades de application.yml)
- [ ] Manejo apropiado de excepciones con tipos específicos de excepción
- [ ] Anotaciones de Lombok usadas correctly (@Data, @Builder, etc.)
- [ ] Mapper de MapStruct generado exitosamente (verificar carpeta target/)
- [ ] Sin advertencias del compilador
- [ ] Código bien comentado con Javadoc

### Funcionalidad
- [ ] El endpoint retorna 200 para peticiones válidas
- [ ] Las versiones están ordenadas ascendentemente por numeroSecuencial
- [ ] El flag esVersionActual es correcto
- [ ] La paginación funciona correctamente (metadata es precisa)
- [ ] El aislamiento multi-tenancy se enforce
- [ ] La validación de permisos funciona correctamente
- [ ] Las respuestas de error coinciden con la especificación
- [ ] Todas las restricciones de validación funcionan (@Min, @Max)

### Pruebas
- [ ] Todas las pruebas unitarias pasan: `mvn test -Dtest=DocumentoVersionServiceTest`
- [ ] Todas las pruebas de integración pasan: `mvn test -Dtest=VersionControllerIT`
- [ ] Cobertura de pruebas ≥ 90%: `mvn jacoco:report` (verificar target/site/jacoco)
- [ ] Sin pruebas inestables (ejecutar suite 3 veces para verificar)
- [ ] Las pruebas son limpias y bien nombradas

### Integración
- [ ] El servicio inicia exitosamente: `mvn spring-boot:run`
- [ ] Swagger UI accesible: `http://localhost:8082/swagger-ui.html`
- [ ] Endpoint visible y testeable en Swagger
- [ ] Enrutamiento del Gateway configurado (si aplica)
- [ ] Sin errores en los logs de aplicación durante el inicio

### Documentación
- [ ] Especificación OpenAPI actualizada en `ai-specs/specs/api-spec.yml`
- [ ] README actualizado en `backend/document-core/README.md`
- [ ] Toda la documentación está en Inglés
- [ ] Peticiones y respuestas de ejemplo son precisas
- [ ] Javadoc completo para métodos públicos

### Base de Datos
- [ ] El índice `idx_documento_version_doc_numero` existe
- [ ] EXPLAIN ANALYZE muestra que el índice se usa
- [ ] El rendimiento de la consulta es aceptable
- [ ] Sin conflictos de migración

### Rendimiento
- [ ] Tiempo de respuesta < 100ms para 50 versiones (medir con JMeter o similar)
- [ ] Sin problemas de consultas N+1 (verificar logs SQL)
- [ ] El uso de memoria es razonable (sin fugas)
- [ ] El pool de conexiones de base de datos está configurado apropiadamente

### Seguridad
- [ ] Los headers X-User-Id y X-Organization-Id son requeridos
- [ ] La validación multi-tenancy funciona
- [ ] La verificación de permisos se enforce
- [ ] Los mensajes de error no filtran información sensible
- [ ] Sin vulnerabilidades de inyección SQL (usar consultas parametrizadas)

### Git
- [ ] Rama de funcionalidad `feature/US-DOC-004-backend` creada
- [ ] Los commits tienen mensajes significativos
- [ ] Código comprometido y enviado al remoto
- [ ] Sin conflictos de fusión con develop/main
- [ ] Pull Request creado con descripción

---

## Ejemplos de Peticiones (para Pruebas Manuales)

### Petición 1: Listar Todas las Versiones (Sin Paginación)

```bash
curl -X GET "http://localhost:8082/api/documentos/42/versiones" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-User-Id: 5" \
  -H "X-Organization-Id: 10" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada (200)**:
```json
{
  "versiones": [
    {
      "id": 101,
      "numeroSecuencial": 1,
      "tamanioBytes": 1048576,
      "hashContenido": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "comentarioCambio": "Versión inicial",
      "creadoPor": {
        "id": 5,
        "nombreCompleto": "Juan Pérez",
        "email": "juan.perez@empresa.com"
      },
      "fechaCreacion": "2026-01-15T10:30:00Z",
      "descargas": 12,
      "ultimaDescargaEn": "2026-02-01T14:20:00Z",
      "esVersionActual": false
    },
    {
      "id": 102,
      "numeroSecuencial": 2,
      "tamanioBytes": 1150000,
      "hashContenido": "a3c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85512",
      "comentarioCambio": "Actualización sección 3",
      "creadoPor": {
        "id": 5,
        "nombreCompleto": "Juan Pérez",
        "email": "juan.perez@empresa.com"
      },
      "fechaCreacion": "2026-01-20T16:45:00Z",
      "descargas": 5,
      "ultimaDescargaEn": "2026-02-05T09:10:00Z",
      "esVersionActual": true
    }
  ],
  "documentoId": 42,
  "totalVersiones": 2
}
```

### Petición 2: Listar con Paginación

```bash
curl -X GET "http://localhost:8082/api/documentos/42/versiones?pagina=2&tamanio=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-User-Id: 5" \
  -H "X-Organization-Id: 10" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada (200)**:
```json
{
  "versiones": [ /* 10 versions */ ],
  "documentoId": 42,
  "totalVersiones": 50,
  "paginacion": {
    "paginaActual": 2,
    "tamanio": 10,
    "totalPaginas": 5,
    "totalElementos": 50,
    "primeraPagina": false,
    "ultimaPagina": false
  }
}
```

### Petición 3: Documento No Encontrado

```bash
curl -X GET "http://localhost:8082/api/documentos/99999/versiones" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-User-Id: 5" \
  -H "X-Organization-Id: 10" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada (404)**:
```json
{
  "error": "DOCUMENT_NOT_FOUND",
  "message": "Documento con ID 99999 no encontrado",
  "timestamp": "2026-02-09T14:30:00Z",
  "path": "/api/documentos/99999/versiones"
}
```

### Petición 4: Parámetro de Paginación Inválido

```bash
curl -X GET "http://localhost:8082/api/documentos/42/versiones?tamanio=200" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-User-Id: 5" \
  -H "X-Organization-Id: 10" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada (400)**:
```json
{
  "error": "INVALID_PAGINATION",
  "message": "El tamaño de página debe estar entre 1 y 100",
  "timestamp": "2026-02-09T14:35:00Z",
  "path": "/api/documentos/42/versiones"
}
```

### Petición 5: Sin Permiso

```bash
curl -X GET "http://localhost:8082/api/documentos/42/versiones" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-User-Id: 99" \
  -H "X-Organization-Id: 10" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada (403)**:
```json
{
  "error": "ACCESS_DENIED",
  "message": "No tiene permiso para ver las versiones de este documento",
  "timestamp": "2026-02-09T14:40:00Z",
  "path": "/api/documentos/42/versiones"
}
```

---

## Resumen

Este plan de implementación proporciona una guía completa paso a paso para implementar el endpoint de listado de versiones (US-DOC-004) siguiendo la arquitectura y las mejores prácticas del proyecto. El endpoint:

- Listará versiones de documentos ordenadas ascendentemente por número secuencial
- Soportará paginación opcional (1-100 elementos por página)
- Validará aislamiento multi-tenancy
- Aplicará permisos de LECTURA
- Retornará información comprehensiva de versiones incluyendo detalles del creador y flag de versión actual
- Proporcionará mensajes de error claros siguiendo el formato estándar
- Logrará alta cobertura de pruebas (≥ 90%)
- Se ejecutará eficientemente usando índices de base de datos
- Estará completamente documentado en especificación OpenAPI

**Principios Clave Aplicados**:
- ✅ Arquitectura Hexagonal (Ports & Adapters)
- ✅ Domain-Driven Design (DDD)
- ✅ Principios SOLID
- ✅ Código limpio y separación de responsabilidades
- ✅ Pruebas comprehensivas (unitarias + integración)
- ✅ Seguridad y validación multi-tenancy
- ✅ Optimización de rendimiento (indexación)
- ✅ Documentación completa

El desarrollador puede ahora seguir este plan autónomamente desde el Paso 0 hasta el Paso 9 para completar la implementación de principio a fin.
