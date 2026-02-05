-- Índices para eliminación lógica de carpetas vacías (US-FOLDER-004)
-- Optimizan consultas EXISTS y COUNT sobre subcarpetas y documentos activos

-- Subcarpetas activas por carpeta padre
CREATE INDEX IF NOT EXISTS idx_carpeta_padre_eliminacion
ON carpetas(carpeta_padre_id, fecha_eliminacion, organizacion_id)
WHERE fecha_eliminacion IS NULL;

-- Documentos activos por carpeta
CREATE INDEX IF NOT EXISTS idx_documento_carpeta_eliminacion
ON documentos(carpeta_id, fecha_eliminacion, organizacion_id)
WHERE fecha_eliminacion IS NULL;
