package com.tomanagement.template.condition;

import com.tomanagement.template.entity.ActivationConditionData;

/**
 * Builds the specification for a persisted condition (one implementation per operator).
 */
public final class ActivationConditionFactory {

    private ActivationConditionFactory() {
    }

    public static ActivationCondition from(ActivationConditionData data) {
        String expected = data.getExpectedValue();
        return switch (data.getOperator()) {
            case EQUALS -> new EqualsCondition(expected);
            case NOT_EQUALS -> new NotEqualsCondition(expected);
            case GREATER_THAN -> NumericComparisonCondition.greaterThan(expected);
            case LESS_THAN -> NumericComparisonCondition.lessThan(expected);
        };
    }
}
