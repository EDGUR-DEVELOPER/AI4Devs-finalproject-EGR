## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-006] Regla de precedencia de permisos (Documento > Carpeta)

## Descripción Funcional Completa

**Narrativa:** Como sistema, necesito una regla clara y automatizada de precedencia de permisos para resolver conflictos cuando existen permisos tanto a nivel de documento como de carpeta, garantizando que la evaluación sea consistente, predecible y eficiente en todas las operaciones del sistema.

**Objetivo Técnico:** Implementar un servicio centralizado de evaluación de permisos que:
- Aplique la regla de precedencia: **Permiso Documento > Permiso Carpeta (directo o heredado)**
- Resuelva conflictos automáticamente sin intervención manual
- Proporcione información sobre el origen del permiso efectivo
- Sea utilizado consistentemente por todos los guards y middlewares del sistema
- Mantenga el rendimiento incluso con jerarquías complejas de carpetas

### Justificación de Diseño

La precedencia documento > carpeta permite:
1. **Excepciones granulares**: Conceder o restringir acceso a documentos específicos sin afectar la carpeta contenedora
2. **Principio de mínimo privilegio**: Permitir que un documento tenga permisos más restrictivos que su carpeta
3. **Flexibilidad administrativa**: Facilitar la gestión de casos especiales sin reestructurar carpetas

## Criterios de Aceptación Ampliados

| Scenario | Condición Inicial (Given) | Acción (When) | Resultado Esperado (Then) |
|----------|--------------------------|--------------|--------------------------|
| **6.1** | Usuario con `ESCRITURA` en carpeta "Proyectos", `LECTURA` explícita en "Contrato.pdf" | Se evalúa acceso de usuario a "Contrato.pdf" | Permiso efectivo es `LECTURA` (origen: DOCUMENTO), usuario NO puede editar documento |
| **6.2** | Usuario con `LECTURA` en carpeta "Documentos", sin ACL explícita en "Informe.pdf" | Se evalúa acceso de usuario a "Informe.pdf" | Permiso efectivo es `LECTURA` (origen: CARPETA_DIRECTO), usuario puede ver documento |
| **6.3** | Usuario con `LECTURA` recursivo en carpeta raíz "Empresa", sin ACL directo en subcarpeta ni documento | Se evalúa acceso a documento en subcarpeta profunda | Permiso efectivo es `LECTURA` (origen: CARPETA_HEREDADO), se identifica carpeta origen |
| **6.4** | Usuario sin ningún ACL relacionado (carpeta ni documento) | Se evalúa acceso de usuario a documento | Permiso efectivo es `null`, se retorna `403 Forbidden` en endpoints |
| **6.5** | Usuario con `ADMINISTRACION` en carpeta, `LECTURA` en documento específico | Intenta modificar permisos del documento | Operación rechazada (`403`), el permiso documento prevalece aunque sea más restrictivo |
| **6.6** | Admin asigna ACL documento con `LECTURA` a usuario que tiene `ESCRITURA` en carpeta | Sistema registra nueva ACL documento | Warning en UI, ACL creado exitosamente, futuras evaluaciones usan documento |
| **6.7** | Múltiples usuarios con diferentes combinaciones de permisos | Se evalúa acceso para cada usuario | Cada usuario obtiene su permiso correcto según su ACL específico |
| **6.8** | Usuario con permiso documento que se elimina | Se evalúa acceso después de eliminación | Sistema usa permiso de carpeta (fallback automático) |

## Algoritmo de Evaluación de Precedencia

