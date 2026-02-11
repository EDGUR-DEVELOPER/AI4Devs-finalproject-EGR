-- Migration: Create permiso_documento_usuario table for explicit document permissions
-- Description: Creates ACL table for user-document permissions with tenant isolation
-- Author: DocFlow Team
-- Date: 2026-02-11
-- Related US: US-ACL-005

CREATE TABLE IF NOT EXISTS permisos_documento_usuario (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    organizacion_id BIGINT NOT NULL,
    nivel_acceso VARCHAR(20) NOT NULL,
    fecha_expiracion TIMESTAMPTZ NULL,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_permiso_documento_usuario_documento
        FOREIGN KEY (documento_id) REFERENCES documento(id) ON DELETE CASCADE,

    CONSTRAINT uq_permiso_documento_usuario
        UNIQUE (documento_id, usuario_id)
);

-- Indexes for query performance
CREATE INDEX idx_permiso_documento_id
    ON permisos_documento_usuario(documento_id);

CREATE INDEX idx_permiso_usuario_id
    ON permisos_documento_usuario(usuario_id);

CREATE INDEX idx_permiso_organizacion_id
    ON permisos_documento_usuario(organizacion_id);

-- Comments
COMMENT ON TABLE permisos_documento_usuario IS 'ACL de permisos explícitos de usuarios sobre documentos';
COMMENT ON COLUMN permisos_documento_usuario.nivel_acceso IS 'Nivel de acceso (LECTURA, ESCRITURA, ADMINISTRACION)';
COMMENT ON COLUMN permisos_documento_usuario.fecha_expiracion IS 'Fecha opcional de expiración para permisos temporales';
