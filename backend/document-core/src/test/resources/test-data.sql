-- Test data for CarpetaContenidoControllerIntegrationTest

-- Insert test organization
INSERT INTO organizaciones (id, nombre, descripcion, activo, fecha_creacion)
VALUES (10, 'Test Org', 'Test Organization', true, NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert test user
INSERT INTO usuarios (id, nombre, email, activo, fecha_creacion)
VALUES (100, 'Test User', 'test@docflow.local', true, NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert test root folder (carpeta raíz)
INSERT INTO carpetas (id, organizacion_id, nombre, descripcion, creado_por, fecha_creacion, fecha_actualizacion, fecha_eliminacion, carpeta_padre_id)
VALUES (1, 10, 'Test Carpeta', 'Test folder for integration tests', 100, NOW(), NOW(), NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- Insert permission for user on test folder (READ level)
INSERT INTO permisos_carpeta_usuario (carpeta_id, usuario_id, nivel_acceso, fecha_creacion)
VALUES (1, 100, 'LEER', NOW())
ON CONFLICT (carpeta_id, usuario_id) DO NOTHING;