```java
/**
 * Algoritmo central de evaluación de permisos con precedencia
 * Regla: Permiso_Documento > Permiso_Carpeta > Permiso_Heredado > Sin_Permiso
 */
public PermisoEfectivoDTO evaluarPermisoDocumento(
    Long usuarioId, 
    Long documentoId, 
    Long organizacionId
) {
    // 1. PRIORIDAD MÁXIMA: Buscar ACL explícito en documento
    Optional<AclDocumento> aclDocumento = 
        aclDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId);
    
    if (aclDocumento.isPresent()) {
        return PermisoEfectivoDTO.builder()
            .nivelAcceso(aclDocumento.get().getNivelAcceso())
            .origen(OrigenPermiso.DOCUMENTO)
            .recursoOrigenId(documentoId)
            .tipoRecurso(TipoRecurso.DOCUMENTO)
            .build();
    }
    
    // 2. FALLBACK: Obtener carpeta contenedora y evaluar permiso de carpeta
    Documento documento = documentoRepository.findById(documentoId)
        .orElseThrow(() -> new DocumentoNotFoundException(documentoId));
    
    Long carpetaId = documento.getCarpetaId();
    
    // 3. Delegar evaluación de carpeta (incluye herencia)
    return evaluarPermisoCarpeta(usuarioId, carpetaId, organizacionId);
}

public PermisoEfectivoDTO evaluarPermisoCarpeta(
    Long usuarioId, 
    Long carpetaId, 
    Long organizacionId
) {
    // 1. Buscar ACL directo en carpeta
    Optional<AclCarpeta> aclDirecto = 
        aclCarpetaRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId);
    
    if (aclDirecto.isPresent()) {
        return PermisoEfectivoDTO.builder()
            .nivelAcceso(aclDirecto.get().getNivelAcceso())
            .origen(OrigenPermiso.CARPETA_DIRECTO)
            .recursoOrigenId(carpetaId)
            .tipoRecurso(TipoRecurso.CARPETA)
            .build();
    }
    
    // 2. Buscar en ancestros (herencia)
    List<Long> rutaAncestros = carpetaService.obtenerRutaAncestros(carpetaId);
    
    for (Long ancestroId : rutaAncestros) {
        Optional<AclCarpeta> aclHeredado = aclCarpetaRepository
            .findByCarpetaIdAndUsuarioIdAndRecursivoTrue(ancestroId, usuarioId);
        
        if (aclHeredado.isPresent()) {
            return PermisoEfectivoDTO.builder()
                .nivelAcceso(aclHeredado.get().getNivelAcceso())
                .origen(OrigenPermiso.CARPETA_HEREDADO)
                .recursoOrigenId(ancestroId)
                .tipoRecurso(TipoRecurso.CARPETA)
                .build();
        }
    }
    
    // 3. Sin permiso
    return null;
}
```

## Estructura de Datos

### DTOs de Respuesta

#### PermisoEfectivoDTO

```java
@Data
@Builder
public class PermisoEfectivoDTO {
    @NotNull
    private NivelAcceso nivelAcceso;
    
    @NotNull
    private OrigenPermiso origen;
    
    @NotNull
    private Long recursoOrigenId;
    
    @NotNull
    private TipoRecurso tipoRecurso;
    
    private OffsetDateTime evaluadoEn;
}
```

**Ejemplo de respuesta JSON:**

```json
{
  "nivelAcceso": "LECTURA",
  "origen": "DOCUMENTO",
  "recursoOrigenId": 42,
  "tipoRecurso": "DOCUMENTO",
  "evaluadoEn": "2026-02-03T14:30:00Z"
}
```

### Enums

#### OrigenPermiso

```java
public enum OrigenPermiso {
    DOCUMENTO,           // Permiso explícito del documento
    CARPETA_DIRECTO,     // Permiso directo de la carpeta contenedora
    CARPETA_HEREDADO     // Permiso heredado de carpeta ancestro
}
```

#### TipoRecurso

```java
public enum TipoRecurso {
    DOCUMENTO,
    CARPETA
}
```

## Archivos a Crear/Modificar

### Backend - Capa de Dominio

#### 1. Interface del Puerto (Domain)

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/domain/service/IEvaluadorPermisos.java`

```java
package com.docflow.documentcore.domain.service;

import com.docflow.documentcore.domain.model.PermisoEfectivoDTO;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;

/**
 * Puerto para evaluación centralizada de permisos con precedencia.
 * Define el contrato para resolver permisos efectivos considerando
 * la regla: Documento > Carpeta_Directo > Carpeta_Heredado.
 */
public interface IEvaluadorPermisos {
    
    /**
     * Evalúa el permiso efectivo de un usuario sobre un documento.
     * Aplica precedencia: ACL documento > ACL carpeta > Herencia
     * 
     * @param usuarioId ID del usuario
     * @param documentoId ID del documento
     * @param organizacionId ID de la organización (tenant isolation)
     * @return PermisoEfectivoDTO con nivel y origen, o null si sin permiso
     */
    PermisoEfectivoDTO evaluarPermisoDocumento(
        Long usuarioId, 
        Long documentoId, 
        Long organizacionId
    );
    
    /**
     * Evalúa el permiso efectivo de un usuario sobre una carpeta.
     * Considera ACL directo y herencia de ancestros.
     * 
     * @param usuarioId ID del usuario
     * @param carpetaId ID de la carpeta
     * @param organizacionId ID de la organización
     * @return PermisoEfectivoDTO con nivel y origen, o null si sin permiso
     */
    PermisoEfectivoDTO evaluarPermisoCarpeta(
        Long usuarioId, 
        Long carpetaId, 
        Long organizacionId
    );
    
