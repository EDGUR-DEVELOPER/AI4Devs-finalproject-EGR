-- =============================================================================
-- SCHEMA: IAM (Identity & Organization Management)
-- Propósito: Soporte para US-AUTH-001 (Login multi-organización)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. TABLA: organizaciones
-- Contenedor raíz que define el alcance legal y de configuración del cliente
-- -----------------------------------------------------------------------------
CREATE TABLE organizaciones (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    configuracion JSONB NOT NULL DEFAULT '{}',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'ARCHIVADO')),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT ck_organizacion_nombre_longitud CHECK (LENGTH(TRIM(nombre)) >= 2)
);

-- Índice para búsquedas por estado
CREATE INDEX idx_organizaciones_estado ON organizaciones(estado) WHERE estado = 'ACTIVO';

COMMENT ON TABLE organizaciones IS 'Contenedor raíz del sistema multi-tenant';
COMMENT ON COLUMN organizaciones.configuracion IS 'Configuración visual (logo, colores) y técnica (límites, políticas). Ejemplo: {"apariencia": {"logo_url": "..."}, "seguridad": {"mfa_obligatorio": true}}';

-- -----------------------------------------------------------------------------
-- 2. TABLA: usuarios
-- Actor autenticado con credenciales globales (puede pertenecer a N organizaciones)
-- -----------------------------------------------------------------------------
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    hash_contrasena VARCHAR(255) NOT NULL,
    nombre_completo VARCHAR(100) NOT NULL,
    mfa_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_eliminacion TIMESTAMPTZ DEFAULT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT ck_email_formato CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$'),
    CONSTRAINT ck_nombre_longitud CHECK (LENGTH(TRIM(nombre_completo)) >= 2)
);

-- Índice único para email (optimizado para lookups en login)
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios(LOWER(email)) WHERE fecha_eliminacion IS NULL;

-- Índice para soft delete
CREATE INDEX idx_usuarios_activos ON usuarios(id) WHERE fecha_eliminacion IS NULL;

COMMENT ON TABLE usuarios IS 'Actores del sistema con credenciales globales. Soporta soft delete.';
COMMENT ON COLUMN usuarios.hash_contrasena IS 'Hash seguro usando Bcrypt (costo 12) o Argon2id';
COMMENT ON COLUMN usuarios.fecha_eliminacion IS 'Soft delete: si no es NULL, el usuario está desactivado';

-- Trigger para actualizar fecha_actualizacion automáticamente
CREATE OR REPLACE FUNCTION actualizar_fecha_actualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_usuarios_fecha_actualizacion
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_actualizacion();

-- -----------------------------------------------------------------------------
-- 3. TABLA: usuarios_organizaciones (Membresía Multi-Organización)
-- Define pertenencia, estado de membresía y organización predeterminada para login
-- -----------------------------------------------------------------------------
CREATE TABLE usuarios_organizaciones (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    organizacion_id INT NOT NULL REFERENCES organizaciones(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'SUSPENDIDO')),
    es_predeterminada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    PRIMARY KEY (usuario_id, organizacion_id)
);

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

COMMENT ON TABLE usuarios_organizaciones IS 'Membresía multi-organización con soporte para organización predeterminada en login';
COMMENT ON INDEX ux_usuario_org_default_activa IS 'Garantiza máximo 1 predeterminada activa por usuario (regla MVP de US-AUTH-001)';

-- -----------------------------------------------------------------------------
-- 4. FUNCIÓN DE VALIDACIÓN: Verificar configuración válida antes de login
-- Lógica de negocio:
--   - 0 orgs activas → SIN_ORGANIZACION (403)
--   - 1 org activa → OK (emitir token)
--   - 2 orgs activas → DEBE existir exactamente 1 predeterminada (409 si no)
--   - >2 orgs activas → ORGANIZACION_CONFIG_INVALIDA (409)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION validar_configuracion_organizacion_login(p_usuario_id BIGINT)
RETURNS TABLE (
    valido BOOLEAN,
    organizacion_id INT,
    codigo_error VARCHAR(50),
    mensaje_error TEXT
) AS $$
DECLARE
    v_count_activas INT;
    v_count_predeterminadas INT;
    v_org_id INT;
BEGIN
    -- Contar membresías activas
    SELECT COUNT(*), COUNT(*) FILTER (WHERE es_predeterminada = TRUE)
    INTO v_count_activas, v_count_predeterminadas
    FROM usuarios_organizaciones uo
    INNER JOIN organizaciones o ON o.id = uo.organizacion_id
    WHERE uo.usuario_id = p_usuario_id
      AND uo.estado = 'ACTIVO'
      AND o.estado = 'ACTIVO';

    -- Caso 1: Sin organizaciones activas
    IF v_count_activas = 0 THEN
        RETURN QUERY SELECT FALSE, NULL::INT, 'SIN_ORGANIZACION'::VARCHAR(50), 
            'El usuario no pertenece a ninguna organización activa.'::TEXT;
        RETURN;
    END IF;

    -- Caso 2: Exactamente 1 organización activa (auto-seleccionada)
    IF v_count_activas = 1 THEN
        SELECT uo.organizacion_id INTO v_org_id
        FROM usuarios_organizaciones uo
        INNER JOIN organizaciones o ON o.id = uo.organizacion_id
        WHERE uo.usuario_id = p_usuario_id
          AND uo.estado = 'ACTIVO'
          AND o.estado = 'ACTIVO';
        
        RETURN QUERY SELECT TRUE, v_org_id, NULL::VARCHAR(50), NULL::TEXT;
        RETURN;
    END IF;

    -- Caso 3: Exactamente 2 organizaciones activas (requiere predeterminada)
    IF v_count_activas = 2 THEN
        IF v_count_predeterminadas = 1 THEN
            SELECT uo.organizacion_id INTO v_org_id
            FROM usuarios_organizaciones uo
            INNER JOIN organizaciones o ON o.id = uo.organizacion_id
            WHERE uo.usuario_id = p_usuario_id
              AND uo.estado = 'ACTIVO'
              AND uo.es_predeterminada = TRUE
              AND o.estado = 'ACTIVO';
            
            RETURN QUERY SELECT TRUE, v_org_id, NULL::VARCHAR(50), NULL::TEXT;
            RETURN;
        ELSE
            RETURN QUERY SELECT FALSE, NULL::INT, 'ORGANIZACION_CONFIG_INVALIDA'::VARCHAR(50),
                'Usuario con 2 organizaciones activas pero sin predeterminada configurada.'::TEXT;
            RETURN;
        END IF;
    END IF;

    -- Caso 4: Más de 2 organizaciones activas (limitación MVP)
    IF v_count_activas > 2 THEN
        RETURN QUERY SELECT FALSE, NULL::INT, 'ORGANIZACION_CONFIG_INVALIDA'::VARCHAR(50),
            'Usuario con más de 2 organizaciones activas. Limitación MVP.'::TEXT;
        RETURN;
    END IF;

END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION validar_configuracion_organizacion_login IS 'Implementa reglas MVP de resolución de organización para POST /auth/login (US-AUTH-001)';

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

COMMENT ON VIEW v_membresías_activas IS 'Vista optimizada para resolución de organizaciones en login (US-AUTH-001)';