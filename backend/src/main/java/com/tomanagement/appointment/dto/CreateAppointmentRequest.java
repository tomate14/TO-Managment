package com.tomanagement.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAppointmentRequest(
        @NotNull(message = "Elegí el terapeuta.") Long therapistId,
        @NotNull(message = "Elegí el paciente.") Long patientId,
        Long diagnosisId,
        @NotNull(message = "Elegí la fecha.") LocalDate date,
        @NotNull(message = "Elegí el horario.") LocalTime startTime,
        @NotNull(message = "Elegí la duración.") Integer durationMinutes,
        @Size(max = 500, message = "Las notas pueden tener hasta 500 caracteres.") String notes) {

    @Override
    public String toString() {
        return "CreateAppointmentRequest[***]";
    }
}
