## P4 — Documentos + Versionado Lineal

### [US-DOC-004] Listar versiones (API) ordenadas

### Narrativa
Como usuario, quiero listar el historial de versiones, para entender la evolución del documento.

### Criterios de Aceptación
- **Scenario 1:** Dado un documento, Cuando consulto versiones, Entonces recibo una lista ordenada ascendente por `numero_secuencial`.

### 1. Especificación Técnica Completa

#### 1.1 Contexto de la Arquitectura

Esta User Story se implementa siguiendo la arquitectura hexagonal (Ports & Adapters) del servicio **document-core**:

```
backend/document-core/
├── src/main/java/com/docflow/documentcore/
│   ├── domain/
│   │   ├── model/Version.java [✓ ya existe]
│   │   ├── model/Documento.java [✓ ya existe]
│   │   └── repository/VersionRepository.java [✓ ya existe]
│   ├── application/
│   │   ├── dto/VersionListResponse.java [⚠️ crear]
│   │   ├── dto/VersionItemResponse.java [⚠️ crear]
│   │   ├── service/DocumentoVersionService.java [⚠️ crear o modificar]
│   │   └── mapper/VersionListMapper.java [⚠️ crear]
│   ├── infrastructure/
│   │   ├── adapter/controller/VersionController.java [⚠️ crear]
│   │   └── config/SecurityConfig.java [revisar]
│   └── presentation/
│       └── controller/ (si se usa un patrón diferente)
```

**Nota importante:** El proyecto ya tiene implementada la entidad `Version`, el repositorio `VersionRepository`, y el DTO básico `VersionResponse`. Esta historia **extiende** la funcionalidad existente agregando el endpoint de listado.

---

#### 1.2 Contrato de la API REST

##### Endpoint Principal

```http
GET /api/documentos/{documentoId}/versiones
```

##### Headers Requeridos

```http
Authorization: Bearer <JWT_TOKEN>
X-User-Id: <usuario_id>              # Inyectado por gateway
X-Organization-Id: <organizacion_id>  # Inyectado por gateway
```

##### Path Parameters

| Parámetro | Tipo | Obligatorio | Descripción |
|-----------|------|-------------|-------------|
| `documentoId` | Long | Sí | ID del documento cuyas versiones se quieren consultar |

##### Query Parameters (Paginación Opcional)

| Parámetro | Tipo | Obligatorio | Default | Validación | Descripción |
|-----------|------|-------------|---------|------------|-------------|
| `pagina` | Integer | No | N/A | min=1 | Número de página (si se omite, retorna todas las versiones) |
| `tamanio` | Integer | No | 20 | min=1, max=100 | Cantidad de versiones por página |

##### Respuesta Exitosa (200 OK)

**Sin paginación:**
```json
{
  "versiones": [
    {
      "id": 101,
      "numeroSecuencial": 1,
      "tamanioBytes": 1048576,
      "hashContenido": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "comentarioCambio": "Versión inicial del documento",
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
      "comentarioCambio": "Actualización de sección 3",
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

**Con paginación:**
```json
{
  "versiones": [...],
  "documentoId": 42,
  "totalVersiones": 50,
  "paginacion": {
    "paginaActual": 2,
    "tamanio": 10,
    "totalPaginas": 5,
    "totalElementos": 50,
    "primeraPagina": true,
    "ultimaPagina": false
  }
}
```

##### Respuestas de Error

| Código | Tipo de Error | Descripción | Respuesta |
|--------|---------------|-------------|-----------|
| 400 | Bad Request | Parámetros de paginación inválidos | `{"error": "INVALID_PAGINATION", "message": "El tamaño de página debe estar entre 1 y 100"}` |
| 401 | Unauthorized | Token JWT faltante o inválido | `{"error": "UNAUTHORIZED", "message": "Token de autenticación requerido"}` |
| 403 | Forbidden | Usuario sin permiso de lectura | `{"error": "ACCESS_DENIED", "message": "No tiene permiso para ver las versiones de este documento"}` |
| 404 | Not Found | Documento no existe o no pertenece al tenant | `{"error": "DOCUMENT_NOT_FOUND", "message": "Documento con ID 42 no encontrado"}` |
| 500 | Internal Server Error | Error inesperado del servidor | `{"error": "INTERNAL_ERROR", "message": "Error al procesar la solicitud"}` |

---

#### 1.3 Modelo de Datos (Confirmación)

##### Tabla: `documento_version`

La tabla ya existe desde US-DOC-003. Confirmar que tiene los siguientes índices optimizados:

```sql
-- Índice principal para consulta de versiones por documento (DEBE EXISTIR)
CREATE INDEX IF NOT EXISTS idx_documento_version_documento_id 
ON documento_version (documento_id);

-- Índice compuesto para ordenamiento eficiente (VERIFICAR/CREAR)
CREATE INDEX IF NOT EXISTS idx_documento_version_doc_numero 
ON documento_version (documento_id, numero_secuencial);

-- Constraint de unicidad (DEBE EXISTIR desde US-DOC-003)
ALTER TABLE documento_version 
ADD CONSTRAINT uk_documento_version_documento_numero 
UNIQUE (documento_id, numero_secuencial);
```

##### Plan de Ejecución Esperado

Al ejecutar la consulta de versiones, PostgreSQL debe usar el índice `idx_documento_version_doc_numero`:

```sql
EXPLAIN ANALYZE
SELECT v.*
FROM documento_version v
WHERE v.documento_id = 42
ORDER BY v.numero_secuencial ASC;

-- Plan esperado:
-- Index Scan using idx_documento_version_doc_numero on documento_version v
-- (cost=0.15..8.17 rows=1 width=...)
```

---

#### 1.4 Implementación Backend

##### 1.4.1 DTOs de Respuesta

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/VersionItemResponse.java`

