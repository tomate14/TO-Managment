package com.tomanagement.template.rules;

/**
 * Fixed limits of the question rules (spec H3.2) and of their answers (H6.3).
 */
public enum QuestionLimit {
    MINIMUM_OPTIONS(2),
    ANSWER_TEXT_MAX_LENGTH(2000);

    private final int value;

    QuestionLimit(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
