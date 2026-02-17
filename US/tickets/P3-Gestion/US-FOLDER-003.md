## P3 — Gestión de carpetas: API + UI mínima

**[US-FOLDER-003] Mover documento a otra carpeta (API)**

- **Narrativa:** Como usuario con permisos, quiero mover un documento entre carpetas, para mantener orden.
- **Criterios de Aceptación:**
        - **Scenario 1:** Dado `ESCRITURA` en carpeta origen y destino, Cuando muevo un documento, Entonces su `carpeta_id` se actualiza y la acción queda auditada.
        - **Scenario 2:** Dado falta de permiso en origen o destino, Cuando muevo un documento, Entonces recibo `403`.

## [enhanced]

**[US-FOLDER-003] Mover documento a otra carpeta (API)**

### Objetivo
Permitir mover un documento entre carpetas dentro de la misma organización, validando permisos en origen y destino, actualizando la ubicación de forma atómica y emitiendo un evento de auditoría.

### Alcance
- **Incluye:** API REST, validaciones de permisos y pertenencia organizacional, persistencia de cambio, auditoría y pruebas.
- **No incluye:** UI (se implementará en P4).

### Endpoint
- **Método y ruta:** `PATCH /api/documentos/{id}/mover`
- **Autenticación:** Requiere token válido.
- **Request body (JSON):**
    - `carpeta_destino_id` (UUID/ID requerido)

### Reglas de negocio
1. **Documento existe:** si no existe o no pertenece a la organización del usuario → `404`.
2. **Carpeta destino existe:** si no existe o no pertenece a la organización del usuario → `404`.
3. **Destino distinto del origen:** si `carpeta_destino_id` == `carpeta_id` actual → `400`.
4. **Permisos:** usuario debe tener `ESCRITURA` en **carpeta origen** y **carpeta destino**. Si falta en cualquiera → `403` indicando la carpeta que falla (sin filtrar datos sensibles).
5. **Operación atómica:** actualización de `carpeta_id` y registro de auditoría deben ocurrir en la misma transacción.

### Respuestas
- **200 OK:** documento actualizado (incluye `carpeta_id` nuevo).
- **400 Bad Request:** intento de mover a la misma carpeta o request inválido.
- **403 Forbidden:** falta permiso en origen o destino.
- **404 Not Found:** documento o carpeta destino no existe en la organización.

### Auditoría
- **Evento:** `DOCUMENTO_MOVIDO`
- **Payload mínimo:** `documento_id`, `carpeta_origen_id`, `carpeta_destino_id`, `usuario_id`, `timestamp`, `organizacion_id`.

### Datos y persistencia
- Definir estrategia de tracking de movimientos:
    - **Opción A:** columna `carpeta_anterior_id` en `Documento`.
    - **Opción B:** tabla `documento_movimientos` (historial completo).
- Entregable: análisis de opción elegida + migración SQL + documentación.
- Revisar índices en `Documento(carpeta_id)` y proponer mejoras si hay impacto en rendimiento.

### Archivos/Capas a modificar (referencial)
- **Servicio de documentos (backend/document-core):**
    - Controller: exponer `PATCH /api/documentos/{id}/mover`.
    - Service: `moverDocumento(documentoId, carpetaDestinoId, usuarioId, organizacionId)`.
    - ACL/Permisos: `validarPermisosMovimiento(usuarioId, carpetaOrigenId, carpetaDestinoId)`.
    - DTO: `MoverDocumentoRequest` con validaciones `@NotNull` y formato UUID.
    - Repositorios/Adaptadores: actualización `carpeta_id`.
    - Auditoría: integración con servicio P5.

### Pruebas requeridas
- **Unitarias:**
    - Permisos duales (origen/destino) para todos los casos (éxito y fallos).
    - Validación de `carpeta_destino_id` y reglas de negocio.
- **Integración:**
    - `PATCH /api/documentos/{id}/mover` con DB real: 200/403/404/400.
    - Verificar persistencia de `carpeta_id` y emisión del evento de auditoría.

### Consideraciones no funcionales
- **Seguridad:** no revelar existencia de carpetas de otras organizaciones (usar `404`).
- **Consistencia:** operación transaccional para evitar estados intermedios.
- **Performance:** validar índices para actualización de `carpeta_id`.

### Definition of Done
- Endpoint implementado y documentado.
- Validaciones completas (existencia, organización, permisos, no-mismo-destino).
- Auditoría emitida con payload completo.
- Migración y decisión de tracking documentadas.
- Tests unitarios e integraciones pasando.