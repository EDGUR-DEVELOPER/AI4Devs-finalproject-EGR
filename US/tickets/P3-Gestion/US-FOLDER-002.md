## P3 — Gestión de carpetas: API + UI mínima

### [US-FOLDER-002] Listar contenido de carpeta (API) con visibilidad por permisos

### Resumen Ejecutivo

**Historia:** Como usuario autenticado, quiero listar el contenido (subcarpetas y documentos) de una carpeta, para navegar y visualizar solo los elementos a los que tengo acceso de lectura según el sistema de permisos ACL.

**Alcance Técnico:** Implementación completa de endpoint REST que retorna el contenido filtrado de una carpeta basándose en permisos directos y heredados (recursivos), con soporte para paginación, ordenamiento y campos de capacidades del usuario sobre cada elemento.

**Dependencias:**
- US-ACL-004 (Herencia recursiva de permisos)
- US-ACL-005 (Permisos explícitos en documentos)
- US-ACL-006 (Regla de precedencia Documento > Carpeta)
- US-FOLDER-001 (Creación de carpetas)

**Entregables principales:**
1. Endpoint `GET /api/carpetas/{id}/contenido` con filtrado por permisos
2. Endpoint auxiliar `GET /api/carpetas/raiz/contenido` para navegación inicial
3. DTOs de respuesta con información de capacidades
4. Servicio de listado con evaluación eficiente de permisos en lote
5. Índices de BD para optimizar consultas de permisos
6. Suite completa de pruebas unitarias e integración

---

### Criterios de Aceptación Detallados

#### Scenario 1: Usuario con LECTURA lista carpeta y ve solo elementos permitidos

**Dado que:**
- Existe una carpeta "Proyectos" (ID: 100, organizacion_id: 10)
- Usuario `ana.garcia@example.com` (ID: 50, organizacion_id: 10)
- Carpeta contiene:
  - Subcarpeta "Marketing" (ID: 101) - Usuario tiene LECTURA directa
  - Subcarpeta "Finanzas" (ID: 102) - Usuario tiene LECTURA heredada (recursivo)
  - Subcarpeta "Legal" (ID: 103) - Usuario SIN permiso
  - Documento "Presentacion.pdf" (ID: 200) - Usuario tiene LECTURA via carpeta
  - Documento "Confidencial.docx" (ID: 201) - Usuario SIN permiso (ACL documento lo restringe)

**Cuando:**
Ana realiza `GET /api/carpetas/100/contenido`

**Entonces:**
1. Respuesta HTTP 200 OK
2. JSON retorna:
   ```json
   {
     "subcarpetas": [
       {
         "id": 101,
         "nombre": "Marketing",
         "fecha_creacion": "2024-01-15T10:30:00Z",
         "puede_escribir": false,
         "puede_administrar": false
       },
       {
         "id": 102,
         "nombre": "Finanzas",
         "fecha_creacion": "2024-01-20T14:00:00Z",
         "puede_escribir": false,
         "puede_administrar": false
       }
     ],
     "documentos": [
       {
         "id": 200,
         "nombre": "Presentacion.pdf",
         "version_actual": "1.0",
         "fecha_modificacion": "2024-02-01T09:15:00Z",
         "puede_escribir": false
       }
     ],
     "total_subcarpetas": 2,
     "total_documentos": 1,
     "pagina_actual": 1,
     "total_paginas": 1
   }
   ```
3. NO aparecen carpeta "Legal" ni documento "Confidencial.docx"
4. Cada elemento incluye flags de capacidad del usuario

#### Scenario 2: Usuario sin LECTURA en carpeta recibe 403

**Dado que:**
- Existe carpeta "Confidencial" (ID: 300, organizacion_id: 10)
- Usuario `carlos.lopez@example.com` (ID: 51, organizacion_id: 10)
- Carlos NO tiene ningún permiso sobre carpeta 300 (ni directo ni heredado)

**Cuando:**
Carlos realiza `GET /api/carpetas/300/contenido`

**Entonces:**
1. Respuesta HTTP 403 Forbidden
2. JSON retorna:
   ```json
   {
     "error": "SIN_PERMISO_LECTURA",
     "mensaje": "No tienes permisos para ver el contenido de esta carpeta",
     "codigo": "DOC-403",
     "timestamp": "2024-02-04T10:30:00Z"
   }
   ```

#### Scenario 3: Carpeta vacía con permisos retorna listas vacías

**Dado que:**
- Carpeta "Nueva Área" (ID: 400) sin contenido
- Usuario tiene LECTURA sobre carpeta 400

**Cuando:**
Realiza `GET /api/carpetas/400/contenido`

**Entonces:**
1. HTTP 200 OK
2. Respuesta:
   ```json
   {
     "subcarpetas": [],
     "documentos": [],
     "total_subcarpetas": 0,
     "total_documentos": 0,
     "pagina_actual": 1,
     "total_paginas": 0
   }
   ```

#### Scenario 4: Listado respeta soft-delete

**Dado que:**
- Carpeta "Archivos" contiene:
  - Subcarpeta "Activa" (fecha_eliminacion: NULL)
  - Subcarpeta "Eliminada" (fecha_eliminacion: 2024-01-30)
- Usuario tiene LECTURA sobre todas

**Cuando:**
Realiza `GET /api/carpetas/{id}/contenido`

**Entonces:**
- Solo aparece subcarpeta "Activa"
- Elementos con `fecha_eliminacion != NULL` se excluyen automáticamente

---

### Especificación Técnica Detallada

#### 1. Endpoints REST

##### **GET /api/carpetas/{id}/contenido**

Retorna subcarpetas y documentos visibles para el usuario autenticado dentro de una carpeta.

**Path Parameters:**
- `id` (Long, required): ID de la carpeta

**Headers:**
- `Authorization: Bearer {JWT}` (required)
- `X-Organization-Id` (Long, extracted from JWT): ID de la organización
- `X-User-Id` (Long, extracted from JWT): ID del usuario autenticado

**Query Parameters:**
```
page        (int, optional, default=1): Número de página
size        (int, optional, default=20, max=100): Elementos por página
ordenar_por (string, optional, default="nombre"): Campo de ordenamiento
            Valores: "nombre", "fecha_creacion", "fecha_modificacion"
direccion   (string, optional, default="asc"): Dirección del orden
            Valores: "asc", "desc"
```

**Response 200 OK:**
```json
{
  "subcarpetas": [
    {
      "id": 101,
      "nombre": "Marketing",
      "descripcion": "Materiales de marketing",
      "fecha_creacion": "2024-01-15T10:30:00Z",
      "fecha_modificacion": "2024-01-20T15:45:00Z",
      "puede_escribir": false,
      "puede_administrar": false,
      "num_subcarpetas": 3,
      "num_documentos": 15
    }
  ],
  "documentos": [
    {
      "id": 200,
      "nombre": "Presentacion.pdf",
      "extension": "pdf",
      "tamanio_bytes": 2048576,
      "version_actual": "1.2",
      "fecha_creacion": "2024-01-10T08:00:00Z",
      "fecha_modificacion": "2024-02-01T09:15:00Z",
      "creado_por": {
        "id": 25,
        "nombre_completo": "Juan Pérez"
      },
      "puede_escribir": true,
      "puede_descargar": true,
      "puede_administrar": false
    }
  ],
  "total_subcarpetas": 25,
  "total_documentos": 47,
  "pagina_actual": 1,
  "elementos_por_pagina": 20,
  "total_paginas": 3
}
```

