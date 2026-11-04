package com.tomanagement.diagnosis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * New diagnosis (H5.1). Clinical text never appears in {@link #toString()}.
 */
public record CreateDiagnosisRequest(
        @NotBlank(message = "Ingresá la descripción del diagnóstico.")
        @Size(max = 2000, message = "La descripción es demasiado larga.") String description,
        @NotNull(message = "Elegí el tipo de tratamiento.") @Valid TreatmentRequest treatment) {

    public CreateDiagnosisRequest {
        description = description == null ? null : description.trim();
    }

    @Override
    public String toString() {
        return "CreateDiagnosisRequest[***]";
    }
}
