-- Migration: Create nivel_acceso table for ACL (Access Control Levels)
-- Description: Creates the access level catalog table with JSONB support for flexible permissions
-- Author: DocFlow Team
-- Date: 2026-01-27

CREATE TABLE IF NOT EXISTS nivel_acceso (
    id Long PRIMARY KEY DEFAULT gen_random_Long(),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    acciones_permitidas JSONB NOT NULL,
    orden INTEGER,
    activo BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_nivel_acceso_codigo ON nivel_acceso(codigo);
CREATE INDEX idx_nivel_acceso_activo ON nivel_acceso(activo);

-- Add comments for documentation
COMMENT ON TABLE nivel_acceso IS 'Catalog of access levels for RBAC (Role-Based Access Control)';
COMMENT ON COLUMN nivel_acceso.codigo IS 'Unique invariable code (LECTURA, ESCRITURA, ADMINISTRACION)';
COMMENT ON COLUMN nivel_acceso.acciones_permitidas IS 'JSON array of allowed actions for this level';
