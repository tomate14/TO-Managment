-- É4: patients, shared by every therapist. The DNI is stored as digits only and never changes.
CREATE TABLE patient (
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
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_patient_dni ON patient (dni);
CREATE INDEX ix_patient_name ON patient (lower(last_name), lower(first_name));
