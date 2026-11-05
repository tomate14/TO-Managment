package com.tomanagement.appointment.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.tomanagement.appointment.dto.AppointmentResponse;
import com.tomanagement.appointment.dto.CreateAppointmentRequest;
import com.tomanagement.appointment.dto.RescheduleAppointmentRequest;
import com.tomanagement.auth.security.AuthenticatedUser;

public interface AppointmentService {

    List<AppointmentResponse> list(AuthenticatedUser actor, LocalDate from, LocalDate to, Set<Long> therapistIds);

    /** 404 when missing; a therapist asking for another therapist's appointment gets ACCESS_DENIED. */
    AppointmentResponse get(AuthenticatedUser actor, Long id);

    AppointmentResponse book(AuthenticatedUser actor, CreateAppointmentRequest request);

    AppointmentResponse reschedule(AuthenticatedUser actor, Long id, RescheduleAppointmentRequest request);

    void cancel(AuthenticatedUser actor, Long id);
}
