# US-DOC-008: Eliminación de Documento desde la UI

## [original]

**[US-DOC-008] Eliminación de Documento desde la UI**

**Narrativa:** Como administrador o usuario con permiso de escritura, quiero eliminar documentos desde la UI marcándolos como eliminados (soft delete), para mantener la organización del sistema sin perder trazabilidad ni historial para auditoría.

**Criterios de Aceptación:**

- **Escenario 1:** Eliminación exitosa con permiso de escritura
  - Dado que soy un usuario con permiso de ESCRITURA o ADMINISTRACION sobre un documento
  - Cuando hago clic en el botón "Eliminar" y confirmo la acción en el diálogo de confirmación
  - Entonces el documento queda marcado con fecha_eliminacion en la base de datos (soft delete)
  - Y el documento desaparece inmediatamente de la lista visible en la UI
  - Y se muestra una notificación "Documento eliminado exitosamente"
  - Y se emite un evento de auditoría DOCUMENTO_ELIMINADO

- **Escenario 2:** Diálogo de confirmación antes de eliminar
  - Dado que tengo permiso para eliminar un documento
  - Cuando hago clic en el botón "Eliminar"
  - Entonces aparece un diálogo modal de confirmación
  - Y el diálogo muestra el nombre del documento y advierte que la acción no es reversible en la UI
  - Y debo confirmar explícitamente antes de proceder

- **Escenario 3:** Cancelación de eliminación
  - Dado que he abierto el diálogo de confirmación de eliminación
  - Cuando hago clic en "Cancelar" o cierro el diálogo
  - Entonces el diálogo se cierra sin realizar cambios
  - Y el documento permanece visible en la lista

- **Escenario 4:** Botón no visible sin permiso de escritura
  - Dado que soy un usuario con solo permiso de LECTURA sobre un documento
  - Cuando visualizo la lista de documentos
  - Entonces el botón "Eliminar" no aparece o está claramente deshabilitado para ese documento

- **Escenario 5:** Error de permisos al eliminar
  - Dado que intento eliminar un documento sin permisos suficientes
  - Cuando el backend rechaza la operación
  - Entonces se muestra un error "No tiene permisos para eliminar este documento"
  - Y el documento permanece visible en la lista

- **Escenario 6:** Manejo de error en eliminación
  - Dado que tengo permisos para eliminar
  - Cuando ocurre un error durante la eliminación (error de red, BD, etc.)
  - Entonces se muestra una notificación de error descriptiva
  - Y el documento permanece visible en la lista
  - Y puedo reintentar la operación

- **Escenario 7:** Validación de tenant isolation
  - Dado que un usuario intenta eliminar un documento de otra organización
  - Cuando se procesa la solicitud
  - Entonces el backend retorna 404 (no 403) para no filtrar existencia
  - Y la UI muestra "Documento no encontrado"

---

## [enhanced]

## 1. Descripción General

Esta User Story implementa la funcionalidad de eliminación lógica (soft delete) de documentos desde la interfaz de usuario. La implementación sigue el principio de arquitectura hexagonal en el backend y Feature-Driven Clean Architecture en el frontend, garantizando seguridad mediante ACL (Access Control List), trazabilidad mediante eventos de auditoría e inmutabilidad del historial.

**Objetivos clave:**
- Implementar endpoint `DELETE /api/documentos/{id}` en backend
- Crear componente de botón de eliminación con confirmación en frontend
- Marcar documentos como eliminados mediante campo `fecha_eliminacion` (soft delete)
- Validar permisos de ESCRITURA o ADMINISTRACION antes de eliminar
- Emitir evento de auditoría `DOCUMENTO_ELIMINADO`
- Actualizar lista de documentos en tiempo real tras eliminación
- Implementar diálogo de confirmación con UX clara
- Manejar errores de forma descriptiva y amigable

---

## 2. Contexto de Arquitectura

### 2.1 Backend — Capas Involucradas

**Capa de Dominio** (`backend/document-core/src/main/java/com/docflow/documentcore/domain/`)
- Modelo `Document` con campo `deletedAt` nullable
- Interfaz `IDocumentRepository` con método `softDelete()`
- Servicio de dominio para validación de operaciones de eliminación

