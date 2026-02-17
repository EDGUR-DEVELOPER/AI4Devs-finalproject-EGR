-- Seed: Insert standard access levels
-- Description: Inserts the three standard access levels with their permissions (idempotent)
-- Author: DocFlow Team
-- Date: 2026-01-27

INSERT INTO nivel_acceso (id, codigo, nombre, descripcion, acciones_permitidas, orden, activo, fecha_actualizacion, fecha_creacion )
VALUES
  (1, 'LECTURA', 'Lectura / Consulta', 
   'Permite ver, listar y descargar documentos. Sin capacidad de modificación.',
   '["ver", "listar", "descargar"]'::jsonb, 1, true, NOW(), NOW()),
  
  (2, 'ESCRITURA', 'Escritura / Modificación',
   'Permite subir nuevas versiones, renombrar y modificar metadatos de documentos.',
   '["ver", "listar", "descargar", "subir", "modificar", "crear_version"]'::jsonb, 2, true, NOW(), NOW()),
  
  (3, 'ADMINISTRACION', 'Administración / Control Total',
   'Acceso total: crear, modificar, eliminar carpetas/documentos y gestionar permisos granulares.',
   '["ver", "listar", "descargar", "subir", "modificar", "crear_version", "eliminar", "administrar_permisos", 
	"cambiar_version_actual"]'::jsonb, 3, true, NOW(), NOW())
ON CONFLICT (codigo) DO NOTHING;

select * from nivel_acceso
