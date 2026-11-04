-- TO Managment: datos iniciales (catálogos, valores por defecto). Idempotente.
-- Uso: psql -v app_schema=<APP_DB_SCHEMA> -f 02_seed.sql  (después de 01_schema.sql)
-- Cada INSERT usa ON CONFLICT DO NOTHING para poder correrlo más de una vez.

SET search_path TO :"app_schema";

-- (H1.1 no define datos iniciales.)
