-- =============================================================================
-- SCRIPT: DB_ADMIN_005__rename_timestamps_to_spanish.sql
-- Propósito: Migrar columnas created_at/updated_at a convención española
-- Fecha: 9 de enero de 2026
-- Descripción: Alinea las columnas de timestamps con el resto del proyecto
--              que usa nomenclatura en español (fecha_*)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Renombrar columnas de timestamps
-- -----------------------------------------------------------------------------
ALTER TABLE usuarios 
    RENAME COLUMN created_at TO fecha_creacion;

ALTER TABLE usuarios 
    RENAME COLUMN updated_at TO fecha_actualizacion;

-- -----------------------------------------------------------------------------
-- 5. Verificación
-- -----------------------------------------------------------------------------
-- Verificar que las columnas fueron renombradas
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'usuarios' 
  AND column_name IN ('fecha_creacion', 'fecha_actualizacion')
ORDER BY column_name;

-- Verificar que el trigger está activo
SELECT trigger_name, event_manipulation, event_object_table
FROM information_schema.triggers
WHERE trigger_name = 'trigger_usuarios_fecha_actualizacion';

-- Verificar que la función existe
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_name = 'actualizar_fecha_actualizacion';

-- =============================================================================
-- NOTAS DE MIGRACIÓN:
-- 
-- - Este script debe ejecutarse en ambiente de desarrollo ANTES de desplegar
--   la nueva versión del código Java
-- - Las columnas mantienen sus constraints y defaults
-- - Los índices que referencian estas columnas se actualizan automáticamente
-- - NO afecta datos existentes, solo renombra columnas
-- - Tiempo de ejecución estimado: < 1 segundo (solo DDL)
-- 
-- ROLLBACK (si es necesario):
--   ALTER TABLE usuarios RENAME COLUMN fecha_creacion TO created_at;
--   ALTER TABLE usuarios RENAME COLUMN fecha_actualizacion TO updated_at;
--   -- Recrear trigger y función originales (ver DB_AUTH_1.sql)
-- =============================================================================
