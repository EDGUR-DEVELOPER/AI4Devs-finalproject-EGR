## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-007] Enforzar permisos de lectura en endpoints de consulta/descarga

---

### Narrativa de Usuario

**Como** administrador de sistema / usuario con rol auditor  
**Quiero** que el sistema bloquee automáticamente el acceso de lectura a usuarios sin permiso `LECTURA`  
**Para que** la información sensible esté protegida contra accesos no autorizados y cumpla con políticas de seguridad y confidencialidad

---

### Descripción Funcional

Este ticket implementa el **enforcement (aplicación forzada) de permisos de lectura** en todos los endpoints de consulta y descarga del sistema. Un usuario sin permiso `LECTURA` sobre una carpeta o documento recibirá una respuesta HTTP 403 Forbidden, independientemente del rol global.

**Alcance de endpoints protegidos:**
- `GET /api/carpetas/{id}` — Consulta de detalle de carpeta
- `GET /api/carpetas/{id}/contenido` — Listado de contenido (subcarpetas y documentos)
- `GET /api/documentos/{id}` — Consulta de metadatos de documento
- `GET /api/documentos/{id}/descargar` — Descarga del archivo binario
- `GET /api/documentos/{id}/versiones` — Historial de versiones

**Referencia:** Implementación usa el evaluador de permisos de [US-ACL-006](../US-ACL-006.md), que ya maneja herencia y precedencia.

---

### Criterios de Aceptación

| # | Scenario | Given | When | Then |
|---|----------|-------|------|------|
| 1 | Usuario sin LECTURA no accede a carpeta | Usuario "externo@test.com" sin permiso LECTURA en carpeta 123, autenticado | Solicita `GET /api/carpetas/123` | Recibe HTTP 403 con body: `{"error": "forbidden", "message": "Sin permiso de lectura para este recurso"}` |
| 2 | Usuario sin LECTURA no lista contenido | Usuario "externo@test.com" sin permiso en carpeta 123 | Solicita `GET /api/carpetas/123/contenido` | Recibe HTTP 403 (no se filtra contenido, se niega toda operación) |
| 3 | Usuario sin LECTURA no descarga documento | Usuario "externo@test.com" sin permiso LECTURA en documento 456 | Solicita `GET /api/documentos/456/descargar` | Recibe HTTP 403 sin revelar existencia del archivo |
| 4 | Usuario sin LECTURA no ve versiones | Usuario "externo@test.com" sin LECTURA en documento 456 | Solicita `GET /api/documentos/456/versiones` | Recibe HTTP 403 |
| 5 | Usuario CON LECTURA puede acceder | Usuario "lector@test.com" con permiso LECTURA en carpeta 123 | Solicita `GET /api/carpetas/123/contenido` | Recibe HTTP 200 con lista de elementos |
| 6 | Usuario CON LECTURA descarga documento | Usuario "lector@test.com" con LECTURA en documento 456 | Solicita `GET /api/documentos/456/descargar` | Recibe HTTP 200 con el archivo y headers CORS correctos |
| 7 | Admin con ADMINISTRACION puede acceder | Usuario "admin@test.com" con ADMINISTRACION en carpeta 123 | Solicita cualquier endpoint de lectura | Recibe HTTP 200 (ADMINISTRACION incluye LECTURA) |
| 8 | Documento hereda LECTURA de carpeta | Usuario "lector@test.com" con LECTURA en carpeta padre, sin ACL explícito en documento | Solicita `GET /api/documentos/456` | Recibe HTTP 200 (herencia activa) |
| 9 | Usuario desautenticado recibe 401 | Solicitud sin token Bearer válido | Solicita `GET /api/carpetas/123` | Recibe HTTP 401 (no 403) |
| 10 | Usuario de otra organización recibe 404 | Usuario "externo@org-b.com" intenta acceder recurso de org-a | Solicita `GET /api/carpetas/123` (de org-a) | Recibe HTTP 404 (no se revela existencia) |