    /**
     * Verifica si un usuario tiene un nivel de acceso específico sobre un recurso.
     * 
     * @param usuarioId ID del usuario
     * @param recursoId ID del recurso (documento o carpeta)
     * @param tipoRecurso Tipo de recurso
     * @param nivelRequerido Nivel mínimo requerido
     * @param organizacionId ID de la organización
     * @return true si el usuario tiene al menos el nivel requerido
     */
    boolean tieneAcceso(
        Long usuarioId, 
        Long recursoId, 
        TipoRecurso tipoRecurso,
        NivelAcceso nivelRequerido,
        Long organizacionId
    );
}
```

#### 2. DTOs de Dominio

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/domain/model/PermisoEfectivoDTO.java`

```java
package com.docflow.documentcore.domain.model;

import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PermisoEfectivoDTO {
    private NivelAcceso nivelAcceso;
    private OrigenPermiso origen;
    private Long recursoOrigenId;
    private TipoRecurso tipoRecurso;
    private OffsetDateTime evaluadoEn;
}
```

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/domain/model/OrigenPermiso.java`

```java
package com.docflow.documentcore.domain.model;

public enum OrigenPermiso {
    DOCUMENTO,
    CARPETA_DIRECTO,
    CARPETA_HEREDADO
}
```

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/domain/model/TipoRecurso.java`

```java
package com.docflow.documentcore.domain.model;

public enum TipoRecurso {
    DOCUMENTO,
    CARPETA
}
```

### Backend - Capa de Aplicación

#### 3. Servicio de Evaluación (Application Service)

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/application/service/EvaluadorPermisosService.java`

```java
package com.docflow.documentcore.application.service;

import com.docflow.documentcore.domain.model.*;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.model.permiso.PermisoDocumentoUsuario;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.domain.repository.IPermisoDocumentoUsuarioRepository;
import com.docflow.documentcore.domain.service.IEvaluadorPermisos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EvaluadorPermisosService implements IEvaluadorPermisos {

    private final IPermisoDocumentoUsuarioRepository permisoDocumentoRepository;
    private final IPermisoCarpetaUsuarioRepository permisoCarpetaRepository;
    private final CarpetaService carpetaService;
    private final DocumentoService documentoService;

    @Override
    public PermisoEfectivoDTO evaluarPermisoDocumento(
        Long usuarioId, 
        Long documentoId, 
        Long organizacionId
    ) {
        log.debug("Evaluando permiso documento: usuario={}, documento={}, org={}", 
                  usuarioId, documentoId, organizacionId);
        
        // 1. Buscar ACL explícito en documento
        Optional<PermisoDocumentoUsuario> aclDocumento = 
            permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId);
        
        if (aclDocumento.isPresent() && 
            aclDocumento.get().getOrganizacionId().equals(organizacionId)) {
            
            log.info("Permiso documento encontrado: nivel={}", aclDocumento.get().getNivelAcceso());
            return PermisoEfectivoDTO.builder()
                .nivelAcceso(aclDocumento.get().getNivelAcceso())
                .origen(OrigenPermiso.DOCUMENTO)
                .recursoOrigenId(documentoId)
                .tipoRecurso(TipoRecurso.DOCUMENTO)
                .evaluadoEn(OffsetDateTime.now())
                .build();
        }
        
        // 2. Fallback a carpeta contenedora
        Long carpetaId = documentoService.obtenerCarpetaId(documentoId, organizacionId);
        return evaluarPermisoCarpeta(usuarioId, carpetaId, organizacionId);
    }

    @Override
    public PermisoEfectivoDTO evaluarPermisoCarpeta(
        Long usuarioId, 
        Long carpetaId, 
        Long organizacionId
    ) {
        log.debug("Evaluando permiso carpeta: usuario={}, carpeta={}, org={}", 
                  usuarioId, carpetaId, organizacionId);
        
        // 1. Buscar ACL directo en carpeta
        Optional<PermisoCarpetaUsuario> aclDirecto = 
            permisoCarpetaRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId);
        
        if (aclDirecto.isPresent() && 
            aclDirecto.get().getOrganizacionId().equals(organizacionId)) {
            
            log.info("Permiso carpeta directo encontrado: nivel={}", aclDirecto.get().getNivelAcceso());
            return PermisoEfectivoDTO.builder()
                .nivelAcceso(aclDirecto.get().getNivelAcceso())
                .origen(OrigenPermiso.CARPETA_DIRECTO)
                .recursoOrigenId(carpetaId)
                .tipoRecurso(TipoRecurso.CARPETA)
                .evaluadoEn(OffsetDateTime.now())
                .build();
        }
        
        // 2. Buscar en herencia de ancestros
        List<Long> rutaAncestros = carpetaService.obtenerRutaAncestros(carpetaId);
        
        for (Long ancestroId : rutaAncestros) {
            Optional<PermisoCarpetaUsuario> aclHeredado = 
                permisoCarpetaRepository.findByCarpetaIdAndUsuarioIdAndRecursivoTrue(
                    ancestroId, usuarioId
                );
            
            if (aclHeredado.isPresent() && 
                aclHeredado.get().getOrganizacionId().equals(organizacionId)) {
                
                log.info("Permiso heredado encontrado: carpeta_origen={}, nivel={}", 
                         ancestroId, aclHeredado.get().getNivelAcceso());
                return PermisoEfectivoDTO.builder()
                    .nivelAcceso(aclHeredado.get().getNivelAcceso())
                    .origen(OrigenPermiso.CARPETA_HEREDADO)
                    .recursoOrigenId(ancestroId)
                    .tipoRecurso(TipoRecurso.CARPETA)
                    .evaluadoEn(OffsetDateTime.now())
                    .build();
            }
        }
        
        // 3. Sin permiso
        log.warn("Sin permiso encontrado para usuario={} en carpeta={}", usuarioId, carpetaId);
        return null;
    }

    @Override
    public boolean tieneAcceso(
        Long usuarioId,
        Long recursoId,
        TipoRecurso tipoRecurso,
        NivelAcceso nivelRequerido,
        Long organizacionId
    ) {
        PermisoEfectivoDTO permiso = tipoRecurso == TipoRecurso.DOCUMENTO 
            ? evaluarPermisoDocumento(usuarioId, recursoId, organizacionId)
            : evaluarPermisoCarpeta(usuarioId, recursoId, organizacionId);
        
        if (permiso == null) {
            return false;
        }
        
        // Comparar niveles (asumiendo jerarquía: LECTURA < ESCRITURA < ADMINISTRACION)
        return cumpleNivelRequerido(permiso.getNivelAcceso(), nivelRequerido);
    }
    
    private boolean cumpleNivelRequerido(NivelAcceso actual, NivelAcceso requerido) {
        int nivelActual = getNivelJerarquico(actual);
        int nivelReq = getNivelJerarquico(requerido);
        return nivelActual >= nivelReq;
    }
    
    private int getNivelJerarquico(NivelAcceso nivel) {
        return switch (nivel) {
            case LECTURA -> 1;
            case ESCRITURA -> 2;
            case ADMINISTRACION -> 3;
        };
    }
}
```

### Backend - Guards y Middlewares

#### 4. Guard de Permisos Genérico

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/security/RequierePermisoGuard.java`

