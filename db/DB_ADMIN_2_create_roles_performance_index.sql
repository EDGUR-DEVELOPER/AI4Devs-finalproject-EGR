-- ================================================================
-- Migración: V003__create_roles_performance_index.sql
-- Descripción: Índice compuesto para optimizar búsquedas de roles
--              por organización en asignaciones de roles (US-ADMIN-002)
-- Fecha: 2026-01-09
-- ================================================================

-- Índice compuesto para búsquedas eficientes de roles activos por organización.
-- Beneficia queries que filtran roles por organizacion_id y validan estado activo.
-- El índice parcial (WHERE activo = TRUE) reduce el tamaño y mejora el rendimiento.
CREATE INDEX IF NOT EXISTS idx_roles_org_activo_lookup 
ON roles(organizacion_id, activo, id) 
WHERE activo = TRUE;