## P4 — Documentos + Versionado Lineal

### [US-DOC-006] UI mínima de carga y ver historial

## 1. Especificaciones técnicas detalladas

### 1.1. Estructura de datos y modelos

#### Request: Subida de documento
**Endpoint:** `POST /api/folders/{folderId}/documents`

```typescript
// Body: multipart/form-data
{
  file: File,           // Requerido. Máximo 100 MB (validar en cliente y servidor)
  documentName?: string // Opcional. Si no se proporciona, usar nombre del archivo
}

// Headers requeridos:
{
  'Authorization': 'Bearer {token}',
  'Content-Type': 'multipart/form-data'
}
```

#### Response: Documento creado (201 Created)
```typescript
{
  id: UUID,
  folderId: UUID,
  name: string,
  extension: string,
  mimeType: string,
  size: number,          // bytes
  currentVersionId: UUID,
  createdAt: ISO8601,
  createdBy: {
    id: UUID,
    email: string,
    fullName: string
  },
  metadata: {
    checksum: string,    // SHA256 del contenido
    storageKey: string   // Ruta en S3/MinIO
  }
}
```

#### Request: Obtener metadatos
**Endpoint:** `GET /api/documents/{documentId}`

```typescript
// Headers requeridos:
{
  'Authorization': 'Bearer {token}',
  'Accept': 'application/json'
}
```

#### Response: Metadatos (200 OK)
```typescript
{
  id: UUID,
  name: string,
  extension: string,
  mimeType: string,
  size: number,
  folderId: UUID,
  currentVersionNumber: number,
  currentVersionId: UUID,
  createdAt: ISO8601,
  createdBy: {
    id: UUID,
    email: string
  },
  updatedAt: ISO8601,
  updatedBy: {
    id: UUID,
    email: string
  }
}
```

#### Request: Listar versiones
**Endpoint:** `GET /api/documents/{documentId}/versions?page=0&size=20`

```typescript
// Headers requeridos:
{
  'Authorization': 'Bearer {token}',
  'Accept': 'application/json'
}
```

#### Response: Lista de versiones (200 OK)
```typescript
{
  content: Array<{
    id: UUID,
    documentId: UUID,
    versionNumber: number,
    size: number,
    mimeType: string,
    createdAt: ISO8601,
    createdBy: {
      id: UUID,
      email: string,
      fullName: string
    },
    isCurrentVersion: boolean,
    checksum: string
  }>,
  pageNumber: number,
  pageSize: number,
  totalElements: number,
  totalPages: number,
  last: boolean
}
```

#### Request: Descargar versión
**Endpoint:** `GET /api/documents/{documentId}/versions/{versionId}/download`

```typescript
// Headers requeridos:
{
  'Authorization': 'Bearer {token}'
}

// Query params opcionales:
?inline=false  // false = attachment (descargar), true = inline (preview en navegador)
```

#### Response: Contenido binario (200 OK)
```typescript
// Headers de respuesta:
{
  'Content-Type': '{mimeType}',
  'Content-Length': '{bytes}',
  'Content-Disposition': 'attachment; filename="{documentName}"',
  'X-Document-Version': '{versionNumber}',
  'Cache-Control': 'private, no-cache, no-store',
  'X-Content-Type-Options': 'nosniff'
}
// Body: Contenido binario del archivo
```

#### Request: Subir nueva versión
**Endpoint:** `POST /api/documents/{documentId}/versions`

```typescript
// Body: multipart/form-data
{
  file: File,           // Requerido. Máximo 100 MB
  description?: string  // Opcional. Notas sobre la versión
}

// Headers requeridos:
{
  'Authorization': 'Bearer {token}',
  'Content-Type': 'multipart/form-data'
}
```

#### Response: Nueva versión creada (201 Created)
```typescript
{
  id: UUID,
  documentId: UUID,
  versionNumber: number,
  size: number,
  mimeType: string,
  createdAt: ISO8601,
  createdBy: {
    id: UUID,
    email: string
  },
  isCurrentVersion: true,
  checksum: string
}
```

