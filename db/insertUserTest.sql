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

INSERT INTO usuarios (id, email, fecha_creacion, fecha_actualizacion, hash_contrasena, nombre_completo, mfa_habilitado) VALUES
    (1, 'sin-org@test.com', NOW(), NOW(), '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Usuario Sin Org', false),
    (2, 'una-org@test.com', NOW(), NOW(), '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Usuario Con Una Org', false),
    (3, 'dos-org-ok@test.com', NOW(), NOW(), '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Usuario Con Dos Orgs OK', false),
    (4, 'dos-org-sin-default@test.com', NOW(), NOW(), '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Usuario Con Dos Orgs Sin Default', false),
    (5, 'tres-org@test.com', NOW(), NOW(), '$2a$10$7QJ8QwQwQwQwQwQwQwQwQeQwQwQwQwQwQwQwQwQwQwQwQwQwQwQw', 'Usuario Con Tres Orgs', false);

update usuarios
set hash_contrasena  = '$2a$12$cTI2XVipd7yGwltD5D7sVuXshdGYOXWLooUOri6/MApXGMC/t0y5S'
where id = 1

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

select * from usuarios_organizaciones;