```java
package com.docflow.documentcore.infrastructure.security;

import com.docflow.documentcore.application.service.EvaluadorPermisosService;
import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guard que intercepta métodos anotados con @RequierePermiso
 * y valida el acceso usando EvaluadorPermisosService
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RequierePermisoGuard {

    private final EvaluadorPermisosService evaluadorPermisos;
    private final CurrentTenantService tenantService;
    private final CurrentUserService userService;

    @Around("@annotation(requierePermiso)")
    public Object verificarPermiso(
        ProceedingJoinPoint joinPoint, 
        RequierePermiso requierePermiso
    ) throws Throwable {
        
        Long usuarioId = userService.getCurrentUserId();
        Long organizacionId = tenantService.getCurrentTenantId();
        
        // Extraer ID del recurso de los argumentos del método
        Long recursoId = extraerRecursoId(joinPoint, requierePermiso.paramIndex());
        
        boolean tieneAcceso = evaluadorPermisos.tieneAcceso(
            usuarioId,
            recursoId,
            requierePermiso.tipoRecurso(),
            requierePermiso.nivelRequerido(),
            organizacionId
        );
        
        if (!tieneAcceso) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, 
                "No tiene permiso suficiente sobre este recurso"
            );
        }
        
        return joinPoint.proceed();
    }
    
    private Long extraerRecursoId(ProceedingJoinPoint joinPoint, int index) {
        Object[] args = joinPoint.getArgs();
        if (index >= 0 && index < args.length && args[index] instanceof Long) {
            return (Long) args[index];
        }
        throw new IllegalArgumentException("No se pudo extraer recursoId del método");
    }
}
```

#### 5. Anotación Custom

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/security/RequierePermiso.java`

```java
package com.docflow.documentcore.infrastructure.security;

