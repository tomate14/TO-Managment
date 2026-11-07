package com.tomanagement.establishment.dto;

import java.time.LocalDate;

/** Last day of trial chosen by the superadmin; {@code null} removes the expiry. */
public record ChangeTrialEndRequest(LocalDate trialEndsOn) {
}
