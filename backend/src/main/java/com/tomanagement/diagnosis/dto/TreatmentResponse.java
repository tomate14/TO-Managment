package com.tomanagement.diagnosis.dto;

import java.time.LocalDate;

import com.tomanagement.catalog.dto.CatalogItemResponse;
import com.tomanagement.diagnosis.entity.TreatmentType;

/**
 * Treatment as shown to the UI: surgical fields are null for conservative treatments and vice versa; hasImmobilization is null for surgical ones.
 */
public record TreatmentResponse(
        TreatmentType type,
        LocalDate surgeryDate,
        LocalDate stitchesRemovalDate,
        CatalogItemResponse surgeryType,
        LocalDate immobilizationDate,
        Boolean hasImmobilization,
        boolean requiresScarType) {
}
