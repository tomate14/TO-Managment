-- Patient billing keeps legacy follow-up links and adds confirmed appointments as the new source.
ALTER TABLE appointment ADD COLUMN confirmed_at TIMESTAMPTZ;
ALTER TABLE billing ADD COLUMN patient_id BIGINT REFERENCES patient (id);
UPDATE billing b SET patient_id = d.patient_id FROM diagnosis d WHERE d.id = b.diagnosis_id;
ALTER TABLE billing ALTER COLUMN patient_id SET NOT NULL;
ALTER TABLE billing ALTER COLUMN diagnosis_id DROP NOT NULL;
CREATE INDEX ix_billing_patient ON billing (patient_id, billed_on DESC);
CREATE INDEX ix_appointment_confirmed_patient ON appointment (patient_id, start_at, id)
    WHERE confirmed_at IS NOT NULL;

CREATE TABLE billing_appointment (
    billing_id       BIGINT  NOT NULL REFERENCES billing (id),
    appointment_id   BIGINT  NOT NULL REFERENCES appointment (id),
    establishment_id BIGINT  NOT NULL REFERENCES establishment (id),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (billing_id, appointment_id)
);
CREATE UNIQUE INDEX ux_billing_appointment_active ON billing_appointment (appointment_id) WHERE active;
