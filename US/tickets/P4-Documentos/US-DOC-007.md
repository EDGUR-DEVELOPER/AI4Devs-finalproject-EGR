# [US-DOC-007] Descarga de documento actual desde lista de documentos

## [original]

**Narrativa:** Como administrador o usuario con permiso de lectura, quiero descargar la versión actual de un documento directamente desde la lista de documentos en la UI, para poder acceder al contenido del archivo sin navegar al historial de versiones.

**Criterios de Aceptación:**

### Escenario 1: Descarga exitosa con permiso de lectura
Dado que soy un usuario autenticado con permiso de LECTURA sobre un documento
Cuando hago clic en el botón "Descargar" en la lista de documentos
Entonces el sistema descarga la versión actual del documento con el nombre original y extensión correcta
Y se muestra una notificación de éxito "Descarga iniciada"
Y se emite un evento de auditoría DOCUMENTO_DESCARGADO

### Escenario 2: Descarga con permiso de escritura o administración
Dado que soy un usuario con permiso de ESCRITURA o ADMINISTRACION sobre un documento
Cuando hago clic en el botón "Descargar"
Entonces el sistema descarga el documento exitosamente
Y se comporta igual que en el escenario 1

### Escenario 3: Botón deshabilitado sin permiso de lectura
Dado que soy un usuario sin permiso de LECTURA sobre un documento
Cuando visualizo la lista de documentos
Entonces el botón "Descargar" aparece deshabilitado o no visible para ese documento
Y si intento acceder directamente a la URL de descarga, recibo un error 403

### Escenario 4: Manejo de error en descarga
Dado que tengo permiso para descargar un documento
Cuando hago clic en descargar y ocurre un error (red, archivo no disponible, etc.)
Entonces se muestra una notificación de error detallando el problema
Y el botón vuelve a su estado normal permitiendo reintentar

### Escenario 5: Indicador visual durante descarga
Dado que inicio una descarga de documento
Cuando la descarga está en progreso
Entonces el botón muestra un spinner o indicador de carga
Y el botón queda deshabilitado hasta completar la operación

---

## [enhanced]

### Descripción técnica completa

Esta historia de usuario implementa la capacidad de descargar el documento actual (versión activa) desde la interfaz de lista de documentos. El flujo involucra validación de permisos ACL, obtención del contenido binario desde MinIO/S3, generación de eventos de auditoría y manejo robusto de errores.

### Estructura de datos y campos

**Modelos implicados:**

1. **Documento**
   - `documento_id` (UUID, PK)
   - `nombre_original` (VARCHAR(255), required) - nombre del archivo con extensión
   - `carpeta_id` (UUID, FK)
   - `organizacion_id` (UUID, FK)
   - `version_actual_id` (UUID, FK a Documento_Version)
   - `fecha_creacion` (TIMESTAMP)
   - `fecha_eliminacion` (TIMESTAMP, nullable - soft delete)

2. **Documento_Version**
   - `version_id` (UUID, PK)
   - `documento_id` (UUID, FK)
   - `numero_secuencial` (INT)
   - `ruta_almacenamiento` (VARCHAR(255)) - ruta S3/MinIO
   - `nombre_archivo_almacenado` (VARCHAR(255))
   - `tipo_mime` (VARCHAR(100))
   - `tamaño_bytes` (BIGINT)
   - `hash_contenido` (VARCHAR(64), SHA256)
   - `fecha_carga` (TIMESTAMP)

3. **Permiso_Carpeta** / **Permiso_Documento**
   - Para validar acceso (referenciado en US-ACL-002)

4. **Auditoria**
   - `auditoria_id` (UUID)
   - `codigo_evento` (VARCHAR(50)) - ej. "DOCUMENTO_DESCARGADO"
   - `usuario_id` (UUID)
   - `organizacion_id` (UUID)
   - `entidad_tipo` (VARCHAR(50)) - "DOCUMENTO"
   - `entidad_id` (UUID)
   - `detalles` (JSON, nullable)
   - `timestamp` (TIMESTAMP)

### Endpoints API

#### 1. Endpoint para descargar documento

**URL:** `GET /api/v1/organizaciones/{organizacion_id}/documentos/{documento_id}/descargar`

