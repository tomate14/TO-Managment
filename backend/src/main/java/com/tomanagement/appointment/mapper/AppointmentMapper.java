package com.tomanagement.appointment.mapper;

import java.time.Duration;

import com.tomanagement.appointment.dto.AppointmentResponse;
import com.tomanagement.appointment.dto.AppointmentResponse.DiagnosisRef;
import com.tomanagement.appointment.dto.AppointmentResponse.TherapistRef;
import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.appointment.entity.TherapistColor;
import com.tomanagement.diagnosis.dto.PatientRef;
import com.tomanagement.user.entity.User;

import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment appointment) {
        User therapist = appointment.getTherapist();
        return new AppointmentResponse(
                appointment.getId(),
                new TherapistRef(therapist.getId(), therapist.getFullName(),
                        TherapistColor.forUser(therapist.getId()).getIndex()),
                PatientRef.from(appointment.getPatient()),
                appointment.getDiagnosis()
                        .map(diagnosis -> new DiagnosisRef(diagnosis.getId(), diagnosis.getDescription()))
                        .orElse(null),
                appointment.getStartAt().toLocalDate(),
                appointment.getStartAt().toLocalTime(),
                appointment.getEndAt().toLocalTime(),
                (int) Duration.between(appointment.getStartAt(), appointment.getEndAt()).toMinutes(),
                appointment.getNotes().orElse(null),
                appointment.getConfirmedAt(),
                false);
    }
}