```java
package com.docflow.documentcore.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO para representar un item de versión en el listado.
 * 
 * US-DOC-004: Extendido con información del creador y flag de versión actual.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionItemResponse {
    
    private Long id;
    private Integer numeroSecuencial;
    private Long tamanioBytes;
    private String hashContenido;
    private String comentarioCambio;
    
    /**
     * Información del usuario que creó esta versión.
     */
    private CreadorInfo creadoPor;
    
    private OffsetDateTime fechaCreacion;
    private Integer descargas;
    private OffsetDateTime ultimaDescargaEn;
    
    /**
     * Indica si esta es la versión actual del documento.
     * Se calcula comparando con documento.versionActualId.
     */
    private Boolean esVersionActual;
    
    /**
     * DTO anidado para información del creador.
     */
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

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/application/dto/VersionListResponse.java`

```java
package com.docflow.documentcore.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta para listado de versiones de un documento.
 * 
 * US-DOC-004: Soporta formato con y sin paginación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionListResponse {
    
    /**
     * Lista de versiones ordenadas ascendentemente por numeroSecuencial.
     */
    private List<VersionItemResponse> versiones;
    
    /**
     * ID del documento consultado.
     */
    private Long documentoId;
    
    /**
     * Total de versiones del documento (independiente de la paginación).
     */
    private Integer totalVersiones;
    
    /**
     * Metadatos de paginación (null si no se usa paginación).
     */
    private PaginacionInfo paginacion;
    
    /**
     * DTO anidado para información de paginación.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginacionInfo {
        private Integer paginaActual;
        private Integer tamanio;
        private Integer totalPaginas;
        private Integer totalElementos;
        private Boolean primeraPagina;
        private Boolean ultimaPagina;
    }
}
```

##### 1.4.2 Servicio de Aplicación

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/application/service/DocumentoVersionService.java`

```java
package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.VersionItemResponse;
import com.docflow.documentcore.application.dto.VersionListResponse;
import com.docflow.documentcore.application.mapper.VersionListMapper;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de aplicación para gestión de versiones de documentos.
 * 
 * US-DOC-004: Implementa listado de versiones con validación de permisos y paginación opcional.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentoVersionService {
    
    private final DocumentoRepository documentoRepository;
    private final VersionRepository versionRepository;
    private final IEvaluadorPermisos evaluadorPermisos;
    private final VersionListMapper versionListMapper;
    
    /**
     * Lista las versiones de un documento con validación de permisos.
     * 
     * @param documentoId ID del documento
     * @param usuarioId ID del usuario solicitante
     * @param organizacionId ID de la organización (tenant)
     * @param pagina Número de página (opcional, null = sin paginación)
     * @param tamanio Tamaño de página (solo aplica si pagina != null)
     * @return Listado de versiones ordenadas ascendentemente
     * @throws ResourceNotFoundException si el documento no existe
     * @throws AccessDeniedException si el usuario no tiene permiso LECTURA
     */
    @Transactional(readOnly = true)
    public VersionListResponse listarVersiones(
            Long documentoId,
            Long usuarioId,
            Long organizacionId,
            Integer pagina,
            Integer tamanio) {
        
        log.info("Listando versiones del documento {} para usuario {} en organización {}", 
                 documentoId, usuarioId, organizacionId);
        
        // 1. Validar que el documento existe y pertenece al tenant
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento", documentoId));
        
        if (!documento.getOrganizacionId().equals(organizacionId)) {
            throw new ResourceNotFoundException("Documento", documentoId);
        }
        
        // 2. Validar que el usuario tiene permiso de LECTURA
        if (!evaluadorPermisos.tienePermisoLectura(usuarioId, documentoId, organizacionId)) {
            log.warn("Usuario {} sin permiso de lectura para documento {}", 
                     usuarioId, documentoId);
            throw new AccessDeniedException(
                    "No tiene permiso para ver las versiones de este documento");
        }
        
        // 3. Consultar versiones (con o sin paginación)
        List<Version> versiones;
        VersionListResponse.PaginacionInfo paginacionInfo = null;
        
        if (pagina != null && pagina > 0) {
            // Con paginación
            Pageable pageRequest = PageRequest.of(
                    pagina - 1,  // Spring usa índice 0
                    tamanio != null && tamanio > 0 ? tamanio : 20,
                    Sort.by(Sort.Direction.ASC, "numeroSecuencial")
            );
            
            Page<Version> page = versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(
                    documentoId, pageRequest);
            
            versiones = page.getContent();
            paginacionInfo = VersionListResponse.PaginacionInfo.builder()
                    .paginaActual(pagina)
                    .tamanio(page.getSize())
                    .totalPaginas(page.getTotalPages())
                    .totalElementos((int) page.getTotalElements())
                    .primeraPagina(page.isFirst())
                    .ultimaPagina(page.isLast())
                    .build();
            
        } else {
            // Sin paginación - todas las versiones
            versiones = versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(documentoId);
        }
        
        // 4. Mapear a DTOs con flag de versión actual
        List<VersionItemResponse> versionItems = versionListMapper.toItemResponseList(
                versiones, documento.getVersionActualId());
        
        // 5. Construir respuesta
        return VersionListResponse.builder()
                .versiones(versionItems)
                .documentoId(documentoId)
                .totalVersiones(documento.getNumeroVersiones())
                .paginacion(paginacionInfo)
                .build();
    }
}
```

**NOTA:** Si `VersionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc()` no acepta `Pageable`, deberás crear una nueva firma:

```java
// En VersionRepository.java
@Query("SELECT v FROM Version v WHERE v.documentoId = :documentoId ORDER BY v.numeroSecuencial ASC")
Page<Version> findByDocumentoIdOrderByNumeroSecuencialAsc(
    @Param("documentoId") Long documentoId, 
    Pageable pageable);
