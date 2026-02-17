# Historias de Usuario

## Épicas priorizadas (MVP)

1. **P0 — Autenticación + Organizacion (multi-organizacion)**
    - Alcance: login, token con claims, aislamiento de datos por organización y manejo de sesión.
2. **P1 — Administración (UI mínima Admin/Usuario)**
    - Alcance: UI mínima para administrar usuarios/roles dentro de una organización.
3. **P2 — Permisos granulares (ACL) por carpeta/documento**
    - Alcance: permisos por objeto, herencia (si aplica) y enforcement en API/UI.
4. **P3 — Gestión de carpetas (API + UI mínima)**
    - Alcance: crear/navegar jerarquía de carpetas por organizacion.
5. **P4 — Documentos + versionado lineal (API + UI mínima)**
    - Alcance: subir documentos, crear nuevas versiones y consultar versión actual.
6. **P5 — Auditoría (logs inmutables + vista Admin mínima)**
    - Alcance: registrar eventos críticos y permitir consulta básica.
7. **P6 — Búsqueda básica (sin IA, respetando permisos)**
    - Alcance: búsqueda por nombre/metadatos con control de acceso.

---

### P0 — Historias de Usuario (Autenticación + Organizacion)

**[US-AUTH-001] Login multi-organizacion (organización predeterminada)**
- **Narrativa:** Como usuario, quiero iniciar sesión y que el sistema use mi organización predeterminada, para que el acceso sea simple y consistente.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un usuario válido con exactamente una organización activa, Cuando envío `POST /auth/login` con credenciales válidas, Entonces recibo `200` con un token.
    - *Scenario 1b:* Dado un usuario válido perteneciente a múltiples organizaciones activas y con una organización marcada como predeterminada, Cuando envío `POST /auth/login` con credenciales válidas, Entonces recibo `200` con un token emitido para la organización predeterminada.
    - *Scenario 2:* Dado un usuario válido perteneciente a 2 organizaciones activas y sin una organización predeterminada, Cuando envío `POST /auth/login`, Entonces recibo `409` indicando configuración inválida.
    - *Scenario 2b:* Dado un usuario válido perteneciente a más de 2 organizaciones activas, Cuando envío `POST /auth/login`, Entonces recibo `409` indicando que el caso no está soportado en el MVP.
    - *Scenario 2c:* Dado un usuario autenticado con múltiples organizaciones activas, Cuando envío `POST /auth/switch` indicando otra `organizacion_id` válida, Entonces recibo `200` con un nuevo token en el contexto de esa organización.
    - *Scenario 3:* Dado credenciales inválidas, Cuando envío `POST /auth/login`, Entonces recibo `401`.
    - *Scenario 4:* Dado un usuario válido sin organizaciones activas, Cuando envío `POST /auth/login`, Entonces recibo `403` con un error indicando que no pertenece a ninguna organización activa.
- **Notas Técnicas/Datos:** `organizacion_id` debe validarse contra pertenencia del usuario (y contra organización activa) en `POST /auth/switch`.

**[US-AUTH-002] Token con claims de organizacion y roles**
- **Narrativa:** Como sistema, quiero emitir un token con `usuario_id`, `organizacion_id` y roles/permisos, para que la autorización sea consistente en toda la plataforma.
- **Criterios de Aceptación:**
  - *Scenario 1:* Dado un login exitoso, Cuando se emite el token, Entonces incluye `usuario_id` y `organizacion_id` y al menos un rol.
- **Notas Técnicas/Datos:** Definir claim estándar (por ejemplo `org_id`, `roles`).

**[US-AUTH-003] Middleware de autenticación para endpoints protegidos**
- **Narrativa:** Como sistema, quiero validar el token en cada request protegida, para que solo usuarios autenticados accedan a recursos.
- **Criterios de Aceptación:**
  - *Scenario 1:* Dado un request sin token a un endpoint protegido, Cuando se procesa, Entonces recibo `401`.
  - *Scenario 2:* Dado un token inválido/alterado, Cuando se procesa, Entonces recibo `401`.

