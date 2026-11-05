package com.tomanagement.appointment.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import com.tomanagement.appointment.entity.Appointment;

import org.springframework.data.jpa.domain.Specification;

/**
 * Composable filters for the agenda listing.
 */
public final class AppointmentSpecifications {

    private AppointmentSpecifications() {
    }

    /** Appointments starting from {@code from} 00:00 up to the end of {@code to}. */
    public static Specification<Appointment> startsBetween(LocalDate from, LocalDate to) {
        return (root, query, builder) -> builder.and(
                builder.greaterThanOrEqualTo(root.<LocalDateTime>get("startAt"), from.atStartOfDay()),
                builder.lessThan(root.<LocalDateTime>get("startAt"), to.plusDays(1).atStartOfDay()));
    }

    public static Specification<Appointment> ofTherapists(Set<Long> therapistIds) {
        return (root, query, builder) -> root.get("therapist").get("id").in(therapistIds);
    }
}