import com.docflow.documentcore.domain.model.TipoRecurso;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequierePermiso {
    TipoRecurso tipoRecurso();
    NivelAcceso nivelRequerido();
    int paramIndex() default 0; // Índice del parámetro que contiene el resourceId
}
```

### Backend - Controladores Actualizados

#### 6. Ejemplo de uso en controlador

**Modificar:** `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/DocumentoController.java`

```java
@GetMapping("/{documentoId}")
@RequierePermiso(
    tipoRecurso = TipoRecurso.DOCUMENTO, 
    nivelRequerido = NivelAcceso.LECTURA,
    paramIndex = 0
)
public ResponseEntity<DocumentoResponseDTO> obtenerDocumento(
    @PathVariable Long documentoId
) {
    // Lógica del controlador
    // El guard ya verificó el permiso antes de llegar aquí
}

@PutMapping("/{documentoId}")
@RequierePermiso(
    tipoRecurso = TipoRecurso.DOCUMENTO, 
    nivelRequerido = NivelAcceso.ESCRITURA,
    paramIndex = 0
)
public ResponseEntity<DocumentoResponseDTO> actualizarDocumento(
    @PathVariable Long documentoId,
    @RequestBody @Valid ActualizarDocumentoDTO dto
) {
    // Solo llega aquí si tiene permiso ESCRITURA
}
```

## Testing (TDD/BDD)

### Tests Unitarios del Servicio

**Archivo:** `backend/document-core/src/test/java/com/docflow/documentcore/EvaluadorPermisosServiceTest.java`

```java
package com.docflow.documentcore;

import com.docflow.documentcore.application.service.CarpetaService;
import com.docflow.documentcore.application.service.DocumentoService;
import com.docflow.documentcore.application.service.EvaluadorPermisosService;
import com.docflow.documentcore.domain.model.*;
import com.docflow.documentcore.domain.model.acl.NivelAcceso;
import com.docflow.documentcore.domain.model.permiso.PermisoCarpetaUsuario;
import com.docflow.documentcore.domain.model.permiso.PermisoDocumentoUsuario;
import com.docflow.documentcore.domain.repository.IPermisoCarpetaUsuarioRepository;
import com.docflow.documentcore.domain.repository.IPermisoDocumentoUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluadorPermisosService - Tests Unitarios TDD")
class EvaluadorPermisosServiceTest {

    @Mock
    private IPermisoDocumentoUsuarioRepository permisoDocumentoRepository;
    
    @Mock
    private IPermisoCarpetaUsuarioRepository permisoCarpetaRepository;
    
    @Mock
    private CarpetaService carpetaService;
    
    @Mock
    private DocumentoService documentoService;
    
    @InjectMocks
    private EvaluadorPermisosService evaluador;
    
    private Long usuarioId;
    private Long documentoId;
    private Long carpetaId;
    private Long organizacionId;
    
    @BeforeEach
    void setUp() {
        usuarioId = 1L;
        documentoId = 10L;
        carpetaId = 5L;
        organizacionId = 100L;
    }
    
    @Test
    @DisplayName("Debe usar permiso DOCUMENTO cuando existe ACL explícito")
    void should_UseDocumentPermission_When_ExplicitAclExists() {
        // Given: ACL documento con LECTURA
        PermisoDocumentoUsuario aclDocumento = new PermisoDocumentoUsuario();
        aclDocumento.setDocumentoId(documentoId);
        aclDocumento.setUsuarioId(usuarioId);
        aclDocumento.setNivelAcceso(NivelAcceso.LECTURA);
        aclDocumento.setOrganizacionId(organizacionId);
        
        when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
            .thenReturn(Optional.of(aclDocumento));
        
        // When: Se evalúa permiso
        PermisoEfectivoDTO resultado = evaluador.evaluarPermisoDocumento(
            usuarioId, documentoId, organizacionId
        );
        
        // Then: Retorna permiso de documento
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNivelAcceso()).isEqualTo(NivelAcceso.LECTURA);
        assertThat(resultado.getOrigen()).isEqualTo(OrigenPermiso.DOCUMENTO);
        assertThat(resultado.getRecursoOrigenId()).isEqualTo(documentoId);
        
