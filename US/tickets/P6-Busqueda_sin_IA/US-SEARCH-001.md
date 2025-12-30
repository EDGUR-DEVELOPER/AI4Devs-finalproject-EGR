## P6 — Búsqueda básica sin IA

### [US-SEARCH-001] Buscar documentos (API) por texto
---
#### Base de datos
---
* **Título:** Crear índice de búsqueda en tabla de documentos
* **Objetivo:** Optimizar consultas de búsqueda por texto sobre nombre y metadatos de documentos.
* **Tipo:** Tarea
* **Descripción corta:** Crear índices de texto (full-text o LIKE optimizado según motor de BD) sobre las columnas `nombre` y campos de metadatos relevantes en la tabla `Documento`. Esto permitirá búsquedas eficientes sin escaneo completo de tabla.
* **Entregables:**
    - Migración SQL con índice de búsqueda sobre `Documento.nombre`.
    - Índice adicional sobre campos de metadatos si existen (ej. `descripcion`, `tags`).
    - Documentación de tipo de índice elegido (GIN, FULLTEXT, etc.).
---
* **Título:** Crear vista o query optimizada para búsqueda con joins necesarios
* **Objetivo:** Preparar una consulta base que incluya la información necesaria para filtrar por organización y permisos.
* **Tipo:** Tarea
* **Descripción corta:** Diseñar la query SQL/ORM que una `Documento` con `Carpeta` y permita filtrar por `organizacion_id`. Esta query será la base para el repositorio de búsqueda.
* **Entregables:**
    - Query documentada con joins a `Carpeta` para obtener `organizacion_id`.
    - Consideraciones de rendimiento documentadas.
---
#### Backend
---
* **Título:** Implementar repositorio de búsqueda de documentos
* **Objetivo:** Crear la capa de acceso a datos para ejecutar búsquedas por texto.
* **Tipo:** Tarea
* **Descripción corta:** Implementar método en el repositorio que reciba un término de búsqueda y `organizacion_id`, ejecutando la query optimizada. Debe soportar búsqueda parcial (LIKE '%término%' o equivalente full-text).
* **Entregables:**
    - Método `searchDocuments(term: string, organizacionId: string)` en repositorio.
    - Retorno de lista de documentos con campos: `id`, `nombre`, `carpeta_id`, `fecha_creacion`.
    - Manejo de término vacío (retornar lista vacía o error según definición).
---
* **Título:** Implementar servicio de búsqueda de documentos
* **Objetivo:** Encapsular la lógica de negocio de búsqueda incluyendo validaciones.
* **Tipo:** Tarea
* **Descripción corta:** Crear servicio que orqueste la búsqueda: validar término de búsqueda (longitud mínima, caracteres permitidos), invocar repositorio con `organizacion_id` del contexto de usuario, y transformar resultados al DTO de respuesta.
* **Entregables:**
    - Clase/Servicio `SearchService` con método `search(term, userContext)`.
    - Validación de término: mínimo 2 caracteres, sanitización de caracteres especiales.
    - DTO de respuesta `SearchResultDto` con estructura definida.
---
* **Título:** Definir contrato de API para endpoint de búsqueda
* **Objetivo:** Establecer el formato de request/response del endpoint de búsqueda.
* **Tipo:** Diseño
* **Descripción corta:** Documentar el contrato del endpoint incluyendo query params, estructura de respuesta y códigos de error. Debe alinearse con el estilo de API del resto del sistema.
* **Entregables:**
    - Especificación OpenAPI/Swagger del endpoint `GET /api/search`.
    - Query param: `q` (término de búsqueda), `limit` (opcional, default 50), `offset` (opcional, default 0).
    - Response: `{ results: SearchResultDto[], total: number }`.
---
* **Título:** Implementar endpoint `GET /api/search`
* **Objetivo:** Exponer la funcionalidad de búsqueda vía API REST.
* **Tipo:** Historia
* **Descripción corta:** Crear el controlador que reciba el término de búsqueda, extraiga `organizacion_id` del token JWT, invoque al servicio y retorne los resultados. Debe estar protegido por el middleware de autenticación.
* **Entregables:**
    - Ruta/controlador `GET /api/search?q={término}`.
    - Respuesta `200` con lista de documentos encontrados.
    - Respuesta `400` si el término es inválido (muy corto o vacío).
    - Respuesta `401` si no hay token válido.
---
* **Título:** Implementar paginación en resultados de búsqueda
* **Objetivo:** Permitir navegación eficiente en resultados extensos.
* **Tipo:** Tarea
* **Descripción corta:** Agregar soporte de paginación al endpoint de búsqueda. Recibir `limit` y `offset` como query params, aplicarlos en la query de repositorio y retornar metadata de paginación.
* **Entregables:**
    - Query params `limit` (max 100, default 20) y `offset` (default 0).
    - Respuesta incluye `total` (count total sin paginar).
    - Validación de valores negativos o excesivos.
---
#### QA / Testing
---
* **Título:** Pruebas unitarias del servicio de búsqueda
* **Objetivo:** Verificar la lógica de negocio de búsqueda de forma aislada.
* **Tipo:** QA
* **Descripción corta:** Crear suite de tests unitarios para `SearchService` usando mocks del repositorio. Cubrir casos: término válido, término muy corto, término con caracteres especiales, sin resultados.
* **Entregables:**
    - Tests unitarios con cobertura mínima del 80%.
    - Casos de prueba: búsqueda exitosa, validación de término, transformación de resultados.
---
* **Título:** Pruebas de integración del endpoint de búsqueda
* **Objetivo:** Verificar el funcionamiento end-to-end del endpoint.
* **Tipo:** QA
* **Descripción corta:** Ejecutar tests de integración contra el endpoint real con base de datos de prueba. Verificar que solo retorna documentos de la organización del token.
* **Entregables:**
    - Tests de integración para `GET /api/search`.
    - Escenarios: búsqueda con resultados, sin resultados, término inválido, sin autenticación.
    - Verificación de aislamiento por organización.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-SEARCH-001
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API, no interfaz.
* **Tipo:** Tarea
* **Descripción corta:** No se implementa UI en esta historia. La interfaz de búsqueda corresponde a `US-SEARCH-003`. Se puede crear colección de requests para probar la API.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para pruebas manuales del endpoint.