**[US-AUTH-004] Aislamiento de datos por organización (organizacion isolation)**
- **Narrativa:** Como organización, quiero que los datos estén aislados entre organizacions, para garantizar seguridad y cumplimiento.
- **Criterios de Aceptación:**
  - *Scenario 1:* Dado un token del organizacion A, Cuando intento acceder/crear recursos en el organizacion B, Entonces recibo `404` (o `403`) sin filtrar datos.
- **Notas Técnicas/Datos:** En queries/escrituras, `organizacion_id` debe venir del token (no del cliente).

**[US-AUTH-005] UI mínima de Login (Admin/Usuario)**
- **Narrativa:** Como usuario, quiero una pantalla de login simple, para acceder al sistema sin usar herramientas externas.
- **Criterios de Aceptación:**
  - *Scenario 1:* Dado credenciales válidas, Cuando inicio sesión desde la UI, Entonces se almacena el token y accedo a la pantalla principal.
    - *Scenario 1b:* Dado credenciales válidas y múltiples organizaciones, Cuando inicio sesión, Entonces el sistema usa la organización predeterminada y accedo a la pantalla principal (o veo un error claro si falta predeterminada o hay >2 organizaciones activas).
  - *Scenario 2:* Dado credenciales inválidas, Cuando inicio sesión, Entonces veo un mensaje de error y permanezco en login.

**[US-AUTH-006] Manejo de sesión expirada**
- **Narrativa:** Como usuario, quiero que el sistema detecte la expiración de mi sesión, para reautenticarme de forma clara.
- **Criterios de Aceptación:**
  - *Scenario 1:* Dado un token expirado, Cuando hago una petición protegida desde la UI, Entonces se redirige a login con un mensaje “sesión expirada”.

---

### P1 — Historias de Usuario (Administración: UI mínima Admin/Usuario)

**[US-ADMIN-001] Crear usuario (API) dentro del organizacion**
- **Narrativa:** Como administrador, quiero crear un usuario en mi organización, para habilitar su acceso a DocFlow.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un administrador autenticado del organizacion A, Cuando creo un usuario con email válido, Entonces recibo `201` y el usuario pertenece al organizacion A.
    - *Scenario 2:* Dado un email ya existente, Cuando intento crear el usuario, Entonces recibo `400/409` por duplicidad (email global).
- **Notas Técnicas/Datos:** Para multi-org, el “pertenece al organizacion A” se implementa creando un registro en `Usuario_Organizacion` (membresía). Unicidad por `email`.

**[US-ADMIN-002] Asignar rol a usuario (API) en el organizacion**
- **Narrativa:** Como administrador, quiero asignar un rol a un usuario, para controlar sus capacidades.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un usuario del organizacion A, Cuando asigno un rol válido del organizacion A, Entonces recibo `200` y el rol queda efectivo.
    - *Scenario 2:* Dado un usuario de otro organizacion, Cuando intento asignar roles, Entonces recibo `404` (o `403`) sin exponer datos.

**[US-ADMIN-003] Listar usuarios (API) del organizacion con roles**
- **Narrativa:** Como administrador, quiero listar los usuarios de mi organización con sus roles, para administrar accesos.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un administrador autenticado, Cuando solicito la lista, Entonces solo veo usuarios del organizacion actual.

**[US-ADMIN-004] Desactivar usuario (API) sin borrado**
- **Narrativa:** Como administrador, quiero desactivar un usuario, para revocar acceso manteniendo historial.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un usuario desactivado, Cuando intenta iniciar sesión, Entonces recibe `403`.
    - *Scenario 2:* Dado un usuario desactivado, Cuando intento usar endpoints con token previo (si existiera), Entonces recibe `401/403`.

