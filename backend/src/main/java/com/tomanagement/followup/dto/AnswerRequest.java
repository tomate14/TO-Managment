package com.tomanagement.followup.dto;

import java.math.BigDecimal;

import com.tomanagement.template.rules.RawAnswer;

import jakarta.validation.constraints.NotNull;

/**
 * The answer to one template question: only the value matching the question type is sent.
 */
public record AnswerRequest(
        @NotNull(message = "Falta la pregunta de la respuesta.") Long questionId,
        BigDecimal numericValue,
        Boolean booleanValue,
        String optionValue,
        String textValue) {

    public RawAnswer toRawAnswer() {
        return new RawAnswer(numericValue, booleanValue, optionValue, textValue);
    }

    @Override
    public String toString() {
        return "AnswerRequest[questionId=" + questionId + ", ***]";
    }
}