---

### 7.2. Códigos de error (HTTP)

| Código | Mensaje | Significado | Acción en Frontend |
|--------|---------|-------------|-------------------|
| 400 | `INVALID_FILE_SIZE` | Archivo > 100 MB | Mostrar: "El archivo excede 100 MB" |
| 400 | `INVALID_FILE_TYPE` | Tipo MIME no permitido | Mostrar: "Tipo de archivo no permitido" |
| 400 | `DUPLICATE_FILENAME` | Ya existe documento con ese nombre | Permitir nombrar diferente o reemplazar (UX) |
| 403 | `FORBIDDEN` | Usuario sin permiso ESCRITURA en carpeta | Mostrar: "No tienes permiso para subir aquí" |
| 404 | `FOLDER_NOT_FOUND` | Carpeta no existe | Mostrar: "Carpeta no encontrada" |
| 404 | `DOCUMENT_NOT_FOUND` | Documento no existe | Mostrar: "Documento no disponible" |
| 404 | `VERSION_NOT_FOUND` | Versión no existe | Mostrar: "Versión no disponible" |
| 409 | `CONFLICT` | Versión vigente cambió (concurrencia) | Recargar historial y reintentar |
| 413 | `PAYLOAD_TOO_LARGE` | Tamaño total > límite | Mostrar: "Archivo demasiado grande" |
| 503 | `SERVICE_UNAVAILABLE` | S3/MinIO no disponible | Mostrar: "Servicio temporalmente no disponible" |

---

### 7.3. Estados de la UI del componente de upload

```typescript
type UploadState = 
  | 'idle'        // Estado inicial, esperando selección
  | 'selected'    // Archivo seleccionado, no enviado
  | 'uploading'   // Enviando (0-99% progreso)
  | 'processing'  // Servidor procesando (100% progreso)
  | 'success'     // Completado exitosamente
  | 'error'       // Error en el proceso
  | 'cancelled'   // Cancelado por usuario

// Eventos y transiciones:
idle → selected (onFileSelect)
selected → uploading (onUploadStart)
uploading → processing (onUploadComplete, progress = 100)
processing → success (onResponse 201)
processing → error (onResponse 4xx/5xx)
uploading/processing → cancelled (onCancel)
error → idle (onRetry o onClear)
```

---

### 7.4. Validaciones en cliente

#### Validaciones de archivo
```typescript
// Extensiones permitidas (whitelist)
const ALLOWED_EXTENSIONS = [
  'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
  'txt', 'csv', 'jpg', 'jpeg', 'png', 'gif', 'zip', 'rar'
];

// Validaciones antes de enviar:
1. ✓ Archivo seleccionado (tamaño > 0)
2. ✓ Extensión en whitelist
3. ✓ Tamaño <= 100 MB
4. ✓ Usuario tiene permiso ESCRITURA (desde token/API ACL)
5. ✓ Conexión de red disponible (navigator.onLine)
6. ✓ Nombre de archivo válido (sin caracteres especiales peligrosos)
```

#### Validaciones de historial
```typescript
// Antes de mostrar historial:
1. ✓ Documento existe (GET /api/documents/{id} = 200)
2. ✓ Usuario tiene permiso LECTURA en folder/documento
3. ✓ Versiones ordenadas por versionNumber DESC (más reciente primero)
4. ✓ Exactamente una versión marcada como isCurrent
```

---

### 7.5. Requisitos de seguridad

#### Autenticación y autorización
- **JWT en header:** `Authorization: Bearer {token}`  
  - Token debe incluir `sub` (userId), `roles`, y permisos en formato `{resourceType}:{action}:{resourceId}`
  - Ejemplo: `documents:read:c123e4567890` (puede leer documento c123e4567890)
- **Verificación de permiso en cliente:** Antes de mostrar botón "Subir", verificar que JWT incluya `documents:write:{folderId}`
- **Validación en servidor:** Dupla verificación de permisos siempre (no confiar solo en cliente)

