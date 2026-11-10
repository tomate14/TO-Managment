package com.tomanagement.appointment.dto;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;

import com.tomanagement.diagnosis.dto.PatientRef;

public record AppointmentResponse(Long id, TherapistRef therapist, PatientRef patient, DiagnosisRef diagnosis,
        LocalDate date, LocalTime startTime, LocalTime endTime, int durationMinutes, String notes,
        Instant confirmedAt, boolean billed) {

    public record TherapistRef(Long id, String fullName, int color) {
    }

    public record DiagnosisRef(Long id, String description) {

        @Override
        public String toString() {
            return "DiagnosisRef[id=" + id + ", ***]";
        }
    }

    @Override
    public String toString() {
        return "AppointmentResponse[id=" + id + ", ***]";
    }
}
