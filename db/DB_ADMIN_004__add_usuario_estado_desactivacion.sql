-- Migración V004: Agregar campos estado y fecha_desactivacion a tabla usuarios
-- Objetivo: Permitir desactivación de usuarios sin eliminación física
-- Autor: Sistema
-- Fecha: 2026-01-09

-- 1. Agregar campo 'estado' a tabla 'usuarios'
ALTER TABLE usuarios 
ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
CHECK (estado IN ('ACTIVO', 'INACTIVO'));

-- 2. Agregar campo 'fecha_desactivacion' para auditoría
ALTER TABLE usuarios 
ADD COLUMN fecha_desactivacion TIMESTAMPTZ DEFAULT NULL;

-- 3. Índice para optimizar queries de usuarios activos
CREATE INDEX idx_usuarios_estado_activo 
ON usuarios(estado) 
WHERE estado = 'ACTIVO';

-- 4. Agregar valor 'INACTIVO' al CHECK constraint de usuarios_organizaciones
ALTER TABLE usuarios_organizaciones 
DROP CONSTRAINT usuarios_organizaciones_estado_check;

ALTER TABLE usuarios_organizaciones 
ADD CONSTRAINT usuarios_organizaciones_estado_check 
CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'INACTIVO'));

-- 5. Datos de prueba: Inicializar estados existentes
UPDATE usuarios SET estado = 'ACTIVO' WHERE fecha_eliminacion IS NULL;
UPDATE usuarios SET estado = 'INACTIVO' WHERE fecha_eliminacion IS NOT NULL;

-- 6. Comentarios de documentación
COMMENT ON COLUMN usuarios.estado IS 'Estado del usuario: ACTIVO (puede autenticarse), INACTIVO (desactivado, tokens invalidados)';
COMMENT ON COLUMN usuarios.fecha_desactivacion IS 'Timestamp UTC de cuándo se desactivó el usuario para auditoría';
