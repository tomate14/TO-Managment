-- É6: follow-ups of a diagnosis, their segment evaluations and the template answers (one column per value type).
CREATE TABLE follow_up (
    id             BIGSERIAL PRIMARY KEY,
    diagnosis_id   BIGINT    NOT NULL REFERENCES diagnosis (id),
    follow_up_date DATE      NOT NULL,
    scar_type_id   BIGINT    REFERENCES scar_type (id)
);
CREATE INDEX ix_follow_up_diagnosis ON follow_up (diagnosis_id, follow_up_date DESC);

CREATE TABLE segment_evaluation (
    id              BIGSERIAL     PRIMARY KEY,
    follow_up_id    BIGINT        NOT NULL REFERENCES follow_up (id),
    display_order   INTEGER       NOT NULL,
    body_segment_id BIGINT        NOT NULL REFERENCES body_segment (id),
    edema           NUMERIC(6, 2),
    pain            INTEGER       NOT NULL CHECK (pain BETWEEN 0 AND 10),
    rom             NUMERIC(6, 2) NOT NULL,
    strength        NUMERIC(6, 2) NOT NULL,
    template_id     BIGINT        REFERENCES evaluation_template (id),
    CONSTRAINT uq_segment_evaluation_segment UNIQUE (follow_up_id, body_segment_id)
);

CREATE TABLE answer (
    id                    BIGSERIAL      PRIMARY KEY,
    segment_evaluation_id BIGINT         NOT NULL REFERENCES segment_evaluation (id),
    question_id           BIGINT         NOT NULL REFERENCES question (id),
    numeric_value         NUMERIC(12, 3),
    boolean_value         BOOLEAN,
    option_value          VARCHAR(120),
    text_value            TEXT,
    CONSTRAINT uq_answer_question UNIQUE (segment_evaluation_id, question_id),
    CONSTRAINT ck_answer_single_value CHECK (num_nonnulls(numeric_value, boolean_value, option_value, text_value) = 1)
);
CREATE INDEX ix_answer_question ON answer (question_id);
