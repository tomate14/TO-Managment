package com.tomanagement.appointment.mapper;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import com.tomanagement.appointment.dto.AppointmentResponse;
import com.tomanagement.appointment.dto.AppointmentResponse.DiagnosisRef;
import com.tomanagement.appointment.dto.AppointmentResponse.TherapistRef;
import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.appointment.entity.TherapistColor;
import com.tomanagement.appointment.service.BilledAppointmentLookup;
import com.tomanagement.diagnosis.dto.PatientRef;
import com.tomanagement.user.entity.User;

import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    private final BilledAppointmentLookup billedAppointmentLookup;

    public AppointmentMapper(BilledAppointmentLookup billedAppointmentLookup) {
        this.billedAppointmentLookup = billedAppointmentLookup;
    }

    public List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return List.of();
        }
        Set<Long> billed = billedAppointmentLookup
                .billedAmong(appointments.stream().map(Appointment::getId).toList());
        return appointments.stream().map(appointment -> toResponse(appointment, billed.contains(appointment.getId())))
                .toList();
    }

    public AppointmentResponse toResponse(Appointment appointment) {
        return toResponses(List.of(appointment)).getFirst();
    }

    private AppointmentResponse toResponse(Appointment appointment, boolean billed) {
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
                billed);
    }
}
