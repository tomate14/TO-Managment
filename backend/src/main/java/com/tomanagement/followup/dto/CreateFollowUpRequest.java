package com.tomanagement.followup.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * A whole follow-up, saved in one operation (H6.1).
 *
 * @param scarTypeId required when the treatment is surgical, absent when it is conservative
 */
public record CreateFollowUpRequest(
        @NotNull(message = "Ingresá la fecha del seguimiento.") LocalDate date,
        Long scarTypeId,
        @NotEmpty(message = "Agregá al menos una evaluación de segmento.")
        List<@NotNull @Valid SegmentEvaluationRequest> evaluations) {
}
