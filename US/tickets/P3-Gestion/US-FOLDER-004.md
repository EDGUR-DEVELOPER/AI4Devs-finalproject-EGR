## P3 — Gestión de carpetas: API + UI mínima

### [US-FOLDER-004] Eliminar carpeta vacía (soft delete) (API)
---
#### Base de datos
---
* **Título:** Verificar soporte de soft delete en modelo Carpeta
* **Objetivo:** Asegurar que el modelo soporta eliminación lógica.
* **Tipo:** Tarea
* **Descripción corta:** Verificar que la tabla `Carpeta` tiene columna `fecha_eliminacion` (implementada en US-FOLDER-001) y que las consultas existentes la respetan.
* **Entregables:**
    - Confirmación de existencia de columna `fecha_eliminacion`.
    - Revisión de queries para asegurar filtro `WHERE fecha_eliminacion IS NULL`.
---
* **Título:** Query eficiente para verificar si carpeta está vacía
* **Objetivo:** Determinar rápidamente si una carpeta tiene contenido antes de eliminar.
* **Tipo:** Tarea
* **Descripción corta:** Crear consulta optimizada que verifique si existen subcarpetas activas o documentos activos dentro de una carpeta.
* **Entregables:**
    - Query `carpeta_tiene_contenido(carpeta_id)` que retorne boolean.
    - Índices necesarios para performance.
    - Debe considerar solo registros activos (sin `fecha_eliminacion`).
---
#### Backend
---
* **Título:** Implementar servicio de verificación de carpeta vacía
* **Objetivo:** Determinar si una carpeta puede ser eliminada (está vacía).
* **Tipo:** Tarea
* **Descripción corta:** Crear método que verifique que la carpeta no tiene subcarpetas ni documentos activos. Debe ser eficiente para evitar cargar contenido completo.
* **Entregables:**
    - Método `estaVacia(carpetaId)` que retorne boolean.
    - Consulta optimizada (COUNT o EXISTS).
---
* **Título:** Implementar validación de permisos para eliminación de carpeta
* **Objetivo:** Verificar que el usuario tiene permiso de ADMINISTRACIÓN para eliminar.
* **Tipo:** Tarea
* **Descripción corta:** Validar que el usuario tiene nivel de acceso ADMINISTRACIÓN sobre la carpeta a eliminar. La historia menciona "administrador", interpretado como usuario con permiso de administración.
* **Entregables:**
    - Validación de permiso ADMINISTRACIÓN en servicio de eliminación.
    - Error 403 si no tiene permisos.
---
* **Título:** Implementar servicio de eliminación de carpetas (soft delete)
* **Objetivo:** Lógica de negocio para eliminar carpetas vacías.
* **Tipo:** Tarea
* **Descripción corta:** Crear servicio que valide permisos, verifique que la carpeta está vacía, y marque la carpeta con `fecha_eliminacion`. No debe eliminar físicamente.
* **Entregables:**
    - Método `eliminarCarpeta(carpetaId, usuarioId, organizacionId)`.
    - Validación de existencia y pertenencia a organización.
    - Validación de carpeta vacía.
    - Actualización de `fecha_eliminacion = NOW()`.
---
* **Título:** Prevenir eliminación de carpeta raíz
* **Objetivo:** Proteger la integridad de la estructura de carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Agregar validación que impida eliminar la carpeta raíz de la organización (`carpeta_padre_id = NULL`).
* **Entregables:**
    - Validación en servicio de eliminación.
    - Error 400 con mensaje claro si se intenta eliminar raíz.
---
* **Título:** Implementar endpoint `DELETE /api/carpetas/{id}`
* **Objetivo:** Exponer API REST para eliminar carpetas vacías.
* **Tipo:** Historia
* **Descripción corta:** Crear endpoint protegido que valide permisos de administración, verifique que la carpeta está vacía, y ejecute soft delete.
* **Entregables:**
    - Ruta/controlador `DELETE /api/carpetas/{id}`.
    - Respuesta 200/204 si eliminación exitosa.
    - Respuesta 403 si no tiene permisos de administración.
    - Respuesta 404 si carpeta no existe.
    - Respuesta 409 si carpeta tiene contenido.
    - Respuesta 400 si se intenta eliminar carpeta raíz.
---
* **Título:** Normalizar error de carpeta con contenido
* **Objetivo:** Mensaje claro cuando se intenta eliminar carpeta no vacía.
* **Tipo:** Tarea
* **Descripción corta:** Definir código y mensaje de error específico para el caso de carpeta con contenido que no puede eliminarse.
* **Entregables:**
    - Código de error: `CARPETA_NO_VACIA` (409).
    - Mensaje: "La carpeta debe vaciarse antes de eliminarla".
    - (Opcional) Incluir conteo de subcarpetas y documentos en respuesta.
---
* **Título:** Pruebas unitarias de verificación de carpeta vacía
* **Objetivo:** Asegurar que la lógica de verificación funciona correctamente.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios que verifiquen: carpeta vacía retorna true, carpeta con subcarpetas retorna false, carpeta con documentos retorna false, carpeta con ambos retorna false.
* **Entregables:**
    - Suite de tests unitarios con mocks de repositorios.
    - Cobertura de todas las combinaciones.
---
* **Título:** Pruebas unitarias de servicio de eliminación
* **Objetivo:** Asegurar lógica de negocio correcta para eliminación.
* **Tipo:** QA
* **Descripción corta:** Tests unitarios del servicio de eliminación cubriendo: eliminación exitosa, sin permisos, carpeta no vacía, carpeta raíz, carpeta inexistente.
* **Entregables:**
    - Suite de tests unitarios.
    - Verificación de que se actualiza `fecha_eliminacion` y no se borra físicamente.
---
* **Título:** Pruebas de integración de `DELETE /api/carpetas/{id}`
* **Objetivo:** Verificar endpoint completo con base de datos real.
* **Tipo:** QA
* **Descripción corta:** Tests de integración HTTP verificando respuestas 200/204, 403, 404, 409 según escenarios de aceptación.
* **Entregables:**
    - Tests para escenarios 1 y 2 de criterios de aceptación.
    - Verificación de que carpeta queda con `fecha_eliminacion` (no borrada).
    - Verificación de que carpeta eliminada no aparece en listados.
---
#### Frontend
---
* **Título:** Sin cambios de UI para US-FOLDER-004
* **Objetivo:** Aclarar alcance: esta historia define comportamiento de API.
* **Tipo:** Tarea
* **Descripción corta:** La interfaz de usuario para eliminar carpetas se implementará en US-FOLDER-005. Esta historia solo cubre la API.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
    - (Opcional) Colección Postman/HTTP para probar la API.