**Response 403 Forbidden:**
```json
{
  "error": "SIN_PERMISO_LECTURA",
  "mensaje": "No tienes permisos para ver el contenido de esta carpeta",
  "codigo": "DOC-403",
  "carpeta_id": 100,
  "timestamp": "2024-02-04T10:30:00Z"
}
```

**Response 404 Not Found:**
```json
{
  "error": "CARPETA_NO_ENCONTRADA",
  "mensaje": "La carpeta solicitada no existe o no pertenece a tu organización",
  "codigo": "DOC-404",
  "carpeta_id": 999,
  "timestamp": "2024-02-04T10:30:00Z"
}
```

##### **GET /api/carpetas/raiz/contenido**

Endpoint de conveniencia para obtener el contenido de la carpeta raíz de la organización.

**Comportamiento:**
1. Identifica carpeta raíz: `WHERE carpeta_padre_id IS NULL AND organizacion_id = {org}`
2. Delega a la misma lógica de listado
3. Mismos query parameters y formato de respuesta

---

#### 2. Arquitectura de Capas (Hexagonal)

##### **2.1 Capa de Dominio**

**Servicio de Dominio:** `IEvaluadorPermisos`

Método existente a utilizar:
```java
/**
 * Evalúa si el usuario tiene al menos el nivel de acceso requerido
 * sobre un recurso (carpeta o documento).
 */
boolean tieneAcceso(
    Long usuarioId,
    Long recursoId,
    TipoRecurso tipoRecurso,
    NivelAcceso nivelRequerido,
    Long organizacionId
);
```

**Value Objects:**

```java
public record ContenidoCarpeta(
    List<CarpetaItem> subcarpetas,
    List<DocumentoItem> documentos,
    int totalSubcarpetas,
    int totalDocumentos,
    int paginaActual,
    int totalPaginas
) {
    // Validations in compact constructor
}

public record CarpetaItem(
    Long id,
    String nombre,
    String descripcion,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaModificacion,
    int numSubcarpetas,
    int numDocumentos,
    CapacidadesUsuario capacidades
) {}

public record DocumentoItem(
    Long id,
    String nombre,
    String extension,
    Long tamanioBytes,
    String versionActual,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaModificacion,
    UsuarioResumen creadoPor,
    CapacidadesUsuario capacidades
) {}

public record CapacidadesUsuario(
    boolean puedeLeer,      // Siempre true en items listados (filtrado previo)
    boolean puedeEscribir,
    boolean puedeAdministrar,
    boolean puedeDescargar  // Solo para documentos
) {}
```

**Repositorio de Dominio:**

```java
public interface ICarpetaRepository {
    
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
}

public interface IDocumentoRepository {
    
    /**
     * Obtiene documentos visibles en una carpeta para un usuario.
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
}
```

##### **2.2 Capa de Aplicación**

**Servicio de Aplicación:** `CarpetaContenidoService`

```java
package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.model.*;
import com.docflow.documentcore.domain.repository.*;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación para operaciones de listado de contenido de carpetas.
 * Implementa filtrado por permisos y paginación.
 */
@Service
@Transactional(readOnly = true)
public class CarpetaContenidoService {

    private final ICarpetaRepository carpetaRepository;
    private final IDocumentoRepository documentoRepository;
    private final IEvaluadorPermisos evaluadorPermisos;

    public CarpetaContenidoService(
            ICarpetaRepository carpetaRepository,
            IDocumentoRepository documentoRepository,
            IEvaluadorPermisos evaluadorPermisos
    ) {
        this.carpetaRepository = carpetaRepository;
        this.documentoRepository = documentoRepository;
        this.evaluadorPermisos = evaluadorPermisos;
    }

    /**
     * Lista el contenido visible de una carpeta para el usuario autenticado.
     * 
     * @throws CarpetaNotFoundException si carpeta no existe
     * @throws AccesoDenegadoException si usuario no tiene LECTURA
     */
    public ContenidoCarpeta listarContenido(
            Long carpetaId,
            Long usuarioId,
            Long organizacionId,
            OpcionesListado opciones
    ) {
        // PASO 1: Validar que carpeta existe
        Carpeta carpeta = carpetaRepository.findById(carpetaId, organizacionId)
                .orElseThrow(() -> new CarpetaNotFoundException(carpetaId));

        // PASO 2: Validar permiso de LECTURA sobre la carpeta contenedora
        boolean tieneAcceso = evaluadorPermisos.tieneAcceso(
                usuarioId,
                carpetaId,
                TipoRecurso.CARPETA,
                NivelAcceso.LECTURA,
                organizacionId
        );

        if (!tieneAcceso) {
            throw new AccesoDenegadoException(
                    "SIN_PERMISO_LECTURA",
                    "No tienes permisos para ver el contenido de esta carpeta"
            );
        }

        // PASO 3: Obtener subcarpetas visibles con paginación
        PageRequest pageRequest = PageRequest.of(
                opciones.getPagina() - 1,
                opciones.getTamanio(),
                Sort.by(
                        opciones.getDireccion(),
                        opciones.getCampoOrden()
                )
        );

        List<Carpeta> subcarpetas = carpetaRepository.obtenerSubcarpetasVisibles(
                carpetaId, usuarioId, organizacionId, pageRequest
        );

        int totalSubcarpetas = carpetaRepository.contarSubcarpetasVisibles(
                carpetaId, usuarioId, organizacionId
        );

        // PASO 4: Obtener documentos visibles con paginación
        List<Documento> documentos = documentoRepository.obtenerDocumentosVisibles(
                carpetaId, usuarioId, organizacionId, pageRequest
        );

        int totalDocumentos = documentoRepository.contarDocumentosVisibles(
                carpetaId, usuarioId, organizacionId
        );

        // PASO 5: Enriquecer con capacidades del usuario (evaluación en lote)
        List<CarpetaItem> itemsCarpetas = enriquecerCapacidadesCarpetas(
                subcarpetas, usuarioId, organizacionId
        );

        List<DocumentoItem> itemsDocumentos = enriquecerCapacidadesDocumentos(
                documentos, usuarioId, organizacionId
        );

        // PASO 6: Construir respuesta
        int totalPaginas = (int) Math.ceil(
                (double) Math.max(totalSubcarpetas, totalDocumentos) / opciones.getTamanio()
        );

        return new ContenidoCarpeta(
                itemsCarpetas,
                itemsDocumentos,
                totalSubcarpetas,
                totalDocumentos,
                opciones.getPagina(),
                totalPaginas
        );
    }

    /**
     * Obtiene contenido de la carpeta raíz.
     */
    public ContenidoCarpeta listarContenidoRaiz(
            Long usuarioId,
            Long organizacionId,
            OpcionesListado opciones
    ) {
        Carpeta raiz = carpetaRepository.findRaiz(organizacionId)
                .orElseThrow(() -> new CarpetaRaizNoEncontradaException(organizacionId));

        return listarContenido(raiz.getId(), usuarioId, organizacionId, opciones);
    }

    /**
     * Evalúa capacidades en lote para optimizar rendimiento.
     */
    private List<CarpetaItem> enriquecerCapacidadesCarpetas(
            List<Carpeta> carpetas,
            Long usuarioId,
            Long organizacionId
    ) {
        return carpetas.stream()
                .map(carpeta -> {
                    boolean puedeEscribir = evaluadorPermisos.tieneAcceso(
                            usuarioId, carpeta.getId(), TipoRecurso.CARPETA,
                            NivelAcceso.ESCRITURA, organizacionId
                    );

                    boolean puedeAdministrar = evaluadorPermisos.tieneAcceso(
                            usuarioId, carpeta.getId(), TipoRecurso.CARPETA,
                            NivelAcceso.ADMINISTRACION, organizacionId
                    );

                    CapacidadesUsuario capacidades = new CapacidadesUsuario(
                            true, // puedeLeer (ya filtrado)
                            puedeEscribir,
                            puedeAdministrar,
                            false // no aplica a carpetas
                    );

                    return new CarpetaItem(
                            carpeta.getId(),
                            carpeta.getNombre(),
                            carpeta.getDescripcion(),
                            carpeta.getFechaCreacion(),
                            carpeta.getFechaModificacion(),
                            carpeta.getNumSubcarpetas(),
                            carpeta.getNumDocumentos(),
                            capacidades
                    );
                })
                .toList();
    }

    /**
     * Similar para documentos.
     */
    private List<DocumentoItem> enriquecerCapacidadesDocumentos(
            List<Documento> documentos,
            Long usuarioId,
            Long organizacionId
    ) {
        // Implementación análoga evaluando permisos sobre documentos
        // Considera precedencia: ACL documento > ACL carpeta
    }
}

/**
 * Opciones de listado con validaciones.
 */
public record OpcionesListado(
    int pagina,
    int tamanio,
    String campoOrden,
    Sort.Direction direccion
) {
    public OpcionesListado {
        if (pagina < 1) throw new IllegalArgumentException("Página debe ser >= 1");
        if (tamanio < 1 || tamanio > 100) throw new IllegalArgumentException("Tamaño debe estar entre 1 y 100");
        if (campoOrden == null || campoOrden.isBlank()) campoOrden = "nombre";
        if (direccion == null) direccion = Sort.Direction.ASC;
    }
}
```

