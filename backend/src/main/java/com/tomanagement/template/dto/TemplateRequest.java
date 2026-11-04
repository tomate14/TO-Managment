package com.tomanagement.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemplateRequest(
        @NotBlank(message = "Ingresá el nombre.") @Size(max = 120, message = "El nombre es demasiado largo.")
        String name,
        @NotNull(message = "Elegí un segmento.") Long segmentId) {

    public TemplateRequest {
        name = name == null ? null : name.trim();
    }
}