```

##### 1.4.3 Mapper

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/application/mapper/VersionListMapper.java`

```java
package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.application.dto.VersionItemResponse;
import com.docflow.documentcore.domain.model.Version;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper para transformar entidades Version a DTOs de listado.
 * 
 * US-DOC-004: Mapea versiones incluyendo información del creador y flag esVersionActual.
 */
@Mapper(componentModel = "spring")
@Component
public interface VersionListMapper {
    
    /**
     * Convierte una lista de versiones a DTOs de respuesta.
     * 
     * @param versiones Lista de entidades Version
     * @param versionActualId ID de la versión actual del documento
     * @return Lista de DTOs con flag esVersionActual calculado
     */
    default List<VersionItemResponse> toItemResponseList(
            List<Version> versiones, 
            Long versionActualId) {
        
        return versiones.stream()
                .map(v -> toItemResponse(v, versionActualId))
                .toList();
    }
    
    /**
     * Convierte una entidad Version a DTO con información del creador.
     * 
     * @param version Entidad Version
     * @param versionActualId ID de la versión actual del documento
     * @return DTO con todos los campos mapeados
     */
    @Mapping(target = "esVersionActual", expression = "java(version.getId().equals(versionActualId))")
    @Mapping(target = "creadoPor", source = "version", qualifiedByName = "mapCreadorInfo")
    VersionItemResponse toItemResponse(Version version, Long versionActualId);
    
    /**
     * Mapea información del creador.
     * NOTA: Este método requiere acceso al servicio de usuarios o una proyección.
     * 
     * IMPLEMENTACIÓN SUGERIDA:
     * - Opción A: Inyectar UserRepository y consultar usuario por ID
     * - Opción B: Agregar join en el query de VersionRepository
     * - Opción C: Usar proyección con @EntityGraph
     */
    @Named("mapCreadorInfo")
    default VersionItemResponse.CreadorInfo mapCreadorInfo(Version version) {
        // TODO: Implementar según estrategia elegida
        // Por ahora retornamos solo el ID
        return VersionItemResponse.CreadorInfo.builder()
                .id(version.getCreadoPor())
                .nombreCompleto("Usuario " + version.getCreadoPor())  // Temporal
                .email("user" + version.getCreadoPor() + "@example.com")  // Temporal
                .build();
    }
}
```

Agregar join en `VersionRepository` para traer usuario en una sola query:
   ```java
   @Query("""
       SELECT v, u.firstName, u.lastName, u.email 
       FROM Version v 
       LEFT JOIN User u ON v.creadoPor = u.id 
       WHERE v.documentoId = :documentoId 
       ORDER BY v.numeroSecuencial ASC
   """)
   List<Object[]> findVersionsWithCreatorByDocumentoId(@Param("documentoId") Long documentoId);
   ```

##### 1.4.4 Controlador REST

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/VersionController.java`

```java
package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.VersionListResponse;
import com.docflow.documentcore.application.service.DocumentoVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

/**
 * Controlador REST para gestión de versiones de documentos.
 * 
 * US-DOC-004: Expone endpoint de listado de versiones con paginación opcional.
 */
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Versiones", description = "Gestión de versiones de documentos")
public class VersionController {
    
    private final DocumentoVersionService documentoVersionService;
    
    /**
     * Lista las versiones de un documento ordenadas ascendentemente.
     * 
     * @param documentoId ID del documento
     * @param usuarioId ID del usuario (header X-User-Id inyectado por gateway)
     * @param organizacionId ID de la organización (header X-Organization-Id inyectado por gateway)
     * @param pagina Número de página (opcional, 1-based)
     * @param tamanio Tamaño de página (opcional, 1-100)
     * @return Listado de versiones con metadatos
     */
    @GetMapping("/{documentoId}/versiones")
    @Operation(
        summary = "Listar versiones de documento",
        description = "Obtiene el historial completo de versiones de un documento ordenadas ascendentemente. " +
                      "Soporta paginación opcional. Requiere permiso de LECTURA sobre el documento."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Versiones obtenidas exitosamente",
                     content = @Content(schema = @Schema(implementation = VersionListResponse.class))),
        @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos"),
        @ApiResponse(responseCode = "401", description = "Token de autenticación faltante o inválido"),
        @ApiResponse(responseCode = "403", description = "Sin permiso de lectura"),
        @ApiResponse(responseCode = "404", description = "Documento no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<VersionListResponse> listarVersiones(
            @Parameter(description = "ID del documento", required = true)
            @PathVariable Long documentoId,
            
            @Parameter(description = "ID del usuario solicitante (inyectado por gateway)", hidden = true)
            @RequestHeader("X-User-Id") Long usuarioId,
            
            @Parameter(description = "ID de la organización (inyectado por gateway)", hidden = true)
            @RequestHeader("X-Organization-Id") Long organizacionId,
            
            @Parameter(description = "Número de página (1-based, omitir para listar todas)")
            @RequestParam(required = false) 
            @Min(value = 1, message = "La página debe ser mayor o igual a 1")
            Integer pagina,
            
            @Parameter(description = "Tamaño de página (1-100, default 20)")
            @RequestParam(required = false, defaultValue = "20") 
            @Min(value = 1, message = "El tamaño debe ser mayor o igual a 1")
            @Max(value = 100, message = "El tamaño debe ser menor o igual a 100")
            Integer tamanio) {
        
        log.info("GET /api/documentos/{}/versiones - Usuario: {}, Org: {}, Pag: {}, Tam: {}",
                 documentoId, usuarioId, organizacionId, pagina, tamanio);
        
        VersionListResponse response = documentoVersionService.listarVersiones(
                documentoId, usuarioId, organizacionId, pagina, tamanio);
        
        return ResponseEntity.ok(response);
    }
}
```

---

#### 1.5 Testing

##### 1.5.1 Test Unitario del Servicio

**Archivo:** `backend/document-core/src/test/java/com/docflow/documentcore/application/service/DocumentoVersionServiceTest.java`

```java
package com.docflow.documentcore.application.service;

