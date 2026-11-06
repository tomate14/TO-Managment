-- TO Managment: datos iniciales (catálogos, valores por defecto). Idempotente.
-- Uso: psql -v app_schema=<APP_DB_SCHEMA> -f 02_seed.sql  (después de 01_schema.sql)
-- Cada INSERT usa ON CONFLICT DO NOTHING para poder correrlo más de una vez.

SET search_path TO :"app_schema";

-- (É1 no define datos iniciales: el admin inicial sale del .env.)

-- (É8: los catálogos se crean por establecimiento desde el backend; no hay datos globales.)
-- (É3 no define datos iniciales: las plantillas las crean las terapeutas.)
-- (É4 no define datos iniciales: los pacientes son datos reales.)
-- (É5 y É6 no definen datos iniciales.)
-- (É7 no define datos iniciales.)
