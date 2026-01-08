-- Cleanup script for integration tests
-- Removes all test data from tables in reverse dependency order

DELETE FROM usuarios_organizaciones;
DELETE FROM organizaciones;
DELETE FROM usuarios;

-- Reset sequences to 1 (for reproducible tests)
ALTER SEQUENCE usuarios_id_seq RESTART WITH 1;
ALTER SEQUENCE organizaciones_id_seq RESTART WITH 1;
