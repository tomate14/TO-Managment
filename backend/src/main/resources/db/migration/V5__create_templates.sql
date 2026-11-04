-- É3: evaluation templates, their questions (chained through a parent + activation condition) and options.
CREATE TABLE evaluation_template (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    body_segment_id BIGINT       NOT NULL REFERENCES body_segment (id),
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_evaluation_template_segment_name ON evaluation_template (body_segment_id, lower(name));

CREATE TABLE question (
    id                       BIGSERIAL     PRIMARY KEY,
    template_id              BIGINT        NOT NULL REFERENCES evaluation_template (id),
    text                     VARCHAR(255)  NOT NULL,
    answer_type              VARCHAR(20)   NOT NULL CHECK (answer_type IN ('NUMERIC', 'YES_NO', 'OPTION', 'TEXT')),
    display_order            INTEGER       NOT NULL,
    active                   BOOLEAN       NOT NULL DEFAULT TRUE,
    min_value                NUMERIC(12, 3),
    max_value                NUMERIC(12, 3),
    parent_question_id       BIGINT        REFERENCES question (id),
    condition_operator       VARCHAR(20)   CHECK (condition_operator IN ('EQUALS', 'NOT_EQUALS', 'GREATER_THAN', 'LESS_THAN')),
    condition_expected_value VARCHAR(120),
    CONSTRAINT ck_question_condition_with_parent CHECK (
        (parent_question_id IS NULL AND condition_operator IS NULL AND condition_expected_value IS NULL)
        OR (parent_question_id IS NOT NULL AND condition_operator IS NOT NULL AND condition_expected_value IS NOT NULL)),
    CONSTRAINT ck_question_range CHECK (min_value IS NULL OR max_value IS NULL OR min_value <= max_value)
);
CREATE INDEX ix_question_template ON question (template_id, display_order);
CREATE INDEX ix_question_parent ON question (parent_question_id);

CREATE TABLE question_option (
    id            BIGSERIAL    PRIMARY KEY,
    question_id   BIGINT       NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    label         VARCHAR(120) NOT NULL,
    display_order INTEGER      NOT NULL
);
-- Labels are unique per question (case-insensitive), enforced by the app so options can be edited in place.
CREATE INDEX ix_question_option_question ON question_option (question_id, display_order);