---

### Requerimientos No-Funcionales

#### Seguridad
- ✅ No se deben filtrar detalles de la carpeta/documento en respuesta 403
- ✅ La verificación de permiso ocurre **antes** de cualquier lectura de BD
- ✅ Se debe loguear WARN cada intento denegado: `[SECURITY] Access denied for user={userId} resource={resourceType}:{resourceId} permission={required}`

#### Performance
- ✅ La evaluación de permisos debe usar caché en memoria del evaluador (no hacer query a BD en cada request)
- ✅ Máximo 50ms de latencia adicional por evaluación de permiso
- ✅ No generar N+1 queries al listar contenido

#### Mantenibilidad
- ✅ Usar decorator `@RequiereLectura(tipoRecurso)` reutilizable en todos los endpoints
- ✅ Centralizar respuesta 403 en una excepción `AccessDeniedException`
- ✅ Documentar el comportamiento en Swagger/OpenAPI

---

### Estructura Técnica Propuesta

#### Backend - Componentes a Crear/Modificar

**1. Excepción Personalizada**
```java
// en domain/exceptions/
public class AccessDeniedException extends RuntimeException {
  private String resourceType;
  private String resourceId;
  private String requiredPermission;
  // getters, constructor
}
```

**2. Guard/Decorator**
```java
// en infrastructure/security/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiereLectura {
  String value(); // "carpeta" o "documento"
  String idParameterName() default "id"; // nombre del parámetro en ruta
}

@Component
public class RequiereLecturaAspect {
  // Lógica de AOP que:
  // 1. Extrae ID del parámetro de ruta
  // 2. Obtiene user del SecurityContext
  // 3. Llama a evaluadorPermisosService.tienePermiso(...)
  // 4. Lanza AccessDeniedException si no tiene permiso
}
```

**3. ExceptionHandler Global**
```java
// en infrastructure/rest/
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(403).body(new ErrorResponse("forbidden", ...));
  }
}
```

**4. Endpoints a Proteger**
- `CarpetaController.obtenerDetalle(@GetMapping("/{id}"), @RequiereLectura("carpeta"))`
- `CarpetaController.listarContenido(@GetMapping("/{id}/contenido"), @RequiereLectura("carpeta"))`
- `DocumentoController.obtenerMetadatos(@GetMapping("/{id}"), @RequiereLectura("documento"))`
- `DocumentoController.descargar(@GetMapping("/{id}/descargar"), @RequiereLectura("documento"))`
- `DocumentoController.listarVersiones(@GetMapping("/{id}/versiones"), @RequiereLectura("documento"))`

---

#### Frontend - Componentes a Crear/Modificar

**1. Interceptor HTTP**
```typescript
// en core/shared/http/
export function setupErrorInterceptor(http: HttpClient) {
  // Detectar 403 y mostrar toast: "No tiene permiso para acceder a este recurso"
  // NO redirigir a login (diferente de 401)
  // Diferenciación: 401 → sesión expirada, 403 → falta de permisos
}
```

**2. Componente ErrorPage**
```tsx
// en common/ui/
<ErrorPage 
  title="Acceso Denegado"
  message="No tiene permiso para acceder a este recurso"
  action="Volver al inicio"
/>
```

**3. Condicionales en Componentes**
```tsx
// en features/*/components/
{user.hasPermission('LECTURA', carpetaId) && (
  <button onClick={() => openFolder(carpetaId)}>Abrir</button>
)}
```

**4. Route Guards**
```typescript
// en core/shared/guards/
canActivate(route: ActivatedRouteSnapshot): Observable<boolean> {
  // Si intenta navegar a /carpetas/123 sin LECTURA, 
  // redirigir a componente SinAcceso o página de error
}
```

---

### Dependencias

