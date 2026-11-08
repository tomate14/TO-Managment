-- TO Managment: esquema completo (estado final, equivalente a las migraciones Flyway V1..V14).
-- Idempotente. Uso: psql -v app_schema=<APP_DB_SCHEMA> -f 01_schema.sql
-- Mantener sincronizado con backend/src/main/resources/db/migration en cada cambio de esquema.

CREATE SCHEMA IF NOT EXISTS :"app_schema";
COMMENT ON SCHEMA :"app_schema" IS 'TO Management application schema';
SET search_path TO :"app_schema";

-- Establecimientos (É8): cada dato pertenece a un establecimiento
CREATE TABLE IF NOT EXISTS establishment (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_establishment_name_lower ON establishment (lower(name));

-- Tipo y aprobación del establecimiento (É9)
ALTER TABLE establishment ADD COLUMN IF NOT EXISTS kind VARCHAR(20) NOT NULL DEFAULT 'INSTITUTION';
-- La columna y su relleno van juntos: el relleno corre solo cuando la columna se crea, así una nueva ejecución
-- nunca aprueba solicitudes pendientes reales.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = current_schema() AND table_name = 'establishment'
                   AND column_name = 'approved_at') THEN
        ALTER TABLE establishment ADD COLUMN approved_at TIMESTAMPTZ NULL;
        UPDATE establishment SET approved_at = created_at;
    END IF;
END $$;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_establishment_kind'
                   AND conrelid = 'establishment'::regclass) THEN
        ALTER TABLE establishment
            ADD CONSTRAINT ck_establishment_kind CHECK (kind IN ('INSTITUTION', 'INDIVIDUAL'));
    END IF;
END $$;
ALTER TABLE establishment ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMPTZ NULL;

