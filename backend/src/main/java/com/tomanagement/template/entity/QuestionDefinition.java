package com.tomanagement.template.entity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validated, normalized content of a question, ready to be applied with {@link Question#define(QuestionDefinition)}.
 */
public record QuestionDefinition(
        String text,
        AnswerType answerType,
        BigDecimal minValue,
        BigDecimal maxValue,
        List<String> optionLabels,
        Question parent,
        ActivationConditionData condition) {

    public QuestionDefinition {
        optionLabels = optionLabels == null ? List.of() : List.copyOf(optionLabels);
    }
}
