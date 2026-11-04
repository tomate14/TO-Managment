package com.tomanagement.diagnosis.dto;

import java.time.LocalDate;

public record DiagnosisResponse(
        Long id,
        PatientRef patient,
        String description,
        LocalDate createdDate,
        TreatmentResponse treatment) {

    @Override
    public String toString() {
        return "DiagnosisResponse[id=" + id + ", ***]";
    }
}
