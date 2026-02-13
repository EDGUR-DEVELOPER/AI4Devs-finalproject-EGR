-- Fix Script: Resetear secuencia de permiso_carpeta_usuario
-- Este script corrige el problema de clave primaria duplicada

-- 1. Encontrar el máximo ID actual en la tabla
SELECT MAX(id) as max_id FROM permiso_carpeta_usuario;

-- 2. Resetear la secuencia al siguiente valor después del máximo
-- Si la tabla está vacía, esto establecerá la secuencia en 1
SELECT setval('permiso_carpeta_usuario_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM permiso_carpeta_usuario));

-- 3. Verificar el siguiente valor que asignará la secuencia
SELECT nextval('permiso_carpeta_usuario_id_seq');