**Headers requeridos:**
```
Authorization: Bearer {JWT_TOKEN}
Accept: */* (el cliente acepta cualquier tipo de contenido)
```

**Parámetros de ruta:**
- `organizacion_id` (UUID) - debe coincidir con el token
- `documento_id` (UUID)

**Parámetros de query (opcionales):**
- `nombre_alternativo` (string, default: usar `nombre_original`) - permite descargar con otro nombre

**Respuesta exitosa (200 OK):**
```
Content-Type: application/octet-stream (o el tipo MIME del documento)
Content-Disposition: attachment; filename="nombre_original.ext"
Content-Length: {tamaño en bytes}
ETag: {hash del contenido para validación de caché}

[body: contenido binario del archivo]
```

**Respuestas de error:**

| Código | Escenario |
|--------|-----------|
| 401 | Token ausente, expirado o inválido |
| 403 | Usuario sin permiso LECTURA sobre el documento |
| 404 | Documento no existe o pertenece a otra organización |
| 410 | Documento marcado como eliminado (soft delete) |
| 500 | Error del servidor (BD, almacenamiento) |

**Validaciones en el endpoint:**
1. Validar JWT y extraer `usuario_id` y `organizacion_id`
2. Verificar que `organizacion_id` del token coincida con el parámetro
3. Validar que el documento existe y pertenece a la organización
4. Verificar que el documento no está eliminado (fecha_eliminacion IS NULL)
5. Obtener versión actual del documento
6. Validar permiso de usuario: `LECTURA | ESCRITURA | ADMINISTRACION` sobre documento o carpeta padre
7. Obtener contenido binario desde MinIO/S3
8. Generar evento de auditoría "DOCUMENTO_DESCARGADO"
9. Enviar archivo con headers apropiados

#### 2. Endpoint auxiliar: Validar permisos de descarga (GET, opcional para frontend)

**URL:** `GET /api/v1/organizaciones/{organizacion_id}/documentos/{documento_id}/permisos/puede-descargar`

**Respuesta:**
```json
{
  "puede_descargar": true,
  "nivel_permiso": "LECTURA",
  "nombre_archivo": "contrato_2026.pdf",
  "tamaño_bytes": 1048576
}
```

### Archivos a modificar - Backend

#### 1. **`backend/document-core/src/main/java/com/domain/documento/domain/model/Documento.java`**
   - Verificar campos `nombre_original`, `version_actual_id`, `fecha_eliminacion`
   - Agregar getter para nombre con extensión si no existe

#### 2. **`backend/document-core/src/main/java/com/domain/documento/application/service/DocumentoDescargaService.java`** (crear)
   - Orquestar la descarga
   - Validar permisos (inyectar servicio ACL)
   - Obtener versión actual
   - Recuperar contenido de almacenamiento
   - Generar evento de auditoría
   - Manejo de excepciones

#### 3. **`backend/document-core/src/main/java/com/domain/documento/infrastructure/adapters/http/controller/DocumentoDescargaController.java`** (crear)
   - Endpoint `GET /documentos/{id}/descargar`
   - Parsear JWT y extraer contexto
   - Invocar servicio de descarga
   - Construir respuesta HTTP con headers
   - Manejo de excepciones → códigos HTTP

#### 4. **`backend/document-core/src/main/java/com/domain/documento/infrastructure/adapters/storage/S3StorageAdapter.java`** (modificar si existe)
   - Agregar método `obtenerContenidoDocumento(ruta, organizacion_id)` → InputStream
   - Validación de organización en ruta
   - Manejo de errores (archivo no encontrado, acceso denegado)

#### 5. **`backend/document-core/src/main/java/com/domain/auditoria/application/service/AuditoriaService.java`**
   - Método `registrarDescargaDocumento(usuario_id, documento_id, organizacion_id, ip_cliente)` 
   - Crear evento con código "DOCUMENTO_DESCARGADO"

#### 6. **`backend/document-core/src/main/java/com/domain/acl/application/service/AclService.java`** (existente)
   - Asegurar que valida permiso `LECTURA` sobre documento y carpeta padre
   - Retorna nivel de permiso del usuario

