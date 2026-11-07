-- É9: independent therapists. An establishment is an institution or a one-person practice, and practices need approval.
ALTER TABLE establishment
    ADD COLUMN kind varchar(20) NOT NULL DEFAULT 'INSTITUTION',
    ADD COLUMN approved_at timestamptz NULL;
ALTER TABLE establishment
    ADD CONSTRAINT ck_establishment_kind CHECK (kind IN ('INSTITUTION', 'INDIVIDUAL'));
UPDATE establishment SET approved_at = created_at WHERE approved_at IS NULL;