**Capa de Aplicación** (`backend/document-core/src/main/java/com/docflow/documentcore/application/`)
- `DocumentService` con método `deleteDocument()`
- `DocumentValidator` para validar estado del documento antes de eliminar
- `AuditService` para emitir evento `DOCUMENTO_ELIMINADO`
- DTOs: `DeleteDocumentResponse` (opcional, puede ser 204 No Content)

**Capa de Infraestructura** (`backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/`)
- Entidad JPA `DocumentEntity` con campo `fechaEliminacion`
- `DocumentJpaRepository` con query personalizada para soft delete
- `DocumentRepositoryImpl` implementando soft delete
- `PermissionCheckAdapter` para validar permisos ACL

**Capa de Presentación** (`backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/`)
- `DocumentoController` con endpoint `DELETE /api/documentos/{id}`
- Manejador de excepciones global para errores 403, 404, 500

### 2.2 Frontend — Componentes Involucrados

**Feature: `documents`**
- **Ubicación:** `frontend/src/features/documents/`
- **API Service:** `documentService.ts` - Función `deleteDocument(documentId)`
- **Hook:** `useDocumentDelete.ts` - Hook personalizado para lógica de eliminación
- **Componentes:**
  - `DocumentDeleteButton.tsx` - Botón de eliminación con estados
  - `DocumentDeleteConfirmDialog.tsx` - Diálogo modal de confirmación

**Feature: `acl`**
- **Ubicación:** `frontend/src/features/acl/`
- **Uso:** Validar permisos antes de mostrar botón de eliminación
- **Componentes:** `AclDocumentoList.tsx` - Lista de documentos con permisos

**Common:**
- **Notificaciones:** `NotificationCentre` o sistema de toast/alerts
- **Permisos:** Constantes de permisos (`ESCRITURA`, `ADMINISTRACION`)
- **Cliente API:** `axiosInstance` configurado con JWT

---

## 3. Plan de Implementación Backend

### Paso 1: Actualizar Migración de Base de Datos (Ya existe)

**Archivo:** `backend/document-core/src/main/resources/db/migration/V1__create_document_tables.sql`

**Verificación:** Confirmar que la tabla `documento` ya tiene el campo:
```sql
fecha_eliminacion TIMESTAMP NULLABLE
```

**Acción:** Si no existe, crear migración adicional para agregarlo.

**Notas:**
- El soft delete se implementa mediante `fecha_eliminacion IS NOT NULL`
- Los queries deben filtrar `fecha_eliminacion IS NULL` para documentos activos
- Las restricciones únicas deben incluir `fecha_eliminacion IS NULL`

---

### Paso 2: Actualizar Entidad JPA — DocumentEntity

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/persistence/entity/DocumentEntity.java`

**Acción:** Verificar que existe el campo `fechaEliminacion` mapeado correctamente.

**Código esperado:**
```java
@Column(name = "fecha_eliminacion")
private LocalDateTime fechaEliminacion;
```

**Dependencias:** Ninguna (ya debe existir de migraciones anteriores).

---

### Paso 3: Actualizar Repository — Agregar Método de Soft Delete

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/persistence/repository/DocumentJpaRepository.java`

**Acción:** Agregar método personalizado para soft delete con validación de tenant.

**Código a agregar:**
```java
@Modifying
@Transactional
@Query("""
    UPDATE DocumentEntity d 
    SET d.fechaEliminacion = :fechaEliminacion 
    WHERE d.id = :documentId 
      AND d.organizacionId = :organizacionId 
      AND d.fechaEliminacion IS NULL
    """)
int softDeleteDocument(
    @Param("documentId") Long documentId, 
    @Param("organizacionId") Long organizacionId, 
    @Param("fechaEliminacion") LocalDateTime fechaEliminacion
);
```

**Notas:**
- Retorna 0 si el documento ya está eliminado o no existe
- Retorna 1 si la eliminación fue exitosa
- La condición `d.organizacionId = :organizacionId` garantiza tenant isolation

---

