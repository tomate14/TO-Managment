-- TO Managment: datos iniciales (catálogos, valores por defecto). Idempotente.
-- Uso: psql -v app_schema=<APP_DB_SCHEMA> -f 02_seed.sql  (después de 01_schema.sql)
-- Cada INSERT usa ON CONFLICT DO NOTHING para poder correrlo más de una vez.

SET search_path TO :"app_schema";

-- (É1 no define datos iniciales: el admin inicial sale del .env.)

-- Catálogos (É2), igual que la migración V4__seed_catalogs.sql
INSERT INTO body_segment (name) VALUES
    ('Hombro'), ('Codo'), ('Muñeca'), ('Mano'), ('Dedos')
ON CONFLICT DO NOTHING;

INSERT INTO surgery_type (name) VALUES
    ('Osteosíntesis con placa'), ('Tenorrafia'), ('Liberación del túnel carpiano'), ('Artrodesis')
ON CONFLICT DO NOTHING;

INSERT INTO scar_type (name) VALUES
    ('Normotrófica'), ('Hipertrófica'), ('Queloide'), ('Retráctil')
ON CONFLICT DO NOTHING;
