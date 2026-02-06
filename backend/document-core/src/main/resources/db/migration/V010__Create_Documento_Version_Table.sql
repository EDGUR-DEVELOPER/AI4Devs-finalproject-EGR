-- ================================================================================================================
-- Migración V010: Crear tabla documento_version
-- Descripción: Tabla de versiones de documentos (versionado inmutable)
-- Autor: Sistema
-- Fecha: 2025-02-05
-- ================================================================================================================

CREATE TABLE documento_version (
    -- Identificador único de la versión
    id BIGSERIAL PRIMARY KEY,
    
    -- Relación con documento padre
    documento_id BIGINT NOT NULL,
    
    -- Número secuencial de versión (1, 2, 3, ...)
    numero_secuencial INTEGER NOT NULL,
    
    -- Información del archivo
    tamanio_bytes BIGINT NOT NULL,
    ruta_almacenamiento VARCHAR(500) NOT NULL,
    hash_contenido VARCHAR(64) NOT NULL,
    
    -- Comentario de cambio (opcional)
    comentario_cambio VARCHAR(500),
    
    -- Auditoría (inmutable)
    creado_por BIGINT NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Métricas de uso
    descargas INTEGER DEFAULT 0 NOT NULL,
    ultima_descarga_en TIMESTAMP,
    
    -- Restricciones de validación
    CONSTRAINT chk_version_numero_positivo CHECK (numero_secuencial > 0),
    CONSTRAINT chk_version_tamanio_positivo CHECK (tamanio_bytes > 0),
    CONSTRAINT chk_version_hash_formato CHECK (LENGTH(hash_contenido) = 64),
    CONSTRAINT chk_version_descargas_no_negativo CHECK (descargas >= 0)
);

-- ================================================================================================================
-- ÍNDICES
-- ================================================================================================================

-- Índice para búsquedas por documento
CREATE INDEX idx_documento_version_documento_id ON documento_version(documento_id);

-- Índice para búsquedas por creador
CREATE INDEX idx_documento_version_creado_por ON documento_version(creado_por);

-- Índice para búsquedas por ruta de almacenamiento (deduplicación)
CREATE INDEX idx_documento_version_ruta ON documento_version(ruta_almacenamiento);

-- Índice para búsquedas por hash de contenido (detección de duplicados)
CREATE INDEX idx_documento_version_hash ON documento_version(hash_contenido);

-- Índice para ordenar versiones por fecha
CREATE INDEX idx_documento_version_fecha_creacion ON documento_version(fecha_creacion);

-- ================================================================================================================
-- RESTRICCIÓN ÚNICA
-- ================================================================================================================

-- Restricción única: no puede haber dos versiones con el mismo número para un documento
CREATE UNIQUE INDEX uk_documento_version_documento_numero 
ON documento_version(documento_id, numero_secuencial);

-- ================================================================================================================
-- COMENTARIOS
-- ================================================================================================================

COMMENT ON TABLE documento_version IS 'Tabla de versiones de documentos. Cada versión es inmutable y representa un snapshot del contenido';
COMMENT ON COLUMN documento_version.id IS 'Identificador único de la versión';
COMMENT ON COLUMN documento_version.documento_id IS 'ID del documento padre';
COMMENT ON COLUMN documento_version.numero_secuencial IS 'Número secuencial de versión (1, 2, 3, ...)';
COMMENT ON COLUMN documento_version.tamanio_bytes IS 'Tamaño del archivo de esta versión en bytes';
COMMENT ON COLUMN documento_version.ruta_almacenamiento IS 'Ruta completa del archivo en almacenamiento';
COMMENT ON COLUMN documento_version.hash_contenido IS 'Hash SHA256 del contenido del archivo (64 caracteres hexadecimales)';
COMMENT ON COLUMN documento_version.comentario_cambio IS 'Descripción opcional del cambio en esta versión';
COMMENT ON COLUMN documento_version.creado_por IS 'ID del usuario que creó esta versión';
COMMENT ON COLUMN documento_version.fecha_creacion IS 'Timestamp de creación de la versión (inmutable)';
COMMENT ON COLUMN documento_version.descargas IS 'Contador de descargas de esta versión';
COMMENT ON COLUMN documento_version.ultima_descarga_en IS 'Timestamp de la última descarga';
