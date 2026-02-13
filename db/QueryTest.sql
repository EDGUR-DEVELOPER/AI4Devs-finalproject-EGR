-- Insertar organización de prueba
select * from organizaciones;

INSERT INTO organizaciones (id, fecha_creacion, nombre, estado, configuracion) VALUES
    (1, NOW(), 'Acme Corp', 'ACTIVO', '{}'::jsonb),
    (2, NOW(), 'Contoso Ltd', 'ACTIVO', '{}'::jsonb),
    (3, NOW(), 'Fabrikam Inc', 'ACTIVO', '{}'::jsonb),
    (4, NOW(), 'Org Suspendida', 'SUSPENDIDO', '{}'::jsonb);

-- Insertar usuario de prueba
-- El password_hash corresponde a la contraseña 'password' en BCrypt

select * from usuarios;

INSERT INTO usuarios (id, email, estado, fecha_creacion, fecha_actualizacion, hash_contrasena, nombre_completo, mfa_habilitado) VALUES
    (1, 'sin-org@test.com', 'ACTIVO', NOW(), NOW(), '$2a$12$cTI2XVipd7yGwltD5D7sVuXshdGYOXWLooUOri6/MApXGMC/t0y5S', 'Usuario Sin Org', false),
    (2, 'una-org@test.com', 'ACTIVO', NOW(), NOW(), '$2a$12$cTI2XVipd7yGwltD5D7sVuXshdGYOXWLooUOri6/MApXGMC/t0y5S', 'Usuario Con Una Org', false),
    (3, 'dos-org-ok@test.com', 'ACTIVO', NOW(), NOW(), '$2a$12$cTI2XVipd7yGwltD5D7sVuXshdGYOXWLooUOri6/MApXGMC/t0y5S', 'Usuario Con Dos Orgs OK', false),
    (4, 'dos-org-sin-default@test.com', 'ACTIVO', NOW(), NOW(), '$2a$12$cTI2XVipd7yGwltD5D7sVuXshdGYOXWLooUOri6/MApXGMC/t0y5S', 'Usuario Con Dos Orgs Sin Default', false),
    (5, 'tres-org@test.com', 'ACTIVO', NOW(), NOW(), '$2a$12$cTI2XVipd7yGwltD5D7sVuXshdGYOXWLooUOri6/MApXGMC/t0y5S', 'Usuario Con Tres Orgs', false);

-- Escenario 2: Usuario con 1 organización activa (200 - auto-seleccionada)
INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion) VALUES
    (2, 1, 'ACTIVO', false, NOW());

-- Escenario 3: Usuario con 2 organizaciones activas CON predeterminada (200)
INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion) VALUES
    (3, 1, 'ACTIVO', TRUE, NOW()),
    (3, 2, 'ACTIVO', FALSE, NOW());

-- Escenario 4: Usuario con 2 organizaciones activas SIN predeterminada (409)
INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion) VALUES
    (4, 1, 'ACTIVO', FALSE, NOW()),
    (4, 2, 'ACTIVO', FALSE, NOW());

-- Escenario 5: Usuario con >2 organizaciones activas (409 - limitación MVP)
INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion) VALUES
    (5, 1, 'ACTIVO', TRUE, NOW()),
    (5, 2, 'ACTIVO', FALSE, NOW()),
    (5, 3, 'ACTIVO', FALSE, NOW());

-- Roles globales (aplicables a todas las organizaciones)
INSERT INTO roles (id, codigo, nombre, descripcion, organizacion_id, activo, fecha_creacion) VALUES
    (1, 'SUPER_ADMIN', 'Super Administrador', 'Acceso total al sistema, gestión de organizaciones', null, true, NOW()),
    (2, 'ADMIN', 'Administrador', 'Administrador de organización con permisos completos', NULL, true, NOW()),
    (3, 'USER', 'Usuario Estándar', 'Usuario con permisos de lectura/escritura en documentos', 0, true, NOW()),
    (4, 'VIEWER', 'Visor', 'Usuario con permisos de solo lectura', 0, true, NOW());

-- Roles específicos de ejemplo para Acme Corp (organizacion_id=1)
INSERT INTO roles (id, codigo, nombre, descripcion, organizacion_id, activo, fecha_creacion ) VALUES
    (5, 'AUDITOR', 'Auditor Interno', 'Acceso de lectura a logs de auditoría', 1, true, NOW()),
    (6, 'CONTRIBUTOR', 'Colaborador Externo', 'Permisos limitados para usuarios externos', 1, true, NOW());

-- Reset sequence
SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));
SELECT setval('usuarios_id_seq', (SELECT MAX(id) FROM usuarios));

-- =============================================================================
-- DATOS SEMILLA: Asignaciones de Roles para Testing
-- =============================================================================

-- Usuario con 1 org (usuario_id=2, organizacion_id=1) → roles: ADMIN + USER
INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, asignado_por, fecha_asignacion) VALUES
    (2, 2, 1, true, NULL, now()), -- ADMIN
    (2, 3, 1, true, NULL, now()); -- USER

-- Usuario con 2 orgs OK (usuario_id=3)
--   → En Acme Corp (org 1): ADMIN
--   → En Contoso Ltd (org 2): VIEWER
INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, asignado_por, fecha_asignacion) VALUES
    (3, 2, 1, true, NULL, now()), -- ADMIN en Acme
    (3, 4, 2, true, NULL, now()); -- VIEWER en Contoso

-- Usuario con 2 orgs sin default (usuario_id=4)
--   → En Acme Corp (org 1): USER
--   → En Contoso Ltd (org 2): USER
INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, asignado_por, fecha_asignacion) VALUES
    (5, 3, 1, true, NULL, now()), -- USER en Acme
    (5, 3, 2, true, NULL, now()); -- USER en Contoso
