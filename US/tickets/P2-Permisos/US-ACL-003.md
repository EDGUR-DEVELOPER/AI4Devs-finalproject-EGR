## P2 — Permisos Granulares (ACL por carpeta/documento)

### [US-ACL-003] Revocar permiso de carpeta (eliminar ACL)


---

## [enhanced] Especificación Técnica Detallada

### 1. Modelo de Datos

#### Tabla: `ACL_Carpeta` (Reutilizada de US-ACL-002)

```sql
CREATE TABLE ACL_Carpeta (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  carpeta_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  organizacion_id BIGINT NOT NULL,
  nivel_acceso VARCHAR(50) NOT NULL, -- LECTURA, ESCRITURA, ADMINISTRACION
  recursivo BOOLEAN DEFAULT FALSE,
  fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  fecha_eliminacion TIMESTAMP NULL,  -- Para soft delete (si se implementa luego)
  
  UNIQUE KEY uk_acl_carpeta_usuario_org (carpeta_id, usuario_id, organizacion_id),
  FOREIGN KEY (carpeta_id) REFERENCES Carpeta(id),
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id),
  FOREIGN KEY (organizacion_id) REFERENCES Organizacion(id)
);
```

**Para esta US:** operación de **hard delete** (eliminación de la fila).

---

### 2. Contratos API

#### Endpoint: `DELETE /carpetas/{carpetaId}/permisos/{usuarioId}`

**Descripción:** Revoca el acceso de un usuario a una carpeta eliminando su entrada ACL.

**Autenticación:** Bearer Token (JWT con claims: `usuario_id`, `organizacion_id`, `roles`)

**Autorización:**
- Requiere rol `ADMIN` en el organizacion del token **O**
- Requiere permiso `ADMINISTRACION` sobre la carpeta

**Parámetros de ruta:**
- `carpetaId` (Long): ID de la carpeta
- `usuarioId` (Long): ID del usuario al que se revoca acceso

**Headers:**
```http
Authorization: Bearer <token_jwt>
```

**Request Body:** Vacío

**Respuestas:**

| Status | Cuerpo | Descripción |
|--------|--------|-------------|
| **204 No Content** | (vacío) | Permiso revocado exitosamente. No devuelve cuerpo. |
| **400 Bad Request** | `{"error": "...", "message": "..."}` | `carpetaId` o `usuarioId` inválidos. |
| **401 Unauthorized** | `{"error": "UNAUTHORIZED", "message": "Token ausente o inválido"}` | Token JWT ausente o expirado. |
| **403 Forbidden** | `{"error": "FORBIDDEN", "message": "No tienes permiso ADMINISTRACION sobre esta carpeta"}` | Usuario no tiene rol ADMIN ni permiso ADMINISTRACION. |
| **404 Not Found** | `{"error": "NOT_FOUND", "message": "Carpeta no existe" / "Usuario no existe" / "ACL no existe"}` | Carpeta, usuario, o entrada ACL no existen. |
| **409 Conflict** | `{"error": "FORBIDDEN", "message": "Carpeta o usuario pertenecen a otro organizacion"}` | Validación de aislamiento de organizacion. |

**Ejemplo de respuesta 204:**
```http
HTTP/1.1 204 No Content
```

**Ejemplo de respuesta 404:**
```json
{
  "error": "NOT_FOUND",
  "message": "ACL no encontrado para usuario 5 en carpeta 12",
  "timestamp": "2026-01-30T10:15:30Z",
  "path": "/carpetas/12/permisos/5"
}
```

---

### 3. Arquitectura y Archivos a Modificar

#### Backend (Java Spring Boot)