        verify(permisoDocumentoRepository).findByDocumentoIdAndUsuarioId(documentoId, usuarioId);
        verifyNoInteractions(permisoCarpetaRepository); // No consulta carpeta
    }
    
    @Test
    @DisplayName("Debe usar permiso CARPETA cuando NO existe ACL documento")
    void should_UseFolderPermission_When_NoDocumentAcl() {
        // Given: Sin ACL documento, con ACL carpeta ESCRITURA
        when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
            .thenReturn(Optional.empty());
        
        when(documentoService.obtenerCarpetaId(documentoId, organizacionId))
            .thenReturn(carpetaId);
        
        PermisoCarpetaUsuario aclCarpeta = new PermisoCarpetaUsuario();
        aclCarpeta.setCarpetaId(carpetaId);
        aclCarpeta.setUsuarioId(usuarioId);
        aclCarpeta.setNivelAcceso(NivelAcceso.ESCRITURA);
        aclCarpeta.setOrganizacionId(organizacionId);
        
        when(permisoCarpetaRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
            .thenReturn(Optional.of(aclCarpeta));
        
        // When
        PermisoEfectivoDTO resultado = evaluador.evaluarPermisoDocumento(
            usuarioId, documentoId, organizacionId
        );
        
        // Then: Usa permiso de carpeta
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNivelAcceso()).isEqualTo(NivelAcceso.ESCRITURA);
        assertThat(resultado.getOrigen()).isEqualTo(OrigenPermiso.CARPETA_DIRECTO);
        assertThat(resultado.getRecursoOrigenId()).isEqualTo(carpetaId);
    }
    
    @Test
    @DisplayName("Debe usar permiso HEREDADO cuando no hay ACL directo")
    void should_UseInheritedPermission_When_NoDirectAcl() {
        // Given: Sin ACL documento ni carpeta directo, pero con herencia
        when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
            .thenReturn(Optional.empty());
        
        when(documentoService.obtenerCarpetaId(documentoId, organizacionId))
            .thenReturn(carpetaId);
        
        when(permisoCarpetaRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
            .thenReturn(Optional.empty());
        
        Long carpetaPadreId = 2L;
        when(carpetaService.obtenerRutaAncestros(carpetaId))
            .thenReturn(Arrays.asList(carpetaPadreId));
        
        PermisoCarpetaUsuario aclHeredado = new PermisoCarpetaUsuario();
        aclHeredado.setCarpetaId(carpetaPadreId);
        aclHeredado.setUsuarioId(usuarioId);
        aclHeredado.setNivelAcceso(NivelAcceso.LECTURA);
        aclHeredado.setRecursivo(true);
        aclHeredado.setOrganizacionId(organizacionId);
        
        when(permisoCarpetaRepository.findByCarpetaIdAndUsuarioIdAndRecursivoTrue(
            carpetaPadreId, usuarioId
        )).thenReturn(Optional.of(aclHeredado));
        
        // When
        PermisoEfectivoDTO resultado = evaluador.evaluarPermisoDocumento(
            usuarioId, documentoId, organizacionId
        );
        
        // Then: Usa herencia
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNivelAcceso()).isEqualTo(NivelAcceso.LECTURA);
        assertThat(resultado.getOrigen()).isEqualTo(OrigenPermiso.CARPETA_HEREDADO);
        assertThat(resultado.getRecursoOrigenId()).isEqualTo(carpetaPadreId);
    }
    
    @Test
    @DisplayName("Debe retornar NULL cuando no hay ningún permiso")
    void should_ReturnNull_When_NoPermission() {
        // Given: Sin ningún permiso
        when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
            .thenReturn(Optional.empty());
        
        when(documentoService.obtenerCarpetaId(documentoId, organizacionId))
            .thenReturn(carpetaId);
        
        when(permisoCarpetaRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
            .thenReturn(Optional.empty());
        
        when(carpetaService.obtenerRutaAncestros(carpetaId))
            .thenReturn(Arrays.asList());
        
        // When
        PermisoEfectivoDTO resultado = evaluador.evaluarPermisoDocumento(
            usuarioId, documentoId, organizacionId
        );
        
        // Then: Sin permiso
        assertThat(resultado).isNull();
    }
    
    @Test
    @DisplayName("Debe priorizar documento aunque carpeta tenga mayor nivel")
    void should_PrioritizeDocument_Even_When_FolderHasHigherLevel() {
        // Given: LECTURA en documento, ADMINISTRACION en carpeta
        PermisoDocumentoUsuario aclDocumento = new PermisoDocumentoUsuario();
        aclDocumento.setNivelAcceso(NivelAcceso.LECTURA);
        aclDocumento.setOrganizacionId(organizacionId);
        
        when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
            .thenReturn(Optional.of(aclDocumento));
        
        // When
        PermisoEfectivoDTO resultado = evaluador.evaluarPermisoDocumento(
            usuarioId, documentoId, organizacionId
        );
        
        // Then: Usa documento (LECTURA), ignora carpeta
        assertThat(resultado.getNivelAcceso()).isEqualTo(NivelAcceso.LECTURA);
        assertThat(resultado.getOrigen()).isEqualTo(OrigenPermiso.DOCUMENTO);
    }
    
    @Test
    @DisplayName("tieneAcceso debe retornar FALSE cuando no hay permiso")
    void should_ReturnFalse_When_NoPermission() {
        // Given: Sin permiso
        when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
            .thenReturn(Optional.empty());
        when(documentoService.obtenerCarpetaId(documentoId, organizacionId))
            .thenReturn(carpetaId);
        when(permisoCarpetaRepository.findByCarpetaIdAndUsuarioId(carpetaId, usuarioId))
            .thenReturn(Optional.empty());
        when(carpetaService.obtenerRutaAncestros(carpetaId))
            .thenReturn(Arrays.asList());
        
        // When
        boolean resultado = evaluador.tieneAcceso(
            usuarioId, documentoId, TipoRecurso.DOCUMENTO, 
            NivelAcceso.LECTURA, organizacionId
        );
        
        // Then
        assertThat(resultado).isFalse();
    }
    
    @Test
    @DisplayName("tieneAcceso debe retornar TRUE cuando nivel es suficiente")
    void should_ReturnTrue_When_HasSufficientLevel() {
        // Given: ESCRITURA disponible, requiere LECTURA
        PermisoDocumentoUsuario aclDocumento = new PermisoDocumentoUsuario();
        aclDocumento.setNivelAcceso(NivelAcceso.ESCRITURA);
        aclDocumento.setOrganizacionId(organizacionId);
        
        when(permisoDocumentoRepository.findByDocumentoIdAndUsuarioId(documentoId, usuarioId))
            .thenReturn(Optional.of(aclDocumento));
        
        // When: Requiere LECTURA pero tiene ESCRITURA
        boolean resultado = evaluador.tieneAcceso(
            usuarioId, documentoId, TipoRecurso.DOCUMENTO, 
            NivelAcceso.LECTURA, organizacionId
        );
        
        // Then: TRUE (ESCRITURA >= LECTURA)
        assertThat(resultado).isTrue();
    }
}
```

### Tests de Integración BDD

**Archivo:** `backend/document-core/src/test/java/com/docflow/documentcore/integration/PrecedenciaPermisosIntegrationTest.java`

```java
@SpringBootTest
@AutoConfigureTestDatabase
@DisplayName("Tests de Integración - Precedencia de Permisos")
class PrecedenciaPermisosIntegrationTest {
    