#### Sanitización
- **Nombres de archivo:** Remover caracteres especiales peligrosos (`;`, `&`, `|`, `..`, etc.) en cliente antes de mostrar
- **Content-Type:** Validar MIME type header vs extensión en servidor (no solo extensión)
- **Checksum:** Validar integridad del archivo descargado en cliente (SHA256 comparar con X-Content-Checksum header)

#### Privacidad
- **Descargas:** Headers `Cache-Control: private, no-cache, no-store` para evitar cachés públicas
- **Headers anti-sniffing:** `X-Content-Type-Options: nosniff`
- **Logs:** No registrar contenido del archivo, solo metadatos (id, size, user, timestamp)

---

### 7.6. Manejo de caché

```typescript
// Cache strategy por endpoint:

// GET /api/documents/{id}
// ✓ Cacheable por 5 minutos o hasta cambio de carpeta
// ✓ Invalidar si: usuario sube nueva versión, elimina documento

// GET /api/documents/{id}/versions
// ✓ No cacheable (lista puede cambiar frecuentemente)
// ✓ Refresh automático cada 3 min si modal está abierto

// GET /api/documents/{id}/versions/{versionId}/download
// ✓ NO cacheable (binarios nunca cachear en cliente)
// ✓ Usar blob sin persistencia

// POST operations
// ✓ Invalidar caches relacionados tras éxito
// ✓ Ej: tras POST documents → refrescar caché de /api/folders/{folderId}
```

---

### 7.7. Performance y UX

| Requisito | Métrica | Notas |
|-----------|---------|-------|
| Tiempo de inicio de modal upload | < 200 ms | Componente debe ser lazy-loaded |
| Progreso de upload | Update cada 100 ms | Usar throttle para no saturar UI |
| Historial de versiones load | < 500 ms | Para < 100 versiones |
| Descarga de archivo | Progreso visible cada 200 ms | Con ETA si posible |
| Respuesta a clic de botón | < 100 ms | Feedback inmediato (button state change) |

#### Optimizaciones
- **Lazy load DocumentUpload:** Solo importar cuando usuario navega a carpeta
- **Virtual scrolling:** Para listas de > 50 versiones, usar react-window
- **Compresión:** Si archivo es zip/comprimido, mostrar advertencia (bajará velocidad)
- **Chunked upload:** Opcional para archivos > 50 MB (mejorar UX en conexiones lentas)

---

### 7.8. Estructura de archivos y convenciones

#### Directorio frontend (relative a `frontend/src`)

```
features/
  documents/
    components/
      DocumentUpload.tsx          # Componente principal de upload
      DocumentUploadInput.tsx     # Input file + botón
      UploadProgress.tsx          # Barra/spinner de progreso
      UploadError.tsx             # Mostrar errores de upload
      VersionHistory.tsx          # Componente principal de historial
      VersionItem.tsx             # Row individual de versión
      VersionDownloadButton.tsx   # Botón reutilizable descarga
      PermissionGate.tsx          # HOC para validar permisos
    services/
      documentService.ts          # uploadDocument(), getVersions()
      uploadProgressService.ts    # Manejo de eventos XHR
    hooks/
      useDocumentUpload.ts        # Hook: estado + lógica upload
      useDocumentVersions.ts      # Hook: listado de versiones
      usePermissions.ts           # Hook: verificar permisos locales
    types/
      document.types.ts           # Interfaces de documento, versión, etc.
      upload.types.ts             # UploadState, UploadEvent, etc.
    utils/
      fileValidator.ts            # Validaciones de archivo
      permissionChecker.ts        # Lógica de verificación de permisos
      errorMapper.ts              # Mapear códigos HTTP → mensajes amigos
    __tests__/
      DocumentUpload.test.tsx     # Tests unitarios del componente
      documentService.test.ts     # Tests del servicio
      fileValidator.test.ts       # Tests de validaciones
```

