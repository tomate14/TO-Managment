-- TO Managment: esquema completo (estado final, equivalente a las migraciones Flyway V1..V5).
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

-- Catálogos (É2)
CREATE TABLE IF NOT EXISTS surgery_type (
    id     BIGSERIAL   PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_surgery_type_name_lower ON surgery_type (lower(name));

CREATE TABLE IF NOT EXISTS scar_type (
    id     BIGSERIAL   PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_scar_type_name_lower ON scar_type (lower(name));

CREATE TABLE IF NOT EXISTS body_segment (
    id     BIGSERIAL   PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_body_segment_name_lower ON body_segment (lower(name));

-- Plantillas de evaluación (É3)
CREATE TABLE IF NOT EXISTS evaluation_template (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    body_segment_id BIGINT       NOT NULL REFERENCES body_segment (id),
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_evaluation_template_segment_name ON evaluation_template (body_segment_id, lower(name));

CREATE TABLE IF NOT EXISTS question (
    id                       BIGSERIAL     PRIMARY KEY,
    template_id              BIGINT        NOT NULL REFERENCES evaluation_template (id),
    text                     VARCHAR(255)  NOT NULL,
    answer_type              VARCHAR(20)   NOT NULL CHECK (answer_type IN ('NUMERIC', 'YES_NO', 'OPTION', 'TEXT')),
    display_order            INTEGER       NOT NULL,
    active                   BOOLEAN       NOT NULL DEFAULT TRUE,
    min_value                NUMERIC(12, 3),
    max_value                NUMERIC(12, 3),
    parent_question_id       BIGINT        REFERENCES question (id),
    condition_operator       VARCHAR(20)   CHECK (condition_operator IN ('EQUALS', 'NOT_EQUALS', 'GREATER_THAN', 'LESS_THAN')),
    condition_expected_value VARCHAR(120),
    CONSTRAINT ck_question_condition_with_parent CHECK (
        (parent_question_id IS NULL AND condition_operator IS NULL AND condition_expected_value IS NULL)
        OR (parent_question_id IS NOT NULL AND condition_operator IS NOT NULL AND condition_expected_value IS NOT NULL)),
    CONSTRAINT ck_question_range CHECK (min_value IS NULL OR max_value IS NULL OR min_value <= max_value)
);
CREATE INDEX IF NOT EXISTS ix_question_template ON question (template_id, display_order);
CREATE INDEX IF NOT EXISTS ix_question_parent ON question (parent_question_id);

CREATE TABLE IF NOT EXISTS question_option (
    id            BIGSERIAL    PRIMARY KEY,
    question_id   BIGINT       NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    label         VARCHAR(120) NOT NULL,
    display_order INTEGER      NOT NULL
);
-- Labels are unique per question (case-insensitive), enforced by the app so options can be edited in place.
CREATE INDEX IF NOT EXISTS ix_question_option_question ON question_option (question_id, display_order);
