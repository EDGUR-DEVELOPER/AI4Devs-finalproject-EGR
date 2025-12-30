## P3 — Gestión de carpetas: API + UI mínima

### [US-FOLDER-001] Crear carpeta (API) en el organizacion actual
---
#### Base de datos
---
* **Título:** Crear modelo de entidad Carpeta
* **Objetivo:** Persistir la estructura jerárquica de carpetas por organización.
* **Tipo:** Tarea
* **Descripción corta:** Implementar la tabla `Carpeta` con campos esenciales para la jerarquía y aislamiento por organización. Debe soportar relación padre-hijo y soft delete.
* **Entregables:**
    - Migración SQL con tabla `Carpeta(id, organizacion_id, carpeta_padre_id, nombre, descripcion, fecha_creacion, fecha_actualizacion, fecha_eliminacion, creado_por)`.
    - Índice para búsqueda por `organizacion_id` y `carpeta_padre_id`.
    - Foreign key a `Organizacion` y auto-referencia a `Carpeta`.
---
* **Título:** Crear restricción de unicidad de nombre por nivel
* **Objetivo:** Evitar carpetas con el mismo nombre dentro del mismo directorio padre.
* **Tipo:** Tarea
* **Descripción corta:** Agregar índice único compuesto que garantice que no existan dos carpetas activas con el mismo nombre bajo el mismo padre en la misma organización.
* **Entregables:**
    - Índice único parcial `ux_carpeta_nombre_padre_org` sobre `(organizacion_id, carpeta_padre_id, nombre)` donde `fecha_eliminacion IS NULL`.
    - Documentación de la regla de negocio.
---
* **Título:** Crear carpeta raíz por organización (seed/migración)
* **Objetivo:** Garantizar que cada organización tenga una carpeta raíz inicial para anclar la jerarquía.
* **Tipo:** Tarea
* **Descripción corta:** Crear script o trigger que inserte automáticamente una carpeta raíz (`carpeta_padre_id = NULL`) cuando se crea una organización, o migración para organizaciones existentes.
* **Entregables:**
    - Script de seed para crear carpeta raíz en organizaciones existentes.
    - Documentación del comportamiento esperado para nuevas organizaciones.
---
* **Título:** Datos semilla para pruebas de jerarquía de carpetas
* **Objetivo:** Facilitar QA con estructura de carpetas de ejemplo.
* **Tipo:** Tarea
* **Descripción corta:** Crear datos de prueba con estructura de carpetas multinivel: raíz, subcarpetas nivel 1, subcarpetas nivel 2, carpetas vacías y carpetas con contenido.
* **Entregables:**
    - Script de seed con al menos 3 niveles de profundidad.
    - Documentación de la estructura de prueba.
---
#### Backend
---
* **Título:** Implementar repositorio de Carpetas
* **Objetivo:** Proveer acceso a datos de carpetas con filtrado por organización.
* **Tipo:** Tarea
* **Descripción corta:** Crear repositorio/DAO con métodos para CRUD de carpetas. Todas las consultas deben filtrar automáticamente por `organizacion_id` del contexto y excluir registros con `fecha_eliminacion`.
* **Entregables:**
    - Clase/Servicio `CarpetaRepository` con métodos: `findById`, `findByParentId`, `create`, `softDelete`.
    - Filtro automático por `organizacion_id` en todas las consultas.
---
* **Título:** Implementar servicio de validación de permisos para carpeta
* **Objetivo:** Verificar que el usuario tiene permiso de ESCRITURA o ADMINISTRACIÓN en la carpeta padre.
* **Tipo:** Tarea
* **Descripción corta:** Crear servicio que evalúe permisos del usuario sobre una carpeta específica. Debe integrarse con el sistema ACL (P2) para verificar niveles de acceso incluyendo herencia.
* **Entregables:**
    - Método `hasPermission(userId, carpetaId, nivelRequerido)`.
    - Integración con servicio de ACL existente.
    - Soporte para permisos heredados si `recursivo=true`.
---
* **Título:** Implementar servicio de creación de carpetas
* **Objetivo:** Lógica de negocio para crear carpetas con validaciones.
* **Tipo:** Tarea
* **Descripción corta:** Implementar servicio que valide permisos, verifique unicidad de nombre, y persista la nueva carpeta asociada a la organización del token.
* **Entregables:**
    - Método `crearCarpeta(carpetaPadreId, nombre, descripcion, usuarioId, organizacionId)`.
    - Validación de existencia de carpeta padre.
    - Validación de permisos (ESCRITURA o ADMINISTRACIÓN).
    - Asignación automática de `organizacion_id` desde contexto.
---
* **Título:** Implementar endpoint `POST /api/carpetas`
* **Objetivo:** Exponer API REST para creación de carpetas.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint protegido que reciba datos de la nueva carpeta, valide permisos sobre carpeta padre, y devuelva la carpeta creada o error apropiado.
* **Entregables:**
    - Ruta/controlador `POST /api/carpetas`.
    - Request body: `{ carpeta_padre_id, nombre, descripcion? }`.
    - Respuesta 201 con `{ id, nombre, carpeta_padre_id, fecha_creacion }`.
    - Respuesta 403 si no tiene permisos en carpeta padre.
    - Respuesta 400/409 si nombre duplicado.
    - Respuesta 404 si carpeta padre no existe.
---
* **Título:** Implementar DTO de request/response para carpetas
* **Objetivo:** Estandarizar contratos de API para carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Definir DTOs para request de creación y response de carpeta. Incluir validaciones de campos requeridos y formatos.
* **Entregables:**
    - DTO `CrearCarpetaRequest` con validaciones (`nombre` requerido, longitud máxima).
    - DTO `CarpetaResponse` con campos de respuesta.
    - Mappers entre entidad y DTOs.
---
* **Título:** Normalizar errores para operaciones de carpetas
* **Objetivo:** Consistencia en respuestas de error para el módulo de carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Definir códigos de error específicos para operaciones de carpetas y mapearlos a códigos HTTP apropiados.
* **Entregables:**
    - Códigos: `CARPETA_NO_ENCONTRADA` (404), `SIN_PERMISO_CARPETA` (403), `NOMBRE_DUPLICADO` (409).
    - Handler de excepciones para el módulo.
---
* **Título:** Pruebas unitarias de servicio de creación de carpetas
* **Objetivo:** Asegurar lógica de negocio correcta y prevenir regresiones.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios del servicio de creación cubriendo: creación exitosa, sin permisos, nombre duplicado, carpeta padre inexistente.
* **Entregables:**
    - Suite de tests unitarios con mocks de repositorio y ACL.
    - Cobertura de escenarios positivos y negativos.
---
* **Título:** Pruebas de integración de `POST /api/carpetas`
* **Objetivo:** Verificar endpoint completo con base de datos real.
* **Tipo:** QA
* **Descripción corta:** Tests de integración que ejecuten requests HTTP contra el endpoint, verificando respuestas 201, 403, 404, 409.
* **Entregables:**
    - Tests de integración para escenarios 1 y 2 de criterios de aceptación.
    - Verificación de aislamiento por organización.
    - Verificación de persistencia correcta.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-FOLDER-001
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** La interfaz de usuario para crear carpetas se implementará en US-FOLDER-005. Esta historia solo cubre la API.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.
