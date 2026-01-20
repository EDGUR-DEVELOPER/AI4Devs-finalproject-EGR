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

-- Índice para búsquedas por estado
CREATE INDEX idx_organizaciones_estado ON organizaciones(estado) WHERE estado = 'ACTIVO';

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_carpetas_tenant_lookup ON carpetas(organizacion_id, id);
CREATE INDEX idx_carpetas_tenant_padre ON carpetas(organizacion_id, carpeta_padre_id) WHERE carpeta_padre_id IS NOT NULL;
CREATE INDEX idx_carpetas_propietario ON carpetas(propietario_id, organizacion_id);
CREATE INDEX idx_carpetas_activas ON carpetas(organizacion_id) WHERE fecha_eliminacion IS NULL;

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_documentos_tenant_lookup ON documentos(organizacion_id, id);
CREATE INDEX idx_documentos_tenant_carpeta ON documentos(organizacion_id, carpeta_id);
CREATE INDEX idx_documentos_propietario ON documentos(propietario_id, organizacion_id);
CREATE INDEX idx_documentos_activos ON documentos(organizacion_id) WHERE fecha_eliminacion IS NULL;

-- Índice GIN para búsqueda en metadatos JSONB
CREATE INDEX idx_documentos_metadatos_gin ON documentos USING GIN (metadatos_globales);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_versiones_tenant_lookup ON versiones(organizacion_id, id);
CREATE INDEX idx_versiones_tenant_documento ON versiones(organizacion_id, documento_id);
CREATE INDEX idx_versiones_hash ON versiones(hash_sha256);
CREATE INDEX idx_versiones_creador ON versiones(creador_id, organizacion_id);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_permiso_carpeta_usuario_tenant ON permiso_carpeta_usuario(organizacion_id, usuario_id);
CREATE INDEX idx_permiso_carpeta_usuario_carpeta ON permiso_carpeta_usuario(carpeta_id, organizacion_id);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_permiso_carpeta_rol_tenant ON permiso_carpeta_rol(organizacion_id, rol_id);
CREATE INDEX idx_permiso_carpeta_rol_carpeta ON permiso_carpeta_rol(carpeta_id, organizacion_id);

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_permiso_documento_usuario_tenant ON permiso_documento_usuario(organizacion_id, usuario_id);
CREATE INDEX idx_permiso_documento_usuario_documento ON permiso_documento_usuario(documento_id, organizacion_id);
CREATE INDEX idx_permiso_documento_usuario_vigente ON permiso_documento_usuario(organizacion_id, usuario_id) 
    WHERE fecha_expiracion IS NULL OR fecha_expiracion > NOW();

-- Índices compuestos para queries multi-tenant
CREATE INDEX idx_permiso_documento_rol_tenant ON permiso_documento_rol(organizacion_id, rol_id);
CREATE INDEX idx_permiso_documento_rol_documento ON permiso_documento_rol(documento_id, organizacion_id);
CREATE INDEX idx_permiso_documento_rol_vigente ON permiso_documento_rol(organizacion_id, rol_id) 
    WHERE fecha_expiracion IS NULL OR fecha_expiracion > NOW();

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