#### Convenciones de nombres
- Componentes: PascalCase (`DocumentUpload`, `VersionHistory`)
- Servicios: camelCase + descriptivo (`uploadDocument`, `getDocumentVersions`)
- Tipos/Interfaces: PascalCase con sufijo (`DocumentDTO`, `VersionResponse`)
- Constantes: UPPER_SNAKE_CASE (`MAX_FILE_SIZE`, `ALLOWED_EXTENSIONS`)
- Variables de estado: camelCase (`uploadState`, `isLoading`)
- Métodos de manejo: pre `on` + acciones (`onFileSelect`, `onUploadComplete`)

---

### 7.9. Criterios de Aceptación Refinados

#### CA1: Subida con éxito notifica y actualiza listado
```gherkin
Given usuario autenticado con permiso ESCRITURA en carpeta "Reportes"
  And estoy viendo el listado de carpeta "Reportes" vacía
When abro modal de "Subir Documento"
  And selecciono archivo "enero_2026.xlsx"
  And hago clic en botón "Subir"
Then veo indicador de progreso de 0-100% (actualización cada 100 ms)
  And tras completarse, veo Toast verde: "Documento subido exitosamente"
  And el listado de "Reportes" ahora muestra "enero_2026.xlsx"
  And el documento aparece con metadata correcta (nombre, tamaño, fecha creación = AHORA, usuario = YO)
```

#### CA2: Historial muestra todas versiones, marca actual, permite acciones
```gherkin
Given documento "propuesta.docx" con versiones [V1 (3-ene-2026 12:00, usuario1), V2 (5-ene-2026 14:30, usuario2), V3 (5-ene-2026 15:45, usuario1/ACTUAL)]
When abro modal "Ver Versiones" del documento
Then veo tabla con 3 filas:
  | # Versión | Fecha/Hora | Creado por | Acciones |
  | 3 | 5-ene-2026 15:45 | usuario1 | [actual] [Descargar] [Nueva] |
  | 2 | 5-ene-2026 14:30 | usuario2 | [Descargar] |
  | 1 | 3-ene-2026 12:00 | usuario1 | [Descargar] |
  And versiones ordenadas de más nueva a más antigua (DESC por versionNumber)
```

#### CA3: Permisos reflejados en UI
```gherkin
Given usuario con permiso LECTURA en carpeta (no ESCRITURA)
When navego a carpeta
Then botón "Subir Documento" está deshabilitado (gris)
  And al pasar cursor, tooltip dice "Solo lectura: no puedes subir documentos"

Given usuario con permiso ESCRITURA
Then botón "Subir Documento" está habilitado (color)
  And al hacer clic, se abre modal sin problemas
```

#### CA4: Manejo de errores informado
```gherkin
Given intento subir archivo "virus.exe" (extensión no permitida)
When selecciono el archivo y hago clic "Subir"
Then veo Toast rojo: "Tipo de archivo no permitido. Permitidos: PDF, DOC, XLS, etc."
  And botón "Subir" permanece disponible para reintentar con otro archivo

Given intento subir archivo > 100 MB
Then veo Toast rojo: "Archivo excede tamaño máximo (100 MB)"

Given servidor retorna 503 durante descarga
Then veo Toast amarillo: "Servicio temporalmente no disponible. Reintentando..."
  And se reintenta automáticamente (exponential backoff: 1s, 2s, 4s)
```

---

### 7.10. Testing y Cobertura mínima

#### Pruebas unitarias (Jest)
```typescript
// documentService.test.ts
describe('uploadDocument', () => {
  it('debería crear FormData con archivo y enviar POST', () => { ... });
  it('debería rechazar si archivo > 100 MB', () => { ... });
  it('debería incluir Authorization header', () => { ... });
});

describe('fileValidator', () => {
  it('debería permitir extensiones en whitelist', () => { ... });
  it('debería rechazar extensión .exe', () => { ... });
  it('debería validar MIME type', () => { ... });
});
```

