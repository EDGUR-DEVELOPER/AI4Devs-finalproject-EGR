## P5 — Auditoría: logs inmutables + UI mínima

### [US-AUDIT-002] Persistir auditoría como registro inmutable
---
#### Base de datos
---
* **Título:** Configurar restricciones de inmutabilidad en tabla de auditoría
* **Objetivo:** Garantizar que los registros de auditoría no puedan ser modificados ni eliminados.
* **Tipo:** Tarea
* **Descripción corta:** Implementar restricciones a nivel de base de datos para prevenir operaciones UPDATE y DELETE en la tabla `Evento_Auditoria`. Usar triggers o políticas de seguridad según el motor de BD.
* **Entregables:**
    - Trigger `BEFORE UPDATE` que lance excepción en `Evento_Auditoria`.
    - Trigger `BEFORE DELETE` que lance excepción en `Evento_Auditoria`.
    - Documentación técnica de las restricciones implementadas.
---
* **Título:** Crear índices para consultas de auditoría
* **Objetivo:** Optimizar el rendimiento de consultas frecuentes sobre la tabla de auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Crear índices compuestos para las consultas más comunes: filtro por organización + rango de fechas, filtro por usuario, filtro por tipo de evento. Considerar índices parciales si el volumen lo justifica.
* **Entregables:**
    - Índice `ix_auditoria_org_fecha` en `(organizacion_id, timestamp DESC)`.
    - Índice `ix_auditoria_usuario` en `(usuario_id, timestamp DESC)`.
    - Índice `ix_auditoria_codigo` en `(codigo_evento, timestamp DESC)`.
    - Análisis de plan de ejecución para validar uso de índices.
---
* **Título:** Implementar columna de hash de integridad (opcional MVP)
* **Objetivo:** Añadir una capa adicional de verificación de integridad para detección de manipulaciones.
* **Tipo:** Tarea
* **Descripción corta:** Agregar columna `hash_integridad` calculada como SHA-256 del contenido del registro. Permite verificar si un registro fue alterado fuera del sistema (acceso directo a BD).
* **Entregables:**
    - Columna `hash_integridad VARCHAR(64)` en `Evento_Auditoria`.
    - Trigger `BEFORE INSERT` que calcule y asigne el hash.
    - Función de verificación de integridad para auditorías.
---
#### Backend
---
* **Título:** Implementar repositorio de persistencia de auditoría (solo INSERT)
* **Objetivo:** Crear la capa de acceso a datos que solo permita operaciones de inserción.
* **Tipo:** Tarea
* **Descripción corta:** Implementar `AuditRepository` con método `save(event)` únicamente. No exponer métodos `update()` o `delete()`. Validar que el ORM/query builder no permita operaciones de modificación.
* **Entregables:**
    - Clase `AuditRepository` con método `save(event): Promise<AuditEvent>`.
    - Sin métodos `update`, `delete`, `remove` en la interfaz.
    - Mapeo de entidad a tabla `Evento_Auditoria`.
---
* **Título:** Integrar repositorio con servicio emisor
* **Objetivo:** Conectar el servicio de emisión de eventos con la capa de persistencia.
* **Tipo:** Tarea
* **Descripción corta:** Modificar `AuditService.emit()` para invocar `AuditRepository.save()`. Implementar manejo de errores de persistencia con retry opcional y logging de fallos.
* **Entregables:**
    - `AuditService` invocando `AuditRepository.save()`.
    - Manejo de excepciones de BD sin interrumpir flujo principal.
    - Log de errores con contexto suficiente para debugging.
---
* **Título:** Validar inmutabilidad a nivel de aplicación
* **Objetivo:** Asegurar que no existan endpoints ni servicios que modifiquen auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Revisar que no existan rutas PATCH/PUT/DELETE para auditoría. Documentar explícitamente que la auditoría es de solo lectura. Agregar comentarios en código que adviertan contra futuras modificaciones.
* **Entregables:**
    - Verificación de que no existe `PUT/PATCH/DELETE /audit/*`.
    - Documentación en README/Wiki sobre inmutabilidad.
    - Comentarios en código: "INMUTABLE: No agregar métodos de modificación".
---
* **Título:** Implementar timestamp con zona horaria UTC
* **Objetivo:** Garantizar consistencia temporal independiente de la ubicación del servidor.
* **Tipo:** Tarea
* **Descripción corta:** Asegurar que todos los timestamps de auditoría se almacenen en UTC. El servicio debe convertir a UTC antes de persistir. Las consultas deben interpretar correctamente la zona horaria.
* **Entregables:**
    - Timestamps almacenados en UTC (TIMESTAMP WITH TIME ZONE o equivalente).
    - Conversión automática en `AuditService` antes de guardar.
    - Documentación del estándar de zona horaria.
---
#### QA / Testing
---
* **Título:** Pruebas de inmutabilidad a nivel de base de datos
* **Objetivo:** Verificar que las restricciones de BD funcionan correctamente.
* **Tipo:** QA
* **Descripción corta:** Ejecutar queries directos UPDATE y DELETE contra la tabla de auditoría y verificar que son rechazados. Probar con diferentes usuarios de BD si aplica.
* **Entregables:**
    - Test: `UPDATE Evento_Auditoria SET...` → Error esperado.
    - Test: `DELETE FROM Evento_Auditoria WHERE...` → Error esperado.
    - Documentación del mensaje de error esperado.
---
* **Título:** Pruebas de persistencia correcta de eventos
* **Objetivo:** Verificar que los eventos se guardan con todos los campos correctos.
* **Tipo:** QA
* **Descripción corta:** Crear tests que emitan eventos y verifiquen que se persisten con timestamp, hash (si aplica) y todos los campos mapeados correctamente.
* **Entregables:**
    - Test: emitir evento → verificar registro en BD.
    - Test: verificar timestamp en UTC.
    - Test: verificar hash de integridad (si implementado).
    - Test: verificar que `organizacion_id` no puede ser NULL.
---
* **Título:** Pruebas de rendimiento de inserción
* **Objetivo:** Verificar que la auditoría no impacta significativamente el rendimiento.
* **Tipo:** QA
* **Descripción corta:** Medir tiempo de respuesta de operaciones críticas (crear carpeta, subir documento) con y sin auditoría. El overhead no debe superar el 10% del tiempo base.
* **Entregables:**
    - Benchmark de operación sin auditoría.
    - Benchmark de operación con auditoría.
    - Reporte comparativo de tiempos.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-AUDIT-002
* **Objetivo:** Aclarar alcance: esta historia define persistencia inmutable en backend.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La inmutabilidad es una característica del backend que garantiza integridad de datos.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
