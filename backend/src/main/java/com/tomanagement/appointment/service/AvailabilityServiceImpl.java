package com.tomanagement.appointment.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.tomanagement.appointment.dto.SlotResponse;
import com.tomanagement.appointment.dto.SlotUnavailableReason;
import com.tomanagement.appointment.entity.AgendaSchedule;
import com.tomanagement.appointment.entity.AgendaSlot;
import com.tomanagement.appointment.entity.Appointment;
import com.tomanagement.appointment.entity.AppointmentDuration;
import com.tomanagement.appointment.repository.AppointmentRepository;
import com.tomanagement.appointment.visibility.AppointmentVisibilityPolicyFactory;
import com.tomanagement.auth.security.AuthenticatedUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityServiceImpl implements AvailabilityService {

    private final AppointmentRepository repository;
    private final AppointmentVisibilityPolicyFactory policyFactory;
    private final Clock clock;

    public AvailabilityServiceImpl(AppointmentRepository repository, AppointmentVisibilityPolicyFactory policyFactory,
            Clock clock) {
        this.repository = repository;
        this.policyFactory = policyFactory;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlotResponse> slots(AuthenticatedUser actor, Long therapistId, Long patientId, LocalDate date,
            AppointmentDuration duration, Long excludeAppointmentId) {
        policyFactory.forRole(actor.role()).checkCanManage(actor, therapistId);
        if (!AgendaSchedule.isOpenDay(date.getDayOfWeek())) {
            return List.of();
        }
        LocalTime opening = AgendaSchedule.OPENING.getTime();
        LocalTime lastStart = AgendaSchedule.CLOSING.getTime().minusMinutes(duration.getMinutes());
        List<Appointment> loaded = repository.findOverlappingForTherapistOrPatient(therapistId, patientId,
                date.atTime(opening), date.atTime(AgendaSchedule.CLOSING.getTime()), excludeAppointmentId);
        List<Appointment> ofTherapist = loaded.stream()
                .filter(appointment -> Objects.equals(appointment.getTherapist().getId(), therapistId)).toList();
        List<Appointment> ofPatient = patientId == null ? List.of() : loaded.stream()
                .filter(appointment -> Objects.equals(appointment.getPatient().getId(), patientId)).toList();
        LocalDateTime now = LocalDateTime.now(clock);

        List<SlotResponse> slots = new ArrayList<>();
        for (LocalTime time = opening; !time.isAfter(lastStart);
                time = time.plusMinutes(AgendaSlot.STEP.getMinutes())) {
            LocalDateTime start = date.atTime(time);
            LocalDateTime end = start.plusMinutes(duration.getMinutes());
            SlotUnavailableReason reason = reasonFor(start, end, now, ofTherapist, ofPatient);
            slots.add(new SlotResponse(time, reason == null, reason));
        }
        return slots;
    }

    private SlotUnavailableReason reasonFor(LocalDateTime start, LocalDateTime end, LocalDateTime now,
            List<Appointment> ofTherapist, List<Appointment> ofPatient) {
        if (start.isBefore(now)) {
            return SlotUnavailableReason.PAST;
        }
        if (overlapsAny(ofTherapist, start, end)) {
            return SlotUnavailableReason.THERAPIST_BUSY;
        }
        if (overlapsAny(ofPatient, start, end)) {
            return SlotUnavailableReason.PATIENT_BUSY;
        }
        return null;
    }

    private boolean overlapsAny(List<Appointment> appointments, LocalDateTime start, LocalDateTime end) {
        return appointments.stream()
                .anyMatch(appointment -> appointment.getStartAt().isBefore(end) && appointment.getEndAt().isAfter(start));
    }
}
