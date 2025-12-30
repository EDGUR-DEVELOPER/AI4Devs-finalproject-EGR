## P6 — Búsqueda básica sin IA

### [US-SEARCH-003] UI mínima de búsqueda
---
#### Backend
---
* **Título:** Enriquecer respuesta de búsqueda con datos para navegación
* **Objetivo:** Incluir en los resultados de búsqueda la información necesaria para que el frontend pueda navegar al documento.
* **Tipo:** Tarea
* **Descripción corta:** Asegurar que el DTO de respuesta de búsqueda incluya todos los campos necesarios para la UI: `documento_id`, `nombre`, `carpeta_id`, `ruta_carpeta` (breadcrumb), `tipo_archivo`, `fecha_modificacion`.
* **Entregables:**
    - DTO `SearchResultDto` actualizado con campos adicionales.
    - Campo `ruta_carpeta` con path navegable (ej. "Proyectos/2024/Contratos").
    - Campo `tipo_archivo` para mostrar icono apropiado.
---
#### Frontend
---
* **Título:** Crear servicio de búsqueda en frontend
* **Objetivo:** Encapsular las llamadas al endpoint de búsqueda.
* **Tipo:** Tarea
* **Descripción corta:** Implementar un servicio/hook que maneje la comunicación con `GET /api/search`. Debe gestionar estados de carga, errores y debounce para evitar llamadas excesivas mientras el usuario escribe.
* **Entregables:**
    - Servicio `SearchService` o hook `useSearch`.
    - Método `search(term): Promise<SearchResult[]>`.
    - Debounce de 300ms en el input de búsqueda.
    - Estados: `loading`, `results`, `error`.
---
* **Título:** Implementar componente de barra de búsqueda
* **Objetivo:** Crear el input de búsqueda accesible desde cualquier pantalla.
* **Tipo:** Tarea
* **Descripción corta:** Diseñar e implementar un componente de input de búsqueda reutilizable. Debe incluir icono de búsqueda, placeholder descriptivo, y botón para limpiar. Ubicarlo en el header o navbar de la aplicación.
* **Entregables:**
    - Componente `SearchBar` con input controlado.
    - Icono de lupa y botón de limpiar (X).
    - Placeholder: "Buscar documentos...".
    - Integración con el servicio de búsqueda (onSearch callback).
    - Accesibilidad: label para screen readers, focus visible.
---
* **Título:** Implementar componente de lista de resultados de búsqueda
* **Objetivo:** Mostrar los documentos encontrados de forma clara y navegable.
* **Tipo:** Tarea
* **Descripción corta:** Crear componente que renderice la lista de resultados de búsqueda. Cada item debe mostrar nombre del documento, ruta de carpeta, fecha y tipo de archivo. Debe manejar estados vacíos y de carga.
* **Entregables:**
    - Componente `SearchResults` con lista de items.
    - Item de resultado con: icono por tipo, nombre (resaltando coincidencia), ruta, fecha.
    - Estado vacío: "No se encontraron resultados para '{término}'".
    - Estado cargando: skeleton o spinner.
    - Soporte de teclado: navegación con flechas, Enter para abrir.
---
* **Título:** Implementar dropdown/modal de resultados de búsqueda
* **Objetivo:** Mostrar resultados de forma no intrusiva mientras se escribe.
* **Tipo:** Tarea
* **Descripción corta:** Crear un contenedor (dropdown bajo el input o modal) que aparezca cuando hay resultados de búsqueda. Debe cerrarse al hacer clic fuera, al presionar Escape, o al seleccionar un resultado.
* **Entregables:**
    - Componente contenedor con posicionamiento relativo al SearchBar.
    - Lógica de apertura/cierre (click outside, Escape, selección).
    - Transiciones suaves de aparición/desaparición.
    - Z-index apropiado para superponerse al contenido.
---
* **Título:** Integrar navegación a documento desde resultados
* **Objetivo:** Permitir al usuario abrir un documento directamente desde los resultados.
* **Tipo:** Tarea
* **Descripción corta:** Al hacer clic en un resultado de búsqueda, navegar a la vista del documento o a la carpeta que lo contiene. Usar el router del framework para navegación SPA sin recarga.
* **Entregables:**
    - Handler de clic en item de resultado.
    - Navegación a `/documentos/{id}` o `/carpetas/{carpeta_id}?doc={id}`.
    - Cierre automático del dropdown de resultados tras navegación.
    - Manejo de errores si el documento ya no existe.
---
* **Título:** Implementar indicador de cantidad de resultados
* **Objetivo:** Informar al usuario cuántos documentos coinciden con su búsqueda.
* **Tipo:** Tarea
* **Descripción corta:** Mostrar un texto informativo con el número total de resultados encontrados. Si hay más resultados de los mostrados, indicar "Mostrando X de Y resultados".
* **Entregables:**
    - Texto de conteo en la parte superior de resultados.
    - Formato: "X resultados encontrados" o "Mostrando X de Y".
    - Manejo de singular/plural ("1 resultado" vs "N resultados").
