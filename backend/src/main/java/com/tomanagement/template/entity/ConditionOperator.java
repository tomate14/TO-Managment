package com.tomanagement.template.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Operators of an activation condition, with the parent answer types each one can compare.
 */
public enum ConditionOperator {
    EQUALS(EnumSet.allOf(AnswerType.class)),
    NOT_EQUALS(EnumSet.allOf(AnswerType.class)),
    GREATER_THAN(EnumSet.of(AnswerType.NUMERIC)),
    LESS_THAN(EnumSet.of(AnswerType.NUMERIC));

    private final Set<AnswerType> supportedTypes;

    ConditionOperator(Set<AnswerType> supportedTypes) {
        this.supportedTypes = supportedTypes;
    }

    public boolean supports(AnswerType parentType) {
        return supportedTypes.contains(parentType);
    }
}
