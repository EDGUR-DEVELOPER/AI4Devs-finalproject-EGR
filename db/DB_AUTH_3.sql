-- =============================================================================
-- SCHEMA: Document Core (Gestión Documental) - TENANT ISOLATION
-- Propósito: US-AUTH-004 (Aislamiento de datos por organización)
-- Versión: 3 (DB_AUTH_3)
-- Fecha: 8 de enero de 2026
-- =============================================================================
-- 
-- Este script implementa el aislamiento multi-tenant para el servicio document-core.
-- 
-- ESTRATEGIA:
-- 1. Replica tabla 'organizaciones' desde identity-service (sincronización manual)
-- 2. Añade columna 'organizacion_id' a 7 tablas del núcleo documental
-- 3. Crea índices compuestos para optimizar queries filtradas por tenant
-- 4. Valida que NO existan datos legacy sin organizacion_id antes de migrar
-- 
-- TABLAS AFECTADAS:
-- - carpetas
-- - documentos  
-- - versiones
-- - permiso_carpeta_usuario
-- - permiso_carpeta_rol
-- - permiso_documento_usuario
-- - permiso_documento_rol
-- 
-- PREREQUISITOS:
-- - DB_AUTH_1 ejecutado (contiene tabla organizaciones original en identity-service)
-- - Base de datos 'docflow_documentcore' creada y vacía
-- =============================================================================

-- -----------------------------------------------------------------------------
-- PASO 1: REPLICAR TABLA ORGANIZACIONES
-- -----------------------------------------------------------------------------
-- Nota: Esta es una copia local para mantener autonomía del microservicio.
-- La sincronización entre identity-service y document-core es MANUAL.
-- En producción, considerar CDC (Debezium) o eventos Kafka para consistencia eventual.
-- -----------------------------------------------------------------------------

CREATE TABLE organizaciones (
    id INTEGER PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    configuracion JSONB NOT NULL DEFAULT '{}',
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'SUSPENDIDO', 'ARCHIVADO')),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_sincronizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT ck_organizacion_nombre_longitud CHECK (LENGTH(TRIM(nombre)) >= 2)
);

-- Índice para búsquedas por estado
CREATE INDEX idx_organizaciones_estado ON organizaciones(estado) WHERE estado = 'ACTIVO';

COMMENT ON TABLE organizaciones IS 'Réplica de tabla organizaciones desde identity-service. Sincronización MANUAL.';
COMMENT ON COLUMN organizaciones.fecha_sincronizacion IS 'Timestamp de última sincronización desde identity-service';

-- -----------------------------------------------------------------------------
-- PASO 2: TABLA CARPETAS (Estructura jerárquica para organizar documentos)
-- -----------------------------------------------------------------------------

CREATE TABLE carpetas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    carpeta_padre_id BIGINT DEFAULT NULL,
    organizacion_id INTEGER NOT NULL,
    propietario_id BIGINT NOT NULL,
    ruta_jerarquia VARCHAR(500),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_eliminacion TIMESTAMPTZ DEFAULT NULL,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_carpetas_organizacion FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id),
    CONSTRAINT fk_carpetas_padre FOREIGN KEY (carpeta_padre_id) REFERENCES carpetas(id) ON DELETE RESTRICT,
    CONSTRAINT ck_carpeta_nombre_longitud CHECK (LENGTH(TRIM(nombre)) >= 1),
    CONSTRAINT ck_carpeta_no_autopadre CHECK (carpeta_padre_id IS NULL OR carpeta_padre_id != id)
);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_carpetas_tenant_lookup ON carpetas(organizacion_id, id);
CREATE INDEX idx_carpetas_tenant_padre ON carpetas(organizacion_id, carpeta_padre_id) WHERE carpeta_padre_id IS NOT NULL;
CREATE INDEX idx_carpetas_propietario ON carpetas(propietario_id, organizacion_id);
CREATE INDEX idx_carpetas_activas ON carpetas(organizacion_id) WHERE fecha_eliminacion IS NULL;

COMMENT ON TABLE carpetas IS 'Estructura jerárquica para organizar documentos. Aislada por organizacion_id.';
COMMENT ON COLUMN carpetas.ruta_jerarquia IS 'Path materializado (ej: 1.5.20) para consultas sin recursividad';
COMMENT ON COLUMN carpetas.organizacion_id IS 'FK a organizaciones. Discriminador multi-tenant crítico.';

