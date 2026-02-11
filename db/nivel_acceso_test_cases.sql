-- Nivel de acceso - Casos de prueba
-- Archivo: db/nivel_acceso_test_cases.sql
-- Propósito: Insertar registros de permisos (nivel_acceso) para usuarios ya existentes
-- Nota: Este script asume una tabla "nivel_acceso" con columnas mínimas:
--   usuario_id (int), recurso_tipo (text), recurso_id (text), permiso_crear (bool), permiso_modificar (bool), permiso_eliminar (bool), permiso_ver (bool), creado_en (timestamptz), nota (text)
-- Si su esquema difiere, ajuste los nombres/columnas según corresponda.

BEGIN;

-- ==================================================
-- CASOS DE USO: CREAR / MODIFICAR / ELIMINAR
-- ==================================================

-- 1) Caso: Usuario ADMIN (101) — permisos completos sobre una carpeta (prueba crear/modificar/eliminar)
INSERT INTO nivel_acceso (usuario_id, recurso_tipo, recurso_id, permiso_crear, permiso_modificar, permiso_eliminar, permiso_ver, creado_en, nota)
VALUES ((SELECT id FROM usuarios WHERE email = 'admin.user@acme.com'), 'CARPETA', 'FOLDER_1', true, true, true, true, NOW(), 'ADMIN: acceso total carpeta FOLDER_1')
RETURNING *;

-- 2) Caso: Usuario REGULAR (102) — puede crear y modificar documentos, pero no eliminarlos
INSERT INTO nivel_acceso (usuario_id, recurso_tipo, recurso_id, permiso_crear, permiso_modificar, permiso_eliminar, permiso_ver, creado_en, nota)
VALUES ((SELECT id FROM usuarios WHERE email = 'regular.user@acme.com'), 'DOCUMENTO', 'DOC_1001', true, true, false, true, NOW(), 'REGULAR: crear/modificar documento DOC_1001')
RETURNING *;

-- 3) Caso: Usuario sin roles (103) — solo lectura sobre carpeta (probar acceso denegado a acciones de escritura)
INSERT INTO nivel_acceso (usuario_id, recurso_tipo, recurso_id, permiso_crear, permiso_modificar, permiso_eliminar, permiso_ver, creado_en, nota)
VALUES ((SELECT id FROM usuarios WHERE email = 'noroles.user@acme.com'), 'CARPETA', 'FOLDER_2', false, false, false, true, NOW(), 'NO_ROLES: solo lectura carpeta FOLDER_2')
RETURNING *;

-- 4) Caso: Usuario Juan (106) — puede modificar documentos existentes, no crear nuevos ni eliminar
INSERT INTO nivel_acceso (usuario_id, recurso_tipo, recurso_id, permiso_crear, permiso_modificar, permiso_eliminar, permiso_ver, creado_en, nota)
VALUES ((SELECT id FROM usuarios WHERE email = 'juan.perez@acme.com'), 'DOCUMENTO', 'DOC_2001', false, true, false, true, NOW(), 'JUAN: modificar DOC_2001')
RETURNING *;

-- 5) Caso: Usuario SUSPENDIDO (104) — se guarda un registro sin permisos (ver comportamiento de la aplicación)
INSERT INTO nivel_acceso (usuario_id, recurso_tipo, recurso_id, permiso_crear, permiso_modificar, permiso_eliminar, permiso_ver, creado_en, nota)
VALUES ((SELECT id FROM usuarios WHERE email = 'suspended.user@acme.com'), 'CARPETA', 'FOLDER_3', false, false, false, false, NOW(), 'SUSPENDIDO: sin permisos')
RETURNING *;

-- 6) Caso: Usuario con múltiples roles (105) — permisos puntuales sobre carpeta compartida
INSERT INTO nivel_acceso (usuario_id, recurso_tipo, recurso_id, permiso_crear, permiso_modificar, permiso_eliminar, permiso_ver, creado_en, nota)
VALUES ((SELECT id FROM usuarios WHERE email = 'multi.role@acme.com'), 'CARPETA', 'FOLDER_SHARED', true, true, false, true, NOW(), 'MULTI: crear/modificar en carpeta compartida')
RETURNING *;

-- ==================================================
-- OPERACIONES DE MODIFICACIÓN (UPDATE) - casos de prueba
-- ==================================================

-- Caso: conceder permiso de eliminar al usuario 102 sobre DOC_1001
UPDATE nivel_acceso
SET permiso_eliminar = true, nota = coalesce(nota, '') || ' | otorgado permiso_eliminar'
WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'regular.user@acme.com')
  AND recurso_tipo = 'DOCUMENTO' AND recurso_id = 'DOC_1001'
RETURNING *;

-- Caso: revocar todos los permisos de usuario 105 sobre FOLDER_SHARED (simula revocación)
UPDATE nivel_acceso
SET permiso_crear = false, permiso_modificar = false, permiso_eliminar = false, permiso_ver = false, nota = coalesce(nota, '') || ' | revocados todos los permisos'
WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'multi.role@acme.com')
  AND recurso_tipo = 'CARPETA' AND recurso_id = 'FOLDER_SHARED'
RETURNING *;

-- ==================================================
-- OPERACIONES DE ELIMINACIÓN (DELETE) - casos de prueba
-- ==================================================

-- Caso: eliminar permiso específico (usuario 103 sobre FOLDER_2) — simula eliminación de acceso cuando se borra asignación
DELETE FROM nivel_acceso
WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'noroles.user@acme.com')
  AND recurso_tipo = 'CARPETA' AND recurso_id = 'FOLDER_2'
RETURNING *;

-- Caso: eliminar todos los accesos asociados a una carpeta (simula borrado de carpeta y limpieza de permisos)
DELETE FROM nivel_acceso
WHERE recurso_tipo = 'CARPETA' AND recurso_id = 'FOLDER_1'
RETURNING *;

-- ==================================================
-- CONSULTAS DE VERIFICACIÓN
-- ==================================================

-- Ver permisos de un usuario por email
SELECT * FROM nivel_acceso
WHERE usuario_id = (SELECT id FROM usuarios WHERE email = 'regular.user@acme.com');

-- Ver todos los permisos sobre un recurso
SELECT * FROM nivel_acceso
WHERE recurso_tipo = 'DOCUMENTO' AND recurso_id = 'DOC_1001';

-- Ver permisos que permitirían eliminar (útil para pruebas de UI/acciones destructivas)
SELECT u.email, n.*
FROM nivel_acceso n
JOIN usuarios u ON u.id = n.usuario_id
WHERE n.permiso_eliminar = true;

COMMIT;

-- ==================================================
-- UTILIDADES (opcional): limpieza de datos insertados por este script
-- ==================================================
-- -- Para deshacer todo lo insertado por este script, descomente y ejecute:
-- BEGIN;
-- DELETE FROM nivel_acceso WHERE nota ILIKE '%DOC_1001%' OR recurso_id IN ('FOLDER_1','FOLDER_2','FOLDER_3','FOLDER_SHARED','DOC_2001');
-- COMMIT;

-- FIN
