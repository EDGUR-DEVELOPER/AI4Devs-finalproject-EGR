## P3 — Gestión de carpetas: API + UI mínima

### [US-FOLDER-002] Listar contenido de carpeta (API) con visibilidad por permisos
---
#### Base de datos
---
* **Título:** Query optimizado para listar contenido de carpeta
* **Objetivo:** Obtener eficientemente subcarpetas y documentos de una carpeta con filtrado por permisos.
* **Tipo:** Tarea
* **Descripción corta:** Crear consulta o vista que devuelva el contenido de una carpeta (subcarpetas + documentos) incluyendo solo elementos sobre los que el usuario tiene al menos LECTURA, considerando permisos directos y heredados.
* **Entregables:**
    - Query/procedimiento `listar_contenido_carpeta(carpeta_id, usuario_id, organizacion_id)`.
    - Índices necesarios para performance en tablas `Carpeta`, `Documento`, `Permiso_Carpeta`, `Permiso_Documento`.
---
* **Título:** Índices para filtrado por permisos en listados
* **Objetivo:** Optimizar consultas de listado con join a tablas de permisos.
* **Tipo:** Tarea
* **Descripción corta:** Crear índices compuestos que aceleren las consultas de listado cuando se filtran por permisos de usuario.
* **Entregables:**
    - Índice en `Permiso_Carpeta(usuario_id, carpeta_id, nivel_acceso)`.
    - Índice en `Permiso_Documento(usuario_id, documento_id, nivel_acceso)`.
    - Análisis de plan de ejecución para validar mejoras.
---
#### Backend
---
* **Título:** Implementar servicio de evaluación de permisos de lectura
* **Objetivo:** Determinar si un usuario puede ver una carpeta o documento específico.
* **Tipo:** Tarea
* **Descripción corta:** Extender el servicio de ACL para evaluar permisos de LECTURA considerando: permiso directo, permiso heredado de carpeta padre (si recursivo=true), y regla de precedencia documento > carpeta.
* **Entregables:**
    - Método `canRead(userId, resourceType, resourceId)`.
    - Soporte para evaluación en lote (múltiples recursos).
    - Caché de permisos para optimizar listados grandes.
---
* **Título:** Implementar servicio de listado de contenido de carpeta
* **Objetivo:** Lógica de negocio para obtener contenido visible de una carpeta.
* **Tipo:** Tarea
* **Descripción corta:** Crear servicio que obtenga subcarpetas y documentos de una carpeta, filtrando solo aquellos sobre los que el usuario tiene permiso de LECTURA.
* **Entregables:**
    - Método `listarContenido(carpetaId, usuarioId, organizacionId, opciones?)`.
    - Retorno con estructura: `{ subcarpetas: [], documentos: [] }`.
    - Filtrado automático por permisos.
    - Exclusión de elementos con `fecha_eliminacion`.
---
* **Título:** Implementar validación de acceso a carpeta contenedora
* **Objetivo:** Verificar que el usuario puede acceder a la carpeta antes de listar su contenido.
* **Tipo:** Tarea
* **Descripción corta:** Antes de listar contenido, validar que el usuario tiene al menos LECTURA sobre la carpeta solicitada. Si no tiene permiso, denegar con 403.
* **Entregables:**
    - Validación previa en servicio de listado.
    - Respuesta 403 con código `SIN_PERMISO_LECTURA`.
---
* **Título:** Implementar endpoint `GET /api/carpetas/{id}/contenido`
* **Objetivo:** Exponer API REST para listar contenido de una carpeta.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint protegido que valide permisos de lectura sobre la carpeta y devuelva subcarpetas y documentos visibles para el usuario.
* **Entregables:**
    - Ruta/controlador `GET /api/carpetas/{id}/contenido`.
    - Query params opcionales: `page`, `size`, `ordenar_por`.
    - Respuesta 200 con `{ subcarpetas: [...], documentos: [...], total_subcarpetas, total_documentos }`.
    - Respuesta 403 si no tiene LECTURA en la carpeta.
    - Respuesta 404 si carpeta no existe (en la organización del token).
---
* **Título:** Implementar endpoint `GET /api/carpetas/raiz` para carpeta raíz
* **Objetivo:** Permitir obtener el contenido de la carpeta raíz de la organización.
* **Tipo:** Tarea
* **Descripción corta:** Endpoint auxiliar que devuelve el contenido de la carpeta raíz de la organización del usuario autenticado.
* **Entregables:**
    - Ruta/controlador `GET /api/carpetas/raiz`.
    - Lógica para identificar carpeta raíz (`carpeta_padre_id = NULL`).
    - Misma estructura de respuesta que listado normal.
---
* **Título:** Implementar DTOs de respuesta para listado
* **Objetivo:** Estandarizar formato de respuesta para listados de carpetas y documentos.
* **Tipo:** Tarea
* **Descripción corta:** Definir DTOs para representar items en el listado, incluyendo información básica y capacidades del usuario sobre cada elemento.
* **Entregables:**
    - DTO `CarpetaItemResponse` con: `id, nombre, fecha_creacion, puede_escribir, puede_administrar`.
    - DTO `DocumentoItemResponse` con: `id, nombre, version_actual, fecha_modificacion, puede_escribir`.
    - DTO `ContenidoCarpetaResponse` con colecciones y metadatos de paginación.
---
* **Título:** Incluir capacidades de usuario en respuesta de listado
* **Objetivo:** Informar al frontend qué acciones puede realizar el usuario sobre cada elemento.
* **Tipo:** Tarea
* **Descripción corta:** Enriquecer cada item del listado con flags de capacidad basados en los permisos del usuario: `puede_leer`, `puede_escribir`, `puede_administrar`.
* **Entregables:**
    - Campos de capacidad en DTOs de respuesta.
    - Evaluación eficiente de permisos para cada item.
---
* **Título:** Pruebas unitarias de filtrado por permisos
* **Objetivo:** Asegurar que el filtrado de visibilidad funciona correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios que verifiquen que solo se retornan elementos con permiso de LECTURA, considerando permisos directos y heredados.
* **Entregables:**
    - Tests para: usuario ve solo lo permitido, herencia de permisos, precedencia documento > carpeta.
    - Mocks de repositorios y servicio ACL.
---
* **Título:** Pruebas de integración de `GET /api/carpetas/{id}/contenido`
* **Objetivo:** Verificar endpoint completo con escenarios reales.
* **Tipo:** QA
* **Descripción corta:** Tests de integración HTTP verificando respuestas 200 con contenido filtrado, 403 sin permiso, 404 carpeta inexistente.
* **Entregables:**
    - Tests para escenarios 1 y 2 de criterios de aceptación.
    - Verificación de aislamiento por organización.
    - Verificación de que no se filtran datos de otras organizaciones.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-FOLDER-002
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** La interfaz de usuario para navegar carpetas se implementará en US-FOLDER-005. Esta historia solo cubre la API.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.
