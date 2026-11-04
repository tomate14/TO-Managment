package com.tomanagement.template.condition;

import java.util.Optional;

public final class EqualsCondition implements ActivationCondition {

    private final String expectedValue;

    public EqualsCondition(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean isSatisfiedBy(Optional<AnswerValue> parentAnswer) {
        return parentAnswer.map(answer -> answer.matches(expectedValue)).orElse(false);
    }
}
