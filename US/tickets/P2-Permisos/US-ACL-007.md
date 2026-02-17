## P2 — Permisos Granulares (ACL por carpeta/documento)

**[US-ACL-009] UI muestra capacidades (acciones habilitadas) por carpeta/documento**

### 1) Descripción funcional
Como usuario autenticado, quiero que la UI refleje mis capacidades por carpeta y documento, habilitando o deshabilitando acciones según permisos, para evitar errores y confusión.

**Alcance:** Frontend (UI/UX) con consumo de endpoints de capacidades ya definidos en backend. No requiere cambios de BD.

### 2) Reglas de negocio / capacidades
- **Carpeta:** `puede_leer`, `puede_escribir`, `puede_administrar`.
- **Documento:** `puede_leer`, `puede_descargar`, `puede_escribir`, `puede_administrar`.
- **Precedencia documento > carpeta** cuando ambos existen (backend ya lo resuelve).
- La UI **no debe habilitar** acciones si la capacidad es `false`.
- Decisión de UX: **acciones visibles pero deshabilitadas con tooltip** explicativo.

### 3) Contratos/Endpoints a consumir
> Usar lo disponible en backend. Si ambos existen, priorizar los que eviten N+1.

- **Listado de contenido:** `GET /carpetas/{id}/contenido`
    - Cada elemento debe incluir `capacidades` (si no está, fallback a endpoint específico).
    - Ejemplo por elemento:
        ```json
        {
            "id": 123,
            "tipo": "DOCUMENTO",
            "nombre": "Contrato.pdf",
            "capacidades": {
                "puede_leer": true,
                "puede_descargar": true,
                "puede_escribir": false,
                "puede_administrar": false
            }
        }
        ```

- **Capacidades de carpeta:** `GET /carpetas/{id}/capacidades`
    - Respuesta:
        ```json
        { "puede_leer": true, "puede_escribir": false, "puede_administrar": false }
        ```

- **Capacidades de documento:** `GET /documentos/{id}/capacidades`
    - Respuesta:
        ```json
        { "puede_leer": true, "puede_descargar": true, "puede_escribir": false, "puede_administrar": false }
        ```

### 4) UI/UX requerida
- **Toolbar de carpeta (contextual):**
    - “Subir” y “Nueva carpeta” requieren `puede_escribir`.
    - “Administrar permisos” requiere `puede_administrar`.
    - En estado `false`, botón deshabilitado + tooltip: “Requiere permiso de escritura/administración”.

- **Menú contextual de carpeta/documento:**
    - “Abrir” requiere `puede_leer`.
    - “Descargar” requiere `puede_descargar` (solo documento).
    - “Editar”, “Mover” requieren `puede_escribir`.
    - “Permisos” requiere `puede_administrar`.
    - Opción visible pero deshabilitada con tooltip si no aplica.

- **Indicador visual en listado:**
    - Ícono ojo = lectura, lápiz = escritura, engranaje = administración.
    - Tooltip con texto: “Solo lectura”, “Lectura + escritura”, “Administración”.

- **Estado de carga:**
    - Mientras se cargan capacidades, mostrar skeleton/spinner y **no** habilitar acciones por defecto.

- **Mensaje informativo:**
    - Si el usuario solo tiene lectura en carpeta actual, mostrar banner: “Sus permisos solo permiten visualizar contenido. Contacte al administrador para solicitar más acceso.”

### 5) Comportamiento de cache y navegación
- Cache por `carpetaId` y `documentoId`.
- Al cambiar de carpeta, invalidar y reconsultar capacidades.
- Preferir capacidades embebidas en `GET /carpetas/{id}/contenido` para evitar N+1.

### 6) Archivos a modificar/crear (frontend)
- **Tipos:**
    - Crear/actualizar tipos en [frontend/src/features/acl/types/index.ts](frontend/src/features/acl/types/index.ts).
- **Servicios API:**
    - Crear servicio de capacidades en [frontend/src/features/acl/services/capacidadesService.ts](frontend/src/features/acl/services/capacidadesService.ts).
- **Hooks:**
    - Crear `useCapacidadesCarpeta` en [frontend/src/features/acl/hooks/useCapacidadesCarpeta.ts](frontend/src/features/acl/hooks/useCapacidadesCarpeta.ts).
    - Crear `useCapacidadesDocumento` en [frontend/src/features/acl/hooks/useCapacidadesDocumento.ts](frontend/src/features/acl/hooks/useCapacidadesDocumento.ts).
- **Componentes UI:**
    - Crear `ToolbarCarpeta` en [frontend/src/features/acl/components/ToolbarCarpeta.tsx](frontend/src/features/acl/components/ToolbarCarpeta.tsx).
    - Crear `AccesoBadge` (indicador visual) en [frontend/src/features/acl/components/AccesoBadge.tsx](frontend/src/features/acl/components/AccesoBadge.tsx).
    - Integrar en las vistas de navegación/listado de documentos/carpeta (ruta `/documents`).

### 7) Requisitos no funcionales
- **Seguridad:** la UI no sustituye controles backend; solo refleja capacidades.
- **Performance:** evitar N+1; usar listado con capacidades embebidas.
- **Accesibilidad:** usar `aria-disabled`, `title`/tooltip accesible y foco visible.

### 8) Criterios de aceptación (extendidos)
1. **Lectura:** Dado usuario con `LECTURA`, cuando navega a carpeta, entonces “Subir” y “Administrar permisos” están deshabilitados y muestran tooltip correspondiente.
2. **Escritura:** Dado usuario con `ESCRITURA`, cuando navega a carpeta, entonces “Subir” y “Nueva carpeta” están habilitados; “Administrar permisos” deshabilitado.
3. **Admin:** Dado usuario con `ADMINISTRACION`, todas las acciones están habilitadas.
4. **Documento:** Dado un documento con `puede_descargar=false`, la opción “Descargar” aparece deshabilitada.
5. **Carga:** Mientras se cargan capacidades, la UI no habilita acciones y muestra estado de carga.

### 9) Definition of Done
- Implementación en UI con hooks y servicios.
- Tooltips y estados de carga visibles.
- Indicador de nivel de acceso en listado.
- Documentación mínima en README del frontend si se agrega nueva ruta de API.