**[US-ADMIN-005] UI mínima de gestión de usuarios**
- **Narrativa:** Como administrador, quiero una pantalla simple para crear/listar/desactivar usuarios, para operar el sistema sin scripts.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un administrador, Cuando navego a “Usuarios”, Entonces veo una tabla simple con email, estado y roles.

---

### P2 — Historias de Usuario (Permisos granulares: ACL por carpeta/documento)

**[US-ACL-001] Definir niveles de acceso estándar (catálogo mínimo)**
- **Narrativa:** Como sistema, quiero un conjunto mínimo y consistente de niveles de acceso, para evaluar permisos de forma uniforme.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado el sistema inicializado, Cuando se consultan niveles, Entonces existen al menos `LECTURA`, `ESCRITURA`, `ADMINISTRACION`.
- **Notas Técnicas/Datos:** El nivel controla acciones (ver/listar/descargar vs. subir/modificar vs. administrar permisos).

**[US-ACL-002] Conceder permiso de carpeta a usuario (crear ACL)**
- **Narrativa:** Como administrador, quiero conceder un permiso sobre una carpeta a un usuario, para controlar acceso por área.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un admin del organizacion A, Cuando asigno `LECTURA` a un usuario del organizacion A sobre una carpeta, Entonces el usuario puede listar/ver esa carpeta.
    - *Scenario 2:* Dado un usuario/carpeta de otro organizacion, Cuando intento asignar permisos, Entonces recibo `404/403` sin filtrar información.

**[US-ACL-003] Revocar permiso de carpeta (eliminar ACL)**
- **Narrativa:** Como administrador, quiero revocar un permiso sobre una carpeta, para retirar accesos.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un usuario con acceso por ACL, Cuando revoco el permiso, Entonces el usuario deja de poder acceder (`403`).

**[US-ACL-004] Permiso recursivo en carpeta (herencia simple)**
- **Narrativa:** Como administrador, quiero que un permiso de carpeta pueda aplicarse a subcarpetas, para evitar configuraciones repetitivas.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un permiso con `recursivo=true` en una carpeta padre, Cuando el usuario accede a una subcarpeta, Entonces el permiso aplica.
    - *Scenario 2:* Dado `recursivo=false`, Cuando accede a una subcarpeta, Entonces no aplica.

**[US-ACL-005] Conceder permiso explícito a documento**
- **Narrativa:** Como administrador, quiero asignar un permiso directamente a un documento, para manejar excepciones de acceso.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un documento en una carpeta, Cuando asigno `LECTURA` explícita a un usuario, Entonces el usuario puede acceder a ese documento.

**[US-ACL-006] Regla de precedencia de permisos (Documento > Carpeta)**
- **Narrativa:** Como sistema, quiero una regla clara de precedencia, para resolver conflictos de permisos.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un permiso explícito de documento para un usuario, Cuando se evalúa el acceso al documento, Entonces ese permiso explícito se usa como fuente de verdad.
    - *Scenario 2:* Dado que NO existe permiso explícito de documento, Cuando se evalúa el acceso, Entonces se usa el permiso de carpeta (incluyendo herencia si aplica).
- **Notas Técnicas/Datos:** Regla simple para MVP: `Permiso_Documento` (si existe) > `Permiso_Carpeta`.

**[US-ACL-007] UI muestra capacidades (acciones habilitadas) por carpeta/documento**
- **Narrativa:** Como usuario, quiero que la UI habilite o deshabilite acciones según mis permisos, para evitar errores.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un usuario con solo `LECTURA`, Cuando navega una carpeta, Entonces la UI deshabilita “Subir” y “Administrar permisos”.

---

### P3 — Historias de Usuario (Gestión de carpetas: API + UI mínima)

**[US-FOLDER-001] Crear carpeta (API) en el organizacion actual**
- **Narrativa:** Como usuario con permisos, quiero crear una carpeta en mi organización, para organizar documentos.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un usuario con `ESCRITURA` (o `ADMINISTRACION`) en la carpeta padre, Cuando crea una carpeta, Entonces recibe `201` y la carpeta pertenece al organizacion del token.
    - *Scenario 2:* Dado un usuario sin permiso en la carpeta padre, Cuando crea una carpeta, Entonces recibe `403`.

