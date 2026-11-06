-- É8: multi-tenancy by establishment. Starts from an empty database (decided with the user).
TRUNCATE TABLE answer, segment_evaluation, follow_up, appointment, diagnosis, surgical_treatment, conservative_treatment,
    treatment, patient, question_option, question, evaluation_template, body_segment, surgery_type, scar_type, app_user
    RESTART IDENTITY CASCADE;

CREATE TABLE establishment (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX ux_establishment_name_lower ON establishment (lower(name));

ALTER TABLE app_user ADD COLUMN establishment_id BIGINT REFERENCES establishment (id);
ALTER TABLE app_user DROP CONSTRAINT app_user_role_check;
ALTER TABLE app_user ADD CONSTRAINT app_user_role_check CHECK (role IN ('SUPERADMIN', 'ADMIN', 'TERAPEUTA'));
ALTER TABLE app_user ADD CONSTRAINT ck_app_user_establishment CHECK ((role = 'SUPERADMIN') = (establishment_id IS NULL));
CREATE INDEX ix_app_user_establishment ON app_user (establishment_id);

-- every data table gets a NOT NULL establishment (tables are empty after the TRUNCATE above)
ALTER TABLE body_segment        ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE surgery_type        ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE scar_type           ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE evaluation_template ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE question            ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE question_option     ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE patient             ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE treatment           ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE diagnosis           ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE follow_up           ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE segment_evaluation  ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE answer              ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);
ALTER TABLE appointment         ADD COLUMN establishment_id BIGINT NOT NULL REFERENCES establishment (id);

DROP INDEX ux_surgery_type_name_lower;
DROP INDEX ux_scar_type_name_lower;
DROP INDEX ux_body_segment_name_lower;
DROP INDEX ux_patient_dni;
CREATE UNIQUE INDEX ux_surgery_type_name_lower ON surgery_type (establishment_id, lower(name));
CREATE UNIQUE INDEX ux_scar_type_name_lower    ON scar_type (establishment_id, lower(name));
CREATE UNIQUE INDEX ux_body_segment_name_lower ON body_segment (establishment_id, lower(name));
CREATE UNIQUE INDEX ux_patient_dni             ON patient (establishment_id, dni);
CREATE INDEX ix_evaluation_template_establishment ON evaluation_template (establishment_id);
CREATE INDEX ix_appointment_establishment_start   ON appointment (establishment_id, start_at);