    @Autowired
    private EvaluadorPermisosService evaluador;
    
    @Autowired
    private TestDataBuilder testData;
    
    @Test
    @DisplayName("Scenario: Usuario con ESCRITURA en carpeta pero LECTURA en documento")
    void scenario_RestrictiveDocumentPermission() {
        // Given
        var org = testData.crearOrganizacion("OrgTest");
        var usuario = testData.crearUsuario("user@test.com", org);
        var carpeta = testData.crearCarpeta("Proyectos", org);
        var documento = testData.crearDocumento("contrato.pdf", carpeta);
        
        testData.asignarPermisoC arpeta(usuario, carpeta, NivelAcceso.ESCRITURA, false);
        testData.asignarPermisoDocumento(usuario, documento, NivelAcceso.LECTURA);
        
        // When
        PermisoEfectivoDTO permiso = evaluador.evaluarPermisoDocumento(
            usuario.getId(), documento.getId(), org.getId()
        );
        
        // Then
        assertThat(permiso.getNivelAcceso()).isEqualTo(NivelAcceso.LECTURA);
        assertThat(permiso.getOrigen()).isEqualTo(OrigenPermiso.DOCUMENTO);
    }
}
```

## Frontend

### Tipos TypeScript

**Archivo:** `frontend/src/features/acl/types/PermisoEfectivo.ts`

```typescript
export enum OrigenPermiso {
  DOCUMENTO = 'DOCUMENTO',
  CARPETA_DIRECTO = 'CARPETA_DIRECTO',
  CARPETA_HEREDADO = 'CARPETA_HEREDADO'
}

export enum NivelAcceso {
  LECTURA = 'LECTURA',
  ESCRITURA = 'ESCRITURA',
  ADMINISTRACION = 'ADMINISTRACION'
}

export interface PermisoEfectivo {
  nivelAcceso: NivelAcceso;
  origen: OrigenPermiso;
  recursoOrigenId: number;
  tipoRecurso: 'DOCUMENTO' | 'CARPETA';
  evaluadoEn: string;
}
```

### Servicio de Permisos

**Archivo:** `frontend/src/features/acl/services/permisoService.ts`

```typescript
import { apiClient } from '@/core/shared/apiClient';
import { PermisoEfectivo } from '../types/PermisoEfectivo';

