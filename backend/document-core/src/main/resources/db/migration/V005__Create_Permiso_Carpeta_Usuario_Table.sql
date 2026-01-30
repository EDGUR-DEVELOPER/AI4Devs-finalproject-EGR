-- Migration: Create permiso_carpeta_usuario table for explicit folder permissions
-- Description: Creates ACL table for user-folder permissions with tenant isolation
-- Author: DocFlow Team
-- Date: 2026-01-29
-- Related US: US-ACL-002

CREATE TABLE IF NOT EXISTS permiso_carpeta_usuario (
    id BIGSERIAL PRIMARY KEY,
    carpeta_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    organizacion_id BIGINT NOT NULL,
    nivel_acceso VARCHAR(20) NOT NULL,
    recursivo BOOLEAN NOT NULL DEFAULT false,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_permiso_carpeta_usuario_carpeta
        FOREIGN KEY (carpeta_id) REFERENCES carpetas(id) ON DELETE CASCADE,

    CONSTRAINT uq_permiso_carpeta_usuario
        UNIQUE (carpeta_id, usuario_id)
);

-- Indexes for query performance
CREATE INDEX idx_permiso_carpeta_usuario_carpeta
    ON permiso_carpeta_usuario(carpeta_id);

CREATE INDEX idx_permiso_carpeta_usuario_usuario
    ON permiso_carpeta_usuario(usuario_id);

CREATE INDEX idx_permiso_carpeta_usuario_org
    ON permiso_carpeta_usuario(organizacion_id);

CREATE INDEX idx_permiso_carpeta_usuario_carpeta_recursivo
    ON permiso_carpeta_usuario(carpeta_id, recursivo);

-- Comments
COMMENT ON TABLE permiso_carpeta_usuario IS 'ACL de permisos explícitos de usuarios sobre carpetas';
COMMENT ON COLUMN permiso_carpeta_usuario.nivel_acceso IS 'Nivel de acceso (LECTURA, ESCRITURA, ADMINISTRACION)';
