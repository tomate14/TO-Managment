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
