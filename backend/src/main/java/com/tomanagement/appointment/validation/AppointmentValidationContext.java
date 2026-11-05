package com.tomanagement.appointment.validation;

import com.tomanagement.auth.security.AuthenticatedUser;

/**
 * Everything the booking rules need: who acts and what is being booked.
 */
public record AppointmentValidationContext(AuthenticatedUser actor, AppointmentDraft draft) {
}
