## P4 — Documentos + Versionado Lineal

### [US-DOC-002] Descargar versión actual (API)

### Narrativa de Usuario
**Como** usuario con permiso de `LECTURA` sobre un documento  
**Quiero** descargar la versión actual del documento  
**Para** utilizar el archivo en mi sistema local

### Criterios de Aceptación

#### Scenario 1: Descarga exitosa con permiso de LECTURA
**Dado** que soy un usuario autenticado con permiso `LECTURA` sobre un documento  
**Cuando** solicito la descarga del documento mediante `GET /api/documents/{documentId}/download`  
**Entonces** recibo una respuesta `200 OK` con el binario del archivo  
**Y** los headers HTTP incluyen:
- `Content-Type`: MIME type correcto según extensión del archivo
- `Content-Disposition: attachment; filename="nombre_original.ext"`
- `Content-Length`: tamaño en bytes del archivo

#### Scenario 2: Descarga denegada sin permiso de LECTURA
**Dado** que soy un usuario autenticado sin permiso `LECTURA` sobre un documento  
**Cuando** intento descargar el documento mediante `GET /api/documents/{documentId}/download`  
**Entonces** recibo una respuesta `403 Forbidden`  
**Y** el cuerpo de la respuesta incluye:
```json
{
  "codigo": "ACCESO_DENEGADO",
  "mensaje": "No tiene permisos de lectura sobre este documento",
  "detalles": {
    "documento_id": "uuid-del-documento",
    "permiso_requerido": "LECTURA"
  }
}
```

### Especificación Técnica del Endpoint

#### Request
```http
GET /api/documents/{documentId}/download
Authorization: Bearer {jwt_token}
```

**Path Parameters:**
- `documentId` (UUID, required): Identificador único del documento

**Headers:**
- `Authorization` (string, required): Token JWT con claims `usuario_id` y `organizacion_id`

#### Response - Descarga Exitosa (200 OK)
```http
HTTP/1.1 200 OK
Content-Type: {mime-type-detectado}
Content-Disposition: attachment; filename="{nombre_archivo}.{extension}"
Content-Length: {tamaño-en-bytes}

{binary-stream}
```

#### Response - Sin Permisos (403 Forbidden)
```json
{
  "codigo": "ACCESO_DENEGADO",
  "mensaje": "No tiene permisos de lectura sobre este documento",
  "timestamp": "2026-02-05T10:30:00Z",
  "detalles": {
    "documento_id": "uuid",
    "permiso_requerido": "LECTURA"
  }
}
```

#### Response - Documento No Encontrado (404 Not Found)
```json
{
  "codigo": "DOCUMENTO_NO_ENCONTRADO",
  "mensaje": "El documento solicitado no existe o no pertenece a su organización",
  "timestamp": "2026-02-05T10:30:00Z",
  "detalles": {
    "documento_id": "uuid"
  }
}
```

#### Response - Archivo Físico No Encontrado (500 Internal Server Error)
```json
{
  "codigo": "ARCHIVO_NO_DISPONIBLE",
  "mensaje": "El archivo del documento no está disponible en el almacenamiento",
  "timestamp": "2026-02-05T10:30:00Z",
  "detalles": {
    "documento_id": "uuid",
    "version_id": "uuid"
  }
}
```

### Lógica de Negocio Detallada

1. **Validación de Token:**
   - Extraer `usuario_id` y `organizacion_id` del JWT
   - Verificar que el token no esté expirado

2. **Validación de Existencia del Documento:**
   - Consultar tabla `documento` con filtros: `documento_id` AND `organizacion_id` AND `estado != 'ELIMINADO'`
   - Si no existe → retornar `404`

3. **Validación de Permisos:**
   - Invocar `PermissionService.hasReadPermission(usuario_id, documento_id, organizacion_id)`
   - Aplicar lógica de precedencia: permiso explícito en documento > permiso heredado de carpeta
   - Si no tiene permiso → retornar `403`

4. **Obtención de la Versión Actual:**
   - Recuperar `version_actual_id` del documento
   - Consultar tabla `version_documento` para obtener:
     - `ruta_almacenamiento`
     - `extension`
     - `tamanio_bytes`
     - `nombre_archivo_original` (si está disponible, sino usar `nombre` del documento)

5. **Descarga del Archivo:**
   - Invocar `StorageService.download(ruta_almacenamiento)` que retorna un `InputStream`
   - Si el archivo no existe físicamente → retornar `500` con código `ARCHIVO_NO_DISPONIBLE`

