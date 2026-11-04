package com.tomanagement.template.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Persisted part of a chained question: operator and expected value (canonical text, see AnswerTypeRules).
 */
@Embeddable
public class ActivationConditionData {

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_operator", length = 20)
    private ConditionOperator operator;

    @Column(name = "condition_expected_value", length = 120)
    private String expectedValue;

    protected ActivationConditionData() {
        // Required by JPA.
    }

    public ActivationConditionData(ConditionOperator operator, String expectedValue) {
        this.operator = Objects.requireNonNull(operator, "operator is required");
        this.expectedValue = Objects.requireNonNull(expectedValue, "expectedValue is required");
    }

    public ConditionOperator getOperator() {
        return operator;
    }

    public String getExpectedValue() {
        return expectedValue;
    }
}