### Paso 4: Implementar Servicio de Aplicación — DocumentService.deleteDocument()

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/application/service/DocumentService.java`

**Acción:** Implementar método `deleteDocument()` con validación de permisos y auditoría.

**Firma del método:**
```java
public void deleteDocument(Long documentId, Long usuarioId, Long organizacionId)
```

**Pasos de implementación:**
1. Buscar documento por ID y validar que `organizacionId` coincide
   - Si no existe o es de otra org → lanzar `DocumentNotFoundException` (mapear a 404)
2. Validar que `fechaEliminacion IS NULL` (no está eliminado)
   - Si ya eliminado → lanzar `DocumentAlreadyDeletedException` (mapear a 409 Conflict)
3. Validar permisos de usuario (ESCRITURA o ADMINISTRACION)
   - Llamar a `PermissionCheckAdapter.hasPermission(usuarioId, documentId, List.of(ESCRITURA, ADMINISTRACION))`
   - Si sin permisos → lanzar `InsufficientPermissionsException` (mapear a 403)
4. Ejecutar soft delete
   - Llamar `documentJpaRepository.softDeleteDocument(documentId, organizacionId, LocalDateTime.now())`
   - Verificar que retorna 1 (documento actualizado)
5. Emitir evento de auditoría
   - Llamar `auditService.emitEvent(DOCUMENTO_ELIMINADO, usuarioId, documentId, organizacionId)`
6. Log de operación exitosa

**Dependencias requeridas:**
```java
@Autowired
private DocumentJpaRepository documentJpaRepository;

@Autowired
private PermissionCheckAdapter permissionCheckAdapter;

@Autowired
private AuditService auditService;
```

**Excepciones personalizadas:**
- `DocumentNotFoundException` (404)
- `DocumentAlreadyDeletedException` (409)
- `InsufficientPermissionsException` (403)

---

### Paso 5: Crear Endpoint REST — DELETE /api/documentos/{id}

**Archivo:** `backend/document-core/src/main/java/com/docflow/documentcore/infrastructure/adapter/controller/DocumentoController.java`

**Acción:** Agregar endpoint `DELETE /api/documentos/{id}` con documentación Swagger.

**Código a agregar:**
```java
/**
 * Elimina (soft delete) un documento del sistema.
 * 
 * <p>Marca el documento con fecha_eliminacion sin eliminar físicamente
 * el registro ni los archivos de versiones. Esto permite mantener
 * trazabilidad y auditoría completa.
 * 
 * <p><b>Validaciones:</b>
 * <ul>
 *   <li>Documento debe existir y pertenecer a la organización del usuario</li>
 *   <li>Usuario debe tener permiso de ESCRITURA o ADMINISTRACION sobre el documento</li>
 *   <li>Documento no debe estar ya eliminado</li>
 * </ul>
 * 
 * <p><b>Auditoría:</b> Emite evento DOCUMENTO_ELIMINADO para trazabilidad.
 * 
 * <p><b>US-DOC-008:</b> Eliminación de documento desde la UI.
 * 
 * @param id ID del documento a eliminar
 * @param usuarioId ID del usuario autenticado (header X-User-Id)
 * @param organizacionId ID de la organización (header X-Organization-Id)
 * @return ResponseEntity sin contenido (204) si exitoso
 */
