## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-008] Enforzar permisos de escritura en endpoints de creación/actualización

### Narrativa
**Como** sistema de gestión documental  
**Quiero** bloquear operaciones de escritura a usuarios sin permiso `ESCRITURA`  
**Para** evitar cambios no autorizados y garantizar la integridad de los documentos y carpetas

### Contexto de Negocio
Esta historia implementa el enforcement de permisos de escritura en todos los endpoints que modifican carpetas y documentos. Es crítica para la seguridad del sistema ya que previene modificaciones no autorizadas de contenido.

### Criterios de Aceptación

#### Scenario 1: Usuario sin ESCRITURA no puede subir documento
```gherkin
Given un usuario autenticado "lector@example.com" con permiso LECTURA en carpeta ID=1
When el usuario intenta subir un documento a carpeta ID=1 via POST /api/carpetas/1/documentos
Then el sistema debe retornar HTTP 403 Forbidden
And el mensaje de error debe indicar "Requiere permiso de escritura en esta carpeta"
And el documento no debe ser creado en la base de datos
And el evento debe ser registrado en auditoría con código ACL_WRITE_DENIED
```

#### Scenario 2: Usuario sin ESCRITURA no puede crear subcarpeta
```gherkin
Given un usuario autenticado "lector@example.com" con permiso LECTURA en carpeta ID=1
When el usuario intenta crear una subcarpeta via POST /api/carpetas/1/subcarpetas
Then el sistema debe retornar HTTP 403 Forbidden
And el mensaje de error debe indicar "Requiere permiso de escritura en carpeta padre"
And la subcarpeta no debe ser creada
And el evento debe ser registrado en auditoría
```

#### Scenario 3: Usuario con ESCRITURA puede subir documento
```gherkin
Given un usuario autenticado "escritor@example.com" con permiso ESCRITURA en carpeta ID=1
When el usuario sube un documento a carpeta ID=1 via POST /api/carpetas/1/documentos
Then el sistema debe retornar HTTP 201 Created
And el documento debe ser creado en la base de datos
And el archivo debe ser almacenado en MinIO
And el evento debe ser registrado en auditoría con código DOC_UPLOADED
```

#### Scenario 4: Mover documento requiere ESCRITURA en origen y destino
```gherkin
Given un usuario con ESCRITURA en carpeta ID=1 pero solo LECTURA en carpeta ID=2
When el usuario intenta mover documento de carpeta ID=1 a carpeta ID=2 via PATCH /api/documentos/{id}/mover
Then el sistema debe retornar HTTP 403 Forbidden
And el mensaje debe indicar "Requiere permiso de escritura en carpeta destino"
And el documento no debe ser movido
```

#### Scenario 5: Usuario de otra organización no puede escribir
```gherkin
Given un usuario del organizacion "B" autenticado
When el usuario intenta subir documento a carpeta de organizacion "A"
Then el sistema debe retornar HTTP 404 Not Found
And no debe revelar la existencia del recurso (seguridad)
```

#### Scenario 6: Usuario con ESCRITURA puede actualizar metadatos
```gherkin
Given un usuario con permiso ESCRITURA en documento ID=123
When el usuario actualiza el nombre del documento via PUT /api/documentos/123
Then el sistema debe retornar HTTP 200 OK
And los metadatos deben ser actualizados
And se debe crear una entrada en auditoría
```

#### Scenario 7: Permiso revocado durante operación
```gherkin
Given un usuario comienza a subir un documento con permiso ESCRITURA
When el permiso es revocado durante la operación
Then el sistema debe retornar HTTP 403 Forbidden
And debe limpiar cualquier archivo parcialmente subido
And debe notificar al usuario del cambio de permiso
```

---

## Detalles Técnicos de Implementación

### Endpoints Afectados

