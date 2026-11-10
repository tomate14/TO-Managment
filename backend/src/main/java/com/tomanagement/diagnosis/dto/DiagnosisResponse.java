package com.tomanagement.diagnosis.dto;

import java.time.LocalDate;

public record DiagnosisResponse(
        Long id,
        PatientRef patient,
        String description,
        LocalDate createdDate,
        TreatmentResponse treatment,
        long followUpCount) {

    @Override
    public String toString() {
        return "DiagnosisResponse[id=" + id + ", ***]";
    }
}
