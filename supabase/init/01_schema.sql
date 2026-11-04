-- TO Managment: esquema completo (estado final, equivalente a las migraciones Flyway V1..V2).
-- Idempotente. Uso: psql -v app_schema=<APP_DB_SCHEMA> -f 01_schema.sql
-- Mantener sincronizado con backend/src/main/resources/db/migration en cada cambio de esquema.

CREATE SCHEMA IF NOT EXISTS :"app_schema";
COMMENT ON SCHEMA :"app_schema" IS 'TO Management application schema';
SET search_path TO :"app_schema";

-- Usuarios (H1.1)
CREATE TABLE IF NOT EXISTS app_user (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'TERAPEUTA')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_email_lower ON app_user (lower(email));
