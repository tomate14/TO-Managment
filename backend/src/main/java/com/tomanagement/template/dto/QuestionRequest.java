package com.tomanagement.template.dto;

import java.math.BigDecimal;
import java.util.List;

import com.tomanagement.template.entity.AnswerType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Question content. Range only applies to NUMERIC and options only to OPTION (ignored otherwise). A condition is
 * required when there is a parent and ignored without one.
 */
public record QuestionRequest(
        @NotBlank(message = "Ingresá el texto de la pregunta.")
        @Size(max = 255, message = "El texto es demasiado largo.") String text,
        @NotNull(message = "Elegí el tipo de respuesta.") AnswerType answerType,
        @Digits(integer = 9, fraction = 3, message = "El mínimo no es un número válido.") BigDecimal minValue,
        @Digits(integer = 9, fraction = 3, message = "El máximo no es un número válido.") BigDecimal maxValue,
        List<@NotBlank(message = "Las opciones no pueden quedar vacías.")
             @Size(max = 120, message = "La opción es demasiado larga.") String> options,
        Long parentQuestionId,
        @Valid ConditionDto condition) {

    public QuestionRequest {
        text = text == null ? null : text.trim();
        options = options == null ? List.of() : options.stream().map(option -> option == null ? null : option.trim()).toList();
    }
}
