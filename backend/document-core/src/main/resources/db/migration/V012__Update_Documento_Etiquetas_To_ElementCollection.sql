-- Migración: Cambiar etiquetas de array a tabla de relación
-- Fecha: 2026-02-05
-- US-DOC-001: Se cambia el modelo de etiquetas de array TEXT[] a @ElementCollection 
-- para mejor compatibilidad con Hibernate 6.x

-- Paso 1: Crear tabla para etiquetas
CREATE TABLE IF NOT EXISTS documento_etiqueta (
    documento_id BIGINT NOT NULL,
    etiqueta VARCHAR(100),
    CONSTRAINT fk_documento_etiqueta_documento FOREIGN KEY (documento_id) 
        REFERENCES documento(id) ON DELETE CASCADE
);

-- Paso 2: Crear índice para mejor rendimiento
CREATE INDEX IF NOT EXISTS idx_documento_etiqueta_documento_id ON documento_etiqueta(documento_id);

-- Paso 3: Migrar datos existentes del array a la tabla (si hubiera datos)
-- INSERT INTO documento_etiqueta (documento_id, etiqueta)
-- SELECT id, unnest(etiquetas) 
-- FROM documento 
-- WHERE etiquetas IS NOT NULL AND array_length(etiquetas, 1) > 0;

-- Paso 4: Eliminar la columna etiquetas del documento
-- ALTER TABLE documento DROP COLUMN IF EXISTS etiquetas;

-- NOTA: Comentamos los pasos 3 y 4 porque la columna etiquetas no existe aún en producción
-- ya que el modelo de Documento se creó recientemente. Si existe en algún ambiente, 
-- descomentar estos pasos.