#### Carpetas
1. **POST /api/carpetas/{id}/subcarpetas**
   - Requiere: `ESCRITURA` en carpeta padre (ID={id})
   - Request Body:
     ```json
     {
       "nombre": "string",
       "descripcion": "string"
     }
     ```
   - Response Success: 201 Created + CarpetaResponse
   - Response Error: 403 Forbidden + ErrorResponse

2. **PUT /api/carpetas/{id}**
   - Requiere: `ESCRITURA` en carpeta (ID={id})
   - Request Body:
     ```json
     {
       "nombre": "string",
       "descripcion": "string"
     }
     ```
   - Response Success: 200 OK + CarpetaResponse
   - Response Error: 403 Forbidden + ErrorResponse

3. **DELETE /api/carpetas/{id}**
   - Requiere: `ESCRITURA` o `ADMINISTRACION` en carpeta (definir regla)
   - Response Success: 204 No Content
   - Response Error: 403 Forbidden + ErrorResponse

#### Documentos
1. **POST /api/carpetas/{id}/documentos**
   - Requiere: `ESCRITURA` en carpeta destino (ID={id})
   - Request Body: multipart/form-data
     ```
     file: binary
     nombre: string
     descripcion: string
     etiquetas: string[]
     ```
   - Response Success: 201 Created + DocumentoResponse
   - Response Error: 403 Forbidden + ErrorResponse

2. **PUT /api/documentos/{id}**
   - Requiere: `ESCRITURA` en documento (o carpeta padre si no hay ACL directo)
   - Request Body:
     ```json
     {
       "nombre": "string",
       "descripcion": "string",
       "etiquetas": ["string"]
     }
     ```
   - Response Success: 200 OK + DocumentoResponse
   - Response Error: 403 Forbidden + ErrorResponse

3. **POST /api/documentos/{id}/versiones**
   - Requiere: `ESCRITURA` en documento
   - Request Body: multipart/form-data
     ```
     file: binary
     comentario: string
     ```
   - Response Success: 201 Created + DocumentVersionResponse
   - Response Error: 403 Forbidden + ErrorResponse

4. **PATCH /api/documentos/{id}/mover**
   - Requiere: `ESCRITURA` en carpeta origen Y carpeta destino
   - Request Body:
     ```json
     {
       "carpetaDestinoId": number
     }
     ```
   - Response Success: 200 OK + DocumentoResponse
   - Response Error: 403 Forbidden + ErrorResponse

### Estructura de Archivos

#### Backend - document-core

```
backend/document-core/src/main/java/com/docflow/documentcore/
├── application/
│   ├── services/
│   │   └── PermissionEnforcementService.java      # Servicio de enforcement
│   └── guards/
│       ├── RequiereEscrituraGuard.java             # Guard principal
│       └── RequiereEscritura.java                   # Anotación
├── domain/
│   ├── model/
│   │   └── Permission.java                         # Modelo existente
│   └── ports/
│       ├── in/
│       │   ├── CreateSubfolderUseCase.java         # Casos de uso existentes
│       │   ├── UploadDocumentUseCase.java
│       │   ├── UpdateDocumentUseCase.java
│       │   └── MoveDocumentUseCase.java
│       └── out/
│           └── PermissionRepositoryPort.java       # Puerto existente
└── infrastructure/
    ├── adapters/
    │   ├── in/
    │   │   └── rest/
    │   │       ├── FolderController.java           # A modificar
    │   │       └── DocumentController.java         # A modificar
    │   └── out/
    │       └── persistence/
    │           └── PermissionJpaAdapter.java       # Existente
    └── config/
        └── SecurityConfig.java                      # Configuración de guards
```

#### Tests
```
backend/document-core/src/test/java/com/docflow/documentcore/
├── application/
│   └── guards/
│       └── RequiereEscrituraGuardTest.java         # Tests unitarios
└── infrastructure/
    └── adapters/
        └── in/
            └── rest/
                ├── FolderControllerTest.java       # Tests integración
                └── DocumentControllerTest.java     # Tests integración
```

