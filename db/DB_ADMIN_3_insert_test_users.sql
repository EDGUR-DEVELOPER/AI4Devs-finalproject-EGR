-- =============================================================================
-- SCRIPT: DB_ADMIN_3_insert_test_users.sql
-- Propósito: Datos semilla para pruebas de US-ADMIN-003 (Listar usuarios con roles)
-- Orden de ejecución: Ejecutar DESPUÉS de DB_AUTH_3.sql en orden secuencial
-- =============================================================================

-- -----------------------------------------------------------------------------
-- USUARIOS PARA ORGANIZACIÓN 1 (Acme Corp)
-- Password para todos: "password123" (BCrypt con cost 12)
-- -----------------------------------------------------------------------------

-- Usuario 1: ADMIN + OPERATOR (múltiples roles activos)
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo, mfa_habilitado, fecha_eliminacion, created_at, updated_at) 
VALUES (101, 'admin.user@acme.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TuPcGmy30x9SToB5xLqf7gF8F3ni', 'Admin User', false, NULL, NOW(), NOW());

INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion)
VALUES (101, 1, 'ACTIVO', true, NOW());

INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, fecha_asignacion, asignado_por)
VALUES 
    (101, 1, 1, true, NOW(), NULL),  -- ADMIN
    (101, 3, 1, true, NOW(), NULL);  -- OPERATOR

-- Usuario 2: USER (un solo rol activo)
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo, mfa_habilitado, fecha_eliminacion, created_at, updated_at) 
VALUES (102, 'regular.user@acme.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TuPcGmy30x9SToB5xLqf7gF8F3ni', 'Regular User', false, NULL, NOW(), NOW());

INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion)
VALUES (102, 1, 'ACTIVO', true, NOW());

INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, fecha_asignacion, asignado_por)
VALUES (102, 2, 1, true, NOW(), 101);  -- USER

-- Usuario 3: Sin roles asignados (para probar listado sin roles)
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo, mfa_habilitado, fecha_eliminacion, created_at, updated_at) 
VALUES (103, 'noroles.user@acme.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TuPcGmy30x9SToB5xLqf7gF8F3ni', 'No Roles User', false, NULL, NOW(), NOW());

INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion)
VALUES (103, 1, 'ACTIVO', true, NOW());

-- Usuario 4: Usuario SUSPENDIDO (para probar filtros por estado)
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo, mfa_habilitado, fecha_eliminacion, created_at, updated_at) 
VALUES (104, 'suspended.user@acme.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TuPcGmy30x9SToB5xLqf7gF8F3ni', 'Suspended User', false, NULL, NOW(), NOW());

INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion)
VALUES (104, 1, 'SUSPENDIDO', true, NOW());

INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, fecha_asignacion, asignado_por)
VALUES (104, 2, 1, true, NOW(), 101);  -- USER

-- Usuario 5: USER + OPERATOR (múltiples roles)
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo, mfa_habilitado, fecha_eliminacion, created_at, updated_at) 
VALUES (105, 'multi.role@acme.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TuPcGmy30x9SToB5xLqf7gF8F3ni', 'Multi Role User', false, NULL, NOW(), NOW());

INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion)
VALUES (105, 1, 'ACTIVO', true, NOW());

INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, fecha_asignacion, asignado_por)
VALUES 
    (105, 2, 1, true, NOW(), 101),  -- USER
    (105, 3, 1, true, NOW(), 101);  -- OPERATOR

-- Usuario 6: Usuario para búsqueda por nombre (nombre contiene "Juan")
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo, mfa_habilitado, fecha_eliminacion, created_at, updated_at) 
VALUES (106, 'juan.perez@acme.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TuPcGmy30x9SToB5xLqf7gF8F3ni', 'Juan Pérez González', false, NULL, NOW(), NOW());

INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion)
VALUES (106, 1, 'ACTIVO', true, NOW());

INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, fecha_asignacion, asignado_por)
VALUES (106, 2, 1, true, NOW(), 101);  -- USER

-- Usuario 7: Usuario para búsqueda por email (email contiene "test")
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo, mfa_habilitado, fecha_eliminacion, created_at, updated_at) 
VALUES (107, 'test.search@acme.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TuPcGmy30x9SToB5xLqf7gF8F3ni', 'Test Search User', false, NULL, NOW(), NOW());

INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion)
VALUES (107, 1, 'ACTIVO', true, NOW());

INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, fecha_asignacion, asignado_por)
VALUES (107, 2, 1, true, NOW(), 101);  -- USER

-- -----------------------------------------------------------------------------
-- USUARIOS PARA ORGANIZACIÓN 2 (Contoso Ltd) - VALIDACIÓN DE AISLAMIENTO
-- -----------------------------------------------------------------------------

-- Usuario 201: Admin de Contoso (NO debe aparecer en listados de Acme)
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo, mfa_habilitado, fecha_eliminacion, created_at, updated_at) 
VALUES (201, 'admin@contoso.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TuPcGmy30x9SToB5xLqf7gF8F3ni', 'Contoso Admin', false, NULL, NOW(), NOW());

INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion)
VALUES (201, 2, 'ACTIVO', true, NOW());

INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, fecha_asignacion, asignado_por)
VALUES (201, 1, 2, true, NOW(), NULL);  -- ADMIN

-- Usuario 202: Usuario de Contoso (validación aislamiento)
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo, mfa_habilitado, fecha_eliminacion, created_at, updated_at) 
VALUES (202, 'user@contoso.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TuPcGmy30x9SToB5xLqf7gF8F3ni', 'Contoso User', false, NULL, NOW(), NOW());

INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada, fecha_asignacion)
VALUES (202, 2, 'ACTIVO', true, NOW());

INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, activo, fecha_asignacion, asignado_por)
VALUES (202, 2, 2, true, NOW(), 201);  -- USER

-- -----------------------------------------------------------------------------
-- RESUMEN DE DATOS INSERTADOS
-- -----------------------------------------------------------------------------
-- Organización 1 (Acme Corp): 7 usuarios
--   - 101: 2 roles (ADMIN, OPERATOR) - ACTIVO
--   - 102: 1 rol (USER) - ACTIVO
--   - 103: 0 roles - ACTIVO
--   - 104: 1 rol (USER) - SUSPENDIDO
--   - 105: 2 roles (USER, OPERATOR) - ACTIVO
--   - 106: 1 rol (USER) - ACTIVO (nombre contiene "Juan")
--   - 107: 1 rol (USER) - ACTIVO (email contiene "test")
--
-- Organización 2 (Contoso Ltd): 2 usuarios
--   - 201: 1 rol (ADMIN) - ACTIVO
--   - 202: 1 rol (USER) - ACTIVO
-- -----------------------------------------------------------------------------

COMMENT ON TABLE usuarios IS 'Datos semilla para US-ADMIN-003: Incluye usuarios con múltiples configuraciones de roles y estados';