---
* **Título:** Implementar link a búsqueda completa (página dedicada)
* **Objetivo:** Permitir ver todos los resultados cuando hay muchos.
* **Tipo:** Tarea
* **Descripción corta:** Si hay más resultados de los que caben en el dropdown, mostrar un link "Ver todos los resultados" que lleve a una página de búsqueda dedicada con paginación completa.
* **Entregables:**
    - Link "Ver todos los resultados" al final del dropdown.
    - Página `/search?q={término}` con resultados paginados.
    - Componente de paginación reutilizable.
---
* **Título:** Manejar estados de error en búsqueda
* **Objetivo:** Informar al usuario cuando la búsqueda falla.
* **Tipo:** Tarea
* **Descripción corta:** Mostrar mensajes de error apropiados cuando la API de búsqueda falla (error de red, error del servidor, timeout). Ofrecer opción de reintentar.
* **Entregables:**
    - Componente/estado de error en SearchResults.
    - Mensajes amigables: "Error al buscar. Intenta de nuevo."
    - Botón de reintentar que ejecute la búsqueda nuevamente.
    - Log de errores para debugging.
---
* **Título:** Implementar persistencia de búsqueda reciente
* **Objetivo:** Mejorar UX recordando búsquedas anteriores del usuario.
* **Tipo:** Tarea (Opcional/Nice-to-have)
* **Descripción corta:** Guardar las últimas N búsquedas del usuario en localStorage. Mostrarlas como sugerencias cuando el input está vacío y enfocado.
* **Entregables:**
    - Almacenamiento de últimas 5 búsquedas en localStorage.
    - Lista de "Búsquedas recientes" al enfocar input vacío.
    - Opción de borrar historial de búsquedas.
---
#### QA / Testing
---
* **Título:** Pruebas unitarias de componentes de búsqueda
* **Objetivo:** Verificar el comportamiento de los componentes de UI.
* **Tipo:** QA
* **Descripción corta:** Crear tests unitarios para los componentes de búsqueda: SearchBar, SearchResults, items de resultado. Usar testing-library para simular interacciones de usuario.
* **Entregables:**
    - Tests para SearchBar: input, debounce, limpiar.
    - Tests para SearchResults: renderizado de items, estados vacío/carga/error.
    - Tests de interacción: clic en resultado, navegación con teclado.
---
* **Título:** Pruebas de integración del flujo de búsqueda
* **Objetivo:** Verificar el flujo completo desde input hasta navegación.
* **Tipo:** QA
* **Descripción corta:** Tests de integración que simulen el flujo completo: escribir término, esperar resultados, hacer clic, verificar navegación. Usar mocks de API o MSW.
* **Entregables:**
    - Test: Escribir término → ver resultados → clic → navegación correcta.
    - Test: Término sin resultados → mensaje apropiado.
    - Test: Error de API → mensaje de error.
---
* **Título:** Pruebas E2E del flujo de búsqueda
* **Objetivo:** Verificar el funcionamiento real con backend.
* **Tipo:** QA
* **Descripción corta:** Tests end-to-end (Cypress/Playwright) que ejecuten búsquedas reales contra el backend, verificando que los resultados mostrados corresponden a documentos con permiso y que la navegación funciona.
* **Entregables:**
    - Test E2E: Login → Búsqueda → Ver resultados → Abrir documento.
    - Test E2E: Búsqueda con usuario sin permisos → resultados apropiados.
    - Test E2E: Búsqueda sin resultados → mensaje correcto.
---
* **Título:** Pruebas de accesibilidad de la UI de búsqueda
* **Objetivo:** Asegurar que la búsqueda es usable con tecnologías asistivas.
* **Tipo:** QA
* **Descripción corta:** Verificar accesibilidad de los componentes de búsqueda: navegación por teclado, labels para screen readers, contraste de colores, anuncios de resultados.
* **Entregables:**
    - Auditoría con herramienta de accesibilidad (axe, Lighthouse).
    - Test de navegación completa solo con teclado.
    - Verificación de ARIA labels y roles apropiados.
    - Corrección de issues de accesibilidad encontrados.
---
#### Diseño
---
* **Título:** Diseño de UI/UX para búsqueda
* **Objetivo:** Definir la apariencia y comportamiento de la interfaz de búsqueda.
* **Tipo:** Diseño
* **Descripción corta:** Crear mockups/wireframes del componente de búsqueda: barra en header, dropdown de resultados, página de resultados completa. Definir estados (vacío, cargando, con resultados, error).
* **Entregables:**
    - Wireframe de SearchBar en header.
    - Diseño de dropdown de resultados (máx 5-10 items visibles).
    - Diseño de página de búsqueda completa con paginación.
    - Especificación de estados y transiciones.
    - Guía de estilo: colores, tipografía, espaciados consistentes con el sistema.