#### Frontend
```
frontend/src/features/
├── documents/
│   ├── services/
│   │   └── documentService.ts                      # Actualizar manejo 403
│   ├── components/
│   │   ├── DocumentList.tsx                        # Deshabilitar acciones
│   │   ├── UploadDocumentModal.tsx                 # Validación previa
│   │   └── DocumentActions.tsx                     # Control de botones
│   └── hooks/
│       └── useDocumentPermissions.ts               # Hook para permisos
└── folders/
    ├── services/
    │   └── folderService.ts                        # Actualizar manejo 403
    ├── components/
    │   ├── FolderList.tsx                          # Deshabilitar acciones
    │   ├── CreateFolderModal.tsx                   # Validación previa
    │   └── FolderActions.tsx                       # Control de botones
    └── hooks/
        └── useFolderPermissions.ts                 # Hook para permisos
```

---

## Especificación de Componentes Backend

### 1. Guard de Escritura

**Archivo:** `RequiereEscrituraGuard.java`

```java
package com.docflow.documentcore.application.guards;

import com.docflow.documentcore.application.services.PermissionEnforcementService;
import com.docflow.documentcore.domain.model.NivelAcceso;
import com.docflow.documentcore.infrastructure.exceptions.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RequiereEscrituraGuard {
    
    private final PermissionEnforcementService permissionService;
    
    public RequiereEscrituraGuard(PermissionEnforcementService permissionService) {
        this.permissionService = permissionService;
    }
    
    @Around("@annotation(requiereEscritura)")
    public Object enforce(ProceedingJoinPoint joinPoint, RequiereEscritura requiereEscritura) 
            throws Throwable {
        
        HttpServletRequest request = getCurrentRequest();
        Long userId = extractUserId(request);
        Long organizationId = extractOrganizationId(request);
        Long recursoId = extractRecursoId(joinPoint, requiereEscritura.paramName());
        
        boolean hasPermission = permissionService.tienePermiso(
            userId,
            organizationId,
            recursoId,
            requiereEscritura.tipoRecurso(),
            NivelAcceso.ESCRITURA
        );
        
        if (!hasPermission) {
            throw new ForbiddenException(
                String.format("Requiere permiso de escritura en %s ID=%d",
                    requiereEscritura.tipoRecurso(), recursoId)
            );
        }
        
        return joinPoint.proceed();
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
    
    private Long extractUserId(HttpServletRequest request) {
        // Extraer de JWT token en SecurityContext
        return (Long) request.getAttribute("userId");
    }
    
    private Long extractOrganizationId(HttpServletRequest request) {
        // Extraer de JWT token en SecurityContext
        return (Long) request.getAttribute("organizationId");
    }
    
    private Long extractRecursoId(ProceedingJoinPoint joinPoint, String paramName) {
        // Extraer ID del parámetro del método (e.g., @PathVariable id)
        Object[] args = joinPoint.getArgs();
        // Implementar extracción basada en nombre de parámetro
        return null; // Simplificado para ejemplo
    }
}
```

**Archivo:** `RequiereEscritura.java`

```java
package com.docflow.documentcore.application.guards;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereEscritura {
    TipoRecurso tipoRecurso();
    String paramName() default "id";
    
    enum TipoRecurso {
        CARPETA,
        DOCUMENTO
    }
}
```

### 2. Aplicación en Controladores

**Ejemplo:** `FolderController.java`

```java
@RestController
@RequestMapping("/api/carpetas")
public class FolderController {
    
    private final CreateSubfolderUseCase createSubfolderUseCase;
    private final UpdateFolderUseCase updateFolderUseCase;
    
    @PostMapping("/{id}/subcarpetas")
    @RequiereEscritura(tipoRecurso = TipoRecurso.CARPETA, paramName = "id")
    public ResponseEntity<CarpetaResponse> crearSubcarpeta(
            @PathVariable Long id,
            @Valid @RequestBody CrearSubcarpetaRequest request,
            @RequestAttribute Long userId,
            @RequestAttribute Long organizationId) {
        
        CarpetaResponse response = createSubfolderUseCase.execute(
            id, request, userId, organizationId
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{id}")
    @RequiereEscritura(tipoRecurso = TipoRecurso.CARPETA)
    public ResponseEntity<CarpetaResponse> actualizarCarpeta(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCarpetaRequest request,
            @RequestAttribute Long userId,
            @RequestAttribute Long organizationId) {
        
        CarpetaResponse response = updateFolderUseCase.execute(
            id, request, userId, organizationId
        );
        
        return ResponseEntity.ok(response);
    }
}
```