#### Tests E2E (Cypress/Playwright)
```typescript
// upload.e2e.ts
describe('Upload document flow', () => {
  it('should upload file and show in list', () => {
    cy.login('user@example.com');
    cy.navigateToFolder('Reports');
    cy.clickUploadButton();
    cy.selectFile('file.pdf');
    cy.clickUploadConfirm();
    cy.should('see', 'Documento subido exitosamente');
    cy.should('see', 'file.pdf', { inList: true });
  });

  it('should show error for unauthorized user', () => {
    cy.login('readonly@example.com');
    cy.navigateToFolder('Reports');
    cy.should('notSee', 'Subir Documento'); // botón no visible
  });
});
```

#### Cobertura objetivo
- Componentes: > 80% statements
- Servicios: > 90% statements
- Utilidades: 100% statements
- E2E: Todos los happy paths + 3 error paths principales

---

### 7.11. Documentación de usuario

#### Ayuda en tooltip
```
"Subir" → "Selecciona archivos (PDF, DOC, XLSX, etc. hasta 100 MB)"
"Versiones" → "Ver historial de cambios y descargar versiones anteriores"
"Nueva versión" → "Subir una versión actualizada del documento"
```

#### Mensajes de validación (inline)
```
Extensión no permitida → "Solo se permiten: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX"
Archivo muy grande → "El archivo excede 100 MB. Comprime o divide en partes."
Sin conexión → "Revisa tu conexión a internet y reinenta"
```

---

### 7.12. Consideraciones de Accesibilidad (WCAG 2.1 AA)

- ✓ Input file con label asociado (`<label htmlFor="fileInput">`)
- ✓ Botones con atributos `aria-label` descriptivos
- ✓ Progreso barra: `role="progressbar"` con `aria-valuenow`, `aria-valuemin`, `aria-valuemax`
- ✓ Toasts: `role="alert"` para acceso a lectores de pantalla
- ✓ Teclado: Tab navega por botones, Enter activa, ESC cierra modal
- ✓ Contraste: Textos y botones con ratio > 4.5:1 en foreground/background
- ✓ Responsive: Funcional en pantallas >= 320px (mobile) y >= 1024px (desktop)

---

## Resumen de refinamientos [enhanced]

Esta versión mejorada incluye:

✅ **Especificación de APIs completa** con estructura de request/response, tipos de datos exactos y ejemplos  
✅ **Mapa de códigos de error (HTTP)** con acciones esperadas en frontend  
✅ **Máquina de estados** del componente upload (idle → selected → uploading → success/error)  
✅ **Validaciones de cliente detalladas** (whitelist de extensiones, límites de tamaño, permisos)  
✅ **Requisitos de seguridad** (JWT, sanitización, privacidad, headers anti-sniffing)  
✅ **Estrategia de caché** por endpoint con invalidación explícita  
✅ **Métricas de performance y UX** con tiempos máximos y optimizaciones  
✅ **Estructura de archivos y convenciones** detalladas para el equipo frontend  
✅ **Criterios de Aceptación refinados** con escenarios BDD específicos y verificables  
✅ **Plan de testing mínimo** con cobertura objetivo  
✅ **Requisitos de accesibilidad (WCAG 2.1 AA)** explícitos  

### Cambios principales respecto a [original]:
1. Agregado especificación técnica de todas las APIs (requests, responses, headers)
2. Mapa de errores con códigos HTTP y acciones esperadas en cliente
3. Estados de UI y máquina de estados del upload
4. Validaciones de cliente exhaustivas (archivo, historial)
5. Requisitos de seguridad (autenticación, autorización, sanitización)
6. Estrategia de caché con invalidación
7. Requisitos no funcionales (performance, accesibilidad)
8. Estructura de directorios frontend específica
9. Convenciones de nombres y patrones
10. Criterios de aceptación cuantitativos y verificables
11. Cobertura de testing mínima (> 80% en componentes)

### Estado recomendado: **Pending refinement validation**
Este ticket está listo para revisión por Product Owner antes de comenzar desarrollo. Confirmar con el equipo de backend que todos los endpoints y códigos de error coinciden.
