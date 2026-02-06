-- ================================================================================================================
-- Migración V011: Agregar Foreign Keys y relaciones entre tablas de documentos
-- Descripción: Establece las relaciones entre documento, documento_version y otras tablas del sistema
-- Autor: Sistema
-- Fecha: 2025-02-05
-- ================================================================================================================

-- ================================================================================================================
-- FOREIGN KEYS para tabla DOCUMENTO
-- ================================================================================================================

-- Relación documento -> carpeta (SET NULL cuando se elimina la carpeta)
-- Permite documentos huérfanos cuando se elimina la carpeta contenedora
ALTER TABLE documento
ADD CONSTRAINT fk_documento_carpeta
    FOREIGN KEY (carpeta_id) 
    REFERENCES carpeta(id) 
    ON DELETE SET NULL;

-- Relación documento -> documento_version (version_actual_id)
-- SET NULL cuando se elimina la versión (aunque esto no debería ocurrir en operación normal)
ALTER TABLE documento
ADD CONSTRAINT fk_documento_version_actual
    FOREIGN KEY (version_actual_id) 
    REFERENCES documento_version(id) 
    ON DELETE SET NULL;

-- ================================================================================================================
-- FOREIGN KEYS para tabla DOCUMENTO_VERSION
-- ================================================================================================================

-- Relación documento_version -> documento (CASCADE DELETE)
-- Si se elimina un documento, se eliminan todas sus versiones
ALTER TABLE documento_version
ADD CONSTRAINT fk_documento_version_documento
    FOREIGN KEY (documento_id) 
    REFERENCES documento(id) 
    ON DELETE CASCADE;

-- ================================================================================================================
-- COMENTARIOS
-- ================================================================================================================

COMMENT ON CONSTRAINT fk_documento_carpeta ON documento IS 
'Relación con carpeta contenedora. SET NULL permite documentos sin carpeta si la carpeta se elimina';

COMMENT ON CONSTRAINT fk_documento_version_actual ON documento IS 
'Referencia a la versión activa del documento. SET NULL si la versión se elimina';

COMMENT ON CONSTRAINT fk_documento_version_documento ON documento_version IS 
'Relación con documento padre. CASCADE elimina todas las versiones si el documento se elimina';
