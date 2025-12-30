## P0 — Autenticación + Organizacion

### [US-AUTH-004] Aislamiento de datos por organización (organizacion isolation)
---
#### Base de datos
---
* **Título:** Auditoría y Migración de Schema para Multi-tenancy
* **Objetivo:** Garantizar que todas las tablas sensibles tengan la columna discriminadora.
* **Tipo:** Tarea
* **Descripción corta:** Revisar el esquema actual de base de datos. Crear un script de migración para asegurar que las tablas `usuarios`, `roles`, `carpetas`, `documentos` y `auditoria` tengan la columna `organizacion_id` (FK) con restricción `NOT NULL`.
* **Entregables:**
    - Script SQL de migración (`ALTER TABLE ... ADD COLUMN ...`).
    - Diagrama ER actualizado.
---
#### Backend
---
* **Título:** Utilidad de Contexto de Organización
* **Objetivo:** Extraer y disponibilizar el `organizacion_id` de forma segura en la capa de servicio.
* **Tipo:** Tarea
* **Descripción corta:** Crear un helper o servicio que, dado el contexto de la petición (request object poblado por el middleware de Auth), extraiga el `organizacion_id` del token JWT. Debe lanzar una excepción crítica si el ID no está presente en el contexto de una ruta protegida.
* **Entregables:**
    - Función/Clase `CurrentTenantService` o similar.
    - Unit tests probando extracción exitosa y fallo si no hay token.
---
* **Título:** Capa de Persistencia - Inyección en Escritura (Create/Update)
* **Objetivo:** Asegurar que nada se guarde sin el ID de la organización del token.
* **Tipo:** Tarea
* **Descripción corta:** Modificar el repositorio base o los repositorios específicos (Usuarios, Carpetas, Docs). Al crear (`INSERT`) o actualizar (`UPDATE`), el sistema debe sobrescribir cualquier `organizacion_id` que venga del input con el valor obtenido del `CurrentTenantService`.
* **Entregables:**
    - Código de repositorios actualizado.
    - Test de integración: Intentar guardar un objeto con `org_id=999` y verificar que se guardó con el `org_id` del token del usuario.
---
* **Título:** Capa de Persistencia - Filtrado Global en Lectura (Read/List)
* **Objetivo:** Asegurar que ninguna consulta devuelva datos de otros "tenants".
* **Tipo:** Historia
* **Descripción corta:** Implementar un "Scope" global o cláusulas `WHERE` obligatorias en todas las consultas `find`, `findAll` o `search`. La consulta debe concatenar siempre `AND organizacion_id = [ID_TOKEN]`.
* **Entregables:**
    - Implementación de Global Scope (si se usa ORM) o modificación de queries base.
    - Test: Insertar datos en Org A y Org B. Consultar como usuario de Org A y verificar que solo llegan datos de A.
---
* **Título:** Manejo de Errores de Aislamiento (Security by Obscurity)
* **Objetivo:** Evitar revelar la existencia de recursos de otras organizaciones.
* **Tipo:** Tarea / Seguridad
* **Descripción corta:** Implementar la lógica para `GET /resource/:id`. Si el recurso existe en la DB pero `recurso.organizacion_id != token.organizacion_id`, el sistema debe lanzar una excepción que se traduzca en un HTTP `404 Not Found`, no un `403`.
* **Entregables:**
    - Middleware o lógica de servicio para captura de recursos.
    - Test: Pedir un ID existente de otra org y recibir 404.
---
#### Frontend
---
* **Título:** Interceptor de Errores de Recurso no Encontrado
* **Objetivo:** Manejar la respuesta de aislamiento en la UI.
* **Tipo:** Tarea
* **Descripción corta:** Configurar el cliente HTTP (ej. Axios/Fetch) para interceptar errores 404 en peticiones de recursos específicos. Si un usuario intenta acceder por URL directa a un recurso que no es de su organización (y recibe 404), mostrar una página de "Recurso no encontrado o sin acceso".
* **Entregables:**
    - Interceptor configurado.
    - Página o componente de "Not Found" genérico.
