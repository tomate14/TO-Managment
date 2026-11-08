-- É12: billing of follow-up sessions per diagnosis. A follow-up is billed while it has an active link.
CREATE TABLE billing (
    id               BIGSERIAL      PRIMARY KEY,
    establishment_id BIGINT         NOT NULL REFERENCES establishment (id),
    diagnosis_id     BIGINT         NOT NULL REFERENCES diagnosis (id),
    billed_on        DATE           NOT NULL,
    amount           NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    created_at       TIMESTAMPTZ    NOT NULL,
    voided_at        TIMESTAMPTZ
);
CREATE INDEX ix_billing_diagnosis ON billing (diagnosis_id, billed_on DESC);

CREATE TABLE billing_follow_up (
    billing_id       BIGINT  NOT NULL REFERENCES billing (id),
    follow_up_id     BIGINT  NOT NULL REFERENCES follow_up (id),
    establishment_id BIGINT  NOT NULL REFERENCES establishment (id),
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (billing_id, follow_up_id)
);
CREATE UNIQUE INDEX ux_billing_follow_up_active ON billing_follow_up (follow_up_id) WHERE active;