**[US-FOLDER-002] Listar contenido de carpeta (API) con visibilidad por permisos**
- **Narrativa:** Como usuario, quiero listar subcarpetas y documentos visibles, para navegar la estructura documental.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un usuario con `LECTURA`, Cuando lista una carpeta, Entonces solo ve elementos permitidos.
    - *Scenario 2:* Dado un usuario sin `LECTURA`, Cuando lista una carpeta, Entonces recibe `403`.

**[US-FOLDER-003] Mover documento a otra carpeta (API)**
- **Narrativa:** Como usuario con permisos, quiero mover un documento entre carpetas, para mantener orden.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado `ESCRITURA` en carpeta origen y destino, Cuando muevo un documento, Entonces su `carpeta_id` se actualiza y la acción queda auditada.
    - *Scenario 2:* Dado falta de permiso en origen o destino, Cuando muevo un documento, Entonces recibo `403`.

**[US-FOLDER-004] Eliminar carpeta vacía (soft delete) (API)**
- **Narrativa:** Como administrador, quiero eliminar una carpeta vacía, para mantener higiene sin perder trazabilidad.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado una carpeta sin hijos ni documentos, Cuando la elimino, Entonces queda marcada con `fecha_eliminacion`.
    - *Scenario 2:* Dado una carpeta con contenido, Cuando la elimino, Entonces recibo `409` (o `400`) indicando que debe vaciarse primero.

**[US-FOLDER-005] UI mínima de navegación por carpetas**
- **Narrativa:** Como usuario, quiero una vista tipo explorador para entrar/salir de carpetas, para encontrar mis documentos.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un usuario autenticado, Cuando entra a una carpeta desde la UI, Entonces ve su contenido y puede navegar a subcarpetas.

---

### P4 — Historias de Usuario (Documentos + versionado lineal: API + UI mínima)

**[US-DOC-001] Subir documento (API) crea documento + versión 1**
- **Narrativa:** Como usuario con permisos, quiero subir un documento a una carpeta, para centralizarlo y compartirlo.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado `ESCRITURA` en la carpeta, Cuando subo un archivo, Entonces recibo `201` con `documento_id` y `version_actual` con `numero_secuencial=1`.
    - *Scenario 2:* Dado sin permisos, Cuando subo, Entonces recibo `403`.

**[US-DOC-002] Descargar versión actual (API)**
- **Narrativa:** Como usuario con `LECTURA`, quiero descargar la versión actual, para usar el documento.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado `LECTURA`, Cuando descargo, Entonces recibo `200` con el binario.
    - *Scenario 2:* Dado sin `LECTURA`, Cuando descargo, Entonces recibo `403`.

**[US-DOC-003] Subir nueva versión (API) incrementa secuencia**
- **Narrativa:** Como usuario con permisos, quiero subir una nueva versión, para mantener historial sin sobrescribir.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un documento existente y `ESCRITURA`, Cuando subo una nueva versión, Entonces se crea una nueva versión con `numero_secuencial` incrementado y pasa a ser `version_actual`.

**[US-DOC-004] Listar versiones (API) ordenadas**
- **Narrativa:** Como usuario, quiero listar el historial de versiones, para entender la evolución del documento.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un documento, Cuando consulto versiones, Entonces recibo una lista ordenada ascendente por `numero_secuencial`.

**[US-DOC-005] Cambiar versión actual (API) (rollback)**
- **Narrativa:** Como usuario autorizado, quiero marcar una versión anterior como actual, para revertir cambios.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un documento con múltiples versiones y permiso requerido, Cuando selecciono una versión anterior, Entonces `version_actual_id` cambia y se registra auditoría.

