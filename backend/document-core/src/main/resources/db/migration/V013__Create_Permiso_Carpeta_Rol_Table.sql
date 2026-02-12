-- Migration: Create permiso_carpeta_rol table for role-based folder permissions
-- Description: Creates ACL table for role-folder permissions with tenant isolation
-- Author: DocFlow Team
-- Date: 2026-02-01
-- Related US: US-ACL-003, US-ACL-004

CREATE TABLE IF NOT EXISTS permiso_carpeta_rol (
    id BIGSERIAL PRIMARY KEY,
    carpeta_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    organizacion_id BIGINT NOT NULL,
    nivel_acceso VARCHAR(20) NOT NULL,
    recursivo BOOLEAN NOT NULL DEFAULT false,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_permiso_carpeta_rol_carpeta
        FOREIGN KEY (carpeta_id) REFERENCES carpetas(id) ON DELETE CASCADE,

    CONSTRAINT uq_permiso_carpeta_rol
        UNIQUE (carpeta_id, rol_id)
);

-- Indexes for query performance
CREATE INDEX idx_permiso_carpeta_rol_carpeta
    ON permiso_carpeta_rol(carpeta_id);

CREATE INDEX idx_permiso_carpeta_rol_rol
    ON permiso_carpeta_rol(rol_id);

CREATE INDEX idx_permiso_carpeta_rol_org
    ON permiso_carpeta_rol(organizacion_id);

CREATE INDEX idx_permiso_carpeta_rol_carpeta_recursivo
    ON permiso_carpeta_rol(carpeta_id, recursivo);

-- Comments
COMMENT ON TABLE permiso_carpeta_rol IS 'ACL de permisos explícitos de roles sobre carpetas';
COMMENT ON COLUMN permiso_carpeta_rol.nivel_acceso IS 'Nivel de acceso (LECTURA, ESCRITURA, ADMINISTRACION)';
COMMENT ON COLUMN permiso_carpeta_rol.recursivo IS 'Si true, el permiso se hereda a subcarpetas';