**Ejemplo:** `DocumentController.java`

```java
@RestController
@RequestMapping("/api/documentos")
public class DocumentController {
    
    private final MoveDocumentUseCase moveDocumentUseCase;
    
    @PatchMapping("/{id}/mover")
    @RequiereEscritura(tipoRecurso = TipoRecurso.DOCUMENTO)
    public ResponseEntity<DocumentoResponse> moverDocumento(
            @PathVariable Long id,
            @Valid @RequestBody MoverDocumentoRequest request,
            @RequestAttribute Long userId,
            @RequestAttribute Long organizationId) {
        
        // Validar ESCRITURA en carpeta destino
        verificarEscrituraDestino(request.getCarpetaDestinoId(), userId, organizationId);
        
        DocumentoResponse response = moveDocumentUseCase.execute(
            id, request, userId, organizationId
        );
        
        return ResponseEntity.ok(response);
    }
    
    private void verificarEscrituraDestino(Long carpetaId, Long userId, Long orgId) {
        boolean hasPermission = permissionService.tienePermiso(
            userId, orgId, carpetaId, TipoRecurso.CARPETA, NivelAcceso.ESCRITURA
        );
        
        if (!hasPermission) {
            throw new ForbiddenException("Requiere permiso de escritura en carpeta destino");
        }
    }
}
```

### 3. Formato de Respuesta de Error

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .message(ex.getMessage())
            .code("ACL_WRITE_DENIED")
            .build();
        
        // Registrar en auditoría
        auditService.logAccessDenied(ex);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}
```

**Estructura ErrorResponse:**
```json
{
  "timestamp": "2026-02-03T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Requiere permiso de escritura en carpeta ID=123",
  "code": "ACL_WRITE_DENIED",
  "path": "/api/carpetas/123/documentos"
}
```

---

## Especificación Frontend

### 1. Hook de Permisos

**Archivo:** `useDocumentPermissions.ts`

```typescript
import { useMemo } from 'react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { Permission } from '@/core/domain/Permission';

export interface DocumentPermissions {
  canRead: boolean;
  canWrite: boolean;
  canAdmin: boolean;
  canDelete: boolean;
}

export function useDocumentPermissions(
  documentId: number,
  folderId?: number
): DocumentPermissions {
  const { user } = useAuth();
  
  return useMemo(() => {
    // Obtener permisos del documento o carpeta padre
    const permissions = getPermissionsForResource(
      documentId,
      folderId,
      user.organizationId
    );
    
    return {
      canRead: permissions.includes('LECTURA'),
      canWrite: permissions.includes('ESCRITURA'),
      canAdmin: permissions.includes('ADMINISTRACION'),
      canDelete: permissions.includes('ADMINISTRACION'), // Regla de negocio
    };
  }, [documentId, folderId, user]);
}
```

### 2. Componente de Acciones

**Archivo:** `DocumentActions.tsx`

```typescript
interface DocumentActionsProps {
  document: Document;
  folderId: number;
}

