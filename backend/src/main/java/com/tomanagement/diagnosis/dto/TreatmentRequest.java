package com.tomanagement.diagnosis.dto;

import java.time.LocalDate;

import com.tomanagement.diagnosis.entity.TreatmentType;

import jakarta.validation.constraints.NotNull;

/**
 * Treatment data of a new diagnosis. Which fields are required depends on the type; the treatment itself checks
 * them (SURGERY_DATA_REQUIRED, IMMOBILIZATION_DATE_REQUIRED). Fields of the other type are ignored.
 */
public record TreatmentRequest(
        @NotNull(message = "Elegí el tipo de tratamiento.") TreatmentType type,
        LocalDate surgeryDate,
        LocalDate stitchesRemovalDate,
        Long surgeryTypeId,
        LocalDate immobilizationDate) {
}
