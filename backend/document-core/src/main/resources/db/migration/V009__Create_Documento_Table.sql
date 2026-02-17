-- ================================================================================================================
-- Migración V009: Crear tabla documento
-- Descripción: Tabla principal para gestión de documentos en DocFlow
-- Autor: Sistema
-- Fecha: 2025-02-05
-- ================================================================================================================

CREATE TABLE documento (
    -- Identificador único del documento
    id BIGSERIAL PRIMARY KEY,
    
    -- Relación con organización (multi-tenancy)
    organizacion_id BIGINT NOT NULL,
    
    -- Relación con carpeta contenedora (nullable para documentos huérfanos)
    carpeta_id BIGINT,
    
    -- Información básica del documento
    nombre VARCHAR(255) NOT NULL,
    extension VARCHAR(50),
    tipo_contenido VARCHAR(100) NOT NULL,
    tamanio_bytes BIGINT NOT NULL,
    
    -- Control de versiones
    version_actual_id BIGINT,
    numero_versiones INTEGER DEFAULT 1 NOT NULL,
    
    -- Control de edición (bloqueo)
    bloqueado BOOLEAN DEFAULT FALSE NOT NULL,
    bloqueado_por BIGINT,
    bloqueado_en TIMESTAMP,
    
    -- Metadatos extensibles
    etiquetas TEXT[],
    metadatos JSONB,
    
    -- Auditoría
    creado_por BIGINT NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_eliminacion TIMESTAMP,
    
    -- Restricciones de validación
    CONSTRAINT chk_documento_tamanio_positivo CHECK (tamanio_bytes > 0),
    CONSTRAINT chk_documento_versiones_positivo CHECK (numero_versiones > 0)
);

-- ================================================================================================================
-- ÍNDICES
-- ================================================================================================================

-- Índice para búsquedas por organización (multi-tenancy)
CREATE INDEX idx_documento_organizacion_id ON documento(organizacion_id);

-- Índice para búsquedas por carpeta
CREATE INDEX idx_documento_carpeta_id ON documento(carpeta_id);

-- Índice para búsquedas por creador
CREATE INDEX idx_documento_creado_por ON documento(creado_por);

-- Índice para referencia a versión actual
CREATE INDEX idx_documento_version_actual_id ON documento(version_actual_id);

-- Índice para soporte de eliminación lógica
CREATE INDEX idx_documento_fecha_eliminacion ON documento(fecha_eliminacion);

-- Índice para búsquedas por nombre (común en búsquedas)
CREATE INDEX idx_documento_nombre ON documento(nombre);

-- Índice para búsquedas por tipo de contenido
CREATE INDEX idx_documento_tipo_contenido ON documento(tipo_contenido);

-- Índice GIN para búsquedas en etiquetas (PostgreSQL array index)
CREATE INDEX idx_documento_etiquetas ON documento USING GIN(etiquetas);

-- Índice GIN para búsquedas en metadatos JSONB
CREATE INDEX idx_documento_metadatos ON documento USING GIN(metadatos);

-- ================================================================================================================
-- RESTRICCIÓN ÚNICA
-- ================================================================================================================

-- Restricción única: no puede haber dos documentos con el mismo nombre en la misma carpeta
-- (excluye documentos eliminados usando eliminación lógica)
CREATE UNIQUE INDEX uk_documento_nombre_carpeta 
ON documento(nombre, carpeta_id, organizacion_id) 
WHERE fecha_eliminacion IS NULL;

-- ================================================================================================================
-- COMENTARIOS
-- ================================================================================================================

COMMENT ON TABLE documento IS 'Tabla principal de documentos. Almacena metadata de documentos con soporte de versionado, multi-tenancy y eliminación lógica';
COMMENT ON COLUMN documento.id IS 'Identificador único del documento';
COMMENT ON COLUMN documento.organizacion_id IS 'ID de la organización propietaria (multi-tenancy)';
COMMENT ON COLUMN documento.carpeta_id IS 'ID de la carpeta contenedora (nullable para documentos sin carpeta)';
COMMENT ON COLUMN documento.nombre IS 'Nombre del documento (sin extensión obligatoria)';
COMMENT ON COLUMN documento.extension IS 'Extensión del archivo (pdf, docx, etc.)';
COMMENT ON COLUMN documento.tipo_contenido IS 'MIME type del documento (application/pdf, image/png, etc.)';
COMMENT ON COLUMN documento.tamanio_bytes IS 'Tamaño del archivo en bytes de la versión actual';
COMMENT ON COLUMN documento.version_actual_id IS 'Referencia a la versión activa del documento';
COMMENT ON COLUMN documento.numero_versiones IS 'Contador de versiones del documento';
COMMENT ON COLUMN documento.bloqueado IS 'Indica si el documento está bloqueado para edición';
COMMENT ON COLUMN documento.bloqueado_por IS 'ID del usuario que bloqueó el documento';
COMMENT ON COLUMN documento.bloqueado_en IS 'Timestamp del bloqueo';
COMMENT ON COLUMN documento.etiquetas IS 'Array de etiquetas para categorización';
COMMENT ON COLUMN documento.metadatos IS 'Metadatos extensibles en formato JSONB';
COMMENT ON COLUMN documento.creado_por IS 'ID del usuario creador';
COMMENT ON COLUMN documento.fecha_creacion IS 'Timestamp de creación';
COMMENT ON COLUMN documento.fecha_actualizacion IS 'Timestamp de última actualización';
COMMENT ON COLUMN documento.fecha_eliminacion IS 'Timestamp de eliminación lógica (NULL si activo)';
