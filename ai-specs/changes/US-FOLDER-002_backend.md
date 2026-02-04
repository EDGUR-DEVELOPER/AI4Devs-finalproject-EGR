# Backend Implementation Plan: US-FOLDER-002 - Listar Contenido de Carpeta (API) con Visibilidad por Permisos

## Overview

Esta historia implementa un endpoint REST completo que permite a usuarios autenticados listar el contenido (subcarpetas y documentos) de una carpeta, aplicando filtrado automático basado en permisos ACL. El sistema debe considerar:
- Permisos directos en subcarpetas/documentos
- Herencia recursiva de permisos desde carpetas padre
- Precedencia: ACL en documento > ACL en carpeta
- Soft-delete: exclusión de elementos marcados como eliminados
- Aislamiento multi-tenant por organizacion_id

Sigue principios de **Arquitectura Hexagonal** (Domain → Application → Infrastructure) y **Clean Code** para máxima testabilidad y mantenibilidad.

---

## Architecture Context

### Layers Involved

**Domain Layer**:
- Value Objects: `ContenidoCarpeta`, `CarpetaItem`, `DocumentoItem`, `CapacidadesUsuario`
- Interfaces de Repositorio: `ICarpetaRepository`, `IDocumentoRepository`
- Servicio de Dominio: `IEvaluadorPermisos` (existente)
- Excepciones: `CarpetaNotFoundException`, `AccesoDenegadoException`, `CarpetaRaizNoEncontradaException`

**Application Layer**:
- Servicio: `CarpetaContenidoService`
- DTOs: `ContenidoCarpetaDTO`, `CarpetaItemDTO`, `DocumentoItemDTO`, `UsuarioResumenDTO`
- Mappers: `ContenidoCarpetaMapper` (MapStruct)

**Infrastructure Layer**:
- Controlador REST: `CarpetaContenidoController`
- Repositorios JPA: `CarpetaJpaRepository`, `DocumentoJpaRepository`
- Exception Handler: `GlobalExceptionHandler`
- Migraciones BD: Índices compuestos optimizados

### Components/Files Referenced

```
backend/document-core/
├── src/main/java/com/docflow/documentcore/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── ContenidoCarpeta.java (NEW)
│   │   │   ├── CarpetaItem.java (NEW)
│   │   │   ├── DocumentoItem.java (NEW)
│   │   │   ├── CapacidadesUsuario.java (NEW)
│   │   │   ├── OpcionesListado.java (NEW)
│   │   │   ├── Carpeta.java (EXISTING)
│   │   │   └── Documento.java (EXISTING)
│   │   ├── repository/
│   │   │   ├── ICarpetaRepository.java (MODIFY)
│   │   │   ├── IDocumentoRepository.java (MODIFY)
│   │   └── exception/
│   │       ├── CarpetaRaizNoEncontradaException.java (NEW)
│   ├── application/
│   │   ├── service/
│   │   │   └── CarpetaContenidoService.java (NEW)
│   │   ├── dto/
│   │   │   ├── ContenidoCarpetaDTO.java (NEW)
│   │   │   ├── CarpetaItemDTO.java (NEW)
│   │   │   ├── DocumentoItemDTO.java (NEW)
│   │   │   └── UsuarioResumenDTO.java (NEW)
│   │   └── mapper/
│   │       └── ContenidoCarpetaMapper.java (NEW)
│   └── infrastructure/
│       ├── adapter/
│       │   ├── controller/
│       │   │   └── CarpetaContenidoController.java (NEW)
│       │   ├── persistence/
│       │   │   ├── CarpetaJpaRepository.java (MODIFY)
│       │   │   └── DocumentoJpaRepository.java (MODIFY)
│       │   └── exception/
│       │       └── GlobalExceptionHandler.java (MODIFY)
│       └── security/
│           └── SecurityContext.java (EXISTING - para extraer usuario/org)
├── src/main/resources/db/migration/
│   └── V008__indices_listado_contenido.sql (NEW)
└── src/test/java/com/docflow/documentcore/
    ├── application/service/
    │   └── CarpetaContenidoServiceTest.java (NEW)
    └── infrastructure/adapter/controller/
        └── CarpetaContenidoControllerIntegrationTest.java (NEW)
```

---

## Implementation Steps

### **Step 1: Create Domain Value Objects**

**Files**: 
- `src/main/java/com/docflow/documentcore/domain/model/ContenidoCarpeta.java`
- `src/main/java/com/docflow/documentcore/domain/model/CarpetaItem.java`
- `src/main/java/com/docflow/documentcore/domain/model/DocumentoItem.java`
- `src/main/java/com/docflow/documentcore/domain/model/CapacidadesUsuario.java`
- `src/main/java/com/docflow/documentcore/domain/model/OpcionesListado.java`

**Action**: Definir value objects inmutables que representen el contenido de una carpeta y opciones de listado

**Implementation Steps**:

1. **CapacidadesUsuario.java** - Record con flags de capacidades:
   - Campos: `puedeLeer` (boolean), `puedeEscribir` (boolean), `puedeAdministrar` (boolean), `puedeDescargar` (boolean)
   - Validación: En constructor compacto, si puedeLeer=false lanzar excepción (invariante)
   - Javadoc: Explicar que puedeLeer siempre true para items listados (filtrado previo)

2. **CarpetaItem.java** - Record con datos de subcarpeta:
   - Campos: `id` (Long), `nombre` (String), `descripcion` (String, nullable), `fechaCreacion` (LocalDateTime), `fechaModificacion` (LocalDateTime), `numSubcarpetas` (int), `numDocumentos` (int), `capacidades` (CapacidadesUsuario)
   - Validación: nombre no nulo/vacío, id > 0