export function DocumentActions({ document, folderId }: DocumentActionsProps) {
  const permissions = useDocumentPermissions(document.id, folderId);
  const { mutate: uploadVersion, isLoading } = useUploadVersion();
  
  const handleUploadVersion = () => {
    if (!permissions.canWrite) {
      toast.error('No tiene permiso de escritura en este documento');
      return;
    }
    
    // Abrir modal de subida
    openUploadModal();
  };
  
  return (
    <div className="flex gap-2">
      <Button
        onClick={handleUploadVersion}
        disabled={!permissions.canWrite || isLoading}
        title={!permissions.canWrite ? 'Requiere permiso de escritura' : undefined}
      >
        Nueva Versión
      </Button>
      
      {!permissions.canWrite && (
        <Tooltip content="Requiere permiso de escritura">
          <InfoIcon className="text-gray-400" />
        </Tooltip>
      )}
    </div>
  );
}
```

### 3. Manejo de Error 403

**Archivo:** `documentService.ts`

```typescript
import axios, { AxiosError } from 'axios';

export async function uploadDocument(
  folderId: number,
  file: File,
  metadata: DocumentMetadata
): Promise<Document> {
  try {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('nombre', metadata.nombre);
    formData.append('descripcion', metadata.descripcion);
    
    const response = await axios.post(
      `/api/carpetas/${folderId}/documentos`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
      }
    );
    
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const axiosError = error as AxiosError<ErrorResponse>;
      
      if (axiosError.response?.status === 403) {
        throw new PermissionDeniedError(
          axiosError.response.data.message || 
          'No tiene permiso para subir documentos en esta carpeta'
        );
      }
    }
    
    throw error;
  }
}

export class PermissionDeniedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'PermissionDeniedError';
  }
}
```

### 4. Validación Previa

**Archivo:** `UploadDocumentModal.tsx`

```typescript
export function UploadDocumentModal({ folderId, isOpen, onClose }: Props) {
  const permissions = useFolderPermissions(folderId);
  
  if (!permissions.canWrite) {
    return (
      <Modal isOpen={isOpen} onClose={onClose}>
        <div className="p-6">
          <AlertCircle className="mx-auto h-12 w-12 text-red-500" />
          <h2 className="mt-4 text-lg font-semibold">Sin Permiso de Escritura</h2>
          <p className="mt-2 text-gray-600">
            No tiene permiso para subir documentos en esta carpeta.
            Contacte al administrador para solicitar acceso.
          </p>
          <Button onClick={onClose} className="mt-4">
            Cerrar
          </Button>
        </div>
      </Modal>
    );
  }
  
  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      {/* Formulario de subida normal */}
    </Modal>
  );
}
```

---

## Tests Requeridos

### Backend - Tests Unitarios

**Archivo:** `RequiereEscrituraGuardTest.java`

```java
@ExtendWith(MockitoExtension.class)
class RequiereEscrituraGuardTest {
    
    @Mock
    private PermissionEnforcementService permissionService;
    
    @InjectMocks
    private RequiereEscrituraGuard guard;
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Test
    void should_permitAccess_when_userHasEscrituraPermission() throws Throwable {
        // Given
        RequiereEscritura annotation = mockAnnotation(TipoRecurso.CARPETA);
        mockRequestWithUser(1L, 1L);
        when(permissionService.tienePermiso(1L, 1L, 123L, TipoRecurso.CARPETA, NivelAcceso.ESCRITURA))
            .thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");
        
        // When
        Object result = guard.enforce(joinPoint, annotation);
        
        // Then
        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
    }
    
    @Test
    void should_throwForbidden_when_userLacksEscrituraPermission() {
        // Given
        RequiereEscritura annotation = mockAnnotation(TipoRecurso.CARPETA);
        mockRequestWithUser(1L, 1L);
        when(permissionService.tienePermiso(any(), any(), any(), any(), any()))
            .thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> guard.enforce(joinPoint, annotation))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Requiere permiso de escritura");
        
