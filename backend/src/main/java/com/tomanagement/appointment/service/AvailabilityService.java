package com.tomanagement.appointment.service;

import java.time.LocalDate;
import java.util.List;

import com.tomanagement.appointment.dto.SlotResponse;
import com.tomanagement.appointment.entity.AppointmentDuration;
import com.tomanagement.auth.security.AuthenticatedUser;

public interface AvailabilityService {

    /**
     * Every start from OPENING to CLOSING minus the duration, every AgendaSlot.STEP minutes; empty on closed days.
     * {@code patientId} and {@code excludeAppointmentId} are optional.
     */
    List<SlotResponse> slots(AuthenticatedUser actor, Long therapistId, Long patientId, LocalDate date,
            AppointmentDuration duration, Long excludeAppointmentId);
}
