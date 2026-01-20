-- =============================================================================
-- SCHEMA: IAM - Extensión para US-AUTH-002 (Roles y Permisos)
-- Propósito: Soportar JWT con claims de organizacion_id + roles[]
-- =============================================================================

-- Índice para búsquedas de roles activos por organización
CREATE INDEX idx_roles_organizacion ON roles(organizacion_id) WHERE activo = TRUE;

-- Índice para búsqueda de roles globales activos
CREATE INDEX idx_roles_globales ON roles(id) WHERE organizacion_id IS NULL AND activo = TRUE;

-- Índice compuesto para query crítico del login: obtener roles por usuario + org
CREATE INDEX idx_usuarios_roles_lookup ON usuarios_roles(usuario_id, organizacion_id) 
    WHERE activo = TRUE;

-- Índice para auditoría: buscar qué roles asignó un admin
CREATE INDEX idx_usuarios_roles_asignador ON usuarios_roles(asignado_por) 
    WHERE asignado_por IS NOT NULL;

-- Índice para listados por organización (US-ADMIN-003)
CREATE INDEX idx_usuarios_roles_org ON usuarios_roles(organizacion_id) 
    WHERE activo = TRUE;

-- -----------------------------------------------------------------------------
-- 8. VISTA: Contexto completo de usuario para generación de token
-- Combina membresía organizacional + roles en una única consulta
-- -----------------------------------------------------------------------------
CREATE OR REPLACE VIEW v_contexto_usuario_token AS
SELECT 
    u.id AS usuario_id,
    u.email,
    u.nombre_completo,
    uo.organizacion_id,
    o.nombre AS organizacion_nombre,
    obtener_roles_usuario_organizacion(u.id, uo.organizacion_id) AS roles
FROM usuarios u
INNER JOIN usuarios_organizaciones uo ON uo.usuario_id = u.id
INNER JOIN organizaciones o ON o.id = uo.organizacion_id
WHERE u.fecha_eliminacion IS NULL
  AND uo.estado = 'ACTIVO'
  AND o.estado = 'ACTIVO';

-- =============================================================================
-- DATOS SEMILLA: Roles Base del Sistema (US-AUTH-002)
-- =============================================================================

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

-- =============================================================================
-- DATOS SEMILLA: Asignaciones de Roles para Testing (US-AUTH-002)
-- =============================================================================

-- Usuario con 1 org (usuario_id=2, organizacion_id=1) → roles: ADMIN + USER
INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, asignado_por) VALUES
    (2, 2, 1, NULL), -- ADMIN
    (2, 3, 1, NULL); -- USER

-- Usuario con 2 orgs OK (usuario_id=3)
--   → En Acme Corp (org 1): ADMIN
--   → En Contoso Ltd (org 2): VIEWER
INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, asignado_por) VALUES
    (3, 2, 1, NULL), -- ADMIN en Acme
    (3, 4, 2, NULL); -- VIEWER en Contoso

-- Usuario con 2 orgs sin default (usuario_id=4)
--   → En Acme Corp (org 1): USER
--   → En Contoso Ltd (org 2): USER
INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id, asignado_por) VALUES
    (4, 3, 1, NULL), -- USER en Acme
    (4, 3, 2, NULL); -- USER en Contoso

-- Usuario sin roles en ninguna organización (usuario_id=5)
-- No se insertan registros → debe retornar array vacío []

-- =============================================================================
-- QUERIES DE VALIDACIÓN Y TESTING
-- =============================================================================

-- Test 1: Verificar que el constraint único funciona (debe fallar)
-- INSERT INTO usuarios_roles (usuario_id, rol_id, organizacion_id) 
-- VALUES (2, 2, 1); -- Debe fallar por ux_usuario_rol_org (ya existe)

-- Test 2: Obtener roles de usuario en organización específica
SELECT obtener_roles_usuario_organizacion(2, 1); -- Debe retornar: {ADMIN, USER}
SELECT obtener_roles_usuario_organizacion(3, 1); -- Debe retornar: {ADMIN}
SELECT obtener_roles_usuario_organizacion(3, 2); -- Debe retornar: {VIEWER}
SELECT obtener_roles_usuario_organizacion(5, 1); -- Debe retornar: {} (array vacío)
SELECT obtener_roles_usuario_organizacion(1, 1); -- Usuario sin org → {} (array vacío)

-- Test 3: Vista de contexto completo para token
SELECT * FROM v_contexto_usuario_token WHERE usuario_id = 2;
SELECT * FROM v_contexto_usuario_token WHERE usuario_id = 3 ORDER BY organizacion_id;

-- Test 4: Verificar performance del query de login (plan debe usar índices)
EXPLAIN ANALYZE 
SELECT * FROM v_contexto_usuario_token 
WHERE usuario_id = 2 AND organizacion_id = 1;

-- Test 5: Verificar que roles inactivos NO aparecen en resultados
UPDATE roles SET activo = FALSE WHERE codigo = 'USER';
SELECT obtener_roles_usuario_organizacion(2, 1); -- Debe retornar solo: {ADMIN}
UPDATE roles SET activo = TRUE WHERE codigo = 'USER'; -- Restaurar para otros tests

-- =============================================================================
-- ÍNDICES ADICIONALES PARA OPTIMIZACIÓN (Opcional - Evaluar en producción)
-- =============================================================================

-- Si el volumen de roles por organización crece mucho, considerar:
-- CREATE INDEX idx_roles_org_codigo ON roles(organizacion_id, codigo) WHERE activo = TRUE;

-- Si hay queries frecuentes de "qué usuarios tienen rol X en org Y":
-- CREATE INDEX idx_usuarios_roles_rol_org ON usuarios_roles(rol_id, organizacion_id) WHERE activo = TRUE;