3. **DocumentoItem.java** - Record con datos de documento:
   - Campos: `id` (Long), `nombre` (String), `extension` (String), `tamanioBytes` (Long), `versionActual` (String), `fechaCreacion` (LocalDateTime), `fechaModificacion` (LocalDateTime), `creadoPor` (UsuarioResumen), `capacidades` (CapacidadesUsuario)
   - Validación: nombre no nulo/vacío, id > 0, tamanioBytes >= 0

4. **ContenidoCarpeta.java** - Record agregador:
   - Campos: `subcarpetas` (List<CarpetaItem>), `documentos` (List<DocumentoItem>), `totalSubcarpetas` (int), `totalDocumentos` (int), `paginaActual` (int), `totalPaginas` (int)
   - Validación: Todas las colecciones no nulas (crear como unmodifiable), paginación >= 1, totalPaginas = ceil(max(totalSub, totalDoc) / tamanioPagina)

5. **OpcionesListado.java** - Record con opciones:
   - Campos: `pagina` (int), `tamanio` (int), `campoOrden` (String), `direccion` (Sort.Direction)
   - Validación en constructor compacto:
     - pagina >= 1, sino lanzar IllegalArgumentException("Página debe ser >= 1")
     - tamanio >= 1 && tamanio <= 100, sino IllegalArgumentException("Tamaño debe estar entre 1 y 100")
     - campoOrden: validar que esté en conjunto permitido ["nombre", "fecha_creacion", "fecha_modificacion"], sino "nombre"
     - direccion: no nulo, sino Sort.Direction.ASC

**Dependencies**:
```java
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import org.springframework.data.domain.Sort;
```

**Implementation Notes**:
- Usar Java 21 records para inmutabilidad
- Compact constructor para validaciones
- Javadoc debe explicar invariantes
- UsuarioResumen es record existente (o crear si no existe)

---

### **Step 2: Create Domain Exception Classes**

**File**: `src/main/java/com/docflow/documentcore/domain/exception/CarpetaRaizNoEncontradaException.java`

**Action**: Definir excepción de dominio para caso de carpeta raíz inexistente

**Implementation Steps**:

1. Crear clase que extienda `DomainException` (existente):
   ```java
   public class CarpetaRaizNoEncontradaException extends DomainException {
       public CarpetaRaizNoEncontradaException(Long organizacionId) {
           super(
               "CARPETA_RAIZ_NO_ENCONTRADA",
               String.format("No existe carpeta raíz para organización %d", organizacionId),
               "DOC-404"
           );
       }
   }
   ```

2. Verificar que `DomainException` base tiene estructura:
   - Constructor con (String codigo, String mensaje, String codigoHttp)
   - Getters para codigo, mensaje, codigoHttp
   - Extensión de `RuntimeException`

**Notes**: 
- Otras excepciones (`CarpetaNotFoundException`, `AccesoDenegadoException`) ya deben existir
- Si no existen, crearlas con mismo patrón

---

### **Step 3: Extend Domain Repository Interfaces**

**Files**:
- `src/main/java/com/docflow/documentcore/domain/repository/ICarpetaRepository.java`
- `src/main/java/com/docflow/documentcore/domain/repository/IDocumentoRepository.java`

**Action**: Agregar métodos de contrato para obtener contenido visible filtrado por permisos

**Implementation Steps**:

1. **ICarpetaRepository** - Agregar métodos:

   ```java
   /**
    * Obtiene subcarpetas visibles de una carpeta para un usuario.
    * Incluye solo carpetas NO eliminadas.
    */
   List<Carpeta> obtenerSubcarpetasVisibles(
       Long carpetaId,
       Long usuarioId,
       Long organizacionId,
       Pageable pageable
   );
   
   /**
    * Cuenta subcarpetas visibles para el usuario.
    */
   int contarSubcarpetasVisibles(
       Long carpetaId,
       Long usuarioId,
       Long organizacionId
   );
   
   /**
    * Encuentra la carpeta raíz de una organización.
    */
   Optional<Carpeta> findRaiz(Long organizacionId);
   
   /**
    * Encuentra carpeta por ID verificando pertenencia a organizacion.
    */
   Optional<Carpeta> findById(Long id, Long organizacionId);
   ```

2. **IDocumentoRepository** - Agregar métodos:

   ```java
   /**
    * Obtiene documentos visibles en una carpeta para un usuario.
    * Considera: ACL documento (precedencia) > ACL carpeta (herencia)
    * Incluye solo documentos NO eliminados.
    */
   List<Documento> obtenerDocumentosVisibles(
       Long carpetaId,
       Long usuarioId,
       Long organizacionId,
       Pageable pageable
   );
   
   /**
    * Cuenta documentos visibles en la carpeta para el usuario.
    */
   int contarDocumentosVisibles(
       Long carpetaId,
       Long usuarioId,
       Long organizacionId
   );
   ```

**Dependencies**: 
- `org.springframework.data.domain.Pageable`
- `java.util.Optional`

**Notes**: 
- No implementar en esta etapa (Step 8 lo hace)
- Solo definir contratos (métodos abstractos)

---

### **Step 4: Create Domain Service Interface**

**File**: Verificar/usar existente `src/main/java/com/docflow/documentcore/domain/service/IEvaluadorPermisos.java`

**Action**: Validar que servicio de evaluación de permisos existe con método necesario

**Implementation Steps**:

1. Verificar que `IEvaluadorPermisos` tiene método:
   ```java
   boolean tieneAcceso(
       Long usuarioId,
       Long recursoId,
       TipoRecurso tipoRecurso,
       NivelAcceso nivelRequerido,
       Long organizacionId
   );
   ```

2. Verificar enums: `TipoRecurso` (CARPETA, DOCUMENTO), `NivelAcceso` (LECTURA, ESCRITURA, ADMINISTRACION)

3. Si faltan, crear estos tipos en domain/model/

**Notes**: Este servicio es consumido en Step 5

---

### **Step 5: Create Application Service (CarpetaContenidoService)**