@DeleteMapping("/{id}")
@Operation(
    summary = "Eliminar documento (soft delete)",
    description = """
        Marca un documento como eliminado sin borrar físicamente los datos.
        
        La operación requiere:
        - Documento debe existir y pertenecer a la organización del usuario (tenant isolation)
        - Usuario debe tener permiso de ESCRITURA o ADMINISTRACION sobre el documento
        - Documento no debe estar ya eliminado
        
        La eliminación es lógica (soft delete) mediante campo fecha_eliminacion.
        Se emite un evento de auditoría DOCUMENTO_ELIMINADO.
        
        Importante para seguridad:
        - HTTP 404 en lugar de 403 para documentos de otras organizaciones
        - HTTP 403 solo cuando el usuario está autenticado pero sin permisos en su propia organización
        - HTTP 409 si el documento ya está eliminado
        """
)
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "204",
        description = "Documento eliminado exitosamente"
    ),
    @ApiResponse(
        responseCode = "401",
        description = "No autenticado - Token JWT ausente o inválido",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    ),
    @ApiResponse(
        responseCode = "403",
        description = "Sin permiso de ESCRITURA o ADMINISTRACION sobre el documento",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Documento no encontrado o pertenece a otra organización",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    ),
    @ApiResponse(
        responseCode = "409",
        description = "Documento ya está eliminado",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "Error interno del servidor",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    )
})
public ResponseEntity<Void> deleteDocument(
        @PathVariable
        @Parameter(description = "ID del documento a eliminar", required = true, example = "100")
        Long id,
        
        @RequestHeader(value = "X-User-Id", required = true)
        @Parameter(description = "ID del usuario autenticado (inyectado por gateway)", required = true, example = "1")
        Long usuarioId,
        
        @RequestHeader(value = "X-Organization-Id", required = true)
        @Parameter(description = "ID de la organización del usuario (inyectado por gateway)", required = true, example = "1")
        Long organizacionId
) {
    log.info("REST: DELETE /api/documentos/{} - usuarioId={}, orgId={}", 
             id, usuarioId, organizacionId);
    
    documentService.deleteDocument(id, usuarioId, organizacionId);
    
    log.info("REST: Document deleted successfully - documentoId={}", id);
    return ResponseEntity.noContent().build();
}
```

**Notas de implementación:**
- Retornar `204 No Content` (sin body) en caso de éxito
- Los headers `X-User-Id` y `X-Organization-Id` son inyectados por el gateway
- Documentación Swagger completa para facilitar testing e integración

---

### Paso 6: Actualizar Queries Existentes — Filtrar Documentos Eliminados

**Archivos afectados:**
- `DocumentJpaRepository.java`
- `DocumentRepositoryImpl.java`
- Cualquier query que liste o busque documentos

**Acción:** Agregar condición `WHERE fecha_eliminacion IS NULL` a todos los queries de consulta.

**Ejemplo de query actualizado:**
```java
@Query("""
    SELECT d FROM DocumentEntity d 
    WHERE d.carpetaId = :carpetaId 
      AND d.organizacionId = :organizacionId 
      AND d.fechaEliminacion IS NULL
    ORDER BY d.fechaCreacion DESC
    """)
List<DocumentEntity> findByCarpetaIdAndOrganizacionId(
    @Param("carpetaId") Long carpetaId,
    @Param("organizacionId") Long organizacionId
);
```

**Notas:**
- Esto garantiza que documentos eliminados no aparezcan en listados
- Documentos eliminados siguen accesibles para auditoría mediante queries específicos

---

### Paso 7: Implementar Tests Unitarios — DocumentServiceTest

**Archivo:** `backend/document-core/src/test/java/com/docflow/documentcore/application/service/DocumentServiceTest.java`

**Acción:** Crear tests unitarios para `deleteDocument()` con todos los escenarios.

**Tests requeridos:**
1. `testDeleteDocument_Success()` - Eliminación exitosa con permisos correctos
2. `testDeleteDocument_DocumentNotFound()` - Documento no existe → 404
3. `testDeleteDocument_DocumentFromOtherOrganization()` - Tenant isolation → 404
4. `testDeleteDocument_InsufficientPermissions()` - Usuario sin permisos → 403
5. `testDeleteDocument_AlreadyDeleted()` - Documento ya eliminado → 409
6. `testDeleteDocument_EmitsAuditEvent()` - Verifica evento de auditoría

**Dependencias de test:**
```java
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    @Mock
    private DocumentJpaRepository documentJpaRepository;
    
    @Mock
    private PermissionCheckAdapter permissionCheckAdapter;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private DocumentService documentService;
    
    // Tests...
}
```

**Cobertura requerida:** 90% mínimo (según estándares del proyecto).

---

### Paso 8: Implementar Tests de Integración — DocumentControllerIT

**Archivo:** `backend/document-core/src/test/java/com/docflow/documentcore/infrastructure/adapter/controller/DocumentControllerIT.java`

**Acción:** Crear tests de integración para endpoint `DELETE /api/documentos/{id}`.

**Tests requeridos:**
1. `testDeleteDocument_Success()` - E2E exitoso (201 → 204)
2. `testDeleteDocument_Unauthorized()` - Sin JWT → 401
3. `testDeleteDocument_Forbidden()` - Sin permisos → 403
4. `testDeleteDocument_NotFound()` - Documento inexistente → 404
5. `testDeleteDocument_Conflict()` - Ya eliminado → 409

**Ejemplo de test:**
```java
@Test
void testDeleteDocument_Success() throws Exception {
    // Given
    Long documentId = 1L;
    Long usuarioId = 1L;
    Long organizacionId = 1L;
    
    // When & Then
    mockMvc.perform(delete("/api/documentos/{id}", documentId)
            .header("X-User-Id", usuarioId)
            .header("X-Organization-Id", organizacionId)
            .header("Authorization", "Bearer " + jwtToken))
        .andExpect(status().isNoContent());
    
    // Verify soft delete
    DocumentEntity doc = documentRepository.findById(documentId).orElseThrow();
    assertThat(doc.getFechaEliminacion()).isNotNull();
}
```

---

## 4. Plan de Implementación Frontend

### Paso 1: Extender Servicio API — documentService.ts

**Archivo:** `frontend/src/features/documents/api/documentService.ts`

**Acción:** Agregar función `deleteDocument()` para llamar al endpoint DELETE.

**Código a agregar:**
```typescript
/**
 * Elimina (soft delete) un documento del sistema.
 * 
 * @param documentId ID del documento a eliminar
 * @throws AxiosError con códigos:
 *   - 401: No autenticado
 *   - 403: Sin permisos
 *   - 404: Documento no encontrado
 *   - 409: Documento ya eliminado
 */
