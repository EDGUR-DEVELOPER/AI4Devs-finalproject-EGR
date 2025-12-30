## P3 — Gestión de carpetas: API + UI mínima

### [US-FOLDER-005] UI mínima de navegación por carpetas
---
#### Base de datos
---
* **Título:** Sin cambios de BD para US-FOLDER-005
* **Objetivo:** Aclarar que esta historia es puramente de frontend.
* **Tipo:** Tarea
* **Descripción corta:** La UI de navegación consume las APIs definidas en US-FOLDER-001, US-FOLDER-002, US-FOLDER-004. No requiere cambios adicionales en base de datos.
* **Entregables:**
    - Confirmación de "no aplica" en planning.
---
#### Backend
---
* **Título:** Verificar que APIs de carpetas soportan necesidades de UI
* **Objetivo:** Asegurar que los endpoints existentes proveen toda la información necesaria para la UI.
* **Tipo:** Tarea
* **Descripción corta:** Revisar que los endpoints de P3 devuelven la información necesaria para renderizar el explorador: nombre, tipo, fecha, capacidades de usuario.
* **Entregables:**
    - Checklist de campos requeridos vs disponibles.
    - Ajustes menores a DTOs si falta información (ej. breadcrumb).
---
* **Título:** Implementar endpoint de breadcrumb/ruta de navegación
* **Objetivo:** Permitir mostrar la ruta completa desde raíz hasta carpeta actual.
* **Tipo:** Tarea
* **Descripción corta:** Crear endpoint que devuelva la jerarquía de carpetas desde la raíz hasta la carpeta solicitada, para mostrar breadcrumb en la UI.
* **Entregables:**
    - Ruta/controlador `GET /api/carpetas/{id}/ruta`.
    - Respuesta con array de `{ id, nombre }` ordenado desde raíz.
    - Alternativa: incluir `ruta` en respuesta de `GET /api/carpetas/{id}`.
---
#### Frontend
---
* **Título:** Diseño de wireframe para explorador de carpetas
* **Objetivo:** Definir la estructura visual del componente de navegación.
* **Tipo:** Diseño
* **Descripción corta:** Crear wireframe de baja fidelidad para la vista de explorador mostrando: breadcrumb, lista de contenido (iconos, nombres, fechas), acciones contextuales.
* **Entregables:**
    - Wireframe de vista principal del explorador.
    - Especificación de estados: vacío, cargando, error.
    - Mockup de acciones habilitadas/deshabilitadas según permisos.
---
* **Título:** Implementar servicio/cliente HTTP para APIs de carpetas
* **Objetivo:** Centralizar llamadas a APIs de carpetas desde el frontend.
* **Tipo:** Tarea
* **Descripción corta:** Crear servicio que encapsule las llamadas a endpoints de carpetas: listar contenido, crear carpeta, eliminar carpeta, obtener ruta.
* **Entregables:**
    - Servicio `CarpetaService` con métodos: `getContenido(id)`, `getRaiz()`, `crear(data)`, `eliminar(id)`, `getRuta(id)`.
    - Tipado de respuestas (interfaces TypeScript o equivalente).
    - Manejo de errores HTTP.
---
* **Título:** Implementar componente de breadcrumb de navegación
* **Objetivo:** Mostrar la ruta actual y permitir navegación rápida a carpetas padres.
* **Tipo:** Tarea
* **Descripción corta:** Crear componente que muestre la jerarquía de carpetas como breadcrumb clickeable. Cada segmento debe navegar a esa carpeta.
* **Entregables:**
    - Componente `Breadcrumb` o `RutaCarpeta`.
    - Navegación al hacer clic en cualquier nivel.
    - Estilo responsive (truncar en pantallas pequeñas).
---
* **Título:** Implementar componente de lista de contenido de carpeta
* **Objetivo:** Mostrar subcarpetas y documentos de la carpeta actual.
* **Tipo:** Tarea
* **Descripción corta:** Crear componente que renderice la lista de items (carpetas y documentos) con iconos diferenciadores, nombre, fecha, y acciones disponibles.
* **Entregables:**
    - Componente `ContenidoCarpeta` o `FileExplorer`.
    - Diferenciación visual entre carpetas y documentos (iconos).
    - Columnas: icono, nombre, tipo, fecha modificación.
    - Doble clic o clic en carpeta para navegar.
---
* **Título:** Implementar estado vacío para carpetas sin contenido
* **Objetivo:** Mostrar mensaje amigable cuando una carpeta está vacía.
* **Tipo:** Tarea
* **Descripción corta:** Crear vista de estado vacío con mensaje e ícono indicando que la carpeta no tiene contenido, con CTA para crear subcarpeta si tiene permisos.
* **Entregables:**
    - Componente o estado `EmptyFolder`.
    - Mensaje: "Esta carpeta está vacía".
    - Botón "Crear subcarpeta" visible si `puede_escribir`.