6. **Streaming de Respuesta:**
   - Determinar MIME type usando `MimeTypeResolver.getMimeType(extension)`
   - Configurar headers HTTP
   - Transmitir el stream al cliente
   - Cerrar recursos apropiadamente

7. **Auditoría:**
   - Emitir evento: `DOCUMENTO_DESCARGADO`
   - Payload: `{ documento_id, version_id, usuario_id, timestamp, tamanio_bytes }`

### Archivos a Modificar

#### Backend - document-core

**1. Domain Layer:**
- `src/main/java/com/docflow/documentcore/domain/service/DocumentService.java`
  - Agregar método: `DownloadDocumentDto downloadDocument(UUID documentId, UUID userId, UUID orgId)`

**2. Application Layer:**
- `src/main/java/com/docflow/documentcore/application/dto/DownloadDocumentDto.java` (nuevo)
  ```java
  public record DownloadDocumentDto(
      InputStream stream,
      String filename,
      String extension,
      String mimeType,
      Long sizeBytes
  ) {}
  ```

**3. Infrastructure Layer:**
- `src/main/java/com/docflow/documentcore/infrastructure/adapter/storage/StorageService.java`
  - Agregar método: `InputStream download(String path) throws FileNotFoundException`

- `src/main/java/com/docflow/documentcore/infrastructure/adapter/storage/LocalStorageService.java`
  - Implementar método `download()`

- `src/main/java/com/docflow/documentcore/infrastructure/adapter/web/DocumentController.java`
  - Agregar endpoint:
    ```java
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
        @PathVariable UUID documentId,
        @AuthenticationPrincipal UserPrincipal principal
    )
    ```

- `src/main/java/com/docflow/documentcore/infrastructure/adapter/persistence/DocumentRepository.java`
  - Agregar método: `Optional<DocumentWithVersion> findDocumentWithCurrentVersion(UUID documentId, UUID orgId)`

**4. Shared/Utils:**
- `src/main/java/com/docflow/documentcore/infrastructure/util/MimeTypeResolver.java` (nuevo)
  - Método: `String getMimeType(String extension)`
  - Mapeo de extensiones comunes a MIME types

**5. Tests:**
- `src/test/java/com/docflow/documentcore/domain/service/DocumentServiceTest.java`
  - Tests unitarios para lógica de descarga

- `src/test/java/com/docflow/documentcore/infrastructure/adapter/web/DocumentControllerIntegrationTest.java`
  - Tests de integración del endpoint

### Requisitos No Funcionales

**Seguridad:**
- Validar que el `organizacion_id` del token coincida con el del documento (tenant isolation)
- No revelar información sobre documentos de otras organizaciones (siempre retornar 404, nunca 403)
- Sanitizar el nombre del archivo en el header `Content-Disposition` para prevenir inyección de headers

**Performance:**
- Usar streaming para archivos grandes (no cargar todo en memoria)
- Considerar buffer size apropiado (8KB - 64KB)
- El endpoint debe soportar archivos de hasta 100MB sin degradación significativa

**Observabilidad:**
- Registrar log INFO al inicio de cada descarga: `"Iniciando descarga - documento_id={}, usuario_id={}, version_id={}"`
- Registrar log ERROR si el archivo físico no existe
- Métricas: tiempo de respuesta, tamaño de archivos descargados, tasa de errores

**Compatibilidad:**
- Soportar al menos los siguientes MIME types:
  - `application/pdf` (.pdf)
  - `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (.docx)
  - `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (.xlsx)
  - `image/jpeg` (.jpg, .jpeg)
  - `image/png` (.png)
  - `application/octet-stream` (fallback para extensiones desconocidas)

### Definition of Done

- [ ] Endpoint implementado y funcionando según especificación
- [ ] Validación de permisos integrada con sistema ACL
- [ ] Streaming de archivos implementado correctamente
- [ ] MIME types configurados para extensiones comunes
- [ ] Headers HTTP correctos en todas las respuestas
- [ ] Manejo de errores completo (404, 403, 500)
- [ ] Evento de auditoría emitido en descargas exitosas
- [ ] Tests unitarios con cobertura >80%
- [ ] Tests de integración para todos los escenarios de aceptación
- [ ] Documentación actualizada (OpenAPI/Swagger)
- [ ] Code review aprobado
- [ ] Merge a rama de desarrollo
