-- É7: appointments. Exclusion constraints keep one appointment per therapist and per patient at any time.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE appointment (
    id           BIGSERIAL    PRIMARY KEY,
    therapist_id BIGINT       NOT NULL REFERENCES app_user (id),
    patient_id   BIGINT       NOT NULL REFERENCES patient (id),
    diagnosis_id BIGINT       REFERENCES diagnosis (id),
    start_at     TIMESTAMP    NOT NULL,
    end_at       TIMESTAMP    NOT NULL,
    notes        VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_appointment_range CHECK (end_at > start_at),
    CONSTRAINT ex_appointment_therapist_overlap EXCLUDE USING gist (therapist_id WITH =, tsrange(start_at, end_at) WITH &&),
    CONSTRAINT ex_appointment_patient_overlap EXCLUDE USING gist (patient_id WITH =, tsrange(start_at, end_at) WITH &&)
);
CREATE INDEX ix_appointment_start ON appointment (start_at);