**File**: `src/main/java/com/docflow/documentcore/application/service/CarpetaContenidoService.java`

**Action**: Implementar lógica de negocio principal para listado de contenido

**Function Signatures**:

```java
@Service
@Transactional(readOnly = true)
public class CarpetaContenidoService {
    
    public ContenidoCarpeta listarContenido(
        Long carpetaId,
        Long usuarioId,
        Long organizacionId,
        OpcionesListado opciones
    );
    
    public ContenidoCarpeta listarContenidoRaiz(
        Long usuarioId,
        Long organizacionId,
        OpcionesListado opciones
    );
}
```

**Implementation Steps**:

1. **Constructor e inyecciones**:
   - Inyectar: `ICarpetaRepository`, `IDocumentoRepository`, `IEvaluadorPermisos`
   - Usar inyección por constructor (preferencia del proyecto)

2. **Método listarContenido()**:
   
   Paso 1: Validar que carpeta existe
   ```
   - Carpeta carpeta = carpetaRepository.findById(carpetaId, organizacionId)
     .orElseThrow(() -> new CarpetaNotFoundException(carpetaId))
   ```
   
   Paso 2: Validar permiso LECTURA en carpeta contenedora
   ```
   - boolean tieneAcceso = evaluadorPermisos.tieneAcceso(
       usuarioId, carpetaId, TipoRecurso.CARPETA, 
       NivelAcceso.LECTURA, organizacionId)
   - Si !tieneAcceso: throw new AccesoDenegadoException(...)
   ```
   
   Paso 3: Crear Pageable
   ```
   - PageRequest pageRequest = PageRequest.of(
       opciones.pagina() - 1,  // Spring es 0-indexed
       opciones.tamanio(),
       Sort.by(opciones.direccion(), opciones.campoOrden())
     )
   ```
   
   Paso 4: Obtener subcarpetas visibles
   ```
   - List<Carpeta> subcarpetas = carpetaRepository
       .obtenerSubcarpetasVisibles(carpetaId, usuarioId, 
         organizacionId, pageRequest)
   - int totalSubcarpetas = carpetaRepository
       .contarSubcarpetasVisibles(carpetaId, usuarioId, organizacionId)
   ```
   
   Paso 5: Obtener documentos visibles
   ```
   - List<Documento> documentos = documentoRepository
       .obtenerDocumentosVisibles(carpetaId, usuarioId, 
         organizacionId, pageRequest)
   - int totalDocumentos = documentoRepository
       .contarDocumentosVisibles(carpetaId, usuarioId, organizacionId)
   ```
   
   Paso 6: Enriquecer con capacidades (evaluación en lote)
   ```
   - List<CarpetaItem> itemsCarpetas = subcarpetas.stream()
       .map(carpeta -> {
         boolean puedeEscribir = evaluadorPermisos.tieneAcceso(..., ESCRITURA, ...)
         boolean puedeAdministrar = evaluadorPermisos.tieneAcceso(..., ADMINISTRACION, ...)
         return new CarpetaItem(
           carpeta.getId(), carpeta.getNombre(), ..., 
           new CapacidadesUsuario(true, puedeEscribir, 
             puedeAdministrar, false))
       })
       .toList()
   - Análogo para documentos
   ```
   
   Paso 7: Construir respuesta
   ```
   - int totalPaginas = (int) Math.ceil(
       (double) Math.max(totalSubcarpetas, totalDocumentos) 
       / opciones.tamanio())
   - return new ContenidoCarpeta(itemsCarpetas, itemsDocumentos,
       totalSubcarpetas, totalDocumentos, opciones.pagina(), 
       totalPaginas)
   ```

3. **Método listarContenidoRaiz()**:
   ```
   - Carpeta raiz = carpetaRepository.findRaiz(organizacionId)
     .orElseThrow(() -> new CarpetaRaizNoEncontradaException(organizacionId))
   - return listarContenido(raiz.getId(), usuarioId, organizacionId, opciones)
   ```

4. **Métodos privados de enriquecimiento**:
   - `enriquecerCapacidadesCarpetas(List<Carpeta>, Long, Long): List<CarpetaItem>`
   - `enriquecerCapacidadesDocumentos(List<Documento>, Long, Long): List<DocumentoItem>`

**Dependencies**:
```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.docflow.documentcore.domain.model.*;
import com.docflow.documentcore.domain.repository.*;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
```

**Implementation Notes**:
- `@Transactional(readOnly = true)` optimiza para consultas
- Evaluación lazy de capacidades solo cuando sea necesario
- Logging en niveles INFO (success), WARN (acceso denegado), DEBUG (metrics)

---

### **Step 6: Create Application DTOs and Mapper**

**Files**:
- `src/main/java/com/docflow/documentcore/application/dto/ContenidoCarpetaDTO.java`
- `src/main/java/com/docflow/documentcore/application/dto/CarpetaItemDTO.java`
- `src/main/java/com/docflow/documentcore/application/dto/DocumentoItemDTO.java`
- `src/main/java/com/docflow/documentcore/application/dto/UsuarioResumenDTO.java`
- `src/main/java/com/docflow/documentcore/application/mapper/ContenidoCarpetaMapper.java`

**Action**: Definir DTOs para serialización JSON y mapper

**Implementation Steps**:

1. **ContenidoCarpetaDTO.java** - Record con anotaciones:
   ```java
   public record ContenidoCarpetaDTO(
       @Schema(description = "Lista de subcarpetas")
       List<CarpetaItemDTO> subcarpetas,
       
       @Schema(description = "Lista de documentos")
       List<DocumentoItemDTO> documentos,
       
       @JsonProperty("total_subcarpetas")
       @Schema(description = "Total de subcarpetas visibles")
       int totalSubcarpetas,
       
       @JsonProperty("total_documentos")
       int totalDocumentos,
       
       @JsonProperty("pagina_actual")
       int paginaActual,
       
       @JsonProperty("elementos_por_pagina")
       int elementosPorPagina,
       
       @JsonProperty("total_paginas")
       int totalPaginas
   ) {}
   ```

