## P3 — Gestión de carpetas: API + UI mínima

### [US-FOLDER-004] Eliminar carpeta vacía (soft delete) (API)

#### Resumen
Como administrador, quiero eliminar una carpeta vacía mediante eliminación lógica, para mantener higiene sin perder trazabilidad ni historial.

#### Alcance
- Solo API (sin UI en esta historia).
- Eliminación lógica de carpetas vacías dentro de una organización.
- No se elimina físicamente ningún registro.

#### Reglas de negocio
1. **Permisos:** solo usuarios con permiso de **ADMINISTRACIÓN** sobre la carpeta pueden eliminarla.
2. **Carpeta raíz:** no se puede eliminar una carpeta raíz (`carpeta_padre_id IS NULL`).
3. **Carpeta vacía:** solo se puede eliminar si **no** tiene subcarpetas activas ni documentos activos.
4. **Soft delete:** se marca `fecha_eliminacion` con fecha/hora actual y la carpeta deja de aparecer en listados activos.

#### Modelo y base de datos
- Tabla: `carpeta`.
- Columna requerida: `fecha_eliminacion` (timestamp nullable).
- Consultas existentes deben filtrar activos con `fecha_eliminacion IS NULL`.

**Consulta recomendada para verificación eficiente:**
- Usar `EXISTS` para subcarpetas activas y documentos activos.
- Ejemplo lógico:
    - `EXISTS (SELECT 1 FROM carpeta c WHERE c.carpeta_padre_id = :carpetaId AND c.fecha_eliminacion IS NULL)`
    - `EXISTS (SELECT 1 FROM documento d WHERE d.carpeta_id = :carpetaId AND d.fecha_eliminacion IS NULL)`

**Índices sugeridos:**
- `carpeta(carpeta_padre_id, fecha_eliminacion)`
- `documento(carpeta_id, fecha_eliminacion)`

#### Endpoints
**DELETE /api/carpetas/{id}**
- **Auth:** requerido (token válido) y permiso ADMINISTRACIÓN.
- **Path params:**
    - `id` (UUID de carpeta).
- **Respuestas:**
    - `204 No Content` cuando se elimina correctamente.
    - `403 Forbidden` si no tiene permiso ADMINISTRACIÓN.
    - `404 Not Found` si la carpeta no existe o no pertenece a la organización.
    - `409 Conflict` si la carpeta tiene contenido.
    - `400 Bad Request` si se intenta eliminar carpeta raíz.

**Error estandarizado para carpeta no vacía:**
- Código: `CARPETA_NO_VACIA`.
- Mensaje: "La carpeta debe vaciarse antes de eliminarla".
- (Opcional) incluir:
    - `subcarpetasActivas` (int).
    - `documentosActivos` (int).

#### Servicios y componentes esperados
- **Servicio de carpetas**
    - `boolean estaVacia(UUID carpetaId)`.
    - `void eliminarCarpeta(UUID carpetaId, UUID usuarioId, UUID organizacionId)`.
- **Repositorio**
    - Método de existencia de contenido basado en `EXISTS`/`COUNT`.
- **Controlador**
    - Manejo de errores y mapeo a códigos HTTP.

#### Archivos a modificar (referenciales)
- Servicio/Controller de carpetas en `backend/document-core/src/main/java/.../carpeta/`.
- Repositorios en `backend/document-core/src/main/java/.../carpeta/` y `.../documento/`.
- Manejo de errores/códigos en `backend/document-core/src/main/java/.../error/`.
- Tests en `backend/document-core/src/test/java/...`.

#### Pruebas
**Unitarias (servicio):**
- `should_Delete_When_EmptyAndAdmin`.
- `should_Fail_When_NotAdmin`.
- `should_Fail_When_NotEmpty`.
- `should_Fail_When_RootFolder`.
- `should_Fail_When_NotFound`.

**Unitarias (verificación de vacía):**
- carpeta vacía -> true.
- con subcarpetas -> false.
- con documentos -> false.
- con ambos -> false.

**Integración (endpoint):**
- `DELETE /api/carpetas/{id}` con carpeta vacía -> 204 y `fecha_eliminacion` seteada.
- carpeta con contenido -> 409 y `CARPETA_NO_VACIA`.
- sin permisos -> 403.
- raíz -> 400.
- inexistente -> 404.

#### Documentación
- Actualizar documentación de API (OpenAPI) si aplica.
- Registrar el nuevo error `CARPETA_NO_VACIA` en el catálogo de errores del servicio.

#### Requisitos no funcionales
- **Seguridad:** validar pertenencia a organización y permisos; no filtrar información sensible.
- **Performance:** usar consultas `EXISTS`/índices para evitar cargas completas.
- **Trazabilidad:** no borrar físicamente; conservar historial mediante `fecha_eliminacion`.
