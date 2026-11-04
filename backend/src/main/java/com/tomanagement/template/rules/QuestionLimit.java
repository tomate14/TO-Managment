package com.tomanagement.template.rules;

/**
 * Fixed limits of the question rules (spec H3.2).
 */
public enum QuestionLimit {
    MINIMUM_OPTIONS(2);

    private final int value;

    QuestionLimit(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
