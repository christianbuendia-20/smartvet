-- ==============================================
-- MIGRACIÓN: Añade columna ruta_foto a mascota
-- Ejecutar manualmente en la BD Smartvet (ddl-auto=none).
-- ==============================================

ALTER TABLE mascota
    ADD COLUMN ruta_foto VARCHAR(255) NULL;