-- Usuarios (H1.1; É8 agrega SUPERADMIN y el establecimiento)
CREATE TABLE IF NOT EXISTS app_user (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('SUPERADMIN', 'ADMIN', 'TERAPEUTA')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    establishment_id BIGINT    REFERENCES establishment (id),
    CONSTRAINT ck_app_user_establishment CHECK ((role = 'SUPERADMIN') = (establishment_id IS NULL))
);
CREATE INDEX IF NOT EXISTS ix_app_user_establishment ON app_user (establishment_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_email_lower ON app_user (lower(email));

-- Catálogos (É2)
CREATE TABLE IF NOT EXISTS surgery_type (
    id     BIGSERIAL   PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    establishment_id BIGINT NOT NULL REFERENCES establishment (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_surgery_type_name_lower ON surgery_type (establishment_id, lower(name));

CREATE TABLE IF NOT EXISTS scar_type (
    id     BIGSERIAL   PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    establishment_id BIGINT NOT NULL REFERENCES establishment (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_scar_type_name_lower ON scar_type (establishment_id, lower(name));

CREATE TABLE IF NOT EXISTS body_segment (
    id     BIGSERIAL   PRIMARY KEY,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    establishment_id BIGINT NOT NULL REFERENCES establishment (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_body_segment_name_lower ON body_segment (establishment_id, lower(name));

-- Plantillas de evaluación (É3)
CREATE TABLE IF NOT EXISTS evaluation_template (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    body_segment_id BIGINT       NOT NULL REFERENCES body_segment (id),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    establishment_id BIGINT NOT NULL REFERENCES establishment (id)
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
    establishment_id BIGINT NOT NULL REFERENCES establishment (id),
    CONSTRAINT ck_question_condition_with_parent CHECK (
        (parent_question_id IS NULL AND condition_operator IS NULL AND condition_expected_value IS NULL)
        OR (parent_question_id IS NOT NULL AND condition_operator IS NOT NULL AND condition_expected_value IS NOT NULL)),
    CONSTRAINT ck_question_range CHECK (min_value IS NULL OR max_value IS NULL OR min_value <= max_value)
);
CREATE INDEX IF NOT EXISTS ix_evaluation_template_establishment ON evaluation_template (establishment_id);
CREATE INDEX IF NOT EXISTS ix_question_template ON question (template_id, display_order);
CREATE INDEX IF NOT EXISTS ix_question_parent ON question (parent_question_id);

CREATE TABLE IF NOT EXISTS question_option (
    id            BIGSERIAL    PRIMARY KEY,
    question_id   BIGINT       NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    label         VARCHAR(120) NOT NULL,
    display_order INTEGER      NOT NULL,
    establishment_id BIGINT NOT NULL REFERENCES establishment (id)
);
-- Labels are unique per question (case-insensitive), enforced by the app so options can be edited in place.
CREATE INDEX IF NOT EXISTS ix_question_option_question ON question_option (question_id, display_order);

-- Métricas ROM por segmento (É11)
CREATE TABLE IF NOT EXISTS segment_metric (
    id               BIGSERIAL    PRIMARY KEY,
    establishment_id BIGINT       NOT NULL REFERENCES establishment (id),
    body_segment_id  BIGINT       NOT NULL REFERENCES body_segment (id),
    name             VARCHAR(120) NOT NULL,
    answer_type      VARCHAR(20)  NOT NULL CHECK (answer_type IN ('NUMERIC', 'YES_NO', 'OPTION', 'TEXT')),
    required         BOOLEAN      NOT NULL DEFAULT FALSE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order    INTEGER      NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_segment_metric_segment_name ON segment_metric (body_segment_id, lower(name));
CREATE INDEX IF NOT EXISTS ix_segment_metric_segment ON segment_metric (body_segment_id, display_order);

-- Labels are unique per metric (case-insensitive), enforced by the app so options can be edited in place.
CREATE TABLE IF NOT EXISTS segment_metric_option (
    id                BIGSERIAL    PRIMARY KEY,
    establishment_id  BIGINT       NOT NULL REFERENCES establishment (id),
    segment_metric_id BIGINT       NOT NULL REFERENCES segment_metric (id) ON DELETE CASCADE,
    label             VARCHAR(120) NOT NULL,
    display_order     INTEGER      NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_segment_metric_option_metric ON segment_metric_option (segment_metric_id, display_order);

-- Pacientes (É4)
CREATE TABLE IF NOT EXISTS patient (
    id                      BIGSERIAL    PRIMARY KEY,
    dni                     VARCHAR(12)  NOT NULL,
    first_name              VARCHAR(80)  NOT NULL,
    last_name               VARCHAR(80)  NOT NULL,
    birth_date              DATE         NOT NULL,
    phone                   VARCHAR(40)  NOT NULL,
    occupation              VARCHAR(120) NOT NULL,
    health_insurance        VARCHAR(120) NOT NULL,
    health_insurance_number VARCHAR(60)  NOT NULL,
    dominance               VARCHAR(10)  NOT NULL CHECK (dominance IN ('LEFT', 'RIGHT')),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    establishment_id BIGINT NOT NULL REFERENCES establishment (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_patient_dni ON patient (establishment_id, dni);
CREATE INDEX IF NOT EXISTS ix_patient_name ON patient (lower(last_name), lower(first_name));

-- Diagnósticos y tratamientos (É5)
CREATE TABLE IF NOT EXISTS treatment (
    id             BIGSERIAL   PRIMARY KEY,
    treatment_type VARCHAR(20) NOT NULL CHECK (treatment_type IN ('SURGICAL', 'CONSERVATIVE')),
    establishment_id BIGINT NOT NULL REFERENCES establishment (id)
);

CREATE TABLE IF NOT EXISTS surgical_treatment (
    treatment_id          BIGINT PRIMARY KEY REFERENCES treatment (id),
    surgery_date          DATE   NOT NULL,
    stitches_removal_date DATE   NOT NULL,
    surgery_type_id       BIGINT NOT NULL REFERENCES surgery_type (id),
    CONSTRAINT ck_surgical_treatment_dates CHECK (stitches_removal_date >= surgery_date)
);

CREATE TABLE IF NOT EXISTS conservative_treatment (
    treatment_id        BIGINT  PRIMARY KEY REFERENCES treatment (id),
    has_immobilization  BOOLEAN NOT NULL DEFAULT TRUE,
    immobilization_date DATE,
    CONSTRAINT ck_conservative_immobilization CHECK (
        (has_immobilization AND immobilization_date IS NOT NULL)
        OR (NOT has_immobilization AND immobilization_date IS NULL))
);

CREATE TABLE IF NOT EXISTS diagnosis (
    id           BIGSERIAL PRIMARY KEY,
    patient_id   BIGINT    NOT NULL REFERENCES patient (id),
    description  TEXT      NOT NULL,
    created_date DATE      NOT NULL,
    treatment_id BIGINT    NOT NULL UNIQUE REFERENCES treatment (id),
    establishment_id BIGINT NOT NULL REFERENCES establishment (id)
);
CREATE INDEX IF NOT EXISTS ix_diagnosis_patient ON diagnosis (patient_id, created_date DESC);

-- Seguimientos, evaluaciones por segmento y respuestas de plantilla (É6)
CREATE TABLE IF NOT EXISTS follow_up (
    id             BIGSERIAL PRIMARY KEY,
    diagnosis_id   BIGINT    NOT NULL REFERENCES diagnosis (id),
    follow_up_date DATE      NOT NULL,
    scar_type_id   BIGINT    REFERENCES scar_type (id),
    establishment_id BIGINT NOT NULL REFERENCES establishment (id)
);
CREATE INDEX IF NOT EXISTS ix_follow_up_diagnosis ON follow_up (diagnosis_id, follow_up_date DESC);

CREATE TABLE IF NOT EXISTS segment_evaluation (
    id              BIGSERIAL     PRIMARY KEY,
    follow_up_id    BIGINT        NOT NULL REFERENCES follow_up (id),
    display_order   INTEGER       NOT NULL,
    body_segment_id BIGINT        NOT NULL REFERENCES body_segment (id),
    edema           NUMERIC(6, 2),
    pain            INTEGER       NOT NULL CHECK (pain BETWEEN 0 AND 10),
    rom             NUMERIC(6, 2),
    strength        NUMERIC(6, 2) NOT NULL,
    template_id     BIGINT        REFERENCES evaluation_template (id),
    establishment_id BIGINT NOT NULL REFERENCES establishment (id),
    CONSTRAINT uq_segment_evaluation_segment UNIQUE (follow_up_id, body_segment_id)
);

CREATE TABLE IF NOT EXISTS answer (
    id                    BIGSERIAL      PRIMARY KEY,
    segment_evaluation_id BIGINT         NOT NULL REFERENCES segment_evaluation (id),
    question_id           BIGINT         NOT NULL REFERENCES question (id),
    numeric_value         NUMERIC(12, 3),
    boolean_value         BOOLEAN,
    option_value          VARCHAR(120),
    text_value            TEXT,
    establishment_id BIGINT NOT NULL REFERENCES establishment (id),
    CONSTRAINT uq_answer_question UNIQUE (segment_evaluation_id, question_id),
    CONSTRAINT ck_answer_single_value CHECK (num_nonnulls(numeric_value, boolean_value, option_value, text_value) = 1)
);
CREATE INDEX IF NOT EXISTS ix_answer_question ON answer (question_id);

-- É11: valores de las métricas ROM de cada evaluación de segmento (reemplazan el ROM fijo, que queda como historia).
CREATE TABLE IF NOT EXISTS metric_value (
    id                    BIGSERIAL      PRIMARY KEY,
    establishment_id      BIGINT         NOT NULL REFERENCES establishment (id),
    segment_evaluation_id BIGINT         NOT NULL REFERENCES segment_evaluation (id),
    segment_metric_id     BIGINT         NOT NULL REFERENCES segment_metric (id),
    numeric_value         NUMERIC(12, 3),
    boolean_value         BOOLEAN,
    option_value          VARCHAR(120),
    text_value            TEXT,
    CONSTRAINT uq_metric_value_metric UNIQUE (segment_evaluation_id, segment_metric_id),
    CONSTRAINT ck_metric_value_single_value CHECK (num_nonnulls(numeric_value, boolean_value, option_value, text_value) = 1)
);
CREATE INDEX IF NOT EXISTS ix_metric_value_metric ON metric_value (segment_metric_id);

-- Facturación de sesiones (É12)
CREATE TABLE IF NOT EXISTS billing (
    id               BIGSERIAL      PRIMARY KEY,
    establishment_id BIGINT         NOT NULL REFERENCES establishment (id),
    diagnosis_id     BIGINT         NOT NULL REFERENCES diagnosis (id),
    billed_on        DATE           NOT NULL,
    amount           NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    created_at       TIMESTAMPTZ    NOT NULL,
    voided_at        TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS ix_billing_diagnosis ON billing (diagnosis_id, billed_on DESC);

CREATE TABLE IF NOT EXISTS billing_follow_up (
    billing_id       BIGINT  NOT NULL REFERENCES billing (id),
    follow_up_id     BIGINT  NOT NULL REFERENCES follow_up (id),
    establishment_id BIGINT  NOT NULL REFERENCES establishment (id),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (billing_id, follow_up_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_billing_follow_up_active ON billing_follow_up (follow_up_id) WHERE active;

-- É7: turnos. Las restricciones de exclusión impiden solapamientos por terapeuta y por paciente.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS appointment (
    id           BIGSERIAL    PRIMARY KEY,
    therapist_id BIGINT       NOT NULL REFERENCES app_user (id),
    patient_id   BIGINT       NOT NULL REFERENCES patient (id),
    diagnosis_id BIGINT       REFERENCES diagnosis (id),
    start_at     TIMESTAMP    NOT NULL,
    end_at       TIMESTAMP    NOT NULL,
    notes        VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL,
    establishment_id BIGINT NOT NULL REFERENCES establishment (id),
    CONSTRAINT ck_appointment_range CHECK (end_at > start_at),
    CONSTRAINT ex_appointment_therapist_overlap EXCLUDE USING gist (therapist_id WITH =, tsrange(start_at, end_at) WITH &&),
    CONSTRAINT ex_appointment_patient_overlap EXCLUDE USING gist (patient_id WITH =, tsrange(start_at, end_at) WITH &&)
);
CREATE INDEX IF NOT EXISTS ix_appointment_start ON appointment (start_at);
CREATE INDEX IF NOT EXISTS ix_appointment_establishment_start ON appointment (establishment_id, start_at);
