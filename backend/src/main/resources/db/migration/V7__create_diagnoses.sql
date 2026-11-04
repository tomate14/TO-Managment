-- É5: diagnoses and their treatment (JPA JOINED inheritance: one table per treatment type).
CREATE TABLE treatment (
    id             BIGSERIAL   PRIMARY KEY,
    treatment_type VARCHAR(20) NOT NULL CHECK (treatment_type IN ('SURGICAL', 'CONSERVATIVE'))
);

CREATE TABLE surgical_treatment (
    treatment_id          BIGINT PRIMARY KEY REFERENCES treatment (id),
    surgery_date          DATE   NOT NULL,
    stitches_removal_date DATE   NOT NULL,
    surgery_type_id       BIGINT NOT NULL REFERENCES surgery_type (id),
    CONSTRAINT ck_surgical_treatment_dates CHECK (stitches_removal_date >= surgery_date)
);

CREATE TABLE conservative_treatment (
    treatment_id        BIGINT PRIMARY KEY REFERENCES treatment (id),
    immobilization_date DATE   NOT NULL
);

CREATE TABLE diagnosis (
    id           BIGSERIAL PRIMARY KEY,
    patient_id   BIGINT    NOT NULL REFERENCES patient (id),
    description  TEXT      NOT NULL,
    created_date DATE      NOT NULL,
    treatment_id BIGINT    NOT NULL UNIQUE REFERENCES treatment (id)
);
CREATE INDEX ix_diagnosis_patient ON diagnosis (patient_id, created_date DESC);
