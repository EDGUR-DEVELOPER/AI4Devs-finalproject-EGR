-- V007__Indices_Listado_Contenido.sql
-- Migración para crear índices de optimización para consultas de listado de contenido con filtrado de permisos
-- Created for US-FOLDER-002: List Folder Content with Permission-Based Visibility

-- =====================================================================
-- INDICES PARA CONSULTAS DE CARPETAS CON PERMISOS
-- =====================================================================

-- Índice para búsqueda rápida de subcarpetas por carpeta padre
CREATE INDEX IF NOT EXISTS idx_carpetas_padre_activas 
ON carpetas(carpeta_padre_id, organizacion_id, fecha_eliminacion)
WHERE fecha_eliminacion IS NULL;

-- Índice para búsqueda rápida de carpeta raíz por organización
CREATE INDEX IF NOT EXISTS idx_carpetas_raiz_activas
ON carpetas(organizacion_id, carpeta_padre_id)
WHERE carpeta_padre_id IS NULL AND fecha_eliminacion IS NULL;

-- Índice para ordenamiento en consultas de listado
CREATE INDEX IF NOT EXISTS idx_carpetas_nombre_ordenamiento
ON carpetas(organizacion_id, nombre ASC)
WHERE fecha_eliminacion IS NULL;

-- =====================================================================
-- INDICES PARA CONSULTAS DE DOCUMENTOS CON PERMISOS
-- =====================================================================

-- Índice para búsqueda rápida de documentos por carpeta
CREATE INDEX IF NOT EXISTS idx_documentos_carpeta_activos
ON documentos(carpeta_id, organizacion_id, fecha_eliminacion)
WHERE fecha_eliminacion IS NULL;

-- Índice para ordenamiento en consultas de listado de documentos
CREATE INDEX IF NOT EXISTS idx_documentos_nombre_ordenamiento
ON documentos(organizacion_id, nombre ASC)
WHERE fecha_eliminacion IS NULL;

-- =====================================================================
-- INDICES PARA EVALUACIÓN RÁPIDA DE PERMISOS EN CARPETAS
-- =====================================================================

-- Índice para búsqueda rápida de permisos de carpeta por usuario
-- Critical para JOIN en consultas de listado con filtrado de permisos
CREATE INDEX IF NOT EXISTS idx_permisos_carpeta_usuario_acceso
ON permisos_carpeta_usuario(carpeta_id, usuario_id, nivel_acceso);

-- Índice para contar permisos rápidamente
CREATE INDEX IF NOT EXISTS idx_permisos_carpeta_usuario_nivel
ON permisos_carpeta_usuario(usuario_id, nivel_acceso);

-- =====================================================================
-- INDICES PARA EVALUACIÓN RÁPIDA DE PERMISOS EN DOCUMENTOS
-- =====================================================================

-- Índice para búsqueda rápida de permisos de documento por usuario
-- Critical para JOIN en consultas de listado con filtrado de permisos
CREATE INDEX IF NOT EXISTS idx_permisos_documento_usuario_acceso
ON permisos_documento_usuario(documento_id, usuario_id, nivel_acceso);

-- Índice para contar permisos rápidamente
CREATE INDEX IF NOT EXISTS idx_permisos_documento_usuario_nivel
ON permisos_documento_usuario(usuario_id, nivel_acceso);

-- =====================================================================
-- INDICES PARA HIERARCHÍA Y ANCESTROS
-- =====================================================================

-- Índice para traversal de jerarquía en CTEs recursivos
CREATE INDEX IF NOT EXISTS idx_carpetas_padre_id
ON carpetas(carpeta_padre_id)
WHERE fecha_eliminacion IS NULL;