        verify(joinPoint, never()).proceed();
    }
    
    @Test
    void should_denyAccess_when_userHasOnlyLecturaPermission() {
        // Given: Usuario con LECTURA pero sin ESCRITURA
        RequiereEscritura annotation = mockAnnotation(TipoRecurso.DOCUMENTO);
        mockRequestWithUser(2L, 1L);
        when(permissionService.tienePermiso(2L, 1L, 456L, TipoRecurso.DOCUMENTO, NivelAcceso.ESCRITURA))
            .thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> guard.enforce(joinPoint, annotation))
            .isInstanceOf(ForbiddenException.class);
    }
}
```

### Backend - Tests de Integración

**Archivo:** `FolderControllerTest.java`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class FolderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PermissionEnforcementService permissionService;
    
    @Test
    @WithMockUser(userId = "1", organizationId = "1")
    void should_createSubfolder_when_userHasEscrituraPermission() throws Exception {
        // Given
        when(permissionService.tienePermiso(1L, 1L, 100L, TipoRecurso.CARPETA, NivelAcceso.ESCRITURA))
            .thenReturn(true);
        
        String requestBody = """
            {
                "nombre": "Nueva Subcarpeta",
                "descripcion": "Test subfolder"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/carpetas/100/subcarpetas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Nueva Subcarpeta"));
    }
    
    @Test
    @WithMockUser(userId = "2", organizationId = "1")
    void should_return403_when_userLacksEscrituraPermission() throws Exception {
        // Given: Usuario sin permiso
        when(permissionService.tienePermiso(2L, 1L, 100L, TipoRecurso.CARPETA, NivelAcceso.ESCRITURA))
            .thenReturn(false);
        
        String requestBody = """
            {
                "nombre": "Subcarpeta Bloqueada",
                "descripcion": "Should fail"
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/api/carpetas/100/subcarpetas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACL_WRITE_DENIED"))
            .andExpect(jsonPath("$.message").value(containsString("permiso de escritura")));
    }
}
```

