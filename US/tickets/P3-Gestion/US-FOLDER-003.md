## P3 — Gestión de carpetas: API + UI mínima

### [US-FOLDER-003] Mover documento a otra carpeta (API)
---
#### Base de datos
---
* **Título:** Agregar columna de tracking para movimientos de documentos
* **Objetivo:** Mantener historial de ubicaciones anteriores para auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Agregar columna `carpeta_anterior_id` o crear tabla de historial de movimientos para trazabilidad. Considerar si se necesita para el MVP o es suficiente con auditoría.
* **Entregables:**
    - Análisis de opción elegida (columna vs tabla historial).
    - Migración SQL según decisión.
    - Documentación de la estrategia de tracking.
---
* **Título:** Verificar índices para actualización de carpeta_id en documentos
* **Objetivo:** Asegurar que la actualización de ubicación de documento sea eficiente.
* **Tipo:** Tarea
* **Descripción corta:** Revisar y optimizar índices en tabla `Documento` para operaciones de actualización de `carpeta_id`.
* **Entregables:**
    - Análisis de índices existentes.
    - Índices adicionales si son necesarios.
---
#### Backend
---
* **Título:** Implementar servicio de validación de permisos para movimiento
* **Objetivo:** Verificar que el usuario tiene ESCRITURA en carpeta origen Y destino.
* **Tipo:** Tarea
* **Descripción corta:** Crear método que valide permisos de ESCRITURA en ambas carpetas involucradas en el movimiento. Debe fallar si falta permiso en cualquiera de las dos.
* **Entregables:**
    - Método `validarPermisosMovimiento(usuarioId, carpetaOrigenId, carpetaDestinoId)`.
    - Respuesta clara indicando cuál carpeta falla si aplica.
---
* **Título:** Implementar servicio de movimiento de documentos
* **Objetivo:** Lógica de negocio para mover documentos entre carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Crear servicio que valide permisos en origen y destino, actualice `carpeta_id` del documento, y registre el evento de auditoría.
* **Entregables:**
    - Método `moverDocumento(documentoId, carpetaDestinoId, usuarioId, organizacionId)`.
    - Validación de existencia de documento y carpeta destino.
    - Validación de que ambos pertenecen a la misma organización.
    - Actualización atómica de `carpeta_id`.
---
* **Título:** Integrar emisión de evento de auditoría para movimiento
* **Objetivo:** Registrar la acción de mover documento para cumplir con requisitos de auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Emitir evento de auditoría con detalles del movimiento: documento_id, carpeta_origen_id, carpeta_destino_id, usuario_id, timestamp.
* **Entregables:**
    - Llamada a servicio de auditoría (P5) tras movimiento exitoso.
    - Código de evento: `DOCUMENTO_MOVIDO`.
    - Payload con información completa del movimiento.
---
* **Título:** Implementar endpoint `PATCH /api/documentos/{id}/mover`
* **Objetivo:** Exponer API REST para mover documentos entre carpetas.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint protegido que reciba carpeta destino, valide permisos en origen y destino, ejecute el movimiento y registre auditoría.
* **Entregables:**
    - Ruta/controlador `PATCH /api/documentos/{id}/mover`.
    - Request body: `{ carpeta_destino_id }`.
    - Respuesta 200 con documento actualizado.
    - Respuesta 403 si falta permiso en origen o destino.
    - Respuesta 404 si documento o carpeta no existe.
    - Respuesta 400 si se intenta mover a la misma carpeta.
---
* **Título:** Implementar DTO de request para movimiento
* **Objetivo:** Estandarizar contrato de API para operación de movimiento.
* **Tipo:** Tarea
* **Descripción corta:** Definir DTO para request de movimiento con validaciones de campos requeridos.
* **Entregables:**
    - DTO `MoverDocumentoRequest` con `carpeta_destino_id` requerido.
    - Validación de formato UUID/ID válido.
---
* **Título:** Validar que documento y carpeta destino pertenecen a la misma organización
* **Objetivo:** Prevenir movimientos entre organizaciones.
* **Tipo:** Tarea
* **Descripción corta:** Agregar validación que verifique que tanto el documento como la carpeta destino pertenecen a la organización del token del usuario.
* **Entregables:**
    - Validación en servicio de movimiento.
    - Error 404 si carpeta destino no existe en la organización (sin filtrar información).
---
* **Título:** Pruebas unitarias de validación de permisos para movimiento
* **Objetivo:** Asegurar que la lógica de permisos dual (origen + destino) funciona correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios que verifiquen: movimiento exitoso con permisos en ambas, fallo sin permiso en origen, fallo sin permiso en destino, fallo sin permiso en ambas.
* **Entregables:**
    - Suite de tests unitarios con mocks de ACL.
    - Cobertura de todas las combinaciones de permisos.
---
* **Título:** Pruebas unitarias de integración con auditoría
* **Objetivo:** Verificar que el evento de auditoría se emite correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests que verifiquen que tras un movimiento exitoso se llama al servicio de auditoría con los parámetros correctos.
* **Entregables:**
    - Tests con mock de servicio de auditoría.
    - Verificación de payload del evento.
---
* **Título:** Pruebas de integración de `PATCH /api/documentos/{id}/mover`
* **Objetivo:** Verificar endpoint completo con base de datos real.
* **Tipo:** QA
* **Descripción corta:** Tests de integración HTTP verificando respuestas 200, 403, 404, y que el documento realmente cambió de carpeta.
* **Entregables:**
    - Tests para escenarios 1 y 2 de criterios de aceptación.
    - Verificación de persistencia de nuevo `carpeta_id`.
    - Verificación de registro de auditoría.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-FOLDER-003
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** La interfaz de usuario para mover documentos se implementará como parte de la UI de gestión documental (P4). Esta historia solo cubre la API.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.
