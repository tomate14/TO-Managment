package com.tomanagement.segmentmetric.dto;

import java.util.List;

import com.tomanagement.template.entity.AnswerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create or edit a metric. The answer type is required on create and ignored on update (it cannot change).
 */
public record SegmentMetricRequest(
        @NotBlank(message = "Ingresá el nombre de la métrica.") @Size(max = 120, message = "El nombre es demasiado largo.")
        String name,
        AnswerType answerType,
        boolean required,
        List<@NotBlank(message = "Las opciones no pueden quedar vacías.") @Size(max = 120, message = "La opción es demasiado larga.") String> options) {

    public SegmentMetricRequest {
        name = name == null ? null : name.trim();
        options = options == null ? List.of() : options.stream().map(option -> option == null ? null : option.trim()).toList();
    }
}