export async function deleteDocument(documentId: string): Promise<void> {
  await apiClient.delete(`/api/documentos/${documentId}`);
}
```

**Dependencias:**
```typescript
import { apiClient } from '@core/shared/api/axiosInstance';
```

**Notas:**
- La función no retorna datos (204 No Content)
- Los headers JWT, X-User-Id, X-Organization-Id se agregan automáticamente por interceptores
- Errores se propagan para manejo en componentes

---

### Paso 2: Crear Hook Personalizado — useDocumentDelete

**Archivo:** `frontend/src/features/documents/hooks/useDocumentDelete.ts` (nuevo)

**Acción:** Crear hook que orqueste lógica de eliminación con estados y notificaciones.

**Código completo:**
```typescript
import { useState } from 'react';
import { deleteDocument } from '../api/documentService';
import { useNotification } from '@common/hooks/useNotification';
import { AxiosError } from 'axios';

interface UseDocumentDeleteReturn {
  isDeleting: boolean;
  error: string | null;
  deleteDocumentWithConfirmation: (documentId: string, documentName: string) => Promise<boolean>;
  clearError: () => void;
}

/**
 * Hook personalizado para manejar eliminación de documentos.
 * 
 * Proporciona estados de carga, manejo de errores y notificaciones.
 * 
 * @returns {UseDocumentDeleteReturn} Estados y funciones para eliminación
 */
export function useDocumentDelete(): UseDocumentDeleteReturn {
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { showSuccess, showError } = useNotification();

  const deleteDocumentWithConfirmation = async (
    documentId: string,
    documentName: string
  ): Promise<boolean> => {
    // Validaciones iniciales
    if (!documentId) {
      setError('ID de documento inválido');
      return false;
    }

    setIsDeleting(true);
    setError(null);

    try {
      await deleteDocument(documentId);
      
      // Notificación de éxito
      showSuccess(`Documento "${documentName}" eliminado exitosamente`);
      
      return true;
    } catch (err) {
      const axiosError = err as AxiosError<{ title?: string; detail?: string }>;
      
      let errorMessage: string;
      
      switch (axiosError.response?.status) {
        case 401:
          errorMessage = 'Sesión expirada. Por favor, inicie sesión nuevamente';
          break;
        case 403:
          errorMessage = 'No tiene permisos para eliminar este documento';
          break;
        case 404:
          errorMessage = 'Documento no encontrado';
          break;
        case 409:
          errorMessage = 'El documento ya está eliminado';
          break;
        default:
          errorMessage = axiosError.response?.data?.detail 
            || 'Error al eliminar el documento. Por favor, intente nuevamente';
      }
      
      setError(errorMessage);
      showError(errorMessage);
      
      return false;
    } finally {
      setIsDeleting(false);
    }
  };

  const clearError = () => {
    setError(null);
  };

  return {
    isDeleting,
    error,
    deleteDocumentWithConfirmation,
    clearError
  };
}
```

**Notas de implementación:**
- Retorna `boolean` indicando si la eliminación fue exitosa
- Maneja todos los códigos de error del backend con mensajes amigables
- Integra con sistema de notificaciones del proyecto
- Estado `isDeleting` para mostrar spinners en UI

---

### Paso 3: Crear Componente Diálogo de Confirmación

**Archivo:** `frontend/src/features/documents/components/DocumentDeleteConfirmDialog.tsx` (nuevo)

**Acción:** Crear diálogo modal de confirmación reutilizable.

**Código completo:**
```typescript
import React from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@common/components/Dialog';
import { Button } from '@common/components/Button';
import { AlertTriangle } from 'lucide-react';