##### **2.3 Capa de Infraestructura - Controlador**

```java
package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.application.dto.*;
import com.docflow.documentcore.application.mapper.*;
import com.docflow.documentcore.application.service.CarpetaContenidoService;
import com.docflow.documentcore.domain.model.*;
import com.docflow.documentcore.infrastructure.security.RequierePermiso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para operaciones de listado de contenido de carpetas.
 */
@RestController
@RequestMapping("/api/carpetas")
@Tag(name = "Carpetas - Contenido", description = "Endpoints para listar contenido de carpetas")
@SecurityRequirement(name = "bearer-jwt")
public class CarpetaContenidoController {

    private final CarpetaContenidoService service;
    private final ContenidoCarpetaMapper mapper;

    public CarpetaContenidoController(
            CarpetaContenidoService service,
            ContenidoCarpetaMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{id}/contenido")
    @Operation(
        summary = "Listar contenido de carpeta",
        description = "Retorna subcarpetas y documentos visibles para el usuario, " +
                      "filtrados por permisos de LECTURA. Incluye capacidades por elemento."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Contenido listado exitosamente"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuario sin permiso de LECTURA en la carpeta"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Carpeta no encontrada"
        )
    })
    public ResponseEntity<ContenidoCarpetaDTO> listarContenido(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") 
            @Parameter(description = "Número de página (base 1)") 
            int page,
            @RequestParam(defaultValue = "20") 
            @Parameter(description = "Elementos por página (1-100)") 
            int size,
            @RequestParam(defaultValue = "nombre") 
            @Parameter(description = "Campo de ordenamiento") 
            String ordenar_por,
            @RequestParam(defaultValue = "asc") 
            @Parameter(description = "Dirección del orden (asc/desc)") 
            String direccion,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") 
            Long organizacionId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") 
            Long usuarioId
    ) {
        OpcionesListado opciones = new OpcionesListado(
                page,
                size,
                ordenar_por,
                Sort.Direction.fromString(direccion.toUpperCase())
        );

        ContenidoCarpeta contenido = service.listarContenido(
                id, usuarioId, organizacionId, opciones
        );

        ContenidoCarpetaDTO response = mapper.toDto(contenido);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/raiz/contenido")
    @Operation(
        summary = "Listar contenido de carpeta raíz",
        description = "Endpoint auxiliar para obtener el contenido de la carpeta raíz de la organización"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contenido listado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Carpeta raíz no encontrada")
    })
    public ResponseEntity<ContenidoCarpetaDTO> listarContenidoRaiz(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nombre") String ordenar_por,
            @RequestParam(defaultValue = "asc") String direccion,
            @RequestHeader(value = "X-Organization-Id", required = false, defaultValue = "1") Long organizacionId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long usuarioId
    ) {
        OpcionesListado opciones = new OpcionesListado(
                page, size, ordenar_por, Sort.Direction.fromString(direccion.toUpperCase())
        );

        ContenidoCarpeta contenido = service.listarContenidoRaiz(
                usuarioId, organizacionId, opciones
        );

        ContenidoCarpetaDTO response = mapper.toDto(contenido);
        return ResponseEntity.ok(response);
    }
}
```

##### **2.4 Capa de Infraestructura - Repositorio JPA**

**Query Optimizado con Filtro de Permisos:**

```java
package com.docflow.documentcore.infrastructure.adapter.persistence;

import com.docflow.documentcore.domain.model.Carpeta;
import com.docflow.documentcore.domain.repository.ICarpetaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarpetaJpaRepository extends JpaRepository<Carpeta, Long>, ICarpetaRepository {

    /**
     * Obtiene subcarpetas visibles considerando:
     * 1. Permiso directo en carpeta
     * 2. Permiso heredado de ancestro con recursivo=true
     * 
     * Optimización: JOIN LATERAL para evaluar herencia solo cuando no hay permiso directo.
     */
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
                  -- Obtener ruta de ancestros de la carpeta
                  WITH RECURSIVE ancestros AS (
                      SELECT id, carpeta_padre_id, 1 AS nivel
                      FROM carpeta
                      WHERE id = c.id
                    UNION ALL
                      SELECT c2.id, c2.carpeta_padre_id, a.nivel + 1
                      FROM carpeta c2
                      INNER JOIN ancestros a ON c2.id = a.carpeta_padre_id
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

    /**
     * Cuenta subcarpetas visibles (misma lógica que query anterior).
     */
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
                  WITH RECURSIVE ancestros AS (
                      SELECT id, carpeta_padre_id, 1 AS nivel
                      FROM carpeta WHERE id = c.id
                    UNION ALL
                      SELECT c2.id, c2.carpeta_padre_id, a.nivel + 1
                      FROM carpeta c2
                      INNER JOIN ancestros a ON c2.id = a.carpeta_padre_id
                      WHERE a.carpeta_padre_id IS NOT NULL
                  )
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
}
```

**Repositorio de Documentos (similar):**

```java
@Repository
public interface DocumentoJpaRepository extends JpaRepository<Documento, Long>, IDocumentoRepository {

    /**
     * Obtiene documentos visibles considerando precedencia:
     * 1. ACL explícito en documento (máxima prioridad)
     * 2. ACL directo en carpeta contenedora
     * 3. ACL heredado recursivo de ancestro
     */
    @Query(value = """
        SELECT DISTINCT d.*
        FROM documento d
        WHERE d.carpeta_id = :carpetaId
          AND d.organizacion_id = :organizacionId
          AND d.fecha_eliminacion IS NULL
          AND (
              -- CASO 1: Permiso explícito en documento
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
                              WITH RECURSIVE ancestros AS (
                                  SELECT id, carpeta_padre_id, 1 AS nivel
                                  FROM carpeta WHERE id = :carpetaId
                                UNION ALL
                                  SELECT c.id, c.carpeta_padre_id, a.nivel + 1
                                  FROM carpeta c
                                  INNER JOIN ancestros a ON c.id = a.carpeta_padre_id
                                  WHERE a.carpeta_padre_id IS NOT NULL
                              )
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
}
```