2. **CarpetaItemDTO.java** - Similar a CarpetaItem pero con anotaciones JSON:
   - Campos con @JsonProperty para snake_case
   - @Schema para Swagger documentation

3. **DocumentoItemDTO.java** - Similar a DocumentoItem con anotaciones

4. **UsuarioResumenDTO.java** - Record simple con id y nombre_completo

5. **ContenidoCarpetaMapper.java** - Interfaz MapStruct:
   ```java
   @Mapper(componentModel = "spring", 
       unmappedTargetPolicy = ReportingPolicy.IGNORE)
   public interface ContenidoCarpetaMapper {
       ContenidoCarpetaDTO toDto(ContenidoCarpeta contenido);
       CarpetaItemDTO toItemDto(CarpetaItem item);
       DocumentoItemDTO toItemDto(DocumentoItem item);
       UsuarioResumenDTO toResumenDto(UsuarioResumen usuario);
   }
   ```

**Dependencies**:
```java
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.mapstruct.*;
```

**Implementation Notes**:
- @JsonProperty para mapeo a snake_case en JSON
- Anotaciones @Schema para documentación Swagger
- MapStruct genera automáticamente mappers

---

### **Step 7: Create REST Controller**

**File**: `src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/CarpetaContenidoController.java`

**Action**: Exponer endpoints REST para listado de contenido

**Function Signatures**:

```java
@RestController
@RequestMapping("/api/carpetas")
@Tag(name = "Carpetas - Contenido")
@SecurityRequirement(name = "bearer-jwt")
public class CarpetaContenidoController {
    
    @GetMapping("/{id}/contenido")
    public ResponseEntity<ContenidoCarpetaDTO> listarContenido(
        @PathVariable Long id,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "nombre") String ordenar_por,
        @RequestParam(defaultValue = "asc") String direccion,
        @RequestHeader(value = "X-Organization-Id", required = false) Long organizacionId,
        @RequestHeader(value = "X-User-Id", required = false) Long usuarioId
    );
    
    @GetMapping("/raiz/contenido")
    public ResponseEntity<ContenidoCarpetaDTO> listarContenidoRaiz(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "nombre") String ordenar_por,
        @RequestParam(defaultValue = "asc") String direccion,
        @RequestHeader(value = "X-Organization-Id", required = false) Long organizacionId,
        @RequestHeader(value = "X-User-Id", required = false) Long usuarioId
    );
}
```

**Implementation Steps**:

1. **Inyecciones**:
   - `CarpetaContenidoService`
   - `ContenidoCarpetaMapper`

2. **Método listarContenido()**:
   
   Paso 1: Validar query parameters
   ```
   - if (page < 1) page = 1
   - if (size < 1) size = 1; if (size > 100) size = 100
   - si ordenar_por no válido, usar "nombre"
   - if (!["asc", "desc"].contains(direccion)) direccion = "asc"
   ```
   
   Paso 2: Crear OpcionesListado
   ```
   - OpcionesListado opciones = new OpcionesListado(
       page, size, ordenar_por, 
       Sort.Direction.fromString(direccion.toUpperCase()))
   ```
   
   Paso 3: Llamar servicio
   ```
   - ContenidoCarpeta contenido = service.listarContenido(
       id, usuarioId, organizacionId, opciones)
   ```
   
   Paso 4: Mapear y retornar
   ```
   - ContenidoCarpetaDTO response = mapper.toDto(contenido)
   - return ResponseEntity.ok(response)
   ```

3. **Método listarContenidoRaiz()**:
   - Análogo, pero sin @PathVariable
   - Llamar `service.listarContenidoRaiz(...)`

4. **Anotaciones Swagger**:
   - @Operation con summary y description
   - @ApiResponses para 200/403/404
   - @Parameter para query params
   - @SecurityRequirement(name = "bearer-jwt")

**Dependencies**:
```java
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Sort;
```

**Implementation Notes**:
- Headers X-Organization-Id, X-User-Id pueden venir del JWT (extractar en filtro anterior)
- Sin validación explícita de headers (asumir que existen y son válidos)
- Logging en entrada: `log.debug("GET /api/carpetas/{}/contenido", id)`

---

### **Step 8: Implement JPA Repository Methods**

**Files**:
- `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/CarpetaJpaRepository.java`
- `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/DocumentoJpaRepository.java`

**Action**: Implementar queries nativas SQL con filtrado por permisos

**Implementation Steps**:

1. **CarpetaJpaRepository** - Extender `JpaRepository<Carpeta, Long>` e implementar `ICarpetaRepository`:

   ```java
   @Repository
   public interface CarpetaJpaRepository 
       extends JpaRepository<Carpeta, Long>, ICarpetaRepository {
       
       @Query(value = """
           SELECT DISTINCT c.*
           FROM carpeta c
           WHERE c.carpeta_padre_id = :carpetaId
             AND c.organizacion_id = :organizacionId
             AND c.fecha_eliminacion IS NULL
             AND EXISTS (
                 -- Permiso directo en la carpeta
                 SELECT 1
                 FROM permiso_carpeta_usuario pcu
                 WHERE pcu.carpeta_id = c.id
                   AND pcu.usuario_id = :usuarioId
                   AND pcu.organizacion_id = :organizacionId
               UNION
                 -- Permiso heredado recursivo
                 SELECT 1
                 FROM permiso_carpeta_usuario pcu_heredado
                 INNER JOIN (
                     -- Obtener ruta de ancestros
                     WITH RECURSIVE ancestros AS (
                         SELECT id, carpeta_padre_id, 1 AS nivel
                         FROM carpeta
                         WHERE id = c.id
                       UNION ALL
                         SELECT c2.id, c2.carpeta_padre_id, a.nivel + 1
                         FROM carpeta c2
                         INNER JOIN ancestros a 
                           ON c2.id = a.carpeta_padre_id
                         WHERE a.carpeta_padre_id IS NOT NULL
                     )
                     SELECT id FROM ancestros WHERE nivel > 1
                 ) ancestros ON pcu_heredado.carpeta_id = ancestros.id
                 WHERE pcu_heredado.usuario_id = :usuarioId
                   AND pcu_heredado.organizacion_id = :organizacionId
                   AND pcu_heredado.recursivo = true
             )
           ORDER BY c.nombre
           """, nativeQuery = true)
       List<Carpeta> obtenerSubcarpetasVisibles(
           @Param("carpetaId") Long carpetaId,
           @Param("usuarioId") Long usuarioId,
           @Param("organizacionId") Long organizacionId,
           Pageable pageable
       );
       
       @Query(value = """
           SELECT COUNT(DISTINCT c.id)
           FROM carpeta c
           WHERE c.carpeta_padre_id = :carpetaId
             AND c.organizacion_id = :organizacionId
             AND c.fecha_eliminacion IS NULL
             AND EXISTS (
                 SELECT 1 FROM permiso_carpeta_usuario pcu
                 WHERE pcu.carpeta_id = c.id
                   AND pcu.usuario_id = :usuarioId
                   AND pcu.organizacion_id = :organizacionId
               UNION
                 SELECT 1
                 FROM permiso_carpeta_usuario pcu_heredado
                 INNER JOIN (
                     WITH RECURSIVE ancestros AS (...)
                     SELECT id FROM ancestros WHERE nivel > 1
                 ) ancestros ON pcu_heredado.carpeta_id = ancestros.id
                 WHERE pcu_heredado.usuario_id = :usuarioId
                   AND pcu_heredado.organizacion_id = :organizacionId
                   AND pcu_heredado.recursivo = true
             )
           """, nativeQuery = true)
       int contarSubcarpetasVisibles(
           @Param("carpetaId") Long carpetaId,
           @Param("usuarioId") Long usuarioId,
           @Param("organizacionId") Long organizacionId
       );
       
       @Query("SELECT c FROM Carpeta c WHERE c.carpetaPadreId IS NULL "
           + "AND c.organizacionId = :organizacionId")
       Optional<Carpeta> findRaiz(@Param("organizacionId") Long organizacionId);
       
       @Query("SELECT c FROM Carpeta c WHERE c.id = :id AND c.organizacionId = :organizacionId")
       Optional<Carpeta> findById(@Param("id") Long id, @Param("organizacionId") Long organizacionId);
   }
   ```

2. **DocumentoJpaRepository** - Implementar con precedencia documento > carpeta:

   ```java
   @Repository
   public interface DocumentoJpaRepository 
       extends JpaRepository<Documento, Long>, IDocumentoRepository {
       
       @Query(value = """
           SELECT DISTINCT d.*
           FROM documento d
           WHERE d.carpeta_id = :carpetaId
             AND d.organizacion_id = :organizacionId
             AND d.fecha_eliminacion IS NULL
             AND (
                 -- CASO 1: Permiso explícito en documento (máxima prioridad)
                 EXISTS (
                     SELECT 1 FROM permiso_documento_usuario pdu
                     WHERE pdu.documento_id = d.id
                       AND pdu.usuario_id = :usuarioId
                       AND pdu.organizacion_id = :organizacionId
                 )
                 OR
                 -- CASO 2: Sin permiso documento -> heredar de carpeta
                 (
                     NOT EXISTS (
                         SELECT 1 FROM permiso_documento_usuario pdu2
                         WHERE pdu2.documento_id = d.id
                           AND pdu2.organizacion_id = :organizacionId
                     )
                     AND (
                         -- Permiso directo en carpeta contenedora
                         EXISTS (
                             SELECT 1 FROM permiso_carpeta_usuario pcu
                             WHERE pcu.carpeta_id = :carpetaId
                               AND pcu.usuario_id = :usuarioId
                               AND pcu.organizacion_id = :organizacionId
                         )
                         OR
                         -- Permiso heredado recursivo
                         EXISTS (
                             SELECT 1
                             FROM permiso_carpeta_usuario pcu_heredado
                             INNER JOIN (
                                 WITH RECURSIVE ancestros AS (...)
                                 SELECT id FROM ancestros WHERE nivel > 1
                             ) ancestros ON pcu_heredado.carpeta_id = ancestros.id
                             WHERE pcu_heredado.usuario_id = :usuarioId
                               AND pcu_heredado.organizacion_id = :organizacionId
                               AND pcu_heredado.recursivo = true
                         )
                     )
                 )
             )
           ORDER BY d.nombre
           """, nativeQuery = true)
       List<Documento> obtenerDocumentosVisibles(
           @Param("carpetaId") Long carpetaId,
           @Param("usuarioId") Long usuarioId,
           @Param("organizacionId") Long organizacionId,
           Pageable pageable
       );
       
       // Query análoga para count...
       int contarDocumentosVisibles(
           @Param("carpetaId") Long carpetaId,
           @Param("usuarioId") Long usuarioId,
           @Param("organizacionId") Long organizacionId
       );
   }
   ```

**Dependencies**:
```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
```

**Implementation Notes**:
- `nativeQuery = true` para usar SQL puro (no JPQL)
- CTEs recursivos: `WITH RECURSIVE ancestros AS (...)`
- `EXISTS` es más eficiente que COUNT para verificar existencia
- `UNION` combina permisos directos + heredados
- `DISTINCT` para evitar duplicados por JOIN múltiples

---

### **Step 9: Create Database Migration for Indices**

**File**: `src/main/resources/db/migration/V008__indices_listado_contenido.sql`

**Action**: Crear índices compuestos optimizados para filtrado de permisos

**Implementation Steps**:

```sql
-- Índice para búsqueda de permisos directos en carpeta
CREATE INDEX idx_permiso_carpeta_usuario_carpeta_usuario_org
ON permiso_carpeta_usuario (carpeta_id, usuario_id, organizacion_id);

-- Índice para búsqueda de permisos recursivos
CREATE INDEX idx_permiso_carpeta_usuario_recursivo_org
ON permiso_carpeta_usuario (usuario_id, organizacion_id, recursivo)
WHERE recursivo = true;

-- Índice para búsqueda de permisos en documento
CREATE INDEX idx_permiso_documento_usuario_doc_usuario_org
ON permiso_documento_usuario (documento_id, usuario_id, organizacion_id);

-- Índice para listado de subcarpetas por padre (con soft-delete)
CREATE INDEX idx_carpeta_padre_org_eliminacion
ON carpeta (carpeta_padre_id, organizacion_id)
WHERE fecha_eliminacion IS NULL;

-- Índice para listado de documentos por carpeta (con soft-delete)
CREATE INDEX idx_documento_carpeta_org_eliminacion
ON documento (carpeta_id, organizacion_id)
WHERE fecha_eliminacion IS NULL;

-- Índice para búsqueda de carpeta raíz
CREATE INDEX idx_carpeta_padre_null_org
ON carpeta (carpeta_padre_id, organizacion_id)
WHERE carpeta_padre_id IS NULL;
```

**Implementation Notes**:
- Índices compuestos siguen orden: WHERE condiciones más restrictivas primero
- Partial indices (WHERE fecha_eliminacion IS NULL) reducen tamaño y mejoran selectividad
- Ejecutar `EXPLAIN ANALYZE` después de aplicar para validar uso

---

### **Step 10: Write Unit Tests for Service**

**File**: `src/test/java/com/docflow/documentcore/application/service/CarpetaContenidoServiceTest.java`

**Action**: Implementar tests unitarios para lógica de negocio

**Implementation Steps**:

1. **Setup Test Class**:
   ```java
   @ExtendWith(MockitoExtension.class)
   @DisplayName("CarpetaContenidoService - Tests Unitarios")
   class CarpetaContenidoServiceTest {
       
       @Mock
       private ICarpetaRepository carpetaRepository;
       
       @Mock
       private IDocumentoRepository documentoRepository;
       
       @Mock
       private IEvaluadorPermisos evaluadorPermisos;
       
       @InjectMocks
       private CarpetaContenidoService service;
   }
   ```

2. **Test Cases**:
   
   a) **Successful Path**:
   - `should_ListContent_When_UserHasReadPermission()`
     - Dado: usuario con LECTURA en carpeta
     - Cuando: llama listarContenido
     - Entonces: retorna contenido filtrado con capacidades
   
   b) **Permission Errors**:
   - `should_ThrowAccesoDenegado_When_UserLacksReadPermission()`
   - `should_ThrowCarpetaNotFound_When_FolderDoesNotExist()`
   
   c) **Edge Cases**:
   - `should_ReturnEmptyLists_When_FolderIsEmpty()`
   - `should_CalculatePaginationCorrectly()`
   - `should_EnrichCapabilities_When_MultipleItems()`

3. **Test Pattern** (AAA):
   ```
   // GIVEN
   - Setup mocks con datos de prueba
   
   // WHEN
   - Llamar método bajo test
   
   // THEN
   - Verificar resultado con assertions
   - Verificar interacciones de mocks
   ```

4. **Assertions**:
   - Usar AssertJ: `assertThat(result).isNotNull()`, `hasSize()`, etc.
   - Verificar logs si es crítico

**Dependencies**:
```java
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
```

**Implementation Notes**:
- Cobertura mínima 90%
- Mocks en lugar de integraciones reales
- No hacer queries a BD (eso es integración, Step 11)

---

### **Step 11: Write Integration Tests for Controller**

**File**: `src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/CarpetaContenidoControllerIntegrationTest.java`

**Action**: Tests de integración HTTP completos con BD real

**Implementation Steps**:

1. **Setup Test Class**:
   ```java
   @SpringBootTest
   @AutoConfigureMockMvc
   @Transactional
   @DisplayName("GET /api/carpetas/{id}/contenido - Tests de Integración")
   class CarpetaContenidoControllerIntegrationTest {
       
       @Autowired
       private MockMvc mockMvc;
       
       private static final String BASE_URL = "/api/carpetas";
   }
   ```

2. **Test Cases** (por Scenario de criterios de aceptación):
   
   a) **Scenario 1**: Usuario con permisos ve contenido filtrado
   - `should_Return200WithFilteredContent_When_UserHasReadPermission()`
   - `@Sql("/test-data/carpetas-con-permisos.sql")`
   - Verificar: status 200, 2 subcarpetas visibles, 2 documentos
   
   b) **Scenario 2**: Usuario sin permisos recibe 403
   - `should_Return403_When_UserLacksReadPermission()`
   - `@Sql("/test-data/carpetas-sin-permisos.sql")`
   
   c) **Scenario 3**: Carpeta vacía retorna listas vacías
   - `should_Return200WithEmptyLists_When_FolderIsEmpty()`
   
   d) **Scenario 4**: Respeta soft-delete
   - `should_ExcludeSoftDeletedItems()`
   - `@Sql("/test-data/carpetas-con-soft-delete.sql")`

3. **HTTP Assertions**:
   ```java
   mockMvc.perform(get(BASE_URL + "/100/contenido")
       .header("X-Organization-Id", "10")
       .header("X-User-Id", "50")
       .contentType(MediaType.APPLICATION_JSON))
     .andExpect(status().isOk())
     .andExpect(jsonPath("$.subcarpetas", hasSize(2)))
     .andExpect(jsonPath("$.subcarpetas[0].puede_escribir").isBoolean());
   ```

