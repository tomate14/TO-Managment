-- É10: trial period. NULL = no expiry; existing establishments stay without one.
ALTER TABLE establishment ADD COLUMN trial_ends_at timestamptz NULL;
