## P5 — Auditoría: logs inmutables + UI mínima

### [US-AUDIT-004] UI mínima de auditoría
---
#### Backend
---
* **Título:** Endpoint para obtener catálogo de códigos de evento
* **Objetivo:** Proveer al frontend la lista de códigos de evento para filtros.
* **Tipo:** Tarea
* **Descripción corta:** Crear endpoint `GET /audit/event-codes` que retorne la lista de códigos de evento disponibles con sus descripciones. Útil para poblar dropdowns de filtro en la UI.
* **Entregables:**
    - Endpoint `GET /audit/event-codes`.
    - Respuesta con array `[{ codigo, descripcion, categoria }]`.
    - Cache de respuesta (códigos son estáticos).
---
#### Frontend
---
* **Título:** Crear página/ruta de Auditoría
* **Objetivo:** Definir la estructura base de la vista de auditoría.
* **Tipo:** Tarea
* **Descripción corta:** Crear componente página `AuditPage` y configurar ruta `/admin/audit` o `/audit`. La página debe estar protegida para usuarios administradores. Incluir layout básico con título y contenedor para tabla.
* **Entregables:**
    - Componente `AuditPage`.
    - Ruta configurada en router.
    - Guard de ruta para rol administrador.
    - Redirección a login o 403 si no autorizado.
---
* **Título:** Crear componente de tabla de auditoría
* **Objetivo:** Mostrar los eventos de auditoría en formato tabular.
* **Tipo:** Historia
* **Descripción corta:** Implementar componente `AuditTable` que muestre columnas: Código de evento, Usuario, Fecha/Hora, Entidad afectada (tipo + ID), y opcionalmente Detalles. La tabla debe ser responsive y manejar estado de carga.
* **Entregables:**
    - Componente `AuditTable` con columnas definidas.
    - Formateo de fecha a formato legible (locale del usuario).
    - Indicador de carga (skeleton o spinner).
    - Estado vacío cuando no hay resultados.
---
* **Título:** Implementar filtros de búsqueda de auditoría
* **Objetivo:** Permitir al administrador filtrar eventos por criterios.
* **Tipo:** Tarea
* **Descripción corta:** Crear componente `AuditFilters` con campos: selector de rango de fechas (desde/hasta), dropdown de código de evento, y botón de aplicar filtros. Los filtros deben actualizar la URL para permitir compartir/guardar búsquedas.
* **Entregables:**
    - Componente `AuditFilters`.
    - Date picker para `desde` y `hasta`.
    - Dropdown con códigos de evento (desde API).
    - Botón "Buscar" que dispare consulta.
    - Sincronización de filtros con query params de URL.
---
* **Título:** Implementar paginación en UI de auditoría
* **Objetivo:** Permitir navegación entre páginas de resultados.
* **Tipo:** Tarea
* **Descripción corta:** Agregar componente de paginación debajo de la tabla. Mostrar página actual, total de páginas y total de registros. Permitir navegación a página siguiente/anterior y salto a página específica.
* **Entregables:**
    - Componente `Pagination` o uso de librería existente.
    - Botones Anterior/Siguiente con estados disabled apropiados.
    - Indicador de "Página X de Y (Z registros)".
    - Actualización de URL al cambiar de página.
---
* **Título:** Integrar tabla con servicio de API
* **Objetivo:** Conectar la UI con el endpoint de auditoría.
* **Tipo:** Tarea
* **Descripción corta:** En `AuditPage`, usar `AuditApiService` para cargar datos al montar y al cambiar filtros/página. Manejar estados de loading, error y datos vacíos. Implementar debounce en filtros si es necesario.
* **Entregables:**
    - Hook `useAuditLogs(filters)` o lógica equivalente.
    - Carga inicial al montar componente.
    - Recarga al cambiar filtros o página.
    - Manejo de error con mensaje al usuario.
---
* **Título:** Agregar enlace de navegación a Auditoría en menú admin
* **Objetivo:** Hacer accesible la vista de auditoría desde la navegación principal.
* **Tipo:** Tarea
* **Descripción corta:** Agregar item "Auditoría" en el menú lateral o de navegación, visible solo para administradores. El enlace debe llevar a `/admin/audit`.
* **Entregables:**
    - Item de menú "Auditoría" con icono apropiado.
    - Visibilidad condicionada a rol administrador.
    - Highlight cuando la ruta está activa.
---
* **Título:** Implementar modal/panel de detalles de evento
* **Objetivo:** Permitir ver información completa de un evento específico.
* **Tipo:** Tarea
* **Descripción corta:** Al hacer clic en una fila de la tabla, mostrar modal o panel lateral con todos los detalles del evento, incluyendo metadata en formato JSON legible. Opcional para MVP pero mejora usabilidad.
* **Entregables:**
    - Componente `AuditEventDetail`.
    - Visualización de todos los campos del evento.
    - Formateo de JSON para metadata.
    - Botón de cerrar modal/panel.
---
* **Título:** Estilos y UX de la página de auditoría
* **Objetivo:** Asegurar que la UI sea consistente con el resto del sistema y usable.
* **Tipo:** Diseño / Tarea
* **Descripción corta:** Aplicar estilos consistentes con el design system del proyecto. Asegurar accesibilidad básica (contraste, navegación por teclado). Validar responsive en tablet y desktop (no prioritario mobile).
* **Entregables:**
    - Estilos aplicados según design system.
    - Tabla con scroll horizontal en pantallas pequeñas.
    - Filtros colapsables en pantallas pequeñas (opcional).
    - Validación de contraste WCAG AA.
---
#### QA / Testing
---
* **Título:** Pruebas de componentes de UI de auditoría
* **Objetivo:** Verificar el correcto funcionamiento de los componentes.
* **Tipo:** QA
* **Descripción corta:** Crear tests de componentes para `AuditTable`, `AuditFilters` y `Pagination`. Mockear datos y verificar renderizado correcto, interacciones y estados.
* **Entregables:**
    - Test: tabla renderiza columnas correctamente.
    - Test: filtros actualizan estado al cambiar valores.
    - Test: paginación deshabilita botones en límites.
    - Test: estado vacío se muestra cuando no hay datos.
---
* **Título:** Pruebas E2E de flujo de auditoría
* **Objetivo:** Verificar el flujo completo desde UI hasta backend.
* **Tipo:** QA
* **Descripción corta:** Crear test E2E que: navegue a auditoría como admin, aplique filtros, verifique resultados, navegue entre páginas. Verificar que usuarios no-admin no pueden acceder.
* **Entregables:**
    - Test E2E: admin accede y ve tabla de auditoría.
    - Test E2E: filtro por fechas muestra resultados filtrados.
    - Test E2E: paginación funciona correctamente.
    - Test E2E: usuario no-admin es redirigido/bloqueado.
---
* **Título:** Pruebas de accesibilidad de UI de auditoría
* **Objetivo:** Asegurar que la UI es accesible para todos los usuarios.
* **Tipo:** QA
* **Descripción corta:** Ejecutar auditoría de accesibilidad con herramientas automatizadas (axe, lighthouse). Verificar navegación por teclado. Validar que screen readers pueden interpretar la tabla.
* **Entregables:**
    - Reporte de axe/lighthouse con puntuación.
    - Verificación de navegación por teclado en tabla y filtros.
    - Labels ARIA apropiados en componentes interactivos.