4. **Test Data SQL Scripts**:
   - `src/test/resources/test-data/carpetas-con-permisos.sql`
   - `src/test/resources/test-data/carpetas-sin-permisos.sql`
   - `src/test/resources/test-data/carpetas-con-soft-delete.sql`
   - `src/test/resources/test-data/carpeta-raiz.sql`

**Dependencies**:
```java
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
```

**Implementation Notes**:
- Transactional auto-rollback entre tests
- @Sql carga datos antes del test
- MockMvc no requiere servidor HTTP real
- Verificar precedencia documento > carpeta en tests

---

### **Step 12: Update Global Exception Handler**

**File**: `src/main/java/com/docflow/documentcore/infrastructure/adapter/exception/GlobalExceptionHandler.java`

**Action**: Agregar handlers para excepciones de US-FOLDER-002

**Implementation Steps**:

1. Agregar handlers si no existen:
   ```java
   @ExceptionHandler(CarpetaNotFoundException.class)
   public ResponseEntity<Map<String, Object>> handleCarpetaNotFound(
       CarpetaNotFoundException ex) {
       return buildErrorResponse(HttpStatus.NOT_FOUND, ...);
   }
   
   @ExceptionHandler(AccesoDenegadoException.class)
   public ResponseEntity<Map<String, Object>> handleAccesoDenegado(
       AccesoDenegadoException ex) {
       return buildErrorResponse(HttpStatus.FORBIDDEN, ...);
   }
   
   @ExceptionHandler(CarpetaRaizNoEncontradaException.class)
   public ResponseEntity<Map<String, Object>> handleCarpetaRaizNotFound(
       CarpetaRaizNoEncontradaException ex) {
       return buildErrorResponse(HttpStatus.NOT_FOUND, ...);
   }
   ```

2. Response format:
   ```json
   {
       "error": "COD_ERROR",
       "mensaje": "descripción",
       "codigo": "DOC-XXX",
       "timestamp": "2024-02-04T10:30:00Z"
   }
   ```

---

### **Step 13: Update Technical Documentation**

**Action**: Revisar y actualizar documentación técnica según cambios implementados

**Implementation Steps**:

1. **Review Changes**:
   - Nuevos endpoints: GET /api/carpetas/{id}/contenido, GET /api/carpetas/raiz/contenido
   - Nuevos DTOs con estructura JSON
   - Nuevos índices de BD
   - Nuevas excepciones de dominio

2. **Update Files**:

   a) **backend/document-core/README.md**:
      - Agregar sección "Listado de Contenido de Carpetas (US-FOLDER-002)"
      - Explicar filtrado por permisos (directos + heredados + precedencia)
      - Incluir ejemplos de uso con curl/postman
      - Documentar errores comunes

   b) **ai-specs/specs/api-spec.yml** (OpenAPI):
      - Agregar paths: `/carpetas/{id}/contenido`, `/carpetas/raiz/contenido`
      - Definir request/response schemas
      - Mapear status codes (200, 403, 404)
      - Incluir ejemplos JSON
   
   c) **ai-specs/specs/data-model.md**:
      - Documentar índices creados
      - Explicar relaciones permiso_carpeta_usuario ↔ permiso_documento_usuario
   
   d) **ai-specs/specs/backend-standards.mdc**:
      - Agregar pattern de "Servicios de Listado Filtrado"
      - Documentar uso de CTEs recursivos para herencia
      - Pattern de evaluación en lote de capacidades

3. **Verify Documentation**:
   - Confirmar que todos los endpoints están documentados
   - Revisar ejemplos de request/response
   - Validar que estructura sigue convenciones del proyecto

4. **Report Updates**:
   - Listar archivos actualizados
   - Breve descripción de cambios en cada archivo

**References**:
- `ai-specs/specs/documentation-standards.mdc`
- `CONTRIBUTING.md` (si existe)

---

### **Step 14: Run All Tests and Verify**

**Action**: Ejecutar tests para validar implementación completa

**Implementation Steps**:

1. **Run Unit Tests**:
   ```bash
   cd backend/document-core
   mvn clean test -Dtest=CarpetaContenidoServiceTest
   ```
   - Verificar: todos los tests pasan ✓
   - Cobertura: >= 90%

2. **Run Integration Tests**:
   ```bash
   mvn clean test -Dtest=CarpetaContenidoControllerIntegrationTest
   ```
   - Verificar: todos los tests pasan ✓
   - Base de datos se resuelve (H2/TestContainers)

3. **Run All Tests** (incluyendo existentes):
   ```bash
   mvn clean test
   ```
   - No hay regresiones

4. **Build Project**:
   ```bash
   mvn clean package
   ```
   - Sin errores de compilación

5. **Static Analysis** (si está configurado):
   ```bash
   mvn checkstyle:check
   mvn findbugs:check  # O SpotBugs
   ```

6. **Verify Indices** (manual en staging):
   ```sql
   EXPLAIN ANALYZE SELECT * FROM carpeta c WHERE c.carpeta_padre_id = 100 AND ...;
   -- Verificar uso de idx_carpeta_padre_org_eliminacion
   ```

---

## Implementation Order

2. ✅ Step 1: Create Domain Value Objects
3. ✅ Step 2: Create Domain Exception Classes
4. ✅ Step 3: Extend Domain Repository Interfaces
5. ✅ Step 4: Create Domain Service Interface (verify existing)
6. ✅ Step 5: Create Application Service (CarpetaContenidoService)
7. ✅ Step 6: Create Application DTOs and Mapper
8. ✅ Step 7: Create REST Controller
9. ✅ Step 8: Implement JPA Repository Methods
10. ✅ Step 9: Create Database Migration for Indices
11. ✅ Step 10: Write Unit Tests for Service
12. ✅ Step 11: Write Integration Tests for Controller
13. ✅ Step 12: Update Global Exception Handler
14. ✅ Step 13: Update Technical Documentation
15. ✅ Step 14: Run All Tests and Verify

