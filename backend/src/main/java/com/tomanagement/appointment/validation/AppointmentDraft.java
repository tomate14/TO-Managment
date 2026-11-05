package com.tomanagement.appointment.validation;

import java.time.LocalDateTime;

import com.tomanagement.appointment.entity.AppointmentDuration;
import com.tomanagement.diagnosis.entity.Diagnosis;
import com.tomanagement.patient.entity.Patient;
import com.tomanagement.user.entity.User;

/**
 * What is being booked or rescheduled, already resolved to entities.
 *
 * @param diagnosis            optional, may be {@code null}
 * @param excludeAppointmentId the appointment being rescheduled, or {@code null} when booking
 */
public record AppointmentDraft(User therapist, Patient patient, Diagnosis diagnosis, LocalDateTime startAt,
        AppointmentDuration duration, Long excludeAppointmentId) {

    public LocalDateTime endAt() {
        return startAt.plusMinutes(duration.getMinutes());
    }
}