---

#### 3. Base de Datos - Índices de Optimización

**Índices Compuestos para Performance:**

```sql
-- Índice para búsqueda de permisos directos en carpeta
CREATE INDEX IF NOT EXISTS idx_permiso_carpeta_usuario_carpeta_usuario_org
ON permiso_carpeta_usuario (carpeta_id, usuario_id, organizacion_id);

-- Índice para búsqueda de permisos recursivos
CREATE INDEX IF NOT EXISTS idx_permiso_carpeta_usuario_recursivo_org
ON permiso_carpeta_usuario (usuario_id, organizacion_id, recursivo)
WHERE recursivo = true;

-- Índice para búsqueda de permisos en documento
CREATE INDEX IF NOT EXISTS idx_permiso_documento_usuario_doc_usuario_org
ON permiso_documento_usuario (documento_id, usuario_id, organizacion_id);

-- Índice para listado de subcarpetas por padre
CREATE INDEX IF NOT EXISTS idx_carpeta_padre_org_eliminacion
ON carpeta (carpeta_padre_id, organizacion_id)
WHERE fecha_eliminacion IS NULL;

-- Índice para listado de documentos por carpeta
CREATE INDEX IF NOT EXISTS idx_documento_carpeta_org_eliminacion
ON documento (carpeta_id, organizacion_id)
WHERE fecha_eliminacion IS NULL;
```

**Análisis de Performance:**

```sql
-- Validar uso de índices en query de subcarpetas
EXPLAIN ANALYZE
SELECT DISTINCT c.*
FROM carpeta c
WHERE c.carpeta_padre_id = 100
  AND c.organizacion_id = 10
  AND c.fecha_eliminacion IS NULL
  AND EXISTS (
      SELECT 1 FROM permiso_carpeta_usuario pcu
      WHERE pcu.carpeta_id = c.id
        AND pcu.usuario_id = 50
        AND pcu.organizacion_id = 10
  );

-- Resultado esperado: Index Scan en idx_carpeta_padre_org_eliminacion
--                     Index Scan en idx_permiso_carpeta_usuario_carpeta_usuario_org
```

---

#### 4. DTOs y Mappers

**DTOs de Respuesta:**

```java
package com.docflow.documentcore.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Contenido de una carpeta con subcarpetas y documentos visibles")
public record ContenidoCarpetaDTO(
    @Schema(description = "Lista de subcarpetas")
    List<CarpetaItemDTO> subcarpetas,

    @Schema(description = "Lista de documentos")
    List<DocumentoItemDTO> documentos,

    @JsonProperty("total_subcarpetas")
    @Schema(description = "Total de subcarpetas visibles")
    int totalSubcarpetas,

    @JsonProperty("total_documentos")
    @Schema(description = "Total de documentos visibles")
    int totalDocumentos,

    @JsonProperty("pagina_actual")
    @Schema(description = "Página actual (base 1)")
    int paginaActual,

    @JsonProperty("elementos_por_pagina")
    @Schema(description = "Número de elementos por página")
    int elementosPorPagina,

    @JsonProperty("total_paginas")
    @Schema(description = "Total de páginas disponibles")
    int totalPaginas
) {}

@Schema(description = "Información de una carpeta en el listado")
public record CarpetaItemDTO(
    @Schema(description = "ID único de la carpeta")
    Long id,

    @Schema(description = "Nombre de la carpeta")
    String nombre,

    @Schema(description = "Descripción de la carpeta", nullable = true)
    String descripcion,

    @JsonProperty("fecha_creacion")
    @Schema(description = "Fecha de creación")
    LocalDateTime fechaCreacion,

    @JsonProperty("fecha_modificacion")
    @Schema(description = "Última modificación")
    LocalDateTime fechaModificacion,

    @JsonProperty("num_subcarpetas")
    @Schema(description = "Número de subcarpetas")
    int numSubcarpetas,

    @JsonProperty("num_documentos")
    @Schema(description = "Número de documentos")
    int numDocumentos,

    @JsonProperty("puede_escribir")
    @Schema(description = "Usuario puede crear/modificar contenido")
    boolean puedeEscribir,

    @JsonProperty("puede_administrar")
    @Schema(description = "Usuario puede administrar permisos")
    boolean puedeAdministrar
) {}

@Schema(description = "Información de un documento en el listado")
public record DocumentoItemDTO(
    @Schema(description = "ID único del documento")
    Long id,

    @Schema(description = "Nombre del documento con extensión")
    String nombre,

    @Schema(description = "Extensión del archivo")
    String extension,

    @JsonProperty("tamanio_bytes")
    @Schema(description = "Tamaño en bytes")
    Long tamanioBytes,

    @JsonProperty("version_actual")
    @Schema(description = "Versión actual del documento")
    String versionActual,

    @JsonProperty("fecha_creacion")
    @Schema(description = "Fecha de creación")
    LocalDateTime fechaCreacion,

    @JsonProperty("fecha_modificacion")
    @Schema(description = "Última modificación")
    LocalDateTime fechaModificacion,

    @JsonProperty("creado_por")
    @Schema(description = "Usuario que creó el documento")
    UsuarioResumenDTO creadoPor,

    @JsonProperty("puede_escribir")
    @Schema(description = "Usuario puede modificar el documento")
    boolean puedeEscribir,

    @JsonProperty("puede_descargar")
    @Schema(description = "Usuario puede descargar el archivo")
    boolean puedeDescargar,

    @JsonProperty("puede_administrar")
    @Schema(description = "Usuario puede administrar permisos")
    boolean puedeAdministrar
) {}

@Schema(description = "Resumen de usuario")
public record UsuarioResumenDTO(
    @Schema(description = "ID del usuario")
    Long id,

    @JsonProperty("nombre_completo")
    @Schema(description = "Nombre completo del usuario")
    String nombreCompleto
) {}
```

**Mapper con MapStruct:**

```java
package com.docflow.documentcore.application.mapper;

import com.docflow.documentcore.application.dto.*;
import com.docflow.documentcore.domain.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContenidoCarpetaMapper {

    ContenidoCarpetaDTO toDto(ContenidoCarpeta contenido);

    @Mapping(target = "capacidades", ignore = true) // Manual
    CarpetaItemDTO toItemDto(CarpetaItem item);

    @Mapping(target = "capacidades", ignore = true)
    DocumentoItemDTO toItemDto(DocumentoItem item);

    UsuarioResumenDTO toResumenDto(UsuarioResumen usuario);
}
```

---

#### 5. Manejo de Errores

**Excepciones de Dominio:**

```java
package com.docflow.documentcore.domain.exception;

/**
 * Lanzada cuando una carpeta no existe o no pertenece a la organización.
 */
public class CarpetaNotFoundException extends DomainException {
    public CarpetaNotFoundException(Long carpetaId) {
        super(
                "CARPETA_NO_ENCONTRADA",
                String.format("Carpeta %d no encontrada", carpetaId),
                "DOC-404"
        );
    }
}

/**
 * Lanzada cuando el usuario no tiene permisos suficientes.
 */
public class AccesoDenegadoException extends DomainException {
    public AccesoDenegadoException(String codigo, String mensaje) {
        super(codigo, mensaje, "DOC-403");
    }
}

/**
 * Lanzada cuando no existe carpeta raíz para la organización.
 */
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

**Global Exception Handler:**

```java
package com.docflow.documentcore.infrastructure.adapter.exception;