**Archivo:** `DocumentControllerTest.java`

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DocumentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PermissionEnforcementService permissionService;
    
    @Test
    @WithMockUser(userId = "1", organizationId = "1")
    void should_uploadDocument_when_userHasEscrituraInFolder() throws Exception {
        // Given
        when(permissionService.tienePermiso(1L, 1L, 50L, TipoRecurso.CARPETA, NivelAcceso.ESCRITURA))
            .thenReturn(true);
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test content".getBytes()
        );
        
        // When & Then
        mockMvc.perform(multipart("/api/carpetas/50/documentos")
                .file(file)
                .param("nombre", "Test Document")
                .param("descripcion", "Test description"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nombre").value("Test Document"));
    }
    
    @Test
    @WithMockUser(userId = "3", organizationId = "2")
    void should_return404_when_userFromDifferentOrganization() throws Exception {
        // Given: Usuario de otra organización
        when(permissionService.tienePermiso(3L, 2L, 50L, TipoRecurso.CARPETA, NivelAcceso.ESCRITURA))
            .thenReturn(false); // No encuentra el recurso
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "Test".getBytes()
        );
        
        // When & Then
        mockMvc.perform(multipart("/api/carpetas/50/documentos")
                .file(file))
            .andExpect(status().isNotFound()); // No revelar existencia
    }
    
    @Test
    @WithMockUser(userId = "1", organizationId = "1")
    void should_return403_when_movingDocumentWithoutDestinationWritePermission() throws Exception {
        // Given: Usuario con ESCRITURA en origen pero NO en destino
        when(permissionService.tienePermiso(1L, 1L, 100L, TipoRecurso.DOCUMENTO, NivelAcceso.ESCRITURA))
            .thenReturn(true); // Permiso en documento origen
        when(permissionService.tienePermiso(1L, 1L, 200L, TipoRecurso.CARPETA, NivelAcceso.ESCRITURA))
            .thenReturn(false); // Sin permiso en carpeta destino
        
        String requestBody = """
            {
                "carpetaDestinoId": 200
            }
            """;
        
        // When & Then
        mockMvc.perform(patch("/api/documentos/100/mover")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message").value(containsString("carpeta destino")));
    }
}
```

## Requisitos No Funcionales

### Seguridad
1. **Validación de Organización**: Todos los endpoints deben verificar que el recurso pertenece a la organización del usuario
2. **No revelar existencia**: Retornar 404 en lugar de 403 cuando usuario de otra organización intenta acceder
3. **Auditoría completa**: Registrar todos los intentos de escritura denegados con:
   - Usuario ID
   - Recurso ID y tipo
   - Timestamp
   - IP de origen
   - Acción intentada

### Performance
1. **Cache de permisos**: Cachear evaluación de permisos por 5 minutos (TTL configurable)
2. **Consultas optimizadas**: Usar joins para evitar N+1 queries
3. **Índices requeridos**:
   - `idx_permissions_user_org_resource` en (user_id, organization_id, resource_id, resource_type)
   - `idx_permissions_level` en (nivel_acceso_id)

### Observabilidad
1. **Métricas**:
   - Contador de 403 por endpoint
   - Latencia p95 de verificación de permisos
   - Rate de denegaciones por usuario
2. **Logging**:
   - Level INFO para operaciones permitidas
   - Level WARN para denegaciones
   - Level ERROR para excepciones inesperadas
3. **Alertas**:
   - Alerta si tasa de 403 supera 20% en 5 minutos
   - Alerta si mismo usuario recibe >10 denegaciones en 1 minuto (posible ataque)

---

## Definición de Terminado (DoD)

- [ ] Guard `RequiereEscrituraGuard` implementado y probado unitariamente
- [ ] Anotación `@RequiereEscritura` aplicada a todos los endpoints de escritura listados
- [ ] Tests de integración E2E para cada endpoint protegido (mínimo 3 casos: con permiso, sin permiso, otra org)
- [ ] Frontend deshabilita botones de acciones sin permiso
- [ ] Frontend muestra tooltips explicativos
- [ ] Manejo de error 403 implementado con mensajes claros
- [ ] Auditoría registra intentos denegados con código ACL_WRITE_DENIED
- [ ] Documentación actualizada en README y docs/
- [ ] Code review aprobado por al menos 2 desarrolladores
- [ ] Coverage de tests ≥90% en nuevo código
- [ ] Tests de seguridad ejecutados y pasando
- [ ] Performance validado (verificación de permisos <50ms p95)

---

## Dependencias

### Depende de:
- [US-ACL-006] Evaluador de permisos heredados (CRÍTICO)
- [US-ACL-001] Catálogo de niveles de acceso (tabla `nivel_acceso`)
- [US-ACL-002] Modelo ACL (tabla `permissions`)
- Endpoints de carpetas de P3 (Gestión Documental)
- Endpoints de documentos de P4 (Subida y Versionado)

### Bloquea a:
- [US-ACL-009] Endpoints de consulta de capacidades (depende de guard funcional)
- Todas las US de P4 y P5 que requieren escritura controlada

---

## Notas de Implementación

1. **Orden de verificación**: Siempre verificar permisos ANTES de lógica de negocio para evitar side effects
2. **Transacciones**: Usar `@Transactional` en servicios, no en controladores
3. **Cleanup**: Si operación falla por permisos después de subir archivo, limpiar archivo de MinIO
4. **Rate limiting**: Considerar rate limiting por usuario en endpoints de escritura
5. **Soft delete**: Verificar que soft-deleted resources no sean accesibles
6. **Herencia**: Recordar que herencia de permisos aplica (carpeta → documento)

---

## Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Performance de verificación de permisos en operaciones masivas | Media | Alto | Implementar cache de permisos y batch verification |
| Race condition: permiso revocado durante operación larga | Baja | Medio | Verificar permiso al inicio Y al finalizar operación |
| Usuario frustrado por acciones deshabilitadas sin explicación | Alta | Bajo | Tooltips claros y mensajes de error informativos |
| Bypass de permisos mediante manipulación de request | Baja | Crítico | Extraer organization_id solo de token JWT, nunca de body |
| Inconsistencia entre permisos frontend y backend | Media | Medio | Centralizar lógica de permisos y sincronizar con backend |