#### 7. **`backend/gateway/src/main/java/.../gateway/config/HttpSecurityConfig.java`** (si aplica)
   - Configurar CORS para descarga de binarios (si es cross-origin)

### Archivos a modificar - Frontend

#### 1. **`frontend/src/features/documentos/services/documentoApiService.ts`** (crear o expandir)
   ```typescript
   export async function descargarDocumento(
     organizacionId: string,
     documentoId: string,
     nombreAlternativo?: string
   ): Promise<void>
   ```
   - Construir URL: `/api/v1/organizaciones/{org}/documentos/{doc}/descargar`
   - Realizar GET con `Authorization` header
   - Manejo de blob y creación de descarga
   - Retornar Promise para que UI pueda gestionar estado

#### 2. **`frontend/src/features/documentos/components/DocumentList.tsx`** (o similar)
   - Agregar botón "Descargar" con ícono ⬇️
   - Botón visible solo si usuario tiene permisos (validar desde contexto ACL o state)
   - Manejo de clicks: `handleDescargar(documentoId)`
   - Deshabilitar botón mientras se descarga (`isLoading`) y mostrar spinner

#### 3. **`frontend/src/features/documentos/hooks/useDocumentoDescarga.ts`** (crear)
   ```typescript
   export function useDocumentoDescarga() {
     const [isLoading, setIsLoading] = useState(false);
     const [error, setError] = useState<string | null>(null);
     
     const descargar = async (docId: string, nombre: string) => {
       // Lógica de descarga y notificaciones
     };
     
     return { isLoading, error, descargar };
   }
   ```

#### 4. **`frontend/src/common/components/NotificationCentre.tsx`** (existente)
   - Mostrar notificación de éxito: "Descarga iniciada"
   - Mostrar notificación de error con detalles

#### 5. **`frontend/src/features/documentos/types/index.ts`**
   - Tipo `DescargarDocumentoRequest`
   - Tipo `DescargarDocumentoResponse` (si es necesario para permisos previos)

#### 6. **Contexto ACL disponible** (referenciado)
   - En el list component, usar contexto/state de permisos para determinar si el botón está habilitado
   - Llamar a hook `usePermiso(documentoId, 'LECTURA')` para validar

### Pasos de implementación

#### Fase 1: Backend - API de descarga
1. **Servicio de descarga** (`DocumentoDescargaService.java`)
   - Método `descargarDocumento(usuarioId, documentoId, organizacionId)` → bytes + metadata
   - Invocar `AclService.validarPermiso(usuarioId, documentoId, LECTURA)`
   - Obtener versión actual del documento
   - Llama a almacenamiento con ruta segura
   - Registra auditoría

2. **Controlador** (`DocumentoDescargaController.java`)
   - Endpoint GET `/documentos/{id}/descargar`
   - Extrae JWT, invoca servicio, retorna ResponseEntity con headers
   - Traduce excepciones a códigos HTTP

3. **Tests unitarios** (TDD)
   - `DocumentoDescargaServiceTests.java`: validar permisos, obtener contenido, auditoría
   - `DocumentoDescargaControllerTests.java`: validar respuestas HTTP, headers

#### Fase 2: Frontend - UI de descarga
1. **Hook `useDocumentoDescarga`**
   - Estados: `isLoading`, `error`
   - Función: `descargar(documentoId, nombreDocumento)`
   - Manejo de errores y notificaciones

2. **Componente DocumentList (actualizar)**
   - Agregar columna o botón "Descargar"
   - Deshabilitar si no hay permiso
   - Spinner durante descarga
   - Notificación de éxito/error

3. **Tests (Vitest + @testing-library/react)**
   - Mock del servicio API
   - Validar que el botón está habilitado/deshabilitado según permisos
   - Validar que la notificación se muestra

#### Fase 3: Integración y validación
1. Pruebas e2e (si aplica)
2. Validar con diferentes niveles de permiso
3. Validar errores de red/almacenamiento
4. Documentación API (OpenAPI/Swagger)

### Requisitos no funcionales

