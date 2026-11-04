package com.tomanagement.template.condition;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import com.tomanagement.template.entity.ActivationConditionData;
import com.tomanagement.template.entity.ConditionOperator;

import org.junit.jupiter.api.Test;

class ActivationConditionTest {

    @Test
    void equalsOnYesNo() {
        ActivationCondition showIfYes = condition(ConditionOperator.EQUALS, "true");

        assertThat(showIfYes.isSatisfiedBy(Optional.of(AnswerValue.ofYesNo(true)))).isTrue();
        assertThat(showIfYes.isSatisfiedBy(Optional.of(AnswerValue.ofYesNo(false)))).isFalse();
    }

    @Test
    void anUnansweredParentNeverActivatesItsChildren() {
        assertThat(condition(ConditionOperator.EQUALS, "true").isSatisfiedBy(Optional.empty())).isFalse();
        assertThat(condition(ConditionOperator.NOT_EQUALS, "true").isSatisfiedBy(Optional.empty())).isFalse();
        assertThat(condition(ConditionOperator.GREATER_THAN, "7").isSatisfiedBy(Optional.empty())).isFalse();
    }

    @Test
    void notEqualsOnYesNo() {
        ActivationCondition showIfNotYes = condition(ConditionOperator.NOT_EQUALS, "true");

        assertThat(showIfNotYes.isSatisfiedBy(Optional.of(AnswerValue.ofYesNo(false)))).isTrue();
        assertThat(showIfNotYes.isSatisfiedBy(Optional.of(AnswerValue.ofYesNo(true)))).isFalse();
    }

    @Test
    void numbersCompareByValue() {
        Optional<AnswerValue> eight = Optional.of(AnswerValue.ofNumber(new BigDecimal("8.00")));

        assertThat(condition(ConditionOperator.EQUALS, "8").isSatisfiedBy(eight)).isTrue();
        assertThat(condition(ConditionOperator.GREATER_THAN, "7").isSatisfiedBy(eight)).isTrue();
        assertThat(condition(ConditionOperator.GREATER_THAN, "8").isSatisfiedBy(eight)).isFalse();
        assertThat(condition(ConditionOperator.LESS_THAN, "8.5").isSatisfiedBy(eight)).isTrue();
        assertThat(condition(ConditionOperator.LESS_THAN, "8").isSatisfiedBy(eight)).isFalse();
    }

    @Test
    void optionsAndTextIgnoreCaseAndSpaces() {
        assertThat(condition(ConditionOperator.EQUALS, "Cilíndrico")
                .isSatisfiedBy(Optional.of(AnswerValue.ofOption("cilíndrico")))).isTrue();
        assertThat(condition(ConditionOperator.NOT_EQUALS, "Esférico")
                .isSatisfiedBy(Optional.of(AnswerValue.ofOption("Cilíndrico")))).isTrue();
        assertThat(condition(ConditionOperator.EQUALS, "nada")
                .isSatisfiedBy(Optional.of(AnswerValue.ofText("  Nada "))))
                .isTrue();
    }

    @Test
    void numericComparisonsNeverMatchNonNumericAnswers() {
        assertThat(condition(ConditionOperator.GREATER_THAN, "1")
                .isSatisfiedBy(Optional.of(AnswerValue.ofYesNo(true)))).isFalse();
    }

    private static ActivationCondition condition(ConditionOperator operator, String expected) {
        return ActivationConditionFactory.from(new ActivationConditionData(operator, expected));
    }
}