**[US-DOC-006] UI mínima de carga y ver historial**
- **Narrativa:** Como usuario, quiero subir documentos y ver su historial desde la UI, para operar sin herramientas externas.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado permisos, Cuando subo desde la UI, Entonces el documento aparece en la carpeta.
    - *Scenario 2:* Dado un documento con versiones, Cuando abro “Versiones”, Entonces veo el listado y cuál es la actual.

**[US-DOC-007] Descarga de documento actual desde lista de documentos**
- **Narrativa:** Como administrador o usuario con permiso de lectura, quiero descargar la versión actual de un documento directamente desde la lista de documentos en la UI, para poder acceder al contenido del archivo sin navegar al historial de versiones.
- **Cirterios de Aceptación:**
    - **Escenario 1:** Descarga exitosa con permiso de lectura
        Dado que soy un usuario autenticado con permiso de LECTURA sobre un documento
        Cuando hago clic en el botón "Descargar" en la lista de documentos
        Entonces el sistema descarga la versión actual del documento con el nombre original y extensión    correcta
        Y se muestra una notificación de éxito "Descarga iniciada"
        Y se emite un evento de auditoría DOCUMENTO_DESCARGADO

    - **Escenario 2:** Descarga con permiso de escritura o administración
        Dado que soy un usuario con permiso de ESCRITURA o ADMINISTRACION sobre un documento
        Cuando hago clic en el botón "Descargar"
        Entonces el sistema descarga el documento exitosamente
        Y se comporta igual que en el escenario 1
        
    - **Escenario 3:** Botón deshabilitado sin permiso de lectura
        Dado que soy un usuario sin permiso de LECTURA sobre un documento
        Cuando visualizo la lista de documentos
        Entonces el botón "Descargar" aparece deshabilitado o no visible para ese documento
        Y si intento acceder directamente a la URL de descarga, recibo un error 403

    - **Escenario 4:** Manejo de error en descarga
        Dado que tengo permiso para descargar un documento
        Cuando hago clic en descargar y ocurre un error (red, archivo no disponible, etc.)
        Entonces se muestra una notificación de error detallando el problema
        Y el botón vuelve a su estado normal permitiendo reintentar

    - **Escenario 5:** Indicador visual durante descarga
        Dado que inicio una descarga de documento
        Cuando la descarga está en progreso
        Entonces el botón muestra un spinner o indicador de carga
        Y el botón queda deshabilitado hasta completar la operación

**[US-DOC-008] Eliminación de Documento desde la UI**
- **Narrativa:** Como administrador o usuario con permiso de escritura, quiero eliminar documentos desde la UI marcándolos como eliminados (soft delete), para mantener la organización del sistema sin perder trazabilidad ni historial para auditoría.
- **Criterios de Aceptación:**

    - **Escenario 1:** Eliminación exitosa con permiso de escritura
    Dado que soy un usuario con permiso de ESCRITURA o ADMINISTRACION sobre un documento
    Cuando hago clic en el botón "Eliminar" y confirmo la acción en el diálogo de confirmación
    Entonces el documento queda marcado con fecha_eliminacion en la base de datos (soft delete)
    Y el documento desaparece inmediatamente de la lista visible en la UI
    Y se muestra una notificación "Documento eliminado exitosamente"
    Y se emite un evento de auditoría DOCUMENTO_ELIMINADO

    - **Escenario 2:** Diálogo de confirmación antes de eliminar
    Dado que tengo permiso para eliminar un documento
    Cuando hago clic en el botón "Eliminar"
    Entonces aparece un diálogo modal de confirmación
    Y el diálogo muestra el nombre del documento y advierte que la acción no es reversible en la UI
    Y debo confirmar explícitamente antes de proceder

    - **Escenario 3:** Cancelación de eliminación
    Dado que he abierto el diálogo de confirmación de eliminación
    Cuando hago clic en "Cancelar" o cierro el diálogo
    Entonces el diálogo se cierra sin realizar cambios
    Y el documento permanece visible en la lista

    - **Escenario 4:** Botón no visible sin permiso de escritura
    Dado que soy un usuario con solo permiso de LECTURA sobre un documento
    Cuando visualizo la lista de documentos
    Entonces el botón "Eliminar" no aparece o está claramente deshabilitado para ese documento

    - **Escenario 5:** Error de permisos al eliminar
    Dado que intento eliminar un documento sin permisos suficientes
    Cuando el backend rechaza la operación
    Entonces se muestra un error "No tiene permisos para eliminar este documento"
    Y el documento permanece visible en la lista

    - **Escenario 6:** Manejo de error en eliminación
    Dado que tengo permisos para eliminar
    Cuando ocurre un error durante la eliminación (error de red, BD, etc.)
    Entonces se muestra una notificación de error descriptiva
    Y el documento permanece visible en la lista
    Y puedo reintentar la operación

    - **Escenario 7:** Validación de tenant isolation
    Dado que un usuario intenta eliminar un documento de otra organización
    Cuando se procesa la solicitud
    Entonces el backend retorna 404 (no 403) para no filtrar existencia
    Y la UI muestra "Documento no encontrado"