export const permisoService = {
  async obtenerPermisoEfectivo(
    recursoId: number, 
    tipoRecurso: 'DOCUMENTO' | 'CARPETA'
  ): Promise<PermisoEfectivo> {
    const endpoint = tipoRecurso === 'DOCUMENTO' 
      ? `/api/documentos/${recursoId}/mi-permiso`
      : `/api/carpetas/${recursoId}/mi-permiso`;
    
    const response = await apiClient.get<PermisoEfectivo>(endpoint);
    return response.data;
  }
};
```

### Componente de Visualización

**Archivo:** `frontend/src/features/acl/components/PermisoIndicator.tsx`

```tsx
import React from 'react';
import { PermisoEfectivo, OrigenPermiso } from '../types/PermisoEfectivo';
import { InfoTooltip } from '@/common/ui/InfoTooltip';

interface Props {
  permiso: PermisoEfectivo;
}

export const PermisoIndicator: React.FC<Props> = ({ permiso }) => {
  const getOrigenLabel = (origen: OrigenPermiso): string => {
    switch (origen) {
      case OrigenPermiso.DOCUMENTO:
        return 'Permiso explícito del documento';
      case OrigenPermiso.CARPETA_DIRECTO:
        return 'Heredado de la carpeta contenedora';
      case OrigenPermiso.CARPETA_HEREDADO:
        return 'Heredado de carpeta ancestro';
    }
  };
  
  const getOrigenIcon = (origen: OrigenPermiso): string => {
    return origen === OrigenPermiso.DOCUMENTO ? '📄' : '📁';
  };
  
  return (
    <div className="flex items-center gap-2 text-sm">
      <span className="font-medium">{permiso.nivelAcceso}</span>
      <InfoTooltip content={getOrigenLabel(permiso.origen)}>
        <span className="text-gray-500">
          {getOrigenIcon(permiso.origen)}
        </span>
      </InfoTooltip>
    </div>
  );
};
```

## Documentación OpenAPI

Endpoint adicional para consultar permiso efectivo:

```yaml
/api/documentos/{documentoId}/mi-permiso:
  get:
    summary: Obtener permiso efectivo del usuario autenticado
    description: Retorna el permiso efectivo considerando precedencia documento > carpeta
    tags:
      - Permisos
    security:
      - bearerAuth: []
    parameters:
      - name: documentoId
        in: path
        required: true
        schema:
          type: integer
    responses:
      '200':
        description: Permiso efectivo del usuario
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PermisoEfectivo'
      '403':
        description: Usuario sin permiso sobre el documento
      '404':
        description: Documento no encontrado

components:
  schemas:
    PermisoEfectivo:
      type: object
      properties:
        nivelAcceso:
          type: string
          enum: [LECTURA, ESCRITURA, ADMINISTRACION]
        origen:
          type: string
          enum: [DOCUMENTO, CARPETA_DIRECTO, CARPETA_HEREDADO]
        recursoOrigenId:
          type: integer
        tipoRecurso:
          type: string
          enum: [DOCUMENTO, CARPETA]
        evaluadoEn:
          type: string
          format: date-time
```

## Criterios de Completitud

- [ ] Interface `IEvaluadorPermisos` definida en domain
- [ ] DTOs (`PermisoEfectivoDTO`, enums) creados
- [ ] Servicio `EvaluadorPermisosService` implementado y testeado
- [ ] Guards refactorizados para usar evaluador central
- [ ] Tests unitarios con cobertura 100% del algoritmo
- [ ] Tests de integración E2E con escenarios complejos
- [ ] Frontend actualizado con tipos y servicios
- [ ] Componentes de UI muestran origen de permiso
- [ ] Documentación OpenAPI actualizada
- [ ] Sin lógica duplicada de evaluación en el sistema

## Dependencias

- **Depende de:** US-ACL-002 (ACL Carpeta), US-ACL-004 (Herencia), US-ACL-005 (ACL Documento)
- **Bloquea:** US-ACL-007 (Enforcement LECTURA), US-ACL-008 (Enforcement ESCRITURA)

---

## Notas de Implementación

1. **Performance:** Considerar caché de evaluaciones frecuentes (carpetas root, usuarios activos)
2. **Logging:** Registrar todas las evaluaciones para debugging de permisos
3. **Auditoría:** No se audita la evaluación en sí, solo la asignación/revocación de ACLs
4. **Multi-tenancy:** Todas las queries DEBEN filtrar por `organizacion_id`
5. **Testing:** Priorizar TDD para el algoritmo de precedencia (lógica crítica)
6. **Refactorización:** Eliminar lógica duplicada de otros servicios después de implementar evaluador