---
* **Título:** Implementar navegación entre carpetas (entrar/salir)
* **Objetivo:** Permitir al usuario navegar la jerarquía de carpetas.
* **Tipo:** Tarea
* **Descripción corta:** Implementar lógica de navegación: clic en carpeta carga su contenido, botón/breadcrumb para subir niveles, actualización de URL para deep linking.
* **Entregables:**
    - Navegación al hacer clic en subcarpeta.
    - Navegación hacia arriba vía breadcrumb.
    - Actualización de ruta en URL (ej. `/carpetas/{id}`).
    - Soporte para navegador (atrás/adelante).
---
* **Título:** Implementar indicadores de carga y estados de error
* **Objetivo:** Feedback visual durante operaciones asíncronas.
* **Tipo:** Tarea
* **Descripción corta:** Agregar spinners/skeleton durante carga de contenido, y mensajes de error con opción de reintentar cuando fallen las llamadas.
* **Entregables:**
    - Loading state con skeleton o spinner.
    - Error state con mensaje y botón "Reintentar".
    - Toast/notificación para errores de acciones (crear, eliminar).
---
* **Título:** Implementar modal/formulario para crear nueva carpeta
* **Objetivo:** Permitir crear carpetas desde la UI.
* **Tipo:** Tarea
* **Descripción corta:** Crear modal o inline form para ingresar nombre de nueva carpeta. Debe validar campos y llamar a API de creación.
* **Entregables:**
    - Componente `CrearCarpetaModal` o `NuevaCarpetaForm`.
    - Campo de nombre con validación (requerido, longitud).
    - Botones Cancelar y Crear.
    - Llamada a `POST /api/carpetas` y actualización de lista.
---
* **Título:** Implementar confirmación para eliminar carpeta
* **Objetivo:** Prevenir eliminaciones accidentales.
* **Tipo:** Tarea
* **Descripción corta:** Crear diálogo de confirmación antes de eliminar una carpeta. Mostrar mensaje claro y nombre de la carpeta a eliminar.
* **Entregables:**
    - Componente `ConfirmarEliminacion` o uso de modal genérico.
    - Mensaje: "¿Está seguro que desea eliminar la carpeta '{nombre}'?".
    - Manejo de error 409 (carpeta no vacía) con mensaje específico.
---
* **Título:** Implementar control de acciones según permisos del usuario
* **Objetivo:** Habilitar/deshabilitar acciones según capacidades del usuario.
* **Tipo:** Tarea
* **Descripción corta:** Usar los flags `puede_escribir` y `puede_administrar` de la respuesta de API para habilitar/deshabilitar botones de crear, eliminar, etc.
* **Entregables:**
    - Botón "Nueva carpeta" visible solo si `puede_escribir` en carpeta actual.
    - Opción "Eliminar" visible solo si `puede_administrar` en carpeta específica.
    - Tooltips explicando por qué una acción está deshabilitada.
---
* **Título:** Implementar menú contextual o acciones por item
* **Objetivo:** Acceso rápido a acciones sobre carpetas y documentos.
* **Tipo:** Tarea
* **Descripción corta:** Agregar menú contextual (clic derecho) o columna de acciones con opciones según permisos: abrir, eliminar, mover (futuro).
* **Entregables:**
    - Menú contextual o dropdown de acciones por fila.
    - Acciones condicionadas por permisos.
    - Iconos representativos para cada acción.
---
* **Título:** Pruebas de componente de explorador de carpetas
* **Objetivo:** Verificar comportamiento correcto de la UI.
* **Tipo:** QA
* **Descripción corta:** Tests de componente que verifiquen: renderizado de lista, navegación entre carpetas, estados vacío/carga/error, acciones según permisos.
* **Entregables:**
    - Tests unitarios de componentes con mocks de API.
    - Tests de integración de flujo de navegación.
---
* **Título:** Pruebas E2E de flujo de navegación de carpetas
* **Objetivo:** Verificar flujo completo de usuario en navegación.
* **Tipo:** QA
* **Descripción corta:** Tests end-to-end que simulen usuario navegando: ver raíz, entrar a subcarpeta, crear carpeta, eliminar carpeta vacía, usar breadcrumb.
* **Entregables:**
    - Tests E2E cubriendo escenario 1 de criterios de aceptación.
    - Verificación de que acciones deshabilitadas no son ejecutables.
---
* **Título:** Revisión de accesibilidad del explorador
* **Objetivo:** Asegurar que el explorador sea usable con teclado y lectores de pantalla.
* **Tipo:** QA
* **Descripción corta:** Verificar navegación con teclado (Tab, Enter, Escape), roles ARIA apropiados, y contraste de colores.
* **Entregables:**
    - Checklist de accesibilidad verificado.
    - Correcciones de issues encontrados.