**Estructura Hexagonal esperada:**
```
backend/document-core/
├── src/main/java/com/docflow/
│   ├── domain/
│   │   ├── model/
│   │   │   └── AclCarpeta.java          [entidad de dominio]
│   │   ├── repository/
│   │   │   └── AclCarpetaRepository.java [contrato del repositorio]
│   │   └── service/
│   │       └── AclCarpetaService.java     [lógica de negocio]
│   │
│   ├── application/
│   │   ├── dto/
│   │   │   ├── RevocacionAclRequest.java
│   │   │   └── AclCarpetaDTO.java
│   │   └── service/
│   │       └── AclCarpetaApplicationService.java [orquestación]
│   │
│   ├── infrastructure/
│   │   ├── adapters/
│   │   │   ├── persistence/
│   │   │   │   └── JpaAclCarpetaRepository.java
│   │   │   └── api/
│   │   │       └── AclCarpetaController.java
│   │   └── config/
│   │       └── SecurityConfig.java
│   │
│   └── common/
│       ├── exception/
│       │   └── AclNotFoundException.java
│       └── validation/
│           └── AclValidator.java
│
├── src/test/java/com/docflow/
│   ├── domain/
│   │   └── service/
│          └── AclCarpetaServiceTest.java
```

**Archivos concretos a crear/modificar:**

1. **`JpaAclCarpetaRepository.java`** (infraestructura)
   - Extender `JpaRepository<AclCarpeta, Long>`
   - Agregar método: `int deleteByUsuarioIdAndCarpetaIdAndOrganizacionId(...)`
   - Agregar método: `Optional<AclCarpeta> findByUsuarioIdAndCarpetaIdAndOrganizacionId(...)`

2. **`AclCarpetaService.java`** (dominio)
   - Agregar método: `void revocarPermiso(Long carpetaId, Long usuarioId, Long organizacionId)`
   - Validar existencia previa
   - Validar aislamiento de organizacion
   - Lanzar excepciones específicas

3. **`AclCarpetaController.java`** (infraestructura)
   - Agregar endpoint: `DELETE /carpetas/{carpetaId}/permisos/{usuarioId}`
   - Mapear autenticación desde JWT
   - Validar autorización
   - Responder con 204 si éxito, 404/403 si error

4. **`AclCarpetaServiceTest.java`** (unitarias)
   - `testRevocarPermisoExistente()`
   - `testRevocarPermisoInexistente()`
   - `testRevocarPermisoOrganizacionDiferente()`

---

#### Frontend (React + TypeScript)

**Estructura esperada:**
```
frontend/src/features/acl/
├── services/
│   └── aclService.ts                    [llamadas HTTP]
├── hooks/
│   └── useAclCarpeta.ts                 [hook reutilizable]
├── components/
│   ├── ListaPermisosCarpeta.tsx          [listado de permisos]
│   ├── DialogoConfirmacionRevocacion.tsx [confirmación]
│   └── ModaloAdministracionPermisos.tsx  [modal integrado]
└── types/
    └── acl.types.ts                     [tipos TypeScript]
```

**Archivos concretos:**

1. **`aclService.ts`**
   - Método: `revocarPermisoCarpeta(carpetaId: number, usuarioId: number): Promise<void>`
   - Manejo de errores HTTP (404, 403, etc.)
   - Actualización de estado tras éxito

2. **`ListaPermisosCarpeta.tsx`**
   - Props: `carpetaId: number`, `permisosActuales: AclDTO[]`, `onRevocarExito: () => void`
   - Mostrar tabla/lista con: usuario, nivel acceso, botón "Revocar"
   - Confirmar antes de ejecutar

3. **`DialogoConfirmacionRevocacion.tsx`**
   - Modal de confirmación con texto: "¿Deseas revocar el acceso a [usuario] en [carpeta]?"
   - Botones: "Cancelar", "Revocar"

4. **`useAclCarpeta.ts`**
   - Hook personalizado para manejar estado y llamadas
   - Método: `revocar(usuarioId)` con manejo de error y loading

---

### 4. Criterios de Aceptación Ampliados

#### AC-001: Revocación exitosa
**Dado:** Un administrador autenticado con rol ADMIN en organizacion "A"  
**Y:** Un usuario "juan@test.com" con permiso "LECTURA" sobre carpeta "Documentos"  
**Cuando:** Se ejecuta `DELETE /carpetas/12/permisos/5` con token válido  
**Entonces:**
- Respuesta HTTP: `204 No Content`
- La entrada ACL se elimina de la BD
- En los 100ms siguientes, una consulta de permisos por usuario no retorna esa ACL