import com.docflow.documentcore.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CarpetaNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCarpetaNotFound(
            CarpetaNotFoundException ex
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getCodigo(),
                ex.getMessage(),
                ex.getCodigoHttp()
        );
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoDenegado(
            AccesoDenegadoException ex
    ) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                ex.getCodigo(),
                ex.getMessage(),
                ex.getCodigoHttp()
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String error,
            String mensaje,
            String codigo
    ) {
        Map<String, Object> body = Map.of(
                "error", error,
                "mensaje", mensaje,
                "codigo", codigo,
                "timestamp", LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }
}
```

---

#### 6. Estrategia de Testing

##### **6.1 Tests Unitarios**

**Test: Servicio de Listado con Permisos**

```java
package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.model.*;
import com.docflow.documentcore.domain.repository.*;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import com.docflow.documentcore.domain.exception.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    private Long usuarioId = 50L;
    private Long organizacionId = 10L;
    private Long carpetaId = 100L;

    @Test
    @DisplayName("Debe listar contenido cuando usuario tiene LECTURA")
    void should_ListContent_When_UserHasReadPermission() {
        // GIVEN
        Carpeta carpeta = new Carpeta();
        carpeta.setId(carpetaId);
        carpeta.setNombre("Proyectos");

        when(carpetaRepository.findById(carpetaId, organizacionId))
                .thenReturn(Optional.of(carpeta));

        when(evaluadorPermisos.tieneAcceso(
                usuarioId, carpetaId, TipoRecurso.CARPETA, NivelAcceso.LECTURA, organizacionId
        )).thenReturn(true);

        List<Carpeta> subcarpetas = List.of(
                crearCarpeta(101L, "Marketing"),
                crearCarpeta(102L, "Finanzas")
        );

        when(carpetaRepository.obtenerSubcarpetasVisibles(
                eq(carpetaId), eq(usuarioId), eq(organizacionId), any(PageRequest.class)
        )).thenReturn(subcarpetas);

        when(carpetaRepository.contarSubcarpetasVisibles(carpetaId, usuarioId, organizacionId))
                .thenReturn(2);

        when(documentoRepository.obtenerDocumentosVisibles(
                eq(carpetaId), eq(usuarioId), eq(organizacionId), any(PageRequest.class)
        )).thenReturn(List.of());

        when(documentoRepository.contarDocumentosVisibles(carpetaId, usuarioId, organizacionId))
                .thenReturn(0);

        // Simular evaluación de capacidades
        when(evaluadorPermisos.tieneAcceso(
                anyLong(), anyLong(), eq(TipoRecurso.CARPETA), eq(NivelAcceso.ESCRITURA), anyLong()
        )).thenReturn(false);

        when(evaluadorPermisos.tieneAcceso(
                anyLong(), anyLong(), eq(TipoRecurso.CARPETA), eq(NivelAcceso.ADMINISTRACION), anyLong()
        )).thenReturn(false);

        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre", Sort.Direction.ASC);

        // WHEN
        ContenidoCarpeta resultado = service.listarContenido(
                carpetaId, usuarioId, organizacionId, opciones
        );

        // THEN
        assertThat(resultado).isNotNull();
        assertThat(resultado.subcarpetas()).hasSize(2);
        assertThat(resultado.documentos()).isEmpty();
        assertThat(resultado.totalSubcarpetas()).isEqualTo(2);
        assertThat(resultado.totalDocumentos()).isEqualTo(0);

        // Verificar que cada item tiene capacidades
        resultado.subcarpetas().forEach(item -> {
            assertThat(item.capacidades()).isNotNull();
            assertThat(item.capacidades().puedeLeer()).isTrue();
            assertThat(item.capacidades().puedeEscribir()).isFalse();
        });

        // Verificar interacciones
        verify(carpetaRepository).findById(carpetaId, organizacionId);
        verify(evaluadorPermisos).tieneAcceso(
                usuarioId, carpetaId, TipoRecurso.CARPETA, NivelAcceso.LECTURA, organizacionId
        );
        verify(carpetaRepository).obtenerSubcarpetasVisibles(
                eq(carpetaId), eq(usuarioId), eq(organizacionId), any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("Debe lanzar AccesoDenegadoException cuando usuario sin LECTURA")
    void should_ThrowAccesoDenegado_When_UserLacksReadPermission() {
        // GIVEN
        Carpeta carpeta = new Carpeta();
        carpeta.setId(carpetaId);

        when(carpetaRepository.findById(carpetaId, organizacionId))
                .thenReturn(Optional.of(carpeta));

        when(evaluadorPermisos.tieneAcceso(
                usuarioId, carpetaId, TipoRecurso.CARPETA, NivelAcceso.LECTURA, organizacionId
        )).thenReturn(false);

        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre", Sort.Direction.ASC);

        // WHEN / THEN
        assertThatThrownBy(() -> service.listarContenido(
                carpetaId, usuarioId, organizacionId, opciones
        ))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("SIN_PERMISO_LECTURA");

        // No debe intentar listar contenido
        verify(carpetaRepository, never()).obtenerSubcarpetasVisibles(
                anyLong(), anyLong(), anyLong(), any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("Debe lanzar CarpetaNotFoundException cuando carpeta no existe")
    void should_ThrowCarpetaNotFound_When_FolderDoesNotExist() {
        // GIVEN
        when(carpetaRepository.findById(carpetaId, organizacionId))
                .thenReturn(Optional.empty());

        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre", Sort.Direction.ASC);

        // WHEN / THEN
        assertThatThrownBy(() -> service.listarContenido(
                carpetaId, usuarioId, organizacionId, opciones
        ))
                .isInstanceOf(CarpetaNotFoundException.class)
                .hasMessageContaining("Carpeta 100 no encontrada");
    }

    @Test
    @DisplayName("Debe retornar listas vacías cuando carpeta está vacía")
    void should_ReturnEmptyLists_When_FolderIsEmpty() {
        // GIVEN
        Carpeta carpeta = new Carpeta();
        carpeta.setId(carpetaId);

        when(carpetaRepository.findById(carpetaId, organizacionId))
                .thenReturn(Optional.of(carpeta));

        when(evaluadorPermisos.tieneAcceso(
                usuarioId, carpetaId, TipoRecurso.CARPETA, NivelAcceso.LECTURA, organizacionId
        )).thenReturn(true);

        when(carpetaRepository.obtenerSubcarpetasVisibles(
                eq(carpetaId), eq(usuarioId), eq(organizacionId), any(PageRequest.class)
        )).thenReturn(List.of());

        when(carpetaRepository.contarSubcarpetasVisibles(carpetaId, usuarioId, organizacionId))
                .thenReturn(0);

        when(documentoRepository.obtenerDocumentosVisibles(
                eq(carpetaId), eq(usuarioId), eq(organizacionId), any(PageRequest.class)
        )).thenReturn(List.of());

        when(documentoRepository.contarDocumentosVisibles(carpetaId, usuarioId, organizacionId))
                .thenReturn(0);

        OpcionesListado opciones = new OpcionesListado(1, 20, "nombre", Sort.Direction.ASC);

        // WHEN
        ContenidoCarpeta resultado = service.listarContenido(
                carpetaId, usuarioId, organizacionId, opciones
        );

        // THEN
        assertThat(resultado.subcarpetas()).isEmpty();
        assertThat(resultado.documentos()).isEmpty();
        assertThat(resultado.totalSubcarpetas()).isZero();
        assertThat(resultado.totalDocumentos()).isZero();
        assertThat(resultado.totalPaginas()).isZero();
    }

    @Test
    @DisplayName("Debe calcular correctamente la paginación")
    void should_CalculatePaginationCorrectly() {
        // GIVEN: 47 elementos totales, tamaño página 20
        // Resultado esperado: 3 páginas (20 + 20 + 7)
        Carpeta carpeta = new Carpeta();
        carpeta.setId(carpetaId);

        when(carpetaRepository.findById(carpetaId, organizacionId))
                .thenReturn(Optional.of(carpeta));

        when(evaluadorPermisos.tieneAcceso(
                usuarioId, carpetaId, TipoRecurso.CARPETA, NivelAcceso.LECTURA, organizacionId
        )).thenReturn(true);

        when(carpetaRepository.obtenerSubcarpetasVisibles(
                eq(carpetaId), eq(usuarioId), eq(organizacionId), any(PageRequest.class)
        )).thenReturn(List.of(crearCarpeta(101L, "Test")));

        when(carpetaRepository.contarSubcarpetasVisibles(carpetaId, usuarioId, organizacionId))
                .thenReturn(25);

        when(documentoRepository.obtenerDocumentosVisibles(
                eq(carpetaId), eq(usuarioId), eq(organizacionId), any(PageRequest.class)
        )).thenReturn(List.of());

        when(documentoRepository.contarDocumentosVisibles(carpetaId, usuarioId, organizacionId))
                .thenReturn(47);

        OpcionesListado opciones = new OpcionesListado(2, 20, "nombre", Sort.Direction.ASC);

        // WHEN
        ContenidoCarpeta resultado = service.listarContenido(
                carpetaId, usuarioId, organizacionId, opciones
        );

        // THEN
        assertThat(resultado.paginaActual()).isEqualTo(2);
        assertThat(resultado.elementosPorPagina()).isEqualTo(20);
        assertThat(resultado.totalPaginas()).isEqualTo(3); // ceil(47 / 20)
    }

    // Helpers
    private Carpeta crearCarpeta(Long id, String nombre) {
        Carpeta carpeta = new Carpeta();
        carpeta.setId(id);
        carpeta.setNombre(nombre);
        return carpeta;
    }
}
```

##### **6.2 Tests de Integración**

**Test: Endpoint completo con BD real**

```java
package com.docflow.documentcore.infrastructure.adapter.controller;

import com.docflow.documentcore.domain.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GET /api/carpetas/{id}/contenido - Tests de Integración")
class CarpetaContenidoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/carpetas";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer mock-jwt-token";

    @Test
    @Sql("/test-data/carpetas-con-permisos.sql")
    @DisplayName("Debe retornar 200 y contenido filtrado cuando usuario tiene LECTURA")
    void should_Return200WithFilteredContent_When_UserHasReadPermission() throws Exception {
        // GIVEN: Script SQL crea:
        // - Carpeta Proyectos (ID: 100) con 3 subcarpetas
        // - Usuario tiene LECTURA en 2 de las 3 subcarpetas
        // - 2 documentos visibles, 1 oculto por ACL

        // WHEN / THEN
        mockMvc.perform(get(BASE_URL + "/100/contenido")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .header("X-Organization-Id", "10")
                        .header("X-User-Id", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subcarpetas", hasSize(2)))
                .andExpect(jsonPath("$.documentos", hasSize(2)))
                .andExpect(jsonPath("$.total_subcarpetas").value(2))
                .andExpect(jsonPath("$.total_documentos").value(2))
                .andExpect(jsonPath("$.subcarpetas[0].id").isNumber())
                .andExpect(jsonPath("$.subcarpetas[0].nombre").isString())
                .andExpect(jsonPath("$.subcarpetas[0].puede_escribir").isBoolean())
                .andExpect(jsonPath("$.subcarpetas[0].puede_administrar").isBoolean())
                .andExpect(jsonPath("$.documentos[0].puede_escribir").isBoolean())
                .andExpect(jsonPath("$.documentos[0].puede_descargar").isBoolean());
    }

    @Test
    @Sql("/test-data/carpetas-sin-permisos.sql")
    @DisplayName("Debe retornar 403 cuando usuario sin LECTURA")
    void should_Return403_When_UserLacksReadPermission() throws Exception {
        // GIVEN: Usuario sin permisos sobre carpeta 300

        // WHEN / THEN
        mockMvc.perform(get(BASE_URL + "/300/contenido")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .header("X-Organization-Id", "10")
                        .header("X-User-Id", "51")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("SIN_PERMISO_LECTURA"))
                .andExpect(jsonPath("$.codigo").value("DOC-403"))
                .andExpect(jsonPath("$.mensaje").isString());
    }

    @Test
    @DisplayName("Debe retornar 404 cuando carpeta no existe")
    void should_Return404_When_FolderNotFound() throws Exception {
        // WHEN / THEN
        mockMvc.perform(get(BASE_URL + "/99999/contenido")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .header("X-Organization-Id", "10")
                        .header("X-User-Id", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CARPETA_NO_ENCONTRADA"))
                .andExpect(jsonPath("$.codigo").value("DOC-404"));
    }

    @Test
    @Sql("/test-data/carpetas-multiorg.sql")
    @DisplayName("Debe aislar por organización (multi-tenancy)")
    void should_IsolateByOrganization_When_MultiTenant() throws Exception {
        // GIVEN: Carpeta ID 100 existe en org 10 y org 20
        // Usuario de org 10 no debe ver carpeta de org 20

        // WHEN / THEN
        mockMvc.perform(get(BASE_URL + "/100/contenido")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .header("X-Organization-Id", "10")
                        .header("X-User-Id", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subcarpetas[*].id").value(everyItem(not(equalTo(999)))));
        // Verificar que no se filtran IDs de otras orgs
    }

    @Test
    @Sql("/test-data/carpetas-con-soft-delete.sql")
    @DisplayName("Debe excluir elementos con soft delete")
    void should_ExcludeSoftDeletedItems() throws Exception {
        // GIVEN: Carpeta contiene 3 subcarpetas, 1 eliminada (fecha_eliminacion != NULL)

        // WHEN / THEN
        mockMvc.perform(get(BASE_URL + "/100/contenido")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .header("X-Organization-Id", "10")
                        .header("X-User-Id", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subcarpetas", hasSize(2))) // Solo las activas
                .andExpect(jsonPath("$.total_subcarpetas").value(2));
    }

    @Test
    @Sql("/test-data/carpetas-paginacion.sql")
    @DisplayName("Debe paginar correctamente el resultado")
    void should_PaginateResultsCorrectly() throws Exception {
        // GIVEN: Carpeta con 47 subcarpetas

        // WHEN: Solicitar página 2, tamaño 20
        mockMvc.perform(get(BASE_URL + "/100/contenido")
                        .param("page", "2")
                        .param("size", "20")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .header("X-Organization-Id", "10")
                        .header("X-User-Id", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina_actual").value(2))
                .andExpect(jsonPath("$.elementos_por_pagina").value(20))
                .andExpect(jsonPath("$.total_paginas").value(3))
                .andExpect(jsonPath("$.subcarpetas", hasSize(lessThanOrEqualTo(20))));
    }

    @Test
    @Sql("/test-data/carpetas-ordenamiento.sql")
    @DisplayName("Debe ordenar por nombre ascendente por defecto")
    void should_OrderByNameAscending_ByDefault() throws Exception {
        // WHEN / THEN
        mockMvc.perform(get(BASE_URL + "/100/contenido")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .header("X-Organization-Id", "10")
                        .header("X-User-Id", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subcarpetas[0].nombre").value(lessThan("Z")))
                .andExpect(jsonPath("$.subcarpetas[1].nombre").value(greaterThan("A")));
        // Validar orden alfabético
    }

    @Test
    @Sql("/test-data/carpeta-raiz.sql")
    @DisplayName("GET /raiz/contenido debe retornar carpeta raíz")
    void should_ReturnRootFolderContent() throws Exception {
        // WHEN / THEN
        mockMvc.perform(get(BASE_URL + "/raiz/contenido")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .header("X-Organization-Id", "10")
                        .header("X-User-Id", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subcarpetas").isArray())
                .andExpect(jsonPath("$.documentos").isArray());
    }
}
```

**Scripts SQL de Test:**

```sql
-- test-data/carpetas-con-permisos.sql
INSERT INTO carpeta (id, nombre, carpeta_padre_id, organizacion_id, fecha_eliminacion)
VALUES
    (100, 'Proyectos', NULL, 10, NULL),
    (101, 'Marketing', 100, 10, NULL),
    (102, 'Finanzas', 100, 10, NULL),
    (103, 'Legal', 100, 10, NULL);

INSERT INTO permiso_carpeta_usuario (usuario_id, carpeta_id, organizacion_id, nivel_acceso, recursivo)
VALUES
    (50, 100, 10, 'LECTURA', false),
    (50, 101, 10, 'LECTURA', false),
    (50, 102, 10, 'LECTURA', false);
-- Usuario NO tiene permiso en carpeta 103 (Legal)

INSERT INTO documento (id, nombre, carpeta_id, organizacion_id, fecha_eliminacion)
VALUES
    (200, 'Presentacion.pdf', 100, 10, NULL),
    (201, 'Confidencial.docx', 100, 10, NULL),
    (202, 'Informe.xlsx', 100, 10, NULL);

-- ACL en documento 201 restringe acceso
INSERT INTO permiso_documento_usuario (usuario_id, documento_id, organizacion_id, nivel_acceso)
VALUES (99, 201, 10, 'LECTURA'); -- Otro usuario, no el 50
```

---

#### 7. Documentación Técnica

##### **7.1 Actualizar README del Servicio**

Agregar sección en `backend/document-core/README.md`:

```markdown
### Listado de Contenido de Carpetas (US-FOLDER-002)

#### Concepto

El endpoint `/api/carpetas/{id}/contenido` permite a los usuarios navegar la estructura documental jerárquica, retornando solo las subcarpetas y documentos sobre los que tienen al menos permiso de LECTURA.

#### Filtrado de Permisos

El listado aplica las siguientes reglas de visibilidad:

1. **Para Subcarpetas:**
   - Usuario tiene permiso directo en la subcarpeta
   - Usuario tiene permiso heredado recursivo desde ancestro

2. **Para Documentos:**
   - Si existe ACL explícito en documento → evaluar ese permiso (prioridad máxima)
   - Si NO existe ACL documento → heredar de carpeta contenedora (directa o recursiva)

3. **Exclusiones:**
   - Elementos con `fecha_eliminacion != NULL` (soft-delete)
   - Elementos de otras organizaciones (aislamiento multi-tenant)

#### Ejemplo de Uso

```bash
# Listar contenido de carpeta
GET /api/carpetas/100/contenido?page=1&size=20&ordenar_por=nombre&direccion=asc
Authorization: Bearer {JWT}

# Respuesta
{
  "subcarpetas": [
    {
      "id": 101,
      "nombre": "Marketing",
      "puede_escribir": false,
      "puede_administrar": false
    }
  ],
  "documentos": [
    {
      "id": 200,
      "nombre": "Presentacion.pdf",
      "puede_escribir": true,
      "puede_descargar": true
    }
  ],
  "total_subcarpetas": 1,
  "total_documentos": 1
}
```

#### Performance

- **Índices Compuestos:** Optimizan queries con filtro de permisos
- **Paginación:** Máximo 100 elementos por página
- **Evaluación en Lote:** Las capacidades de usuario se evalúan eficientemente

#### Errores Comunes

| Código | Status | Causa |
|--------|--------|-------|
| `SIN_PERMISO_LECTURA` | 403 | Usuario no tiene LECTURA en carpeta |
| `CARPETA_NO_ENCONTRADA` | 404 | Carpeta no existe o no pertenece a la organización |
```

##### **7.2 Documentación OpenAPI**

El endpoint se documenta automáticamente con Springdoc OpenAPI mediante las anotaciones `@Operation`, `@ApiResponses`, `@Schema` ya incluidas en el código del controlador.

Accesible en: `http://localhost:8082/swagger-ui.html`

---

#### 8. Requisitos No Funcionales

##### **8.1 Performance**

- **Tiempo de Respuesta:** < 500ms para listados de hasta 100 elementos
- **Timeout:** 5 segundos máximo
- **Escalabilidad:** Soportar hasta 10,000 elementos en una carpeta con paginación eficiente
- **Índices:** Validar con `EXPLAIN ANALYZE` que se usan correctamente

##### **8.2 Seguridad**

- **Autenticación:** JWT obligatorio en header `Authorization`
- **Autorización:** Validación de permiso LECTURA antes de retornar datos
- **Aislamiento Multi-Tenant:** Filtro por `organizacion_id` en todas las queries
- **Inyección SQL:** Uso de queries parametrizadas (JPA/Hibernate)
- **Rate Limiting:** Implementar en Gateway (fuera del alcance de esta US)

##### **8.3 Mantenibilidad**

- **Código Limpio:** Seguir principios SOLID y arquitectura hexagonal
- **Cobertura de Tests:** Mínimo 90% en lógica de negocio
- **Documentación:** Javadoc en interfaces y métodos públicos
- **Logging:** Nivel INFO para operaciones exitosas, WARN para permisos denegados, ERROR para fallos

##### **8.4 Observabilidad**

```java
// Logging estratégico
log.info("Listando contenido de carpeta: carpetaId={}, usuarioId={}, organizacionId={}",
         carpetaId, usuarioId, organizacionId);

log.warn("Acceso denegado a carpeta: carpetaId={}, usuarioId={}, motivo={}",
         carpetaId, usuarioId, "SIN_PERMISO_LECTURA");

log.debug("Contenido obtenido: subcarpetas={}, documentos={}, tiempo={}ms",
          subcarpetas.size(), documentos.size(), duracion);
```

---

### Archivos a Crear/Modificar

#### **Nuevos Archivos**

1. **Domain - Value Objects**
   - `src/main/java/com/docflow/documentcore/domain/model/ContenidoCarpeta.java`
   - `src/main/java/com/docflow/documentcore/domain/model/CarpetaItem.java`
   - `src/main/java/com/docflow/documentcore/domain/model/DocumentoItem.java`
   - `src/main/java/com/docflow/documentcore/domain/model/CapacidadesUsuario.java`
   - `src/main/java/com/docflow/documentcore/domain/model/OpcionesListado.java`

2. **Domain - Excepciones**
   - `src/main/java/com/docflow/documentcore/domain/exception/CarpetaRaizNoEncontradaException.java`

3. **Application - Servicio**
   - `src/main/java/com/docflow/documentcore/application/service/CarpetaContenidoService.java`

4. **Application - DTOs**
   - `src/main/java/com/docflow/documentcore/application/dto/ContenidoCarpetaDTO.java`
   - `src/main/java/com/docflow/documentcore/application/dto/CarpetaItemDTO.java`
   - `src/main/java/com/docflow/documentcore/application/dto/DocumentoItemDTO.java`
   - `src/main/java/com/docflow/documentcore/application/dto/UsuarioResumenDTO.java`

5. **Application - Mappers**
   - `src/main/java/com/docflow/documentcore/application/mapper/ContenidoCarpetaMapper.java`

6. **Infrastructure - Controlador**
   - `src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/CarpetaContenidoController.java`

7. **Tests**
   - `src/test/java/com/docflow/documentcore/application/service/CarpetaContenidoServiceTest.java`
   - `src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/CarpetaContenidoControllerIntegrationTest.java`
   - `src/test/resources/test-data/carpetas-con-permisos.sql`
   - `src/test/resources/test-data/carpetas-sin-permisos.sql`
   - `src/test/resources/test-data/carpetas-multiorg.sql`
   - `src/test/resources/test-data/carpetas-con-soft-delete.sql`
   - `src/test/resources/test-data/carpetas-paginacion.sql`
   - `src/test/resources/test-data/carpetas-ordenamiento.sql`
   - `src/test/resources/test-data/carpeta-raiz.sql`

8. **Database - Migrations**
   - `src/main/resources/db/migration/V008__indices_listado_contenido.sql`

#### **Archivos a Modificar**

1. **Domain - Repositorios**
   - `src/main/java/com/docflow/documentcore/domain/repository/ICarpetaRepository.java`
     - Agregar métodos: `obtenerSubcarpetasVisibles()`, `contarSubcarpetasVisibles()`, `findRaiz()`
   
   - `src/main/java/com/docflow/documentcore/domain/repository/IDocumentoRepository.java`
     - Agregar métodos: `obtenerDocumentosVisibles()`, `contarDocumentosVisibles()`

2. **Infrastructure - Repositorios JPA**
   - `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/CarpetaJpaRepository.java`
     - Implementar queries nativas con filtro de permisos
   
   - `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/DocumentoJpaRepository.java`
     - Implementar queries nativas con precedencia documento > carpeta

3. **Infrastructure - Exception Handler**
   - `src/main/java/com/docflow/documentcore/infrastructure/adapter/exception/GlobalExceptionHandler.java`
     - Agregar handler para `CarpetaRaizNoEncontradaException`

4. **Documentación**
   - `backend/document-core/README.md`
     - Agregar sección "Listado de Contenido de Carpetas (US-FOLDER-002)"

5. **OpenAPI Spec** (opcional)
   - `ai-specs/specs/api-spec.yml`
     - Documentar endpoints y contratos de respuesta

---

### Definición de Done (DoD)

- [ ] **Código Implementado:**
  - [ ] Todos los archivos nuevos creados
  - [ ] Todos los archivos existentes modificados
  - [ ] Endpoints REST expuestos y documentados con Swagger
  - [ ] Servicios de aplicación implementados con lógica de negocio
  - [ ] Repositorios con queries nativas optimizadas

- [ ] **Tests:**
  - [ ] Tests unitarios con cobertura ≥ 90%
  - [ ] Tests de integración para ambos endpoints
  - [ ] Scripts SQL de test para todos los escenarios
  - [ ] Todos los tests pasan localmente (`mvn test`)

- [ ] **Base de Datos:**
  - [ ] Índices creados y validados con `EXPLAIN ANALYZE`
  - [ ] Queries optimizadas (sin N+1, sin table scans innecesarios)
  - [ ] Migrations aplicadas sin errores

- [ ] **Documentación:**
  - [ ] README actualizado con ejemplos de uso
  - [ ] Javadoc en interfaces y clases públicas
  - [ ] OpenAPI documentado (visible en `/swagger-ui.html`)

- [ ] **Calidad:**
  - [ ] Código sigue arquitectura hexagonal
  - [ ] Respeta principios SOLID y DRY
  - [ ] ESLint/Checkstyle pasa sin warnings
  - [ ] Code review aprobado por al menos 1 reviewer

- [ ] **Seguridad:**
  - [ ] Validación de autenticación (JWT)
  - [ ] Validación de autorización (permisos LECTURA)
  - [ ] Aislamiento multi-tenant verificado
  - [ ] Sin vulnerabilidades de inyección SQL

- [ ] **Performance:**
  - [ ] Tiempo de respuesta < 500ms para 100 elementos
  - [ ] Paginación funciona correctamente
  - [ ] Índices utilizados correctamente (verificado con EXPLAIN)

- [ ] **Integración:**
  - [ ] Funciona correctamente con servicios ACL (US-ACL-004, US-ACL-006)
  - [ ] Compatible con carpetas soft-deleted
  - [ ] Compatible con estructura jerárquica profunda

---

### Notas de Implementación

1. **Orden de Desarrollo Recomendado:**
   1. Crear Value Objects y DTOs
   2. Extender interfaces de repositorio (Domain)
   3. Implementar queries JPA nativas
   4. Crear índices de BD
   5. Implementar servicio de aplicación
   6. Implementar controlador REST
   7. Escribir tests unitarios
   8. Escribir tests de integración
   9. Actualizar documentación

2. **Consideraciones de Rendimiento:**
   - Las queries deben usar índices compuestos
   - Evitar evaluación N+1 de permisos (usar evaluación en lote)
   - Considerar caché de permisos si el volumen es alto (fuera del alcance MVP)

3. **Casos Borde:**
   - Carpeta con más de 10,000 elementos → paginación obligatoria
   - Usuario sin permisos en ninguna subcarpeta → lista vacía (no 403)
   - Carpeta eliminada (soft-delete) → 404
   - Organización sin carpeta raíz → crear automáticamente o error 404

4. **Extensiones Futuras (fuera del alcance):**
   - Búsqueda/filtrado por nombre dentro del listado
   - Ordenamiento por múltiples campos
   - Vistas guardadas (favoritos, recientes)
   - Cache distribuido (Redis) para permisos

---

### Referencias

- **US Relacionadas:**
  - [US-ACL-004](../P2-Permisos/US-ACL-004.md) - Herencia recursiva de permisos
  - [US-ACL-005](../P2-Permisos/US-ACL-005.md) - Permisos explícitos en documentos
  - [US-ACL-006](../P2-Permisos/US-ACL-006.md) - Precedencia documento > carpeta
  - [US-FOLDER-001](./US-FOLDER-001.md) - Creación de carpetas

- **Documentación Técnica:**
  - [Data Model](../../../ai-specs/specs/data-model.md)
  - [Backend Standards](../../../ai-specs/specs/backend-standards.md)
  - [API Spec](../../../ai-specs/specs/api-spec.yml)

- **Implementaciones Existentes:**
  - `EvaluadorPermisosService` - Servicio de evaluación de permisos
  - `PermisoHerenciaService` - Servicio de herencia recursiva
  - `CarpetaController` - Controlador base de carpetas

---

**Fin de la especificación técnica detallada de US-FOLDER-002**