import com.docflow.documentcore.application.dto.VersionListResponse;
import com.docflow.documentcore.application.mapper.VersionListMapper;
import com.docflow.documentcore.domain.exception.AccessDeniedException;
import com.docflow.documentcore.domain.exception.ResourceNotFoundException;
import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentoVersionService - Listar Versiones")
class DocumentoVersionServiceTest {
    
    @Mock
    private DocumentoRepository documentoRepository;
    
    @Mock
    private VersionRepository versionRepository;
    
    @Mock
    private IEvaluadorPermisos evaluadorPermisos;
    
    @Mock
    private VersionListMapper versionListMapper;
    
    @InjectMocks
    private DocumentoVersionService service;
    
    private Documento documento;
    private List<Version> versiones;
    
    @BeforeEach
    void setUp() {
        documento = new Documento();
        documento.setId(42L);
        documento.setOrganizacionId(10L);
        documento.setVersionActualId(102L);
        documento.setNumeroVersiones(2);
        
        Version v1 = new Version();
        v1.setId(101L);
        v1.setDocumentoId(42L);
        v1.setNumeroSecuencial(1);
        v1.setTamanioBytes(1024L);
        v1.setFechaCreacion(OffsetDateTime.now().minusDays(5));
        v1.setCreadoPor(5L);
        
        Version v2 = new Version();
        v2.setId(102L);
        v2.setDocumentoId(42L);
        v2.setNumeroSecuencial(2);
        v2.setTamanioBytes(2048L);
        v2.setFechaCreacion(OffsetDateTime.now());
        v2.setCreadoPor(5L);
        
        versiones = List.of(v1, v2);
    }
    
    @Test
    @DisplayName("Debe listar versiones ordenadas ascendentemente cuando el usuario tiene permisos")
    void should_ListVersions_When_UserHasPermission() {
        // Given
        Long documentoId = 42L;
        Long usuarioId = 5L;
        Long organizacionId = 10L;
        
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tienePermisoLectura(usuarioId, documentoId, organizacionId))
                .thenReturn(true);
        when(versionRepository.findByDocumentoIdOrderByNumeroSecuencialAsc(documentoId))
                .thenReturn(versiones);
        when(versionListMapper.toItemResponseList(anyList(), anyLong()))
                .thenReturn(List.of());  // Simplificado para este test
        
