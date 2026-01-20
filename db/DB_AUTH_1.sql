-- =============================================================================
-- SCHEMA: IAM (Identity & Organization Management)
-- Propósito: Soporte para US-AUTH-001 (Login multi-organización)
-- =============================================================================

-- Índice para búsquedas por estado
CREATE INDEX idx_organizaciones_estado ON organizaciones(estado) WHERE estado = 'ACTIVO';

-- Índice único para email (optimizado para lookups en login)
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios(LOWER(email)) WHERE fecha_eliminacion IS NULL;

-- Índice para soft delete
CREATE INDEX idx_usuarios_activos ON usuarios(id) WHERE fecha_eliminacion IS NULL;

-- Índice para consultas por usuario (lookup en login)
CREATE INDEX idx_usuarios_org_usuario ON usuarios_organizaciones(usuario_id) WHERE estado = 'ACTIVO';

-- Índice para consultas por organización
CREATE INDEX idx_usuarios_org_organizacion ON usuarios_organizaciones(organizacion_id) WHERE estado = 'ACTIVO';

-- -----------------------------------------------------------------------------
-- REGLA DE NEGOCIO CRÍTICA (US-AUTH-001):
-- Solo puede existir 1 membresía ACTIVA marcada como predeterminada por usuario
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX ux_usuario_org_default_activa 
    ON usuarios_organizaciones(usuario_id) 
    WHERE (estado = 'ACTIVO' AND es_predeterminada = TRUE);

-- =============================================================================
-- DATOS SEMILLA (Para QA y Pruebas Automatizadas)
-- Escenarios de criterios de aceptación US-AUTH-001
-- =============================================================================

-- Organizaciones de prueba
INSERT INTO organizaciones (id, nombre, estado) VALUES
    (1, 'Acme Corp', 'ACTIVO'),
    (2, 'Contoso Ltd', 'ACTIVO'),
    (3, 'Fabrikam Inc', 'ACTIVO'),
    (4, 'Org Suspendida', 'SUSPENDIDO');

-- Usuarios de prueba
INSERT INTO usuarios (id, email, hash_contrasena, nombre_completo) VALUES
    (1, 'sin-org@test.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVxW3Q8qKm', 'Usuario Sin Org'),
    (2, 'una-org@test.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVxW3Q8qKm', 'Usuario Con Una Org'),
    (3, 'dos-org-ok@test.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVxW3Q8qKm', 'Usuario Con Dos Orgs OK'),
    (4, 'dos-org-sin-default@test.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVxW3Q8qKm', 'Usuario Con Dos Orgs Sin Default'),
    (5, 'tres-org@test.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVxW3Q8qKm', 'Usuario Con Tres Orgs');

-- Escenario 1: Usuario sin organizaciones activas (403)
-- usuario_id=1 no tiene membresías

-- Escenario 2: Usuario con 1 organización activa (200 - auto-seleccionada)
INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada) VALUES
    (2, 1, 'ACTIVO', FALSE);

-- Escenario 3: Usuario con 2 organizaciones activas CON predeterminada (200)
INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada) VALUES
    (3, 1, 'ACTIVO', TRUE),
    (3, 2, 'ACTIVO', FALSE);

-- Escenario 4: Usuario con 2 organizaciones activas SIN predeterminada (409)
INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada) VALUES
    (4, 1, 'ACTIVO', FALSE),
    (4, 2, 'ACTIVO', FALSE);

-- Escenario 5: Usuario con >2 organizaciones activas (409 - limitación MVP)
INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada) VALUES
    (5, 1, 'ACTIVO', TRUE),
    (5, 2, 'ACTIVO', FALSE),
    (5, 3, 'ACTIVO', FALSE);

-- Reset sequences
SELECT setval('organizaciones_id_seq', (SELECT MAX(id) FROM organizaciones));
SELECT setval('usuarios_id_seq', (SELECT MAX(id) FROM usuarios));

-- =============================================================================
-- QUERIES DE VERIFICACIÓN (Para Testing)
-- =============================================================================

-- Verificar índice único funciona (debe fallar si se intenta insertar 2da predeterminada)
-- INSERT INTO usuarios_organizaciones (usuario_id, organizacion_id, estado, es_predeterminada) 
-- VALUES (3, 3, 'ACTIVO', TRUE); -- Debe fallar por ux_usuario_org_default_activa

-- Verificar función de validación
SELECT * FROM validar_configuracion_organizacion_login(1); -- Escenario 1: Sin org (403)
SELECT * FROM validar_configuracion_organizacion_login(2); -- Escenario 2: 1 org (200)
SELECT * FROM validar_configuracion_organizacion_login(3); -- Escenario 3: 2 orgs OK (200)
SELECT * FROM validar_configuracion_organizacion_login(4); -- Escenario 4: 2 orgs sin default (409)
SELECT * FROM validar_configuracion_organizacion_login(5); -- Escenario 5: >2 orgs (409)

-- Listar membresías activas por usuario (query optimizada para backend)
CREATE OR REPLACE VIEW v_membresías_activas AS
SELECT 
    uo.usuario_id,
    u.email,
    u.nombre_completo,
    uo.organizacion_id,
    o.nombre AS organizacion_nombre,
    uo.es_predeterminada,
    uo.fecha_asignacion
FROM usuarios_organizaciones uo
INNER JOIN usuarios u ON u.id = uo.usuario_id
INNER JOIN organizaciones o ON o.id = uo.organizacion_id
WHERE uo.estado = 'ACTIVO'
  AND o.estado = 'ACTIVO'
  AND u.fecha_eliminacion IS NULL;