---

### P5 — Historias de Usuario (Auditoría: logs inmutables + UI mínima)

**[US-AUDIT-001] Emitir evento de auditoría en acciones críticas**
- **Narrativa:** Como sistema, quiero emitir un evento/auditoría por cada acción crítica, para tener trazabilidad.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado la acción “crear carpeta”, Cuando se completa, Entonces se genera un evento con `codigo_evento`, `organizacion_id` y `usuario_id`.
    - *Scenario 2:* Dado la acción “subir documento”, Cuando se completa, Entonces se genera un evento similar.

**[US-AUDIT-002] Persistir auditoría como registro inmutable**
- **Narrativa:** Como administrador, quiero que la auditoría sea inmutable, para confiar en su integridad.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un evento generado, Cuando se persiste, Entonces queda almacenado con timestamp y no puede editarse por endpoints del MVP.

**[US-AUDIT-003] Consultar auditoría (API) con paginación y fechas**
- **Narrativa:** Como administrador, quiero consultar la auditoría por rango de fechas, para investigar actividad.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un admin del organizacion A, Cuando consulta auditoría con `desde/hasta`, Entonces recibe solo eventos del organizacion A.
    - *Scenario 2:* Dado paginación, Cuando solicita página siguiente, Entonces recibe resultados consistentes.

**[US-AUDIT-004] UI mínima de auditoría**
- **Narrativa:** Como administrador, quiero una vista simple de auditoría, para revisar eventos sin herramientas externas.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un administrador autenticado, Cuando abre “Auditoría”, Entonces ve una lista/tabla con `codigo_evento`, usuario, fecha y entidad afectada.

---

### P6 — Historias de Usuario (Búsqueda básica sin IA)

**[US-SEARCH-001] Buscar documentos (API) por texto**
- **Narrativa:** Como usuario, quiero buscar documentos por texto (nombre/metadatos), para encontrarlos rápidamente.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un término de búsqueda, Cuando consulto, Entonces recibo una lista de documentos del organizacion actual.

**[US-SEARCH-002] La búsqueda respeta permisos y no filtra existencia**
- **Narrativa:** Como organización, quiero que la búsqueda no devuelva documentos no autorizados, para evitar filtraciones.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un usuario sin `LECTURA` sobre un documento, Cuando busca términos que coinciden, Entonces el documento no aparece en resultados.

**[US-SEARCH-003] UI mínima de búsqueda**
- **Narrativa:** Como usuario, quiero una barra de búsqueda y resultados clicables, para abrir documentos sin navegar carpetas.
- **Criterios de Aceptación:**
    - *Scenario 1:* Dado un término, Cuando busco desde la UI, Entonces veo resultados y puedo abrir el documento si tengo permisos.