#### AC-002: Acceso denegado post-revocación
**Dado:** Un usuario "juan@test.com" al que se le revocó permiso  
**Cuando:** El usuario intenta `GET /carpetas/12` (listar carpeta)  
**Entonces:**
- Respuesta: `403 Forbidden`
- Mensaje: "No tienes permiso LECTURA sobre esta carpeta"
- NO se filtran datos de la carpeta

#### AC-003: Intento de revocación de ACL inexistente
**Dado:** Un usuario sin permiso sobre una carpeta  
**Cuando:** Se ejecuta `DELETE /carpetas/12/permisos/5`  
**Entonces:**
- Respuesta: `404 Not Found`
- Mensaje: "ACL no encontrado"
- No se modifica ningún dato

#### AC-004: Validación de aislamiento de organizacion
**Dado:** Un token del organizacion "A" y una carpeta del organizacion "B"  
**Cuando:** Se intenta `DELETE /carpetas/b_folder_id/permisos/5`  
**Entonces:**
- Respuesta: `409 Conflict` (o `404`)
- El error NO revela la existencia de la carpeta en otro organizacion

#### AC-005: Autorización por ADMINISTRACION
**Dado:** Un usuario con permiso `ADMINISTRACION` en la carpeta (pero no rol ADMIN global)  
**Cuando:** Se ejecuta `DELETE /carpetas/12/permisos/5`  
**Entonces:**
- Respuesta: `204 No Content`
- El permiso se revoca exitosamente

#### AC-006: UI - Confirmación antes de revocar
**Dado:** Un administrador en la UI del modal de administración de permisos  
**Cuando:** Hace clic en botón "Revocar" de una entrada  
**Entonces:**
- Aparece diálogo de confirmación
- Solo al confirmar se ejecuta la revocación
- Si hay error, se muestra mensaje claro

#### AC-007: Auditoría de revocación
**Dado:** Una revocación exitosa  
**Cuando:** Se completa la operación  
**Entonces:**
- Se registra evento en tabla de auditoría
- Evento contiene: `codigo_evento: "ACL_REVOKED"`, `usuario_id`, `carpeta_id`, `timestamp`

---

### 5. Requisitos No-Funcionales

#### Seguridad
- El `organizacion_id` se obtiene **únicamente del token JWT**, nunca del cliente
- Validar que usuario y carpeta pertenecen al mismo organizacion del token
- Logs de auditoría para toda revocación (incluye fallos)
- No devolver información que revele existencia de recursos en otros organizacions

#### Rendimiento
- Operación de DELETE debe completarse en < 100ms
- Índices en `ACL_Carpeta`: 
  - `(carpeta_id, usuario_id, organizacion_id)` para búsqueda rápida
  - `(usuario_id, organizacion_id)` para listar permisos de un usuario

#### Confiabilidad
- Si la revocación falla, la UI lo comunica claramente al usuario
- Retry automático (máx 3 intentos) en fallos de conexión
- Transacción BD atómica: eliminar ACL + registrar auditoría

#### Disponibilidad
- Endpoint disponible 24/7 (excluyendo mantenimiento programado)
- Timeout configurado: 10 segundos para la operación

---

### 6. Plan de Pruebas Detallado

#### Pruebas Unitarias (TDD)

**`AclCarpetaServiceTest.java`:**
```java
@Test
void shouldRevocarPermisoExistente() {
  // Given
  AclCarpeta acl = crearAcl(usuarioId=5, carpetaId=12, nivelAcceso="LECTURA");
  repositoryMock.save(acl);
  
  // When
  service.revocarPermiso(12, 5, 1);
  
  // Then
  assertFalse(repositoryMock.findByUsuarioIdAndCarpetaIdAndOrganizacionId(5, 12, 1).isPresent());
}

@Test
void shouldLanzarExcepcionSiAclNoExiste() {
  // When & Then
  assertThrows(AclNotFoundException.class, 
    () -> service.revocarPermiso(12, 5, 1));
}

@Test
void shouldValidarAislamintoOrganizacion() {
  // Given
  AclCarpeta acl = crearAcl(organizacionId=2);
  
  // When & Then
  assertThrows(OrganizacionIsolationException.class,
    () -> service.revocarPermiso(12, 5, 1)); // org_token=1, org_acl=2
}
```

