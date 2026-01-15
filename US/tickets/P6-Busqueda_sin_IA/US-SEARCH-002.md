## P6 — Búsqueda básica sin IA

> Estado: **MVP (prioridad: básico)** — La búsqueda por permisos se mantiene en MVP con queries síncronas y joins. Optimización avanzada que dependa de pipelines o colas queda Post-MVP.

### [US-SEARCH-002] La búsqueda respeta permisos y no filtra existencia
---
#### Base de datos
---
* **Título:** Optimizar query de búsqueda con join a permisos
* **Objetivo:** Crear una consulta eficiente que combine búsqueda de texto con validación de ACL.
* **Tipo:** Tarea
* **Descripción corta:** Diseñar y documentar la query SQL/ORM que filtre documentos no solo por término de búsqueda y `organizacion_id`, sino también por permisos del usuario. Debe considerar permisos directos en documento y heredados de carpeta.
* **Entregables:**
    - Query SQL documentada con joins a `Permiso_Documento` y `Permiso_Carpeta`.
    - Lógica de precedencia: permiso de documento > permiso de carpeta.
    - Consideración de flag `recursivo` en permisos de carpeta.
    - Análisis de rendimiento con EXPLAIN/ANALYZE.
---
* **Título:** Crear índices para optimizar filtrado por permisos
* **Objetivo:** Mejorar rendimiento de queries de búsqueda con filtro de ACL.
* **Tipo:** Tarea
* **Descripción corta:** Agregar índices compuestos en tablas de permisos que aceleren los joins necesarios para filtrar por usuario y nivel de acceso.
* **Entregables:**
    - Índice en `Permiso_Documento(documento_id, usuario_id, nivel_acceso)`.
    - Índice en `Permiso_Carpeta(carpeta_id, usuario_id, nivel_acceso, recursivo)`.
    - Migración SQL con los nuevos índices.
---
#### Backend
---
* **Título:** Implementar filtro de permisos en repositorio de búsqueda
* **Objetivo:** Modificar el repositorio para que solo retorne documentos donde el usuario tiene permiso de LECTURA.
* **Tipo:** Tarea
* **Descripción corta:** Extender la query del repositorio de búsqueda para incluir validación de ACL. El usuario debe tener al menos `LECTURA` (directa en documento o heredada de carpeta) para que el documento aparezca en resultados.
* **Entregables:**
    - Método actualizado `searchDocuments(term, organizacionId, usuarioId)`.
    - Lógica de evaluación de permisos integrada en la query (no post-filtrado).
    - Soporte para permisos heredados con `recursivo=true`.
---
* **Título:** Integrar validación de permisos en servicio de búsqueda
* **Objetivo:** Asegurar que el servicio pase el `usuario_id` al repositorio para filtrado.
* **Tipo:** Tarea
* **Descripción corta:** Modificar `SearchService` para extraer `usuario_id` del contexto de usuario y pasarlo al repositorio. Garantizar que no hay forma de bypasear esta validación.
* **Entregables:**
    - Servicio actualizado con paso de `usuario_id` obligatorio.
    - Validación de que `usuario_id` nunca es nulo en el flujo de búsqueda.
---
* **Título:** Garantizar no filtración de existencia de documentos
* **Objetivo:** Asegurar que usuarios sin permiso no puedan inferir la existencia de documentos.
* **Tipo:** Tarea
* **Descripción corta:** Revisar que la respuesta de búsqueda no incluya ningún hint sobre documentos sin permiso (ni conteo total, ni mensajes de "X resultados ocultos"). La lista simplemente no incluye lo que no tiene permiso.
* **Entregables:**
    - Revisión de código del endpoint y servicio.
    - Confirmación de que `total` solo cuenta documentos con permiso.
    - Documentación de la política de no-filtración.
---
* **Título:** Manejo de caso sin permisos en ningún resultado
* **Objetivo:** Definir comportamiento cuando el usuario busca pero no tiene permisos sobre ningún resultado.
* **Tipo:** Tarea
* **Descripción corta:** Cuando la búsqueda no retorna resultados (ya sea porque no hay coincidencias o porque no tiene permisos), retornar lista vacía con `total: 0`. No diferenciar entre "no existe" y "no tienes permiso".
* **Entregables:**
    - Respuesta consistente: `{ results: [], total: 0 }`.
    - Sin mensajes diferenciadores que revelen existencia.
---
#### QA / Testing
---
* **Título:** Pruebas unitarias de filtrado por permisos
* **Objetivo:** Verificar que la lógica de ACL se aplica correctamente en búsqueda.
* **Tipo:** QA
* **Descripción corta:** Crear tests unitarios que verifiquen diferentes escenarios de permisos: usuario con LECTURA directa, usuario con LECTURA heredada, usuario sin ningún permiso, usuario con ESCRITURA pero sin LECTURA.
* **Entregables:**
    - Tests unitarios para cada escenario de ACL.
    - Casos: permiso directo en documento, permiso heredado de carpeta padre, permiso heredado recursivo, sin permisos.
    - Verificación de que ESCRITURA no implica LECTURA automáticamente (según reglas del sistema).
---
* **Título:** Pruebas de integración de seguridad de búsqueda
* **Objetivo:** Verificar end-to-end que usuarios no autorizados no ven documentos.
* **Tipo:** QA
* **Descripción corta:** Ejecutar tests de integración con múltiples usuarios y configuraciones de permisos. Crear documentos, asignar permisos selectivos, y verificar que cada usuario solo ve lo autorizado.
* **Entregables:**
    - Setup de datos: 2+ usuarios, múltiples documentos con permisos variados.
    - Test: Usuario A ve documento X pero no Y; Usuario B ve Y pero no X.
    - Test: Mismo término retorna diferentes resultados según usuario.
    - Verificación de que `total` es consistente con resultados visibles.
---
* **Título:** Pruebas de no-filtración de información
* **Objetivo:** Asegurar que la API no revela existencia de documentos sin permiso.
* **Tipo:** QA
* **Descripción corta:** Tests específicos de seguridad que verifican que no hay side-channels de información. Comparar respuestas de búsqueda para usuario con/sin permisos y confirmar que son indistinguibles.
* **Entregables:**
    - Test: Búsqueda de término exacto de documento sin permiso retorna lista vacía.
    - Test: Tiempos de respuesta similares con/sin resultados (no timing attacks).
    - Test: No hay diferencia en estructura de respuesta que revele existencia.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-SEARCH-002
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de seguridad en API.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. El filtrado de permisos es transparente para el frontend que simplemente muestra los resultados que recibe de la API.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - Documentación de que el frontend confía en el filtrado del backend.
