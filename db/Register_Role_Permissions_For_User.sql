-- Script: Register existing folders with admin role permissions for una-org@test.com user
-- Purpose: Enable role-based folder permissions for testing
-- Table: permiso_carpeta_rol (newly created via V013 migration)
-- User: una-org@test.com

BEGIN;

-- Step 1: Find the user ID for una-org@test.com
-- SELECT id, email, organizacion_id FROM usuarios WHERE email = 'una-org@test.com';

-- Step 2: Find the user's roles in their organization
-- SELECT ur.* FROM usuarios_roles ur 
-- JOIN usuarios u ON ur.usuario_id = u.id 
-- WHERE u.email = 'una-org@test.com';

-- Step 3: Insert admin permissions for all folders of user's organization
-- This query registers all existing folders with ADMINISTRACION level permissions for user's roles

INSERT INTO permiso_carpeta_rol (carpeta_id, rol_id, organizacion_id, nivel_acceso, recursivo, fecha_asignacion)
SELECT DISTINCT
    c.id,
    ur.rol_id,
    c.organizacion_id,
    'ADMINISTRACION'::varchar,
    true,  -- Recursive permissions
    NOW()
FROM 
    carpetas c
    CROSS JOIN usuarios u
    CROSS JOIN usuarios_roles ur
WHERE 
    u.email = 'una-org@test.com'
    AND c.organizacion_id = u.organizacion_id
    AND ur.usuario_id = u.id
    AND ur.estado = true  -- Only active roles
    AND NOT EXISTS (
        -- Avoid duplicates: only insert if permission doesn't already exist
        SELECT 1 FROM permiso_carpeta_rol pcr
        WHERE pcr.carpeta_id = c.id
        AND pcr.rol_id = ur.rol_id
        AND pcr.organizacion_id = c.organizacion_id
    )
ON CONFLICT (carpeta_id, rol_id) DO NOTHING;

-- Step 4: Verify the insertions
SELECT 
    pcr.id,
    c.nombre as carpeta,
    r.nombre as rol,
    u.email,
    pcr.nivel_acceso,
    pcr.recursivo,
    pcr.fecha_asignacion
FROM 
    permiso_carpeta_rol pcr
    JOIN carpetas c ON pcr.carpeta_id = c.id
    JOIN roles r ON pcr.rol_id = r.id
    JOIN usuarios u ON pcr.organizacion_id = u.organizacion_id
WHERE 
    u.email = 'una-org@test.com'
    AND pcr.organizacion_id = u.organizacion_id
ORDER BY c.nombre, r.nombre;

COMMIT;
