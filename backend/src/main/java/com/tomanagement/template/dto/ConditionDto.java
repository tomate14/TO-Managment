package com.tomanagement.template.dto;

import com.tomanagement.template.entity.ActivationConditionData;
import com.tomanagement.template.entity.ConditionOperator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Activation condition of a chained question. Yes/no values travel as "true" / "false".
 */
public record ConditionDto(
        @NotNull(message = "Elegí una condición.") ConditionOperator operator,
        @NotBlank(message = "Ingresá el valor esperado.") @Size(max = 120, message = "El valor es demasiado largo.")
        String expectedValue) {

    public static ConditionDto from(ActivationConditionData data) {
        return new ConditionDto(data.getOperator(), data.getExpectedValue());
    }
}