        // When
        VersionListResponse response = service.listarVersiones(
                documentoId, usuarioId, organizacionId, null, null);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getDocumentoId()).isEqualTo(documentoId);
        assertThat(response.getTotalVersiones()).isEqualTo(2);
        assertThat(response.getPaginacion()).isNull();  // Sin paginación
        
        verify(documentoRepository).findById(documentoId);
        verify(evaluadorPermisos).tienePermisoLectura(usuarioId, documentoId, organizacionId);
        verify(versionRepository).findByDocumentoIdOrderByNumeroSecuencialAsc(documentoId);
        verify(versionListMapper).toItemResponseList(versiones, 102L);
    }
    
    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el documento no existe")
    void should_ThrowNotFoundException_When_DocumentNotExists() {
        // Given
        Long documentoId = 999L;
        when(documentoRepository.findById(documentoId)).thenReturn(Optional.empty());
        
        // When / Then
        assertThatThrownBy(() -> service.listarVersiones(documentoId, 5L, 10L, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Documento");
        
        verify(documentoRepository).findById(documentoId);
        verifyNoInteractions(versionRepository);
    }
    
    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException cuando el documento no pertenece al tenant")
    void should_ThrowNotFoundException_When_DocumentNotInTenant() {
        // Given
        documento.setOrganizacionId(99L);  // Diferente organizacionId
        when(documentoRepository.findById(42L)).thenReturn(Optional.of(documento));
        
        // When / Then
        assertThatThrownBy(() -> service.listarVersiones(42L, 5L, 10L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        
        verifyNoInteractions(versionRepository);
    }
    
    @Test
    @DisplayName("Debe lanzar AccessDeniedException cuando el usuario no tiene permiso de lectura")
    void should_ThrowAccessDenied_When_UserHasNoPermission() {
        // Given
        when(documentoRepository.findById(42L)).thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tienePermisoLectura(5L, 42L, 10L)).thenReturn(false);
        
        // When / Then
        assertThatThrownBy(() -> service.listarVersiones(42L, 5L, 10L, null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permiso");
        
        verifyNoInteractions(versionRepository);
    }
    
    @Test
    @DisplayName("Debe aplicar paginación cuando se especifican parámetros")
    void should_ApplyPagination_When_ParametersProvided() {
        // Given
        when(documentoRepository.findById(42L)).thenReturn(Optional.of(documento));
        when(evaluadorPermisos.tienePermisoLectura(anyLong(), anyLong(), anyLong()))
                .thenReturn(true);
        // Este test requiere mockear Page<Version> - implementación simplificada
        // TODO: Completar con Page mock
        
        // When
        // VersionListResponse response = service.listarVersiones(42L, 5L, 10L, 2, 10);
        
        // Then
        // assertThat(response.getPaginacion()).isNotNull();
        // assertThat(response.getPaginacion().getPaginaActual()).isEqualTo(2);
    }
}
```

##### 1.5.2 Test de Integración del Endpoint

**Archivo:** `backend/document-core/src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/VersionControllerIT.java`

```java
package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.domain.model.Documento;
import com.docflow.documentcore.domain.model.Version;
import com.docflow.documentcore.domain.repository.DocumentoRepository;
import com.docflow.documentcore.domain.repository.VersionRepository;
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

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("VersionController - Integration Tests")
class VersionControllerIT {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private DocumentoRepository documentoRepository;
    
    @Autowired
    private VersionRepository versionRepository;
    
    private Documento documento;
    private Version version1;
    private Version version2;
    
    @BeforeEach
    void setUp() {
        // Crear documento de prueba
        documento = new Documento();
        documento.setOrganizacionId(10L);
        documento.setNombre("documento-test.pdf");
        documento.setTipoContenido("application/pdf");
        documento.setTamanioBytes(2048L);
        documento.setNumeroVersiones(2);
        documento.setCreadoPor(5L);
        documento.setFechaCreacion(OffsetDateTime.now());
        documento.setFechaActualizacion(OffsetDateTime.now());
        documento = documentoRepository.save(documento);
        
        // Crear versiones
        version1 = new Version();
        version1.setDocumentoId(documento.getId());
        version1.setNumeroSecuencial(1);
        version1.setTamanioBytes(1024L);
        version1.setRutaAlmacenamiento("/storage/doc1-v1.pdf");
        version1.setHashContenido("hash1");
        version1.setCreadoPor(5L);
        version1.setFechaCreacion(OffsetDateTime.now().minusDays(1));
        version1 = versionRepository.save(version1);
        
        version2 = new Version();
        version2.setDocumentoId(documento.getId());
        version2.setNumeroSecuencial(2);
        version2.setTamanioBytes(2048L);
        version2.setRutaAlmacenamiento("/storage/doc1-v2.pdf");
        version2.setHashContenido("hash2");
        version2.setCreadoPor(5L);
        version2.setFechaCreacion(OffsetDateTime.now());
        version2 = versionRepository.save(version2);
        
        documento.setVersionActualId(version2.getId());
        documentoRepository.save(documento);
    }
    
    @Test
    @DisplayName("GET /api/documentos/{id}/versiones - Debe retornar 200 con versiones ordenadas")
    void should_Return200WithOrderedVersions_When_ValidRequest() throws Exception {
        mockMvc.perform(get("/api/documentos/{id}/versiones", documento.getId())
                        .header("X-User-Id", "5")
                        .header("X-Organization-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentoId").value(documento.getId()))
                .andExpect(jsonPath("$.totalVersiones").value(2))
                .andExpect(jsonPath("$.versiones").isArray())
                .andExpect(jsonPath("$.versiones", hasSize(2)))
                // Primera versión (orden ascendente)
                .andExpect(jsonPath("$.versiones[0].numeroSecuencial").value(1))
                .andExpect(jsonPath("$.versiones[0].esVersionActual").value(false))
                // Segunda versión
                .andExpect(jsonPath("$.versiones[1].numeroSecuencial").value(2))
                .andExpect(jsonPath("$.versiones[1].esVersionActual").value(true))
                // Sin paginación
                .andExpect(jsonPath("$.paginacion").doesNotExist());
    }
    
    @Test
    @DisplayName("GET /api/documentos/{id}/versiones - Debe retornar 404 cuando documento no existe")
    void should_Return404_When_DocumentNotFound() throws Exception {
        mockMvc.perform(get("/api/documentos/{id}/versiones", 99999L)
                        .header("X-User-Id", "5")
                        .header("X-Organization-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("DOCUMENT_NOT_FOUND"));
    }
    
    @Test
    @DisplayName("GET /api/documentos/{id}/versiones - Debe retornar 403 sin permisos")
    void should_Return403_When_NoPermission() throws Exception {
        // Este test requiere mockear IEvaluadorPermisos
        // o usar un usuario sin permisos en BD de pruebas
        // TODO: Implementar según estrategia de permisos en tests
    }
    
    @Test
    @DisplayName("GET /api/documentos/{id}/versiones?pagina=1&tamanio=1 - Debe retornar con paginación")
    void should_ReturnPaginatedResults_When_PaginationParametersProvided() throws Exception {
        mockMvc.perform(get("/api/documentos/{id}/versiones", documento.getId())
                        .param("pagina", "1")
                        .param("tamanio", "1")
                        .header("X-User-Id", "5")
                        .header("X-Organization-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versiones", hasSize(1)))
                .andExpect(jsonPath("$.totalVersiones").value(2))
                .andExpect(jsonPath("$.paginacion").exists())
                .andExpect(jsonPath("$.paginacion.paginaActual").value(1))
                .andExpect(jsonPath("$.paginacion.tamanio").value(1))
                .andExpect(jsonPath("$.paginacion.totalPaginas").value(2))
                .andExpect(jsonPath("$.paginacion.primeraPagina").value(true))
                .andExpect(jsonPath("$.paginacion.ultimaPagina").value(false));
    }
    
    @Test
    @DisplayName("GET /api/documentos/{id}/versiones?tamanio=200 - Debe retornar 400 cuando tamaño excede límite")
    void should_Return400_When_PageSizeExceedsLimit() throws Exception {
        mockMvc.perform(get("/api/documentos/{id}/versiones", documento.getId())
                        .param("tamanio", "200")
                        .header("X-User-Id", "5")
                        .header("X-Organization-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PAGINATION"))
                .andExpect(jsonPath("$.message").value(containsString("100")));
    }
}
```

---

#### 1.6 Validación de Seguridad

##### Flujo de Autenticación y Autorización

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant DocCore
    participant AuthService
    participant DB
    
    Client->>Gateway: GET /api/documentos/42/versiones<br/>Authorization: Bearer <JWT>
    Gateway->>Gateway: Validar JWT
    Gateway->>Gateway: Extraer userId, orgId del token
    Gateway->>DocCore: GET /internal/documentos/42/versiones<br/>X-User-Id: 5<br/>X-Organization-Id: 10
    DocCore->>DB: SELECT * FROM documento WHERE id=42
    DB-->>DocCore: Documento (orgId=10)
    DocCore->>DocCore: Validar orgId == tokenOrgId
    DocCore->>DocCore: Evaluar permiso LECTURA<br/>(IEvaluadorPermisos)
    alt Sin permiso
        DocCore-->>Gateway: 403 Forbidden
        Gateway-->>Client: 403 Forbidden
    else Con permiso
        DocCore->>DB: SELECT * FROM documento_version<br/>WHERE documento_id=42<br/>ORDER BY numero_secuencial ASC
        DB-->>DocCore: Versiones [v1, v2, v3]
        DocCore->>DocCore: Mapear a DTOs
        DocCore-->>Gateway: 200 OK + VersionListResponse
        Gateway-->>Client: 200 OK + VersionListResponse
    end
```

##### Checklist de Seguridad

- [x] **Autenticación:** JWT validado por Gateway antes de llegar a Document-Core
- [x] **Multi-tenancy:** Validación de `organizacionId` en el documento
- [x] **Autorización:** Verificación de permiso LECTURA mediante `IEvaluadorPermisos`
- [x] **Inyección SQL:** Uso de JPA con parámetros seguros
- [x] **Validación de entrada:** Validaciones con `@Min`, `@Max` en parámetros de paginación
- [x] **Rate Limiting:** (Implementado en Gateway, fuera del alcance de esta US)
- [x] **Logs de auditoría:** Logging de acceso a versiones en servicio

---

#### 1.7 Documentación API (OpenAPI)

Agregar la siguiente especificación al archivo **`api-spec.yml`**:

```yaml
/api/documentos/{documentoId}/versiones:
  get:
    summary: Listar versiones de un documento
    description: |
      Obtiene el historial completo de versiones de un documento ordenadas ascendentemente
      por número secuencial. Soporta paginación opcional.
      
      **Permisos requeridos:** LECTURA sobre el documento.
      
      **Comportamiento:**
      - Sin parámetros de paginación: retorna todas las versiones
      - Con `pagina` y `tamanio`: retorna página específica
      - Siempre ordenado ascendentemente por `numero_secuencial`
      - Flag `esVersionActual` indica cuál es la versión actual del documento
    tags:
      - Versiones
    security:
      - bearerAuth: []
    parameters:
      - name: documentoId
        in: path
        required: true
        schema:
          type: integer
          format: int64
        description: ID del documento
      - name: X-User-Id
        in: header
        required: true
        schema:
          type: integer
          format: int64
        description: ID del usuario (inyectado por gateway)
      - name: X-Organization-Id
        in: header
        required: true
        schema:
          type: integer
          format: int64
        description: ID de la organización (inyectado por gateway)
      - name: pagina
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
        description: Número de página (1-based). Omitir para listar todas las versiones.
      - name: tamanio
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
          maximum: 100
          default: 20
        description: Tamaño de página (solo aplica si se especifica pagina)
    responses:
      '200':
        description: Listado de versiones obtenido exitosamente
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/VersionListResponse'
            examples:
              sinPaginacion:
                summary: Sin paginación (todas las versiones)
                value:
                  versiones:
                    - id: 101
                      numeroSecuencial: 1
                      tamanioBytes: 1048576
                      hashContenido: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                      comentarioCambio: "Versión inicial"
                      creadoPor:
                        id: 5
                        nombreCompleto: "Juan Pérez"
                        email: "juan.perez@empresa.com"
                      fechaCreacion: "2026-01-15T10:30:00Z"
                      descargas: 12
                      ultimaDescargaEn: "2026-02-01T14:20:00Z"
                      esVersionActual: false
                    - id: 102
                      numeroSecuencial: 2
                      tamanioBytes: 1150000
                      hashContenido: "a3c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85512"
                      comentarioCambio: "Actualización sección 3"
                      creadoPor:
                        id: 5
                        nombreCompleto: "Juan Pérez"
                        email: "juan.perez@empresa.com"
                      fechaCreacion: "2026-01-20T16:45:00Z"
                      descargas: 5
                      ultimaDescargaEn: "2026-02-05T09:10:00Z"
                      esVersionActual: true
                  documentoId: 42
                  totalVersiones: 2
              conPaginacion:
                summary: Con paginación
                value:
                  versiones:
                    - id: 110
                      numeroSecuencial: 10
                      tamanioBytes: 2048000
                      hashContenido: "d4e5f6..."
                      comentarioCambio: "Revisión Q1"
                      creadoPor:
                        id: 8
                        nombreCompleto: "María García"
                        email: "maria.garcia@empresa.com"
                      fechaCreacion: "2026-02-08T11:00:00Z"
                      descargas: 0
                      ultimaDescargaEn: null
                      esVersionActual: false
                  documentoId: 42
                  totalVersiones: 25
                  paginacion:
                    paginaActual: 2
                    tamanio: 10
                    totalPaginas: 3
                    totalElementos: 25
                    primeraPagina: false
                    ultimaPagina: false
      '400':
        description: Parámetros de paginación inválidos
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            example:
              error: "INVALID_PAGINATION"
              message: "El tamaño de página debe estar entre 1 y 100"
      '401':
        description: Token de autenticación faltante o inválido
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
      '403':
        description: Usuario sin permiso de lectura
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            example:
              error: "ACCESS_DENIED"
              message: "No tiene permiso para ver las versiones de este documento"
      '404':
        description: Documento no encontrado o no pertenece a la organización
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'
            example:
              error: "DOCUMENT_NOT_FOUND"
              message: "Documento con ID 42 no encontrado"
      '500':
        description: Error interno del servidor
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ErrorResponse'

components:
  schemas:
    VersionListResponse:
      type: object
      required:
        - versiones
        - documentoId
        - totalVersiones
      properties:
        versiones:
          type: array
          items:
            $ref: '#/components/schemas/VersionItemResponse'
          description: Lista de versiones ordenadas ascendentemente
        documentoId:
          type: integer
          format: int64
          description: ID del documento consultado
        totalVersiones:
          type: integer
          description: Total de versiones del documento (no afectado por paginación)
        paginacion:
          $ref: '#/components/schemas/PaginacionInfo'
          description: Metadatos de paginación (null si no se usa paginación)
    
    VersionItemResponse:
      type: object
      required:
        - id
        - numeroSecuencial
        - tamanioBytes
        - hashContenido
        - creadoPor
        - fechaCreacion
        - descargas
        - esVersionActual
      properties:
        id:
          type: integer
          format: int64
          description: ID único de la versión
        numeroSecuencial:
          type: integer
          minimum: 1
          description: Número secuencial de la versión (1, 2, 3, ...)
        tamanioBytes:
          type: integer
          format: int64
          description: Tamaño del archivo en bytes
        hashContenido:
          type: string
          pattern: '^[a-f0-9]{64}$'
          description: Hash SHA256 del contenido
        comentarioCambio:
          type: string
          maxLength: 500
          nullable: true
          description: Comentario de cambios de esta versión
        creadoPor:
          $ref: '#/components/schemas/CreadorInfo'
        fechaCreacion:
          type: string
          format: date-time
          description: Fecha de creación de la versión
        descargas:
          type: integer
          minimum: 0
          description: Número de veces que se ha descargado esta versión
        ultimaDescargaEn:
          type: string
          format: date-time
          nullable: true
          description: Fecha de la última descarga
        esVersionActual:
          type: boolean
          description: Indica si esta es la versión actual del documento
    
    CreadorInfo:
      type: object
      required:
        - id
        - nombreCompleto
        - email
      properties:
        id:
          type: integer
          format: int64
          description: ID del usuario creador
        nombreCompleto:
          type: string
          description: Nombre completo del creador
        email:
          type: string
          format: email
          description: Email del creador
    
    PaginacionInfo:
      type: object
      required:
        - paginaActual
        - tamanio
        - totalPaginas
        - totalElementos
        - primeraPagina
        - ultimaPagina
      properties:
        paginaActual:
          type: integer
          minimum: 1
          description: Número de página actual
        tamanio:
          type: integer
          minimum: 1
          maximum: 100
          description: Tamaño de página
        totalPaginas:
          type: integer
          minimum: 0
          description: Total de páginas disponibles
        totalElementos:
          type: integer
          minimum: 0
          description: Total de elementos (versiones) disponibles
        primeraPagina:
          type: boolean
          description: Indica si es la primera página
        ultimaPagina:
          type: boolean
          description: Indica si es la última página
```

---

#### 1.8 Consideraciones de Performance

##### Optimizaciones Implementadas

1. **Índice Compuesto:**
   - `idx_documento_version_doc_numero (documento_id, numero_secuencial)` permite ordenamiento eficiente
   - Evita full table scan y sort adicional

2. **Query Optimizado:**
   - Single SELECT con ordenamiento en índice
   - Sin joins innecesarios (información de usuario se resuelve después si es requerida)

3. **Paginación:**
   - Reduce transferencia de datos para documentos con muchas versiones
   - Usa `LIMIT` y `OFFSET` de forma eficiente con índice

##### Escenarios de Carga

| Escenario | Versiones por Doc | Performance Esperada | Recomendación |
|-----------|-------------------|----------------------|---------------|
| Documento nuevo | 1-5 | < 10ms | Sin paginación |
| Documento activo | 10-50 | < 50ms | Sin/con paginación |
| Documento legacy | 100-500 | < 200ms | **Paginación obligatoria** |
| Documento masivo | 1000+ | < 500ms | **Paginación + límite máximo** |

##### Futuras Optimizaciones (Out of Scope)

- **Caché distribuido:** Redis para versiones frecuentemente consultadas
- **Proyecciones:** Usar `@EntityGraph` o DTOs en query para evitar N+1 con información de usuario
- **Archivado:** Mover versiones antiguas a tabla histórica después de X tiempo

---

#### 1.9 Checklist de Implementación

##### Base de Datos
- [ ] Verificar existencia de índice `idx_documento_version_doc_numero`
- [ ] Ejecutar `EXPLAIN ANALYZE` en query de versiones
- [ ] Validar constraint `uk_documento_version_documento_numero`

##### Backend - DTOs
- [ ] Crear `VersionItemResponse.java` con campo `esVersionActual` y `CreadorInfo`
- [ ] Crear `VersionListResponse.java` con soporte de paginación
- [ ] Actualizar `VersionResponse.java` existente si es necesario (o mantener separado)

##### Backend - Servicio
- [ ] Crear/modificar `DocumentoVersionService.java`
- [ ] Implementar método `listarVersiones()` con validación de permisos
- [ ] Agregar lógica de paginación opcional
- [ ] Calcular flag `esVersionActual` comparando con `documento.versionActualId`

##### Backend - Repositorio
- [ ] Agregar firma con `Pageable` en `VersionRepository` si no existe:
  ```java
  Page<Version> findByDocumentoIdOrderByNumeroSecuencialAsc(Long documentoId, Pageable pageable);
  ```
- [ ] Mantener método sin `Pageable` para listados completos

##### Backend - Mapper
- [ ] Crear `VersionListMapper.java` con MapStruct
- [ ] Implementar mapeo de `Version` a `VersionItemResponse`
- [ ] Resolver información del creador (decidir estrategia: join, servicio, o solo ID)

##### Backend - Controlador
- [ ] Crear `VersionController.java` en `infrastructure/adapter/controller/`
- [ ] Implementar endpoint `GET /api/documentos/{id}/versiones`
- [ ] Agregar validaciones con `@Min`, `@Max` en parámetros
- [ ] Documentar con anotaciones de Swagger (`@Operation`, `@ApiResponses`)

##### Testing - Unitario
- [ ] Test: Listar versiones ordenadas ascendentemente
- [ ] Test: Flag `esVersionActual` correcto
- [ ] Test: Documento no existe → 404
- [ ] Test: Documento de otro tenant → 404
- [ ] Test: Sin permisos → 403
- [ ] Test: Paginación correcta (offset, límite, metadatos)

##### Testing - Integración
- [ ] Test HTTP: `GET /versiones` retorna 200 con array ordenado
- [ ] Test HTTP: Verificar estructura JSON de respuesta
- [ ] Test HTTP: Validar paginación con query params
- [ ] Test HTTP: `tamanio > 100` retorna 400
- [ ] Test HTTP: Documento inexistente retorna 404

##### Documentación
- [ ] Actualizar `api-spec.yml` con endpoint de versiones
- [ ] Agregar esquemas de `VersionListResponse`, `VersionItemResponse`, `PaginacionInfo`
- [ ] Documentar códigos de error y ejemplos de respuesta
- [ ] Actualizar `README.md` del servicio document-core

##### Seguridad
- [ ] Validar autenticación JWT en gateway (fuera del alcance de esta US)
- [ ] Verificar inyección de headers `X-User-Id` y `X-Organization-Id` por gateway
- [ ] Validar aislamiento multi-tenant en servicio
- [ ] Probar intento de acceso a documento de otra organización
- [ ] Verificar que no se exponga información sensible en logs
- [ ] Test de autorización: usuario sin permiso LECTURA

##### Deployment
- [ ] Ejecutar suite completa de tests (`mvn test`)
- [ ] Verificar cobertura de código ≥ 90%
- [ ] Build exitoso sin warnings (`mvn clean package`)
- [ ] Validar en entorno de desarrollo con Postman/curl
- [ ] Actualizar changelog con US-DOC-004

---

#### 1.10 Definición de Completado (DoD)

Esta User Story se considera **DONE** cuando:

1. ✅ **Código implementado:**
   - Endpoint `GET /api/documentos/{id}/versiones` funcional
   - Servicio con validación de permisos completa
   - Paginación opcional implementada
   - Flag `esVersionActual` calculado correctamente

2. ✅ **Testing:**
   - Tests unitarios del servicio (cobertura ≥ 90%)
   - Tests de integración del endpoint (todos los escenarios pasando)
   - Tests de validación de permisos (403, 404)
   - Tests de paginación (parámetros válidos e inválidos)

3. ✅ **Documentación:**
   - OpenAPI spec actualizado en `api-spec.yml`
   - README del servicio actualizado
   - Comentarios Javadoc en clases y métodos principales

4. ✅ **Performance:**
   - Query usa índice `idx_documento_version_doc_numero`
   - Tiempo de respuesta < 100ms para 50 versiones
   - Plan de ejecución validado con `EXPLAIN ANALYZE`

5. ✅ **Seguridad:**
   - Validación de permisos funcional
   - Aislamiento multi-tenant verificado
   - Sin exposición de datos sensibles en logs

6. ✅ **Code Review:**
   - PR aprobado por al menos 1 reviewer
   - Todos los comentarios resueltos
   - Build de CI/CD pasando

7. ✅ **Despliegue:**
   - Código mergeado a `main`/`develop`
   - Desplegado en entorno de QA
   - Validación manual con colección Postman

---

#### 1.11 Ejemplos de Uso (Postman/Curl)

##### Listar todas las versiones

```bash
curl -X GET "http://localhost:8082/api/documentos/42/versiones" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-User-Id: 5" \
  -H "X-Organization-Id: 10" \
  -H "Content-Type: application/json"
```

**Respuesta:**
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

##### Listar con paginación (página 2, tamaño 10)

```bash
curl -X GET "http://localhost:8082/api/documentos/42/versiones?pagina=2&tamanio=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-User-Id: 5" \
  -H "X-Organization-Id: 10" \
  -H "Content-Type: application/json"
```

**Respuesta:**
```json
{
  "versiones": [
    // Versiones 11-20 del documento
  ],
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

##### Error: Documento no encontrado

```bash
curl -X GET "http://localhost:8082/api/documentos/99999/versiones" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-User-Id: 5" \
  -H "X-Organization-Id: 10" \
  -H "Content-Type: application/json"
```

**Respuesta (404):**
```json
{
  "error": "DOCUMENT_NOT_FOUND",
  "message": "Documento con ID 99999 no encontrado",
  "timestamp": "2026-02-09T14:30:00Z",
  "path": "/api/documentos/99999/versiones"
}
```

##### Error: Sin permisos

```bash
curl -X GET "http://localhost:8082/api/documentos/42/versiones" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "X-User-Id: 99" \
  -H "X-Organization-Id: 10" \
  -H "Content-Type: application/json"
```

**Respuesta (403):**
```json
{
  "error": "ACCESS_DENIED",
  "message": "No tiene permiso para ver las versiones de este documento",
  "timestamp": "2026-02-09T14:35:00Z",
  "path": "/api/documentos/42/versiones"
}
```

---

#### 1.12 Dependencias Técnicas

##### Librerías Maven (Verificar en pom.xml)

```xml
<!-- Ya existen en el proyecto, verificar versiones -->
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
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.7.0</version>
</dependency>
```