- ✅ [US-ACL-006](../US-ACL-006.md) — Evaluador de Permisos (debe estar implementado)
- ✅ [US-ADMIN-001](../P1-Administracion/US-ADMIN-001.md) — Endpoints base de carpetas
- ✅ [US-DOC-001](../P4-Documentos/US-DOC-001.md) — Endpoints base de documentos
- ✅ Token JWT con user ID y org ID (sistema de autenticación)

---

### Archivos Base a Revisar / Modificar

**Backend:**
- `backend/document-core/src/main/java/com/docflow/application/service/` — Servicios de carpeta/documento
- `backend/document-core/src/main/java/com/docflow/infrastructure/rest/controller/` — Controladores
- `backend/document-core/src/main/java/com/docflow/domain/exception/` — Excepciones
- `backend/document-core/src/main/java/com/docflow/infrastructure/security/` — Guards y aspectos

**Frontend:**
- `frontend/src/core/shared/http/` — Interceptores
- `frontend/src/common/ui/` — Componentes de error
- `frontend/src/features/*/hooks/` — Hooks de permiso si existen

---


---

## [enhanced] Checklist de Implementación y Validación

### ✅ Backend - Implementación

- [ ] **Crear excepciones personalizadas**
  - [ ] `AccessDeniedException` en `domain/exceptions/`
  - [ ] `ResourceNotFoundException` (para ocultar existencia cuando no hay acceso)

- [ ] **Implementar Guard/Decorator**
  - [ ] Anotación `@RequiereLectura` con parámetro `value`
  - [ ] Aspecto `RequiereLecturaAspect` usando AOP de Spring
  - [ ] Extracción automática de ID de ruta usando expresiones regulares
  - [ ] Integración con `SecurityContext` para obtener usuario actual

- [ ] **Integración con EvaluadorPermisosService**
  - [ ] Llamar a método `tienePermiso(usuarioId, recursoId, tipoRecurso, permiso)`
  - [ ] Respetar herencia y precedencia ya implementada
  - [ ] Usar caché si está disponible

- [ ] **ExceptionHandler Global**
  - [ ] Manejar `AccessDeniedException` → HTTP 403
  - [ ] Respuesta JSON estandarizada: `{"error": "forbidden", "message": "..."}`
  - [ ] NO incluir detalles del recurso en respuesta

- [ ] **Proteger Endpoints**
  - [ ] `GET /api/carpetas/{id}` — `@RequiereLectura("carpeta")`
  - [ ] `GET /api/carpetas/{id}/contenido` — `@RequiereLectura("carpeta")`
  - [ ] `GET /api/documentos/{id}` — `@RequiereLectura("documento")`
  - [ ] `GET /api/documentos/{id}/descargar` — `@RequiereLectura("documento")`
  - [ ] `GET /api/documentos/{id}/versiones` — `@RequiereLectura("documento")`

- [ ] **Logging de Seguridad**
  - [ ] Logging WARN para accesos denegados con usuario, recurso y permiso requerido
  - [ ] No loguear datos sensibles (archivos, contenido)

- [ ] **Documentación OpenAPI/Swagger**
  - [ ] Documentar respuesta 403 en cada endpoint
  - [ ] Especificar que se requiere permiso LECTURA

### ✅ Backend - Testing

- [ ] **Tests Unitarios del Guard**
  - [ ] [ ] Test: con permiso LECTURA → permite acceso
  - [ ] [ ] Test: sin permiso LECTURA → lanza `AccessDeniedException`
  - [ ] [ ] Test: usuario desautenticado → lanza `AuthenticationException`
  - [ ] [ ] Test: herencia de permisos funciona correctamente
  - [ ] [ ] Test: extracción correcta de ID de ruta

- [ ] **Tests Unitarios de Controladores**
  - [ ] [ ] Test cada endpoint protegido con usuario autorizado
  - [ ] [ ] Test cada endpoint protegido con usuario sin permisos
  - [ ] [ ] Verificar status codes correctos (200, 403)