#### Seguridad
- **ACL enforcement**: Validar permiso en backend (nunca confiar en cliente)
- **Validación de organización**: Ingresa del token, no del cliente
- **Prevención de path traversal**: Usar UUID y rutas validadas en S3/MinIO
- **Auditoría**: Registrar cada descarga para detectar acceso anómalo
- **Timeout**: Limitar tiempo de descarga (ej. 5 minutos)

#### Performance
- **Streaming**: Enviar archivo en chunks (no cargar todo en memoria)
- **Cache HTTP**: Usar ETag para permitir caché del cliente
- **Rate limiting**: Considerar límite de descargas por usuario/hora si aplica
- **Compresión**: Opcional, según tipo de archivo (documentos ya comprimidos típicamente)

#### Disponibilidad
- **Reintentos**: Si MinIO está temporalmente no disponible (circuit breaker)
- **Timeout de almacenamiento**: Configurar timeout de conexión a S3/MinIO
- **Logging**: Registrar errores de almacenamiento para troubleshooting

### Pruebas unitarias

#### Backend (JUnit + Mockito)

**`DocumentoDescargaServiceTests.java`**
```java
class DocumentoDescargaServiceTests {
  @Test
  void debería_descargar_documento_con_permiso_lectura() { }
  
  @Test
  void debería_lanzar_403_sin_permiso() { }
  
  @Test
  void debería_retornar_404_si_documento_no_existe() { }
  
  @Test
  void debería_registrar_auditoría_en_descarga_exitosa() { }
  
  @Test
  void debería_lanzar_excepción_si_documento_está_eliminado() { }
  
  @Test
  void debería_validar_que_documento_pertenece_a_la_organización() { }
}
```

**`DocumentoDescargaControllerTests.java`**
```java
class DocumentoDescargaControllerTests {
  @Test
  void debería_retornar_200_con_headers_correctos() { }
  
  @Test
  void debería_retornar_401_sin_token() { }
  
  @Test
  void debería_retornar_403_sin_permiso() { }
  
  @Test
  void debería_retornar_404_si_no_existe() { }
}
```

#### Frontend (Vitest + React Testing Library)

**`useDocumentoDescarga.test.ts`**
```typescript
describe('useDocumentoDescarga', () => {
  test('debería descargar documento cuando usuario tiene permiso', async () => { });
  
  test('debería mostrar notificación de éxito', async () => { });
  
  test('debería mostrar notificación de error', async () => { });
  
  test('debería habilitar el refresh del botón después de error', async () => { });
});
```

**`DocumentList.test.tsx`**
```typescript
describe('DocumentList - Descarga', () => {
  test('debería mostrar botón descargar si usuario tiene LECTURA', () => { });
  
  test('debería deshabilitar botón si usuario NO tiene LECTURA', () => { });
  
  test('debería mostrar spinner durante descarga', async () => { });
  
  test('debería iniciar descarga al hacer clic', async () => { });
});
```

### Documentación

#### 1. OpenAPI (Swagger)
- Documentar endpoint en `backend/document-core/src/main/resources/swagger-docs.yml` o vía anotaciones `@Operation`
- Incluir ejemplos de request/response y códigos de error

#### 2. README
- Agregar en `backend/document-core/README.md` sección de "Descarga de Documentos"
- Explicar flujo de autorización y almacenamiento

#### 3. Guía interna
- Documentar en wikis internas o Confluence (si aplica)
- Incluir ejemplos de curl para testing manual

### Criterios de Aceptación técnicos

1. ✅ Endpoint `GET /documentos/{id}/descargar` implementado y documentado
2. ✅ Validación de permisos ACL en backend
3. ✅ Auditoría registrada con código "DOCUMENTO_DESCARGADO"
4. ✅ Errores retornan códigos HTTP correctos (401, 403, 404, 500)
5. ✅ UI muestra botón habilitado/deshabilitado según permisos
6. ✅ Descarga inicia con spinner y se completa sin interrupciones
7. ✅ Notificaciones de éxito y error se muestran
8. ✅ Tests unitarios ≥ 80% cobertura (backend + frontend)
9. ✅ Documentación API actualizada
10. ✅ Validado con diferentes niveles de permiso (LECTURA, ESCRITURA, ADMINISTRACION, NINGUNO)

