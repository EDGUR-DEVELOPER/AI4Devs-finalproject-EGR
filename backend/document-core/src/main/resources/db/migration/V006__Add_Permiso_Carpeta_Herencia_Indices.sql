-- Migration: Add indices for recursive folder permission inheritance
-- Related US: US-ACL-004

-- Index for inheritance lookups (recursive permissions only)
CREATE INDEX IF NOT EXISTS idx_permiso_carpeta_usuario_herencia
    ON permiso_carpeta_usuario(usuario_id, recursivo, organizacion_id)
    WHERE recursivo = true;

-- Index for efficient parent navigation on folders
CREATE INDEX IF NOT EXISTS idx_carpetas_padre_org
    ON carpetas(carpeta_padre_id, organizacion_id)
    WHERE fecha_eliminacion IS NULL;

-- Composite index for permission lookups by folder and user
CREATE INDEX IF NOT EXISTS idx_permiso_carpeta_usuario_carpeta_usuario_org
    ON permiso_carpeta_usuario(carpeta_id, usuario_id, organizacion_id);

-- Prevent self-referencing folders
ALTER TABLE carpetas
    ADD CONSTRAINT chk_carpetas_no_self_parent
    CHECK (id <> carpeta_padre_id);
