-- É11: métricas ROM por segmento, valores por evaluación e inmovilización opcional.
CREATE TABLE segment_metric (
    id               BIGSERIAL    PRIMARY KEY,
    establishment_id BIGINT       NOT NULL REFERENCES establishment (id),
    body_segment_id  BIGINT       NOT NULL REFERENCES body_segment (id),
    name             VARCHAR(120) NOT NULL,
    answer_type      VARCHAR(20)  NOT NULL CHECK (answer_type IN ('NUMERIC', 'YES_NO', 'OPTION', 'TEXT')),
    required         BOOLEAN      NOT NULL DEFAULT FALSE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order    INTEGER      NOT NULL
);
CREATE UNIQUE INDEX ux_segment_metric_segment_name ON segment_metric (body_segment_id, lower(name));
CREATE INDEX ix_segment_metric_segment ON segment_metric (body_segment_id, display_order);

-- Labels are unique per metric (case-insensitive), enforced by the app so options can be edited in place.
CREATE TABLE segment_metric_option (
    id                BIGSERIAL    PRIMARY KEY,
    establishment_id  BIGINT       NOT NULL REFERENCES establishment (id),
    segment_metric_id BIGINT       NOT NULL REFERENCES segment_metric (id) ON DELETE CASCADE,
    label             VARCHAR(120) NOT NULL,
    display_order     INTEGER      NOT NULL
);
CREATE INDEX ix_segment_metric_option_metric ON segment_metric_option (segment_metric_id, display_order);

-- Values of the metrics per segment evaluation; the fixed ROM stays only as legacy history.
ALTER TABLE segment_evaluation ALTER COLUMN rom DROP NOT NULL;

CREATE TABLE metric_value (
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
CREATE INDEX ix_metric_value_metric ON metric_value (segment_metric_id);

-- A conservative treatment may have no immobilization: then there is no date.
ALTER TABLE conservative_treatment ADD COLUMN has_immobilization BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE conservative_treatment ALTER COLUMN immobilization_date DROP NOT NULL;
ALTER TABLE conservative_treatment ADD CONSTRAINT ck_conservative_immobilization
    CHECK ((has_immobilization AND immobilization_date IS NOT NULL)
        OR (NOT has_immobilization AND immobilization_date IS NULL));