---

## Testing Checklist

### Unit Tests
- [ ] `CarpetaContenidoServiceTest` cubre todos los casos
- [ ] Mocking de repositorios y evaluador de permisos
- [ ] Cobertura >= 90% en rama

### Integration Tests
- [ ] `CarpetaContenidoControllerIntegrationTest` valida cada scenario
- [ ] HTTP requests con headers correctos
- [ ] BD en memoria (H2/TestContainers)
- [ ] Transactional rollback entre tests

### Manual Testing (en staging)
- [ ] GET /api/carpetas/100/contenido retorna 200 con contenido
- [ ] GET /api/carpetas/{sin-permisos} retorna 403
- [ ] GET /api/carpetas/999 retorna 404
- [ ] GET /api/carpetas/raiz/contenido funciona
- [ ] Query performance < 500ms (EXPLAIN ANALYZE)
- [ ] Indices están siendo usados

### Code Quality
- [ ] ESLint/Checkstyle pasa sin warnings
- [ ] Tests naming sigue patrón: should_XX_When_YY
- [ ] Javadoc completo en métodos públicos
- [ ] No valores hardcodeados (usar constantes)

---

## Error Response Format

**HTTP 403 Forbidden** (sin permisos):
```json
{
  "error": "SIN_PERMISO_LECTURA",
  "mensaje": "No tienes permisos para ver el contenido de esta carpeta",
  "codigo": "DOC-403",
  "carpeta_id": 100,
  "timestamp": "2024-02-04T10:30:00Z"
}
```

**HTTP 404 Not Found**:
```json
{
  "error": "CARPETA_NO_ENCONTRADA",
  "mensaje": "La carpeta solicitada no existe o no pertenece a tu organización",
  "codigo": "DOC-404",
  "carpeta_id": 999,
  "timestamp": "2024-02-04T10:30:00Z"
}
```

**HTTP 404 Not Found** (carpeta raíz):
```json
{
  "error": "CARPETA_RAIZ_NO_ENCONTRADA",
  "mensaje": "No existe carpeta raíz para organización 10",
  "codigo": "DOC-404",
  "timestamp": "2024-02-04T10:30:00Z"
}
```

---

## Dependencies

### Java Libraries (ya en pom.xml)
- Spring Boot 3.x
- Spring Data JPA
- MapStruct
- JUnit 5
- Mockito
- AssertJ
- Springdoc OpenAPI (Swagger)

### Database
- PostgreSQL 13+ (CTEs recursivos soportados)
- Flyway para migraciones

### Tools
- Maven 3.8+
- Git

---

## Notes

### Business Rules
- Usuario debe tener permiso LECTURA en carpeta para ver su contenido
- Subcarpetas/documentos sin permiso se filtran (no retornar 403)
- Soft-delete: elementos con `fecha_eliminacion != NULL` siempre excluidos
- Multi-tenant: filtro por `organizacion_id` en todas las queries

### Code Principles
- Arquitectura Hexagonal: Domain → Application → Infrastructure
- Clean Code: métodos cortos, nombres significativos
- SOLID: Single Responsibility, Dependency Injection
- DDD: Value Objects, Repositories, Services

### Performance Considerations
- Target: < 500ms para 100 elementos
- Índices compuestos: orden es crítico
- Evaluación lazy de capacidades
- Paginación obligatoria

### Security
- JWT validado en interceptor previo
- X-Organization-Id/X-User-Id en headers (o extraer del JWT)
- No revelar existencia de carpetas sin permisos (404 en lugar de 403 para listados vacíos)
- Queries parametrizadas (JPA maneja prevención de SQL injection)

---

## Next Steps After Implementation

1. **Merge Feature Branch**:
   - Después de aprobación, mergear a `main` o `develop`
   - Eliminar rama feature

2. **Deploy to Staging**:
   - Desplegar en ambiente staging
   - Smoke tests básicos
   - QA team valida criterios de aceptación

3. **Prepare for Production**:
   - Documentación de usuario finalizada
   - Training si es necesario
   - Release notes preparadas

4. **Future Enhancements** (fuera del alcance):
   - Búsqueda/filtrado por nombre
   - Ordenamiento por múltiples campos
   - Cache distribuido (Redis)
   - GraphQL endpoint adicional

---

## Implementation Verification

### Final Checklist

- [ ] **Code Quality**:
  - [ ] Arquitectura Hexagonal seguida
  - [ ] SOLID principles aplicados
  - [ ] Javadoc en métodos públicos
  - [ ] No hardcoded values

- [ ] **Functionality**:
  - [ ] Endpoints exponen correcto
  - [ ] Filtrado de permisos funciona
  - [ ] Paginación correcta
  - [ ] Soft-delete respetado

- [ ] **Testing**:
  - [ ] Unit tests cobertura >= 90%
  - [ ] Integration tests para cada scenario
  - [ ] Todos los tests pasan localmente
  - [ ] Nombres siguien pattern should_XX_When_YY

- [ ] **Integration**:
  - [ ] Funciona con US-ACL-004, US-ACL-005, US-ACL-006
  - [ ] Compatible con estructura jerárquica profunda
  - [ ] Multi-tenant correctamente aislado

- [ ] **Documentation**:
  - [ ] README.md actualizado
  - [ ] OpenAPI spec actualizado
  - [ ] Data model documentation revisada
  - [ ] Backend standards actualizado si aplica

- [ ] **Performance**:
  - [ ] Query time < 500ms (verificado con EXPLAIN)
  - [ ] Índices están siendo utilizados
  - [ ] No N+1 queries

- [ ] **Security**:
  - [ ] JWT/autenticación validada
  - [ ] Autorización (permisos) validada
  - [ ] No SQL injection vulnerable
  - [ ] Multi-tenant aislamiento verificado

---

**Fin del Plan de Implementación Backend - US-FOLDER-002**