-- -----------------------------------------------------------------------------
-- PASO 3: TABLA DOCUMENTOS (Entidad lógica del archivo)
-- -----------------------------------------------------------------------------

CREATE TABLE documentos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT,
    organizacion_id INTEGER NOT NULL,
    carpeta_id BIGINT NOT NULL,
    propietario_id BIGINT NOT NULL,
    version_actual_id BIGINT DEFAULT NULL,
    metadatos_globales JSONB NOT NULL DEFAULT '{}',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_eliminacion TIMESTAMPTZ DEFAULT NULL,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_documentos_organizacion FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id),
    CONSTRAINT fk_documentos_carpeta FOREIGN KEY (carpeta_id) REFERENCES carpetas(id) ON DELETE RESTRICT,
    CONSTRAINT ck_documento_nombre_longitud CHECK (LENGTH(TRIM(nombre)) >= 1)
);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_documentos_tenant_lookup ON documentos(organizacion_id, id);
CREATE INDEX idx_documentos_tenant_carpeta ON documentos(organizacion_id, carpeta_id);
CREATE INDEX idx_documentos_propietario ON documentos(propietario_id, organizacion_id);
CREATE INDEX idx_documentos_activos ON documentos(organizacion_id) WHERE fecha_eliminacion IS NULL;

-- Índice GIN para búsqueda en metadatos JSONB
CREATE INDEX idx_documentos_metadatos_gin ON documentos USING GIN (metadatos_globales);

COMMENT ON TABLE documentos IS 'Entidad lógica del documento. Aislada por organizacion_id.';
COMMENT ON COLUMN documentos.metadatos_globales IS 'Campos custom por organización (tags, cliente, fecha_venc). Ejemplo: {"cliente": "Acme Corp", "tags": ["urgente"]}';
COMMENT ON COLUMN documentos.organizacion_id IS 'FK a organizaciones. Discriminador multi-tenant crítico.';

-- -----------------------------------------------------------------------------
-- PASO 4: TABLA VERSIONES (Entidad física del archivo inmutable)
-- -----------------------------------------------------------------------------

CREATE TABLE versiones (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    organizacion_id INTEGER NOT NULL,
    numero_secuencial INTEGER NOT NULL,
    etiqueta_version VARCHAR(50),
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    tipo_mime VARCHAR(100),
    tamano_bytes BIGINT NOT NULL,
    hash_sha256 CHAR(64) NOT NULL,
    creador_id BIGINT NOT NULL,
    metadatos_version JSONB NOT NULL DEFAULT '{}',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_versiones_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
    CONSTRAINT fk_versiones_organizacion FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id),
    CONSTRAINT ck_version_numero_positivo CHECK (numero_secuencial > 0),
    CONSTRAINT ck_version_tamano_positivo CHECK (tamano_bytes >= 0),
    CONSTRAINT uq_version_documento_numero UNIQUE (documento_id, numero_secuencial)
);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_versiones_tenant_lookup ON versiones(organizacion_id, id);
CREATE INDEX idx_versiones_tenant_documento ON versiones(organizacion_id, documento_id);
CREATE INDEX idx_versiones_hash ON versiones(hash_sha256);
CREATE INDEX idx_versiones_creador ON versiones(creador_id, organizacion_id);

COMMENT ON TABLE versiones IS 'Versiones inmutables de documentos. Aislada por organizacion_id.';
COMMENT ON COLUMN versiones.hash_sha256 IS 'Checksum para integridad y deduplicación';
COMMENT ON COLUMN versiones.organizacion_id IS 'FK a organizaciones. Debe coincidir con documentos.organizacion_id';

-- -----------------------------------------------------------------------------
-- PASO 5: TABLA PERMISO_CARPETA_USUARIO (ACL explícito usuario-carpeta)
-- -----------------------------------------------------------------------------