select * from usuarios_organizaciones;

SELECT * FROM roles

-- ============================================================================
-- SCRIPT: Insertar carpetas raíz para todas las organizaciones
-- Propósito: Crear la carpeta raíz de cada organización (carpeta_padre_id = NULL)
-- Seguridad: Solo inserta si la organización no tiene carpeta raíz ya
-- ============================================================================

-- 1. VERIFICAR ORGANIZACIONES Y SUS CARPETAS RAÍZ
SELECT 
    org.id,
    org.nombre,
    COALESCE(c.id, 0) AS carpeta_raiz_id
FROM organizaciones org
LEFT JOIN carpetas c ON c.organizacion_id = org.id 
    AND c.carpeta_padre_id IS NULL 
    AND c.fecha_eliminacion IS NULL
WHERE org.estado = 'ACTIVO'
ORDER BY org.id;

-- 2. INSERTAR CARPETAS RAÍZ (solo para organizaciones sin carpeta raíz)
INSERT INTO carpetas (
    id,
    organizacion_id,
    carpeta_padre_id,
    nombre,
    descripcion,
    creado_por,
    fecha_creacion,
    fecha_actualizacion,
    fecha_eliminacion
)
SELECT    
    (random() * 2147483647)::BIGINT AS id,
    org.id AS organizacion_id,
    NULL AS carpeta_padre_id,
    'Documentos' AS nombre,
    CONCAT('Carpeta raíz de ', org.nombre) AS descripcion,
    COALESCE(
        (SELECT id FROM usuarios WHERE email = 'admin.user@acme.com' LIMIT 1),
        1
    ) AS creado_por,
    NOW() AS fecha_creacion,
    NOW() AS fecha_actualizacion,
    NULL AS fecha_eliminacion
FROM organizaciones org
WHERE org.estado = 'ACTIVO'
    AND NOT EXISTS (
        SELECT 1 FROM carpetas c
        WHERE c.organizacion_id = org.id
            AND c.carpeta_padre_id IS NULL
            AND c.fecha_eliminacion IS NULL
    )
ON CONFLICT DO NOTHING;

-- 3. VERIFICAR RESULTADO
SELECT 
    org.nombre AS organizacion,
    COUNT(c.id) AS total_carpetas,
    SUM(CASE WHEN c.carpeta_padre_id IS NULL THEN 1 ELSE 0 END) AS carpetas_raiz
FROM organizaciones org
LEFT JOIN carpetas c ON c.organizacion_id = org.id 
    AND c.fecha_eliminacion IS NULL
WHERE org.estado = 'ACTIVO'
GROUP BY org.id, org.nombre
ORDER BY org.id;

SELECT * FROM carpetas

-- ============================================================================
-- Query: Insertar nivel de acceso ADMINISTRACION para usuario una-org@test.com
-- Carpeta: RAIZ (carpeta_padre_id IS NULL)
-- Tipo: Permiso a nivel de usuario con herencia recursiva
-- ============================================================================

BEGIN;

-- Paso 1: Obtener el ID del usuario y su organización
SET search_path TO public;

WITH user_data AS (
    SELECT 
        u.id AS usuario_id,
        u.email,
        uo.organizacion_id
    FROM usuarios u
    JOIN usuarios_organizaciones uo ON u.id = uo.usuario_id
    WHERE u.email = 'una-org@test.com'
        AND uo.estado = 'ACTIVO'
    LIMIT 1
),

-- Paso 2: Obtener la carpeta RAIZ de la organización del usuario
carpeta_raiz AS (
    SELECT 
        c.id,
        c.organizacion_id,
        c.nombre
    FROM carpetas c
    WHERE c.organizacion_id = (SELECT organizacion_id FROM user_data)
        AND c.carpeta_padre_id IS NULL  -- RAIZ
        AND c.fecha_eliminacion IS NULL
    LIMIT 1
)

-- Paso 3: Insertar el permiso
INSERT INTO permiso_carpeta_usuario (
    carpeta_id,
    usuario_id,
    organizacion_id,
    nivel_acceso,
    recursivo,
    fecha_asignacion
)

SELECT 
    cr.id,
    ud.usuario_id,
    ud.organizacion_id,
    'ADMINISTRACION'::varchar,
    true,  -- Recursivo: heredará a todas las subcarpetas
    NOW()
FROM carpeta cr, user_data ud
WHERE NOT EXISTS (
    -- Evitar duplicados
    SELECT 1 FROM permiso_carpeta_usuario pcu
    WHERE pcu.carpeta_id = cr.id
        AND pcu.usuario_id = ud.usuario_id
)

SELECT u.id, u.email, o.id AS idOrganizacion, c.id AS idCarpeta
FROM usuarios u
JOIN usuarios_organizaciones uo
    ON u.id = uo.usuario_id
JOIN organizaciones o
    ON uo.organizacion_id = o.id
JOIN carpetas c
    ON c.organizacion_id = o.id


SELECT * FROM permiso_carpeta_usuario

INSERT INTO permiso_carpeta_usuario
VALUES (1, 1202497455, NOW(), 'ADMINISTRACION', 1, TRUE, 2)

-- Paso 4: Verificar la inserción
SELECT 
    pcu.id,
    pcu.organizacion_id,
    c.nombre AS carpeta,
    u.email,
    pcu.nivel_acceso,
    pcu.recursivo,
    pcu.fecha_asignacion
FROM permiso_carpeta_usuario pcu
JOIN carpetas c ON pcu.carpeta_id = c.id
JOIN usuarios u ON pcu.usuario_id = u.id
WHERE u.email = 'una-org@test.com'
    AND c.carpeta_padre_id IS NULL
ORDER BY pcu.fecha_asignacion DESC
LIMIT 1;

--COMMIT;
ROLLBACK