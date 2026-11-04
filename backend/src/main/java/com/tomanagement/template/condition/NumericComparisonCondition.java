package com.tomanagement.template.condition;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.IntPredicate;

/**
 * GREATER_THAN / LESS_THAN: compares a numeric parent answer with the expected number.
 */
public final class NumericComparisonCondition implements ActivationCondition {

    private final Optional<BigDecimal> expected;
    private final IntPredicate acceptsComparison;

    private NumericComparisonCondition(String expectedValue, IntPredicate acceptsComparison) {
        this.expected = AnswerValue.parseNumber(expectedValue);
        this.acceptsComparison = acceptsComparison;
    }

    public static NumericComparisonCondition greaterThan(String expectedValue) {
        return new NumericComparisonCondition(expectedValue, comparison -> comparison > 0);
    }

    public static NumericComparisonCondition lessThan(String expectedValue) {
        return new NumericComparisonCondition(expectedValue, comparison -> comparison < 0);
    }

    @Override
    public boolean isSatisfiedBy(Optional<AnswerValue> parentAnswer) {
        return parentAnswer.flatMap(AnswerValue::number)
                .flatMap(number -> expected.map(limit -> acceptsComparison.test(number.compareTo(limit))))
                .orElse(false);
    }
}