CREATE TABLE permiso_carpeta_usuario (
    id BIGSERIAL PRIMARY KEY,
    carpeta_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    organizacion_id INTEGER NOT NULL,
    nivel_acceso VARCHAR(20) NOT NULL CHECK (nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')),
    recursivo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_permiso_carpeta_usuario_carpeta FOREIGN KEY (carpeta_id) REFERENCES carpetas(id) ON DELETE CASCADE,
    CONSTRAINT fk_permiso_carpeta_usuario_org FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id),
    CONSTRAINT uq_permiso_carpeta_usuario UNIQUE (carpeta_id, usuario_id)
);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_permiso_carpeta_usuario_tenant ON permiso_carpeta_usuario(organizacion_id, usuario_id);
CREATE INDEX idx_permiso_carpeta_usuario_carpeta ON permiso_carpeta_usuario(carpeta_id, organizacion_id);

COMMENT ON TABLE permiso_carpeta_usuario IS 'Permisos explícitos de usuarios sobre carpetas. Aislado por organizacion_id.';

-- -----------------------------------------------------------------------------
-- PASO 6: TABLA PERMISO_CARPETA_ROL (ACL rol-carpeta)
-- -----------------------------------------------------------------------------

CREATE TABLE permiso_carpeta_rol (
    id BIGSERIAL PRIMARY KEY,
    carpeta_id BIGINT NOT NULL,
    rol_id INTEGER NOT NULL,
    organizacion_id INTEGER NOT NULL,
    nivel_acceso VARCHAR(20) NOT NULL CHECK (nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')),
    recursivo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_permiso_carpeta_rol_carpeta FOREIGN KEY (carpeta_id) REFERENCES carpetas(id) ON DELETE CASCADE,
    CONSTRAINT fk_permiso_carpeta_rol_org FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id),
    CONSTRAINT uq_permiso_carpeta_rol UNIQUE (carpeta_id, rol_id)
);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_permiso_carpeta_rol_tenant ON permiso_carpeta_rol(organizacion_id, rol_id);
CREATE INDEX idx_permiso_carpeta_rol_carpeta ON permiso_carpeta_rol(carpeta_id, organizacion_id);

COMMENT ON TABLE permiso_carpeta_rol IS 'Permisos de roles sobre carpetas. Aislado por organizacion_id.';

-- -----------------------------------------------------------------------------
-- PASO 7: TABLA PERMISO_DOCUMENTO_USUARIO (ACL explícito usuario-documento)
-- -----------------------------------------------------------------------------

CREATE TABLE permiso_documento_usuario (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    organizacion_id INTEGER NOT NULL,
    nivel_acceso VARCHAR(20) NOT NULL CHECK (nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')),
    fecha_expiracion TIMESTAMPTZ DEFAULT NULL,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_permiso_documento_usuario_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
    CONSTRAINT fk_permiso_documento_usuario_org FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id),
    CONSTRAINT uq_permiso_documento_usuario UNIQUE (documento_id, usuario_id)
);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_permiso_documento_usuario_tenant ON permiso_documento_usuario(organizacion_id, usuario_id);
CREATE INDEX idx_permiso_documento_usuario_documento ON permiso_documento_usuario(documento_id, organizacion_id);
CREATE INDEX idx_permiso_documento_usuario_vigente ON permiso_documento_usuario(organizacion_id, usuario_id) 
    WHERE fecha_expiracion IS NULL OR fecha_expiracion > NOW();

COMMENT ON TABLE permiso_documento_usuario IS 'Permisos explícitos de usuarios sobre documentos. Aislado por organizacion_id.';

-- -----------------------------------------------------------------------------
-- PASO 8: TABLA PERMISO_DOCUMENTO_ROL (ACL rol-documento)
-- -----------------------------------------------------------------------------

CREATE TABLE permiso_documento_rol (
    id BIGSERIAL PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    rol_id INTEGER NOT NULL,
    organizacion_id INTEGER NOT NULL,
    nivel_acceso VARCHAR(20) NOT NULL CHECK (nivel_acceso IN ('LECTURA', 'ESCRITURA', 'ADMINISTRACION')),
    fecha_expiracion TIMESTAMPTZ DEFAULT NULL,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_permiso_documento_rol_documento FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE,
    CONSTRAINT fk_permiso_documento_rol_org FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id),
    CONSTRAINT uq_permiso_documento_rol UNIQUE (documento_id, rol_id)
);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_permiso_documento_rol_tenant ON permiso_documento_rol(organizacion_id, rol_id);
CREATE INDEX idx_permiso_documento_rol_documento ON permiso_documento_rol(documento_id, organizacion_id);
CREATE INDEX idx_permiso_documento_rol_vigente ON permiso_documento_rol(organizacion_id, rol_id) 
    WHERE fecha_expiracion IS NULL OR fecha_expiracion > NOW();