- [ ] **Tests de Integración (E2E)**
  - [ ] [ ] Usuario con LECTURA puede listar carpeta
  - [ ] [ ] Usuario sin LECTURA recibe 403
  - [ ] [ ] Usuario con ADMINISTRACION puede acceder
  - [ ] [ ] Documento hereda LECTURA de carpeta
  - [ ] [ ] Usuario de otra organización recibe 404
  - [ ] [ ] Usuario desautenticado recibe 401 (no 403)

- [ ] **Tests de Seguridad**
  - [ ] [ ] Token alterado → 401/403 según corresponda
  - [ ] [ ] Sin token → 401
  - [ ] [ ] Usuario desactivado → 403
  - [ ] [ ] Intentos repetidos (rate limiting si aplica)

### ✅ Frontend - Implementación

- [ ] **Interceptor de Errores HTTP**
  - [ ] Detectar status 403
  - [ ] Mostrar toast/notificación: "No tiene permiso para acceder a este recurso"
  - [ ] NO redirigir a login (diferente de 401)
  - [ ] Diferenciación visual: 401 (sesión) vs 403 (permisos)

- [ ] **Componente de Error "Sin Acceso"**
  - [ ] Crear componente reutilizable `AccessDeniedPage` o modal
  - [ ] Título: "Acceso Denegado"
  - [ ] Mensaje: "No tiene permiso para acceder a este recurso"
  - [ ] Botón "Volver al inicio" o "Volver a la lista"

- [ ] **Condicionales en Componentes**
  - [ ] En componentes de carpeta: ocultar botón "Abrir" si no tiene LECTURA
  - [ ] En componentes de documento: ocultar botón "Descargar" si no tiene LECTURA
  - [ ] Mostrar disabled state si está verificando permisos

- [ ] **Router Guards**
  - [ ] Crear `canActivate` guard para rutas de lectura
  - [ ] Si usuario sin permisos intenta navegar a `/carpetas/:id` → mostrar `AccessDeniedPage`
  - [ ] Considerar pre-fetching de permisos para mejor UX

- [ ] **Manejo de Estados**
  - [ ] Estado de "cargando permisos" mientras valida
  - [ ] State management (Zustand/Redux) si aplica

### ✅ Frontend - Testing

- [ ] **Tests de Componentes**
  - [ ] [ ] Interceptor detecta 403 y muestra notificación
  - [ ] [ ] AccessDeniedPage se renderiza correctamente
  - [ ] [ ] Botones deshabilitados cuando no hay permisos

- [ ] **Tests de Router Guards**
  - [ ] [ ] Guard bloquea navegación sin LECTURA
  - [ ] [ ] Guard permite navegación con LECTURA
  - [ ] [ ] Redirecciona a AccessDeniedPage cuando corresponde

### ✅ Documentación

- [ ] **README/Wiki**
  - [ ] Documentar comportamiento de enforcement de permisos
  - [ ] Explicar diferencia entre 401 (autenticación) y 403 (autorización)
  - [ ] Ejemplos de flujos de error

- [ ] **Especificación Técnica**
  - [ ] Documentar estructura del Guard/Decorator
  - [ ] Documentar flujo de evaluación de permisos

- [ ] **API Documentation**
  - [ ] Swagger/OpenAPI refleja respuestas 403
  - [ ] Documentar campos de error

### ✅ Criterios de Finalización

- [ ] Todos los tests pasan (unitarios, integración, seguridad)
- [ ] Cobertura de código ≥ 85% en componentes críticos
- [ ] No hay vulnerabilidades de seguridad según análisis estático
- [ ] Se validan todos los scenarios de la tabla de Criterios de Aceptación
- [ ] Frontend se probó en navegadores principales (Chrome, Firefox, Safari)
- [ ] Documentación está actualizada y accesible
- [ ] PR aprobado por code review (backend y frontend)
- [ ] Logging de seguridad funciona correctamente en ambiente de staging