interface DocumentDeleteConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  documentName: string;
  isDeleting: boolean;
}

/**
 * Diálogo de confirmación para eliminación de documentos.
 * 
 * Muestra advertencia clara y requiere confirmación explícita.
 */
export function DocumentDeleteConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  documentName,
  isDeleting
}: DocumentDeleteConfirmDialogProps) {
  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-red-600">
            <AlertTriangle className="h-5 w-5" />
            Confirmar eliminación
          </DialogTitle>
        </DialogHeader>
        
        <div className="py-4">
          <p className="text-sm text-gray-600 mb-3">
            ¿Está seguro que desea eliminar el siguiente documento?
          </p>
          <p className="font-semibold text-gray-900 mb-3">
            {documentName}
          </p>
          <p className="text-sm text-red-600 bg-red-50 p-3 rounded border border-red-200">
            ⚠️ Esta acción no se puede deshacer desde la interfaz de usuario.
            El documento quedará marcado como eliminado.
          </p>
        </div>
        
        <DialogFooter>
          <Button
            variant="secondary"
            onClick={onClose}
            disabled={isDeleting}
          >
            Cancelar
          </Button>
          <Button
            variant="danger"
            onClick={onConfirm}
            disabled={isDeleting}
            isLoading={isDeleting}
          >
            {isDeleting ? 'Eliminando...' : 'Eliminar documento'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
```

**Dependencias:**
```typescript
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@common/components/Dialog';
import { Button } from '@common/components/Button';
import { AlertTriangle } from 'lucide-react';
```

**Notas de diseño:**
- Color rojo para indicar acción destructiva
- Icono de advertencia para llamar la atención
- Mensaje claro sobre irreversibilidad
- Botón de confirmar deshabilitado durante operación
- Cierre automático al hacer clic fuera (opcional)

---

### Paso 4: Crear Componente Botón de Eliminación

**Archivo:** `frontend/src/features/documents/components/DocumentDeleteButton.tsx` (nuevo)

**Acción:** Crear botón reutilizable que orquesta diálogo + eliminación.

**Código completo:**
```typescript
import React, { useState } from 'react';
import { Trash2 } from 'lucide-react';
import { Button } from '@common/components/Button';
import { DocumentDeleteConfirmDialog } from './DocumentDeleteConfirmDialog';
import { useDocumentDelete } from '../hooks/useDocumentDelete';

interface DocumentDeleteButtonProps {
  documentId: string;
  documentName: string;
  canDelete: boolean;
  onDeleteSuccess?: () => void;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'danger' | 'ghost' | 'outline';
  showLabel?: boolean;
  className?: string;
}

/**
 * Botón de eliminación de documentos con confirmación.
 * 
 * Valida permisos, muestra diálogo de confirmación y ejecuta eliminación.
 * Emite callback onDeleteSuccess para refrescar listas.
 */
export function DocumentDeleteButton({
  documentId,
  documentName,
  canDelete,
  onDeleteSuccess,
  disabled = false,
  size = 'sm',
  variant = 'ghost',
  showLabel = false,
  className = ''
}: DocumentDeleteButtonProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const { isDeleting, deleteDocumentWithConfirmation } = useDocumentDelete();

  const handleDeleteClick = () => {
    if (canDelete && !disabled) {
      setIsDialogOpen(true);
    }
  };

  const handleConfirmDelete = async () => {
    const success = await deleteDocumentWithConfirmation(documentId, documentName);
    
    if (success) {
      setIsDialogOpen(false);
      onDeleteSuccess?.();
    }
  };

  const handleCancelDelete = () => {
    setIsDialogOpen(false);
  };

  // No renderizar si no tiene permisos
  if (!canDelete) {
    return null;
  }

  return (
    <>
      <Button
        variant={variant}
        size={size}
        onClick={handleDeleteClick}
        disabled={disabled || isDeleting}
        className={className}
        title="Eliminar documento"
      >
        <Trash2 className="h-4 w-4" />
        {showLabel && <span className="ml-2">Eliminar</span>}
      </Button>

      <DocumentDeleteConfirmDialog
        isOpen={isDialogOpen}
        onClose={handleCancelDelete}
        onConfirm={handleConfirmDelete}
        documentName={documentName}
        isDeleting={isDeleting}
      />
    </>
  );
}
```

**Notas de implementación:**
- Renderiza `null` si `canDelete === false` (simplifica lógica en componentes padre)
- Callback `onDeleteSuccess` para invalidar cache de React Query o refrescar listas
- Botón deshabilitado durante eliminación
- Variantes de estilo flexibles para diferentes contextos

---

### Paso 5: Integrar en Lista de Documentos

**Archivo:** `frontend/src/features/folders/components/FolderList.tsx` o `frontend/src/features/acl/components/AclDocumentoList.tsx`

**Acción:** Agregar `DocumentDeleteButton` en cada fila de documento.

**Ejemplo de integración:**
```typescript
import { DocumentDeleteButton } from '@features/documents/components/DocumentDeleteButton';
import { useQueryClient } from '@tanstack/react-query';

// Dentro del componente de lista
const queryClient = useQueryClient();

const handleDeleteSuccess = () => {
  // Invalidar cache de React Query para refrescar lista
  queryClient.invalidateQueries(['documents', folderId]);
};

// En el render de cada documento
<DocumentDeleteButton
  documentId={documento.id}
  documentName={documento.nombre}
  canDelete={documento.permisoEfectivo === 'ESCRITURA' || documento.permisoEfectivo === 'ADMINISTRACION'}
  onDeleteSuccess={handleDeleteSuccess}
  size="sm"
  variant="ghost"
/>
```

**Validación de permisos:**
```typescript
const canDelete = ['ESCRITURA', 'ADMINISTRACION'].includes(documento.permisoEfectivo);
```

**Notas:**
- Usar React Query's `invalidateQueries` para refrescar lista automáticamente
- Mostrar botón solo si `canDelete === true`
- Posicionar botón en menú de acciones o como columna en tabla

---

### Paso 6: Implementar Tests Frontend — DocumentDeleteButton.test.tsx

**Archivo:** `frontend/src/features/documents/components/DocumentDeleteButton.test.tsx` (nuevo)

**Acción:** Crear tests unitarios para componente de botón.

**Tests requeridos:**
1. `renders null when canDelete is false` - No renderiza sin permisos
2. `opens confirmation dialog on click` - Abre diálogo al hacer clic
3. `calls delete and onDeleteSuccess on confirm` - Ejecuta eliminación al confirmar
4. `closes dialog on cancel` - Cierra diálogo al cancelar
5. `disables button while deleting` - Deshabilita durante operación
6. `shows error notification on failure` - Muestra error si falla

**Ejemplo de test:**
```typescript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { DocumentDeleteButton } from './DocumentDeleteButton';
import { useDocumentDelete } from '../hooks/useDocumentDelete';
import { vi } from 'vitest';

vi.mock('../hooks/useDocumentDelete');

describe('DocumentDeleteButton', () => {
  it('renders null when canDelete is false', () => {
    const { container } = render(
      <DocumentDeleteButton
        documentId="1"
        documentName="Test.pdf"
        canDelete={false}
      />
    );
    
    expect(container.firstChild).toBeNull();
  });

  it('opens confirmation dialog on click', () => {
    (useDocumentDelete as any).mockReturnValue({
      isDeleting: false,
      deleteDocumentWithConfirmation: vi.fn()
    });

    render(
      <DocumentDeleteButton
        documentId="1"
        documentName="Test.pdf"
        canDelete={true}
      />
    );
    
    const button = screen.getByRole('button');
    fireEvent.click(button);
    
    expect(screen.getByText(/Confirmar eliminación/i)).toBeInTheDocument();
  });
});
```

**Cobertura requerida:** 90% mínimo.

---

## 5. Consideraciones de Seguridad

### 5.1 Tenant Isolation
- **Backend:** Todas las queries deben incluir filtro `organizacionId`
- **Retorno 404:** Para documentos de otras organizaciones (no 403)
- **Validación doble:** En repository y en service layer

### 5.2 Validación de Permisos
- **ACL Check:** Verificar permisos ESCRITURA o ADMINISTRACION antes de eliminar
- **Frontend:** Ocultar botón si no tiene permisos (UX)
- **Backend:** Validar siempre en backend (no confiar en frontend)

### 5.3 Auditoría
- **Evento obligatorio:** `DOCUMENTO_ELIMINADO` con userId, documentId, timestamp
- **Inmutabilidad:** Eventos de auditoría no deben ser modificables
- **Trazabilidad:** Permitir recuperar quién eliminó qué y cuándo

---

## 6. Criterios de Aceptación Verificables

### Backend
- [ ] Endpoint `DELETE /api/documentos/{id}` retorna 204 en éxito
- [ ] Validación de tenant isolation (404 para otras organizaciones)
- [ ] Validación de permisos (403 si sin permisos)
- [ ] Soft delete mediante `fecha_eliminacion`
- [ ] Evento de auditoría `DOCUMENTO_ELIMINADO` emitido
- [ ] Tests unitarios con 90%+ cobertura
- [ ] Tests de integración para todos los escenarios
- [ ] Documentación Swagger completa

### Frontend
- [ ] Botón de eliminación solo visible con permisos ESCRITURA/ADMINISTRACION
- [ ] Diálogo de confirmación antes de eliminar
- [ ] Notificación de éxito tras eliminación
- [ ] Manejo de errores con mensajes descriptivos
- [ ] Actualización automática de lista tras eliminación
- [ ] Botón deshabilitado durante operación
- [ ] Tests unitarios con 90%+ cobertura
- [ ] Componentes reutilizables y tipados con TypeScript

---

## 7. Tareas Técnicas Específicas

### Backend
1. ✅ Verificar campo `fecha_eliminacion` en migración
2. ✅ Actualizar `DocumentEntity` con mapeo JPA
3. ✅ Agregar método `softDeleteDocument()` en repository
4. ✅ Implementar `documentService.deleteDocument()` con validaciones
5. ✅ Crear endpoint `DELETE /api/documentos/{id}` en controller
6. ✅ Actualizar queries existentes para filtrar eliminados
7. ✅ Implementar tests unitarios `DocumentServiceTest`
8. ✅ Implementar tests de integración `DocumentControllerIT`
9. ✅ Documentar endpoint en Swagger/OpenAPI

### Frontend
1. ✅ Crear función `deleteDocument()` en `documentService.ts`
2. ✅ Crear hook `useDocumentDelete()` con lógica de eliminación
3. ✅ Crear componente `DocumentDeleteConfirmDialog`
4. ✅ Crear componente `DocumentDeleteButton`
5. ✅ Integrar botón en `FolderList` / `AclDocumentoList`
6. ✅ Configurar invalidación de cache React Query
7. ✅ Implementar tests unitarios para componentes
8. ✅ Validar UX con diseñador/producto

---

## 8. Estimación de Esfuerzo

- **Backend:** 6-8 horas (implementación + tests)
- **Frontend:** 8-10 horas (componentes + integración + tests)
- **QA/Testing:** 3-4 horas (E2E, validación manual)
- **Total:** 17-22 horas (~3 días)

---

## 9. Dependencias y Bloqueos

- ✅ Sistema de autenticación JWT operativo
- ✅ Sistema de permisos ACL implementado
- ✅ Sistema de auditoría implementado
- ✅ Frontend con React Query configurado
- ✅ Componentes comunes (Dialog, Button) disponibles

**No hay dependencias bloqueantes identificadas.**

---

## 10. Documentación Adicional

- [Backend Standards](../../ai-specs/specs/backend-standards.md)
- [Frontend Standards](../../ai-specs/specs/frontend-standards.md)
- [Data Model](../../ai-specs/specs/data-model.md)
- [US-DOC-001 Backend](../../ai-specs/changes/US-DOC-001_backend.md) - Referencia de patrón
- [US-DOC-007 Frontend](../../ai-specs/changes/US-DOC-007_frontend.md) - Referencia de patrón

---

**Fin del documento enriquecido US-DOC-008**