COMMENT ON TABLE permiso_documento_rol IS 'Permisos de roles sobre documentos. Aislado por organizacion_id.';

-- -----------------------------------------------------------------------------
-- PASO 9: FUNCIÓN HELPER PARA VALIDACIÓN CROSS-TENANT
-- -----------------------------------------------------------------------------
-- Valida que un recurso pertenece a la organización del usuario antes de update/delete.
-- Retorna TRUE si es válido, FALSE si hay violación de aislamiento.
-- -----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION validar_acceso_recurso(
    p_recurso_org_id INTEGER,
    p_usuario_org_id INTEGER
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Validar que ambos IDs coincidan
    IF p_recurso_org_id IS NULL OR p_usuario_org_id IS NULL THEN
        RAISE EXCEPTION 'IDs de organización no pueden ser NULL en validación de acceso';
    END IF;
    
    IF p_recurso_org_id != p_usuario_org_id THEN
        -- Log de auditoría de intento cross-tenant
        RAISE WARNING 'Intento de acceso cross-tenant detectado: recurso_org=%, usuario_org=%', 
            p_recurso_org_id, p_usuario_org_id;
        RETURN FALSE;
    END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION validar_acceso_recurso IS 'Valida aislamiento multi-tenant antes de operaciones update/delete';

-- -----------------------------------------------------------------------------
-- PASO 10: TRIGGERS PARA AUTO-ACTUALIZACIÓN DE TIMESTAMPS
-- -----------------------------------------------------------------------------

-- Función reutilizable para actualizar fecha_actualizacion
CREATE OR REPLACE FUNCTION actualizar_fecha_actualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar trigger a carpetas
CREATE TRIGGER trigger_carpetas_updated_at
    BEFORE UPDATE ON carpetas
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_actualizacion();

-- Aplicar trigger a documentos
CREATE TRIGGER trigger_documentos_updated_at
    BEFORE UPDATE ON documentos
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_actualizacion();

-- -----------------------------------------------------------------------------
-- PASO 11: VALIDACIÓN DE CONSISTENCIA MULTI-TENANT
-- -----------------------------------------------------------------------------
-- Constraint que valida que versiones.organizacion_id == documentos.organizacion_id
-- Previene inconsistencias donde una versión esté en org diferente al documento
-- -----------------------------------------------------------------------------

ALTER TABLE versiones
ADD CONSTRAINT ck_version_mismo_tenant_documento
CHECK (
    organizacion_id = (
        SELECT organizacion_id 
        FROM documentos 
        WHERE documentos.id = versiones.documento_id
    )
);

COMMENT ON CONSTRAINT ck_version_mismo_tenant_documento ON versiones IS 
    'Garantiza que versiones y documentos estén en la misma organización';

-- -----------------------------------------------------------------------------
-- PASO 12: DATOS DE PRUEBA (SEED) - Solo para desarrollo
-- -----------------------------------------------------------------------------
-- IMPORTANTE: NO ejecutar en producción. Solo para testing local.
-- -----------------------------------------------------------------------------

-- Insertar organización de prueba (debe coincidir con identity-service)
INSERT INTO organizaciones (id, nombre, configuracion, estado) VALUES 
(1, 'Organización Demo', '{"apariencia": {"logo_url": "/assets/demo-logo.png"}}', 'ACTIVO')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- FIN DEL SCRIPT DB_AUTH_3
-- =============================================================================
-- 
-- SIGUIENTE PASO: Ejecutar validación de datos legacy
-- Query de validación (debe retornar 0 rows):
-- 
-- SELECT 'carpetas' AS tabla, COUNT(*) FROM carpetas WHERE organizacion_id IS NULL
-- UNION ALL
-- SELECT 'documentos', COUNT(*) FROM documentos WHERE organizacion_id IS NULL
-- UNION ALL
-- SELECT 'versiones', COUNT(*) FROM versiones WHERE organizacion_id IS NULL;
-- 
-- Si retorna > 0, hay datos legacy sin organizacion_id que requieren backfill manual.
-- =============================================================================
