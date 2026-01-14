-- =============================================================================
-- FIX: Resincronizar secuencias después de inserciones manuales
-- Propósito: Corregir DataIntegrityViolationException por duplicate key
-- =============================================================================
-- Este script resincroniza las secuencias con los valores máximos existentes
-- en las tablas después de inserciones manuales en los scripts de inicialización.
--
-- Error original:
-- ERROR: duplicate key value violates unique constraint "usuarios_pkey"
-- Detail: Key (id)=(1) already exists.
--
-- Causa: Los scripts de inicialización insertan usuarios con IDs explícitos (1,2,3...),
-- pero la secuencia no se actualiza. Cuando Hibernate intenta crear un nuevo usuario,
-- la secuencia retorna 1, causando un conflicto de clave primaria.
-- =============================================================================

-- 1. Resincronizar secuencia de usuarios
-- Establece el siguiente valor de la secuencia al máximo ID + 1
SELECT setval(
    pg_get_serial_sequence('usuarios', 'id'),
    COALESCE(MAX(id), 0) + 1
) FROM usuarios;

-- 2. Resincronizar secuencia de organizaciones
SELECT setval(
    pg_get_serial_sequence('organizaciones', 'id'),
    COALESCE(MAX(id), 0) + 1
) FROM organizaciones;

-- 3. Resincronizar secuencia de roles
SELECT setval(
    pg_get_serial_sequence('roles', 'id'),
    COALESCE(MAX(id), 0) + 1
) FROM roles;

-- 4. Resincronizar secuencia de usuarios_roles (si existe tabla)
SELECT setval(
    pg_get_serial_sequence('usuarios_roles', 'id'),
    COALESCE(MAX(id), 0) + 1
) FROM usuarios_roles WHERE id IS NOT NULL;

-- Verificar estado de las secuencias
SELECT 
    schemaname,
    sequencename,
    last_value,
    is_called
FROM pg_sequences
WHERE sequencename IN (
    'usuarios_id_seq',
    'organizaciones_id_seq',
    'roles_id_seq',
    'usuarios_roles_id_seq'
)
ORDER BY sequencename;
