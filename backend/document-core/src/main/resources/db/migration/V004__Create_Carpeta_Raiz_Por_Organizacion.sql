-- Migration: Initialize root folders for existing organizations
-- Description: Creates default root folder for each existing organization
-- Author: DocFlow Team
-- Date: 2026-01-28
-- Related US: US-FOLDER-001

-- ============================================================================
-- DATOS INICIALES: Carpeta raíz por organización
-- ============================================================================

-- Nota: Esta migración asume que la tabla 'organizaciones' existe.
-- Si no existe, esta migración no insertará datos pero no fallará.
-- 
-- Para nuevas organizaciones creadas después de esta migración,
-- la lógica de creación de carpeta raíz debe implementarse en
-- el servicio de administración de organizaciones (US-ADMIN-001).

-- Insertar carpeta raíz para cada organización existente
-- Se usa un usuario del sistema como creador (Long nulo o del sistema)
INSERT INTO carpetas (
    id,
    organizacion_id,
    carpeta_padre_id,
    nombre,
    descripcion,
    creado_por,
    fecha_creacion,
    fecha_actualizacion,
    fecha_eliminacion
)
SELECT 
    ( random() * 100 ) AS id,
    org.id AS organizacion_id,
    NULL AS carpeta_padre_id,  -- Carpeta raíz no tiene padre
    'Raíz' AS nombre,
    'Carpeta raíz de la organización' AS descripcion,
    ( random() * 10 ) AS creado_por,  -- Usuario del sistema
    CURRENT_TIMESTAMP AS fecha_creacion,
    CURRENT_TIMESTAMP AS fecha_actualizacion,
    NULL AS fecha_eliminacion
FROM (
    -- Seleccionar organizaciones que aún no tienen carpeta raíz
    -- Nota: Si la tabla organizaciones no existe, esta subconsulta retornará 0 filas
    SELECT id FROM organizaciones
    WHERE NOT EXISTS (
        SELECT 1 FROM carpetas 
        WHERE carpetas.organizacion_id = organizaciones.id 
        AND carpetas.carpeta_padre_id IS NULL
        AND carpetas.fecha_eliminacion IS NULL
    )
) AS org;

SELECT * FROM carpetas

-- ============================================================================
-- COMENTARIOS Y NOTAS
-- ============================================================================

-- IMPORTANTE: Para nuevas organizaciones creadas después de esta migración,
-- el servicio de administración debe crear automáticamente una carpeta raíz
-- al momento de crear la organización.
-- 
-- Implementar en el servicio OrganizacionService.crear() o similar:
-- 1. Crear organización
-- 2. Crear carpeta raíz para esa organización
-- 3. Emitir evento OrganizacionCreatedEvent

COMMENT ON TABLE carpetas IS 'Estructura jerárquica de carpetas. Cada organización debe tener exactamente una carpeta raíz (carpeta_padre_id IS NULL)';


SELECT 
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default
FROM 
    information_schema.columns
WHERE 
    table_name = 'carpetas'
ORDER BY 
    ordinal_position;