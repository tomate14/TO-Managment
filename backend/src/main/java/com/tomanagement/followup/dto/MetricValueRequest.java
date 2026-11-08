package com.tomanagement.followup.dto;

import java.math.BigDecimal;

import com.tomanagement.template.rules.RawAnswer;

import jakarta.validation.constraints.NotNull;

/**
 * The value of one segment ROM metric (É11): only the value matching the metric type is sent.
 */
public record MetricValueRequest(
        @NotNull(message = "Falta la métrica del valor.") Long metricId,
        BigDecimal numericValue,
        Boolean booleanValue,
        String optionValue,
        String textValue) {

    public RawAnswer toRawAnswer() {
        return new RawAnswer(numericValue, booleanValue, optionValue, textValue);
    }

    @Override
    public String toString() {
        return "MetricValueRequest[metricId=" + metricId + ", ***]";
    }
}
