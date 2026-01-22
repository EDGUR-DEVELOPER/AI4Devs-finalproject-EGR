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