#### Pruebas de Integración

**`AclCarpetaControllerTest.java`:**
```java
@Test
void testDeleteAclSuccess() {
  // Given
  String token = generarTokenAdmin(organizacionId=1);
  
  // When
  mockMvc.perform(
    delete("/carpetas/12/permisos/5")
      .header("Authorization", "Bearer " + token)
  )
  
  // Then
  .andExpect(status().isNoContent());
}

@Test
void testDeleteAclNotFound() {
  // When & Then
  mockMvc.perform(
    delete("/carpetas/12/permisos/999") // usuario inexistente
      .header("Authorization", "Bearer " + tokenAdmin)
  )
  .andExpect(status().isNotFound())
  .andExpect(jsonPath("$.error").value("NOT_FOUND"));
}
```

---

### 7. Checklist de Entrega

**Backend:**
- [ ] Método `deleteByUsuarioIdAndCarpetaIdAndOrganizacionId()` en repositorio
- [ ] Método `revocarPermiso()` en servicio con validaciones
- [ ] Endpoint `DELETE /carpetas/{carpetaId}/permisos/{usuarioId}`
- [ ] Excepciones específicas: `AclNotFoundException`, `UnauthorizedException`
- [ ] Validación de aislamiento de organizacion
- [ ] Pruebas unitarias con cobertura > 80%
- [ ] Pruebas de integración (positivos y negativos)
- [ ] Documentación OpenAPI actualizada
- [ ] Evento de auditoría registrado

**Frontend:**
- [ ] Método `revocarPermisoCarpeta()` en servicio
- [ ] Componente `ListaPermisosCarpeta` con lista y botones
- [ ] Diálogo de confirmación antes de ejecutar
- [ ] Manejo de estados: loading, success, error
- [ ] Actualización de lista post-revocación
- [ ] Tests de componentes (snapshot + comportamiento)
- [ ] Validación de roles antes de mostrar botón "Revocar"

**Documentación:**
- [ ] README del servicio actualizado con nuevos endpoints
- [ ] Definición en `api-spec.yml` (OpenAPI)
- [ ] Guía de uso en documentación de ACL

---

### 8. Ejemplo de Flujo Completo

```
Usuario Admin -> UI modal de permisos
               -> Ve lista con usuarios y botón "Revocar"
               -> Hace clic en "Revocar" para usuario "juan@test.com"
               -> Aparece diálogo: "¿Revoca acceso a Documentos para juan@test.com?"
               -> Confirma
               -> Frontend envía: DELETE /carpetas/12/permisos/5
               -> Backend:
                  1. Valida token (usuario 1 del org A)
                  2. Busca ACL (user=5, carpeta=12, org=A)
                  3. Si no existe → 404
                  4. Si existe:
                     a. Verifica autorización (ADMIN o ADMINISTRACION)
                     b. Elimina ACL (hard delete)
                     c. Registra auditoría
                     d. Retorna 204
               -> UI: Actualiza lista, muestra "Permiso revocado"
               
Usuario "juan@test.com" intenta acceder a Documentos:
               -> GET /carpetas/12
               -> Backend: Verifica ACL → NO existe
               -> Retorna 403 Forbidden
```

---

### 9. Consideraciones de Migración de Datos

**No aplica para esta US** (solo eliminación de registros existentes en US-ACL-002).

Si en el futuro se cambia de hard delete a soft delete:
- Agregar columna `fecha_eliminacion` (ya existe en tabla)
- Actualizar lógica de búsqueda para ignorar registros con `fecha_eliminacion IS NOT NULL`
- Cambiar DELETE por UPDATE en método del servicio
