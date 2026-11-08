package com.tomanagement.followup.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * One segment evaluation (H6.2) with the values of the segment ROM metrics (É11) and the optional template answers
 * (H6.3). Ranges are checked by the follow-up validation chain, so the messages come from {@code ClinicalRange}.
 *
 * @param edema   optional
 * @param answers only allowed with a template; null means no answers
 * @param metrics values of the active metrics of the segment; null means none
 */
public record SegmentEvaluationRequest(
        @NotNull(message = "Elegí el segmento.") Long segmentId,
        BigDecimal edema,
        @NotNull(message = "Ingresá el dolor.") Integer pain,
        @NotNull(message = "Ingresá la fuerza.") BigDecimal strength,
        Long templateId,
        List<@NotNull @Valid AnswerRequest> answers,
        List<@NotNull @Valid MetricValueRequest> metrics) {

    public List<AnswerRequest> answersOrEmpty() {
        return answers == null ? List.of() : answers;
    }

    public List<MetricValueRequest> metricsOrEmpty() {
        return metrics == null ? List.of() : metrics;
    }
}
