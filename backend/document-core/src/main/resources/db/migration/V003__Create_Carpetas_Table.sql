-- Migration: Create carpetas table for hierarchical folder structure
-- Description: Creates the folders table with soft delete support and multi-tenant isolation
-- Author: DocFlow Team
-- Date: 2026-01-28
-- Related US: US-FOLDER-001

-- ============================================================================
-- TABLE: carpetas (Folders)
-- ============================================================================
CREATE TABLE IF NOT EXISTS carpetas (
    id INT PRIMARY KEY DEFAULT gen_random_INT(),
    organizacion_id INT NOT NULL,
    carpeta_padre_id INT,
    nombre VARCHAR(255) NOT NULL,
    descripcion VARCHAR(500),
    creado_por INT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    
    -- Foreign key para la carpeta padre (auto-referencia)
    CONSTRAINT fk_carpeta_padre FOREIGN KEY (carpeta_padre_id) 
        REFERENCES carpetas(id) 
        ON DELETE SET NULL,
    
    -- Restricción: la carpeta padre debe pertenecer a la misma organización
    CONSTRAINT ck_org_padre_org CHECK (
        carpeta_padre_id IS NULL OR organizacion_id IS NOT NULL
    )
);

-- ============================================================================
-- INDEXES: Optimización de consultas
-- ============================================================================

-- Índice compuesto para consultas por organización y padre
CREATE INDEX idx_carpetas_org_padre 
    ON carpetas(organizacion_id, carpeta_padre_id) 
    WHERE fecha_eliminacion IS NULL;

-- Índice para búsqueda por nombre dentro de una organización
CREATE INDEX idx_carpetas_org_nombre 
    ON carpetas(organizacion_id, nombre) 
    WHERE fecha_eliminacion IS NULL;

-- Índice único parcial para garantizar nombres únicos por nivel
-- (COALESCE maneja el caso de carpetas raíz donde carpeta_padre_id es NULL)
CREATE UNIQUE INDEX idx_carpetas_unique_nombre_por_nivel 
    ON carpetas(organizacion_id, COALESCE(carpeta_padre_id::TEXT, 'ROOT'), LOWER(nombre)) 
    WHERE fecha_eliminacion IS NULL;

-- Índice para consultas de eliminación lógica
CREATE INDEX idx_carpetas_fecha_eliminacion 
    ON carpetas(fecha_eliminacion) 
    WHERE fecha_eliminacion IS NOT NULL;

-- Índice para consultas por creador
CREATE INDEX idx_carpetas_creado_por 
    ON carpetas(creado_por);

-- ============================================================================
-- COMMENTS: Documentación de esquema
-- ============================================================================
COMMENT ON TABLE carpetas IS 'Estructura jerárquica de carpetas para organización de documentos con soft delete';
COMMENT ON COLUMN carpetas.id IS 'Identificador único de la carpeta';
COMMENT ON COLUMN carpetas.organizacion_id IS 'Organización propietaria (multi-tenancy)';
COMMENT ON COLUMN carpetas.carpeta_padre_id IS 'Referencia a carpeta padre (NULL para carpeta raíz)';
COMMENT ON COLUMN carpetas.nombre IS 'Nombre de la carpeta (único por nivel dentro de la organización)';
COMMENT ON COLUMN carpetas.descripcion IS 'Descripción opcional de la carpeta';
COMMENT ON COLUMN carpetas.creado_por IS 'Usuario que creó la carpeta';
COMMENT ON COLUMN carpetas.fecha_creacion IS 'Timestamp de creación';
COMMENT ON COLUMN carpetas.fecha_actualizacion IS 'Timestamp de última actualización';
COMMENT ON COLUMN carpetas.fecha_eliminacion IS 'Timestamp de eliminación lógica (NULL = activa)';
